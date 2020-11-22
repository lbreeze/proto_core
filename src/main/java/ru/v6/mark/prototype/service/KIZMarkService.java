package ru.v6.mark.prototype.service;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.constant.KIZMarkStatus;
import ru.v6.mark.prototype.domain.constant.KIZPositionStatus;
import ru.v6.mark.prototype.domain.constant.LogPerformance;
import ru.v6.mark.prototype.domain.constant.Status;
import ru.v6.mark.prototype.domain.criteria.KIZMarkCriteria;
import ru.v6.mark.prototype.domain.criteria.KIZPositionCriteria;
import ru.v6.mark.prototype.domain.dao.*;
import ru.v6.mark.prototype.domain.entity.KIZMark;
import ru.v6.mark.prototype.domain.entity.KIZPosition;
import ru.v6.mark.prototype.domain.entity.ValidateMarksLog;
import ru.v6.mark.prototype.service.util.SleepUtil;
import ru.v6.mark.prototype.service.util.StringUtil;
import ru.v6.mark.prototype.web.context.CompleteMarkInfo;
import ru.v6.mark.prototype.web.context.RequestWrapper;
import ru.v6.mark.prototype.web.context.Response;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Created by Michael on 21.12.2019.
 */
@Service
public class KIZMarkService extends EntityCriteriaService<KIZMark, KIZMarkCriteria> {

    @Autowired
    KIZMarkDao kizMarkDao;

    @Resource
    KIZMarkService kizMarkService;

    @Autowired
    KIZPositionDao kizPositionDao;

    @Autowired
    ValidateMarksLogDao validateMarksLogDao;

    @Autowired
    GtinDao gtinDao;

    @Autowired
    GoodsDao goodsDao;

    @Autowired
    GoodsService goodsService;

    @Autowired
    ClientService clientService;

    @Autowired
    KIZPositionService kizPositionService;

    @Autowired
    UserDao userDao;

    @Autowired
    ArticleService articleService;

    @Autowired
    PrintCodeService printCodeService;

    @Autowired
    GtinService gtinService;

    @Autowired
    OrganizationService organizationService;


    @Override
    protected BaseCriteriaDao<KIZMark, KIZMarkCriteria> getPrimaryCriteriaDao() {
        return kizMarkDao;
    }

    public String checkMark(String mark) {
        return clientService.checkCode(getToken(), mark);
    }

    public void checkMarks(List<String> marks) {
        String result = clientService.checkCodes(getToken(), marks);

        JSONArray array = new JSONArray(result);
        for (int i=0; i < array.length(); i++) {
            JSONObject json = array.getJSONObject(i).getJSONObject("cisInfo");
            if (json.has("status")) {
                String status = json.get("status").toString();
                String mark = json.get("cis").toString();
                KIZMark kizMark = kizMarkDao.findByMark(mark);
                if (status.equalsIgnoreCase("EMITTED") || status.equalsIgnoreCase("APPLIED")) {
                    kizMark.setStatus(KIZMarkStatus.VALIDATED);
                } else {
                    kizMark.setStatus(KIZMarkStatus.CIRCULATED);
                }
                kizMarkDao.save(kizMark);
            }
        }
    }

    public KIZMark findByMark(String mark) {
        return kizMarkDao.findByMark(mark);
    }

