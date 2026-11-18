
package com.bigstock.schedule.job.handlers;

import org.springframework.stereotype.Component;

import com.bigstock.schedule.job.core.JobContext;
import com.bigstock.schedule.job.core.ManagedJob;
import com.bigstock.schedule.service.GrabFromThirdParty;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MarginTradingJob implements ManagedJob {

    private final GrabFromThirdParty grabFromThirdParty;

    @Override
    public String code() {
        return "MARGIN_TRADING_SYNC";
    }

    @Override
    public String displayName() {
        return "融資融券資料同步";
    }

    @Override
    public void execute(JobContext context) throws Exception {

        grabFromThirdParty.calculateMarginTradingManaged(
                context
        );
    }
}
