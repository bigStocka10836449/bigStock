package com.bigstock.sharedComponent.utils;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RocYearMonthUtils {

    // 支援：民國114年11月 / 民國 114 年 11 月
    private static final Pattern ROC_YM = Pattern.compile("民國\\s*(\\d{2,3})\\s*年\\s*(\\d{1,2})\\s*月");

    private RocYearMonthUtils() {}

    public static String findAndConvertRocYearMonth(Sheet sheet) {
        int maxScan = Math.min(20, sheet.getLastRowNum());
        for (int r = 0; r <= maxScan; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String rowText = rowToText(row);
            Matcher m = ROC_YM.matcher(rowText);
            if (m.find()) {
                int rocYear = Integer.parseInt(m.group(1));
                int month = Integer.parseInt(m.group(2));
                int adYear = rocYear + 1911;
                return String.format("%04d-%02d", adYear, month);
            }
        }
        return null;
    }

    private static String rowToText(Row row) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < row.getLastCellNum(); c++) {
            String s = ExcelCellUtils.getCellString(row.getCell(c));
            if (s != null) sb.append(s).append(" ");
        }
        return sb.toString();
    }
}
