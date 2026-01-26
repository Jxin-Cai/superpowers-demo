package com.example.cms.presentation.dto;

import com.example.cms.domain.model.sortorder.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private ResourceType resourceType;
    private Long resourceId;
    private Integer sortOrder;
}
