package com.hfing.tonadmin.repositories;

import com.hfing.tonadmin.entities.SalesOrderItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, String> {

    @EntityGraph(attributePaths = {"product", "product.category"})
    List<SalesOrderItem> findBySalesOrderId(String salesOrderId);
}
