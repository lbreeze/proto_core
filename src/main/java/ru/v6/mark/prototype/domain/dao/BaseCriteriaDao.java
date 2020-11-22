package ru.v6.mark.prototype.domain.dao;

import ru.v6.mark.prototype.domain.criteria.OrderInfo;
import ru.v6.mark.prototype.domain.criteria.PagingCriteria;
import ru.v6.mark.prototype.domain.entity.BaseEntity;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;

public abstract class BaseCriteriaDao<T extends BaseEntity, C> extends BaseDao<T> {

    public List<T> findByCriteria(C criteria) {
        return findList(new QueryBuilder<T>() {
            @Override
            protected TypedQuery<T> doBuildQuery(EntityManager entityManager) {
                String alias = getEntityClass().getSimpleName().toLowerCase();
                String criteriaCondition = getCriteriaCondition(criteria);
                String queryStr = "select distinct " + alias + " from " + getEntityClass().getSimpleName() + " " + alias + " " + (criteriaCondition == null? "" : criteriaCondition);

                if (criteria instanceof OrderInfo) {
                    OrderInfo orderInfo = (OrderInfo) criteria;
                    if (orderInfo.getSortField() != null) {
                        queryStr += " order by " + alias + "." + orderInfo.getSortField() + " " + orderInfo.getSortDir();
                    }
                }

                TypedQuery<T> query = entityManager.createQuery(queryStr, getEntityClass());

                for (Map.Entry<String, Object> param : getCriteriaParams(criteria).entrySet()) {
                    query.setParameter(param.getKey(), param.getValue());
                }

                if (criteria instanceof PagingCriteria) {
                    PagingCriteria pagingCriteria = (PagingCriteria) criteria;
                    if (pagingCriteria.getRowsPerPage() > 0) {
                        query.setFirstResult(pagingCriteria.getPage() * pagingCriteria.getRowsPerPage());
                        query.setMaxResults(pagingCriteria.getRowsPerPage());
                    }
                }

                return query;
            }
        });
    }

    public Long countByCriteria(C criteria) {
        return findUnique(new QueryBuilder<Long>() {
            @Override
            protected TypedQuery<Long> doBuildQuery(EntityManager entityManager) {
                String criteriaCondition = getCriteriaCondition(criteria);
                TypedQuery<Long> query = entityManager.createQuery("select count(distinct " + getEntityClass().getSimpleName().toLowerCase() + ") from " + getEntityClass().getSimpleName() + " " + getEntityClass().getSimpleName().toLowerCase() + " " + (criteriaCondition == null? "" : criteriaCondition), Long.class);
                for (Map.Entry<String, Object> param : getCriteriaParams(criteria).entrySet()) {
                    query.setParameter(param.getKey(), param.getValue());
                }
                return query;
            }
        }, true);
    }

    public abstract String getCriteriaCondition(C criteria);

    public abstract Map<String, Object> getCriteriaParams(C criteria);
}
