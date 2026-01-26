package com.example.cms.infrastructure.persistence.repository;

import com.example.cms.domain.model.sortorder.ResourceType;
import com.example.cms.infrastructure.persistence.entity.SortOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataSortOrderRepository extends JpaRepository<SortOrderEntity, Long> {

    Optional<SortOrderEntity> findByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId);

    List<SortOrderEntity> findByParentTypeAndParentIdOrderBySortOrderAsc(ResourceType parentType, Long parentId);

    List<SortOrderEntity> findByResourceTypeOrderBySortOrderAsc(ResourceType resourceType);

    boolean existsByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId);

    @Modifying
    @Query("UPDATE SortOrderEntity s SET s.sortOrder = :order WHERE s.id = :id")
    void updateSortOrder(@Param("id") Long id, @Param("order") int order);

    @Modifying
    @Query("DELETE FROM SortOrderEntity s WHERE s.resourceType = :resourceType AND s.resourceId = :resourceId")
    void deleteByResourceTypeAndResourceId(@Param("resourceType") ResourceType resourceType,
                                           @Param("resourceId") Long resourceId);

    @Modifying
    @Query("DELETE FROM SortOrderEntity s WHERE s.parentType = :parentType AND s.parentId = :parentId")
    void deleteByParentTypeAndParentId(@Param("parentType") ResourceType parentType,
                                       @Param("parentId") Long parentId);
}
