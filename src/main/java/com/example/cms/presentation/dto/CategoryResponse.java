package com.example.cms.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CategoryResponse from(com.example.cms.domain.model.category.Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                null,
                null,
                category.getAudit().getCreatedAt(),
                category.getAudit().getUpdatedAt()
        );
    }

    public static CategoryResponse from(com.example.cms.domain.model.category.Category category, Long parentId, Integer sortOrder) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                parentId,
                sortOrder,
                category.getAudit().getCreatedAt(),
                category.getAudit().getUpdatedAt()
        );
    }
}
