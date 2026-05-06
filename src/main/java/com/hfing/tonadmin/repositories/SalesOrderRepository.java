package com.hfing.tonadmin.repositories;

import com.hfing.tonadmin.entities.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.hfing.tonadmin.common.PaymentStatus;
import com.hfing.tonadmin.common.SalesOrderStatus;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import java.util.Optional;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, String>, JpaSpecificationExecutor<SalesOrder> {

    @EntityGraph(attributePaths = {"branch"})
    Page<SalesOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"branch"})
    Page<SalesOrder> findAll(Specification<SalesOrder> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"branch"})
    Page<SalesOrder> findByBranchIdOrderByCreatedAtDesc(String branchId, Pageable pageable);

    @EntityGraph(attributePaths = {"branch"})
    Optional<SalesOrder> findByIdAndBranchId(String id, String branchId);

    @Query("""
        select coalesce(sum(s.finalAmount), 0)
        from SalesOrder s
        where s.status <> :cancelledStatus
          and s.createdAt >= :from
          and s.createdAt < :to
        """)
    BigDecimal sumRevenue(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("cancelledStatus") SalesOrderStatus cancelledStatus
    );

    @Query("""
        select coalesce(sum(s.finalAmount), 0)
        from SalesOrder s
        where s.status <> :cancelledStatus
          and s.branch.id = :branchId
          and s.createdAt >= :from
          and s.createdAt < :to
        """)
    BigDecimal sumRevenueByBranch(
            @Param("branchId") String branchId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("cancelledStatus") SalesOrderStatus cancelledStatus
    );

    @Query("""
        select count(s)
        from SalesOrder s
        where s.status <> :cancelledStatus
          and s.createdAt >= :from
          and s.createdAt < :to
        """)
    long countOrders(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("cancelledStatus") SalesOrderStatus cancelledStatus
    );

    @Query("""
        select count(s)
        from SalesOrder s
        where s.status <> :cancelledStatus
          and s.branch.id = :branchId
          and s.createdAt >= :from
          and s.createdAt < :to
        """)
    long countOrdersByBranch(
            @Param("branchId") String branchId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("cancelledStatus") SalesOrderStatus cancelledStatus
    );

    @Query("""
        select count(s)
        from SalesOrder s
        where s.status <> :cancelledStatus
          and s.paymentStatus <> :paidStatus
        """)
    long countUnpaidOrders(
            @Param("cancelledStatus") SalesOrderStatus cancelledStatus,
            @Param("paidStatus") PaymentStatus paidStatus
    );

    @Query("""
        select count(s)
        from SalesOrder s
        where s.status <> :cancelledStatus
          and s.paymentStatus <> :paidStatus
          and s.branch.id = :branchId
        """)
    long countUnpaidOrdersByBranch(
            @Param("branchId") String branchId,
            @Param("cancelledStatus") SalesOrderStatus cancelledStatus,
            @Param("paidStatus") PaymentStatus paidStatus
    );

    @Query("""
        select coalesce(sum(s.remainingAmount), 0)
        from SalesOrder s
        where s.status <> :cancelledStatus
          and s.paymentStatus <> :paidStatus
        """)
    BigDecimal sumUnpaidAmount(
            @Param("cancelledStatus") SalesOrderStatus cancelledStatus,
            @Param("paidStatus") PaymentStatus paidStatus
    );

    @Query("""
        select coalesce(sum(s.remainingAmount), 0)
        from SalesOrder s
        where s.status <> :cancelledStatus
          and s.paymentStatus <> :paidStatus
          and s.branch.id = :branchId
        """)
    BigDecimal sumUnpaidAmountByBranch(
            @Param("branchId") String branchId,
            @Param("cancelledStatus") SalesOrderStatus cancelledStatus,
            @Param("paidStatus") PaymentStatus paidStatus
    );

    @EntityGraph(attributePaths = {"branch"})
    List<SalesOrder> findTop5ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"branch"})
    List<SalesOrder> findTop5ByBranchIdOrderByCreatedAtDesc(String branchId);
}
