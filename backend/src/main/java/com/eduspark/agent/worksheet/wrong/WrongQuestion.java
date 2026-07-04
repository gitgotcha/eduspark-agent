package com.eduspark.agent.worksheet.wrong;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("edu_wrong_question")
public class WrongQuestion {

  @TableId private String id;
  private String userId;
  private String worksheetId;
  private String attemptId;
  private String questionId;
  private String questionJson;
  private String submittedAnswer;
  private String correctAnswer;
  private String explanation;
  private String weaknessTag;
  private String retryWorksheetId;
  private Boolean resolved;
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

  public String getWorksheetId() {
    return worksheetId;
  }

  public void setWorksheetId(String worksheetId) {
    this.worksheetId = worksheetId;
  }

  public String getAttemptId() {
    return attemptId;
  }

  public void setAttemptId(String attemptId) {
    this.attemptId = attemptId;
  }

  public String getQuestionId() {
    return questionId;
  }

  public void setQuestionId(String questionId) {
    this.questionId = questionId;
  }

  public String getQuestionJson() {
    return questionJson;
  }

  public void setQuestionJson(String questionJson) {
    this.questionJson = questionJson;
  }

  public String getSubmittedAnswer() {
    return submittedAnswer;
  }

  public void setSubmittedAnswer(String submittedAnswer) {
    this.submittedAnswer = submittedAnswer;
  }

  public String getCorrectAnswer() {
    return correctAnswer;
  }

  public void setCorrectAnswer(String correctAnswer) {
    this.correctAnswer = correctAnswer;
  }

  public String getExplanation() {
    return explanation;
  }

  public void setExplanation(String explanation) {
    this.explanation = explanation;
  }

  public String getWeaknessTag() {
    return weaknessTag;
  }

  public void setWeaknessTag(String weaknessTag) {
    this.weaknessTag = weaknessTag;
  }

  public String getRetryWorksheetId() {
    return retryWorksheetId;
  }

  public void setRetryWorksheetId(String retryWorksheetId) {
    this.retryWorksheetId = retryWorksheetId;
  }

  public Boolean getResolved() {
    return resolved;
  }

  public void setResolved(Boolean resolved) {
    this.resolved = resolved;
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
