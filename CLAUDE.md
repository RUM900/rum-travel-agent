# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Quick Reference

```bash
# Build backend (skip tests)
./mvnw compile

# Run backend (Windows; adjust JAVA_HOME to your JDK 21 path)
JAVA_HOME="E:\\develop\\jdk" ./mvnw spring-boot:run

# Run a single test class
./mvnw test -Dtest=YuManusTest

# Run a single test method
./mvnw test -Dtest=YuManusTest#testMethodName

# Install frontend deps
cd yu-ai-agent-frontend && npm install

# Run frontend dev server (port 3000, proxies to backend localhost:8123)
cd yu-ai-agent-frontend && npm run dev

# Build frontend for production
cd yu-ai-agent-frontend && npm run build
```

## Project Structure

This is a **monorepo** containing three modules:

| Directory | Purpose |
|---|---|
| `rumagent/` | Main Spring Boot app — AI travel planner ("RUM") |
| `rumagent/rumagent-frontend/` | Vue 3 SPA frontend |
| `rumagent/yu-image-search-mcp-server/` | Standalone MCP server for image search |

## Backend Architecture

### Tech Stack
- **Java 21** · **Spring Boot 3.4.4** · **Spring AI 1.0**
- **AI**: DashScope (qwen-plus, primary) + Ollama (local, secondary)
- **Vector store**: PGVector (PostgreSQL + pgvector extension, cloud RDS)
- **MCP**: Spring AI MCP Client, connects to Amap (高德地图) MCP server via stdio
- **PDF**: iText 9.1 · **HTML parse**: Jsoup · **Utils**: Hutool · **Serialization**: Kryo

### Key Configuration Files
- `application.yml` — default config (port 8123, context-path `/api`, DashScope model, Ollama fallback)
- `application-local.yml` — local dev secrets (**gitignored**; contains `dashscope.api-key` and `search-api.api-key`)
- `application-prod.yml` — production overrides (mostly empty, secrets via env vars)
- `mcp-servers.json` — MCP server definitions (**gitignored**; template at `mcp-servers.example.json`)

### Agent Hierarchy (core of the codebase)

```
BaseAgent (abstract)
  └─ ReActAgent (abstract, think → act loop)
       └─ ToolCallAgent (concrete, manages tool execution via ToolCallingManager)
            └─ MyManus (final agent, system prompt + 20 max steps)
```

- **`BaseAgent`** — State machine (`IDLE → RUNNING → FINISHED/ERROR`), step-loop with `maxSteps`, maintains `messageList` memory, supports both sync `run()` and SSE streaming `runStream()`
- **`ReActAgent`** — Implements `step()` as `think() → act()`; each step decides whether tool calls are needed
- **`ToolCallAgent`** — Disables Spring AI's internal tool execution (`withInternalToolExecutionEnabled(false)`) to manually manage tool call flow. `think()` calls the LLM and checks for tool calls; `act()` executes them via `ToolCallingManager` and checks if `doTerminate` was called
- **`MyManus`** — The production agent. Configured with all 7 local tools, DashScope chat model, `MyLoggerAdvisor`, 20 max steps

### `TravelApp` (the application service layer)

Central `@Component` that wires ChatClient with system prompt (travel planner persona with current date), chat memory (in-memory `MessageWindowChatMemory`, 20 messages), and exposes methods for various chat modes:

| Method | What it does |
|---|---|
| `doChat` | Basic chat with conversation memory |
| `doChatByStream` | Same but SSE streaming |
| `doChatWithRag` | Chat + `QueryRewriter` rewrite + `QuestionAnswerAdvisor` (vector store) |
| `doChatWithTools` | Chat + local tools only |
| `doChatWithMcp` | Chat + MCP tools only (Amap) |
| `doChatWithFull` | Chat + local tools + MCP tools |
| `doChatWithFullByStream` | Full tools, SSE streaming (used by frontend) |

### Tool Registration (`ToolRegistration.java`)

