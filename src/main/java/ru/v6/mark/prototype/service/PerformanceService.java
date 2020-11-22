package ru.v6.mark.prototype.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.entity.Performance;
import ru.v6.mark.prototype.domain.dao.PerformanceDao;

import java.util.List;
import java.util.Map;

@Service
public class PerformanceService extends BaseService {
    @Autowired
    PerformanceDao performanceDao;

    public List executeSelect(String sql, Map<String, Object> params) {
        return performanceDao.executeSelect(sql, params);
    }

    public void save(Performance performance) {
        performanceDao.save(performance);
    }

}
