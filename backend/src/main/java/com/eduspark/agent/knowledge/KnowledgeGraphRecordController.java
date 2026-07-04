package com.eduspark.agent.knowledge;

import com.eduspark.agent.knowledge.dto.KnowledgeGraphCreateRequest;
import com.eduspark.agent.knowledge.dto.KnowledgeGraphCreateResponse;
import com.eduspark.agent.knowledge.dto.KnowledgeGraphRecord;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/knowledge-graphs")
public class KnowledgeGraphRecordController {

  private final KnowledgeGraphService graphService;

  public KnowledgeGraphRecordController(KnowledgeGraphService graphService) {
    this.graphService = graphService;
  }

  @PostMapping
  public KnowledgeGraphCreateResponse createGraph(
      @PathVariable String userId, @Valid @RequestBody KnowledgeGraphCreateRequest request) {
    return graphService.create(userId, request);
  }

  @GetMapping
  public List<KnowledgeGraphRecord> listGraphs(@PathVariable String userId) {
    return graphService.list(userId);
  }

  @GetMapping("/{graphId}")
  public KnowledgeGraphRecord getGraphRecord(
      @PathVariable String userId, @PathVariable String graphId) {
    return graphService.findOrThrow(userId, graphId);
  }

  @DeleteMapping("/{graphId}")
  public ResponseEntity<Void> deleteGraph(
      @PathVariable String userId, @PathVariable String graphId) {
    graphService.delete(userId, graphId);
    return ResponseEntity.noContent().build();
  }
}
