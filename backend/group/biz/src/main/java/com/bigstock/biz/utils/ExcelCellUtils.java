package com.bigstock.biz.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;

public class ExcelCellUtils {

    private ExcelCellUtils() {}

    public static String getCellString(Cell cell) {
        if (cell == null) return null;

        try {
            CellType type = cell.getCellType();
            if (type == CellType.STRING) return safe(cell.getStringCellValue());
            if (type == CellType.NUMERIC) {
                if (DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toString();
                double d = cell.getNumericCellValue();
                // 盡量不要帶小數
                long l = (long) d;
                return String.valueOf(l);
            }
            if (type == CellType.BOOLEAN) return String.valueOf(cell.getBooleanCellValue());
            if (type == CellType.FORMULA) {
                // 優先用公式結果的字串
                try {
                    return safe(cell.getStringCellValue());
                } catch (Exception ignore) {
                    double d = cell.getNumericCellValue();
                    return String.valueOf((long) d);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Long getCellLong(Cell cell) {
        if (cell == null) return null;

        try {
            CellType type = cell.getCellType();
            if (type == CellType.NUMERIC) {
                return (long) cell.getNumericCellValue();
            }
            String s = getCellString(cell);
            if (s == null) return null;
            s = s.replace(",", "").trim();
            if (s.isBlank() || "-".equals(s)) return null;
            // 有些表格可能是 (123) 代表負數
            boolean neg = s.startsWith("(") && s.endsWith(")");
            if (neg) s = s.substring(1, s.length() - 1);
            long v = Long.parseLong(s);
            return neg ? -v : v;
        } catch (Exception e) {
            return null;
        }
    }

    private static String safe(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isBlank() ? null : t;
    }
}
