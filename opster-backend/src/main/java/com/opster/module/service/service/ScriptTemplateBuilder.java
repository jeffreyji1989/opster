package com.opster.module.service.service;

import org.springframework.stereotype.Component;

/**
 * 启动脚本模板构建器
 * 用于生成标准化的 Spring Boot 启动脚本
 */
@Component
public class ScriptTemplateBuilder {

    /**
     * 构建标准启动脚本
     *
     * @param name           脚本名称
     * @param versionNo      版本号
     * @param port           服务端口
     * @param jvmArgs        JVM 参数
     * @param preJavaCmd     Java 命令前的命令
     * @param postJavaCmd    Java 命令后的命令
     * @return 完整的 Shell 脚本内容
     */
    public String buildStandardScript(String name, String versionNo, Integer port,
                                       String jvmArgs, String preJavaCmd, String postJavaCmd) {
        // 转义单引号
        String escapedName = escapeShell(name);
        String escapedVersion = escapeShell(versionNo);
        String escapedJvmArgs = escapeShell(jvmArgs);
        String escapedPreCmd = escapeShell(preJavaCmd);
        String escapedPostCmd = escapeShell(postJavaCmd);

        return String.format(SCRIPT_TEMPLATE,
                escapedName,
                escapedVersion,
                port,
                escapedJvmArgs,
                escapedPreCmd,
                escapedPostCmd
        );
    }

    /**
     * 转义 Shell 特殊字符
     * 主要用于处理单引号
     */
    private String escapeShell(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        // 转义单引号：' -> '\\''
        return str.replace("'", "'\\''");
    }

    /**
     * 标准 Spring Boot 启动脚本模板
     */
    private static final String SCRIPT_TEMPLATE = """
            #!/bin/bash
            # ============================================================
            # Spring Boot 应用启动脚本
            # 自动生成于 Opster 平台
            # ============================================================
            # 脚本名称：%1$s
            # 版本号：%2$s
            # 使用方式：sh start.sh (无需传递参数)
            # ============================================================

            # ========== 配置区 ==========
            PORT="%3$s"
            JVM_ARGS="%4$s"
            PRE_JAVA_CMD="%5$s"
            POST_JAVA_CMD="%6$s"
            JAR_NAME="*.jar"
            LOG_DIR="logs"
            PID_FILE="app.pid"
            STARTUP_WAIT_TIME=30

            # ========== 颜色定义 ==========
            RED='\\033[0;31m'
            GREEN='\\033[0;32m'
            YELLOW='\\033[1;33m'
            BLUE='\\033[0;34m'
            NC='\\033[0m'

            # ========== 日志函数 ==========
            log_info() { echo -e "${GREEN}[$(date '+%%Y-%%m-%%d %%H:%%M:%%S')] INFO: $1${NC}"; }
            log_error() { echo -e "${RED}[$(date '+%%Y-%%m-%%d %%H:%%M:%%S')] ERROR: $1${NC}"; }
            log_warn() { echo -e "${YELLOW}[$(date '+%%Y-%%m-%%d %%H:%%M:%%S')] WARN: $1${NC}"; }
            log_step() { echo -e "${BLUE}[$(date '+%%Y-%%m-%%d %%H:%%M:%%S')] STEP: $1${NC}"; }

            # ========== 检查 Java 环境 ==========
            check_java() {
                log_step "检查 Java 环境..."
                if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
                    export PATH="$JAVA_HOME/bin:$PATH"
                    log_info "使用 JAVA_HOME: $JAVA_HOME"
                    return 0
                fi
                if command -v java &> /dev/null; then
                    log_info "使用系统 Java: $(java -version 2>&1 | head -1)"
                    return 0
                fi
                log_error "Java 未安装，请设置 JAVA_HOME"
                exit 1
            }

            # ========== 检查并清理端口 ==========
            check_and_kill_port() {
                log_step "检查端口 $PORT 占用情况..."
                local pid=$(lsof -ti:$PORT 2>/dev/null)
                if [ -n "$pid" ]; then
                    log_warn "端口 $PORT 被占用 (PID: $pid)"
                    if ps -p "$pid" -o command= 2>/dev/null | grep -q "java"; then
                        log_info "尝试优雅停止 Java 进程..."
                        kill "$pid" 2>/dev/null && sleep 2 && kill -9 "$pid" 2>/dev/null || true
                        sleep 1
                    else
                        log_error "端口被非 Java 进程占用，请手动处理"
                        exit 1
                    fi
                    if lsof -ti:$PORT > /dev/null 2>&1; then
                        log_error "端口仍未释放"
                        exit 1
                    fi
                    log_info "端口 $PORT 已释放"
                fi
            }

            # ========== 执行前置命令 ==========
            execute_pre_java_cmd() {
                if [ -n "$PRE_JAVA_CMD" ]; then
                    log_step "执行启动前命令：$PRE_JAVA_CMD"
                    eval "$PRE_JAVA_CMD" 2>&1 || log_warn "前置命令执行失败，继续启动"
                fi
            }

            # ========== 启动 Java 应用 ==========
            start_java_app() {
                log_step "启动 Java 应用..."
                local jar_file=$(ls -1 $JAR_NAME 2>/dev/null | head -1)
                if [ -z "$jar_file" ]; then
                    log_error "未找到 jar 文件"
                    exit 1
                fi
                log_info "找到 jar 文件：$jar_file"

                mkdir -p "$LOG_DIR"
                local java_cmd="java $JVM_ARGS -jar $jar_file"
                log_info "启动命令：$java_cmd"

                nohup $java_cmd > "$LOG_DIR/startup.log" 2>&1 &
                local app_pid=$!
                echo $app_pid > "$PID_FILE"
                log_info "服务已启动 (PID: $app_pid)"

                log_step "等待服务启动（最多 ${STARTUP_WAIT_TIME} 秒）..."
                local waited=0
                while [ $waited -lt $STARTUP_WAIT_TIME ]; do
                    if ! ps -p "$app_pid" > /dev/null 2>&1; then
                        log_error "服务进程已退出"
                        exit 1
                    fi
                    if lsof -ti:$PORT > /dev/null 2>&1; then
                        log_info "端口 $PORT 开始监听"
                        break
                    fi
                    sleep 1
                    waited=$((waited + 1))
                done
            }

            # ========== 执行后置命令 ==========
            execute_post_java_cmd() {
                if [ -n "$POST_JAVA_CMD" ]; then
                    log_step "执行启动后命令：$POST_JAVA_CMD"
                    eval "$POST_JAVA_CMD" 2>&1 || log_warn "后置命令执行失败"
                fi
            }

            # ========== 主流程 ==========
            main() {
                log_info "========================================"
                log_info "启动 Spring Boot 服务"
                log_info "端口：$PORT"
                log_info "========================================"
                check_java
                check_and_kill_port
                execute_pre_java_cmd
                start_java_app
                execute_post_java_cmd
                log_info "========================================"
                log_info "服务启动完成"
                log_info "========================================"
            }

            main
            """;
}
