package com.example.cms.infrastructure.persistence.entity;

import com.example.cms.domain.model.sortorder.ResourceType;
import com.example.cms.domain.model.sortorder.SortOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sort_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SortOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 20)
    private ResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "parent_type", length = 20)
    private ResourceType parentType;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SortOrder toDomainModel() {
        return SortOrder.builder()
                .id(id)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .parentType(parentType)
                .parentId(parentId)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .build();
    }

    public static SortOrderEntity fromDomainModel(SortOrder sortOrderModel) {
        SortOrderEntity entity = new SortOrderEntity();
        entity.id = sortOrderModel.getId();
        entity.resourceType = sortOrderModel.getResourceType();
        entity.resourceId = sortOrderModel.getResourceId();
        entity.parentType = sortOrderModel.getParentType();
        entity.parentId = sortOrderModel.getParentId();
        entity.sortOrder = sortOrderModel.getSortOrder();
        entity.createdAt = LocalDateTime.now();
        entity.updatedAt = LocalDateTime.now();
        return entity;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
