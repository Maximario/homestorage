package ru.homestorage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.homestorage.config.JwtUtil;
import ru.homestorage.dto.request.LoginRequest;
import ru.homestorage.dto.request.RegisterRequest;
import ru.homestorage.dto.response.AuthResponse;
import ru.homestorage.exception.DuplicateResourceException;
import ru.homestorage.model.User;
import ru.homestorage.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final AuthenticationManager authenticationManager;

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new DuplicateResourceException("User with email " + request.getEmail() + " already exists");
    }

    User user = User.builder()
        .email(request.getEmail())
        .passwordHash(passwordEncoder.encode(request.getPassword()))
        .fullName(request.getFullName())
        .isActive(true)
        .build();

    user = userRepository.save(user);

    String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
    String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

    return AuthResponse.builder()
        .userId(user.getId())
        .email(user.getEmail())
        .fullName(user.getFullName())
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .build();
  }

  public AuthResponse login(LoginRequest request) {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
    );

    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    User user = userRepository.findByEmail(userDetails.getEmail())
        .orElseThrow(() -> new RuntimeException("User not found"));

    String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
    String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

    return AuthResponse.builder()
        .userId(user.getId())
        .email(user.getEmail())
        .fullName(user.getFullName())
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .build();
  }

  public AuthResponse refreshToken(String refreshToken) {
    String email = jwtUtil.extractEmail(refreshToken);
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found"));

    if (jwtUtil.validateToken(refreshToken, email)) {
      String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
      return AuthResponse.builder()
          .userId(user.getId())
          .email(user.getEmail())
          .fullName(user.getFullName())
          .accessToken(newAccessToken)
          .build();
    }
    throw new RuntimeException("Invalid refresh token");
  }
}