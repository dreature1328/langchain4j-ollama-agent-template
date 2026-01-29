package xyz.dreature.loat.domain.agent.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

// ReAct 请求
public class ReActRequest {
    // ===== 字段 =====
    // 对话 ID
    private String conversationId = UUID.randomUUID().toString();

    // 用户输入
    @NotBlank(message = "用户输入不能为空")
    private String userInput;

    // ReAct 参数
    @Min(value = 1, message = "最大步数至少为 1")
    @Max(value = 100, message = "最大步数至多为 100")
    private int maxSteps = 10;

    @Min(value = 0, message = "刷新间隔不能为负")
    private int refreshInterval = 0; // 0 表示不启用刷新

    // ===== 构造方法 =====
    // 无参构造器
    public ReActRequest() {
    }

    // 基础构造器
    public ReActRequest(String conversationId, String userInput) {
        this.conversationId = conversationId;
        this.userInput = userInput;
    }

    // ===== Getter 与 Setter 方法 =====
    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    public int getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(int refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    // ===== 其他 =====
    // 字符串表示
    @Override
    public String toString() {
        return "ReActRequest{" +
                "conversationId='" + conversationId + '\'' +
                ", userInput='" + userInput + '\'' +
                ", maxSteps=" + maxSteps +
                ", refreshInterval=" + refreshInterval +
                '}';
    }
}
