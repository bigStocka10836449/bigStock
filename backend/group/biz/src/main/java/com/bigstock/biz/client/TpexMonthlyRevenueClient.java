package com.bigstock.biz.client;

import com.bigstock.biz.parser.MonthlyRevenueExcelParser;
import com.bigstock.biz.utils.HttpDownloadUtils;
import com.bigstock.biz.vo.MonthlyRevenueVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TpexMonthlyRevenueClient {

    private final MonthlyRevenueExcelParser parser;

    /**
     * 你提供的規則：
     * https://www.tpex.org.tw/storage/statistic/sales_revenue/O_YYYYMM.xls
     */
    private static String buildUrl(String yyyyMM) {
        return "https://www.tpex.org.tw/storage/statistic/sales_revenue/O_" + yyyyMM + ".xls";
    }

    public List<MonthlyRevenueVo> fetchMonthlyRevenue(String yearMonth) {
        String yyyyMM = yearMonth.replace("-", "");
        byte[] xlsBytes = HttpDownloadUtils.downloadBytes(buildUrl(yyyyMM));

        return parser.parseXls(
                new ByteArrayInputStream(xlsBytes),
                yearMonth,
                "TPEX"
        );
    }
}
