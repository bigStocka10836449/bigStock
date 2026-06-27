package com.bigstock.schedule.service;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class SecuritiesFirmsRankReadySchedule {

    private final SecuritiesFirmsRankPrecomputeService precomputeService;

    /**
     * 每天晚上 21:30 觸發券商買賣超排名預先計算。
     *
     * 實際計算基準日不是系統當日，而是 securities_firms_day_operate 中最近可用交易日。
     * 例如週末或假日執行時，會沿用最近一個有買賣日報表資料的交易日。
     */
    @Scheduled(cron = "0 30 21 * * ?", zone = "Asia/Taipei")
    public void precomputeSecuritiesFirmsRank() {
        log.info("Securities firms rank scheduled trigger fired. triggerSource=SCHEDULE_2130");
        try {
            precomputeService.precomputeAsync("SCHEDULE_2130");
        } catch (Exception e) {
            log.warn("Securities firms rank scheduled trigger skipped because async executor is busy.", e);
        }
    }
}
