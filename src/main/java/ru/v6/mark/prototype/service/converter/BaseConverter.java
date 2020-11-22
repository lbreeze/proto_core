package ru.v6.mark.prototype.service.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.v6.mark.prototype.domain.entity.BaseEntity;

import java.io.IOException;

public class BaseConverter<T extends BaseEntity> {

    public T convertToInternal(String externalJson, Class<T> clazz) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(externalJson, clazz);
    }

    public String inverseConvert(T entity) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(entity);
    }

}