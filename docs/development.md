# 本地开发指南

后端模块的架构与开发注意事项。

## 后端分层

```
com.tripcraft
├── TripCraftApplication        # 启动类；@MapperScan("com.tripcraft.mapper")
├── controller/                 # 仅做参数接收与路由
│   ├── HealthController        #   GET /api/health
│   └── TripMarkerController    #   GET|POST|DELETE /api/markers，返回 Result<T> 统一包装
├── common/                     # Result 统一返回体、GlobalExceptionHandler 全局异常
├── service/
│   ├── TripMarkerService       # 接口，继承 MyBatis-Plus IService<TripMarker>
│   └── impl/TripMarkerServiceImpl
├── mapper/
│   └── TripMarkerMapper        # 继承 BaseMapper<TripMarker>，由 @MapperScan 注册
└── entity/
    └── TripMarker              # Lombok @Data
```

约定：Controller 不直接依赖 Mapper，业务逻辑下沉到 Service。

## 本地跑起来

依赖：JDK 21、Maven 3.8+、Docker。

```bash
# 1. 中间件（MySQL/Redis）
cd docker && docker compose up -d

# 2. 后端（端口 8080）
cd trip-craft-server && mvn spring-boot:run

# 3. 前端（端口 5173；需高德 Key，见 .env.example）
cd trip-craft-web && npm install && npm run dev
```

> 首次 `docker compose up -d` 会自动建库 `trip_craft` 并执行 `docker/mysql/init/01_schema.sql` 建表。

## MySQL 配置（字符集）

- 库表一律使用 `utf8mb4`（docker-compose 已指定 server charset/collation）。
- **JDBC URL 不要写 `characterEncoding=utf8mb4`**。Connector/J 把该参数当作 Java 字符集名解析，
  `utf8mb4` 不是 Java 字符集名，会抛 `UnsupportedEncodingException`。
  应写 `characterEncoding=utf8`（Java UTF-8），Connector/J 会自动与服务端 utf8mb4 协商。
- 连接本地 MySQL 的客户端也要用 utf8mb4（后端连的是容器，宿主端口 3307）：
  `mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -proot`

## Docker MySQL 数据卷说明

- MySQL 数据放在 Docker **命名卷** `docker_mysql_data` 中，而非宿主机目录：
  - 原因：Docker Desktop 的宿主文件共享层不支持 `chown`，而 MySQL 官方镜像初始化必须把数据目录
    owner 改成 mysql(uid 999)；bind mount 数据目录会导致容器无限重启
    （日志：`chown: changing ownership of ... Operation not permitted`）。
- 查看/备份数据：`docker exec -it tripcraft-mysql mysql -uroot -proot trip_craft`
- **禁止把 `docker/mysql_data/`、`docker/redis_data/` 提交进 git**（已在根 `.gitignore` 忽略）。

## ⚠️ 宿主 / 容器双 MySQL 实例

本机可能存在两台**互相独立、数据互不相通**的 MySQL：

| 实例 | 访问方式 | 用途 |
|---|---|---|
| 宿主机本机 MySQL | `127.0.0.1:3306`（`/usr/sbin/mysqld` 系统服务） | 本机直装，端口 3306（**遗留，后端已不连它**） |
| Docker 容器 MySQL | 宿主机 `3307` → 容器内部 `3306`（`docker-compose.yml` 端口映射） | **后端当前连接的对象** |

- 后端 `application.yml` 连接的是 `localhost:3307`（**容器那台**），不是宿主机的 3306。
- 一开始误用 `3306:3306` 映射时，容器会和宿主机 mysqld 争抢同一端口，并各自建出同名的
  `trip_craft.trip_marker` 表——两张表结构、数据、注释可能不同，极易混淆。
- 当前 compose 已改为 `3307:3306` 以避开冲突，后端也固定连容器 3307。确认「改的是哪台」：
  ```bash
  mysql -h127.0.0.1 -P3307 -uroot -proot trip_craft              # 容器(后端连的这台)
  docker exec -it tripcraft-mysql mysql -uroot -proot trip_craft  # 容器内直接进(内部3306)
  mysql -h127.0.0.1 -P3306 -uroot -proot trip_craft              # 宿主机(遗留,已不再使用)
  ```

## 已知问题 / 踩坑记录

1. **中文 COMMENT 双重编码（双重编码 / mojibake）**
   建表脚本里中文注释被写入时，若连接字符集不是 utf8mb4，会被“解码再编码”一遍而变成乱码。

   - **成因**：MySQL 官方镜像首次启动会用自带 `mysql` 客户端执行 `/docker-entrypoint-initdb.d/*.sql`，
     那次连接默认 latin1。脚本注释「开终究建时…」的 UTF-8 字节被当作 latin1 解读，
     再按列的 utf8mb4 存进去，就变成 `åˆ›å»ºæ—¶é—´`。本质是 **UTF-8 → 当 latin1 → 再 utf8mb4** 的双重编码。
   - **判定特征**：乱码形如 `åˆ› å»º æ—¶é—´`、`ä¸»é”®ID`，而不是 `???`；
     对乱码做 `latin1 → utf8mb4` 逆向转换能还原中文，即可确认为双重编码。
   - **典型现象（双实例对比）**：宿主机表注释正常、容器表注释乱码，是因为两台实例建表时的
     连接字符集不同、各建各的（见上「宿主/容器双 MySQL 实例」）。甚至可能出现同一张表
     「表注释正常、列注释乱码」——表级 `COMMENT` 与列级 `COMMENT` 在不同连接/不同时刻写入所致。
   - **该问题只影响元数据**：表/列注释、报错信息等；应用经 JDBC（characterEncoding=utf8，整条链路 utf8mb4）
     写入的**中文数据不受影响**，不会丢。
   - **修复**：
     1. 用 utf8mb4 连接重写已坏注释：
        ```bash
        docker exec -it tripcraft-mysql mysql -uroot -proot --default-character-set=utf8mb4 trip_craft
        ```
        ```sql
        SET NAMES utf8mb4;
        ALTER TABLE trip_marker
          MODIFY COLUMN `title` varchar(100) NOT NULL COMMENT '标记地点名称',
          ...; -- 逐列重写正确注释
        ```
     2. 治本：在 `docker/mysql/init/01_schema.sql` **首行**加 `SET NAMES utf8mb4;`
        （仅对「删除命名卷后的首次初始化」生效；已存在的库用上面 ALTER 修）。

2. **经纬度返回精度**
   DB 列 `decimal(10,7)`，查询返回按 7 位小数序列化（如 `121.4903000`）；
   POST 回显保持请求体原样。如要求一致，可在 Service 中插入后 `getById` 重查。

3. **无 Maven Wrapper**
   仓库未提交 `mvnw`，统一用系统 `mvn`。如需 wrapper：`mvn wrapper:wrapper`。

## 前端调用约定

- 前端通过相对路径 `/api/...` 请求后端，由 `vite.config.ts` 开发代理转发到 `localhost:8080`。
- 高德 Key 与安全密钥从 `.env.local` 读取（`VITE_AMAP_KEY` / `VITE_AMAP_SECURITY_CODE`），该文件已被忽略、不入库。
