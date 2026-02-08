package com.opster.module.service.transfer.checker;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 传输前检查结果
 */
@Data
public class TransferCheckResult {
    private boolean passed = true;
    private List<CheckItem> items = new ArrayList<>();

    public void addCheck(String name, boolean passed, String message) {
        items.add(new CheckItem(name, passed, message));
        if (!passed) {
            this.passed = false;
        }
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("传输前检查结果: ").append(passed ? "通过" : "失败").append("\n");
        for (CheckItem item : items) {
            sb.append("  - ").append(item.getName())
              .append(": ").append(item.isPassed() ? "✓" : "✗")
              .append(" - ").append(item.getMessage()).append("\n");
        }
        return sb.toString();
    }

    @Data
    public static class CheckItem {
        private final String name;
        private final boolean passed;
        private final String message;
    }
}
