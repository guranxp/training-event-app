package com.guranxp.trainingeventapp.domain.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyCleanupServiceTest {

    @Mock
    private IdempotencyRepository idempotencyRepository;

    @Mock
    private IdempotencyCleanupProperties properties;

    @InjectMocks
    private IdempotencyCleanupService cleanupService;

    @Test
    void shouldMarkOldKeysForDeletion() {
        // Given
        when(properties.getRetentionDays()).thenReturn(7);
        when(idempotencyRepository.markOlderThanCutoffDateForDeletion(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5);

        // When
        cleanupService.markOldKeysForDeletion();

        // Then
        verify(idempotencyRepository).markOlderThanCutoffDateForDeletion(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void shouldNotLogWhenNoKeysMarked() {
        // Given
        when(properties.getRetentionDays()).thenReturn(7);
        when(idempotencyRepository.markOlderThanCutoffDateForDeletion(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0);

        // When
        cleanupService.markOldKeysForDeletion();

        // Then
        verify(idempotencyRepository).markOlderThanCutoffDateForDeletion(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void shouldHandleExceptionDuringMarking() {
        // Given
        when(properties.getRetentionDays()).thenReturn(7);
        when(idempotencyRepository.markOlderThanCutoffDateForDeletion(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When
        cleanupService.markOldKeysForDeletion();

        // Then
        verify(idempotencyRepository).markOlderThanCutoffDateForDeletion(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(idempotencyRepository, never()).countMarkedForDeletion();
    }

    @Test
    void shouldDeleteMarkedKeys() {
        // Given
        when(properties.getDeletionDelayDays()).thenReturn(1);
        when(idempotencyRepository.deleteMarkedForDeletionOlderThan(any(LocalDateTime.class)))
                .thenReturn(3);

        // When
        cleanupService.deleteMarkedKeys();

        // Then
        verify(idempotencyRepository).deleteMarkedForDeletionOlderThan(any(LocalDateTime.class));
    }

    @Test
    void shouldNotLogWhenNoKeysDeleted() {
        // Given
        when(properties.getDeletionDelayDays()).thenReturn(1);
        when(idempotencyRepository.deleteMarkedForDeletionOlderThan(any(LocalDateTime.class)))
                .thenReturn(0);

        // When
        cleanupService.deleteMarkedKeys();

        // Then
        verify(idempotencyRepository).deleteMarkedForDeletionOlderThan(any(LocalDateTime.class));
    }

    @Test
    void shouldHandleExceptionDuringDeletion() {
        // Given
        when(properties.getDeletionDelayDays()).thenReturn(1);
        when(idempotencyRepository.deleteMarkedForDeletionOlderThan(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Delete error"));

        // When
        cleanupService.deleteMarkedKeys();

        // Then
        verify(idempotencyRepository).deleteMarkedForDeletionOlderThan(any(LocalDateTime.class));
    }

    @Test
    void shouldUseConfiguredRetentionDays() {
        // Given
        when(properties.getRetentionDays()).thenReturn(14);
        when(idempotencyRepository.markOlderThanCutoffDateForDeletion(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1);

        // When
        cleanupService.markOldKeysForDeletion();

        // Then
        verify(idempotencyRepository).markOlderThanCutoffDateForDeletion(
                argThat(date -> date.isBefore(LocalDateTime.now().minusDays(13))),
                any(LocalDateTime.class)
        );
    }

    @Test
    void shouldUseConfiguredDeletionDelay() {
        // Given
        when(properties.getDeletionDelayDays()).thenReturn(2);
        when(idempotencyRepository.deleteMarkedForDeletionOlderThan(any(LocalDateTime.class)))
                .thenReturn(1);

        // When
        cleanupService.deleteMarkedKeys();

        // Then
        verify(idempotencyRepository).deleteMarkedForDeletionOlderThan(
                argThat(date -> date.isBefore(LocalDateTime.now().minusDays(1)))
        );
    }
}
