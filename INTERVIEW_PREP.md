# RUM · AI 旅行规划助手 — 面试深度准备指南

---

## 一、项目一句话概述

> 基于 **Spring AI + ReAct Agent + MCP 协议 + RAG** 构建的智能旅行规划应用。支持多轮对话、工具调用（7 个本地工具 + 19 个高德地图 MCP 工具）、知识库检索增强生成、自主多步推理智能体、SSE 流式输出、PDF 行程导出。

---

## 二、系统架构全景图

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Frontend (Vue 3 + Vite)                         │
│   EventSource SSE → 字符级流式渲染 → ChatRoom 复用组件               │
│   /travel-planner (TravelApp)  │  /super-agent (MyManus Agent)      │
└────────────────────────────┬────────────────────────────────────────┘
                             │ HTTP GET + SSE
                             │ context-path: /api
┌────────────────────────────▼────────────────────────────────────────┐
│                     AiController (REST API)                          │
│  10 个端点: sync / sse / sse/full / rag / tools / mcp / full / manus│
└────────────────────────────┬────────────────────────────────────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼                              ▼
┌──────────────────────────┐   ┌──────────────────────────────┐
│       TravelApp          │   │    MyManus (ReAct Agent)      │
│  (ChatClient + Advisors) │   │  (think→act 循环, max 20步)  │
│                          │   │                              │
│  System Prompt           │   │  BaseAgent                   │
│  ChatMemory (20条窗口)    │   │   ├─ ReActAgent              │
│  MyLoggerAdvisor         │   │   │   └─ ToolCallAgent       │
│  + 可选: RAG / Tools /   │   │   │       └─ MyManus         │
│          MCP Advisors    │   │   │                          │
└──────────┬───────────────┘   └──────────────┬───────────────┘
           │                                  │
    ┌──────┼──────┬──────────┐         ┌──────▼──────┐
    ▼      ▼      ▼          ▼         │ ToolCalling │
┌──────┐ ┌────┐ ┌──────┐ ┌──────┐     │  Manager    │
│ RAG  │ │本地│ │ MCP  │ │ 对话 │     │ (手动执行)   │
│向量库│ │工具│ │(高德)│ │ 记忆 │     └─────────────┘
└──────┘ └────┘ └──────┘ └──────┘
```

### 两种请求路径

| 路径 | 模式 | 实现方式 |
|------|------|----------|
| **TravelApp** (`/ai/travel_app/...`) | 单次 LLM 调用 + Spring AI Advisors 链 | ChatClient.prompt().advisors(...).stream() |
| **MyManus** (`/ai/manus/chat`) | 多步自主推理 ReAct Agent | 手动 think→act 循环 + SseEmitter 流式推送 |

---

## 三、核心技术栈

| 层级 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 语言 | Java | 21 | 后端主语言 |
| 框架 | Spring Boot | 3.4.4 | 应用框架 |
| AI 框架 | Spring AI | 1.0 | LLM 集成、工具调用、MCP、RAG |
| AI 模型 | DashScope (qwen-plus) | — | 主模型（阿里云百炼） |
| 备选模型 | Ollama (gemma3:1b) | — | 本地备选 |
| 向量存储 | SimpleVectorStore | — | 内存向量库（可切换 PgVector） |
| MCP 客户端 | spring-ai-starter-mcp-client | 1.0 | 连接高德地图 MCP Server |
| PDF 生成 | iText | 9.1 | 行程 PDF 导出 |
| HTML 解析 | Jsoup | 1.19.1 | 网页抓取 |
| 工具库 | Hutool | 5.8.37 | 通用工具（文件、HTTP） |
| 序列化 | Kryo | 5.6.2 | 对话记忆持久化 |
| 接口文档 | Knife4j | 4.4.0 | Swagger UI |
| 前端 | Vue 3 + Vite | 3.x / 4.x | SPA |
| 前端路由 | Vue Router | 4.x | 三页面路由 |
| HTTP 客户端 | Axios | — | 前端 API 调用 |
| SSE | EventSource (浏览器原生) | — | 流式接收 AI 响应 |

---

## 四、深度组件解析

### 4.1 Agent 继承体系（最核心的设计）

```
BaseAgent (抽象类)                     ← 生命周期 + 状态机 + 消息记忆
  ├─ state: IDLE → RUNNING → FINISHED/ERROR
  ├─ messageList: List<Message>（手动管理，不用 Spring AI ChatMemory）
  ├─ run(String) → 同步循环调用 step()
  └─ runStream(String) → CompletableFuture.runAsync + SseEmitter（SSE 流式）

    └─ ReActAgent (抽象类)             ← ReAct 模式实现
         └─ step() = think() + act()   ← 每步 = 思考 + 行动

           └─ ToolCallAgent (具体类)   ← 工具调用实现
                ├─ think() → 调 LLM，判断是否需要工具
                ├─ act()   → 通过 ToolCallingManager 执行工具
                └─ 检测 doTerminate → 设置 state = FINISHED

                  └─ MyManus (最终 Agent)  ← @Component，生产使用
                       ├─ maxSteps = 20
                       ├─ System Prompt: "全知全能 AI 助手"
                       └─ 每次请求 new 新实例（非单例）
