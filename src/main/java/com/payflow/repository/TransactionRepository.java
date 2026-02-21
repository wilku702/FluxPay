package com.payflow.repository;

import com.payflow.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    List<Transaction> findByCorrelationId(UUID correlationId);

    @Query(value = "SELECT * FROM transactions t WHERE t.account_id = :accountId " +
           "AND (CAST(:type AS VARCHAR) IS NULL OR t.type = :type) " +
           "AND (CAST(:status AS VARCHAR) IS NULL OR t.status = :status) " +
           "AND (CAST(:from AS TIMESTAMP) IS NULL OR t.created_at >= :from) " +
           "AND (CAST(:to AS TIMESTAMP) IS NULL OR t.created_at <= :to) " +
           "AND (CAST(:minAmount AS DECIMAL) IS NULL OR t.amount >= :minAmount) " +
           "AND (CAST(:maxAmount AS DECIMAL) IS NULL OR t.amount <= :maxAmount)",
           countQuery = "SELECT COUNT(*) FROM transactions t WHERE t.account_id = :accountId " +
           "AND (CAST(:type AS VARCHAR) IS NULL OR t.type = :type) " +
           "AND (CAST(:status AS VARCHAR) IS NULL OR t.status = :status) " +
           "AND (CAST(:from AS TIMESTAMP) IS NULL OR t.created_at >= :from) " +
           "AND (CAST(:to AS TIMESTAMP) IS NULL OR t.created_at <= :to) " +
           "AND (CAST(:minAmount AS DECIMAL) IS NULL OR t.amount >= :minAmount) " +
           "AND (CAST(:maxAmount AS DECIMAL) IS NULL OR t.amount <= :maxAmount)",
           nativeQuery = true)
    Page<Transaction> findByFilters(
            @Param("accountId") Long accountId,
            @Param("type") String type,
            @Param("status") String status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable);
}
