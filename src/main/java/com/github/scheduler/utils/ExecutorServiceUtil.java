package com.github.scheduler.utils;

import java.util.concurrent.*;

public class ExecutorServiceUtil {
    /**
     * Helper method to submit the callabale task, gets the original future object, and wrap it
     * in another future object with the ability to decorate the {@link Future#cancel(boolean)} method;
     * this decorator will block when future cancellation is invoked (and the "mayInterruptIfRunning"
     * parameter is set to true).
     *
     * @param service the executor service
     * @param callable a callable task
     *
     * @return decorated future object upon successful submission
     * @see {@link ExecutorService#submit(Callable)
     */
    public static <T> Future<T> submit(ExecutorService service, Callable<T> callable) {
        // Wrap the original callable object
        CallableTaskWrapper<T> wrapper = new CallableTaskWrapper<T>(callable);
        // Submit the wrapper object and set the original future object within the wrapper
        wrapper.setFuture(service.submit(wrapper));

        return wrapper;
    }

    /**
     * launch callable job once.
     * @param service executor service.
     * @param callable callable.
     * @param delay delay.
     * @param timeUnit TimeUnit.
     * */
    public static <T> CallableTaskWrapper<T> schedule(ScheduledExecutorService service,
                                                      Callable<T> callable,
                                                      long delay,
                                                      TimeUnit timeUnit){
        // Wrap the original callable object
        CallableTaskWrapper<T> wrapper = new CallableTaskWrapper<T>(callable);
        // Submit the wrapper object and set the original future object within the wrapper
        wrapper.setFuture(service.schedule(callable,delay,timeUnit));

        return wrapper;
    }

    /**
     * launch runnable job at fixed rate.
     * @param service ScheduledExecutorService
     * @param runnable WrappedRunnable
     * @param initialDelay initialDelay
     * @param period period
     * @param timeUnit timeUnit
     * @return ScheduledFuture<?>
     *     which can returns the remaining delay associated with this object,
     *     in the given time unit by the method of getDelay.
     * */
    public static ScheduledFuture<?> scheduleAtFixedRate(ScheduledExecutorService service,
                                           WrappedRunnable runnable,
                                           long initialDelay,
                                           long period, TimeUnit timeUnit){
        return service.scheduleAtFixedRate(runnable,initialDelay,period,timeUnit);
    }

    /**
     * Executor task wrapper to enhance task cancellation behavior
     * */
    public static final class CallableTaskWrapper<T> implements Callable<T>, Future<T> {
        /** Callable object */
        private final Callable<T> callableTask;
        /** Feature object returned after submission of the callback task */
        private volatile Future<T> future;
        /** Captures the callable task execution status */
        private volatile STATE state = STATE.NOT_RUNNING;
        /** Monitor object */
        private final Object monitor = new Object();

        /** Captures task's execution state */
        private enum STATE {
            NOT_RUNNING,
            RUNNING,
            DONE
        };

        /**
         * CTOR.
         * @param callableTask original callable task
         */
        public CallableTaskWrapper(Callable<T> callableTask) {
            this.callableTask = callableTask;
            Utils.checkNotNull(this.callableTask);
        }

        /** {@inheritDoc} */
        @Override
        public T call() throws Exception {
            try {
                state = STATE.RUNNING;

                return callableTask.call();
            } finally {
                state = STATE.DONE;

                // Optimization: no need to notify if the state is not "cancelled"
                if (isCancelled()) {
                    synchronized (monitor) {
                        monitor.notifyAll();
                    }
                }
            }
        }

        /**
         * This method will block waiting if the callbale thread is still executing and the "mayInterruptIfRunning"
         * flag is set; this method will return when:
         * <ul>
         * <li>The callbale thread is done executing</li>
         * <li>The current thread got interrupted; no exception will be thrown and instead the interrupted flag will
         * be set</li>
         * </ul>
         *
         * @see {@link Future#cancel(boolean)}
         */
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            try {
                return future.cancel(mayInterruptIfRunning);
            } finally {
                // If this thread wishes immediate completion of the task and was interrupted (because it was still running),
                // then block this thread till the callable task is done executing.
                if (mayInterruptIfRunning) {
                    waitTillDone();
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public boolean isCancelled() {
            // this method could be called from the call() API before the future is set
            return future != null && future.isCancelled();
        }

        /**
         * @return true if the task has completed execution
         */
        @Override
        public boolean isDone() {
            return state == STATE.DONE;
        }

        /** {@inheritDoc} */
        @Override
        public T get() throws InterruptedException, ExecutionException {
            return future.get();
        }

        /** {@inheritDoc} */
        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return future.get(timeout, unit);
        }

        /**
         * @param feature the feature to set
         */
        public void setFuture(Future<T> feature) {
            this.future = feature;
        }

        public void waitTillDone() {

            if (isRunning()) {
                // Save the current interrupted flag and clear it to allow wait operations
                boolean interrupted = Thread.interrupted();

                try {
                    synchronized (monitor) {
                        while (isRunning()) {
                            try {
                                monitor.wait();
                            } catch (InterruptedException e) {
                                interrupted = true;
                            }
                        }
                    }
                } finally {
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        private boolean isRunning() {
            return state == STATE.RUNNING;
        }
    }


    /**
     * Disabling object instantiation
     * */
    private ExecutorServiceUtil() {
    }
}
