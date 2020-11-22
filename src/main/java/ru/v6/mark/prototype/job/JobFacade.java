package ru.v6.mark.prototype.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JobFacade implements Runnable {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    public static final int MAX_THREADS = 5;
    protected static final long POLL_INTERVAL_S = 15;

    @Override
    public synchronized void run() {
        logger.info(getClass().getSimpleName() + " STARTED.");

        doJob();

        logger.info(getClass().getSimpleName() + " FINISHED.");

    }

    public abstract void doJob();
}
