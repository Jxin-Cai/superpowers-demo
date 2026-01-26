package com.example.cms.domain.shared;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

import java.time.LocalDateTime;

@Value
public class Audit {
    LocalDateTime createdAt;
    @Getter(AccessLevel.NONE)
    LocalDateTime updatedAt;

    private Audit(LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Audit create() {
        LocalDateTime now = LocalDateTime.now();
        return new Audit(now, now);
    }

    public static Audit of(LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Audit(createdAt, updatedAt);
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt != null ? updatedAt : createdAt;
    }

    public Audit markModified() {
        return new Audit(this.createdAt, LocalDateTime.now());
    }
}
