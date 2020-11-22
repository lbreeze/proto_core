package ru.v6.mark.prototype.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Status {
    OK(0), ERROR_COMMON(1), CONFIRMATION_REQUIRED(2), VALIDATION_ERROR(3);

    private int code;

    Status(int code) {
        this.code = code;
    }

    @JsonCreator
    public static Status forValue(Integer code) {
        Status result = null;
        if (code != null) {
            int idx = 0;
            while (result == null && idx < Status.values().length) {
                if (Status.values()[idx++].code == code) {
                    result = Status.values()[idx - 1];
                }
            }
        }
        return result;
    }

    @JsonValue
    public int toValue() {
        return code;
    }
}
