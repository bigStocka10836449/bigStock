package com.bigstock.sharedComponent.repository;

import com.bigstock.sharedComponent.entity.StockThreeInstitutionalTrading;
import com.bigstock.sharedComponent.entity.StockThreeInstitutionalTradingId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StockThreeInstitutionalTradingRepository
        extends JpaRepository<StockThreeInstitutionalTrading, StockThreeInstitutionalTradingId> {


	
	@Modifying
	@Query(value = """
			DELETE FROM bstock.stock_three_institutional_trading r WHERE r.trade_date <= :expiredDate
			""", nativeQuery = true )
	void deleteByExpiredDate( @Param("expiredDate") LocalDate expiredDate);
	
	@Query(value = """
			SELECT r.*
			FROM bstock.stock_three_institutional_trading r
			WHERE r.trade_date between :startDate AND :endDate 
			""", nativeQuery = true )
	List<StockThreeInstitutionalTrading> getDateRangMarketStockData(    @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
	
	
	@Query(value = """
			SELECT r.*
			FROM bstock.stock_three_institutional_trading r
			WHERE r.stock_code = :stockCode
			ORDER BY r.trade_date DESC
			LIMIT 3
			""", nativeQuery = true )
	List<StockThreeInstitutionalTrading> getLastThreeSingleStockData( @Param("stockCode") String stockCode);
	
	
    // 查某檔股票區間（App 最常用）
    @Query("""
            SELECT e
            FROM StockThreeInstitutionalTrading e
            WHERE e.id.stockCode = :stockCode
              AND (:market IS NULL OR e.id.market = :market)
              AND e.id.tradeDate BETWEEN :startDate AND :endDate
            ORDER BY e.id.tradeDate ASC
           """)
    List<StockThreeInstitutionalTrading> findByStockCodeAndDateRange(
            @Param("stockCode") String stockCode,
            @Param("market") String market,                 // nullable
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // 查某天某市場（可分頁）
    @Query("""
            SELECT e
            FROM StockThreeInstitutionalTrading e
            WHERE e.id.tradeDate = :tradeDate
              AND (:market IS NULL OR e.id.market = :market)
            ORDER BY e.id.market ASC, e.id.stockCode ASC
           """)
    Page<StockThreeInstitutionalTrading> findByTradeDate(
            @Param("tradeDate") LocalDate tradeDate,
            @Param("market") String market,                 // nullable
            Pageable pageable
    );

    // 排行：某天某市場，依外資買賣超排序（net = buy - sell）
    @Query("""
            SELECT e
            FROM StockThreeInstitutionalTrading e
            WHERE e.id.tradeDate = :tradeDate
              AND (:market IS NULL OR e.id.market = :market)
            ORDER BY (e.foreignBuy - e.foreignSell) DESC, e.id.stockCode ASC
           """)
    Page<StockThreeInstitutionalTrading> rankForeignNetBuy(
            @Param("tradeDate") LocalDate tradeDate,
            @Param("market") String market,                 // nullable
            Pageable pageable
    );

    // 排行：投信 net
    @Query("""
            SELECT e
            FROM StockThreeInstitutionalTrading e
            WHERE e.id.tradeDate = :tradeDate
              AND (:market IS NULL OR e.id.market = :market)
            ORDER BY (e.investmentTrustBuy - e.investmentTrustSell) DESC, e.id.stockCode ASC
           """)
    Page<StockThreeInstitutionalTrading> rankInvestmentTrustNetBuy(
            @Param("tradeDate") LocalDate tradeDate,
            @Param("market") String market,                 // nullable
            Pageable pageable
    );

    //  排行：自營商 net
    @Query("""
            SELECT e
            FROM StockThreeInstitutionalTrading e
            WHERE e.id.tradeDate = :tradeDate
              AND (:market IS NULL OR e.id.market = :market)
            ORDER BY (e.dealerBuy - e.dealerSell) DESC, e.id.stockCode ASC
           """)
    Page<StockThreeInstitutionalTrading> rankDealerNetBuy(
            @Param("tradeDate") LocalDate tradeDate,
            @Param("market") String market,                 // nullable
            Pageable pageable
    );
}