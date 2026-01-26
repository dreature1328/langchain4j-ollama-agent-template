package xyz.dreature.loat.domain.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

// ReAct 响应
public class ReActResponse {
    @JsonProperty("question")
    private String question;

    @JsonProperty("thought")
    private String thought;

    @JsonProperty("action")
    private List<Map<String, Object>> action;

    @JsonProperty("observation")
    private String observation;

    @JsonProperty("final_answer")
    private String finalAnswer;

    @JsonProperty("incorrect_answer_format")
    private String incorrectAnswerFormat;

    // ===== 构造方法 =====
    // 无参构造器
    public ReActResponse() {
    }

    // ===== Getter 与 Setter 方法 =====
    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getThought() {
        return thought;
    }

    public void setThought(String thought) {
        this.thought = thought;
    }

    public List<Map<String, Object>> getAction() {
        return action;
    }

    public void setAction(List<Map<String, Object>> action) {
        this.action = action;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public String getFinalAnswer() {
        return finalAnswer;
    }

    public void setFinalAnswer(String finalAnswer) {
        this.finalAnswer = finalAnswer;
    }

    public String getIncorrectAnswerFormat() {
        return incorrectAnswerFormat;
    }

    public void setIncorrectAnswerFormat(String incorrectAnswerFormat) {
        this.incorrectAnswerFormat = incorrectAnswerFormat;
    }

    // ===== 判断方法 =====
    public boolean hasAction() {
        return action != null && !action.isEmpty();
    }

    public boolean hasFinalAnswer() {
        return finalAnswer != null && !finalAnswer.isEmpty();
    }

    public boolean hasError() {
        return incorrectAnswerFormat != null && !incorrectAnswerFormat.isEmpty();
    }

    // ===== 其他 =====
    // 字符串表示
    @Override
    public String toString() {
        return "ReActResponse{" +
                "question='" + question + '\'' +
                ", thought='" + thought + '\'' +
                ", action=" + action +
                ", observation='" + observation + '\'' +
                ", finalAnswer='" + finalAnswer + '\'' +
                ", incorrectAnswerFormat='" + incorrectAnswerFormat + '\'' +
                '}';
    }
}
