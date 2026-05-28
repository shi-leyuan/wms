package org.jeecg.modules.wms.ai.chat.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.common.util.*;
import org.jeecg.modules.airag.app.entity.AiragApp;
import org.jeecg.modules.airag.app.service.IAiragAppService;
import org.jeecg.modules.airag.app.vo.ChatSendParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;


@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class ChatController {

    @Resource(name = "chatClientOpenAi") //使用openai
    //@Resource(name = "chatClientOllama") //使用ollama
    public ChatClient chatClient;

    @Autowired
    private IAiragAppService airagAppService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ISysBaseAPI sysBaseApi;

    // 请求方式和路径不要改动，将来要与前端联调
    @RequestMapping("/chat")
    public String chat(String prompt) {
        return chatClient
                .prompt(prompt) // 传入user提示词
                .call() // 同步请求，会等待AI全部输出完才返回结果
                .content(); //返回响应内容
    }


    private String getUsername(HttpServletRequest httpRequest) {
        try {
            TokenUtils.getTokenByRequest();
            String token;
            if(null != httpRequest){
                token = TokenUtils.getTokenByRequest(httpRequest);
            }else{
                token = TokenUtils.getTokenByRequest();
            }
            if (TokenUtils.verifyToken(token, sysBaseApi, redisUtil)) {
                return JwtUtil.getUsername(token);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }


    @RequestMapping(value = "/chat/send")
    public SseEmitter send(@RequestBody ChatSendParams chatSendParams, HttpServletRequest httpRequest) {
        AssertUtils.assertNotEmpty("参数异常", chatSendParams);
        String userMessage = chatSendParams.getContent();
        AssertUtils.assertNotEmpty("至少发送一条消息", userMessage);
        //获取当前用户
        String username = getUsername(httpRequest);
        chatSendParams.setUsername( username);
        // 获取会话信息
        String conversationId = chatSendParams.getConversationId();
        String topicId = oConvertUtils.getString(chatSendParams.getTopicId(), UUIDGenerator.generate());
        chatSendParams.setTopicId(topicId);

        // 每次会话都生成一个新的,用来缓存emitter
        String requestId = UUIDGenerator.generate();

        SseEmitter emitter = new SseEmitter(-0L);
        // 发送初始思考提示消息
        try {
            // 发送 "> " 消息
            sendMessage(emitter, conversationId, topicId, requestId, "> ","MESSAGE");

            // 发送 "\n> " 消息
            sendMessage(emitter, conversationId, topicId, requestId, "\n> ","MESSAGE");

        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }
        Flux<String> flux = null;
        //准备传入参数
        String chatSendParamsJson = JSON.toJSONString(chatSendParams);
        // 获取app信息
        AiragApp app = null;
        if (oConvertUtils.isNotEmpty(chatSendParams.getAppId())) {
            app = airagAppService.getById(chatSendParams.getAppId());
        }
        if(app != null){
            //系统提示词
            String systemMessage = app.getPrompt();
            flux = chatClient
                    .prompt()
                    .user(chatSendParams.getContent())
                    .system(systemMessage)
                    .advisors(a->a.param(CHAT_MEMORY_CONVERSATION_ID_KEY,chatSendParamsJson))
                    .stream()
                    .content();
        }else{
            flux = chatClient
                    .prompt()
                    .user(chatSendParams.getContent())
                    .advisors(a->a.param(CHAT_MEMORY_CONVERSATION_ID_KEY,chatSendParamsJson))
                    .stream()
                    .content();
        }


        /**
         * 是否正在思考
         */
        AtomicBoolean isThinking = new AtomicBoolean(false);
        flux.subscribe(
                data -> {
                    try {
                        // 兼容推理模型
                        if ("<think>".equals(data)) {
                            isThinking.set(true);
                            data = "> ";
                        }
                        if ("</think>".equals(data)) {
                            isThinking.set(false);
                            data = "\n\n";
                        }
                        if (isThinking.get()) {
                            if (null != data && data.contains("\n")) {
                                data = "\n> ";
                            }
                        }

                        sendMessage(emitter, conversationId, topicId, requestId, data,"MESSAGE");
                        //保存会话

                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    try {
                        // 错误响应
                        sendMessage(emitter, conversationId, topicId, requestId, error.getMessage(),"ERROR");

                        // 发送结束消息
                        sendEndMessage(emitter, conversationId, topicId, requestId);
                    } catch (IOException ioException) {
                        emitter.completeWithError(ioException);
                    }
                },
                () -> {
                    try {
                        // 发送结束消息两次
                        sendEndMessage(emitter, conversationId, topicId, requestId);
                        sendEndMessage(emitter, conversationId, topicId, requestId);
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                }
        );

        return emitter;
    }


    // 辅助方法：向客户端发送消息
    private void sendMessage(SseEmitter emitter, String conversationId, String topicId,
                             String requestId, String message, String event) throws IOException {
        Map<String, Object> response = new HashMap<>();
        response.put("conversationId", conversationId);
        response.put("topicId", topicId);
        response.put("requestId", requestId);
        response.put("event", event);

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("message", message);
        response.put("data", messageData);

        String jsonData = JSONObject.toJSONString(response);
        emitter.send(SseEmitter.event().data(jsonData));
    }

    // 辅助方法：发送结束消息
    private void sendEndMessage(SseEmitter emitter, String conversationId, String topicId,
                                String requestId) throws IOException {
        Map<String, Object> endResponse = new HashMap<>();
        endResponse.put("event", "MESSAGE_END");
        endResponse.put("flowId", null);
        endResponse.put("requestId", requestId);
        endResponse.put("conversationId", conversationId);
        endResponse.put("topicId", topicId);
        endResponse.put("data", null);

        String endJson = JSONObject.toJSONString(endResponse);
        emitter.send(SseEmitter.event().data(endJson));
    }
}