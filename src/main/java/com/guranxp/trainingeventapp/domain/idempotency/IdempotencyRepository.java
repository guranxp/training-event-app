package com.guranxp.trainingeventapp.domain.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyKey, UUID> {

    @Modifying
    @Query("UPDATE IdempotencyKey ik SET ik.markedForDeletionDate = :markedDate WHERE ik.createdAt < :cutoffDate AND ik.markedForDeletionDate IS NULL")
    int markOlderThanCutoffDateForDeletion(@Param("cutoffDate") LocalDateTime cutoffDate, @Param("markedDate") LocalDateTime markedDate);

    @Modifying
    @Query("DELETE FROM IdempotencyKey ik WHERE ik.markedForDeletionDate IS NOT NULL AND ik.markedForDeletionDate < :deleteCutoff")
    int deleteMarkedForDeletionOlderThan(@Param("deleteCutoff") LocalDateTime deleteCutoff);

    @Query("SELECT COUNT(ik) FROM IdempotencyKey ik WHERE ik.markedForDeletionDate IS NOT NULL")
    long countMarkedForDeletion();

    @Query("SELECT ik FROM IdempotencyKey ik WHERE ik.markedForDeletionDate IS NOT NULL ORDER BY ik.markedForDeletionDate")
    List<IdempotencyKey> findMarkedForDeletion();

}
