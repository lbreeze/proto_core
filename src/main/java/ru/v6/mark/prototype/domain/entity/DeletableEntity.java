package ru.v6.mark.prototype.domain.entity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class DeletableEntity extends BaseEntity {

    @Column(name="deleted")
    protected Boolean deleted = Boolean.FALSE;

    protected DeletableEntity() {
        super();
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
