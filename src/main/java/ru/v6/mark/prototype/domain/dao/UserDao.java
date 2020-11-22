package ru.v6.mark.prototype.domain.dao;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Repository;
import ru.v6.mark.prototype.domain.criteria.UserCriteria;
import ru.v6.mark.prototype.domain.entity.User;

import java.util.HashMap;
import java.util.Map;

@Repository
public class UserDao extends BaseCriteriaDao<User, UserCriteria> {

    @Override
    public String getCriteriaCondition(UserCriteria criteria) {
        String result = "";
        if (criteria.getLogin() != null) {
            result += " and user.username like :login";
        }

        if (criteria.getRegistered() != null) {
            result += " and user.registered >= :date and user.registered < :nextdate";
        }
        if (criteria.getRole() != null) {
            result += " and user.role.id = :role";
        }
        if (criteria.getDepartment() != null) {
            result += " and (from Department where code = :department) member of user.departments";
        } else if (criteria.getAllDepartments() != null) {
            result += " and user.departments in :allDepartments";
        }        return !result.isEmpty()? result.replaceFirst(" and ", " where ") : "";
    }

    @Override
    public Map<String, Object> getCriteriaParams(UserCriteria criteria) {
        Map<String, Object> result = new HashMap<>();
        if (criteria.getLogin() != null) {
            result.put("login", "%" + criteria.getLogin().toUpperCase() + "%");
        }
        if (criteria.getRegistered() != null) {
            result.put("date", criteria.getRegistered());
            result.put("nextdate", DateUtils.addDays(criteria.getRegistered(), 1));
        }

        if (criteria.getRole() != null) {
            result.put("role", criteria.getRole());
        }

        if (criteria.getDepartment() != null) {
            result.put("department", criteria.getDepartment());
        } else if (criteria.getAllDepartments() != null) {
            result.put("allDepartments", criteria.getAllDepartments());
        }
        return result;
    }
}
