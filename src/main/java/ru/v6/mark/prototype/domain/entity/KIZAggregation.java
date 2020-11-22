package ru.v6.mark.prototype.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.hibernate.validator.constraints.Length;
import ru.v6.mark.prototype.domain.constant.KIZAggregationStatus;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "kiz_aggregation")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KIZAggregation extends BaseEntity {

    @Id
    @Length(min = 18, max = 18)
    private String sscc;

    @Column(name = "box_num")
    private Long boxNum;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)//, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "acceptance_position")
    private AcceptancePosition acceptancePosition;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)//, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "shipment_position")
    private ShipmentPosition shipmentPosition;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kiz_order")
    private KIZOrder kizOrder;

    @Enumerated(EnumType.STRING)
    private KIZAggregationStatus status;

    @Column(name = "doc_id")
    private String docId;

    @Column(name = "status_desc")
    private String statusDesc;

    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @OneToMany(mappedBy = "aggregation")
    private List<KIZMark> marks;

    @Override
    public String getId() {
        return sscc;
    }

    public Long getBoxNum() {
        return boxNum;
    }

    public void setBoxNum(Long boxNum) {
        this.boxNum = boxNum;
    }

    public String getSscc() {
        return sscc;
    }

    public void setSscc(String sscc) {
        this.sscc = sscc;
    }

    public AcceptancePosition getAcceptancePosition() {
        return acceptancePosition;
    }

    public void setAcceptancePosition(AcceptancePosition acceptancePosition) {
        this.acceptancePosition = acceptancePosition;
    }

    public ShipmentPosition getShipmentPosition() {
        return shipmentPosition;
    }

    public void setShipmentPosition(ShipmentPosition shipmentPosition) {
        this.shipmentPosition = shipmentPosition;
    }

    public KIZOrder getKizOrder() {
        return kizOrder;
    }

    public void setKizOrder(KIZOrder kizOrder) {
        this.kizOrder = kizOrder;
    }

    public KIZAggregationStatus getStatus() {
        return status;
    }

    public void setStatus(KIZAggregationStatus status) {
        this.status = status;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getStatusDesc() {
        return statusDesc;
    }

    public void setStatusDesc(String statusDesc) {
        this.statusDesc = statusDesc;
    }

    public List<KIZMark> getMarks() {
        return marks;
    }

    public void setMarks(List<KIZMark> marks) {
        this.marks = marks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        KIZAggregation that = (KIZAggregation) o;
        return Objects.equals(sscc, that.sscc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sscc);
    }
}
