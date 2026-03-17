-- ============================================================
-- 启动脚本管理模块 - 插入默认脚本数据
-- 创建时间：2026-03-15
-- ============================================================

-- 插入默认脚本主表记录
INSERT INTO service_start_script (id, name, description, port, is_default, current_version_id, create_time, update_time)
VALUES (1, '标准 Spring Boot 启动脚本', '适用于大多数 Spring Boot 应用，支持端口清理、JVM 参数配置、前后置命令', 8080, 1, 1, datetime('now'), datetime('now'));

-- 插入默认脚本版本记录 (脚本内容简化版)
INSERT INTO service_start_script_version (
    script_id, version_no, version_description, port, jvm_args, pre_java_cmd, post_java_cmd,
    script_content, is_active, create_time
)
VALUES (
    1,
    'v1.0.0',
    '初始版本 - 基础标准脚本',
    8080,
    '',
    '',
    '',
    '#!/bin/bash
# Spring Boot 应用启动脚本 - Opster 平台
PORT="8080"
JVM_ARGS=""
PRE_JAVA_CMD=""
POST_JAVA_CMD=""
JAR_NAME="*.jar"
LOG_DIR="logs"
PID_FILE="app.pid"
STARTUP_WAIT_TIME=30

RED=$(printf "\033[0;31m")
GREEN=$(printf "\033[0;32m")
YELLOW=$(printf "\033[1;33m")
BLUE=$(printf "\033[0;34m")
NC=$(printf "\033[0m")

log_info() { printf "${GREEN}[%s] INFO: %s${NC}\n" "$(date "+%Y-%m-%d %H:%M:%S")" "$1"; }
log_error() { printf "${RED}[%s] ERROR: %s${NC}\n" "$(date "+%Y-%m-%d %H:%M:%S")" "$1"; }
log_warn() { printf "${YELLOW}[%s] WARN: %s${NC}\n" "$(date "+%Y-%m-%d %H:%M:%S")" "$1"; }
log_step() { printf "${BLUE}[%s] STEP: %s${NC}\n" "$(date "+%Y-%m-%d %H:%M:%S")" "$1"; }

check_java() {
    log_step "检查 Java 环境..."
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        export PATH="$JAVA_HOME/bin:$PATH"
        log_info "使用 JAVA_HOME: $JAVA_HOME"
        return 0
    fi
    if command -v java &> /dev/null; then
        log_info "使用系统 Java"
        return 0
    fi
    log_error "Java 未安装，请设置 JAVA_HOME"
    exit 1
}

check_and_kill_port() {
    log_step "检查端口 $PORT 占用情况..."
    local pid=$(lsof -ti:$PORT 2>/dev/null)
    if [ -n "$pid" ]; then
        log_warn "端口 $PORT 被占用 (PID: $pid)"
        kill -9 "$pid" 2>/dev/null || true
        sleep 1
    fi
}

execute_pre_java_cmd() {
    if [ -n "$PRE_JAVA_CMD" ]; then
        log_step "执行启动前命令：$PRE_JAVA_CMD"
        eval "$PRE_JAVA_CMD" 2>&1 || log_warn "前置命令执行失败，继续启动"
    fi
}

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
    log_step "等待服务启动..."
    sleep 5
}

execute_post_java_cmd() {
    if [ -n "$POST_JAVA_CMD" ]; then
        log_step "执行启动后命令：$POST_JAVA_CMD"
        eval "$POST_JAVA_CMD" 2>&1 || log_warn "后置命令执行失败"
    fi
}

main() {
    log_info "启动 Spring Boot 服务，端口：$PORT"
    check_java
    check_and_kill_port
    execute_pre_java_cmd
    start_java_app
    execute_post_java_cmd
    log_info "服务启动完成"
}

main
',
    1,
    datetime('now')
);

-- 更新主表的 current_version_id
UPDATE service_start_script SET current_version_id = 1 WHERE id = 1;
