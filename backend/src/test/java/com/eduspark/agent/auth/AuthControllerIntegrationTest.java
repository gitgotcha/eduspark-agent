package com.eduspark.agent.auth;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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

  @Test
  void rejectsShortPasswordOnRegisterAndLogin() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"teacher-short-register\",\"password\":\"12345\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"teacher-short-login\",\"password\":\"12345\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  void changesPasswordWithOldPasswordVerification() throws Exception {
    AuthSession session = register("teacher-change-password", "secret123");

    mockMvc
        .perform(
            post("/api/users/{userId}/auth/change-password", session.userId())
                .header("Authorization", "Bearer " + session.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"wrong123\",\"newPassword\":\"secret456\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

    mockMvc
        .perform(
            post("/api/users/{userId}/auth/change-password", session.userId())
                .header("Authorization", "Bearer " + session.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"secret123\",\"newPassword\":\"secret456\"}"))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"teacher-change-password\",\"password\":\"secret123\"}"))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"teacher-change-password\",\"password\":\"secret456\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token", startsWith("ey")));
  }

  private AuthSession register(String username, String password) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"username":"%s","password":"%s"}
                        """
                            .formatted(username, password)))
            .andExpect(status().isOk())
            .andReturn();
    String body = result.getResponse().getContentAsString();
    return new AuthSession(JsonPath.read(body, "$.userId"), JsonPath.read(body, "$.token"));
  }

  private record AuthSession(String userId, String token) {}
}
