package ru.v6.mark.prototype.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import ru.v6.mark.prototype.domain.constant.*;
import ru.v6.mark.prototype.domain.criteria.ShipmentCriteria;
import ru.v6.mark.prototype.domain.dao.*;
import ru.v6.mark.prototype.domain.entity.*;
import ru.v6.mark.prototype.exception.ApplicationException;
import ru.v6.mark.prototype.service.converter.AcceptanceConverter;
import ru.v6.mark.prototype.service.util.DateUtil;
import ru.v6.mark.prototype.service.util.ResultError;
import ru.v6.mark.prototype.web.context.RequestContext;
import ru.v6.mark.prototype.web.context.Response;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Boolean.FALSE;

@Service
public class ShipmentService extends EntityCriteriaService<Shipment, ShipmentCriteria> {

    @Value("${config.GRAVITEE_API_KEY}")
    private String GRAVITEE_API_KEY;

    @Value("${config.INFOLOG_URL}")
    private String INFOLOG_URL;

    @Autowired
    ShipmentDao shipmentDao;

    @Autowired
    DepartmentDao departmentDao;

    @Autowired
    VendorDao vendorDao;

    @Autowired
    AcceptanceDao acceptanceDao;

    @Autowired
    ShipmentPositionDao shipmentPositionDao;

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
    protected BaseCriteriaDao<Shipment, ShipmentCriteria> getPrimaryCriteriaDao() {
        return shipmentDao;
    }

    public Shipment completeShipment(Long id, Boolean confirmed, ResultError resultError) {
        protocolService.saveAsNew(new Protocol()
                .entity("SHIPMENT")
                .entityId(String.valueOf(id))
                .action(confirmed ? "Подтверждение завершения отгрузки" : "Завершение отгрузки")
                .username(requestContext.getUserName())
        );

        Shipment shipment = shipmentDao.getById(id);
        if (!confirmed) {
            List<Integer> articles = shipment.getPositions().parallelStream()
                    .filter(shipmentPosition -> shipmentPosition.getConfirmed() == null && shipmentPosition.getArticle() != null && shipmentPosition.getArticleItem() != null)
                    .map(ShipmentPosition::getArticle)
                    .collect(Collectors.toList());
            if (!articles.isEmpty()) {
                final String paramList = articles.stream().map(String::valueOf).collect(Collectors.joining(", "));
                throw ApplicationException.build("Позиции с артикулами " + paramList + " не проходили проверку и не будут отгружены. Завершить сканирование (отмена - продолжить сканирование)?")
                        .status(Status.CONFIRMATION_REQUIRED);
            }
        }

        int acceptCount = 0, totalCount = 0;

        for (ShipmentPosition shipmentPosition : shipment.getPositions()) {
            // учитываем только маркируемые
            if (shipmentPosition.getArticleItem() != null && shipmentPosition.getArticleItem().isMarked()) {
                totalCount++;
                if (shipmentPosition.getConfirmed() == null) {
                    shipmentPosition.setQuantityShipped(0);
                    shipmentPosition.setConfirmed(FALSE);
                    resultError.setDescription(
                            resultError.getDescription() == null ?
                                    String.valueOf(shipmentPosition.getArticle()) :
                                    resultError.getDescription() + ", " + String.valueOf(shipmentPosition.getArticle())
                    );
                } else {
                    if (shipmentPosition.getConfirmed()) {
                        acceptCount++;
                    } else {
                        resultError.setDescription(
                                resultError.getDescription() == null ?
                                        String.valueOf(shipmentPosition.getArticle()) :
                                        resultError.getDescription() + ", " + String.valueOf(shipmentPosition.getArticle())
                        );
                    }
                }
            } else {
                shipmentPosition.setConfirmed(Boolean.TRUE);
            }
        }

        shipment.setStatus(ShipmentStatus.COMPLETED);
        shipment.setResult(acceptCount == 0 ? AcceptanceResult.REJECT : (acceptCount == totalCount ? AcceptanceResult.FULL : AcceptanceResult.PARTIAL));
        shipment = shipmentDao.save(shipment);

        if (resultError.getDescription() != null)
            resultError.setDescription("Позиции с артикулами " + resultError.getDescription() + " не проходили проверку и не будут отгружены.");

        return shipment;
    }

    public Response completeImportAcceptance(Long id, Boolean confirmed) {
        protocolService.saveAsNew(new Protocol()
                .entity("SHIPMENT")
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
        }
        ;
        acceptance.setStatus(AcceptanceStatus.COMPLETED);
        acceptance.setResult(!partial && acceptCount == 0 ? AcceptanceResult.REJECT : (acceptCount == acceptance.getPositions().size() ? AcceptanceResult.FULL : AcceptanceResult.PARTIAL));

        acceptanceDao.save(acceptance);

        return new Response(Status.OK);
    }

