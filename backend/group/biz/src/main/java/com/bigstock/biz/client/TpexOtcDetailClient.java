package com.bigstock.biz.client;

import com.bigstock.biz.model.CompanyInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TpexOtcDetailClient {

    private final ObjectMapper objectMapper;

    public CompanyInfo fetch(String stockId) {
        String sid = stockId == null ? "" : stockId.trim();
        if (sid.isBlank()) return null;

        String url = "https://www.tpex.org.tw/www/zh-tw/company/otcDetail";

        try {
            Map<String, String> form = new HashMap<>();
            form.put("code", sid);
            form.put("response", "json"); // ✅ 跟 Postman 一致，避免回 HTML

            String json = SimpleHttpUtils.postMultipartForm(url, form);
            if (json == null || json.isBlank()) return null;

            JsonNode root = objectMapper.readTree(json);

            JsonNode tables = root.get("tables");
            if (tables == null || !tables.isArray()) return null;

            // 0) 從 tables[0].title 解析 stockName (格式通常 "1295 生合")
            String stockName = null;
            if (tables.size() > 0) {
                String t0 = text(tables.get(0).get("title"));
                stockName = parseStockNameFromTitle(t0, sid);
            }

            // 1) 找到「基本資料」那張 table
            JsonNode basicTable = null;
            for (JsonNode t : tables) {
                String title = text(t.get("title"));
                if (title != null && title.contains("基本資料")) {
                    basicTable = t;
                    break;
                }
            }
            if (basicTable == null) return null;

            JsonNode fields = basicTable.get("fields");
            JsonNode data = basicTable.get("data");
            if (fields == null || !fields.isArray()) return null;
            if (data == null || !data.isArray() || data.size() == 0) return null;

            JsonNode row0 = data.get(0);
            if (row0 == null || !row0.isArray()) return null;

            String listingDateRaw = getValueByField(fields, row0, "上櫃日期");
            String industry = getValueByField(fields, row0, "產品類別");
            String mainBiz = getValueByField(fields, row0, "主要經營業務");

            CompanyInfo out = new CompanyInfo();
            out.setStockId(sid);
            out.setMarket("TPEX");
            out.setStockName(clean(stockName));
            out.setIndustryCategory(clean(industry));
            out.setMainBusiness(clean(mainBiz));
            out.setListingDate(normalizeMinguoToYmd(clean(listingDateRaw)));

            return out;

        } catch (Exception e) {
            return null;
        }
    }

    private static String getValueByField(JsonNode fields, JsonNode row, String fieldName) {
        for (int i = 0; i < fields.size(); i++) {
            String f = text(fields.get(i));
            if (f == null) continue;
            if (f.trim().equals(fieldName)) {
                return (i < row.size()) ? text(row.get(i)) : null;
            }
        }
        return null;
    }

    private static String parseStockNameFromTitle(String title, String stockId) {
        if (title == null) return null;
        String t = title.trim();
        // 常見: "1295 生合"
        if (t.startsWith(stockId)) {
            String rest = t.substring(stockId.length()).trim();
            return rest.isBlank() ? null : rest;
        }
        // 其他情況：嘗試取最後一段
        int sp = t.indexOf(' ');
        if (sp >= 0 && sp < t.length() - 1) {
            String rest = t.substring(sp + 1).trim();
            return rest.isBlank() ? null : rest;
        }
        return null;
    }

    private static String text(JsonNode n) {
        if (n == null) return null;
        if (n.isTextual() || n.isNumber()) return n.asText();
        return null;
    }

    private static String clean(String s) {
        if (s == null) return null;
        s = s.replace("\u0000", "").trim();
        return s.isBlank() ? null : s;
    }

    // 114/05/27 -> 2025-05-27
    private static String normalizeMinguoToYmd(String s) {
        if (s == null) return null;
        String x = s.trim().replace('-', '/');
        String[] parts = x.split("/");
        if (parts.length < 3) return normalizeDateYmd(s);

        try {
            int y = Integer.parseInt(parts[0].trim()) + 1911;
            int m = Integer.parseInt(parts[1].trim());
            int d = Integer.parseInt(parts[2].trim());
            return String.format("%04d-%02d-%02d", y, m, d);
        } catch (Exception ignore) {
            return normalizeDateYmd(s);
        }
    }

    private static String normalizeDateYmd(String s) {
        if (s == null) return null;
        String x = s.trim().replace('/', '-');
        if (x.contains(" ")) x = x.substring(0, x.indexOf(' ')).trim();
        return x.isBlank() ? null : x;
    }
}
