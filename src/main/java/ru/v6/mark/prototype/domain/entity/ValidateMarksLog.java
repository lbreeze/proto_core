package ru.v6.mark.prototype.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "validate_marks_log")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidateMarksLog extends BaseEntity {

    @Id
    @GeneratedValue(generator = "VALIDATE_MARKS_LOG_SEQ", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "VALIDATE_MARKS_LOG_SEQ", sequenceName = "validate_marks_log_id_seq", initialValue = 1, allocationSize = 1)
    private Long id;

    private String username;
    private String ean;
    private Integer quantity;

    @Override
    public Serializable getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
