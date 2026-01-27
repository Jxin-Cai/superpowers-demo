package com.example.cms.domain.model.user;

import lombok.Value;

@Value
public class Password {
    String encodedValue;

    private static final ThreadLocal<java.security.SecureRandom> RANDOM = ThreadLocal.withInitial(java.security.SecureRandom::new);
    private static final int MIN_RAW_LENGTH = 6;

    private Password(String encodedValue) {
        if (encodedValue == null || encodedValue.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        this.encodedValue = encodedValue;
    }

    public static Password encode(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_RAW_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_RAW_LENGTH + " characters");
        }
        return new Password("{bcrypt}" + rawPassword.hashCode()); // 临时实现，后续用BCrypt
    }

    public static Password fromEncoded(String encoded) {
        return new Password(encoded);
    }

    public boolean matches(String rawPassword) {
        // 临时实现，后续用BCrypt
        return encodedValue.equals("{bcrypt}" + rawPassword.hashCode());
    }
}
