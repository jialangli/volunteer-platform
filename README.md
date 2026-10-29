# 志愿服务管理平台（Volunteer Platform）

广东石油化工学院 计算机科学与技术专业 Capstone 课程项目（7 人小组）。
覆盖 **活动发布 → 在线报名 → 签到签退 → 工时核算 → 个人服务档案 → 服务证明 → 统计可视化 → 系统管理** 全流程。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Java 17 · Spring Boot 3.3 · Spring Security(JWT+RBAC) · MyBatis-Plus · MySQL 8 · Redis 7 |
| 前端 | Vue 3 · Vite · Element Plus · Pinia · Axios · ECharts |
| 部署 | Nginx · Docker Compose（可选） |

## 目录结构

```
D:\VolunteerPlatform
├── backend/                 Spring Boot 后端（模块化单体）
│   └── src/main/java/com/gdupt/volunteer/
│       ├── common/          统一响应/错误码/异常处理/分页
│       ├── config/          Security、Redis、MyBatis 等配置
│       ├── auth/            认证授权模块
│       ├── activity/        活动管理模块
│       ├── registration/    报名管理模块
│       ├── attendance/      签到签退模块
│       ├── hours/           工时核算模块
│       ├── archive/         档案与证明模块
│       ├── stats/           统计可视化模块
│       └── admin/           系统管理模块
├── frontend/                Vue 3 前端
│   └── src/
│       ├── api/             axios 接口封装（全接口）
│       ├── store/           Pinia 认证状态
│       ├── router/          路由与登录/角色守卫
│       ├── layout/          侧边导航布局（按角色渲染菜单）
│       └── views/           页面组件（志愿者端 5 / 组织者端 5 / 管理端 5 + 登录）
├── sql/init.sql             建库建表 + 初始数据
├── deploy/                  演示环境数据目录（Docker 卷，自动生成）
└── docker-compose.yml       备选：用容器跑 MySQL + Redis（不装本机版时使用）
```

## 快速启动与访问

> 本机环境已由安装流程自动完成：MySQL 8.0.29（D:\dev\mysql）、Redis（D:\dev\redis）、Nginx 1.28（D:\dev\nginx）均已运行并配置开机自启（启动文件夹），数据库 `volunteer` 及 11 张表已初始化，`application.yml` 已配置好密码。

1. **启动后端**：双击 `D:\dev\scripts\start-backend.cmd`（自动设置 JAVA_HOME），或 `cd backend && mvn spring-boot:run`
2. **访问生产入口**：浏览器打开 **http://localhost**（Nginx 托管前端静态产物 + 反向代理 /api→8080）
3. **开发模式**（改前端代码时）：`cd frontend && npm run dev`，访问 `http://localhost:5173`（Vite 代理 /api→8080）
4. **接口文档**：后端启动后访问 `http://localhost/swagger-ui/index.html`
5. **部署与验收报告**：见 `docs/DeployAndAcceptance.md`

> 若用 Docker 跑 MySQL/Redis（不装本机版）：`docker compose up -d` 后，数据库 root 密码为 `volunteer123`（仅开发用）。

## 开发进度

- [x] 项目骨架、数据库 DDL、Docker Compose
- [x] 认证授权（JWT + RBAC + 登录/刷新）
- [x] 活动与报名模块
- [x] 签到签退与工时核算模块
- [x] 档案证明、统计可视化、系统管理模块
- [x] 前端三端页面与统计看板（Vue 3 + Element Plus + ECharts）
- [x] 部署联调（Nginx 生产入口 http://localhost）、并发压测与验收文档
- [x] 后续能力补齐：活动封面上传、证明 PDF 生成与下载、扫码签到（验收报告"已知限制"第 2/3/4 条）

## 角色

- **志愿者**：浏览报名、签到签退、查看档案、申请服务证明
- **组织者**：发布活动、审核报名、管理签到与工时
- **管理员**：用户管理、活动类型、公告发布、全局统计

**默认演示账号**（后端启动时自动创建）：管理员 `admin / admin123`；组织者 `organizer / organizer123`。

**演示数据**（答辩演示用，见 `sql/demo_data.sql`，可重复执行）：10 名志愿者（密码统一 `vol123456`，如 `linxiaoyu` 林晓雨、`chenhao` 陈昊、`wangjiaqi` 王佳琪 等）、12 个真实场景活动（图书馆整理/敬老探访/乡村支教/迎新/运动会等，状态覆盖 DRAFT→ARCHIVED 全生命周期）、80 条报名、60 条签到与 167.92 小时已确认工时、3 张周期证明、3 条公告、20 条审计日志——统计看板、档案星级与证明签发均有完整数据可直接演示。

```bat
mysql -uroot -pvolunteer123 --default-character-set=utf8mb4 -e "source D:/VolunteerPlatform/sql/demo_data.sql"
```
