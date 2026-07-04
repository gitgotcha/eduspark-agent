package com.eduspark.agent.worksheet;

import com.eduspark.agent.worksheet.dto.WorksheetDetailResponse;
import com.eduspark.agent.worksheet.dto.WorksheetGenerationRationale;
import com.eduspark.agent.worksheet.dto.WorksheetQuestion;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

@Service
public class WordExportService {

  public byte[] export(WorksheetDetailResponse worksheet) {
    try (XWPFDocument document = new XWPFDocument();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      writeTitle(document, worksheet.title());
      writeMeta(document, worksheet);
      writeGenerationRationale(document, worksheet.generationRationale());
      writeQuestions(document, worksheet.questions());
      writeAnswers(document, worksheet.questions(), worksheet.config().includeExplanation());
      document.write(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to export worksheet document", exception);
    }
  }

  private void writeTitle(XWPFDocument document, String title) {
    XWPFParagraph paragraph = document.createParagraph();
    paragraph.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun run = paragraph.createRun();
    run.setBold(true);
    run.setFontSize(18);
    run.setText(title);
  }

  private void writeMeta(XWPFDocument document, WorksheetDetailResponse worksheet) {
    writeLine(
        document,
        "年级：" + worksheet.config().gradeLevel() + "    难度：" + worksheet.config().difficulty());
    writeLine(document, "题量：" + worksheet.questions().size() + "    状态：" + worksheet.status());
    writeSpacer(document);
  }

  private void writeQuestions(XWPFDocument document, List<WorksheetQuestion> questions) {
    writeHeading(document, "一、题目");
    for (int index = 0; index < questions.size(); index++) {
      WorksheetQuestion question = questions.get(index);
      writeLine(document, (index + 1) + ". [" + question.type() + "] " + question.stem());
      if (question.options() != null) {
        for (String option : question.options()) {
          writeLine(document, "   " + option);
        }
      }
      writeSpacer(document);
    }
  }

  private void writeGenerationRationale(
      XWPFDocument document, WorksheetGenerationRationale rationale) {
    if (rationale == null) {
      return;
    }
    writeHeading(document, "AI 出题依据");
    writeLabeledLine(document, "摘要：", rationale.summary());
    if (rationale.keyPoints() != null && !rationale.keyPoints().isEmpty()) {
      writeLine(document, "关键点：");
      for (String keyPoint : rationale.keyPoints()) {
        writeLabeledLine(document, "   - ", keyPoint);
      }
    }
    writeLabeledLine(document, "难度规划：", rationale.difficultyPlan());
    writeLabeledLine(document, "题型规划：", rationale.typePlan());
    writeLabeledLine(document, "偏离说明：", rationale.deviationFromPreference());
    writeSpacer(document);
  }

  private void writeAnswers(
      XWPFDocument document, List<WorksheetQuestion> questions, boolean includeExplanation) {
    writeHeading(document, "二、答案与解析");
    for (int index = 0; index < questions.size(); index++) {
      WorksheetQuestion question = questions.get(index);
      writeLine(document, (index + 1) + ". 答案：" + question.answer());
      if (includeExplanation && question.explanation() != null && !question.explanation().isBlank()) {
        writeLine(document, "   解析：" + question.explanation());
      }
    }
  }

  private void writeHeading(XWPFDocument document, String text) {
    XWPFParagraph paragraph = document.createParagraph();
    XWPFRun run = paragraph.createRun();
    run.setBold(true);
    run.setFontSize(14);
    run.setText(text);
  }

  private void writeLine(XWPFDocument document, String text) {
    XWPFParagraph paragraph = document.createParagraph();
    XWPFRun run = paragraph.createRun();
    run.setFontSize(11);
    run.setText(text);
  }

  private void writeLabeledLine(XWPFDocument document, String label, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    writeLine(document, label + value);
  }

  private void writeSpacer(XWPFDocument document) {
    document.createParagraph();
  }
}
