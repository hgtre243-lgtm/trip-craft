# 接口文档 (REST API)

默认基础地址：`http://localhost:8080`

## 统一返回体 `Result<T>`

所有业务接口均返回统一包装结构（含异常，见「错误处理」）：

```json
{ "code": 200, "message": "success", "data": { } }
```

| 字段 | 说明 |
|---|---|
| code | 业务状态码：`200` 成功，`500` 失败 |
| message | 提示信息：成功为 `success`，失败为错误描述 |
| data | 业务数据；无数据时为 `null` |

---

## 1. 健康检查

```
GET /api/health
```

> 此接口为裸返回，不属于 `Result` 包装。

**响应 200**

```json
{ "status": "UP", "appName": "TripCraft" }
```

---

## 2. 打卡点

### 2.1 查询打卡点（可按省份筛选）

```
GET /api/markers               # 全部
GET /api/markers?province=浙江省  # 仅某省
```

按创建时间倒序返回记录；传入 `province` 时按该省过滤（用于足迹大盘的省份下钻抽屉）。**响应 200**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "title": "外滩",
      "province": "上海市",
      "city": "上海市",
      "longitude": 121.4903000,
      "latitude": 31.2397000,
      "notes": "夜景",
      "createdAt": "2026-09-08T10:48:17"
    }
  ]
}
```

> 经纬度来自数据库 `decimal(10,7)`，故会按 7 位小数返回。

### 2.2 新增打卡点

```
POST /api/markers
Content-Type: application/json
```

**请求体**

```json
{ "title": "外滩", "province": "上海市", "city": "上海市", "longitude": 121.4903, "latitude": 31.2397, "notes": "夜景" }
```

| 字段 | 必填 | 说明 |
|---|---|---|
| title | 是 | 打卡点名称 |
| longitude | 是 | 经度 (GCJ-02) |
| latitude | 是 | 纬度 (GCJ-02) |
| province | 否 | 省份（前端地图点击后逆地理编码自动填充） |
| city | 否 | 城市（直辖市时与省份相同） |
| notes | 否 | 备注，默认 `''` |
| id / createdAt | 否 | 由服务端生成，传入会被忽略覆盖 |

**响应 200**：返回保存后的完整记录（含自增 `id` 与 `createdAt`）。

```json
{
  "code": 200,
  "message": "success",
  "data": { "id": 2, "title": "外滩", "province": "上海市", "city": "上海市", "longitude": 121.4903, "latitude": 31.2397, "notes": "夜景", "createdAt": "2026-09-08T10:48:17" }
}
```

### 2.3 删除打卡点

```
DELETE /api/markers/{id}
```

按主键物理删除指定打卡点（删除成功后同步失效 Redis 足迹缓存，保持统计一致）。**响应 200**

```json
{ "code": 200, "message": "success", "data": null }
```

### 2.4 全国足迹统计（带 Redis 缓存）

```
GET /api/markers/stats
```

Aggregate 接口：累计打卡数、已点亮省份数、全国覆盖率，以及各省打卡明细。首次请求查询 MySQL 聚合后回写
Redis（TTL 2h）；后续命中缓存直接返回。打卡点新增 / 删除时会主动删除该缓存键，保证统计及时。

> 使用 Cache-Aside 模式：读回源 + 写回填，写操作（增/删）同时清理缓存。

**响应 200**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalMarkers": 23,
    "visitedProvinceCount": 6,
    "coverageRate": 17.6,
    "provinceList": [
      { "province": "浙江省", "count": 12 },
      { "province": "上海市", "count": 5 }
    ]
  }
}
```

| 字段 | 说明 |
|---|---|
| totalMarkers | 累计打卡总数 |
| visitedProvinceCount | 已点亮省份数量（不含空省份） |
| coverageRate | 全国覆盖率百分比 = 已点亮省份数 / 34 |
| provinceList[].province | 省份名 |
| provinceList[].count | 该省打卡数量 |

**示例（curl）**

