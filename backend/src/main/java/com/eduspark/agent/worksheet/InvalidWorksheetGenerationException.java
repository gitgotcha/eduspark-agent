package com.eduspark.agent.worksheet;

public class InvalidWorksheetGenerationException extends RuntimeException {

  public InvalidWorksheetGenerationException(String message) {
    super(message);
  }

  public InvalidWorksheetGenerationException(String message, Throwable cause) {
    super(message, cause);
  }
}
