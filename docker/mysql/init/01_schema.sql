-- =============================================================================
-- TripCraft（伴游行）— 数据库初始化脚本
-- =============================================================================
-- 用途：创建 trip_craft 库及其全部业务表。
--
-- 执行方式：Docker MySQL 首次启动时，由官方镜像入口自动执行本目录下的 *.sql
--          （docker/docker-compose.yml 已将 ./mysql/init 挂载到
--           /docker-entrypoint-initdb.d，并按文件名顺序执行）。
--
-- ⚠️ 仅在数据卷为空（即首次初始化）时执行一次。
--    若库已存在，修改本文件不会生效，需先删除命名卷再重启：
--        docker compose down -v && docker compose up -d
--    （-v 会清空 MySQL 数据，请先确认无需保留；已有库请改用 ALTER 手动同步）
--
-- 表清单（按依赖顺序创建，被引用方在前）：
--   trip_marker  旅行地图打卡标记表   独立表
--   trip_plan    行程规划主表         独立表
--   trip_day     行程每日日程表       逻辑关联 trip_plan.id
--   trip_node    行程每日打卡节点表   逻辑关联 trip_day.id
--
-- 说明：表之间未建数据库级外键约束（仅建索引 + 逻辑关联），
--       关联完整性由 Service 层保证，删除主记录时不会级联删除子记录。
-- =============================================================================

-- 强制本次导入连接使用 utf8mb4。
-- MySQL 官方镜像初始化时的客户端连接默认为 latin1，若不加这一行，脚本中的中文
-- 注释会被「UTF-8 字节按 latin1 解码 → 再按 utf8mb4 编码」，双重编码成乱码
-- （表现为 åˆ›å»ºæ—¶é—´ 之类，而非 ???）。
SET NAMES utf8mb4;

-- 建库。Docker 入口会依据 MYSQL_DATABASE 自动选中该库，此处显式声明是为了让
-- 脚本也能脱离 Docker 独立执行（如手动 source 到本机 MySQL）。
CREATE DATABASE IF NOT EXISTS `trip_craft`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `trip_craft`;


-- -----------------------------------------------------------------------------
-- 1. trip_marker — 旅行地图打卡标记表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `trip_marker` (
    `id`         BIGINT        NOT NULL AUTO_INCREMENT           COMMENT '主键ID',
    `title`      VARCHAR(100)  NOT NULL                          COMMENT '标记地点名称',
    `province`   VARCHAR(50)   DEFAULT ''                        COMMENT '所属省份',
    `city`       VARCHAR(50)   DEFAULT ''                        COMMENT '所属城市（直辖市时与省份相同）',
    `longitude`  DECIMAL(10,7) NOT NULL                          COMMENT '经度 (GCJ-02)',
    `latitude`   DECIMAL(10,7) NOT NULL                          COMMENT '纬度 (GCJ-02)',
    `notes`      VARCHAR(255)  DEFAULT ''                        COMMENT '打卡备注',
    `created_at` DATETIME      DEFAULT CURRENT_TIMESTAMP         COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '旅行地图打卡标记表';


-- -----------------------------------------------------------------------------
-- 2. trip_plan — 行程规划主表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `trip_plan` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '行程主键ID',
    `title`       VARCHAR(100) NOT NULL                          COMMENT '行程标题（如：杭州秋季3日游）',
    `start_date`  DATE         NOT NULL                          COMMENT '出发日期',
    `end_date`    DATE         NOT NULL                          COMMENT '结束日期',
    `total_days`  INT          NOT NULL                          COMMENT '总天数（由服务端按起止日期计算）',
    `cover_color` VARCHAR(20)  DEFAULT '#3b82f6'                 COMMENT '卡片主题色',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP         COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '行程规划主表';


-- -----------------------------------------------------------------------------
-- 3. trip_day — 行程每日日程表（逻辑关联 trip_plan.id）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `trip_day` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT                COMMENT '主键ID',
    `trip_id`    BIGINT   NOT NULL                               COMMENT '所属行程ID（关联 trip_plan.id）',
    `day_index`  INT      NOT NULL                               COMMENT '第几天（如：1 代表 Day 1）',
    `plan_date`  DATE     NOT NULL                               COMMENT '对应具体公历日期',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP              COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_trip_id` (`trip_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '行程每日日程表';


-- -----------------------------------------------------------------------------
-- 4. trip_node — 行程每日打卡节点表（逻辑关联 trip_day.id）
--    order_num 显式记录当天游览次序，拖拽重排时按下标整体重写为 index + 1。
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `trip_node` (
    `id`         BIGINT        NOT NULL AUTO_INCREMENT           COMMENT '主键ID',
    `day_id`     BIGINT        NOT NULL                          COMMENT '所属每日日程ID（关联 trip_day.id）',
    `spot_name`  VARCHAR(100)  NOT NULL                          COMMENT '游玩地点名称（如：西湖断桥）',
    `longitude`  DECIMAL(10,7) DEFAULT NULL                      COMMENT '经度 (GCJ-02)，可空（暂未接入地图选点）',
    `latitude`   DECIMAL(10,7) DEFAULT NULL                      COMMENT '纬度 (GCJ-02)，可空（暂未接入地图选点）',
    `order_num`  INT           NOT NULL DEFAULT 1                COMMENT '当天游览次序（1, 2, 3...）',
    `notes`      VARCHAR(255)  DEFAULT ''                        COMMENT '游玩备注 / 攻略提示',
    `created_at` DATETIME      DEFAULT CURRENT_TIMESTAMP         COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_day_id` (`day_id`),
    INDEX `idx_day_order` (`day_id`, `order_num`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '行程每日打卡节点表';
