package ru.v6.mark.prototype.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.v6.mark.prototype.domain.constant.LogPerformance;
import ru.v6.mark.prototype.domain.dao.BaseDao;
import ru.v6.mark.prototype.domain.entity.BaseEntity;
import ru.v6.mark.prototype.domain.entity.DeletableEntity;
import ru.v6.mark.prototype.exception.ApplicationException;
import ru.v6.mark.prototype.web.aspect.NamedThreadFactory;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public abstract class EntityService<T extends BaseEntity> extends BaseService {
    protected Logger logger = LoggerFactory.getLogger(getClass());

    private ExecutorService service = Executors.newSingleThreadExecutor(new NamedThreadFactory(getClass().getSimpleName()));
    private Future<?> future = null;

    private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();

    public List<T> findAll() {
        return getPrimaryDao().findAll(null);
    }

    public List<T> findAll(String orderBy) {
        return getPrimaryDao().findAll(orderBy);
    }

    public <C> C getById(Class<C> clazz, Serializable id) {
        return getPrimaryDao().getById(clazz, id);
    }

    public T getById(Serializable id) {
        return getPrimaryDao().getById(id);
    }

    public T getById(Serializable id, String... properties) {
        return getPrimaryDao().getById(id, properties);
    }

    @SuppressWarnings("unchecked")
    public T save(T entity) {
        T target;
        if (entity.getId() == null) {
            try {
                target = (T) entity.getClass().newInstance();
            } catch (InstantiationException  | IllegalAccessException e) {
                e.printStackTrace();
                throw ApplicationException.build(e, e.getMessage());
            }
        } else {
            target = getPrimaryDao().getById(entity.getId());
            if (target == null) {
                try {
                    target = (T) entity.getClass().newInstance();
                } catch (InstantiationException  | IllegalAccessException e) {
                    e.printStackTrace();
                    throw ApplicationException.build(e, e.getMessage());
                }
            }
        }
        getPrimaryDao().mergeEntity(entity, target);
        return getPrimaryDao().save(target);
    }

    public T saveAsNew(T entity) {
        return getPrimaryDao().save(entity);
    }

    @LogPerformance
    public void deferredSave(T entity) {
        if (getDeferredService() != null) {
            //getPrimaryDao().detach(entity);
            queue.add(entity);
            if (future == null || (future.isDone() && !queue.isEmpty())) {
                future = service.submit(new Runnable() {
                    @Override
                    public void run() {
                        T item = queue.poll();
                        while (item != null) {
                            getDeferredService().saveAsNew(item);
                            item = queue.poll();
                        }
                    }
                });
            }
        } else {
            save(entity);
        }
    }

    public void remove(T entity) {
        if (entity instanceof DeletableEntity) {
            ((DeletableEntity) entity).setDeleted(Boolean.TRUE);
            getPrimaryDao().save(entity);
        } else {
            getPrimaryDao().remove(entity);
        }
    }

    protected EntityService<T> getDeferredService() {
        return null;
    }

    protected abstract BaseDao<T> getPrimaryDao();

}