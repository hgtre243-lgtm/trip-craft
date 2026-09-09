-- trip_marker 建表脚本（mysql 首次初始化时自动执行）
USE trip_craft;

CREATE TABLE IF NOT EXISTS `trip_marker` (
    `id`         bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title`      varchar(100) NOT NULL COMMENT '标记地点名称',
    `longitude`  decimal(10,7) NOT NULL COMMENT '经度 (GCJ-02)',
    `latitude`   decimal(10,7) NOT NULL COMMENT '纬度 (GCJ-02)',
    `notes`      varchar(255) DEFAULT '' COMMENT '打卡备注',
    `created_at` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '旅行地图打卡标记表';
