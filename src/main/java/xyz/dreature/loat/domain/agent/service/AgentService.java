package xyz.dreature.loat.domain.agent.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface AgentService {
    @SystemMessage("""
            # 角色定位
            {{rolePrompt}}
            # 环境信息
            {{environmentPrompt}}
            """)
    String react(
            @MemoryId String conversationId,
            @V("rolePrompt") String rolePrompt,
            @V("environmentPrompt") String environmentPrompt,
            @UserMessage String userInput
    );
}
