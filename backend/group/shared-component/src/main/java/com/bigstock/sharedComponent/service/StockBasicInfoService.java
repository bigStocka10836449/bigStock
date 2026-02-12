package com.bigstock.sharedComponent.service;

import com.bigstock.sharedComponent.entity.StockBasicInfo;
import com.bigstock.sharedComponent.entity.StockBasicInfoId;
import com.bigstock.sharedComponent.repository.StockBasicInfoRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockBasicInfoService {

    private final StockBasicInfoRepository repository;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Data
    public static class SyncResult {
        private String stockId;
        private String market;

        private boolean existedBefore;
        private boolean updated;     // 有寫入/更新 DB
        private boolean skipped;     // 因為資料新鮮或無需更新而略過

        private String message;

        private String stockName;
        private String listingDate;
    }

    public SyncResult upsertFromRaw(
            String stockId,
            String market,
            String stockName,
            String mainBusiness,
            String industryCategory,
            String listingDateYmd // nullable
    ) {
        SyncResult sr = new SyncResult();
        sr.setStockId(stockId);
        sr.setMarket(market);

        StockBasicInfoId id = new StockBasicInfoId(stockId, market);
        Optional<StockBasicInfo> existedOpt = repository.findById(id);
        sr.setExistedBefore(existedOpt.isPresent());

        StockBasicInfo e = existedOpt.orElseGet(() -> new StockBasicInfo(id));
        e.setStockName(stockName);
        e.setMainBusiness(mainBusiness);
        e.setIndustryCategory(industryCategory);

        if (listingDateYmd != null && !listingDateYmd.isBlank()) {
            // 容錯：只吃 yyyy-MM-dd；解析失敗就當 null
            try {
                e.setListingDate(LocalDate.parse(listingDateYmd.trim(), YMD));
                sr.setListingDate(listingDateYmd.trim());
            } catch (Exception ignore) {
                e.setListingDate(null);
                sr.setListingDate(null);
            }
        } else {
            e.setListingDate(null);
            sr.setListingDate(null);
        }

        repository.save(e);

        sr.setUpdated(true);
        sr.setSkipped(false);
        sr.setStockName(stockName);
        sr.setMessage(sr.isExistedBefore() ? "updated" : "inserted");
        return sr;
    }
}
