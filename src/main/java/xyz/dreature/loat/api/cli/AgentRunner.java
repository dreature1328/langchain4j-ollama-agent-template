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
    // 当前处理模式
    private int currentMode = 0;

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

                String response;
                ReActRequest request = new ReActRequest(conversationId, userInput);
                if (currentMode == 1) {
                    System.out.printf("🔄 使用自动模式（框架托管）%n");
                    response = reActService.processX(request);
                } else {
                    System.out.printf("🔄 使用手动模式（程序控制）%n");
                    response = reActService.process(request);
                }

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
            case "/new":    // 开始新对话
                conversationId = UUID.randomUUID().toString();
                System.out.printf("🆕 新对话：%s%n", conversationId);
                return true;

            case "/use":    // 使用指定对话 ID
                if (parts.length > 1) {
                    conversationId = parts[1];
                    System.out.printf("🔄 切换到对话：%s%n", conversationId);
                } else {
                    System.out.printf("❌ 请指定对话 ID: /use <conversation-id>%n");
                }
                return true;

            case "/mode":
                if (parts.length > 1) {    // 切换处理模式
                    changeMode(parts[1]);
                } else {    // 显示当前模式
                    printModeInfo();
                }
                return true;

            case "/history":    // 显示对话历史
                printConversationHistory();
                return true;

            case "/help":    // 显示帮助说明
                printHelp();
                return true;

            case "/exit":
                System.out.printf("👋 再见！%n");
                return false;

            default:
                System.out.printf("❌ 未知命令，输入 /help 查看可用命令%n");
                return true;
        }
    }

    // 打印模式信息
    private void printModeInfo() {
        System.out.printf("""
                🤖 当前处理模式: %d
                📝 模式说明:
                  • 0：手动模式，程序控制 ReAct 流程，适用于基础推理模型
                  • 1：自动模式，框架托管 ReAct 流程，适用于进阶模型（支持 function calling 协议）
                使用 /mode <mode> 切换模式%n
                """, currentMode);
    }

    // 打印对话历史
    private void changeMode(String mode) {
        switch (mode) {
            case "0":
                currentMode = 0;
                System.out.printf("🔄 切换至手动模式（程序控制）%n");
                break;
            case "1":
                currentMode = 1;
                System.out.printf("🔄 切换至自动模式（框架托管）%n");
                break;
            default:
                System.out.printf("❌ 无效的模式%n");
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
                  /mode [0|1]   展示或切换处理模式
                  /help         显示帮助
                  /exit         退出程序%n
                """);
    }
}
