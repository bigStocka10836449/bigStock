package com.bigstock.sharedComponent;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ooxml.POIXMLDocument;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.bigstock.sharedComponent.dto.QuarterlyFinancialVo;
import com.bigstock.sharedComponent.utils.ExcelCellUtils;

@Component
public class QuarterlyFinancialExcelParser {

    // 允許：1101台泥 / 1101 台泥 / 1101　台泥（全形空白）/ 1101  台泥
    private static final Pattern CODE_NAME_RELAX =
            Pattern.compile("^\\s*(\\d{4,6})[\\s\\u3000]*([^\\s].+?)\\s*$");

    private List<QuarterlyFinancialVo> parseXls(InputStream is, int year, int quarter, String market) {
        try (HSSFWorkbook wb = new HSSFWorkbook(is)) {
            return parseSheet(wb.getSheetAt(0), year, quarter, market);
        } catch (Exception e) {
            throw new RuntimeException("parse xls failed: " + e.getMessage(), e);
        }
    }

    private List<QuarterlyFinancialVo> parseXlsx(InputStream is, int year, int quarter, String market) {
        try (XSSFWorkbook wb = new XSSFWorkbook(is)) {
            return parseSheet(wb.getSheetAt(0), year, quarter, market);
        } catch (Exception e) {
            throw new RuntimeException("parse xlsx failed: " + e.getMessage(), e);
        }
    }
    
    private List<QuarterlyFinancialVo> parseSheet(Sheet sheet, int year, int quarter, String market) {

        ColumnLocate cl = locateColumnsByMergedHeaders(sheet);
        int dataStartRow = locateDataStartRow(sheet, cl);

        List<QuarterlyFinancialVo> out = new ArrayList<>();

        for (int r = dataStartRow; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            // --- stockId / stockName ---
            String stockId = null;
            String stockName = null;

            // 優先：分欄
            if (cl.stockIdCol >= 0) {
                String id = ExcelCellUtils.getCellString(row.getCell(cl.stockIdCol));
                if (id != null) id = normalizeCellText(id);
                if (id != null && id.matches("^\\d{4,6}$")) stockId = id;
            }
            if (cl.stockNameCol >= 0) {
                String nm = ExcelCellUtils.getCellString(row.getCell(cl.stockNameCol));
                if (nm != null) nm = normalizeCellText(nm);
                if (nm != null && !nm.isBlank()) stockName = nm;
            }

            // fallback：合併欄（代號+名稱同一格）
            if (stockId == null || stockName == null) {
                String merged = ExcelCellUtils.getCellString(row.getCell(cl.fallbackMergedCol));
                if (merged != null) {
                    merged = normalizeCellText(merged);
                    Matcher m = CODE_NAME_RELAX.matcher(merged);
                    if (m.find()) {
                        if (stockId == null) stockId = m.group(1).trim();
                        if (stockName == null) stockName = m.group(2).trim();
                    }
                }
            }

            if (stockId == null || stockName == null || stockName.isBlank()) continue;

            QuarterlyFinancialVo vo = new QuarterlyFinancialVo();
            vo.setStockId(stockId);
            vo.setStockName(stockName);
            vo.setMarket(market);

            vo.setYear(year);
            vo.setQuarter(quarter);
            vo.setPeriodStartMonth(1);
            vo.setPeriodEndMonth(quarter * 3);
            vo.setUnit("TWD");

            // 財務欄位（可抓就抓；抓不到就 null）
            vo.setOperatingRevenue(readLong(row, cl.operatingRevenueCol));
            vo.setOperatingProfit(readLong(row, cl.operatingProfitCol));
            vo.setNonOperatingIncomeExpense(readLong(row, cl.nonOperatingIncomeExpenseCol));
            vo.setNetProfitAfterTax(readLong(row, cl.netProfitAfterTaxCol));

            vo.setCapitalStockEndPeriod(readLong(row, cl.capitalStockEndPeriodCol));
            vo.setEarningsPerShare(readRatio(row, cl.earningsPerShareCol));
            vo.setNetAssetValuePerShare(readRatio(row, cl.netAssetValuePerShareCol));
            vo.setQuickRatio(readRatio(row, cl.quickRatioCol));
            vo.setCurrentRatio(readRatio(row, cl.currentRatio));
            vo.setDepn(readRatio(row, cl.depn));;
            out.add(vo);
        }

        return out;
    } 

