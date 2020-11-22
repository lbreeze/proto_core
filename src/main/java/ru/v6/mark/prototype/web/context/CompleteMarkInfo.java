package ru.v6.mark.prototype.web.context;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompleteMarkInfo {
    private String ean = null;
    private List<String> marks = null;

    public CompleteMarkInfo() {}

    public CompleteMarkInfo(String ean) {
        this.ean = ean;
    }

    public CompleteMarkInfo(String ean, List<String> marks) {
        this.ean = ean;
        this.marks = marks;
    }

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }

    public List<String> getMarks() {
        return marks;
    }

    public void setMarks(List<String> marks) {
        this.marks = marks;
    }

    @Override
    public String toString() {
        return "CompleteMarkInfo{" +
                "ean='" + ean + '\'' +
                ", marks=" + marks +
                '}';
    }
}
