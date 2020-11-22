package ru.v6.mark.prototype.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import ru.v6.mark.prototype.domain.constant.KIZOrderForm;
import ru.v6.mark.prototype.domain.constant.KIZOrderStatus;
import ru.v6.mark.prototype.domain.constant.KIZOrderType;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "kiz_order")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KIZOrder extends DeletableEntity {

    @Id
    @GeneratedValue(generator = "KIZ_ORDER_SEQ", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "KIZ_ORDER_SEQ", sequenceName = "kiz_order_id_seq", initialValue = 1, allocationSize = 1)
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Europe/Moscow")
    private Date created;
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department", insertable = false, updatable = false)
    private Department department;

    @Column(name = "department")
    private String departmentCode;

    @Enumerated(EnumType.STRING)
    private KIZOrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type")
    private KIZOrderType orderType = KIZOrderType.REMAINS;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_form")
    private KIZOrderForm orderForm = KIZOrderForm.EAN;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "Europe/Moscow")
    @Column(name = "sent")
    private Date sent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "Europe/Moscow")
    @Column(name = "reply")
    private Date reply;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "kizOrder", orphanRemoval = true)
    private List<KIZPosition> positions;

    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @OneToMany(cascade = {}, mappedBy = "kizOrder")
    private List<KIZAggregation> aggregations;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Europe/Moscow")
    @Column(name = "decl_date")
    private Date declarationDate;

    @Column(name = "decl_num")
    private String declarationNumber;

    @Column(name = "customs_code")
    private String customsCode;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public KIZOrderStatus getStatus() {
        return status;
    }

    public void setStatus(KIZOrderStatus status) {
        this.status = status;
    }

    public Date getSent() {
        return sent;
    }

    public void setSent(Date sent) {
        this.sent = sent;
    }

    public Date getReply() {
        return reply;
    }

    public void setReply(Date reply) {
        this.reply = reply;
    }

    public KIZOrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(KIZOrderType orderType) {
        this.orderType = orderType;
    }

    public KIZOrderForm getOrderForm() {
        return orderForm;
    }

    public void setOrderForm(KIZOrderForm orderForm) {
        this.orderForm = orderForm;
    }

    public List<KIZPosition> getPositions() {
        return positions;
    }

    public void setPositions(List<KIZPosition> positions) {
        this.positions = positions;
    }

    public List<KIZAggregation> getAggregations() {
        return aggregations;
    }

    public void setAggregations(List<KIZAggregation> aggregations) {
        this.aggregations = aggregations;
    }

    public Date getDeclarationDate() {
        return declarationDate;
    }

    public void setDeclarationDate(Date declarationDate) {
        this.declarationDate = declarationDate;
    }

    public String getDeclarationNumber() {
        return declarationNumber;
    }

    public void setDeclarationNumber(String declarationNumber) {
        this.declarationNumber = declarationNumber;
    }

    public String getCustomsCode() {
        return customsCode;
    }

    public void setCustomsCode(String customsCode) {
        this.customsCode = customsCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        KIZOrder kizOrder = (KIZOrder) o;
        return Objects.equals(id, kizOrder.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }
}
