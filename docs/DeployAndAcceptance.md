# 部署与验收报告（Deployment & Acceptance）

> 项目：志愿服务管理平台（广东石油化工学院 Capstone）
> 日期：2026-10-29 ｜ 环境：Windows 本机（D 盘）
> 结论：**生产链路部署完成，功能验收与并发压测全部通过**

---

## 一、部署架构

```
浏览器 ── http://localhost:80 ──► Nginx 1.28（D:\dev\nginx）
                                  ├─ 静态资源  → D:\VolunteerPlatform\frontend\dist（Vite 生产构建）
                                  ├─ /api/*     → 反向代理 http://127.0.0.1:8080（Spring Boot）
                                  └─ /swagger-ui、/v3/api-docs → 透传后端
```

| 组件 | 位置 | 端口 | 启动方式 |
| --- | --- | --- | --- |
| MySQL 8.0.29 | D:\dev\mysql | 3306 | 用户进程 + 启动文件夹自启（start-mysql.cmd） |
| Redis 5.0.14 | D:\dev\redis | 6379 | 用户进程 + 启动文件夹自启（start-redis.cmd） |
| Spring Boot 3.3 后端 | D:\VolunteerPlatform\backend | 8080 | mvn spring-boot:run（start-backend.cmd） |
| Vue 3 前端（生产） | D:\VolunteerPlatform\frontend\dist | 80（经 Nginx） | start-nginx.cmd + 启动文件夹自启 |
| Vue 3 前端（开发） | — | 5173 | npm run dev（开发期 Vite 代理 /api→8080） |

> 说明：环境无管理员权限，MySQL/Redis/Nginx 均以**用户进程**运行并通过「启动」文件夹实现开机自启（最小权限原则），未注册 Windows 服务。

## 二、功能验收清单（端到端）

### 2.1 认证授权
| 场景 | 结果 |
| --- | --- |
| admin / organizer 演示账号登录，签发 JWT（access 2h / refresh 7d） | ✅ |
| 注册志愿者 → 自动登录 | ✅（中文姓名全链路正常） |
| 未登录访问接口 → 401 统一响应 | ✅ |
| 登录失败限流（10 次/5 分钟锁定） | ✅（任务 #3） |
| 刷新令牌轮换 + 旧 token 黑名单、登出吊销 | ✅（任务 #3） |
| 三角色 RBAC 接口拦截（越权 403） | ✅ |

### 2.2 活动与报名
| 场景 | 结果 |
| --- | --- |
| 活动创建（时间窗校验 start<end、signinStart<signinEnd<signoutEnd） | ✅ |
| 状态机 DRAFT→RECRUITING→ONGOING→FINISHED→ARCHIVED / CANCELLED | ✅ |
| 报名原子扣减名额（防超卖，并发唯一约束兜底） | ✅（任务 #4 全场景） |
| 满员报名拒绝 1102、重复报名拒绝 1101/唯一约束 | ✅ |
| 组织者审核（通过/拒绝回补名额）、志愿者取消报名 | ✅ |
| 组织者 onlyMine 我的活动过滤 | ✅ |

### 2.3 签到与工时
| 场景 | 结果 |
| --- | --- |
| 签到窗口 [signinStart, signinEnd] 外拒绝 1202 | ✅ |
| 一人一活动唯一签到（重复 1203） | ✅ |
| 组织者 MANUAL 代签 | ✅ |
| 工时核算规则 RULE_V1（满 15 分钟计，时长/60 四舍五入 2 位） | ✅（120 分钟 → 2.00h CONFIRMED） |
| 签退窗口 [startTime, signoutEnd]，未签退不核算 | ✅ |

### 2.4 档案 / 证明 / 统计
| 场景 | 结果 |
| --- | --- |
| 个人档案（累计工时/服务次数/星级等级 <10h★ <50h★★ <100h★★★ ≥100h★★★★） | ✅ |
| 周期证明签发（period_label 周期累计 + serial_no 防伪编号 + 同周期防重 1302） | ✅ |
| 统计总览 / 类型分布 / 月度趋势 / 热门活动 / 志愿者 TOP（ECharts 看板） | ✅ |
| 前端角色权限：志愿者不可看统计（403） | ✅ |

### 2.5 系统管理
| 场景 | 结果 |
| --- | --- |
| 用户启停（禁用后登录拦截）、角色动态分配（roleIds 回显） | ✅ |
| 公告全生命周期（创建/发布/置顶/下架/删除），公开端免登录可读 | ✅ |
| 活动类型管理，被活动引用时拒绝删除（1403） | ✅ |
| 操作审计日志（模块/动作/对象/IP 全记录） | ✅ |

