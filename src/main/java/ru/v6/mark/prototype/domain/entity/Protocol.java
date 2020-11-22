package ru.v6.mark.prototype.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import ru.v6.mark.prototype.domain.constant.ProtocolOperation;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "protocol")
@BatchSize(size = 100)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Protocol extends BaseEntity implements Comparable {

    @Id
    @SequenceGenerator(name = "SEQ_PROTOCOL", sequenceName = "protocol_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PROTOCOL")
    private Long id;

    @Enumerated(EnumType.STRING)
    private ProtocolOperation operation;

    private String action;

    private String entity;
    @Column(name = "entity_id")
    private String entityId;

    private String username;

    @NotFound(action = NotFoundAction.IGNORE)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "username", insertable = false, updatable = false)
    private User user;

    @Column(name = "external_link")
    private String externalLink = null;

    @Transient
    private String error;

    @Transient
    private String changeLog = "";

    @Transient
    private String comment = null;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProtocolOperation getOperation() {
        return operation;
    }

    public void setOperation(ProtocolOperation operation) {
        this.operation = operation;
    }

    public String getAction() {
        if (getError() != null && !getError().isEmpty())
            return action + " С ошибками: " + getError();
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getExternalLink() {
        return externalLink;
    }

    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(String changeLog) {
        this.changeLog = changeLog;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public int compareTo(Object o) {
        Protocol protocol = (Protocol) o;
        return protocol.getLastUpdate() != null ? getLastUpdate() != null ? getLastUpdate().compareTo(protocol.getLastUpdate()) : -1 : 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Protocol)) return false;

        Protocol protocol = (Protocol) o;

        if (getAction() != null ? !getAction().equals(protocol.getAction()) : protocol.getAction() != null) return false;
        //if (getEntity() != null ? !getEntity().equals(protocol.getEntity()) : protocol.getEntity() != null) return false;
        //if (getEntityId() != null ? !getEntityId().equals(protocol.getEntityId()) : protocol.getEntityId() != null) return false;
        if (getLastUpdate() != null ? !equalsLastUpdate(protocol.getLastUpdate()) : protocol.getLastUpdate() != null) return false;
        return getUsername() == null || protocol.getUsername() == null || getUsername().equals(protocol.getUsername());
    }

    private boolean equalsLastUpdate(Date lastUpdate) {

        long delta = Math.abs(getLastUpdate().getTime() - (lastUpdate == null ? 0L : lastUpdate.getTime()));
        return delta < 100;
    }

    @Override
    public int hashCode() {
        int result = getAction() != null ? getAction().hashCode() : 0;
        //result = 31 * result + (getEntity() != null ? getEntity().hashCode() : 0);
        //result = 31 * result + (getEntityId() != null ? getEntityId().hashCode() : 0);
        result = 31 * result + (getLastUpdate() != null ? (int)getLastUpdate().getTime()/86400000 : 0);
//        result = 31 * result + (getUser() != null ? getUser().hashCode() : 0);
        return result;
    }

    public Protocol entity(String entity) {
        this.entity = entity;
        return this;
    }

    public Protocol entityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public Protocol action(String action) {
        this.action = action;
        return this;
    }

    public Protocol username(String username) {
        this.username = username;
        return this;
    }

    @Override
    public String toString() {
        return "Protocol{" +
                "id=" + id +
                ", operation=" + operation +
                ", action='" + action + '\'' +
                ", entity='" + entity + '\'' +
                ", entityId=" + entityId +
                ", username=" + username +
                ", externalLink='" + externalLink + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}
