package com.hfing.tonadmin.repositories;

import com.hfing.tonadmin.entities.SalesPayment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalesPaymentRepository extends JpaRepository<SalesPayment, String> {

    @EntityGraph(attributePaths = {"salesOrder"})
    List<SalesPayment> findBySalesOrderIdOrderByCreatedAtDesc(String salesOrderId);
}