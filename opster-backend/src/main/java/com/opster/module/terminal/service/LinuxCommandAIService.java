package com.opster.module.terminal.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Linux命令AI生成服务
 */
@Service
public class LinuxCommandAIService {

    private final DashScopeChatModel chatModel;

    @Autowired
    public LinuxCommandAIService(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String generateCommand(String description) {
        String userPrompt = buildUserPrompt(description);
        
        try {
            // 调用AI模型生成命令
            String aiResponse = chatModel.call(userPrompt);
            
            if (aiResponse != null && !aiResponse.isEmpty()) {
                // 安全过滤：移除可能的 Markdown 包裹（如 ```bash ... ```）
                String command = aiResponse.replaceAll("^(?s)```(?:bash|sh)?\\s*(.*?)\\s*```$", "$1").trim();
                return command;
            }
            
            throw new RuntimeException("AI returned empty response.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate command from AI: " + e.getMessage(), e);
        }
    }
    
    private String buildUserPrompt(String description) {
        return "你是一个Linux命令专家，请根据用户的描述生成5个最相关的Linux命令。\n" +
                    "用户描述：" + description + "\n" +
                    "要求：\n" +
                    "1. 只返回命令本身，不要包含任何解释或说明\n" +
                    "2. 每个命令占一行\n" +
                    "3. 确保命令语法正确\n" +
                    "4. 按照相关性从高到低排序\n" +
                    "5. 不要包含任何额外的文本\n" +
                    "6. 只返回3个命令\n" +
                    "\n" +
                    "示例输出：\n" +
                    "ps aux\n" +
                    "top\n" +
                    "ps -ef | grep java";
    }
}