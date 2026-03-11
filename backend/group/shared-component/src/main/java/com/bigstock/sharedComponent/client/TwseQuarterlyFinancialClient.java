package com.bigstock.sharedComponent.client;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.springframework.stereotype.Component;

import com.bigstock.sharedComponent.QuarterlyFinancialExcelParser;
import com.bigstock.sharedComponent.dto.QuarterlyFinancialVo;
import com.bigstock.sharedComponent.utils.HttpDownloadUtils;
import com.bigstock.sharedComponent.utils.ZipExtractUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TwseQuarterlyFinancialClient {

    private final QuarterlyFinancialExcelParser parser;

    private static String buildUrl(int year, int quarter) {
        return "https://www.twse.com.tw/staticFiles/inspection/inspection/05/001/"
                + year + "Q" + quarter + "_C05001.zip";
    }

    public List<QuarterlyFinancialVo> fetch(int year, int quarter) {
        byte[] zipBytes = HttpDownloadUtils.downloadBytes(buildUrl(year, quarter));
        byte[] xlsBytes = ZipExtractUtils.extractFirstFileBytes(zipBytes, ".xls");
        if (xlsBytes == null) {
            throw new RuntimeException("TWSE zip does not contain xls");
        }
        return parser.parse(new ByteArrayInputStream(xlsBytes), year, quarter, "TWSE");
    }
}
