# 基于 Langchain4j + Ollama 的智能体交互模板

本项目是针对智能体交互的模板工程，基于Langchain4j + Ollama，提供智能体对话理解与任务执行的实现方案（目前仅基础对话），支持对话历史记忆，便于初学者接触场景。

## 模型选择

借由 Ollama 拉取本地模型（如 `deepseek-r1:8b`）。

## 对话响应

阻塞式对话和流式对话。

## 对话历史

通过对话 ID （`conversationId`）区分对话，目前仅支持内存存储对话历史。

## 启动流程

1. **AI 模型配置**：编辑 `application.properties` 文件，配置 AI 模型源参数

3. **项目启动**：运行 `Application.java` 主类，启动 Spring Boot 应用
4. **接口测试**：发起请求调用控制层接口，进行聊天
