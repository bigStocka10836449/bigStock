package com.bigstock.biz.service;

import com.bigstock.sharedComponent.client.CompanyInfoClient;
import com.bigstock.sharedComponent.dto.CompanyInfo;
import com.bigstock.sharedComponent.repository.StockBasicInfoRepository;
import com.bigstock.sharedComponent.service.StockBasicInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class StockBasicInfoDbService {

    private final CompanyInfoClient companyInfoClient;

    // 用來做 freshness 檢查（避免一直重抓）
    private final StockBasicInfoRepository stockBasicInfoRepository;

    // 真正 upsert DB 的 shared-component service
    private final StockBasicInfoService stockBasicInfoService;

    /**
     * 同步單一個股基本資料到 DB
     *
     * @param stockId 必填
     * @param market 可空；空時走 CompanyInfoClient.bestEffort，回來的 CompanyInfo.market 會決定落哪個 market
     * @param forceRefresh true 時無視 freshness，必定重抓並 upsert
     * @param maxAgeHours 資料可接受的最大陳舊程度（預設 24 小時）
     */
    public StockBasicInfoService.SyncResult syncToDb(
            String stockId,
            String market,
            boolean forceRefresh,
            Integer maxAgeHours
    ) {
        if (stockId == null || stockId.trim().isEmpty()) {
            StockBasicInfoService.SyncResult sr = new StockBasicInfoService.SyncResult();
            sr.setMessage("stockId is required");
            return sr;
        }

        String sid = stockId.trim();
        String mkt = (market == null || market.isBlank()) ? null : market.trim();

        int hours = (maxAgeHours == null || maxAgeHours <= 0) ? 24 : maxAgeHours;
        Instant freshAfter = Instant.now().minus(hours, ChronoUnit.HOURS);

        // 只有 market 明確時才做 existsFresh 檢查（因為 PK 含 market）
        if (!forceRefresh && mkt != null) {
            boolean fresh = stockBasicInfoRepository.existsFresh(sid, mkt, freshAfter);
            if (fresh) {
                StockBasicInfoService.SyncResult sr = new StockBasicInfoService.SyncResult();
                sr.setStockId(sid);
                sr.setMarket(mkt);
                sr.setSkipped(true);
                sr.setUpdated(false);
                sr.setExistedBefore(true);
                sr.setMessage("skipped: record is fresh (maxAgeHours=" + hours + ")");
                return sr;
            }
        }

        // 外部抓取（best effort）
        CompanyInfo info = companyInfoClient.fetchBestEffort(sid, mkt);

        if (info == null) {
            StockBasicInfoService.SyncResult sr = new StockBasicInfoService.SyncResult();
            sr.setStockId(sid);
            sr.setMarket(mkt);
            sr.setMessage("fetch failed: CompanyInfoClient returned null");
            return sr;
        }

        if (info.getMarket() == null || info.getMarket().isBlank()) {
            // 容錯：若來源沒回 market，退回用參數 market（若也沒有，就給 UNKNOWN）
            info.setMarket(mkt != null ? mkt : "UNKNOWN");
        }

        // 如果 market 不給但抓回來的是 TWSE/TPEX，那就用抓回來的 market 做 freshness 檢查（可減少重複寫）
        if (!forceRefresh && mkt == null) {
            String resolvedMarket = info.getMarket().trim();
            boolean fresh = stockBasicInfoRepository.existsFresh(sid, resolvedMarket, freshAfter);
            if (fresh) {
                StockBasicInfoService.SyncResult sr = new StockBasicInfoService.SyncResult();
                sr.setStockId(sid);
                sr.setMarket(resolvedMarket);
                sr.setSkipped(true);
                sr.setUpdated(false);
                sr.setExistedBefore(true);
                sr.setMessage("skipped: record is fresh (resolvedMarket=" + resolvedMarket + ", maxAgeHours=" + hours + ")");
                return sr;
            }
        }

        // upsert DB（shared-component）
        return stockBasicInfoService.upsertFromRaw(
                sid,
                info.getMarket().trim(),
                safe(info.getStockName()),
                nullIfBlank(info.getMainBusiness()),
                nullIfBlank(info.getIndustryCategory()),
                nullIfBlank(info.getListingDate())
        );
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    private String nullIfBlank(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
