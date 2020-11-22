package ru.v6.mark.prototype.domain.dao;

import org.springframework.stereotype.Repository;
import ru.v6.mark.prototype.domain.criteria.ArticleCriteria;
import ru.v6.mark.prototype.domain.entity.Article;

import java.util.HashMap;
import java.util.Map;

@Repository
public class ArticleDao extends BaseCriteriaDao<Article, ArticleCriteria> {

    @Override
    public String getCriteriaCondition(ArticleCriteria criteria) {
        String result = "";

        if ((criteria.getEan() != null) || (criteria.getProducerInn() != null)) {
            result += " join article.goods goods";
        }

        if (criteria.getArticle() != null) {
            result += " and article.article = : article";
        }
        if (criteria.getEan() != null) {
            result += " and goods.ean like :ean";
        }
        if (criteria.getImported() != null) {
            result += " and imported = :imported";
        }
        if (criteria.getProducerInn() != null) {
            result += " and goods.producerInn = :producerInn";
        }
        return !result.isEmpty() ? result.replaceFirst(" and ", " where ") : "";
    }

    @Override
    public Map<String, Object> getCriteriaParams(ArticleCriteria criteria) {
        Map<String, Object> result = new HashMap<>();
        if (criteria.getArticle() != null) {
            result.put("article", criteria.getArticle());
        }
        if (criteria.getEan() != null) {
            result.put("ean", '%' + criteria.getEan() + '%');
        }
        if (criteria.getImported() != null) {
            result.put("imported", criteria.getImported());
        }
        if (criteria.getProducerInn() != null) {
            result.put("producerInn", criteria.getProducerInn());
        }

        return result;
    }
}
