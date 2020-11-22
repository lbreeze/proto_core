package ru.v6.mark.prototype.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import ru.v6.mark.prototype.domain.constant.Frequency;
import ru.v6.mark.prototype.domain.constant.JobType;
import ru.v6.mark.prototype.domain.entity.JobTask;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Component
@Profile("job")
public class JobInitializer extends JobRunner {
    private Logger logger = LoggerFactory.getLogger(getClass());
/*
    @Autowired
    private JobTaskService jobTaskService;
    @Autowired
    @Qualifier("taskScheduler")
    private ThreadPoolTaskScheduler taskScheduler;
    @Autowired
    private ArticleImportJobFacade articleImportJob;
    @Autowired
    private RetrieveKIZJobFacade retrieveKIZJob;
    @Autowired
    private MarkToTurnJobFacade markToTurnJob;
    @Autowired
    private GetStatusDocumentJobFacade getStatusDocumentJobFacade;
    @Autowired
    private AggregationJobFacade aggregationJobFacade;
    @Autowired
    private AcceptanceJobFacade acceptanceJobFacade;
    @Autowired
    private OrderJobFacade orderJobFacade;
*/

    private Map<JobType, ScheduledFuture<?>> tasks = new HashMap<>();

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

/*
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        List<JobTask> jobTaskList = jobTaskService.findAll();
        for (JobTask jobTask : jobTaskList) {
            reschedule(jobTask);
        }
    }

    @Override
    public Status runJobTask(JobType jobType) {
        Runnable task = getTaskOfType(jobType);
        if (task != null) {
            task.run();
        } else {
            throw ApplicationException.build("Невозможно запустить задачу. Обратитесь к разработчику.");
        }
        return Status.OK;
    }
*/

    @Override
    public void reschedule(final JobTask jobTask) {
        JobType jobType = jobTask.getJobType();
        Frequency frequency = jobTask.getFrequency();

        ScheduledFuture scheduledFuture = tasks.get(jobType);
        if (scheduledFuture != null)
            scheduledFuture.cancel(true);

        Trigger trigger = null;

        ScheduledFuture scheduled = null;
        Calendar initial = Calendar.getInstance();
        initial.setTime(jobTask.getInitial());

        Runnable task = new Runnable() {
            @Override
            public void run() {
                jobTaskService.runTask(jobTask);
            }
        };

        switch (frequency) {
            case ONCE:
                if (jobTask.getInitial().after(new Date()))
                    scheduled = taskScheduler.schedule(task, jobTask.getInitial());
/*
                trigger = new CronTrigger(initial.get(Calendar.SECOND) + " " +
                        initial.get(Calendar.MINUTE) + " " +
                        initial.get(Calendar.HOUR_OF_DAY) + " " +
                        initial.get(Calendar.DATE) + " " +
                        (initial.get(Calendar.MONTH) + 1) + " *");
*/
                break;
            case HOURLY:
                trigger = new CronTrigger(initial.get(Calendar.SECOND) + " " +
                        initial.get(Calendar.MINUTE) + " * * * *");
                break;
            case DAILY:
                trigger = new CronTrigger(initial.get(Calendar.SECOND) + " " +
                        initial.get(Calendar.MINUTE) + " " +
                        initial.get(Calendar.HOUR_OF_DAY) + " * * *");
                break;
            case WEEKLY:
                trigger = new CronTrigger(initial.get(Calendar.SECOND) + " " +
                        initial.get(Calendar.MINUTE) + " " +
                        initial.get(Calendar.HOUR_OF_DAY) + " * * " +
                        initial.get(Calendar.DAY_OF_WEEK));
                break;
            case MONTHLY:
                trigger = new CronTrigger(initial.get(Calendar.SECOND) + " " +
                        initial.get(Calendar.MINUTE) + " " +
                        initial.get(Calendar.HOUR_OF_DAY) + " " +
                        initial.get(Calendar.DATE) + " * *");
                break;
        }

        if (trigger != null) {
            scheduled = taskScheduler.schedule(task, trigger);
        }

        if (scheduled != null)
            tasks.put(jobType, scheduled);
    }

/*
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
*/
}
