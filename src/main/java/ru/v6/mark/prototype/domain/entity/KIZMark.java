package ru.v6.mark.prototype.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import ru.v6.mark.prototype.domain.constant.KIZMarkStatus;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Created by Michael on 21.12.2019.
 */
@Entity
@Table(name = "kiz_mark")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KIZMark extends BaseEntity {

    @Id
    @GeneratedValue(generator = "KIZ_MARK_SEQ", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "KIZ_MARK_SEQ", sequenceName = "kiz_mark_id_seq", initialValue = 1, allocationSize = 1)
    private Long id;

    @Column(name = "mark")
    private String mark;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private KIZMarkStatus status;

    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)//, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "acceptance_position")
    private AcceptancePosition acceptancePosition;

    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)//, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "shipment_position")
    private ShipmentPosition shipmentPosition;

    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)//, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "production")
    private Production production;

    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kiz_position")
    private KIZPosition position;

    private String sscc;

    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sscc", insertable = false, updatable = false)
    private KIZAggregation aggregation;

    @Column(name = "is_printed")
    private Boolean isPrinted = Boolean.FALSE;

    @Column(name = "who_printed")
    private String whoPrinted;

    @Column(name = "date_printed")
    private Date datePrinted;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "parent_id")
    private KIZMark parent;

    @Override
    public Serializable getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public KIZMarkStatus getStatus() {
        return status;
    }

    public void setStatus(KIZMarkStatus status) {
        this.status = status;
    }

    public AcceptancePosition getAcceptancePosition() {
        return acceptancePosition;
    }

    public void setAcceptancePosition(AcceptancePosition acceptancePosition) {
        this.acceptancePosition = acceptancePosition;
    }

    public KIZPosition getPosition() {
        return position;
    }

    public void setPosition(KIZPosition position) {
        this.position = position;
    }

    public String getSscc() {
        return sscc;
    }

    public void setSscc(String sscc) {
        this.sscc = sscc;
    }

    public KIZAggregation getAggregation() {
        return aggregation;
    }

    public void setAggregation(KIZAggregation aggregation) {
        this.aggregation = aggregation;
    }

    public Boolean getPrinted() {
        return isPrinted;
    }

    public void setPrinted(Boolean printed) {
        isPrinted = printed;
    }

    public ShipmentPosition getShipmentPosition() {
        return shipmentPosition;
    }

    public void setShipmentPosition(ShipmentPosition shipmentPosition) {
        this.shipmentPosition = shipmentPosition;
    }

    public Production getProduction() {
        return production;
    }

    public void setProduction(Production production) {
        this.production = production;
    }

    public String getWhoPrinted() {
        return whoPrinted;
    }

    public void setWhoPrinted(String whoPrinted) {
        this.whoPrinted = whoPrinted;
    }

    public Date getDatePrinted() {
        return datePrinted;
    }

    public void setDatePrinted(Date datePrinted) {
        this.datePrinted = datePrinted;
    }

    public KIZMark getParent() {
        return parent;
    }

    public void setParent(KIZMark parent) {
        this.parent = parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        KIZMark kizMark = (KIZMark) o;
        return Objects.equals(mark, kizMark.mark);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mark);
    }
}
