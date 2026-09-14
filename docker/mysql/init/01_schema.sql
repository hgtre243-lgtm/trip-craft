-- 确保导入连接使用 utf8mb4，避免中文 COMMENT 被双重编码成乱码
SET NAMES utf8mb4;

-- trip_marker 建表脚本（mysql 首次初始化时自动执行）
USE trip_craft;

CREATE TABLE IF NOT EXISTS `trip_marker` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title` varchar(100) NOT NULL COMMENT '标记地点名称',
    `province` varchar(50) DEFAULT '' COMMENT '所属省份',
    `city` varchar(50) DEFAULT '' COMMENT '所属城市',
    `longitude` decimal(10, 7) NOT NULL COMMENT '经度 (GCJ-02)',
    `latitude` decimal(10, 7) NOT NULL COMMENT '纬度 (GCJ-02)',
    `notes` varchar(255) DEFAULT '' COMMENT '打卡备注',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '旅行地图打卡标记表';

-- 1. 行程主表
CREATE TABLE IF NOT EXISTS `trip_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '行程主键ID',
  `title` VARCHAR(100) NOT NULL COMMENT '行程标题（如：杭州秋季3日游）',
  `start_date` DATE NOT NULL COMMENT '出发日期',
  `end_date` DATE NOT NULL COMMENT '结束日期',
  `total_days` INT NOT NULL COMMENT '总天数',
  `cover_color` VARCHAR(20) DEFAULT '#3b82f6' COMMENT '卡片主题色',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行程规划主表';

-- 2. 每日日程子表 (关联 trip_plan)
CREATE TABLE IF NOT EXISTS `trip_day` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `trip_id` BIGINT NOT NULL COMMENT '所属行程ID',
  `day_index` INT NOT NULL COMMENT '第几天（如：1代表Day 1）',
  `plan_date` DATE NOT NULL COMMENT '对应具体公历日期',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_trip_id` (`trip_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行程每日日程表';
