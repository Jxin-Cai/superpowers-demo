package com.example.cms.infrastructure.persistence.repository;

import com.example.cms.infrastructure.persistence.entity.ArticleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataArticleRepository extends JpaRepository<ArticleEntity, Long> {
    List<ArticleEntity> findByCategoryId(Long categoryId);
    List<ArticleEntity> findByStatus(String status);
    List<ArticleEntity> findByStatusOrderByPublishedAtDesc(String status);
    List<ArticleEntity> findByCategoryIdAndStatus(Long categoryId, String status);
    boolean existsByCategoryId(Long categoryId);

    @Query("SELECT a FROM ArticleEntity a WHERE " +
           "(:keyword IS NULL OR a.title LIKE %:keyword% OR a.keywords LIKE %:keyword%) " +
           "AND (:categoryId IS NULL OR a.categoryId = :categoryId)")
    Page<ArticleEntity> searchByKeyword(@Param("keyword") String keyword,
                                        @Param("categoryId") Long categoryId,
                                        Pageable pageable);
}
