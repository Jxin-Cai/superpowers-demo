package com.example.cms.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Lob
    @Column(nullable = false)
    private String renderedContent;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static ArticleEntity of(String title, String content, String renderedContent,
                                    String status, Long categoryId) {
        ArticleEntity entity = new ArticleEntity();
        entity.title = title;
        entity.content = content;
        entity.renderedContent = renderedContent;
        entity.status = status;
        entity.categoryId = categoryId;
        entity.createdAt = LocalDateTime.now();
        entity.updatedAt = LocalDateTime.now();
        return entity;
    }

    public void update(String title, String content, String renderedContent, Long categoryId) {
        this.title = title;
        this.content = content;
        this.renderedContent = renderedContent;
        this.categoryId = categoryId;
        this.updatedAt = LocalDateTime.now();
    }

    public void publish() {
        this.status = "PUBLISHED";
        this.publishedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void unpublish() {
        this.status = "DRAFT";
        this.publishedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public static ArticleEntity forUpdate(Long id, String title, String content, String renderedContent,
                                          String status, Long categoryId, LocalDateTime createdAt,
                                          LocalDateTime publishedAt) {
        ArticleEntity entity = new ArticleEntity();
        entity.id = id;
        entity.title = title;
        entity.content = content;
        entity.renderedContent = renderedContent;
        entity.status = status;
        entity.categoryId = categoryId;
        entity.createdAt = createdAt;
        entity.updatedAt = LocalDateTime.now();
        entity.publishedAt = publishedAt;
        return entity;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
