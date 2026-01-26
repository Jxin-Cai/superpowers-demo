package com.example.cms.presentation.dto;

import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String status;

    public static UserResponse from(com.example.cms.domain.model.user.User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId().getValue());
        response.setUsername(user.getUsername().getValue());
        user.getEmailValue().ifPresent(response::setEmail);
        response.setRole(user.getRole().getValue());
        response.setStatus(user.getStatus().getValue());
        return response;
    }
}
