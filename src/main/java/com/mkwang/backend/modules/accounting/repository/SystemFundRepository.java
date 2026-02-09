package com.mkwang.backend.modules.accounting.repository;

import com.mkwang.backend.modules.accounting.entity.SystemFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for SystemFund entity.
 */
@Repository
public interface SystemFundRepository extends JpaRepository<SystemFund, Long> {

  /**
   * Find the default system fund (id = 1)
   */
  default Optional<SystemFund> findDefault() {
    return findById(1L);
  }
}
