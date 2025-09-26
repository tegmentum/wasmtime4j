package ai.tegmentum.wasmtime4j.debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Synchronizes debugging operations across multiple targets.
 *
 * <p>This class provides synchronization capabilities for multi-target debugging,
 * ensuring coordinated execution steps, breakpoint handling, and state management
 * across all debug targets.
 *
 * @since 1.0.0
 */
final class DebugSynchronizer {

    private static final Logger LOGGER = Logger.getLogger(DebugSynchronizer.class.getName());

    private static final int DEFAULT_SYNC_TIMEOUT_MS = 10000; // 10 seconds
    private static final int BARRIER_WAIT_INTERVAL_MS = 50;

    /**
     * Synchronizes a step operation across all targets.
     *
     * <p>This method ensures that all targets step together, waiting for the slowest
     * target to complete before considering the operation finished.
     *
     * @param targets the collection of targets to step
     * @param stepType the type of step to perform
     * @return future containing the multi-target execution result
     */
    CompletableFuture<MultiTargetExecutionResult> synchronizeStep(
            final Collection<DebugTarget> targets,
            final StepType stepType) {

        if (targets.isEmpty()) {
            return CompletableFuture.completedFuture(
                new MultiTargetExecutionResult("sync-step", List.of(), System.currentTimeMillis())
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            final String operationName = "sync-step-" + stepType.name().toLowerCase();
            final long startTime = System.currentTimeMillis();

            try {
                // Phase 1: Prepare all targets for synchronized stepping
                final SyncBarrier prepareBarrier = new SyncBarrier(targets.size(), "prepare-step");
                final List<CompletableFuture<Void>> prepareFutures = targets.stream()
                    .map(target -> prepareTargetForSync(target, prepareBarrier))
                    .collect(Collectors.toList());

                // Wait for all targets to be ready
                CompletableFuture.allOf(prepareFutures.toArray(new CompletableFuture[0]))
                    .get(DEFAULT_SYNC_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                LOGGER.fine("All targets prepared for synchronized step: " + stepType);

                // Phase 2: Execute synchronized step
                final SyncBarrier stepBarrier = new SyncBarrier(targets.size(), "execute-step");
                final List<CompletableFuture<TargetExecutionResult>> stepFutures = targets.stream()
                    .map(target -> executeStepWithBarrier(target, stepType, stepBarrier))
                    .collect(Collectors.toList());

                // Wait for all steps to complete
                final List<TargetExecutionResult> results = CompletableFuture.allOf(stepFutures.toArray(new CompletableFuture[0]))
                    .thenApply(ignored -> stepFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()))
                    .get(DEFAULT_SYNC_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                LOGGER.fine("Synchronized step completed for all targets");

                return new MultiTargetExecutionResult(operationName, results, startTime);

            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Error during synchronized step: " + stepType, e);

                // Create error results for all targets
                final List<TargetExecutionResult> errorResults = targets.stream()
                    .map(target -> new TargetExecutionResult(target.getTargetId(), null, e))
                    .collect(Collectors.toList());

                return new MultiTargetExecutionResult(operationName, errorResults, startTime);
            }
        });
    }

    /**
     * Synchronizes breakpoint hits across targets.
     *
     * <p>When a global breakpoint is hit on one target, this method can coordinate
     * the response across all targets (e.g., pause all targets).
     *
     * @param targets the collection of targets
     * @param breakpoint the breakpoint that was hit
     * @param hitTarget the target where the breakpoint was hit
     * @return future containing the coordination result
     */
    CompletableFuture<MultiTargetBreakpointResult> synchronizeBreakpointHit(
            final Collection<DebugTarget> targets,
            final GlobalBreakpoint breakpoint,
            final DebugTarget hitTarget) {

        return CompletableFuture.supplyAsync(() -> {
            final long startTime = System.currentTimeMillis();
            final List<TargetBreakpointResult> results = new ArrayList<>();

            LOGGER.fine("Synchronizing breakpoint hit: " + breakpoint.getId() + " on target: " + hitTarget.getTargetId());

            // Pause all other targets
            for (final DebugTarget target : targets) {
                if (target.equals(hitTarget)) {
                    // Target that hit the breakpoint is already paused
                    results.add(new TargetBreakpointResult(
                        target.getTargetId(),
                        TargetBreakpointAction.HIT,
                        null,
                        null
                    ));
                } else {
                    // Pause other targets
                    try {
                        final DebugEvent event = target.getSession().pause().get(5, TimeUnit.SECONDS);
                        results.add(new TargetBreakpointResult(
                            target.getTargetId(),
                            TargetBreakpointAction.PAUSED,
                            event,
                            null
                        ));
                    } catch (final Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to pause target after breakpoint hit: " + target.getTargetId(), e);
                        results.add(new TargetBreakpointResult(
                            target.getTargetId(),
                            TargetBreakpointAction.FAILED,
                            null,
                            e
                        ));
                    }
                }
            }

            return new MultiTargetBreakpointResult(
                breakpoint.getId(),
                hitTarget.getTargetId(),
                results,
                startTime
            );
        });
    }

    /**
     * Coordinates resume operations across targets.
     *
     * @param targets the collection of targets to resume
     * @return future containing the coordination result
     */
    CompletableFuture<MultiTargetExecutionResult> synchronizeResume(final Collection<DebugTarget> targets) {
        return CompletableFuture.supplyAsync(() -> {
            final String operationName = "sync-resume";
            final long startTime = System.currentTimeMillis();
            final List<TargetExecutionResult> results = new ArrayList<>();

            // Use a countdown latch to ensure all targets resume simultaneously
            final CountDownLatch resumeLatch = new CountDownLatch(targets.size());

            // Start resume operations on all targets
            final List<CompletableFuture<TargetExecutionResult>> futures = targets.stream()
                .map(target -> CompletableFuture.supplyAsync(() -> {
                    try {
                        // Wait for all targets to be ready
                        resumeLatch.countDown();
                        resumeLatch.await(DEFAULT_SYNC_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                        // Resume execution
                        final DebugEvent event = target.getSession().continueExecution().get();
                        return new TargetExecutionResult(target.getTargetId(), event, null);

                    } catch (final Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to resume target: " + target.getTargetId(), e);
                        return new TargetExecutionResult(target.getTargetId(), null, e);
                    }
                }))
                .collect(Collectors.toList());

            // Wait for all resume operations to complete
            try {
                for (final CompletableFuture<TargetExecutionResult> future : futures) {
                    results.add(future.get(DEFAULT_SYNC_TIMEOUT_MS, TimeUnit.MILLISECONDS));
                }
            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Error during synchronized resume", e);
            }

            return new MultiTargetExecutionResult(operationName, results, startTime);
        });
    }

    // Private methods

    private CompletableFuture<Void> prepareTargetForSync(final DebugTarget target, final SyncBarrier barrier) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Ensure target is in a state ready for synchronized operations
                if (target.getSession().getExecutionState().getState() == ExecutionStateType.Running) {
                    // Pause running targets
                    target.getSession().pause().get(5, TimeUnit.SECONDS);
                }

                // Wait at barrier
                barrier.await();

            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Failed to prepare target for sync: " + target.getTargetId(), e);
                throw new RuntimeException(e);
            }
        });
    }

    private CompletableFuture<TargetExecutionResult> executeStepWithBarrier(
            final DebugTarget target,
            final StepType stepType,
            final SyncBarrier barrier) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Wait at barrier before executing step
                barrier.await();

                // Execute step
                final DebugEvent event;
                switch (stepType) {
                    case INTO:
                        event = target.getSession().stepInto().get();
                        break;
                    case OVER:
                        event = target.getSession().stepOver().get();
                        break;
                    case OUT:
                        event = target.getSession().stepOut().get();
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown step type: " + stepType);
                }

                return new TargetExecutionResult(target.getTargetId(), event, null);

            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Failed to execute synchronized step on target: " + target.getTargetId(), e);
                return new TargetExecutionResult(target.getTargetId(), null, e);
            }
        });
    }

    /**
     * Simple synchronization barrier for coordinating operations.
     */
    private static final class SyncBarrier {
        private final int targetCount;
        private final String name;
        private final AtomicInteger arrived;
        private final Object lock;
        private volatile boolean released;

        SyncBarrier(final int targetCount, final String name) {
            this.targetCount = targetCount;
            this.name = name;
            this.arrived = new AtomicInteger(0);
            this.lock = new Object();
            this.released = false;
        }

        void await() throws InterruptedException {
            final int arrivedCount = arrived.incrementAndGet();

            if (arrivedCount == targetCount) {
                // Last thread to arrive, release all
                synchronized (lock) {
                    released = true;
                    lock.notifyAll();
                }
                LOGGER.finest("Barrier released: " + name + " (all " + targetCount + " arrived)");
            } else {
                // Wait for release
                synchronized (lock) {
                    while (!released) {
                        lock.wait(BARRIER_WAIT_INTERVAL_MS);
                    }
                }
            }
        }
    }

    /**
     * Step types for synchronization.
     */
    enum StepType {
        INTO,
        OVER,
        OUT
    }

    /**
     * Actions taken on targets during breakpoint coordination.
     */
    enum TargetBreakpointAction {
        HIT,      // Target hit the breakpoint
        PAUSED,   // Target was paused in response
        FAILED    // Failed to coordinate with target
    }
}