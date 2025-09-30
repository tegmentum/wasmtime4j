package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.Instance;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Represents a debug target in a multi-target debugging session.
 *
 * <p>A debug target wraps a WebAssembly instance and its associated debug session,
 * providing management and monitoring capabilities for multi-target scenarios.
 *
 * @since 1.0.0
 */
public final class DebugTarget implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(DebugTarget.class.getName());

    private final String targetId;
    private final Instance instance;
    private final DebugSession session;
    private final Debugger debugger;
    private final MultiTargetDebugManager manager;
    private final long creationTime;
    private final AtomicBoolean alive;
    private final AtomicLong lastActivityTime;

    DebugTarget(
            final String targetId,
            final Instance instance,
            final DebugSession session,
            final Debugger debugger,
            final MultiTargetDebugManager manager) {
        this.targetId = targetId;
        this.instance = instance;
        this.session = session;
        this.debugger = debugger;
        this.manager = manager;
        this.creationTime = System.currentTimeMillis();
        this.alive = new AtomicBoolean(true);
        this.lastActivityTime = new AtomicLong(creationTime);

        // Add event listener to monitor target activity
        session.addDebugEventListener(this::handleDebugEvent);
    }

    /**
     * Gets the target ID.
     *
     * @return the target ID
     */
    public String getTargetId() {
        return targetId;
    }

    /**
     * Gets the WebAssembly instance.
     *
     * @return the instance
     */
    public Instance getInstance() {
        return instance;
    }

    /**
     * Gets the debug session.
     *
     * @return the debug session
     */
    public DebugSession getSession() {
        return session;
    }

    /**
     * Gets the debugger.
     *
     * @return the debugger
     */
    public Debugger getDebugger() {
        return debugger;
    }

    /**
     * Gets the creation time.
     *
     * @return the creation time in milliseconds since epoch
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Gets the age of this target.
     *
     * @return the age in milliseconds
     */
    public long getAge() {
        return System.currentTimeMillis() - creationTime;
    }

    /**
     * Gets the last activity time.
     *
     * @return the last activity time in milliseconds since epoch
     */
    public long getLastActivityTime() {
        return lastActivityTime.get();
    }

    /**
     * Gets the time since last activity.
     *
     * @return the time since last activity in milliseconds
     */
    public long getTimeSinceLastActivity() {
        return System.currentTimeMillis() - lastActivityTime.get();
    }

    /**
     * Checks if the target is alive.
     *
     * @return true if the target is alive
     */
    public boolean isAlive() {
        return alive.get() && session.isActive();
    }

    /**
     * Gets target information summary.
     *
     * @return target information
     */
    public DebugTargetInfo getInfo() {
        return new DebugTargetInfo(
            targetId,
            instance.getClass().getSimpleName(),
            creationTime,
            lastActivityTime.get(),
            isAlive(),
            session.isActive() ? session.getExecutionState() : null
        );
    }

    @Override
    public void close() {
        if (!alive.compareAndSet(true, false)) {
            return;
        }

        try {
            if (session.isActive()) {
                session.close();
            }
        } catch (final Exception e) {
            LOGGER.warning("Error closing debug session for target: " + targetId + " - " + e.getMessage());
        }

        try {
            debugger.close();
        } catch (final Exception e) {
            LOGGER.warning("Error closing debugger for target: " + targetId + " - " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "DebugTarget{" +
               "targetId='" + targetId + '\'' +
               ", instance=" + instance.getClass().getSimpleName() +
               ", alive=" + isAlive() +
               ", age=" + getAge() + "ms" +
               ", lastActivity=" + getTimeSinceLastActivity() + "ms ago" +
               '}';
    }

    // Private methods

    private void handleDebugEvent(final DebugEvent event) {
        // Update last activity time
        lastActivityTime.set(System.currentTimeMillis());

        // Forward event to manager
        if (manager != null) {
            manager.notifyTargetEvent(this, event);
        }
    }

    /**
     * Debug target information record.
     *
     * @param targetId the target ID
     * @param instanceType the instance type name
     * @param creationTime the creation time
     * @param lastActivityTime the last activity time
     * @param alive whether the target is alive
     * @param executionState the current execution state
     */
    public record DebugTargetInfo(
            String targetId,
            String instanceType,
            long creationTime,
            long lastActivityTime,
            boolean alive,
            ExecutionState executionState
    ) {
        /**
         * Gets the target age in milliseconds.
         *
         * @return the age
         */
        public long getAge() {
            return System.currentTimeMillis() - creationTime;
        }

        /**
         * Gets the time since last activity in milliseconds.
         *
         * @return the time since last activity
         */
        public long getTimeSinceLastActivity() {
            return System.currentTimeMillis() - lastActivityTime;
        }

        /**
         * Formats the target information as a string.
         *
         * @return formatted information
         */
        public String format() {
            return String.format(
                "%s (%s): %s, age=%dms, lastActivity=%dms ago, state=%s",
                targetId,
                instanceType,
                alive ? "ALIVE" : "DEAD",
                getAge(),
                getTimeSinceLastActivity(),
                executionState != null ? executionState.getState() : "UNKNOWN"
            );
        }
    }
}