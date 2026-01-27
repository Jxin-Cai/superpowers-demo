package com.example.cms.infrastructure.persistence.repository;

import com.example.cms.domain.model.category.Category;
import com.example.cms.domain.repository.CategoryRepository;
import com.example.cms.infrastructure.persistence.entity.CategoryEntity;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    private final SpringDataCategoryRepository springDataRepository;

    @Override
    public Category save(Category category) {
        CategoryEntity entity = toEntity(category);
        CategoryEntity saved = springDataRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Category> findById(Long id) {
        return springDataRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public List<Category> findAll() {
        return springDataRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        springDataRepository.deleteById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return springDataRepository.existsByName(name);
    }

    @Override
    public List<Category> findByIds(List<Long> ids) {
        return springDataRepository.findAllById(ids)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCategoryId(Long id) {
        return springDataRepository.existsById(id);
    }

    private Category toDomain(CategoryEntity entity) {
        return Category.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .audit(com.example.cms.domain.shared.Audit.of(
                        entity.getCreatedAt(),
                        entity.getUpdatedAt()
                ))
                .build();
    }

    private CategoryEntity toEntity(Category category) {
        if (category.getId() == null) {
            return CategoryEntity.of(category.getName(), category.getDescription());
        }
        return CategoryEntity.forUpdate(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getAudit().getCreatedAt()
        );
    }
}
