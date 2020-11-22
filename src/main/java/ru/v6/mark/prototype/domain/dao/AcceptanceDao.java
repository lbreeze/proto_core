package ru.v6.mark.prototype.domain.dao;

import org.springframework.stereotype.Repository;
import ru.v6.mark.prototype.domain.criteria.AcceptanceCriteria;
import ru.v6.mark.prototype.domain.entity.Acceptance;

import java.util.HashMap;
import java.util.Map;

@Repository
public class AcceptanceDao extends BaseCriteriaDao<Acceptance, AcceptanceCriteria> {

    @Override
    public String getCriteriaCondition(AcceptanceCriteria criteria) {
        String result = "";

        if (criteria.getDeleted() != null) {
            result += " and acceptance.deleted = :deleted";
        }

        if (criteria.getStatus() != null) {
            result += " and acceptance.status = :status";
        }

        if (criteria.getResult() != null) {
            result += " and acceptance.result = :result";
        }

        if (criteria.getDepartment() != null) {
            result += " and acceptance.consignee.code = :department";
        } else if (criteria.getAllDepartments() != null) {
            result += " and acceptance.consignee.code in :allDepartments";
        }

        if (criteria.getOrder() != null) {
            result += " and cast(acceptance.order as integer) = :order";
        }

        if (criteria.getContainer() != null) {
            result += " and acceptance.container = :container";
        }

        if (criteria.getNumber() != null) {
            result += " and acceptance.number = :number";
        }

        if (criteria.getDate() != null) {
            result += " and acceptance.date = :date";
        }

        if (criteria.getGlnConsignee() != null) {
            result += " and acceptance.glnConsignee = :glnConsignee";
        }

        if (criteria.getVendorInn() != null) {
            result += " and acceptance.vendorInn = :vendorInn";
        }

        if (criteria.getType() != null) {
            result += " and acceptance.type = :type";
        }

        return !result.isEmpty() ? result.replaceFirst(" and ", " where ") : "";
    }

    @Override
    public Map<String, Object> getCriteriaParams(AcceptanceCriteria criteria) {
        Map<String, Object> result = new HashMap<>();
        if (criteria.getDeleted() != null) {
            result.put("deleted", criteria.getDeleted());
        }

        if (criteria.getStatus() != null) {
            result.put("status", criteria.getStatus());
        }

        if (criteria.getResult() != null) {
            result.put("result", criteria.getResult());
        }

        if (criteria.getDepartment() != null) {
            result.put("department", criteria.getDepartment());
        } else if (criteria.getAllDepartments() != null) {
            result.put("allDepartments", criteria.getAllDepartments());
        }

        if (criteria.getOrder() != null) {
            result.put("order", criteria.getOrder());
        }

        if (criteria.getContainer() != null) {
            result.put("container", criteria.getContainer().trim());
        }

        if (criteria.getNumber() != null) {
            result.put("number", criteria.getNumber());
        }

        if (criteria.getDate() != null) {
            result.put("date", criteria.getDate());
        }

        if (criteria.getVendorInn() != null) {
            result.put("vendorInn", criteria.getVendorInn());
        }

        if (criteria.getGlnConsignee() != null) {
            result.put("glnConsignee", criteria.getGlnConsignee());
        }

        if (criteria.getType() != null) {
            result.put("type", criteria.getType());
        }

        return result;
    }
}
