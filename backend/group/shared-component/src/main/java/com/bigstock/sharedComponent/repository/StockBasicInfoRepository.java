package com.bigstock.sharedComponent.repository;

import com.bigstock.sharedComponent.entity.StockBasicInfo;
import com.bigstock.sharedComponent.entity.StockBasicInfoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StockBasicInfoRepository extends JpaRepository<StockBasicInfo, StockBasicInfoId> {

    @Query("""
            SELECT e
            FROM StockBasicInfo e
            WHERE e.id.stockId = :stockId
            ORDER BY e.id.market ASC
           """)
    List<StockBasicInfo> findAllByStockId(@Param("stockId") String stockId);

    @Query("""
            SELECT e
            FROM StockBasicInfo e
            WHERE e.id.stockId = :stockId
              AND e.id.market = :market
           """)
    Optional<StockBasicInfo> findOne(@Param("stockId") String stockId, @Param("market") String market);

    @Query("""
            SELECT COUNT(e) > 0
            FROM StockBasicInfo e
            WHERE e.id.stockId = :stockId
              AND e.id.market = :market
              AND e.updatedAt >= :freshAfter
           """)
    boolean existsFresh(
            @Param("stockId") String stockId,
            @Param("market") String market,
            @Param("freshAfter") Instant freshAfter
    );
}
