package com.bigstock.biz.service;

import com.bigstock.biz.vo.ThreeInstitutionalTradingVo;
import com.bigstock.sharedComponent.entity.StockThreeInstitutionalTrading;
import com.bigstock.sharedComponent.repository.StockThreeInstitutionalTradingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ThreeInstiQueryService {

    private final StockThreeInstitutionalTradingRepository repository;

    public List<ThreeInstitutionalTradingVo> queryStockRange(
            String stockCode,
            String marketNullable,
            LocalDate startDate,
            LocalDate endDate
    ) {
        String market = normalizeMarketNullable(marketNullable);
        List<StockThreeInstitutionalTrading> list =
                repository.findByStockCodeAndDateRange(stockCode, market, startDate, endDate);

        return list.stream().map(this::toVo).toList();
    }

    public Page<ThreeInstitutionalTradingVo> queryByDate(
            LocalDate tradeDate,
            String marketNullable,
            int page,
            int size
    ) {
        String market = normalizeMarketNullable(marketNullable);
        Pageable pageable = PageRequest.of(safePage(page), safeSize(size));
        return repository.findByTradeDate(tradeDate, market, pageable).map(this::toVo);
    }

    public Page<ThreeInstitutionalTradingVo> rank(
            String type, // foreign / trust / dealer
            LocalDate tradeDate,
            String marketNullable,
            int page,
            int size
    ) {
        String market = normalizeMarketNullable(marketNullable);
        Pageable pageable = PageRequest.of(safePage(page), safeSize(size));

        return switch (safe(type).toLowerCase()) {
            case "trust", "investmenttrust", "投信" ->
                    repository.rankInvestmentTrustNetBuy(tradeDate, market, pageable).map(this::toVo);
            case "dealer", "自營商" ->
                    repository.rankDealerNetBuy(tradeDate, market, pageable).map(this::toVo);
            default ->
                    repository.rankForeignNetBuy(tradeDate, market, pageable).map(this::toVo);
        };
    }

    private ThreeInstitutionalTradingVo toVo(StockThreeInstitutionalTrading e) {
        ThreeInstitutionalTradingVo vo = new ThreeInstitutionalTradingVo();
        vo.setStockCode(e.getId().getStockCode());
        vo.setMarket(e.getId().getMarket());
        vo.setTradeDate(e.getId().getTradeDate().toString()); // LocalDate -> yyyy-MM-dd

        vo.setStockName(e.getStockName());

        vo.setForeignBuy(e.getForeignBuy());
        vo.setForeignSell(e.getForeignSell());
        vo.setInvestmentTrustBuy(e.getInvestmentTrustBuy());
        vo.setInvestmentTrustSell(e.getInvestmentTrustSell());
        vo.setDealerBuy(e.getDealerBuy());
        vo.setDealerSell(e.getDealerSell());

        vo.setForeignNet(e.getForeignBuy() - e.getForeignSell());
        vo.setInvestmentTrustNet(e.getInvestmentTrustBuy() - e.getInvestmentTrustSell());
        vo.setDealerNet(e.getDealerBuy() - e.getDealerSell());
        return vo;
    }

    private static String normalizeMarketNullable(String market) {
        String m = safe(market).toUpperCase();
        return m.isBlank() ? null : m; // null 表示不過濾市場
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static int safePage(int page) {
        return Math.max(page, 0);
    }

    private static int safeSize(int size) {
        if (size <= 0) return 50;
        return Math.min(size, 200); // 防止一次撈太大
    }
}