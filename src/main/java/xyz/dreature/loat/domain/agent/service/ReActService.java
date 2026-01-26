package xyz.dreature.loat.domain.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
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
    private ChatLanguageModel model;
    @Autowired
    private ChatMemoryStore chatMemoryStore;
    @Autowired
    private ToolRegistry toolRegistry;
    @Autowired
    private PromptService promptService;
    @Autowired
    private ObjectMapper objectMapper;

    private int interaction;

    // 处理流程
    public String process(ReActRequest request) {
        System.out.printf("\uD83D\uDE80 开始处理用户请求 | 会话 ID: %s%n", request.getConversationId());

        // 获取历史记录作为上下文
        List<ChatMessage> messages = chatMemoryStore.getMessages(request.getConversationId());

        // 若对话为新，则添加系统消息
        if (messages.isEmpty()) {
            messages.add(SystemMessage.from(promptService.buildSystemPrompt()));
        }
        // 添加用户消息
        messages.add(UserMessage.from(promptService.buildUserPrompt(request)));

        // 进入“推理-行动-观察”流程，循环直至步骤上限
        for (int step = 0; step < request.getMaxSteps(); step++, interaction++) {
            System.out.printf("\uD83D\uDCCA 第 %d 步 | 最多步数：%d%n", step + 1, request.getMaxSteps());

            // 调用模型
            ChatResponse response = model.chat(messages);
            AiMessage aiMessage = response.aiMessage();
            // 添加 AI 消息
            messages.add(aiMessage);

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
                    messages.add(UserMessage.from(errorObs));
                    continue;
                }

                if (reActResponse.hasFinalAnswer()) {
                    System.out.printf("✅ 处理完成 | 总交互次数: %d%n", this.interaction + 1);
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
                    messages.add(UserMessage.from(observationJson));
                }

            } catch (Exception e) {
                log.error("JSON 解析异常 | 错误: {} | 原始响应: {}",
                        e.getMessage(), aiMessage.text(), e);
                // 添加错误观察
                String errorObs = String.format(
                        "{\"observation\": \"JSON 解析失败: %s\"}",
                        e.getMessage());
                messages.add(UserMessage.from(errorObs));
            }

            chatMemoryStore.updateMessages(request.getConversationId(), messages);
        }
        log.warn("⚠️ 达到最大步骤限制");
        return "任务未完成，已达到最大步骤数";
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
