package com.example.cms.domain.model.sortorder;

import com.example.cms.domain.shared.Audit;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SortOrder {
    private final Long id;
    private final ResourceType resourceType;
    private final Long resourceId;
    private final ResourceType parentType;
    private final Long parentId;
    private int sortOrder;
    private final Audit audit;

    public void updateOrder(int newSortOrder) {
        this.sortOrder = newSortOrder;
    }

    public boolean isRootLevel() {
        return parentType == null || parentId == null;
    }
}
