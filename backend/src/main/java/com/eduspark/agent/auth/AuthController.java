package com.eduspark.agent.auth;

import com.eduspark.agent.user.EduUser;
import com.eduspark.agent.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

  private final UserService userService;
  private final JwtService jwtService;

  public AuthController(UserService userService, JwtService jwtService) {
    this.userService = userService;
    this.jwtService = jwtService;
  }

  @PostMapping("/auth/register")
  public AuthResponse register(@Valid @RequestBody AuthRequest request) {
    EduUser user = userService.register(request.username(), request.password());
    return toResponse(user);
  }

  @PostMapping("/auth/login")
  public AuthResponse login(@Valid @RequestBody AuthRequest request) {
    EduUser user = userService.authenticate(request.username(), request.password());
    return toResponse(user);
  }

  @PostMapping("/users/{userId}/auth/change-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void changePassword(
      @PathVariable String userId, @Valid @RequestBody ChangePasswordRequest request) {
    userService.changePassword(userId, request.oldPassword(), request.newPassword());
  }

  private AuthResponse toResponse(EduUser user) {
    return new AuthResponse(user.getId(), user.getUsername(), jwtService.createToken(user.getId(), user.getUsername()));
  }
}
