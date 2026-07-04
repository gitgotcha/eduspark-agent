package com.eduspark.agent.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
    @NotBlank @Size(max = 64) String username, @NotBlank @Size(min = 6, max = 128) String password) {}
