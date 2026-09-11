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

### 2.1 查询所有打卡点

```
GET /api/markers
```

按创建时间倒序返回全部记录。**响应 200**

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

按主键物理删除指定打卡点。**响应 200**

```json
{ "code": 200, "message": "success", "data": null }
```

**示例（curl）**

```bash
curl http://localhost:8080/api/markers

curl -X POST http://localhost:8080/api/markers \
  -H 'Content-Type: application/json' \
  -d '{"title":"外滩","province":"上海市","city":"上海市","longitude":121.4903,"latitude":31.2397,"notes":"夜景"}'

curl -X DELETE http://localhost:8080/api/markers/2
```

---

## 错误处理

由 `GlobalExceptionHandler`（`@RestControllerAdvice`）统一捕获未处理异常，返回 `Result`：

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
| province | varchar(50) | 所属省份 |
| city | varchar(50) | 所属城市 |
| longitude | decimal(10,7) | 经度 (GCJ-02) |
| latitude | decimal(10,7) | 纬度 (GCJ-02) |
| notes | varchar(255) | 打卡备注，默认 `''` |
| created_at | datetime | 创建时间，默认 `CURRENT_TIMESTAMP` |

> 建表脚本见 `docker/mysql/init/01_schema.sql`（首行 `SET NAMES utf8mb4;` 确保中文注释正确）。
> Docker 首次启动会自动执行；实体字段与列名经 MyBatis-Plus 下划线→驼峰映射（`created_at` ↔ `createdAt`）。

### 分层调用链

```
TripMarkerController  →  TripMarkerService (IService<TripMarker>)
                     →  TripMarkerMapper (BaseMapper<TripMarker>)
                     →  trip_marker 表
```

Controller 直接使用 `IService` 提供的内置方法（`list` / `save` / `removeById`），
`TripMarkerService` / `TripMarkerServiceImpl` 中的自定义方法（`listMarkers` / `createMarker`）当前未被 Controller 调用。