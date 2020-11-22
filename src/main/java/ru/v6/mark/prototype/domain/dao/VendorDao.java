package ru.v6.mark.prototype.domain.dao;

import org.springframework.stereotype.Repository;
import ru.v6.mark.prototype.domain.criteria.VendorCriteria;
import ru.v6.mark.prototype.domain.entity.Vendor;

import java.util.HashMap;
import java.util.Map;

@Repository
public class VendorDao extends BaseCriteriaDao<Vendor, VendorCriteria> {

    @Override
    public String getCriteriaCondition(VendorCriteria criteria) {
        String result = "";

        if (criteria.getDeleted() != null) {
            result += " and vendor.deleted = :deleted ";
        }

        if (criteria.getInn() != null) {
            result += " and vendor.inn = :inn";
        }
        return !result.isEmpty() ? result.replaceFirst(" and ", " where ") : "";
    }

    @Override
    public Map<String, Object> getCriteriaParams(VendorCriteria criteria) {
        Map<String, Object> result = new HashMap<>();
        if (criteria.getDeleted() != null) {
            result.put("deleted", criteria.getDeleted());
        }

        if (criteria.getInn() != null) {
            result.put("inn", criteria.getInn());
        }

        return result;
    }
}