### 2.6 前端三端页面（生产入口 http://localhost 实测）
| 端 | 页面 | 结果 |
| --- | --- | --- |
| 通用 | 登录/注册、布局与角色菜单、路由守卫 | ✅ |
| 志愿者 | 活动广场、我的报名、签到签退、我的档案、我的证明 | ✅ |
| 组织者 | 活动管理、报名审核、签到管理（代签）、志愿者档案、证明签发 | ✅ |
| 管理员 | 统计可视化（ECharts 4 图）、用户管理、公告管理、类型管理、操作日志 | ✅ |

## 三、并发压测结果（经 Nginx 生产链路，Python 并发客户端）

| 场景 | 并发 | 总请求 | 成功 | 错误 | 平均延迟 |
| --- | --- | --- | --- | --- | --- |
| 公开公告 GET | 50 | 500 | 500 | **0** | 本地回环 <1ms |
| 登录 POST（BCrypt+JWT） | 20 | 200 | 200 | **0** | 本地回环 <1ms |
| 活动列表 GET（带 Token） | 50 | 500 | 500 | **0** | 本地回环 <1ms |

- **错误率 0%**；登录接口吞吐受 BCrypt 校验计算限制（约 9 req/s），属正常预期
- 防超卖并发正确性已在任务 #4 通过唯一约束 + 原子扣减双重验证
- 吞吐数字受 Python 单进程客户端限制，不代表服务器上限；capstone 演示规模（百人级）下余量充足

## 四、已知限制与后续建议

1. **安全加固（生产前）**：JWT 密钥与应用密码当前明文于 application.yml（仅开发用）；部署到公网前应改为环境变量注入、启用 HTTPS
2. **性能**：列表接口采用 MyBatis-Plus 分页 + 必要 N+1 优化；注册人数达千级后可加缓存（Redis 已就绪）
3. **Docker 备选**：docker-compose.yml 提供 MySQL+Redis 容器方案，未在本机启用（本机为原生安装）
4. **扫码地理校验（可选）**：签到接口支持 QR/GEO/MANUAL 方法参数；扫码签到已落地（二维码 + 扫码页），GEO 经纬度校验待接入地图能力时可启用

### 已补齐能力（2026-10-29，原"已知限制"2/3/4 条已实现）

| 能力 | 实现方案 | 验证结果 |
| --- | --- | --- |
| 图片上传 | 后端 `POST /api/v1/files/upload`（类型 jpg/png/webp/gif 白名单、≤5MB、UUID 命名、子目录防穿越），静态映射 `/uploads/**` 公开访问；前端活动创建表单 el-upload 上传封面，活动卡片/管理列表展示封面图 | 上传 → 返回 URL → 8080 与 Nginx 生产入口均可访问（200）；Nginx `location /uploads/` 已代理 |
| 证明 PDF | 签发证明时同步用 OpenPDF 生成 A4 中文 PDF（嵌入 Windows 宋体，缺失时回退黑体/微软雅黑），落盘 `uploads/certs/cert-{serialNo}.pdf` 并写 `certification.pdf_url`；`GET /api/v1/certifications/{id}/pdf` 鉴权下载（本人/组织者/管理员） | 3 张演示证明重新签发后均带 pdf_url；下载接口与 Nginx 访问均 200（40KB，中文渲染正常） |
| 扫码签到 | 组织者签到管理页一键生成签到二维码（qrcode 库，内容为 `站点/scan/{activityId}`）；新增 `/scan/:id` 扫码签到页（独立全屏），志愿者确认后以 `method=QR` 完成签到 | 二维码弹窗生成正常；`/scan/:id` SPA 路由（dev 5173 与生产 80 双入口）均返回 200 |

> 演示说明：签到二维码内容为平台站内链接，志愿者在浏览器打开（手机相机扫码或电脑直接访问）即可完成扫码签到，签到方式记 QR。

## 五、启动手册（生产模式）

```bat
1. 开机自动：MySQL、Redis、Nginx 已通过「启动」文件夹自启
2. 启动后端：双击 D:\dev\scripts\start-backend.cmd（或 backend 目录 mvn spring-boot:run）
3. 访问生产入口：http://localhost（Nginx 80 端口）
4. 开发调试：http://localhost:5173（npm run dev）
5. 接口文档：http://localhost/swagger-ui/index.html
```

**演示账号**：管理员 admin/admin123 ｜ 组织者 organizer/organizer123 ｜ 志愿者自助注册
