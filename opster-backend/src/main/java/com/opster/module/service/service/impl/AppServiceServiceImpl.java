package com.opster.module.service.service.impl;

import cn.hutool.core.util.StrUtil;
import com.jcraft.jsch.Session;
import com.opster.common.SshUtils;
import com.opster.common.enums.RepositoryType;
import com.opster.common.enums.RunStatus;
import com.opster.common.enums.Status;
import com.opster.module.project.entity.Project;
import com.opster.module.project.repository.ProjectRepository;
import com.opster.module.server.entity.Server;
import com.opster.module.server.repository.ServerRepository;
import com.opster.module.service.entity.AppService;
import com.opster.module.service.repository.AppServiceRepository;
import com.opster.module.service.service.AppServiceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

@Slf4j
@Service
@Transactional
public class AppServiceServiceImpl implements AppServiceService {

    @Autowired
    private AppServiceRepository appServiceRepository;

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Value("${opster.maven-home}")
    private String mavenHome;

    @Value("${opster.java-home}")
    private String javaHome;

    @Value("${opster.deploy-path}")
    private String deployPath;

    @Override
    public List<AppService> findAll() {
        return appServiceRepository.findAll();
    }

    @Override
    public List<AppService> findList(Integer projectId, String businessLine, String env, Integer runStatus, Integer status) {
        // 先查询所有服务
        List<AppService> services = appServiceRepository.findAll();
        
        // 过滤条件
        return services.stream()
                // 项目ID
                .filter(service -> projectId == null || service.getProjectId().equals(projectId))
                // 业务线
                .filter(service -> {
                    if (businessLine == null || businessLine.isEmpty()) {
                        return true;
                    }
                    // 查询项目信息
                    Optional<Project> projectOptional = projectRepository.findById(service.getProjectId());
                    return projectOptional.isPresent() && projectOptional.get().getBusinessLine() != null && 
                           projectOptional.get().getBusinessLine().contains(businessLine);
                })
                // 环境
                .filter(service -> env == null || env.isEmpty() || service.getEnv() != null && service.getEnv().equals(env))
                // 运行状态
                .filter(service -> runStatus == null || service.getRunStatus() != null && service.getRunStatus().equals(RunStatus.fromCode(runStatus)))
                // 启用状态
                .filter(service -> status == null || service.getStatus() != null && service.getStatus().equals(Status.fromCode(status)))
                .collect(Collectors.toList());
    }

    @Override
    public Page<AppService> findPage(Pageable pageable) {
        return appServiceRepository.findAll(pageable);
    }

    @Override
    public Optional<AppService> findById(Integer id) {
        return appServiceRepository.findById(id);
    }

    @Override
    public AppService save(AppService service) {
        boolean isNew = service.getId() == null;

        // 自动计算源码目录（仅在新增时或源码目录为空时计算）
        if (isNew || service.getSourcePath() == null || service.getSourcePath().isEmpty()) {
            String computedSourcePath = computeSourcePath(service);
            service.setSourcePath(computedSourcePath);
        }

        // 自动提取仓库别名（仅在新增时或 serviceRepoAlias 为空时）
        if (isNew || (service.getServiceRepoAlias() == null || service.getServiceRepoAlias().isEmpty())) {
            String gitUrl = service.getRepoGitUrl();
            if (StrUtil.isBlank(gitUrl) && service.getProjectId() != null) {
                // 尝试从项目配置获取
                Optional<Project> projectOptional = projectRepository.findById(service.getProjectId());
                if (projectOptional.isPresent()) {
                    Project project = projectOptional.get();
                    if (project.getRepositories() != null && !project.getRepositories().isEmpty()) {
                        gitUrl = project.getRepositories().get(0).getGitUrl();
                    }
                }
            }

            if (StrUtil.isNotBlank(gitUrl)) {
                String repoAlias = extractServiceRepoAliasFromGitUrl(gitUrl);
                service.setServiceRepoAlias(repoAlias);
                log.info("自动提取服务仓库别名：{} (从 Git URL: {})", repoAlias, gitUrl);
            }
        }

        // 如果 serviceType 为空，尝试从 repositoryType 获取（兼容旧数据）
        if (service.getServiceType() == null && service.getRepositoryType() != null) {
            service.setServiceType(service.getRepositoryType());
        }

        // ========== 自动修正构建命令 ==========
        // 优先使用已设置的 buildScript
        if (service.getBuildScript() == null || service.getBuildScript().isEmpty()) {
            // 如果 buildScript 为空，尝试从旧字段获取
            if (service.getMavenCmd() != null && !service.getMavenCmd().isEmpty()) {
                service.setBuildScript(service.getMavenCmd());
            } else if (service.getBuildCmd() != null && !service.getBuildCmd().isEmpty()) {
                service.setBuildScript(service.getBuildCmd());
            } else {
                // 如果旧字段也为空，根据 serviceType 自动设置默认构建命令
                if (service.getServiceType() != null) {
                    if (service.getServiceType() == 1) {
                        // 后端项目：使用 Maven
                        service.setBuildScript("mvn clean package -DskipTests");
                    } else {
                        // 前端项目（0-前端, 2-管理后台, 3-移动端）：使用 npm
                        service.setBuildScript("npm install && npm run build");
                    }
                }
            }
        }

        // ========== 验证构建命令与服务类型是否匹配 ==========
        if (service.getServiceType() != null && service.getBuildScript() != null && !service.getBuildScript().isEmpty()) {
            boolean isFrontend = service.getServiceType() != 1; // 0, 2, 3 是前端
            boolean isMavenCommand = service.getBuildScript().trim().toLowerCase().startsWith("mvn");

            if (isFrontend && isMavenCommand) {
                log.warn("服务 ID={} 的服务类型为前端，但构建命令为 Maven 命令，已自动修正为 npm 命令", service.getId());
                service.setBuildScript("npm install && npm run build");
            } else if (!isFrontend && !isMavenCommand) {
                log.warn("服务 ID={} 的服务类型为后端，但构建命令为 npm 命令，已自动修正为 Maven 命令", service.getId());
                service.setBuildScript("mvn clean package -DskipTests");
            }
        }

        AppService savedService = appServiceRepository.save(service);

        // Update server deployed count
        if (isNew) {
            updateServerDeployedCount(savedService.getServerId(), 1);
        }

        return savedService;
    }

