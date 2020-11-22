package ru.v6.mark.prototype.domain.dao;

import org.springframework.stereotype.Repository;
import ru.v6.mark.prototype.domain.constant.KIZAggregationStatus;
import ru.v6.mark.prototype.domain.criteria.KIZAggregationCriteria;
import ru.v6.mark.prototype.domain.entity.KIZAggregation;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by Michael on 21.12.2019.
 */
@Repository
public class KIZAggregationDao extends BaseCriteriaDao<KIZAggregation, KIZAggregationCriteria> {

    public List<KIZAggregation> findByGln(final String gln, int quantity) {
        return findList(new QueryBuilder<KIZAggregation>() {
            @Override
            protected TypedQuery<KIZAggregation> doBuildQuery(EntityManager entityManager) {
                TypedQuery<KIZAggregation> result = entityManager.createQuery("from KIZAggregation kizAggregation where substring(kizAggregation.sscc, 2, 9) = :gln and kizAggregation.status = :status", KIZAggregation.class);
                result.setParameter("gln", gln.substring(0, 9));
                result.setParameter("status", KIZAggregationStatus.AVAILABLE);
                result.setMaxResults(quantity);
                return result;
            }
        });
    }

    public Long findLastByGln(final String gln) {
        return findUnique(new QueryBuilder<Long>() {
            @Override
            protected TypedQuery<Long> doBuildQuery(EntityManager entityManager) {
                TypedQuery<Long> result = entityManager.createQuery("select max(kizAggregation.boxNum) from KIZAggregation kizAggregation where substring(kizAggregation.sscc, 2, 9) = :gln", Long.class);
                result.setParameter("gln", gln.substring(0, 9));
                return result;
            }
        }, false);
    }

    @Override
    public String getCriteriaCondition(KIZAggregationCriteria criteria) {
        String result = "";
        if (criteria.getStatus() != null) {
            result += " and kizaggregation.status = :status";
        }
        if (criteria.getGln() != null) {
            result += " and kizaggregation.sscc like :gln";
        }
        if (criteria.getKizOrder() != null) {
            result += " and kizaggregation.kizOrder = :kizOrder";
        }
        if (criteria.getStatuses() != null) {
            result = " and kizaggregation.status in (:statuses)";
        }
        return !result.isEmpty() ? result.replaceFirst(" and ", " where ") : "";
    }

    @Override
    public Map<String, Object> getCriteriaParams(KIZAggregationCriteria criteria) {
        Map<String, Object> result = new HashMap<>();
        if (criteria.getStatus() != null) {
            result.put("status", criteria.getStatus());
        }
        if (criteria.getGln() != null) {
            result.put("gln", "0" + criteria.getGln().substring(0, 9) + "%");
        }
        if (criteria.getKizOrder() != null) {
            result.put("kizOrder", criteria.getKizOrder());
        }
        if (criteria.getStatuses() != null) {
            result.put("statuses", criteria.getStatuses());
        }
        return result;
    }
}
