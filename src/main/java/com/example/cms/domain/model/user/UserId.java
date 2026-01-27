package com.example.cms.domain.model.user;

import lombok.Value;

@Value
public class UserId {
    Long value;

    private UserId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Invalid user id");
        }
        this.value = value;
    }

    public static UserId of(Long value) {
        return new UserId(value);
    }
}
