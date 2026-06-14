<div align="center">

# 🔍 代码分析服务平台

**后端驱动的 AI 代码审查服务 — 异步架构 · Redis 缓存 · 多租户 · 限流**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6db33f?logo=springboot)](https://spring.io/)
[![Java](https://img.shields.io/badge/Java-17-ed8b00?logo=openjdk)](https://openjdk.org/)
[![Vue 3](https://img.shields.io/badge/Vue-3.5-4fc08d?logo=vue.js)](https://vuejs.org/)
[![DeepSeek](https://img.shields.io/badge/AI-DeepSeek-6366f1)](https://platform.deepseek.com/)
[![Redis](https://img.shields.io/badge/Cache-Redis-dc382d?logo=redis)](https://redis.io/)
[![H2](https://img.shields.io/badge/DB-H2-0072b8)](https://www.h2database.com/)
[![Guava](https://img.shields.io/badge/RateLimit-Guava-4285f4?logo=google)](https://github.com/google/guava)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

---

## 🏛️ 后端架构亮点（面试重点）

| 特性 | 描述 | 技术实现 |
|------|------|----------|
| 🔄 **异步审查架构** | 提交后立即返回 taskId，线程池后台调 AI，结果持久化，前端轮询 | 线程池 + `review_task` 表 + `NoteParseTask` |
| ⚡ **结果缓存** | 相同代码 MD5 1h 内命中缓存，跳过 API 调用，减少约 80% 成本 | Redis + MD5 hash |
| 🛡️ **Redis 降级** | 缓存不可用时自动 fallback 到直接调 API，不阻塞业务 | try-catch 双路径 |
| 👥 **多租户隔离** | X-User-Id 请求头提取用户标识，SQL 层自动注入过滤条件 | `ApiKeyFilter` + WHERE user_id |
| 🚦 **限流保护** | 单用户每分钟 5 次审查，令牌桶算法，超限 HTTP 429 | `@RateLimit` + AOP + Guava |
| 📝 **统一日志** | AOP 记录每个请求的方法/URI/耗时，慢请求 >3s 告警 | `WebLogAspect` |
| 🛑 **全局异常处理** | 8 种异常统一捕获，结构化 JSON 响应 | `@RestControllerAdvice` + `ApiResponse` |
| ✅ **启动校验** | 启动时检测 `DEEPSEEK_API_KEY` 配置，缺失直接报错 | `@PostConstruct` |
| ⚙️ **工程规范** | HikariCP 连接池 + 优雅停机 + Actuator 健康检查 | H2 · `server.shutdown=graceful` |

---

## 📸 项目截图

### 代码审查 — 异步提交 + 实时流式双模式

![审查输入](screenshots/review-input.png)

*左侧粘贴代码片段或 Git Diff，顶部切换异步提交 / SSE 实时流式模式*

### 审查结果 — 流式渲染

![审查结果](screenshots/review-results.png)

*右侧面板实时流式展示审查结果，按严重程度（严重/建议/优化）分组*

### 问题详情 — 展开卡片

![问题详情](screenshots/review-results-expanded.png)

*点击卡片展开查看问题描述、修改建议和修复代码示例*

### Git Diff 审查模式

![Git Diff](screenshots/review-diff-input.png)

*切换至「粘贴 Git Diff」模式，直接粘贴 `git diff` 输出进行变更审查*

### 历史记录

![历史记录](screenshots/history-with-data.png)

*审查历史自动保存在本地，支持查看详情和删除*

### 详情回看

![审查详情](screenshots/review-detail.png)

*点击历史记录中的「查看」回顾完整审查结果*

---

## ✨ 功能特性

| 特性 | 说明 |
|------|------|
| 🤖 **AI 智能审查** | DeepSeek 大模型，覆盖 Bug、安全、性能、代码规范四大维度 |
| 🔄 **异步提交模式** | 提交后返回 taskId，每 2s 轮询状态，可离开页面 |
| 📡 **SSE 流式模式** | 实时逐条渲染审查结果，适合快速预览 |
| ⚡ **结果缓存** | 相同代码 1h 内直接返回缓存，零 API 消耗 |
| 🔀 **双模式输入** | 粘贴代码片段 / Git Diff 两种输入 |
| 📊 **三级问题分级** | error（红色）/ warning（黄色）/ info（蓝色）|
| 👥 **多租户隔离** | 不同用户的审查历史和任务互相隔离 |
| 📝 **审查历史** | 自动保存至本地，支持回看和删除 |
| 🔒 **隐私优先** | API Key 环境变量配置，不硬编码 |

---

## 🛠️ 技术栈

| 层级 | 技术 |
|------|------|
| **后端** | Spring Boot 3.5 · Java 17 · MyBatis-Plus 3.5.7 · Maven |
| **数据库** | H2（嵌入式）· `review_task` + `review_issue` 两张核心表 |
| **缓存** | Redis 7.x（Lettuce）· MD5 去重 key |
| **AI** | DeepSeek API（OpenAI 兼容接口）· 流式 + 非流式双模式 |
| **限流** | Guava RateLimiter · AOP 注解 · per-user 令牌桶 |
| **监控** | Actuator 健康检查 · AOP 统一日志 |
| **前端** | Vue 3 · TypeScript · Vite · Element Plus · Pinia |
| **测试** | JUnit 5 + Mockito（11 后端用例）· Vitest（30 前端用例） |

---

## 📐 系统架构

```
                    ┌── SSE 实时流式 (POST /api/review/stream) ──┐
                    │                                              ▼
┌─────────────────┐ │  ┌──────────────────┐    ┌─────────────────┐
│                 │ ┘  │  Spring Boot 3   │───▶│  DeepSeek API   │
│  Vue 3 前端     │    │  SseEmitter      │◀───│  (stream:true)  │
│  (Vite)         │    └──────────────────┘    └─────────────────┘
└─────────────────┘
                    ┌── 异步提交 (POST /api/review/submit) ──┐
                    │                                         ▼
┌─────────────────┐ │  ┌──────────┐  ┌──────────┐  ┌─────────────────┐
│                 │ │  │ H2 DB    │  │ 线程池    │  │  DeepSeek API   │
│  Vue 3 前端     │ ┘  │ task 表  │  │ (3 workers│─▶│  (stream:false) │
│  poll /2s       │───▶│ PENDING→ │  │ 异步处理) │◀─│                 │
│                 │◀───│ COMPLETED│  └──────────┘  └─────────────────┘
└─────────────────┘    └────┬─────┘
                            │
                     ┌──────┴──────┐
                     │  Redis 缓存  │
                     │  (MD5 去重)  │
                     │  1h TTL     │
                     └─────────────┘
```

---

## 🚀 快速启动

### 环境要求

- Java 17+ · Maven 3.8+ · Node.js 18+
- DeepSeek API Key — [获取](https://platform.deepseek.com/api_keys)
- Redis 7.x（可选 — 不配置则审查结果不缓存，功能正常）

### 步骤

```bash
# 1. 克隆
git clone https://github.com/spider-freedom/code-review-bot.git
cd code-review-bot

# 2. 后端
cd code-review-bot-backend
export DEEPSEEK_API_KEY=sk-xxxxxxxx
mvn spring-boot:run
# → http://localhost:8080 · GET /actuator/health

# 3. 前端（新终端）
cd code-review-bot-fronted
npm install && npm run dev
# → http://localhost:3000
```

---

## 🧪 测试

```bash
# 后端（11 用例 — ReviewAsyncService 核心逻辑）
cd code-review-bot-backend && mvn test

# 前端（30 用例 — Store / SSE 解析 / 组件渲染）
cd code-review-bot-fronted && npm test
```

---

## 🚢 部署

```bash
cd code-review-bot-fronted && npm run build
cd code-review-bot-backend && mvn clean package -DskipTests

java -Dspring.profiles.active=prod \
     -DDEEPSEEK_API_KEY=sk-xxxxxxxx \
     -jar target/code-review-bot-1.0.0.jar
```

> 生产部署需配置 Nginx 反向代理 + SPA fallback + SSE 长连接超时。详见 README 原部署章节。

---

## 📄 License

MIT © 2024 [spider-freedom](https://github.com/spider-freedom)

---

<div align="center">
  <sub>Built with Spring Boot, Vue 3, Redis, H2, and DeepSeek AI</sub>
</div>
