# 本地开发指南

后端模块的架构与开发注意事项。

## 后端分层

```
com.tripcraft
├── TripCraftApplication        # 启动类；@MapperScan("com.tripcraft.mapper")
├── controller/                 # 仅做参数接收与路由，统一返回 Result<T>
│   ├── HealthController        #   GET /api/health（裸返回，不包装）
│   ├── TripMarkerController    #   GET|POST|DELETE /api/markers、GET /api/markers/stats
│   ├── TripPlanController      #   GET|POST /api/trips、GET /api/trips/{id}
│   └── TripNodeController      #   GET    /api/trips/days/{dayId}/nodes
│                               #   POST   /api/trips/nodes
│                               #   DELETE /api/trips/nodes/{id}
│                               #   PUT    /api/trips/days/{dayId}/nodes/reorder
├── common/                     # Result 统一返回体、GlobalExceptionHandler 全局异常
├── service/
│   ├── TripMarkerService       # 继承 IService<TripMarker>；getFootprintStats/saveMarker/deleteMarker
│   ├── TripPlanService         # 继承 IService<TripPlan>；getTripDetail/createTripWithDays
│   ├── TripNodeService         # 继承 IService<TripNode>；listByDayId/addNode/reorderNodes
│   └── impl/
│       ├── TripMarkerServiceImpl   # 足迹统计（Redis Cache-Aside）、增删打卡点并失效缓存
│       ├── TripPlanServiceImpl     # @Transactional 创建行程并自动生成分天子记录
│       └── TripNodeServiceImpl     # 节点增删 + @Transactional 拖拽批量重排序号
├── mapper/
│   ├── TripMarkerMapper        # 继承 BaseMapper<TripMarker>
│   ├── TripPlanMapper          # 继承 BaseMapper<TripPlan>
│   ├── TripDayMapper           # 继承 BaseMapper<TripDay>
│   └── TripNodeMapper          # 继承 BaseMapper<TripNode>
├── entity/
│   ├── TripMarker              # trip_marker：打卡标记（含 province/city）
│   ├── TripPlan                # trip_plan：行程主表
│   ├── TripDay                 # trip_day：行程分天子表
│   └── TripNode                # trip_node：每日游玩节点（含 order_num 游览次序）
└── vo/
    ├── FootprintStatsVO        # 足迹统计聚合结果（含内部类 ProvinceStat）
    └── TripDetailVO            # 行程详情（plan + days 一次组装）
```

约定：
- Controller 不直接依赖 Mapper，业务逻辑下沉到 Service。
- 复杂聚合（足迹统计）放在 Service 中做，Controller 保持薄；SQL 聚合用 `QueryWrapper`/`LambdaQueryWrapper`。
- 行程相关接口统一挂在 **`/api/trips`** 前缀下（行程主表 + 分天子表 + 游玩节点共用一个 Controller 前缀，靠子路径区分）。
- 涉及「一次写多行」的操作（创建行程建多天子表、拖拽重排批量改 `order_num`）一律加
  `@Transactional(rollbackFor = Exception.class)`，避免写一半失败留下脏数据。

### 行程节点排序模型

游玩节点用 **`order_num` 显式存游览次序**（而非依赖主键或创建时间）：

- 新增节点：`addNode` 先查当天已有的最大 `order_num`，+1 作为新节点次序，追加到末尾。
- 拖拽重排：前端把拖拽后的**有序 ID 列表**整体 `PUT` 上来，后端按下标重写 `order_num = index + 1`，
  整批包在一个事务里。
- 查询：`listByDayId` 固定 `ORDER BY order_num ASC`，保证前端渲染顺序与库中一致。

> 为什么不用「只改两个节点」的相邻交换？整体重写下标实现简单、无边界判断，
> 且一天节点数量很小（十几个），一次批量更新的成本可以忽略。

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

## Redis 缓存（足迹统计，Cache-Aside 模式）

后端 `spring-boot-starter-data-redis`，`application.yml` 连接 `localhost:6380`（容器内 6379）。

- 缓存键：`trip:footprint:stats`，值为 `FootprintStatsVO` 的 JSON。
- **读**（`getFootprintStats`）：先查 Redis → 命中直接反序列化返回；未命中查 MySQL 聚合 → 回写 Redis 并设 **2 小时 TTL**。
- **写**（认真做好一致性）：
  - `saveMarker`：先写库，成功后**删除缓存**（`redisTemplate.delete`）。
  - `deleteMarker`：先删库，成功后**删除缓存**。
- 反序列化失败（如缓存格式变更）会打日志降级到查库，不阻塞业务。

> 约定：**写库必删缓存**，杜绝「先写库再改缓存」的老旧做法带来的脏读窗口。
> 当前仅足迹统计这一处用缓存，后续行程分类似「读多写少」接口可复用同一模式。

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

