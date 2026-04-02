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

## 6. 下一步建议（可选）

- 将 `Map<String,Object>` 升级为强类型 DTO + 参数校验
- 引入 JWT 鉴权与多用户登录
- 热力图由 bitmap(0/1) 扩展为 bitmap + hash 计数分层
- 增加接口测试（Spring Boot Test + Testcontainers Redis）
