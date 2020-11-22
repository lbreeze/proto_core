package ru.v6.mark.prototype.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Measure implements EnumDesc {
    PC("шт"), KG("кг");

    private String description;

    Measure(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static Measure forValue(String value) {
        return value == null ? null : Measure.valueOf(value);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
