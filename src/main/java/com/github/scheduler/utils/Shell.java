package com.github.scheduler.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author yusheng
 * @version 1.0.0
 * @datetime 2021-03-29 08:58
 * @description A base class for running a Windows/Linux command.
 */
public abstract class Shell {
    private static final Logger LOG = LogManager.getLogger(Shell.class);

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final boolean WINDOWS = System.getProperty("os.name") != null &&
            System.getProperty("os.name").startsWith("Windows");

    // the working directory.
    private File dir;

    // refresh interval in msec
    private final long interval;

    // last time the command was performed
    private long lastTime;

    // merge stdout and stderr
    private final boolean redirectErrorStream;

    // env for the command execution
    private Map<String, String> environment;

    // sub process used to execute the command.
    private Process process;

    // exit code.
    private int exitCode;

    private Thread waitingThread;

    // indicates if the parent env vars should be inherited or not
    protected boolean inheritParentEnv = true;

    private static final Map<Shell, Object> childShells = Collections.synchronizedMap(new WeakHashMap<>());

    // after timeOutInterval milliseconds which the executing script would be timed out.
    protected long timeOutInterval = 0L;

    // if or not script timed out
    private final AtomicBoolean timedOut = new AtomicBoolean(false);

    // flag to indicate whether or not the script has finished executing.
    private final AtomicBoolean completed = new AtomicBoolean(false);

    // Windows CreateProcess synchronization object.
    private static final Object WindowsProcessLaunchLock = new Object();

    /**
     * create an instance with no minimum interval between runs;
     * stderr is not merged with stdout.
     */
    protected Shell() {
        this(0L);
    }

    /**
     * create an instance with a minimum interval between executions;
     * stderr is not merged with stdout.
     * @param interval interval in milliseconds between command executions.
     */
    protected Shell(long interval) {
        this(interval, false);
    }

    /**
     * Create a shell instance which can be re-executed when the {@link #run()}
     * method is invoked with a given elapsed time between calls.
     *
     * @param interval the minimum duration in milliseconds to wait before
     *        re-executing the command. If set to 0, there is no minimum.
     * @param redirectErrorStream should the error stream be merged with
     *        the normal output stream?
     */
    protected Shell(long interval, boolean redirectErrorStream) {
        this.interval = interval;
        this.lastTime = (interval < 0) ? 0 : -interval;
        this.redirectErrorStream = redirectErrorStream;
        this.environment = Collections.emptyMap();
    }

    private static long monotonicNow() {
        return System.nanoTime() / 1000000L;
    }

    /**
     *  check to see if a command needs to be executed and execute if needed.
     * */
    protected void run() throws IOException {
        if (lastTime + interval > monotonicNow()) {
            return;
        }

        // reset for next run
        exitCode = 0;

        runCommand();
    }

