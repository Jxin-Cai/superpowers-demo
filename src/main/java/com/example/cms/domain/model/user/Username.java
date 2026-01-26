package com.example.cms.domain.model.user;

import lombok.Value;

@Value
public class Username {
    String value;

    private Username(String value) {
        if (value == null || value.isBlank() || value.length() < 3 || value.length() > 50) {
            throw new IllegalArgumentException("Username must be between 3 and 50 characters");
        }
        this.value = value.trim();
    }

    public static Username of(String value) {
        return new Username(value);
    }
}
