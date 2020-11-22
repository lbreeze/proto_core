package ru.v6.mark.prototype.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.criteria.UserCriteria;
import ru.v6.mark.prototype.domain.dao.BaseCriteriaDao;
import ru.v6.mark.prototype.domain.dao.RoleDao;
import ru.v6.mark.prototype.domain.dao.UserDao;
import ru.v6.mark.prototype.domain.entity.User;

@Service
public class UserService extends EntityCriteriaService<User, UserCriteria> {

    @Autowired
    UserDao userDao;
    @Autowired
    RoleDao roleDao;

    @Override
    protected BaseCriteriaDao<User, UserCriteria> getPrimaryCriteriaDao() {
        return userDao;
    }

    @Override
    public User save(User entity) {
        entity.setUsername(entity.getUsername().toUpperCase());
        User target;
        if (entity.getId() == null || entity.getLastUpdate() == null) {
            target = entity;
        } else {
            target = getPrimaryDao().getById(entity.getId());
        }
        getPrimaryDao().mergeEntity(entity, target);
        return getPrimaryDao().save(target);
    }
}
