package org.jeecg.modules.airag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfiguration {

    // 注意参数中的model就是使用的模型，这里用了Ollama
    @Bean
    public ChatClient chatClientOllama(OllamaChatModel model) {
        return ChatClient.builder(model) // 创建ChatClient工厂
                .build(); // 构建ChatClient实例

    }
    @Bean
    public ChatClient chatClientOpenAi(OpenAiChatModel model) {
        return ChatClient
                .builder(model)
                .build();
    }
}