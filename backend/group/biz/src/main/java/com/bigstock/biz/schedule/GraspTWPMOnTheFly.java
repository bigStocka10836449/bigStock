package com.bigstock.biz.schedule;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.bigstock.biz.dto.MarketSnapshot;
import com.bigstock.biz.service.MarketBroadcastService;
import com.bigstock.biz.utils.ChromeDriverUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GraspTWPMOnTheFly {


    private final MarketBroadcastService broadcastService;
    private final ObjectMapper objectMapper;

    private static final String REDIS_TWPMN_CHANNEL = "/topic/marketTWPMN";
    private static final String REDIS_TWPM_CHANNEL = "/topic/marketTWPM";

    private static final String TWSE_PM_NIGHT_ON_FLY_URL =
        "https://tw.stock.yahoo.com/_td-stock/api/resource/FinanceChartService.ApacLibraCharts;symbols=%5B%22WTX%26%22%5D;type=tick?bkt=c1-stock-pc-homepage&device=desktop&ecma=modern&feature=enableGAMAds%2CenableGAMEdgeToEdge%2CenableEvPlayer%2CuseCG%2CuseCGV2&intl=tw&lang=zh-Hant-TW&partner=none&prid=5sf7v61kpdgps&region=TW&site=finance&tz=Asia%2FTaipei";

    private static final String TWSE_PM_ON_FLY_URL =
            """
    		https://tw.stock.yahoo.com/_td-stock/api/resource/FinanceChartService.ApacLibraCharts;symbols=%5B%22WTX00%22%5D;type=tick?bkt=c1-stock-pc-homepage&device=desktop&ecma=modern&feature=enableGAMAds%2CenableGAMEdgeToEdge%2CenableEvPlayer%2CuseCG%2CuseCGV2&intl=tw&lang=zh-Hant-TW&partner=none&prid=5m7ec31kpdhbg&region=TW&site=finance&tz=Asia%2FTaipei
    		""";
    
    @Scheduled(fixedDelayString = "${bigstock.market.interval-ms:10000}")
    public void fetchTWPMNightData() {

        try {
            String data = ChromeDriverUtils.fetchApiData(TWSE_PM_NIGHT_ON_FLY_URL);

            MarketSnapshot snapshot = new MarketSnapshot();
            snapshot.setData(data);

            String json = objectMapper.writeValueAsString(snapshot);

            broadcastService.broadcastMarketData(REDIS_TWPMN_CHANNEL,json);

        } catch (Exception e) {
            log.error("Error fetching market data", e);
        }
    }
    
    @Scheduled(fixedDelayString = "${bigstock.market.interval-ms:10000}")
    public void fetchTWPMData() {

        try {
            String data = ChromeDriverUtils.fetchApiData(TWSE_PM_ON_FLY_URL);

            MarketSnapshot snapshot = new MarketSnapshot();
            snapshot.setData(data);

            String json = objectMapper.writeValueAsString(snapshot);

            broadcastService.broadcastMarketData(REDIS_TWPM_CHANNEL,json);

        } catch (Exception e) {
            log.error("Error fetching market data", e);
        }
    }
}
