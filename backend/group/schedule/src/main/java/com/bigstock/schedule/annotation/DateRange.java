package com.bigstock.schedule.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.bigstock.schedule.infra.DateRangeValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
public @interface DateRange {
    String message() default "Start date must be before or equal to end date.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}