package com.example.cms.domain.repository;

import com.example.cms.domain.model.sortorder.ResourceType;
import com.example.cms.domain.model.sortorder.SortOrder;

import java.util.List;
import java.util.Optional;

public interface SortOrderRepository {
    SortOrder save(SortOrder sortOrder);
    Optional<SortOrder> findByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId);
    List<SortOrder> findByParentTypeAndParentIdOrderBySortOrder(ResourceType parentType, Long parentId);
    List<SortOrder> findByResourceTypeOrderBySortOrder(ResourceType resourceType);
    List<SortOrder> findAll();
    void deleteByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId);
    void deleteByParentTypeAndParentId(ResourceType parentType, Long parentId);
    void updateSortOrder(Long id, int newSortOrder);
    boolean existsByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId);
}
