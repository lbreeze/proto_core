package ru.v6.mark.prototype.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.v6.mark.prototype.service.KIZAggregationService;
import ru.v6.mark.prototype.service.KIZMarkService;

@Component
public class GetStatusDocumentJobFacade extends JobFacade {

    @Autowired
    KIZMarkService kizMarkService;

    @Autowired
    KIZAggregationService kizAggregationService;

    @Override
    public void doJob() {
        kizMarkService.getStatusDocument();
        kizAggregationService.getStatusAggregation();
    }

}
