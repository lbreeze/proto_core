package ru.v6.mark.prototype.domain.dao;

import org.springframework.stereotype.Repository;
import ru.v6.mark.prototype.domain.criteria.GtinCriteria;
import ru.v6.mark.prototype.domain.entity.Gtin;

import java.util.HashMap;
import java.util.Map;

@Repository
public class GtinDao extends BaseCriteriaDao<Gtin, GtinCriteria> {

    @Override
    public String getCriteriaCondition(GtinCriteria criteria) {
        String result = "";
        if (criteria.getImported() != null) {
            result += " and gtin.imported = :imported";
        }
        if (criteria.getMarkSubType() != null) {
            result += " and gtin.markSubType = :markSubType";
        }
        if (criteria.getOrganization() != null) {
            result += " and gtin.organization = :organization";
        }
        if (criteria.getEan() != null) {
            result += " and gtin.gtin = :ean";
        }
        return !result.isEmpty() ? result.replaceFirst(" and ", " where ") : "";
    }

    @Override
    public Map<String, Object> getCriteriaParams(GtinCriteria criteria) {
        Map<String, Object> result = new HashMap<>();
        if (criteria.getImported() != null) {
            result.put("imported", criteria.getImported());
        }
        if (criteria.getMarkSubType()  != null) {
            result.put("markSubType", criteria.getMarkSubType());
        }
        if (criteria.getOrganization() != null) {
            result.put("organization", criteria.getOrganization());
        }
        if (criteria.getEan() != null) {
            result.put("ean", criteria.getEan());
        }
        return result;
    }
}
