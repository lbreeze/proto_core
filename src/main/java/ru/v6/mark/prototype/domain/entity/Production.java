package ru.v6.mark.prototype.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "production")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Production extends DeletableEntity {

    @Id
    @GeneratedValue(generator = "PRODUCTION_SEQ", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "PRODUCTION_SEQ", sequenceName = "production_id_seq", initialValue = 1, allocationSize = 1)
    private Long id;

    @NotFound(action = NotFoundAction.IGNORE)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ean", insertable = false, updatable = false)
    private Goods goods;

    @NotFound(action = NotFoundAction.IGNORE)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ean", insertable = false, updatable = false)
    private Gtin gtin;

    @Column(name = "ean")
    private String ean;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Europe/Moscow")
    @Column(name = "produced_date")
    private Date producedDate;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Europe/Moscow")
    @Column(name = "valid_date")
    private Date validDate;

    @Column(name = "certificate")
    private String certificate;

    @Column(name = "qty_produced")
    private Integer qtyProduced;
    @Column(name = "qty_actual")
    private Integer qtyActual;
    @Column(name = "qty_applied")
    private Integer qtyApplied;

    @Override
    public Serializable getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }

    public Goods getGoods() {
        return goods;
    }

    public void setGoods(Goods goods) {
        this.goods = goods;
    }

    public Gtin getGtin() {
        return gtin;
    }

    public void setGtin(Gtin gtin) {
        this.gtin = gtin;
    }

    public Date getProducedDate() {
        return producedDate;
    }

    public void setProducedDate(Date producedDate) {
        this.producedDate = producedDate;
    }

    public Date getValidDate() {
        return validDate;
    }

    public void setValidDate(Date validDate) {
        this.validDate = validDate;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public Integer getQtyProduced() {
        return qtyProduced;
    }

    public void setQtyProduced(Integer qtyProduced) {
        this.qtyProduced = qtyProduced;
    }

    public Integer getQtyActual() {
        return qtyActual;
    }

    public void setQtyActual(Integer qtyActual) {
        this.qtyActual = qtyActual;
    }

    public Integer getQtyApplied() {
        return qtyApplied;
    }

    public void setQtyApplied(Integer qtyApplied) {
        this.qtyApplied = qtyApplied;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Production production = (Production) o;
        return Objects.equals(id, production.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }
}
