package com.example.cms.domain.model.article;

import lombok.Getter;

@Getter
public enum ArticleStatus {
    DRAFT("草稿"),
    PUBLISHED("已发布");

    private final String description;

    ArticleStatus(String description) {
        this.description = description;
    }
}
