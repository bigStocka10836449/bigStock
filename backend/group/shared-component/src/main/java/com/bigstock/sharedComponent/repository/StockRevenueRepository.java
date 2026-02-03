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

    // 查單一股票區間（月營收）
    @Query("""
            SELECT e
            FROM StockRevenue e
            WHERE e.id.stockId = :stockId
              AND (:market IS NULL OR e.id.market = :market)
              AND e.id.revenueMonth BETWEEN :startMonth AND :endMonth
            ORDER BY e.id.revenueMonth ASC
           """)
    List<StockRevenue> findByStockIdAndMonthRange(
            @Param("stockId") String stockId,
            @Param("market") String market,                 // nullable
            @Param("startMonth") LocalDate startMonth,
            @Param("endMonth") LocalDate endMonth
    );

    // 查某月份（可分頁）
    @Query("""
            SELECT e
            FROM StockRevenue e
            WHERE e.id.revenueMonth = :revenueMonth
              AND (:market IS NULL OR e.id.market = :market)
            ORDER BY e.id.market ASC, e.id.stockId ASC
           """)
    Page<StockRevenue> findByRevenueMonth(
            @Param("revenueMonth") LocalDate revenueMonth,
            @Param("market") String market,                 // nullable
            Pageable pageable
    );

    // 排行：某月份（依 revenue DESC）
    @Query("""
            SELECT e
            FROM StockRevenue e
            WHERE e.id.revenueMonth = :revenueMonth
              AND (:market IS NULL OR e.id.market = :market)
            ORDER BY e.revenue DESC, e.id.stockId ASC
           """)
    Page<StockRevenue> rankByRevenue(
            @Param("revenueMonth") LocalDate revenueMonth,
            @Param("market") String market,                 // nullable
            Pageable pageable
    );
}
