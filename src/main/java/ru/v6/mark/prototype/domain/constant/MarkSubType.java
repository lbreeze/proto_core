package ru.v6.mark.prototype.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

import static ru.v6.mark.prototype.domain.constant.MarkType.TYPE8;

public enum MarkSubType implements EnumDesc {
    FOOTWEAR_MAN(TYPE8, "Обувь Мужская"),
    FOOTWEAR_WOMAN(TYPE8, "Обувь Женская"),
    FOOTWEAR_UNISEX(TYPE8, "Обувь Унисекс"),
    FOOTWEAR_KID(TYPE8, "Обувь Детская")
    ;

    private MarkType parent;
    private String description;

    MarkSubType(MarkType parent, String description) {
        this.parent = parent;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public MarkType getParent() {
        return parent;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static MarkSubType forValue(String value) {
        return value == null ? null : MarkSubType.valueOf(value);
    }

    //@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static MarkSubType forMap(@JsonProperty("name") String name, @JsonProperty("description") String description) {
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
