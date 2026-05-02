package com.mkwang.backend.modules.wallet.repository;

import com.mkwang.backend.modules.wallet.entity.DepositLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepositLogRepository extends JpaRepository<DepositLog, Long> {
    Optional<DepositLog> findByDepositCode(String depositCode);
    Page<DepositLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
