package ru.v6.mark.prototype.domain.dao;

import org.springframework.stereotype.Repository;
import ru.v6.mark.prototype.domain.criteria.GoodsCriteria;
import ru.v6.mark.prototype.domain.entity.Goods;

import java.util.HashMap;
import java.util.Map;

@Repository
public class GoodsDao extends BaseCriteriaDao<Goods, GoodsCriteria> {

    @Override
    public String getCriteriaCondition(GoodsCriteria criteria) {
        String result = "";
        if (criteria.getDeleted() != null) {
            result += " and goods.deleted = :deleted";
        }

        if (criteria.getArticle() != null) {
            result += " and goods.article = : article";
        }
        if (criteria.getEan() != null) {
            result += " and goods.ean like :ean";
        }
        if (criteria.getProducerInn() != null) {
            result += " and goods.producerInn = :producerInn";
        }
        return !result.isEmpty() ? result.replaceFirst(" and ", " where ") : "";
    }

    @Override
    public Map<String, Object> getCriteriaParams(GoodsCriteria criteria) {
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
        if (criteria.getProducerInn() != null) {
            result.put("producerInn", criteria.getProducerInn());
        }

        return result;
    }
}
