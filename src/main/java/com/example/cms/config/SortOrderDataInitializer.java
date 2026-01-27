package com.example.cms.config;

import com.example.cms.domain.model.sortorder.ResourceType;
import com.example.cms.domain.model.sortorder.SortOrder;
import com.example.cms.domain.repository.CategoryRepository;
import com.example.cms.domain.repository.SortOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SortOrderDataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final SortOrderRepository sortOrderRepository;

    @Override
    public void run(String... args) {
        log.info("开始检查并补充分类的 sort_orders 记录");

        List<com.example.cms.domain.model.category.Category> allCategories = categoryRepository.findAll();

        for (com.example.cms.domain.model.category.Category category : allCategories) {
            boolean exists = sortOrderRepository.existsByResourceTypeAndResourceId(
                    ResourceType.CATEGORY, category.getId());

            if (!exists) {
                log.info("为分类 {} 补充 sort_orders 记录", category.getName());
                SortOrder sortOrder = SortOrder.builder()
                        .resourceType(ResourceType.CATEGORY)
                        .resourceId(category.getId())
                        .parentType(null) // 简化处理：默认所有已存在分类作为根分类
                        .parentId(null)
                        .sortOrder(0)
                        .build();
                sortOrderRepository.save(sortOrder);
            }
        }

        log.info("sort_orders 记录检查完成");
    }
}
