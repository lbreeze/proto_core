package ru.v6.mark.prototype.web.context;

import com.fasterxml.jackson.annotation.JsonInclude;
import ru.v6.mark.prototype.domain.entity.Protocol;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProtocolResult {
    private List<Protocol> data;
    private String header;

    public List<Protocol> getData() {
        return data;
    }

    public void setData(List<Protocol> data) {
        this.data = data;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }
}
