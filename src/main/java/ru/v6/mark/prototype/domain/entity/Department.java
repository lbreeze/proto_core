package ru.v6.mark.prototype.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import ru.v6.mark.prototype.domain.constant.DepartmentType;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "department")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Department extends DeletableEntity {

    @Id
    private String code;

    @Column
    private String name;

    @JoinColumn(name = "organization")
    @ManyToOne(fetch = FetchType.EAGER)
    private Organization organization;

    private String address;

    @Column(name = "dept_type")
    @Enumerated(EnumType.STRING)
    private DepartmentType departmentType;

    private String gln;

    @Column(name = "key_alias")
    private String keyAlias;

    @ElementCollection
    @Column(name = "saas")
    @CollectionTable(name="department_saas", joinColumns=@JoinColumn(name="code"))
    private List<String> saasCodes;

    @Override
    public Serializable getId() {
        return code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public DepartmentType getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(DepartmentType departmentType) {
        this.departmentType = departmentType;
    }

    public String getGln() {
        return gln;
    }

    public void setGln(String gln) {
        this.gln = gln;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public List<String> getSaasCodes() {
        return saasCodes;
    }

    public void setSaasCodes(List<String> saasCodes) {
        this.saasCodes = saasCodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Department that = (Department) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), code);
    }
}
