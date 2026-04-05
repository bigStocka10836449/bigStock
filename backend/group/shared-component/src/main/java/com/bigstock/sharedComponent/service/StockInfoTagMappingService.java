package com.bigstock.sharedComponent.service;

import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.StockInfoTagMapping;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockInfoTagMappingService {

    private final DataSource dataSource;

    public void bulkUpsert(List<StockInfoTagMapping> list) {

        if (list == null || list.isEmpty()) return;

        Map<String, String> dedupMap = new LinkedHashMap<>();

        for (StockInfoTagMapping dto : list) {
            if (StringUtils.isBlank(dto.getTag())) continue;

            String tag = dto.getTag().trim().toUpperCase();
            if (tag.isEmpty()) continue;

            String tagName = dto.getTagName();

            dedupMap.put(tag, tagName); // overwrite duplicates safely
        }

        try (Connection conn = dataSource.getConnection()) {

            conn.setAutoCommit(false);

            // ✅ 2. Create temp table
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TEMP TABLE tmp_tag_mapping (
                        tag text,
                        tag_name text
                    ) ON COMMIT DROP
                """);
            }

            CopyManager copyManager = new CopyManager((BaseConnection) conn.unwrap(BaseConnection.class));

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : dedupMap.entrySet()) {
                sb.append(entry.getKey()).append("\t")
                  .append(entry.getValue() == null ? "\\N" : entry.getValue())
                  .append("\n");
            }

            try (Reader reader = new StringReader(sb.toString())) {
                copyManager.copyIn(
                        "COPY tmp_tag_mapping(tag, tag_name) FROM STDIN WITH (FORMAT text)",
                        reader
                );
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    INSERT INTO bstock.stock_info_tag_mapping (tag, tag_name)
                    SELECT tag, tag_name
                    FROM tmp_tag_mapping
                    ON CONFLICT (tag) DO NOTHING
                """);
            }

            conn.commit();

        } catch (Exception e) {
            throw new RuntimeException("bulkUpsert failed", e);
        }
    }
}
