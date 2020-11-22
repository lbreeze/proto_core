package ru.v6.mark.prototype.domain.dao;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.Session;
import org.hibernate.jpa.QueryHints;
import org.springframework.stereotype.Repository;
import ru.v6.mark.prototype.domain.entity.BaseEntity;
import ru.v6.mark.prototype.exception.*;

import javax.persistence.*;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@Repository
public class BaseDao<T extends BaseEntity> {
    @PersistenceContext
    protected EntityManager entityManager;
    //@PersistenceContext
    //protected EntityManager entityManagerSecondary;

    public static final int DEFAULT_BATCH_SIZE = 30;

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    public final Session getSession() {
        return getEntityManager().unwrap(Session.class);
//        Object delegate = getEntityManager().getDelegate();
//        if (delegate instanceof Session) {
//            return (Session) delegate;
//        } else if (getEntityManager() instanceof HibernateEntityManager) {
//            return ((HibernateEntityManager) getEntityManager()).getSession();
//        } else {
//            throw new IllegalStateException("Cannot obtain native Hibernate Session from given JPA EntityManager: " + getEntityManager().getClass());
//        }
    }

    public T getByNaturalId(Class<T> entityClass, Map<String, Object> naturalIds) {
        try {
            //noinspection unchecked
            return (T) getNaturalIdLoadAccess(entityClass, naturalIds).load();
        } catch (Throwable e) {
            if (e instanceof ApplicationException) {
                throw (ApplicationException) e;
            } else {
                throw new DaoException(e);
            }
        }
    }

    public T loadNaturalById(Class<T> entityClass, Map<String, Object> naturalIds) {
        try {
            //noinspection unchecked
            return (T) getNaturalIdLoadAccess(entityClass, naturalIds).getReference();
        } catch (Throwable e) {
            if (e instanceof ApplicationException) {
                throw (ApplicationException) e;
            } else {
                throw new DaoException(e, "error.record.not.found", naturalIds, getTableName(entityClass));
            }
        }
    }

    public T getById(Serializable id) {
        return getById(id, false);
    }

    protected T getById(Serializable id, boolean strict) {
        if (strict) {
            return loadById(getEntityClass(), id);
        } else {
            return getById(getEntityClass(), id);
        }
    }

    public <E> E getById(Class<E> entityClass, Serializable id) {
        try {
            return getEntityManager().find(entityClass, id);
        } catch (Throwable e) {
            if (e instanceof ApplicationException) {
                throw (ApplicationException) e;
            } else {
                throw new DaoException(e);
            }
        }
    }

    public T getById(Serializable id, String... properties) {
        EntityGraph graph = getEntityManager().createEntityGraph(getEntityClass());
        for (String property : properties) {
            if (property.contains(".")) {
                String[] split = property.split("\\.");
                Subgraph subgraph = graph.addSubgraph(split[0]);
                for (int i = 1; i < split.length; i++) {
                    subgraph = subgraph.addSubgraph(split[i]);
                }
            } else {
                graph.addSubgraph(property);
            }
        }

        Map<String, Object> hints = new HashMap<>();
        hints.put(QueryHints.HINT_FETCHGRAPH, graph);

        return getEntityManager().find(getEntityClass(), id, hints);
    }

    protected T loadById(Class<T> entityClass, Serializable id) {
        try {
            return getEntityManager().getReference(entityClass, id);
        } catch (Throwable e) {
            if (e instanceof ApplicationException) {
                throw (ApplicationException) e;
            } else {
                throw new DaoException(e, "error.record.not.found", id, getTableName(entityClass));
            }
        }
    }

