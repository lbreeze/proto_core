package ru.v6.mark.prototype.domain.criteria;

import com.fasterxml.jackson.annotation.JsonInclude;
import ru.v6.mark.prototype.domain.constant.MarkSubType;
import ru.v6.mark.prototype.domain.entity.Organization;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GtinCriteria extends PagingCriteria {


    private MarkSubType markSubType;
    private Boolean imported;
    private Organization organization;
    private String ean;

    public MarkSubType getMarkSubType() {
        return markSubType;
    }

    public void setMarkSubType(MarkSubType markSubType) {
        this.markSubType = markSubType;
    }

    public Boolean getImported() {
        return imported;
    }

    public void setImported(Boolean imported) {
        this.imported = imported;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }

}
