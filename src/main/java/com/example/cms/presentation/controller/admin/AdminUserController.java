package com.example.cms.presentation.controller.admin;

import com.example.cms.application.service.UserService;
import com.example.cms.domain.model.user.Role;
import com.example.cms.domain.model.user.User;
import com.example.cms.presentation.dto.ApiResponse;
import com.example.cms.presentation.dto.UserRequest;
import com.example.cms.presentation.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<List<UserResponse>> getAll() {
        List<User> users = userService.findAll();
        return ApiResponse.success(users.stream()
                .map(UserResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getById(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ApiResponse.success(UserResponse.from(user)))
                .orElse(ApiResponse.error("用户不存在: " + id));
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserRequest request) {
        try {
            Role role = request.getRole() != null
                    ? Role.from(request.getRole())
                    : Role.USER;
            User user = "ADMIN".equals(role.getValue())
                    ? userService.createAdmin(request.getUsername(), request.getPassword(), request.getEmail())
                    : userService.register(request.getUsername(), request.getPassword(), request.getEmail());
            return ApiResponse.success(UserResponse.from(user));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @RequestBody UserRequest request) {
        try {
            if (request.getRole() != null) {
                Role newRole = Role.from(request.getRole());
                User user = userService.updateRole(id, newRole);
                return ApiResponse.success(UserResponse.from(user));
            }
            return ApiResponse.error("无更新内容");
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success(null);
    }
}
