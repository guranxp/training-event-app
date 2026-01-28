package com.guranxp.trainingeventapp.domain.idempotency;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.idempotency.cleanup")
public class IdempotencyCleanupProperties {

    private int retentionDays = 7;
    private int deletionDelayDays = 1;
    private int markingIntervalHours = 1;
    private int deletionIntervalHours = 2;
}
