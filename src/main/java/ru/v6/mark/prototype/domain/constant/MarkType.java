package ru.v6.mark.prototype.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MarkType implements EnumDesc {
//    TYPE1(1, "TYPE1"),
//    TYPE2(2, "TYPE2"),
//    TYPE3(3, "TYPE3"),
//    TYPE4(4, "TYPE4"),
    TYPE5(5, "Ветеринария", true),
    TYPE6(6, "Без признака", false),
    TYPE7(7, "Сигареты", true),
    TYPE8(8, "Обувь", true),
    TYPE9(9, "Легпром/текстиль", false),
    TYPE10(10, "Шины", false),
    TYPE11(11, "Фото", false),
    TYPE12(12, "Парфюмерия", false),
    TYPE13(13, "Табачная продукция", false)
    ;

    private Integer code;
    private String description;
    private boolean marking;

    MarkType(Integer code, String description, boolean marking) {
        this.code = code;
        this.description = description;
        this.marking = marking;
    }

    public String getDescription() {
        return description;
    }

    public Integer getCode() {
        return code;
    }

    public boolean isMarking() {
        return marking;
    }

    @JsonCreator
    public static MarkType forValue(String value) {
        return value == null ? null : MarkType.valueOf(value);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
