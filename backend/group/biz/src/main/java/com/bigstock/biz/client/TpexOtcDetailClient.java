package com.bigstock.biz.client;

import com.bigstock.biz.model.CompanyInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

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

            String json = SimpleHttpUtils.postMultipartForm(url, form);
            if (json == null || json.isBlank()) return null;

            JsonNode root = objectMapper.readTree(json);

            // 容錯 key 搜尋（避免欄位名不同就掛）
            String industry = findFirstValueByKeyHints(root, List.of("產業", "industry", "產業類", "產業別"));
            String mainBiz = findFirstValueByKeyHints(root, List.of("主要經營", "營業項目", "business", "公司簡介"));
            String listingDate = findFirstValueByKeyHints(root, List.of("上櫃日期", "掛牌日期", "listingDate", "listDate"));

            CompanyInfo out = new CompanyInfo();
            out.setStockId(sid);
            out.setMarket("TPEX");
            out.setIndustryCategory(clean(industry));
            out.setMainBusiness(clean(mainBiz));
            out.setListingDate(normalizeDate(clean(listingDate)));
            return out;

        } catch (Exception e) {
            return null;
        }
    }

    private static String findFirstValueByKeyHints(JsonNode node, List<String> hints) {
        if (node == null) return null;

        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                String k = e.getKey() == null ? "" : e.getKey();
                JsonNode v = e.getValue();

                for (String h : hints) {
                    if (!h.isBlank() && k.toLowerCase().contains(h.toLowerCase())) {
                        String s = nodeToText(v);
                        if (s != null && !s.isBlank()) return s;
                    }
                }

                String deeper = findFirstValueByKeyHints(v, hints);
                if (deeper != null && !deeper.isBlank()) return deeper;
            }
        } else if (node.isArray()) {
            for (JsonNode n : node) {
                String s = findFirstValueByKeyHints(n, hints);
                if (s != null && !s.isBlank()) return s;
            }
        }
        return null;
    }

    private static String nodeToText(JsonNode v) {
        if (v == null) return null;
        if (v.isTextual() || v.isNumber()) return v.asText();
        return null;
    }

    private static String normalizeDate(String s) {
        if (s == null) return null;
        s = s.trim().replace('/', '-');
        if (s.contains(" ")) s = s.substring(0, s.indexOf(' ')).trim();
        return s.isBlank() ? null : s;
    }

    private static String clean(String s) {
        if (s == null) return null;
        s = s.replace("\u0000", "").trim();
        return s.isBlank() ? null : s;
    }
}
