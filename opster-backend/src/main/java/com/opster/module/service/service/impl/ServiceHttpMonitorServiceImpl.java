package com.opster.module.service.service.impl;

import com.opster.common.enums.RunStatus;
import com.opster.common.enums.Status;
import com.opster.module.project.entity.Project;
import com.opster.module.project.repository.ProjectRepository;
import com.opster.module.server.entity.Server;
import com.opster.module.server.repository.ServerRepository;
import com.opster.module.service.entity.AppService;
import com.opster.module.service.entity.ServiceMonitorHistory;
import com.opster.module.service.repository.AppServiceRepository;
import com.opster.module.service.repository.ServiceMonitorHistoryRepository;
import com.opster.module.service.service.ServiceHttpMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 服务HTTP监控服务实现
 */
@Service
public class ServiceHttpMonitorServiceImpl implements ServiceHttpMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceHttpMonitorServiceImpl.class);

    private static final int DEFAULT_TIMEOUT_MS = 5000;  // 默认超时时间5秒
    private static final int HISTORY_RETENTION_DAYS = 7;  // 历史记录保留天数

    @Autowired
    private AppServiceRepository appServiceRepository;

    @Autowired
    private ServiceMonitorHistoryRepository monitorHistoryRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ServerRepository serverRepository;

    @Override
    public void checkAndRecord(Integer serviceId) {
        try {
            // 获取服务信息
            Optional<AppService> serviceOpt = appServiceRepository.findById(serviceId);
            if (!serviceOpt.isPresent()) {
                logger.warn("服务不存在: {}", serviceId);
                return;
            }

            AppService service = serviceOpt.get();

            // 检查服务是否启用且配置了监控URL
            if (service.getStatus() != Status.ENABLED) {
                logger.debug("服务未启用，跳过监控: {}", serviceId);
                return;
            }

            String monitorUrl = service.getMonitorUrl();
            if (monitorUrl == null || monitorUrl.trim().isEmpty()) {
                logger.debug("服务未配置监控URL，跳过监控: {}", serviceId);
                return;
            }

            // 执行HTTP检查
            MonitorResult result = checkHttpStatus(monitorUrl);

            // 获取关联信息
            Optional<Project> projectOpt = projectRepository.findById(service.getProjectId());
            Optional<Server> serverOpt = serverRepository.findById(service.getServerId());

            // 创建监控记录
            ServiceMonitorHistory history = new ServiceMonitorHistory();
            history.setServiceId(serviceId);
            history.setProjectId(service.getProjectId());
            history.setServerId(service.getServerId());
            history.setProjectName(projectOpt.map(Project::getProjectName).orElse(null));
            history.setServiceName(service.getServiceName());
            history.setServerIp(serverOpt.map(Server::getIp).orElse(null));
            history.setPort(service.getPort());
            history.setMonitorUrl(monitorUrl);
            history.setStatus(result.success ? 1 : 0);
            history.setResponseTime(result.responseTime);
            history.setErrorMessage(result.errorMessage);
            history.setRecordTime(LocalDateTime.now());

            // 保存记录
            monitorHistoryRepository.save(history);

            // 更新服务运行状态
            RunStatus newStatus = result.success ? RunStatus.NORMAL : RunStatus.ABNORMAL;
            if (service.getRunStatus() != newStatus) {
                service.setRunStatus(newStatus);
                appServiceRepository.save(service);
            }

            logger.info("服务监控完成 - ID: {}, URL: {}, 状态: {}, 耗时: {}ms",
                    serviceId, monitorUrl, result.success ? "正常" : "异常", result.responseTime);

        } catch (Exception e) {
            logger.error("服务监控失败: {}", serviceId, e);
        }
    }

    @Override
    public ServiceMonitorHistory getLatestRecord(Integer serviceId) {
        return monitorHistoryRepository.findFirstByServiceIdOrderByRecordTimeDesc(serviceId).orElse(null);
    }

    @Override
    public List<ServiceMonitorHistory> getAllLatestRecords() {
        return monitorHistoryRepository.findAllLatestRecords();
    }

    @Override
    public List<ServiceMonitorHistory> getHistoryRecords(Integer serviceId, Integer days) {
        if (days == null || days <= 0) {
            days = HISTORY_RETENTION_DAYS;
        }
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days);
        return monitorHistoryRepository.findByServiceIdAndRecordTimeBetweenOrderByRecordTimeDesc(
                serviceId, startTime, endTime);
    }

    @Override
    public void cleanupOldRecords(Integer days) {
        if (days == null || days <= 0) {
            days = HISTORY_RETENTION_DAYS;
        }
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);
        monitorHistoryRepository.deleteByRecordTimeBefore(cutoffTime);
        logger.info("已清理 {} 天前的监控历史记录", days);
    }

    @Override
    public void checkAllServices() {
        List<AppService> services = appServiceRepository.findAll();
        services.stream()
                .filter(s -> s.getStatus() == Status.ENABLED)
                .filter(s -> s.getMonitorUrl() != null && !s.getMonitorUrl().trim().isEmpty())
                .forEach(s -> checkAndRecord(s.getId()));
    }

    @Override
    public List<Map<String, Object>> getAvailableServices() {
        List<AppService> services = appServiceRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (AppService service : services) {
            // 获取关联的项目和服务器信息
            Optional<Project> projectOpt = projectRepository.findById(service.getProjectId());
            Optional<Server> serverOpt = serverRepository.findById(service.getServerId());

            if (!projectOpt.isPresent() || !serverOpt.isPresent()) {
                continue;  // 跳过信息不完整的服务
            }

            Project project = projectOpt.get();
            Server server = serverOpt.get();

            Map<String, Object> serviceInfo = new HashMap<>();
            serviceInfo.put("id", service.getId());
            serviceInfo.put("serviceId", service.getId());
            serviceInfo.put("serviceAlias", service.getServiceAlias());
            serviceInfo.put("serviceName", service.getServiceName());
            serviceInfo.put("projectId", project.getId());
            serviceInfo.put("projectName", project.getProjectName());
            serviceInfo.put("projectCode", project.getProjectCode());
            serviceInfo.put("serverId", server.getId());
            serviceInfo.put("serverIp", server.getIp());
            serviceInfo.put("port", service.getPort());
            serviceInfo.put("monitorUrl", service.getMonitorUrl());
            serviceInfo.put("env", service.getEnv());
            serviceInfo.put("status", service.getStatus());
            serviceInfo.put("runStatus", service.getRunStatus());

            result.add(serviceInfo);
        }

        return result;
    }

    @Override
    public void updateMonitorUrl(Integer serviceId, String monitorUrl) {
        Optional<AppService> serviceOpt = appServiceRepository.findById(serviceId);
        if (!serviceOpt.isPresent()) {
            throw new IllegalArgumentException("服务不存在: " + serviceId);
        }

        AppService service = serviceOpt.get();
        service.setMonitorUrl(monitorUrl);
        appServiceRepository.save(service);

        logger.info("更新服务监控URL - 服务ID: {}, 新URL: {}", serviceId, monitorUrl);
    }

    /**
     * HTTP状态检查结果
     */
    private static class MonitorResult {
        boolean success;
        int responseTime;
        String errorMessage;
    }

    /**
     * 检查HTTP服务状态
     * @param urlStr 监控地址
     * @return 监控结果
     */
    private MonitorResult checkHttpStatus(String urlStr) {
        MonitorResult result = new MonitorResult();
        HttpURLConnection connection = null;
        long startTime = System.currentTimeMillis();

        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(DEFAULT_TIMEOUT_MS);
            connection.setReadTimeout(DEFAULT_TIMEOUT_MS);
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);

            int responseCode = connection.getResponseCode();
            result.responseTime = (int) (System.currentTimeMillis() - startTime);
            result.success = responseCode >= 200 && responseCode < 400;

            if (!result.success) {
                result.errorMessage = "HTTP状态码: " + responseCode;
            }

        } catch (IOException e) {
            result.responseTime = (int) (System.currentTimeMillis() - startTime);
            result.success = false;
            result.errorMessage = e.getMessage();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return result;
    }
}