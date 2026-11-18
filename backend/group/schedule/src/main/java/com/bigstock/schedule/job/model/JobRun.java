package com.bigstock.schedule.job.model;
import java.time.OffsetDateTime;
import java.util.UUID;
public record JobRun(
 UUID executionId, String jobCode, String status, String triggerType, UUID retryOf,
 double progress, long totalItems, long processedItems, long successItems,
 long failedItems, long skippedItems, String currentStage, String message,
 OffsetDateTime createdAt, OffsetDateTime startedAt, OffsetDateTime finishedAt,
 OffsetDateTime heartbeatAt) {}
