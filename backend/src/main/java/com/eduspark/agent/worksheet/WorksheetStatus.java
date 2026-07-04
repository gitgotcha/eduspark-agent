package com.eduspark.agent.worksheet;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum WorksheetStatus {
  PENDING("PENDING"),
  GENERATING("GENERATING"),
  COMPLETED("COMPLETED"),
  FAILED("FAILED");

  @EnumValue private final String value;

  WorksheetStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
