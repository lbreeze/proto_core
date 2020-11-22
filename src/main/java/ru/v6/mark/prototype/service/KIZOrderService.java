package ru.v6.mark.prototype.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.DocumentException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Hibernate;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.criteria.AcceptanceCriteria;
import ru.v6.mark.prototype.domain.criteria.GtinCriteria;
import ru.v6.mark.prototype.domain.criteria.KIZOrderCriteria;
import ru.v6.mark.prototype.exception.ApplicationException;
import ru.v6.mark.prototype.service.importer.KIZOrderImportService;
import ru.v6.mark.prototype.service.importer.ProductImportService;
import ru.v6.mark.prototype.service.util.ExcelUtil;
import ru.v6.mark.prototype.service.util.SleepUtil;
import ru.v6.mark.prototype.service.util.StringUtil;
import ru.v6.mark.prototype.web.context.RequestWrapper;
import ru.v6.mark.prototype.web.context.Response;
import ru.v6.mark.prototype.domain.constant.*;
import ru.v6.mark.prototype.domain.dao.*;
import ru.v6.mark.prototype.domain.entity.*;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class KIZOrderService extends EntityCriteriaService<KIZOrder, KIZOrderCriteria> {

    @Autowired
    ClientService clientService;

    @Autowired
    KIZAggregationService kizAggregationService;
    @Autowired
    KIZOrderDao kizOrderDao;
    @Autowired
    AcceptanceDao acceptanceDao;
    @Autowired
    KIZMarkDao kizMarkDao;
    @Autowired
    KIZMarkService kizMarkService;
    @Autowired
    GoodsService goodsService;
    @Autowired
    GoodsDao goodsDao;
    @Autowired
    DepartmentDao departmentDao;
    @Autowired
    ArticleDao articleDao;
    @Autowired
    UserDao userDao;
    @Autowired
    ProductImportService productImportService;
    @Autowired
    PrintCodeService printCodeService;
    @Autowired
    KIZOrderImportService kizOrderImportService;
    @Autowired
    GtinService gtinService;
    @Autowired
    GtinDao gtinDao;
    @Autowired
    KIZAggregationDao kizAggregationDao;
    @Autowired
    @Qualifier("serverMessages")
    MessageSource messages;

    private static final String[] CSV_ORDER_EAN_HEADERS = {"№", "Артикул", "Наименование", "ШК", "Серийный номер", "Код маркировки"};
    private static final String[] CSV_ORDER_GTIN_HEADERS = {"Магазин", "Вид обуви", "Тип обуви", "Серийный номер", "Код маркировки"};

    private static final String[] CSV_CODES_HEADERS = {"№", "Код агрегации"};

    @Override
    protected BaseCriteriaDao<KIZOrder, KIZOrderCriteria> getPrimaryCriteriaDao() {
        return kizOrderDao;
    }

    public KIZOrder getWithPositions(Serializable id) {
        KIZOrder result = getById(id);
        Hibernate.initialize(result.getPositions());
        if (KIZOrderType.IMPORT.equals(result.getOrderType())) {
            result.getPositions().forEach(kizPosition -> {
                if (kizPosition.getScanned() == null) {
                    AtomicInteger scanned = new AtomicInteger(0);
                    kizPosition.getMarks().parallelStream().forEach(kizMark -> {
                        if (kizMark.getStatus() != null && !KIZMarkStatus.RECEIVED.equals(kizMark.getStatus())) {
                            scanned.incrementAndGet();
                        }
                    });
                    kizPosition.setScanned(scanned.get());
                }
            });
        }
        return result;
    }

    public void validateKIZ(Long id) {
        validateKIZ(id, null);
    }
    public void validateKIZ(Long id, Boolean ssccOnly) {
        KIZOrder kizOrder = kizOrderDao.getById(id);
        if (KIZOrderType.IMPORT.equals(kizOrder.getOrderType())) {
            kizOrder.getPositions().forEach(kizPosition -> {
                if (kizPosition.getScanned() == null) {
                    kizPosition.getMarks().parallelStream().forEach(kizMark -> {
                        if (KIZMarkStatus.SCANNED.equals(kizMark.getStatus())) {
                            kizMark.setStatus(KIZMarkStatus.VALIDATED);
                        }
                    });
                }
            });
            kizOrder = kizOrderDao.save(kizOrder);
            sendMarksToTurn(kizOrder, ssccOnly);

/*
            kizOrder.getPositions().forEach(position -> {
                kizMarkService.sendMarksToTurn(position);
            });
*/
        }
    }

    /**
     * Ввод в оборот КМ заказа
     */
    public void sendMarksToTurn(KIZOrder kizOrder, Boolean ssccOnly) {

        List<KIZMark> marks = kizOrder.getPositions().parallelStream().flatMap(position -> position.getMarks().parallelStream()).filter(kizMark -> KIZMarkStatus.VALIDATED.equals(kizMark.getStatus())).collect(Collectors.toList());

        String keyAlias = kizOrder.getDepartment().getKeyAlias();
        while (!CollectionUtils.isEmpty(marks)) {

            String docId = clientService.createDocument(keyAlias, getToken(keyAlias), kizOrder, ssccOnly);

            if (StringUtils.isEmpty(docId)) {
                kizOrder.getPositions().parallelStream().forEach(position -> position.setStatus(KIZPositionStatus.ERROR));
            } else {
                kizOrder.getPositions().parallelStream().forEach(position -> position.setDocId(docId));

                String[] status = null;

                long t = System.currentTimeMillis();
                while (status == null) {

                    SleepUtil.sleep(SleepUtil.CRPT_QUERY_INTERVAL);
                    try {
                        status = clientService.getDocumentStatus(getToken(keyAlias), docId);

                        KIZPositionStatus kpStatus = (!StringUtil.hasLength(status[0])) ? null : KIZPositionStatus.forValue(status[0]);
                        kizOrder.getPositions().parallelStream().forEach(position -> position.setStatus(kpStatus != null ? kpStatus : KIZPositionStatus.ERROR));

                        if (kpStatus != null) {
                            if (!kpStatus.equals(KIZPositionStatus.CHECKED_OK)) {
                                String error = status[1];
                                kizOrder.getPositions().parallelStream().forEach(position -> position.setStatusDesc(error));
/*
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
*/
                                    marks.parallelStream().forEach(kizMark -> kizMark.setStatus(KIZMarkStatus.ERROR));
                                    marks.clear();
//                                }
                            } else {
                                marks.parallelStream().forEach(kizMark -> kizMark.setStatus(KIZMarkStatus.CIRCULATED));
                                marks.clear();
                            }
                        } else {
                            status = null;
                        }
                    } catch (JSONException e) {
                        logger.info(e.getMessage());
                    }
                }
            }
        }
        kizOrder = kizOrderDao.save(kizOrder);
    }

    private String getGtin(Article article, Organization organization) {
        GtinCriteria criteria = new GtinCriteria();
        criteria.setImported(article.getImported());
        criteria.setMarkSubType(article.getMarkSubType());
        criteria.setOrganization(organization);
        List<Gtin> gtins = gtinService.findByCriteria(criteria);
        return gtins != null && gtins.get(0) != null ? gtins.get(0).getGtin() : "";
    }

    public KIZOrder save(KIZOrder entity) {
        Department department = departmentDao.getById(entity.getDepartmentCode());
        List<KIZPosition> toRemove = new ArrayList<>();
        for (KIZPosition position : entity.getPositions()) {

            position.setKizOrder(entity);
            if (position.getEan() != null) {
                position.setEan(goodsService.completeEan(position.getEan()));

                if (KIZOrderForm.EAN.equals(entity.getOrderForm())) {
                    Goods goods = goodsDao.getById(position.getEan());
                    if (goods == null) {
                        throw ApplicationException.build("Товара со штрих-кодом {0} не существует.").status(Status.ERROR_COMMON).parameters(position.getEan());
                    }
                }
            } else if (position.getArticle() != null) {
                Article article = articleDao.getById(position.getArticle());
                if (article.getImported() == null || article.getMarkSubType() == null) {
                    logger.error("LOAD KIZ_ORDER FROM GUI. ERROR: For article " + article.getId() + " not found Imported or MarkSubType");
                    toRemove.add(position);
                    continue;
                }

                if (!StringUtil.hasLength(position.getEan())) {
                    position.setEan(getGtin(article, department.getOrganization()));
                }
            }

        }
        entity.getPositions().removeAll(toRemove);
        if (entity.getId() != null)
            return super.save(entity);
        else
            return kizOrderDao.save(entity);
    }

    public void sendOrder(RequestWrapper<String> requestWrapper) {
        KIZOrder kizOrder = getById(Long.parseLong(requestWrapper.getEntity()));
        kizOrderImportService.sendKIZOrder(kizOrder.getDepartment().getKeyAlias(), kizOrder.getDepartment().getOrganization(), kizOrder, requestWrapper.getCurrentUser(), false);
        kizOrderDao.save(kizOrder);
    }

    public void sendErrorOrder(KIZOrder kizOrder) {
        kizOrderImportService.sendKIZOrder(kizOrder.getDepartment().getKeyAlias(), kizOrder.getDepartment().getOrganization(), kizOrder, null, true);
        kizOrderDao.save(kizOrder);
    }

    public Response parseAggregation(String currentUser, byte[] csvFile) throws IOException {
        CSVFormat format = CSVFormat.EXCEL.withHeader("Код марки", "Код агрегации").withDelimiter('\t').withEscape('\\').withQuoteMode(QuoteMode.NONE);

        ByteArrayInputStream stream = new ByteArrayInputStream(csvFile);

        Reader reader = null;
        CSVParser parser = null;
        try {
            reader = new InputStreamReader(stream, "windows-1251");

            parser = new CSVParser(reader, format);

            final List<KIZMark> marks = new ArrayList<>();
            final Set<KIZOrder> orders = new HashSet<>();
            parser.getRecords().forEach(record -> {
                String mark = record.get(0);
                if (mark != null && mark.length() >= 31) {
                    String aggregationCode = record.get(1);
                    KIZMark kizMark = kizMarkDao.findByMark(mark.substring(0, 31));
                    KIZAggregation kizAggregation = kizAggregationDao.getById(aggregationCode);

                    if (kizMark != null) {
                        KIZOrder markOrder = kizMark.getPosition().getKizOrder();
                        if (kizAggregation != null && kizAggregation.getKizOrder().getId().equals(markOrder.getId())) {
                            if (kizMark.getSscc() == null) {
                                kizMark.setSscc(aggregationCode);
                                orders.add(markOrder);
                                marks.add(kizMark);
                            } else {
                                if (aggregationCode.equals(kizMark.getSscc())) {
                                    throw ApplicationException.build("Строка: {0}. Для марки код агрегации уже был загружен.").parameters(record.getRecordNumber());
                                } else {
                                    throw ApplicationException.build("Строка: {0}. Для марки уже был назначен другой код агрегации {1}.").parameters(record.getRecordNumber(), kizMark.getSscc());
                                }
                            }
                        } else {
                            throw ApplicationException.build("Строка: {0}. Код агрегации {1} отсутствует или не назначен для заказа {2}.").parameters(record.getRecordNumber(), aggregationCode, markOrder.getId());
                        }
                    }
                }
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

    public Response parseImportSpace(String currentUser, String departmentCode, byte[] xlsFile) throws IOException {

        Department departmentItem = departmentDao.getById(departmentCode);

        Map<String, KIZOrder> orders = new HashMap<>();
        Map<Integer, Map<String, Acceptance>> acceptances = new HashMap<>();

        Map<String, KIZPosition> eanPositions = new HashMap<>();
        Map<Integer, AcceptancePosition> articlePositions = new HashMap<>();

        XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsFile));

        int i = 0;

        List<ApplicationException> exceptionList = new ArrayList<>();
        int exceptionsCount = exceptionList.size();

        while (i < workbook.getNumberOfSheets()) {
            XSSFSheet sheet = workbook.getSheetAt(i);
            int r = 1;
            XSSFRow row = sheet.getRow(r++);

            while (row != null) {

                String ean = null;
                Goods goods = null;

                Integer article = null;
                Article articleItem = null;

                Integer quantity = null;
                Integer orderNum = null;
                String tnved = null;
                String container = null;
                String invoice = null;
                Date invoiceDate = null;
                String supplier = null;

                XSSFCell cell = row.getCell(54);
                if (cell != null) {

                    if (cell.getCellType().equals(CellType.STRING)) {
                        try {
                            article = Integer.parseInt(cell.getStringCellValue().trim());
                        } catch (NumberFormatException e) {
                            exceptionList.add(ApplicationException.build("На листе {0} в строке {1} неверный номер артикула: '{2}'").parameters(sheet.getSheetName(), r, article));
                        }
                    } else if (cell.getCellType().equals(CellType.NUMERIC)) {
                        article = ((Double) cell.getNumericCellValue()).intValue();
                    }

                    if (article != null) {
                        articleItem = articleDao.getById(article);
                        if (articleItem == null || !articleItem.isMarked()) {
                            exceptionList.add(ApplicationException.build("На листе {0} в строке {1} не найден артикул {2} в списке маркированной продукции").parameters(sheet.getSheetName(), r, article));
                        }
                    }
                }

                cell = row.getCell(1);
                if (cell != null) {
                    if (cell.getCellType().equals(CellType.STRING)) {
                        ean = cell.getStringCellValue();
                    } else if (cell.getCellType().equals(CellType.NUMERIC)) {
                        ean = String.valueOf(((Double) cell.getNumericCellValue()).longValue());
                    }
                    if (ean != null) {
                        ean = goodsService.completeEan(ean.trim());
                        goods = goodsDao.getById(ean);
                        if (goods == null) {
                            exceptionList.add(ApplicationException.build("На листе {0} в строке {1} штрих-код {2} не найден в списке маркированной продукции").parameters(sheet.getSheetName(), r, ean));
                        }
                    }
                }

                cell = row.getCell(9);
                if (cell != null) {
                    if (cell.getCellType().equals(CellType.STRING)) {
                        try {
                            orderNum = Integer.parseInt(cell.getStringCellValue().trim());
                        } catch (NumberFormatException e) {
                            exceptionList.add(ApplicationException.build("На листе {0} в строке {1} неверный номер заказа: '{2}'").parameters(sheet.getSheetName(), r, cell.getStringCellValue().trim()));
                        }
                    } else if (cell.getCellType().equals(CellType.NUMERIC)) {
                        Double doubleVal = cell.getNumericCellValue();
                        orderNum = doubleVal.intValue();
                        if (doubleVal - (double) quantity != 0) {
                            exceptionList.add(ApplicationException.build("На листе {0} в строке {1} неверный номер заказа: '{2}'").parameters(sheet.getSheetName(), r, orderNum));
                        }
                    }
                }

                cell = row.getCell(12);
                if (cell != null) {
                    if (cell.getCellType().equals(CellType.STRING)) {
                        supplier = cell.getStringCellValue().trim();
                    }
                }

                cell = row.getCell(68);
                if (cell != null) {
                    if (cell.getCellType().equals(CellType.STRING)) {
                        try {
                            quantity = Integer.parseInt(cell.getStringCellValue().trim());
                        } catch (NumberFormatException e) {
                            exceptionList.add(ApplicationException.build("На листе {0} в строке {1} неверное целое количество: '{2}'").parameters(sheet.getSheetName(), r, cell.getStringCellValue().trim()));
                        }
                    } else if (cell.getCellType().equals(CellType.NUMERIC)) {
                        Double doubleQty = cell.getNumericCellValue();
                        quantity = doubleQty.intValue();
                        if (doubleQty - (double) quantity != 0) {
                            exceptionList.add(ApplicationException.build("На листе {0} в строке {1} неверное целое количество: '{2}'").parameters(sheet.getSheetName(), r, quantity));
                        }
                    }
                }

                cell = row.getCell(ExcelUtil.colNameToIndex("BZ"));
                if (cell != null) {
                    if (cell.getCellType().equals(CellType.STRING)) {
                        tnved = cell.getStringCellValue().trim();
                    }
                }
                cell = row.getCell(85);
                if (cell != null) {
                    if (cell.getCellType().equals(CellType.STRING)) {
                        container = cell.getStringCellValue().trim();
                    }
                }

                cell = row.getCell(88);
                if (cell != null) {
                    if (cell.getCellType().equals(CellType.STRING)) {
                        invoice = cell.getStringCellValue().trim();
                    } else if (cell.getCellType().equals(CellType.NUMERIC)) {
                        invoice = String.format("%d", ((Double) cell.getNumericCellValue()).intValue());
                    }
                }

                cell = row.getCell(89);
                if (cell != null) {
                    invoiceDate = cell.getDateCellValue();
//                    if (cell.getCellType().equals(CellType.STRING)) {
//                        invoice = cell.getStringCellValue().trim();
//                    } else if (cell.getCellType().equals(CellType.NUMERIC)) {
//                        invoice = String.valueOf(cell.getNumericCellValue());
//                    }
                }

                if (ean != null || article != null || quantity != null) {
                    if (ean == null && article == null) {
                        exceptionList.add(ApplicationException.build("На листе {0} в строке {1} отсутствуют и артикул и штрихкод").parameters(sheet.getSheetName(), r));
                    }

                    if (goods != null && article != null) {
                        if (!goods.getArticle().equals(article)) {
                            exceptionList.add(ApplicationException.build("На листе {0} в строке {1} отсутствует связь между указанным артикулом {2} и штрихкодом {3}").parameters(sheet.getSheetName(), r, article, ean));
                        }
                    }

                    if (quantity == null || quantity <= 0) {
                        exceptionList.add(ApplicationException.build("На листе {0} в строке {1} не указано/некорректное количество марок").parameters(sheet.getSheetName(), r));
                    }

                    if (exceptionsCount == exceptionList.size()) {
                        KIZOrder kizOrder = orders.computeIfAbsent(container, c -> {
                            KIZOrder ko = new KIZOrder();
                            ko.setCreated(new Date());
                            ko.setDepartmentCode(departmentCode);
                            ko.setStatus(KIZOrderStatus.CREATED);
                            ko.setOrderType(KIZOrderType.IMPORT);
                            ko.setOrderForm(KIZOrderForm.EAN);
                            ko.setPositions(new ArrayList<>());
                            return ko;
                        });

                        KIZPosition kizPosition = eanPositions.get(ean);
                        if (kizPosition == null) {
                            kizPosition = new KIZPosition();
                            kizPosition.setEan(ean);
                            kizPosition.setTnved(tnved);
                            kizPosition.setQuantity(quantity);
                            //kizPosition.setOrder(String.valueOf(orderNum));
                            //kizPosition.setContainer(container);

                            eanPositions.put(ean, kizPosition);
                            kizOrder.getPositions().add(kizPosition);
                            kizPosition.setKizOrder(kizOrder);
                        } else {
                            kizPosition.setQuantity(kizPosition.getQuantity() + quantity);
                        }

                        kizPosition.setArticle(article);

                        Acceptance acceptance = acceptances.computeIfAbsent(orderNum, k -> new HashMap<>()).computeIfAbsent(container, c -> {
                            Acceptance a = new Acceptance();
                            a.setType(AcceptanceType.IMPORT);
                            a.setGlnConsignee(departmentItem.getGln());
                            a.setStatus(AcceptanceStatus.PENDING);
                            a.setPositions(new ArrayList<>());
                            return a;
                        });

                        acceptance.setNumber(invoice);
                        acceptance.setDate(invoiceDate);
                        acceptance.setOrder(String.valueOf(orderNum));
                        acceptance.setContainer(container);
                        acceptance.setSupplier(supplier);

                        AcceptancePosition acceptancePosition = articlePositions.get(article);
                        if (acceptancePosition == null) {
                            acceptancePosition = new AcceptancePosition();
                            acceptancePosition.setArticle(article);
                            acceptancePosition.setQuantitySupplied(quantity);

                            articlePositions.put(article, acceptancePosition);
                            acceptance.getPositions().add(acceptancePosition);
                            acceptancePosition.setAcceptance(acceptance);
                        } else {
                            acceptancePosition.setQuantitySupplied(acceptancePosition.getQuantitySupplied() + quantity);
                        }


                    } else {
                        exceptionsCount = exceptionList.size();
                    }
                }
                row = sheet.getRow(r++);
            }
            i++;
        }

        Response response;
        if (!exceptionList.isEmpty()) {
            response = new Response(Status.ERROR_COMMON);
            StringBuilder message = new StringBuilder();
            exceptionList.forEach(e -> {
                String msg;
                try {
                    msg = messages.getMessage(e.getMessage(), e.getParameters(), Locale.getDefault());
                } catch (NoSuchMessageException e1) {
                    msg = MessageFormat.format(e.getMessage(), e.getParameters());
                }
                message.append(msg).append('\n');
            });
            response.setMessage(message.toString());
        } else {
            response = new Response(Status.OK);
        }

        ObjectMapper jsonMapper = Jackson2ObjectMapperBuilder.json().build();

        orders.values().forEach(kizOrder -> {
            try {
                logger.info(jsonMapper.writeValueAsString(kizOrder));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            kizOrderDao.save(kizOrder);
        });

        acceptances.values().forEach(containers -> {
            containers.values().forEach(acceptance -> {
                try {
                    logger.info(jsonMapper.writeValueAsString(acceptance));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

                AcceptanceCriteria criteria = new AcceptanceCriteria();
                criteria.setOrder(Integer.parseInt(acceptance.getOrder()));
                criteria.setContainer(acceptance.getContainer());
                List<Acceptance> duplicated = acceptanceDao.findByCriteria(criteria);
                if (!duplicated.isEmpty()) {
                    duplicated.forEach(dupAcceptance -> {
                        dupAcceptance.setDeleted(true);
                    });
                    acceptanceDao.saveAll(duplicated, true);
                }
                acceptanceDao.save(acceptance);
            });
        });

        return response;
    }

    public Response parseImportDamco(String currentUser, String departmentCode, byte[] xlsFile) throws IOException {
        return new Response(Status.ERROR_COMMON, "Функционал не реализован.");
    }

    public Response parseOrder(String currentUser, String departmentCode, byte[] xlsFile, KIZOrderForm orderForm) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsFile));
        int i = 0;

        Department departmentItem = departmentCode == null || departmentCode.isEmpty() ? null : departmentDao.getById(departmentCode);

        Map<Department, KIZOrder> orders = new HashMap<>();

        Map<Department, Map<String, KIZPosition>> eanPositions = new HashMap<>();

        List<ApplicationException> exceptionList = new ArrayList<>();
        int exceptionsCount = exceptionList.size();
        while (i < workbook.getNumberOfSheets()) {
            XSSFSheet sheet = workbook.getSheetAt(i);
            int r = 1;
            XSSFRow row = sheet.getRow(r++);

            while (row != null) {
                String ean = null;
                Integer article = null;
                Goods goods = null;
                Gtin gtin = null;
                Article articleItem = null;
                Integer quantity = null;

                XSSFCell cell = row.getCell(0);
                if (cell != null) {

                    if (cell.getCellType().equals(CellType.STRING)) {
                        try {
                            article = Integer.parseInt(cell.getStringCellValue().trim());
                        } catch (NumberFormatException e) {
                            exceptionList.add(ApplicationException.build("На листе {0} в строке {1} неверный номер артикула: '{2}'").parameters(sheet.getSheetName(), r, article));
                        }
                    } else if (cell.getCellType().equals(CellType.NUMERIC)) {
                        article = ((Double) cell.getNumericCellValue()).intValue();
                    }

                    if (article != null) {
                        articleItem = articleDao.getById(article);
                        if (articleItem == null || !articleItem.isMarked()) {
                            exceptionList.add(ApplicationException.build("На листе {0} в строке {1} не найден артикул {2} в списке маркированной продукции").parameters(sheet.getSheetName(), r, article));
                        }
                    }
                }

                cell = row.getCell(1);
                if (cell != null) {
                    if (cell.getCellType().equals(CellType.STRING)) {
                        ean = cell.getStringCellValue();
                    } else if (cell.getCellType().equals(CellType.NUMERIC)) {
                        ean = String.valueOf(((Double) cell.getNumericCellValue()).longValue());
                    }
                    if (ean != null) {
                        ean = goodsService.completeEan(ean.trim());
                        goods = goodsDao.getById(ean);
                        if (goods == null) {
                            if (KIZOrderForm.GTIN.equals(orderForm)) {
                                gtin = gtinDao.getById(ean);
                                if (gtin == null) {
                                    exceptionList.add(ApplicationException.build("На листе {0} в строке {1} штрих-код {2} не найден в списке маркированной продукции").parameters(sheet.getSheetName(), r, ean));
                                }
                            } else {
                                exceptionList.add(ApplicationException.build("На листе {0} в строке {1} штрих-код {2} не найден в списке маркированной продукции").parameters(sheet.getSheetName(), r, ean));
                            }
                        }
                    }

                }

                cell = row.getCell(3);
                if (cell != null) {
                    if (cell.getCellType().equals(CellType.STRING)) {
                        try {
                            quantity = Integer.parseInt(cell.getStringCellValue().trim());
                        } catch (NumberFormatException e) {
                            exceptionList.add(ApplicationException.build("На листе {0} в строке {1} неверное целое количество: '{2}'").parameters(sheet.getSheetName(), r, cell.getStringCellValue().trim()));
                        }
                    } else if (cell.getCellType().equals(CellType.NUMERIC)) {
                        Double doubleQty = cell.getNumericCellValue();
                        quantity = doubleQty.intValue();
                        if (doubleQty - (double) quantity != 0) {
                            exceptionList.add(ApplicationException.build("На листе {0} в строке {1} неверное целое количество: '{2}'").parameters(sheet.getSheetName(), r, quantity));
                        }
                    }
                }

                if (ean != null || article != null || quantity != null) {
                    if (ean == null && article == null) {
                        exceptionList.add(ApplicationException.build("На листе {0} в строке {1} отсутствуют и артикул и штрихкод").parameters(sheet.getSheetName(), r));
                    }

                    if (goods != null && articleItem != null) {
                        if (!goods.getArticle().equals(article)) {
                            exceptionList.add(ApplicationException.build("На листе {0} в строке {1} отсутствует связь между указанным артикулом {2} и штрихкодом {3}").parameters(sheet.getSheetName(), r, article, ean));
                        }
                    }

                    if (quantity != null && quantity > 0) {

                        if (goods != null && articleItem == null) {
                            articleItem = goods.getArticleItem();
                        }

                        if (articleItem == null) {
                            if (KIZOrderForm.EAN.equals(orderForm)) {
                                exceptionList.add(ApplicationException.build("На листе {0} в строке {1} не задан артикул").parameters(sheet.getSheetName(), r));
                            }
                        } else {
                            article = articleItem.getArticle();
                            if (KIZOrderForm.GTIN.equals(orderForm) && (articleItem.getImported() == null || articleItem.getMarkSubType() == null)) {
                                exceptionList.add(ApplicationException.build("По артикулу {0} не удалось определить упрощенный GTIN").parameters(article));
                            }
                        }

//                        if (kizOrder.getOrderForm() == null) {
//                            kizOrder.setOrderForm(goods == null ? KIZOrderForm.GTIN : KIZOrderForm.EAN);
//                        }

                        Department department = null;

                        if (departmentItem == null) {
                            cell = row.getCell(4);
                            if (cell != null) {
                                if (cell.getCellType().equals(CellType.STRING)) {
                                    departmentCode = cell.getStringCellValue();
                                } else if (cell.getCellType().equals(CellType.NUMERIC)) {
                                    departmentCode = String.valueOf(((Double) cell.getNumericCellValue()).intValue());
                                }

                                if (departmentCode != null) {

                                    while (departmentCode.length() < 3) departmentCode = "0" + departmentCode;
                                    department = departmentDao.getById(departmentCode);
                                    if (department == null)
                                        exceptionList.add(ApplicationException.build("На листе {0} в строке {1} не удалось определить подразделение").parameters(sheet.getSheetName(), r));
                                } else {
                                    exceptionList.add(ApplicationException.build("На листе {0} в строке {1} не удалось определить подразделение").parameters(sheet.getSheetName(), r));
                                }
                            } else {
                                exceptionList.add(ApplicationException.build("На листе {0} в строке {1} не удалось определить подразделение").parameters(sheet.getSheetName(), r));
                            }
                        } else {
                            department = departmentItem;
                        }

                        if (department != null) {

                            if (gtin == null && KIZOrderForm.GTIN.equals(orderForm)) {
                                if (articleItem != null) {
                                    ean = getGtin(articleItem, department.getOrganization());
                                } else
                                    exceptionList.add(ApplicationException.build("На листе {0} в строке {1} указан некорректный GTIN (2)").parameters(sheet.getSheetName(), r, ean));
                            }

                            if (exceptionsCount == exceptionList.size()) {
                                KIZOrder kizOrder = orders.get(department);
                                if (kizOrder == null) {
                                    kizOrder = new KIZOrder();
                                    kizOrder.setCreated(new Date());
                                    kizOrder.setDepartmentCode(departmentCode);
                                    kizOrder.setStatus(KIZOrderStatus.CREATED);
                                    kizOrder.setOrderType(KIZOrderType.REMAINS);
                                    kizOrder.setOrderForm(orderForm);
                                    kizOrder.setPositions(new ArrayList<>());

                                    orders.put(department, kizOrder);
                                }


                                Map<String, KIZPosition> departmentEans = eanPositions.computeIfAbsent(department, k -> new HashMap<>());
                                KIZPosition kizPosition = departmentEans.get(ean);
                                if (kizPosition == null) {
                                    kizPosition = new KIZPosition();
                                    kizPosition.setEan(ean);
                                    kizPosition.setQuantity(quantity);

                                    departmentEans.put(ean, kizPosition);
                                    kizOrder.getPositions().add(kizPosition);
                                    kizPosition.setKizOrder(kizOrder);
                                } else {
                                    kizPosition.setQuantity(kizPosition.getQuantity() + quantity);
                                }

                                if (KIZOrderForm.EAN.equals(kizOrder.getOrderForm()))
                                    kizPosition.setArticle(article);

                            } else {
                                exceptionsCount = exceptionList.size();
                            }
                        }
                    } else {
                        exceptionList.add(ApplicationException.build("На листе {0} в строке {1} не указано/некорректное количество марок").parameters(sheet.getSheetName(), r));
                    }
                }
                row = sheet.getRow(r++);
            }

            i++;
        }

        Response response;
        if (!exceptionList.isEmpty()) {
            response = new Response(Status.ERROR_COMMON);
            StringBuilder message = new StringBuilder();
            exceptionList.forEach(e -> {
                String msg;
                try {
                    msg = messages.getMessage(e.getMessage(), e.getParameters(), Locale.getDefault());
                } catch (NoSuchMessageException e1) {
                    msg = MessageFormat.format(e.getMessage(), e.getParameters());
                }
                message.append(msg).append('\n');
            });
            response.setMessage(message.toString());
        } else {
            ObjectMapper jsonMapper = Jackson2ObjectMapperBuilder.json().build();

            for (KIZOrder kizOrder : orders.values()) {
                logger.info(jsonMapper.writeValueAsString(kizOrder));
            }
            kizOrderDao.saveAll(orders.values(), true);
            response = new Response(Status.OK);
        }
        return response;
    }


    public void retrieveKIZ(Long id) {
/*
        KIZOrder order = kizOrderDao.getById(id);
        boolean result = kizOrderImportService.retrieveKIZ(order.getDepartmentCode(), order.getDepartment().getOrganization(), order);
        if (result) {
            order.setReply(new Date());
            order.setStatus(KIZOrderStatus.RECEIVED);
            kizOrderDao.save(order);
        }
*/
    }

    public byte[] generateMarks(Long id, StringBuilder fileName) {
        KIZOrder kizOrder = kizOrderDao.getById(id);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            fileName.append("dept_").append(kizOrder.getDepartmentCode()).append("_order_").append(kizOrder.getId()).append(".pdf");
            printCodeService.getPdf(stream, kizOrder.getPositions());

            stream.close();
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
        }
        return stream.toByteArray();
    }

    public byte[] brokerReport(Long id, StringBuilder fileName) {
        KIZOrder kizOrder = kizOrderDao.getById(id);

        fileName.append("broker_report_order_").append(kizOrder.getId()).append("_marks.xlsx");

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook();
            final XSSFSheet sheet = workbook.createSheet("Order " + kizOrder.getId());

            XSSFRow header = sheet.createRow(0);
            header.createCell(0).setCellValue("Артикул");
            header.createCell(1).setCellValue("GTIN");
            header.createCell(2).setCellValue("Код марки");

            kizOrder.getPositions().forEach(kizPosition -> {
                kizPosition.getMarks().forEach(kizMark -> {
                    if (kizMark.getSscc() != null) {
                        XSSFRow row = sheet.createRow(sheet.getLastRowNum() + 1);
                        if (kizPosition.getArticle() != null)
                            row.createCell(0).setCellValue(kizPosition.getArticle());

                        if (kizPosition.getEan() != null)
                            row.createCell(1).setCellValue(kizPosition.getEan());

                        if (kizMark.getMark() != null)
                            row.createCell(2).setCellValue(kizMark.getMark().length() < 31 ? kizMark.getMark() : kizMark.getMark().substring(0, 31));
                    }
                });
            });
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);
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

    public byte[] downloadOrder(Long id, StringBuilder fileName) {
        KIZOrder kizOrder = kizOrderDao.getById(id);

        fileName.append("dept_").append(kizOrder.getDepartmentCode()).append("_order_").append(kizOrder.getId()).append("_marks.csv");

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        Writer writer = null;
        CSVPrinter printer = null;
        try {
            boolean isEan = KIZOrderForm.EAN.equals(kizOrder.getOrderForm());
            writer = new OutputStreamWriter(stream, "windows-1251");
            CSVFormat format = CSVFormat.EXCEL.withHeader(isEan ? CSV_ORDER_EAN_HEADERS : CSV_ORDER_GTIN_HEADERS).withDelimiter('\t').withEscape('\\').withQuoteMode(QuoteMode.NONE);

            printer = new CSVPrinter(writer, format);

            int i = 1;

            for (KIZPosition kizPosition : kizOrder.getPositions()) {
                for (KIZMark kizMark : kizPosition.getMarks()) {
                    if (isEan) {
                        printer.printRecord(i++, kizPosition.getArticle(), kizPosition.getArticleItem() == null ? null : kizPosition.getArticleItem().getName(), kizPosition.getEan(), kizMark.getMark().substring(18, 31), kizMark.getMark());
                    } else {
                        printer.printRecord(kizOrder.getDepartmentCode(), kizPosition.getGtin().getMarkSubType().getDescription(), kizPosition.getGtin().getImported() ? "Ввезен в РФ" : "Произведен в РФ", kizMark.getMark().substring(18, 31), kizMark.getMark());
                    }
                }
            }
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

    public synchronized byte[] downloadCodes(Long id, int quantity, StringBuilder fileName) {
        KIZOrder kizOrder = kizOrderDao.getById(id);

        fileName.append("dept_").append(kizOrder.getDepartmentCode()).append("_order_").append(kizOrder.getId()).append("_codes.csv");

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        Writer writer = null;
        CSVPrinter printer = null;
        try {
            writer = new OutputStreamWriter(stream, "windows-1251");
            CSVFormat format = CSVFormat.EXCEL.withHeader(CSV_CODES_HEADERS).withDelimiter('\t').withEscape('\\').withQuoteMode(QuoteMode.NONE);

            printer = new CSVPrinter(writer, format);

            int i = 0;

            synchronized (KIZAggregation.class) {
                List<KIZAggregation> aggregations = kizAggregationDao.findByGln(kizOrder.getDepartment().getGln(), quantity);
                if (aggregations != null && !aggregations.isEmpty()) {
                    for (KIZAggregation aggregation : aggregations) {
                        printer.printRecord(i++, aggregation.getSscc());
                        aggregation.setKizOrder(kizOrder);
                        aggregation.setStatus(KIZAggregationStatus.DOWNLOADED);
                    }
                }
                kizAggregationDao.saveAll(aggregations, true);

                String gln = kizOrder.getDepartment().getGln();
                Long lastBox = kizAggregationDao.findLastByGln(gln);
                long boxNum = lastBox == null ? 0 : lastBox + 1;
                while (i < quantity) {
                    printer.printRecord(i++, generateAggregationCode(kizOrder, gln, boxNum++));
                }
            }
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

    private String generateAggregationCode(KIZOrder kizOrder, String gln, long boxNum) {
        KIZAggregation kizAggregation = new KIZAggregation();
        kizAggregation.setStatus(KIZAggregationStatus.DOWNLOADED);
        kizAggregation.setKizOrder(kizOrder);
        kizAggregation.setBoxNum(boxNum);
        StringBuilder sscc = new StringBuilder(String.valueOf(kizAggregation.getBoxNum()));
        while (sscc.length() < 7) {
            sscc.insert(0, "0");
        }
        sscc.insert(0, gln.substring(0, 9)).insert(0, "0");

        int sum = ((Character.digit(sscc.charAt(0), 10) +
                Character.digit(sscc.charAt(2), 10) +
                Character.digit(sscc.charAt(4), 10) +
                Character.digit(sscc.charAt(6), 10) +
                Character.digit(sscc.charAt(8), 10) +
                Character.digit(sscc.charAt(10), 10) +
                Character.digit(sscc.charAt(12), 10) +
                Character.digit(sscc.charAt(14), 10) +
                Character.digit(sscc.charAt(16), 10)) * 3 +
                Character.digit(sscc.charAt(1), 10) +
                Character.digit(sscc.charAt(3), 10) +
                Character.digit(sscc.charAt(5), 10) +
                Character.digit(sscc.charAt(7), 10) +
                Character.digit(sscc.charAt(9), 10) +
                Character.digit(sscc.charAt(11), 10) +
                Character.digit(sscc.charAt(13), 10) +
                Character.digit(sscc.charAt(15), 10)) % 10;
        sscc.append(sum == 0 ? 0 : 10 - sum);

        kizAggregation.setSscc(sscc.toString());
        kizAggregation = kizAggregationDao.save(kizAggregation);
        return kizAggregation.getSscc();
    }

    public Response aggregateKIZOrder(Long orderId) {
        KIZOrder kizOrder = kizOrderDao.getById(orderId);
        for (KIZAggregation kizAggregation : kizOrder.getAggregations()) {
            if (KIZAggregationStatus.READY.equals(kizAggregation.getStatus()))
                kizAggregationService.createAggregation(kizAggregation.getSscc());
        }
        return new Response(Status.OK);
    }

    public Response cleanAggregation(String sscc) {
        String docId = kizAggregationService.cleanAggregation(kizAggregationService.getById(sscc));
        return new Response(Status.OK, docId);
    }
}
