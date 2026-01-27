package com.example.cms.domain.model.user;

import lombok.Getter;

@Getter
public enum UserStatus {
    ACTIVE("ACTIVE"),
    DISABLED("DISABLED");

    private final String value;

    UserStatus(String value) {
        this.value = value;
    }

    public static UserStatus from(String value) {
        for (UserStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}
