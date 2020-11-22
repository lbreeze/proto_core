package ru.v6.mark.prototype.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.dao.BaseDao;
import ru.v6.mark.prototype.domain.dao.RoleDao;
import ru.v6.mark.prototype.domain.entity.Role;

import java.util.List;

@Service
public class RoleService extends EntityService<Role> {

    @Autowired
    RoleDao roleDao;

    @Override
    protected BaseDao<Role> getPrimaryDao() {
        return roleDao;
    }

    public List<Role> findAll() {
        return roleDao.findAll("id");
    }

}
