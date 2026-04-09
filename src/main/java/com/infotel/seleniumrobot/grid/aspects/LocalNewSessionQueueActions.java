package com.infotel.seleniumrobot.grid.aspects;

import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.grid.data.*;
import org.openqa.selenium.grid.distributor.local.LocalDistributor;
import org.openqa.selenium.grid.sessionqueue.local.LocalNewSessionQueue;
import org.openqa.selenium.internal.Debug;
import org.openqa.selenium.internal.Require;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This aspect will only work if using Hub globally, not separated grid components
 * We should check if using only SlotSelector could be enough
 */
@Aspect
public class LocalNewSessionQueueActions {

    private static final Logger logger = Logger.getLogger(LocalNewSessionQueueActions.class.getName());
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LocalNewSessionQueueActions.class);

    private final ReadWriteLock lock = new ReentrantReadWriteLock(/* fair */ true);

    @Around("execution(private * org.openqa.selenium.grid.sessionqueue.local.LocalNewSessionQueue.timeoutSessions (..))")
    public Object timeoutSession(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.log(Debug.getDebugLogLevel(), "timeout sessions");
        return joinPoint.proceed(joinPoint.getArgs());
    }

    //    @Around("execution(private * org.openqa.selenium.grid.sessionqueue.local.LocalNewSessionQueue.isTimedOut (..))")
