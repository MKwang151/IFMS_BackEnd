package com.mkwang.backend.modules.wallet.repository;

import com.mkwang.backend.modules.wallet.entity.DepositLog;
import com.mkwang.backend.modules.wallet.entity.DepositStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface DepositLogRepository extends JpaRepository<DepositLog, Long> {
    Optional<DepositLog> findByDepositCode(String depositCode);
    Page<DepositLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT d FROM DepositLog d WHERE d.userId = :userId " +
           "AND (:status IS NULL OR d.status = :status) " +
           "AND (:from IS NULL OR d.createdAt >= :from) " +
           "AND (:to IS NULL OR d.createdAt <= :to) " +
           "ORDER BY d.createdAt DESC")
    Page<DepositLog> findByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("status") DepositStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
