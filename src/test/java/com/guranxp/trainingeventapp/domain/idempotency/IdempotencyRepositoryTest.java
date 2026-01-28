package com.guranxp.trainingeventapp.domain.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class IdempotencyRepositoryTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID ID2 = UUID.randomUUID();
    private static final UUID ID3 = UUID.randomUUID();

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IdempotencyRepository repository;

    private IdempotencyKey idempotencyKey;
    private IdempotencyKey oldKey;
    private IdempotencyKey markedKey;

    @BeforeEach
    void setUp() {
        // Simplified approach - just create the test data fresh for each test
        idempotencyKey = IdempotencyKey.builder()
                .id(ID)
                .response("{\"message\": \"success\"}")
                .statusCode(200)
                .build();

        oldKey = IdempotencyKey.builder()
                .id(ID2)
                .response("{\"message\": \"old\"}")
                .statusCode(200)
                .build();

        markedKey = IdempotencyKey.builder()
                .id(ID3)
                .response("{\"message\": \"marked\"}")
                .statusCode(200)
                .markedForDeletionDate(LocalDateTime.now().minusHours(2))
                .build();
    }

    @Test
    void shouldSaveIdempotencyKey() {
        // Given
        entityManager.persistAndFlush(idempotencyKey);

        // When
        final IdempotencyKey saved = repository.findById(ID).orElseThrow();

        // Then
        assertThat(saved.getId()).isEqualTo(ID);
        assertThat(saved.getResponse()).isEqualTo("{\"message\": \"success\"}");
        assertThat(saved.getStatusCode()).isEqualTo(200);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getMarkedForDeletionDate()).isNull();
    }

    @Test
    void shouldFindByIdempotencyKey() {
        // Given
        entityManager.persistAndFlush(idempotencyKey);

        // When
        final Optional<IdempotencyKey> found = repository.findById(ID);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(ID);
        assertThat(found.get().getResponse()).isEqualTo("{\"message\": \"success\"}");
    }

    @Test
    void shouldReturnEmptyWhenKeyNotFound() {
        // When
        final Optional<IdempotencyKey> found = repository.findById(UUID.randomUUID());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldCheckIfKeyExists() {
        // Given
        entityManager.persistAndFlush(idempotencyKey);

        // Then
        assertThat(repository.existsById(ID)).isTrue();
        assertThat(repository.existsById(UUID.randomUUID())).isFalse();
    }

    @Test
    void shouldDeleteByKey() {
        // Given
        entityManager.persistAndFlush(idempotencyKey);

        // When
        repository.deleteById(ID);
        entityManager.flush();

        // Then
        assertThat(repository.existsById(ID)).isFalse();
    }

    @Test
    void shouldEnforceUniqueKeyConstraint() {
        // Given
        entityManager.persistAndFlush(idempotencyKey);
        final IdempotencyKey duplicateKey = IdempotencyKey.builder()
                .id(ID)
                .response("{\"message\": \"duplicate\"}")
                .statusCode(409)
                .build();

        // When & Then
        final Exception exception = org.junit.jupiter.api.Assertions.assertThrows(
                jakarta.persistence.PersistenceException.class,
                () -> entityManager.persistAndFlush(duplicateKey)
        );
        System.out.println(exception);
    }

    @Test
    void shouldMarkOlderKeysForDeletion() {
        // Given - Create keys with different creation times using native SQL
        entityManager.getEntityManager().createNativeQuery("INSERT INTO idempotency_keys (id, response, status_code, created_at) VALUES (?, ?, ?, ?)")
                .setParameter(1, ID)
                .setParameter(2, "{\"message\": \"success\"}")
                .setParameter(3, 200)
                .setParameter(4, LocalDateTime.now().minusDays(1)) // recent
                .executeUpdate();
                
        entityManager.getEntityManager().createNativeQuery("INSERT INTO idempotency_keys (id, response, status_code, created_at) VALUES (?, ?, ?, ?)")
                .setParameter(1, ID2)
                .setParameter(2, "{\"message\": \"old\"}")
                .setParameter(3, 200)
                .setParameter(4, LocalDateTime.now().minusDays(10)) // old - should be marked
                .executeUpdate();
                
        entityManager.getEntityManager().createNativeQuery("INSERT INTO idempotency_keys (id, response, status_code, created_at, marked_for_deletion_date) VALUES (?, ?, ?, ?, ?)")
                .setParameter(1, ID3)
                .setParameter(2, "{\"message\": \"marked\"}")
                .setParameter(3, 200)
                .setParameter(4, LocalDateTime.now().minusDays(5))
                .setParameter(5, LocalDateTime.now().minusHours(2)) // already marked
                .executeUpdate();
        
        entityManager.flush();

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        LocalDateTime markedDate = LocalDateTime.now();

        // When
        int markedCount = repository.markOlderThanCutoffDateForDeletion(cutoffDate, markedDate);

        // Then
        assertThat(markedCount).isEqualTo(1);

        IdempotencyKey updatedOldKey = repository.findById(ID2).orElseThrow();
        assertThat(updatedOldKey.getMarkedForDeletionDate()).isNotNull();
        assertThat(updatedOldKey.getMarkedForDeletionDate()).isAfter(markedDate.minusSeconds(1));

        IdempotencyKey unchangedRecentKey = repository.findById(ID).orElseThrow();
        assertThat(unchangedRecentKey.getMarkedForDeletionDate()).isNull();

        IdempotencyKey unchangedMarkedKey = repository.findById(ID3).orElseThrow();
        assertThat(unchangedMarkedKey.getMarkedForDeletionDate()).isNotNull();
    }

    @Test
    void shouldDeleteMarkedKeysOlderThanCutoff() {
        // Given
        entityManager.persistAndFlush(idempotencyKey); // not marked
        entityManager.persistAndFlush(markedKey); // marked 2 hours ago
        
        IdempotencyKey recentMarkedKey = IdempotencyKey.builder()
                .id(UUID.randomUUID())
                .response("{\"message\": \"recent marked\"}")
                .statusCode(200)
                .markedForDeletionDate(LocalDateTime.now().minusMinutes(30))
                .build();
        entityManager.persistAndFlush(recentMarkedKey);

        LocalDateTime deleteCutoff = LocalDateTime.now().minusHours(1);

        // When
        int deletedCount = repository.deleteMarkedForDeletionOlderThan(deleteCutoff);

        // Then
        assertThat(deletedCount).isEqualTo(1);
        assertThat(repository.existsById(ID)).isTrue(); // not marked
        assertThat(repository.existsById(ID3)).isFalse(); // deleted
        assertThat(repository.existsById(recentMarkedKey.getId())).isTrue(); // too recent
    }

    @Test
    void shouldCountMarkedKeys() {
        // Given
        entityManager.persistAndFlush(idempotencyKey); // not marked
        entityManager.persistAndFlush(markedKey); // marked

        // When
        long markedCount = repository.countMarkedForDeletion();

        // Then
        assertThat(markedCount).isEqualTo(1);
    }

    @Test
    void shouldFindMarkedKeys() {
        // Given
        entityManager.persistAndFlush(idempotencyKey); // not marked
        entityManager.persistAndFlush(markedKey); // marked

        // When
        List<IdempotencyKey> markedKeys = repository.findMarkedForDeletion();

        // Then
        assertThat(markedKeys).hasSize(1);
        assertThat(markedKeys.get(0).getId()).isEqualTo(ID3);
        assertThat(markedKeys.get(0).getMarkedForDeletionDate()).isNotNull();
    }

    @Test
    void shouldReturnEmptyWhenNoMarkedKeys() {
        // Given
        entityManager.persistAndFlush(idempotencyKey);

        // When
        long markedCount = repository.countMarkedForDeletion();
        List<IdempotencyKey> markedKeys = repository.findMarkedForDeletion();

        // Then
        assertThat(markedCount).isZero();
        assertThat(markedKeys).isEmpty();
    }
}
