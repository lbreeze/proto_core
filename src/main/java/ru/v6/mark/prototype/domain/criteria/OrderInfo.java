package ru.v6.mark.prototype.domain.criteria;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderInfo {
    private String sortField = null;
    private String sortDir = "asc";

    /**
     * Creates a new sort field instance.
     */
    public OrderInfo() {

    }

    /**
     * Creates a new sort info instance.
     *
     * @param field the sort field
     * @param sortDir the sort direction
     */
    public OrderInfo(String field, String sortDir) {
        this.sortField = field;
        this.sortDir = sortDir;
    }

    /**
     * Returns the sort field.
     *
     * @return the sort field
     */
    public String getSortField() {
        return sortField;
    }

    /**
     * Sets the sort field.
     *
     * @param sortField the sort field
     */
    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    /**
     * Returns the sort direction.
     *
     * @return the sort direction
     */
    public String getSortDir() {
        return sortDir;
    }

    /**
     * Sets the sort direction.
     *
     * @param sortDir the sort direction
     */
    public void setSortDir(String sortDir) {
        this.sortDir = sortDir;
    }

    @Override
    public String toString() {
        return "OrderInfo{" +
                "sortField='" + sortField + '\'' +
                ", sortDir='" + sortDir + '\'' +
                '}';
    }
}
