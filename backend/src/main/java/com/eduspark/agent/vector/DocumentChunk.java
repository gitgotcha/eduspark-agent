package com.eduspark.agent.vector;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("edu_document_chunk")
public class DocumentChunk {

  @TableId private String id;
  private String userId;
  private String documentId;
  private Integer chunkIndex;
  private String content;
  private String embeddingJson;
  private Integer tokenCount;
  private LocalDateTime createdAt;

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

  public String getDocumentId() {
    return documentId;
  }

  public void setDocumentId(String documentId) {
    this.documentId = documentId;
  }

  public Integer getChunkIndex() {
    return chunkIndex;
  }

  public void setChunkIndex(Integer chunkIndex) {
    this.chunkIndex = chunkIndex;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getEmbeddingJson() {
    return embeddingJson;
  }

  public void setEmbeddingJson(String embeddingJson) {
    this.embeddingJson = embeddingJson;
  }

  public Integer getTokenCount() {
    return tokenCount;
  }

  public void setTokenCount(Integer tokenCount) {
    this.tokenCount = tokenCount;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
