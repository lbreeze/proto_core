package ru.v6.mark.prototype.domain.criteria;

import com.fasterxml.jackson.annotation.JsonInclude;
import ru.v6.mark.prototype.domain.constant.AcceptanceResult;
import ru.v6.mark.prototype.domain.constant.AcceptanceStatus;
import ru.v6.mark.prototype.domain.constant.AcceptanceType;

import java.util.Date;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShipmentCriteria extends PagingCriteria implements LocalCriteria {
    private String department = null;
    private List<String> allDepartments = null;

    private AcceptanceStatus status = null;
    private AcceptanceResult result = null;
    private AcceptanceType type = null;
    private Integer order = null;
    private String container = null;
    private Boolean deleted = Boolean.FALSE;

    private String glnConsignee = null;
    private String vendorInn = null;
    private String number = null;
    private Date date = null;

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    @Override
    public List<String> getAllDepartments() {
        return allDepartments;
    }

    public void setAllDepartments(List<String> allDepartments) {
        this.allDepartments = allDepartments;
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

    public AcceptanceType getType() {
        return type;
    }

    public void setType(AcceptanceType type) {
        this.type = type;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getGlnConsignee() {
        return glnConsignee;
    }

    public void setGlnConsignee(String glnConsignee) {
        this.glnConsignee = glnConsignee;
    }

    public String getVendorInn() {
        return vendorInn;
    }

    public void setVendorInn(String vendorInn) {
        this.vendorInn = vendorInn;
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

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
