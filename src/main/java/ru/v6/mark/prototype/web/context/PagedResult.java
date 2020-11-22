package ru.v6.mark.prototype.web.context;

import com.fasterxml.jackson.annotation.JsonInclude;
import ru.v6.mark.prototype.domain.criteria.PagingCriteria;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagedResult<T> extends PagingCriteria {
    private List<T> data;
    private Long count;

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
