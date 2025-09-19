package ai.tegmentum.wasmtime4j.jni.async;

import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.async.AsyncEngine;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.JniEngine;
import ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of the AsyncEngine interface.
 *
 * <p>This implementation provides asynchronous WebAssembly operations using JNI
 * to interface with the native Wasmtime async runtime. All operations return
 * CompletableFuture instances that integrate with Java's async programming model.
 *
 * <p>The implementation uses defensive programming patterns to prevent JVM crashes
 * and provides comprehensive error handling and resource management.
 *
 * @since 1.0.0
 */
public final class JniAsyncEngine extends JniEngine implements AsyncEngine {
    private static final Logger LOGGER = Logger.getLogger(JniAsyncEngine.class.getName());

    private final ConcurrentHashMap<Long, AsyncOperationContext> activeOperations;
    private final AtomicLong operationCounter;
    private final Executor defaultAsyncExecutor;
    private final AsyncEngineStatisticsImpl statistics;
    private volatile Executor customAsyncExecutor;

    // Native method declarations
    private static native long nativeCreateAsyncEngine(long configHandle);
    private static native long nativeCompileModuleAsync(long engineHandle, byte[] wasmBytes, long operationId);
    private static native long nativeCompileModuleAsyncStream(long engineHandle, long operationId);
    private static native long nativeValidateModuleAsync(long engineHandle, byte[] wasmBytes, long operationId);
    private static native long nativeCreateStoreAsync(long engineHandle, long operationId);
    private static native int nativeCancelAsyncOperation(long engineHandle, long operationId);
    private static native int nativePollAsyncOperation(long engineHandle, long operationId);
    private static native long nativeGetAsyncOperationResult(long engineHandle, long operationId);
    private static native void nativeCleanupAsyncOperation(long engineHandle, long operationId);
    private static native void nativeGetAsyncStatistics(long engineHandle, long[] statsArray);

    static {
        try {
            System.loadLibrary("wasmtime4j");
        } catch (UnsatisfiedLinkError e) {
            LOGGER.log(Level.SEVERE, "Failed to load native library", e);
            throw new RuntimeException("Failed to load wasmtime4j native library", e);
        }
    }

    /**
     * Creates a new JNI async engine with the specified configuration.
     *
     * @param config the engine configuration
     * @throws WasmException if engine creation fails
     */
    public JniAsyncEngine(final EngineConfig config) throws WasmException {
        super(config);
        this.activeOperations = new ConcurrentHashMap<>();
        this.operationCounter = new AtomicLong(1);
        this.defaultAsyncExecutor = createDefaultAsyncExecutor();
        this.statistics = new AsyncEngineStatisticsImpl();

        // Initialize native async engine
        final long asyncEngineHandle = nativeCreateAsyncEngine(getHandle());
        if (asyncEngineHandle == 0) {
            throw new WasmException("Failed to create native async engine");
        }

        LOGGER.info("JNI async engine created successfully");
    }

    @Override
    public CompletableFuture<Module> compileModuleAsync(final byte[] wasmBytes) {
        JniValidation.requireNonNull(wasmBytes, "wasmBytes");
        return compileModuleAsync(wasmBytes, createDefaultCompilationOptions());
    }

    @Override
    public CompletableFuture<Module> compileModuleAsync(final InputStream wasmStream) {
        JniValidation.requireNonNull(wasmStream, "wasmStream");

        return CompletableFuture.supplyAsync(() -> {
            try {
                final byte[] wasmBytes = wasmStream.readAllBytes();
                return compileModuleAsync(wasmBytes).join();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read WASM bytes from stream", e);
            }
        }, getAsyncExecutor());
    }

