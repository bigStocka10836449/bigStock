package com.bigstock.sharedComponent.client;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.springframework.stereotype.Component;

import com.bigstock.sharedComponent.MonthlyRevenueExcelParser;
import com.bigstock.sharedComponent.dto.MonthlyRevenueVo;
import com.bigstock.sharedComponent.utils.HttpDownloadUtils;

import lombok.RequiredArgsConstructor;

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