```

#### 关键设计决策

**1. 为什么手动管理 `List<Message>` 而不是用 Spring AI 的 ChatMemory？**

- `ToolCallingManager.executeToolCalls()` 返回的 `conversationHistory` 需要**整体替换**消息列表（因为工具调用消息和工具响应消息必须成对出现）
- Spring AI 的 `ChatMemory` 是追加（append-only）模式，不支持替换
- 手动管理可以精确控制何时添加 assistant 消息（无工具调用时手动添加，有工具调用时由 ToolCallingManager 统一管理）

**2. 为什么设置 `withInternalToolExecutionEnabled(false)`？**

- 禁用 Spring AI 的内部自动工具执行，获得完全手动控制权
- 这样才能实现 think→act 分离（ReAct 模式的核心）
- 才能在 act 中检测 `doTerminate` 工具调用来终止 Agent
- 才能自定义日志和错误处理

**3. 为什么 MyManus 每次请求 new 新实例？**

- 每个请求需要干净的状态（IDLE）、空的消息列表、独立的 SseEmitter
- 避免并发请求间的状态污染
- Agent 用完即弃，不需要并发控制

#### 状态机转换

```
IDLE ──run()/runStream()──▶ RUNNING ──doTerminate 被调用──▶ FINISHED
                              │                                   
                              ├── 达到 maxSteps ──▶ FINISHED     
                              │                                   
                              └── 异常 ──▶ ERROR                  
                                                      
所有路径最终都执行 cleanup()（SSE 的超时/完成回调也会触发）
```

#### think() 方法核心逻辑

```java
// 1. 每步都追加 nextStepPrompt 到消息列表（强化引导）
messageList.add(new UserMessage(nextStepPrompt));

// 2. 调 LLM，传入 system prompt + messageList + 所有工具
ChatResponse response = chatClient.prompt(new Prompt(messageList))
    .system(systemPrompt)
    .tools(availableTools)   // 7 个本地工具
    .call().chatResponse();

// 3. 判断是否需要工具调用
if (toolCalls.isEmpty()) {
    messageList.add(assistantMessage);  // 无工具：手动记录
    return false;  // 不需要 act
} else {
    return true;   // 需要 act（assistantMessage 由 act 统一管理）
}
```

#### act() 方法核心逻辑

```java
// 1. 通过 ToolCallingManager 执行所有工具
ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, response);

// 2. 整体替换消息列表（包含 assistant 工具调用消息 + 所有工具响应消息）
messageList = result.conversationHistory();

// 3. 检查是否调用了 doTerminate
boolean terminated = toolResponseMessage.getResponses().stream()
    .anyMatch(r -> r.name().equals("doTerminate"));
if (terminated) state = FINISHED;
```

---

### 4.2 七个本地工具 (ToolCallback[])

| # | 工具 | 功能 | 输入 | 输出 | 依赖 |
|---|------|------|------|------|------|
| 1 | FileOperationTool | 文件读写 | 文件名/内容 | 文件内容/路径 | Hutool FileUtil |
| 2 | WebSearchTool | 网页搜索 | 搜索关键词 | 前5条结果的 JSON | SearchAPI.io (Baidu) |
| 3 | WebScrapingTool | 网页抓取 | URL | 页面 HTML | Jsoup |
| 4 | ResourceDownloadTool | 资源下载 | URL + 文件名 | 本地路径 | Hutool HttpUtil |
| 5 | PDFGenerationTool | PDF 生成 | 内容文本 | PDF 文件路径 | iText 9.1 + SimHei 字体 |
| 6 | HolidayCalendarTool | 节假日查询 | 日期/日期范围 | 节日信息 | chinese-days CDN 数据集 |
| 7 | TerminateTool | 终止 Agent | 无 | "任务结束" | — |

#### 工具注册方式

```java
@Configuration
public class ToolRegistration {
    @Value("${search-api.api-key}")
    private String searchApiKey;

