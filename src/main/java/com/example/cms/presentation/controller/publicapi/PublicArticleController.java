package com.example.cms.presentation.controller.publicapi;

import com.example.cms.application.service.ArticleService;
import com.example.cms.application.service.CategoryService;
import com.example.cms.domain.model.article.Article;
import com.example.cms.domain.model.category.Category;
import com.example.cms.presentation.dto.ApiResponse;
import com.example.cms.presentation.dto.ArticleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/articles")
@RequiredArgsConstructor
public class PublicArticleController {

    private final ArticleService articleService;
    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<ArticleResponse>> getPublished(
            @RequestParam(required = false) Long categoryId) {
        List<Article> articles = categoryId != null
                ? articleService.findByCategory(categoryId)
                : articleService.findPublished();

        return ApiResponse.success(articles.stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/search")
    public ApiResponse<Page<ArticleResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            Pageable pageable) {
        Page<Article> articles = articleService.search(keyword, categoryId, pageable);
        return ApiResponse.success(articles.map(article -> {
            String categoryName = categoryService.findById(article.getCategoryId())
                    .map(Category::getName)
                    .orElse("未知分类");
            return ArticleResponse.from(article, categoryName);
        }));
    }

    @GetMapping("/{id}")
    public ApiResponse<ArticleResponse> getById(@PathVariable Long id) {
        return articleService.findById(id)
                .filter(article -> article.getStatus().name().equals("PUBLISHED"))
                .map(article -> ApiResponse.success(toResponse(article)))
                .orElse(ApiResponse.error(404, "文章不存在或未发布"));
    }

    private ArticleResponse toResponse(Article article) {
        String categoryName = categoryService.findById(article.getCategoryId())
                .map(Category::getName)
                .orElse("未知分类");
        return ArticleResponse.from(article, categoryName);
    }
}
