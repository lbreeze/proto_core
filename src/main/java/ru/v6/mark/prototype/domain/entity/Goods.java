package ru.v6.mark.prototype.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "goods")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Goods extends DeletableEntity {

    public static final int EAN_MAX_LENGTH = 14;

    @Id
    private String ean;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article", insertable = false, updatable = false)
    private Article articleItem;

    private Integer article;

    private String name;

    @Column(name = "producer_inn")
    private String producerInn;
    @Column(name = "producer_name")
    private String producerName;
    @Column(name = "producer_country")
    private String producerCountry;

    @Column(name = "crpt_identity")
    private Long crptIdentity;
    @Column(name = "crpt_group")
    private String crptGroup;
    @Column(name = "crpt_type")
    private String crptType;

    @ElementCollection
    @MapKeyColumn(name="attr_name")
    @Column(name="attr_value")
    @CollectionTable(name="goods_attr", joinColumns=@JoinColumn(name="ean"))
    private Map<String, String> attributes = new HashMap<String, String>();
    
    @Override
    public Serializable getId() {
        return ean;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }

    public String getProducerInn() {
        return producerInn;
    }

    public void setProducerInn(String producerInn) {
        this.producerInn = producerInn;
    }

    public String getProducerName() {
        return producerName;
    }

    public void setProducerName(String producerName) {
        this.producerName = producerName;
    }

    public String getProducerCountry() {
        return producerCountry;
    }

    public void setProducerCountry(String producerCountry) {
        this.producerCountry = producerCountry;
    }

    public Long getCrptIdentity() {
        return crptIdentity;
    }

    public void setCrptIdentity(Long crptIdentity) {
        this.crptIdentity = crptIdentity;
    }

    public String getCrptGroup() {
        return crptGroup;
    }

    public void setCrptGroup(String crptGroup) {
        this.crptGroup = crptGroup;
    }

    public String getCrptType() {
        return crptType;
    }

    public void setCrptType(String crptType) {
        this.crptType = crptType;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Goods goods = (Goods) o;
        return Objects.equals(ean, goods.ean);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ean);
    }
}
