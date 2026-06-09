<div align="center">

# 🔍 Code Review Bot

**AI 驱动的自动化代码审查工具**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Vue 3](https://img.shields.io/badge/Vue-3.5-4fc08d?logo=vue.js)](https://vuejs.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6db33f?logo=springboot)](https://spring.io/projects/spring-boot)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6-3178c6?logo=typescript)](https://www.typescriptlang.org/)
[![Java](https://img.shields.io/badge/Java-17-ed8b00?logo=openjdk)](https://openjdk.org/)
[![DeepSeek](https://img.shields.io/badge/AI-DeepSeek-6366f1)](https://platform.deepseek.com/)

*粘贴代码片段或 Git Diff，AI 实时分析并以流式方式逐一输出审查结果*

</div>

---

## ✨ 功能特性

| 特性 | 说明 |
|------|------|
| 🤖 **AI 智能审查** | 基于 DeepSeek 大模型，覆盖 Bug、安全、性能、代码规范四大维度 |
| 📡 **SSE 流式输出** | 服务端推送（Server-Sent Events）实时传输，逐条渲染审查结果 |
| 🔀 **双模式输入** | 支持粘贴代码片段和 Git Diff 两种输入方式 |
| 📊 **问题分级** | 按严重程度分为 **严重**（红色）、**建议**（黄色）、**优化**（蓝色） |
| 💡 **可执行建议** | 每个问题附有描述、修改建议和代码修复示例 |
| 📝 **审查历史** | 所有审查记录自动保存至浏览器本地，支持查看和删除 |
| 🎨 **现代 UI** | Vue 3 + Element Plus 构建，响应式布局，深色代码编辑器 |
| 🔒 **隐私优先** | API Key 通过环境变量配置，绝不硬编码或上传 |

## 📸 项目截图

### 代码审查 — 输入页面

![代码审查输入](screenshots/review-input.png)

*左侧粘贴代码片段或 Git Diff，支持两种输入模式切换*

### 代码审查 — 结果展示

![审查结果](screenshots/review-results.png)

*右侧面板实时流式展示审查结果，按严重程度分组排列*

### 问题详情 — 展开卡片

![问题详情展开](screenshots/review-results-expanded.png)

*点击问题卡片展开详情，查看问题描述、修改建议和代码修复示例*

### Git Diff 审查模式

![Git Diff 输入](screenshots/review-diff-input.png)

*切换到「粘贴 Git Diff」模式，直接粘贴 `git diff` 输出进行审查*

### 审查历史记录

![历史记录](screenshots/history-with-data.png)

*所有审查记录保存在本地，支持查看详情和删除操作*

### 审查详情回看

![审查详情](screenshots/review-detail.png)

*点击历史记录中的「查看」按钮，回顾完整的审查结果*

---

## 🛠️ 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue 3 (Composition API) | 3.5 |
| 类型系统 | TypeScript | 5.6 |
| 构建工具 | Vite | 6.x |
| UI 组件库 | Element Plus | 2.9 |
| 状态管理 | Pinia | 2.2 |
| 路由 | Vue Router | 4.4 |
| 代码高亮 | highlight.js | 11.x |
| 后端框架 | Spring Boot | 3.5 |
| 运行环境 | Java | 17 |
| 构建工具 | Maven | 3.8+ |
| AI 模型 | DeepSeek API | OpenAI 兼容 |
| 前端测试 | Vitest + @vue/test-utils | 3.x |
| 后端测试 | JUnit 5 | - |

## 🏗️ 项目结构

```
code-review-bot/
├── code-review-bot-fronted/          # Vue 3 + TypeScript 前端
│   ├── src/
│   │   ├── api/                      # API 层（fetch + SSE 流式客户端）
│   │   ├── components/               # Vue 组件
│   │   │   ├── CodeInput.vue         # 代码输入面板（模式切换 + 编辑器）
│   │   │   ├── DiffViewer.vue        # Git Diff 可视化渲染器
│   │   │   ├── IssueCard.vue         # 可展开的问题卡片
│   │   │   ├── ReviewReport.vue      # 审查报告（分组 + 统计）
│   │   │   └── CodeHighlight.vue     # 语法高亮展示
│   │   ├── layouts/                  # MainLayout（顶栏 + 导航）
│   │   ├── router/                   # Vue Router 4 路由配置
│   │   ├── stores/                   # Pinia 状态管理（审查 + 历史持久化）
│   │   ├── types/                    # TypeScript 类型定义
│   │   ├── utils/                    # SSE 解析、格式化工具
│   │   └── views/                    # 页面组件
│   │       ├── ReviewView.vue        # 审查主页面（双栏布局）
│   │       ├── HistoryView.vue       # 历史记录列表
│   │       └── ReviewDetail.vue      # 审查详情回看
│   └── vite.config.ts                # Vite 配置（含 /api 反向代理）
├── code-review-bot-backend/          # Spring Boot 3 后端
│   ├── src/main/java/com/codereviewbot/
│   │   ├── config/                   # CORS 跨域配置
│   │   ├── controller/               # POST /api/review（SSE 端点）
│   │   ├── dto/                      # ReviewRequest、ReviewIssue 数据传输对象
│   │   └── service/impl/             # ReviewServiceImpl（DeepSeek 集成）
│   └── src/main/resources/           # application.yml、application-dev.yml
├── screenshots/                      # 项目截图
├── .github/                          # GitHub Actions CI/CD
└── .gitignore
```

## 📐 系统架构

```
┌─────────────────┐     POST /api/review     ┌─────────────────┐     POST /chat/completions    ┌─────────────────┐
│                 │ ──── SSE ReadableStream ▶ │                 │ ──── stream: true ──────────▶ │                 │
│  Vue 3 前端     │                           │  Spring Boot 3  │                                │  DeepSeek API   │
│  (Vite 代理)    │ ◀── data: {...}\n\n ──── │  (SseEmitter)   │ ◀── data: {...}\n\n ──────── │  (Chat Model)   │
│                 │                           │                 │                                │                 │
└─────────────────┘                           └─────────────────┘                                └─────────────────┘
  localhost:3000                                 localhost:8080                                   api.deepseek.com
```

**数据流程：**

1. **前端** → 用户粘贴代码/Diff，通过 `fetch` 发起 POST 请求（`Content-Type: application/json`）
2. **后端** → 接收代码，组装结构化审查提示词（含四个审查维度），向 DeepSeek API 发起流式请求
3. **DeepSeek** → 通过 SSE 流式返回 JSON 格式的审查条目（`data: {...}\n\n`）
4. **后端转发** → 实时提取每个已完成的 JSON 对象，通过 Spring `SseEmitter` 透传给前端
5. **前端渲染** → SSE `ReadableStream` 逐条解析，每收到一个问题立即渲染为卡片
6. **本地持久化** → 审查完成后自动保存至 `localStorage`，可通过「历史记录」查看

## 🚀 快速启动

### 环境要求

- **Java 17+** 和 Maven 3.8+
- **Node.js 18+** 和 npm
- **DeepSeek API Key** — [点击获取](https://platform.deepseek.com/api_keys)

### 1. 克隆仓库

```bash
git clone https://github.com/spider-freedom/code-review-bot.git
cd code-review-bot
```

### 2. 启动后端

```bash
cd code-review-bot-backend

# 配置 API Key（推荐通过环境变量）
# Windows PowerShell:
$env:DEEPSEEK_API_KEY="sk-xxxxxxxxxxxxxxxx"
# macOS / Linux:
export DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxx

# 启动 Spring Boot
mvn spring-boot:run
```

后端运行在 **http://localhost:8080**`GET /actuator/health` 可检查服务状态。

> ⚠️ **安全提示：** 请通过环境变量 `DEEPSEEK_API_KEY` 配置密钥，不要将 API Key 硬编码在 `application-dev.yml` 中或提交到 Git。

### 3. 启动前端

```bash
cd code-review-bot-fronted

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端运行在 **http://localhost:3000**，Vite 自动将 `/api/*` 请求代理到 `localhost:8080`。

### 4. 开始审查

1. 打开浏览器访问 http://localhost:3000
2. 选择「粘贴代码片段」或「粘贴 Git Diff」模式
3. 将代码粘贴到编辑器，点击「开始审查」
4. 右侧面板实时流式展示审查结果，问题按严重程度分组
5. 审查完成后可在「历史记录」页签查看和回顾

## 🤖 AI 审查维度

审查提示词引导 DeepSeek 大模型从以下四个维度分析代码：

| 维度 | 严重级别 | 关注点 |
|------|----------|--------|
| 🔴 **潜在 Bug** | `error` | 空指针、未处理异常、资源泄漏、边界条件、逻辑错误、并发安全 |
| 🔴 **安全漏洞** | `error` | SQL/XSS 注入、敏感信息泄露、权限校验缺失、会话管理缺陷 |
| 🟡 **性能问题** | `warning` | 冗余循环、N+1 查询、资源未释放、低效算法、内存泄漏 |
| 🔵 **代码规范** | `info` | 命名不当、硬编码常量、重复代码、函数过长、缺少类型 |

## 📋 可用命令

### 前端

| 命令 | 说明 |
|------|------|
| `npm run dev` | 启动 Vite 开发服务器（HMR 热更新） |
| `npm run build` | TypeScript 类型检查 + Vite 生产构建 |
| `npm run preview` | 本地预览生产构建结果 |
| `npm test` | 运行 Vitest 单元测试（单次） |
| `npm run test:watch` | 监听模式运行测试 |

### 后端

| 命令 | 说明 |
|------|------|
| `mvn spring-boot:run` | 启动 Spring Boot（开发模式） |
| `mvn clean compile` | 编译 Java 源码 |
| `mvn test` | 运行 JUnit 5 测试 |
| `mvn clean package -DskipTests` | 打包可执行 JAR |

## 🧪 测试

- **前端：** 30 个测试用例，覆盖 Pinia store、SSE 流解析、DiffViewer 组件渲染
- **后端：** JUnit 5 集成测试框架（`spring-boot-starter-test`）

```bash
# 前端测试
cd code-review-bot-fronted && npm test

# 后端测试
cd code-review-bot-backend && mvn test
```

## 🚢 生产部署

### 构建

```bash
# 前端构建
cd code-review-bot-fronted && npm run build

# 后端构建
cd code-review-bot-backend && mvn clean package -DskipTests

# 运行
java -Dspring.profiles.active=prod \
     -DDEEPSEEK_API_KEY=sk-xxxxxxxx \
     -jar code-review-bot-backend/target/code-review-bot-1.0.0.jar
```

### Nginx 反向代理

前端使用 `createWebHistory()` 路由模式，需配置 SPA fallback：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    root /var/www/code-review-bot/dist;
    index index.html;

    # SPA fallback
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Connection '';
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 3600s;  # SSE 长连接
    }
}
```

### 生产环境检查清单

- [ ] 将 `application.yml` 的 Spring profile 改为 `prod`
- [ ] 通过环境变量 `DEEPSEEK_API_KEY` 配置 API Key
- [ ] 在 `application-prod.yml` 中配置具体的 `allowedOrigins` 替代 CORS 通配符
- [ ] 配置反向代理（Nginx/Caddy）提供 HTTPS
- [ ] 验证健康检查端点：`GET /actuator/health`

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建功能分支：`git checkout -b feat/amazing-feature`
3. 提交代码：`git commit -m 'feat: add amazing feature'`
4. 推送分支：`git push origin feat/amazing-feature`
5. 提交 Pull Request

### 本地开发环境

建议使用 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/) 规范编写提交信息：

- `feat:` 新功能
- `fix:` 修复 Bug
- `docs:` 文档更新
- `style:` 代码格式（不影响功能）
- `refactor:` 重构
- `test:` 测试相关
- `chore:` 构建/工具变动

## 📄 License

MIT © 2024 [spider-freedom](https://github.com/spider-freedom)

---

<div align="center">
  <sub>Built with ❤️ using Vue 3, Spring Boot, and DeepSeek AI</sub>
</div>
