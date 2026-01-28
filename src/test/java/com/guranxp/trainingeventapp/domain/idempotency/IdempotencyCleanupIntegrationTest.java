package com.guranxp.trainingeventapp.domain.idempotency;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.idempotency.cleanup.retention-days=1",
        "app.idempotency.cleanup.deletion-delay-days=0",
        "app.idempotency.cleanup.marking-interval-hours=1",
        "app.idempotency.cleanup.deletion-interval-hours=1"
})
@Transactional
class IdempotencyCleanupIntegrationTest {

    @Autowired
    private IdempotencyRepository repository;

    @Autowired
    private IdempotencyCleanupService cleanupService;

    @Test
    void shouldCompleteFullCleanupFlow() {
        // Given - Create test data
        UUID recentKeyId = UUID.randomUUID();
        UUID oldKeyId = UUID.randomUUID();
        UUID veryOldKeyId = UUID.randomUUID();

        // Recent key (should not be marked)
        IdempotencyKey recentKey = IdempotencyKey.builder()
                .id(recentKeyId)
                .response("{\"message\": \"recent\"}")
                .statusCode(200)
                .createdAt(LocalDateTime.now().minusMinutes(30))
                .build();

        // Old key (should be marked but not deleted yet)
        IdempotencyKey oldKey = IdempotencyKey.builder()
                .id(oldKeyId)
                .response("{\"message\": \"old\"}")
                .statusCode(200)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        // Very old key (already marked, should be deleted)
        IdempotencyKey veryOldKey = IdempotencyKey.builder()
                .id(veryOldKeyId)
                .response("{\"message\": \"very old\"}")
                .statusCode(200)
                .createdAt(LocalDateTime.now().minusDays(3))
                .markedForDeletionDate(LocalDateTime.now().minusHours(2))
                .build();

        repository.saveAll(List.of(recentKey, oldKey, veryOldKey));
        repository.flush();

        // Verify initial state
        assertThat(repository.count()).isEqualTo(3);
        assertThat(repository.countMarkedForDeletion()).isEqualTo(1);

        // When - Phase 1: Mark old keys for deletion
        cleanupService.markOldKeysForDeletion();

        // Then - Verify marking
        assertThat(repository.count()).isEqualTo(3);
        assertThat(repository.countMarkedForDeletion()).isEqualTo(2);

        IdempotencyKey updatedOldKey = repository.findById(oldKeyId).orElseThrow();
        assertThat(updatedOldKey.getMarkedForDeletionDate()).isNotNull();

        IdempotencyKey unchangedRecentKey = repository.findById(recentKeyId).orElseThrow();
        assertThat(unchangedRecentKey.getMarkedForDeletionDate()).isNull();

        // When - Phase 2: Delete marked keys
        cleanupService.deleteMarkedKeys();

        // Then - Verify deletion
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.countMarkedForDeletion()).isEqualTo(0);

        // Only recent key should remain
        assertThat(repository.existsById(recentKeyId)).isTrue();
        assertThat(repository.existsById(oldKeyId)).isFalse();
        assertThat(repository.existsById(veryOldKeyId)).isFalse();
    }

    @Test
    void shouldHandleEmptyDatabase() {
        // Given
        assertThat(repository.count()).isZero();

        // When
        cleanupService.markOldKeysForDeletion();
        cleanupService.deleteMarkedKeys();

        // Then
        assertThat(repository.count()).isZero();
        assertThat(repository.countMarkedForDeletion()).isZero();
    }

    @Test
    void shouldOnlyDeleteMarkedKeysAfterDelay() {
        // Given
        UUID oldKeyId = UUID.randomUUID();
        UUID recentMarkedKeyId = UUID.randomUUID();

        IdempotencyKey oldKey = IdempotencyKey.builder()
                .id(oldKeyId)
                .response("{\"message\": \"old\"}")
                .statusCode(200)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        IdempotencyKey recentMarkedKey = IdempotencyKey.builder()
                .id(recentMarkedKeyId)
                .response("{\"message\": \"recent marked\"}")
                .statusCode(200)
                .createdAt(LocalDateTime.now().minusDays(2))
                .markedForDeletionDate(LocalDateTime.now().minusMinutes(30))
                .build();

        repository.saveAll(List.of(oldKey, recentMarkedKey));
        repository.flush();

        // When - Mark old keys
        cleanupService.markOldKeysForDeletion();

        // Then - Both should be marked
        assertThat(repository.countMarkedForDeletion()).isEqualTo(2);

        // When - Delete marked keys (with 0 hour delay for test)
        cleanupService.deleteMarkedKeys();

        // Then - Both should be deleted due to 0 hour delay in test config
        assertThat(repository.count()).isZero();
    }

    @Test
    void shouldPreserveRecentKeys() {
        // Given
        UUID recentKeyId = UUID.randomUUID();

        IdempotencyKey recentKey = IdempotencyKey.builder()
                .id(recentKeyId)
                .response("{\"message\": \"recent\"}")
                .statusCode(200)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();

        repository.save(recentKey);
        repository.flush();

        // When
        cleanupService.markOldKeysForDeletion();
        cleanupService.deleteMarkedKeys();

        // Then
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.existsById(recentKeyId)).isTrue();

        IdempotencyKey preservedKey = repository.findById(recentKeyId).orElseThrow();
        assertThat(preservedKey.getMarkedForDeletionDate()).isNull();
    }
}
