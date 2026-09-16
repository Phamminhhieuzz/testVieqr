package com.demo.vietqr.repository;

import com.demo.vietqr.entity.QrOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QrOrderRepository extends JpaRepository<QrOrder, Long> {

    Optional<QrOrder> findByOrderId(String orderId);

    Optional<QrOrder> findByVqrCode(String vqrCode);

    java.util.List<QrOrder> findByStatus(QrOrder.Status status);
}
