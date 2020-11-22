package ru.v6.mark.prototype.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import ru.v6.mark.prototype.domain.constant.Privilege;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "roles")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Role extends BaseEntity {

    @Id
    @GeneratedValue(generator = "ROLE_SEQ", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "ROLE_SEQ", sequenceName = "roles_id_seq", initialValue = 1, allocationSize = 1)
    private Long id;

    private String description;

    @ElementCollection
    @Column(name = "privilege")
    @Enumerated(EnumType.STRING)
    @CollectionTable(name="role_privileges", joinColumns=@JoinColumn(name="role_id"))
    private List<Privilege> privileges;

    @Override
    public Serializable getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Privilege> getPrivileges() {
        return privileges;
    }

    public void setPrivileges(List<Privilege> privileges) {
        this.privileges = privileges;
    }

}
