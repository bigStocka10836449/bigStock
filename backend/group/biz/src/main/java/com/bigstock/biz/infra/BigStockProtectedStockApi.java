package com.bigstock.biz.infra;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BigStockProtectedStockApi {

    private static final Pattern SHAREHOLDER = Pattern.compile(
            "(?:^|/)stockShareholderStructure/([A-Za-z0-9]+)$"
    );

    private static final Pattern MARGIN_SHORT = Pattern.compile(
            "(?:^|/)stockCodeMarginShortInfo/([A-Za-z0-9]+)$"
    );

    private static final Pattern STOCK_PRICE = Pattern.compile(
            "(?:^|/)SingleStockPrice$"
    );

    private BigStockProtectedStockApi() {
    }

    public enum ApiType {

        SHAREHOLDER,

        MARGIN_SHORT,

        STOCK_PRICE
    }

    public record Match(
            ApiType type,
            String stockCode
    ) {
    }

    public static Match resolve(
            String method,
            String path
    ) {

        if (method == null || path == null) {
            return null;
        }

        if ("GET".equalsIgnoreCase(method)) {

            Matcher shareholder = SHAREHOLDER.matcher(path);

            if (shareholder.find()) {

                return new Match(
                        ApiType.SHAREHOLDER,
                        shareholder.group(1)
                );
            }

            Matcher margin = MARGIN_SHORT.matcher(path);

            if (margin.find()) {

                return new Match(
                        ApiType.MARGIN_SHORT,
                        margin.group(1)
                );
            }
        }

        if ("POST".equalsIgnoreCase(method)
                && STOCK_PRICE.matcher(path).find()) {

            return new Match(
                    ApiType.STOCK_PRICE,
                    null
            );
        }

        return null;
    }
}