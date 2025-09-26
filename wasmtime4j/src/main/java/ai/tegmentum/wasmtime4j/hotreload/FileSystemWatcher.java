package ai.tegmentum.wasmtime4j.hotreload;

import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;

/**
 * File system watcher for triggering hot-reload operations when WebAssembly
 * modules are modified on disk.
 *
 * <p>This class monitors specified directories and files for changes, and
 * automatically triggers hot-reload operations through a registered
 * {@link HotReloadManager}. It supports:
 * <ul>
 *   <li>Recursive directory monitoring</li>
 *   <li>File pattern filtering (e.g., "*.wasm", "*.wit")</li>
 *   <li>Debouncing to handle rapid file changes</li>
 *   <li>Automatic component version detection</li>
 *   <li>Configurable reload strategies per component</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * HotReloadManager manager = HotReloadManager.create(engine, config);
 * FileSystemWatcher watcher = new FileSystemWatcher(manager);
 *
 * // Watch a directory for WebAssembly modules
 * WatchConfig watchConfig = WatchConfig.builder()
 *     .pattern("*.wasm")
 *     .debounceDelayMs(500)
 *     .reloadStrategy(SwapStrategy.canary(10.0f, 25.0f, 0.99f))
 *     .build();
 *
 * watcher.watchDirectory(Paths.get("/app/modules"), watchConfig);
 * watcher.start();
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe and uses a
 * background thread to monitor file system events.
 *
 * <p><strong>Resource Management:</strong> This class implements
 * {@link AutoCloseable} and should be used with try-with-resources.
 */
