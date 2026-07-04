package com.eduspark.agent.knowledge;

import com.eduspark.agent.knowledge.dto.KnowledgeGraphCreateRequest;
import com.eduspark.agent.knowledge.dto.KnowledgeGraphCreateResponse;
import com.eduspark.agent.knowledge.dto.KnowledgeGraphRecord;
import com.eduspark.agent.knowledge.dto.KnowledgeGraphResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/knowledge-graph")
public class KnowledgeGraphController {

  private final KnowledgeGraphService graphService;

  public KnowledgeGraphController(KnowledgeGraphService graphService) {
    this.graphService = graphService;
  }

  @GetMapping
  public KnowledgeGraphResponse getGraph(
      @PathVariable String userId, @RequestParam(defaultValue = "12") int limit) {
    return graphService.buildGraph(userId, Math.min(Math.max(limit, 1), 30));
  }
}
