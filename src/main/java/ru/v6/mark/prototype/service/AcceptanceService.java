package ru.v6.mark.prototype.service;


import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.v6.mark.xsd.SferaConnectorReject;
import ru.v6.mark.xsd.Файл;
import ru.v6.mark.prototype.domain.criteria.AcceptanceCriteria;
import ru.v6.mark.prototype.domain.criteria.DepartmentCriteria;
import ru.v6.mark.prototype.exception.ApplicationException;
import ru.v6.mark.prototype.service.converter.AcceptanceConverter;
import ru.v6.mark.prototype.web.context.InfologData;
import ru.v6.mark.prototype.web.context.RequestContext;
import ru.v6.mark.prototype.web.context.Response;
import ru.v6.mark.prototype.domain.constant.*;
import ru.v6.mark.prototype.domain.dao.*;
import ru.v6.mark.prototype.domain.entity.*;
import ru.v6.mark.prototype.service.util.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.lang.Boolean.FALSE;

@Service
public class AcceptanceService extends EntityCriteriaService<Acceptance, AcceptanceCriteria> {

    @Value("${config.GRAVITEE_API_KEY}")
    private String GRAVITEE_API_KEY;

    @Value("${config.INFOLOG_URL}")
    private String INFOLOG_URL;

    @Autowired
    AcceptanceDao acceptanceDao;

    @Autowired
    AcceptancePositionDao acceptancePositionDao;

    @Autowired
    GoodsDao goodsDao;

    @Autowired
    ProtocolService protocolService;

    @Autowired
    RequestContext requestContext;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    AcceptanceConverter acceptanceConverter;
    @Autowired
    KIZAggregationDao kizAggregationDao;

    @Autowired
    ClientService clientService;

    @Autowired
    ArticleService articleService;

    @Autowired
    KIZMarkDao kizMarkDao;

    @Autowired
    RestTemplate restTemplate;
    @Autowired
    FileService fileService;

    public static final Date LAUNCH_DATE = DateUtil.setDate(2020, Calendar.JULY, 1);
    @Override
    protected BaseCriteriaDao<Acceptance, AcceptanceCriteria> getPrimaryCriteriaDao() {
        return acceptanceDao;
    }

    public Acceptance completeAcceptance(Long id, Boolean confirmed, ResultError resultError) {
        protocolService.saveAsNew(new Protocol()
                .entity("ACCEPTANCE")
                .entityId(String.valueOf(id))
                .action(confirmed ? "Подтверждение завершения приемки" : "Завершение приемки")
                .username(requestContext.getUserName())
        );

        Acceptance acceptance = acceptanceDao.getById(id);
        if (!confirmed) {
            List<Integer> articles = acceptance.getPositions().parallelStream()
                    .filter(acceptancePosition -> acceptancePosition.getAcceptable() == null && acceptancePosition.getArticle() != null && acceptancePosition.getArticleItem() != null)
                    .map(AcceptancePosition::getArticle)
                    .collect(Collectors.toList());
            if (!articles.isEmpty()) {
                final String paramList = articles.stream().map(String::valueOf).collect(Collectors.joining(", "));
                throw ApplicationException.build("Позиции с артикулами " + paramList + " не проходили проверку и не будут приняты. Завершить сканирование (отмена - продолжить сканирование)?")
                        .status(Status.CONFIRMATION_REQUIRED);
            }
        }

        int acceptCount = 0, totalCount = 0;

        for (AcceptancePosition acceptancePosition : acceptance.getPositions()) {
            // учитываем только маркируемые
            if (acceptancePosition.getArticleItem() != null && acceptancePosition.getArticleItem().isMarked()) {
                totalCount++;
                if (acceptancePosition.getAcceptable() == null) {
                    acceptancePosition.setQuantityAccepted(0);
                    acceptancePosition.setAcceptable(FALSE);
                    resultError.setDescription(
                            resultError.getDescription() == null ?
                                    String.valueOf(acceptancePosition.getArticle()) :
                                    resultError.getDescription() + ", " + String.valueOf(acceptancePosition.getArticle())
                    );
                } else {
                    if (acceptancePosition.getAcceptable()) {
                        acceptCount++;
                    } else {
                        resultError.setDescription(
                                resultError.getDescription() == null ?
                                        String.valueOf(acceptancePosition.getArticle()) :
                                        resultError.getDescription() + ", " + String.valueOf(acceptancePosition.getArticle())
                        );
                    }
                }

                acceptancePosition.getMarks().forEach(kizMark -> {
                    if (KIZMarkStatus.SCANNED.equals(kizMark.getStatus())) {
                        kizMark.setStatus(KIZMarkStatus.VALIDATED);
                    }
                });
                kizMarkDao.saveAll(acceptancePosition.getMarks(), true);

            } else {
                acceptancePosition.setAcceptable(Boolean.TRUE);
            }
        }

        acceptance.setStatus(AcceptanceStatus.COMPLETED);
        acceptance.setResult(acceptCount == 0 ? AcceptanceResult.REJECT : (acceptCount == totalCount ? AcceptanceResult.FULL : AcceptanceResult.PARTIAL));
        acceptance = acceptanceDao.save(acceptance);

        if (resultError.getDescription() != null)
            resultError.setDescription("Позиции с артикулами " + resultError.getDescription() + " не проходили проверку и не будут приняты.");

        return acceptance;
    }

