<div align="center">

# 🗺️ 伴游行 TripCraft

面向国内出行的轻量级**行程规划 + 足迹打卡** Web 应用。

![license](https://img.shields.io/github/license/hgtre243-lgtm/trip-craft)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3%2B-brightgreen)
![Vue](https://img.shields.io/badge/Vue-3.5%2B-4FC08D)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.12-blue)

</div>

高德地图上**点击落点即可打卡**，前后端打通、数据落库；配备 **高德探索打卡 · 全国足迹大盘 · 行程规划** 三个视图，
后续规划智能路线避绕路、出游 AA 平账。

## ✨ 功能

**当前已实现**
- 🖱️ **高德地图打卡**：地图点击 → 逆地理编码自动识别省市 → 输入名称 → 立即落点打卡
- 📍 打卡点携带归属地（省份/城市），支持点击查看 / 删除（物理删除，地图与列表同步移除）
- 🇨🇳 **全国足迹大盘**（ECharts 中国地图）：已点亮省份染色、覆盖率 / 累计打卡统计，点击省份下钻查看历史足迹
- ⚡ 足迹统计经 **Redis Cache-Aside** 缓存（TTL 2h），增删打卡自动失效缓存
- 🧭 **行程规划**：创建行程 → 自动按起止日期生成多天日程骨架（强事务），分天 Tab 编排
- 🎯 **拖拽排程**：分天内游玩地点（景点 / 餐厅 / 酒店）卡片可拖拽重排游览次序，松手即批量落库
- 🔗 **前端路由**（vue-router）：`/` 打卡 · `/footprint` 足迹大盘 · `/planner` 行程规划，URL 即页面、刷新不丢当前视图
- 🛠️ 后端 REST API（MyBatis-Plus + MySQL 8 持久化，`Result` 统一返回体）
- 💚 健康检查接口

**规划中**
- 智能行程路线（避绕路）、出游 AA 平账
- 游玩节点接入经纬度与地图联动

## 🖼️ 演示

<!-- 截图占位：在此插入高德地图打卡页面的截图/动图 -->

> 项目处于开发早期，截图待补充，欢迎贡献。

## 🛠️ 技术栈

| 端 | 技术 |
|---|---|
| 后端 | Java 21 · Spring Boot 3.3 · Spring Web · MyBatis-Plus · MySQL 8 · Redis（Spring Data Redis）· Lombok |
| 前端 | Vue 3 · Vue Router 4 · Vite · TypeScript · 高德地图 JS API（逆地理编码）· ECharts 6 · Element Plus · Axios · vuedraggable |
| 基础设施 | Docker Compose（MySQL 8.0 · Redis 7），MySQL 命名卷 + 自动建表 |

## 📁 目录结构

```text
trip-craft/
├── docker/            # 中间件编排 + MySQL 初始化 SQL
├── trip-craft-server/ # 后端 Spring Boot 工程（controller / service / mapper / entity / vo）
├── trip-craft-web/    # 前端 Vue 3 工程
│   └── src/
│       ├── router/     # vue-router 路由表（/ · /footprint · /planner）
│       └── components/ # MapContainer · FootprintBoard · TripPlanner
├── docs/              # 接口文档 / 开发指南
└── LICENSE
```

## 🚀 快速开始

**依赖**：JDK 21 · Maven 3.8+ · Node 18+ · Docker

```bash
# 1. 基础设施（自动建库建表）
cd docker && docker compose up -d

# 2. 后端
cd ../trip-craft-server && mvn spring-boot:run
curl http://localhost:8080/api/health        # {"status":"UP","appName":"TripCraft"}
curl http://localhost:8080/api/markers/stats # 足迹统计（Redis 加速）

# 3. 前端（需高德 Key）
cd ../trip-craft-web
npm install
cp .env.example .env.local   # 填入 VITE_AMAP_KEY / VITE_AMAP_SECURITY_CODE
npm run dev                  # http://localhost:5173
```

> 本地端口：后端 8080 · 前端 5173 · MySQL 3307（容器内 3306，库 `trip_craft`）· Redis 6380（容器内 6379）。

## 📚 文档

- [接口文档 (REST API)](docs/api.md)：统一返回体、打卡点 / 足迹统计 / 行程 / 行程节点接口、数据模型
- [本地开发指南](docs/development.md)：后端分层、Redis 缓存、MySQL 字符集与踩坑记录、前端路由与调用约定

## 🤝 贡献

欢迎 PR / Issue！

- Fork 后从 `main` 切分支开发
- 后端遵循既有分层（controller → service → mapper），保持小步提交
- 改动请同时更新对应 `docs/` 文档

## 📄 License

[MIT](LICENSE)
