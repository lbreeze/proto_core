package ru.v6.mark.prototype.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import ru.v6.mark.prototype.domain.constant.MarkSubType;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "gtin")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gtin extends BaseEntity {

    @Id
    private String gtin;

    @Enumerated(EnumType.STRING)
    @Column(name = "mark_sub_type")
    private MarkSubType markSubType;

    private Boolean imported;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JoinColumn(name = "organization")
    @ManyToOne(fetch = FetchType.LAZY)
    private Organization organization;

    @Override
    public Serializable getId() {
        return gtin;
    }

    public String getGtin() {
        return gtin;
    }

    public void setGtin(String gtin) {
        this.gtin = gtin;
    }


    public MarkSubType getMarkSubType() {
        return markSubType;
    }

    public void setMarkSubType(MarkSubType markSubType) {
        this.markSubType = markSubType;
    }

    public Boolean getImported() {
        return imported;
    }

    public void setImported(Boolean imported) {
        this.imported = imported;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Gtin goods = (Gtin) o;
        return Objects.equals(gtin, goods.gtin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gtin);
    }
}
