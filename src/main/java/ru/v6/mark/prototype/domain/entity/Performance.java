package ru.v6.mark.prototype.domain.entity;

import javax.persistence.*;

@Entity
@Table(name = "performance")
public class Performance extends BaseEntity {

    @Id
    @GeneratedValue(generator = "PERFORMANCE_SEQ", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "PERFORMANCE_SEQ", sequenceName = "performance_id_seq", initialValue = 1, allocationSize = 1)
    private Long id;

    private String clazz;

    private String method;

    private String args;

    private String thread;

    private Long millis;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Long getMillis() {
        return millis;
    }

    public void setMillis(Long millis) {
        this.millis = millis;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

}
