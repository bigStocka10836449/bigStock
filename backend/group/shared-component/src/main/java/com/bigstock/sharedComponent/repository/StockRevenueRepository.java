package com.bigstock.sharedComponent.repository;

import com.bigstock.sharedComponent.entity.StockRevenue;
import com.bigstock.sharedComponent.entity.StockRevenueId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StockRevenueRepository extends JpaRepository<StockRevenue, StockRevenueId> {

    // ✅ 永遠不以 market 過濾，但保留 :market 參數避免 Spring 啟動期檢查失敗
    @Query("""
            SELECT e
            FROM StockRevenue e
            WHERE e.id.stockId = :stockId
              AND (:market IS NULL OR :market IS NOT NULL)
              AND e.id.revenueMonth BETWEEN :startMonth AND :endMonth
            ORDER BY e.id.revenueMonth ASC
           """)
    List<StockRevenue> findByStockIdAndMonthRange(
            @Param("stockId") String stockId,
            @Param("market") String market,
            @Param("startMonth") LocalDate startMonth,
            @Param("endMonth") LocalDate endMonth
    );

    // ✅ 永遠不以 market 過濾（分頁查某月份）
    @Query("""
            SELECT e
            FROM StockRevenue e
            WHERE e.id.revenueMonth = :revenueMonth
              AND (:market IS NULL OR :market IS NOT NULL)
            ORDER BY e.id.market ASC, e.id.stockId ASC
           """)
    Page<StockRevenue> findByRevenueMonth(
            @Param("revenueMonth") LocalDate revenueMonth,
            @Param("market") String market,
            Pageable pageable
    );

    // ✅ 永遠不以 market 過濾（排行）
    @Query("""
            SELECT e
            FROM StockRevenue e
            WHERE e.id.revenueMonth = :revenueMonth
              AND (:market IS NULL OR :market IS NOT NULL)
            ORDER BY e.revenue DESC, e.id.stockId ASC
           """)
    Page<StockRevenue> rankByRevenue(
            @Param("revenueMonth") LocalDate revenueMonth,
            @Param("market") String market,
            Pageable pageable
    );
}
