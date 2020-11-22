package ru.v6.mark.prototype.job;

import ru.v6.mark.prototype.domain.constant.JobType;
import ru.v6.mark.prototype.domain.constant.Status;
import ru.v6.mark.prototype.domain.entity.JobTask;

import java.util.concurrent.ExecutionException;

public interface JobScheduler {
    void reschedule(JobTask jobTask);
    Status runJobTask(JobType jobType) throws ExecutionException, InterruptedException;
}
