package ru.v6.mark.prototype.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import ru.v6.mark.prototype.domain.constant.MarkSubType;
import ru.v6.mark.prototype.domain.constant.MarkType;
import ru.v6.mark.prototype.domain.constant.Measure;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "article")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Article extends DeletableEntity {

    @Id
    private Integer article;
    private String name;

    private String market;
    private String segment;
    private String category;
    private String family;

    @Enumerated(EnumType.STRING)
    private Measure measure;
    private Integer pcb;

    @Column(name = "valid_period")
    private Integer validPeriod;

    @Enumerated(EnumType.STRING)
    @Column(name = "mark_type")
    private MarkType markType;

    @Enumerated(EnumType.STRING)
    @Column(name = "mark_sub_type")
    private MarkSubType markSubType;

    private Boolean imported;

    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @OneToMany(mappedBy = "articleItem")
    private List<Goods> goods;

    @Transient
    private boolean marked = false;

    @PostLoad
    public void postLoad() {
        marked = markType != null && markType.isMarking();
    }

    @Override
    public Serializable getId() {
        return article;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getArticle() {
        return article;
    }

    public void setArticle(Integer article) {
        this.article = article;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public Measure getMeasure() {
        return measure;
    }

    public void setMeasure(Measure measure) {
        this.measure = measure;
    }

    public Integer getPcb() {
        return pcb;
    }

    public void setPcb(Integer pcb) {
        this.pcb = pcb;
    }

    public MarkType getMarkType() {
        return markType;
    }

    public void setMarkType(MarkType markType) {
        this.markType = markType;
    }

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

    public List<Goods> getGoods() {
        return goods;
    }

    public void setGoods(List<Goods> goods) {
        this.goods = goods;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    public Integer getValidPeriod() {
        return validPeriod;
    }

    public void setValidPeriod(Integer validPeriod) {
        this.validPeriod = validPeriod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Article article1 = (Article) o;
        return Objects.equals(article, article1.article);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), article);
    }
}
