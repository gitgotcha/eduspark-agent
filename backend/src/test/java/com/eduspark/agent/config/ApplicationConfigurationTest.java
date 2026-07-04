package com.eduspark.agent.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ApplicationConfigurationTest {

  @Test
  void applicationYamlDoesNotHardcodeProviderApiKeys() throws Exception {
    String yaml = Files.readString(Path.of("src/main/resources/application.yml"));

    assertThat(yaml).doesNotContain("sk-");
    assertThat(yaml).doesNotContain("eduSpark001");
    assertThat(yaml).doesNotContain("rm-bp11lptui94ztuts20o");
    assertThat(yaml).doesNotContain("eduspark-local-development-secret-key-change-me-please-32");
    assertThat(yaml).contains("url: ${SPRING_DATASOURCE_URL:}");
    assertThat(yaml).contains("username: ${SPRING_DATASOURCE_USERNAME:}");
    assertThat(yaml).contains("password: ${SPRING_DATASOURCE_PASSWORD:}");
    assertThat(yaml).contains("secret: ${AUTH_JWT_SECRET:}");
    assertThat(yaml).contains("api-key: ${DASHSCOPE_API_KEY:}");
    assertThat(yaml)
        .contains(
            "base-url: ${SPRING_AI_OPENAI_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode}");
    assertThat(yaml)
        .contains(
            "      chat:\n"
                + "        options:\n"
                + "          model: ${SPRING_AI_OPENAI_CHAT_MODEL:qwen-plus}");
    assertThat(yaml).contains("temperature: 0.3");
    assertThat(yaml)
        .contains(
            "      embedding:\n"
                + "        options:\n"
                + "          model: ${SPRING_AI_OPENAI_EMBEDDING_MODEL:text-embedding-v4}");
    assertThat(yaml).contains("mode: ${AGENT_PLANNER_MODE:mock}");
  }
}
