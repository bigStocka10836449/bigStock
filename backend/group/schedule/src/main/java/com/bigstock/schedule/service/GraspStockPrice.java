package com.bigstock.schedule.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.bigstock.schedule.utils.ChromeDriverUtils;
import com.bigstock.sharedComponent.entity.SecuritiesFirmsDayOperate;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.service.SecuritiesFirmsDayOperateService;
import com.bigstock.sharedComponent.service.StockDayPriceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.Lists;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import jakarta.annotation.PostConstruct;
import javazoom.jl.decoder.JavaLayerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GraspStockPrice {
	@Value("${schedule.chromeDriverPath.windows.active}")
	private boolean windowsActive;

	@Value("${schedule.chromeDriverPath.windows.path}")
	private String windowsChromeDriverPath;

	@Value("${schedule.chromeDriverPath.linux.active}")
	private boolean linuxActive;

	@Value("${schedule.chromeDriverPath.linux.driver-path}")
	private String linuxChromeDriverPath;

//	@Value("${schedule.chromeDriverPath.linux.chrome-path}")
//	private String linuxChromePath;

	@Value("${schedule.stock-price.url.tpex}")
	private String stockPriceTPEXUrl;

	@Value("${schedule.stock-price.url.twse}")
	private String stockPriceTWSEUrl;

	@Value("${schedule.chromeDriverPath.download-path}")
	private String downloadPath;

	@Value("${schedule.bpython-url}")
	private String bpythonUrl;

	@Value("${schedule.credentials-pathl}")
	private String credentialsPath;

	private final StockDayPriceService stockDayPriceService;

	private final SecuritiesFirmsDayOperateService securitiesFirmsDayOperateService;

	private final StockInfoService stockInfoService;

	// 每周日早上8点触发更新
	@Scheduled(cron = "${schedule.task.scheduling.cron.expression.grasp-stock-price}")
	public void updateShareholderStructure() throws RestClientException, URISyntaxException, JsonMappingException,
			JsonProcessingException, InterruptedException {
		// 先抓DB裡面全部的代號資料
		List<StockDayPrice> stockTpexDayPrices = ChromeDriverUtils
				.graspTpexDayPrice("https://www.tpex.org.tw/openapi/v1/tpex_mainboard_quotes");

		Date tradeDate = stockTpexDayPrices.stream().findFirst().get().getTradingDay();
		List<StockDayPrice> stockTwseDayPrices = ChromeDriverUtils
				.graspTwseDayPrice("https://openapi.twse.com.tw/v1/exchangeReport/STOCK_DAY_ALL", tradeDate);
		stockDayPriceService.saveAll(stockTpexDayPrices);
		stockDayPriceService.saveAll(stockTwseDayPrices);
		log.info("finsh sync stockDayPrice");
	}

    @PostConstruct
	@Scheduled(cron = "${schedule.task.scheduling.cron.expression.grasp-securitiesfirms-dayoperate}")
	public void grepSecuritiesFirmsDayOperate() throws InterruptedException, RestClientException, URISyntaxException,
			UnsupportedAudioFileException, IOException, LineUnavailableException, JavaLayerException {
//			ChromeDriverUtils.grepCanvas(windowsChromeDriverPath);
    	File downloadPathFolder = new File(downloadPath);
    	downloadPathFolder.delete();
    	downloadPathFolder.mkdirs();
		List<String> TPEXStockCodes = stockInfoService.findByStockType("0").stream()
				.map(stockInfo -> stockInfo.getStockCode()).toList();
		List<String> TESEtockCodes = stockInfoService.findByStockType("1").stream()
				.map(stockInfo -> stockInfo.getStockCode()).toList();
		ChromeDriverUtils.grepTPEXsecuritiesFirmsDayOperate(downloadPath,
				windowsActive ? windowsChromeDriverPath : linuxChromeDriverPath, TPEXStockCodes, credentialsPath);
		ChromeDriverUtils.grepTWSESsecuritiesFirmsDayOperate(downloadPath,
				windowsActive ? windowsChromeDriverPath : linuxChromeDriverPath, TESEtockCodes, bpythonUrl);
		List<StockDayPrice> stockTpexDayPrices = ChromeDriverUtils
				.graspTpexDayPrice("https://www.tpex.org.tw/openapi/v1/tpex_mainboard_quotes");
		Date tradeDate = stockTpexDayPrices.stream().findFirst().get().getTradingDay();
		List.of(downloadPathFolder.listFiles()).stream().forEach(downloadFile -> {
			// 讀取CSV文件
			CSVReader reader = null;
			List<JSONObject> jsonArray = Lists.newArrayList();
			List<String[]> csvData = Lists.newArrayList();
			try {
				reader = new CSVReader(new InputStreamReader(new FileInputStream(downloadFile), "Big5"));
				csvData = reader.readAll();
				reader.close();
			} catch (IOException | CsvException e) {
				log.info(e.getMessage(),e);
				return;
			}

			jsonArray = convertTESECSVtoJSON(csvData);
			List<SecuritiesFirmsDayOperate> securitiesFirmsDayOperates = jsonArray.stream().map(jsb -> {
				SecuritiesFirmsDayOperate securitiesFirmsDayOperate = new SecuritiesFirmsDayOperate();
				String stockCode = removeFileExtension(downloadFile.getName());
				securitiesFirmsDayOperate.setPrice(jsb.getString("價格"));
				securitiesFirmsDayOperate.setSeq(jsb.getInt("序號"));
				securitiesFirmsDayOperate.setStockCode(stockCode.contains("_") ? stockCode.split("_")[0] : stockCode);
				securitiesFirmsDayOperate.setSecuritiesFirms(jsb.getString("券商"));
				securitiesFirmsDayOperate
						.setStockBuyAmount(Long.valueOf(jsb.getString("買進股數").trim().replace(",", "")));
				securitiesFirmsDayOperate.setTradingDay(tradeDate);
				return securitiesFirmsDayOperate;
			}).sorted((x1, x2) -> x1.getSeq().compareTo(x2.getSeq())).toList();
			securitiesFirmsDayOperateService.insertAll(securitiesFirmsDayOperates);
		});
	}

	private List<JSONObject> convertTESECSVtoJSON(List<String[]> csvData) {
		// 跳過前 3 行表頭數據
		// 創建 JSON 數組
		List<JSONObject> jsonArray = Lists.newArrayList();
		// 跳過前 3 行表頭數據
		for (int i = 3; i < csvData.size(); i++) {
			String[] row = csvData.get(i);

			// 檢查第一部分（列 1 到列 5 是否有數據）
			if (row[0] != null && !row[0].trim().isEmpty()) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("序號", Integer.parseInt(row[0].trim()));
				jsonObject.put("券商", row[1].trim());
				jsonObject.put("價格", row[2].trim());
				jsonObject.put("買進股數", row[3].trim());
				jsonObject.put("賣出股數", row[4].trim());

				jsonArray.add(jsonObject); // 將第一部分的 JSON 對象添加到列表中
			}

			// 檢查第二部分（列 7 到列 11 是否有數據）
			if (row.length > 6 && row[6] != null && !row[6].trim().isEmpty()) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("序號", Integer.parseInt(row[6].trim())); // 從第 7 列開始
				jsonObject.put("券商", row[7].trim());
				jsonObject.put("價格", row[8].trim());
				jsonObject.put("買進股數", row[9].trim());
				jsonObject.put("賣出股數", row[10].trim());

				jsonArray.add(jsonObject); // 將第二部分的 JSON 對象添加到列表中
			}
		}
		return jsonArray;
	}

	public static String removeFileExtension(String fileName) {
		// 找到最後一個點的位置
		int lastDotIndex = fileName.lastIndexOf('.');

		// 如果找到了點，截取之前的部分；如果沒有點，則返回原始文件名
		if (lastDotIndex != -1) {
			return fileName.substring(0, lastDotIndex);
		} else {
			return fileName; // 沒有擴展名
		}
	}

}
