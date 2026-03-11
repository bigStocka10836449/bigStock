package com.bigstock.sharedComponent.client;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.springframework.stereotype.Component;

import com.bigstock.sharedComponent.QuarterlyFinancialExcelParser;
import com.bigstock.sharedComponent.dto.QuarterlyFinancialVo;
import com.bigstock.sharedComponent.utils.HttpDownloadUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TpexQuarterlyFinancialClient {

    private final QuarterlyFinancialExcelParser parser;

    private static String buildUrl(int year, int quarter) {
        return "https://www.tpex.org.tw/storage/statistic/financial/O_"
                + year + "Q" + quarter + ".xls";
    }

    public List<QuarterlyFinancialVo> fetch(int year, int quarter) {
        byte[] bytes = HttpDownloadUtils.downloadBytes(buildUrl(year, quarter));
        return parser.parse(new ByteArrayInputStream(bytes), year, quarter, "TPEX");
    }
}
