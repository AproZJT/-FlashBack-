# FlashBack（闪回）

一个面向计算机/理工科复习场景的 UniApp + Spring Boot + Redis 项目。

- 前端：Vue + UniApp（移动端交互）
- 后端：Spring Boot 3
- 存储：Redis（Hash/Set/ZSet/Bitmap 思路）

## 1. 技术架构

### 1.1 核心目标

- 毫秒级响应：基于 Redis 内存模型承载高频复习交互
- 智能调度：基于遗忘曲线反馈（forget/blur/master）动态计算复习时间
- 极客内容：Markdown + 代码块渲染（前端支持轻量高亮样式）
- 学习追踪：Bitmap 驱动 GitHub 风格热力图
- 跨端体验：UniApp 统一微信小程序/移动端形态

### 1.2 Redis Key 设计（当前实现）

- `fb:user:{userId}`：用户信息（JSON）
- `fb:user:decks:{userId}`：用户卡片集集合（Set）
- `fb:deck:{deckId}`：卡片集信息（JSON）
- `fb:deck:cards:{deckId}`：卡片 ID 集合（Set）
- `fb:card:{cardId}`：卡片信息（JSON）
- `fb:review:zset:{userId}`：复习调度（ZSet，score = next_review_time）
- `fb:study:bitmap:{userId}:{year}`：学习打卡位图（Bitmap）

## 2. 功能覆盖

- 遗忘曲线调度（forget +12h / blur +1d / master 按等级 1/2/4/7d）
- 首页待复习统计（基于 `next_review_time <= now`）
- Markdown + 代码块渲染
- 学习热力图（最近 120 天）
- 卡片集市（公开卡组浏览 + 一键克隆）
- 卡组发布开关、卡片 CRUD、连续复习

## 3. 启动说明

## 3.1 先启动 Redis

默认配置：

- host: `localhost`
- port: `6379`
- db: `0`

## 3.2 启动后端

在 `backend` 目录执行：

```bash
mvn spring-boot:run
```

默认监听：`http://127.0.0.1:8080`

## 3.3 启动前端

按你当前 UniApp 项目方式运行。前端接口基址已配置为：

- `http://127.0.0.1:8080/api`
- 默认 userId：`local_user_001`

## 4. 接口清单（REST API）

统一返回结构：

```json
{
  "ok": true,
  "message": "",
  "data": {}
}
```

### 4.1 用户

- `GET /api/profile?userId=local_user_001`
  - 获取用户信息（首次自动初始化）
- `PUT /api/profile?userId=local_user_001`
  - 更新用户信息
  - body: `{ "nickname": "小张", "goal": "每天 30 分钟" }`

### 4.2 卡片集（Deck）

- `GET /api/decks?userId=local_user_001`
  - 获取我的卡片集（含 cards）
- `POST /api/decks?userId=local_user_001`
  - 新建卡片集
  - body: `{ "name": "操作系统" }`
- `GET /api/decks/{deckId}?userId=local_user_001`
  - 获取单个卡片集详情
- `PUT /api/decks/{deckId}/rename?userId=local_user_001`
  - 重命名卡片集
  - body: `{ "name": "新名称" }`
- `PUT /api/decks/{deckId}/public?userId=local_user_001`
  - 发布/取消发布到集市
  - body: `{ "value": true }`

### 4.3 卡片（Card）

- `POST /api/decks/{deckId}/cards?userId=local_user_001`
  - 新增卡片
  - body: `{ "front": "问题", "back": "答案 markdown" }`
- `PUT /api/decks/{deckId}/cards/{cardId}?userId=local_user_001`
  - 编辑卡片
  - body: `{ "front": "新问题", "back": "新答案" }`
- `DELETE /api/decks/{deckId}/cards/{cardId}?userId=local_user_001`
  - 删除卡片
- `POST /api/decks/{deckId}/cards/{cardId}/review?userId=local_user_001`
  - 提交复习反馈
  - body: `{ "feedback": "forget" | "blur" | "master" }`

### 4.4 复习调度

- `GET /api/review/due?userId=local_user_001`
  - 获取待复习卡片（ZSet score <= now）

### 4.5 集市

- `GET /api/market/decks?userId=local_user_001`
  - 获取公开卡片集
- `POST /api/market/decks/{deckId}/clone?userId=local_user_001`
  - 一键克隆公开卡片集到我的库

### 4.6 学习热力图

- `GET /api/study/heatmap?userId=local_user_001&days=120`
  - 获取最近 N 天学习位图映射结果

## 5. 已内置演示数据（开箱可看效果）

首次访问用户时自动注入：

- 卡片集：
  - `操作系统-进程状态`（公开）
  - `算法基础-数组与哈希`
  - `前端工程化-构建与部署`（公开）
- 卡片内容：含标题、加粗、行内代码、代码块（便于验证 Markdown 渲染）
- 复习调度：
  - 已构造“部分到期、部分未到期”卡片，可直接看到首页待复习数字变化
- 热力图：
  - 最近 45 天注入稀疏学习记录，能直观看到深浅分布
- 集市：
  - 至少两个公开卡组，可立即执行“一键保存到我的库”

## 6. Sprint 0 基线兼容说明（v1 可并行）

### 6.1 卡片字段兼容（向后兼容）

当前卡片在 v1 字段基础上新增可扩展字段（均为可选，旧前端不传也可运行）：

- `version`：乐观锁版本号（默认 `0`）
- `difficulty`：难度（默认 `0.3`）
- `stability`：稳定度（默认 `0.0`）
- `retrievability`：可提取率（默认 `0.0`）
- `media`：富媒体扩展对象（默认 `{}`）

### 6.2 v1 接口兼容策略

- 所有现有 `/api/**` 路由保持不变
- 默认 Feature Flag 关闭时，复习调度仍使用 v1 逻辑
- 仅当 `flashback.feature.enable-v2-schedule=true` 时，启用扩展调度字段参与计算

### 6.3 埋点（可回滚）

当 `flashback.feature.enable-metrics=true` 时，记录：

- `request_latency`：接口响应时延（path / method / status / cost_ms）
- `review_metrics`：复习事件埋点（每日复习卡片数、完成率）

### 6.4 回滚策略（Feature Flag）

配置位于 `application.yml`：

- `flashback.feature.enable-v2-schedule`
- `flashback.feature.enable-offline-sync`
- `flashback.feature.enable-media-fields`
- `flashback.feature.enable-metrics`

出现回归时可快速关闭对应开关实现回滚。

## 7. 下一步建议（可选）

- 将 `Map<String,Object>` 进一步升级为强类型响应 DTO
- 引入统一监控上报（Prometheus + Grafana）
- 完成离线队列同步与冲突可视化
- 增加接口测试（Spring Boot Test + Testcontainers Redis）
