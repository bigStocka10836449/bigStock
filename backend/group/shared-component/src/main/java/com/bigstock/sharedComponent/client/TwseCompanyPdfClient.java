package com.bigstock.sharedComponent.client;

import java.io.ByteArrayInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import com.bigstock.sharedComponent.dto.CompanyInfo;

@Component
public class TwseCompanyPdfClient {

    // ✅ 抓日期：1994/09/05 或 1994-09-05 或 1994/9/5
    private static final Pattern YMD_PATTERN = Pattern.compile("(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})");

    public CompanyInfo fetch(String stockId) {
        String sid = stockId == null ? "" : stockId.trim();
        if (sid.isBlank()) return null;

        String url = "https://www.twse.com.tw/pdf/ch/" + sid + "_ch.pdf";

        try {
            byte[] pdf = SimpleHttpUtils.getBytes(url);
            if (pdf == null || pdf.length == 0) return null;

            String text = extractText(pdf);
            if (text == null || text.isBlank()) return null;

            String[] lines = text.split("\\R");
            for (int i = 0; i < Math.min(lines.length, 120); i++) {
                System.out.printf("%03d | %s%n", i, lines[i]);
            }
            
            // ✅ 這些 label 在 TWSE PDF 都是「公司基本資料」區塊的欄位
            String companyName = findValueSmart(text, "公司名稱");
            String industry = findValueSmart(text, "產業類別");
            String listingDate = findDateSmart(text, "上市日期");
            String mainBiz = findValueBlockSmart(text, "主要經營業務");

            CompanyInfo out = new CompanyInfo();
            out.setStockId(sid);
            out.setMarket("TWSE");
            out.setStockName(clean(companyName));
            out.setIndustryCategory(clean(industry));
            out.setListingDate(clean(listingDate));
            out.setMainBusiness(clean(mainBiz));
            return out;

        } catch (Exception e) {
            return null;
        }
    }

    private static String extractText(byte[] pdfBytes) throws Exception {
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
        	PDFTextStripper stripper = new PDFTextStripper();
        	stripper.setSortByPosition(true);
        	return stripper.getText(doc);

        }
    }

    /**
     * 更穩的 label 取值：
     * 1) 同行 label 後面有值就取
     * 2) 否則取下一行（但只取像值的那行）
     */
    private static String findValueSmart(String text, String label) {
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String s = norm(lines[i]);
            if (s.isBlank()) continue;
            int pos = s.indexOf(label);
            if (pos < 0) continue;

            String after = s.substring(pos + label.length()).trim();
            after = cutBeforeNextLabel(after,
                    "公司名稱","上市日期","產業類別","公司網址","實收資本額","總機","發言人","主要經營業務");
            if (!after.isBlank()) return after;

            // fallback: 下一行像「值」的才取（避免抓到別的 label）
            for (int j = i + 1; j < lines.length; j++) {
                String next = norm(lines[j]);
                if (next.isBlank()) continue;

                // 若下一行本身又包含很多欄位 label 關鍵字，就不要當值
                if (containsAny(next, "公司名稱", "上市日期", "產業類別", "公司網址", "實收資本額", "總機", "發言人", "主要經營業務")) {
                    break;
                }
                return next;
            }
            return null;
        }
        return null;
    }
    
    private static String cutBeforeNextLabel(String s, String... labels) {
        if (s == null) return null;
        int cut = s.length();
        for (String lb : labels) {
            int p = s.indexOf(lb);
            if (p >= 0 && p < cut) cut = p;
        }
        String out = s.substring(0, cut).trim();
        return out;
    }

    private static String findDateSmart(String text, String label) {
        String v = findValueSmart(text, label);
        if (v == null) return null;

        // ✅ 從字串中抽出日期片段，避免 "1994/09/05上市日期" 這種污染
        Matcher m = YMD_PATTERN.matcher(v);
        if (m.find()) {
            int y = Integer.parseInt(m.group(1));
            int mo = Integer.parseInt(m.group(2));
            int d = Integer.parseInt(m.group(3));
            return String.format("%04d-%02d-%02d", y, mo, d);
        }
        return null;
    }

    /**
     * 主要經營業務常常會很長，可能跨多行。
     * 做法：找到 label 後，收集同一行 label 後面的值 + 後續幾行，直到遇到下一個欄位 label。
     */
    private static String findValueBlockSmart(String text, String label) {
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String s = norm(lines[i]);
            if (s.isBlank()) continue;

            int pos = s.indexOf(label);
            if (pos < 0) continue;

            StringBuilder sb = new StringBuilder();
            String after = s.substring(pos + label.length()).trim();
            if (!after.isBlank()) sb.append(after);

            // 往下抓最多 8 行（可調），直到遇到其他欄位 label
            for (int j = i + 1; j < lines.length && j <= i + 8; j++) {
                String next = norm(lines[j]);
                if (next.isBlank()) continue;

                if (containsAny(next, "公司名稱", "上市日期", "產業類別", "公司網址", "實收資本額", "總機", "發言人")) {
                    break;
                }
                sb.append(next);
            }

            String out = sb.toString().trim();
            return out.isBlank() ? null : out;
        }
        return null;
    }

    private static boolean containsAny(String s, String... keys) {
        for (String k : keys) {
            if (s.contains(k)) return true;
        }
        return false;
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s.replace("\u0000", "").trim();
    }

    private static String clean(String s) {
        if (s == null) return null;
        s = s.replace("\u0000", "").trim();
        return s.isBlank() ? null : s;
    }
}
