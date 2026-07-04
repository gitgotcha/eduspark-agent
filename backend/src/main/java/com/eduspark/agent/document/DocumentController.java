package com.eduspark.agent.document;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/{userId}/documents")
public class DocumentController {

  private final DocumentService documentService;

  public DocumentController(DocumentService documentService) {
    this.documentService = documentService;
  }

  @PostMapping
  public EduDocument uploadDocument(
      @PathVariable String userId, @RequestParam("file") MultipartFile file) {
    return documentService.upload(userId, file);
  }

  @GetMapping
  public List<EduDocument> listDocuments(@PathVariable String userId) {
    return documentService.listDocuments(userId);
  }

  @GetMapping("/{documentId}")
  public EduDocument getDocument(@PathVariable String userId, @PathVariable String documentId) {
    return documentService.findDocumentOrThrow(userId, documentId);
  }

  @DeleteMapping("/{documentId}")
  public ResponseEntity<Void> deleteDocument(
      @PathVariable String userId, @PathVariable String documentId) {
    documentService.deleteDocument(userId, documentId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{documentId}/reindex")
  public EduDocument reindexDocument(@PathVariable String userId, @PathVariable String documentId) {
    return documentService.reindex(userId, documentId);
  }
}
