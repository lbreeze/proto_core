package ru.v6.mark.prototype.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum AcceptancePositionStatus implements EnumDesc {

    INCORRECT_OWNER("Некорректный владелец продукции"),
    WRONG_CODE("Неправильный статус кодов"),
    NOT_ENOUGH_MARK("Запрет приемки - Не хватает марок"),
    NON_UNIQUE_CODE("Неуникальные коды");

    private String description;

    AcceptancePositionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AcceptancePositionStatus forValue(String value) {
        return value == null ? null : AcceptancePositionStatus.valueOf(value);
    }

    //@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static AcceptancePositionStatus forMap(@JsonProperty("name") String name, @JsonProperty("description") String description) {
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
