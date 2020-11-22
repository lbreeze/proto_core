package ru.v6.mark.prototype.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;


@JsonIgnoreProperties(ignoreUnknown = true)
@MappedSuperclass
public abstract class BaseEntity implements Serializable {

    /**
     * Дата последней модификации
     */
    @Column(name = "last_update")
    @Version
    @Temporal(TemporalType.TIMESTAMP)
    protected Date lastUpdate = null;

    protected BaseEntity() {
    }

    @Transient
    public abstract Serializable getId();

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    @PrePersist
    @PreUpdate
    public void preSave() {
        if (lastUpdate == null)
            lastUpdate = new Date();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (this == o) return true;
        if (!this.getClass().equals(o.getClass())) return false;

        BaseEntity that = (BaseEntity) o;

        Serializable id = getId();
        Serializable thatId = that.getId();
        return id != null && thatId != null && id.equals(thatId);
    }

    @Override
    public int hashCode() {
        Serializable id = getId();
        return id != null ? id.hashCode() : super.hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "id=" + getId() +
                '}';
    }
}

