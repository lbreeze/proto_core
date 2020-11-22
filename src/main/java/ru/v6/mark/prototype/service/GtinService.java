package ru.v6.mark.prototype.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.criteria.GtinCriteria;
import ru.v6.mark.prototype.domain.dao.BaseCriteriaDao;
import ru.v6.mark.prototype.domain.dao.GtinDao;
import ru.v6.mark.prototype.domain.entity.Gtin;

@Service
public class GtinService extends EntityCriteriaService<Gtin, GtinCriteria> {

    @Autowired
    GtinDao gtinDao;

    @Override
    protected BaseCriteriaDao<Gtin, GtinCriteria> getPrimaryCriteriaDao() {
        return gtinDao;
    }
}
