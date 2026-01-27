package com.example.cms.presentation.controller.auth;

import com.example.cms.application.service.UserService;
import com.example.cms.domain.model.user.User;
import com.example.cms.presentation.dto.ApiResponse;
import com.example.cms.presentation.dto.LoginRequest;
import com.example.cms.presentation.dto.RegisterRequest;
import com.example.cms.presentation.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        return userService.findByUsername(request.getUsername())
                .filter(User::isActive)
                .map(user -> ApiResponse.success(UserResponse.from(user)))
                .orElse(ApiResponse.error("用户名或密码错误"));
    }

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.register(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail()
            );
            return ApiResponse.success(UserResponse.from(user));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/current")
    public ApiResponse<UserResponse> getCurrentUser() {
        org.springframework.security.core.Authentication auth =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !"anonymousUser".equals(auth.getPrincipal())) {
            return userService.findByUsername(auth.getName())
                    .map(user -> ApiResponse.success(UserResponse.from(user)))
                    .orElse(ApiResponse.error("用户不存在"));
        }
        return ApiResponse.error("未登录");
    }
}
