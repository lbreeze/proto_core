package ru.v6.mark.prototype.domain.criteria;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserCriteria extends OrderInfo implements LocalCriteria {

    private String login = null;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Europe/Moscow")
    private Date registered = null;
    private Long role = null;
    private String department = null;
    private List<String> allDepartments = null;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Date getRegistered() {
        return registered;
    }

    public void setRegistered(Date registered) {
        this.registered = registered;
    }

    public Long getRole() {
        return role;
    }

    public void setRole(Long role) {
        this.role = role;
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
}
