-- CRM customer module schema for a single-enterprise MySQL 8.0+ deployment.

DROP TABLE IF EXISTS `crm_followup_record`;
DROP TABLE IF EXISTS `crm_customer`;

CREATE TABLE `crm_customer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '客户ID',
    `customer_name` VARCHAR(100) NOT NULL COMMENT '客户名称',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号码',
    `wechat` VARCHAR(64) DEFAULT NULL COMMENT '微信号',
    `company_name` VARCHAR(200) DEFAULT NULL COMMENT '公司名称',
    `pool_type` TINYINT NOT NULL DEFAULT 0 COMMENT '公私海状态：0-公海，1-私海',
    `owner_user_id` BIGINT DEFAULT NULL COMMENT '归属销售人员ID，公海时为空',
    `owner_user_name` VARCHAR(64) DEFAULT NULL COMMENT '归属销售姓名',
    `last_followup_time` DATETIME DEFAULT NULL COMMENT '最后跟进时间',
    `last_followup_record` VARCHAR(500) DEFAULT NULL COMMENT '最后跟进摘要',
    `lock_status` TINYINT NOT NULL DEFAULT 0 COMMENT '锁定状态：0-未锁定，1-已锁定',
    `customer_stage` VARCHAR(16) NOT NULL DEFAULT '线索' COMMENT '客户阶段：线索/初访/意向/报价/签约',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_customer_pool_owner` (`pool_type`, `owner_user_id`),
    KEY `idx_customer_phone` (`phone`),
    KEY `idx_customer_auto_pool` (`pool_type`, `lock_status`, `last_followup_time`),
    CONSTRAINT `fk_customer_owner` FOREIGN KEY (`owner_user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `chk_customer_pool_type` CHECK (`pool_type` IN (0, 1)),
    CONSTRAINT `chk_customer_lock_status` CHECK (`lock_status` IN (0, 1)),
    CONSTRAINT `chk_customer_stage` CHECK (`customer_stage` IN ('线索', '初访', '意向', '报价', '签约')),
    CONSTRAINT `chk_customer_owner` CHECK (
        (`pool_type` = 0 AND `owner_user_id` IS NULL AND `owner_user_name` IS NULL)
        OR (`pool_type` = 1 AND `owner_user_id` IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='CRM客户主表';

CREATE TABLE `crm_followup_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '跟进记录ID',
    `customer_id` BIGINT NOT NULL COMMENT '客户ID',
    `followup_type` VARCHAR(16) NOT NULL COMMENT '跟进方式：电话/拜访/微信/系统',
    `content` TEXT NOT NULL COMMENT '跟进内容',
    `next_followup_time` DATETIME DEFAULT NULL COMMENT '下次跟进时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_followup_customer_time` (`customer_id`, `create_time`),
    KEY `idx_followup_next_time` (`next_followup_time`),
    CONSTRAINT `fk_followup_customer` FOREIGN KEY (`customer_id`) REFERENCES `crm_customer` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `chk_followup_type` CHECK (`followup_type` IN ('电话', '拜访', '微信', '系统'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='CRM客户跟进记录表';
