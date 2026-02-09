package com.bigstock.biz.client;

import com.bigstock.biz.parser.QuarterlyFinancialExcelParser;
import com.bigstock.biz.utils.HttpDownloadUtils;
import com.bigstock.biz.utils.ZipExtractUtils;
import com.bigstock.biz.vo.QuarterlyFinancialVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.List;

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
