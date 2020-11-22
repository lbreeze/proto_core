package ru.v6.mark.prototype.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.constant.KIZMarkStatus;
import ru.v6.mark.prototype.domain.criteria.KIZPositionCriteria;
import ru.v6.mark.prototype.domain.dao.BaseCriteriaDao;
import ru.v6.mark.prototype.domain.dao.KIZPositionDao;
import ru.v6.mark.prototype.domain.entity.KIZPosition;

import java.util.List;

@Service
public class KIZPositionService extends EntityCriteriaService<KIZPosition, KIZPositionCriteria> {

    @Autowired
    KIZPositionDao kizPositionDao;

    @Override
    protected BaseCriteriaDao<KIZPosition, KIZPositionCriteria> getPrimaryCriteriaDao() {
        return kizPositionDao;
    }

    public List<KIZPosition> findPositionsToTurn() {
        KIZPositionCriteria criteria = new KIZPositionCriteria();
        criteria.setMarkStatus(KIZMarkStatus.VALIDATED);
        return kizPositionDao.findByCriteria(criteria);
    }

}
