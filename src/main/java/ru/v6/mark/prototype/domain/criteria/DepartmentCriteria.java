package ru.v6.mark.prototype.domain.criteria;

import com.fasterxml.jackson.annotation.JsonInclude;
import ru.v6.mark.prototype.domain.constant.DepartmentType;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DepartmentCriteria extends OrderInfo {

    private Boolean deleted = Boolean.FALSE;
    private DepartmentType departmentType;
    private List<DepartmentType> departmentTypes;

    private String gln;

    public DepartmentType getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(DepartmentType departmentType) {
        this.departmentType = departmentType;
    }

    public List<DepartmentType> getDepartmentTypes() {
        return departmentTypes;
    }

    public void setDepartmentTypes(List<DepartmentType> departmentTypes) {
        this.departmentTypes = departmentTypes;
    }

    public String getGln() {
        return gln;
    }

    public void setGln(String gln) {
        this.gln = gln;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
