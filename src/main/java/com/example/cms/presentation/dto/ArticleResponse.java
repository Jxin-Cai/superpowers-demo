package com.example.cms.presentation.dto;

import com.example.cms.domain.model.article.ArticleStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleResponse {
    private Long id;
    private String title;
    private String content;
    private String renderedContent;
    private String status;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ArticleResponse from(com.example.cms.domain.model.article.Article article, String categoryName) {
        return new ArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                article.getRenderedContent().getHtml(),
                article.getStatus().name(),
                article.getCategoryId(),
                categoryName,
                article.getPublishedAt(),
                article.getAudit().getCreatedAt(),
                article.getAudit().getUpdatedAt()
        );
    }
}
