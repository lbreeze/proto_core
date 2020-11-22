package ru.v6.mark.prototype.domain.criteria;

import com.fasterxml.jackson.annotation.JsonInclude;
import ru.v6.mark.prototype.domain.constant.KIZMarkStatus;

/**
 * Created by Michael on 21.12.2019.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KIZMarkCriteria extends PagingCriteria {

    private KIZMarkStatus status;

    public KIZMarkStatus getStatus() {
        return status;
    }

    public void setStatus(KIZMarkStatus status) {
        this.status = status;
    }
}
