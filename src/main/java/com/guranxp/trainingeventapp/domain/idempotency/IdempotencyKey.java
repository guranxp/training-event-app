package com.guranxp.trainingeventapp.domain.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey {

   @Id
   @Column(name = "id", updatable = false, nullable = false)
   private UUID id;

   @Column(name = "response", columnDefinition = "TEXT", updatable = false)
   private String response;

   @Column(name = "status_code", updatable = false)
   private Integer statusCode;

   @CreationTimestamp
   @Column(name = "created_at", nullable = false, updatable = false)
   private LocalDateTime createdAt;

   @Column(name = "marked_for_deletion_date")
   private LocalDateTime markedForDeletionDate;
}
