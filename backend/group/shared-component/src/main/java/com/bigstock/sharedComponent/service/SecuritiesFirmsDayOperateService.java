package com.bigstock.sharedComponent.service;


import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.bigstock.sharedComponent.entity.SecuritiesFirmsDayOperate;
import com.bigstock.sharedComponent.repository.SecuritiesFirmsDayOperateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
	public void grepSecuritiesFirmsDayOperate(JSONObject json)
			throws InterruptedException, RestClientException, URISyntaxException, JsonProcessingException, JSONException, ParseException {
		List<Object> datas = json.getJSONArray("batch").toList();
		for(Object  jdata : datas) {
			ObjectMapper mapper = new ObjectMapper();
			String jsonString = mapper.writeValueAsString((HashMap) jdata);
			JSONObject dataJson = new JSONObject(jsonString);
			SimpleDateFormat sim = new SimpleDateFormat("yyyy-MM-dd");
			Date tradingDate = sim.parse(dataJson.get("tradingDate").toString());;
			// 讀取CSV文件
			List<SecuritiesFirmsDayOperate> securitiesFirmsDayOperates = dataJson.getJSONArray("data").toList().stream()
					.map(jm -> {
						SecuritiesFirmsDayOperate securitiesFirmsDayOperate = new SecuritiesFirmsDayOperate();
						try {
							String innerJsonString = mapper.writeValueAsString((HashMap) jm);
							JSONObject jsb = new JSONObject(innerJsonString);
						String stockCode = dataJson.getString("stockCode").toString();
						securitiesFirmsDayOperate.setPrice(jsb.getString("價格"));
						securitiesFirmsDayOperate.setSeq(jsb.getInt("序號"));
						securitiesFirmsDayOperate
						.setStockCode(stockCode.contains("_") ? stockCode.split("_")[0] : stockCode);
						securitiesFirmsDayOperate.setSecuritiesFirms( jsb.getString("券商"));
						
						securitiesFirmsDayOperate.setStockBuyAmount(
								Long.valueOf( jsb.getString("買進股數").trim().replace(",", "")));
						securitiesFirmsDayOperate.setStockSellAmount(Long.valueOf( jsb.getString("賣出股數").trim().replace(",", "")));
							securitiesFirmsDayOperate.setTradingDate(sim.parse(dataJson.get("tradingDate").toString()));
						} catch (JSONException | ParseException |JsonProcessingException e) {
							log.error(e.getMessage(),e);
							return null;
						} 
						return securitiesFirmsDayOperate;
					}).filter(data -> !Optional.ofNullable(data).isEmpty()).sorted((x1, x2) -> x1.getSeq().compareTo(x2.getSeq())).toList();
			if(CollectionUtils.isNotEmpty(securitiesFirmsDayOperates)) {
				batchReplace(dataJson.getString("stockCode").toString(), tradingDate,securitiesFirmsDayOperates);
			}
		}
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
