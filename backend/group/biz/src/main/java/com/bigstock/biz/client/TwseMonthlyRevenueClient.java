package com.bigstock.biz.client;

import com.bigstock.biz.parser.MonthlyRevenueExcelParser;
import com.bigstock.biz.utils.HttpDownloadUtils;
import com.bigstock.biz.utils.ZipExtractUtils;
import com.bigstock.biz.vo.MonthlyRevenueVo;
import com.google.common.collect.Lists;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TwseMonthlyRevenueClient {

    private final MonthlyRevenueExcelParser parser;

    /**
     * 你提供的規則：
     * https://www.twse.com.tw/.../003/YYYYMM_C04003.zip
     * 只有 YYYYMM 會變
     */
    private static String buildUrl(String yyyyMM) {
        return "https://www.twse.com.tw/staticFiles/inspection/inspection/04/003/"
                + yyyyMM + "_C04003.zip";
    }

    public List<MonthlyRevenueVo> fetchMonthlyRevenue(String yearMonth) {
        String yyyyMM = yearMonth.replace("-", "");
        byte[] zipBytes = HttpDownloadUtils.downloadBytes(buildUrl(yyyyMM));

        // ZIP 裡找第一個 .xls，抽出 bytes
        byte[] xlsBytes = ZipExtractUtils.extractFirstFileBytes(zipBytes, ".xls");
        if (xlsBytes == null) {
            return Lists.newArrayList();
        }

        // 交給 parser 解析
        List<MonthlyRevenueVo> list = parser.parseXls(
                new ByteArrayInputStream(xlsBytes),
                yearMonth,
                "TWSE"
        );
        return list;
    }
}
