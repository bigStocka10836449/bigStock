package com.bigstock.biz.parser;

import com.bigstock.biz.utils.ExcelCellUtils;
import com.bigstock.biz.utils.RocYearMonthUtils;
import com.bigstock.biz.vo.MonthlyRevenueVo;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MonthlyRevenueExcelParser {

    // 例如：1101  台泥 / 2330 台積電
    private static final Pattern CODE_NAME_PATTERN = Pattern.compile("^\\s*(\\d{4,6})\\s+(.+?)\\s*$");

    // TWSE 月份縮寫對照
    private static final String[] MONTH_ABBR = {
            "JAN.", "FEB.", "MAR.", "APR.", "MAY.", "JUN.",
            "JUL.", "AUG.", "SEP.", "OCT.", "NOV.", "DEC."
    };

    public List<MonthlyRevenueVo> parseXls(InputStream is, String fallbackYearMonth, String market) {
        try (HSSFWorkbook wb = new HSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);

            // date：優先從表頭「民國xxx年xx月」轉成 yyyy-MM，找不到才用 fallback
            String date = RocYearMonthUtils.findAndConvertRocYearMonth(sheet);
            if (date == null || date.isBlank()) date = fallbackYearMonth;

            // 先嘗試「一般版」（TPEX 常見：公司名稱/本月）
            ParseResult general = tryParseGeneral(sheet, date, market);
            if (general != null) return general.list;

            // 再嘗試「TWSE 版」（你貼的 JAN./FEB. 多層表頭）
            ParseResult twse = tryParseTwse(sheet, date, market);
            if (twse != null) return twse.list;

            throw new RuntimeException("Cannot locate header for either GENERAL(TPEX) or TWSE(JAN./FEB.) format.");
        } catch (Exception e) {
            throw new RuntimeException("parse xls failed: " + e.getMessage(), e);
        }
    }

    // =========================
    // 1) GENERAL (TPEX 常見)：
    //    header 同一列包含「公司名稱」+「本月」
    // =========================
    private ParseResult tryParseGeneral(Sheet sheet, String date, String market) {
        int headerRowIdx = findHeaderRowIndexGeneral(sheet);
        if (headerRowIdx < 0) return null;

        Row headerRow = sheet.getRow(headerRowIdx);
        Map<String, Integer> colMap = buildColumnMap(headerRow);

        Integer colName = findCol(colMap, "公司名稱");
        Integer colRevenue = findCol(colMap, "本月");
        Integer colCode = findCol(colMap, "公司代號"); // 可能沒有

        if (colName == null || colRevenue == null) return null;

        List<MonthlyRevenueVo> out = new ArrayList<>();

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String nameCell = ExcelCellUtils.getCellString(row.getCell(colName));
            if (nameCell == null || nameCell.isBlank()) continue;

            String nameTrim = nameCell.trim();
            if (containsAny(nameTrim, "合計", "總計", "小計")) continue;

            String stockId = null;
            String stockName = null;

            // 先用公司代號欄
            if (colCode != null) {
                String codeCell = ExcelCellUtils.getCellString(row.getCell(colCode));
                if (codeCell != null && codeCell.trim().matches("^\\d{4,6}$")) {
                    stockId = codeCell.trim();
                    stockName = nameTrim;
                }
            }

            // 沒代號欄就從公司名稱拆
            if (stockId == null) {
                Matcher m = CODE_NAME_PATTERN.matcher(nameTrim);
                if (m.find()) {
                    stockId = m.group(1).trim();
                    stockName = m.group(2).trim();
                } else {
                    stockName = nameTrim;
                }
            }

            Long revenue = ExcelCellUtils.getCellLong(row.getCell(colRevenue));

            MonthlyRevenueVo vo = new MonthlyRevenueVo();
            vo.setStockId(stockId);
            vo.setStockName(stockName);
            vo.setRevenue(revenue);
            vo.setDate(date);
            vo.setMarket(market);
            out.add(vo);
        }

        return new ParseResult(out);
    }

    private int findHeaderRowIndexGeneral(Sheet sheet) {
        int maxScan = Math.min(80, sheet.getLastRowNum());
        for (int r = 0; r <= maxScan; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String rowText = collectRowText(row);
            if (rowText.contains("公司名稱") && rowText.contains("本月")) {
                return r;
            }
        }
        return -1;
    }

    // =========================
    // 2) TWSE 格式：
    //    第一欄是 Code & Name
    //    header 會有月份縮寫：JAN. FEB. ... 且同月會出現兩次（本年度、上年度）
    //    我們要「第一個 目標月份縮寫」那一欄
    // =========================
    private ParseResult tryParseTwse(Sheet sheet, String date, String market) {
        // date = yyyy-MM
        int month = Integer.parseInt(date.substring(5, 7)); // 01~12
        String targetMonthAbbr = MONTH_ABBR[month - 1];

        // 找到「月份欄所在的 header row」與「目標月份欄 index」
        HeaderLocate hl = locateTwseHeader(sheet, targetMonthAbbr);
        if (hl == null) return null;

        int dataStartRow = hl.headerRowIdx + 1;
        int codeNameCol = hl.codeNameColIdx;
        int revenueCol = hl.revenueColIdx;

        List<MonthlyRevenueVo> out = new ArrayList<>();

        for (int r = dataStartRow; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String codeName = ExcelCellUtils.getCellString(row.getCell(codeNameCol));
            if (codeName == null || codeName.isBlank()) continue;

            String codeNameTrim = codeName.trim();

            // 只吃真正股票列：必須是「4~6位數 + 空白 + 名稱」
            Matcher m = CODE_NAME_PATTERN.matcher(codeNameTrim);
            if (!m.find()) {
                // 像「01 水泥工業類」「Foods」這種分類列，跳過
                continue;
            }

            String stockId = m.group(1).trim();
            String stockName = m.group(2).trim();

            Long revenue = ExcelCellUtils.getCellLong(row.getCell(revenueCol));

            MonthlyRevenueVo vo = new MonthlyRevenueVo();
            vo.setStockId(stockId);
            vo.setStockName(stockName);
            vo.setRevenue(revenue);
            vo.setDate(date);
            vo.setMarket(market);
            out.add(vo);
        }

        // 有抓到資料才算成功，避免誤判
        if (out.isEmpty()) return null;
        return new ParseResult(out);
    }

    /**
     * 掃描前 120 列，找到：
     * 1) 哪一列包含月份縮寫 (JAN./FEB./...)
     * 2) 同一列中找「第一個 targetMonthAbbr」作為 revenue 欄
     * 3) 同時找出「Code & Name」欄（若找不到，預設用第一欄）
     */
    private HeaderLocate locateTwseHeader(Sheet sheet, String targetMonthAbbr) {
        int maxScan = Math.min(120, sheet.getLastRowNum());

        for (int r = 0; r <= maxScan; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            // 找出此列每個 cell 的文字
            int last = row.getLastCellNum();
            if (last <= 0) continue;

            int revenueCol = -1;

            // 找第一個目標月份縮寫欄
            for (int c = 0; c < last; c++) {
                String t = ExcelCellUtils.getCellString(row.getCell(c));
                if (t == null) continue;
                if (normalize(t).equals(targetMonthAbbr)) {
                    revenueCol = c;
                    break; // 第一個命中就是本年度本月
                }
            }

            if (revenueCol < 0) continue;

            // 嘗試找 Code & Name 欄（有些檔第一欄就是公司代號+名稱）
            int codeNameCol = 0; // fallback
            // 往上/本列找一下是否有 "Code & Name" or "Security" 文字
            // 但很多檔是合併儲存格，實際上抓不到，故預設 0 最穩
            // 若你之後遇到不是 0，也可以再強化這邊

            return new HeaderLocate(r, codeNameCol, revenueCol);
        }

        return null;
    }

    private static String normalize(String s) {
        return s.trim().toUpperCase(Locale.ROOT);
    }

    // =========================
    // common helpers
    // =========================

    private static String collectRowText(Row row) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < row.getLastCellNum(); c++) {
            String s = ExcelCellUtils.getCellString(row.getCell(c));
            if (s != null) sb.append(s.trim());
            sb.append("|");
        }
        return sb.toString();
    }

    private static Map<String, Integer> buildColumnMap(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            String title = ExcelCellUtils.getCellString(headerRow.getCell(c));
            if (title == null) continue;
            title = title.trim();
            if (!title.isBlank()) map.put(title, c);
        }
        return map;
    }

    private static Integer findCol(Map<String, Integer> colMap, String key) {
        if (colMap.containsKey(key)) return colMap.get(key);
        for (Map.Entry<String, Integer> e : colMap.entrySet()) {
            if (e.getKey().contains(key)) return e.getValue();
        }
        return null;
    }

    private static boolean containsAny(String s, String... needles) {
        for (String n : needles) {
            if (s.contains(n)) return true;
        }
        return false;
    }

    private static class ParseResult {
        final List<MonthlyRevenueVo> list;
        ParseResult(List<MonthlyRevenueVo> list) { this.list = list; }
    }

    private static class HeaderLocate {
        final int headerRowIdx;
        final int codeNameColIdx;
        final int revenueColIdx;

        HeaderLocate(int headerRowIdx, int codeNameColIdx, int revenueColIdx) {
            this.headerRowIdx = headerRowIdx;
            this.codeNameColIdx = codeNameColIdx;
            this.revenueColIdx = revenueColIdx;
        }
    }
}
