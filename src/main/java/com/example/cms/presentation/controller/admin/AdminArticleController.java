package com.example.cms.presentation.controller.admin;

import com.example.cms.application.service.ArticleService;
import com.example.cms.application.service.CategoryService;
import com.example.cms.domain.model.article.Article;
import com.example.cms.domain.model.category.Category;
import com.example.cms.presentation.dto.ApiResponse;
import com.example.cms.presentation.dto.ArticleRequest;
import com.example.cms.presentation.dto.ArticleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
public class AdminArticleController {

    private final ArticleService articleService;
    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<ArticleResponse>> getAll() {
        List<Article> articles = articleService.findAll();
        return ApiResponse.success(articles.stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<ArticleResponse> getById(@PathVariable Long id) {
        return articleService.findById(id)
                .map(article -> ApiResponse.success(toResponse(article)))
                .orElse(ApiResponse.error("文章不存在: " + id));
    }

    @PostMapping
    public ApiResponse<ArticleResponse> create(@RequestBody ArticleRequest request) {
        Article article = articleService.create(
                request.getTitle(),
                request.getContent(),
                request.getCategoryId(),
                request.getKeywords()
        );
        return ApiResponse.success(toResponse(article));
    }

    @PutMapping("/{id}")
    public ApiResponse<ArticleResponse> update(@PathVariable Long id, @RequestBody ArticleRequest request) {
        Article article = articleService.update(
                id,
                request.getTitle(),
                request.getContent(),
                request.getCategoryId()
        );
        return ApiResponse.success(toResponse(article));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<ArticleResponse> publish(@PathVariable Long id) {
        Article article = articleService.publish(id);
        return ApiResponse.success(toResponse(article));
    }

    @PostMapping("/{id}/unpublish")
    public ApiResponse<ArticleResponse> unpublish(@PathVariable Long id) {
        Article article = articleService.unpublish(id);
        return ApiResponse.success(toResponse(article));
    }

    private ArticleResponse toResponse(Article article) {
        String categoryName = categoryService.findById(article.getCategoryId())
                .map(Category::getName)
                .orElse("未知分类");
        return ArticleResponse.from(article, categoryName);
    }
}
