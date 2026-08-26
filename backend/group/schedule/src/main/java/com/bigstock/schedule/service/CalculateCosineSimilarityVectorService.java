package com.bigstock.schedule.service;


import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.bigstock.sharedComponent.dto.StockTrendCache;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.redis.StockTrendRedisService;
import com.bigstock.sharedComponent.service.StockDayPriceService;
import com.bigstock.sharedComponent.service.StockInfoService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Service
@RequiredArgsConstructor
@Slf4j
public class CalculateCosineSimilarityVectorService {

	private static final RestTemplate restTemplate = new RestTemplate();
    private final StockDayPriceService stockDayPriceService;
    private final StockTrendRedisService stockTrendRedisService;
    private final StockInfoService stockInfoService;

    @Value("${schedule.task.scheduling.similiar-search-reload}")
    private String similiarSearchReloadUrl;

//    @PostConstruct
    @Async("stockVectorExecutor")
    public void calculateAsDailyAspect() {

        try {

            List<String> tpexStockCodes =
                    stockInfoService
                            .getStockCodeByStockType("0")
                            .stream()
                            .filter(code -> !code.matches(".*[a-zA-Z].*"))
                            .toList();

            List<String> twseStockCodes =
                    stockInfoService
                            .getStockCodeByStockType("1")
                            .stream()
                            .filter(code -> !code.matches(".*[a-zA-Z].*"))
                            .toList();

            List<String> allStockCodes = new ArrayList<>();

            allStockCodes.addAll(tpexStockCodes);
            allStockCodes.addAll(twseStockCodes);


            int chunkSize = 100;

            log.info(
                    "Start stock vector calculation, total={}",
                    allStockCodes.size()
            );


            for (
                    int i = 0;
                    i < allStockCodes.size();
                    i += chunkSize
            ) {

                List<String> chunk =
                        allStockCodes.subList(
                                i,
                                Math.min(
                                        i + chunkSize,
                                        allStockCodes.size()
                                )
                        );


                List<StockTrendCache> stockTrendCaches =
                        chunk.stream()
                                .map(this::calculateOneStock)
                                .filter(Objects::nonNull)
                                .toList();


                if (!stockTrendCaches.isEmpty()) {

                    stockTrendRedisService.saveBatch(
                            stockTrendCaches
                    );
                }


                log.info(
                        "Stock vector chunk completed, progress={}/{}",
                        Math.min(
                                i + chunkSize,
                                allStockCodes.size()
                        ),
                        allStockCodes.size()
                );
            }


            log.info(
                    "Finished stock vector calculation"
            );


            reloadPythonCache();


        } catch (Exception e) {

            log.error(
                    "Stock vector calculation failed",
                    e
            );
        }
    }


    private StockTrendCache calculateOneStock(
            String stockCode
    ) {

        try {

            log.debug(
                    "Calculate stock vector, stockCode={}",
                    stockCode
            );


            List<StockDayPrice> prices =
                    stockDayPriceService
                            .findLastest600StockDayPriceByStockCodeCache(
                                    stockCode
                            );


            if (CollectionUtils.isEmpty(prices)) {

                log.warn(
                        "Skip stock vector calculation, no prices, stockCode={}",
                        stockCode
                );

                return null;
            }

            /*
             * DEBUG duplicate trading dates BEFORE calculationVectors()
             */
            Map<LocalDate, List<StockDayPrice>> groupedByDate =
                    prices.stream()
                            .collect(
                                    Collectors.groupingBy(
                                            item ->
                                                    item.getTradingDay()
                                                            .toInstant()
                                                            .atZone(
                                                                    ZoneId.systemDefault()
                                                            )
                                                            .toLocalDate()
                                    )
                            );

            groupedByDate.forEach(
                    (tradingDate, records) -> {

                        if (records.size() <= 1) {
                            return;
                        }

                        log.error(
                                "SOURCE DUPLICATE FOUND "
                                        + "stockCode={}, tradingDate={}, count={}",
                                stockCode,
                                tradingDate,
                                records.size()
                        );

                        for (int i = 0; i < records.size(); i++) {

                            StockDayPrice record =
                                    records.get(i);

                            log.error(
                                    "duplicate[{}] "
                                            + "rawTradingDay={}, "
                                            + "close={}, "
                                            + "volume={}, "
                                            + "ma5={}, "
                                            + "ma10={}, "
                                            + "ma20={}, "
                                            + "ma60={}, "
                                            + "entity={}",
                                    i,
                                    record.getTradingDay(),
                                    record.getClosingPrice(),
                                    record.getTradingVolume(),
                                    record.getFiveDaysMa(),
                                    record.getTenDaysMa(),
                                    record.getTwentyDaysMa(),
                                    record.getSixtyDaysMa(),
                                    record
                            );
                        }
                    }
            );


            return stockDayPriceService
                    .calculationVectors(prices);


        } catch (Exception e) {

            log.error(
                    "Failed to calculate stock vector, stockCode={}",
                    stockCode,
                    e
            );

            return null;
        }
    }


    private void reloadPythonCache() {



        try {

            log.info(
                    "Calling Python cache reload"
            );


            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                    		similiarSearchReloadUrl,
                            null,
                            String.class
                    );


            log.info(
                    "Python cache reload completed, status={}, response={}",
                    response.getStatusCode(),
                    response.getBody()
            );


        } catch (Exception e) {

            log.error(
                    "Failed to reload Python cache",
                    e
            );
        }
    }
}