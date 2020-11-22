package ru.v6.mark.prototype.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum JobType implements EnumDesc {
    IMPORT_GOODS("Импорт товаров"),
    RETRIEVE_KIZ("Получение марок"),
    MARK_TO_TURN("Ввод марок в оборот"),
    GET_DOC_STATUS("Получение статуса документов"),
    ACCEPTANCE("Загрузка УПД"),
    AGGREGATION("Агрегация кодов"),
    ORDER("Доотправка заказов в СУЗ");

    private String description;

    JobType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static JobType forValue(String value) {
        return value == null ? null : JobType.valueOf(value);
    }

    //@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static JobType forMap(@JsonProperty("name") String name, @JsonProperty("description") String description) {
        return forValue(name);
    }

    @JsonValue
    public Map<String, String> toValue() {
        return new HashMap<String, String>() {{
            put("name", name());
            put("description", description);
        }};
    }

    @Override
   public String toString() {
        return description;
    }
}
