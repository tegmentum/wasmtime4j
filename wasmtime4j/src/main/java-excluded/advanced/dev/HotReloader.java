package ai.tegmentum.wasmtime4j.dev;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Hot-reloading development support for WebAssembly modules. Provides file system watching, module
 * recompilation, and state preservation across reloads.
 */
public final class HotReloader implements AutoCloseable {

  private final Engine engine;
  private final Map<Path, ModuleWatcher> watchers;
  private final Map<Path, ModuleState> moduleStates;
  private final List<ReloadListener> listeners;
  private final ScheduledExecutorService executor;
  private final WatchService watchService;
  private final long reloadHandle;
  private volatile boolean isActive;
  private volatile ReloadConfiguration configuration;

  /**
   * Creates a hot reloader with the given engine.
   *
   * @param engine The engine to use for recompilation
   * @throws IOException if watch service cannot be created
   * @throws IllegalArgumentException if engine is null
   */
  public HotReloader(final Engine engine) throws IOException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    this.engine = engine;
    this.watchers = new ConcurrentHashMap<>();
    this.moduleStates = new ConcurrentHashMap<>();
    this.listeners = new ArrayList<>();
    this.executor = Executors.newScheduledThreadPool(2);
    this.watchService = FileSystems.getDefault().newWatchService();
    this.reloadHandle = initializeHotReloader(engine);
    this.isActive = false;
    this.configuration = ReloadConfiguration.getDefault();
  }

  /**
   * Starts hot reloading for the specified module file.
   *
   * @param modulePath The path to the WebAssembly module file
   * @param instance The current instance to hot-reload
   * @return Hot reload session for managing the reload process
   * @throws IOException if file watching cannot be set up
   */
  public HotReloadSession startWatching(final Path modulePath, final Instance instance)
      throws IOException {
    if (modulePath == null) {
      throw new IllegalArgumentException("Module path cannot be null");
    }
    if (instance == null) {
      throw new IllegalArgumentException("Instance cannot be null");
    }

    final Path parentDir = modulePath.getParent();
    if (parentDir == null) {
      throw new IllegalArgumentException("Module path must have a parent directory");
    }

    final ModuleWatcher watcher = new ModuleWatcher(modulePath, instance);
    watchers.put(modulePath, watcher);

    // Capture initial state
    final ModuleState initialState = captureModuleState(instance);
    moduleStates.put(modulePath, initialState);

    // Set up file system watching
    final WatchKey watchKey =
        parentDir.register(
            watchService,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_CREATE);

    watcher.setWatchKey(watchKey);

    if (!isActive) {
      startWatchingService();
    }

    return new HotReloadSession(modulePath, this);
  }

  /**
   * Stops watching the specified module file.
   *
   * @param modulePath The path to stop watching
   */
  public void stopWatching(final Path modulePath) {
    final ModuleWatcher watcher = watchers.remove(modulePath);
    if (watcher != null) {
      watcher.cleanup();
    }
    moduleStates.remove(modulePath);

    if (watchers.isEmpty() && isActive) {
      stopWatchingService();
    }
  }

  /**
   * Manually triggers a reload for the specified module.
   *
   * @param modulePath The path to the module to reload
   * @return Completion future for the reload operation
   */
  public CompletableFuture<ReloadResult> triggerReload(final Path modulePath) {
    final ModuleWatcher watcher = watchers.get(modulePath);
    if (watcher == null) {
      return CompletableFuture.completedFuture(
          ReloadResult.failed("Module not being watched: " + modulePath));
    }

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return performReload(modulePath, watcher);
          } catch (final Exception e) {
            return ReloadResult.failed("Reload failed: " + e.getMessage());
          }
        },
        executor);
  }

  /**
   * Configures hot reload behavior.
   *
   * @param configuration The reload configuration
   */
  public void setConfiguration(final ReloadConfiguration configuration) {
    if (configuration == null) {
      throw new IllegalArgumentException("Configuration cannot be null");
    }
    this.configuration = configuration;
    updateNativeConfiguration(reloadHandle, configuration);
  }

  /**
   * Adds a reload listener to receive notifications about reload events.
   *
   * @param listener The listener to add
   */
  public void addReloadListener(final ReloadListener listener) {
    if (listener != null) {
      synchronized (listeners) {
        listeners.add(listener);
      }
    }
  }

  /**
   * Removes a reload listener.
   *
   * @param listener The listener to remove
   */
  public void removeReloadListener(final ReloadListener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
    }
  }

  /**
   * Gets the current reload statistics.
   *
   * @return Reload statistics
   */
  public ReloadStatistics getStatistics() {
    final long totalReloads =
        watchers.values().stream().mapToLong(ModuleWatcher::getReloadCount).sum();

    final long successfulReloads =
        watchers.values().stream().mapToLong(ModuleWatcher::getSuccessfulReloads).sum();

    final long failedReloads = totalReloads - successfulReloads;

    final double averageReloadTime =
        watchers.values().stream()
            .mapToDouble(ModuleWatcher::getAverageReloadTime)
            .average()
            .orElse(0.0);

    return new ReloadStatistics(
        totalReloads, successfulReloads, failedReloads, averageReloadTime, watchers.size());
  }

  /**
   * Precompiles a module for faster hot reloading.
   *
   * @param modulePath The path to the module to precompile
   * @return Precompilation result
   */
  public CompletableFuture<PrecompilationResult> precompileModule(final Path modulePath) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final byte[] moduleBytes = java.nio.file.Files.readAllBytes(modulePath);
            final Module module = Module.fromBinary(engine, moduleBytes);
            final byte[] compiled = precompileNative(reloadHandle, moduleBytes);

            return new PrecompilationResult(modulePath, compiled, true, null);
          } catch (final Exception e) {
            return new PrecompilationResult(modulePath, null, false, e.getMessage());
          }
        },
        executor);
  }

  @Override
  public void close() {
    if (isActive) {
      stopWatchingService();
    }

    for (final ModuleWatcher watcher : watchers.values()) {
      watcher.cleanup();
    }
    watchers.clear();
    moduleStates.clear();

    executor.shutdown();
    try {
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (final InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    try {
      watchService.close();
    } catch (final IOException e) {
      // Log but continue cleanup
    }

    cleanupNativeReloader(reloadHandle);
  }

  private void startWatchingService() {
    isActive = true;
    executor.submit(this::watchLoop);
  }

  private void stopWatchingService() {
    isActive = false;
  }

  private void watchLoop() {
    while (isActive) {
      try {
        final WatchKey key = watchService.poll(100, TimeUnit.MILLISECONDS);
        if (key != null) {
          processWatchEvents(key);
          key.reset();
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  private void processWatchEvents(final WatchKey key) {
    for (final WatchEvent<?> event : key.pollEvents()) {
      final WatchEvent.Kind<?> kind = event.kind();

      if (kind == StandardWatchEventKinds.OVERFLOW) {
        continue;
      }

      final Object context = event.context();
      if (context instanceof Path) {
        final Path changed = (Path) context;
        final Path fullPath = ((Path) key.watchable()).resolve(changed);

        handleFileChange(fullPath, kind);
      }
    }
  }

  private void handleFileChange(final Path changedPath, final WatchEvent.Kind<?> kind) {
    final ModuleWatcher watcher = watchers.get(changedPath);
    if (watcher == null) {
      return;
    }

    if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
      // Debounce rapid file changes
      watcher.scheduleReload(configuration.getDebounceDelayMs());
    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
      notifyListeners(listener -> listener.onModuleDeleted(changedPath));
    }
  }

  private ReloadResult performReload(final Path modulePath, final ModuleWatcher watcher) {
    final long startTime = System.nanoTime();

    try {
      // Read the updated module
      final byte[] moduleBytes = java.nio.file.Files.readAllBytes(modulePath);

      // Validate before reloading
      if (configuration.isValidationEnabled()) {
        final ValidationResult validation = validateModule(moduleBytes);
        if (!validation.isValid()) {
          return ReloadResult.failed("Module validation failed: " + validation.getError());
        }
      }

      // Create new module and instance
      final Module newModule = Module.fromBinary(engine, moduleBytes);
      final Store newStore = Store.newStore(engine);
      final Instance newInstance = Instance.newInstance(newStore, newModule, watcher.getImports());

      // Preserve state if enabled
      if (configuration.isStatePreservationEnabled()) {
        final ModuleState oldState = moduleStates.get(modulePath);
        if (oldState != null) {
          restoreModuleState(newInstance, oldState);
        }
      }

      // Update the watcher with the new instance
      final Instance oldInstance = watcher.getInstance();
      watcher.setInstance(newInstance);

      // Capture new state
      final ModuleState newState = captureModuleState(newInstance);
      moduleStates.put(modulePath, newState);

      final long reloadTime = System.nanoTime() - startTime;
      watcher.recordReload(reloadTime, true);

      final ReloadResult result =
          ReloadResult.successful(modulePath, oldInstance, newInstance, reloadTime);

      notifyListeners(listener -> listener.onReloadSuccessful(result));

      return result;

    } catch (final Exception e) {
      final long reloadTime = System.nanoTime() - startTime;
      watcher.recordReload(reloadTime, false);

      final ReloadResult result = ReloadResult.failed("Reload failed: " + e.getMessage());
      notifyListeners(listener -> listener.onReloadFailed(modulePath, e));

      return result;
    }
  }

  private ValidationResult validateModule(final byte[] moduleBytes) {
    try {
      // Basic WebAssembly validation
      Module.fromBinary(engine, moduleBytes);
      return ValidationResult.valid();
    } catch (final Exception e) {
      return ValidationResult.invalid(e.getMessage());
    }
  }

  private void notifyListeners(final Consumer<ReloadListener> action) {
    final List<ReloadListener> currentListeners;
    synchronized (listeners) {
      currentListeners = new ArrayList<>(listeners);
    }

    for (final ReloadListener listener : currentListeners) {
      try {
        action.accept(listener);
      } catch (final Exception e) {
        // Log but continue with other listeners
      }
    }
  }

  private native long initializeHotReloader(Engine engine);

  private native void updateNativeConfiguration(long handle, ReloadConfiguration config);

  private native ModuleState captureModuleState(Instance instance);

  private native void restoreModuleState(Instance instance, ModuleState state);

  private native byte[] precompileNative(long handle, byte[] moduleBytes);

  private native void cleanupNativeReloader(long handle);

  /** Hot reload session for managing a specific module's hot reload lifecycle. */
  public static final class HotReloadSession implements AutoCloseable {
    private final Path modulePath;
    private final HotReloader reloader;

    private HotReloadSession(final Path modulePath, final HotReloader reloader) {
      this.modulePath = modulePath;
      this.reloader = reloader;
    }

    /**
     * Gets the module path being watched.
     *
     * @return The module path
     */
    public Path getModulePath() {
      return modulePath;
    }

    /**
     * Manually triggers a reload.
     *
     * @return Completion future for the reload operation
     */
    public CompletableFuture<ReloadResult> reload() {
      return reloader.triggerReload(modulePath);
    }

    /**
     * Precompiles the module for faster reloading.
     *
     * @return Precompilation result
     */
    public CompletableFuture<PrecompilationResult> precompile() {
      return reloader.precompileModule(modulePath);
    }

    @Override
    public void close() {
      reloader.stopWatching(modulePath);
    }
  }

  /** Module watcher internal state. */
  private static final class ModuleWatcher {
    private final Path modulePath;
    private volatile Instance instance;
    private volatile WatchKey watchKey;
    private final Map<String, Object> imports;
    private long reloadCount;
    private long successfulReloads;
    private long totalReloadTime;
    private CompletableFuture<Void> pendingReload;

    private ModuleWatcher(final Path modulePath, final Instance instance) {
      this.modulePath = modulePath;
      this.instance = instance;
      this.imports = new HashMap<>(); // TODO: Extract imports from instance
      this.reloadCount = 0;
      this.successfulReloads = 0;
      this.totalReloadTime = 0;
    }

    public Instance getInstance() {
      return instance;
    }

    public void setInstance(final Instance instance) {
      this.instance = instance;
    }

    public Map<String, Object> getImports() {
      return imports;
    }

    public long getReloadCount() {
      return reloadCount;
    }

    public long getSuccessfulReloads() {
      return successfulReloads;
    }

    public double getAverageReloadTime() {
      return reloadCount > 0 ? (double) totalReloadTime / reloadCount : 0.0;
    }

    public void setWatchKey(final WatchKey watchKey) {
      this.watchKey = watchKey;
    }

    public void scheduleReload(final long delayMs) {
      if (pendingReload != null && !pendingReload.isDone()) {
        pendingReload.cancel(false);
      }

      pendingReload =
          CompletableFuture.runAsync(
              () -> {
                try {
                  Thread.sleep(delayMs);
                  // Trigger reload through the parent reloader
                } catch (final InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
              });
    }

    public void recordReload(final long reloadTime, final boolean successful) {
      reloadCount++;
      totalReloadTime += reloadTime;
      if (successful) {
        successfulReloads++;
      }
    }

    public void cleanup() {
      if (watchKey != null) {
        watchKey.cancel();
      }
      if (pendingReload != null && !pendingReload.isDone()) {
        pendingReload.cancel(true);
      }
    }
  }

  /** Hot reload configuration. */
  public static final class ReloadConfiguration {
    private final boolean validationEnabled;
    private final boolean statePreservationEnabled;
    private final long debounceDelayMs;
    private final boolean precompilationEnabled;
    private final int maxReloadAttempts;

    public ReloadConfiguration(
        final boolean validationEnabled,
        final boolean statePreservationEnabled,
        final long debounceDelayMs,
        final boolean precompilationEnabled,
        final int maxReloadAttempts) {
      this.validationEnabled = validationEnabled;
      this.statePreservationEnabled = statePreservationEnabled;
      this.debounceDelayMs = debounceDelayMs;
      this.precompilationEnabled = precompilationEnabled;
      this.maxReloadAttempts = maxReloadAttempts;
    }

    public static ReloadConfiguration getDefault() {
      return new ReloadConfiguration(true, true, 100, true, 3);
    }

    public boolean isValidationEnabled() {
      return validationEnabled;
    }

    public boolean isStatePreservationEnabled() {
      return statePreservationEnabled;
    }

    public long getDebounceDelayMs() {
      return debounceDelayMs;
    }

    public boolean isPrecompilationEnabled() {
      return precompilationEnabled;
    }

    public int getMaxReloadAttempts() {
      return maxReloadAttempts;
    }
  }

  /** Reload result. */
  public static final class ReloadResult {
    private final boolean successful;
    private final String error;
    private final Path modulePath;
    private final Instance oldInstance;
    private final Instance newInstance;
    private final long reloadTimeNs;

    private ReloadResult(
        final boolean successful,
        final String error,
        final Path modulePath,
        final Instance oldInstance,
        final Instance newInstance,
        final long reloadTimeNs) {
      this.successful = successful;
      this.error = error;
      this.modulePath = modulePath;
      this.oldInstance = oldInstance;
      this.newInstance = newInstance;
      this.reloadTimeNs = reloadTimeNs;
    }

    public static ReloadResult successful(
        final Path modulePath,
        final Instance oldInstance,
        final Instance newInstance,
        final long reloadTimeNs) {
      return new ReloadResult(true, null, modulePath, oldInstance, newInstance, reloadTimeNs);
    }

    public static ReloadResult failed(final String error) {
      return new ReloadResult(false, error, null, null, null, 0);
    }

    public boolean isSuccessful() {
      return successful;
    }

    public String getError() {
      return error;
    }

    public Path getModulePath() {
      return modulePath;
    }

    public Instance getOldInstance() {
      return oldInstance;
    }

    public Instance getNewInstance() {
      return newInstance;
    }

    public long getReloadTimeNs() {
      return reloadTimeNs;
    }
  }

  /** Reload statistics. */
  public static final class ReloadStatistics {
    private final long totalReloads;
    private final long successfulReloads;
    private final long failedReloads;
    private final double averageReloadTime;
    private final int activeWatchers;

    public ReloadStatistics(
        final long totalReloads,
        final long successfulReloads,
        final long failedReloads,
        final double averageReloadTime,
        final int activeWatchers) {
      this.totalReloads = totalReloads;
      this.successfulReloads = successfulReloads;
      this.failedReloads = failedReloads;
      this.averageReloadTime = averageReloadTime;
      this.activeWatchers = activeWatchers;
    }

    public long getTotalReloads() {
      return totalReloads;
    }

    public long getSuccessfulReloads() {
      return successfulReloads;
    }

    public long getFailedReloads() {
      return failedReloads;
    }

    public double getAverageReloadTime() {
      return averageReloadTime;
    }

    public int getActiveWatchers() {
      return activeWatchers;
    }

    public double getSuccessRate() {
      return totalReloads > 0 ? (double) successfulReloads / totalReloads : 1.0;
    }
  }

  /** Module state for preservation across reloads. */
  public static final class ModuleState {
    private final Map<String, Object> globalValues;
    private final Map<String, byte[]> memoryContents;
    private final Map<String, Object> tableContents;

    public ModuleState(
        final Map<String, Object> globalValues,
        final Map<String, byte[]> memoryContents,
        final Map<String, Object> tableContents) {
      this.globalValues = Collections.unmodifiableMap(new HashMap<>(globalValues));
      this.memoryContents = Collections.unmodifiableMap(new HashMap<>(memoryContents));
      this.tableContents = Collections.unmodifiableMap(new HashMap<>(tableContents));
    }

    public Map<String, Object> getGlobalValues() {
      return globalValues;
    }

    public Map<String, byte[]> getMemoryContents() {
      return memoryContents;
    }

    public Map<String, Object> getTableContents() {
      return tableContents;
    }
  }

  /** Precompilation result. */
  public static final class PrecompilationResult {
    private final Path modulePath;
    private final byte[] compiledBytes;
    private final boolean successful;
    private final String error;

    public PrecompilationResult(
        final Path modulePath,
        final byte[] compiledBytes,
        final boolean successful,
        final String error) {
      this.modulePath = modulePath;
      this.compiledBytes = compiledBytes;
      this.successful = successful;
      this.error = error;
    }

    public Path getModulePath() {
      return modulePath;
    }

    public byte[] getCompiledBytes() {
      return compiledBytes;
    }

    public boolean isSuccessful() {
      return successful;
    }

    public String getError() {
      return error;
    }
  }

  /** Validation result. */
  private static final class ValidationResult {
    private final boolean valid;
    private final String error;

    private ValidationResult(final boolean valid, final String error) {
      this.valid = valid;
      this.error = error;
    }

    public static ValidationResult valid() {
      return new ValidationResult(true, null);
    }

    public static ValidationResult invalid(final String error) {
      return new ValidationResult(false, error);
    }

    public boolean isValid() {
      return valid;
    }

    public String getError() {
      return error;
    }
  }

  /** Reload event listener interface. */
  public interface ReloadListener {
    /**
     * Called when a reload is successful.
     *
     * @param result The reload result
     */
    void onReloadSuccessful(ReloadResult result);

    /**
     * Called when a reload fails.
     *
     * @param modulePath The path of the module that failed to reload
     * @param error The error that occurred
     */
    void onReloadFailed(Path modulePath, Exception error);

    /**
     * Called when a module file is deleted.
     *
     * @param modulePath The path of the deleted module
     */
    void onModuleDeleted(Path modulePath);
  }
}
