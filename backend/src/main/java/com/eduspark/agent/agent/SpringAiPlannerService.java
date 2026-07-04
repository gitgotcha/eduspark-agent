package com.eduspark.agent.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "agent.planner.mode", havingValue = "spring-ai")
public class SpringAiPlannerService implements PlannerService {

  private static final String SYSTEM_PROMPT =
      """
      You are an objective, rigorous education task planning engine.
      Break a user instruction into executable Java tool steps.
      Only use known tool names: textSummaryTool, quizGeneratorTool.
      Return JSON only.
      """;

  private final ChatClient chatClient;
  private final TaskPlanOutputParser parser;

  public SpringAiPlannerService(ChatClient.Builder chatClientBuilder, TaskPlanOutputParser parser) {
    this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
    this.parser = parser;
  }

  @Override
  public TaskPlan plan(String instruction) {
    String rawOutput =
        chatClient
            .prompt()
            .user(
                """
                Instruction: %s

                Produce a task plan.

                %s
                """
                    .formatted(instruction, parser.format()))
            .call()
            .content();

    return parser.parse(rawOutput);
  }
}