```bash
curl http://localhost:8080/api/markers
curl 'http://localhost:8080/api/markers?province=浙江省'
curl http://localhost:8080/api/markers/stats

curl -X POST http://localhost:8080/api/markers \
  -H 'Content-Type: application/json' \
  -d '{"title":"外滩","province":"上海市","city":"上海市","longitude":121.4903,"latitude":31.2397,"notes":"夜景"}'

curl -X DELETE http://localhost:8080/api/markers/2
```

---

## 3. 行程规划

### 3.1 获取行程列表

```
GET /api/trips
```

返回全部行程（按主键顺序），**响应 200**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "title": "杭州秋季赏枫3日游",
      "startDate": "2026-10-15",
      "endDate": "2026-10-17",
      "totalDays": 3,
      "coverColor": "#3b82f6",
      "createdAt": "2026-09-08T10:48:17"
    }
  ]
}
```

### 3.2 创建行程（自动生成分天子日程）

```
POST /api/trips
Content-Type: application/json
```

**请求体**

```json
{ "title": "杭州秋季赏枫3日游", "startDate": "2026-10-15", "endDate": "2026-10-17" }
```

| 字段 | 必填 | 说明 |
|---|---|---|
| title | 是 | 行程标题 |
| startDate | 是 | 出发日期 |
| endDate | 是 | 结束日期 |
| totalDays | 否 | 由服务端计算（`endDate - startDate + 1`），传入忽略 |
| coverColor | 否 | 卡片主题色，默认 `#3b82f6` |
| id / createdAt | 否 | 由服务端生成 |

> ⚠️ 单个方法内通过 `@Transactional` 强事务保证：先落 `trip_plan` 主记录，再按天数循环插入
> `trip_day` 子记录。当 `endDate < startDate` 时抛
> `IllegalArgumentException`（被全局异常处理器转为 `Result` 500）。

**响应 200**：返回持久化后的行程对象（`id` 与 `totalDays` 已回填）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "title": "杭州秋季赏枫3日游",
    "startDate": "2026-10-15",
    "endDate": "2026-10-17",
    "totalDays": 3,
    "coverColor": "#3b82f6",
    "createdAt": "2026-09-08T10:48:17"
  }
}
```

### 3.3 获取行程详情（含分天列表）

```
GET /api/trips/{id}
```

返回行程主信息 + 按 `dayIndex` 升序的分天子列表（一次请求组装为 `TripDetailVO`，避免前端多次请求）。
行程不存在时返回 `data: null`。**响应 200**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "plan": {
      "id": 1,
      "title": "杭州秋季赏枫3日游",
      "startDate": "2026-10-15",
      "endDate": "2026-10-17",
      "totalDays": 3,
      "coverColor": "#3b82f6",
      "createdAt": "2026-09-08T10:48:17"
    },
    "days": [
      { "id": 1, "tripId": 1, "dayIndex": 1, "planDate": "2026-10-15", "createdAt": "2026-09-08T10:48:17" },
      { "id": 2, "tripId": 1, "dayIndex": 2, "planDate": "2026-10-16", "createdAt": "2026-09-08T10:48:17" },
      { "id": 3, "tripId": 1, "dayIndex": 3, "planDate": "2026-10-17", "createdAt": "2026-09-08T10:48:17" }
    ]
  }
}
```

**示例（curl）**

```bash
curl http://localhost:8080/api/trips

curl -X POST http://localhost:8080/api/trips \
  -H 'Content-Type: application/json' \
  -d '{"title":"杭州秋季赏枫3日游","startDate":"2026-10-15","endDate":"2026-10-17"}'

curl http://localhost:8080/api/trips/1
```

---

## 4. 行程节点（每日游玩安排）

属于某个分天（`trip_day`）下的具体游玩地点，用 `orderNum` 记录当天游览次序。

### 4.1 获取某天的节点列表

```
GET /api/trips/days/{dayId}/nodes
```

