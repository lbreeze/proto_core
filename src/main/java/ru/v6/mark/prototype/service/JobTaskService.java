package ru.v6.mark.prototype.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.constant.HasProtocol;
import ru.v6.mark.prototype.domain.constant.JobType;
import ru.v6.mark.prototype.domain.constant.ProtocolIdentifier;
import ru.v6.mark.prototype.domain.dao.BaseDao;
import ru.v6.mark.prototype.domain.dao.JobTaskDao;
import ru.v6.mark.prototype.domain.entity.JobTask;
import ru.v6.mark.prototype.exception.ApplicationException;
import ru.v6.mark.prototype.job.JobScheduler;
import ru.v6.mark.prototype.web.context.Response;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class JobTaskService extends EntityService<JobTask> {

    @Autowired
    JobTaskDao jobTaskDao;
    @Autowired
    private JobScheduler jobScheduler;

    @Override
    protected BaseDao<JobTask> getPrimaryDao() {
        return jobTaskDao;
    }

    public List<JobTask> findAll() {
        return jobTaskDao.findAll("id");
    }

    public JobTask getByType(JobType jobType) {
        return jobTaskDao.getByType(jobType);
    }

    @HasProtocol(action = "Задача выполнена", identifier = ProtocolIdentifier.ENTITY)
    public Response runTask(JobTask jobTask) {
        try {
            return new Response(jobScheduler.runJobTask(jobTask.getJobType()), "Задача \"" + jobTask.getJobType() + "\" выполнена.");
        } catch (ExecutionException | InterruptedException e) {
            throw ApplicationException.build(e, "Ошибка запуска задачи.");
        }
    }

    @Override
    public JobTask save(JobTask entity) {
        JobTask jobTask = getById(entity.getId());
        jobTask.setInitial(entity.getInitial());
        jobTask.setFrequency(entity.getFrequency());
        jobTask = jobTaskDao.save(jobTask);
        jobScheduler.reschedule(jobTask);
        return jobTask;
    }

}
