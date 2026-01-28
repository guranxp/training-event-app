package com.guranxp.trainingeventapp.domain.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyCleanupService {

    private final IdempotencyRepository idempotencyRepository;
    private final IdempotencyCleanupProperties properties;

    @Scheduled(fixedRateString = "#{@idempotencyCleanupProperties.markingIntervalHours * 60 * 60 * 1000}")
    @Transactional
    public void markOldKeysForDeletion() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(properties.getRetentionDays());
            LocalDateTime markedDate = LocalDateTime.now();

            int markedCount = idempotencyRepository.markOlderThanCutoffDateForDeletion(cutoffDate, markedDate);

            if (markedCount > 0) {
                log.info("Marked {} idempotency keys for deletion (older than {})", markedCount, cutoffDate);
            }

            if (log.isDebugEnabled()) {
                long totalMarked = idempotencyRepository.countMarkedForDeletion();
                if (totalMarked > 0) {
                    log.debug("Total keys marked for deletion awaiting removal: {}", totalMarked);
                }
            }
        } catch (Exception e) {
            log.error("Error during marking keys for deletion", e);
        }
    }

    @Scheduled(fixedRateString = "#{@idempotencyCleanupProperties.deletionIntervalHours * 60 * 60 * 1000}")
    @Transactional
    public void deleteMarkedKeys() {
        try {
            LocalDateTime deleteCutoff = LocalDateTime.now().minusDays(properties.getDeletionDelayDays());

            int deletedCount = idempotencyRepository.deleteMarkedForDeletionOlderThan(deleteCutoff);

            if (deletedCount > 0) {
                log.info("Deleted {} marked idempotency keys (marked before {})", deletedCount, deleteCutoff);
            }
        } catch (Exception e) {
            log.error("Error during deletion of marked keys", e);
        }
    }
}
