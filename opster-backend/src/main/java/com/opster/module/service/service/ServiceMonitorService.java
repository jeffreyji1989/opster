package com.opster.module.service.service;

import com.opster.common.enums.RunStatus;
import com.opster.common.enums.Status;
import com.opster.module.service.entity.AppService;
import com.opster.module.service.repository.AppServiceRepository;
import com.opster.module.server.entity.Server;
import com.opster.module.server.repository.ServerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.Optional;

/**
 * 服务监控服务类
 * 定时检查服务状态并更新运行状态
 */
@Service
public class ServiceMonitorService implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(ServiceMonitorService.class);

    @Autowired
    private AppServiceRepository appServiceRepository;

    @Autowired
    private ServerRepository serverRepository;

    /**
     * 服务启动时初始化
     */
    @Override
    public void afterPropertiesSet() {
        logger.info("ServiceMonitorService initialized, starting service monitoring...");
        // 启动时立即执行一次监控
        monitorServices();
    }

    /**
     * 定时监控服务状态
     * 每隔1分钟执行一次
     */
    @Scheduled(cron = "0 */1 * * * *")
    public void scheduledMonitor() {
        monitorServices();
    }

    /**
     * 监控所有启用状态的服务
     */
    public void monitorServices() {
        try {
            logger.info("Starting service monitoring...");
            // 查询所有启用状态为1的服务
            List<AppService> services = appServiceRepository.findAll();
            services.stream()
                    .filter(service -> service.getStatus() != null && service.getStatus() == Status.ENABLED)
                    .forEach(this::checkServiceStatus);
            logger.info("Service monitoring completed.");
        } catch (Exception e) {
            logger.error("Error during service monitoring: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查单个服务的状态
     * @param service 服务对象
     */
    private void checkServiceStatus(AppService service) {
        try {
            logger.debug("Checking status for service: {} (ID: {})", service.getProjectId(), service.getId());

            // 获取服务器信息
            Optional<Server> serverOptional = serverRepository.findById(service.getServerId());
            if (!serverOptional.isPresent()) {
                logger.warn("Server not found for service ID: {}", service.getId());
                return;
            }

            Server server = serverOptional.get();
            String ip = server.getIp();
            Integer port = service.getPort();
            String monitorUrl = service.getMonitorUrl();

            boolean isAlive = false;

            // 优先使用监控地址进行检查
            if (monitorUrl != null && !monitorUrl.trim().isEmpty()) {
                logger.debug("Checking HTTP monitor URL: {}", monitorUrl);
                isAlive = checkHttpStatus(monitorUrl);
            }
            // 如果没有监控地址，使用IP+端口进行检查
            else if (ip != null && port != null) {
                logger.debug("Checking TCP port {}:{} for service ID: {}", ip, port, service.getId());
                isAlive = checkTcpPort(ip, port);
            } else {
                logger.warn("Service ID {} has no monitor URL or port configured, skipping status check", service.getId());
                return;
            }

            // 更新服务状态
            RunStatus newStatus = isAlive ? RunStatus.NORMAL : RunStatus.ABNORMAL; // 1-正常, 2-异常
            RunStatus currentStatus = service.getRunStatus();

            logger.info("Service status check result - ID: {}, MonitorUrl: {}, Port: {}, IsAlive: {}, CurrentStatus: {}, NewStatus: {}",
                    service.getId(), monitorUrl, port, isAlive, currentStatus, newStatus);

            if (currentStatus != newStatus) {
                service.setRunStatus(newStatus);
                appServiceRepository.save(service);
                logger.info("Service status updated: {} (ID: {}) - {}",
                        service.getProjectId(), service.getId(), isAlive ? "正常" : "异常");
            }

        } catch (Exception e) {
            logger.error("Error checking service status for ID {}: {}", service.getId(), e.getMessage(), e);
            // 发生异常时记录日志，但不修改状态（避免误判）
            logger.warn("Service status check failed for ID {}, keeping current status", service.getId());
        }
    }

    /**
     * 检查HTTP服务状态
     * @param urlStr 监控地址
     * @return 是否正常
     */
    private boolean checkHttpStatus(String urlStr) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);  // 增加连接超时到 5 秒
            connection.setReadTimeout(5000);     // 增加读取超时到 5 秒
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);  // 跟随重定向

            int responseCode = connection.getResponseCode();
            boolean isSuccess = responseCode >= 200 && responseCode < 400;

            logger.debug("HTTP check for {} - ResponseCode: {}, Success: {}", urlStr, responseCode, isSuccess);
            return isSuccess;
        } catch (IOException e) {
            logger.debug("HTTP check failed for {}: {}", urlStr, e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 检查TCP端口状态
     * @param ip IP地址
     * @param port 端口号
     * @return 是否正常
     */
    private boolean checkTcpPort(String ip, int port) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 5000);  // 增加超时到 5 秒
            boolean isSuccess = socket.isConnected() && socket.isBound();

            logger.debug("TCP port check for {}:{} - Success: {}", ip, port, isSuccess);
            return isSuccess;
        } catch (IOException e) {
            logger.debug("TCP check failed for {}:{}, {}", ip, port, e.getMessage());
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }
}