//    public Object isTimedOut(ProceedingJoinPoint joinPoint) throws Throwable {
//        logger.info("isTimedOut");
//        return joinPoint.proceed(joinPoint.getArgs());
//    }
    @Around("execution(private * org.openqa.selenium.grid.sessionqueue.local.LocalNewSessionQueue.failDueToTimeout (..))")
    public Object failDueToTimeout(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.log(Debug.getDebugLogLevel(), "failDueToTimeout " + joinPoint.getArgs()[0]);
        return joinPoint.proceed(joinPoint.getArgs());
    }

    @Around("execution(public * org.openqa.selenium.grid.sessionqueue.local.LocalNewSessionQueue.addToQueue (..))")
    public Object addToQueue(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.log(Debug.getDebugLogLevel(), "addToQueue " + ((SessionRequest) joinPoint.getArgs()[0]).getRequestId() + " date: " + ((SessionRequest) joinPoint.getArgs()[0]).getDesiredCapabilities());
        Object result = joinPoint.proceed(joinPoint.getArgs());
        logger.log(Debug.getDebugLogLevel(), "fin addToQueue " + ((SessionRequest) joinPoint.getArgs()[0]).getRequestId());
        return result;
    }

    @Around("execution(public * org.openqa.selenium.grid.sessionqueue.local.LocalNewSessionQueue.remove (..))")
    public Object remove(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.log(Debug.getDebugLogLevel(), "remove " + joinPoint.getArgs()[0]);
        return joinPoint.proceed(joinPoint.getArgs());
    }

    @Around("execution(public * org.openqa.selenium.grid.sessionqueue.local.LocalNewSessionQueue.retryAddToQueue (..))")
    public Object retryAddToQueue(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.log(Debug.getDebugLogLevel(), "retryAddToQueue " + joinPoint.getArgs()[0]);
        return joinPoint.proceed(joinPoint.getArgs());
    }

    @Around("execution(private * org.openqa.selenium.grid.distributor.local.LocalDistributor.NewSessionRunnable.handleNewSessionRequest (..))")
    public Object handleNewSessionRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();
        logger.log(Debug.getDebugLogLevel(), "handleNewSessionRequest " + ((SessionRequest) joinPoint.getArgs()[0]).getRequestId());
        Object object = joinPoint.proceed(joinPoint.getArgs());
        long end = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();
        logger.log(Debug.getDebugLogLevel(), "end handleNewSessionRequest in  " + (end - start) + ": " + ((SessionRequest) joinPoint.getArgs()[0]).getRequestId());
        return object;
    }

    @Around("execution(public * org.openqa.selenium.grid.distributor.local.LocalDistributor.newSession (..))")
    public Object newSession(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.log(Debug.getDebugLogLevel(), "newSession " + ((SessionRequest) joinPoint.getArgs()[0]).getRequestId());
        Object object = joinPoint.proceed(joinPoint.getArgs());
        logger.log(Debug.getDebugLogLevel(), "end newSession " + ((SessionRequest) joinPoint.getArgs()[0]).getRequestId());
        return object;
    }

    @Around("execution(private * org.openqa.selenium.grid.distributor.local.LocalDistributor.reserveSlot (..))")
    public Object reserveSlot(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.info("reserveSlot " + joinPoint.getArgs()[0]);
        Object object;
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            object = joinPoint.proceed(joinPoint.getArgs());
        } finally {
            writeLock.unlock();
        }
        logger.info("end reserveSlot: reserved " + object);
        return object;
    }

    /**
     * When calling NewSessionQueue, there is a problem with seleniumRobot, as the number of slot available from selenium point of view may be higher
     * than the number of slots that seleniumRobot-grid really exposes (difference between 'sr:maxSessions' capability and 'maxSessions' capability
     * And so, LocalDistributor (calling hasCapacity) believes that a node can take more session whereas it's fully busy.
     * Following that, new session requests are sent to distributor whereas it cannot handle it.
     * In case of high load, they stack in the LocalDistributor queue and they may timeout before session is really created
     * <p>
     * This method only return session requests to distributor if distributor queue is empty
     * The only exception is for session requests that ask to attach to an existing session
     */
    //@Around("call(public * org.openqa.selenium.grid.sessionqueue.NewSessionQueue.getNextAvailable (..))")
    public Object getNextAvailable(ProceedingJoinPoint joinPoint) throws NoSuchFieldException, IllegalAccessException {
        logger.info("getNextAvailable");
        LocalNewSessionQueue sessionQueue = (LocalNewSessionQueue) joinPoint.getTarget();
        Field lockField = LocalNewSessionQueue.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        ReadWriteLock lock = (ReadWriteLock) lockField.get(sessionQueue);

        Map<Capabilities, Long> stereotypes = (Map<Capabilities, Long>) joinPoint.getArgs()[0];
        List<SessionRequest> availableSessions = getNextAvailableAttachingToNode(stereotypes, joinPoint, lock);

        try {

            Field distributorField = Class.forName("org.openqa.selenium.grid.distributor.local.LocalDistributor$NewSessionRunnable").getDeclaredField("this$0");
            distributorField.setAccessible(true);
            LocalDistributor localDistributor = (LocalDistributor) distributorField.get(joinPoint.getThis());

            Method getAvailableNodesMethod = LocalDistributor.class.getDeclaredMethod("getAvailableNodes");
            getAvailableNodesMethod.setAccessible(true);
            Set<NodeStatus> nodeStatus = (Set<NodeStatus>) getAvailableNodesMethod.invoke(localDistributor);

            Field sessionCreatorExecutorField = LocalDistributor.class.getDeclaredField("sessionCreatorExecutor");
            sessionCreatorExecutorField.setAccessible(true);
            ThreadPoolExecutor sessionCreatorExecutor = (ThreadPoolExecutor) sessionCreatorExecutorField.get(localDistributor);

            if (sessionCreatorExecutor.getQueue().isEmpty()) {

                // limit stereotypes to the ones that belongs to node that have free slots
                stereotypes =
                        nodeStatus.stream()
                                .filter(node -> node.getAvailability() == Availability.UP)
                                .filter(NodeStatus::hasCapacity)
                                .filter(node -> {
                                    long runningSession = node.getSlots().stream().filter(slot -> slot.getSession() != null).count();
                                    boolean allowMoreSessions = node.getSlots().stream()
                                            .filter(slot -> slot.getSession() != null)
                                            .filter(slot -> slot.getSession().getCapabilities().getCapability(SeleniumRobotCapabilityType.ALLOW_ADDITIONAL_SESSIONS_ON_NODE) != null
                                                    && (Boolean) slot.getSession().getCapabilities().getCapability(SeleniumRobotCapabilityType.ALLOW_ADDITIONAL_SESSIONS_ON_NODE))
                                            .count() == runningSession;

                                    long maxSessions = (Long) ((Slot) node.getSlots().toArray()[0]).getStereotype().getCapability(LaunchConfig.MAX_SESSIONS);

                                    return runningSession < maxSessions || allowMoreSessions;
                                })
                                .flatMap(node -> node.getSlots().stream().map(Slot::getStereotype))
                                .collect(
                                        Collectors.groupingBy(ImmutableCapabilities::copyOf, Collectors.counting()));

                availableSessions.addAll((List<SessionRequest>) joinPoint.proceed(new Object[]{stereotypes}));
            }

            // to do better filtering, we could
            // - exclude session requests that are about to time out
            // - exclude sessions already in queue, that have timed out

            logger.log(Debug.getDebugLogLevel(), "getNextAvailable: {}", availableSessions.size());

            return availableSessions;

        } catch (Throwable e) {
            return new ArrayList<SessionRequest>();
        }
    }

    /**
     * Select session requests that want to attach to a specific node, due to browser attachment
     */
    private List<SessionRequest> getNextAvailableAttachingToNode(Map<Capabilities, Long> stereotypes, ProceedingJoinPoint joinPoint, ReadWriteLock lock) throws NoSuchFieldException, IllegalAccessException {
        Require.nonNull("Stereotypes", stereotypes);

        LocalNewSessionQueue sessionQueue = (LocalNewSessionQueue) joinPoint.getTarget();
        Field slotMatcherField = LocalNewSessionQueue.class.getDeclaredField("slotMatcher");
        slotMatcherField.setAccessible(true);
        SlotMatcher slotMatcher = (SlotMatcher) slotMatcherField.get(sessionQueue);
        Field queueField = LocalNewSessionQueue.class.getDeclaredField("queue");
        queueField.setAccessible(true);
        Deque<SessionRequest> queue = (Deque<SessionRequest>) queueField.get(sessionQueue);
        Field batchSizeField = LocalNewSessionQueue.class.getDeclaredField("batchSize");
        batchSizeField.setAccessible(true);
        int batchSize = (int) batchSizeField.get(sessionQueue);

        Predicate<Capabilities> matchesStereotype =
                caps -> caps.getCapability(SeleniumRobotCapabilityType.ATTACH_SESSION_ON_NODE) != null
                        && stereotypes.entrySet().stream()
                        .filter(entry -> entry.getValue() > 0)
                        .anyMatch(
                                entry -> {
                                    boolean matches = slotMatcher.matches(entry.getKey(), caps);
                                    if (matches) {
                                        Long value = entry.getValue();
                                        entry.setValue(value - 1);
                                    }
                                    return matches;
                                });

        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            // first take into account requests for attaching an existing session
            List<SessionRequest> availableRequests =
                    queue.stream()
                            .filter(req -> req.getDesiredCapabilities().stream().anyMatch(matchesStereotype))
                            .limit(batchSize)
                            .collect(Collectors.toList());
            availableRequests.forEach(req -> sessionQueue.remove(req.getRequestId()));

            return availableRequests;
        } finally {
            writeLock.unlock();
        }
    }
}