    public <E> void mergeEntity(E source, E target) {
        for (Field field : source.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object srcValue = field.get(source);
                Object tgtValue = target == null ? null : field.get(target);
                if (srcValue != null) {
                    // field.isAnnotationPresent(Cascade.class) && field.getAnnotation(Cascade.class).value();

                    boolean nested = ((field.isAnnotationPresent(OneToOne.class) && Arrays.stream(field.getAnnotation(OneToOne.class).cascade()).anyMatch(c -> c.equals(CascadeType.ALL) || c.equals(CascadeType.PERSIST) || c.equals(CascadeType.MERGE))) ||
                            (field.isAnnotationPresent(ManyToOne.class) && Arrays.stream(field.getAnnotation(ManyToOne.class).cascade()).anyMatch(c -> c.equals(CascadeType.ALL) || c.equals(CascadeType.PERSIST) || c.equals(CascadeType.MERGE))) ||
                            (field.isAnnotationPresent(OneToMany.class) && Arrays.stream(field.getAnnotation(OneToMany.class).cascade()).anyMatch(c -> c.equals(CascadeType.ALL) || c.equals(CascadeType.PERSIST) || c.equals(CascadeType.MERGE))) ||
                            (field.isAnnotationPresent(ManyToMany.class) && Arrays.stream(field.getAnnotation(ManyToMany.class).cascade()).anyMatch(c -> c.equals(CascadeType.ALL) || c.equals(CascadeType.PERSIST) || c.equals(CascadeType.MERGE)))) &&
                            (!field.isAnnotationPresent(JoinColumn.class) || field.getAnnotation(JoinColumn.class).insertable() || field.getAnnotation(JoinColumn.class).updatable());

                    if (srcValue instanceof BaseEntity) {
                        BaseEntity srcEntity = (BaseEntity) srcValue;
                        BaseEntity tgtEntity = (BaseEntity) tgtValue;
                        if (nested) {
                            if (tgtEntity == null) {
                                try {
                                    tgtEntity = srcEntity.getClass().newInstance();
                                } catch (InstantiationException e) {
                                    throw ApplicationException.build(e.getCause(), e.getMessage());
                                }
                            }
                            mergeEntity(srcEntity, tgtEntity);
                            field.set(target, tgtEntity);
                        } else if (srcEntity.getId() == null) {
                            //todo create new item or find reference?
                            field.set(target, srcEntity);
                        } else {
                            field.set(target, getById(srcEntity.getClass(), srcEntity.getId()));
                        }
                    } else if (srcValue instanceof List) {
                        final List srcList = new ArrayList((List) srcValue);
                        List originalList = (List) tgtValue;

                        //List originalList = new ArrayList();
                        if (originalList != null) {
                            //originalList.addAll(tgtList);
                            originalList.clear();
                        } else {
                            originalList = new ArrayList();
                            field.set(target, originalList);
                        }

                        final List tgtList = originalList;

                        srcList.forEach(srcItem -> {
                            //int index = originalList.indexOf(srcItem);
                            if (srcItem instanceof BaseEntity) {
                                BaseEntity tgtItem;
                                if (nested) {
                                    try {
                                        if (((BaseEntity) srcItem).getId() == null) {
                                            tgtItem = (BaseEntity) srcItem.getClass().newInstance();
                                        } else {
                                            tgtItem = (BaseEntity) getById(srcItem.getClass(), ((BaseEntity) srcItem).getId());
                                        }
                                    } catch (InstantiationException  | IllegalAccessException e) {
                                        e.printStackTrace();
                                        throw ApplicationException.build(e, e.getMessage());
                                    }
                                    mergeEntity(srcItem, tgtItem);

                                } else {
                                    if (((BaseEntity) srcItem).getId() != null) {
                                        tgtItem = (BaseEntity) getById(srcItem.getClass(), ((BaseEntity) srcItem).getId());
                                    } else {
                                        tgtItem = (BaseEntity) srcItem;
                                    }
                                }
                                tgtList.add(tgtItem);
                            } else {
                                tgtList.add(srcItem);
                            }
                        });
                    } else {
                        field.set(target, srcValue);
                    }
                } else {
                    // todo default value, null or unchanged value???
//                    if (tgtValue != null && !(tgtValue instanceof List)) {
//                        field.set(target, null);
//                    }
                }
            } catch (IllegalAccessException e) {
                throw ApplicationException.build(e.getCause(), e.getMessage());
                //e.printStackTrace();
            }
        }

    }

    public List<T> findAll() {
        return findAll(null);
    }

    public List<T> findAll(String orderBy) {
        return findList(new QueryBuilder<T>() {
            @Override
            protected TypedQuery<T> doBuildQuery(EntityManager entityManager) {
                String alias = getEntityClass().getSimpleName().toLowerCase();
                TypedQuery<T> result = entityManager.createQuery("from " + getEntityClass().getSimpleName() + " " + alias + (orderBy != null ? " order by " + alias + "." + orderBy : ""), getEntityClass());
                return result;
            }
        });
    }

    public T save(T entity) {
        return save(entity, false);
    }

    protected T save(T entity, boolean clearContext) {
        try {
            if (entity.getId() != null) {
                entity = getEntityManager().merge(entity);   //один раз  insert
            } else {
                getEntityManager().persist(entity);
            }
            getEntityManager().flush();                 //второй  - при нормальной работе update, при неправильной ещё insert
            //
            if (clearContext) {
                getEntityManager().clear();
            }
            //
            return entity;
        } catch (Throwable e) {
            // e.printStackTrace();
            if (ThrowableUtils.isContainsCause(e, org.hibernate.exception.ConstraintViolationException.class)) {
                org.hibernate.exception.ConstraintViolationException violationException = (org.hibernate.exception.ConstraintViolationException) ThrowableUtils.getCauseThrowable(e, org.hibernate.exception.ConstraintViolationException.class);
                throw new ConstraintDatabaseException(e, "error.save.constraint.database", violationException.getConstraintName());
            } else if (ThrowableUtils.isContainsCause(e, javax.validation.ConstraintViolationException.class)) {
                javax.validation.ConstraintViolationException violationException = (javax.validation.ConstraintViolationException) ThrowableUtils.getCauseThrowable(e, javax.validation.ConstraintViolationException.class);
                throw new ConstraintValidationException("error.save.constraint.validation", violationException.getConstraintViolations());
            } else {
                if (e instanceof OptimisticLockException) {
                    throw e;
                } else if (e instanceof ApplicationException) {
                    throw (ApplicationException) e;
                } else {
                    throw new DaoException(e);
                }
            }
        }
    }

    public List<T> saveAll(Collection<T> entities, boolean clearContext) {
        try {
            List<T> result = new ArrayList<>();
            for (T entity : entities) {
                result.add(getEntityManager().merge(entity));
            }
            getEntityManager().flush();
            //
            if (clearContext) {
                getEntityManager().clear();
            }
            //
            return result;
        } catch (Throwable e) {
            // e.printStackTrace();
            if (ThrowableUtils.isContainsCause(e, org.hibernate.exception.ConstraintViolationException.class)) {
                org.hibernate.exception.ConstraintViolationException violationException = (org.hibernate.exception.ConstraintViolationException) ThrowableUtils.getCauseThrowable(e, org.hibernate.exception.ConstraintViolationException.class);
                throw new ConstraintDatabaseException(e, "error.save.constraint.database", violationException.getConstraintName());
            } else if (ThrowableUtils.isContainsCause(e, javax.validation.ConstraintViolationException.class)) {
                javax.validation.ConstraintViolationException violationException = (javax.validation.ConstraintViolationException) ThrowableUtils.getCauseThrowable(e, javax.validation.ConstraintViolationException.class);
                throw new ConstraintValidationException("error.save.constraint.validation", violationException.getConstraintViolations());
            } else {
                if (e instanceof ApplicationException) {
                    throw (ApplicationException) e;
                } else {
                    throw new DaoException(e);
                }
            }
        }
    }

    public void removeAll(Collection<T> entities, boolean clearContext) {
        try {
            for (T entity : entities) {
                getEntityManager().remove(entity);
            }
            getEntityManager().flush();
            //
            if (clearContext) {
                getEntityManager().clear();
            }
            //
        } catch (Throwable e) {
            if (ThrowableUtils.isContainsCause(e, org.hibernate.exception.ConstraintViolationException.class)) {
                org.hibernate.exception.ConstraintViolationException violationException = (org.hibernate.exception.ConstraintViolationException) ThrowableUtils.getCauseThrowable(e, org.hibernate.exception.ConstraintViolationException.class);
                throw new ConstraintDatabaseException(e, "error.save.constraint.database", violationException.getConstraintName());
            } else if (ThrowableUtils.isContainsCause(e, javax.validation.ConstraintViolationException.class)) {
                javax.validation.ConstraintViolationException violationException = (javax.validation.ConstraintViolationException) ThrowableUtils.getCauseThrowable(e, javax.validation.ConstraintViolationException.class);
                throw new ConstraintValidationException("error.save.constraint.validation", violationException.getConstraintViolations());
            } else {
                if (e instanceof ApplicationException) {
                    throw (ApplicationException) e;
                } else {
                    throw new DaoException(e);
                }
            }
        }
    }

    public void remove(T entity) {
        try {
            getEntityManager().remove(entity);
            getEntityManager().flush();
        } catch (Throwable e) {
            if (ThrowableUtils.isContainsCause(e, org.hibernate.exception.ConstraintViolationException.class)) {
                org.hibernate.exception.ConstraintViolationException violationException = (org.hibernate.exception.ConstraintViolationException) ThrowableUtils.getCauseThrowable(e, org.hibernate.exception.ConstraintViolationException.class);
                throw new ConstraintDatabaseException(e, "error.remove.constraint.database", violationException.getConstraintName());
            } else {
                if (e instanceof ApplicationException) {
                    throw (ApplicationException) e;
                } else {
                    throw new DaoException(e);
                }
            }
        }
    }

    protected <U> U findUnique(QueryBuilder<U> builder, boolean strict) {
        try {
            return builder.buildQuery(getEntityManager()).setMaxResults(1).getSingleResult();
        } catch (NoResultException e) {
            if (strict) {
                throw new DaoException(e);
            } else {
                return null;
            }
        } catch (Throwable e) {
            if (e instanceof ApplicationException) {
                throw (ApplicationException) e;
            } else {
                throw new DaoException(e);
            }
        }
    }

    protected <U> List<U> findList(QueryBuilder<U> builder) {
        try {
            return builder.buildQuery(getEntityManager()).getResultList();
        } catch (Throwable e) {
            if (e instanceof ApplicationException) {
                throw (ApplicationException) e;
            } else {
                throw new DaoException(e);
            }
        }
    }

    private NaturalIdLoadAccess getNaturalIdLoadAccess(Class<?> entityClass, Map<String, Object> naturalIds) {
        NaturalIdLoadAccess access = getSession().byNaturalId(entityClass);
        for (Map.Entry<String, Object> entry : naturalIds.entrySet()) {
            access = access.using(entry.getKey(), entry.getValue());
        }
        return access;
    }

    protected String getTableName(Class<?> entityClass) {
        Table table = entityClass.getAnnotation(Table.class);
        if (table != null) {
            if (StringUtils.isNotBlank(table.name())) {
                return table.name();
            }
        }
        return getEntityName(entityClass);
    }

    protected String getEntityName(Class<?> entityClass) {
        Entity entity = entityClass.getAnnotation(Entity.class);
        if (entity != null) {
            if (StringUtils.isNotBlank(entity.name())) {
                return entity.name();
            }
        }
        return entityClass.getName();
    }

    public void detach(T entity) {
        getEntityManager().detach(entity);
    }

    public void refresh(T entity) {
        getEntityManager().refresh(entity);
    }

    public void clearCache(Class entityClass) {
        getEntityManager().getEntityManagerFactory().getCache().evict(entityClass);
    }

    public int executeUpdate(String query) {
        return getEntityManager().createNativeQuery(query).executeUpdate();
    }

    public List executeSelect(String sql) {
        return getEntityManager().createNativeQuery(sql).getResultList();
    }

    public List executeSelect(String sql, Map<String, Object> params) {
        Query query = getEntityManager().createNativeQuery(sql);

        params.forEach(query::setParameter);

        return query.getResultList();
    }

    protected Class<T> getEntityClass() {
        Type type = getClass().getGenericSuperclass();
        while (!(type instanceof ParameterizedType)) {
            type = ((Class) type).getGenericSuperclass();
        }
        //noinspection unchecked
        return (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[0];
    }

    public void clear() {
        getEntityManager().clear();
    }
}

