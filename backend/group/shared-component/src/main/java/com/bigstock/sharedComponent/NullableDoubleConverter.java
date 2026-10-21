package com.bigstock.sharedComponent;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class NullableDoubleConverter
        implements AttributeConverter<Double, String> {

    @Override
    public String convertToDatabaseColumn(Double value) {
        return value == null ? null : value.toString();
    }

    @Override
    public Double convertToEntityAttribute(String value) {

        if (value == null || value.trim().isEmpty()
                || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }

        return Double.valueOf(value.trim());
    }
}