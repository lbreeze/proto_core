package ru.v6.mark.prototype.domain.dao;

import org.springframework.stereotype.Repository;
import ru.v6.mark.prototype.domain.criteria.KIZOrderCriteria;
import ru.v6.mark.prototype.domain.entity.KIZOrder;

import java.util.HashMap;
import java.util.Map;

@Repository
public class KIZOrderDao extends BaseCriteriaDao<KIZOrder, KIZOrderCriteria> {

    @Override
    public String getCriteriaCondition(KIZOrderCriteria criteria) {
        String result = "";
        if ((criteria.getArticle() != null) || (criteria.getEan() != null)) {
            result = " join kizorder.positions positions ";
        }

        if (criteria.getDeleted() != null) {
            result += " and kizorder.deleted = :deleted ";
        }

        if (criteria.getOrderType() != null) {
            result += " and kizorder.orderType= :orderType ";
        }

        if (criteria.getStatus() != null) {
            result += " and kizorder.status = :status ";
        }

        if (criteria.getArticle() != null) {
            result += " and positions.goods.article = : article";
        }
        if (criteria.getEan() != null) {
            result += " and positions.ean like :ean";
        }

        if (criteria.getDepartment() != null) {
            result += " and kizorder.departmentCode = :department";
        } else if (criteria.getAllDepartments() != null) {
            result += " and kizorder.departmentCode in :allDepartments";
        }
        return !result.isEmpty() ? result.replaceFirst(" and ", " where ") : "";
    }

    @Override
    public Map<String, Object> getCriteriaParams(KIZOrderCriteria criteria) {
        Map<String, Object> result = new HashMap<>();
        if (criteria.getDeleted() != null) {
            result.put("deleted", criteria.getDeleted());
        }

        if (criteria.getOrderType() != null) {
            result.put("orderType", criteria.getOrderType());
        }

        if (criteria.getStatus() != null) {
            result.put("status", criteria.getStatus());
        }

        if (criteria.getArticle() != null) {
            result.put("article", criteria.getArticle());
        }

        if (criteria.getEan() != null) {
            result.put("ean", "%" + criteria.getEan() + "%");
        }

        if (criteria.getDepartment() != null) {
            result.put("department", criteria.getDepartment());
        } else if (criteria.getAllDepartments() != null) {
            result.put("allDepartments", criteria.getAllDepartments());
        }

        return result;
    }
}
