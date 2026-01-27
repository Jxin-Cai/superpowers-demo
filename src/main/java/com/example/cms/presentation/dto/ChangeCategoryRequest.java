package com.example.cms.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeCategoryRequest {
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;
}
