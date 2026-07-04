package com.eduspark.agent.auth;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void registersUserAndReturnsJwtSession() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"teacher-register\",\"password\":\"secret123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").isNotEmpty())
        .andExpect(jsonPath("$.username").value("teacher-register"))
        .andExpect(jsonPath("$.token", startsWith("ey")));
  }

  @Test
  void rejectsDuplicateUsername() throws Exception {
    String body = "{\"username\":\"teacher-duplicate\",\"password\":\"secret123\"}";
    mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));

    mockMvc
        .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("USERNAME_EXISTS"));
  }

  @Test
  void logsInExistingUser() throws Exception {
    mockMvc.perform(
        post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"teacher-login\",\"password\":\"secret123\"}"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"teacher-login\",\"password\":\"secret123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").isNotEmpty())
        .andExpect(jsonPath("$.username").value("teacher-login"))
        .andExpect(jsonPath("$.token", startsWith("ey")));
  }

  @Test
  void rejectsWrongPassword() throws Exception {
    mockMvc.perform(
        post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"teacher-wrong-password\",\"password\":\"secret123\"}"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"teacher-wrong-password\",\"password\":\"wrong123\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code", not("VALIDATION_FAILED")));
  }
}
