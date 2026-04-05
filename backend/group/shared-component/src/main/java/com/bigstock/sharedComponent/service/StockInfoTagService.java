package com.bigstock.sharedComponent.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.StockInfoTag;
import com.bigstock.sharedComponent.repository.StockInfoTagRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockInfoTagService {

    private final DataSource dataSource;
    private final StockInfoTagRepository tagRepository;

    // =====================================================
    // 1. SINGLE UPSERT (atomic, no race condition)
    // =====================================================
    public void upsertTags(String stockCode, List<String> tags) {

        List<String> normalizedTags = normalize(tags);
        if (normalizedTags.isEmpty()) return;

        try (Connection conn = dataSource.getConnection()) {

            String sql = """
                INSERT INTO bstock.stock_info_tag (stock_code, tags)
                VALUES (?, ?)
                ON CONFLICT (stock_code)
                DO UPDATE SET tags = (
                    SELECT ARRAY(
                        SELECT DISTINCT UNNEST(
                            bstock.stock_info_tag.tags || EXCLUDED.tags
                        )
                    )
                )
            """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, stockCode);
                ps.setArray(2, conn.createArrayOf("text", normalizedTags.toArray()));

                ps.executeUpdate();
            }

        } catch (Exception e) {
            throw new RuntimeException("upsertTags failed", e);
        }
    }

    // =====================================================
    // 2. BULK UPSERT (for ingestion / crawler)
    // =====================================================
    public void bulkUpsert(Map<String, List<String>> stockTagMap) {

        if (stockTagMap == null || stockTagMap.isEmpty()) return;

        try (Connection conn = dataSource.getConnection()) {

            conn.setAutoCommit(false);

            String sql = """
                INSERT INTO bstock.stock_info_tag (stock_code, tags)
                VALUES (?, ?)
                ON CONFLICT (stock_code)
                DO UPDATE SET tags = (
                    SELECT ARRAY(
                        SELECT DISTINCT UNNEST(
                            bstock.stock_info_tag.tags || EXCLUDED.tags
                        )
                    )
                )
            """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {

                int batchSize = 0;

                for (Map.Entry<String, List<String>> entry : stockTagMap.entrySet()) {

                    List<String> normalized = normalize(entry.getValue());
                    if (normalized.isEmpty()) continue;

                    ps.setString(1, entry.getKey());
                    ps.setArray(2, conn.createArrayOf("text", normalized.toArray()));

                    ps.addBatch();

                    if (++batchSize % 1000 == 0) {
                        ps.executeBatch();
                    }
                }

                ps.executeBatch();
            }

            conn.commit();

        } catch (Exception e) {
            throw new RuntimeException("bulkUpsert failed", e);
        }
    }

    // =====================================================
    // 3. QUERY (use GIN index properly)
    // =====================================================
    @Transactional()
    public List<StockInfoTag> findByTag(String tag) {
        return tagRepository.findByTag(tag.toUpperCase());
    }

    @Transactional()
    public Optional<StockInfoTag> getByStockCode(String stockCode) {
        return tagRepository.findById(stockCode);
    }

    // =====================================================
    // 4. NORMALIZATION (centralized)
    // =====================================================
    private List<String> normalize(List<String> tags) {
        if (tags == null) return List.of();

        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }
}
