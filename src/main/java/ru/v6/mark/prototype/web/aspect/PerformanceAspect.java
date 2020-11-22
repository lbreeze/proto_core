package ru.v6.mark.prototype.web.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.v6.mark.prototype.domain.entity.Performance;
import ru.v6.mark.prototype.service.PerformanceService;

import javax.annotation.PreDestroy;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Aspect
@Component
public class PerformanceAspect {
    @Autowired
    PerformanceService performanceService;

    private static final int LOGGABLE_STRING_MAX = 2000;
    private static final int LOGGABLE_ARGS_MAX = 2000;
    private static final long LOGGABLE_MILLIS_MIN = -1L;

    private Logger logger = LoggerFactory.getLogger(getClass());
    private ExecutorService service = Executors.newFixedThreadPool(5, new NamedThreadFactory(getClass().getSimpleName()));
    private final ConcurrentLinkedQueue<Performance> queue = new ConcurrentLinkedQueue<>();

    @Around(value = "(this(ru.v6.mark.prototype.service.EntityCriteriaService) || @within(ru.v6.mark.prototype.domain.constant.LogPerformance) || @annotation(ru.v6.mark.prototype.domain.constant.LogPerformance))) && execution(public * *.*(..))")
    public Object log(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        try {
            return joinPoint.proceed();
        } finally {
            long millis = System.currentTimeMillis() - start;

            doLogging(joinPoint, millis, LOGGABLE_MILLIS_MIN);

            Thread.currentThread().setName(threadName);
        }
    }

    public void doLogging(ProceedingJoinPoint joinPoint, long millis, long loggableMillis) {
        if(millis > loggableMillis) {
            doLogging(joinPoint, millis);
        }
    }

    public void doLogging(ProceedingJoinPoint joinPoint, long millis) {
        Performance performance = new Performance();
        performance.setClazz(joinPoint.getTarget().getClass().getSimpleName());
        performance.setMethod(joinPoint.getSignature().getName());
        performance.setMillis(millis);
        performance.setThread(Thread.currentThread().getName());
        performance.setLastUpdate(new Date());

        String name = performance.getClazz() + "." + performance.getMethod();
        StringBuilder argStr = new StringBuilder();
        for (Object arg : joinPoint.getArgs()) {
            if (argStr.length() > 0) {
                argStr.append(" ");
            }

            convertArg(argStr, arg);
        }
        performance.setArgs(argStr.toString().length() <= LOGGABLE_ARGS_MAX ? argStr.toString() : argStr.toString().substring(0, LOGGABLE_ARGS_MAX));

        queue.add(performance);
        if (((ThreadPoolExecutor) service).getActiveCount() < ((ThreadPoolExecutor) service).getMaximumPoolSize()) {
            service.execute(new Runnable() {
                @Override
                public void run() {
                    Performance item = queue.poll();
                    while (item != null) {
                        try {
                            performanceService.save(item);
                        } catch (Exception e) {
                            logger.error("Error saving performance data: " + item.toString(), e);
                        }
                        item = queue.poll();
                    }
                }
            });
        }
        logger.debug(performance.getMillis() + " ms: " + name + "(" + argStr.toString() + ") : ");
    }

    private void convertArg(StringBuilder argStr, Object arg) {
        if (argStr.length() < LOGGABLE_ARGS_MAX) {
            if (arg != null) {
                if (arg.getClass().isArray()) {
                    argStr.append("Array[").append(Array.getLength(arg)).append("]");
                } else if (arg instanceof Collection) {
                    argStr.append("Collection[").append(((Collection) arg).size()).append("]");

                } else {
                    String strArg = arg.toString();
                    if (strArg.length() > LOGGABLE_STRING_MAX) {
                        argStr.append("String[").append(strArg.length()).append("]");
                    } else {
                        argStr.append(strArg);
                    }
                }
            } else
                argStr.append("[null]");
        }
    }

    @PreDestroy
    protected void destroy() throws Exception {
        service.shutdown();
    }
}