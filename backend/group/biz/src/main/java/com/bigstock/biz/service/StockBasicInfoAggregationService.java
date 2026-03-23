package com.bigstock.biz.service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.bigstock.biz.dto.StockBasicInfoResponse;
import com.bigstock.sharedComponent.client.CompanyInfoClient;
import com.bigstock.sharedComponent.dto.CompanyInfo;
import com.bigstock.sharedComponent.dto.QuarterlyFinancialVo;
import com.bigstock.sharedComponent.dto.SingleStockDayPriceVo;
import com.bigstock.sharedComponent.dto.SingleStockPriceVo;
import com.bigstock.sharedComponent.dto.SingleStockWeekPriceVo;
import com.bigstock.sharedComponent.service.QuarterlyFinancialRedisService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockBasicInfoAggregationService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    // ✅ 你說的：StockPriceQueryService 其實是 BizService
    private final BizService bizService;

    // ✅ 你說的：QuarterlyFinancialQueryService 其實是 QuarterlyFinancialRedisService
    private final QuarterlyFinancialRedisService quarterlyFinancialRedisService;

    // biz 內 client（直接打 TWSE PDF / TPEX POST）
    private final CompanyInfoClient companyInfoClient;

    private static final ZoneId TZ = ZoneId.of("Asia/Taipei");
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final int ONE_YEAR_TRADING_DAYS = 240;
    private static final int THREE_YEAR_WEEKS = 156; // 約 3 年

    private static String redisKey(String stockId, String market) {
        String m = (market == null || market.isBlank()) ? "ALL" : market.trim().toUpperCase();
        return "STOCK:BASIC:V1:" + stockId.trim() + ":" + m;
    }

    public StockBasicInfoResponse readCache(String stockId, String market) {
        try {
            String json = redis.opsForValue().get(redisKey(stockId, market));
            if (json == null || json.isBlank()) return null;
            return objectMapper.readValue(json, StockBasicInfoResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    public StockBasicInfoResponse queryAndCache(String stockId, String market) {
        StockBasicInfoResponse resp = build(stockId, market);
        try {
            redis.opsForValue().set(redisKey(stockId, market), objectMapper.writeValueAsString(resp));
            redis.expire(redisKey(stockId, market), java.time.Duration.ofDays(1));
        } catch (Exception ignore) {}
        return resp;
    }

    private StockBasicInfoResponse build(String stockId, String market) {
        String sid = safe(stockId);
        String mkt = (market == null || market.isBlank()) ? null : market.trim().toUpperCase();

        // 1) 交易所資料（主要經營業務 / 產業分類 / 掛牌日）
        CompanyInfo company = companyInfoClient.fetchBestEffort(sid, mkt);

        // 2) 財報：從 Redis 往回掃 16 季，找最新有該 stockId 的那筆
        QuarterlyFinancialVo fin = findLatestQuarterlyFinancialFromRedis(sid, mkt);

        // 3) 股價：用 BizService 取日/週資料
        SingleStockPriceVo priceVo;
        try {
            priceVo = bizService.getSingleStockPrices(sid, null, null);
        } catch (ParseException e) {
            throw new RuntimeException("getSingleStockPrices failed: " + sid, e);
        }

        List<SingleStockDayPriceVo> days = priceVo == null ? List.of() : safeList(priceVo.getSingleStockDayPriceVos());
        List<SingleStockWeekPriceVo> weeks = priceVo == null ? List.of() : safeList(priceVo.getSingleStockWeekPriceVos());

        // 3.1) 市值用價：最新月份第一個交易日收盤價（從日K）
        MonthFirstPrice monthFirst = pickLatestMonthFirstClose(days);

        // 3.2) 近一年高低（日K close 最後 240）
        Double oneYearHigh = calcHighFromDayClose(days, ONE_YEAR_TRADING_DAYS);
        Double oneYearLow  = calcLowFromDayClose(days, ONE_YEAR_TRADING_DAYS);

        // 3.3) 近三年高低（週K close 最後 156）
        Double threeYearHigh = calcHighFromWeekClose(weeks, THREE_YEAR_WEEKS);
        Double threeYearLow  = calcLowFromWeekClose(weeks, THREE_YEAR_WEEKS);

        // 4) 組回傳
        StockBasicInfoResponse out = new StockBasicInfoResponse();
        out.setStockId(sid);
        out.setMarket(mkt);

        // stockName：優先財報，其次日K vo 內
        String stockName = fin != null ? fin.getStockName() : pickAnyStockName(days);
        out.setStockName(stockName);

        out.setMainBusiness(company == null ? null : company.getMainBusiness());
        out.setIndustryCategory(company == null ? null : company.getIndustryCategory());
        out.setListingDate(company == null ? null : company.getListingDate());
        out.setSubIndustryCategory(null);

        out.setCapitalStock(fin == null ? null : fin.getCapitalStockEndPeriod());
        out.setNetAssetValuePerShare(fin == null ? null : fin.getNetAssetValuePerShare());
        out.setEarningsPerShare(fin == null ? null : fin.getEarningsPerShare());

        out.setMarketCapPriceDate(monthFirst == null ? null : monthFirst.date);
        out.setMarketCapPrice(monthFirst == null ? null : monthFirst.close);

        // 市值 = 股本 * 月初收盤（股本單位你之後再統一，先讓流程跑）
        Long marketCap = null;
        if (monthFirst != null && monthFirst.close != null && fin != null && fin.getCapitalStockEndPeriod() != null) {
            marketCap = Math.round(monthFirst.close * fin.getCapitalStockEndPeriod());
        }
        out.setMarketCap(marketCap);

        // 本益比 = 價 / EPS
        Double pe = null;
        if (monthFirst != null && monthFirst.close != null && fin != null && fin.getEarningsPerShare() != null) {
            Double eps = fin.getEarningsPerShare();
            if (eps != 0.0) pe = round2(monthFirst.close / eps);
        }
        out.setPeRatio(pe);

        out.setOneYearHigh(oneYearHigh);
        out.setOneYearLow(oneYearLow);
        out.setThreeYearHigh(threeYearHigh);
        out.setThreeYearLow(threeYearLow);

        out.setMaStatus(0); // ✅ 先固定 0

        return out;
    }

    // -------------------------
    // Redis 掃季度：找最新該股財報
    // -------------------------
    private QuarterlyFinancialVo findLatestQuarterlyFinancialFromRedis(String stockId, String market) {
        if (market == null || market.isBlank()) {
            // 沒給 market：先試 TWSE，再試 TPEX（你可依你實際命名調整）
            QuarterlyFinancialVo a = findLatestQuarterlyFinancialFromRedis(stockId, "TWSE");
            if (a != null) return a;
            return findLatestQuarterlyFinancialFromRedis(stockId, "TPEX");
        }

        LocalDate now = LocalDate.now(TZ);
        int year = now.getYear();
        int quarter = ((now.getMonthValue() - 1) / 3) + 1;

        // 往回掃 16 季（4 年）
        int y = year;
        int q = quarter;
        for (int i = 0; i < 16; i++) {
            List<QuarterlyFinancialVo> list = quarterlyFinancialRedisService.read(y, q, market);
            if (list != null && !list.isEmpty()) {
                for (QuarterlyFinancialVo v : list) {
                    if (v != null && stockId.equals(v.getStockId())) {
                        return v;
                    }
                }
            }
            // 上一季
            q--;
            if (q == 0) {
                q = 4;
                y--;
            }
        }
        return null;
    }

    // -------------------------
    // 月初收盤：最新月份第一個交易日
    // -------------------------
    private static class MonthFirstPrice {
        String date;   // yyyy-MM-dd
        Double close;
    }

    private MonthFirstPrice pickLatestMonthFirstClose(List<SingleStockDayPriceVo> daysDesc) {
        if (daysDesc == null || daysDesc.isEmpty()) return null;

        // BizService day list 是倒序（新→舊）【:contentReference[oaicite:4]{index=4}】
        // 找最新日期所屬月份，再在同月份中找最早日期
        LocalDate newest = parseDateSafe(daysDesc.get(0).getTradingDate());
        if (newest == null) return null;

        int y = newest.getYear();
        int m = newest.getMonthValue();

        LocalDate minDate = null;
        Double close = null;

        for (SingleStockDayPriceVo vo : daysDesc) {
            LocalDate d = parseDateSafe(vo.getTradingDate());
            if (d == null) continue;
            if (d.getYear() != y || d.getMonthValue() != m) continue;

            if (minDate == null || d.isBefore(minDate)) {
                minDate = d;
                close = parseDoubleSafe(vo.getClosingPrice());
            }
        }

        if (minDate == null) return null;
        MonthFirstPrice out = new MonthFirstPrice();
        out.date = minDate.format(DF);
        out.close = close;
        return out;
    }

    // -------------------------
    // 高低計算
    // -------------------------
    private static Double calcHighFromDayClose(List<SingleStockDayPriceVo> daysDesc, int takeN) {
        if (daysDesc == null || daysDesc.isEmpty()) return null;
        int limit = Math.min(takeN, daysDesc.size());
        Double max = null;
        for (int i = 0; i < limit; i++) {
            Double c = parseDoubleSafe(daysDesc.get(i).getClosingPrice());
            if (c == null) continue;
            if (max == null || c > max) max = c;
        }
        return max == null ? null : round2(max);
    }

    private static Double calcLowFromDayClose(List<SingleStockDayPriceVo> daysDesc, int takeN) {
        if (daysDesc == null || daysDesc.isEmpty()) return null;
        int limit = Math.min(takeN, daysDesc.size());
        Double min = null;
        for (int i = 0; i < limit; i++) {
            Double c = parseDoubleSafe(daysDesc.get(i).getClosingPrice());
            if (c == null) continue;
            if (min == null || c < min) min = c;
        }
        return min == null ? null : round2(min);
    }

    private static Double calcHighFromWeekClose(List<SingleStockWeekPriceVo> weeksAsc, int takeLastN) {
        if (weeksAsc == null || weeksAsc.isEmpty()) return null;

        int size = weeksAsc.size();
        int from = Math.max(0, size - takeLastN);

        Double max = null;
        for (int i = from; i < size; i++) {
            Double c = parseDoubleSafe(weeksAsc.get(i).getClosingPrice());
            if (c == null) continue;
            if (max == null || c > max) max = c;
        }
        return max == null ? null : round2(max);
    }

    private static Double calcLowFromWeekClose(List<SingleStockWeekPriceVo> weeksAsc, int takeLastN) {
        if (weeksAsc == null || weeksAsc.isEmpty()) return null;

        int size = weeksAsc.size();
        int from = Math.max(0, size - takeLastN);

        Double min = null;
        for (int i = from; i < size; i++) {
            Double c = parseDoubleSafe(weeksAsc.get(i).getClosingPrice());
            if (c == null) continue;
            if (min == null || c < min) min = c;
        }
        return min == null ? null : round2(min);
    }

    // -------------------------
    // util
    // -------------------------
    private static String pickAnyStockName(List<SingleStockDayPriceVo> days) {
        if (days == null || days.isEmpty()) return null;
        String n = days.get(0).getStockName();
        return (n == null || n.isBlank()) ? null : n;
    }

    private static LocalDate parseDateSafe(String s) {
        try {
            if (s == null || s.isBlank()) return null;
            return LocalDate.parse(s.trim(), DF);
        } catch (Exception e) {
            return null;
        }
    }

    private static Double parseDoubleSafe(String s) {
        try {
            if (s == null || s.isBlank()) return null;
            String t = s.replace(",", "").trim();
            return new BigDecimal(t).doubleValue();
        } catch (Exception e) {
            return null;
        }
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }
}
