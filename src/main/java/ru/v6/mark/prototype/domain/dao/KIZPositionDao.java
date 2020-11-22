package ru.v6.mark.prototype.domain.dao;

import org.springframework.stereotype.Repository;

import ru.v6.mark.prototype.domain.criteria.KIZPositionCriteria;
import ru.v6.mark.prototype.domain.entity.KIZPosition;

import java.util.HashMap;
import java.util.Map;

@Repository
public class KIZPositionDao extends BaseCriteriaDao<KIZPosition, KIZPositionCriteria> {

    @Override
    public String getCriteriaCondition(KIZPositionCriteria criteria) {
        String result = "";
        if (criteria.getMarkStatus() != null) {
            result += " join kizposition.marks kizmark";
            result += " and kizmark.status = :markStatus ";
        }
        if (criteria.getStatus() != null) {
            result += " and kizposition.status in (:status)";
        }
        if (criteria.getDepartment() != null) {
            result += " and kizposition.kizOrder.departmentCode = :department";
        } else if (criteria.getAllDepartments() != null) {
            result += " and kizposition.kizOrder.departmentCode in :allDepartments";
        }
        if (criteria.getEan() != null) {
            result += " and kizposition.ean = :ean";
        }
        return !result.isEmpty() ? result.replaceFirst(" and ", " where ") : "";
    }

    @Override
    public Map<String, Object> getCriteriaParams(KIZPositionCriteria criteria) {
        Map<String, Object> result = new HashMap<>();
        if (criteria.getStatus() != null) {
            result.put("status", criteria.getStatus());
        }
        if (criteria.getDepartment() != null) {
            result.put("department", criteria.getDepartment());
        }else if (criteria.getAllDepartments() != null) {
            result.put("allDepartments", criteria.getAllDepartments());
        }
        if (criteria.getEan() != null) {
            result.put("ean", criteria.getEan());
        }
        if (criteria.getMarkStatus() != null) {
            result.put("markStatus", criteria.getMarkStatus());
        }
        return result;
    }
}
