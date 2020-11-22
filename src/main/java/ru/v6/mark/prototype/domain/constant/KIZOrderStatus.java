package ru.v6.mark.prototype.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum KIZOrderStatus implements EnumDesc {
    CREATED("Создан"),
    SENT("Отправлен"),
    ERROR("Ошибка"),
    RECEIVED("Получен ответ"),
    COMPLETED("Выполнен");

    private String description;

    KIZOrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static KIZOrderStatus forValue(String value) {
        return value == null ? null : KIZOrderStatus.valueOf(value);
    }

    //@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static KIZOrderStatus forMap(@JsonProperty("name") String name, @JsonProperty("description") String description) {
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
