package ru.v6.mark.prototype.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.criteria.DepartmentCriteria;
import ru.v6.mark.prototype.domain.dao.BaseCriteriaDao;
import ru.v6.mark.prototype.domain.dao.DepartmentDao;
import ru.v6.mark.prototype.domain.dao.OrganizationDao;
import ru.v6.mark.prototype.domain.entity.Department;

import java.util.List;

@Service
public class DepartmentService extends EntityCriteriaService<Department, DepartmentCriteria> {

    @Autowired
    DepartmentDao departmentDao;
    @Autowired
    OrganizationDao organizationDao;

    @Override
    protected BaseCriteriaDao<Department, DepartmentCriteria> getPrimaryCriteriaDao() {
        return departmentDao;
    }

    public List<Department> findAll() {
        return departmentDao.findAll("code");
    }

}
