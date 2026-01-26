package com.example.cms.infrastructure.persistence.repository;

import com.example.cms.domain.model.sortorder.ResourceType;
import com.example.cms.domain.model.sortorder.SortOrder;
import com.example.cms.domain.repository.SortOrderRepository;
import com.example.cms.infrastructure.persistence.entity.SortOrderEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SortOrderRepositoryImpl implements SortOrderRepository {

    private final SpringDataSortOrderRepository springDataSortOrderRepository;

    @Override
    public SortOrder save(SortOrder sortOrder) {
        SortOrderEntity entity = SortOrderEntity.fromDomainModel(sortOrder);
        SortOrderEntity saved = springDataSortOrderRepository.save(entity);
        return saved.toDomainModel();
    }

    @Override
    public Optional<SortOrder> findByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId) {
        return springDataSortOrderRepository
                .findByResourceTypeAndResourceId(resourceType, resourceId)
                .map(SortOrderEntity::toDomainModel);
    }

    @Override
    public List<SortOrder> findByParentTypeAndParentIdOrderBySortOrder(ResourceType parentType, Long parentId) {
        return springDataSortOrderRepository
                .findByParentTypeAndParentIdOrderBySortOrderAsc(parentType, parentId)
                .stream()
                .map(SortOrderEntity::toDomainModel)
                .toList();
    }

    @Override
    public List<SortOrder> findByResourceTypeOrderBySortOrder(ResourceType resourceType) {
        return springDataSortOrderRepository
                .findByResourceTypeOrderBySortOrderAsc(resourceType)
                .stream()
                .map(SortOrderEntity::toDomainModel)
                .toList();
    }

    @Override
    public List<SortOrder> findAll() {
        return springDataSortOrderRepository.findAll()
                .stream()
                .map(SortOrderEntity::toDomainModel)
                .toList();
    }

    @Override
    public void deleteByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId) {
        springDataSortOrderRepository.deleteByResourceTypeAndResourceId(resourceType, resourceId);
    }

    @Override
    public void deleteByParentTypeAndParentId(ResourceType parentType, Long parentId) {
        springDataSortOrderRepository.deleteByParentTypeAndParentId(parentType, parentId);
    }

    @Override
    public void updateSortOrder(Long id, int newSortOrder) {
        springDataSortOrderRepository.updateSortOrder(id, newSortOrder);
    }

    @Override
    public boolean existsByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId) {
        return springDataSortOrderRepository.existsByResourceTypeAndResourceId(resourceType, resourceId);
    }
}
