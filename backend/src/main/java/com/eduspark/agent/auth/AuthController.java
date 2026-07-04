package com.eduspark.agent.auth;

import com.eduspark.agent.user.EduUser;
import com.eduspark.agent.user.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final UserService userService;
  private final JwtService jwtService;

  public AuthController(UserService userService, JwtService jwtService) {
    this.userService = userService;
    this.jwtService = jwtService;
  }

  @PostMapping("/register")
  public AuthResponse register(@Valid @RequestBody AuthRequest request) {
    EduUser user = userService.register(request.username(), request.password());
    return toResponse(user);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody AuthRequest request) {
    EduUser user = userService.authenticate(request.username(), request.password());
    return toResponse(user);
  }

  private AuthResponse toResponse(EduUser user) {
    return new AuthResponse(user.getId(), user.getUsername(), jwtService.createToken(user.getId(), user.getUsername()));
  }
}
