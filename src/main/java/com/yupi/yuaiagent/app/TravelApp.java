package com.yupi.yuaiagent.app;

import com.yupi.yuaiagent.advisor.MyLoggerAdvisor;
import com.yupi.yuaiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class TravelApp {

    private final ChatClient chatClient;
    private final String systemPrompt;

    public TravelApp(ChatModel dashscopeChatModel, ChatMemory chatMemory) {

        // 构建包含当前日期的 System Prompt
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日"));
        String weekday = LocalDate.now().getDayOfWeek().getDisplayName(
                java.time.format.TextStyle.FULL, java.util.Locale.CHINESE);
        this.systemPrompt = """
                你是一名深耕国内旅游规划领域的资深旅行规划师，精通全国各地的旅游目的地。
                当前日期：%s (%s)。用户提到的"今天""明天""下周"等时间表述，请基于此日期计算。

                【核心行为准则】
                1. 绝不要凭记忆编造任何实时数据（天气、路线、日期等），必须通过调用工具获取。
                2. 调用工具获取数据后，用自然语言组织回复用户，绝不要输出 tool_code、tool_response、
                   function_call、json、``` 代码块等工具调用的内部过程。用户不需要看到这些。
                3. 如果用户的问题可以通过工具回答，直接调用工具，拿到结果后简洁回复。
                4. 只有当用户需求确实模糊（如"我想出去玩但不知道去哪"），才引导补充信息。
                5. 用户给出明确出行需求时，直接规划行程，不要反问无关信息。""".formatted(today, weekday);

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(this.systemPrompt)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 自定义日志 Advisor，可按需开启
                        new MyLoggerAdvisor()
                )
                .build();
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     */
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    /**
     * AI 全量工具对话（本地工具 + MCP，SSE 流式传输）
     */
    public Flux<String> doChatWithFullByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .toolCallbacks(toolCallbackProvider)
                .stream()
                .content();
    }

    record TravelPlan(String title, List<String> suggestions) {
    }

    /**
     * AI 旅行规划报告功能（结构化输出）
     */
    public TravelPlan doChatWithReport(String message, String chatId) {
        TravelPlan travelPlan = chatClient
                .prompt()
                .system(systemPrompt + "每次对话后都要生成旅行规划结果，标题为{用户名}的旅行规划报告，内容为每日行程建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(TravelPlan.class);
        log.info("travelPlan: {}", travelPlan);
        return travelPlan;
    }

    // AI 旅行知识库问答功能

    @Resource
    private VectorStore travelAppVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    /**
     * 和 RAG 知识库进行对话
     */
    public String doChatWithRag(String message, String chatId) {
        // 查询重写
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient
                .prompt()
                // 使用改写后的查询
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用 RAG 知识库问答
                .advisors(new QuestionAnswerAdvisor(travelAppVectorStore))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    // AI 调用工具能力
    @Resource
    private ToolCallback[] allTools;

    /**
     * AI 旅行规划功能（支持调用工具）
     */
    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    // AI 调用 MCP 服务

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * AI 旅行规划功能（同时调用本地工具 + MCP 服务）
     */
    public String doChatWithFull(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * AI 旅行规划功能（调用 MCP 服务）
     */
    public String doChatWithMcp(String message, String chatId) {
        try {
            ChatResponse chatResponse = chatClient
                    .prompt()
                    .user(message)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                    .advisors(new MyLoggerAdvisor())
                    .toolCallbacks(toolCallbackProvider)
                    .call()
                    .chatResponse();
            String content = chatResponse.getResult().getOutput().getText();
            log.info("content: {}", content);
            return content;
        } catch (Exception e) {
            log.error("MCP 调用失败: {}", e.getMessage());
            return "抱歉，调用高德地图服务时出现问题：" + e.getMessage() + "。请稍后重试或换个方式提问。";
        }
    }
}
