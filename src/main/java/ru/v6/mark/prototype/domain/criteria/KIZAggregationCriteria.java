package ru.v6.mark.prototype.domain.criteria;

import com.fasterxml.jackson.annotation.JsonInclude;
import ru.v6.mark.prototype.domain.constant.KIZAggregationStatus;
import ru.v6.mark.prototype.domain.entity.KIZOrder;

import java.util.List;

/**
 * Created by Michael on 21.12.2019.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KIZAggregationCriteria extends PagingCriteria {

    private KIZOrder kizOrder;
    private String gln;
    private KIZAggregationStatus status;


    private List<KIZAggregationStatus> statuses;

    public KIZOrder getKizOrder() {
        return kizOrder;
    }

    public void setKizOrder(KIZOrder kizOrder) {
        this.kizOrder = kizOrder;
    }

    public String getGln() {
        return gln;
    }

    public void setGln(String gln) {
        this.gln = gln;
    }

    public KIZAggregationStatus getStatus() {
        return status;
    }

    public void setStatus(KIZAggregationStatus status) {
        this.status = status;
    }

    public List<KIZAggregationStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<KIZAggregationStatus> statuses) {
        this.statuses = statuses;
    }
}