按 `orderNum` **升序**返回该天的全部游玩节点；当天无节点时 `data` 为 `[]`。**响应 200**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "dayId": 1,
      "spotName": "西湖断桥",
      "longitude": 120.1500000,
      "latitude": 30.2600000,
      "orderNum": 1,
      "notes": "早上人少，适合拍照",
      "createdAt": "2026-09-16T10:48:17"
    },
    {
      "id": 2,
      "dayId": 1,
      "spotName": "灵隐寺",
      "longitude": null,
      "latitude": null,
      "orderNum": 2,
      "notes": "",
      "createdAt": "2026-09-16T10:50:02"
    }
  ]
}
```

> `longitude` / `latitude` 允许为 `null`（当前前端添加节点时只填名称与备注，尚未接入地图选点）。
> `dayId` 不存在时同样返回空数组，不报错。

### 4.2 添加游玩节点

```
POST /api/trips/nodes
Content-Type: application/json
```

**请求体**

```json
{ "dayId": 1, "spotName": "西湖断桥", "notes": "早上人少，适合拍照" }
```

| 字段 | 必填 | 说明 |
|---|---|---|
| dayId | 是 | 所属分天 ID（`trip_day.id`） |
| spotName | 是 | 游玩地点名称 |
| notes | 否 | 游玩备注 / 攻略提示，默认 `''` |
| longitude / latitude | 否 | 经纬度，暂未使用，可省略 |
| orderNum | 否 | **由服务端自动推算**（当天现有最大 `orderNum` + 1），传入忽略 |
| id / createdAt | 否 | 由服务端生成 |

**响应 200**：返回落库后的完整节点对象（`id` 与 `orderNum` 已回填）。

```json
{
  "code": 200,
  "message": "success",
  "data": { "id": 1, "dayId": 1, "spotName": "西湖断桥", "orderNum": 1, "notes": "早上人少，适合拍照", "createdAt": "2026-09-16T10:48:17" }
}
```

> ⚠️ 已知缺陷：`addNode` 内部用 `orderByAsc(...).last("LIMIT 1")` 取「最大 orderNum」，实际取到的是最小值，
> 会让新节点次序算错（详见 `docs/development.md` 踩坑记录第 5 条，应改为 `orderByDesc`）。

### 4.3 删除游玩节点

```
DELETE /api/trips/nodes/{id}
```

按主键物理删除节点。**响应 200**

```json
{ "code": 200, "message": "success", "data": null }
```

> 删除后**不会**自动重排当天剩余节点的 `orderNum`，序号会出现空档（如 1、3、4）。
> 因查询只依赖 `orderByAsc`，显示顺序仍正确；如需连续序号可在删除后调一次 4.4 重排。

### 4.4 拖拽重排（批量更新游览次序）

```
PUT /api/trips/days/{dayId}/nodes/reorder
Content-Type: application/json
```

前端在拖拽结束后，把当前**完整的有序节点 ID 列表**整体提交，服务端按下标重写 `orderNum = index + 1`。

**请求体**（裸数组，不是对象）

```json
[3, 1, 2]
```

- **数组元素**：`trip_node.id`，顺序即拖拽后的目标顺序。
- **必须传全**：应传当天**全部**节点的 ID；只传部分会导致未传到的节点保留旧序号。
- **越权保护**：更新语句带 `WHERE id = ? AND day_id = {dayId}`，即使传入别的天的节点 ID 也不会被误改。

**响应 200**

```json
{ "code": 200, "message": "success", "data": null }
```

> ⚠️ 整个重排过程包在 `@Transactional(rollbackFor = Exception.class)` 中：任何一条更新失败都会**整体回滚**，
> 不会出现「改了一半」的乱序状态。

**示例（curl）**

```bash
curl http://localhost:8080/api/trips/days/1/nodes

curl -X POST http://localhost:8080/api/trips/nodes \
  -H 'Content-Type: application/json' \
  -d '{"dayId":1,"spotName":"西湖断桥","notes":"早上人少，适合拍照"}'

curl -X DELETE http://localhost:8080/api/trips/nodes/1

curl -X PUT http://localhost:8080/api/trips/days/1/nodes/reorder \
  -H 'Content-Type: application/json' \
  -d '[3,1,2]'
