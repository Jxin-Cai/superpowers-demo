package com.example.cms.presentation.controller.admin;

import com.example.cms.application.service.CategoryService;
import com.example.cms.application.service.SortOrderService;
import com.example.cms.domain.model.category.Category;
import com.example.cms.presentation.dto.ApiResponse;
import com.example.cms.presentation.dto.CategoryRequest;
import com.example.cms.presentation.dto.CategoryResponse;
import com.example.cms.presentation.dto.CategoryTreeResponse;
import com.example.cms.presentation.dto.MoveCategoryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final SortOrderService sortOrderService;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getAll() {
        List<CategoryResponse> responses = categoryService.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @PostMapping
    public ApiResponse<CategoryResponse> create(@RequestBody CategoryRequest request) {
        Category category = categoryService.create(
                request.getName(),
                request.getDescription(),
                request.getParentId(),
                request.getSortOrder() != null ? request.getSortOrder() : 0
        );
        return ApiResponse.success(CategoryResponse.from(category));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @RequestBody CategoryRequest request) {
        Category category = categoryService.update(id, request.getName(), request.getDescription());
        return ApiResponse.success(CategoryResponse.from(category));
    }

    @GetMapping("/tree")
    public ApiResponse<CategoryTreeResponse> getTree() {
        return ApiResponse.success(categoryService.getTree());
    }

    @PutMapping("/{id}/move")
    public ApiResponse<Void> moveToCategory(@PathVariable Long id, @RequestBody @Valid MoveCategoryRequest request) {
        categoryService.moveToCategory(id, request.getNewParentId());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean cascade) {
        categoryService.deleteWithCascade(id, cascade);
        return ApiResponse.success(null);
    }
}
