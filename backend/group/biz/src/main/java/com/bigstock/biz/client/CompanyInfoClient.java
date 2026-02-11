package com.bigstock.biz.client;

import com.bigstock.biz.model.CompanyInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompanyInfoClient {

    private final TwseCompanyPdfClient twseCompanyPdfClient;
    private final TpexOtcDetailClient tpexOtcDetailClient;

    public CompanyInfo fetchBestEffort(String stockId, String market) {
        String m = market == null ? "" : market.trim().toUpperCase();

        if ("TWSE".equals(m)) return twseCompanyPdfClient.fetch(stockId);
        if ("TPEX".equals(m)) return tpexOtcDetailClient.fetch(stockId);

        CompanyInfo twse = twseCompanyPdfClient.fetch(stockId);
        if (twse != null && ((twse.getMainBusiness() != null && !twse.getMainBusiness().isBlank())
                || (twse.getIndustryCategory() != null && !twse.getIndustryCategory().isBlank()))) {
            return twse;
        }
        return tpexOtcDetailClient.fetch(stockId);
    }
}
