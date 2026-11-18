package com.bigstock.schedule.job.error;

@FunctionalInterface
public interface ThrowingRunnable {

    void run() throws Exception;
}
