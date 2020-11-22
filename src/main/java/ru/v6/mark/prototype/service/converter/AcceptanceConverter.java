package ru.v6.mark.prototype.service.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.v6.mark.xsd.ТекстИнфТип;
import ru.v6.mark.xsd.УчастникТип;
import ru.v6.mark.xsd.Файл;
import ru.v6.mark.prototype.domain.constant.AcceptancePositionStatus;
import ru.v6.mark.prototype.domain.constant.AcceptanceStatus;
import ru.v6.mark.prototype.domain.constant.AcceptanceType;
import ru.v6.mark.prototype.domain.constant.MarkType;
import ru.v6.mark.prototype.service.ArticleService;
import ru.v6.mark.prototype.service.DepartmentService;
import ru.v6.mark.prototype.service.KIZAggregationService;
import ru.v6.mark.prototype.service.VendorService;
import ru.v6.mark.prototype.service.util.DateUtil;
import ru.v6.mark.prototype.domain.entity.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class AcceptanceConverter {

    @Autowired
    DepartmentService departmentService;

    @Autowired
    ArticleService articleService;

    @Autowired
    VendorService vendorService;

    @Autowired
    VendorConverter vendorConverter;

    @Autowired
    KIZAggregationService kizAggregationService;

    private Logger logger = LoggerFactory.getLogger(getClass());

    public void checkOrCreateVendor(УчастникТип участникТип, String vendorInn, Map<String, Vendor> vendors) {
        Vendor vendor = vendorService.getById(vendorInn);
        if (vendor == null) {
            if (!vendors.keySet().contains(vendorInn)) {
                vendors.put(vendorInn, vendorService.save(vendorConverter.convert(участникТип)));
            }
        }
    }

    public boolean convert(Файл файл, Acceptance acceptance, Map<String, Vendor> vendors) {
        if (файл != null) {
            Файл.Документ.СвСчФакт свСчФакт = файл.getДокумент().getСвСчФакт();
            String numberAcc = свСчФакт.getНомерСчФ();
            String dataAcc = свСчФакт.getДатаСчФ();
            acceptance.setType(AcceptanceType.LOCAL);
            acceptance.setNumber(numberAcc);
            acceptance.setDate(DateUtil.parseDate(dataAcc, new SimpleDateFormat("dd.MM.yyyy")));
            for (УчастникТип участникТип : свСчФакт.getСвПрод()) {
                acceptance.setVendorInn(участникТип.getИдСв().getСвЮЛУч().getИННЮЛ());
                checkOrCreateVendor(участникТип, acceptance.getVendorInn(), vendors);

            }
            for (ТекстИнфТип текстИнфТип : свСчФакт.getИнфПолФХЖ1().getТекстИнф()) {
                if (текстИнфТип.getИдентиф().equals("номер_заказа")) {
                    acceptance.setOrder(текстИнфТип.getЗначен());
                }
                if (текстИнфТип.getИдентиф().equals("GLN_грузополучателя") || текстИнфТип.getИдентиф().equals("грузополучатель")) {
                    acceptance.setGlnConsignee(текстИнфТип.getЗначен());
/*
                    DepartmentCriteria criteria = new DepartmentCriteria();
                    criteria.setGln(текстИнфТип.getЗначен());
                    List<Department> departments = departmentService.findByCriteria(criteria);
                    if (departments != null && departments.size() > 0) {
                        acceptance.setConsignee(departments.get(0));
                    }
*/
                }

                if (текстИнфТип.getИдентиф().equals("получатель") && (acceptance.getGlnConsignee() == null)) {
                    acceptance.setGlnConsignee(текстИнфТип.getЗначен());
                }
            }
            if (файл.getДокумент().getТаблСчФакт() != null
                    && файл.getДокумент().getТаблСчФакт().getСведТов() != null
                    && файл.getДокумент().getТаблСчФакт().getСведТов().size() > 0) {

                List<String> aggregationCodes = new ArrayList<>();
                List<AcceptancePosition> positions = new ArrayList<>();

                for (Файл.Документ.ТаблСчФакт.СведТов сведТов : файл.getДокумент().getТаблСчФакт().getСведТов()) {
                    AcceptancePosition acceptancePosition = new AcceptancePosition();

                    if (сведТов.getИнфПолФХЖ2() != null && сведТов.getИнфПолФХЖ2().size() > 0) {
                        for (ТекстИнфТип текстИнфТип : сведТов.getИнфПолФХЖ2()) {
                            if (текстИнфТип.getИдентиф().equals("код_материала")) {
                                acceptancePosition.setArticle(Integer.valueOf(текстИнфТип.getЗначен()));
                                Article article = articleService.getById(Integer.valueOf(текстИнфТип.getЗначен()));
                                if (article == null) {
                                    logger.error("Импорт позиции приемки {}. Не найден артикул: {}", acceptance.getOrder(), текстИнфТип.getЗначен());
                                } else {
                                    acceptancePosition.setArticleItem(article);
                                }
                            } else if (текстИнфТип.getИдентиф().equals("штрихкод")) {
                                //acceptancePosition.setEan(Integer.valueOf(текстИнфТип.getЗначен()));
                            }
                        }
                    }
                    acceptancePosition.setQuantitySupplied(сведТов.getКолТов().intValue());
                    // Коды агрегации и коды марок
                    // НомУпак или КИЗ - марки
                    // ИдентТрансУпак - код агрегации
                    if (сведТов.getДопСведТов().getНомСредИдентТов() != null
                            && сведТов.getДопСведТов().getНомСредИдентТов().size() > 0) {
                        List<KIZAggregation> aggregations = new ArrayList<>();
                        List<KIZMark> marks = new ArrayList<>();
                        for (Файл.Документ.ТаблСчФакт.СведТов.ДопСведТов.НомСредИдентТов номСредИдентТов : сведТов.getДопСведТов().getНомСредИдентТов()) {

                            if (номСредИдентТов.getИдентТрансУпак() != null) {
                                if (!aggregationCodes.contains(номСредИдентТов.getИдентТрансУпак())) {
                                    KIZAggregation aggregation = new KIZAggregation();
                                    aggregation.setSscc(номСредИдентТов.getИдентТрансУпак());
                                    aggregations.add(aggregation);
                                    aggregation.setAcceptancePosition(acceptancePosition);
                                    aggregationCodes.add(номСредИдентТов.getИдентТрансУпак());
                                } else {
                                    acceptance.setStatus(AcceptanceStatus.PENDING_ERROR);
                                    acceptancePosition.setStatus(AcceptancePositionStatus.NON_UNIQUE_CODE);
                                    acceptancePosition.setAcceptable(false);
                                }

                            }

                            if (номСредИдентТов.getКИЗ() != null && номСредИдентТов.getКИЗ().size() > 0) {
                                for (String mark : номСредИдентТов.getКИЗ()) {
                                    String code = mark;
                                    if (acceptancePosition.getArticleItem() != null && MarkType.TYPE7.equals(acceptancePosition.getArticleItem().getMarkType())) {
                                        code = mark.length() == 35 && mark.substring(25, 29).equals("8005") ? mark.substring(0, 25) : mark;
                                    }

                                    KIZMark kizMark = new KIZMark();
                                    kizMark.setMark(code);
                                    kizMark.setAcceptancePosition(acceptancePosition);
                                    marks.add(kizMark);
                                }

                            }
                            if (номСредИдентТов.getНомУпак() != null && номСредИдентТов.getНомУпак().size() > 0) {
                                for (String номУпак : номСредИдентТов.getНомУпак()) {
                                    String code = номУпак;
                                    if (acceptancePosition.getArticleItem() != null && MarkType.TYPE7.equals(acceptancePosition.getArticleItem().getMarkType())) {
                                        code = номУпак.length() == 35 && номУпак.substring(25, 29).equals("8005") ? номУпак.substring(0, 25) : номУпак;
                                    }

//                                    if (!marks.contains(номУпак)) {
                                        KIZMark kizMark = new KIZMark();
                                        kizMark.setMark(code);
                                        kizMark.setAcceptancePosition(acceptancePosition);
                                        marks.add(kizMark);
//                                    } else {
//                                        acceptance.setStatus(AcceptanceStatus.PENDING_ERROR);
//                                        acceptancePosition.setStatus(AcceptancePositionStatus.NON_UNIQUE_CODE);
//                                        acceptancePosition.setQuantityAccepted(0);
//                                        acceptancePosition.setAcceptable(false);
//                                    }

/*
                                    if (!aggregationCodes.contains(номУпак)) {
                                        KIZAggregation aggregation = new KIZAggregation();
                                        aggregation.setSscc(номУпак);
                                        aggregation.setAcceptancePosition(acceptancePosition);
                                        aggregations.add(aggregation);
                                        aggregationCodes.add(номУпак);
                                    } else {
                                        acceptance.setStatus(AcceptanceStatus.PENDING_ERROR);
                                        acceptancePosition.setStatus(AcceptancePositionStatus.NON_UNIQUE_CODE);
                                        acceptancePosition.setQuantityAccepted(0);
                                        acceptancePosition.setAcceptable(false);
                                    }
*/
                                }
                            }
                        }

                        if (!aggregations.isEmpty()) {
                            acceptancePosition.setAggregations(aggregations);
                        }
                        if (!marks.isEmpty()) {
                            acceptancePosition.setMarks(marks);
                        }
                    }
                    acceptancePosition.setAcceptance(acceptance);
                    positions.add(acceptancePosition);
                }
                acceptance.setPositions(positions);
            }
            acceptance.setStatus(AcceptanceStatus.PENDING);
        }
        return true;
    }

}
