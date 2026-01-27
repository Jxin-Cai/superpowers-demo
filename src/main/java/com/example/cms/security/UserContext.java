package com.example.cms.security;

import com.example.cms.domain.model.user.UserId;

public class UserContext {
    private static final ThreadLocal<UserId> CURRENT = new ThreadLocal<>();

    public static void setCurrent(UserId userId) {
        CURRENT.set(userId);
    }

    public static UserId getCurrentUser() {
        return CURRENT.get();
    }

    public static Long getCurrentUserIdValue() {
        UserId userId = CURRENT.get();
        return userId != null ? userId.getValue() : null;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
