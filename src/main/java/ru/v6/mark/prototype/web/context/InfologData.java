package ru.v6.mark.prototype.web.context;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InfologData {
    private List<InfologDataItem> data = null;

    public List<InfologDataItem> getData() {
        return data;
    }

    public void setData(List<InfologDataItem> data) {
        this.data = data;
    }
}
