package ru.v6.mark.prototype.web.context;

import java.lang.reflect.Field;

public abstract class LoggableParam {

    @Override
    public String toString() {
        StringBuilder fields = new StringBuilder();
        for (Field field : getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object value = field.get(this);
                if (value != null) {
                    if (fields.length() > 0)
                        fields.append(", ");
                    fields.append(field.getName()).append('=').append(value);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return getClass().getSimpleName() + "{" + fields + '}';
    }
}
