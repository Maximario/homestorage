package ru.homestorage.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.homestorage.model.User;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

  private final UUID userId;
  private final String email;
  private final String passwordHash;
  private final boolean isActive;

  public static CustomUserDetails fromUser(User user) {
    return new CustomUserDetails(
        user.getId(),
        user.getEmail(),
        user.getPasswordHash(),
        user.getIsActive()
    );
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
  }

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return isActive;
  }

  @Override
  public boolean isAccountNonLocked() {
    return isActive;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return isActive;
  }

  @Override
  public boolean isEnabled() {
    return isActive;
  }
}