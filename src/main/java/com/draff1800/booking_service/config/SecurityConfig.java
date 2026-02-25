package com.draff1800.booking_service.config;

import com.draff1800.booking_service.security.SecurityErrorHandlers;
import com.draff1800.booking_service.security.jwt.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
    HttpSecurity http, 
    JwtAuthFilter jwtAuthFilter,
    SecurityErrorHandlers securityErrorHandlers
  ) throws Exception {

    return http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(ex -> ex
          .authenticationEntryPoint(securityErrorHandlers)
          .accessDeniedHandler(securityErrorHandlers)
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/health", "/auth/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/events/**").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
