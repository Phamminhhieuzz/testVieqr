package com.demo.vietqr.repository;

import com.demo.vietqr.entity.TransactionSyncEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionSyncRepository extends JpaRepository<TransactionSyncEntity, Long> {

    Optional<TransactionSyncEntity> findByTransactionid(String transactionid);

    List<TransactionSyncEntity> findTop50ByOrderByIdDesc();
}
