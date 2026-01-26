-- 通用排序表（唯一的层级+排序存储）
CREATE TABLE sort_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_type VARCHAR(20) NOT NULL COMMENT '资源类型: CATEGORY, ARTICLE',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    parent_type VARCHAR(20) COMMENT '父资源类型: CATEGORY 或 NULL',
    parent_id BIGINT COMMENT '父资源ID，NULL表示根级',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '同级排序序号，从0开始',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_resource (resource_type, resource_id),
    INDEX idx_parent (parent_type, parent_id)
) COMMENT='通用排序表，管理资源的层级结构和排序';
