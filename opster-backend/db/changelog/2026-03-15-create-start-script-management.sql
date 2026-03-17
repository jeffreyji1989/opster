-- ============================================================
-- 启动脚本管理模块 - 数据库迁移脚本
-- 创建时间：2026-03-15
-- ============================================================

-- ============================================================
-- 1. 启动脚本配置表（主表）
-- ============================================================
CREATE TABLE IF NOT EXISTS service_start_script (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '脚本名称',
    description TEXT COMMENT '脚本描述',
    port INTEGER DEFAULT 8080 COMMENT '服务端口号',
    jvm_args TEXT COMMENT 'JVM 参数',
    pre_java_cmd TEXT COMMENT 'Java 命令执行前的命令',
    post_java_cmd TEXT COMMENT 'Java 命令执行后的命令',
    current_version_id INTEGER COMMENT '当前生效的版本 ID',
    is_default INTEGER DEFAULT 0 COMMENT '是否为默认脚本：0-否 1-是',
    created_by VARCHAR(50),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    del_flag INTEGER DEFAULT 0 COMMENT '删除标志：0-正常 1-已删除'
);
CREATE INDEX IF NOT EXISTS idx_service_start_script_name ON service_start_script(name);
CREATE INDEX IF NOT EXISTS idx_service_start_script_default ON service_start_script(is_default);

-- ============================================================
-- 2. 启动脚本版本表（历史版本）
-- ============================================================
CREATE TABLE IF NOT EXISTS service_start_script_version (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    script_id INTEGER NOT NULL COMMENT '关联脚本 ID',
    version_no VARCHAR(20) NOT NULL COMMENT '版本号：v1.0.0, v1.0.1',
    version_description TEXT COMMENT '版本描述',
    port INTEGER NOT NULL COMMENT '服务端口号',
    jvm_args TEXT COMMENT 'JVM 参数',
    pre_java_cmd TEXT COMMENT 'Java 命令执行前的命令',
    post_java_cmd TEXT COMMENT 'Java 命令执行后的命令',
    script_content TEXT NOT NULL COMMENT '生成的完整脚本内容',
    created_by VARCHAR(50),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_active INTEGER DEFAULT 0 COMMENT '是否为当前激活版本：0-否 1-是',
    FOREIGN KEY (script_id) REFERENCES service_start_script(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_script_version_script_id ON service_start_script_version(script_id);
CREATE INDEX IF NOT EXISTS idx_script_version_active ON service_start_script_version(is_active);

-- ============================================================
-- 3. 服务表新增字段 - 关联启动脚本
-- ============================================================
ALTER TABLE service ADD COLUMN start_script_id INTEGER COMMENT '关联的启动脚本 ID';
ALTER TABLE service ADD COLUMN start_script_version_id INTEGER COMMENT '关联的脚本版本 ID';
CREATE INDEX IF NOT EXISTS idx_service_start_script_id ON service(start_script_id);

-- ============================================================
-- 4. 初始化默认脚本
-- ============================================================
INSERT INTO service_start_script (id, name, description, port, is_default, current_version_id, created_time, updated_time)
VALUES (
    1,
    '标准 Spring Boot 启动脚本',
    '适用于大多数 Spring Boot 应用，支持端口清理、JVM 参数配置、前后置命令',
    8080,
    1,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ============================================================
-- 5. 为默认脚本创建初始版本 (script_id=1, version_no=v1.0.0)
-- ============================================================
INSERT INTO service_start_script_version (
    script_id, version_no, version_description, port, jvm_args, pre_java_cmd, post_java_cmd,
    script_content, is_active, created_time, created_by
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
# ============================================================
# Spring Boot 应用启动脚本
# 自动生成于 Opster 平台
# ============================================================
# 脚本名称：标准 Spring Boot 启动脚本
# 版本号：v1.0.0
# 服务端口：8080
# 使用方式：sh start.sh (无需传递参数)
# ============================================================

# ========== 配置区 ==========
PORT="8080"
JVM_ARGS=""
PRE_JAVA_CMD=""
POST_JAVA_CMD=""
JAR_NAME="*.jar"
LOG_DIR="logs"
PID_FILE="app.pid"
STARTUP_WAIT_TIME=30

# ========== 颜色定义 ==========
RED='\''\033[0;31m'\''
GREEN='\''\033[0;32m'\''
YELLOW='\''\033[1;33m'\''
BLUE='\''\033[0;34m'\''
NC='\''\033[0m'\''

# ========== 日志函数 ==========
log_info() { echo -e "${GREEN}[$(date '\''+%Y-%m-%d %H:%M:%S'\'')] INFO: $1${NC}"; }
log_error() { echo -e "${RED}[$(date '\''+%Y-%m-%d %H:%M:%S'\'')] ERROR: $1${NC}"; }
log_warn() { echo -e "${YELLOW}[$(date '\''+%Y-%m-%d %H:%M:%S'\'')] WARN: $1${NC}"; }
log_step() { echo -e "${BLUE}[$(date '\''+%Y-%m-%d %H:%M:%S'\'')] STEP: $1${NC}"; }

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

main',
    1,
    CURRENT_TIMESTAMP,
    'system'
);

-- ============================================================
-- 6. 更新主表的 current_version_id
-- ============================================================
UPDATE service_start_script SET current_version_id = 1 WHERE id = 1;