public final class FileSystemWatcher implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(FileSystemWatcher.class.getName());

    private final HotReloadManager hotReloadManager;
    private final WatchService watchService;
    private final ExecutorService executorService;
    private final ScheduledExecutorService debounceExecutor;

    private final Map<WatchKey, WatchEntry> watchKeys = new ConcurrentHashMap<>();
    private final Map<Path, ScheduledFuture<?>> debounceTimers = new ConcurrentHashMap<>();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Thread watchThread;

    /**
     * Creates a new file system watcher.
     *
     * @param hotReloadManager The hot reload manager to trigger reloads
     * @throws IllegalArgumentException if hotReloadManager is null
     * @throws WasmRuntimeException if the watch service cannot be created
     */
    public FileSystemWatcher(final HotReloadManager hotReloadManager) {
        if (hotReloadManager == null) {
            throw new IllegalArgumentException("Hot reload manager cannot be null");
        }

        this.hotReloadManager = hotReloadManager;

        try {
            this.watchService = FileSystems.getDefault().newWatchService();
        } catch (final IOException e) {
            throw new WasmRuntimeException("Failed to create watch service", e);
        }

        this.executorService = Executors.newCachedThreadPool(r -> {
            final Thread thread = new Thread(r, "hotreload-watcher");
            thread.setDaemon(true);
            return thread;
        });

        this.debounceExecutor = Executors.newScheduledThreadPool(2, r -> {
            final Thread thread = new Thread(r, "hotreload-debounce");
            thread.setDaemon(true);
            return thread;
        });

        logger.info("File system watcher created");
    }

    /**
     * Watches a directory for changes.
     *
     * @param directory The directory to watch
     * @param config The watch configuration
     * @throws IllegalArgumentException if directory or config is null, or directory doesn't exist
     * @throws WasmRuntimeException if the directory cannot be watched
     */
    public void watchDirectory(final Path directory, final WatchConfig config) {
        if (directory == null) {
            throw new IllegalArgumentException("Directory cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Watch config cannot be null");
        }
        if (!Files.exists(directory)) {
            throw new IllegalArgumentException("Directory does not exist: " + directory);
        }
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Path is not a directory: " + directory);
        }

        logger.info(String.format("Adding watch for directory: %s", directory));

        try {
            final WatchKey watchKey = directory.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);

            final WatchEntry entry = new WatchEntry(directory, config, WatchType.DIRECTORY);
            watchKeys.put(watchKey, entry);

            logger.fine(String.format("Watch registered for directory: %s", directory));

        } catch (final IOException e) {
            throw new WasmRuntimeException("Failed to watch directory: " + directory, e);
        }
    }

    /**
     * Watches a specific file for changes.
     *
     * @param file The file to watch
     * @param config The watch configuration
     * @throws IllegalArgumentException if file or config is null, or file doesn't exist
     * @throws WasmRuntimeException if the file cannot be watched
     */
    public void watchFile(final Path file, final WatchConfig config) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Watch config cannot be null");
        }
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("File does not exist: " + file);
        }
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Path is not a regular file: " + file);
        }

        logger.info(String.format("Adding watch for file: %s", file));

        // Watch the parent directory
        final Path directory = file.getParent();
        if (directory == null) {
            throw new IllegalArgumentException("File has no parent directory: " + file);
        }

        try {
            final WatchKey watchKey = directory.register(watchService,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);

            final WatchEntry entry = new WatchEntry(file, config, WatchType.FILE);
            watchKeys.put(watchKey, entry);

            logger.fine(String.format("Watch registered for file: %s", file));

        } catch (final IOException e) {
            throw new WasmRuntimeException("Failed to watch file: " + file, e);
        }
    }

    /**
     * Starts the file system watcher.
     *
     * @throws IllegalStateException if already started
     */
    public void start() {
        if (running.getAndSet(true)) {
            throw new IllegalStateException("File system watcher is already running");
        }

        logger.info("Starting file system watcher");

        watchThread = new Thread(this::watchLoop, "filesystem-watcher");
        watchThread.setDaemon(true);
        watchThread.start();
    }

    /**
     * Stops the file system watcher.
     */
    public void stop() {
        if (!running.getAndSet(false)) {
            return; // Already stopped
        }

        logger.info("Stopping file system watcher");

        if (watchThread != null) {
            watchThread.interrupt();
        }

        // Cancel all debounce timers
        debounceTimers.values().forEach(future -> future.cancel(false));
        debounceTimers.clear();
    }

    /**
     * Checks if the watcher is running.
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Gets the number of paths being watched.
     *
     * @return The watch count
     */
    public int getWatchCount() {
        return watchKeys.size();
    }

    @Override
    public void close() {
        stop();

        executorService.shutdown();
        debounceExecutor.shutdown();

        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!debounceExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                debounceExecutor.shutdownNow();
            }
        } catch (final InterruptedException e) {
            executorService.shutdownNow();
            debounceExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        try {
            watchService.close();
        } catch (final IOException e) {
            logger.log(Level.WARNING, "Error closing watch service", e);
        }

        watchKeys.clear();

        logger.info("File system watcher closed");
    }

    /**
     * Main watch loop that processes file system events.
     */
    private void watchLoop() {
        logger.fine("File system watch loop started");

        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                final WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                if (key != null) {
                    processWatchKey(key);
                    if (!key.reset()) {
                        logger.warning("Watch key is no longer valid, removing");
                        watchKeys.remove(key);
                    }
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (final Exception e) {
                logger.log(Level.WARNING, "Error in watch loop", e);
            }
        }

        logger.fine("File system watch loop ended");
    }

    /**
     * Processes events for a watch key.
     */
    private void processWatchKey(final WatchKey key) {
        final WatchEntry entry = watchKeys.get(key);
        if (entry == null) {
            logger.warning("No watch entry found for key, ignoring events");
            return;
        }

        for (final WatchEvent<?> event : key.pollEvents()) {
            if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                logger.warning("Watch event overflow occurred");
                continue;
            }

            final Object context = event.context();
            if (!(context instanceof Path)) {
                continue;
            }

            final Path changed = (Path) context;
            final Path fullPath = entry.watchPath.resolve(changed);

            if (entry.watchType == WatchType.FILE && !fullPath.equals(entry.watchPath)) {
                continue; // Not the file we're watching
            }

            if (entry.config.shouldIgnore(fullPath)) {
                logger.fine(String.format("Ignoring change to: %s", fullPath));
                continue;
            }

            logger.fine(String.format("Detected change: %s -> %s", event.kind().name(), fullPath));

            handleFileChange(fullPath, event.kind(), entry.config);
        }
    }

    /**
     * Handles a file change event with debouncing.
     */
    private void handleFileChange(final Path path, final WatchEvent.Kind<?> eventKind, final WatchConfig config) {
        // Cancel existing timer for this path
        final ScheduledFuture<?> existingTimer = debounceTimers.remove(path);
        if (existingTimer != null) {
            existingTimer.cancel(false);
        }

        // Schedule new timer
        final ScheduledFuture<?> timer = debounceExecutor.schedule(() -> {
            debounceTimers.remove(path);
            processFileChange(path, eventKind, config);
        }, config.getDebounceDelayMs(), TimeUnit.MILLISECONDS);

        debounceTimers.put(path, timer);
    }

    /**
     * Processes a debounced file change.
     */
    private void processFileChange(final Path path, final WatchEvent.Kind<?> eventKind, final WatchConfig config) {
        try {
            if (eventKind == StandardWatchEventKinds.ENTRY_DELETE) {
                logger.info(String.format("File deleted: %s", path));
                // Could trigger cleanup or version rollback
                return;
            }

            if (eventKind == StandardWatchEventKinds.ENTRY_MODIFY ||
                eventKind == StandardWatchEventKinds.ENTRY_CREATE) {

                logger.info(String.format("File modified: %s, triggering hot reload", path));

                // Extract component name and version from path
                final String componentName = config.getComponentNameExtractor().apply(path);
                final String targetVersion = config.getVersionExtractor().apply(path);

                if (componentName == null || targetVersion == null) {
                    logger.warning(String.format("Could not extract component info from path: %s", path));
                    return;
                }

                // Trigger hot swap
                executorService.submit(() -> {
                    try {
                        final String operationId = hotReloadManager.startHotSwap(
                                componentName, targetVersion, config.getReloadStrategy());

                        logger.info(String.format("Hot swap triggered: %s -> %s (operation: %s)",
                                                componentName, targetVersion, operationId));

                        // Optionally monitor the operation
                        if (config.isMonitorProgress()) {
                            monitorSwapProgress(operationId);
                        }

                    } catch (final Exception e) {
                        logger.log(Level.SEVERE, String.format("Failed to trigger hot swap for: %s", path), e);
                    }
                });
            }

        } catch (final Exception e) {
            logger.log(Level.WARNING, String.format("Error processing file change: %s", path), e);
        }
    }

    /**
     * Monitors the progress of a hot swap operation.
     */
    private void monitorSwapProgress(final String operationId) {
        executorService.submit(() -> {
            try {
                HotReloadManager.HotSwapStatus status;
                do {
                    Thread.sleep(1000); // Poll every second
                    status = hotReloadManager.getSwapStatus(operationId);

                    if (status != null) {
                        logger.info(String.format("Hot swap progress: %s - %.1f%% (%s)",
                                                operationId, status.getProgress() * 100, status.getStatus()));

                        if (status.getStatus().isTerminal()) {
                            if (status.getStatus() == HotReloadManager.SwapStatus.COMPLETED) {
                                logger.info(String.format("Hot swap completed successfully: %s", operationId));
                            } else {
                                logger.warning(String.format("Hot swap failed or was rolled back: %s (%s)",
                                                            operationId, status.getStatus()));
                            }
                            break;
                        }
                    }
                } while (status != null && !Thread.currentThread().isInterrupted());

            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (final Exception e) {
                logger.log(Level.WARNING, String.format("Error monitoring swap progress: %s", operationId), e);
            }
        });
    }

    /**
     * Watch configuration for file system monitoring.
     */
    public static final class WatchConfig {
        private final List<String> patterns;
        private final List<String> ignorePatterns;
        private final long debounceDelayMs;
        private final HotReloadManager.SwapStrategy reloadStrategy;
        private final ComponentNameExtractor componentNameExtractor;
        private final VersionExtractor versionExtractor;
        private final boolean monitorProgress;

        private WatchConfig(final Builder builder) {
            this.patterns = Collections.unmodifiableList(new ArrayList<>(builder.patterns));
            this.ignorePatterns = Collections.unmodifiableList(new ArrayList<>(builder.ignorePatterns));
            this.debounceDelayMs = builder.debounceDelayMs;
            this.reloadStrategy = builder.reloadStrategy;
            this.componentNameExtractor = builder.componentNameExtractor;
            this.versionExtractor = builder.versionExtractor;
            this.monitorProgress = builder.monitorProgress;
        }

        public static Builder builder() {
            return new Builder();
        }

        public List<String> getPatterns() { return patterns; }
        public List<String> getIgnorePatterns() { return ignorePatterns; }
        public long getDebounceDelayMs() { return debounceDelayMs; }
        public HotReloadManager.SwapStrategy getReloadStrategy() { return reloadStrategy; }
        public ComponentNameExtractor getComponentNameExtractor() { return componentNameExtractor; }
        public VersionExtractor getVersionExtractor() { return versionExtractor; }
        public boolean isMonitorProgress() { return monitorProgress; }

        /**
         * Checks if a path should be ignored based on patterns.
         */
        boolean shouldIgnore(final Path path) {
            final String fileName = path.getFileName().toString();

            // Check ignore patterns first
            for (final String ignorePattern : ignorePatterns) {
                if (fileName.matches(ignorePattern.replace("*", ".*").replace("?", "."))) {
                    return true;
                }
            }

            // If no patterns specified, don't ignore
            if (patterns.isEmpty()) {
                return false;
            }

            // Check include patterns
            for (final String pattern : patterns) {
                if (fileName.matches(pattern.replace("*", ".*").replace("?", "."))) {
                    return false;
                }
            }

            return true; // Not matched by any include pattern
        }

        public static final class Builder {
            private final List<String> patterns = new ArrayList<>();
            private final List<String> ignorePatterns = new ArrayList<>();
            private long debounceDelayMs = 500;
            private HotReloadManager.SwapStrategy reloadStrategy = HotReloadManager.SwapStrategy.getDefault();
            private ComponentNameExtractor componentNameExtractor = new DefaultComponentNameExtractor();
            private VersionExtractor versionExtractor = new DefaultVersionExtractor();
            private boolean monitorProgress = true;

            public Builder pattern(final String pattern) {
                if (pattern != null && !pattern.trim().isEmpty()) {
                    patterns.add(pattern.trim());
                }
                return this;
            }

            public Builder ignorePattern(final String pattern) {
                if (pattern != null && !pattern.trim().isEmpty()) {
                    ignorePatterns.add(pattern.trim());
                }
                return this;
            }

            public Builder debounceDelayMs(final long delayMs) {
                if (delayMs < 0) {
                    throw new IllegalArgumentException("Debounce delay must be non-negative");
                }
                this.debounceDelayMs = delayMs;
                return this;
            }

            public Builder reloadStrategy(final HotReloadManager.SwapStrategy strategy) {
                this.reloadStrategy = strategy != null ? strategy : HotReloadManager.SwapStrategy.getDefault();
                return this;
            }

            public Builder componentNameExtractor(final ComponentNameExtractor extractor) {
                this.componentNameExtractor = extractor != null ? extractor : new DefaultComponentNameExtractor();
                return this;
            }

            public Builder versionExtractor(final VersionExtractor extractor) {
                this.versionExtractor = extractor != null ? extractor : new DefaultVersionExtractor();
                return this;
            }

            public Builder monitorProgress(final boolean monitor) {
                this.monitorProgress = monitor;
                return this;
            }

            public WatchConfig build() {
                return new WatchConfig(this);
            }
        }
    }

    /**
     * Functional interface for extracting component names from file paths.
     */
    @FunctionalInterface
    public interface ComponentNameExtractor {
        String apply(Path path);
    }

    /**
     * Functional interface for extracting version information from file paths.
     */
    @FunctionalInterface
    public interface VersionExtractor {
        String apply(Path path);
    }

    /**
     * Default component name extractor that uses the file name without extension.
     */
    private static final class DefaultComponentNameExtractor implements ComponentNameExtractor {
        @Override
        public String apply(final Path path) {
            final String fileName = path.getFileName().toString();
            final int dotIndex = fileName.lastIndexOf('.');
            return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        }
    }

    /**
     * Default version extractor that generates timestamps.
     */
    private static final class DefaultVersionExtractor implements VersionExtractor {
        @Override
        public String apply(final Path path) {
            return String.valueOf(System.currentTimeMillis());
        }
    }

    /**
     * Internal watch entry data.
     */
    private static final class WatchEntry {
        final Path watchPath;
        final WatchConfig config;
        final WatchType watchType;

        WatchEntry(final Path watchPath, final WatchConfig config, final WatchType watchType) {
            this.watchPath = watchPath;
            this.config = config;
            this.watchType = watchType;
        }
    }

    /**
     * Watch type enumeration.
     */
    private enum WatchType {
        FILE,
        DIRECTORY
    }
}