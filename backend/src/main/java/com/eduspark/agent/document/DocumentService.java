package com.eduspark.agent.document;

import com.eduspark.agent.vector.DocumentIndexService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentService {

  private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

  private final DocumentMapper documentMapper;
  private final DocumentIndexService documentIndexService;
  private final Path uploadRoot;

  public DocumentService(DocumentMapper documentMapper, DocumentIndexService documentIndexService) {
    this.documentMapper = documentMapper;
    this.documentIndexService = documentIndexService;
    this.uploadRoot = Path.of("var", "uploads", "documents").toAbsolutePath().normalize();
  }

  public EduDocument upload(MultipartFile file) {
    return upload("system-test-user", file);
  }

  public EduDocument upload(String userId, MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document file is required");
    }

    try {
      Files.createDirectories(uploadRoot);
      String documentId = UUID.randomUUID().toString();
      String originalFileName = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
      Path storagePath = uploadRoot.resolve(documentId + "-" + originalFileName).normalize();
      try (InputStream inputStream = file.getInputStream()) {
        Files.copy(inputStream, storagePath);
      }

      EduDocument document = new EduDocument();
      document.setId(documentId);
      document.setUserId(userId);
      document.setFileName(originalFileName);
      document.setMimeType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
      document.setStoragePath(storagePath.toString());
      String extractedText = extractText(file);
      document.setExtractedText(extractedText);
      document.setTextPreview(preview(extractedText));
      document.setParseStatus("PARSED");
      document.setCreatedAt(LocalDateTime.now());
      documentMapper.insert(document);
      try {
        documentIndexService.index(document);
        document.setIndexedAt(LocalDateTime.now());
        document.setParseError(null);
        documentMapper.updateById(document);
      } catch (RuntimeException exception) {
        log.warn("Document indexing failed for documentId={}", document.getId(), exception);
        document.setParseStatus("INDEX_FAILED");
        document.setParseError(exception.getMessage());
        documentMapper.updateById(document);
      }
      return document;
    } catch (IOException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document parse failed", exception);
    }
  }

  public EduDocument findDocumentOrThrow(String documentId) {
    EduDocument document = documentMapper.selectById(documentId);
    if (document == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
    }
    return document;
  }

  public EduDocument findDocumentOrThrow(String userId, String documentId) {
    EduDocument document = documentMapper.selectByIdAndUserId(documentId, userId);
    if (document == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
    }
    return document;
  }

  public List<EduDocument> listDocuments(String userId) {
    return documentMapper.selectByUserIdOrderByCreatedAtDesc(userId);
  }

  public void deleteDocument(String userId, String documentId) {
    int deleted = documentMapper.deleteByIdAndUserId(documentId, userId);
    if (deleted == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
    }
  }

  public EduDocument reindex(String userId, String documentId) {
    EduDocument document = findDocumentOrThrow(userId, documentId);
    try {
      documentIndexService.index(document);
      document.setParseStatus("PARSED");
      document.setParseError(null);
      document.setIndexedAt(LocalDateTime.now());
    } catch (RuntimeException exception) {
      document.setParseStatus("INDEX_FAILED");
      document.setParseError(exception.getMessage());
    }
    documentMapper.updateById(document);
    return document;
  }

  public void attachDocumentsToTask(List<String> documentIds, String taskId) {
    if (documentIds == null || documentIds.isEmpty()) {
      return;
    }
    for (String documentId : documentIds) {
      int updated = documentMapper.attachToTask(documentId, taskId);
      if (updated == 0) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
      }
    }
  }

  public void attachDocumentsToTask(String userId, List<String> documentIds, String taskId) {
    if (documentIds == null || documentIds.isEmpty()) {
      return;
    }
    for (String documentId : documentIds) {
      int updated = documentMapper.attachToTaskForUser(documentId, taskId, userId);
      if (updated == 0) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
      }
    }
  }

  public List<EduDocument> findDocumentsByTaskId(String taskId) {
    return documentMapper.selectByTaskId(taskId);
  }

  public List<EduDocument> findDocumentsByTaskId(String userId, String taskId) {
    return documentMapper.selectByTaskIdAndUserId(taskId, userId);
  }

  private String extractText(MultipartFile file) throws IOException {
    String mimeType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
    String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
    String text;
    if (mimeType.contains("pdf") || fileName.endsWith(".pdf")) {
      text = extractPdfText(file.getBytes());
    } else {
      text = new String(file.getBytes(), StandardCharsets.UTF_8);
    }
    text = normalizeText(text);
    if (!hasReadableContent(text)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Document does not contain readable text. Please upload a text-based PDF or TXT file.");
    }
    return text;
  }

  private String extractPdfText(byte[] bytes) throws IOException {
    try (PDDocument document = Loader.loadPDF(bytes)) {
      return new PDFTextStripper().getText(document);
    }
  }

  private String normalizeText(String text) {
    return text == null
        ? ""
        : text.replace('\u0000', ' ').replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ").trim();
  }

  private boolean hasReadableContent(String text) {
    if (text == null || text.length() < 5) {
      return false;
    }
    long readable = text.codePoints().filter(this::isReadableCodePoint).count();
    return readable >= 5 && readable >= text.codePointCount(0, text.length()) * 0.6;
  }

  private boolean isReadableCodePoint(int codePoint) {
    return Character.isLetterOrDigit(codePoint)
        || Character.isWhitespace(codePoint)
        || ".,;:!?()[]{}+-=*/%<>\"'`~@#$^&_|\\".indexOf(codePoint) >= 0
        || (codePoint >= 0x4E00 && codePoint <= 0x9FFF);
  }

  private String preview(String text) {
    if (text == null) {
      return "";
    }
    return text.length() <= 500 ? text : text.substring(0, 500);
  }
}