    public Response completeImportAcceptance(Long id, Boolean confirmed) {
        protocolService.saveAsNew(new Protocol()
                .entity("ACCEPTANCE")
                .entityId(String.valueOf(id))
                .action(confirmed ? "Подтверждение завершения приемки" : "Завершение приемки")
                .username(requestContext.getUserName())
        );

        Acceptance acceptance = acceptanceDao.getById(id);
        if (!confirmed) {
            List<Integer> articles = acceptance.getPositions().parallelStream()
                    .filter(acceptancePosition -> acceptancePosition.getAcceptable() == null && acceptancePosition.getArticle() != null && acceptancePosition.getArticleItem() != null)
                    .map(AcceptancePosition::getArticle)
                    .collect(Collectors.toList());
            if (!articles.isEmpty()) {
                final String paramList = articles.stream().map(String::valueOf).collect(Collectors.joining(", "));

                throw ApplicationException.build("Позиции с артикулами " + paramList + " не проходили проверку и не будут приняты. Завершить сканирование (отмена - продолжить сканирование)?")
                        .status(Status.CONFIRMATION_REQUIRED);
            }
        }

        int acceptCount = 0;
        boolean partial = false;
        for (AcceptancePosition acceptancePosition : acceptance.getPositions()) {
            if (acceptancePosition.getAcceptable() == null) {
                if (acceptancePosition.getQuantityAccepted() != null && acceptancePosition.getQuantityAccepted() != 0) {
                    acceptancePosition.setAcceptable(true);
                    partial = true;
                } else {
                    acceptancePosition.setAcceptable(false);
                }
            } else {
                if (acceptancePosition.getAcceptable()) {
                    acceptCount++;
                }
            }

            acceptancePosition.getMarks().forEach(kizMark -> {
                if (KIZMarkStatus.SCANNED.equals(kizMark.getStatus())) {
                    kizMark.setStatus(KIZMarkStatus.VALIDATED);
                }
            });
            kizMarkDao.saveAll(acceptancePosition.getMarks(), true);
        }
        ;
        acceptance.setStatus(AcceptanceStatus.COMPLETED);
        acceptance.setResult(!partial && acceptCount == 0 ? AcceptanceResult.REJECT : (acceptCount == acceptance.getPositions().size() ? AcceptanceResult.FULL : AcceptanceResult.PARTIAL));

        acceptanceDao.save(acceptance);

        return new Response(Status.OK);
    }

    public AcceptancePosition checkByAcceptanceEanMark(Long id, String ean, String mark) {
        protocolService.saveAsNew(
                new Protocol()
                        .entity("ACCEPTANCE")
                        .entityId(String.valueOf(id))
                        .action(mark == null ? "Отсканирован ШК: " + ean : "Отсканирован код: " + mark)
                        .username(requestContext.getUserName())
        );
        Acceptance acceptance = acceptanceDao.getById(id);

        AcceptancePosition result = null;
        if (acceptance != null && !AcceptanceStatus.PROHIBITED.equals(acceptance.getStatus()) && !AcceptanceStatus.COMPLETED.equals(acceptance.getStatus())) {
            Goods goods = goodsDao.getById(goodsService.completeEan(ean));

            if (goods != null && goods.getArticle() != null) {

                result = acceptance.getPositions().stream().filter(acceptancePosition -> acceptancePosition.getArticle() != null && goods.getArticle().equals(acceptancePosition.getArticle())).findFirst().orElse(null);

                if (result != null) {
                    if (result.getAcceptable() == null || result.getAcceptable()) {
                        if (result.getQuantitySupplied() == null || (result.getQuantityAccepted() != null && result.getQuantitySupplied().equals(result.getQuantityAccepted()))) {
                            throw ApplicationException
                                    .build("Позиция с артикулом {0} уже прошла приемку.")
                                    .parameters(goods.getArticle())
                                    .status(Status.OK)
                                    .identity(-1L);
                        } else {
                            if (mark != null) {
                                final int delimiterPos = mark.indexOf(29);

                                final String identCode = delimiterPos != -1 ? mark.substring(0, delimiterPos) : mark;
                                if (result.getMarks().stream().filter(kizMark -> identCode.equals(delimiterPos != -1 && kizMark.getMark().length() >= delimiterPos ? kizMark.getMark().substring(0, delimiterPos) : kizMark.getMark())).findFirst().orElse(null) != null) {
                                    result.setQuantityAccepted(result.getQuantitySupplied());
                                    result.setAcceptable(true);
                                    result = acceptancePositionDao.save(result);
                                } else if (result.getAggregations().stream().filter(kizAggregation -> identCode.equals(kizAggregation.getSscc())).findFirst().orElse(null) != null) {
                                    result.setQuantityAccepted(result.getQuantitySupplied());
                                    result.setAcceptable(true);
                                    result = acceptancePositionDao.save(result);
                                } else {
                                    result.setQuantityAccepted(0);
                                    result.setAcceptable(false);
                                    result = acceptancePositionDao.save(result);
                                    throw ApplicationException
                                            .build("Позиция с артикулом {0} не содержит код марки/агрегации {1} и запрещена к приемке.")
                                            .parameters(goods.getArticle(), mark)
                                            .identity(result.getId());
                                }
                            }
                        }
                    } else {
                        throw ApplicationException
                                .build("Позиция с артикулом {0} не прошла проверку в ЦРПТ и запрещена к приемке.")
                                .parameters(goods.getArticle())
                                .identity(result.getId());
                    }
                } else
                    throw ApplicationException
                            .build("По штрихкоду определен артикул {0}, который отсутствует в данной приемке. Приемка с ШК {1} запрещена.")
                            .parameters(goods.getArticle(), ean)
                            .identity(-1L);
            } else {
                throw ApplicationException
                        .build("Невозможно определить артикул по штрихкоду {0}. Приемка с этим ШК запрещена.")
                        .parameters(ean)
                        .identity(-1L);
            }
        } else
            throw ApplicationException
                    .build("Приемка с ID={0} не найдена или в текущем статусе сканирование запрещено.")
                    .parameters(id)
                    .identity(-1L);

        return result;
    }

