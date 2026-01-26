package com.example.cms.infrastructure.persistence.repository;

import com.example.cms.infrastructure.persistence.entity.ArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataArticleRepository extends JpaRepository<ArticleEntity, Long> {
    List<ArticleEntity> findByCategoryId(Long categoryId);
    List<ArticleEntity> findByStatus(String status);
    List<ArticleEntity> findByStatusOrderByPublishedAtDesc(String status);
    List<ArticleEntity> findByCategoryIdAndStatus(Long categoryId, String status);
    boolean existsByCategoryId(Long categoryId);
}
