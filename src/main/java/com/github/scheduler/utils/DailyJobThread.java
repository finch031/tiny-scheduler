package com.github.scheduler.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class DailyJobThread implements Runnable{
    private static final Logger LOG = LogManager.getLogger(DailyJobThread.class);

    private static final long JOIN_TIME = 90 * 1000;
    private Thread thread;
    private final CountDownLatch2 waitPoint = new CountDownLatch2(1);
    private volatile AtomicBoolean hasNotified = new AtomicBoolean(false);
    private volatile boolean stopped = false;
    private boolean isDaemon = false;

    // make it able to restart the thread
    private final AtomicBoolean started = new AtomicBoolean(false);

    public DailyJobThread(){}

    public abstract String getThreadName();

    public void start() {
        LOG.info("Try to start service thread:{} started:{} lastThread:{}", getThreadName(), started.get(), thread);
        if (!started.compareAndSet(false, true)) {
            return;
        }

        stopped = false;
        this.thread = new Thread(this, getThreadName());
        this.thread.setDaemon(isDaemon);
        this.thread.start();
    }

    public void shutdown() {
        this.shutdown(false);
    }

    public void shutdown(final boolean interrupt) {
        LOG.info("Try to shutdown service thread:{} started:{} lastThread:{}", getThreadName(), started.get(), thread);
        if (!started.compareAndSet(true, false)) {
            return;
        }

        this.stopped = true;
        LOG.info("shutdown thread " + this.getThreadName() + " interrupt " + interrupt);

        if (hasNotified.compareAndSet(false, true)) {
            // notify
            waitPoint.countDown();
        }

        try {
            if (interrupt) {
                this.thread.interrupt();
            }

            long beginTime = System.currentTimeMillis();
            if (!this.thread.isDaemon()) {
                this.thread.join(this.getJoinTime());
            }
            long elapsedTime = System.currentTimeMillis() - beginTime;
            LOG.info("join thread " + this.getThreadName() + " elapsed time(ms) " + elapsedTime + " "
                    + this.getJoinTime());
        } catch (InterruptedException e) {
            LOG.error("Interrupted", e);
        }
    }

    public long getJoinTime() {
        return JOIN_TIME;
    }

    public void makeStop() {
        if (!started.get()) {
            return;
        }
        this.stopped = true;
        LOG.info("make stop thread " + this.getThreadName());
    }

    public void wakeup() {
        if (hasNotified.compareAndSet(false, true)) {
            // notify
            waitPoint.countDown();
        }
    }

    protected void waitForRunning(long interval) {
        if (hasNotified.compareAndSet(true, false)) {
            this.onWaitEnd();
            return;
        }

        // entry to wait
        waitPoint.reset();

        try {
            waitPoint.await(interval, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.error("Interrupted", e);
        } finally {
            hasNotified.set(false);
            this.onWaitEnd();
        }
    }

    protected void onWaitEnd() {
    }

    public boolean isStopped() {
        return stopped;
    }

    public boolean isDaemon() {
        return isDaemon;
    }

    public void setDaemon(boolean daemon) {
        isDaemon = daemon;
    }
}
