package com.bigstock.schedule.service;



import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
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
             * Deduplicate by trading date.
             *
             * If multiple records exist on the same trading date,
             * keep the record with the latest tradingDay time.
             *
             * Example:
             *
             * 2026-09-30 00:00:00
             * 2026-09-30 14:59:43  <- keep this one
             */
            Map<LocalDate, StockDayPrice> latestPriceByDate =
                    prices.stream()
                            .collect(
                                    Collectors.toMap(

                                            // Key: trading date
                                            item ->
                                                    item.getTradingDay()
                                                            .toInstant()
                                                            .atZone(
                                                                    ZoneId.systemDefault()
                                                            )
                                                            .toLocalDate(),

                                            // Value: StockDayPrice itself
                                            Function.identity(),

                                            // Duplicate trading date
                                            (existing, incoming) -> {

                                                StockDayPrice keep;
                                                StockDayPrice discard;

                                                if (incoming.getTradingDay()
                                                        .after(existing.getTradingDay())) {

                                                    keep = incoming;
                                                    discard = existing;

                                                } else {

                                                    keep = existing;
                                                    discard = incoming;
                                                }

                                                log.warn(
                                                        "Duplicate trading date found, "
                                                                + "stockCode={}, "
                                                                + "tradingDate={}, "
                                                                + "keepTradingDay={}, "
                                                                + "discardTradingDay={}, "
                                                                + "keepClose={}, "
                                                                + "discardClose={}",
                                                        stockCode,
                                                        keep.getTradingDay()
                                                                .toInstant()
                                                                .atZone(
                                                                        ZoneId.systemDefault()
                                                                )
                                                                .toLocalDate(),
                                                        keep.getTradingDay(),
                                                        discard.getTradingDay(),
                                                        keep.getClosingPrice(),
                                                        discard.getClosingPrice()
                                                );

                                                return keep;
                                            }
                                    )
                            );

            /*
             * Convert back to List and restore chronological order.
             *
             * Do not rely on Map iteration order because
             * calculationVectors() works with time-series data.
             */
            List<StockDayPrice> deduplicatedPrices =
                    latestPriceByDate.values()
                            .stream()
                            .sorted(
                                    Comparator.comparing(
                                            StockDayPrice::getTradingDay
                                    )
                            )
                            .toList();

            if (prices.size() != deduplicatedPrices.size()) {

                log.warn(
                        "Stock prices deduplicated, "
                                + "stockCode={}, "
                                + "originalSize={}, "
                                + "deduplicatedSize={}, "
                                + "removed={}",
                        stockCode,
                        prices.size(),
                        deduplicatedPrices.size(),
                        prices.size() - deduplicatedPrices.size()
                );
            }

            return stockDayPriceService
                    .calculationVectors(
                            deduplicatedPrices
                    );

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