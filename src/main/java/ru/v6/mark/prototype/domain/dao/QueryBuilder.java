package ru.v6.mark.prototype.domain.dao;

import org.hibernate.jpa.QueryHints;

import javax.persistence.EntityManager;
import javax.persistence.Parameter;
import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.Map;

public abstract class QueryBuilder<T> {
    private Integer firstResult;
    private Integer maxResult;
    private Boolean cacheable;
    private Map<Parameter, Object> queryParams = new HashMap<>();

    TypedQuery<T> buildQuery(EntityManager entityManager) {
        TypedQuery<T> query = doBuildQuery(entityManager);
        for (Map.Entry<Parameter, Object> entry : queryParams.entrySet()) {
            //noinspection unchecked
            query.setParameter(entry.getKey(), entry.getValue());
        }
        //
        if (firstResult != null && firstResult >= 0) {
            query.setFirstResult(firstResult);
        }
        if (maxResult != null && maxResult >= 0) {
            query.setMaxResults(maxResult);
            query.setHint(QueryHints.HINT_FETCH_SIZE, maxResult);
        }
        if (cacheable != null) {
            query.setHint(QueryHints.HINT_CACHEABLE, cacheable);
        }
        //
        return query;
    }

    protected abstract TypedQuery<T> doBuildQuery(EntityManager entityManager);

    protected <V> void addParam(Parameter<V> parameter, V value) {
        queryParams.put(parameter, value);
    }

    public void setFirstResult(Integer firstResult) {
        this.firstResult = firstResult;
    }

    public void setMaxResult(Integer maxResult) {
        this.maxResult = maxResult;
    }

    public void setCacheable(Boolean cacheable) {
        this.cacheable = cacheable;
    }

}
