package ru.homestorage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(
                "http://localhost:3000",    // React / Vue / Angular (локально)
                "http://localhost:5173",    // Vite
                "http://localhost:4200",    // Angular
                "https://ваш-сайт.com",     // продакшн-фронт
                "exp://192.168.1.x:8081",   // Expo / React Native (локально)
                "capacitor://localhost",    // Ionic / Capacitor
                "http://localhost:8100"     // Ionic
            )
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
      }
    };
  }
}
