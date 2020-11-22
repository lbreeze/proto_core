package ru.v6.mark.prototype.service.importer;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.constant.KIZMarkStatus;
import ru.v6.mark.prototype.domain.constant.KIZOrderStatus;
import ru.v6.mark.prototype.domain.constant.KIZPositionStatus;
import ru.v6.mark.prototype.domain.dao.KIZOrderDao;
import ru.v6.mark.prototype.domain.dao.KIZPositionDao;
import ru.v6.mark.prototype.domain.entity.*;
import ru.v6.mark.prototype.service.CachedDataReceiver;
import ru.v6.mark.prototype.service.ClientService;
import ru.v6.mark.prototype.service.GoodsService;
import ru.v6.mark.prototype.service.converter.ProductConverter;
import ru.v6.mark.prototype.service.util.JSONUtil;
import ru.v6.mark.prototype.service.util.ResultError;

import javax.validation.constraints.NotNull;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class KIZOrderImportService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    ClientService clientService;

    @Autowired
    CachedDataReceiver cachedDataReceiver;

    @Autowired
    ProductConverter productConverter;

    @Autowired
    GoodsService goodsService;

    @Autowired
    KIZOrderDao kizOrderDao;

    @Autowired
    KIZPositionDao kizPositionDao;

    public void sendKIZOrder(String aliasCode,
                             @NotNull Organization organization,
                             KIZOrder entity,
                             String user,
                             boolean isWaitPosition) {
        Map<String, String> params = new HashMap<>();
        params.put("omsId", organization.getOmsId());
        List<KIZPosition> positions = isWaitPosition ? entity.getPositions().parallelStream().filter(p -> p.getStatus() != null && p.getStatus().equals(KIZPositionStatus.WAIT_SENDING)).collect(Collectors.toList()) : entity.getPositions();
        positions.forEach(kizPosition -> {
            Map<String, Integer> gtins = new HashMap<>();
            gtins.put(kizPosition.getEan(), kizPosition.getQuantity());
            Calendar cDate = Calendar.getInstance();
            cDate.add(Calendar.MONTH, 2);
            String contractDate = new SimpleDateFormat("yyyy-MM-dd").format(cDate.getTime());
            ResultError resultErrorPosition = new ResultError();
            String orderId;
            try {
                orderId = clientService.createOrder(aliasCode, organization.getToken(), params, user, contractDate, entity.getOrderType().name(), gtins, String.valueOf(entity.getId()), resultErrorPosition);
                if (orderId != null) {
                    kizPosition.setOrderId(orderId);
                    kizPosition.setStatus(KIZPositionStatus.ORDER_OK);
                } else {
                    if (resultErrorPosition.getDescription().contains("Количество активных заказов не может превышать")) {
                        kizPosition.setStatus(KIZPositionStatus.WAIT_SENDING);
                    } else {
                        kizPosition.setStatus(KIZPositionStatus.ERROR_CUZ);
                    }
                    kizPosition.setStatusDesc(resultErrorPosition.getDescription());
                    entity.setStatus(KIZOrderStatus.ERROR);
                    kizPositionDao.save(kizPosition);
                }
            } catch (Exception e) {
                entity.setStatus(KIZOrderStatus.ERROR);
                kizPosition.setStatus(KIZPositionStatus.ERROR_CUZ);
                logger.error("Error create order position: ", e.getMessage());
            }
        });
        List<KIZPosition> errorPositions = positions.stream().filter(kizPosition -> kizPosition.getStatus() != null && !kizPosition.getStatus().equals(KIZPositionStatus.ORDER_OK)).collect(Collectors.toList());

        if (errorPositions.size() == 0) {
            entity.setStatus(KIZOrderStatus.SENT);
            entity.setSent(new Date());
        }

    }

    public boolean retrieveKIZ(String aliasCode, @NotNull Organization organization, KIZOrder entity) {
        AtomicBoolean changed = new AtomicBoolean(false);

        Map<String, String> params = new HashMap<>();
        params.put("omsId", organization.getOmsId());
        try {
            entity.getPositions().stream().filter(kizPosition -> !KIZPositionStatus.RECEIVED.equals(kizPosition.getStatus())).forEach(kizPosition -> {
                params.put("orderId", kizPosition.getOrderId());
                params.put("gtin", kizPosition.getEan());
                params.put("quantity", String.valueOf(kizPosition.getQuantity()));

                String km = clientService.getCodes(aliasCode, organization.getToken(), params);

                JSONArray array = JSONUtil.getArray("codes", km);
                if (array != null) {
                    if (kizPosition.getMarks() == null)
                        kizPosition.setMarks(new ArrayList<>());

                    for (int i = 0; i < array.length(); i++) {
                        KIZMark mark = new KIZMark();
                        mark.setMark(array.getString(i));
                        mark.setStatus(KIZMarkStatus.RECEIVED);
                        mark.setPosition(kizPosition);
                        kizPosition.getMarks().add(mark);
                    }
                    kizPosition.setStatus(KIZPositionStatus.RECEIVED);
                    closeOrder(kizPosition);
                    changed.set(true);
//                } else {
//                    entity.setStatus(KIZOrderStatus.ERROR);
//                    kizOrderDao.save(entity);
                }
            });
        } catch (Exception e) {
            entity.setStatus(KIZOrderStatus.ERROR);
            kizOrderDao.save(entity);
        }
        return changed.get();
    }

    public List<KIZMark> retrieveKIZ(KIZPosition kizPosition, int quantity) {
        //boolean changed = false;
        List<KIZMark> marks = new ArrayList<>();

        Department department = kizPosition.getKizOrder().getDepartment();
        Map<String, String> params = new HashMap<>();
        params.put("omsId", department.getOrganization().getOmsId());
        try {
                params.put("orderId", kizPosition.getOrderId());
                params.put("gtin", kizPosition.getEan());
                params.put("quantity", String.valueOf(quantity));
                // fake only
                params.put("available", String.valueOf(kizPosition.getQuantity() - kizPosition.getMarks().size()));

                String km = clientService.getCodes(department.getKeyAlias(), department.getOrganization().getToken(), params);

                JSONArray array = JSONUtil.getArray("codes", km);
                if (array != null) {
                    if (kizPosition.getMarks() == null)
                        kizPosition.setMarks(new ArrayList<>());

                    for (int i = 0; i < array.length(); i++) {
                        KIZMark mark = new KIZMark();
                        mark.setMark(array.getString(i));
                        mark.setStatus(KIZMarkStatus.RECEIVED);
                        mark.setPosition(kizPosition);
                        kizPosition.getMarks().add(mark);

                        marks.add(mark);
                    }
                    if (kizPosition.getQuantity() == kizPosition.getMarks().size()) {
                        kizPosition.setStatus(KIZPositionStatus.RECEIVED);
                        closeOrder(kizPosition);
                    }
                    //changed = true;
//                } else {
//                    entity.setStatus(KIZOrderStatus.ERROR);
//                    kizOrderDao.save(entity);
                }
        } catch (Exception e) {
            kizPosition.setStatus(KIZPositionStatus.ERROR);
            kizPositionDao.save(kizPosition);
        }
        return marks;
    }

    public void closeOrder(KIZPosition kizPosition) {
        Map<String, String> params = new HashMap<>();
        params.put("omsId", kizPosition.getKizOrder().getDepartment().getOrganization().getOmsId());
        params.put("gtin", kizPosition.getEan());
        params.put("orderId", kizPosition.getOrderId());

        try {
            clientService.closeOrder(kizPosition.getKizOrder().getDepartment().getKeyAlias(), kizPosition.getKizOrder().getDepartment().getOrganization().getToken(),  params);
        } catch (Exception e) {
            logger.error("Error close order: ", e);
        }
    }
}
