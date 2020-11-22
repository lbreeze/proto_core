package ru.v6.mark.prototype.domain.dao;

import org.springframework.stereotype.Repository;
import ru.v6.mark.prototype.domain.criteria.ValidateMarksLogCriteria;
import ru.v6.mark.prototype.domain.entity.ValidateMarksLog;

import java.util.HashMap;
import java.util.Map;

@Repository
public class ValidateMarksLogDao extends BaseCriteriaDao<ValidateMarksLog, ValidateMarksLogCriteria> {

    @Override
    public String getCriteriaCondition(ValidateMarksLogCriteria criteria) {
        String result = "";

        return !result.isEmpty() ? result.replaceFirst(" and ", " where ") : "";
    }

    @Override
    public Map<String, Object> getCriteriaParams(ValidateMarksLogCriteria criteria) {
        Map<String, Object> result = new HashMap<>();


        return result;
    }
}
