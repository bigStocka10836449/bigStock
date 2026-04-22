package com.bigstock.sharedComponent.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigstock.sharedComponent.entity.FinancialCalendar;

public interface FinancialCalendarRepository extends JpaRepository<FinancialCalendar, String> {
}
