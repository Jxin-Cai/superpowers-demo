package com.example.cms.presentation.controller.admin;

import com.example.cms.application.service.SortOrderService;
import com.example.cms.presentation.dto.ApiResponse;
import com.example.cms.presentation.dto.ReorderRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/sort")
@RequiredArgsConstructor
public class AdminSortController {

    private final SortOrderService sortOrderService;

    @PutMapping("/reorder")
    public ApiResponse<Void> reorder(@RequestBody @Valid ReorderRequest request) {
        sortOrderService.reorder(request);
        return ApiResponse.success(null);
    }
}
