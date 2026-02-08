package com.opster.module.service.transfer.checker;

import com.jcraft.jsch.Session;
import com.opster.common.SshUtils;
import com.opster.config.OpsterProperties;
import com.opster.module.server.entity.Server;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * 传输前检查器
 * 在文件传输前执行各种检查，确保传输可以成功进行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PreTransferChecker {

    private final OpsterProperties opsterProperties;

    /**
     * 执行所有检查
     *
     * @param localFile 本地文件
     * @param server 服务器信息
     * @param session SSH Session
     * @return 检查结果
     */
    public TransferCheckResult check(Path localFile, Server server, Session session) {
        TransferCheckResult result = new TransferCheckResult();

        if (!opsterProperties.getTransfer().getPreCheck().getEnabled()) {
            log.info("传输前检查已禁用，跳过所有检查");
            return result;
        }

        log.info("开始传输前检查...");

        // 1. 检查本地文件
        checkLocalFile(localFile, result);

        // 2. 检查服务器磁盘空间
        if (opsterProperties.getTransfer().getPreCheck().getCheckDiskSpace()) {
            checkDiskSpace(server, localFile, session, result);
        }

        // 3. 检查服务器负载
        if (opsterProperties.getTransfer().getPreCheck().getCheckServerLoad()) {
            checkServerLoad(session, result);
        }

        log.info("传输前检查完成: {}", result.isPassed() ? "通过" : "失败");
        log.debug("检查详情:\n{}", result.getSummary());

        return result;
    }

    /**
     * 检查本地文件
     */
    private void checkLocalFile(Path localFile, TransferCheckResult result) {
        try {
            if (!Files.exists(localFile)) {
                result.addCheck("本地文件存在性", false, "文件不存在: " + localFile);
                return;
            }

            long fileSize = Files.size(localFile);
            if (fileSize == 0) {
                result.addCheck("本地文件大小", false, "文件大小为 0");
                return;
            }

            result.addCheck("本地文件", true,
                String.format("文件存在，大小: %.2f MB", fileSize / 1024.0 / 1024.0));

        } catch (Exception e) {
            result.addCheck("本地文件", false, "检查失败: " + e.getMessage());
        }
    }

    /**
     * 检查服务器磁盘空间
     */
    private void checkDiskSpace(Server server, Path localFile, Session session,
                               TransferCheckResult result) {
        try {
            // 获取本地文件大小
            long fileSize = Files.size(localFile);

            // 执行 df 命令获取磁盘使用情况
            String output = SshUtils.exec(session, "df -h / | tail -1");
            String[] parts = output.trim().split("\\s+");

            if (parts.length >= 5) {
                String usedPercent = parts[4].replace("%", "");
                int used = Integer.parseInt(usedPercent);
                int freePercent = 100 - used;

                double minFreeRatio = opsterProperties.getTransfer().getPreCheck().getMinFreeSpaceRatio();
                int requiredFreePercent = (int) (minFreeRatio * 100);

                if (freePercent >= requiredFreePercent) {
                    result.addCheck("服务器磁盘空间", true,
                        String.format("可用空间: %d%%", freePercent));
                } else {
                    result.addCheck("服务器磁盘空间", false,
                        String.format("可用空间不足: %d%% (需要至少 %d%%)", freePercent, requiredFreePercent));
                }
            } else {
                result.addCheck("服务器磁盘空间", false, "无法解析磁盘信息");
            }

        } catch (Exception e) {
            result.addCheck("服务器磁盘空间", false, "检查失败: " + e.getMessage());
        }
    }

    /**
     * 检查服务器负载
     */
    private void checkServerLoad(Session session, TransferCheckResult result) {
        try {
            // 执行 uptime 命令获取负载信息
            String output = SshUtils.exec(session, "uptime");

            // 解析负载: "load average: 0.50, 0.60, 0.70"
            Pattern loadPattern = Pattern.compile("load average: ([\\d.]+), ([\\d.]+), ([\\d.]+)");
            java.util.regex.Matcher matcher = loadPattern.matcher(output);

            if (matcher.find()) {
                double load1min = Double.parseDouble(matcher.group(1));
                double maxLoad = opsterProperties.getTransfer().getPreCheck().getMaxLoadAverage();

                if (load1min <= maxLoad) {
                    result.addCheck("服务器负载", true,
                        String.format("1分钟平均负载: %.2f", load1min));
                } else {
                    result.addCheck("服务器负载", false,
                        String.format("服务器负载过高: %.2f (最大: %.2f)", load1min, maxLoad));
                }
            } else {
                result.addCheck("服务器负载", false, "无法解析负载信息");
            }

        } catch (Exception e) {
            result.addCheck("服务器负载", false, "检查失败: " + e.getMessage());
        }
    }
}
