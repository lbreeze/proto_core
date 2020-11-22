package ru.v6.mark.prototype.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.dao.BaseDao;
import ru.v6.mark.prototype.domain.dao.OrganizationDao;
import ru.v6.mark.prototype.domain.entity.Organization;

import java.util.List;

@Service
public class OrganizationService extends EntityService<Organization> {

    @Autowired
    OrganizationDao organizationDao;

    @Override
    protected BaseDao<Organization> getPrimaryDao() {
        return organizationDao;
    }

    public List<Organization> findAll() {
        return organizationDao.findAll("code");
    }

}
