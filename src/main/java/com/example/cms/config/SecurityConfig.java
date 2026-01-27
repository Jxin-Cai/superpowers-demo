package com.example.cms.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )
            .authorizeHttpRequests(auth -> auth
                // 静态资源和前端路由放行（让前端Vue Router处理）
                .requestMatchers("/", "/login", "/register", "/admin", "/admin/**").permitAll()
                .requestMatchers("/assets/**", "/favicon.ico").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // 公开API
                .requestMatchers("/api/public/**").permitAll()
                // 认证API
                .requestMatchers("/api/auth/**").permitAll()
                // 管理员API（需要ADMIN角色）
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 其他API请求需要认证
                .requestMatchers("/api/**").authenticated()
                // 其他所有请求允许访问（前端路由）
                .anyRequest().permitAll()
            )
            .httpBasic(withDefaults())
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessUrl("/login")
                .permitAll()
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
