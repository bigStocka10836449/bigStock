package com.bigstock.schedule.infra;

import com.bigstock.schedule.annotation.DateRange;
import com.bigstock.sharedComponent.dto.DateRangeDto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateRangeValidator implements ConstraintValidator<DateRange, DateRangeDto> {

	@Override
	public boolean isValid(DateRangeDto dateRangeDto, ConstraintValidatorContext context) {
		if (dateRangeDto.getStartDate() == null || dateRangeDto.getEndDate() == null) {
			return true; // 让其他的空值验证器处理空值
		}
		return !dateRangeDto.getStartDate().after(dateRangeDto.getEndDate());
	}
}
