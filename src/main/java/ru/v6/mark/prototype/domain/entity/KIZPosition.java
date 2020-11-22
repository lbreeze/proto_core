package ru.v6.mark.prototype.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.validator.constraints.Length;
import ru.v6.mark.prototype.domain.constant.KIZPositionStatus;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "kiz_position")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KIZPosition extends DeletableEntity {

    @Id
    @GeneratedValue(generator = "KIZ_POSITION_SEQ", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "KIZ_POSITION_SEQ", sequenceName = "kiz_position_id_seq", initialValue = 1, allocationSize = 1)
    private Long id;

    @Column(name = "id_order")
    private String orderId;

    @JsonIgnore
    @NotFound(action = NotFoundAction.IGNORE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ean", insertable = false, updatable = false)
    private Goods goods;

    @JsonIgnore
    @NotFound(action = NotFoundAction.IGNORE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ean", insertable = false, updatable = false)
    private Gtin gtin;

    @Column(name = "ean")
    private String ean;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article", insertable = false, updatable = false)
    private Article articleItem;

    @Column(name = "article")
    private Integer article;

    private Integer quantity;

    @Transient
    private Integer scanned;

    @Enumerated(EnumType.STRING)
    private KIZPositionStatus status;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kiz_order")
    private KIZOrder kizOrder;

    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "position")
    private List<KIZMark> marks;

    @Column(name = "doc_id")
    private String docId;

    @Column(name = "status_desc")
    private String statusDesc;

    @Length(max = 10)
    @Column(name = "tnved")
    private String tnved;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Goods getGoods() {
        return goods;
    }

    public void setGoods(Goods goods) {
        this.goods = goods;
    }

    public Gtin getGtin() {
        return gtin;
    }

    public void setGtin(Gtin gtin) {
        this.gtin = gtin;
    }

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getScanned() {
        return scanned;
    }

    public void setScanned(Integer scanned) {
        this.scanned = scanned;
    }

    public KIZPositionStatus getStatus() {
        return status;
    }

    public void setStatus(KIZPositionStatus status) {
        this.status = status;
    }

    public KIZOrder getKizOrder() {
        return kizOrder;
    }

    public void setKizOrder(KIZOrder kizOrder) {
        this.kizOrder = kizOrder;
    }

    public List<KIZMark> getMarks() {
        return marks;
    }

    public void setMarks(List<KIZMark> marks) {
        this.marks = marks;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getStatusDesc() {
        return statusDesc;
    }

    public void setStatusDesc(String statusDesc) {
        this.statusDesc = statusDesc;
    }

    public String getTnved() {
        return tnved;
    }

    public void setTnved(String tnved) {
        this.tnved = tnved;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        KIZPosition position = (KIZPosition) o;
        return Objects.equals(id, position.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }
}
