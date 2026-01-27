package com.example.cms.application.service;

import com.example.cms.domain.model.sortorder.ResourceType;
import com.example.cms.domain.model.sortorder.SortOrder;
import com.example.cms.domain.repository.SortOrderRepository;
import com.example.cms.presentation.dto.OrderItem;
import com.example.cms.presentation.dto.ReorderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SortOrderServiceImpl implements SortOrderService {

    private final SortOrderRepository sortOrderRepository;

    @Override
    @Transactional
    public void reorder(ReorderRequest request) {
        for (OrderItem item : request.getItems()) {
            SortOrder sortOrder = sortOrderRepository
                    .findByResourceTypeAndResourceId(item.getResourceType(), item.getResourceId())
                    .orElseThrow(() -> new IllegalArgumentException(
                        "SortOrder not found for " + item.getResourceType() + ":" + item.getResourceId()));
            sortOrderRepository.updateSortOrder(sortOrder.getId(), item.getSortOrder());
        }
    }

    @Override
    @Transactional
    public void initializeCategorySortOrder(Long categoryId, Long parentId, int sortOrder) {
        if (sortOrderRepository.existsByResourceTypeAndResourceId(ResourceType.CATEGORY, categoryId)) {
            return;
        }
        SortOrder newSortOrder = SortOrder.builder()
                .resourceType(ResourceType.CATEGORY)
                .resourceId(categoryId)
                .parentType(parentId != null ? ResourceType.CATEGORY : null)
                .parentId(parentId)
                .sortOrder(sortOrder)
                .build();
        sortOrderRepository.save(newSortOrder);
    }

    @Override
    @Transactional
    public void initializeArticleSortOrder(Long articleId, Long categoryId, int sortOrder) {
        if (sortOrderRepository.existsByResourceTypeAndResourceId(ResourceType.ARTICLE, articleId)) {
            return;
        }
        SortOrder newSortOrder = SortOrder.builder()
                .resourceType(ResourceType.ARTICLE)
                .resourceId(articleId)
                .parentType(ResourceType.CATEGORY)
                .parentId(categoryId)
                .sortOrder(sortOrder)
                .build();
        sortOrderRepository.save(newSortOrder);
    }

    @Override
    @Transactional
    public void moveCategoryToParent(Long categoryId, Long newParentId) {
        SortOrder sortOrder = sortOrderRepository
                .findByResourceTypeAndResourceId(ResourceType.CATEGORY, categoryId)
                .orElseThrow(() -> new IllegalArgumentException("分类排序记录不存在: " + categoryId));

        int newSortOrder = 0;
        if (newParentId != null) {
            List<SortOrder> siblings = sortOrderRepository
                    .findByParentTypeAndParentIdOrderBySortOrder(ResourceType.CATEGORY, newParentId);
            newSortOrder = siblings.size();
        } else {
            List<SortOrder> rootCategories = sortOrderRepository
                    .findByParentTypeAndParentIdOrderBySortOrder(null, null);
            newSortOrder = rootCategories.size();
        }

        SortOrder updated = SortOrder.builder()
                .id(sortOrder.getId())
                .resourceType(ResourceType.CATEGORY)
                .resourceId(categoryId)
                .parentType(newParentId != null ? ResourceType.CATEGORY : null)
                .parentId(newParentId)
                .sortOrder(newSortOrder)
                .build();
        sortOrderRepository.save(updated);
    }

    @Override
    public SortOrder getCategorySortOrder(Long categoryId) {
        return sortOrderRepository
                .findByResourceTypeAndResourceId(ResourceType.CATEGORY, categoryId)
                .orElse(null);
    }

    @Override
    public SortOrder getArticleSortOrder(Long articleId) {
        return sortOrderRepository
                .findByResourceTypeAndResourceId(ResourceType.ARTICLE, articleId)
                .orElse(null);
    }

    @Override
    public List<SortOrder> getChildrenByParent(ResourceType parentType, Long parentId) {
        return sortOrderRepository.findByParentTypeAndParentIdOrderBySortOrder(parentType, parentId);
    }

    @Override
    @Transactional
    public void deleteSortOrder(ResourceType resourceType, Long resourceId) {
        sortOrderRepository.deleteByResourceTypeAndResourceId(resourceType, resourceId);
    }

    @Override
    @Transactional
    public void deleteChildrenSortOrders(ResourceType parentType, Long parentId) {
        sortOrderRepository.deleteByParentTypeAndParentId(parentType, parentId);
    }

    @Override
    @Transactional
    public void moveArticleToCategory(Long articleId, Long newCategoryId, int sortOrder) {
        SortOrder existing = sortOrderRepository
                .findByResourceTypeAndResourceId(ResourceType.ARTICLE, articleId)
                .orElse(null);

        if (existing != null) {
            SortOrder updated = SortOrder.builder()
                    .id(existing.getId())
                    .resourceType(ResourceType.ARTICLE)
                    .resourceId(articleId)
                    .parentType(ResourceType.CATEGORY)
                    .parentId(newCategoryId)
                    .sortOrder(sortOrder)
                    .build();
            sortOrderRepository.save(updated);
        } else {
            initializeArticleSortOrder(articleId, newCategoryId, sortOrder);
        }
    }
}
