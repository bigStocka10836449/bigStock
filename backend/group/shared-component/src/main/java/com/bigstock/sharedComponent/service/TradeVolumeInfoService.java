package com.bigstock.sharedComponent.service;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.TradeVolumeInfo;
import com.bigstock.sharedComponent.entity.TradeVolumeInfo.TradeVolumeInfoId;
import com.bigstock.sharedComponent.repository.TradeVolumeInfoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradeVolumeInfoService {

    private final TradeVolumeInfoRepository repository;
    
    private final JdbcTemplate jdbcTemplate;

    public List<TradeVolumeInfo> findAll() {
        return repository.findAll();
    }

    public Optional<TradeVolumeInfo> findById(TradeVolumeInfoId id) {
        return repository.findById(id);
    }

    public TradeVolumeInfo save(TradeVolumeInfo tradeVolumeInfo) {
        return repository.save(tradeVolumeInfo);
    }

    public void deleteById(TradeVolumeInfoId id) {
        repository.deleteById(id);
    }
    
    public List<TradeVolumeInfo> saveAll(List<TradeVolumeInfo> tradeVolumeInfos) {

        String sql = """
            INSERT INTO bstock.tmp_trade_volume_info
            (stock_code, trading_day, trade_volume)
            VALUES (?, ?, ?)
            ON CONFLICT (stock_code, trading_day)
            DO UPDATE SET
                trade_volume = EXCLUDED.trade_volume
            """;

        jdbcTemplate.batchUpdate(sql, tradeVolumeInfos, tradeVolumeInfos.size(),
            (ps, tradeVolumeInfo) -> {
                ps.setString(1, tradeVolumeInfo.getStockCode());
                ps.setDate(2, new java.sql.Date(tradeVolumeInfo.getTradingDay().getTime()));
                ps.setString(3, tradeVolumeInfo.getTradeVolume());
            }
        );

        return tradeVolumeInfos;
    }
}
