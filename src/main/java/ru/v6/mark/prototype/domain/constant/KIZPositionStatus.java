package ru.v6.mark.prototype.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum KIZPositionStatus implements EnumDesc {
    RECEIVED("Получены марки"),
    PRINTED("Марки напечатаны"),
    // CIRCULATED("Марки в обороте"),
    // IN_PROGRESS("Отправлена на ввод в оборот"),
    CHECKED_OK("Введены в оборот"),
    CHECKED_NOT_OK("Ошибка ввода в оборот"),
    PROCESSING_ERROR("Ошибка обработки документа ввода в оборот"),
    UNDEFINED("Отправлена на ввод в оборот"),
    ERROR("Ошибка получения статуса"),
    ORDER_OK("Создан заказ СУЗ"),
    ERROR_CUZ("Ошибка при создании заказа СУЗ"),
    WAIT_SENDING("Ожидает отправки");

    private String description;

    KIZPositionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static KIZPositionStatus forValue(String value) {
        return value == null ? null : KIZPositionStatus.valueOf(value);
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static KIZPositionStatus forMap(@JsonProperty("name") String name, @JsonProperty("description") String description) {
        return forValue(name);
    }

    @JsonValue
    public Map<String, String> toValue() {
        return new HashMap<String, String>() {{
            put("name", name());
            put("description", description);
        }};
    }
}