    /**
     * 计算源码目录
     * 公式：{deployPath}/{projectCode}/source/{serviceRepoAlias}
     * 兼容旧数据：如果 serviceRepoAlias 为空，使用旧路径 {deployPath}/{projectCode}/source
     */
    private String computeSourcePath(AppService service) {
        if (service.getProjectId() == null) {
            return null;
        }

        // 获取项目信息
        Optional<Project> projectOptional = projectRepository.findById(service.getProjectId());
        if (projectOptional.isEmpty()) {
            return null;
        }

        Project project = projectOptional.get();
        String projectCode = project.getProjectCode();

        if (projectCode == null || projectCode.isEmpty()) {
            return null;
        }

        // 获取 Git URL 以提取仓库别名
        String gitUrl = service.getRepoGitUrl();
        if (StrUtil.isBlank(gitUrl)) {
            // 尝试从项目配置获取
            if (project.getRepositories() != null && !project.getRepositories().isEmpty()) {
                gitUrl = project.getRepositories().get(0).getGitUrl();
            }
        }

        // 尝试提取仓库别名
        String repoAlias = service.getServiceRepoAlias();
        if (StrUtil.isBlank(repoAlias) && StrUtil.isNotBlank(gitUrl)) {
            repoAlias = extractServiceRepoAliasFromGitUrl(gitUrl);
            // 自动填充到服务对象
            service.setServiceRepoAlias(repoAlias);
        }

        // 构建路径
        if (StrUtil.isNotBlank(repoAlias)) {
            // 新路径：{deployPath}/{projectCode}/source/{repoAlias}
            return deployPath + "/" + projectCode + "/source/" + repoAlias;
        } else {
            // 旧路径（兼容旧数据）：{deployPath}/{projectCode}/source
            return deployPath + "/" + projectCode + "/source";
        }
    }

    @Override
    public void deleteById(Integer id) {
        Optional<AppService> serviceOpt = appServiceRepository.findById(id);
        if (serviceOpt.isPresent()) {
            updateServerDeployedCount(serviceOpt.get().getServerId(), -1);
            appServiceRepository.deleteById(id);
        }
    }

    private void updateServerDeployedCount(Integer serverId, int delta) {
        if (serverId != null) {
            serverRepository.findById(serverId).ifPresent(server -> {
                int count = server.getDeployedCount() == null ? 0 : server.getDeployedCount();
                server.setDeployedCount(Math.max(0, count + delta));
                serverRepository.save(server);
            });
        }
    }

    @Override
    public long count() {
        return appServiceRepository.count();
    }

    @Override
    public String compileAndRestart(Integer id) {
        return "Please use WebSocket for this operation";
    }

    @Override
    public String restart(Integer id) {
        return "Please use WebSocket for this operation";
    }

    @Override
    public String start(Integer id) {
        return "Please use WebSocket for this operation";
    }

