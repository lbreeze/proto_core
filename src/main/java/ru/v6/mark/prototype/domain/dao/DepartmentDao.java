package ru.v6.mark.prototype.domain.dao;

import org.springframework.stereotype.Repository;
import ru.v6.mark.prototype.domain.criteria.DepartmentCriteria;
import ru.v6.mark.prototype.domain.entity.Department;

import java.util.HashMap;
import java.util.Map;

@Repository
public class DepartmentDao extends BaseCriteriaDao<Department, DepartmentCriteria> {

    @Override
    public String getCriteriaCondition(DepartmentCriteria criteria) {
        String result = "";
        if (criteria.getDeleted() != null) {
            result += " and department.deleted = :deleted";
        }
        if (criteria.getDepartmentTypes() != null) {
            result += " and department.departmentType in (:departmentTypes)";
        }
        if (criteria.getDepartmentType() != null) {
            result += " and department.departmentType = :departmentType";
        }
        if (criteria.getGln() != null) {
            result += " and department.gln = :gln";
        }
/*
        if (criteria.getRegistered() != null) {
            result += " and user.registered >= :date and user.registered < :nextdate";
        }
        if (criteria.getRole() != null) {
            result += " and user.role.id = :role";
        }
*/
        return !result.isEmpty()? result.replaceFirst(" and ", " where ") : "";
    }

    @Override
    public Map<String, Object> getCriteriaParams(DepartmentCriteria criteria) {
        Map<String, Object> result = new HashMap<>();

        if (criteria.getDeleted() != null) {
            result.put("deleted", criteria.getDeleted());
        }
        if (criteria.getDepartmentType() != null) {
            result.put("departmentType", criteria.getDepartmentType());
        }
        if (criteria.getDepartmentTypes() != null) {
            result.put("departmentTypes", criteria.getDepartmentTypes());
        }
        if (criteria.getGln() != null) {
            result.put("gln", criteria.getGln());
        }
/*
        if (criteria.getRegistered() != null) {
            result.put("date", criteria.getRegistered());
            result.put("nextdate", DateUtils.addDays(criteria.getRegistered(), 1));
        }

        if (criteria.getRole() != null) {
            result.put("role", criteria.getRole());
        }
*/
        return result;
    }
}
