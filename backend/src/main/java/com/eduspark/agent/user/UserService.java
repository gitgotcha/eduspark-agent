package com.eduspark.agent.user;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
    this.userMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public EduUser register(String username, String password) {
    String normalizedUsername = username.trim();
    validatePassword(password);
    if (userMapper.selectByUsername(normalizedUsername) != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
    }

    EduUser user = new EduUser();
    user.setId(UUID.randomUUID().toString());
    user.setUsername(normalizedUsername);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setCreatedAt(LocalDateTime.now());
    userMapper.insert(user);
    return user;
  }

  public EduUser authenticate(String username, String password) {
    validatePassword(password);
    EduUser user = userMapper.selectByUsername(username.trim());
    if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }
    return user;
  }

  @Transactional
  public void changePassword(String userId, String oldPassword, String newPassword) {
    validatePassword(oldPassword);
    validatePassword(newPassword);
    EduUser user = userMapper.selectById(userId);
    if (user == null || !passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    user.setPasswordHash(passwordEncoder.encode(newPassword));
    userMapper.updateById(user);
  }

  private void validatePassword(String password) {
    if (password == null || password.length() < 6 || password.length() > 128) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be 6-128 characters");
    }
  }
}
