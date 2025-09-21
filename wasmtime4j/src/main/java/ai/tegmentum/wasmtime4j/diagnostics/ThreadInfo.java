package ai.tegmentum.wasmtime4j.diagnostics;

import java.util.Map;

/**
 * Thread execution information for WebAssembly diagnostics.
 *
 * <p>This interface provides information about the thread context when
 * an error occurred during WebAssembly execution.
 *
 * @since 1.0.0
 */
public interface ThreadInfo {

    /**
     * Gets the thread ID.
     *
     * @return the thread ID
     */
    long getThreadId();

    /**
     * Gets the thread name.
     *
     * @return the thread name
     */
    String getThreadName();

    /**
     * Gets the thread state.
     *
     * @return the thread state
     */
    Thread.State getThreadState();

    /**
     * Gets the thread priority.
     *
     * @return the thread priority
     */
    int getThreadPriority();

    /**
     * Checks if the thread is a daemon thread.
     *
     * @return true if the thread is a daemon
     */
    boolean isDaemon();

    /**
     * Gets additional thread properties.
     *
     * @return map of thread properties
     */
    Map<String, Object> getProperties();

    /**
     * Creates a ThreadInfo for the current thread.
     *
     * @return the current thread info
     */
    static ThreadInfo current() {
        final Thread currentThread = Thread.currentThread();
        return new ThreadInfoImpl(
            currentThread.getId(),
            currentThread.getName(),
            currentThread.getState(),
            currentThread.getPriority(),
            currentThread.isDaemon()
        );
    }

    /**
     * Simple implementation of ThreadInfo.
     */
    final class ThreadInfoImpl implements ThreadInfo {
        private final long threadId;
        private final String threadName;
        private final Thread.State threadState;
        private final int threadPriority;
        private final boolean daemon;

        public ThreadInfoImpl(final long threadId, final String threadName,
                             final Thread.State threadState, final int threadPriority,
                             final boolean daemon) {
            this.threadId = threadId;
            this.threadName = threadName;
            this.threadState = threadState;
            this.threadPriority = threadPriority;
            this.daemon = daemon;
        }

        @Override
        public long getThreadId() {
            return threadId;
        }

        @Override
        public String getThreadName() {
            return threadName;
        }

        @Override
        public Thread.State getThreadState() {
            return threadState;
        }

        @Override
        public int getThreadPriority() {
            return threadPriority;
        }

        @Override
        public boolean isDaemon() {
            return daemon;
        }

        @Override
        public Map<String, Object> getProperties() {
            return Map.of(
                "threadId", threadId,
                "threadName", threadName,
                "threadState", threadState.name(),
                "threadPriority", threadPriority,
                "daemon", daemon
            );
        }
    }
}