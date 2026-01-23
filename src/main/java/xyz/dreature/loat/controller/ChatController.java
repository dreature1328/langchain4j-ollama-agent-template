package xyz.dreature.loat.controller;

import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import xyz.dreature.loat.service.ChatService;

// 操作接口（对话）
@RestController
public class ChatController {
    // 对话服务
    @Autowired
    ChatService chatService;

    // 阻塞式对话
    @RequestMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public String chat(
            @RequestParam(name = "conversation-id", required = false)
            String conversationId,

            @RequestParam(name = "user-input")
            @NotBlank(message = "输入不能为空")
            String userInput
    ) {
        return chatService.chat(conversationId, userInput);
    }


    @RequestMapping(value = "/chat-stream", produces = "text/html;charset=utf-8")
    public Flux<String> chatStream(
            @RequestParam(name = "conversation-id", required = false)
            String conversationId,

            @RequestParam(name = "user-input")
            @NotBlank(message = "输入不能为空")
            String userInput
    ) {
        return chatService.chatStream(conversationId, userInput);
    }
}
