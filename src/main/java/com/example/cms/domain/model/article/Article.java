package com.example.cms.domain.model.article;

import com.example.cms.domain.shared.Audit;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Article {
    private final Long id;
    private String title;
    private String content;
    private RenderedContent renderedContent;
    private ArticleStatus status;
    private Long categoryId;
    private LocalDateTime publishedAt;
    private Audit audit;

    private Article(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.content = builder.content;
        this.renderedContent = builder.renderedContent;
        this.status = builder.status;
        this.categoryId = builder.categoryId;
        this.publishedAt = builder.publishedAt;
        this.audit = builder.audit;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void updateContent(String content, RenderedContent renderedContent) {
        this.content = content;
        this.renderedContent = renderedContent;
        this.audit = this.audit.markModified();
    }

    public void publish() {
        if (this.status == ArticleStatus.PUBLISHED) {
            return;
        }
        this.status = ArticleStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.audit = this.audit.markModified();
    }

    public void unpublish() {
        if (this.status == ArticleStatus.DRAFT) {
            return;
        }
        this.status = ArticleStatus.DRAFT;
        this.publishedAt = null;
        this.audit = this.audit.markModified();
    }

    public void changeCategory(Long categoryId) {
        this.categoryId = categoryId;
        this.audit = this.audit.markModified();
    }

    public static class Builder {
        Long id;
        String title;
        String content;
        RenderedContent renderedContent = RenderedContent.of("");
        ArticleStatus status = ArticleStatus.DRAFT;
        Long categoryId;
        LocalDateTime publishedAt;
        Audit audit = Audit.create();

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder renderedContent(RenderedContent renderedContent) {
            this.renderedContent = renderedContent;
            return this;
        }

        public Builder status(ArticleStatus status) {
            this.status = status;
            return this;
        }

        public Builder categoryId(Long categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder publishedAt(LocalDateTime publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public Builder audit(Audit audit) {
            this.audit = audit;
            return this;
        }

        public Article build() {
            return new Article(this);
        }
    }
}
