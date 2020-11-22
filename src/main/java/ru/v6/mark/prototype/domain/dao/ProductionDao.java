package ru.v6.mark.prototype.domain.dao;

import org.springframework.stereotype.Repository;
import ru.v6.mark.prototype.domain.criteria.ProductionCriteria;
import ru.v6.mark.prototype.domain.entity.Production;

import java.util.HashMap;
import java.util.Map;

@Repository
public class ProductionDao extends BaseCriteriaDao<Production, ProductionCriteria> {

    @Override
    public String getCriteriaCondition(ProductionCriteria criteria) {
        String result = "";
        if (criteria.getDeleted() != null) {
            result += " and production.deleted = :deleted";
        }

        if (criteria.getArticle() != null) {
            result += " and production.goods.article = : article";
        }
        if (criteria.getEan() != null) {
            result += " and production.ean like :ean";
        }
        return !result.isEmpty() ? result.replaceFirst(" and ", " where ") : "";
    }

    @Override
    public Map<String, Object> getCriteriaParams(ProductionCriteria criteria) {
        Map<String, Object> result = new HashMap<>();
        if (criteria.getDeleted() != null) {
            result.put("deleted", criteria.getDeleted());
        }
        if (criteria.getArticle() != null) {
            result.put("article", criteria.getArticle());
        }
        if (criteria.getEan() != null) {
            result.put("ean", "%" + criteria.getEan() + "%");
        }

        return result;
    }
}
