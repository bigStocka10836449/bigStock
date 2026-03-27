package com.bigstock.schedule.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class InfraTaskService {

    private final ExecutorService infraExecutor;
    private final ScheduledExecutorService timeoutExecutor;
    private final Map<String, AtomicBoolean> runningFlags = new ConcurrentHashMap<>();

    public InfraTaskService(ExecutorService infraExecutor) {
        this.infraExecutor = infraExecutor;
        this.timeoutExecutor = Executors.newScheduledThreadPool(1);
    }

    public void submitCleanupWithTimeout(
            String taskName,
            Runnable task,
            long timeout,
            TimeUnit unit
    ) {
        AtomicBoolean flag = runningFlags.computeIfAbsent(taskName, k -> new AtomicBoolean(false));

        if (!flag.compareAndSet(false, true)) {
            log.warn("Skip cleanup task because previous one is still running. taskName={}", taskName);
            return;
        }

        Future<?> future;
        try {
            future = infraExecutor.submit(() -> {
                try {
                    task.run();
                    log.info("Cleanup task completed successfully. taskName={}", taskName);
                } catch(Exception e) {
                	log.warn(e.getMessage(), e);
                }
                finally {
                    flag.set(false);
                }
            });
        } catch (RejectedExecutionException e) {
            flag.set(false);
            log.error("Cleanup task rejected by executor. taskName={}", taskName, e);
            return;
        }

        timeoutExecutor.schedule(() -> {
            if (!future.isDone()) {
                log.error("Cleanup task timeout. taskName={}, timeout={} {}", taskName, timeout, unit);
                future.cancel(true);
            }
        }, timeout, unit);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down infraExecutor and timeoutExecutor...");
        infraExecutor.shutdown();
        timeoutExecutor.shutdown();

        try {
            if (!infraExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("infraExecutor did not terminate in time, forcing shutdownNow");
                infraExecutor.shutdownNow();
            }
            if (!timeoutExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("timeoutExecutor did not terminate in time, forcing shutdownNow");
                timeoutExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            infraExecutor.shutdownNow();
            timeoutExecutor.shutdownNow();
        }
    }
}