package com.yupi.yuaiagent.config;

import com.yupi.yuaiagent.chatmemory.FileBasedChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 对话记忆 Bean 配置
 */
@Configuration
public class ChatMemoryConfig {

    @Bean
    public FileBasedChatMemory fileBasedChatMemory() {
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        return new FileBasedChatMemory(fileDir);
    }
}
