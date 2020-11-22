package ru.v6.mark.prototype.domain.dao;

import org.springframework.stereotype.Repository;
import ru.v6.mark.prototype.domain.criteria.KIZMarkCriteria;
import ru.v6.mark.prototype.domain.entity.KIZMark;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by Michael on 21.12.2019.
 */
@Repository
public class KIZMarkDao extends BaseCriteriaDao<KIZMark, KIZMarkCriteria> {

    public KIZMark findByMark(final String mark) {
        return findUnique(new QueryBuilder<KIZMark>() {
            @Override
            protected TypedQuery<KIZMark> doBuildQuery(EntityManager entityManager) {
                TypedQuery<KIZMark> result = entityManager.createQuery("from KIZMark kizMark where substring(kizMark.mark, 1, 31) = :mark", KIZMark.class);
                result.setParameter("mark", mark);
                return result;
            }
        }, false);
    }

    @Override
    public String getCriteriaCondition(KIZMarkCriteria criteria) {
        String result = "";
        if (criteria.getStatus() != null) {
            result += " and kizmark.status = :status";
        }
        return !result.isEmpty() ? result.replaceFirst(" and ", " where ") : "";
    }

    @Override
    public Map<String, Object> getCriteriaParams(KIZMarkCriteria criteria) {
        Map<String, Object> result = new HashMap<>();
        if (criteria.getStatus() != null) {
            result.put("status", criteria.getStatus());
        }
        return result;
    }
}
