package xyz.dreature.loat.domain.agent.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ChatService {
    // 系统提示词
    private static final SystemMessage SYSTEM_MESSAGE = new SystemMessage("你是智能聊天助手。");

    @Autowired
    private ChatLanguageModel chatModel;

    @Autowired
    private StreamingChatLanguageModel streamingChatModel;

    @Autowired
    private ChatMemoryStore chatMemoryStore;

    // 阻塞式对话
    public String chat(String conversationId, String userInput) {
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        // 获取历史记录作为上下文
        List<ChatMessage> messages = chatMemoryStore.getMessages(conversationId);

        // 若为新对话，则添加系统消息
        if (messages.isEmpty()) {
            messages.add(SYSTEM_MESSAGE);
        }

        // 添加用户消息
        UserMessage userMessage = new UserMessage(userInput);
        messages.add(userMessage);

        log.debug("对话 ID：{}，对话上下文：{}", conversationId, messages);

        // 获取回复
        ChatResponse response = chatModel.chat(messages);
        AiMessage aiMessage = response.aiMessage();
        String aiResponse = aiMessage.text();

        // 保存 AI 消息并回复
        messages.add(aiMessage);
        chatMemoryStore.updateMessages(conversationId, messages);
        return aiResponse;
    }

    // 流式对话
    public Flux<String> chatStream(String conversationId, String userInput) {
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        // 获取历史记录作为上下文
        List<ChatMessage> messages = chatMemoryStore.getMessages(conversationId);

        // 若为新对话，则添加系统消息
        if (messages.isEmpty()) {
            messages.add(SYSTEM_MESSAGE);
        }

        // 添加用户消息
        UserMessage userMessage = new UserMessage(userInput);
        messages.add(userMessage);

        log.debug("对话 ID：{}，对话上下文：{}", conversationId, messages);

        // 收集完整响应
        StringBuilder fullResponse = new StringBuilder();

        String finalConversationId = conversationId;
        return Flux.create(sink -> {
            try {
                //调用流式模型
                streamingChatModel.chat(messages, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String s) {
                        sink.next(s);
                        fullResponse.append(s);
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse chatResponse) {
                        messages.add(chatResponse.aiMessage());
                        chatMemoryStore.updateMessages(finalConversationId, messages);
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
