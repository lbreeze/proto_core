package ru.v6.mark.prototype.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import ru.v6.mark.prototype.domain.constant.AcceptanceResult;
import ru.v6.mark.prototype.domain.constant.AcceptanceStatus;
import ru.v6.mark.prototype.domain.constant.AcceptanceType;
import ru.v6.mark.prototype.domain.constant.HasProtocol;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "acceptance")
@JsonInclude(JsonInclude.Include.NON_NULL)
@HasProtocol
public class Acceptance extends DeletableEntity {

    @Id
    @GeneratedValue(generator = "ACCEPTANCE_SEQ", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "ACCEPTANCE_SEQ", sequenceName = "acceptance_id_seq", initialValue = 1, allocationSize = 1)
    private Long id;

    @Column(name = "utd_number")
    private String number;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Europe/Moscow")
    @Column(name = "utd_date")
    private Date date;

    @Column(name = "order_number")
    private String order;

    @Column(name = "container")
    private String container;

    @Column(name = "supplier")
    private String supplier;

    @Column(name = "gln_consignee")
    private String glnConsignee = null;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.EAGER)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "gln_consignee", referencedColumnName = "gln", insertable = false, updatable = false)
    private Department consignee;

    @Column(name = "inn_vendor")
    private String vendorInn;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.EAGER)
    @NotFound(action = NotFoundAction.IGNORE)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "inn_vendor", insertable = false, updatable = false)
    private Vendor vendor;

    @OrderBy("article asc")
    @OneToMany(mappedBy = "acceptance", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private List<AcceptancePosition> positions;

    @Column(name = "file_name")
    private String fileName;

    @Enumerated(EnumType.STRING)
    private AcceptanceType type;

    @Enumerated(EnumType.STRING)
    private AcceptanceStatus status;

    @Enumerated(EnumType.STRING)
    private AcceptanceResult result;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getGlnConsignee() {
        return glnConsignee;
    }

    public void setGlnConsignee(String glnConsignee) {
        this.glnConsignee = glnConsignee;
    }

    public Department getConsignee() {
        return consignee;
    }

    public void setConsignee(Department consignee) {
        this.consignee = consignee;
    }

    public String getVendorInn() {
        return vendorInn;
    }

    public void setVendorInn(String vendorInn) {
        this.vendorInn = vendorInn;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public AcceptanceType getType() {
        return type;
    }

    public void setType(AcceptanceType type) {
        this.type = type;
    }

    public AcceptanceStatus getStatus() {
        return status;
    }

    public void setStatus(AcceptanceStatus status) {
        this.status = status;
    }

    public AcceptanceResult getResult() {
        return result;
    }

    public void setResult(AcceptanceResult result) {
        this.result = result;
    }

    public List<AcceptancePosition> getPositions() {
        return positions;
    }

    public void setPositions(List<AcceptancePosition> positions) {
        this.positions = positions;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "Приемка заказа \'" + order + "\' " + (type.equals(AcceptanceType.LOCAL) ? "по УПД" : " в контейнере " + container);
    }
}