    public ShipmentPosition checkByAcceptanceEanMark(Long id, String ean, String mark) {
        protocolService.saveAsNew(
                new Protocol()
                        .entity("SHIPMENT")
                        .entityId(String.valueOf(id))
                        .action(mark == null ? "Отсканирован ШК: " + ean : "Отсканирован код: " + mark)
                        .username(requestContext.getUserName())
        );
        Shipment shipment = shipmentDao.getById(id);

        ShipmentPosition result = null;
        if (shipment != null && !ShipmentStatus.PROHIBITED.equals(shipment.getStatus()) && !ShipmentStatus.COMPLETED.equals(shipment.getStatus())) {
            Goods goods = goodsDao.getById(goodsService.completeEan(ean));

            if (goods != null && goods.getArticle() != null) {

                result = shipment.getPositions().stream().filter(acceptancePosition -> acceptancePosition.getArticle() != null && goods.getArticle().equals(acceptancePosition.getArticle())).findFirst().orElse(null);

                if (result != null) {
                    if (result.getConfirmed() == null || result.getConfirmed()) {
                        if (result.getQuantityOrdered() == null || (result.getQuantityShipped() != null && result.getQuantityOrdered().equals(result.getQuantityShipped()))) {
                            throw ApplicationException
                                    .build("Позиция с артикулом {0} уже прошла отгрузку.")
                                    .parameters(goods.getArticle())
                                    .status(Status.OK)
                                    .identity(-1L);
                        } else {
                            if (mark != null) {
//                                final int delimiterPos = mark.indexOf(29);

//                                final String identCode = delimiterPos != -1 ? mark.substring(0, delimiterPos) : mark;
                                final String identCode = (mark.length() >= 31) ? mark.substring(0, 31) : mark;
                                KIZMark kizMark = kizMarkDao.findByMark(identCode);
                                if (kizMark != null) {
                                    if (kizMark.getShipmentPosition() == null) {
                                        if (kizMark.getPosition() != null && kizMark.getPosition().getArticle().equals(result.getArticle())) {
                                            kizMark.setShipmentPosition(result);
                                            result.setQuantityShipped(result.getQuantityShipped() == null ? 1 : result.getQuantityShipped() + 1);
                                            kizMark.setStatus(KIZMarkStatus.SCANNED);
                                            kizMarkDao.save(kizMark);
                                            if (result.getQuantityShipped().equals(result.getQuantityOrdered()))
                                                result.setConfirmed(true);
                                            result = shipmentPositionDao.save(result);
                                        } else {
                                            throw ApplicationException
                                                    .build("Код марки {0} используется для другого артикула. Отгрузка такого кода для артикула {1} запрещена.")
                                                    .parameters(mark, goods.getArticle())
                                                    .identity(-1L);
                                        }
                                    } else {
                                        if (kizMark.getShipmentPosition().getId().equals(result.getId())) {
                                            throw ApplicationException
                                                    .build("Код марки {0} был уже добавлен в отгрузку.")
                                                    .parameters(mark)
                                                    .status(Status.OK)
                                                    .identity(-1L);
                                        } else {
                                            throw ApplicationException
                                                    .build("Код марки {0} был уже добавлен в другую позицию/отгрузку. Повторная отгрузка такого кода для артикула {1} запрещена.")
                                                    .parameters(mark, goods.getArticle())
                                                    .identity(-1L);
                                        }
                                    }
                                } else {
                                    KIZAggregation kizAggregation = kizAggregationDao.getById(identCode);
                                    if (kizAggregation != null) {
                                        if (kizAggregation.getShipmentPosition() == null) {
                                            if (kizAggregation.getMarks() != null &&
                                                    !kizAggregation.getMarks().isEmpty() &&
                                                    kizAggregation.getMarks().get(0).getPosition() != null &&
                                                    kizAggregation.getMarks().get(0).getPosition().getArticle().equals(result.getArticle())) {
                                                kizAggregation.setShipmentPosition(result);
                                                List<KIZMark> marks = kizAggregation.getMarks().parallelStream().filter(kizMark1 -> !KIZMarkStatus.SCANNED.equals(kizMark1.getStatus())).collect(Collectors.toList());
                                                result.setQuantityShipped(result.getQuantityShipped() == null ? marks.size() : result.getQuantityShipped() + marks.size());
                                                marks.parallelStream().forEach(m -> m.setStatus(KIZMarkStatus.SCANNED));
                                                kizAggregationDao.save(kizAggregation);
                                                kizMarkDao.saveAll(marks, true);
                                                if (result.getQuantityShipped().equals(result.getQuantityOrdered()))
                                                    result.setConfirmed(true);
                                                result = shipmentPositionDao.save(result);
                                            } else {
                                                throw ApplicationException
                                                        .build("Код агрегации {0} используется для другого артикула. Отгрузка такого кода для артикула {1} запрещена.")
                                                        .parameters(mark, goods.getArticle())
                                                        .identity(-1L);
                                            }
                                        } else {
                                            if (kizAggregation.getShipmentPosition().getId().equals(result.getId())) {
                                                throw ApplicationException
                                                        .build("Код агрегации {0} был уже добавлен в отгрузку.")
                                                        .parameters(goods.getArticle(), mark)
                                                        .status(Status.OK)
                                                        .identity(-1L);
                                            } else {
                                                throw ApplicationException
                                                        .build("Код агрегации {0} был уже добавлен в другую позицию/отгрузку. Повторная отгрузка такого кода запрещена.")
                                                        .parameters(goods.getArticle(), mark)
                                                        .identity(-1L);
                                            }
                                        }
                                    } else {
                                        throw ApplicationException
                                                .build("Для позиции с артикулом {0} не найден код марки/агрегации {1}. Продукция с таким кодом запрещена к отгрузке.")
                                                .parameters(goods.getArticle(), mark)
                                                .identity(-1L);
                                    }
                                }
/*
                                if (result.getMarks().stream().filter(kizMark -> identCode.equals(delimiterPos != -1 && kizMark.getMark().length() >= delimiterPos ? kizMark.getMark().substring(0, delimiterPos) : kizMark.getMark())).findFirst().orElse(null) != null) {
                                    result.setQuantityShipped(result.getQuantityOrdered());
                                    result.setConfirmed(true);
                                    result = shipmentPositionDao.save(result);
                                } else if (result.getAggregations().stream().filter(kizAggregation -> identCode.equals(kizAggregation.getSscc())).findFirst().orElse(null) != null) {
                                    result.setQuantityShipped(result.getQuantityOrdered());
                                    result.setConfirmed(true);
                                    result = shipmentPositionDao.save(result);
                                } else {
                                    result.setQuantityShipped(0);
                                    result.setConfirmed(false);
                                    result = shipmentPositionDao.save(result);
                                    throw ApplicationException
                                            .build("Позиция с артикулом {0} не содержит код марки/агрегации {1} и запрещена к отгрузке.")
                                            .parameters(goods.getArticle(), mark)
                                            .identity(result.getId());
                                }
*/
                            }
                        }
                    } else {
                        throw ApplicationException
                                .build("Позиция с артикулом {0} не прошла проверку в ЦРПТ и запрещена к отгрузке.")
                                .parameters(goods.getArticle())
                                .identity(result.getId());
                    }
                } else
                    throw ApplicationException
                            .build("По штрихкоду определен артикул {0}, который отсутствует в данной отгрузке. Отгрузка с ШК {1} запрещена.")
                            .parameters(goods.getArticle(), ean)
                            .identity(-1L);
            } else {
                throw ApplicationException
                        .build("Невозможно определить артикул по штрихкоду {0}. Отгрузка с этим ШК запрещена.")
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
    public ShipmentPosition checkByAcceptanceImportMark(Long id, String mark) {
        protocolService.saveAsNew(
                new Protocol()
                        .entity("SHIPMENT")
                        .entityId(String.valueOf(id))
                        .action("Отсканирован код: " + mark)
                        .username(requestContext.getUserName())
        );
        Acceptance acceptance = acceptanceDao.getById(id);

        ShipmentPosition result = null;
        if (acceptance != null) {

            if (mark != null) {
                if (mark.length() >= 31) {
                    final String identCode = mark.substring(0, 31);

                    KIZMark kizMark = acceptance
                            .getPositions()
                            .stream()
                            .flatMap(acceptancePosition -> acceptancePosition.getMarks().parallelStream()).filter(kM -> identCode.equals(kM.getMark().length() >= 31 ? kM.getMark().substring(0, 31) : kM.getMark())).findFirst().orElse(null);

                    if (kizMark != null) {
                        result = kizMark.getShipmentPosition();
                        if (result.getConfirmed() == null || result.getConfirmed()) {
                            if (result.getQuantityOrdered() == null || (result.getQuantityShipped() != null && result.getQuantityOrdered().equals(result.getQuantityShipped()))) {
                                throw ApplicationException
                                        .build("Позиция с артикулом {0} уже прошла отгрузку.")
                                        .parameters(result.getArticle())
                                        .status(Status.OK)
                                        .identity(-1L);
                            } else {
                                if (!KIZMarkStatus.SCANNED.equals(kizMark.getStatus())) {
                                    kizMark.setStatus(KIZMarkStatus.SCANNED);
                                    kizMarkDao.save(kizMark);
                                    result.setQuantityShipped(result.getQuantityShipped() == null ? 1 : result.getQuantityShipped() + 1);
                                    if (result.getQuantityShipped().equals(result.getQuantityOrdered()))
                                        result.setConfirmed(true);
                                    result = shipmentPositionDao.save(result);
                                } else {
                                    throw ApplicationException
                                            .build("Марка {0} уже прошла отгрузку.")
                                            .parameters(identCode)
                                            .status(Status.OK)
                                            .identity(-1L);
                                }
                            }
                        } else {
                            throw ApplicationException
                                    .build("Позиция с артикулом {0} не прошла проверку в ЦРПТ и запрещена к отгрузке.")
                                    .parameters(result.getArticle())
                                    .identity(result.getId());
                        }
                    } else {
                        throw ApplicationException
                                .build("Отгрузка контейнера {0} по заказу {1} не содержит код марки {2}. Товар с этой маркой не может быть отгружен.")
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
                        result = kizMarks.get(0).getShipmentPosition();
                        if (result.getConfirmed() == null || result.getConfirmed()) {
                            if (result.getQuantityOrdered() == null || (result.getQuantityShipped() != null && result.getQuantityOrdered().equals(result.getQuantityShipped()))) {
                                throw ApplicationException
                                        .build("Позиция с артикулом {0} уже прошла отгрузку.")
                                        .parameters(result.getArticle())
                                        .status(Status.OK)
                                        .identity(-1L);
                            } else {
                                List<KIZMark> notScanned = kizMarks.parallelStream().filter(kM -> !KIZMarkStatus.SCANNED.equals(kM.getStatus())).collect(Collectors.toList());
                                if (!CollectionUtils.isEmpty(notScanned)) {
                                    notScanned.parallelStream().forEach(kM -> kM.setStatus(KIZMarkStatus.SCANNED));
                                    result.setQuantityShipped(result.getQuantityShipped() == null ? notScanned.size() : result.getQuantityShipped() + notScanned.size());

                                    kizMarkDao.saveAll(notScanned, true);
                                    if (result.getQuantityShipped().equals(result.getQuantityOrdered()))
                                        result.setConfirmed(true);
                                    result = shipmentPositionDao.save(result);
                                } else {
                                    throw ApplicationException
                                            .build("Упаковка товара с кодом агрегации {0} уже прошла отгрузку.")
                                            .parameters(identCode)
                                            .status(Status.OK)
                                            .identity(-1L);
                                }
                            }
                        } else {
                            throw ApplicationException
                                    .build("Позиция с артикулом {0} не прошла проверку в ЦРПТ и запрещена к отгрузке.")
                                    .parameters(result.getArticle())
                                    .identity(result.getId());
                        }
                    } else {
                        throw ApplicationException
                                .build("Отгрузка контейнера {0} по заказу {1} не содержит код агрегации {2}. Товар с этим кодом агрегации не может быть отгружен.")
                                .parameters(acceptance.getContainer(), acceptance.getOrder(), mark)
                                .identity(-1L);
                    }
                }
            }
        } else
            throw ApplicationException
                    .build("Ошибка получения отгрузки с ID {0}")
                    .parameters(id)
                    .identity(-1L);
        return result;
    }

    @Override
    public Shipment save(Shipment entity) {
        if (entity.getVendor() != null && entity.getVendor().getInn() != null) {
            Vendor vendor = vendorDao.getById(entity.getVendor().getInn());
            if (vendor == null) {
                vendor = new Vendor();
                vendor.setInn(entity.getVendor().getInn());
                vendor.setKpp(entity.getVendor().getKpp());
                vendor.setOkpo(entity.getVendor().getOkpo());
                vendor.setName(entity.getVendor().getName());
                vendor = vendorDao.save(vendor);
            }
            entity.setVendorInn(vendor.getInn());
            entity.setVendor(vendor);
        }
        if (entity.getConsignee() != null) {
            entity.setConsignee(departmentDao.getById(entity.getConsignee().getCode()));
            entity.setGlnConsignee(entity.getConsignee().getGln());
        }

        if (entity.getStatus() == null) {
            entity.setStatus(ShipmentStatus.PENDING);
        }
        entity.getPositions().parallelStream().forEach(shipmentPosition -> shipmentPosition.setShipment(entity));
        return shipmentDao.save(entity);
    }
}
