# 本地开发指南

后端模块的架构与开发注意事项。

## 后端分层

```
com.tripcraft
├── TripCraftApplication        # 启动类；@MapperScan("com.tripcraft.mapper")
├── controller/                 # 仅做参数接收与路由
│   ├── HealthController        #   GET /api/health
│   └── TripMarkerController    #   GET|POST /api/markers
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
- 连接本地 MySQL 的客户端也要用 utf8mb4：
  `mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3306 -uroot -proot`

## Docker MySQL 数据卷说明

- MySQL 数据放在 Docker **命名卷** `docker_mysql_data` 中，而非宿主机目录：
  - 原因：Docker Desktop 的宿主文件共享层不支持 `chown`，而 MySQL 官方镜像初始化必须把数据目录
    owner 改成 mysql(uid 999)；bind mount 数据目录会导致容器无限重启
    （日志：`chown: changing ownership of ... Operation not permitted`）。
- 查看/备份数据：`docker exec -it tripcraft-mysql mysql -uroot -proot trip_craft`
- **禁止把 `docker/mysql_data/`、`docker/redis_data/` 提交进 git**（已在根 `.gitignore` 忽略）。

## 已知问题 / 踩坑记录

1. **init SQL 中文 COMMENT 双重编码**
   MySQL 首次初始化执行 `/docker-entrypoint-initdb.d/*.sql` 时，若导入连接未显式使用 utf8mb4，
   文件里的中文表/列注释会被双重编码存成乱码（如 `åˆ›å»ºæ—¶é—´`）。
   - 修复：在 `docker/mysql/init/01_schema.sql` 首行加 `SET NAMES utf8mb4;`
   - 仅对「删除卷后的首次初始化」生效；已存在的库需用 `ALTER TABLE ... COMMENT '正确中文'` 重写元数据。
   - 该问题只影响表/列注释等**元数据**；应用经 JDBC（characterEncoding=utf8）写入的中文数据不受影响。

2. **经纬度返回精度**
   DB 列 `decimal(10,7)`，查询返回按 7 位小数序列化（如 `121.4903000`）；
   POST 回显保持请求体原样。如要求一致，可在 Service 中插入后 `getById` 重查。

3. **无 Maven Wrapper**
   仓库未提交 `mvnw`，统一用系统 `mvn`。如需 wrapper：`mvn wrapper:wrapper`。

## 前端调用约定

- 前端通过相对路径 `/api/...` 请求后端，由 `vite.config.ts` 开发代理转发到 `localhost:8080`。
- 高德 Key 与安全密钥从 `.env.local` 读取（`VITE_AMAP_KEY` / `VITE_AMAP_SECURITY_CODE`），该文件已被忽略、不入库。
