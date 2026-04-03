package com.mkwang.backend.modules.wallet.repository;

import com.mkwang.backend.modules.wallet.entity.ReferenceType;
import com.mkwang.backend.modules.wallet.entity.Transaction;
import com.mkwang.backend.modules.wallet.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

  Optional<Transaction> findByTransactionCode(String transactionCode);

  boolean existsByTransactionCode(String transactionCode);

  Optional<Transaction> findByPaymentRefAndStatus(String paymentRef, TransactionStatus status);

  List<Transaction> findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(
      ReferenceType referenceType, Long referenceId);
}
