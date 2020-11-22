package ru.v6.mark.prototype.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import ru.v6.mark.prototype.domain.constant.JobType;
import ru.v6.mark.prototype.domain.constant.Status;
import ru.v6.mark.prototype.domain.entity.JobTask;
import ru.v6.mark.prototype.exception.ApplicationException;
import ru.v6.mark.prototype.service.JobTaskService;

import java.util.List;

@Component
@Profile("!job")
public class JobRunner implements JobScheduler, ApplicationContextAware {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    protected JobTaskService jobTaskService;
    @Autowired
    @Qualifier("taskScheduler")
    protected ThreadPoolTaskScheduler taskScheduler;
    @Autowired
    protected ArticleImportJobFacade articleImportJob;
    @Autowired
    protected RetrieveKIZJobFacade retrieveKIZJob;
    @Autowired
    protected MarkToTurnJobFacade markToTurnJob;
    @Autowired
    protected GetStatusDocumentJobFacade getStatusDocumentJobFacade;
    @Autowired
    protected AggregationJobFacade aggregationJobFacade;
    @Autowired
    protected AcceptanceJobFacade acceptanceJobFacade;
    @Autowired
    protected OrderJobFacade orderJobFacade;

    static {
        try {
            //Class.forName("com.ibm.as400.access.AS400JDBCDriver", true, ClassUtils.getDefaultClassLoader());
            Class.forName("oracle.jdbc.driver.OracleDriver", true, ClassUtils.getDefaultClassLoader());
            Class.forName("org.postgresql.Driver", true, ClassUtils.getDefaultClassLoader());
        }
        catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Could not load JDBC driver class", ex);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        List<JobTask> jobTaskList = jobTaskService.findAll();
        for (JobTask jobTask : jobTaskList) {
            reschedule(jobTask);
        }
    }

    @Override
    public Status runJobTask(JobType jobType) {
        // todo trigger method at instance with job profile JobInitializer.reschedule
        Runnable task = getTaskOfType(jobType);
        if (task != null) {
            task.run();
        } else {
            throw ApplicationException.build("Невозможно запустить задачу. Обратитесь к разработчику.");
        }
        return Status.OK;
    }

    @Override
    public void reschedule(final JobTask jobTask) {
        // todo trigger method at instance with job profile JobInitializer.reschedule
    }

    private Runnable getTaskOfType(JobType jobType) {
        Runnable task = null;
        switch (jobType) {
            case IMPORT_GOODS:
                task = articleImportJob;
                break;
            case RETRIEVE_KIZ:
                task = retrieveKIZJob;
                break;
            case MARK_TO_TURN:
                task = markToTurnJob;
                break;
            case GET_DOC_STATUS:
                task = getStatusDocumentJobFacade;
                break;
            case AGGREGATION:
                task = aggregationJobFacade;
                break;
            case ACCEPTANCE:
                task = acceptanceJobFacade;
                break;
            case ORDER:
                task = orderJobFacade;
                break;
        }
        return task;
    }
}
