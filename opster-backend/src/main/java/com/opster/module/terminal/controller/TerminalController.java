package com.opster.module.terminal.controller;

import com.opster.module.terminal.service.LinuxCommandAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 终端控制器
 */
@RestController
@RequestMapping("/api/terminal")
public class TerminalController {

    private final LinuxCommandAIService linuxCommandAIService;

    @Autowired
    public TerminalController(LinuxCommandAIService linuxCommandAIService) {
        this.linuxCommandAIService = linuxCommandAIService;
    }

    /**
     * 生成Linux命令
     * @param description 命令描述
     * @return 推荐命令列表
     */
    @PostMapping("/generate-command")
    public List<String> generateCommand(@RequestBody String description) {
        List<String> commands = new ArrayList<>();
        
        try {
            // 使用AI生成Linux命令
            String aiResponse = linuxCommandAIService.generateCommand(description);
            
            // 解析AI返回的命令（每行一个命令）
            String[] lines = aiResponse.split("\\n");
            for (String line : lines) {
                String command = line.trim();
                if (!command.isEmpty()) {
                    commands.add(command);
                }
            }
            
            // 确保返回3个命令，如果AI返回的命令不足3个，补充默认命令
            if (commands.isEmpty()) {
                commands.add("echo \"Hello, World!\"");
                commands.add("pwd");
                commands.add("whoami");
            } else if (commands.size() < 3) {
                while (commands.size() < 3) {
                    commands.add("echo \"Command not found\"");
                }
            } else if (commands.size() > 3) {
                // 如果AI返回的命令超过3个，只取前3个
                commands = commands.subList(0, 3);
            }
        } catch (Exception e) {
            // 异常情况下返回默认命令
            commands.add("echo \"Hello, World!\"");
            commands.add("pwd");
            commands.add("whoami");
        }
        
        return commands;
    }
}