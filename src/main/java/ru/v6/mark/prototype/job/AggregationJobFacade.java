package ru.v6.mark.prototype.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.v6.mark.prototype.domain.entity.KIZAggregation;
import ru.v6.mark.prototype.service.KIZAggregationService;

import java.util.List;

@Component
public class AggregationJobFacade extends JobFacade {

    @Autowired
    KIZAggregationService kizAggregationService;

    @Override
    public void doJob() {
        List<KIZAggregation> aggregations = kizAggregationService.findAggregation();
        aggregations.parallelStream().forEach(aggregation -> {
            kizAggregationService.createAggregation(aggregation.getSscc());
        });
    }
}
