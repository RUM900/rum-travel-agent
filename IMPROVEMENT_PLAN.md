# RUM 项目打磨计划 · 让简历更有亮点

---

## 当前状态评估

### 已有亮点 ✅
| 能力 | 简历关键词 |
|------|-----------|
| 自研 ReAct Agent 框架（状态机 + think→act 循环） | Agent 架构设计 |
| Spring AI 1.0 + MCP 协议集成（高德地图 19 个工具） | MCP 协议实践 |
| 完整 RAG 管线 + 评估体系（HitRate/MRR/LLM-as-Judge） | RAG 工程化 |
| 双轨 SSE 流式（token 级 + step 级） | 流式架构 |
| iText PDF 行程导出 | 文档生成 |

### 当前短板 ❌
| 缺失能力 | 面试风险 |
|----------|---------|
| 无数据库持久化（纯内存，重启全丢） | "数据怎么存的？" → 尴尬 |
| 无认证授权（CORS 全开，裸奔） | "安全怎么做的？" → 扣分 |
| 无全局异常处理 | "线上报错了怎么办？" → 扣分 |
| 无监控/日志体系 | "怎么排查问题？" → 扣分 |
| 前端两个页面 90% 重复代码 | "前端架构怎么设计的？" → 扣分 |
| 工具串行执行（Agent 每次只调一个工具） | "性能怎么优化的？" → 无言 |
| 无容器编排/一键部署 | "怎么部署的？" → 单薄 |

---

## 打磨方案（按优先级排序）

### 🥇 Tier 1 — 必须做，立竿见影

#### 1. 多数据源持久化 + 环境自适应

**现状**：`DataSourceAutoConfiguration` 被排除，零持久化。PgVector 依赖已引入但注释掉。

**改进**：
```
方案：环境自适应存储策略
├─ dev 环境：H2 内存数据库 + SimpleVectorStore（零配置开发）
├─ prod 环境：PostgreSQL + PgVector（生产持久化 + 向量检索一体）
└─ 切换方式：spring.profiles.active=dev|prod
```

**具体要做的事**：
1. 取消 `DataSourceAutoConfiguration` 排除，改为条件装配
2. 引入 H2 依赖（dev profile），PgVector 已有（prod profile）
3. 增加 `application-dev.yml` 配置 H2，完善 `application-prod.yml` 配置 PG
4. 新增 `ChatHistory` JPA Entity + Repository，替换纯内存 ChatMemory
5. 迁移 `FileBasedChatMemory` 为 DB 存储 → 面试可以说 "自研了 Kryo 文件存储方案，后升级为 DB 持久化"

**简历话术**：
> "设计了环境自适应的存储架构——开发环境 H2 零配置启动，生产环境 PostgreSQL + PgVector 一体化存储对话记录和向量数据，避免了多数据库运维复杂度。"

**面试延展话题**：H2 vs PostgreSQL、PgVector 选型、为什么不用 Redis、数据迁移策略

---

#### 2. 全局异常处理 + 统一响应体

**现状**：零异常处理。任何未捕获异常返回 Spring Boot 默认 HTML 错误页或丑陋的 stack trace。

**具体要做的事**：
1. 新增 `Result<T>` 统一响应体（code + message + data）
2. 新增 `GlobalExceptionHandler`（@ControllerAdvice）
   - `AiException` → 业务异常（如 API Key 无效、模型调用失败）
   - `ToolExecutionException` → 工具执行异常
   - `RateLimitException` → 限流异常
   - `Exception` → 兜底（记录 traceId，返回友好提示）
3. 新增 `RequestIdFilter` → 每个请求生成 traceId，贯穿日志
4. 改造 `AiController` 所有方法返回 `Result<T>`

**简历话术**：
> "设计了统一异常处理体系，按异常类型分级响应（业务异常/工具异常/系统异常），配合 traceId 全链路追踪，确保前端始终拿到结构化错误信息而非堆栈。"