    @Bean
    public ToolCallback[] allTools() {
        // 手动 new 每个工具（POJO，非 Spring Bean）
        return ToolCallbacks.from(
            new FileOperationTool(),
            new WebSearchTool(searchApiKey),   // API Key 通过构造器注入
            new WebScrapingTool(),
            new ResourceDownloadTool(),
            new PDFGenerationTool(),
            new HolidayCalendarTool(),          // 启动时加载两年节假日数据
            new TerminateTool()
        );
    }
}
```

**为什么工具是 POJO 而不是 Spring Bean？**
- 工具无需依赖注入（除 WebSearchTool 的 API Key 在 ToolRegistration 中传入）
- `ToolCallbacks.from()` 通过反射扫描 `@Tool` 注解，生成 `ToolCallback` 数组
- 保持工具简单可测试

#### 安全注意
- `TerminalOperationTool`（执行任意系统命令）**类存在但未注册**——这是有意为之的安全决策
- `WebSearchTool` 的 API Key 通过配置文件注入，不硬编码

---

### 4.3 RAG 知识库管线

#### 完整流程

```
应用启动时（一次性初始化）：
  ① TravelAppDocumentLoader.loadMarkdowns()
     ├─ 扫描 classpath:document/*.md（3 个文档）
     ├─ 按 --- 水平分割线切分
     ├─ 按文件名推断 category 分类标签
     └─ 输出: List<Document>（~N 个文档块）

  ② MyTokenTextSplitter.splitCustomized() [当前被注释掉]
     └─ TokenTextSplitter(200, 100, 10, 5000, true)

  ③ MyKeywordEnricher.enrichDocuments()
     └─ 每个文档块调用 LLM 提取 5 个关键词作为元数据

  ④ SimpleVectorStore.add(enrichedDocuments)
     └─ DashScope EmbeddingModel 向量化 + 存入内存

查询时（每次 RAG 请求）：
  ⑤ QueryRewriter.doQueryRewrite(query)
     └─ 基于 LLM 的 RewriteQueryTransformer 重写查询

  ⑥ QuestionAnswerAdvisor(travelAppVectorStore)
     ├─ 向量相似度检索 top-K 文档
     └─ 注入到 LLM Prompt 上下文

  ⑦ LLM 基于检索到的文档生成回答
```

#### 3 个知识库文档

| 文档 | 内容 | 行数 |
|------|------|------|
| `黄金路线模板库.md` | 4 条经典旅游路线（川西小环线、云南经典、西北青甘、苏杭）含日程/预算/住宿/避坑 | 262 |
| `景点实测数据手册.md` | 10 个热门景区（故宫、九寨沟、莫高窟等）含票价/预约/排队/路线 | 136 |
| `花期节庆与摄影日历.md` | 1-12 月花期+节庆+摄影指南，含具体日期/票价/拍摄参数 | 213 |

#### RAG 评估体系（4 个测试类）

| 测试类 | 评估维度 | 指标 |
|--------|----------|------|
| RetrievalEvaluatorTest | 检索质量 | HitRate@K, MRR@K, Recall@K |
| GenerationEvaluatorTest | 生成质量 | Faithfulness(1-5), AnswerRelevance(1-5) — LLM-as-Judge |
| RagDiagnosisTest | 诊断工具 | 三路对比（纯LLM vs RAG vs 纯文档）、文档新颖性判断 |
| EvalDataset | 评估数据集 | 20 个手工标注的评估用例（含预期关键词和参考答案点） |

---

### 4.4 MCP 协议集成

#### 高德地图 MCP Server（外部）

```json
// mcp-servers.json（gitignored）
{
  "mcpServers": {
    "amap-maps": {
      "command": "npx.cmd",
      "args": ["-y", "@amap/amap-maps-mcp-server"],
      "env": { "AMAP_MAPS_API_KEY": "..." }
    }
  }
}
```

- **通信方式**: STDIO（启动子进程，通过标准输入/输出 JSON-RPC 通信）
- **可用工具**: 19 个（天气查询、驾车/公交/步行/骑行路线规划、POI 搜索、周边搜索、地理编码等）
- **注入方式**: Spring AI MCP Client 自动发现 `ToolCallbackProvider` Bean，注入到 TravelApp

#### 自建图片搜索 MCP Server

- **独立项目**: `yu-image-search-mcp-server`，Spring Boot 应用，端口 8127
- **单一工具**: `searchImage(query)` → 调用 Pexels API 返回图片 URL
- **双传输模式**:
  - SSE 模式（WebMVC，独立运行）
  - STDIO 模式（作为子进程被其他 Agent 调用）
- **工具注册**: 使用 `MethodToolCallbackProvider.builder().toolObjects(imageSearchTool).build()` 自动发现 `@Tool` 方法

---

### 4.5 Advisors 体系

| Advisor | 实现接口 | 功能 | 是否启用 |
|---------|---------|------|---------|
| MyLoggerAdvisor | CallAdvisor + StreamAdvisor | 记录每次 LLM 调用的输入 Prompt 和输出文本 | ✅ 全局启用 |
| ReReadingAdvisor | CallAdvisor + StreamAdvisor | Re2 提示技术：让 LLM 重读问题 | ❌ 已实现未启用 |
| MessageChatMemoryAdvisor | Spring AI 内置 | 管理对话记忆窗口 | ✅ 全局启用 |
| QuestionAnswerAdvisor | Spring AI 内置 | RAG 检索增强 | ✅ RAG 路径启用 |

#### MyLoggerAdvisor 的实现

```java
// around advisor 模式
adviseCall(request, chain) {
    before(request);           // 记录 prompt
    response = chain.next();   // 调用 LLM
    observeAfter(response);    // 记录 response text
    return response;
}
```

#### ReReadingAdvisor（Re2 技术）

```java
// 将用户输入改写为: "原始问题\nRead the question again: 原始问题"
// 研究证明让模型重新阅读问题可以提升推理质量
```

---

### 4.6 对话记忆

| 方案 | 实现 | 容量 | 持久化 | 使用 |
|------|------|------|--------|------|
| MessageWindowChatMemory | Spring AI 内置 + InMemoryChatMemoryRepository | 20 条滑动窗口 | ❌ 重启丢失 | ✅ 当前使用 |
| FileBasedChatMemory | 自研 + Kryo 序列化 | 无限 | ✅ 磁盘 .kryo 文件 | ❌ 未启用 |

---

### 4.7 前端架构

```
src/
├── main.js                    # Vue 3 入口
├── App.vue                    # 根组件 + 全局 CSS 设计系统（暗色主题）
├── style.css                  # 空（所有样式在 App.vue 中）
├── api/index.js               # Axios + EventSource（SSE）
├── router/index.js            # 3 条路由（懒加载）
├── views/
│   ├── Home.vue               # 首页：两个入口卡片
│   ├── TravelPlanner.vue      # 旅行规划对话（连接 /ai/travel_app/chat/sse/full）
│   └── SuperAgent.vue         # 超级 Agent 对话（连接 /ai/manus/chat）
└── components/
    ├── ChatRoom.vue           # 可复用聊天组件（消息列表 + 输入框 + 滚动）
    └── AiAvatarFallback.vue   # AI 头像图标组件
```

#### SSE 流式接收

```javascript
// api/index.js
export function connectSSE(url, params, onMessage, onError) {
    const eventSource = new EventSource(url + '?' + queryString);
    eventSource.onmessage = (e) => {
        if (e.data === '[DONE]') {   // 特殊终止标记
            eventSource.close();
            return;
        }
        onMessage(e.data);           // 字符级增量推送
    };
}
```

#### 暗色主题设计系统

```css
:root {
    --bg: #09090b;          /* 近黑色背景 */
    --accent: #f59e0b;      /* 琥珀色强调 */
    --blue: #3b82f6;        /* 蓝色 */
    --orange: #f97316;      /* 橙色（SuperAgent 头像）*/
    --font-display: 'Space Grotesk', 'Noto Sans SC';
    --font-body: 'DM Sans', 'Noto Sans SC';
}
```

---

## 五、常见面试问题与回答

### Q1: 为什么自研 Agent 框架而不是用 LangChain4j？

**答：**
1. **精细控制需求**：需要手动控制 think→act 循环、工具执行时机、消息列表管理
2. **Spring AI 生态整合**：项目使用 Spring AI 1.0，其 ToolCallingManager 和 ChatClient 提供了足够的基础设施
3. **学习目的**：通过自研深入理解 ReAct Agent 原理
4. 实际上项目也依赖了 `langchain4j-community-dashscope`（用于 DashScope 兼容）

### Q2: ReAct Agent 的 think→act 循环是如何实现的？

**答：**
1. `BaseAgent.run()` 提供外层循环：`for (i < maxSteps && state != FINISHED)`
2. 每步调用 `ReActAgent.step()` 分解为：
   - `think()`: 调 LLM，传入 system prompt + messageList + 所有工具定义，LLM 返回是否需要工具调用
   - `act()`: 如果需要，通过 `ToolCallingManager.executeToolCalls()` 执行工具，整体替换消息列表
3. 每次 think 前追加 `nextStepPrompt`（如 "如果你想停止，请调用 terminate 工具"）
4. `doTerminate` 工具被调用后 → `act()` 检测到 → 设置 state=FINISHED → 循环退出

### Q3: 为什么禁用 Spring AI 的内部工具执行？

**答：**
`DashScopeChatOptions.withInternalToolExecutionEnabled(false)` —— 禁用后，`ChatClient.call()` 只返回工具调用意图而不实际执行。这让我们能够：
1. 手动控制 think（决定）和 act（执行）的分离
2. 在 act 中检测 `doTerminate` 工具调用来终止 Agent
3. 自定义错误处理和日志
4. 在工具执行前后添加自定义逻辑

### Q4: SSE 流式输出是如何实现的？两种方式的区别？

**答：**
两种 SSE 实现：

| 方面 | TravelApp SSE (Flux) | MyManus SSE (SseEmitter) |
|------|---------------------|--------------------------|
| 实现 | `ChatClient.prompt().stream().content()` 返回 `Flux<String>` | `CompletableFuture.runAsync()` + 手动 `sseEmitter.send()` |
| 模型 | Spring WebFlux 响应式 | 阻塞循环 + 异步线程 |
| 粒度 | LLM token 级流式 | 每步（step）结果推送 |
| 适用场景 | 单次 LLM 调用 | 多步 Agent 循环 |

**MyManus 为什么用 SseEmitter 而不是 Flux？**
Agent 的多步循环（think→act 反复进行）是同步阻塞的，不能自然地适配响应式 `Flux` 模型。用 `CompletableFuture.runAsync` 在线程池中执行循环，通过 `SseEmitter` 把每步结果推送到 HTTP 响应流，是更直观的适配方式。

### Q5: RAG 管线中为什么要做 Query Rewriting？

**答：**
用户的自然语言查询可能包含：
- 指代不明（"那里好玩吗？"——哪里？）
- 口语化表达（"想去不累的地方"）
- 信息不完整

`QueryRewriter` 使用 LLM (`RewriteQueryTransformer`) 将查询改写为独立的、精确的检索查询，解决上述问题。例如：
- "那边有什么好吃的" → "云南丽江有什么特色美食推荐"

### Q6: 向量存储为什么用 SimpleVectorStore 而不是 PgVector？

**答：**
1. **开发便利**：内存存储零配置，无需数据库
2. **数据量小**：只有 3 个知识库文档，几百个文档块，内存完全够用
3. **可切换**：代码已预留 PgVector 支持（依赖已在 pom.xml，只需取消 DataSource 排除即可激活）
4. **生产建议**：数据量大后切换到 PgVector 可持久化和水平扩展

### Q7: MCP 协议在项目中是如何使用的？

**答：**
1. **什么是 MCP**：Model Context Protocol，Anthropic 提出的 LLM 与外部工具/数据源通信的标准化协议
2. **项目中的角色**：
   - **Client 端**（主应用）：通过 `spring-ai-starter-mcp-client` 启动高德地图 MCP Server 作为 STDIO 子进程
   - **Server 端**（自建）：`yu-image-search-mcp-server` 提供图片搜索工具
3. **通信方式**：JSON-RPC over STDIO（子进程标准输入输出）
4. **集成方式**：Spring AI 自动将 MCP 工具注入为 `ToolCallbackProvider` Bean，与本地工具合并使用
5. **为什么用 MCP**：标准化的工具协议意味着可以替换不同的地图服务（如换用百度地图 MCP Server）而不改代码

### Q8: 如何保证 LLM 不"编造"实时数据？

**答：**
1. **System Prompt 约束**：明确定义角色规则——"永远不要编造实时数据，始终调用工具获取"
2. **工具优先**：在 System Prompt 中强调"如果工具能回答，直接使用工具回答"
3. **Next Step Prompt**：每步都提醒 Agent 使用工具
4. **RAG 知识库**：对于景区票价、路线等静态数据，通过向量检索提供真实数据源
5. **局限性**：这是 Prompt Engineering 层面的约束，如果模型足够强大（如 GPT-4），配合 well-structured system prompt 能有效减少幻觉——但不能 100% 杜绝

### Q9: 前端如何实现 SSE 流式接收？

**答：**
1. 使用浏览器原生 `EventSource` API
2. 连接 `/ai/travel_app/chat/sse/full?message=...&chatId=...`
3. `onmessage` 事件中增量追加文本到对应 AI 消息气泡
4. 服务端发送 `[DONE]` 作为流结束标记，前端收到后关闭连接
5. 通过 Vue 响应式系统（`ref`），每次文本追加自动触发视图更新
6. 使用 `watch` 监听消息内容变化，自动滚动到底部

### Q10: 如果让你改进这个项目，你会做什么？

**答：**
1. **安全性**：添加认证授权（Spring Security + JWT），收紧 CORS 策略
2. **持久化**：对话历史持久化（使用 FileBasedChatMemory 或数据库），向量存储切换到 PgVector
3. **可靠性**：添加限流（rate limiting）、重试机制、熔断器
4. **可观测性**：集成 Micrometer + Prometheus 监控 Agent 步数、工具调用成功率、LLM 延迟
5. **工具安全**：当前 `WebScrapingTool` 无 URL 白名单，应添加 SSRF 防护
6. **Agent 改进**：
   - 支持并行工具调用（当前是串行）
   - 添加人工确认环节（Human-in-the-loop）
   - 支持子 Agent 委派（多 Agent 协作）
7. **测试**：增加单元测试覆盖率（当前大多是集成测试），工具测试 Mock 外部 API
8. **前端**：添加错误重连机制、对话历史列表、Markdown 渲染

---

## 六、技术亮点

1. **自研 ReAct Agent 框架**：从零实现了 think→act 循环、状态机、消息管理、工具终止检测
2. **双模式架构**：既支持简单的 Advisor Chain 单次调用，也支持复杂的 Agent 多步推理
3. **完整的 RAG 管线**：文档加载→分类→关键词增强→向量化→查询重写→检索增强，附带完整的评估体系
4. **MCP 协议实践**：既是 MCP Client（消费高德地图），也是 MCP Server（提供图片搜索）
5. **精细的流式控制**：TravelApp 的 token 级流式 + Agent 的 step 级流式
6. **RAG 评估体系**：HitRate/MRR/Recall 检索指标 + LLM-as-Judge 生成质量指标 + 诊断工具

---

## 七、可能的追问方向

面试官可能会深入追问：

1. **"ReAct 比纯 Tool Calling 好在哪里？"**
2. **"消息列表为什么要整体替换而不是追加？"**
3. **"20 步上限够吗？为什么不设成动态的？"**
4. **"如果 LLM 不调用 doTerminate 怎么办？"**
5. **"SimpleVectorStore 的相似度计算用的什么算法？"**
6. **"MCP 的 STDIO 模式相比 HTTP 有什么优缺点？"**
7. **"如何处理工具调用失败的情况？"**
8. **"前端如何处理 SSE 连接断开？"**
9. **"为什么用 GET 而不是 POST？"**
10. **"怎么评估 RAG 的效果？"**
