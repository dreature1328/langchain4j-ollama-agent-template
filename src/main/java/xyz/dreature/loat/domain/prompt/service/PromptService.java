package xyz.dreature.loat.domain.prompt.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.dreature.loat.domain.agent.model.ReActRequest;
import xyz.dreature.loat.domain.tool.registry.ToolRegistry;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class PromptService {
    @Autowired
    ToolRegistry toolRegistry;

    // 构建系统提示词
    public String buildSystemPrompt() {
        String systemPrompt = """
                # 角色定位
                %s
                # 回答格式
                %s
                # 规则约束
                %s
                # 可用工具
                %s
                # 环境信息
                %s
                """.formatted(
                buildRolePrompt(),
                buildFormatPrompt(),
                buildRulePrompt(),
                toolRegistry.getDescription(),
                buildEnvironmentPrompt()
        );

        log.debug("系统提示词：\n{}", systemPrompt);
        return systemPrompt;
    }

    // 构建【角色定位】提示词
    public String buildRolePrompt() {
        String personaPrompt = """
                你是 AI 助手，你的任务是帮助用户解决问题并执行必要的操作。
                - 性格特点：做事认真严谨又有点俏皮，做错事时会诚恳道歉。
                """;

        return personaPrompt;
    }

    // 构建【回答格式】提示词
    public String buildFormatPrompt() {
        String formatPrompt = """
                你必须严格使用 JSON 格式回复，结构如下：

                {
                  "question": "用户的问题",
                  "thought": "你的思考过程",
                  "action": [{"tool": "工具名", "参数1": "值1", "参数2": "值2"}],
                  "observation": "工具执行后的观察结果",
                  "final_answer": "最终答案"
                }
                """;

        return formatPrompt;
    }

    // 构建【规则约束】提示词
    public String buildRulePrompt() {
        String rulePrompt = """
                1. 必须包含 thought 字段值，描述你的思考过程
                2. 如需使用工具，在 action 字段中指定值（支持多个工具）
                3. 不要生成 observation 字段值，我会提供工具执行结果
                4. 只有在获取所有工具结果后，才提供 final_answer 字段值
                5. 文件路径使用绝对路径
                6. 多行参数使用 \\n 表示换行
                7. 只返回 JSON 对象，不要添加任何额外文本
                """;

        return rulePrompt;
    }

    // 构建【环境信息】提示词
    public String buildEnvironmentPrompt() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            osName = "Windows";
        } else if (osName.contains("mac")) {
            osName = "MacOS";
        } else if (osName.contains("nix") || osName.contains("nux")) {
            osName = "Linux";
        } else {
            osName = "Unknown";
        }

        String environmentPrompt = """
                - 当前时间：%s
                - 操作系统：%s
                """.formatted(
                LocalDateTime.now().format(formatter),
                osName
        );

        return environmentPrompt;
    }

    // 构建用户提示词
    public String buildUserPrompt(ReActRequest request) {
        String userPrompt = """
                # 用户问题
                %s
                """.formatted(
                request.getUserInput()
        );

        log.debug("用户提示词：\n{}", userPrompt);
        return userPrompt;
    }
}
