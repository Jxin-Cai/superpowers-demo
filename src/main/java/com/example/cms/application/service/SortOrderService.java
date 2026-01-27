package com.example.cms.application.service;

import com.example.cms.domain.model.sortorder.ResourceType;
import com.example.cms.domain.model.sortorder.SortOrder;

import java.util.List;

public interface SortOrderService {
    void reorder(com.example.cms.presentation.dto.ReorderRequest request);
    void initializeCategorySortOrder(Long categoryId, Long parentId, int sortOrder);
    void initializeArticleSortOrder(Long articleId, Long categoryId, int sortOrder);
    void moveCategoryToParent(Long categoryId, Long newParentId);
    SortOrder getCategorySortOrder(Long categoryId);
    SortOrder getArticleSortOrder(Long articleId);
    List<SortOrder> getChildrenByParent(ResourceType parentType, Long parentId);
    void deleteSortOrder(ResourceType resourceType, Long resourceId);
    void deleteChildrenSortOrders(ResourceType parentType, Long parentId);
    void moveArticleToCategory(Long articleId, Long newCategoryId, int sortOrder);
}
