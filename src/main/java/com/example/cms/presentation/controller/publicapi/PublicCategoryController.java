package com.example.cms.presentation.controller.publicapi;

import com.example.cms.application.service.CategoryService;
import com.example.cms.presentation.dto.ApiResponse;
import com.example.cms.presentation.dto.CategoryResponse;
import com.example.cms.presentation.dto.CategoryTreeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/categories")
@RequiredArgsConstructor
public class PublicCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getAll() {
        List<CategoryResponse> responses = categoryService.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/tree")
    public ApiResponse<CategoryTreeResponse> getTree() {
        return ApiResponse.success(categoryService.getTree());
    }
}
