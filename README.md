# 伴游行 (TripCraft)

> 面向国内出行的轻量级智能行程规划、足迹打卡与协同中台。
> 致力于解决多景点路线绕路痛点、朋友出游 AA 复杂平账问题，并提供高颜值的中国足迹点亮大盘。

---

## 🛠️ 技术选型

### 后端 (trip-craft-server)
- **核心框架**：Spring Boot 3.3.x + JDK 21 (虚拟线程支持)
- **持久层**：MyBatis-Plus + MySQL 8.0
- **缓存与并发**：Redis 7.x + Redisson 分布式锁
- **协同与通信**：Spring WebSocket (STOMP)

### 前端 (trip-craft-web)
- **核心架构**：Vue 3 (Composition API) + Vite + TypeScript
- **状态管理**：Pinia
- **地图底座**：高德地图 JS API 2.0 (`@amap/amap-jsapi-loader`)
- **数据可视化**：Apache ECharts (全国 GeoJSON 足迹大盘)

### 基础设施
- **容器化**：Docker & Docker Compose 一键编排中间件

---

## 📂 目录结构

```text
trip-craft/
├── docker/                 # 本地开发基础设施编排 (MySQL 8.0, Redis 7)
├── trip-craft-server/      # 后端 Spring Boot 3 工程
└── trip-craft-web/         # 前端 Vue 3 + Vite 工程

🚀 本地快速启动
1. 启动基础设施
进入 docker/ 目录，一键拉起 MySQL 和 Redis：
cd docker
docker compose up -d

2. 启动后端服务
进入 trip-craft-server/：
./mvnw spring-boot:run
访问健康检查接口验证：http://localhost:8080/api/health

3. 启动前端页面
进入 trip-craft-web/：
npm install
# 配置你的高德地图 Key (参考 .env.example 创建 .env.local)
npm run dev
浏览器打开 http://localhost:5173 即可查看全屏高德 3D 地图。
