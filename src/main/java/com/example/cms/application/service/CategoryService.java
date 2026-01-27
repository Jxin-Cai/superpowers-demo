package com.example.cms.application.service;

import com.example.cms.domain.model.category.Category;
import com.example.cms.domain.model.sortorder.ResourceType;
import com.example.cms.domain.model.sortorder.SortOrder;
import com.example.cms.domain.repository.CategoryRepository;
import com.example.cms.domain.repository.ArticleRepository;
import com.example.cms.presentation.dto.CategoryTreeResponse;
import com.example.cms.presentation.dto.CategoryTreeNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;
    private final SortOrderService sortOrderService;

    @Transactional
    public Category create(String name, String description) {
        return create(name, description, null, 0);
    }

    @Transactional
    public Category create(String name, String description, Long parentId, int sortOrder) {
        if (categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("分类名称已存在: " + name);
        }

        // 验证父分类存在
        if (parentId != null) {
            categoryRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("父分类不存在: " + parentId));
        }

        Category category = Category.builder()
                .name(name)
                .description(description)
                .build();

        Category saved = categoryRepository.save(category);

        // 初始化排序 - 根分类的 parentType 和 parentId 都为 null
        sortOrderService.initializeCategorySortOrder(saved.getId(), parentId, sortOrder);

        return saved;
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

    @Transactional
    public CategoryTreeResponse getTree() {
        List<SortOrder> rootSortOrders = sortOrderService.getChildrenByParent(null, null);
        if (rootSortOrders.isEmpty()) {
            return CategoryTreeResponse.builder().tree(new ArrayList<>()).build();
        }
        List<Long> rootIds = rootSortOrders.stream()
                .map(SortOrder::getResourceId)
                .toList();
        Map<Long, Category> categoryMap = categoryRepository.findByIds(rootIds).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        List<CategoryTreeNode> tree = new ArrayList<>();
        for (SortOrder so : rootSortOrders) {
            Category category = categoryMap.get(so.getResourceId());
            if (category != null) {
                tree.add(buildTreeNode(category, so.getSortOrder()));
            }
        }
        return CategoryTreeResponse.builder().tree(tree).build();
    }

    private CategoryTreeNode buildTreeNode(Category category, Integer sortOrder) {
        CategoryTreeNode node = CategoryTreeNode.from(
                category.getId(),
                category.getName(),
                category.getDescription(),
                sortOrder
        );
        List<SortOrder> children = sortOrderService.getChildrenByParent(
                ResourceType.CATEGORY, category.getId()
        );
        if (!children.isEmpty()) {
            List<Long> childIds = children.stream()
                    .map(SortOrder::getResourceId)
                    .toList();
            Map<Long, Category> childMap = categoryRepository.findByIds(childIds).stream()
                    .collect(Collectors.toMap(Category::getId, c -> c));
            for (SortOrder child : children) {
                Category childCategory = childMap.get(child.getResourceId());
                if (childCategory != null) {
                    node.getChildren().add(buildTreeNode(childCategory, child.getSortOrder()));
                }
            }
        }
        return node;
    }

    @Transactional
    public void moveToCategory(Long categoryId, Long newParentId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在: " + categoryId));
        if (newParentId != null) {
            categoryRepository.findById(newParentId)
                    .orElseThrow(() -> new IllegalArgumentException("目标父分类不存在: " + newParentId));
            validateNoCycle(categoryId, newParentId);
        }
        sortOrderService.moveCategoryToParent(categoryId, newParentId);
    }

    private void validateNoCycle(Long categoryId, Long targetParentId) {
        Set<Long> visited = new HashSet<>();
        Long currentId = targetParentId;
        while (currentId != null) {
            if (visited.contains(currentId)) {
                throw new IllegalArgumentException("检测到循环引用");
            }
            if (currentId.equals(categoryId)) {
                throw new IllegalArgumentException("不能将分类移动到其子分类下");
            }
            visited.add(currentId);
            SortOrder currentSort = sortOrderService.getCategorySortOrder(currentId);
            if (currentSort == null || currentSort.getParentId() == null) {
                break;
            }
            currentId = currentSort.getParentId();
        }
    }

    @Transactional
    public void deleteWithCascade(Long categoryId, boolean cascade) {
        if (cascade) {
            List<SortOrder> children = sortOrderService.getChildrenByParent(
                    ResourceType.CATEGORY, categoryId
            );
            for (SortOrder child : children) {
                deleteWithCascade(child.getResourceId(), true);
            }
        } else {
            List<SortOrder> children = sortOrderService.getChildrenByParent(
                    ResourceType.CATEGORY, categoryId
            );
            if (!children.isEmpty()) {
                throw new IllegalArgumentException("该分类下有子分类，无法删除");
            }
        }
        if (articleRepository.existsByCategoryId(categoryId)) {
            throw new IllegalArgumentException("该分类下有文章，无法删除");
        }
        sortOrderService.deleteSortOrder(ResourceType.CATEGORY, categoryId);
        categoryRepository.deleteById(categoryId);
    }
}
