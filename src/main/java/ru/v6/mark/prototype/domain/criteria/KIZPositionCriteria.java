package ru.v6.mark.prototype.domain.criteria;

import com.fasterxml.jackson.annotation.JsonInclude;
import ru.v6.mark.prototype.domain.constant.KIZMarkStatus;
import ru.v6.mark.prototype.domain.constant.KIZPositionStatus;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class KIZPositionCriteria extends PagingCriteria implements LocalCriteria {
    private String department = null;
    private List<String> allDepartments = null;

    private List<KIZPositionStatus> status;
    private String ean;

    private KIZMarkStatus markStatus;

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

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }

    public List<KIZPositionStatus> getStatus() {
        return status;
    }

    public void setStatus(List<KIZPositionStatus> status) {
        this.status = status;
    }

    public KIZMarkStatus getMarkStatus() {
        return markStatus;
    }

    public void setMarkStatus(KIZMarkStatus markStatus) {
        this.markStatus = markStatus;
    }
}
