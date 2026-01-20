package com.opster.module.terminal.service;

import org.springframework.stereotype.Component;

/**
 * Linux命令提示模板
 */
@Component
public class CommandPromptTemplate {

    public String buildUserPrompt(String description) {
        return "你是一个Linux命令专家，请根据用户的描述生成3个最相关的Linux命令。\n" +
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