-- 通用排序表（唯一的层级+排序存储） H2兼容语法
CREATE TABLE sort_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_type VARCHAR(20) NOT NULL,
    resource_id BIGINT NOT NULL,
    parent_type VARCHAR(20),
    parent_id BIGINT,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON COLUMN sort_orders.resource_type IS '资源类型: CATEGORY, ARTICLE';
COMMENT ON COLUMN sort_orders.resource_id IS '资源ID';
COMMENT ON COLUMN sort_orders.parent_type IS '父资源类型: CATEGORY 或 NULL';
COMMENT ON COLUMN sort_orders.parent_id IS '父资源ID，NULL表示根级';
COMMENT ON COLUMN sort_orders.sort_order IS '同级排序序号，从0开始';

CREATE UNIQUE INDEX uk_resource ON sort_orders(resource_type, resource_id);
CREATE INDEX idx_parent ON sort_orders(parent_type, parent_id);
