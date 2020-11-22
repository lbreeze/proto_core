package ru.v6.mark.prototype.service;

import ru.v6.mark.prototype.domain.criteria.OrderInfo;
import ru.v6.mark.prototype.domain.criteria.PagingCriteria;
import ru.v6.mark.prototype.domain.dao.BaseCriteriaDao;
import ru.v6.mark.prototype.domain.dao.BaseDao;
import ru.v6.mark.prototype.domain.entity.BaseEntity;
import ru.v6.mark.prototype.exception.ApplicationException;
import ru.v6.mark.prototype.web.context.PagedResult;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public abstract class EntityCriteriaService<T extends BaseEntity, C> extends EntityService<T> {

    public List<T> findByCriteria(C criteria) {
        return getPrimaryCriteriaDao().findByCriteria(criteria);
    }

    public Long countByCriteria(C criteria) {
        return getPrimaryCriteriaDao().countByCriteria(criteria);
    }

    public PagedResult<T> getPageByCriteria(C criteria) {
        PagedResult<T> result = new PagedResult<>();
        if (criteria instanceof OrderInfo) {
            result.setSortField(((OrderInfo) criteria).getSortField());
            result.setSortDir(((OrderInfo) criteria).getSortDir());
        }
        if (criteria instanceof PagingCriteria) {
            result.setPage(((PagingCriteria) criteria).getPage());
            result.setRowsPerPage(((PagingCriteria) criteria).getRowsPerPage());
        }
        final CountDownLatch latch = new CountDownLatch(1);
        Thread countThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Long count = countByCriteria(criteria);
                result.setCount(count);
                latch.countDown();
            }
        });
        countThread.start();
        List<T> data = findByCriteria(criteria);
        result.setData(data);
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            throw ApplicationException.build(e, e.getMessage());
        }
        return result;
    }

    @Override
    protected final BaseDao<T> getPrimaryDao() {
        return getPrimaryCriteriaDao();
    }

    protected abstract BaseCriteriaDao<T, C> getPrimaryCriteriaDao();
}
