# 接口文档 (REST API)

默认基础地址：`http://localhost:8080`

所有接口当前返回 **未包装** 的原生 JSON；出错时返回 Spring Boot 默认错误体：

```json
{ "timestamp": "...", "status": 500, "error": "Internal Server Error", "path": "/api/markers" }
```

---

## 1. 健康检查

```
GET /api/health
```

**响应 200**

```json
{ "status": "UP", "appName": "TripCraft" }
```

---

## 2. 打卡点

### 查询所有打卡点

```
GET /api/markers
```

按创建时间倒序返回全部记录。**响应 200**

```json
[
  {
    "id": 1,
    "title": "外滩",
    "longitude": 121.4903000,
    "latitude": 31.2397000,
    "notes": "夜景",
    "createdAt": "2026-09-08T10:48:17"
  }
]
```

> 经纬度来自数据库 `decimal(10,7)`，故会按 7 位小数返回。

### 新增打卡点

```
POST /api/markers
Content-Type: application/json
```

**请求体**

```json
{ "title": "外滩", "longitude": 121.4903, "latitude": 31.2397, "notes": "夜景" }
```

| 字段 | 必填 | 说明 |
|---|---|---|
| title | 是 | 打卡点名称 |
| longitude | 是 | 经度 (GCJ-02) |
| latitude | 是 | 纬度 (GCJ-02) |
| notes | 否 | 备注，默认 `''` |
| id / createdAt | 否 | 由服务端生成，传入会被忽略覆盖 |

**响应 201**：返回保存后的完整记录（含自增 `id` 与 `createdAt`）。

```json
{ "id": 2, "title": "外滩", "longitude": 121.4903, "latitude": 31.2397, "notes": "夜景", "createdAt": "2026-09-08T10:48:17" }
```

**示例（curl）**

```bash
curl http://localhost:8080/api/markers
curl -X POST http://localhost:8080/api/markers \
  -H 'Content-Type: application/json' \
  -d '{"title":"外滩","longitude":121.4903,"latitude":31.2397,"notes":"夜景"}'
```

---

## 数据模型

### `trip_marker`（旅行地图打卡标记表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint (PK, AUTO_INCREMENT) | 主键 |
| title | varchar(100) | 标记地点名称 |
| longitude | decimal(10,7) | 经度 (GCJ-02) |
| latitude | decimal(10,7) | 纬度 (GCJ-02) |
| notes | varchar(255) | 打卡备注，默认 `''` |
| created_at | datetime | 创建时间，默认 `CURRENT_TIMESTAMP` |

> 建表脚本见 `docker/mysql/init/01_schema.sql`。Docker 首次启动会自动执行；实体字段与列名经 MyBatis-Plus 下划线→驼峰映射（`created_at` ↔ `createdAt`）。

### 分层调用链

```
TripMarkerController  →  TripMarkerService (IService<TripMarker>)
                     →  TripMarkerServiceImpl (ServiceImpl<Mapper, Entity>)
                     →  TripMarkerMapper (BaseMapper<TripMarker>)
                     →  trip_marker 表
```