    @LogPerformance
    public AcceptancePosition checkByAcceptanceImportMark(Long id, String mark) {
        protocolService.saveAsNew(
                new Protocol()
                        .entity("ACCEPTANCE")
                        .entityId(String.valueOf(id))
                        .action("Отсканирован код: " + mark)
                        .username(requestContext.getUserName())
        );
        Acceptance acceptance = acceptanceDao.getById(id);

        AcceptancePosition result = null;
        if (acceptance != null) {

            if (mark != null) {
                if (mark.length() >= 31) {
                    final String identCode = mark.substring(0, 31);

                    KIZMark kizMark = acceptance
                            .getPositions()
                            .stream()
                            .flatMap(acceptancePosition -> acceptancePosition.getMarks().parallelStream()).filter(kM -> identCode.equals(kM.getMark().length() >= 31 ? kM.getMark().substring(0, 31) : kM.getMark())).findFirst().orElse(null);

                    if (kizMark != null) {
                        result = kizMark.getAcceptancePosition();
                        if (result.getAcceptable() == null || result.getAcceptable()) {
                            if (result.getQuantitySupplied() == null || (result.getQuantityAccepted() != null && result.getQuantitySupplied().equals(result.getQuantityAccepted()))) {
                                throw ApplicationException
                                        .build("Позиция с артикулом {0} уже прошла приемку.")
                                        .parameters(result.getArticle())
                                        .status(Status.OK)
                                        .identity(-1L);
                            } else {
                                if (!KIZMarkStatus.SCANNED.equals(kizMark.getStatus())) {
                                    kizMark.setStatus(KIZMarkStatus.SCANNED);
                                    kizMarkDao.save(kizMark);
                                    result.setQuantityAccepted(result.getQuantityAccepted() == null ? 1 : result.getQuantityAccepted() + 1);
                                    if (result.getQuantityAccepted().equals(result.getQuantitySupplied()))
                                        result.setAcceptable(true);
                                    result = acceptancePositionDao.save(result);
                                } else {
                                    throw ApplicationException
                                            .build("Марка {0} уже прошла приемку.")
                                            .parameters(identCode)
                                            .status(Status.OK)
                                            .identity(-1L);
                                }
                            }
                        } else {
                            throw ApplicationException
                                    .build("Позиция с артикулом {0} не прошла проверку в ЦРПТ и запрещена к приемке.")
                                    .parameters(result.getArticle())
                                    .identity(result.getId());
                        }
                    } else {
                        throw ApplicationException
                                .build("Приемка контейнера {0} по заказу {1} не содержит код марки {2}. Товар с этой маркой не может быть принят.")
                                .parameters(acceptance.getContainer(), acceptance.getOrder(), mark)
                                .identity(-1L);
                    }
                } else {
                    final String identCode = mark;

                    List<KIZMark> kizMarks = acceptance
                            .getPositions()
                            .stream()
                            .flatMap(acceptancePosition -> acceptancePosition.getMarks().parallelStream()).filter(kA -> identCode.equals(kA.getSscc())).collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(kizMarks)) {
                        result = kizMarks.get(0).getAcceptancePosition();
                        if (result.getAcceptable() == null || result.getAcceptable()) {
                            if (result.getQuantitySupplied() == null || (result.getQuantityAccepted() != null && result.getQuantitySupplied().equals(result.getQuantityAccepted()))) {
                                throw ApplicationException
                                        .build("Позиция с артикулом {0} уже прошла приемку.")
                                        .parameters(result.getArticle())
                                        .status(Status.OK)
                                        .identity(-1L);
                            } else {
                                List<KIZMark> notScanned = kizMarks.parallelStream().filter(kM -> !KIZMarkStatus.SCANNED.equals(kM.getStatus())).collect(Collectors.toList());
                                if (!CollectionUtils.isEmpty(notScanned)) {
                                    notScanned.parallelStream().forEach(kM -> kM.setStatus(KIZMarkStatus.SCANNED));
                                    result.setQuantityAccepted(result.getQuantityAccepted() == null ? notScanned.size() : result.getQuantityAccepted() + notScanned.size());

                                    kizMarkDao.saveAll(notScanned, true);
                                    if (result.getQuantityAccepted().equals(result.getQuantitySupplied()))
                                        result.setAcceptable(true);
                                    result = acceptancePositionDao.save(result);
                                } else {
                                    throw ApplicationException
                                            .build("Упаковка товара с кодом агрегации {0} уже прошла приемку.")
                                            .parameters(identCode)
                                            .status(Status.OK)
                                            .identity(-1L);
                                }
                            }
                        } else {
                            throw ApplicationException
                                    .build("Позиция с артикулом {0} не прошла проверку в ЦРПТ и запрещена к приемке.")
                                    .parameters(result.getArticle())
                                    .identity(result.getId());
                        }
                    } else {
                        throw ApplicationException
                                .build("Приемка контейнера {0} по заказу {1} не содержит код агрегации {2}. Товар с этим кодом агрегации не может быть принят.")
                                .parameters(acceptance.getContainer(), acceptance.getOrder(), mark)
                                .identity(-1L);
                    }
                }
            }
        } else
            throw ApplicationException
                    .build("Ошибка получения приемки с ID {0}")
                    .parameters(id)
                    .identity(-1L);
        return result;
    }

    @Deprecated
    public Acceptance save(Файл файл, String fileName, Map<String, Vendor> vendors, ResultError resultError) {
        Acceptance acceptance = new Acceptance();
        acceptance.setFileName(fileName);
        acceptanceConverter.convert(файл, acceptance, vendors);

        boolean accepted = removeDuplicates(acceptance);

        if (!accepted && validationCheck(acceptance, resultError) == null) {
            logger.error("Acceptance id: {}. Проверка не пройдена. Ошибки: {}", acceptance.getId(), resultError.getDescription());
            return acceptance;
        }
        return acceptance;
    }

    /**
     * @param acceptance
     * @return true  - приемка производилась, false - приемка не проводилась
     */
    public boolean removeDuplicates(Acceptance acceptance) {
        boolean result = false;
        AcceptanceCriteria criteria = new AcceptanceCriteria();
        criteria.setGlnConsignee(acceptance.getGlnConsignee());
        criteria.setVendorInn(acceptance.getVendorInn());
        criteria.setNumber(acceptance.getNumber());
        criteria.setDate(acceptance.getDate());

        List<Acceptance> duplicates = findByCriteria(criteria);
        if (!CollectionUtils.isEmpty(duplicates)) {
            for (Acceptance duplicate : duplicates) {
                if (duplicate.getDeleted() == null || duplicate.getDeleted().equals(FALSE)) {
                    AtomicBoolean accepted = new AtomicBoolean(FALSE);
                    duplicate.getPositions().parallelStream().forEach(acceptancePosition -> {
                        accepted.set(accepted.get() || (acceptancePosition.getQuantityAccepted() != null));
                    });
                    result = result || accepted.get();
                    if (!result)
                        duplicate.setDeleted(true);

                }
            }
            acceptanceDao.saveAll(duplicates, true);
        }
        return result;
    }

    @HasProtocol(action = "Загрузка УПД", linkPrefix = "service/fileUPD")
    public Acceptance validationCheck(Acceptance converted, ResultError resultError) {
        Protocol validationProtocol = new Protocol();
        validationProtocol.setEntity("ACCEPTANCE");

        final AtomicBoolean isCheck = new AtomicBoolean(true);
        StringBuffer errors = new StringBuffer();

        Department department = null;

        DepartmentCriteria criteria = new DepartmentCriteria();
        criteria.setGln(converted.getGlnConsignee());
        List<Department> departments = departmentService.findByCriteria(criteria);
        if (departments != null && departments.size() > 0) {
            department = departments.get(0);

        }
        final String keyAlias = department != null ? department.getKeyAlias() : null;

        Acceptance acceptance = acceptanceDao.save(converted);

        for (AcceptancePosition position : converted.getPositions()) {

            // 1. Проверка уникальности кодов агрегации в УПД
            // Проверка перенесена в AcceptanceConverter, т.к. sscc-код уникальны (при наличии их уже в базе)

            // 2. Проверять уникальность кодов марок/групповых кодов в УПД
            if (!checkUniqueMark(position)) {
                acceptance.setStatus(AcceptanceStatus.PENDING_ERROR);
                position.setStatus(AcceptancePositionStatus.NON_UNIQUE_CODE);
                position.setQuantityAccepted(0);
                position.setAcceptable(false);
                acceptance.setStatus(AcceptanceStatus.PROHIBITED);
                errors.append(" ").append(position.getArticle()).append(" - ").append(AcceptancePositionStatus.NON_UNIQUE_CODE.getDescription());
                isCheck.set(false);
                break;
            }

            Queue<KIZAggregation> actualAggregations = new ConcurrentLinkedQueue<>();
            Queue<KIZMark> actualMarks = new ConcurrentLinkedQueue<>();

            //3. По каждому коду марки, указанных в УПД, отправлять запрос на проверку
            if (position.getMarks() != null) {
                position.getMarks().parallelStream().forEach(mark -> {
                    KIZMark kizMark = kizMarkDao.findByMark(mark.getMark());
                    if (kizMark == null)
                        kizMark = mark;

                    kizMark.setAcceptancePosition(position);

                    JSONObject jsonObject = validateCode(keyAlias, mark.getMark());

                    String parent = JSONUtil.getValue("parent", jsonObject);
                    if (parent != null && !parent.isEmpty()) {
                        if (parent.length() != 18) {
                            // todo
/*
                                KIZMark kmParent = kizMarkDao.findByMark(parent);
                                if (kmParent == null) {
                                    kmParent = new KIZMark();
                                    kmParent.setMark(parent);
                                    kmParent.setAcceptancePosition(position);
                                    kmParent = kizMarkDao.save(kmParent);
                                }
                                kizMark.setParent(kmParent);
                                if (!actualMarks.contains(kmParent))
                                    actualMarks.add(kmParent);
*/
                        } else {
                            kizMark.setSscc(parent);
                            KIZAggregation kizAggregation = new KIZAggregation();
                            kizAggregation.setSscc(parent);
                            if (!actualAggregations.contains(kizAggregation)) {
                                kizAggregation = kizAggregationDao.getById(parent);
                                if (kizAggregation == null) {
                                    kizAggregation = new KIZAggregation();
                                }

                                kizAggregation.setSscc(parent);
                                kizAggregation.setAcceptancePosition(position);

                                actualAggregations.add(kizAggregation);
                                //kizAggregationDao.save(kizAggregation);
                            }
                        }
                    }

                    String producedDate = JSONUtil.getValue("producedDate", jsonObject);  // "2020-04-13T20:42:30.070Z"
                    if (producedDate == null || producedDate.isEmpty() || !LAUNCH_DATE.after(DateUtil.parseDate(producedDate, new SimpleDateFormat(DateUtil.PRODUCED_DATE_FORMAT)))) {
                        //logger.info("Acceptance order: {}, Mark: {}, producedDate: {} ", acceptance.getOrder(), mark.getMark(), producedDate);

                        String inn = JSONUtil.getValue("ownerInn", jsonObject);
                        if (inn == null || !inn.equals(acceptance.getVendorInn())) {
                            setErrorStatus(AcceptancePositionStatus.INCORRECT_OWNER, acceptance, position, mark.getMark(), errors);
                            logger.info("Acceptance order: {}, Mark: {}, INN does not match: {} ", acceptance.getOrder(), mark.getMark(), inn);
                            isCheck.set(false);
                        }
                        String status = JSONUtil.getValue("status", jsonObject);
                        if (status == null || !status.equals("INTRODUCED")) {
                            setErrorStatus(AcceptancePositionStatus.WRONG_CODE, acceptance, position, mark.getMark(), errors);
                            logger.info("Acceptance order: {}, Mark: {}, Invalid status code: {} ", acceptance.getOrder(), mark.getMark(), status);
                            isCheck.set(false);
                        }
                        String statusEx = JSONUtil.getValue("statusEx", jsonObject);
                        if (statusEx != null && !statusEx.trim().isEmpty()) {
                            setErrorStatus(AcceptancePositionStatus.WRONG_CODE, acceptance, position, mark.getMark(), errors);
                            logger.info("Acceptance order: {}, Mark: {}, Invalid statusEx code: statusEx = {}", acceptance.getOrder(), mark.getMark(), statusEx);
                            isCheck.set(false);
                        }
                    }

                    // для кодов упаковок
                    JSONArray children = JSONUtil.getArray("child", jsonObject.toString());
                    if (children != null && children.length() > 0) {
                        for (int i = 0; i < children.length(); i++) {
                            KIZMark child = new KIZMark();
                            child.setMark(children.getString(i));
                            if (!actualMarks.contains(child)) {
                                child = kizMarkDao.findByMark(children.getString(i));
                                if (child == null)
                                    child = new KIZMark();
                                if (position.getArticleItem() != null && MarkType.TYPE7.equals(position.getArticleItem().getMarkType())) {
                                    child.setMark(children.getString(i).replaceAll("[()]", ""));
                                } else
                                    child.setMark(children.getString(i));
                                child.setAcceptancePosition(position);
                                child.setParent(kizMark);

                                actualMarks.add(child);
                                //kizMarkDao.save(child);
                            }
                        }
                    }
                    if (!actualMarks.contains(kizMark)) {
                        actualMarks.add(kizMark);
                        //kizMarkDao.save(kizMark);
                    }
                });

            }

            //3. По каждому коду агрегации, указанных в УПД, отправлять запрос на получение марок из него в ЦРПТ
            if (position.getAggregations() != null) {
                position.getAggregations().parallelStream().forEach(aggregation -> {

                    if (aggregation.getSscc().length() != 18) {
                        logger.info("Acceptance order: {}, incorrect SSCC: {} ", acceptance.getOrder(), aggregation.getSscc());
                        resultError.setDescription("Некорректный код упаковки: '" + aggregation.getSscc() + "'");
                        isCheck.set(false);
                    } else {
                        KIZAggregation kizAggregation = kizAggregationDao.getById(aggregation.getSscc());
                        if (kizAggregation == null)
                            kizAggregation = aggregation;

                        kizAggregation.setAcceptancePosition(position);

                        JSONObject jsonObject = validateCode(keyAlias, aggregation.getSscc());

                        String producedDate = JSONUtil.getValue("producedDate", jsonObject);  // "2020-04-13T20:42:30.070Z"
                        if (producedDate == null || producedDate.isEmpty() || !LAUNCH_DATE.after(DateUtil.parseDate(producedDate, new SimpleDateFormat(DateUtil.PRODUCED_DATE_FORMAT)))) {
                            // logger.info("Acceptance order: {}, SSCC: {}, producedDate: {} ", acceptance.getOrder(), aggregation.getSscc(), producedDate);

                            String inn = JSONUtil.getValue("ownerInn", jsonObject);
                            if (inn == null || !inn.equals(acceptance.getVendorInn())) {
                                setErrorStatus(AcceptancePositionStatus.INCORRECT_OWNER, acceptance, position, aggregation.getSscc(), errors);
                                logger.info("Acceptance order: {}, SSCC: {}, INN does not match: {} ", acceptance.getOrder(), aggregation.getSscc(), inn);
                                isCheck.set(false);
                            }

                            String status = JSONUtil.getValue("status", jsonObject);
                            if (status == null || !status.equals("INTRODUCED")) {
                                setErrorStatus(AcceptancePositionStatus.WRONG_CODE, acceptance, position, aggregation.getSscc(), errors);
                                logger.info("Acceptance order: {}, SSCC: {}, Invalid status code: {} ", acceptance.getOrder(), aggregation.getSscc(), status);
                                isCheck.set(false);
                            }
                            String statusEx = JSONUtil.getValue("statusEx", jsonObject);
                            if (statusEx != null && !statusEx.trim().isEmpty()) {
                                setErrorStatus(AcceptancePositionStatus.WRONG_CODE, acceptance, position, aggregation.getSscc(), errors);
                                logger.info("Acceptance order: {}, SSCC: {}, Invalid statusEx code: statusEx = {}", acceptance.getOrder(), aggregation.getSscc(), statusEx);
                                isCheck.set(false);
                            }
                        }

                        JSONArray children = JSONUtil.getArray("child", jsonObject.toString());
                        if (children != null && children.length() > 0) {
                            for (int i = 0; i < children.length(); i++) {
                                KIZMark mark = new KIZMark();
                                if (position.getArticleItem() != null && MarkType.TYPE7.equals(position.getArticleItem().getMarkType())) {
                                    mark.setMark(children.getString(i).replaceAll("[()]", ""));
                                } else
                                    mark.setMark(children.getString(i));

                                if (!actualMarks.contains(mark)) {
                                    if (kizAggregation.getMarks() != null && kizAggregation.getMarks().contains(mark)) {
                                        mark = kizAggregation.getMarks().get(kizAggregation.getMarks().indexOf(mark));
                                    } else {
                                        mark = kizMarkDao.findByMark(children.getString(i));
                                    }
                                    if (mark == null) {
                                        mark = new KIZMark();
                                    }

                                    mark.setMark(children.getString(i));
                                    mark.setAcceptancePosition(position);
                                    mark.setSscc(aggregation.getSscc());
                                    actualMarks.add(mark);
                                    //kizMarkDao.save(mark);
                                }
                            }
                        }

                        if (!actualAggregations.contains(kizAggregation)) {
                            actualAggregations.add(kizAggregation);
                            //kizAggregationDao.save(kizAggregation);
                        }
                    }
                });
            }

            if (position.getArticle() != null) {
                Article article = articleService.getById(position.getArticle());
                if (article != null && article.getMarkType().isMarking()) {
                    if (actualMarks == null || actualMarks.size() == 0) {
                        acceptance.setStatus(AcceptanceStatus.PENDING_WARN);
                        position.setStatus(AcceptancePositionStatus.NOT_ENOUGH_MARK);
                        position.setQuantityAccepted(0);
                        position.setAcceptable(false);
                        errors.append(", ").append(position.getArticle()).append(" - ").append(AcceptancePositionStatus.NOT_ENOUGH_MARK.getDescription());
                        isCheck.set(false);
                    }
                } else {
                    logger.error("Acceptance position id: {}, article not found: {}", position.getId(), position.getArticle());
                }
            }

            kizAggregationDao.saveAll(actualAggregations, true);
            kizMarkDao.saveAll(actualMarks, true);
        }

        if (department != null && DepartmentType.WAREHOUSE.equals(department.getDepartmentType())) {
            try {
                fillInfologQuantities(acceptance, department);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        validationProtocol.setEntityId(String.valueOf(acceptance.getId()));

        if (!isCheck.get()) {
            resultError.setDescription(errors.toString().replaceFirst(", ", ""));
            validationProtocol.setExternalLink("");
            validationProtocol.setAction("Валидация УПД с ошибками");
            validationProtocol = protocolService.saveAsNew(validationProtocol);
            protocolService.saveAsFile(validationProtocol.getId(), resultError.getDescription().replaceAll(", ", "\n"));
        } else {
            validationProtocol.setAction("Валидация УПД без ошибок");
            validationProtocol = protocolService.saveAsNew(validationProtocol);
        }

        return acceptanceDao.save(acceptance);
    }

    private JSONObject validateCode(String keyAlias, String code) {
        String result = clientService.checkCode(getToken(keyAlias), code);
        JSONObject jsonObject = null;
        String errorCode = null;
        try {
            if (result != null)
                jsonObject = (new JSONArray(result)).getJSONObject(0);
            if (jsonObject != null)
                errorCode = JSONUtil.getValue("errorCode", jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        while (jsonObject == null || ((errorCode != null) && Integer.parseInt(errorCode) >= 500)) {
            SleepUtil.sleep(SleepUtil.CRPT_QUERY_INTERVAL);
            result = clientService.checkCode(getToken(keyAlias), code);
            if (result != null) {
                try {
                    jsonObject = (new JSONArray(result)).getJSONObject(0);
                    if (jsonObject != null)
                        errorCode = JSONUtil.getValue("errorCode", jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }

        return !jsonObject.has("cisInfo") ? null : jsonObject.getJSONObject("cisInfo");
    }

    public Acceptance importInfolog(Long id) {
        Acceptance acceptance = acceptanceDao.getById(id);
        try {
            fillInfologQuantities(acceptance, acceptance.getConsignee());
            acceptanceDao.save(acceptance);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return acceptance;
    }

    private boolean fillInfologQuantities(Acceptance acceptance, Department department) {
        boolean hasData = false;
        Map<Integer, AcceptancePosition> positionMap = new HashMap<>();
        acceptance.getPositions().parallelStream().forEach(acceptancePosition -> {
            positionMap.put(acceptancePosition.getArticle(), acceptancePosition);
        });

        hasData = fetchInfologQuantitiesFor(positionMap, department.getCode(), acceptance.getOrder());
        if (department.getSaasCodes() != null) {
            for (String code : department.getSaasCodes()) {
                hasData |= fetchInfologQuantitiesFor(positionMap, code, acceptance.getOrder());
            }
        }
        return hasData;
    }

    private boolean fetchInfologQuantitiesFor(Map<Integer, AcceptancePosition> positionMap, String infologCode, String order) {
        boolean hasData = false;

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("X-Gravitee-Api-Key", GRAVITEE_API_KEY);

        HttpEntity<String> request = new HttpEntity<String>(headers);

        String orderStr = order.length() > 6 ? order.substring(order.length() - 6) : order;

        String url = INFOLOG_URL + infologCode + "/" + orderStr;
        logger.info("Infolog request send: " + url);

        try {
            ResponseEntity<InfologData> result = restTemplate.exchange(url, HttpMethod.GET, request, InfologData.class);
            logger.info("Infolog response: " + result.toString());

            InfologData data = result.getBody();
            if (data != null && data.getData() != null && !data.getData().isEmpty()) {
                hasData = true;
                data.getData().parallelStream().forEach(item -> {
                    int idx = item.getArticle().indexOf('-');
                    Integer article = Integer.parseInt(idx != -1 ? item.getArticle().substring(0, idx) : item.getArticle());
                    AcceptancePosition position = positionMap.get(article);
                    if (position != null) {
                        position.setQuantityInfologOrdered(Integer.parseInt(item.getOrdered()));
                        position.setQuantityInfologAccepted(Integer.parseInt(item.getAccepted()));
                        position.setPcbInfolog(Integer.parseInt(item.getPcb()));
                    }
                });
            }
        } catch (RestClientException e) {
            logger.error(e.getMessage());
        }
        return hasData;
    }

    private void setErrorStatus(AcceptancePositionStatus status, Acceptance acceptance, AcceptancePosition position, String code, StringBuffer errors) {
        acceptance.setStatus(AcceptanceStatus.PENDING_ERROR);
        position.setStatus(status);
        position.setQuantityAccepted(0);
        position.setAcceptable(false);
        acceptance.setStatus(AcceptanceStatus.PROHIBITED);
        errors
                .append(", ")
                .append(position.getArticle()).append(": ")
                .append(status.getDescription())
                .append(" '")
                .append(code)
                .append("'");
    }

    private boolean checkUniqueMark(AcceptancePosition position) {
        if (position.getMarks() != null && position.getMarks().size() > 0) {
            Set<String> setValue = position.getMarks().stream().map(s -> s.getMark()).collect(Collectors.toSet());
            List<String> listValue = position.getMarks().stream().map(s -> s.getMark()).collect(Collectors.toList());
            if (setValue.size() < listValue.size()) {
                return false;
            }
        }
        return true;
    }

    @HasProtocol(action = "Сформирован ответ по УПД", linkPrefix = "service/fileAPERAK", identifier = ProtocolIdentifier.ENTITY)
    public void generateXml(Acceptance acceptance, AperakStatus status, ResultError resultError) {
        String aperakFileName =
                "APERAK_"
                        + status + "_edi-provider_"
                        + acceptance.getFileName()
                        + "_" + DateUtil.getStringByFormat(new Date(), new SimpleDateFormat("yyyyMMdd"))
                        + "_" + DateUtil.getStringByFormat(new Date(), new SimpleDateFormat("hh"))
                        + DateUtil.getStringByFormat(new Date(), new SimpleDateFormat("mm"))
                        + DateUtil.getStringByFormat(new Date(), new SimpleDateFormat("ss"))
                        + ".xml";

        SferaConnectorReject sferaConnectorReject = new SferaConnectorReject();
        sferaConnectorReject.setFilename(acceptance.getFileName());
        sferaConnectorReject.setDocumentNum(acceptance.getNumber());
        sferaConnectorReject.setDocumentDate(DateUtil.getStringByFormat(acceptance.getDate(), new SimpleDateFormat("yyyyMMdd")));
        sferaConnectorReject.setSupplierINN(acceptance.getVendorInn());
        DepartmentCriteria departmentCriteria = new DepartmentCriteria();
        departmentCriteria.setGln(acceptance.getGlnConsignee());
        List<Department> departments = departmentService.findByCriteria(departmentCriteria);
        if (departments != null && departments.size() > 0) {
            sferaConnectorReject.setBuyerINN(departments.get(0).getOrganization().getInn());
        } else {
            logger.error("Error GenerateXml: not found consignee inn, Gln Consignee: ", acceptance.getGlnConsignee());
        }
        sferaConnectorReject.setStatus(status.name());
        if (StringUtil.hasLength(resultError.getDescription()) && !status.equals(AperakStatus.OK)) {
            sferaConnectorReject.setComment(resultError.getDescription());
        }
        sferaConnectorReject.setDate(DateUtil.getStringByFormat(new Date(), new SimpleDateFormat("yyyyMMdd")));

        fileService.generateAperak(sferaConnectorReject, aperakFileName);
    }

    public byte[] updList(StringBuilder fileName) {
        fileName.append("Список УПД.xlsx");

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook();
            final XSSFSheet sheet = workbook.createSheet("Список УПД");

            XSSFCreationHelper createHelper = workbook.getCreationHelper();

            final CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd.MM.yyyy")); // default date formant

            XSSFRow header = sheet.createRow(0);
            header.createCell(0).setCellValue("Номер УПД");
            header.createCell(1).setCellValue("Дата УПД");
            header.createCell(2).setCellValue("Номер заказа");
            header.createCell(3).setCellValue("Получатель");
            header.createCell(4).setCellValue("Статус");
            header.createCell(5).setCellValue("Результат приемки");

            AcceptanceCriteria criteria = new AcceptanceCriteria();
            criteria.setType(AcceptanceType.LOCAL);
            criteria.setSortDir("desc");
            criteria.setSortField("date");
            List<Acceptance> acceptanceList = acceptanceDao.findByCriteria(criteria);

            acceptanceList.forEach(acceptance -> {
                XSSFRow row = sheet.createRow(sheet.getLastRowNum() + 1);
                if (acceptance.getNumber() != null)
                    row.createCell(0).setCellValue(acceptance.getNumber());

                if (acceptance.getDate() != null) {
                    XSSFCell cell = row.createCell(1);
                    cell.setCellValue(acceptance.getDate());
                    cell.setCellStyle(cellStyle);
                }

                if (acceptance.getOrder() != null)
                    row.createCell(2).setCellValue(acceptance.getOrder());

                if (acceptance.getConsignee() != null)
                    row.createCell(3).setCellValue("[" + acceptance.getConsignee().getCode() + "] " + acceptance.getConsignee().getName());

                if (acceptance.getStatus() != null)
                    row.createCell(4).setCellValue(acceptance.getStatus().getDescription());

                if (acceptance.getResult() != null)
                    row.createCell(5).setCellValue(acceptance.getResult().getDescription());
            });
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);
            sheet.autoSizeColumn(3);
            sheet.autoSizeColumn(4);
            sheet.autoSizeColumn(5);

            workbook.write(stream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (workbook != null) {
                    workbook.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return stream.toByteArray();
    }

}