    public List<QuarterlyFinancialVo> parse(InputStream is, int year, int quarter, String market) {
        try {
            byte[] data = readAllBytes(is);

            if (isXls(data)) {
                return parseXls(new ByteArrayInputStream(data), year, quarter, market);
            }

            if (isXlsx(data)) {
                return parseXlsx(new ByteArrayInputStream(data), year, quarter, market);
            }

            // ❌ NOT EXCEL → print preview
            String preview = new String(data, 0, Math.min(200, data.length));
            throw new RuntimeException("Invalid Excel file. Response preview:\n" + preview);

        } catch (Exception e) {
            throw new RuntimeException("parse quarterly financial failed: " + e.getMessage(), e);
        }
    }
    
    private boolean isXls(byte[] data) {
        if (data.length < 8) return false;

        // OLE2 magic header
        byte[] ole2 = new byte[] {
            (byte)0xD0, (byte)0xCF, (byte)0x11, (byte)0xE0,
            (byte)0xA1, (byte)0xB1, (byte)0x1A, (byte)0xE1
        };

        for (int i = 0; i < ole2.length; i++) {
            if (data[i] != ole2[i]) return false;
        }
        return true;
    }

    private boolean isXlsx(byte[] data) {
        if (data.length < 4) return false;

        // ZIP header (xlsx is zip-based)
        return (data[0] == 0x50 && data[1] == 0x4B);
    }
    
    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int n;
        while ((n = is.read(tmp)) != -1) {
            buffer.write(tmp, 0, n);
        }
        return buffer.toByteArray();
    }
    
