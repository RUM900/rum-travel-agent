# 旅途智策 · AI 旅行规划助手

基于 Spring AI + Spring Boot + Vue 3 构建的智能旅行规划助手。支持自然语言对话式交互，集成 RAG 知识库检索、高德地图 MCP、工具调用（Tool Calling）和 ReAct 自主规划智能体，为旅行爱好者提供从行程决策到 PDF 导出的全流程服务。

## 核心功能

- **AI 对话规划** — 基于 System Prompt 角色定义 + 对话记忆，根据出行时间、预算、偏好定制个性化行程
- **RAG 知识库** — 3 本实测旅行手册（黄金路线模板库、景点实测数据手册、花期节庆与摄影日历），结合查询重写与向量检索增强 AI 回复质量
- **高德地图集成** — 通过 MCP 协议接入 19 个地图工具：天气查询、驾车/公交/步行/骑行路线、POI 搜索、周边搜索
- **工具调用** — 文件读写、网页搜索、网页抓取、节假日日历、PDF 行程生成，可按需扩展
- **ReAct 智能体** — 基于 Agent Loop 实现自主多步推理与执行，最大步数限制 + 状态管理 + 终止控制防止死循环
- **对话历史** — 基于文件的对话记忆持久化，支持历史对话列表查看与回放
- **流式对话** — SSE 实时推送，工具调用与流式输出无缝结合

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Java 21 · Spring Boot 3.4 · Spring AI 1.0 |
| AI 模型 | 阿里云 DashScope (qwen-plus) · Ollama |
| RAG | SimpleVectorStore 内存向量库 · Markdown 文档解析 · 查询重写 · 关键词增强 |
| 工具与协议 | @Tool 注解 · MCP 模型上下文协议 · 高德地图 MCP Server |
| 智能体 | ReAct Agent (BaseAgent → ToolCallAgent → MyManus) |
| 对话记忆 | FileBasedChatMemory 文件持久化 |
| 前端 | Vue 3 · Vite · SSE 流式通信 |
| 工具库 | Hutool · iText PDF · Jsoup · Knife4j

## 项目架构

```
rumagent/
├── src/main/java/com/yupi/yuaiagent/
│   ├── agent/              # 智能体 (BaseAgent/ReActAgent/ToolCallAgent/MyManus)
│   ├── app/                # 应用层 (TravelApp)
│   ├── chatmemory/         # 对话记忆 (FileBasedChatMemory)
│   ├── config/             # 配置 (ChatMemory/CORS)
│   ├── controller/         # 控制器 (Ai/Conversation/Health)
│   ├── rag/                # RAG (文档加载/向量存储/查询增强/Advisor)
│   ├── tools/              # 工具 (PDF/搜索/节假日/文件/抓取等)
│   ├── advisor/            # 自定义 Advisor (日志/重读)
│   └── demo/               # 示例 (HTTP/Spring AI/LangChain/Ollama 调用方式)
├── src/main/resources/
│   ├── document/           # RAG 知识库文档
│   ├── static/fonts/       # PDF 中文字体
│   ├── application.yml     # 主配置（公开）
│   └── mcp-servers.example.json
└── rumagent-frontend/      # Vue 3 前端
```

## 快速开始

### 环境要求

- Java 21+
- Node.js 18+
- Maven 3.8+

### 1. 克隆项目

```bash
git clone https://github.com/RUM/rumagent.git
cd rumagent
```

### 2. 配置密钥

创建 `src/main/resources/application-local.yml`：

```yaml
spring:
  ai:
    dashscope:
      api-key: <你的 DashScope API Key>
    mcp:
      client:
        stdio:
          servers-configuration: classpath:mcp-servers.json

search-api:
  api-key: <你的 SearchAPI Key>
```

创建 `src/main/resources/mcp-servers.json`：

```json
{
  "mcpServers": {
    "amap-maps": {
      "command": "npx",
      "args": ["-y", "@amap/amap-maps-mcp-server"],
      "env": { "AMAP_MAPS_API_KEY": "<你的高德地图 API Key>" }
    }
  }
}
```

### 3. 启动后端

```bash
./mvnw spring-boot:run
```

后端运行在 `http://localhost:8123/api`

### 4. 启动前端

```bash
cd rumagent-frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`

## API 端点

### 旅行规划

| 端点 | 说明 |
|------|------|
| `GET /ai/travel_app/chat/sse/full` | 全量工具流式对话（前端主入口） |
| `GET /ai/travel_app/chat/sync` | 同步对话 |
| `GET /ai/travel_app/chat/sse` | 基础流式对话 |
| `GET /ai/travel_app/chat/rag` | RAG 知识库对话 |
| `GET /ai/travel_app/chat/tools` | 本地工具对话 |
| `GET /ai/travel_app/chat/mcp` | MCP 服务对话 |
| `GET /ai/travel_app/chat/full` | 全量工具同步对话 |
| `GET /ai/manus/chat` | ReAct 自主规划智能体 |

### 对话历史

| 端点 | 说明 |
|------|------|
| `GET /conversations` | 获取所有对话列表 |
| `GET /conversations/{id}` | 获取对话完整消息 |
| `DELETE /conversations/{id}` | 删除对话 |

## 工具清单

### 本地工具（7 个）

| 工具 | 功能 |
|------|------|
| `FileOperationTool` | 文件读写 |
| `WebSearchTool` | 百度搜索 |
| `WebScrapingTool` | 网页内容抓取 |
| `ResourceDownloadTool` | 资源下载 |
| `PDFGenerationTool` | PDF 行程生成（支持中文） |
| `HolidayCalendarTool` | 节假日日历查询（基于 chinese-days） |
| `TerminateTool` | 智能体任务终止 |

### MCP 工具（高德地图 19 个）

| 类别 | 工具 | 功能 |
|------|------|------|
| 🌤️ 天气 | `maps_weather` | 城市天气查询 |
| 🚗 驾车 | `maps_direction_driving_by_address` | 驾车路线规划 |
| 🚌 公交 | `maps_direction_transit_integrated_by_address` | 公交地铁路线 |
| 🚶 步行 | `maps_direction_walking_by_address` | 步行路线规划 |
| 🚴 骑行 | `maps_direction_bicycling_by_address` | 骑行路线规划 |
| 🔍 POI | `maps_text_search` / `maps_around_search` / `maps_search_detail` | 地点搜索 / 周边 / 详情 |
| 🗺️ 地理 | `maps_geo` / `maps_regeocode` / `maps_distance` | 地址↔坐标 / 距离 |
| 📱 联动 | `maps_schema_*` | 导航/打车/收藏到高德APP |

## Author

**RUM**

## License

MIT
