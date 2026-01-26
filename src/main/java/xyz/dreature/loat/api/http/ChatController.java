package xyz.dreature.loat.api.http;

import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import xyz.dreature.loat.domain.agent.service.ChatService;

// 操作接口（对话）
@RestController
@RequestMapping("/chat")
@Validated
public class ChatController {
    // 对话服务
    @Autowired
    ChatService chatService;

    // 阻塞式对话
    @RequestMapping(value = "/complete", produces = "text/html;charset=utf-8")
    public ResponseEntity<String> chat(
            @RequestParam(name = "conversation-id", required = false)
            String conversationId,

            @RequestParam(name = "user-input")
            @NotBlank(message = "输入不能为空")
            String userInput
    ) {
        return ResponseEntity.ok(chatService.chat(conversationId, userInput));
    }

    // 流式对话
    @RequestMapping(value = "/complete-stream", produces = "text/html;charset=utf-8")
    public ResponseEntity<Flux<String>> chatStream(
            @RequestParam(name = "conversation-id", required = false)
            String conversationId,

            @RequestParam(name = "user-input")
            @NotBlank(message = "输入不能为空")
            String userInput
    ) {
        return ResponseEntity.ok(chatService.chatStream(conversationId, userInput));
    }
}
