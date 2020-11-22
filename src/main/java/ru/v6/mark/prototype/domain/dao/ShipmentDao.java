package ru.v6.mark.prototype.domain.dao;

import org.springframework.stereotype.Repository;
import ru.v6.mark.prototype.domain.criteria.ShipmentCriteria;
import ru.v6.mark.prototype.domain.entity.Shipment;

import java.util.HashMap;
import java.util.Map;

@Repository
public class ShipmentDao extends BaseCriteriaDao<Shipment, ShipmentCriteria> {

    @Override
    public String getCriteriaCondition(ShipmentCriteria criteria) {
        String result = "";

        if (criteria.getDeleted() != null) {
            result += " and shipment.deleted = :deleted";
        }

        if (criteria.getStatus() != null) {
            result += " and shipment.status = :status";
        }

        if (criteria.getResult() != null) {
            result += " and shipment.result = :result";
        }

        if (criteria.getDepartment() != null) {
            result += " and shipment.consignee.code = :department";
        } else if (criteria.getAllDepartments() != null) {
            result += " and shipment.consignee.code in :allDepartments";
        }

        if (criteria.getOrder() != null) {
            result += " and cast(shipment.order as integer) = :order";
        }

        if (criteria.getContainer() != null) {
            result += " and shipment.container = :container";
        }

        if (criteria.getNumber() != null) {
            result += " and shipment.number = :number";
        }

        if (criteria.getDate() != null) {
            result += " and shipment.date = :date";
        }

        if (criteria.getGlnConsignee() != null) {
            result += " and shipment.glnConsignee = :glnConsignee";
        }

        if (criteria.getVendorInn() != null) {
            result += " and shipment.vendorInn = :vendorInn";
        }

        if (criteria.getType() != null) {
            result += " and shipment.type = :type";
        }

        return !result.isEmpty() ? result.replaceFirst(" and ", " where ") : "";
    }

    @Override
    public Map<String, Object> getCriteriaParams(ShipmentCriteria criteria) {
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
