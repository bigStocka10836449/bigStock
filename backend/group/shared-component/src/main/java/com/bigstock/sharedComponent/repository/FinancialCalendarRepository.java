package com.bigstock.sharedComponent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigstock.sharedComponent.entity.FinancialCalendar;

public interface FinancialCalendarRepository extends JpaRepository<FinancialCalendar, String> {

	@Query("""
			 SELECT e
			 FROM FinancialCalendar e
			 WHERE e.sorucePlatfont = :sorucePlatfont
			 and  e.year = :year
			 and e.month = :month
			 ORDER BY e.eventDate ASC
			""")
	List<FinancialCalendar> findAllBySorucePlatfontAndYearAndMonth(@Param("sorucePlatfont") String sorucePlatfont,
			@Param("year") Integer year, @Param("month") Integer month);
}
