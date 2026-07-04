package com.eduspark.agent.agent;

public class InvalidTaskPlanException extends RuntimeException {

  public InvalidTaskPlanException(String message) {
    super(message);
  }

  public InvalidTaskPlanException(String message, Throwable cause) {
    super(message, cause);
  }
}
