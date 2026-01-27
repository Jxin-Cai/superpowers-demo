package com.example.cms.presentation.dto;

import lombok.Data;

@Data
public class CategoryRequest {
    private String name;
    private String description;
    private Long parentId;
    private Integer sortOrder;
}
