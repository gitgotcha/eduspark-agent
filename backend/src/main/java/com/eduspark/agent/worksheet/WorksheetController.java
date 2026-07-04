package com.eduspark.agent.worksheet;

import com.eduspark.agent.worksheet.dto.WorksheetCreateRequest;
import com.eduspark.agent.worksheet.dto.WorksheetCreateResponse;
import com.eduspark.agent.worksheet.dto.WorksheetDetailResponse;
import com.eduspark.agent.worksheet.dto.WorksheetListItemResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/worksheets")
public class WorksheetController {

  private static final MediaType WORD_MEDIA_TYPE =
      MediaType.parseMediaType(
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

  private final WorksheetService worksheetService;
  private final WordExportService wordExportService;

  public WorksheetController(
      WorksheetService worksheetService, WordExportService wordExportService) {
    this.worksheetService = worksheetService;
    this.wordExportService = wordExportService;
  }

  @PostMapping
  public WorksheetCreateResponse create(
      @PathVariable String userId, @Valid @RequestBody WorksheetCreateRequest request) {
    return worksheetService.create(userId, request);
  }

  @GetMapping("/{worksheetId}")
  public WorksheetDetailResponse findDetail(
      @PathVariable String userId, @PathVariable String worksheetId) {
    return worksheetService.findDetail(userId, worksheetId);
  }

  @GetMapping
  public List<WorksheetListItemResponse> list(
      @PathVariable String userId, @RequestParam(defaultValue = "20") int limit) {
    return worksheetService.list(userId, limit);
  }

  @GetMapping("/{worksheetId}/export.docx")
  public ResponseEntity<byte[]> exportWord(
      @PathVariable String userId, @PathVariable String worksheetId) {
    WorksheetDetailResponse worksheet = worksheetService.findDetail(userId, worksheetId);
    return ResponseEntity.ok()
        .contentType(WORD_MEDIA_TYPE)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(sanitizeFileName(worksheet.title()) + ".docx", StandardCharsets.UTF_8)
                .build()
                .toString())
        .body(wordExportService.export(worksheet));
  }

  private String sanitizeFileName(String title) {
    return title.replaceAll("[\\\\/:*?\"<>|]", "_");
  }
}
