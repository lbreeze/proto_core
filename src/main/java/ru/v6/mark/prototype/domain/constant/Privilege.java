package ru.v6.mark.prototype.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum Privilege implements EnumDesc {
    ALL("Полный доступ"),
    DICTIONARIES("Системные справочники"),
    DEPARTMENT_EDIT("Редактирование подразделений"),
    GOODS_EDIT("Редактирование товаров"),
    ORDER_KIZ("Заказ КМ"),
    ORDER_KIZ_REMAINS_UPLOAD("Управление заказами КМ остатков"),
    ORDER_KIZ_IMPORT_UPLOAD("Управление заказами КМ импорта"),
    MARK_CONTROL("Контроль нанесения КМ"),
    ACCEPTANCE("Приемка"),
    ACCEPTANCE_EDIT("Управление приемками"),
    PRINT_COMMON_KIZ("Печать КМ общего заказа");

    private String description;

    Privilege(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Privilege forValue(String value) {
        return value == null ? null : Privilege.valueOf(value);
    }

    //@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static Privilege forMap(@JsonProperty("name") String name, @JsonProperty("description") String description) {
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
