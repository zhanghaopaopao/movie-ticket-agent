-- 购票附加零食迁移脚本（MySQL 8）。请在当前项目数据库中手动执行一次。
-- 脚本不会删除或修改既有订单数据；零食商品使用下架代替物理删除。
USE `szml`;

-- 执行前检查基础表是否存在且使用 InnoDB。
SELECT table_name, engine
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('cinema', 'orders');

CREATE TABLE IF NOT EXISTS `snack_product` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `cinema_id` BIGINT UNSIGNED NOT NULL COMMENT '影院 ID',
    `name` VARCHAR(128) NOT NULL COMMENT '商品名称',
    `description` VARCHAR(500) NULL COMMENT '商品描述',
    `image` VARCHAR(500) NULL COMMENT '商品图片',
    `price_fen` INT UNSIGNED NOT NULL COMMENT '单价，单位分',
    `stock` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '可售库存，已预占库存也从这里扣除',
    `sold_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计售出数量',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 上架，0 下架',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_snack_cinema_status` (`cinema_id`, `status`),
    KEY `idx_snack_cinema_name` (`cinema_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='影院零食商品';

CREATE TABLE IF NOT EXISTS `order_snack_item` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单 ID',
    `snack_id` BIGINT UNSIGNED NOT NULL COMMENT '零食商品 ID',
    `snack_name` VARCHAR(128) NOT NULL COMMENT '下单时商品名称快照',
    `unit_price_fen` INT UNSIGNED NOT NULL COMMENT '下单时商品单价快照，单位分',
    `quantity` INT UNSIGNED NOT NULL COMMENT '购买数量',
    `inventory_status` VARCHAR(16) NOT NULL DEFAULT 'RESERVED' COMMENT 'RESERVED、SOLD、RELEASED',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_snack` (`order_id`, `snack_id`),
    KEY `idx_order_snack_status` (`order_id`, `inventory_status`),
    CONSTRAINT `fk_order_snack_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
    CONSTRAINT `fk_order_snack_product` FOREIGN KEY (`snack_id`) REFERENCES `snack_product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单零食明细';

-- 如果表是历史版本创建的，下面的检查用于发现缺失的物理关联或孤儿数据。
SELECT '以下结果应为空：零食商品孤儿影院记录' AS check_name;
SELECT sp.id, sp.cinema_id
FROM snack_product sp
LEFT JOIN cinema c ON c.id = sp.cinema_id
WHERE c.id IS NULL;

SELECT '以下结果应为空：订单零食孤儿订单记录' AS check_name;
SELECT oi.id, oi.order_id
FROM order_snack_item oi
LEFT JOIN orders o ON o.id = oi.order_id
WHERE o.id IS NULL;

SELECT '以下结果应为空：订单零食孤儿商品记录' AS check_name;
SELECT oi.id, oi.snack_id
FROM order_snack_item oi
LEFT JOIN snack_product sp ON sp.id = oi.snack_id
WHERE sp.id IS NULL;

-- 仅在外键不存在时补充外键；执行前请确认父表和本表 ID 类型一致。
SET @schema_name = DATABASE();
SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.referential_constraints
            WHERE BINARY constraint_schema = BINARY @schema_name
              AND BINARY constraint_name = BINARY 'fk_order_snack_order'),
    'SELECT 1',
    'ALTER TABLE order_snack_item ADD CONSTRAINT fk_order_snack_order FOREIGN KEY (order_id) REFERENCES orders (id)'
);
PREPARE snack_stmt FROM @sql; EXECUTE snack_stmt; DEALLOCATE PREPARE snack_stmt;

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.referential_constraints
            WHERE BINARY constraint_schema = BINARY @schema_name
              AND BINARY constraint_name = BINARY 'fk_order_snack_product'),
    'SELECT 1',
    'ALTER TABLE order_snack_item ADD CONSTRAINT fk_order_snack_product FOREIGN KEY (snack_id) REFERENCES snack_product (id)'
);
PREPARE snack_stmt FROM @sql; EXECUTE snack_stmt; DEALLOCATE PREPARE snack_stmt;

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.referential_constraints
            WHERE BINARY constraint_schema = BINARY @schema_name
              AND BINARY constraint_name = BINARY 'fk_snack_product_cinema'),
    'SELECT 1',
    'ALTER TABLE snack_product ADD CONSTRAINT fk_snack_product_cinema FOREIGN KEY (cinema_id) REFERENCES cinema (id)'
);
PREPARE snack_stmt FROM @sql; EXECUTE snack_stmt; DEALLOCATE PREPARE snack_stmt;

SELECT 'snack_order migration complete' AS message;
