package com.yupi.yuaiagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于文件持久化的对话记忆
 */
@Slf4j
public class FileBasedChatMemory implements ChatMemory {

    private static final int MAX_MESSAGES = 50;
    private final String BASE_DIR;
    private static final Kryo kryo = new Kryo();

    static {
        kryo.setRegistrationRequired(false);
        // 设置实例化策略
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    // 构造对象时，指定文件保存目录
    public FileBasedChatMemory(String dir) {
        this.BASE_DIR = dir;
        File baseDir = new File(dir);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> conversationMessages = getOrCreateConversation(conversationId);
        conversationMessages.addAll(messages);
        // 滑动窗口：超过上限则截断，只保留最近的消息
        if (conversationMessages.size() > MAX_MESSAGES) {
            conversationMessages = new ArrayList<>(
                    conversationMessages.subList(conversationMessages.size() - MAX_MESSAGES,
                            conversationMessages.size()));
        }
        saveConversation(conversationId, conversationMessages);
    }

    @Override
    public List<Message> get(String conversationId) {
        return getOrCreateConversation(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            if (file.delete()) {
                log.info("已删除对话文件: {}", conversationId);
            }
        }
    }

    /**
     * 列出所有已保存的对话 ID，按文件修改时间倒序（最新的在前）
     */
    public List<String> listConversationIds() {
        File baseDir = new File(BASE_DIR);
        File[] files = baseDir.listFiles((dir, name) -> name.endsWith(".kryo"));
        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }
        return Arrays.stream(files)
                .sorted(Comparator.comparingLong(File::lastModified).reversed())
                .map(file -> file.getName().replace(".kryo", ""))
                .collect(Collectors.toList());
    }

    /**
     * 获取对话的预览信息：最后一条用户消息 + 消息总数 + 最后修改时间
     */
    public ConversationPreview getPreview(String conversationId) {
        File file = getConversationFile(conversationId);
        if (!file.exists()) {
            return null;
        }
        List<Message> messages = getOrCreateConversation(conversationId);
        if (messages.isEmpty()) {
            return new ConversationPreview(conversationId, "空对话", 0, file.lastModified());
        }
        // 从后往前找最后一条用户消息
        String lastUserMessage = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("USER".equals(messages.get(i).getMessageType().name())) {
                lastUserMessage = messages.get(i).getText();
                break;
            }
        }
        // 截断预览文本
        if (lastUserMessage.length() > 50) {
            lastUserMessage = lastUserMessage.substring(0, 50) + "...";
        }
        if (lastUserMessage.isEmpty()) {
            lastUserMessage = "对话中...";
        }
        return new ConversationPreview(conversationId, lastUserMessage, messages.size(),
                file.lastModified());
    }

    /**
     * 对话预览信息
     */
    public record ConversationPreview(String conversationId, String preview, int messageCount,
                                      long lastModified) {
    }

    private List<Message> getOrCreateConversation(String conversationId) {
        File file = getConversationFile(conversationId);
        List<Message> messages = new ArrayList<>();
        if (file.exists()) {
            try (Input input = new Input(new FileInputStream(file))) {
                messages = kryo.readObject(input, ArrayList.class);
            } catch (Exception e) {
                log.error("反序列化对话文件失败: {}, 清空重建", conversationId, e);
                messages = new ArrayList<>();
            }
        }
        return messages;
    }

    private void saveConversation(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            kryo.writeObject(output, messages);
        } catch (IOException e) {
            log.error("保存对话文件失败: {}", conversationId, e);
        }
    }

    private File getConversationFile(String conversationId) {
        return new File(BASE_DIR, conversationId + ".kryo");
    }
}
