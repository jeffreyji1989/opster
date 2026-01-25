package com.opster.module.monitor.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.jcraft.jsch.Session;
import com.opster.common.SshUtils;
import com.opster.module.monitor.entity.ServerMetric;
import com.opster.module.monitor.entity.ServerMetricDTO;
import com.opster.module.monitor.repository.ServerMetricRepository;
import com.opster.module.monitor.service.ServerMonitorService;
import com.opster.module.server.entity.Server;
import com.opster.module.server.repository.ServerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ServerMonitorServiceImpl implements ServerMonitorService {

    private static final Logger log = LoggerFactory.getLogger(ServerMonitorServiceImpl.class);

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private ServerMetricRepository serverMetricRepository;

    @Override
    public ServerMetricDTO getCurrentMetric(Integer serverId) {
        Server server = serverRepository.findById(serverId).orElseThrow(() -> new RuntimeException("Server not found"));
        
        Session session = null;
        try {
            // SshUtils.connect 内部会自动解密密码
            session = SshUtils.connect(server.getIp(), 22, server.getUsername(), server.getPassword());
            
            // Execute compound command
            // 1. CPU: top -bn1 | grep "Cpu(s)"
            // 2. Mem: free -m | grep "Mem:"
            // 3. Disk: df -h
            String cmd = "top -bn1 | grep \"Cpu(s)\" && free -m | grep \"Mem:\" && df -h";
            String output = SshUtils.exec(session, cmd);
            
            return parseOutput(output);

        } catch (Exception e) {
            log.error("Failed to monitor server: {}", server.getIp(), e);
            throw new RuntimeException("Monitor failed: " + e.getMessage());
        } finally {
            SshUtils.disconnect(session);
        }
    }

    @Override
    public void saveMetricSnapshot(Integer serverId, ServerMetricDTO dto) {
        ServerMetric entity = new ServerMetric();
        entity.setServerId(serverId);
        entity.setCpuUsage(dto.getCpuUsage());
        entity.setMemoryTotal(dto.getMemoryTotal());
        entity.setMemoryUsed(dto.getMemoryUsed());
        entity.setMemoryUsage(dto.getMemoryUsage());
        entity.setDiskUsageJson(JSONUtil.toJsonStr(dto.getDiskInfos()));
        entity.setRecordTime(LocalDateTime.now());
        
        serverMetricRepository.save(entity);
    }

    @Override
    public List<ServerMetric> getHistory(Integer serverId, String range) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(1); // Default 1 hour
        
        if ("24h".equals(range)) {
            start = end.minusHours(24);
        }
        
        return serverMetricRepository.findByServerIdAndRecordTimeBetweenOrderByRecordTimeAsc(serverId, start, end);
    }

    private ServerMetricDTO parseOutput(String output) {
        ServerMetricDTO dto = new ServerMetricDTO();
        dto.setTime(DateUtil.date().toString("HH:mm:ss"));
        
        String[] lines = output.split("\n");
        List<ServerMetricDTO.DiskInfo> disks = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Parse CPU
            // %Cpu(s):  1.0 us,  1.0 sy,  0.0 ni, 98.0 id, ...
            if (line.startsWith("%Cpu(s)") || line.startsWith("Cpu(s)")) {
                try {
                    // Extract idle value using regex
                    Pattern p = Pattern.compile("(\\d+\\.\\d+)\\s*id");
                    Matcher m = p.matcher(line);
                    if (m.find()) {
                        double idle = Double.parseDouble(m.group(1));
                        dto.setCpuUsage(Math.round((100.0 - idle) * 100.0) / 100.0);
                    } else {
                        // Fallback simple parsing if regex fails (simplified)
                        dto.setCpuUsage(0.0);
                    }
                } catch (Exception e) {
                    log.warn("Parse CPU error: " + line);
                }
            }
            
            // Parse Memory
            // Mem:           7961        1234         500 ...
            else if (line.startsWith("Mem:")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    try {
                        long total = Long.parseLong(parts[1]);
                        long used = Long.parseLong(parts[2]);
                        dto.setMemoryTotal(total);
                        dto.setMemoryUsed(used);
                        if (total > 0) {
                            dto.setMemoryUsage(Math.round((double) used / total * 100.0 * 100.0) / 100.0);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Parse Mem error: " + line);
                    }
                }
            }
            
            // Parse Disk
            // /dev/sda1       50G   20G   30G  40% /
            else if (line.startsWith("/")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 6) {
                    ServerMetricDTO.DiskInfo disk = new ServerMetricDTO.DiskInfo();
                    disk.setMountedOn(parts[parts.length - 1]); // Last is mount point
                    disk.setSize(parts[1]);
                    disk.setUsed(parts[2]);
                    disk.setAvail(parts[3]);
                    disk.setUsePercent(parts[4]);
                    disks.add(disk);
                }
            }
        }
        
        dto.setDiskInfos(disks);
        return dto;
    }
}
