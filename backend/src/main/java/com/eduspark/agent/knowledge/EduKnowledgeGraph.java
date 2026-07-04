package com.eduspark.agent.knowledge;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("edu_knowledge_graph")
public class EduKnowledgeGraph {

  @TableId private String id;
  private String userId;
  private String taskId;
  private String title;
  private String documentIdsJson;
  private String graphJson;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDocumentIdsJson() {
    return documentIdsJson;
  }

  public void setDocumentIdsJson(String documentIdsJson) {
    this.documentIdsJson = documentIdsJson;
  }

  public String getGraphJson() {
    return graphJson;
  }

  public void setGraphJson(String graphJson) {
    this.graphJson = graphJson;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
