package com.eduspark.agent.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class UserScopeInterceptorTest {

  private final UserScopeInterceptor interceptor = new UserScopeInterceptor();

  @Test
  void allowsCorsPreflightRequestsForUserScopedApis() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/users/user-1/documents/doc-1");
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean allowed = interceptor.preHandle(request, response, new Object());

    assertThat(allowed).isTrue();
    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
  }
}
