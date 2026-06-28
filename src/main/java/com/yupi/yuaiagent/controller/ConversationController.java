package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.chatmemory.FileBasedChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话历史管理接口
 */
@RestController
@RequestMapping("/conversations")
@Slf4j
public class ConversationController {

    @Resource
    private FileBasedChatMemory fileBasedChatMemory;

    /**
     * 获取所有对话列表（含预览信息）
     */
    @GetMapping
    public Result<List<Map<String, Object>>> listConversations() {
        List<String> ids = fileBasedChatMemory.listConversationIds();
        List<Map<String, Object>> list = new ArrayList<>();
        for (String id : ids) {
            FileBasedChatMemory.ConversationPreview preview = fileBasedChatMemory.getPreview(id);
            if (preview != null) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("conversationId", preview.conversationId());
                item.put("preview", preview.preview());
                item.put("messageCount", preview.messageCount());
                item.put("lastModified", preview.lastModified());
                list.add(item);
            }
        }
        return Result.ok(list);
    }

    /**
     * 获取某条对话的完整消息历史
     */
    @GetMapping("/{conversationId}")
    public Result<List<Map<String, Object>>> getConversation(
            @PathVariable String conversationId) {
        List<Message> messages = fileBasedChatMemory.get(conversationId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Message msg : messages) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", msg.getMessageType().name());
            item.put("content", msg.getText());
            list.add(item);
        }
        return Result.ok(list);
    }

    /**
     * 删除某条对话
     */
    @DeleteMapping("/{conversationId}")
    public Result<Void> deleteConversation(@PathVariable String conversationId) {
        fileBasedChatMemory.clear(conversationId);
        return Result.ok(null);
    }

    /**
     * 统一响应体
     */
    public record Result<T>(int code, String message, T data) {
        public static <T> Result<T> ok(T data) {
            return new Result<>(200, "success", data);
        }

        public static <T> Result<T> error(int code, String message) {
            return new Result<>(code, message, null);
        }
    }
}
