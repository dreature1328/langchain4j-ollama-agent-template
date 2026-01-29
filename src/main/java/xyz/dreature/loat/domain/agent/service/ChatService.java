package xyz.dreature.loat.domain.agent.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Slf4j
@Service
public class ChatService {
    // 系统提示词
    private final SystemMessage SYSTEM_MESSAGE = new SystemMessage("你是智能聊天助手。");
    @Autowired
    private ChatModel chatModel;
    @Autowired
    private StreamingChatModel streamingChatModel;
    @Autowired
    private ChatMemoryProvider chatMemoryProvider;

    // 阻塞式对话
    public String chat(String conversationId, String userInput) {
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        // 获取对话记忆
        ChatMemory chatMemory = chatMemoryProvider.get(conversationId);

        // 若为新对话，则添加系统消息
        if (chatMemory.messages().isEmpty()) {
            chatMemory.add(SYSTEM_MESSAGE);
        }

        // 添加用户消息
        UserMessage userMessage = new UserMessage(userInput);
        chatMemory.add(userMessage);

        log.debug("对话 ID：{}，对话上下文：{}", conversationId, chatMemory.messages());

        // 获取回复
        ChatResponse response = chatModel.chat(chatMemory.messages());
        AiMessage aiMessage = response.aiMessage();
        String aiResponse = aiMessage.text();

        // 保存 AI 消息并回复
        chatMemory.add(aiMessage);
        return aiResponse;
    }

    // 流式对话
    public Flux<String> chatStream(String conversationId, String userInput) {
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        // 获取对话记忆
        ChatMemory chatMemory = chatMemoryProvider.get(conversationId);

        // 若为新对话，则添加系统消息
        if (chatMemory.messages().isEmpty()) {
            chatMemory.add(SYSTEM_MESSAGE);
        }

        // 添加用户消息
        UserMessage userMessage = new UserMessage(userInput);
        chatMemory.add(userMessage);

        log.debug("对话 ID：{}，对话上下文：{}", conversationId, chatMemory.messages());

        // 收集完整响应
        StringBuilder fullResponse = new StringBuilder();

        return Flux.create(sink -> {
            try {
                //调用流式模型
                streamingChatModel.chat(chatMemory.messages(), new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String s) {
                        sink.next(s);
                        fullResponse.append(s);
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse chatResponse) {
                        chatMemory.add(chatResponse.aiMessage());
                        sink.complete();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        log.error("AI 流式调用失败", throwable);
                        sink.error(throwable);
                    }
                });

            } catch (Exception e) {
                log.error("聊天处理异常", e);
                sink.error(e);
            }
        });
    }
}
