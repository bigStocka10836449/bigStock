package com.bigstock.biz.client;

import com.bigstock.biz.model.CompanyInfo;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.*;

@Component
public class TwseCompanyPdfClient {

    public CompanyInfo fetch(String stockId) {
        String sid = stockId == null ? "" : stockId.trim();
        if (sid.isBlank()) return null;

        String url = "https://www.twse.com.tw/pdf/ch/" + sid + "_ch.pdf";

        try {
            byte[] pdf = SimpleHttpUtils.getBytes(url);
            if (pdf == null || pdf.length == 0) return null;

            String text = extractText(pdf);
            if (text == null || text.isBlank()) return null;

            String industry = findValueByLabel(text, "產業類別");
            String listingDate = findDateByLabel(text, "上市日期");
            String mainBiz = extractMainBusinessBlock(text);

            CompanyInfo out = new CompanyInfo();
            out.setStockId(sid);
            out.setMarket("TWSE");
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
            return new PDFTextStripper().getText(doc);
        }
    }

    private static String findValueByLabel(String text, String label) {
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String s = line == null ? "" : line.trim();
            if (s.contains(label)) {
                String after = s.substring(s.indexOf(label) + label.length()).trim();
                if (!after.isBlank()) return after;
            }
        }
        return null;
    }

    private static String findDateByLabel(String text, String label) {
        String v = findValueByLabel(text, label);
        if (v == null) return null;
        v = v.replace('/', '-').trim();
        if (v.contains(" ")) v = v.substring(0, v.indexOf(' ')).trim();
        return v;
    }

    /**
     * 「主要經營業務」通常是標題行，內容散在它上方數行（你之前也確認是文字 PDF）
     * 這裡做 best-effort：向上收集，遇到其他欄位標題就停。
     */
    private static String extractMainBusinessBlock(String text) {
        List<String> lines = Arrays.asList(text.split("\\r?\\n"));
        int idx = -1;
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i) == null ? "" : lines.get(i).trim();
            if (s.contains("主要經營業務")) { idx = i; break; }
        }
        if (idx < 0) return null;

        Set<String> stopKeys = Set.of("公司名稱", "上市日期", "產業類別", "公司網址", "實收資本額", "總機", "發言人");

        List<String> buf = new ArrayList<>();
        for (int i = idx - 1; i >= 0; i--) {
            String s = lines.get(i) == null ? "" : lines.get(i).trim();
            if (s.isBlank()) continue;

            boolean stop = false;
            for (String k : stopKeys) {
                if (s.contains(k)) { stop = true; break; }
            }
            if (stop) break;

            buf.add(s);
            if (buf.size() >= 10) break;
        }
        Collections.reverse(buf);
        String joined = String.join("", buf).trim();
        return joined.isBlank() ? null : joined;
    }

    private static String clean(String s) {
        if (s == null) return null;
        s = s.replace("\u0000", "").trim();
        return s.isBlank() ? null : s;
    }
}
