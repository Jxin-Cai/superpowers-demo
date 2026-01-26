package com.example.cms.infrastructure.persistence.repository;

import com.example.cms.domain.model.article.Article;
import com.example.cms.domain.model.article.ArticleStatus;
import com.example.cms.domain.repository.ArticleRepository;
import com.example.cms.infrastructure.persistence.entity.ArticleEntity;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
@RequiredArgsConstructor
public class ArticleRepositoryImpl implements ArticleRepository {

    private final SpringDataArticleRepository springDataRepository;

    @Override
    public Article save(Article article) {
        ArticleEntity entity = toEntity(article);
        ArticleEntity saved = springDataRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Article> findById(Long id) {
        return springDataRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public List<Article> findAll() {
        return springDataRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Article> findByCategoryId(Long categoryId) {
        return springDataRepository.findByCategoryId(categoryId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Article> findByStatus(ArticleStatus status) {
        return springDataRepository.findByStatus(status.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Article> findByStatusOrderByPublishedAtDesc(ArticleStatus status) {
        return springDataRepository.findByStatusOrderByPublishedAtDesc(status.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Article> findByCategoryIdAndStatus(Long categoryId, ArticleStatus status) {
        return springDataRepository.findByCategoryIdAndStatus(categoryId, status.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        springDataRepository.deleteById(id);
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        return springDataRepository.existsByCategoryId(categoryId);
    }

    private Article toDomain(ArticleEntity entity) {
        return Article.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .renderedContent(com.example.cms.domain.model.article.RenderedContent.of(entity.getRenderedContent()))
                .status(ArticleStatus.valueOf(entity.getStatus()))
                .categoryId(entity.getCategoryId())
                .publishedAt(entity.getPublishedAt())
                .audit(com.example.cms.domain.shared.Audit.of(
                        entity.getCreatedAt(),
                        entity.getUpdatedAt()
                ))
                .build();
    }

    private ArticleEntity toEntity(Article article) {
        if (article.getId() == null) {
            return ArticleEntity.of(
                    article.getTitle(),
                    article.getContent(),
                    article.getRenderedContent().getHtml(),
                    article.getStatus().name(),
                    article.getCategoryId()
            );
        }
        return ArticleEntity.forUpdate(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                article.getRenderedContent().getHtml(),
                article.getStatus().name(),
                article.getCategoryId(),
                article.getAudit().getCreatedAt(),
                article.getPublishedAt()
        );
    }
}
