package com.eduspark.agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan({
  "com.eduspark.agent.task",
  "com.eduspark.agent.document",
  "com.eduspark.agent.knowledge",
  "com.eduspark.agent.user",
  "com.eduspark.agent.vector",
  "com.eduspark.agent.worksheet"
})
@SpringBootApplication
public class EduSparkAgentApplication {

  public static void main(String[] args) {
    SpringApplication.run(EduSparkAgentApplication.class, args);
  }
}
