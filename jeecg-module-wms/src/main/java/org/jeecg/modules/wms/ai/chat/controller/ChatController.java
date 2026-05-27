package org.jeecg.modules.wms.ai.chat.controller;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class ChatController {

    //@Resource(name = "chatClientOpenAi") //使用openai
    @Resource(name = "chatClientOllama") //使用ollama
    public  ChatClient chatClient;
    
    // 请求方式和路径不要改动，将来要与前端联调
    @RequestMapping("/chat")
    public String chat(String prompt) {
        return chatClient
                .prompt(prompt) // 传入user提示词
                .call() // 同步请求，会等待AI全部输出完才返回结果
                .content(); //返回响应内容
    }
}