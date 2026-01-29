package xyz.dreature.loat.domain.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.dreature.loat.domain.agent.model.ReActRequest;
import xyz.dreature.loat.domain.agent.model.ReActResponse;
import xyz.dreature.loat.domain.prompt.service.PromptService;
import xyz.dreature.loat.domain.tool.model.ExecutableTool;
import xyz.dreature.loat.domain.tool.registry.ToolRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ReActService {
    @Autowired
    private ChatModel chatModel;
    @Autowired
    private ChatMemoryProvider chatMemoryProvider;
    @Autowired
    private ToolRegistry toolRegistry;
    @Autowired
    private PromptService promptService;
    @Autowired
    private ObjectMapper objectMapper;

    // 手动处理流程（程序控制）
    public String process(ReActRequest request) {
        String conversationId = request.getConversationId();
        System.out.printf("\uD83D\uDE80 开始处理用户请求 | 会话 ID: %s%n", conversationId);

        // 获取对话记忆
        ChatMemory chatMemory = chatMemoryProvider.get(conversationId);

        // 定义保留消息（不被刷新）
        SystemMessage systemMessage = SystemMessage.from(promptService.buildSystemPrompt());
        UserMessage userMessage = UserMessage.from(promptService.buildUserPrompt(request));
        List<ChatMessage> reservedMessages = List.of(systemMessage, userMessage);

        // 若对话为新，则添加系统消息
        if (chatMemory.messages().isEmpty()) {
            chatMemory.add(systemMessage);
        }
        // 添加当前用户消息
        chatMemory.add(userMessage);


        int maxSteps = request.getMaxSteps();
        int refreshInterval = request.getRefreshInterval();

        // 进入“推理-行动-观察”流程，循环直至步骤上限
        for (int currentStep = 1; currentStep <= maxSteps; currentStep++) {
            System.out.printf("\uD83D\uDCCA 第 %d 步 | 最多步数：%d%n", currentStep, maxSteps);

            // 调用模型
            ChatResponse response = chatModel.chat(chatMemory.messages());
            AiMessage aiMessage = response.aiMessage();
            // 添加 AI 消息
            chatMemory.add(aiMessage);

            System.out.printf("\uD83E\uDD16 AI 响应：\n%s%n", aiMessage.text());

            try {
                // 解析 JSON 响应
                ReActResponse reActResponse = objectMapper.readValue(
                        aiMessage.text(), ReActResponse.class);

                if (reActResponse.hasError()) {
                    log.warn("❌ 响应格式错误: {}", reActResponse.getIncorrectAnswerFormat());
                    // 添加错误观察
                    String errorObs = String.format(
                            "{\"observation\": \"%s\"}",
                            reActResponse.getIncorrectAnswerFormat());
                    chatMemory.add(UserMessage.from(errorObs));
                    continue;
                }

                if (reActResponse.hasFinalAnswer()) {
                    System.out.printf("✅ 处理完成%n");
                    return reActResponse.getFinalAnswer();
                }

                if (reActResponse.hasAction()) {
                    List<Map<String, Object>> actions = reActResponse.getAction();
                    List<String> observations = new ArrayList<>();

                    for (Map<String, Object> action : actions) {
                        String toolName = (String) action.get("tool");
                        Map<String, Object> params = new HashMap<>(action);
                        params.remove("tool");

                        // 执行工具
                        Object result = executeTool(toolName, params);
                        observations.add(result.toString());

                        System.out.printf("\uD83D\uDEE0\uFE0F 工具执行结果 [%s]: %s%n", toolName, result);
                    }

                    // 添加观察结果
                    String observationJson = String.format(
                            "{\"observation\": \"%s\"}",
                            String.join("; ", observations));
                    chatMemory.add(UserMessage.from(observationJson));
                }

                if (refreshInterval > 0 && currentStep % refreshInterval == 0) {
                    refreshContext(chatMemory, reservedMessages);
                }
            } catch (Exception e) {
                log.error("JSON 解析异常 | 错误: {} | 原始响应: {}",
                        e.getMessage(), aiMessage.text(), e);
                // 添加错误观察
                String errorObs = String.format(
                        "{\"observation\": \"JSON 解析失败: %s\"}",
                        e.getMessage());
                chatMemory.add(UserMessage.from(errorObs));
            }
        }
        log.warn("⚠️ 达到最大步骤限制");
        return "任务未完成，已达到最大步骤数";
    }

    // 自动处理流程（框架托管，需模型原生支持 function calling 协议）
    public String processX(ReActRequest request) {
        String rolePrompt = promptService.buildRolePrompt();
        String environmentPrompt = promptService.buildEnvironmentPrompt();
        String userPrompt = promptService.buildUserPrompt(request);

        AgentService agentService = AiServices.builder(AgentService.class)
                .chatModel(chatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(toolRegistry.getToolComponents())
                .build();

        String result = agentService.react(
                request.getConversationId(),
                rolePrompt,
                environmentPrompt,
                userPrompt
        );

        return result;
    }

    // 清空上下文但保留关键信息
    public void refreshContext(ChatMemory chatMemory, List<ChatMessage> reservedMessages) {
        chatMemory.clear();
        chatMemory.add(reservedMessages);
    }

    // 执行工具
    public Object executeTool(String toolName, Map<String, Object> params) {
        try {
            // 1. 从注册中心获取可执行工具对象
            ExecutableTool tool = toolRegistry.get(toolName);
            if (tool == null) {
                log.error("未知工具: {}", toolName);
                return "未知工具: " + toolName;
            }
            // 2. 执行工具（具体的参数映射和调用逻辑封装在 ExecutableTool 内部）
            log.debug("执行工具: {} | 参数: {}", toolName, params);
            return tool.execute(params);
        } catch (Exception e) {
            log.error("工具执行异常 | 工具: {} | 参数: {} | 错误: {}",
                    toolName, params, e.getMessage(), e);
            return "工具执行失败: " + e.getMessage();
        }
    }
}
