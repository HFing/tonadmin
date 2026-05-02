package com.hfing.tonadmin.repositories;

import com.hfing.tonadmin.dto.response.StockTransactionSummaryProjection;
import com.hfing.tonadmin.entities.StockTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, String> {

    @EntityGraph(attributePaths = {"branch", "product", "product.category"})
    List<StockTransaction> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"branch", "product", "product.category"})
    List<StockTransaction> findByBranchIdOrderByCreatedAtDesc(String branchId);

    @EntityGraph(attributePaths = {"branch", "product", "product.category"})
    List<StockTransaction> findByProductIdOrderByCreatedAtDesc(String productId);

    @EntityGraph(attributePaths = {"branch", "product", "product.category"})
    List<StockTransaction> findByBranchIdAndProductIdOrderByCreatedAtDesc(String branchId, String productId);

    @Query(
            value = """
                    select 
                        st.batchCode as batchCode,
                        st.transactionType as transactionType,
                        b.name as branchName,
                        count(st.id) as itemCount,
                        sum(st.quantity) as totalQuantity,
                        max(st.batchNote) as note,
                        max(st.createdAt) as createdAt,
                        max(st.createdBy) as createdBy
                    from StockTransaction st
                    join st.branch b
                    group by st.batchCode, st.transactionType, b.name
                    order by max(st.createdAt) desc
                    """,
            countQuery = """
                    select count(distinct st.batchCode)
                    from StockTransaction st
                    """
    )
    Page<StockTransactionSummaryProjection> findTransactionSummaries(Pageable pageable);


    @EntityGraph(attributePaths = {"branch", "product"})
    List<StockTransaction> findTop5ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"branch", "product"})
    List<StockTransaction> findTop5ByBranchIdOrderByCreatedAtDesc(String branchId);
}