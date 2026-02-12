package com.bigstock.sharedComponent.service;

import com.bigstock.sharedComponent.dto.StockBasicInfoResponse;
import com.bigstock.sharedComponent.entity.StockBasicInfo;
import com.bigstock.sharedComponent.repository.StockBasicInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockBasicInfoQueryService {

    private final StockBasicInfoRepository repository;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public Optional<StockBasicInfoResponse> queryOne(String stockId, String market) {
        if (stockId == null || stockId.trim().isEmpty()) return Optional.empty();
        if (market == null || market.trim().isEmpty()) return Optional.empty();

        return repository.findOne(stockId.trim(), market.trim())
                .map(this::toResponse);
    }

    public List<StockBasicInfoResponse> queryAllMarkets(String stockId) {
        if (stockId == null || stockId.trim().isEmpty()) return new ArrayList<>();
        List<StockBasicInfo> rows = repository.findAllByStockId(stockId.trim());
        List<StockBasicInfoResponse> out = new ArrayList<>();
        for (StockBasicInfo e : rows) out.add(toResponse(e));
        return out;
    }

    private StockBasicInfoResponse toResponse(StockBasicInfo e) {
        StockBasicInfoResponse r = new StockBasicInfoResponse();
        r.setStockId(e.getId().getStockId());
        r.setMarket(e.getId().getMarket());
        r.setStockName(e.getStockName());
        r.setMainBusiness(e.getMainBusiness());
        r.setIndustryCategory(e.getIndustryCategory());
        r.setListingDate(e.getListingDate() == null ? null : e.getListingDate().format(YMD));
        return r;
    }
}
