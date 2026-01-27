package com.example.cms.domain.model.user;

import lombok.Value;

import java.util.Optional;

@Value
public class Email {
    String value;

    private Email(String value) {
        if (value != null && !value.isBlank()) {
            String trimmed = value.trim();
            if (!isValidEmail(trimmed)) {
                throw new IllegalArgumentException("Invalid email format");
            }
            this.value = trimmed;
        } else {
            this.value = null;
        }
    }

    public static Email of(String value) {
        return new Email(value);
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public static Email ofNullable(String value) {
        return value == null || value.isBlank() ? null : of(value);
    }
}
