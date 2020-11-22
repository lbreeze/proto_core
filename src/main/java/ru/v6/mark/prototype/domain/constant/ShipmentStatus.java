package ru.v6.mark.prototype.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Michael on 21.12.2019.
 */
public enum ShipmentStatus implements EnumDesc {
    PROHIBITED("Запрет отгрузки"),
    PENDING("Ожидание отгрузки"),
    // PENDING_WARN("Ожидание приемки: есть предупреждения"),
    // PENDING_ERROR("Ожидание приемки: есть ошибки"),
    COMPLETED("Отгрузка выполнена");

    private String description;

    ShipmentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ShipmentStatus forValue(String value) {
        return value == null ? null : ShipmentStatus.valueOf(value);
    }

    //@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static ShipmentStatus forMap(@JsonProperty("name") String name, @JsonProperty("description") String description) {
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
