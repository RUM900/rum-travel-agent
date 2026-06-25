package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.agent.MyManus;
import com.yupi.yuaiagent.app.TravelApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private TravelApp travelApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 同步调用 AI 旅行规划大师应用
     */
    @GetMapping("/travel_app/chat/sync")
    public String doChatWithTravelAppSync(String message, String chatId) {
        return travelApp.doChat(message, chatId);
    }

    /**
     * SSE 流式调用 AI 旅行规划大师应用
     */
    @GetMapping(value = "/travel_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithTravelAppSSE(String message, String chatId) {
        return travelApp.doChatByStream(message, chatId);
    }

    /**
     * SSE 流式调用 AI 旅行规划大师（全量工具：本地 + 高德 MCP）
     */
    @GetMapping(value = "/travel_app/chat/sse/full", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithTravelAppFullSSE(String message, String chatId) {
        return travelApp.doChatWithFullByStream(message, chatId);
    }

    /**
     * SSE 流式调用 AI 旅行规划大师应用（ServerSentEvent 格式）
     */
    @GetMapping(value = "/travel_app/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithTravelAppServerSentEvent(String message, String chatId) {
        return travelApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    /**
     * SSE 流式调用 AI 旅行规划大师应用（SseEmitter）
     */
    @GetMapping(value = "/travel_app/chat/sse_emitter")
    public SseEmitter doChatWithTravelAppServerSseEmitter(String message, String chatId) {
        SseEmitter sseEmitter = new SseEmitter(180000L); // 3 分钟超时
        travelApp.doChatByStream(message, chatId)
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        return sseEmitter;
    }

    /**
     * RAG 知识库对话（检索增强）
     */
    @GetMapping("/travel_app/chat/rag")
    public String doChatWithTravelAppRag(String message, String chatId) {
        return travelApp.doChatWithRag(message, chatId);
    }

    /**
     * 工具调用对话（天气、PDF、搜索等）
     */
    @GetMapping("/travel_app/chat/tools")
    public String doChatWithTravelAppTools(String message, String chatId) {
        return travelApp.doChatWithTools(message, chatId);
    }

    /**
     * MCP 服务调用对话（高德地图：天气、路线、POI 等）
     */
    @GetMapping("/travel_app/chat/mcp")
    public String doChatWithTravelAppMcp(String message, String chatId) {
        return travelApp.doChatWithMcp(message, chatId);
    }

    /**
     * 全工具调用对话（本地工具 + 高德 MCP，统一入口）
     */
    @GetMapping("/travel_app/chat/full")
    public String doChatWithTravelAppFull(String message, String chatId) {
        return travelApp.doChatWithFull(message, chatId);
    }

    /**
     * 流式调用 Manus 超级智能体
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        MyManus myManus = new MyManus(allTools, dashscopeChatModel);
        return myManus.runStream(message);
    }
}