    @Override
    public String viewLog(Integer id) {
        Optional<AppService> serviceOpt = findById(id);
        if (serviceOpt.isEmpty()) return "Service not found";
        AppService service = serviceOpt.get();
        
        Optional<Server> serverOpt = serverRepository.findById(service.getServerId());
        if (serverOpt.isEmpty()) return "Server not found";
        Server server = serverOpt.get();

        if (StrUtil.isBlank(service.getLogPath())) {
            return "Log path not configured";
        }
        
        StringBuilder logs = new StringBuilder();
        Session session = null;
        try {
            // SshUtils.connect 内部会自动解密密码
            session = SshUtils.connect(server.getIp(), 22, server.getUsername(), server.getPassword());
            String cmd = "tail -n 100 " + service.getLogPath();
            logs.append(SshUtils.exec(session, cmd));
        } catch (Exception e) {
            logs.append("ERROR: ").append(e.getMessage());
        } finally {
            SshUtils.disconnect(session);
        }
        
        return logs.toString();
    }

    @Override
    public long countByStatus(Integer status) {
        return appServiceRepository.countByStatus(Status.fromCode(status));
    }

    @Override
    public List<Map<String, Object>> searchMonitor(Integer projectId, String ip) {
        List<AppService> allServices = appServiceRepository.findAll();
        
        // Filter in memory for simplicity (or use Specification for complex queries)
        return allServices.stream()
                .filter(service -> {
                    boolean matchProject = (projectId == null || service.getProjectId().equals(projectId));
                    boolean matchIp = true;
                    if (StrUtil.isNotBlank(ip)) {
                        Optional<Server> server = serverRepository.findById(service.getServerId());
                        matchIp = server.isPresent() && server.get().getIp().contains(ip);
                    }
                    return matchProject && matchIp;
                })
                .map(service -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", service.getId());
                    // 使用 runStatus（运行状态）而非 status（启用状态）
                    map.put("status", service.getRunStatus() != null ? service.getRunStatus().getCode() : RunStatus.NOT_STARTED.getCode());
                    map.put("updateTime", service.getUpdateTime());

                    projectRepository.findById(service.getProjectId()).ifPresent(p -> {
                        map.put("projectName", p.getProjectName());
                        // 监控地址从服务配置获取，不再从项目配置获取
                        map.put("monitorUrl", service.getMonitorUrl());
                    });

                    serverRepository.findById(service.getServerId()).ifPresent(s -> {
                        map.put("serverIp", s.getIp());
                        map.put("serverAlias", s.getAlias());
                    });

                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void uploadStartScript(Integer id, String scriptContent) throws Exception {
        // 获取服务配置
        Optional<AppService> serviceOpt = findById(id);
        if (serviceOpt.isEmpty()) {
            throw new Exception("服务不存在");
        }
        AppService service = serviceOpt.get();

        // 获取服务器信息
        Optional<Server> serverOpt = serverRepository.findById(service.getServerId());
        if (serverOpt.isEmpty()) {
            throw new Exception("服务器不存在");
        }
        Server server = serverOpt.get();

        // 获取项目信息
        Optional<Project> projectOpt = projectRepository.findById(service.getProjectId());
        if (projectOpt.isEmpty()) {
            throw new Exception("项目不存在");
        }
        Project project = projectOpt.get();

        // 使用服务配置的部署路径
        String deployDir = service.getDeployPath();
        if (StrUtil.isBlank(deployDir)) {
            throw new IllegalArgumentException("部署路径不能为空（请在服务配置中填写部署路径）");
        }
        String scriptFile = deployDir + "/start.sh";

        Session session = null;
        try {
            session = SshUtils.connect(server.getIp(), 22, server.getUsername(), server.getPassword());

            // 检查并创建部署目录（如果不存在）
            String mkdirCmd = "mkdir -p " + deployDir;
            SshUtils.exec(session, mkdirCmd);

            // 检查脚本是否已上传过
            boolean alreadyUploaded = service.getScriptUploaded() != null && service.getScriptUploaded() == 1;

            if (alreadyUploaded) {
                // 如果已上传过，先备份旧脚本
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String backupFile = scriptFile + "." + timestamp;
                SshUtils.exec(session, "mv " + scriptFile + " " + backupFile);
            }

            // 使用 cat 命令创建文件
            String createScriptCmd = "cat > " + scriptFile + " << 'EOF_SCRIPT'\n" +
                    scriptContent + "\n" +
                    "EOF_SCRIPT";

            SshUtils.exec(session, createScriptCmd);

            // 添加执行权限
            SshUtils.exec(session, "chmod +x " + scriptFile);

            // 更新上传状态
            service.setScriptUploaded(1);
            appServiceRepository.save(service);

        } finally {
            SshUtils.disconnect(session);
        }
    }

    /**
     * 确定 Git URL（用于服务）
     */
    private String determineGitUrlForService(AppService service, Project project) {
        // 优先使用服务的Git URL
        if (StrUtil.isNotBlank(service.getRepoGitUrl())) {
            return service.getRepoGitUrl();
        }

        // 从项目的repositories中查找
        if (project.getRepositories() != null && !project.getRepositories().isEmpty()) {
            RepositoryType repoType = service.getRepositoryType() != null ?
                RepositoryType.values()[service.getRepositoryType()] : RepositoryType.BACKEND;

            return project.getRepositories().stream()
                .filter(r -> r.getType() == repoType)
                .findFirst()
                .map(com.opster.module.project.dto.RepositoryDTO::getGitUrl)
                .orElse(project.getRepositories().get(0).getGitUrl());
        }

        throw new RuntimeException("未找到Git仓库地址");
    }

    /**
     * 从 Git URL 提取服务仓库别名（用于源码目录结构）
     * 支持多种 Git URL 格式：
     * - HTTPS: https://github.com/xxx/opster-backend.git → opster-backend
     * - HTTP: http://git.example.com/project/frontend.git → frontend
     * - SSH: git@github.com:xxx/repo.git → repo
     *
     * @param gitUrl Git 仓库地址
     * @return 服务仓库别名（仓库名，不含 .git 后缀）
     */
    private String extractServiceRepoAliasFromGitUrl(String gitUrl) {
        if (StrUtil.isBlank(gitUrl)) {
            return null;
        }

        try {
            String repoName = "";

            // 处理 SSH 格式：git@github.com:xxx/repo.git
            if (gitUrl.startsWith("git@")) {
                int colonIndex = gitUrl.indexOf(':');
                if (colonIndex > 0) {
                    String pathPart = gitUrl.substring(colonIndex + 1);
                    String[] parts = pathPart.split("/");
                    repoName = parts[parts.length - 1];
                }
            }
            // 处理 HTTPS/HTTP 格式
            else {
                String urlWithoutProtocol = gitUrl.replaceFirst("^https?://", "");
                int atIndex = urlWithoutProtocol.indexOf('@');
                if (atIndex > 0) {
                    urlWithoutProtocol = urlWithoutProtocol.substring(atIndex + 1);
                }
                String[] parts = urlWithoutProtocol.split("/");
                repoName = parts[parts.length - 1];
            }

            // 移除 .git 后缀
            if (repoName.endsWith(".git")) {
                repoName = repoName.substring(0, repoName.length() - 4);
            }

            // 移除其他可能的特殊字符
            repoName = repoName.replaceAll("[^a-zA-Z0-9_-]", "");

            return StrUtil.isBlank(repoName) ? null : repoName;

        } catch (Exception e) {
            log.warn("从 Git URL 提取服务仓库别名失败：{}, 使用默认值", gitUrl, e);
            return null;
        }
    }

    /**
     * 从 Git URL 提取服务别名（仓库名）
     */
    private String extractServiceAliasFromGitUrl(String gitUrl) {
        if (StrUtil.isBlank(gitUrl)) {
            return "service";
        }

        try {
            String repoName = "";

            // 处理 SSH 格式：git@github.com:xxx/repo.git
            if (gitUrl.startsWith("git@")) {
                int colonIndex = gitUrl.indexOf(':');
                if (colonIndex > 0) {
                    String pathPart = gitUrl.substring(colonIndex + 1);
                    String[] parts = pathPart.split("/");
                    repoName = parts[parts.length - 1];
                }
            }
            // 处理 HTTPS/HTTP 格式
            else {
                // 移除协议部分
                String urlWithoutProtocol = gitUrl.replaceFirst("^https?://", "");
                // 移除认证信息（如：username:password@）
                int atIndex = urlWithoutProtocol.indexOf('@');
                if (atIndex > 0) {
                    urlWithoutProtocol = urlWithoutProtocol.substring(atIndex + 1);
                }
                // 按 / 分割，获取最后一部分
                String[] parts = urlWithoutProtocol.split("/");
                repoName = parts[parts.length - 1];
            }

            // 移除 .git 后缀
            if (repoName.endsWith(".git")) {
                repoName = repoName.substring(0, repoName.length() - 4);
            }

            // 移除其他可能的特殊字符
            repoName = repoName.replaceAll("[^a-zA-Z0-9_-]", "");

            return StrUtil.isBlank(repoName) ? "service" : repoName;

        } catch (Exception e) {
            log.warn("从 Git URL 提取服务别名失败: {}, 使用默认值", gitUrl, e);
            return "service";
        }
    }
}
