package ru.v6.mark.prototype.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.constant.*;
import ru.v6.mark.prototype.domain.criteria.KIZPositionCriteria;
import ru.v6.mark.prototype.domain.criteria.ProductionCriteria;
import ru.v6.mark.prototype.domain.dao.*;
import ru.v6.mark.prototype.domain.entity.*;
import ru.v6.mark.prototype.exception.ApplicationException;
import ru.v6.mark.prototype.service.importer.KIZOrderImportService;
import ru.v6.mark.prototype.service.util.DateUtil;
import ru.v6.mark.prototype.web.context.Response;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ProductionService extends EntityCriteriaService<Production, ProductionCriteria> {

    @Autowired
    AcceptanceDao acceptanceDao;
    @Autowired
    DepartmentDao departmentDao;
    @Autowired
    ProductionDao productionDao;
    @Autowired
    GoodsDao goodsDao;
    @Autowired
    GoodsService goodsService;
    @Autowired
    KIZPositionService kizPositionService;
    @Autowired
    KIZOrderImportService kizOrderImportService;
    @Autowired
    KIZMarkDao kizMarkDao;
    @Autowired
    KIZAggregationDao kizAggregationDao;

    private static final String[] CSV_ORDER_EAN_HEADERS = {"№", "Код маркировки"};

    @Override
    protected BaseCriteriaDao<Production, ProductionCriteria> getPrimaryCriteriaDao() {
        return productionDao;
    }

    @Override
    public Production save(Production entity) {
        entity.setEan(goodsService.completeEan(entity.getEan()));
        if (entity.getValidDate() == null && entity.getProducedDate() != null) {
            Goods goods = goodsDao.getById(entity.getEan());
            if (goods != null && goods.getArticleItem() != null && goods.getArticleItem().getValidPeriod() != null) {
                entity.setValidDate(DateUtil.plusDays(entity.getProducedDate(), goods.getArticleItem().getValidPeriod()));
            }
        }
        return super.save(entity);
    }

    public byte[] productionReceive(Long id, StringBuilder fileName) {
        Production production = productionDao.getById(id);

        int quantity = production.getQtyProduced() - (production.getQtyActual() == null ? 0 : production.getQtyActual());

        KIZPositionCriteria criteria = new KIZPositionCriteria();
        criteria.setEan(production.getEan());
        List<KIZPosition> positionList = kizPositionService.findByCriteria(criteria);

        fileName.append("ean_").append(production.getEan()).append("_qty_").append(quantity).append("_marks.csv");

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        Writer writer = null;
        CSVPrinter printer = null;
        try {
            writer = new OutputStreamWriter(stream, "windows-1251");
            CSVFormat format = CSVFormat.EXCEL.withHeader(CSV_ORDER_EAN_HEADERS).withDelimiter('\t').withEscape('\\').withQuoteMode(QuoteMode.NONE);

            printer = new CSVPrinter(writer, format);

            int i = 1;

            List<KIZMark> allMarks = new ArrayList<>();
            for (KIZPosition kizPosition : positionList) {
                if (quantity > 0 && (kizPosition.getStatus() == null || !KIZPositionStatus.RECEIVED.equals(kizPosition.getStatus()))) {
                    List<KIZMark> marks = kizOrderImportService.retrieveKIZ(kizPosition, quantity);
                    for (KIZMark mark : marks) {
                        printer.printRecord(i++, mark.getMark());
                    }
                    quantity -= marks.size();
                    allMarks.addAll(marks);
                }
            }
            production.setQtyActual((production.getQtyActual() == null ? 0 : production.getQtyActual()) + production.getQtyProduced() - quantity);

            production = productionDao.save(production);
            for (KIZMark mark : allMarks) { mark.setProduction(production); }
            kizMarkDao.saveAll(allMarks, true);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
                if (printer != null)
                    printer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return stream.toByteArray();
    }

    public Response parseAggregation(String currentUser, byte[] csvFile) throws IOException {
        CSVFormat format = CSVFormat.EXCEL.withHeader("Код марки", "Код агрегации", "Получатель").withDelimiter('\t').withEscape('\\').withQuoteMode(QuoteMode.NONE);

        ByteArrayInputStream stream = new ByteArrayInputStream(csvFile);

        Reader reader = null;
        CSVParser parser = null;
        try {
            reader = new InputStreamReader(stream, "windows-1251");

            parser = new CSVParser(reader, format);

            Acceptance acceptance = new Acceptance();

            final List<KIZMark> marks = new ArrayList<>();
            final Set<KIZOrder> orders = new HashSet<>();
            final Set<Department> consignees = new HashSet<>();
            final Map<Integer, AcceptancePosition> positions = new HashMap<>();
            final Set<Production> productions = new HashSet<>();
            parser.getRecords().forEach(record -> {
                String mark = record.get(0);
                if (mark != null && mark.length() >= 31) {
                    String aggregationCode = record.get(1);
                    KIZMark kizMark = kizMarkDao.findByMark(mark.substring(0, 31));
                    KIZAggregation kizAggregation = kizAggregationDao.getById(aggregationCode);

                    if (kizMark != null) {
                        KIZOrder markOrder = kizMark.getPosition().getKizOrder();
                        if (kizAggregation == null) {// && kizAggregation.getKizOrder().getId().equals(markOrder.getId())) {
                             kizAggregation = new KIZAggregation();
                             kizAggregation.setSscc(aggregationCode);
                             kizAggregation.setKizOrder(markOrder);
                             kizAggregation.setStatus(KIZAggregationStatus.READY);
                             //kizMark.setAggregation(kizAggregation);
                            // throw ApplicationException.build("Строка: {0}. Код агрегации {1} отсутствует или не назначен для заказа {2}.").parameters(record.getRecordNumber(), aggregationCode, markOrder.getId());
                        }
                        if (kizMark.getSscc() == null) {
                            kizMark.setSscc(aggregationCode);
                            orders.add(markOrder);
                            marks.add(kizMark);
                            productions.add(kizMark.getProduction());
                            kizMark.getProduction().setQtyApplied(kizMark.getProduction().getQtyApplied() == null ? 1 : kizMark.getProduction().getQtyApplied() + 1);


                            AcceptancePosition position = positions.get(kizMark.getPosition().getArticle());
                            if (position == null) {
                                position = new AcceptancePosition();
                                position.setAcceptance(acceptance);
                                position.setArticle(kizMark.getPosition().getArticle());
                                position.setQuantitySupplied(0);
                                position.setMarks(new ArrayList<>());
                                position.setAggregations(new ArrayList<>());
                                positions.put(position.getArticle(), position);
                            }
                            position.setQuantitySupplied(position.getQuantitySupplied() + 1);
                            position.getMarks().add(kizMark);
                            position.getAggregations().add(kizAggregation);
                        } else {
                            if (aggregationCode.equals(kizMark.getSscc())) {
                                throw ApplicationException.build("Строка: {0}. Для марки код агрегации уже был загружен.").parameters(record.getRecordNumber());
                            } else {
                                throw ApplicationException.build("Строка: {0}. Для марки уже был назначен другой код агрегации {1}.").parameters(record.getRecordNumber(), kizMark.getSscc());
                            }
                        }

                        consignees.add(departmentDao.getById(record.get(2)));
                    }
                }
            });

            consignees.remove(null);
            Department consignee = consignees.iterator().next();

            acceptance.setConsignee(consignee);
            acceptance.setDate(DateUtil.cutTime(new Date()));
            acceptance.setGlnConsignee(consignee.getGln());
            acceptance.setNumber(DateUtil.getStringByFormat(acceptance.getDate(), new SimpleDateFormat(DateUtil.PRODUCED_DATE_FORMAT)) + "_" + consignee.getCode());
            acceptance.setOrder(consignee.getCode() + DateUtil.getStringByFormat(acceptance.getDate(), new SimpleDateFormat("MMdd")));
            acceptance.setStatus(AcceptanceStatus.PENDING);
            acceptance.setType(AcceptanceType.IMPORT);
            acceptance.setPositions(new ArrayList<>(positions.values()));

            acceptanceDao.save(acceptance);

            positions.values().parallelStream().forEach(position -> {
                position.getMarks().parallelStream().forEach(kizMark -> {
                    kizMark.setAcceptancePosition(position);
                });
                position.getAggregations().parallelStream().forEach(aggregation -> {
                    aggregation.setAcceptancePosition(position);
                });
            });

            kizMarkDao.saveAll(marks, true);

            final List<KIZAggregation> aggregations = new ArrayList<>();
            orders.forEach(order -> {
                aggregations.addAll(order.getAggregations());
            });

            marks.parallelStream().forEach(kizMark -> {
                aggregations.parallelStream().filter(kizAggregation -> kizAggregation.getSscc() != null && kizMark.getSscc() != null && kizAggregation.getSscc().equals(kizMark.getSscc())).findFirst().ifPresent(aggregation -> aggregation.setStatus(KIZAggregationStatus.READY));
            });

            aggregations
                    .parallelStream()
                    .filter(aggregation -> aggregation.getStatus() != null && KIZAggregationStatus.DOWNLOADED.equals(aggregation.getStatus()))
                    .forEach(aggregation -> {
                        aggregation.setStatus(KIZAggregationStatus.AVAILABLE);
                        aggregation.setKizOrder(null);
                    });

            kizAggregationDao.saveAll(aggregations, true);

            productionDao.saveAll(productions, true);

        } catch (
                IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (parser != null)
                    parser.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new Response(Status.OK);

    }
}
