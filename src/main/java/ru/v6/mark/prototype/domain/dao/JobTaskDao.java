package ru.v6.mark.prototype.domain.dao;

import org.springframework.stereotype.Repository;
import ru.v6.mark.prototype.domain.constant.JobType;
import ru.v6.mark.prototype.domain.entity.JobTask;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

@Repository
public class JobTaskDao extends BaseDao<JobTask> {

    public JobTask getByType(JobType jobType) {
        return findUnique(new QueryBuilder<JobTask>() {
            @Override
            protected TypedQuery<JobTask> doBuildQuery(EntityManager entityManager) {
                TypedQuery<JobTask> result = entityManager.createQuery("from JobTask jobTask where jobTask.jobType = :jobType", JobTask.class);
                result.setParameter("jobType", jobType);
                return result;
            }
        }, true);
    }
}
