package com.example.cms.domain.model.sortorder;

public enum ResourceType {
    CATEGORY("CATEGORY"),
    ARTICLE("ARTICLE");

    private final String value;

    ResourceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ResourceType fromValue(String value) {
        for (ResourceType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown resource type: " + value);
    }
}
