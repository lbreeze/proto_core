package ru.v6.mark.prototype.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.dao.AcceptancePositionDao;
import ru.v6.mark.prototype.domain.dao.BaseDao;
import ru.v6.mark.prototype.domain.entity.AcceptancePosition;

import javax.annotation.Resource;

@Service
public class AcceptancePositionService extends EntityService<AcceptancePosition> {

    @Autowired
    AcceptancePositionDao acceptancePositionDao;
    @Resource
    AcceptancePositionService acceptancePositionService;

    @Override
    protected EntityService<AcceptancePosition> getDeferredService() {
        return acceptancePositionService;
    }

    @Override
    protected BaseDao<AcceptancePosition> getPrimaryDao() {
        return acceptancePositionDao;
    }

}
