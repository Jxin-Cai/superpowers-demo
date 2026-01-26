package com.example.cms.presentation.controller.admin;

import com.example.cms.application.service.CategoryService;
import com.example.cms.domain.model.category.Category;
import com.example.cms.presentation.dto.ApiResponse;
import com.example.cms.presentation.dto.CategoryRequest;
import com.example.cms.presentation.dto.CategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getAll() {
        List<CategoryResponse> responses = categoryService.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @PostMapping
    public ApiResponse<CategoryResponse> create(@RequestBody CategoryRequest request) {
        Category category = categoryService.create(request.getName(), request.getDescription());
        return ApiResponse.success(CategoryResponse.from(category));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @RequestBody CategoryRequest request) {
        Category category = categoryService.update(id, request.getName(), request.getDescription());
        return ApiResponse.success(CategoryResponse.from(category));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.success(null);
    }
}
