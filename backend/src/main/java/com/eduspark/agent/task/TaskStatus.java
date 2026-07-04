package com.eduspark.agent.task;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum TaskStatus {
  PENDING("PENDING"),
  PLANNING("PLANNING"),
  EXECUTING("EXECUTING"),
  REVIEWING("REVIEWING"),
  COMPLETED("COMPLETED"),
  FAILED("FAILED");

  @EnumValue private final String value;

  TaskStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