4. **`01_schema.sql` 里混入了 shell 命令（会导致初始化失败）**
   `docker/mysql/init/01_schema.sql` 的 `trip_node` 建表段落目前是这样写的：

   ```sql
   -- 3. 每日行程节点表 (关联 trip_node)
   docker exec -i tripcraft-mysql mysql -uroot -proot trip_craft << 'EOF'
   CREATE TABLE IF NOT EXISTS `trip_node` ( ... );
   EOF
   ```

   第 43 行的 `docker exec ... << 'EOF'` 与结尾的 `EOF`，是**在终端里手动补建表时敲的命令被一并粘贴进了
   .sql 文件**。该文件由 MySQL 客户端执行，读到这一行会直接语法报错，`trip_node` 建不出来。
   **修法**：删掉 `docker exec ... << 'EOF'` 那一行和末尾的 `EOF` 行，只保留 `CREATE TABLE ... ;`
   语句（与上方 `trip_plan`、`trip_day` 保持同一风格）。

   > 这条命令本身没错，只是用错了地方——它属于「手动补建表」时在宿主机终端执行的操作，
   > 应写在文档或命令行里，而不是放进 `docker-entrypoint-initdb.d` 的初始化脚本。

5. **`addNode` 取「最大 order_num」的写法有误**
   `TripNodeServiceImpl.addNode` 中：

   ```java
   query.eq(TripNode::getDayId, node.getDayId())
        .orderByAsc(TripNode::getOrderNum)   // ← 升序
        .last("LIMIT 1");
   ```

   `orderByAsc + LIMIT 1` 取到的是**最小**的 `order_num`，与注释「推算当前最大的 orderNum + 1」不符，
   会让新节点次序算错（如已有 1、2、3，新节点又算成 2，与既有节点撞号）。
   **修法**：改成 `.orderByDesc(TripNode::getOrderNum)`。

## 前端路由（vue-router）

三个视图由 **vue-router** 按 URL 切换，取代了早期 `App.vue` 里的 `currentView` 内存变量 +
`el-radio-group` + `v-show`/`v-if` 的写法。

路由表见 `src/router/index.ts`：

| path | name | 组件 |
|---|---|---|
| `/` | map | `MapContainer.vue` |
| `/footprint` | footprint | `FootprintBoard.vue` |
| `/planner` | planner | `TripPlanner.vue` |

- `main.ts` 中 `app.use(router)`（必须在 `mount` 之前）。
- `App.vue` 顶部用 `<router-link to="...">` 导航，主体只留一个 `<router-view />`，
  组件由路由表决定渲染，App 不再 import 任何页面组件。
- 高亮当前页用 vue-router 自动加的 `.router-link-active` class，无需自己维护选中状态。

> **为什么改成路由？** 早期的 `currentView` 是纯内存变量，浏览器一刷新整个 JS 重跑就归零，
> 于是永远跳回默认的打卡页。把「页面」绑定到 URL 后，刷新请求的就是当前 URL，自然停在原页；
> 顺带获得了可分享链接、前进/后退、收藏到具体页面的能力。

## 前端调用约定

- 前端通过相对路径 `/api/...` 请求后端，由 `vite.config.ts` 开发代理转发到 `localhost:8080`。
- `vite.config.ts` 里 `server.host = '0.0.0.0'`，允许**局域网内其他设备**（如同学电脑）访问 `http://<你的IP>:5173`。
- 高德 Key 与安全密钥从 `.env.local` 读取（`VITE_AMAP_KEY` / `VITE_AMAP_SECURITY_CODE`），该文件已被忽略、不入库。
- 各视图职责：
  - `MapContainer.vue`：高德地图点击落点打卡（`AMap.Geocoder` 逆地理编码自动填充省市）。
  - `FootprintBoard.vue`：ECharts 中国地图，本地 `public/100000_full.json` GeoJSON（避免离线/局域网 CDN 失败），
    消费 `/api/markers/stats` + `/api/markers?province=` 下钻抽屉。
  - `TripPlanner.vue`：行程清单 + 分天 Tab 编排 + **游玩节点拖拽重排**（`vuedraggable`，
    松手后 `PUT .../nodes/reorder` 回传有序 ID 列表）。
- UI 组件库：Element Plus；地图渲染：ECharts + `@amap/amap-jsapi-loader`。

### vuedraggable 使用注意

`vuedraggable@4`（SortableJS 的 Vue 3 封装）包名就是 `vuedraggable`——**没有 `vuedraggable-next` 这个包**，
装错会 E404。

列表绑定推荐用 **`:list` 而不是 `v-model`**：

- `v-model` 会让组件内部维护一份数组副本，和父组件的 `nodeList` 争夺控制权，
  容易出现「拖着卡死」或「界面顺序与数据顺序不一致」。
- 改用 `:list="nodeList"`（组件直接就地修改父数组）后，在 `@end` 回调里
  `nodeList.value = [...nodeList.value]` 手动触发一次响应式更新，再取 `map(item => item.id)` 上报后端。
- 拖拽把手用 `handle=".drag-handle"` 限定，避免点删除按钮时误触发拖拽。
