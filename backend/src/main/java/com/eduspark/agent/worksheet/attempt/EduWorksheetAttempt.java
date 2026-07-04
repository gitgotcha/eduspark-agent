package com.eduspark.agent.worksheet.attempt;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("edu_worksheet_attempt")
public class EduWorksheetAttempt {

  @TableId private String id;
  private String userId;
  private String worksheetId;
  private String answersJson;
  private String gradingResultJson;
  private BigDecimal score;
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

  public String getWorksheetId() {
    return worksheetId;
  }

  public void setWorksheetId(String worksheetId) {
    this.worksheetId = worksheetId;
  }

  public String getAnswersJson() {
    return answersJson;
  }

  public void setAnswersJson(String answersJson) {
    this.answersJson = answersJson;
  }

  public String getGradingResultJson() {
    return gradingResultJson;
  }

  public void setGradingResultJson(String gradingResultJson) {
    this.gradingResultJson = gradingResultJson;
  }

  public BigDecimal getScore() {
    return score;
  }

  public void setScore(BigDecimal score) {
    this.score = score;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
