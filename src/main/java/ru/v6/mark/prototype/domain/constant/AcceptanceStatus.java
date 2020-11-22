package ru.v6.mark.prototype.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Michael on 21.12.2019.
 */
public enum AcceptanceStatus implements EnumDesc {
    PROHIBITED("Запрет приемки"),
    PENDING("Ожидание приемки"),
    PENDING_WARN("Ожидание приемки: есть предупреждения"),
    PENDING_ERROR("Ожидание приемки: есть ошибки"),
    COMPLETED("Приемка завершена");

    private String description;

    AcceptanceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AcceptanceStatus forValue(String value) {
        return value == null ? null : AcceptanceStatus.valueOf(value);
    }

    //@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static AcceptanceStatus forMap(@JsonProperty("name") String name, @JsonProperty("description") String description) {
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
