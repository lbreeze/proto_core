package ru.v6.mark.prototype.service;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.constant.KIZAggregationStatus;
import ru.v6.mark.prototype.domain.criteria.KIZAggregationCriteria;
import ru.v6.mark.prototype.domain.dao.BaseCriteriaDao;
import ru.v6.mark.prototype.domain.dao.KIZAggregationDao;
import ru.v6.mark.prototype.domain.entity.KIZAggregation;
import ru.v6.mark.prototype.service.util.ResultError;
import ru.v6.mark.prototype.service.util.SleepUtil;
import ru.v6.mark.prototype.service.util.StringUtil;

import java.util.ArrayList;
import java.util.List;


@Service
public class KIZAggregationService extends EntityCriteriaService<KIZAggregation, KIZAggregationCriteria>{

    @Autowired
    KIZAggregationDao kizAggregationDao;

    @Autowired
    ClientService clientService;

    @Autowired
    CachedDataReceiver cachedDataReceiver;

    @Override
    protected BaseCriteriaDao<KIZAggregation, KIZAggregationCriteria> getPrimaryCriteriaDao() {
        return kizAggregationDao;
    }

    public String cleanAggregation(KIZAggregation aggregation) {
        String keyAlias = aggregation.getKizOrder().getDepartment().getKeyAlias();
        String docId = clientService.cleanAggregation(keyAlias, getToken(keyAlias), aggregation);
        return docId;
    }

    /**
     * Агрегация кодов и марок
     * @param sscc код агрегации
     */
    public void createAggregation(String sscc) {
        KIZAggregation aggregation = kizAggregationDao.getById(sscc);
        String keyAlias = aggregation.getKizOrder().getDepartment().getKeyAlias();
        ResultError resultError = new ResultError();
        String docId = clientService.createAggregation(keyAlias, getToken(keyAlias), aggregation, resultError);
        if (!StringUtil.hasLength(docId)) {
            aggregation.setStatus(KIZAggregationStatus.ERROR);
            if (StringUtil.hasLength(resultError.getDescription())) {
                aggregation.setStatusDesc(resultError.getDescription());
            } else {
                aggregation.setStatusDesc("Ошибка не определена");
            }

        } else {
            aggregation.setDocId(docId);
            aggregation.setStatus(KIZAggregationStatus.SENT);

            String statusCode = null;

            long t = System.currentTimeMillis();
            while (statusCode == null) {
                SleepUtil.sleep(SleepUtil.CRPT_QUERY_INTERVAL);

                try {
                    String[] status = clientService.getDocumentStatus(getToken(keyAlias), docId);
                    if (status != null && status[0] != null) {
                        statusCode = status[0];
                        if (!statusCode.equals(KIZAggregationStatus.CHECKED_OK.name()) && status[1] != null) {
                            aggregation.setStatusDesc(status[1]);
                        }
                        aggregation.setStatus(getStatus(statusCode) != null ? getStatus(statusCode) : KIZAggregationStatus.ERROR);
                    }
                } catch (JSONException e) {
                    logger.info(e.getMessage());
                }
            }
        }
        kizAggregationDao.save(aggregation);
    }

    public void getStatusAggregation() {
        List<KIZAggregation> aggregations = findErrorAggregation();
        for (KIZAggregation aggregation : aggregations) {
            String keyAlias = aggregation.getKizOrder().getDepartment().getKeyAlias();
            String[] status = clientService.getDocumentStatus(getToken(keyAlias), aggregation.getDocId());
            if (status != null && status[0] != null && !status[0].equals(aggregation.getStatus().name())) {
                aggregation.setStatus(getStatus(status[0]));
                if (status[1] != null && !status[0].equals(KIZAggregationStatus.CHECKED_OK.name())) {
                    aggregation.setStatusDesc(status[1]);
                }
                kizAggregationDao.save(aggregation);
            }
        }
    }

    /**
     * Поиск агрегаций с ошибочными статусами
     * @return
     */
    private List<KIZAggregation> findErrorAggregation() {
        KIZAggregationCriteria criteria = new KIZAggregationCriteria();
        List<KIZAggregationStatus> statuses = new ArrayList<>();
        statuses.add(KIZAggregationStatus.SENT);
        statuses.add(KIZAggregationStatus.CHECKED_NOT_OK);
        statuses.add(KIZAggregationStatus.ERROR);
        criteria.setStatuses(statuses);
        return findByCriteria(criteria);
    }

    /**
     * Поиск кодов агрегации в статусе "Готов к агрегации"
     * @return
     */
    public List<KIZAggregation> findAggregation() {
        KIZAggregationCriteria criteria = new KIZAggregationCriteria();
        criteria.setStatus(KIZAggregationStatus.READY);
        return kizAggregationDao.findByCriteria(criteria);
    }

    private KIZAggregationStatus getStatus(String status) {
        if (!StringUtil.hasLength(status)) {
            return null;
        }
        if (status.equals(KIZAggregationStatus.CHECKED_OK.name())) {
            return KIZAggregationStatus.CHECKED_OK;
        } else if (status.equals(KIZAggregationStatus.CHECKED_NOT_OK.name())) {
            return KIZAggregationStatus.CHECKED_NOT_OK;
        }
        return null;
    }
}
