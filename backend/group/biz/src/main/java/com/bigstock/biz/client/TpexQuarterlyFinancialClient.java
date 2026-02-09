package com.bigstock.biz.client;

import com.bigstock.biz.parser.QuarterlyFinancialExcelParser;
import com.bigstock.biz.vo.QuarterlyFinancialVo;
import com.bigstock.biz.utils.HttpDownloadUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.List;

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
