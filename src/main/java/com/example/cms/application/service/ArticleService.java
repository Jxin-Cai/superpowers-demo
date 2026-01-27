package com.example.cms.application.service;

import com.example.cms.domain.model.article.Article;
import com.example.cms.domain.model.article.ArticleStatus;
import com.example.cms.domain.model.article.RenderedContent;
import com.example.cms.domain.model.sortorder.ResourceType;
import com.example.cms.domain.model.sortorder.SortOrder;
import com.example.cms.domain.repository.ArticleRepository;
import com.example.cms.domain.repository.CategoryRepository;
import com.example.cms.domain.service.MarkdownRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final MarkdownRenderer markdownRenderer;
    private final SortOrderService sortOrderService;

    @Transactional
    public Article create(String title, String content, Long categoryId, String keywords) {
        validateCategoryExists(categoryId);

        RenderedContent renderedContent = markdownRenderer.render(content);

        Article article = Article.builder()
                .title(title)
                .content(content)
                .renderedContent(renderedContent)
                .categoryId(categoryId)
                .keywords(keywords)
                .build();
        return articleRepository.save(article);
    }

    @Transactional
    public Article update(Long id, String title, String content, Long categoryId) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文章不存在: " + id));

        validateCategoryExists(categoryId);

        RenderedContent renderedContent = markdownRenderer.render(content);
        article.updateContent(content, renderedContent);
        article.changeCategory(categoryId);

        return articleRepository.save(article);
    }

    @Transactional
    public void delete(Long id) {
        articleRepository.deleteById(id);
    }

    @Transactional
    public Article publish(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文章不存在: " + id));
        article.publish();
        return articleRepository.save(article);
    }

    @Transactional
    public Article unpublish(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文章不存在: " + id));
        article.unpublish();
        return articleRepository.save(article);
    }

    public Optional<Article> findById(Long id) {
        return articleRepository.findById(id);
    }

    public List<Article> findAll() {
        return articleRepository.findAll();
    }

    public List<Article> findPublished() {
        return articleRepository.findByStatusOrderByPublishedAtDesc(ArticleStatus.PUBLISHED);
    }

    public List<Article> findByCategory(Long categoryId) {
        return articleRepository.findByCategoryIdAndStatus(categoryId, ArticleStatus.PUBLISHED);
    }

    public Page<Article> search(String keyword, Long categoryId, Pageable pageable) {
        return articleRepository.searchByKeyword(keyword, categoryId, pageable);
    }

    private void validateCategoryExists(Long categoryId) {
        if (!categoryRepository.findById(categoryId).isPresent()) {
            throw new IllegalArgumentException("分类不存在: " + categoryId);
        }
    }

    @Transactional
    public void changeCategory(Long articleId, Long newCategoryId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("文章不存在: " + articleId));
        categoryRepository.findById(newCategoryId)
                .orElseThrow(() -> new IllegalArgumentException("目标分类不存在: " + newCategoryId));

        SortOrder currentSort = sortOrderService.getArticleSortOrder(articleId);
        int newSortOrder = 0;

        if (currentSort != null && currentSort.getParentId().equals(newCategoryId)) {
            return;
        }

        List<SortOrder> siblings = sortOrderService.getChildrenByParent(
                ResourceType.CATEGORY, newCategoryId
        );
        newSortOrder = siblings.size();

        article.setCategoryId(newCategoryId);
        articleRepository.save(article);

        if (currentSort != null) {
            sortOrderService.moveArticleToCategory(articleId, newCategoryId, newSortOrder);
        } else {
            sortOrderService.initializeArticleSortOrder(articleId, newCategoryId, newSortOrder);
        }
    }
}
