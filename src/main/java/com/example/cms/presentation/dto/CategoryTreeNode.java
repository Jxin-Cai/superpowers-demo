package com.example.cms.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTreeNode {
    private Long id;
    private String name;
    private String description;
    private Integer sortOrder;
    private Long parentId;
    @Builder.Default
    private List<CategoryTreeNode> children = new ArrayList<>();

    public static CategoryTreeNode from(Long id, String name, String description, Integer sortOrder) {
        return CategoryTreeNode.builder()
                .id(id)
                .name(name)
                .description(description)
                .sortOrder(sortOrder)
                .parentId(null)
                .children(new ArrayList<>())
                .build();
    }

    public static CategoryTreeNode from(Long id, String name, String description, Integer sortOrder, Long parentId) {
        return CategoryTreeNode.builder()
                .id(id)
                .name(name)
                .description(description)
                .sortOrder(sortOrder)
                .parentId(parentId)
                .children(new ArrayList<>())
                .build();
    }

    public CategoryTreeNode withParent(Long parentId) {
        this.setParentId(parentId);
        return this;
    }
}
