package com.eduspark.agent.worksheet;

import com.eduspark.agent.worksheet.dto.GeneratedWorksheetQuestion;
import com.eduspark.agent.worksheet.dto.WorksheetCreateRequest;
import com.eduspark.agent.worksheet.dto.WorksheetGenerationRationale;
import com.eduspark.agent.worksheet.dto.WorksheetGenerationResult;
import java.util.List;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

@Component
public class WorksheetQuestionOutputParser {

  private final BeanOutputConverter<WorksheetGenerationResult> converter =
      new BeanOutputConverter<>(WorksheetGenerationResult.class);

  public String format() {
    return converter.getFormat();
  }

  public WorksheetGenerationResult parse(String rawOutput, WorksheetCreateRequest request) {
    try {
      WorksheetGenerationResult result = converter.convert(rawOutput);
      validate(result, request);
      return result;
    } catch (InvalidWorksheetGenerationException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new InvalidWorksheetGenerationException(
          "Model output cannot be parsed as worksheet questions", exception);
    }
  }

  private void validate(WorksheetGenerationResult result, WorksheetCreateRequest request) {
    if (result == null) {
      throw new InvalidWorksheetGenerationException("WorksheetGenerationResult must not be null");
    }
    if (result.questions() == null || result.questions().isEmpty()) {
      throw new InvalidWorksheetGenerationException("WorksheetGenerationResult must contain questions");
    }
    validateRationale(result.generationRationale());
    validateQuestionCount(result.questions().size(), request.questionCount());

    for (int index = 0; index < result.questions().size(); index++) {
      validateQuestion(result.questions().get(index), index, request.includeExplanation());
    }
  }

  private void validateRationale(WorksheetGenerationRationale rationale) {
    if (rationale == null) {
      throw new InvalidWorksheetGenerationException(
          "WorksheetGenerationResult must contain generationRationale");
    }
    if (rationale.summary() == null || rationale.summary().isBlank()) {
      throw new InvalidWorksheetGenerationException("generationRationale must contain summary");
    }
    if (rationale.keyPoints() == null || rationale.keyPoints().isEmpty()) {
      throw new InvalidWorksheetGenerationException("generationRationale must contain keyPoints");
    }
    if (rationale.difficultyPlan() == null || rationale.difficultyPlan().isBlank()) {
      throw new InvalidWorksheetGenerationException(
          "generationRationale must contain difficultyPlan");
    }
    if (rationale.typePlan() == null || rationale.typePlan().isBlank()) {
      throw new InvalidWorksheetGenerationException("generationRationale must contain typePlan");
    }
  }

  private void validateQuestionCount(int actualCount, int requestedCount) {
    int minimum = Math.max(1, Math.round(requestedCount * 0.8f));
    int maximum = Math.max(minimum, Math.round(requestedCount * 1.2f));
    if (actualCount < minimum || actualCount > maximum) {
      throw new InvalidWorksheetGenerationException(
          "Expected question count between "
              + minimum
              + " and "
              + maximum
              + " but received "
              + actualCount);
    }
  }

  private void validateQuestion(
      GeneratedWorksheetQuestion question, int index, boolean includeExplanation) {
    if (question == null) {
      throw new InvalidWorksheetGenerationException("Question " + index + " must not be null");
    }
    if (question.type() == null || question.type().isBlank()) {
      throw new InvalidWorksheetGenerationException("Question " + index + " must contain type");
    }
    if (question.stem() == null || question.stem().isBlank()) {
      throw new InvalidWorksheetGenerationException("Question " + index + " must contain stem");
    }
    if (question.answer() == null || question.answer().isBlank()) {
      throw new InvalidWorksheetGenerationException("Question " + index + " must contain answer");
    }
    if (includeExplanation && (question.explanation() == null || question.explanation().isBlank())) {
      throw new InvalidWorksheetGenerationException(
          "Question " + index + " must contain explanation");
    }
    if (question.difficulty() == null || question.difficulty().isBlank()) {
      throw new InvalidWorksheetGenerationException(
          "Question " + index + " must contain difficulty");
    }

    if (isChoiceQuestion(question.type())) {
      validateChoiceQuestion(question, index);
    } else if (isJudgmentQuestion(question.type())) {
      validateJudgmentQuestion(question, index);
    } else {
      validateNonBlankOptions(question.options(), index);
    }
  }

  private void validateChoiceQuestion(GeneratedWorksheetQuestion question, int index) {
    List<String> options = question.options();
    if (options == null || options.size() != 4) {
      throw new InvalidWorksheetGenerationException(
          "Question " + index + " choice question must contain exactly 4 options");
    }
    validateNonBlankOptions(options, index);

    String[] expectedPrefixes = {"A.", "B.", "C.", "D."};
    for (int optionIndex = 0; optionIndex < expectedPrefixes.length; optionIndex++) {
      if (!options.get(optionIndex).startsWith(expectedPrefixes[optionIndex])) {
        throw new InvalidWorksheetGenerationException(
            "Question "
                + index
                + " options item "
                + optionIndex
                + " must start with "
                + expectedPrefixes[optionIndex]);
      }
    }

    if (!List.of("A", "B", "C", "D").contains(question.answer().trim())) {
      throw new InvalidWorksheetGenerationException(
          "Question " + index + " choice question answer must be A, B, C, or D");
    }
  }

  private void validateJudgmentQuestion(GeneratedWorksheetQuestion question, int index) {
    List<String> expectedOptions = List.of("A. 正确", "B. 错误");
    List<String> expectedEnglishOptions = List.of("A. True", "B. False");
    if (!expectedOptions.equals(question.options()) && !expectedEnglishOptions.equals(question.options())) {
      throw new InvalidWorksheetGenerationException(
          "Question " + index + " judgment question options must be [A. 正确, B. 错误] or [A. True, B. False]");
    }
    if (!List.of("A", "B").contains(question.answer().trim())) {
      throw new InvalidWorksheetGenerationException(
          "Question " + index + " judgment question answer must be A or B");
    }
  }

  private void validateNonBlankOptions(List<String> options, int index) {
    if (options == null || options.isEmpty()) {
      return;
    }
    for (int optionIndex = 0; optionIndex < options.size(); optionIndex++) {
      String option = options.get(optionIndex);
      if (option == null || option.isBlank()) {
        throw new InvalidWorksheetGenerationException(
            "Question " + index + " options item " + optionIndex + " must not be blank");
      }
    }
  }

  private boolean isChoiceQuestion(String type) {
    return type.contains("选择");
  }

  private boolean isJudgmentQuestion(String type) {
    return type.contains("判断");
  }
}