```

---

## 错误处理

由 `GlobalExceptionHandler`（`@RestControllerAdvice`）统一捕获未处理异常（含业务抛出的
`IllegalArgumentException`，如行程结束日期早于开始日期），返回 `Result`：

```json
{ "code": 500, "message": "服务器内部错误：<异常信息>", "data": null }
```

---

## 数据模型

### `trip_marker`（旅行地图打卡标记表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint (PK, AUTO_INCREMENT) | 主键 |
| title | varchar(100) | 标记地点名称 |
| province | varchar(50) | 所属省份，默认 `''` |
| city | varchar(50) | 所属城市（直辖市时与省份相同），默认 `''` |
| longitude | decimal(10,7) | 经度 (GCJ-02) |
| latitude | decimal(10,7) | 纬度 (GCJ-02) |
| notes | varchar(255) | 打卡备注，默认 `''` |
| created_at | datetime | 创建时间，默认 `CURRENT_TIMESTAMP` |

> 建表脚本见 `docker/mysql/init/01_schema.sql`（首行 `SET NAMES utf8mb4;` 确保中文注释正确）。
> Docker 首次启动会自动执行；实体字段与列名经 MyBatis-Plus 下划线→驼峰映射（`created_at` ↔ `createdAt`）。

### 分层调用链

```
TripMarkerController  →  TripMarkerServiceImpl (继承 IService<TripMarker>)
                     →  TripMarkerMapper (BaseMapper<TripMarker>)
                     →  trip_marker 表
                     +  StringRedisTemplate / ObjectMapper（足迹统计缓存）
```

---

### `trip_plan`（行程规划主表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint (PK, AUTO_INCREMENT) | 主键 |
| title | varchar(100) | 行程标题 |
| start_date | date | 出发日期 |
| end_date | date | 结束日期 |
| total_days | int | 总天数，由服务端计算 |
| cover_color | varchar(20) | 卡片主题色，默认 `#3b82f6` |
| created_at | datetime | 创建时间，默认 `CURRENT_TIMESTAMP` |

### `trip_day`（行程每日日程子表，关联 `trip_plan`）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint (PK, AUTO_INCREMENT) | 主键 |
| trip_id | bigint | 所属行程 ID（外键逻辑关联，建表 `idx_trip_id` 索引） |
| day_index | int | 第几天（1 代表 Day 1） |
| plan_date | date | 对应公历日期 |
| created_at | datetime | 创建时间，默认 `CURRENT_TIMESTAMP` |

### `trip_node`（每日游玩节点表，关联 `trip_day`）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint (PK, AUTO_INCREMENT) | 主键 |
| day_id | bigint | 所属分天 ID（逻辑关联 `trip_day.id`，建表 `idx_day_id` / `idx_day_order` 索引） |
| spot_name | varchar(100) | 游玩地点名称 |
| longitude | decimal(10,7) | 经度，可空（尚未接入地图选点） |
| latitude | decimal(10,7) | 纬度，可空 |
| order_num | int | 当天游览次序（1, 2, 3…），拖拽重排时被整体重写 |
| notes | varchar(255) | 游玩备注 / 攻略提示，默认 `''` |
| created_at | datetime | 创建时间，默认 `CURRENT_TIMESTAMP` |

### 分层调用链

```
TripPlanController  →  TripPlanServiceImpl (继承 IService<TripPlan>)
                   →  TripPlanMapper (BaseMapper<TripPlan>)
                   →  trip_plan 表
                   →  TripDayMapper (BaseMapper<TripDay>)  →  trip_day 表

TripNodeController  →  TripNodeServiceImpl (继承 IService<TripNode>)
                   →  TripNodeMapper (BaseMapper<TripNode>)
                   →  trip_node 表
```

- 上述行程/子表映射：`total_days ↔ totalDays`、`plan_date ↔ planDate`、`order_num ↔ orderNum`、
  `spot_name ↔ spotName` 等均经 MyBatis-Plus 驼峰映射。
- `trip_day` / `trip_node` 与 `TripMarker` 目前**均无外键约束**，靠 `trip_id` / `day_id` 逻辑关联，
  索引已建但数据库层不强制一致性（删除行程时不会级联删子表）。
- 行程三张表共用 `/api/trips` 前缀，分别由 `TripPlanController`（`/api/trips`、`/api/trips/{id}`）
  与 `TripNodeController`（`/api/trips/days/...`、`/api/trips/nodes/...`）提供接口。