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
 // 1) GENERAL (TPEX 常見)：多列表頭（公司名稱/本月不在同一列）
//     我們改成：把前 N 列的每欄 header 文字拼起來，再找欄位 index
 // =========================
 private ParseResult tryParseGeneral(Sheet sheet, String date, String market) {

	 TpexLocate tl = locateTpexColumnsByMergedHeaders(sheet, date);
     if (tl == null) return null;

     int codeNameCol = tl.codeNameCol;
     int revenueCol = tl.revenueCol;
     int dataStartRow = tl.dataStartRow;

     List<MonthlyRevenueVo> out = new ArrayList<>();

     for (int r = dataStartRow; r <= sheet.getLastRowNum(); r++) {
         Row row = sheet.getRow(r);
         if (row == null) continue;

         String codeName = ExcelCellUtils.getCellString(row.getCell(codeNameCol));
         if (codeName == null || codeName.isBlank()) continue;

         String codeNameTrim = codeName.trim();

         // 跳過產業分類列：例如 "02 食品工業"（只有 2 位代號）
         // 只吃真正股票列：4~6 位代號 + 空白 + 名稱
         Matcher m = CODE_NAME_PATTERN.matcher(codeNameTrim);
         if (!m.find()) continue;

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

     if (out.isEmpty()) return null;
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

        // TWSE 營收區通常在前幾欄：A=Code&Name, B~F=本年度/上年度營收等
        // 右側背書保證區通常在更右邊的欄（例如 I、J 或更後面）
        // 我們只在營收區範圍（col 1~5）找月份，避免誤判
        final int REVENUE_MONTH_COL_START = 1;
        final int REVENUE_MONTH_COL_END = 5;

        for (int r = 0; r <= maxScan; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            int last = row.getLastCellNum();
            if (last <= 0) continue;

            // 1) 在營收區 col 1~5 計算「月份縮寫」出現數量
            int monthHits = 0;
            int revenueCol = -1;

            for (int c = REVENUE_MONTH_COL_START; c <= REVENUE_MONTH_COL_END && c < last; c++) {
                String t = ExcelCellUtils.getCellString(row.getCell(c));
                if (t == null) continue;

                String norm = normalize(t);
                if (isMonthAbbr(norm)) {
                    monthHits++;
                }

                // 2) 在營收區 col 1~5 找「第一個 targetMonthAbbr」作為本年度本月欄
                if (revenueCol < 0 && norm.equals(targetMonthAbbr)) {
                    revenueCol = c;
                }
            }

            // 必須：這列像月份表頭（至少有兩個月份縮寫，例如 OCT./NOV.）
            if (monthHits < 2) continue;

            // 必須：在營收區命中目標月份
            if (revenueCol < 0) continue;

            // Code & Name 通常在第 0 欄（A 欄），用 0 最穩
            int codeNameCol = 0;

            return new HeaderLocate(r, codeNameCol, revenueCol);
        }

        return null;
    }

    private static boolean isMonthAbbr(String s) {
        // 允許 "NOV." / "NOV" / "Nov." 等
        String t = s.replace(".", "").toUpperCase(Locale.ROOT);
        return t.equals("JAN") || t.equals("FEB") || t.equals("MAR") || t.equals("APR")
                || t.equals("MAY") || t.equals("JUN") || t.equals("JUL") || t.equals("AUG")
                || t.equals("SEP") || t.equals("OCT") || t.equals("NOV") || t.equals("DEC");
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
    
    private static class TpexLocate {
        final int codeNameCol;
        final int revenueCol;
        final int dataStartRow;

        private TpexLocate(int codeNameCol, int revenueCol, int dataStartRow) {
            this.codeNameCol = codeNameCol;
            this.revenueCol = revenueCol;
            this.dataStartRow = dataStartRow;
        }
    }

    /**
     * 針對 TPEX xls：
     * - 表頭分散在多列（公司名稱/本月不在同一列）
     * - 需避免抓到右側「背書保證金額」的本月份
     *
     * 做法：
     * 1) 掃描前 40 列，把每一欄的 header 文字做拼接（含換行）
     * 2) codeNameCol：header 含 "CODE & NAME" 或 "公司名稱"
     * 3) revenueCol：header 同時含 "SALES AMOUNT"/"營業額" 且含 "本月" 且不含 "ENDORSED"/"背書"
     * 4) dataStartRow：找到第一列出現 "上月" 且 "本月" 的那列之後再 +1（通常是 20~26 行附近）
     */
    private TpexLocate locateTpexColumnsByMergedHeaders(Sheet sheet, String date) {
        int maxHeaderScan = Math.min(80, sheet.getLastRowNum()); // TPEX 表頭有時比 40 還深
        int maxCol = 0;

        for (int r = 0; r <= maxHeaderScan; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            maxCol = Math.max(maxCol, row.getLastCellNum());
        }
        if (maxCol <= 0) return null;

        String[] merged = new String[maxCol];
        Arrays.fill(merged, "");

        // 拼接每一欄的 header 文字（跨多列）
        for (int r = 0; r <= maxHeaderScan; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < maxCol; c++) {
                String s = ExcelCellUtils.getCellString(row.getCell(c));
                if (s == null || s.isBlank()) continue;
                merged[c] = (merged[c] + " " + s).replace("\n", " ").trim();
            }
        }

        // 依 date 推出英文月份縮寫（去掉點後的版本：NOV / OCT / SEP）
        int month = Integer.parseInt(date.substring(5, 7));
        String monthToken = MONTH_ABBR[month - 1].replace(".", ""); // e.g. "NOV"

        int codeNameCol = -1;
        List<Integer> revenueCandidates = new ArrayList<>();

        for (int c = 0; c < maxCol; c++) {
            String h = normalizeHeader(merged[c]); // 會變成大寫、去點

            // Code & Name 欄
            if (codeNameCol < 0 && (h.contains("CODE & NAME") || h.contains("公司名稱"))) {
                codeNameCol = c;
            }

            // revenue 欄候選：
            // - 必須含「本月」（或 THIS MONTH）
            // - 必須含該月英文縮寫（NOV/OCT/...）
            // - 必須排除背書保證區（ENDORSED / 背書 / 保證）
            boolean isThisMonth = h.contains("本月") || h.contains("THIS MONTH");
            boolean hasMonthToken = h.contains(monthToken);
            boolean isEndorsed = h.contains("ENDORSED") || h.contains("背書") || h.contains("保證");

            if (isThisMonth && hasMonthToken && !isEndorsed) {
                revenueCandidates.add(c);
            }
        }

        if (codeNameCol < 0 || revenueCandidates.isEmpty()) {
            // 讓你 debug 時可以快速看每欄 header 拼接結果
            // System.out.println("[TPEX] codeNameCol=" + codeNameCol + ", revenueCandidates=" + revenueCandidates);
            // for (int i = 0; i < merged.length; i++) System.out.println("[TPEX] col=" + i + " header=" + merged[i]);
            return null;
        }

        // 選最靠近 codeNameCol 右側的那個「本月」欄（通常就是 Sales Amount 本月）
        int revenueCol = -1;
        for (int c : revenueCandidates) {
            if (c > codeNameCol) {
                revenueCol = c;
                break;
            }
        }
        if (revenueCol < 0) {
            // 如果全部都在左邊，取最小的
            revenueCol = revenueCandidates.get(0);
        }

        // dataStartRow：不要猜，直接找第一筆符合 4~6 位代號的列當資料起點
        int dataStartRow = -1;
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String codeName = ExcelCellUtils.getCellString(row.getCell(codeNameCol));
            if (codeName == null) continue;
            if (CODE_NAME_PATTERN.matcher(codeName.trim()).find()) {
                dataStartRow = r;
                break;
            }
        }
        if (dataStartRow < 0) return null;

        return new TpexLocate(codeNameCol, revenueCol, dataStartRow);
    }

    private static String normalizeHeader(String s) {
        if (s == null) return "";
        return s.replace(".", "")              // 去掉月份後的點（NOV. -> NOV）
                .replace("\u00A0", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}