**面试延展话题**：异常分级策略、traceId 实现方式（MDC + Filter）、前后端错误码约定

---

#### 3. 认证授权 (Spring Security + JWT)

**现状**：CORS 全开（`allowedOriginPatterns("*")`），零认证。任何人可以直接调 API。

**具体要做的事**：
1. 引入 `spring-boot-starter-security`
2. 实现 JWT 登录流程：
   - `POST /api/auth/login` → 验证用户名密码 → 返回 JWT token
   - `POST /api/auth/register` → 注册
   - `JwtAuthenticationFilter` → 解析 token，注入 SecurityContext
3. API 路径权限设计：
   - `/api/auth/**` → 公开
   - `/api/health` → 公开
   - `/api/ai/**` → 需认证
4. 前端：登录页 + token 存储（localStorage）+ Axios 拦截器自动带 token
5. CORS 收紧：只允许前端域名

**简历话术**：
> "基于 Spring Security + JWT 实现了无状态认证体系，配合自定义 Filter 链实现请求级鉴权，前后端通过 Axios 拦截器自动携带 token。"

**面试延展话题**：JWT vs Session、token 刷新策略（refresh token）、CSRF/XSS 防护、为什么选无状态

---

#### 4. Agent 并行工具调用

**现状**：`ToolCallAgent.act()` 中工具调用是**串行**的（`SyncToolCallingManager`）。LLM 一次可能返回 3 个工具调用，但它们是逐个执行的。

**具体要做的事**：
1. 分析 `ToolCallingManager` 源码，改为并行执行无依赖的工具调用
2. 在 `act()` 中：
```java
// 当前：串行
ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, response);

// 改进：并行（无依赖的工具同时执行）
List<CompletableFuture<ToolResponse>> futures = toolCalls.stream()
    .map(tc -> CompletableFuture.supplyAsync(() -> executeSingleTool(tc), executor))
    .toList();
List<ToolResponse> responses = futures.stream()
    .map(CompletableFuture::join).toList();
```
3. 新增 `ToolExecutor` 线程池配置（核心线程数 = CPU 核数）
4. 增加超时控制（每个工具调用 max 30s）
5. 编写 JMH 基准测试：串行 vs 并行性能对比

**简历话术**：
> "优化了 Agent 的工具调用机制——从串行执行重构为基于 CompletableFuture 的并行执行架构，对无依赖关系的工具调用并发执行，性能提升 N 倍（附 JMH 基准测试数据）。"

**面试延展话题**：CompletableFuture vs 虚拟线程（Java 21）、线程池参数调优、工具依赖图分析、超时熔断

---

### 🥈 Tier 2 — 强烈建议，显著加分

#### 5. 可观测性三件套（Metrics + Tracing + Logging）

**现状**：只有 `MyLoggerAdvisor` 在 INFO 级别记录 LLM 调用的输入输出。无 Metrics，无 Tracing。

**具体要做的事**：
1. **Metrics**（Micrometer + Prometheus）：
   - `agent.steps.total` — Agent 每步耗时（Timer）
   - `agent.steps.count` — Agent 总步数（Counter）
   - `tool.calls.total` — 每个工具的调用次数
   - `tool.calls.duration` — 每个工具的调用耗时
   - `llm.token.usage` — LLM token 消耗（Counter）
   - `rag.retrieval.hits` — RAG 检索命中率
2. **Tracing**（Micrometer Tracing + 日志 traceId）：
   - 一次 Agent 请求 = 一个 trace
   - 每一步 think/act = 一个 span
   - 每个工具调用 = 一个子 span
3. **Logging**：
   - 结构化日志（JSON 格式，方便 ELK 采集）
   - 日志级别动态调整（/actuator/loggers）
4. **可视化**：Grafana Dashboard（PromQL 查询）

