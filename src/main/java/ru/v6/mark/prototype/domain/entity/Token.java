package ru.v6.mark.prototype.domain.entity;

import ru.v6.mark.prototype.web.cache.Identifiable;

import java.io.Serializable;
import java.util.Date;

public class Token implements Serializable, Identifiable {

    public Token(String id, String value, Date expiryDate) {
        this.id = id;
        this.value = value;
        this.expiryDate = expiryDate;
    }

    protected String id;

    private String value;

    private Date expiryDate;



    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

}
