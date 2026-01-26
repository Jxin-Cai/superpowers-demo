package com.example.cms.application.service;

import com.example.cms.domain.model.category.Category;
import com.example.cms.domain.repository.CategoryRepository;
import com.example.cms.domain.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;

    @Transactional
    public Category create(String name, String description) {
        if (categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("分类名称已存在: " + name);
        }
        Category category = Category.builder()
                .name(name)
                .description(description)
                .build();
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, String name, String description) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在: " + id));

        if (!category.getName().equals(name) && categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("分类名称已存在: " + name);
        }

        category.rename(name);
        if (description != null) {
            category.updateDescription(description);
        }
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        if (articleRepository.existsByCategoryId(id)) {
            throw new IllegalArgumentException("该分类下有文章，无法删除");
        }
        categoryRepository.deleteById(id);
    }

    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }
}
