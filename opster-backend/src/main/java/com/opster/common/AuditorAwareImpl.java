package com.opster.common;

import org.springframework.data.domain.AuditorAware;
import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        // 默认返回管理员ID 1
        return Optional.of(1L);
    }
}
