package com.bigstock.schedule.job.core;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.bigstock.schedule.job.error.ThrowingRunnable;
import com.bigstock.schedule.job.repo.JobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Executes a FULL, existing business batch in the same DataSource transaction
 * as the checkpoint update. Use ONLY when underlying batch writer joins
 * REQUIRED transaction. If an inner REQUIRES_NEW or separate datasource exists,
 * do not use this API.
 */

@Service
public class JobTransactionalCheckpoint {

	private final TransactionTemplate tx;

	private final JobRepository repo;

	private final ObjectMapper mapper;

	public JobTransactionalCheckpoint(TransactionTemplate tx, JobRepository repo, ObjectMapper mapper) {

		this.tx = tx;
		this.repo = repo;
		this.mapper = mapper;
	}

	public void execute(JobContext ctx, Map<String, Object> checkpoint, ThrowingRunnable businessWriter)
			throws Exception {

		// 1. Transaction 外先檢查取消狀態
		ctx.checkCancellation();

		// 2. 先序列化，避免進入交易後才發現 JSON 問題
		final String checkpointJson = mapper.writeValueAsString(checkpoint);

		try {

			// 3. 業務資料與 Checkpoint 使用同一筆交易
			tx.executeWithoutResult(status -> {

				ctx.checkCancellation();

				try {

					// 3.1 執行原本的 PostgreSQL Bulk Upsert
					businessWriter.run();

				} catch (Exception e) {

					// TransactionTemplate 的 callback
					// 不允許直接拋出 Checked Exception
					throw new BusinessWriteException(e);
				}

				// 3.2 再次確認任務是否已取消
				ctx.checkCancellation();

				// 3.3 使用 owner + epoch 驗證執行權
				boolean saved = repo.saveCheckpoint(ctx.executionId(), ctx.owner(), ctx.epoch(), checkpointJson);

				if (!saved) {
					throw new JobCancelledException();
				}

			});

		} catch (BusinessWriteException e) {

			// 將原始 Checked Exception 還原給呼叫端
			throw (Exception) e.getCause();

		}

		// 4. Transaction 正常返回後，才更新 Redis Progress
		ctx.checkpoint(checkpoint);
	}

	private static class BusinessWriteException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public BusinessWriteException(Exception cause) {
			super(cause);
		}
	}
}
