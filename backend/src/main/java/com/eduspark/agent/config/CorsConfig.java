package com.eduspark.agent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

  private final UserScopeInterceptor userScopeInterceptor;

  public CorsConfig(UserScopeInterceptor userScopeInterceptor) {
    this.userScopeInterceptor = userScopeInterceptor;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*", "http://[::1]:*")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*");
  }

  @Override
  public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
    registry.addInterceptor(userScopeInterceptor).addPathPatterns("/api/users/**");
  }
}