    /**
     * Ввод в оборот КМ позиции
     */
    public void sendMarksToTurn(KIZPosition pos) {

        KIZPosition position = kizPositionService.getById(pos.getId());

        List<KIZMark> marks = position.getMarks().parallelStream().filter(kizMark -> KIZMarkStatus.VALIDATED.equals(kizMark.getStatus())).collect(Collectors.toList());

        String keyAlias = position.getKizOrder().getDepartment().getKeyAlias();
        while (!CollectionUtils.isEmpty(marks)) {
            List<String> marksList = marks.parallelStream().map(KIZMark::getMark).collect(Collectors.toList());
            String docId = clientService.createDocument(keyAlias, getToken(keyAlias), marksList, position);

            if (StringUtils.isEmpty(docId)) {
                position.setStatus(KIZPositionStatus.ERROR);
            } else {
                position.setDocId(docId);

                String[] status = null;

                long t = System.currentTimeMillis();
                while (status == null) {

                    SleepUtil.sleep(SleepUtil.CRPT_QUERY_INTERVAL);
                    try {
                        status = clientService.getDocumentStatus(getToken(keyAlias), docId);

                        KIZPositionStatus kpStatus = getStatus(status[0]);
                        position.setStatus(kpStatus != null ? kpStatus : KIZPositionStatus.ERROR);

                        if (kpStatus != null) {
                            if (!kpStatus.equals(KIZPositionStatus.CHECKED_OK)) {
                                String error = status[1];
                                position.setStatusDesc(error);
                                if (error != null) {
                                    if (error.length() > 71) {
                                        // 14: Недопустимый статус кода маркировки 010290000019415121HBHSrNjb-mDj?, указанного в документе "Ввод в оборот остатков товара".
                                        KIZMark kizMark = marks.parallelStream().filter(km -> km.getMark().startsWith(error.substring(40, 71))).findFirst().orElse(null);
                                        if (kizMark != null) {
                                            kizMark.setStatus(KIZMarkStatus.ERROR);
                                            marks.remove(kizMark);
                                        } else {
                                            marks.parallelStream().forEach(km -> km.setStatus(KIZMarkStatus.ERROR));
                                            marks.clear();
                                        }
                                    } else {
                                        marks.parallelStream().forEach(kizMark -> kizMark.setStatus(KIZMarkStatus.ERROR));
                                        marks.clear();
                                    }
                                } else {
                                    marks.parallelStream().forEach(kizMark -> kizMark.setStatus(KIZMarkStatus.ERROR));
                                    marks.clear();
                                }
                            } else {
                                marks.parallelStream().forEach(kizMark -> kizMark.setStatus(KIZMarkStatus.CIRCULATED));
                                marks.clear();
                            }
                        }
                    } catch (JSONException e) {
                        logger.info(e.getMessage());
                    }
                }
            }
        }
        position = kizPositionDao.save(position);
    }

    /**
     * Получение статуса документов
     */
    public void getStatusDocument() {
        List<KIZPosition> positions = findErrorPosition();
        for (KIZPosition position : positions) {
            String keyAlias = position.getKizOrder().getDepartment().getKeyAlias();
            String[] status = clientService.getDocumentStatus(getToken(keyAlias), position.getDocId());
            if (status != null && status[0] != null && !status[0].equals(position.getStatus().name())) {
                if (status[0].equals(KIZPositionStatus.CHECKED_OK.name())) {
                    position.setStatusDesc("");
                }
                if (status[1] != null && !status[0].equals(KIZPositionStatus.CHECKED_OK.name())) {
                    position.setStatusDesc(status[1]);
                }
                position.setStatus(getStatus(status[0]));
                kizPositionDao.save(position);
            }
        }
    }

    /**
     * Поиск позиций с ошибочными статусами
     *
     * @return
     */
    private List<KIZPosition> findErrorPosition() {
        KIZPositionCriteria criteria = new KIZPositionCriteria();
        List<KIZPositionStatus> statuses = new ArrayList<>();
        // statuses.add(KIZPositionStatus.IN_PROGRESS);
        statuses.add(KIZPositionStatus.CHECKED_NOT_OK);
        statuses.add(KIZPositionStatus.PROCESSING_ERROR);
        statuses.add(KIZPositionStatus.UNDEFINED);
        statuses.add(KIZPositionStatus.ERROR);
        criteria.setStatus(statuses);
        return kizPositionService.findByCriteria(criteria);
    }

    @LogPerformance
    public Response markControlCompleteScan(RequestWrapper<CompleteMarkInfo> completeMarkInfo) {
        ValidateMarksLog log = new ValidateMarksLog();
        log.setUsername(completeMarkInfo.getCurrentUser());
        log.setEan(goodsService.completeEan(completeMarkInfo.getEntity().getEan()));

        List<String> marks = completeMarkInfo.getEntity().getMarks();
        marks.forEach(mark -> {
            KIZMark kizMark = kizMarkDao.findByMark(mark.substring(0, 31));
            kizMark.setStatus(KIZMarkStatus.VALIDATED);
            kizMarkDao.save(kizMark);
        });
        log.setQuantity(marks.size());

        validateMarksLogDao.save(log);
        return new Response(Status.OK);
    }

    private KIZPositionStatus getStatus(String status) {
        if (!StringUtil.hasLength(status)) {
            return null;
        }
//        if (status.equals(KIZPositionStatus.IN_PROGRESS.name())) {
//            return KIZPositionStatus.IN_PROGRESS;
//        } else
        if (status.equals(KIZPositionStatus.CHECKED_OK.name())) {
            return KIZPositionStatus.CHECKED_OK;
        } else if (status.equals(KIZPositionStatus.CHECKED_NOT_OK.name())) {
            return KIZPositionStatus.CHECKED_NOT_OK;
        } else if (status.equals(KIZPositionStatus.PROCESSING_ERROR.name())) {
            return KIZPositionStatus.PROCESSING_ERROR;
        } else if (status.equals(KIZPositionStatus.UNDEFINED.name())) {
            return KIZPositionStatus.UNDEFINED;
        }
        return null;
    }

}

