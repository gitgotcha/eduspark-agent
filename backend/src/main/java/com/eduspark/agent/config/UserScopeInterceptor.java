package com.eduspark.agent.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserScopeInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    String path = request.getRequestURI();
    if (request.getDispatcherType() != DispatcherType.REQUEST) {
      return true;
    }

    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return true;
    }

    if (!path.startsWith("/api/users/")) {
      return true;
    }

    String[] segments = path.split("/");
    if (segments.length < 4) {
      return true;
    }

    String pathUserId = segments[3];
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && pathUserId.equals(authentication.getName())) {
      return true;
    }

    response.sendError(HttpServletResponse.SC_FORBIDDEN, "User scope mismatch");
    return false;
  }
}
