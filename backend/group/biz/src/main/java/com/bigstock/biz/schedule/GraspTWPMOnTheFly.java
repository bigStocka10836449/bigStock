package com.bigstock.biz.schedule;

import java.util.List;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.bigstock.biz.dto.MarketSnapshot;
import com.bigstock.biz.dto.NewsDataInfo;
import com.bigstock.biz.service.MarketBroadcastService;
import com.bigstock.sharedComponent.utils.ChromeDriverUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GraspTWPMOnTheFly {


    private final MarketBroadcastService broadcastService;
    private final ObjectMapper objectMapper;

    private static final String REDIS_NEWS_CHANNEL = "/topic/NewsData";
    private static final String REDIS_TWPMN_CHANNEL = "/topic/marketTWPMN";
    private static final String REDIS_TWPM_CHANNEL = "/topic/marketTWPM";
    private static final String REDIS_NASDAQ_F_CHANNEL = "/topic/marketNASDAQ_F";
    private static final String REDIS_S_P_500_F_CHANNEL = "/topic/marketS_P_500_F";
    
    private static final String REDIS_DOW_JONES_F_CHANNEL = "/topic/marketDOW_JONES_F";
    
    private static final String REDIS_PHLX_SEMICONDUCTOR_SECTOR_CHANNEL = "/topic/marketPHLX_SEMICONDUCTOR_SECTOR";
    
    private static final String TWSE_PM_NIGHT_ON_FLY_URL =
        "https://tw.stock.yahoo.com/_td-stock/api/resource/FinanceChartService.ApacLibraCharts;symbols=%5B%22WTX%26%22%5D;type=tick?bkt=c1-stock-pc-homepage&device=desktop&ecma=modern&feature=enableGAMAds%2CenableGAMEdgeToEdge%2CenableEvPlayer%2CuseCG%2CuseCGV2&intl=tw&partner=none&prid=5sf7v61kpdgps&region=TW&site=finance&tz=Asia%2FTaipei";

    private static final String TWSE_PM_ON_FLY_URL =
            """
    		https://tw.stock.yahoo.com/_td-stock/api/resource/FinanceChartService.ApacLibraCharts;symbols=%5B%22WTX00%22%5D;type=tick?bkt=c1-stock-pc-homepage&device=desktop&ecma=modern&feature=enableGAMAds%2CenableGAMEdgeToEdge%2CenableEvPlayer%2CuseCG%2CuseCGV2&intl=tw&partner=none&prid=5m7ec31kpdhbg&region=TW&site=finance&tz=Asia%2FTaipei
    		""";
    
    private static final String NASDAQ_F_ON_FLY_URL = """
    		https://query1.finance.yahoo.com/v8/finance/chart/MNQ=F?interval=1m&includePrePost=true&events=div%7Csplit%7Cearn&region=US&source=cosaic
    		""";
    
    private static final String DOW_JONES_F_ON_FLY_URL = """
    		https://query1.finance.yahoo.com/v8/finance/chart/MYM=F?interval=1m&includePrePost=true&events=div%7Csplit%7Cearn&region=HK&source=cosaic
    		""";
    
    private static final String S_P_500_F_ON_FLY_URL = """
    		https://query1.finance.yahoo.com/v8/finance/chart/MES=F?interval=1m&includePrePost=true&events=div%7Csplit%7Cearn&region=US&source=cosaic
    		""";
    
    
    private static final String PHLX_SEMICONDUCTOR_SECTOR_ON_FLY_URL = """
    		https://query1.finance.yahoo.com/v8/finance/chart/%5ESOX?interval=1m&includePrePost=true&events=div%7Csplit%7CearnS&region=US&source=cosaic
    		""";
    
    
    @Scheduled(fixedDelayString = "${bigstock.market.interval-ms:18000}")
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
    
    @Scheduled(fixedDelayString = "${bigstock.market.interval-ms:18000}")
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
    
    @Scheduled(fixedDelayString = "${bigstock.market.interval-ms:18000}")
    public void fetchNASDAQFData() {

        try {
            String data = ChromeDriverUtils.fetchApiData(NASDAQ_F_ON_FLY_URL);

            MarketSnapshot snapshot = new MarketSnapshot();
            snapshot.setData(data);

            String json = objectMapper.writeValueAsString(snapshot);

            broadcastService.broadcastMarketData(REDIS_NASDAQ_F_CHANNEL,json);

        } catch (Exception e) {
            log.error("Error fetching market data", e);
        }
    }
    
    @Scheduled(fixedDelayString = "${bigstock.market.interval-ms:18000}")
    public void fetchDOWJONESFData() {

        try {
            String data = ChromeDriverUtils.fetchApiData(DOW_JONES_F_ON_FLY_URL);

            MarketSnapshot snapshot = new MarketSnapshot();
            snapshot.setData(data);

            String json = objectMapper.writeValueAsString(snapshot);

            broadcastService.broadcastMarketData(REDIS_DOW_JONES_F_CHANNEL,json);

        } catch (Exception e) {
            log.error("Error fetching market data", e);
        }
    }    
    
    @Scheduled(fixedDelayString = "${bigstock.market.interval-ms:18000}")
    public void fetchSP500FData() {

        try {
            String data = ChromeDriverUtils.fetchApiData(S_P_500_F_ON_FLY_URL);

            MarketSnapshot snapshot = new MarketSnapshot();
            snapshot.setData(data);

            String json = objectMapper.writeValueAsString(snapshot);

            broadcastService.broadcastMarketData(REDIS_S_P_500_F_CHANNEL,json);

        } catch (Exception e) {
            log.error("Error fetching market data", e);
        }
    }
    
    @Scheduled(fixedDelayString = "${bigstock.market.interval-ms:18000}")
    public void fetchPHLXSemiconductorSectorData() {

        try {
            String data = ChromeDriverUtils.fetchApiData(PHLX_SEMICONDUCTOR_SECTOR_ON_FLY_URL);

            MarketSnapshot snapshot = new MarketSnapshot();
            snapshot.setData(data);

            String json = objectMapper.writeValueAsString(snapshot);

            broadcastService.broadcastMarketData(REDIS_PHLX_SEMICONDUCTOR_SECTOR_CHANNEL,json);

        } catch (Exception e) {
            log.error("Error fetching market data", e);
        }
    }
    
    public void newsDataBroadCase(String newsDataInfoStr, String title, String content) {

        try {

            MarketSnapshot snapshot = new MarketSnapshot();
            snapshot.setData(newsDataInfoStr);

            String json = objectMapper.writeValueAsString(snapshot);

            broadcastService.broadcastMarketData(json,title, content, content);

        } catch (Exception e) {
            log.error("Error fetching market data", e);
        }
    }
}