//    public List<QuarterlyFinancialVo> parse(InputStream is, int year, int quarter, String market) {
//        try (HSSFWorkbook wb = new HSSFWorkbook(is)) {
//            Sheet sheet = wb.getSheetAt(0);
//
//            ColumnLocate cl = locateColumnsByMergedHeaders(sheet);
//            int dataStartRow = locateDataStartRow(sheet, cl);
//
//            List<QuarterlyFinancialVo> out = new ArrayList<>();
//
//            for (int r = dataStartRow; r <= sheet.getLastRowNum(); r++) {
//                Row row = sheet.getRow(r);
//                if (row == null) continue;
//
//                // --- stockId / stockName ---
//                String stockId = null;
//                String stockName = null;
//
//                // 優先：分欄
//                if (cl.stockIdCol >= 0) {
//                    String id = ExcelCellUtils.getCellString(row.getCell(cl.stockIdCol));
//                    if (id != null) id = normalizeCellText(id);
//                    if (id != null && id.matches("^\\d{4,6}$")) stockId = id;
//                }
//                if (cl.stockNameCol >= 0) {
//                    String nm = ExcelCellUtils.getCellString(row.getCell(cl.stockNameCol));
//                    if (nm != null) nm = normalizeCellText(nm);
//                    if (nm != null && !nm.isBlank()) stockName = nm;
//                }
//
//                // fallback：合併欄（代號+名稱同一格）
//                if (stockId == null || stockName == null) {
//                    String merged = ExcelCellUtils.getCellString(row.getCell(cl.fallbackMergedCol));
//                    if (merged != null) {
//                        merged = normalizeCellText(merged);
//                        Matcher m = CODE_NAME_RELAX.matcher(merged);
//                        if (m.find()) {
//                            if (stockId == null) stockId = m.group(1).trim();
//                            if (stockName == null) stockName = m.group(2).trim();
//                        }
//                    }
//                }
//
//                if (stockId == null || stockName == null || stockName.isBlank()) continue;
//
//                QuarterlyFinancialVo vo = new QuarterlyFinancialVo();
//                vo.setStockId(stockId);
//                vo.setStockName(stockName);
//                vo.setMarket(market);
//
//                vo.setYear(year);
//                vo.setQuarter(quarter);
//                vo.setPeriodStartMonth(1);
//                vo.setPeriodEndMonth(quarter * 3);
//                vo.setUnit("TWD");
//
//                // 財務欄位（可抓就抓；抓不到就 null）
//                vo.setOperatingRevenue(readLong(row, cl.operatingRevenueCol));
//                vo.setOperatingProfit(readLong(row, cl.operatingProfitCol));
//                vo.setNonOperatingIncomeExpense(readLong(row, cl.nonOperatingIncomeExpenseCol));
//                vo.setNetProfitAfterTax(readLong(row, cl.netProfitAfterTaxCol));
//
//                vo.setCapitalStockEndPeriod(readLong(row, cl.capitalStockEndPeriodCol));
//                vo.setEarningsPerShare(readRatio(row, cl.earningsPerShareCol));
//                vo.setNetAssetValuePerShare(readRatio(row, cl.netAssetValuePerShareCol));
//                vo.setQuickRatio(readRatio(row, cl.quickRatioCol));
//                vo.setCurrentRatio(readRatio(row, cl.currentRatio));
//                vo.setDepn(readRatio(row, cl.depn));;
//                out.add(vo);
//            }
//
//            return out;
//        } catch (Exception e) {
//            throw new RuntimeException("parse quarterly financial xls failed: " + e.getMessage(), e);
//        }
//    }

    // =========================================================
    //  Column locate: keyword + fallback (merged headers + merged region aware)
    // =========================================================
    private ColumnLocate locateColumnsByMergedHeaders(Sheet sheet) {
        int maxHeaderScan = Math.min(120, sheet.getLastRowNum());
        int maxCol = findMaxCol(sheet, maxHeaderScan);
        if (maxCol <= 0) {
            ColumnLocate cl = new ColumnLocate();
            cl.headerBottomRow = 0;
            cl.fallbackMergedCol = 0;
            return cl;
        }

        // 1) 取得「每一欄」在 header 區域的合併文字（跨多列 + merged region 也讀得到）
        String[] merged = new String[maxCol];
        Arrays.fill(merged, "");

        for (int r = 0; r <= maxHeaderScan; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < maxCol; c++) {
                String s = getHeaderTextAt(sheet, r, c);
                if (s == null || s.isBlank()) continue;
                merged[c] = (merged[c] + " " + s).trim();
            }
        }

        ColumnLocate cl = new ColumnLocate();
        cl.headerBottomRow = guessHeaderBottomRow(sheet, maxHeaderScan);

        for (int c = 0; c < maxCol; c++) {
            String h = normalizeHeader(merged[c]);

            // --- stock columns ---
            if (cl.stockIdCol < 0 && containsAny(h, "公司代號", "股票代號", "代號", "CODE")) cl.stockIdCol = c;
            if (cl.stockNameCol < 0 && containsAny(h, "公司名稱", "名稱", "NAME")) cl.stockNameCol = c;
            if (cl.mergedCodeNameCol < 0 && containsAny(h, "CODE&NAME", "公司代號及名稱", "代號及名稱")) cl.mergedCodeNameCol = c;

            // --- income statement ---
            if (cl.operatingRevenueCol < 0 && containsAny(h,
                    "營業收入", "營業收益", "REVENUE", "OPERATINGREVENUE", "SALES", "NETREVENUE")) {
                cl.operatingRevenueCol = c;
            }

            if (cl.operatingProfitCol < 0 && containsAny(h,
                    "營業利益", "營業利益(損失)", "OPERATINGPROFIT", "OPERATINGINCOME")) {
                cl.operatingProfitCol = c;
            }

            if (cl.nonOperatingIncomeExpenseCol < 0 && containsAny(h,
                    "營業外收支淨額", "營業外收入及支出", "業外收支", "NON-OPERATING", "NONOPERATING")) {
                cl.nonOperatingIncomeExpenseCol = c;
            }

            if (cl.netProfitAfterTaxCol < 0 && containsAny(h,
                    "稅後純益", "稅後淨利", "稅後淨利(淨損)", "本期淨利",
                    "NETPROFIT", "PROFITAFTERTAX", "INCOMEAFTERTAX")) {
                cl.netProfitAfterTaxCol = c;
            }

            // --- capital & per share ---
            if (cl.capitalStockEndPeriodCol < 0 && containsAny(h,
                    "本期末股本", "期末股本", "股本", "CAPITALSTOCK")) {
                cl.capitalStockEndPeriodCol = c;
            }

            // EPS：上市常見「每股稅後純益」、上櫃可能換行很多
            if (cl.earningsPerShareCol < 0 && containsAny(h,
                    "每股盈餘", "EPS", "EARNINGSPERSHARE", "每股稅後純益", "NETINCOMEPERSHARE")) {
                cl.earningsPerShareCol = c;
            }

            // ✅ 每股淨值：上市會拆成「每 股 / 淨 值」，normalize 後會變成「每股淨值」
            if (cl.netAssetValuePerShareCol < 0 && containsAny(h,
                    "每股淨值", "淨值/股", "NETASSETVALUEPERSHARE", "BOOKVALUEPERSHARE", "BVPS")) {
                cl.netAssetValuePerShareCol = c;
            }

            // ✅ 速動比：上市常見「速動 / 比率 / (%)」
            if (cl.quickRatioCol < 0 && containsAny(h,
                    "速動比", "速動比率", "QUICKRATIO")) {
                cl.quickRatioCol = c;
            }
            
            if (cl.currentRatio < 0 && containsAny(h,
                    "流動比", "流動比率", "CURRENTRATIO")) {
                cl.currentRatio = c;
            }
            
            if (cl.depn < 0 && containsAny(h,
                    "淨值佔總資產", "淨值佔總資產比率", "DEPN")) {
                cl.depn = c;
            }
        }

        // fallbackMergedCol
        cl.fallbackMergedCol = (cl.mergedCodeNameCol >= 0)
                ? cl.mergedCodeNameCol
                : (cl.stockIdCol >= 0 ? cl.stockIdCol : 0);

        // ✅ 你之前加的推斷：Code/Name 分欄
        inferSplitCodeNameColumns(sheet, cl);

        return cl;
    }

    /**
     * 上市/上櫃很多檔案：表頭寫 Code&Name/公司名稱 但實際資料是「代號在該欄、名稱在下一欄」
     */
    private void inferSplitCodeNameColumns(Sheet sheet, ColumnLocate cl) {
        int probeStart = Math.min(cl.headerBottomRow + 1, sheet.getLastRowNum());

        int codeCol = (cl.mergedCodeNameCol >= 0) ? cl.mergedCodeNameCol : cl.fallbackMergedCol;
        int nameCol = codeCol + 1;
        if (nameCol < 0) return;

        int probeEnd = Math.min(probeStart + 60, sheet.getLastRowNum());
        for (int r = probeStart; r <= probeEnd; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String code = ExcelCellUtils.getCellString(row.getCell(codeCol));
            String name = ExcelCellUtils.getCellString(row.getCell(nameCol));
            if (code == null || name == null) continue;

            code = normalizeCellText(code);
            name = normalizeCellText(name);

            if (code.matches("^\\d{4,6}$") && !name.isBlank() && !name.matches("^\\d+$")) {
                cl.stockIdCol = codeCol;
                cl.stockNameCol = nameCol;
                return;
            }
        }
    }

    private int locateDataStartRow(Sheet sheet, ColumnLocate cl) {
        int start = Math.min(cl.headerBottomRow + 1, sheet.getLastRowNum());

        for (int r = start; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            if (cl.stockIdCol >= 0) {
                String id = ExcelCellUtils.getCellString(row.getCell(cl.stockIdCol));
                if (id != null && normalizeCellText(id).matches("^\\d{4,6}$")) return r;
            }

            String merged = ExcelCellUtils.getCellString(row.getCell(cl.fallbackMergedCol));
            if (merged != null) {
                String t = normalizeCellText(merged);
                if (t.matches("^\\d{4,6}.*$")) return r;
            }
        }

        throw new RuntimeException("Cannot locate data start row (quarterly financial)");
    }

    // =========================================================
    //  Header cell getter (handles merged regions + weird spaces/newlines)
    // =========================================================
    private String getHeaderTextAt(Sheet sheet, int r, int c) {
        Row row = sheet.getRow(r);
        if (row != null) {
            Cell cell = row.getCell(c);
            String s = ExcelCellUtils.getCellString(cell);
            if (s != null && !s.isBlank()) return s;
        }

        // ✅ 如果是 merged region，非左上角 cell 可能讀不到值 → 回推左上角
        CellRangeAddress merged = findMergedRegion(sheet, r, c);
        if (merged != null) {
            Row topRow = sheet.getRow(merged.getFirstRow());
            if (topRow != null) {
                Cell topLeft = topRow.getCell(merged.getFirstColumn());
                String s = ExcelCellUtils.getCellString(topLeft);
                if (s != null && !s.isBlank()) return s;
            }
        }
        return null;
    }

    private CellRangeAddress findMergedRegion(Sheet sheet, int r, int c) {
        int count = sheet.getNumMergedRegions();
        for (int i = 0; i < count; i++) {
            CellRangeAddress addr = sheet.getMergedRegion(i);
            if (addr.isInRange(r, c)) return addr;
        }
        return null;
    }

    // =========================================================
    //  Read helpers
    // =========================================================
    private static Long readLong(Row row, int col) {
        if (col < 0) return null;
        try {
            return ExcelCellUtils.getCellLong(row.getCell(col));
        } catch (Exception ignore) {
            try {
                String s = ExcelCellUtils.getCellString(row.getCell(col));
                if (s == null) return null;
                s = normalizeNumberText(s);
                if (s.isBlank()) return null;
                if (s.contains(".")) return (long) Double.parseDouble(s);
                return Long.parseLong(s);
            } catch (Exception e) {
                return null;
            }
        }
    }

    private static Double readDouble(Row row, int col) {
        if (col < 0) return null;
        try {
            String s = ExcelCellUtils.getCellString(row.getCell(col));
            if (s == null) return null;
            s = normalizeNumberText(s);
            if (s.isBlank()) return null;
            return Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
    }

    // =========================================================
    //  Utils
    // =========================================================
    private static int findMaxCol(Sheet sheet, int maxHeaderScan) {
        int maxCol = 0;
        for (int r = 0; r <= maxHeaderScan; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            maxCol = Math.max(maxCol, row.getLastCellNum());
        }
        return maxCol;
    }

    private static int guessHeaderBottomRow(Sheet sheet, int maxHeaderScan) {
        for (int r = 0; r <= maxHeaderScan; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String text = collectRowText(row);
            String norm = normalizeHeader(text);
            if (containsAny(norm, "公司代號", "股票代號", "公司名稱", "CODE", "NAME", "代號及名稱")) {
                return r;
            }
        }
        return 0;
    }
    
    private static Double readRatio(Row row, int col) {
        if (col < 0) return null;

        try {
            Cell cell = row.getCell(col);
            if (cell == null) return null;

            // 1) 如果是數字
            if (cell.getCellType() == CellType.NUMERIC) {
                double v = cell.getNumericCellValue();

                // 2) 判斷是否為百分比格式
                CellStyle style = cell.getCellStyle();
                if (style != null) {
                    String fmt = style.getDataFormatString();
                    if (fmt != null && fmt.contains("%")) {
                        v = v * 100.0;
                    }
                }

                return round2(v);
            }

            // 3) fallback：字串
            String s = ExcelCellUtils.getCellString(cell);
            if (s == null) return null;

            s = normalizeNumberText(s);
            if (s.isBlank()) return null;

            double v = Double.parseDouble(s);

            // 字串帶 % 的情況
            if (s.contains("%")) {
                v = v; // 已經是百分比
            }

            return round2(v);

        } catch (Exception e) {
            return null;
        }
    }
    
    private static Double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static String collectRowText(Row row) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < row.getLastCellNum(); c++) {
            String s = ExcelCellUtils.getCellString(row.getCell(c));
            if (s != null) sb.append(s.trim());
            sb.append("|");
        }
        return sb.toString();
    }

    private static boolean containsAny(String s, String... keys) {
        if (s == null) return false;
        for (String k : keys) {
            if (k == null) continue;
            if (s.contains(k)) return true;
        }
        return false;
    }

    /**
     * ✅ 最關鍵：把「上下兩格」「多行」「全形空白」「NBSP」都統一，並移除所有空白
     * 讓：每 股 + 淨 值 + (元) → 每股淨值(元)
     * 讓：Quick\nRatio → QUICKRATIO
     */
    private static String normalizeHeader(String s) {
        if (s == null) return "";
        String t = s.replace("\u00A0", " ")
                .replace("\u3000", " ")
                .replace("\r", " ")
                .replace("\n", " ")
                .replace(".", "")
                .replace("：", ":");
        t = t.replaceAll("\\s+", ""); // 移除所有空白
        return t.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeCellText(String s) {
        if (s == null) return null;
        return s.replace("\u00A0", " ")
                .replace("\u3000", " ")
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
    }

    private static String normalizeNumberText(String s) {
        String t = normalizeCellText(s);
        if (t == null) return "";
        t = t.replace(",", "").replace("%", "");
        if (t.startsWith("(") && t.endsWith(")")) {
            t = "-" + t.substring(1, t.length() - 1);
        }
        return t.trim();
    }

    private static class ColumnLocate {
        int headerBottomRow = 0;

        int stockIdCol = -1;
        int stockNameCol = -1;
        int mergedCodeNameCol = -1;
        int fallbackMergedCol = 0;

        int operatingRevenueCol = -1;
        int operatingProfitCol = -1;
        int nonOperatingIncomeExpenseCol = -1;
        int netProfitAfterTaxCol = -1;

        int capitalStockEndPeriodCol = -1;
        int earningsPerShareCol = -1;
        int netAssetValuePerShareCol = -1;

        int quickRatioCol = -1;
        int currentRatio = -1;
        int depn = -1;
    }
}
