package com.example.cms.domain.repository;

import com.example.cms.domain.model.article.Article;
import com.example.cms.domain.model.article.ArticleStatus;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository {
    Article save(Article article);
    Optional<Article> findById(Long id);
    List<Article> findAll();
    List<Article> findByCategoryId(Long categoryId);
    List<Article> findByStatus(ArticleStatus status);
    List<Article> findByStatusOrderByPublishedAtDesc(ArticleStatus status);
    List<Article> findByCategoryIdAndStatus(Long categoryId, ArticleStatus status);
    void deleteById(Long id);
    boolean existsByCategoryId(Long categoryId);
}
