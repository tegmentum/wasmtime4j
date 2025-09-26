package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Multi-target debugging manager for concurrent WebAssembly instance debugging.
 *
 * <p>This class provides advanced debugging capabilities for managing multiple
 * WebAssembly instances simultaneously. It supports:
 * <ul>
 * <li>Concurrent debugging of multiple instances</li>
 * <li>Coordinated breakpoint management across targets</li>
 * <li>Synchronized stepping operations</li>
 * <li>Cross-instance variable inspection</li>
 * <li>Unified event handling and notifications</li>
 * <li>Performance monitoring across all targets</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * MultiTargetDebugManager manager = new MultiTargetDebugManager();
 *
 * // Add multiple targets
 * manager.addTarget("service1", instance1, debugConfig1);
 * manager.addTarget("service2", instance2, debugConfig2);
 *
 * // Set coordinated breakpoints
 * manager.setGlobalBreakpoint("main", 42);
 *
 * // Step all targets synchronously
 * manager.stepAllTargets(StepType.INTO).join();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class MultiTargetDebugManager implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(MultiTargetDebugManager.class.getName());

    private static final int DEFAULT_THREAD_POOL_SIZE = 10;
    private static final int SYNC_TIMEOUT_MS = 5000;
    private static final int STATUS_UPDATE_INTERVAL_MS = 1000;

    private final String managerId;
    private final ConcurrentMap<String, DebugTarget> targets;
    private final ConcurrentMap<String, GlobalBreakpoint> globalBreakpoints;
    private final List<MultiTargetEventListener> eventListeners;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final AtomicBoolean active;
    private final AtomicLong nextBreakpointId;
    private final DebugSynchronizer synchronizer;
    private final PerformanceMonitor performanceMonitor;

    /**
     * Creates a new multi-target debug manager.
     */
    public MultiTargetDebugManager() {
        this(DEFAULT_THREAD_POOL_SIZE);
    }

    /**
     * Creates a new multi-target debug manager with custom thread pool size.
     *
     * @param threadPoolSize the size of the thread pool for concurrent operations
     * @throws IllegalArgumentException if threadPoolSize is not positive
     */
    public MultiTargetDebugManager(final int threadPoolSize) {
        if (threadPoolSize <= 0) {
            throw new IllegalArgumentException("Thread pool size must be positive");
        }

        this.managerId = "multi-target-" + System.currentTimeMillis();
        this.targets = new ConcurrentHashMap<>();
        this.globalBreakpoints = new ConcurrentHashMap<>();
        this.eventListeners = Collections.synchronizedList(new ArrayList<>());
        this.executorService = Executors.newFixedThreadPool(threadPoolSize, r -> {
            final Thread thread = new Thread(r, "MultiTargetDebug-" + managerId);
            thread.setDaemon(true);
            return thread;
        });
        this.scheduledExecutorService = Executors.newScheduledThreadPool(2, r -> {
            final Thread thread = new Thread(r, "MultiTargetDebug-Scheduler-" + managerId);
            thread.setDaemon(true);
            return thread;
        });
        this.active = new AtomicBoolean(true);
        this.nextBreakpointId = new AtomicLong(1);
        this.synchronizer = new DebugSynchronizer();
        this.performanceMonitor = new PerformanceMonitor();

        startStatusMonitoring();

        LOGGER.info("Created multi-target debug manager: " + managerId);
    }

    /**
     * Adds a debug target to the manager.
     *
     * @param targetId unique identifier for the target
     * @param instance the WebAssembly instance to debug
     * @param config the debug configuration for this target
     * @return the created debug target
     * @throws WasmException if target cannot be added
     * @throws IllegalArgumentException if parameters are invalid
     * @throws IllegalStateException if manager is closed
     */
    public DebugTarget addTarget(final String targetId, final Instance instance, final DebugConfig config) throws WasmException {
        if (targetId == null || targetId.trim().isEmpty()) {
            throw new IllegalArgumentException("Target ID cannot be null or empty");
        }
        if (instance == null) {
            throw new IllegalArgumentException("Instance cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Debug config cannot be null");
        }
        if (!active.get()) {
            throw new IllegalStateException("Multi-target debug manager is closed");
        }

        if (targets.containsKey(targetId)) {
            throw new IllegalArgumentException("Target with ID already exists: " + targetId);
        }

        // Create debug session for the instance
        final Debugger debugger = instance.createDebugger();
        final DebugSession session = debugger.createSession(instance, config);

        // Create debug target wrapper
        final DebugTarget target = new DebugTarget(targetId, instance, session, debugger, this);
        targets.put(targetId, target);

        // Apply existing global breakpoints to the new target
        applyGlobalBreakpointsToTarget(target);

        // Notify listeners
        notifyTargetAdded(target);

        LOGGER.info("Added debug target: " + targetId + " (total targets: " + targets.size() + ")");
        return target;
    }

    /**
     * Removes a debug target from the manager.
     *
     * @param targetId the target ID to remove
     * @return true if the target was removed
     */
    public boolean removeTarget(final String targetId) {
        if (targetId == null) {
            return false;
        }

        final DebugTarget target = targets.remove(targetId);
        if (target != null) {
            // Close the target's debug session
            try {
                target.close();
            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Error closing debug target: " + targetId, e);
            }

            // Notify listeners
            notifyTargetRemoved(target);

            LOGGER.info("Removed debug target: " + targetId + " (remaining targets: " + targets.size() + ")");
            return true;
        }

        return false;
    }

    /**
     * Gets a debug target by ID.
     *
     * @param targetId the target ID
     * @return the debug target, or empty if not found
     */
    public Optional<DebugTarget> getTarget(final String targetId) {
        return Optional.ofNullable(targets.get(targetId));
    }

    /**
     * Gets all debug targets.
     *
     * @return immutable list of debug targets
     */
    public List<DebugTarget> getAllTargets() {
        return List.copyOf(targets.values());
    }

    /**
     * Gets targets that match the specified predicate.
     *
     * @param predicate the filter predicate
     * @return list of matching targets
     */
    public List<DebugTarget> getTargets(final Predicate<DebugTarget> predicate) {
        return targets.values().stream()
            .filter(predicate)
            .collect(Collectors.toList());
    }

    /**
     * Gets the number of active targets.
     *
     * @return the number of active targets
     */
    public int getTargetCount() {
        return targets.size();
    }

    /**
     * Sets a global breakpoint that applies to all targets.
     *
     * @param functionName the function name
     * @param line the line number
     * @return the global breakpoint
     * @throws WasmException if breakpoint cannot be set
     */
    public GlobalBreakpoint setGlobalBreakpoint(final String functionName, final int line) throws WasmException {
        final String breakpointId = "global-bp-" + nextBreakpointId.getAndIncrement();
        final GlobalBreakpoint globalBreakpoint = new GlobalBreakpoint(
            breakpointId,
            functionName,
            line,
            System.currentTimeMillis()
        );

        globalBreakpoints.put(breakpointId, globalBreakpoint);

        // Apply to all current targets
        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (final DebugTarget target : targets.values()) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    final Breakpoint targetBreakpoint = target.getSession().setBreakpoint(functionName, line);
                    globalBreakpoint.addTargetBreakpoint(target.getTargetId(), targetBreakpoint);
                } catch (final Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to set breakpoint on target: " + target.getTargetId(), e);
                }
            }, executorService));
        }

        // Wait for all breakpoints to be set
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Notify listeners
        notifyGlobalBreakpointSet(globalBreakpoint);

        LOGGER.info("Set global breakpoint: " + breakpointId + " (" + functionName + ":" + line + ")");
        return globalBreakpoint;
    }

    /**
     * Removes a global breakpoint.
     *
     * @param breakpointId the breakpoint ID
     * @return true if the breakpoint was removed
     */
    public boolean removeGlobalBreakpoint(final String breakpointId) {
        final GlobalBreakpoint globalBreakpoint = globalBreakpoints.remove(breakpointId);
        if (globalBreakpoint != null) {
            // Remove from all targets
            final List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (final Map.Entry<String, Breakpoint> entry : globalBreakpoint.getTargetBreakpoints().entrySet()) {
                final String targetId = entry.getKey();
                final Breakpoint breakpoint = entry.getValue();
                final DebugTarget target = targets.get(targetId);

                if (target != null) {
                    futures.add(CompletableFuture.runAsync(() -> {
                        try {
                            target.getSession().removeBreakpoint(breakpoint);
                        } catch (final Exception e) {
                            LOGGER.log(Level.WARNING, "Failed to remove breakpoint from target: " + targetId, e);
                        }
                    }, executorService));
                }
            }

            // Wait for all breakpoints to be removed
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Notify listeners
            notifyGlobalBreakpointRemoved(globalBreakpoint);

            LOGGER.info("Removed global breakpoint: " + breakpointId);
            return true;
        }

        return false;
    }

    /**
     * Gets all global breakpoints.
     *
     * @return immutable list of global breakpoints
     */
    public List<GlobalBreakpoint> getGlobalBreakpoints() {
        return List.copyOf(globalBreakpoints.values());
    }

    /**
     * Continues execution on all targets.
     *
     * @return future that completes when all targets have continued or stopped
     */
    public CompletableFuture<MultiTargetExecutionResult> continueAllTargets() {
        return executeOnAllTargets("continue", target ->
            target.getSession().continueExecution().thenApply(event ->
                new TargetExecutionResult(target.getTargetId(), event, null)
            )
        );
    }

    /**
     * Steps all targets with the specified step type.
     *
     * @param stepType the type of step to perform
     * @return future that completes when all targets have stepped
     */
    public CompletableFuture<MultiTargetExecutionResult> stepAllTargets(final StepType stepType) {
        return synchronizer.synchronizeStep(targets.values(), stepType);
    }

    /**
     * Pauses execution on all targets.
     *
     * @return future that completes when all targets have paused
     */
    public CompletableFuture<MultiTargetExecutionResult> pauseAllTargets() {
        return executeOnAllTargets("pause", target ->
            target.getSession().pause().thenApply(event ->
                new TargetExecutionResult(target.getTargetId(), event, null)
            )
        );
    }

    /**
     * Gets the execution state of all targets.
     *
     * @return map of target ID to execution state
     */
    public Map<String, ExecutionState> getAllExecutionStates() {
        return targets.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    try {
                        return entry.getValue().getSession().getExecutionState();
                    } catch (final Exception e) {
                        LOGGER.log(Level.WARNING, "Error getting execution state for target: " + entry.getKey(), e);
                        return null;
                    }
                }
            ));
    }

    /**
     * Gets variables from all targets at the current execution point.
     *
     * @return map of target ID to variables list
     */
    public Map<String, List<Variable>> getAllCurrentVariables() {
        return targets.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    try {
                        return entry.getValue().getSession().getCurrentVariables();
                    } catch (final Exception e) {
                        LOGGER.log(Level.WARNING, "Error getting variables for target: " + entry.getKey(), e);
                        return Collections.emptyList();
                    }
                }
            ));
    }

    /**
     * Evaluates an expression on all targets.
     *
     * @param expression the expression to evaluate
     * @return map of target ID to evaluation result
     */
    public Map<String, EvaluationResult> evaluateExpressionOnAllTargets(final String expression) {
        final Map<String, CompletableFuture<EvaluationResult>> futures = targets.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return entry.getValue().getSession().evaluateExpression(expression);
                    } catch (final Exception e) {
                        return new EvaluationResult(
                            false,
                            null,
                            null,
                            expression,
                            e.getMessage(),
                            0
                        );
                    }
                }, executorService)
            ));

        // Wait for all evaluations and collect results
        return futures.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().join()
            ));
    }

    /**
     * Gets performance metrics from all targets.
     *
     * @return aggregated performance metrics
     */
    public MultiTargetPerformanceMetrics getPerformanceMetrics() {
        return performanceMonitor.collectMetrics(targets.values());
    }

    /**
     * Adds a multi-target event listener.
     *
     * @param listener the event listener
     */
    public void addEventListener(final MultiTargetEventListener listener) {
        if (listener != null) {
            eventListeners.add(listener);
        }
    }

    /**
     * Removes a multi-target event listener.
     *
     * @param listener the event listener
     * @return true if the listener was removed
     */
    public boolean removeEventListener(final MultiTargetEventListener listener) {
        return eventListeners.remove(listener);
    }

    /**
     * Gets the manager ID.
     *
     * @return the manager ID
     */
    public String getManagerId() {
        return managerId;
    }

    /**
     * Checks if the manager is active.
     *
     * @return true if the manager is active
     */
    public boolean isActive() {
        return active.get();
    }

    @Override
    public void close() {
        if (!active.compareAndSet(true, false)) {
            return;
        }

        LOGGER.info("Closing multi-target debug manager: " + managerId);

        // Close all targets
        targets.values().parallelStream().forEach(target -> {
            try {
                target.close();
            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Error closing target: " + target.getTargetId(), e);
            }
        });
        targets.clear();

        // Clear breakpoints
        globalBreakpoints.clear();

        // Clear listeners
        eventListeners.clear();

        // Shutdown executors
        executorService.shutdown();
        scheduledExecutorService.shutdown();

        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutorService.shutdownNow();
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
            scheduledExecutorService.shutdownNow();
        }

        LOGGER.info("Multi-target debug manager closed");
    }

    // Private methods

    private CompletableFuture<MultiTargetExecutionResult> executeOnAllTargets(
            final String operationName,
            final java.util.function.Function<DebugTarget, CompletableFuture<TargetExecutionResult>> operation) {

        final List<CompletableFuture<TargetExecutionResult>> futures = targets.values().stream()
            .map(target -> operation.apply(target)
                .exceptionally(throwable -> new TargetExecutionResult(
                    target.getTargetId(),
                    null,
                    throwable
                ))
            )
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(ignored -> {
                final List<TargetExecutionResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

                return new MultiTargetExecutionResult(
                    operationName,
                    results,
                    System.currentTimeMillis()
                );
            });
    }

    private void applyGlobalBreakpointsToTarget(final DebugTarget target) {
        for (final GlobalBreakpoint globalBreakpoint : globalBreakpoints.values()) {
            CompletableFuture.runAsync(() -> {
                try {
                    final Breakpoint targetBreakpoint = target.getSession().setBreakpoint(
                        globalBreakpoint.getFunctionName(),
                        globalBreakpoint.getLine()
                    );
                    globalBreakpoint.addTargetBreakpoint(target.getTargetId(), targetBreakpoint);
                } catch (final Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to apply global breakpoint to target: " + target.getTargetId(), e);
                }
            }, executorService);
        }
    }

    private void startStatusMonitoring() {
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (!active.get()) {
                return;
            }

            try {
                // Update performance metrics
                final MultiTargetPerformanceMetrics metrics = getPerformanceMetrics();

                // Notify listeners of status update
                notifyStatusUpdate(metrics);

                // Check for dead targets
                final List<String> deadTargets = targets.entrySet().stream()
                    .filter(entry -> !entry.getValue().isAlive())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

                deadTargets.forEach(targetId -> {
                    LOGGER.warning("Removing dead target: " + targetId);
                    removeTarget(targetId);
                });

            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Error in status monitoring", e);
            }
        }, STATUS_UPDATE_INTERVAL_MS, STATUS_UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    // Event notification methods

    private void notifyTargetAdded(final DebugTarget target) {
        eventListeners.forEach(listener -> {
            try {
                listener.onTargetAdded(target);
            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Error notifying listener of target added", e);
            }
        });
    }

    private void notifyTargetRemoved(final DebugTarget target) {
        eventListeners.forEach(listener -> {
            try {
                listener.onTargetRemoved(target);
            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Error notifying listener of target removed", e);
            }
        });
    }

    private void notifyGlobalBreakpointSet(final GlobalBreakpoint breakpoint) {
        eventListeners.forEach(listener -> {
            try {
                listener.onGlobalBreakpointSet(breakpoint);
            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Error notifying listener of global breakpoint set", e);
            }
        });
    }

    private void notifyGlobalBreakpointRemoved(final GlobalBreakpoint breakpoint) {
        eventListeners.forEach(listener -> {
            try {
                listener.onGlobalBreakpointRemoved(breakpoint);
            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Error notifying listener of global breakpoint removed", e);
            }
        });
    }

    private void notifyStatusUpdate(final MultiTargetPerformanceMetrics metrics) {
        eventListeners.forEach(listener -> {
            try {
                listener.onStatusUpdate(metrics);
            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Error notifying listener of status update", e);
            }
        });
    }

    void notifyTargetEvent(final DebugTarget target, final DebugEvent event) {
        eventListeners.forEach(listener -> {
            try {
                listener.onTargetEvent(target, event);
            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Error notifying listener of target event", e);
            }
        });
    }
}