    @Override
    public CompletableFuture<Module> compileModuleAsync(final byte[] wasmBytes, final CompilationOptions options) {
        JniValidation.requireNonNull(wasmBytes, "wasmBytes");
        JniValidation.requireNonNull(options, "options");

        if (!isValid()) {
            return CompletableFuture.failedFuture(new WasmException("Engine is not valid"));
        }

        final long operationId = operationCounter.getAndIncrement();
        final CompletableFuture<Module> future = new CompletableFuture<>();

        final AsyncOperationContext context = new AsyncOperationContext(
            operationId,
            AsyncOperationType.MODULE_COMPILATION,
            future,
            System.nanoTime()
        );

        activeOperations.put(operationId, context);
        statistics.incrementAsyncCompilationsStarted();

        // Apply timeout if specified
        if (options.getTimeout() != null) {
            future.orTimeout(options.getTimeout().toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        }

        final Executor executor = options.getExecutor() != null ? options.getExecutor() : getAsyncExecutor();

        executor.execute(() -> {
            try {
                final long nativeOperationId = nativeCompileModuleAsync(getHandle(), wasmBytes, operationId);
                if (nativeOperationId == 0) {
                    completeExceptionally(operationId, new WasmException("Failed to start async compilation"));
                    return;
                }

                // Start polling for completion
                pollOperationCompletion(operationId, executor);

            } catch (Exception e) {
                completeExceptionally(operationId, JniExceptionMapper.mapToWasmException(e));
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Module> compileModuleAsync(final InputStream wasmStream, final CompilationOptions options) {
        JniValidation.requireNonNull(wasmStream, "wasmStream");
        JniValidation.requireNonNull(options, "options");

        return CompletableFuture.supplyAsync(() -> {
            try {
                final byte[] wasmBytes = wasmStream.readAllBytes();
                return compileModuleAsync(wasmBytes, options).join();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read WASM bytes from stream", e);
            }
        }, options.getExecutor() != null ? options.getExecutor() : getAsyncExecutor());
    }

    @Override
    public CompletableFuture<Void> validateModuleAsync(final byte[] wasmBytes) {
        JniValidation.requireNonNull(wasmBytes, "wasmBytes");

        if (!isValid()) {
            return CompletableFuture.failedFuture(new WasmException("Engine is not valid"));
        }

        final long operationId = operationCounter.getAndIncrement();
        final CompletableFuture<Void> future = new CompletableFuture<>();

        final AsyncOperationContext context = new AsyncOperationContext(
            operationId,
            AsyncOperationType.MODULE_VALIDATION,
            future,
            System.nanoTime()
        );

        activeOperations.put(operationId, context);
        statistics.incrementAsyncValidations();

        getAsyncExecutor().execute(() -> {
            try {
                final long nativeOperationId = nativeValidateModuleAsync(getHandle(), wasmBytes, operationId);
                if (nativeOperationId == 0) {
                    completeExceptionally(operationId, new WasmException("Failed to start async validation"));
                    return;
                }

                pollOperationCompletion(operationId, getAsyncExecutor());

            } catch (Exception e) {
                completeExceptionally(operationId, JniExceptionMapper.mapToWasmException(e));
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Void> validateModuleAsync(final InputStream wasmStream) {
        JniValidation.requireNonNull(wasmStream, "wasmStream");

        return CompletableFuture.supplyAsync(() -> {
            try {
                final byte[] wasmBytes = wasmStream.readAllBytes();
                return validateModuleAsync(wasmBytes).join();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read WASM bytes from stream", e);
            }
        }, getAsyncExecutor());
    }

    @Override
    public CompletableFuture<Store> createStoreAsync() {
        return createStoreAsync(null);
    }

    @Override
    public CompletableFuture<Store> createStoreAsync(final Object data) {
        if (!isValid()) {
            return CompletableFuture.failedFuture(new WasmException("Engine is not valid"));
        }

        final long operationId = operationCounter.getAndIncrement();
        final CompletableFuture<Store> future = new CompletableFuture<>();

        final AsyncOperationContext context = new AsyncOperationContext(
            operationId,
            AsyncOperationType.STORE_CREATION,
            future,
            System.nanoTime()
        );

        activeOperations.put(operationId, context);
        statistics.incrementAsyncStoreCreations();

        getAsyncExecutor().execute(() -> {
            try {
                final long nativeOperationId = nativeCreateStoreAsync(getHandle(), operationId);
                if (nativeOperationId == 0) {
                    completeExceptionally(operationId, new WasmException("Failed to start async store creation"));
                    return;
                }

                pollOperationCompletion(operationId, getAsyncExecutor());

            } catch (Exception e) {
                completeExceptionally(operationId, JniExceptionMapper.mapToWasmException(e));
            }
        });

        return future;
    }

    @Override
    public Executor getAsyncExecutor() {
        return customAsyncExecutor != null ? customAsyncExecutor : defaultAsyncExecutor;
    }

    @Override
    public void setAsyncExecutor(final Executor executor) {
        this.customAsyncExecutor = executor;
    }

    @Override
    public AsyncEngineStatistics getAsyncStatistics() {
        updateNativeStatistics();
        return statistics;
    }

    @Override
    public void close() {
        LOGGER.info("Closing JNI async engine and cancelling active operations");

        // Cancel all active operations
        for (final AsyncOperationContext context : activeOperations.values()) {
            try {
                nativeCancelAsyncOperation(getHandle(), context.operationId);
                context.future.cancel(true);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to cancel operation " + context.operationId, e);
            }
        }

        activeOperations.clear();

        // Close the underlying engine
        super.close();
    }

    // Private helper methods

    private void pollOperationCompletion(final long operationId, final Executor executor) {
        executor.execute(() -> {
            try {
                final AsyncOperationContext context = activeOperations.get(operationId);
                if (context == null || context.future.isDone()) {
                    return;
                }

                final int status = nativePollAsyncOperation(getHandle(), operationId);

                switch (status) {
                    case 0: // Pending
                        // Schedule next poll
                        try {
                            Thread.sleep(10); // Brief pause before next poll
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            completeExceptionally(operationId, new WasmException("Operation interrupted"));
                            return;
                        }
                        pollOperationCompletion(operationId, executor);
                        break;

                    case 1: // Completed successfully
                        final long resultHandle = nativeGetAsyncOperationResult(getHandle(), operationId);
                        completeSuccessfully(operationId, resultHandle);
                        break;

                    case -1: // Failed
                        completeExceptionally(operationId, new WasmException("Async operation failed"));
                        break;

                    case -2: // Cancelled
                        context.future.cancel(true);
                        cleanupOperation(operationId);
                        break;

                    default:
                        completeExceptionally(operationId, new WasmException("Unknown operation status: " + status));
                        break;
                }
            } catch (Exception e) {
                completeExceptionally(operationId, JniExceptionMapper.mapToWasmException(e));
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void completeSuccessfully(final long operationId, final long resultHandle) {
        final AsyncOperationContext context = activeOperations.get(operationId);
        if (context == null) {
            return;
        }

        try {
            final Object result = createResultFromHandle(context.operationType, resultHandle);
            ((CompletableFuture<Object>) context.future).complete(result);

            final long durationNanos = System.nanoTime() - context.startTime;
            updateSuccessStatistics(context.operationType, durationNanos);

        } catch (Exception e) {
            completeExceptionally(operationId, JniExceptionMapper.mapToWasmException(e));
        } finally {
            cleanupOperation(operationId);
        }
    }

    @SuppressWarnings("unchecked")
    private void completeExceptionally(final long operationId, final Throwable throwable) {
        final AsyncOperationContext context = activeOperations.get(operationId);
        if (context == null) {
            return;
        }

        ((CompletableFuture<Object>) context.future).completeExceptionally(throwable);

        updateFailureStatistics(context.operationType);
        cleanupOperation(operationId);
    }

    private void cleanupOperation(final long operationId) {
        activeOperations.remove(operationId);

        try {
            nativeCleanupAsyncOperation(getHandle(), operationId);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to cleanup native operation " + operationId, e);
        }
    }

    private Object createResultFromHandle(final AsyncOperationType operationType, final long resultHandle) throws WasmException {
        switch (operationType) {
            case MODULE_COMPILATION:
                // Create Module from native handle
                return createModuleFromHandle(resultHandle);

            case STORE_CREATION:
                // Create Store from native handle
                return createStoreFromHandle(resultHandle);

            case MODULE_VALIDATION:
                // Validation returns void
                return null;

            default:
                throw new WasmException("Unknown operation type: " + operationType);
        }
    }

    private Module createModuleFromHandle(final long moduleHandle) throws WasmException {
        // Implementation would create a JniModule instance from the native handle
        // This is a placeholder - actual implementation would use proper JNI module creation
        throw new UnsupportedOperationException("createModuleFromHandle not yet implemented");
    }

    private Store createStoreFromHandle(final long storeHandle) throws WasmException {
        // Implementation would create a JniStore instance from the native handle
        // This is a placeholder - actual implementation would use proper JNI store creation
        throw new UnsupportedOperationException("createStoreFromHandle not yet implemented");
    }

    private CompilationOptions createDefaultCompilationOptions() {
        return new CompilationOptionsImpl(
            8192, // buffer size
            false, // progress tracking disabled
            null, // no timeout
            null, // use default executor
            false, // streaming disabled
            -1 // unlimited memory
        );
    }

    private Executor createDefaultAsyncExecutor() {
        return ForkJoinPool.commonPool();
    }

    private void updateNativeStatistics() {
        final long[] statsArray = new long[8];
        try {
            nativeGetAsyncStatistics(getHandle(), statsArray);

            statistics.updateFromNative(
                statsArray[0], // async compilations started
                statsArray[1], // async compilations completed
                statsArray[2], // async compilations failed
                statsArray[3], // async validations
                statsArray[4], // async store creations
                statsArray[5], // average compilation time ms
                statsArray[6], // average validation time ms
                (int) statsArray[7] // active async operations
            );
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to update native statistics", e);
        }
    }

    private void updateSuccessStatistics(final AsyncOperationType operationType, final long durationNanos) {
        final double durationMs = durationNanos / 1_000_000.0;

        switch (operationType) {
            case MODULE_COMPILATION:
                statistics.incrementAsyncCompilationsCompleted();
                statistics.updateAverageCompilationTime(durationMs);
                break;

            case MODULE_VALIDATION:
                statistics.updateAverageValidationTime(durationMs);
                break;

            case STORE_CREATION:
                // Store creation statistics handled by base statistics
                break;
        }
    }

    private void updateFailureStatistics(final AsyncOperationType operationType) {
        switch (operationType) {
            case MODULE_COMPILATION:
                statistics.incrementAsyncCompilationsFailed();
                break;

            case MODULE_VALIDATION:
            case STORE_CREATION:
                // Other failure statistics handled by base statistics
                break;
        }
    }

    // Inner classes and enums

    private enum AsyncOperationType {
        MODULE_COMPILATION,
        MODULE_VALIDATION,
        STORE_CREATION
    }

    private static final class AsyncOperationContext {
        final long operationId;
        final AsyncOperationType operationType;
        final CompletableFuture<?> future;
        final long startTime;

        AsyncOperationContext(final long operationId, final AsyncOperationType operationType,
                            final CompletableFuture<?> future, final long startTime) {
            this.operationId = operationId;
            this.operationType = operationType;
            this.future = future;
            this.startTime = startTime;
        }
    }

    private static final class CompilationOptionsImpl implements CompilationOptions {
        private final int bufferSize;
        private final boolean progressTrackingEnabled;
        private final Duration timeout;
        private final Executor executor;
        private final boolean streamingEnabled;
        private final long maxMemoryUsage;

        CompilationOptionsImpl(final int bufferSize, final boolean progressTrackingEnabled,
                              final Duration timeout, final Executor executor,
                              final boolean streamingEnabled, final long maxMemoryUsage) {
            this.bufferSize = bufferSize;
            this.progressTrackingEnabled = progressTrackingEnabled;
            this.timeout = timeout;
            this.executor = executor;
            this.streamingEnabled = streamingEnabled;
            this.maxMemoryUsage = maxMemoryUsage;
        }

        @Override
        public int getBufferSize() {
            return bufferSize;
        }

        @Override
        public boolean isProgressTrackingEnabled() {
            return progressTrackingEnabled;
        }

        @Override
        public Duration getTimeout() {
            return timeout;
        }

        @Override
        public Executor getExecutor() {
            return executor;
        }

        @Override
        public boolean isStreamingEnabled() {
            return streamingEnabled;
        }

        @Override
        public long getMaxMemoryUsage() {
            return maxMemoryUsage;
        }
    }

    private static final class AsyncEngineStatisticsImpl implements AsyncEngineStatistics {
        private final AtomicLong asyncCompilationsStarted = new AtomicLong();
        private final AtomicLong asyncCompilationsCompleted = new AtomicLong();
        private final AtomicLong asyncCompilationsFailed = new AtomicLong();
        private final AtomicLong asyncValidations = new AtomicLong();
        private final AtomicLong asyncStoreCreations = new AtomicLong();
        private volatile double averageCompilationTimeMs = 0.0;
        private volatile double averageValidationTimeMs = 0.0;
        private volatile int activeAsyncOperations = 0;

        void incrementAsyncCompilationsStarted() {
            asyncCompilationsStarted.incrementAndGet();
        }

        void incrementAsyncCompilationsCompleted() {
            asyncCompilationsCompleted.incrementAndGet();
        }

        void incrementAsyncCompilationsFailed() {
            asyncCompilationsFailed.incrementAndGet();
        }

        void incrementAsyncValidations() {
            asyncValidations.incrementAndGet();
        }

        void incrementAsyncStoreCreations() {
            asyncStoreCreations.incrementAndGet();
        }

        void updateAverageCompilationTime(final double timeMs) {
            // Simple moving average - in production, this would use a more sophisticated approach
            final long completedCount = asyncCompilationsCompleted.get();
            if (completedCount > 0) {
                averageCompilationTimeMs = ((averageCompilationTimeMs * (completedCount - 1)) + timeMs) / completedCount;
            }
        }

        void updateAverageValidationTime(final double timeMs) {
            final long validationCount = asyncValidations.get();
            if (validationCount > 0) {
                averageValidationTimeMs = ((averageValidationTimeMs * (validationCount - 1)) + timeMs) / validationCount;
            }
        }

        void updateFromNative(final long compilationsStarted, final long compilationsCompleted,
                             final long compilationsFailed, final long validations,
                             final long storeCreations, final long avgCompilationTimeMs,
                             final long avgValidationTimeMs, final int activeOps) {
            asyncCompilationsStarted.set(compilationsStarted);
            asyncCompilationsCompleted.set(compilationsCompleted);
            asyncCompilationsFailed.set(compilationsFailed);
            asyncValidations.set(validations);
            asyncStoreCreations.set(storeCreations);
            averageCompilationTimeMs = avgCompilationTimeMs;
            averageValidationTimeMs = avgValidationTimeMs;
            activeAsyncOperations = activeOps;
        }

        @Override
        public long getAsyncCompilationsStarted() {
            return asyncCompilationsStarted.get();
        }

        @Override
        public long getAsyncCompilationsCompleted() {
            return asyncCompilationsCompleted.get();
        }

        @Override
        public long getAsyncCompilationsFailed() {
            return asyncCompilationsFailed.get();
        }

        @Override
        public long getAsyncValidations() {
            return asyncValidations.get();
        }

        @Override
        public long getAsyncStoreCreations() {
            return asyncStoreCreations.get();
        }

        @Override
        public double getAverageCompilationTimeMs() {
            return averageCompilationTimeMs;
        }

        @Override
        public double getAverageValidationTimeMs() {
            return averageValidationTimeMs;
        }

        @Override
        public int getActiveAsyncOperations() {
            return activeAsyncOperations;
        }
    }
}