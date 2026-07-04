package com.eduspark.agent.task;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("edu_task")
public class EduTask {

  @TableId private String id;
  private String userId;
  private String userInstruction;
  private TaskStatus status;
  private String planJson;
  private String finalAnswer;
  private String failureReason;
  private Integer retryCount;
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

  public String getUserInstruction() {
    return userInstruction;
  }

  public void setUserInstruction(String userInstruction) {
    this.userInstruction = userInstruction;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  public String getPlanJson() {
    return planJson;
  }

  public void setPlanJson(String planJson) {
    this.planJson = planJson;
  }

  public String getFinalAnswer() {
    return finalAnswer;
  }

  public void setFinalAnswer(String finalAnswer) {
    this.finalAnswer = finalAnswer;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public void setFailureReason(String failureReason) {
    this.failureReason = failureReason;
  }

  public Integer getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(Integer retryCount) {
    this.retryCount = retryCount;
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
