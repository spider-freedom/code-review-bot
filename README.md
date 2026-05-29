# Code Review Bot

AI-driven automated code review tool. Paste code snippets or Git diffs, and the AI analyzes your code in real time, streaming issues as it finds them.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Vue 3, TypeScript, Vite 6, Element Plus, Pinia, Vue Router 4 |
| Backend | Spring Boot 3.5, Java 17, Maven |
| AI | DeepSeek API (OpenAI-compatible chat completions, streaming) |
| Testing | Vitest, @vue/test-utils, JUnit 5 |

## Project Structure

```
code-review-bot/
├── code-review-bot-fronted/    # Vue 3 + TypeScript frontend
│   ├── src/
│   │   ├── api/                # API layer (SSE streaming client)
│   │   ├── components/         # CodeInput, DiffViewer, IssueCard, ReviewReport, etc.
│   │   ├── layouts/            # MainLayout (header + nav)
│   │   ├── router/             # Vue Router 4 routes
│   │   ├── stores/             # Pinia store (review state, history persistence)
│   │   ├── types/              # TypeScript type definitions
│   │   ├── utils/              # SSE stream utility
│   │   └── views/              # ReviewView, HistoryView, ReviewDetail
│   └── vite.config.ts          # Vite config with /api proxy
├── code-review-bot-backend/    # Spring Boot 3 backend
│   ├── src/main/java/com/codereviewbot/
│   │   ├── config/             # CORS configuration
│   │   ├── controller/         # POST /api/review (SSE endpoint)
│   │   ├── dto/                # ReviewRequest, ReviewIssue
│   │   └── service/impl/       # ReviewServiceImpl (DeepSeek integration)
│   └── src/main/resources/     # application.yml, application-dev.yml
└── .gitignore
```

## Architecture

```
Browser (Vue 3)                    Spring Boot 3                    DeepSeek API
┌──────────────┐    POST /api/review    ┌──────────────┐    POST /chat/completions   ┌──────────────┐
│  fetch +     │ ──── SSE stream ────▶  │  SseEmitter   │ ──── stream: true ───────▶ │  DeepSeek    │
│  ReadableStream │ ◀── data: {...}\n\n │  (streaming)  │ ◀── data: {...}\n\n       │  LLM         │
└──────────────┘                       └──────────────┘                             └──────────────┘
```

1. Frontend sends a POST request with `{ code, mode }` (mode: `"code"` or `"diff"`)
2. Backend forwards the code to DeepSeek API with a structured review prompt, requesting JSON output
3. DeepSeek streams chunks back via SSE; backend extracts and forwards individual issue JSON objects
4. Frontend renders issues in real time as they arrive

## Setup

### Prerequisites

- **Java 17+** and Maven 3.8+
- **Node.js 18+** and npm
- **DeepSeek API key** ([platform.deepseek.com](https://platform.deepseek.com))

### Backend

```bash
cd code-review-bot-backend

# Set your API key (or add to application-dev.yml)
export DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxx

# Run the Spring Boot application
mvn spring-boot:run
```

The backend starts on **http://localhost:8080**. The endpoint `POST /api/review` returns an SSE stream.

**Important:** Set `DEEPSEEK_API_KEY` as an environment variable. Never hardcode the key in application-dev.yml.

### Frontend

```bash
cd code-review-bot-fronted

# Install dependencies
npm install

# Start dev server
npm run dev
```

The frontend starts on **http://localhost:3000**. Vite proxies `/api/*` to `localhost:8080`.

### API Key Configuration

The backend reads the API key in this order:
1. Environment variable: `DEEPSEEK_API_KEY`
2. Default value in `application-dev.yml` (placeholder: `your-api-key-here`)

## Usage

1. Open http://localhost:3000
2. Choose **Code Snippet** or **Git Diff** mode via the tabs
3. Paste your code into the editor
4. Click **开始审查** (Start Review)
5. Issues stream in real time on the right panel, grouped by severity:
   - **error** (red) — bugs, security vulnerabilities
   - **warning** (yellow) — potential issues, best practice violations
   - **info** (blue) — optimization suggestions
6. Each issue card includes a description, suggestion, and code fix example
7. Review history is saved locally and accessible via the **历史记录** tab

## What the AI Checks

The review prompt instructs the model to analyze:

- **Bug risks** — null pointers, unhandled exceptions, edge cases, logic errors
- **Security** — injection risks, sensitive data exposure, missing validation
- **Performance** — unnecessary loops, unreleased resources, inefficient algorithms
- **Code quality** — naming conventions, hardcoded values, duplication, complexity

## Scripts

### Frontend

| Command | Description |
|---------|-------------|
| `npm run dev` | Start Vite dev server |
| `npm run build` | Type-check and production build |
| `npm run preview` | Preview production build |
| `npm test` | Run Vitest unit tests |
| `npm run test:watch` | Run tests in watch mode |

### Backend

| Command | Description |
|---------|-------------|
| `mvn spring-boot:run` | Start Spring Boot |
| `mvn clean compile` | Compile the project |
| `mvn test` | Run JUnit tests |

## Testing

- **Frontend:** 25 test cases across 3 test files (Pinia store, SSE stream parsing, DiffViewer rendering)
- **Backend:** JUnit 5 integration tests (Spring Boot Test starter included)

```bash
# Frontend tests
cd code-review-bot-fronted && npm test

# Backend tests
cd code-review-bot-backend && mvn test
```

## License

MIT
