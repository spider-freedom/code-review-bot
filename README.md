# Code Review Bot

AI 驱动的自动化代码审查工具。粘贴代码片段或 Git Diff，AI 会实时分析代码并以流式方式逐一输出问题。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3、TypeScript、Vite 6、Element Plus、Pinia、Vue Router 4 |
| 后端 | Spring Boot 3.5、Java 17、Maven |
| AI | DeepSeek API（OpenAI 兼容的流式聊天补全） |
| 测试 | Vitest、@vue/test-utils、JUnit 5 |

## 项目结构

```
code-review-bot/
├── code-review-bot-fronted/    # Vue 3 + TypeScript 前端
│   ├── src/
│   │   ├── api/                # API 层（SSE 流式客户端）
│   │   ├── components/         # CodeInput、DiffViewer、IssueCard、ReviewReport 等组件
│   │   ├── layouts/            # 主布局（顶栏 + 导航）
│   │   ├── router/             # Vue Router 4 路由
│   │   ├── stores/             # Pinia 状态管理（审查状态、历史记录持久化）
│   │   ├── types/              # TypeScript 类型定义
│   │   ├── utils/              # SSE 流式工具
│   │   └── views/              # ReviewView、HistoryView、ReviewDetail 页面
│   └── vite.config.ts          # Vite 配置（含 /api 代理）
├── code-review-bot-backend/    # Spring Boot 3 后端
│   ├── src/main/java/com/codereviewbot/
│   │   ├── config/             # CORS 跨域配置
│   │   ├── controller/         # POST /api/review（SSE 端点）
│   │   ├── dto/                # ReviewRequest、ReviewIssue 数据传输对象
│   │   └── service/impl/       # ReviewServiceImpl（DeepSeek 集成）
│   └── src/main/resources/     # application.yml、application-dev.yml
└── .gitignore
```

## 架构

```
浏览器 (Vue 3)                    Spring Boot 3                    DeepSeek API
┌──────────────┐    POST /api/review    ┌──────────────┐    POST /chat/completions   ┌──────────────┐
│  fetch +     │ ──── SSE 流 ────────▶  │  SseEmitter   │ ──── stream: true ───────▶ │  DeepSeek    │
│  ReadableStream │ ◀── data: {...}\n\n │  (流式转发)    │ ◀── data: {...}\n\n       │  大模型       │
└──────────────┘                       └──────────────┘                             └──────────────┘
```

1. 前端发送 POST 请求，携带 `{ code, mode }`（mode: `"code"` 或 `"diff"`）
2. 后端将代码连同结构化审查提示词一起转发给 DeepSeek API，要求返回 JSON 格式结果
3. DeepSeek 通过 SSE 流式返回数据块；后端实时提取每个已完成的 JSON 审查条目并转发给前端
4. 前端随收到随渲染，用户可实时看到审查结果

## 环境准备

- **Java 17+** 和 Maven 3.8+
- **Node.js 18+** 和 npm
- **DeepSeek API Key**（[platform.deepseek.com](https://platform.deepseek.com)）

## 后端启动

```bash
cd code-review-bot-backend

# 设置 API Key（也可配置在 application-dev.yml 中）
export DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxx

# 启动 Spring Boot
mvn spring-boot:run
```

后端运行在 **http://localhost:8080**，端点 `POST /api/review` 返回 SSE 流。

> 请通过环境变量 `DEEPSEEK_API_KEY` 配置密钥，不要硬编码在 `application-dev.yml` 中。

## 前端启动

```bash
cd code-review-bot-fronted

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端运行在 **http://localhost:3000**，Vite 自动将 `/api/*` 请求代理到 `localhost:8080`。

## API Key 配置优先级

1. 环境变量：`DEEPSEEK_API_KEY`
2. `application-dev.yml` 中的默认值（占位符：`your-api-key-here`）

## 使用说明

1. 打开 http://localhost:3000
2. 通过页签选择「代码片段」或「Git Diff」模式
3. 将代码粘贴到编辑器中
4. 点击「开始审查」
5. 右侧面板实时流式展示问题，按严重程度分组：
   - **error**（红色）— Bug、安全漏洞等严重缺陷
   - **warning**（黄色）— 潜在隐患或违反最佳实践
   - **info**（蓝色）— 优化建议，代码可正常运行但可改进
6. 每个问题卡片包含：问题描述、修改建议、修复代码示例
7. 审查历史保存在浏览器本地，可通过「历史记录」页签查看

## AI 审查维度

审查提示词引导模型从以下四个方面分析代码：

- **潜在 Bug** — 空指针、未处理异常、边界条件、逻辑错误
- **安全漏洞** — 注入风险、敏感信息泄露、权限校验缺失
- **性能问题** — 冗余循环、资源未释放、低效算法
- **代码规范** — 命名不当、硬编码、重复代码、过度复杂

## 可用命令

### 前端

| 命令 | 说明 |
|------|------|
| `npm run dev` | 启动 Vite 开发服务器 |
| `npm run build` | 类型检查 + 生产构建 |
| `npm run preview` | 预览生产构建结果 |
| `npm test` | 运行 Vitest 单元测试 |
| `npm run test:watch` | 监听模式运行测试 |

### 后端

| 命令 | 说明 |
|------|------|
| `mvn spring-boot:run` | 启动 Spring Boot |
| `mvn clean compile` | 编译项目 |
| `mvn test` | 运行 JUnit 测试 |

## 测试

- **前端：** 30 个测试用例，覆盖 3 个模块（Pinia store、SSE 流解析、DiffViewer 渲染）
- **后端：** JUnit 5 集成测试框架（spring-boot-starter-test 已引入）

```bash
# 前端测试
cd code-review-bot-fronted && npm test

# 后端测试
cd code-review-bot-backend && mvn test
```

## 生产部署

### 前端

前端使用 `createWebHistory()` 路由模式，生产环境需配置服务器将所有未知路径回退到 `index.html`。

**Nginx 示例：**

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

### 后端

生产环境必须修改以下配置：

1. **CORS：** 将 `application.yml` 的 profile 改为非 `dev`，或新增 `application-prod.yml` 配置具体的 `allowedOrigins`
2. **API Key：** 通过环境变量 `DEEPSEEK_API_KEY` 配置，部署时不要在配置文件中硬编码
3. **健康检查：** 访问 `/actuator/health` 可验证服务状态

```bash
# 构建
cd code-review-bot-fronted && npm run build
cd code-review-bot-backend && mvn clean package -DskipTests

# 运行后端
java -jar code-review-bot-backend/target/code-review-bot-1.0.0.jar
```

## License

MIT
