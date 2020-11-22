package ru.v6.mark.prototype.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import ru.v6.mark.prototype.domain.constant.AcceptancePositionStatus;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "shipment_position")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShipmentPosition extends DeletableEntity {

    @Id
    @GeneratedValue(generator = "SHIPMENT_POSITION_SEQ", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "SHIPMENT_POSITION_SEQ", sequenceName = "shipment_position_id_seq", initialValue = 1, allocationSize = 1)
    private Long id;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @NotFound(action = NotFoundAction.IGNORE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article", insertable = false, updatable = false)
    private Article articleItem;

    @Column(name = "article")
    private Integer article;

    @Column(name = "qty_ordered")
    private Integer quantityOrdered;
    @Column(name = "qty_shipped")
    private Integer quantityShipped;

    private Boolean confirmed;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "shipment")
    private Shipment shipment;

    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @OneToMany(mappedBy = "shipmentPosition")//, cascade = {CascadeType.ALL}, orphanRemoval = true)
    private List<KIZAggregation> aggregations;

    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @OneToMany(mappedBy = "shipmentPosition")//, cascade = {CascadeType.ALL}, orphanRemoval = true)
    private List<KIZMark> marks;

    @Enumerated(EnumType.STRING)
    private AcceptancePositionStatus status;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Article getArticleItem() {
        return articleItem;
    }

    public void setArticleItem(Article articleItem) {
        this.articleItem = articleItem;
    }

    public Integer getArticle() {
        return article;
    }

    public void setArticle(Integer article) {
        this.article = article;
    }

    public Integer getQuantityOrdered() {
        return quantityOrdered;
    }

    public void setQuantityOrdered(Integer quantityOrdered) {
        this.quantityOrdered = quantityOrdered;
    }

    public Integer getQuantityShipped() {
        return quantityShipped;
    }

    public void setQuantityShipped(Integer quantityShipped) {
        this.quantityShipped = quantityShipped;
    }

    public Boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    public List<KIZAggregation> getAggregations() {
        return aggregations;
    }

    public void setAggregations(List<KIZAggregation> aggregations) {
        this.aggregations = aggregations;
    }

    public List<KIZMark> getMarks() {
        return marks;
    }

    public void setMarks(List<KIZMark> marks) {
        this.marks = marks;
    }

    public AcceptancePositionStatus getStatus() {
        return status;
    }

    public void setStatus(AcceptancePositionStatus status) {
        this.status = status;
    }
}
