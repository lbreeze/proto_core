package ru.v6.mark.prototype.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum KIZAggregationStatus implements EnumDesc {
    AVAILABLE("Доступен для выгрузки"),
    DOWNLOADED("Выгружен для заказа"),
    READY("Готовы к агрерированию"),
    SENT("Отправлен на агрегацию"),
    ERROR("Ошибка получения статуса"),
    CHECKED_NOT_OK("Ошибка агрегирования"),
    CHECKED_OK("Успешно агрегирован");

    private String description;

    KIZAggregationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static KIZAggregationStatus forValue(String value) {
        return value == null ? null : KIZAggregationStatus.valueOf(value);
    }

    //@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static KIZAggregationStatus forMap(@JsonProperty("name") String name, @JsonProperty("description") String description) {
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
