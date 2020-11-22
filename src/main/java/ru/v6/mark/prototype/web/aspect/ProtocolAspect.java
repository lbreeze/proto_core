package ru.v6.mark.prototype.web.aspect;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.hibernate.Session;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ru.v6.mark.prototype.domain.constant.*;
import ru.v6.mark.prototype.domain.dao.UserDao;
import ru.v6.mark.prototype.domain.entity.BaseEntity;
import ru.v6.mark.prototype.domain.entity.DeletableEntity;
import ru.v6.mark.prototype.domain.entity.Protocol;
import ru.v6.mark.prototype.service.ProtocolService;
import ru.v6.mark.prototype.web.context.RequestContext;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by DVDanchenko on 17.05.2017.
 */
@Aspect
@Component
public class ProtocolAspect {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    ProtocolService protocolService;
    @Autowired
    RequestContext requestContext;
    @Autowired
    UserDao userDao;
    @Autowired
    @Qualifier("serverMessages")
    MessageSource messageSource;

    @PersistenceContext
    private EntityManager entityManager;

    private ExecutorService service = Executors.newCachedThreadPool(new ProtocolThreadFactory(getClass().getSimpleName()));
    private final ConcurrentLinkedQueue<Protocol> queue = new ConcurrentLinkedQueue<>();

    public static final int MAX_VALUE_LEN = 240;
    private static final int SEPARATOR_LENGTH = 50;
    private static final int LEVEL_OFFSET = 4; // offset spaces for each protocol levels

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private static class ProtocolThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        ProtocolThreadFactory(String poolName) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = (poolName == null ? "pool" : poolName) + "-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

    @Around(value = "execution(public * ru.v6.mark.prototype.service.EntityService.remove(..)) && args(entity)")
    public Object logRemove(ProceedingJoinPoint joinPoint, BaseEntity entity) throws Throwable {
        Assert.notNull(entity);
        //
        if (checkLoggable(entity)) {
            HasProtocol loggable = entity.getClass().getAnnotation(HasProtocol.class);
            Protocol protocol = new Protocol();

            protocol.setOperation(ProtocolOperation.DELETE);

            String actionMsg;
            try {
                actionMsg = messageSource.getMessage(loggable.action(), null, Locale.getDefault());
            } catch (NoSuchMessageException e) {
                actionMsg = loggable.action();
            }
            protocol.setAction(actionMsg);

            createChangeLog(entity, protocol);

            Object result = null;
            try {
                result = joinPoint.proceed();
                return result;
            } finally {
                if (!loggable.linkPrefix().isEmpty())
                    protocol.setExternalLink("../" + loggable.linkPrefix() + '/' + entity.getId());
                doLog(entity, protocol, loggable.now());
            }
        } else {
            return joinPoint.proceed();
        }
    }

    @Around(value = "execution(public * ru.v6.mark.prototype.service.EntityService.save(..)) && args(entity)")
    public Object logMerge(ProceedingJoinPoint joinPoint, BaseEntity entity) throws Throwable {
        Assert.notNull(entity);
        //
        if (checkLoggable(entity)) {
            HasProtocol loggable = entity.getClass().getAnnotation(HasProtocol.class);
            Protocol protocol = new Protocol();

            // log.setType(loggable.type());
            if (entity.getId() == null)
                protocol.setOperation(ProtocolOperation.CREATE);
            else {
                protocol.setOperation(ProtocolOperation.EDIT);
            }
            String actionMsg;
            try {
                actionMsg = messageSource.getMessage(loggable.action(), null, Locale.getDefault());
            } catch (NoSuchMessageException e) {
                actionMsg = loggable.action();
            }
            protocol.setAction(actionMsg);

            createChangeLog(entity, protocol);

            Object result = null;
            try {
                result = joinPoint.proceed();
                return result;
            } finally {
                if(!loggable.onlyIfChangedAtributes() || !protocol.getChangeLog().isEmpty()) {
                    if ((result != null) || !protocol.getOperation().equals(ProtocolOperation.CREATE)) {
                        if (!loggable.linkPrefix().isEmpty())
                            protocol.setExternalLink(loggable.linkPrefix() + '/' + entity.getId());
                        doLog((BaseEntity) result, protocol, loggable.now());
                    }
                }
            }
        } else {
            return joinPoint.proceed();
        }
    }

