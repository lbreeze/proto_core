package ru.v6.mark.prototype.web.context;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckMarkInfo {
    private String ean = null;
    private String mark = null;

    public CheckMarkInfo() {}

    public CheckMarkInfo(String ean) {
        this.ean = ean;
    }

    public CheckMarkInfo(String ean, String mark) {
        this.ean = ean;
        this.mark = mark;
    }

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    @Override
    public String toString() {
        return "CheckMarkInfo{" +
                "ean=" + ean +
                ", message='" + mark + '\'' +
                '}';
    }
}
