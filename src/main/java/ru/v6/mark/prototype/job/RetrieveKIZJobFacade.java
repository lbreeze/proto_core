package ru.v6.mark.prototype.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.v6.mark.prototype.domain.constant.KIZOrderStatus;
import ru.v6.mark.prototype.domain.criteria.KIZOrderCriteria;
import ru.v6.mark.prototype.domain.entity.KIZOrder;
import ru.v6.mark.prototype.service.KIZOrderService;

import java.util.List;

@Component
public class RetrieveKIZJobFacade extends JobFacade {

    @Autowired
    private KIZOrderService kizOrderService;

    @Override
    public void doJob() {
        KIZOrderCriteria criteria = new KIZOrderCriteria();
        criteria.setStatus(KIZOrderStatus.SENT);
        List<KIZOrder> orders = kizOrderService.findByCriteria(criteria);
        for (KIZOrder order : orders) {
            kizOrderService.retrieveKIZ(order.getId());
        };
    }

}