    @Around(value = "(@within(ru.v6.mark.prototype.domain.constant.HasProtocol) || @annotation(ru.v6.mark.prototype.domain.constant.HasProtocol))  && (args(entity) || args(entity,..))")
    public Object log(ProceedingJoinPoint joinPoint, BaseEntity entity) throws Throwable {
        Assert.notNull(entity);
        //
        if (checkLoggable(entity)) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            HasProtocol loggable = method.getAnnotation(HasProtocol.class);
            Protocol protocol = new Protocol();

            //log.setType(loggable.type());
            if (entity.getId() == null)
                protocol.setOperation(ProtocolOperation.CREATE);
            else {
                protocol.setOperation(ProtocolOperation.EDIT);
            }
            String actionMsg;
            try {
                actionMsg = messageSource.getMessage(loggable.action(), null, Locale.getDefault());
            } catch (NoSuchMessageException e) {
                actionMsg = loggable.action();
            }
            protocol.setAction(actionMsg);

            Object result = null;

            createChangeLog(entity, protocol);

            try {
                result = joinPoint.proceed();
                return result;
            } finally {
                if (!loggable.linkPrefix().isEmpty()) {
                    Serializable id = null;
/*
                    if (loggable.identifier().equals(ProtocolIdentifier.UTM_DOCUMENT)) {
                        if (entity instanceof HasUtmDocument) {
                            id = ((HasUtmDocument) entity).getUtmDocument().getId();
                        } else if (entity instanceof HasLastUtmDocument<?>) {
                            BaseEntity docEntity = ((HasLastUtmDocument<? extends BaseEntity>) entity).getLastUtmDocument();
                            if (docEntity != null) {
                                id = ((HasUtmDocument) docEntity).getUtmDocument().getId();
                            }
                        } else if (entity instanceof HasCancelAct) {
                            BaseEntity docEntity = ((HasCancelAct<? extends BaseEntity>) entity).getCancelAct();
                            if (docEntity instanceof HasUtmDocument) {
                                id = ((HasUtmDocument) docEntity).getUtmDocument().getId();
                            }
                        } else {
                            id = entity.getId(); // defaults to entity id if no utm document found
                        }
                    } else {
*/
                        id = loggable.identifier().equals(ProtocolIdentifier.ENTITY) ? entity.getId() : (result == null ? null : ((BaseEntity) result).getId());
//                    }
                    if (id != null)
                        protocol.setExternalLink(loggable.linkPrefix() + '/' + id);
                }
                if (!loggable.identifier().equals(ProtocolIdentifier.RESULT) || (result != null)) {
                    doLog((entity.getId() == null && result != null) ? (BaseEntity) result : entity, protocol, loggable.now());
                }
            }
        } else {
            return joinPoint.proceed();
        }
    }

    private boolean checkLoggable(BaseEntity entity) {
        boolean result = false;
        if (entity.getClass().isAnnotationPresent(HasProtocol.class)) {
            HasProtocol loggable = entity.getClass().getAnnotation(HasProtocol.class);

            try {
                result = !(loggable.userOnly() && requestContext.getUserName() == null);
//                    if (requestContext.getUser() != null) {
//                        result = true;
//                    }
//                } else {
//                    result = true;
//                }
            } catch (Exception e) {
                // no user found
            }
        }
        return result;
    }

    private void doLog(BaseEntity entity, Protocol protocol, boolean now) throws Throwable {
        if (entity != null) {
            String entityId = entity.getId().toString();
            protocol.setEntityId(entityId.length() > 50 ? entityId.substring(0, 50) : entityId);
            protocol.setEntity(entity.getClass().getSimpleName().toUpperCase());
            try {
                if (requestContext.getUserName() != null)
                    protocol.setUsername(requestContext.getUserName());
            } catch (Exception e) {
                // no bean scope found, no user
//                e.printStackTrace();
            }

            protocol.setLastUpdate(now ? new Date() : entity.getLastUpdate());//new Date());

            queue.add(protocol);
            if (((ThreadPoolExecutor) service).getActiveCount() < ((ThreadPoolExecutor) service).getMaximumPoolSize()) {
                service.execute(new Runnable() {
                    @Override
                    public void run() {
                        Protocol item = queue.poll();
                        while (item != null) {
                            try {
                                if (!item.getChangeLog().isEmpty() && item.getExternalLink() == null) {
                                    item.setExternalLink("");
                                }

                                String changeLog = item.getChangeLog();
                                item = protocolService.save(item);
                                if (!changeLog.isEmpty())
                                    protocolService.saveAsFile(item.getId(), changeLog);

                            } catch (Exception e) {
                                logger.error(item.toString(), e);
                                e.printStackTrace();
                            }
                            item = queue.poll();
                        }
                    }
                });
            }
        }
    }

    /**
     * @param entity   changed entity to be persisted
     * @param original original entity persisted before in database
     * @param method   method to be processed, or null if entity processing.
     * @return changelog as string if any changes detected, empty string otherwise
     * @throws InvocationTargetException thrown due method invocation throws exception
     * @throws IllegalAccessException    thrown due method is inaccessible and unable to be invoked
     */
    private String processAttributes(Object entity, Object original, Method method, int level) throws InvocationTargetException, IllegalAccessException {
        StringBuilder result = new StringBuilder("");
        if (method == null) {
            for (Method inMethod : entity.getClass().getDeclaredMethods()) {
                result.append(processAttributes(entity, original, inMethod, level));
            }
            if (result.length() > 0 && !entity.toString().isEmpty()) {
                result.insert(0, StringUtils.repeat("-", SEPARATOR_LENGTH)).insert(0, StringUtils.repeat(" ", level * LEVEL_OFFSET)).insert(0, "\n").insert(0, entity.toString()).insert(0, StringUtils.repeat(" ", level * LEVEL_OFFSET)).insert(0, "\n").append("\n").append(StringUtils.repeat(" ", level * LEVEL_OFFSET)).append(StringUtils.repeat("-", SEPARATOR_LENGTH));
            }

        } else {
            if (method.isAnnotationPresent(ProtocolAttribute.class)) {
                method.setAccessible(true);
                String description = method.getAnnotation(ProtocolAttribute.class).value();
                Object resultE = method.invoke(entity);
                Object resultO = original == null ? null : method.invoke(original);
                String value;
                if (resultE == null) {
                    if (resultO != null) {
                        value = "null";
                        result.append("\n").append(StringUtils.repeat(" ", level * LEVEL_OFFSET)).append(description).append(": ").append(value);
                    }
                } else if (resultE instanceof Collection) {
                    for (Object resultEItem : (Collection) resultE) {
                        if (resultEItem instanceof BaseEntity) {
                            if (((BaseEntity) resultEItem).getId() == null) {
                                result.append(processAttributes(resultEItem, null, null, description.isEmpty() ? level : level + 1));
                            } else {
                                boolean found = false;
                                if (resultO != null) {
                                    for (BaseEntity resultOItem : (Collection<? extends BaseEntity>) resultO) {
                                        if (((BaseEntity) resultEItem).getId().equals(resultOItem.getId())) {
                                            found = true;
                                            result.append(processAttributes(resultEItem, resultOItem, null, description.isEmpty() ? level : level + 1));
                                        }
                                    }
                                }

                                if (!found)
                                    result.append(processAttributes(resultEItem, null, null, description.isEmpty() ? level : level + 1));
                            }
                        }
                    }

                    if (resultO != null) {
                        for (Object resultOItem : (Collection) resultO) {
                            if (resultOItem instanceof BaseEntity) {
                                boolean found = false;
                                for (Object resultEItem : (Collection) resultE) {
                                    if (resultEItem instanceof BaseEntity) {
                                        if (((BaseEntity) resultOItem).getId().equals(((BaseEntity) resultEItem).getId())) {
                                            found = true;
                                        }
                                    }
                                }

                                if (!found) {
                                    result.append("\n").append(StringUtils.repeat(" ", level * LEVEL_OFFSET)).append(resultOItem.toString()).append(": удалено");
                                }
                            }
                        }
                    }

                    if (result.length() > 0) {
                        if (!description.isEmpty()) {
                            result.insert(0, StringUtils.repeat("-", SEPARATOR_LENGTH)).insert(0, StringUtils.repeat(" ", level * LEVEL_OFFSET)).insert(0, "\n").insert(0, description).insert(0, StringUtils.repeat(" ", level * LEVEL_OFFSET)).insert(0, "\n").append("\n").append(StringUtils.repeat(" ", level * LEVEL_OFFSET)).append(StringUtils.repeat("-", SEPARATOR_LENGTH));
                        }
                    }
                } else if (BaseEntity.class.isAssignableFrom(resultE.getClass())) {
                    result.append(processAttributes(resultE, resultO, null, description.isEmpty() ? level : level + 1));
                    if (result.length() > 0) {
                        if (!description.isEmpty()) {
                            result.insert(0, StringUtils.repeat("-", SEPARATOR_LENGTH)).insert(0, StringUtils.repeat(" ", level * LEVEL_OFFSET)).insert(0, "\n").insert(0, description).insert(0, StringUtils.repeat(" ", level * LEVEL_OFFSET)).insert(0, "\n").append("\n").append(StringUtils.repeat(" ", level * LEVEL_OFFSET)).append(StringUtils.repeat("-", SEPARATOR_LENGTH));
                        }
                    }
                } else {
                    if (!resultE.equals(resultO)) {
                        if (resultE instanceof Date) {
                            if (resultE.equals(DateUtils.truncate(resultE, Calendar.DATE))) {
                                value = dateFormat.format(resultE);
                            } else {
                                value = dateTimeFormat.format(resultE);
                            }
                        } else if (resultE instanceof BigDecimal) {
                            value = ((BigDecimal) resultE).toPlainString();
                        } else if (resultE instanceof EnumDesc) {
                            try {
                                value = messageSource.getMessage(((EnumDesc) resultE).getDescription(), null, Locale.getDefault());
                            } catch (NoSuchMessageException e) {
                                value = ((EnumDesc) resultE).getDescription();
                            }
                        } else if (resultE instanceof Enum) {
                            value = ((Enum) resultE).name();
                        } else {
                            value = resultE.toString();
                        }
                        result.append("\n").append(StringUtils.repeat(" ", level * LEVEL_OFFSET)).append(description).append(": ").append(value);
                    }
                }
            }
        }
        return result.toString();
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public <T extends BaseEntity> void createChangeLog(T entity, Protocol protocol) {
        T original = null;
        Session newSession = null;
        if (entity.getId() != null) {
            Session session = entityManager.unwrap(Session.class);
            newSession = session.getSessionFactory().openSession();
            if (entity instanceof HibernateProxy) {
                original = (T) newSession.get(((HibernateProxy) entity).getHibernateLazyInitializer().getPersistentClass(), entity.getId());
            } else {
                original = (T) newSession.get(entity.getClass(), entity.getId());
            }
        }

        try {
            if (protocol.getOperation().equals(ProtocolOperation.EDIT) && entity instanceof DeletableEntity && ((DeletableEntity) entity).getDeleted() && !((DeletableEntity) original).getDeleted()) {
                protocol.setOperation(ProtocolOperation.DELETE);
            }

            protocol.setChangeLog(processAttributes(entity, original, null, 0));
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }

        if (newSession != null && entity.getId() != null) {
            if(original != null) {
                newSession.evict(original);
            }
            newSession.close();
        }
    }
}
