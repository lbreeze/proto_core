package ru.v6.mark.prototype.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import ru.v6.mark.prototype.domain.constant.AcceptancePositionStatus;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "acceptance_position")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AcceptancePosition extends DeletableEntity {

    @Id
    @GeneratedValue(generator = "ACCEPTANCE_POSITION_SEQ", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "ACCEPTANCE_POSITION_SEQ", sequenceName = "acceptance_position_id_seq", initialValue = 1, allocationSize = 1)
    private Long id;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @NotFound(action = NotFoundAction.IGNORE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article", insertable = false, updatable = false)
    private Article articleItem;

    @Column(name = "article")
    private Integer article;

    @Column(name = "qty_supplied")
    private Integer quantitySupplied;
    @Column(name = "qty_accepted")
    private Integer quantityAccepted;
    @Column(name = "qty_infolog_ordered")
    private Integer quantityInfologOrdered;
    @Column(name = "qty_infolog_accepted")
    private Integer quantityInfologAccepted;
    @Column(name = "pcb_infolog")
    private Integer pcbInfolog;

    private Boolean acceptable;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "acceptance")
    private Acceptance acceptance;

    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @OneToMany(mappedBy = "acceptancePosition")//, cascade = {CascadeType.ALL}, orphanRemoval = true)
    private List<KIZAggregation> aggregations;

    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @OneToMany(mappedBy = "acceptancePosition")//, cascade = {CascadeType.ALL}, orphanRemoval = true)
    private List<KIZMark> marks;

    @Enumerated(EnumType.STRING)
    private AcceptancePositionStatus status;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Article getArticleItem() {
        return articleItem;
    }

    public void setArticleItem(Article articleItem) {
        this.articleItem = articleItem;
    }

    public Integer getArticle() {
        return article;
    }

    public void setArticle(Integer article) {
        this.article = article;
    }

    public Integer getQuantitySupplied() {
        return quantitySupplied;
    }

    public void setQuantitySupplied(Integer quantitySupplied) {
        this.quantitySupplied = quantitySupplied;
    }

    public Integer getQuantityAccepted() {
        return quantityAccepted;
    }

    public void setQuantityAccepted(Integer quantityAccepted) {
        this.quantityAccepted = quantityAccepted;
    }

    public Integer getQuantityInfologOrdered() {
        return quantityInfologOrdered;
    }

    public void setQuantityInfologOrdered(Integer quantityInfologOrdered) {
        this.quantityInfologOrdered = quantityInfologOrdered;
    }

    public Integer getQuantityInfologAccepted() {
        return quantityInfologAccepted;
    }

    public void setQuantityInfologAccepted(Integer quantityInfologAccepted) {
        this.quantityInfologAccepted = quantityInfologAccepted;
    }

    public Integer getPcbInfolog() {
        return pcbInfolog;
    }

    public void setPcbInfolog(Integer pcbInfolog) {
        this.pcbInfolog = pcbInfolog;
    }

    public Boolean getAcceptable() {
        return acceptable;
    }

    public void setAcceptable(Boolean acceptable) {
        this.acceptable = acceptable;
    }

    public Acceptance getAcceptance() {
        return acceptance;
    }

    public void setAcceptance(Acceptance acceptance) {
        this.acceptance = acceptance;
    }

    public List<KIZAggregation> getAggregations() {
        return aggregations;
    }

    public void setAggregations(List<KIZAggregation> aggregations) {
        this.aggregations = aggregations;
    }

    public List<KIZMark> getMarks() {
        return marks;
    }

    public void setMarks(List<KIZMark> marks) {
        this.marks = marks;
    }

    public AcceptancePositionStatus getStatus() {
        return status;
    }

    public void setStatus(AcceptancePositionStatus status) {
        this.status = status;
    }
}
