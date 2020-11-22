package ru.v6.mark.prototype.domain.criteria;

import com.fasterxml.jackson.annotation.JsonInclude;
import ru.v6.mark.prototype.domain.constant.KIZOrderStatus;
import ru.v6.mark.prototype.domain.constant.KIZOrderType;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class KIZOrderCriteria extends PagingCriteria implements LocalCriteria {
    private String department = null;
    private List<String> allDepartments = null;

    private Integer article = null;
    private String ean = null;

    private KIZOrderType orderType = null;
    private KIZOrderStatus status = null;
    private Boolean deleted = Boolean.FALSE;

    public Integer getArticle() {
        return article;
    }

    public void setArticle(Integer article) {
        this.article = article;
    }

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }

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

    public KIZOrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(KIZOrderType orderType) {
        this.orderType = orderType;
    }

    public KIZOrderStatus getStatus() {
        return status;
    }

    public void setStatus(KIZOrderStatus status) {
        this.status = status;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