**简历话术**：
> "搭建了基于 Micrometer + Prometheus + Grafana 的三维可观测性体系：Agent 多步推理的每步耗时与 token 消耗可量化追踪，工具调用成功率与延迟有实时大盘，配合 traceId 实现分布式链路追踪。"

**面试延展话题**：Prometheus 数据模型（Counter/Gauge/Histogram）、Grafana Dashboard 设计、为什么不用 SkyWalking/Pinpoint

---

#### 6. Resilience4j 弹性防护

**现状**：零防护。外部 API（DashScope、SearchAPI、Amap MCP）调用失败会直接抛异常。

**具体要做的事**：
1. **限流**（RateLimiter）：
   - `/api/ai/**` 整体 QPS 限制
   - 单个 IP 每分钟请求数限制
2. **熔断**（CircuitBreaker）：
   - DashScope LLM 调用 → 3 次失败打开熔断 → 降级到 Ollama 本地模型
   - SearchAPI 调用 → 熔断后返回 "搜索服务暂时不可用"
3. **重试**（Retry）：
   - 网络超时类异常自动重试 2 次（指数退避）
4. **舱壁**（Bulkhead）：
   - Agent 执行线程池隔离（防止某个慢请求占满所有线程）

**简历话术**：
> "引入 Resilience4j 实现了四层弹性防护——API 限流防刷、LLM 调用熔断自动降级到本地模型、网络抖动自动重试、Agent 执行线程池隔离。确保系统在外部依赖不稳定时依然可用。"

**面试延展话题**：熔断器状态机（CLOSED→OPEN→HALF_OPEN）、降级策略设计、限流算法（令牌桶 vs 滑动窗口）

---

#### 7. 前端架构重构 + Markdown 渲染 + 对话历史

**现状**：两个 Vue 页面（TravelPlanner / SuperAgent）90% 代码重复。消息纯文本无格式。无对话历史。无断连恢复。

**具体要做的事**：
1. **提取 Composable**：
   - `useChat(apiFunction)` — 封装 SSE 连接、消息列表、断连重试
   - `useConversation()` — 对话历史管理
2. **Markdown 渲染**：
   - 引入 `markdown-it` + `highlight.js`
   - AI 回复中的表格/代码块/链接正确渲染
3. **对话历史侧边栏**：
   - 左侧对话列表 + 右侧聊天区
   - 支持新建/切换/删除对话
4. **SSE 断连恢复**：
   - 指数退避重连（1s → 2s → 4s → max 30s）
   - 重连后从上次中断位置继续
5. **流式工具调用可视化**：
   - 当 Agent 调用工具时，显示 "🔍 正在搜索..." 状态卡片
   - 工具返回结果折叠显示

**简历话术**：
> "前端基于 Vue 3 Composition API 设计了 Composable 模式（useChat/useConversation），将 SSE 流式通信、断连恢复、对话管理等能力抽象为可复用逻辑。引入 Markdown 渲染引擎让 AI 回复支持富文本展示。"

**面试延展话题**：Vue 3 Composition API vs Options API、Composable 设计模式、SSE vs WebSocket、指数退避算法

---

### 🥉 Tier 3 — 锦上添花，差异化竞争

#### 8. 模型网关 + 多模型切换

**现状**：DashScope 硬编码为主模型，Ollama 为备选仅在配置中声明但未实际切换。

**改进**：
```
设计一个 ModelRouter：
├─ 路由策略：轮询 / 最低延迟 / 故障转移
├─ 模型注册表：DashScope(qwen-plus) / Ollama(gemma3) / OpenAI 兼容 API
├─ 运行时切换：前端下拉选择模型
└─ 负载均衡：多 API Key 轮询（避免单个 Key 限流）
```

**简历话术**：
> "设计了统一模型网关层，支持 DashScope/Ollama/OpenAI 兼容协议的模型即插即用，通过路由策略实现故障自动转移和负载均衡。"

---

#### 9. Human-in-the-Loop（人工确认）

**现状**：Agent 全自动执行，无人工干预点。

