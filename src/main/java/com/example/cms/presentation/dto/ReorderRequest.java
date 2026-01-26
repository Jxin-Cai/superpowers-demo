package com.example.cms.presentation.dto;

import com.example.cms.domain.model.sortorder.ResourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderRequest {
    private ResourceType parentType;
    private Long parentId;
    @Valid
    @NotEmpty(message = "排序项不能为空")
    private List<OrderItem> items;
}