Seven local tools registered as a `ToolCallback[]` bean:
1. **`FileOperationTool`** — Read/write/list files
2. **`WebSearchTool`** — Web search (requires `search-api.api-key`)
3. **`WebScrapingTool`** — Scrape webpage content via Jsoup
4. **`ResourceDownloadTool`** — Download files from URLs
5. **`PDFGenerationTool`** — Generate PDF travel itineraries (iText)
6. **`HolidayCalendarTool`** — Chinese holiday/calendar lookup
7. **`TerminateTool`** — Signals the agent to stop (tool name: `doTerminate`)

MCP tools (Amap: weather, routes, POI search, etc.) come in via `ToolCallbackProvider` autowired from Spring AI MCP client config.

### RAG Pipeline
1. `TravelAppDocumentLoader` — Loads 5 Markdown files from `classpath:document/*.md` (travel knowledge base: 决策/规划/准备/体验/安全篇)
2. `MyTokenTextSplitter` — Custom document splitting logic
3. `MyKeywordEnricher` — Adds keyword metadata to documents
4. `TravelAppVectorStoreConfig` — Creates a `PgVectorStore` bean (PostgreSQL, DashScope embeddings, HNSW index)
5. `QueryRewriter` — Uses Spring AI's `RewriteQueryTransformer` to rewrite user queries before retrieval

### Custom Advisors
- **`MyLoggerAdvisor`** — Logs request prompt and response text at INFO level; implements both `CallAdvisor` and `StreamAdvisor`
- **`ReReadingAdvisor`** — Implements the Re2 prompting technique (repeats the question in the prompt); currently not wired into TravelApp by default

### Chat Memory
- Default: `MessageWindowChatMemory` (in-memory, 20 messages, in `TravelApp` constructor)
- Alternative: `FileBasedChatMemory` — Disk-persisted via Kryo serialization (one `.kryo` file per conversation ID)

### API Endpoints (all under `/api` context path)

| Endpoint | Controller |
|---|---|
| `GET /health` | `HealthController` |
| `GET /ai/travel_app/chat/sync` | Basic sync chat |
| `GET /ai/travel_app/chat/sse` | Basic SSE streaming |
| `GET /ai/travel_app/chat/sse/full` | **Full tool SSE stream (frontend uses this)** |
| `GET /ai/travel_app/chat/rag` | RAG-augmented chat |
| `GET /ai/travel_app/chat/tools` | Local tools chat |
| `GET /ai/travel_app/chat/mcp` | MCP-only chat |
| `GET /ai/travel_app/chat/full` | Full tools sync chat |
| `GET /ai/manus/chat` | MyManus autonomous agent (SSE) |

## Frontend Architecture

- **Vue 3** (Composition API, `<script setup>`) · **Vite 4** · **Vue Router 4** · **Axios**
- Dev server on port 3000 (configured in `vite.config.js`, not 5173 as README says)
- API base URL: `http://localhost:8123/api` (dev) or `/api` (production, via nginx reverse proxy)
- SSE streaming via `EventSource` in `src/api/index.js`
- Three routes: `/` (Home), `/travel-planner` (TravelPlanner with ChatRoom), `/super-agent` (SuperAgent with ChatRoom)
- `ChatRoom.vue` is the reusable chat component used by both planner and agent views
- Dark theme via CSS custom properties defined in `App.vue` (amber accent, DM Sans + Noto Sans SC fonts)
- Production deployment: nginx serves static files and reverse-proxies `/api/` to backend

## Docker

Backend Dockerfile uses `maven:3.9-amazoncorretto-21`, runs `mvn clean package -DskipTests`, and starts with `--spring.profiles.active=prod`. Frontend has its own Dockerfile with nginx.

## Secrets & Security

- **`application-local.yml`** and **`mcp-servers.json`** are in `.gitignore` — never commit these
- `application-local.yml` contains `spring.ai.dashscope.api-key` and `search-api.api-key` in plaintext
- `mcp-servers.json` contains the Amap API key under `env.AMAP_MAPS_API_KEY`
- Production uses environment variables: `DASHSCOPE_API_KEY`, `SEARCH_API_KEY` (see `application.yml` placeholders)

## Database Note

The app uses PGVector (PostgreSQL + pgvector extension) for vector storage. Configuration is in `application-local.yml` (cloud RDS). `DataSourceAutoConfiguration` is enabled, and `TravelAppVectorStoreConfig` creates a `PgVectorStore` bean with HNSW index and cosine distance.
