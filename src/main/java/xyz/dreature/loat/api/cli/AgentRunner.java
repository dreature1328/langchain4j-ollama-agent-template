package xyz.dreature.loat.api.cli;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import xyz.dreature.loat.domain.agent.model.ReActRequest;
import xyz.dreature.loat.domain.agent.service.ReActService;

import java.util.List;
import java.util.Scanner;
import java.util.UUID;

@Component
@Order(1) // 启动顺序
public class AgentRunner implements CommandLineRunner {
    @Autowired
    private ChatMemoryStore chatMemoryStore;
    @Autowired
    private ReActService reActService;

    // 当前对话 ID
    private String conversationId = UUID.randomUUID().toString();

    // 在 Spring Boot 完全启动后自动执行
    @Override
    public void run(String... args) {
        System.out.printf("🤖 智能体启动%n");
        System.out.printf("💡 输入 /help 查看可用命令%n");
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("💬 请输入内容：");
                String userInput = scanner.nextLine().trim();

                if (userInput.isEmpty()) {
                    continue;
                }

                if (userInput.startsWith("/")) {
                    if (!handleCommand(userInput.toLowerCase())) break;
                    continue;
                }

                String response = reActService.process(new ReActRequest(conversationId, userInput));
                System.out.printf("✅ 回复：%s%n", response);
            }
        } finally {
            System.exit(0);
        }
    }

    // 处理命令
    private boolean handleCommand(String command) {
        String[] parts = command.split("\\s+"); // 按空格分割命令和参数

        switch (parts[0]) {
            case "/new":
                // 开始新对话
                conversationId = UUID.randomUUID().toString();
                System.out.printf("🆕 新对话：%s%n", conversationId);
                return true;

            case "/use":
                // 使用指定对话 ID
                if (parts.length > 1) {
                    conversationId = parts[1];
                    System.out.printf("🔄 切换到对话：%s%n", conversationId);
                } else {
                    System.out.printf("❌ 请指定对话 ID: /use <conversation-id>%n");
                }
                return true;

            case "/history":
                printConversationHistory();
                return true;

            case "/help":
                printHelp();
                return true;

            case "/quit":
                System.out.printf("👋 再见！%n");
                return false;

            default:
                System.out.printf("❌ 未知命令，输入 /help 查看可用命令%n");
                return true;
        }
    }

    // 打印对话历史
    private void printConversationHistory() {
        List<ChatMessage> messages = chatMemoryStore.getMessages(conversationId);
        if (messages.isEmpty()) {
            System.out.printf("当前对话没有历史消息%n");
        } else {
            System.out.printf("💬 对话历史%n");
            for (int i = 0; i < messages.size(); i++) {
                ChatMessage message = messages.get(i);
                if (message instanceof SystemMessage) {
                    System.out.printf("%d. ⚙️ 系统提示：%s%n", i, ((SystemMessage) message).text());
                } else if (message instanceof UserMessage) {
                    System.out.printf("%d. 👤 用户：%s%n", i, ((UserMessage) message).singleText());
                } else if (message instanceof AiMessage) {
                    System.out.printf("%d. 🤖 AI：%s%n", i, ((AiMessage) message).text());
                }
            }
        }
    }

    // 打印帮助说明
    private void printHelp() {
        System.out.printf("""
                🤖 可用命令：
                  /new          开始新对话
                  /use <conversation-id>     切换到指定对话
                  /help         显示帮助
                  /quit         退出程序%n
                """);
    }
}
