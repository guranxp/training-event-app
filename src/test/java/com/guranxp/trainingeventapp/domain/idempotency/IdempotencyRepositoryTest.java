package com.guranxp.trainingeventapp.domain.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
class IdempotencyRepositoryTest {

    private static final UUID ID = UUID.randomUUID();

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IdempotencyRepository repository;

    private IdempotencyKey idempotencyKey;

    @BeforeEach
    void setUp() {
        idempotencyKey = IdempotencyKey.builder()
                .id(ID)
                .response("{\"message\": \"success\"}")
                .statusCode(200)
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
        final Exception exception = assertThrows(
                jakarta.persistence.PersistenceException.class,
                () -> entityManager.persistAndFlush(duplicateKey)
        );
        System.out.println(exception);
    }

}
