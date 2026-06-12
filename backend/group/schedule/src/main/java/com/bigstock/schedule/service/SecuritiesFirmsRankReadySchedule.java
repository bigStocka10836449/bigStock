package com.bigstock.schedule.service;

// LOCAL_RANK_MANUAL_ONLY: disable automatic scheduling for local production-data rank test.
// import org.springframework.scheduling.annotation.EnableScheduling;
// import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
// LOCAL_RANK_MANUAL_ONLY: @EnableScheduling disabled. Only manual API may trigger rank precompute.
// @EnableScheduling
@RequiredArgsConstructor
public class SecuritiesFirmsRankReadySchedule {

    private final SecuritiesFirmsRankPrecomputeService precomputeService;

    /**
     * LOCAL_RANK_MANUAL_ONLY:
     * 本機連正式環境測試時，關閉 21:30 自動排程。
     * 僅允許透過 SecuritiesFirmsRankManualController 手動觸發非同步計算。
     */
    // LOCAL_RANK_MANUAL_ONLY: @Scheduled disabled. Use POST /api/securities-firms/rank/precompute/manual instead.
    // @Scheduled(cron = "0 30 21 * * ?", zone = "Asia/Taipei")
    public void precomputeSecuritiesFirmsRank() {
        log.info("Securities firms rank scheduled trigger fired.");
        try {
            precomputeService.precomputeAsync("SCHEDULE_2130");
        } catch (Exception e) {
            log.warn("Securities firms rank scheduled trigger skipped because async executor is busy.", e);
        }
    }
}
