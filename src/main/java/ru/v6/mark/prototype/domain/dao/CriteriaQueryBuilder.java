package ru.v6.mark.prototype.domain.dao;

import org.apache.commons.lang3.StringUtils;
import ru.v6.mark.prototype.domain.criteria.OrderInfo;
import ru.v6.mark.prototype.exception.ApplicationException;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public abstract class CriteriaQueryBuilder<T> extends QueryBuilder<T> {
    private CriteriaBuilder cb;
    private Metamodel metamodel;
    protected List<Order> orderBy = new ArrayList<>();

    @Override
    protected TypedQuery<T> doBuildQuery(EntityManager entityManager) {
        cb = entityManager.getCriteriaBuilder();
        metamodel = entityManager.getMetamodel();
        //
        CriteriaQuery<T> criteriaQuery = buildCriteria(cb);
        //
        if (!orderBy.isEmpty()) {
            criteriaQuery.orderBy(orderBy);
        }
        //
        return entityManager.createQuery(criteriaQuery);
    }

    protected <R> void addOrderBy(Root<R> entity, List<OrderInfo> orderInfo) {
        if (orderInfo != null) {
            for (OrderInfo oi : orderInfo) {
                if (oi.getSortField() != null) {
                    Path path = getOrderedProperty(entity, oi.getSortField());
                    if ("DESC".equals(oi.getSortDir())) {
                        orderBy.add(cb.desc(path));
                    } else {
                        orderBy.add(cb.asc(path));
                    }
                }
            }
        }
    }

    protected Path getOrderedProperty(Root entity, String fullPath) {
        boolean useFrom = false;
        //
        Path<?> path = entity;
        From from = entity;
        //
        if (StringUtils.isNotBlank(fullPath)) {
            List<String> paths = Arrays.asList(fullPath.split("\\."));
            Iterator<String> it = paths.iterator();
            while (it.hasNext()) {
                String property = it.next();
                //
                path = useFrom ? from.get(property) : path.get(property);
                //
                if (it.hasNext()) {
                    // если есть еще звенья
                    ManagedType<?> managedType = metamodel.managedType(path.getJavaType());
                    switch (managedType.getPersistenceType()) {
                        case MAPPED_SUPERCLASS:
                        case ENTITY:
                            // делаем join на связанную таблицу
                            from = from.join(property, JoinType.LEFT);
                            useFrom = true;
                            break;
                        case EMBEDDABLE:
                            // составное свойство, перехода на др. таблицу нет
                            useFrom = false;
                            break;
                        case BASIC:
                            // ошибка в fullPath, у простого свойства нет дочерних звеньев
                            throw ApplicationException.build("error.dao.sort").parameters(fullPath, property);
                    }
                }
            }
        }
        //
        return path;
    }

    public abstract CriteriaQuery<T> buildCriteria(CriteriaBuilder cb);
    //protected abstract <R> List<Predicate> createPredicates(CriteriaQuery<T> query, CriteriaBuilder cb, Root<R> root, C searchCriteria);
}
