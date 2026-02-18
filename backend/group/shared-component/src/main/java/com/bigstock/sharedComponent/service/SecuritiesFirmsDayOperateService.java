package com.bigstock.sharedComponent.service;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.SecuritiesFirmsDayOperate;
import com.bigstock.sharedComponent.repository.SecuritiesFirmsDayOperateRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecuritiesFirmsDayOperateService {
	private final SecuritiesFirmsDayOperateRepository securitiesFirmsDayOperateRepository;
	
	 private final JdbcTemplate jdbcTemplate;
	
	public SecuritiesFirmsDayOperate insert(SecuritiesFirmsDayOperate securitiesFirmsDayOperate) {
		return securitiesFirmsDayOperateRepository.save(securitiesFirmsDayOperate);
	}
	
	public List<SecuritiesFirmsDayOperate> insertAll(List<SecuritiesFirmsDayOperate> securitiesFirmsDayOperates) {
		return securitiesFirmsDayOperateRepository.saveAll(securitiesFirmsDayOperates);
	}
	
	@Transactional
	public void batchReplace(
	        String stockCode,
	        Date tradingDate,
	        List<SecuritiesFirmsDayOperate> list) {

	    // 1️⃣ Delete existing records
	    String deleteSql = """
	        DELETE FROM bstock.securities_firms_day_operate
	        WHERE stock_code = ?
	          AND trading_date = ?
	        """;

	    jdbcTemplate.update(deleteSql, stockCode, tradingDate);


	    // 2️⃣ Batch Insert
	    String insertSql = """
	        INSERT INTO bstock.securities_firms_day_operate
	        (stock_code, trading_date, seq, price,
	         stock_buy_amount, stock_sell_amount, securities_firms)
	        VALUES (?, ?, ?, ?, ?, ?, ?)
	        """;

	    jdbcTemplate.batchUpdate(insertSql,
	        list,
	        list.size(),
	        (ps, item) -> {
	            ps.setString(1, item.getStockCode());
	            ps.setDate(2, new java.sql.Date(item.getTradingDate().getTime()));
	            ps.setLong(3, item.getSeq());
	            ps.setString(4, item.getPrice());
	            ps.setLong(5, ObjectUtils.isEmpty(item.getStockBuyAmount()) ? 0: item.getStockBuyAmount());
	            ps.setLong(6, ObjectUtils.isEmpty(item.getStockSellAmount()) ? 0: item.getStockSellAmount());
	            ps.setString(7, item.getSecuritiesFirms());
	        }
	    );
	}
	
	public void deleteById(SecuritiesFirmsDayOperate.SecuritiesFirmsDayOperateId id) {
		securitiesFirmsDayOperateRepository.deleteById(id);
	}
	
	public void deleteByIds(List<SecuritiesFirmsDayOperate.SecuritiesFirmsDayOperateId> ids) {
		securitiesFirmsDayOperateRepository.deleteAllByIdInBatch(ids);
	}
	
	public List<SecuritiesFirmsDayOperate> getByStockCode(String stockCode){
		return securitiesFirmsDayOperateRepository.findByStockCode(stockCode);
	}
	
	public List<SecuritiesFirmsDayOperate> getByStockCodeAndTradingDate(String stockCode, Date tradingDate){
		return securitiesFirmsDayOperateRepository.findByStockCodeAndTradingDate(stockCode, tradingDate);
	}
}
