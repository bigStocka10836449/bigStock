package com.bigstock.sharedComponent.service;

import java.io.IOException;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.Session;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigstock.sharedComponent.entity.FinancialCalendar;
import com.bigstock.sharedComponent.redis.CacheOperatorService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinancialCalendarService {

	@PersistenceContext
	private EntityManager em;

	private static final int CHUNK_SIZE = 5000;

	private final CacheOperatorService cacheOperatorService;
	
	
	
	@Transactional
	public void importFinancialCalendar(int year, int month, List<FinancialCalendar> list, String platForm) throws Exception {
		List<FinancialCalendar> finallist =  list.stream().filter(financialCalendar -> ObjectUtils.isNotEmpty(financialCalendar.getDataType())).map(financialCalendar ->{
			financialCalendar.setSorucePlatfont(platForm);
			financialCalendar.setId(platForm + financialCalendar.getArticleId());
			return financialCalendar;
		}).toList();
		cacheOperatorService.putSnapshotDataListAtomic("ultraLongLivedCache",
				"financialCalendar:" + year+ ":" + String.format("%02d", month) + ":" + platForm,
				finallist);
		LocalDate ld = LocalDate.now();
		
		ld = ld.minusMonths(24);
		cacheOperatorService.delete("ultraLongLivedCache",
				"financialCalendar:" + ld.getYear() + ":" + String.format("%02d", ld.getMonth().getValue()) + ":"+ platForm);
		cacheOperatorService.cleanupOldSnapshots("ultraLongLivedCache", "financialCalendar:" + ld.getYear() + ":" + String.format("%02d", ld.getMonth().getValue()) + ":" + platForm, 2);
		refreshData(finallist);
	}
	
	@Transactional
	public void refreshData(List<FinancialCalendar> list) throws Exception {

		if (list == null || list.isEmpty())
			return;

		Session session = em.unwrap(Session.class);

		session.doWork(connection -> {

			// 1️⃣ Create temp table
			try (Statement stmt = connection.createStatement()) {
				stmt.execute("""
						    CREATE TEMP TABLE tmp_financial_calendar (
						        "month" text,
						        "year" text,
						        event_date date,
						        title text,
						        data_type text,
						        data_type_name text,
						        article_id text,
						        hyper_link text,
						        soruce_platfont text,
						        id text
						    ) ON COMMIT DROP
						""");
			}

			CopyManager copyManager = new CopyManager(connection.unwrap(BaseConnection.class));

			// 2️⃣ COPY ALL chunks into temp table
			for (int i = 0; i < list.size(); i += CHUNK_SIZE) {
				int end = Math.min(i + CHUNK_SIZE, list.size());
				List<FinancialCalendar> chunk = list.subList(i, end);

				StringBuilder sb = new StringBuilder(chunk.size() * 128);

				for (FinancialCalendar item : chunk) {
					sb.append(safe(item.getMonth())).append('\t').append(safe(item.getYear())).append('\t')
							.append(item.getEventDate() != null ? item.getEventDate() : "").append('\t')
							.append(safe(item.getTitle())).append('\t').append(safe(item.getDataType())).append('\t')
							.append(safe(item.getDataTypeName())).append('\t').append(safe(item.getArticleId()))
							.append('\t').append(safe(item.getHyperLink())).append('\t')
							.append(safe(item.getSorucePlatfont())).append('\t').append(safe(item.getId()))
							.append('\n');
				}

				try {
					copyManager.copyIn("""
							    COPY tmp_financial_calendar
							    ("month","year",event_date,title,data_type,data_type_name,
							     article_id,hyper_link,soruce_platfont,id)
							    FROM STDIN WITH (FORMAT text)
							""", new StringReader(sb.toString()));
				} catch (IOException e) {
					throw new RuntimeException("COPY failed", e);
				}
			}

			// 3️ DELETE (ONLY ONCE)
			try (PreparedStatement ps = connection.prepareStatement("""
					    DELETE FROM bstock.financial_calendar t
					    USING tmp_financial_calendar tmp
					    WHERE t."year" = tmp."year"
					      AND t."month" = tmp."month"
					      AND t.soruce_platfont = tmp.soruce_platfont
					""")) {
				ps.executeUpdate();
			}

			// 4️ INSERT (ONLY ONCE)
			try (PreparedStatement ps = connection.prepareStatement("""
					    INSERT INTO bstock.financial_calendar
					    ("month","year",event_date,title,data_type,data_type_name,
					     article_id,hyper_link,soruce_platfont,id)
					    SELECT "month","year",event_date,title,data_type,data_type_name,
					           article_id,hyper_link,soruce_platfont,id
					    FROM tmp_financial_calendar
					    ON CONFLICT (id) DO NOTHING
					""")) {
				ps.executeUpdate();
			}

		});
	}

	private String safe(Object obj) {
		if (obj == null)
			return "";
		return obj.toString().replace("\t", " ").replace("\n", " ");
	}
}