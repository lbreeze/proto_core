package ru.v6.mark.prototype.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Michael on 21.12.2019.
 */
public enum KIZMarkStatus implements EnumDesc {
    RECEIVED("Получена"),
    SCANNED("Отсканирована"),
    VALIDATED("Проверена"),
    ERROR("Ошибка"),
    CIRCULATED("В обороте"),
    PRINTED("Напечатана");

    private String description;

    KIZMarkStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static KIZMarkStatus forValue(String value) {
        return value == null ? null : KIZMarkStatus.valueOf(value);
    }

    //@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static KIZMarkStatus forMap(@JsonProperty("name") String name, @JsonProperty("description") String description) {
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
