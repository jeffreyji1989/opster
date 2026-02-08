package com.opster.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.HashMap;

/**
 * 临时工具：修复数据库约束
 * 执行完成后可删除此文件
 */
@RestController
@RequestMapping("/api/util")
public class DatabaseFixController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 修复 service 表的 run_status 约束
     * POST /api/util/fix-run-status-constraint
     */
    @PostMapping("/fix-run-status-constraint")
    public Map<String, Object> fixRunStatusConstraint() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 检查约束是否存在（MySQL 8.0+ 语法）
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS " +
                "WHERE CONSTRAINT_NAME = 'service_chk_1' AND TABLE_NAME = 'service' AND TABLE_SCHEMA = DATABASE()",
                Long.class
            );

            if (count != null && count == 0) {
                result.put("success", true);
                result.put("message", "约束不存在或已被修复");
                return result;
            }

            // 2. 删除旧约束
            jdbcTemplate.execute("ALTER TABLE service DROP CONSTRAINT service_chk_1");
            result.put("step1", "✓ 删除旧约束成功");

            // 3. 添加新约束（支持 0-4）
            jdbcTemplate.execute("ALTER TABLE service ADD CONSTRAINT service_chk_1 CHECK (run_status BETWEEN 0 AND 4)");
            result.put("step2", "✓ 添加新约束成功（0-4）");

            // 4. 验证
            String checkClause = jdbcTemplate.queryForObject(
                "SELECT CHECK_CLAUSE FROM information_schema.CHECK_CONSTRAINTS " +
                "WHERE CONSTRAINT_NAME = 'service_chk_1' AND TABLE_NAME = 'service' AND TABLE_SCHEMA = DATABASE()",
                String.class
            );
            result.put("verification", "新约束: " + checkClause);

            result.put("success", true);
            result.put("message", "数据库约束修复成功！run_status 现在支持 0-4 的值");

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("message", "修复失败: " + e.getMessage());
        }

        return result;
    }
}