**改进**：
1. 定义敏感操作清单（如：生成 PDF、执行网页搜索、修改文件）
2. 在这些操作执行前，Agent 暂停，向前端发送确认请求
3. 前端弹出确认对话框，用户批准/拒绝后继续

**简历话术**：
> "在 Agent 自主执行流程中引入 Human-in-the-Loop 机制，对敏感操作（PDF 生成、网络搜索等）设置人工确认卡点，平衡了自动化效率与安全可控性。"

---

#### 10. CI/CD + 容器化部署

**现状**：Dockerfile 各一个（后端/前端），无编排，无 CI/CD。

**改进**：
```
├─ docker-compose.yml         # 一键启动：backend + frontend + PostgreSQL
├─ .github/workflows/
│   ├── ci.yml                # PR → 编译 + 测试
│   └── cd.yml                # main → 构建镜像 → 推送 Docker Hub
├─ nginx.conf                  # 反向代理（前端 + /api → 后端）
└─ 环境变量管理：.env.example + Docker secrets
```

---

### 📊 建议实施路线

```
第一周（基础工程化）：
├─ Day 1-2: 统一响应体 + 全局异常处理 + traceId Filter
├─ Day 3-4: Spring Security + JWT 认证
└─ Day 5-6: 多数据源持久化（H2 dev + PostgreSQL prod）

第二周（Agent 深挖）：
├─ Day 1-3: Agent 并行工具调用 + JMH 基准测试
├─ Day 4-5: Resilience4j（限流/熔断/重试）
└─ Day 6:   Human-in-the-Loop

第三周（可观测 + 前端）：
├─ Day 1-3: Micrometer + Prometheus + Grafana
├─ Day 4-5: 前端 Composable 重构 + Markdown 渲染
└─ Day 6:   对话历史管理

第四周（收尾）：
├─ Day 1-2: 模型网关
├─ Day 3-4: Docker Compose + CI/CD
└─ Day 5:   整体联调 + 文档更新
```

---

### ⚡ 如果要快速出效果（3 天冲刺版）

只做这 4 件事，简历就能脱胎换骨：

| 优先级 | 事项 | 预计耗时 | 简历关键词 |
|--------|------|----------|-----------|
| 1 | 全局异常处理 + 统一响应体 | 3h | 工程化 |
| 2 | JWT 认证 + 收紧 CORS | 4h | 安全 |
| 3 | H2 + PostgreSQL 双环境持久化 | 5h | 数据架构 |
| 4 | Agent 并行工具调用 | 4h | 性能优化 |

---

### 🔥 面试时最容易被追问的"坑"（提前准备）

1. **"为什么用 GET 而不是 POST 传聊天消息？"**
   → 实话：开发方便。改进：改成 POST + RequestBody（消息可能很长，GET URL 长度受限）

2. **"MyLoggerAdvisor 被添加了两次？"**
   → 在 TravelApp 构造器中 `defaultAdvisors(new MyLoggerAdvisor())` 且各方法又 `.advisors(new MyLoggerAdvisor())` 显式添加。实际是个小 bug，可以主动说 "我发现了这个问题并修复了，改为只在 ChatClient Builder 中统一配置"

3. **"为什么 MyManus new 新实例而不做成单例 Bean？"**
   → 这是有意为之：每个 Agent 请求需要独立的状态机、消息列表、SseEmitter。但可以提升为 "Prototype Scope Bean + ObjectProvider 懒加载"

4. **"前端 SSE 断连了怎么办？"**
   → 当前只是 `onerror` 关闭连接，没有任何恢复。这是改进点——加上指数退避重连机制

5. **"工具调用失败会导致 Agent 崩溃吗？"**
   → 当前 ToolCallAgent.think() 的 catch 块捕获异常后将错误信息作为 AssistantMessage 加入消息列表，Agent 可以继续但不一定能正确恢复。缺少结构化的错误分类处理
