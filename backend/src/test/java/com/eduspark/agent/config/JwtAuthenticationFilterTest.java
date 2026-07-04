package com.eduspark.agent.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtAuthenticationFilterTest {

  @Test
  void skipsAsyncDispatchesAfterSseRequestHasStarted() {
    ExposedJwtAuthenticationFilter filter = new ExposedJwtAuthenticationFilter();

    assertThat(filter.skipsAsyncDispatch()).isTrue();
  }

  @Test
  void skipsErrorDispatchesAfterSseResponseHasCommitted() {
    ExposedJwtAuthenticationFilter filter = new ExposedJwtAuthenticationFilter();

    assertThat(filter.skipsErrorDispatch()).isTrue();
  }

  private static class ExposedJwtAuthenticationFilter extends JwtAuthenticationFilter {
    ExposedJwtAuthenticationFilter() {
      super(null);
    }

    boolean skipsAsyncDispatch() {
      return shouldNotFilterAsyncDispatch();
    }

    boolean skipsErrorDispatch() {
      return shouldNotFilterErrorDispatch();
    }
  }
}