    /**
     *  run the command.
     * */
    private void runCommand() throws IOException {
        ProcessBuilder builder = new ProcessBuilder(getExecString());
        Timer timeOutTimer = null;
        ShellTimeoutTimerTask timeoutTimerTask = null;
        timedOut.set(false);
        completed.set(false);

        // Remove all env vars from the Builder to prevent leaking of env vars from
        // the parent process.
        if (!inheritParentEnv) {
            builder.environment().clear();
        }

        builder.environment().putAll(this.environment);

        if (dir != null) {
            builder.directory(this.dir);
        }

        builder.redirectErrorStream(redirectErrorStream);

        if (Shell.WINDOWS) {
            synchronized (WindowsProcessLaunchLock) {
                // To workaround the race condition issue with child processes
                // inheriting unintended handles during process launch that can
                // lead to hangs on reading output and error streams, we
                // serialize process creation. More info available at:
                // http://support.microsoft.com/kb/315939
                process = builder.start();
            }
        } else {
            process = builder.start();
        }

        waitingThread = Thread.currentThread();
        childShells.put(this, null);

        if (timeOutInterval > 0) {
            timeOutTimer = new Timer("Shell command timeout");
            timeoutTimerTask = new ShellTimeoutTimerTask(this);

            // One time scheduling.
            timeOutTimer.schedule(timeoutTimerTask, timeOutInterval);
        }

        Charset charset = Charset.defaultCharset();
        if(WINDOWS){
            charset = Charset.forName("GBK");
        }

        final BufferedReader errReader = new BufferedReader(new InputStreamReader(
                process.getErrorStream(), charset));

        BufferedReader inReader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), charset));

        final StringBuffer errMsg = new StringBuffer();

        // read error and input streams as this would free up the buffers
        // free the error stream buffer
        Thread errThread = new Thread() {
            @Override
            public void run() {
                try {
                    String line = errReader.readLine();
                    while((line != null) && !isInterrupted()) {
                        errMsg.append(line)
                                .append(System.getProperty("line.separator"));
                        line = errReader.readLine();
                    }
                } catch(IOException ioe) {
                    // Its normal to observe a "Stream closed" I/O error on
                    // command timeouts destroying the underlying process
                    // so only log a WARN if the command didn't time out
                    if (!isTimedOut()) {
                        LOG.warn("Error reading the error stream", ioe);
                    } else {
                        LOG.debug("Error reading the error stream due to shell "
                                + "command timeout", ioe);
                    }
                }
            }
        };

        try {
            errThread.start();
        } catch (IllegalStateException ise) {
        } catch (OutOfMemoryError oe) {
            LOG.error("Caught " + oe + ". One possible reason is that ulimit"
                    + " setting of 'max user processes' is too low. If so, do"
                    + " 'ulimit -u <largerNum>' and try again.");
            throw oe;
        }

        try {
            // parse the output
            parseExecResult(inReader);

            // clear the input stream buffer
            String line = inReader.readLine();
            while(line != null) {
                line = inReader.readLine();
            }

            // wait for the process to finish and check the exit code
            exitCode  = process.waitFor();

            // make sure that the error thread exits
            joinThread(errThread);

            setExecErrorResult(errMsg);

            completed.set(true);
            //the timeout thread handling
            //taken care in finally block
            if (exitCode != 0) {
                LOG.error("exec error, exit code:{} ,errMsg:{}", exitCode, errMsg.toString());
                // throw new ExitCodeException(exitCode, errMsg.toString());
            }
        } catch (InterruptedException ie) {
            InterruptedIOException iie = new InterruptedIOException(ie.toString());
            iie.initCause(ie);
            throw iie;
        } finally {
            if (timeOutTimer != null) {
                timeOutTimer.cancel();
            }

            // close the input stream
            try {
                inReader.close();
            } catch (IOException ioe) {
                LOG.warn("Error while closing the input stream", ioe);
            }

            if (!completed.get()) {
                errThread.interrupt();
                joinThread(errThread);
            }

            try {
                errReader.close();
            } catch (IOException ioe) {
                LOG.warn("Error while closing the error stream", ioe);
            }

            process.destroy();
            waitingThread = null;
            childShells.remove(this);
            lastTime = monotonicNow();
        }

    }

    /**
     * join the thread.
     * */
    private static void joinThread(Thread t) {
        while (t.isAlive()) {
            try {
                t.join();
            } catch (InterruptedException ie) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Interrupted while joining on: " + t, ie);
                }
                // propagate interrupt
                t.interrupt();
            }
        }
    }

    /**
     * Set the working directory.
     * @param dir The directory where the command will be executed
     */
    protected void setWorkingDirectory(File dir) {
        this.dir = dir;
    }

    /**
     * set the environment for the command.
     * @param env Mapping of environment variables
     */
    protected void setEnvironment(Map<String, String> env) {
        this.environment = Objects.requireNonNull(env);
    }

    /**
     * to check if the passed script to shell command executor timed out or not.
     * @return if the script timed out.
     */
    public boolean isTimedOut() {
        return timedOut.get();
    }

    /**
     * declare that the command has timed out.
     */
    private void setTimedOut() {
        this.timedOut.set(true);
    }

    /**
     * static method to execute a shell command.
     * covers most of the simple cases without requiring the user
     * to implement the Shell interface.
     * @param cmd shell command to execute.
     * @return the output of the executed command.
     */
    public static String execCommand(String ... cmd) throws IOException {
        return execCommand(null, cmd,null, 0L);
    }

    /**
     * static method to execute a shell command.
     * covers most of the simple cases without requiring the user
     * to implement the Shell interface.
     * @param env the map of environment key=value
     * @param cmd shell command to execute.
     * @param dir working dir.
     * @param timeout time in milliseconds after which script should be marked timeout
     * @return the output of the executed command.
     * @throws IOException on any problem.
     */
    public static String execCommand(Map<String, String> env,
                                     String[] cmd,
                                     File dir,
                                     long timeout) throws IOException {
        ShellCommandExecutor exec = new ShellCommandExecutor(cmd, dir, env, timeout);
        exec.execute();

        StringBuilder sb = new StringBuilder();
        sb.append("process execution output:");
        sb.append(LINE_SEPARATOR);
        sb.append(exec.getOutput());
        sb.append(LINE_SEPARATOR);
        sb.append("process error output:");
        sb.append(LINE_SEPARATOR);
        sb.append(exec.getError());

        return sb.toString();
    }

    /**
     * static method to execute a shell command.
     * covers most of the simple cases without requiring the user
     * to implement the Shell interface.
     * @param env the map of environment key=value
     * @param cmd shell command to execute.
     * @return the output of the executed command.
     * @throws IOException on any problem.
     */
    public static String execCommand(Map<String,String> env, String ... cmd) throws IOException {
        return execCommand(env, cmd,null, 0L);
    }

    /**
     * returns a command to run the given script.  The script interpreter is
     * inferred by platform: cmd on Windows or bash otherwise.
     *
     * @param script File script to run
     * @return String[] command to run the script
     */
    public static String[] getRunScriptCommand(File script) {
        String absolutePath = script.getAbsolutePath();
        return WINDOWS ?
                new String[] {"cmd", "/c", absolutePath }
                : new String[] {"bash", bashQuote(absolutePath) };
    }

    /**
     * quote the given arg so that bash will interpret it as a single value.
     * note that this quotes it for one level of bash, if you are passing it
     * into a badly written shell script, you need to fix your shell script.
     * @param arg the argument to quote
     * @return the quoted string
     */
    private static String bashQuote(String arg) {
        StringBuilder sb = new StringBuilder(arg.length() + 2);
        sb.append('\'').append(arg.replace("'", "'\\''")).append('\'');
        return sb.toString();
    }

    /**
     *  return an array containing the command name and its parameters.
     * */
    protected abstract String[] getExecString();

    /**
     *  parse the execution result.
     * */
    protected abstract void parseExecResult(BufferedReader lines) throws IOException;

    /**
     * set the execution error result.
     * */
    protected abstract void setExecErrorResult(StringBuffer errorMsg) throws IOException;

    /**
     * get an environment variable.
     * @param env the environment var
     * @return the value or null if it was unset.
     */
    public String getEnvironment(String env) {
        return this.environment.get(env);
    }

    /** get the current sub-process executing the given command.
     * @return process executing the command
     */
    public Process getProcess() {
        return this.process;
    }

    /** get the exit code.
     * @return the exit code of the process
     */
    public int getExitCode() {
        return this.exitCode;
    }

    /** get the thread that is waiting on this instance of Shell.
     * @return the thread that ran runCommand() that spawned this shell
     * or null if no thread is waiting for this shell to complete
     */
    public Thread getWaitingThread() {
        return this.waitingThread;
    }

    public interface CommandExecutor {
        void execute() throws IOException;
        int getExitCode() throws IOException;
        String getOutput() throws IOException;
        String getError() throws IOException;
        void close();
    }

    /**
     * A simple shell command executor.
     * ShellCommandExecutor should be used in cases where the output
     * of the command needs no explicit parsing and where the command, working
     * directory and the environment remains unchanged. The output of the command
     * is stored as-is and is expected to be small.
     */
    public static class ShellCommandExecutor extends Shell implements CommandExecutor {
        private final String[] command;
        private StringBuffer output;
        private StringBuffer errorMsg;

        public ShellCommandExecutor(String[] execString) {
            this(execString, null);
        }

        public ShellCommandExecutor(String[] execString, File dir) {
            this(execString, dir, null);
        }

        public ShellCommandExecutor(String[] execString, File dir, Map<String, String> env) {
            this(execString, dir, env , 0L);
        }

        public ShellCommandExecutor(String[] execString, File dir, Map<String, String> env, long timeout) {
            this(execString, dir, env , timeout, true);
        }

        /**
         * Create a new instance of the ShellCommandExecutor to execute a command.
         *
         * @param execString The command to execute with arguments
         * @param dir If not-null, specifies the directory which should be set
         *            as the current working directory for the command.
         *            If null, the current working directory is not modified.
         * @param env If not-null, environment of the command will include the
         *            key-value pairs specified in the map. If null, the current
         *            environment is not modified.
         * @param timeout Specifies the time in milliseconds, after which the
         *                command will be killed and the status marked as timed-out.
         *                If 0, the command will not be timed out.
         * @param inheritParentEnv Indicates if the process should inherit the env
         *                         vars from the parent process or not.
         */
        public ShellCommandExecutor(String[] execString, File dir, Map<String, String> env, long timeout, boolean inheritParentEnv) {
            command = execString.clone();

            if (dir != null) {
                setWorkingDirectory(dir);
            }

            if (env != null) {
                setEnvironment(env);
            }
            timeOutInterval = timeout;
            this.inheritParentEnv = inheritParentEnv;
        }

        /**
         * returns the timeout value set for the executor's sub-commands.
         * @return The timeout value in milliseconds
         */
        public long getTimeoutInterval() {
            return this.timeOutInterval;
        }

        /**
         * execute the shell command.
         * @throws IOException if the command fails, or if the command is
         * not well constructed.
         */
        public void execute() throws IOException {
            for (String s : command) {
                if (s == null) {
                    throw new IOException("(null) entry in command string: "
                            + join(" ", command));
                }
            }
            this.run();
        }

        @Override
        public String[] getExecString() {
            return this.command;
        }

        @Override
        protected void parseExecResult(BufferedReader lines) throws IOException {
            output = new StringBuffer();
            char[] buf = new char[512];
            int nRead;
            while ( (nRead = lines.read(buf, 0, buf.length)) > 0 ) {
                output.append(buf, 0, nRead);
            }
        }

        @Override
        protected void setExecErrorResult(StringBuffer errorMsg){
            this.errorMsg = errorMsg;
        }

        /**
         * get the output of the shell command.
         * */
        @Override
        public String getOutput() {
            return (output == null) ? "" : output.toString();
        }

        /**
         * get the error of the shell command.
         * */
        @Override
        public String getError() {
            return (errorMsg == null) ? "" : errorMsg.toString();
        }

        /**
         * returns the commands of this instance.
         * arguments with spaces in are presented with quotes round
         * other arguments are presented raw.
         * @return a string representation of the object.
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            String[] args = getExecString();
            for (String s : args) {
                if (s.indexOf(' ') >= 0) {
                    builder.append('"').append(s).append('"');
                } else {
                    builder.append(s);
                }
                builder.append(' ');
            }
            return builder.toString();
        }

        @Override
        public void close() {
        }
    }

    /**
     * Timer which is used to timeout scripts spawned off by shell.
     */
    private static class ShellTimeoutTimerTask extends TimerTask {
        private final Shell shell;

        public ShellTimeoutTimerTask(Shell shell) {
            this.shell = shell;
        }

        @Override
        public void run() {
            Process p = shell.getProcess();
            try {
                p.exitValue();
            } catch (Exception e) {
                //Process has not terminated.
                //So check if it has completed
                //if not just destroy it.
                if (p != null && !shell.completed.get()) {
                    shell.setTimedOut();
                    p.destroy();
                }
            }
        }
    }

    /**
     * this is an IOException with exit code added.
     */
    private static class ExitCodeException extends IOException {
        private final int exitCode;

        public ExitCodeException(int exitCode, String message) {
            super(message);
            this.exitCode = exitCode;
        }

        public int getExitCode() {
            return exitCode;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("ExitCodeException ");
            sb.append("exitCode=");
            sb.append(exitCode);
            sb.append(": ");
            sb.append(super.getMessage());
            return sb.toString();
        }
    }

    /**
     * concatenates strings, using a separator.
     * @param separator to join with
     * @param strings to join
     * @return  the joined string
     */
    private static String join(CharSequence separator, String[] strings) {
        // ideally we don't have to duplicate the code here if array is iterable.
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : strings) {
            if (first) {
                first = false;
            } else {
                sb.append(separator);
            }
            sb.append(s);
        }
        return sb.toString();
    }
}

