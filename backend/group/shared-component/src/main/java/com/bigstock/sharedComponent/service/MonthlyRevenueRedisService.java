package com.bigstock.sharedComponent.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.MonthlyRevenueVo;
import com.bigstock.sharedComponent.dto.StockRevenueResponse;
import com.bigstock.sharedComponent.redis.StockRevenueRedisRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MonthlyRevenueRedisService {

    private final StockRevenueRedisRepository redisRepository;

    // ============================================================
    // EXISTS
    // ============================================================

    public boolean exists(String yearMonth) {

        return redisRepository.getMonthlyJson(yearMonth) != null;
    }

    // ============================================================
    // READ
    // ============================================================

    public List<MonthlyRevenueVo> read(String yearMonth) {

        List<StockRevenueResponse> list =
                redisRepository.getMonthlyList(yearMonth);

        return toMonthlyRevenueVoList(list);
    }

    // ============================================================
    // WRITE
    //
    // 保留原本的方法簽章。
    //
    // 將 MonthlyRevenueVo 轉成統一的 Redis DTO。
    // ============================================================

    public void write(
            String yearMonth,
            List<MonthlyRevenueVo> list) {

        List<StockRevenueResponse> data =
                new ArrayList<>();

        if (list != null) {

            for (MonthlyRevenueVo vo : list) {

                if (vo == null) {
                    continue;
                }

                StockRevenueResponse dto =
                        new StockRevenueResponse();

                dto.setStockId(vo.getStockId());
                dto.setStockName(vo.getStockName());
                dto.setMarket(
                        vo.getMarket() == null
                                ? null
                                : vo.getMarket()
                                        .trim()
                                        .toUpperCase(Locale.ROOT)
                );
                dto.setDate(yearMonth);
                dto.setRevenue(vo.getRevenue());

                data.add(dto);
            }
        }

        redisRepository.saveMonthlyList(
                yearMonth,
                data
        );
    }

    // ============================================================
    // DTO MAPPING
    // ============================================================

    public List<MonthlyRevenueVo> toMonthlyRevenueVoList(
            List<StockRevenueResponse> list) {

        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        List<MonthlyRevenueVo> result =
                new ArrayList<>(list.size());

        for (StockRevenueResponse dto : list) {

            if (dto == null) {
                continue;
            }

            MonthlyRevenueVo vo =
                    new MonthlyRevenueVo();

            vo.setStockId(dto.getStockId());
            vo.setStockName(dto.getStockName());
            vo.setMarket(dto.getMarket());
            vo.setDate(dto.getDate());
            vo.setRevenue(dto.getRevenue());

            result.add(vo);
        }

        return result;
    }
}