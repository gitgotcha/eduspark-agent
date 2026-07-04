package com.eduspark.agent.document;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("edu_document")
public class EduDocument {

  @TableId private String id;
  private String userId;
  private String taskId;
  private String fileName;
  private String mimeType;
  private String storagePath;
  private String extractedText;
  private String parseStatus;
  private String parseError;
  private LocalDateTime indexedAt;
  private String textPreview;
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

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getStoragePath() {
    return storagePath;
  }

  public void setStoragePath(String storagePath) {
    this.storagePath = storagePath;
  }

  public String getExtractedText() {
    return extractedText;
  }

  public void setExtractedText(String extractedText) {
    this.extractedText = extractedText;
  }

  public String getParseStatus() {
    return parseStatus;
  }

  public void setParseStatus(String parseStatus) {
    this.parseStatus = parseStatus;
  }

  public String getParseError() {
    return parseError;
  }

  public void setParseError(String parseError) {
    this.parseError = parseError;
  }

  public LocalDateTime getIndexedAt() {
    return indexedAt;
  }

  public void setIndexedAt(LocalDateTime indexedAt) {
    this.indexedAt = indexedAt;
  }

  public String getTextPreview() {
    return textPreview;
  }

  public void setTextPreview(String textPreview) {
    this.textPreview = textPreview;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
