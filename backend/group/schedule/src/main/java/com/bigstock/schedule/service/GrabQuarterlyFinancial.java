package com.bigstock.schedule.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.client.TpexQuarterlyFinancialClient;
import com.bigstock.sharedComponent.client.TwseQuarterlyFinancialClient;
import com.bigstock.sharedComponent.dto.QuarterlyFinancialVo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GrabQuarterlyFinancial {

    private final TpexQuarterlyFinancialClient tpexClient;
    private final TwseQuarterlyFinancialClient twseClient;
	
	@Scheduled(cron = "0 40 19 * * ?", zone = "Asia/Taipei")
	public void syncToDb() {
//        List<QuarterlyFinancialVo> twselist =twseClient.fetch(year, quarter);
//        List<QuarterlyFinancialVo> tpexlist =twseClient.fetch(year, quarter);
        
	}
	
	public static YearQuarter resolveLatestReportQuarter() {

        LocalDate now = LocalDate.now();

        int year = now.getYear();
        int month = now.getMonthValue();

        int currentQuarter =
                (month - 1) / 3 + 1;

        int targetQuarter;
        int targetYear = year;

        if (currentQuarter == 1) {

            // previous quarter is last year's Q4
            targetQuarter = 4;
            targetYear = year - 1;

        } else {

            targetQuarter = currentQuarter - 1;
        }

        return new YearQuarter(targetYear, targetQuarter);
    }

    public static class YearQuarter {

        private final int year;
        private final int quarter;

        public YearQuarter(int year, int quarter) {
            this.year = year;
            this.quarter = quarter;
        }

        public int year() {
            return year;
        }

        public int quarter() {
            return quarter;
        }
    }
}
