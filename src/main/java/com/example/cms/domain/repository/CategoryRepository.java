package com.example.cms.domain.repository;

import com.example.cms.domain.model.category.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    Category save(Category category);
    Optional<Category> findById(Long id);
    List<Category> findAll();
    void deleteById(Long id);
    boolean existsByName(String name);
}
