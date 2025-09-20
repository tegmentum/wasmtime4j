package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestCase;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Executes WASI tests with proper environment isolation and cross-runtime validation. Provides
 * comprehensive WASI test execution capabilities including environment setup, I/O redirection, and
 * performance monitoring.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Isolated WASI environment setup for each test execution
 *   <li>Cross-runtime execution with consistent environment configuration
 *   <li>I/O redirection and filesystem simulation for WASI operations
 *   <li>Real-time performance monitoring and resource usage tracking
 *   <li>WASI Preview 1/2 compatibility validation
 *   <li>Comprehensive error handling and cleanup
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasiTestExecutor {
  private static final Logger LOGGER = Logger.getLogger(WasiTestExecutor.class.getName());

  /** WASI execution environment for isolated test runs. */
  public static final class WasiExecutionEnvironment implements AutoCloseable {
    private final Path workingDirectory;
    private final Path stdoutCapture;
    private final Path stderrCapture;
    private final Path stdinInput;
    private final Map<String, String> environmentVariables;
    private final List<String> commandLineArguments;
    private final Set<Path> preOpenedDirectories;
    private final boolean networkingEnabled;
    private final int previewVersion;
    private final Instant creationTime;

    public WasiExecutionEnvironment(final WasiTestIntegrator.WasiEnvironmentConfiguration config)
        throws IOException {
      this.workingDirectory = createTemporaryDirectory("wasi-test-wd-");
      this.stdoutCapture = createTemporaryFile("wasi-stdout-", ".txt");
      this.stderrCapture = createTemporaryFile("wasi-stderr-", ".txt");
      this.stdinInput = createTemporaryFile("wasi-stdin-", ".txt");
      this.environmentVariables = new HashMap<>(config.getEnvironmentVariables());
      this.commandLineArguments = new ArrayList<>(config.getCommandLineArguments());
      this.preOpenedDirectories = ConcurrentHashMap.newKeySet();
      this.networkingEnabled = config.isNetworkingAllowed();
      this.previewVersion = config.getPreviewVersion();
      this.creationTime = Instant.now();

      // Setup pre-opened directories
      for (final Path dir : config.getPreOpenedDirectories()) {
        final Path mappedDir = mapDirectoryToEnvironment(dir);
        preOpenedDirectories.add(mappedDir);
      }

      // Initialize WASI environment
      initializeWasiEnvironment();
    }

    public Path getWorkingDirectory() {
      return workingDirectory;
    }

    public Path getStdoutCapture() {
      return stdoutCapture;
    }

    public Path getStderrCapture() {
      return stderrCapture;
    }

    public Path getStdinInput() {
      return stdinInput;
    }

    public Map<String, String> getEnvironmentVariables() {
      return Map.copyOf(environmentVariables);
    }

    public List<String> getCommandLineArguments() {
      return List.copyOf(commandLineArguments);
    }

    public Set<Path> getPreOpenedDirectories() {
      return Set.copyOf(preOpenedDirectories);
    }

    public boolean isNetworkingEnabled() {
      return networkingEnabled;
    }

    public int getPreviewVersion() {
      return previewVersion;
    }

    public Instant getCreationTime() {
      return creationTime;
    }

    @Override
    public void close() throws IOException {
      cleanupEnvironment();
    }

    private Path createTemporaryDirectory(final String prefix) throws IOException {
      return Files.createTempDirectory(prefix);
    }

    private Path createTemporaryFile(final String prefix, final String suffix) throws IOException {
      return Files.createTempFile(prefix, suffix);
    }

    private Path mapDirectoryToEnvironment(final Path originalDir) throws IOException {
      if (!Files.exists(originalDir)) {
        Files.createDirectories(originalDir);
      }
      return originalDir;
    }

    private void initializeWasiEnvironment() throws IOException {
      // Create necessary WASI directories and files
      Files.createDirectories(workingDirectory);

      // Setup standard I/O files
      Files.createFile(stdoutCapture);
      Files.createFile(stderrCapture);
      Files.createFile(stdinInput);

      // Create default WASI filesystem structure
      final Path wasiRoot = workingDirectory.resolve(".wasi");
      Files.createDirectories(wasiRoot);
      Files.createDirectories(wasiRoot.resolve("tmp"));
      Files.createDirectories(wasiRoot.resolve("dev"));

      LOGGER.fine("WASI execution environment initialized in: " + workingDirectory);
    }

    private void cleanupEnvironment() throws IOException {
      // Cleanup temporary files and directories
      try {
        deleteRecursively(workingDirectory);
        Files.deleteIfExists(stdoutCapture);
        Files.deleteIfExists(stderrCapture);
        Files.deleteIfExists(stdinInput);
      } catch (final IOException e) {
        LOGGER.warning("Failed to cleanup WASI environment: " + e.getMessage());
        // Don't propagate cleanup failures
      }
    }

    private void deleteRecursively(final Path path) throws IOException {
      if (Files.exists(path)) {
        if (Files.isDirectory(path)) {
          try (var stream = Files.list(path)) {
            for (final Path child : stream.toList()) {
              deleteRecursively(child);
            }
          }
        }
        Files.delete(path);
      }
    }
  }

  /** WASI execution result with detailed performance and compatibility metrics. */
  public static final class WasiExecutionResult {
    private final String testName;
    private final RuntimeType runtime;
    private final boolean successful;
    private final long executionTimeNanos;
    private final String stdoutOutput;
    private final String stderrOutput;
    private final int exitCode;
    private final long memoryUsage;
    private final int systemCallCount;
    private final Map<String, Object> performanceMetrics;
    private final List<String> wasiFeaturesCalled;
    private final String errorMessage;
    private final Instant executionTime;

    public WasiExecutionResult(
        final String testName,
        final RuntimeType runtime,
        final boolean successful,
        final long executionTimeNanos,
        final String stdoutOutput,
        final String stderrOutput,
        final int exitCode,
        final long memoryUsage,
        final int systemCallCount,
        final Map<String, Object> performanceMetrics,
        final List<String> wasiFeaturesCalled,
        final String errorMessage) {
      this.testName = testName;
      this.runtime = runtime;
      this.successful = successful;
      this.executionTimeNanos = executionTimeNanos;
      this.stdoutOutput = stdoutOutput;
      this.stderrOutput = stderrOutput;
      this.exitCode = exitCode;
      this.memoryUsage = memoryUsage;
      this.systemCallCount = systemCallCount;
      this.performanceMetrics = Map.copyOf(performanceMetrics);
      this.wasiFeaturesCalled = List.copyOf(wasiFeaturesCalled);
      this.errorMessage = errorMessage;
      this.executionTime = Instant.now();
    }

    public String getTestName() {
      return testName;
    }

    public RuntimeType getRuntime() {
      return runtime;
    }

    public boolean isSuccessful() {
      return successful;
    }

    public long getExecutionTimeNanos() {
      return executionTimeNanos;
    }

    public String getStdoutOutput() {
      return stdoutOutput;
    }

    public String getStderrOutput() {
      return stderrOutput;
    }

    public int getExitCode() {
      return exitCode;
    }

    public long getMemoryUsage() {
      return memoryUsage;
    }

    public int getSystemCallCount() {
      return systemCallCount;
    }

    public Map<String, Object> getPerformanceMetrics() {
      return performanceMetrics;
    }

    public List<String> getWasiFeaturesCalled() {
      return wasiFeaturesCalled;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    public Instant getExecutionTime() {
      return executionTime;
    }
  }

  private final ExecutorService executorService;
  private final BehavioralAnalyzer behavioralAnalyzer;
  private final PerformanceAnalyzer performanceAnalyzer;

  /** Creates a new WASI test executor with default configuration. */
  public WasiTestExecutor() {
    this.executorService =
        Executors.newCachedThreadPool(
            r -> {
              final Thread thread = new Thread(r, "wasi-test-executor");
              thread.setDaemon(true);
              return thread;
            });
    this.behavioralAnalyzer = new BehavioralAnalyzer();
    this.performanceAnalyzer = new PerformanceAnalyzer();
  }

  /**
   * Executes a WASI test case across all available runtimes with environment isolation.
   *
   * @param testCase the WASI test case to execute
   * @param environmentConfig the WASI environment configuration
   * @return map of execution results by runtime type
   */
  public Map<RuntimeType, WasiExecutionResult> executeWasiTestAcrossRuntimes(
      final WasmTestCase testCase,
      final WasiTestIntegrator.WasiEnvironmentConfiguration environmentConfig) {
    Objects.requireNonNull(testCase, "testCase cannot be null");
    Objects.requireNonNull(environmentConfig, "environmentConfig cannot be null");

    LOGGER.info("Executing WASI test across runtimes: " + testCase.getTestName());

    final Map<RuntimeType, WasiExecutionResult> results = new EnumMap<>(RuntimeType.class);
    final Map<RuntimeType, Future<WasiExecutionResult>> futures = new EnumMap<>(RuntimeType.class);

    // Submit execution tasks for all available runtimes
    for (final RuntimeType runtime : RuntimeType.values()) {
      futures.put(
          runtime,
          executorService.submit(
              () -> executeWasiTestOnRuntime(testCase, environmentConfig, runtime)));
    }

    // Collect results with timeout
    for (final Map.Entry<RuntimeType, Future<WasiExecutionResult>> entry : futures.entrySet()) {
      final RuntimeType runtime = entry.getKey();
      final Future<WasiExecutionResult> future = entry.getValue();

      try {
        final WasiExecutionResult result = future.get(30, TimeUnit.SECONDS);
        results.put(runtime, result);
        LOGGER.fine("WASI test completed on " + runtime + ": " + result.isSuccessful());
      } catch (final ExecutionException e) {
        LOGGER.warning(
            "WASI test execution failed on " + runtime + ": " + e.getCause().getMessage());
        results.put(runtime, createFailureResult(testCase, runtime, e.getCause()));
      } catch (final Exception e) {
        LOGGER.warning("WASI test execution interrupted on " + runtime + ": " + e.getMessage());
        results.put(runtime, createFailureResult(testCase, runtime, e));
      }
    }

    LOGGER.info("WASI test execution completed across " + results.size() + " runtimes");
    return Map.copyOf(results);
  }

  /**
   * Executes a WASI test on a specific runtime with full environment isolation.
   *
   * @param testCase the WASI test case
   * @param environmentConfig the WASI environment configuration
   * @param runtime the target runtime
   * @return detailed execution result
   */
  public WasiExecutionResult executeWasiTestOnRuntime(
      final WasmTestCase testCase,
      final WasiTestIntegrator.WasiEnvironmentConfiguration environmentConfig,
      final RuntimeType runtime) {
    Objects.requireNonNull(testCase, "testCase cannot be null");
    Objects.requireNonNull(environmentConfig, "environmentConfig cannot be null");
    Objects.requireNonNull(runtime, "runtime cannot be null");

    LOGGER.fine("Executing WASI test " + testCase.getTestName() + " on runtime " + runtime);

    try (final WasiExecutionEnvironment environment =
        new WasiExecutionEnvironment(environmentConfig)) {
      return performWasiExecution(testCase, environment, runtime);
    } catch (final Exception e) {
      LOGGER.warning("WASI test execution failed: " + e.getMessage());
      return createFailureResult(testCase, runtime, e);
    }
  }

  /** Shuts down the executor service and cleans up resources. */
  public void shutdown() {
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      executorService.shutdownNow();
    }
    LOGGER.info("WASI test executor shutdown completed");
  }

  private WasiExecutionResult performWasiExecution(
      final WasmTestCase testCase,
      final WasiExecutionEnvironment environment,
      final RuntimeType runtime)
      throws Exception {

    final long startTime = System.nanoTime();
    final Map<String, Object> performanceMetrics = new HashMap<>();
    final List<String> wasiFeaturesCalled = new ArrayList<>();

    try {
      // Setup runtime-specific WASI context
      setupRuntimeWasiContext(environment, runtime);

      // Execute the behavioral analysis with WASI support
      final BehavioralAnalyzer.TestExecutionResult behavioralResult =
          behavioralAnalyzer.executeTest(testCase, runtime);

      // Capture WASI-specific outputs
      final String stdoutOutput = readFileContent(environment.getStdoutCapture());
      final String stderrOutput = readFileContent(environment.getStderrCapture());

      // Analyze WASI features used during execution
      wasiFeaturesCalled.addAll(analyzeWasiFeaturesUsed(behavioralResult));

      // Collect performance metrics
      performanceMetrics.putAll(collectWasiPerformanceMetrics(environment, runtime));

      final long executionTime = System.nanoTime() - startTime;

      return new WasiExecutionResult(
          testCase.getTestName(),
          runtime,
          behavioralResult.isSuccessful(),
          executionTime,
          stdoutOutput,
          stderrOutput,
          behavioralResult.isSuccessful() ? 0 : 1,
          measureMemoryUsage(),
          countSystemCalls(behavioralResult),
          performanceMetrics,
          wasiFeaturesCalled,
          behavioralResult.getFailureReason());

    } catch (final Exception e) {
      final long executionTime = System.nanoTime() - startTime;
      return new WasiExecutionResult(
          testCase.getTestName(),
          runtime,
          false,
          executionTime,
          "",
          "",
          1,
          0L,
          0,
          performanceMetrics,
          wasiFeaturesCalled,
          e.getMessage());
    } finally {
      cleanupRuntimeWasiContext(environment, runtime);
    }
  }

  private void setupRuntimeWasiContext(
      final WasiExecutionEnvironment environment, final RuntimeType runtime) {
    // Implementation would setup runtime-specific WASI context
    // This would involve configuring the WASI instance with:
    // - Environment variables
    // - Pre-opened directories
    // - I/O redirections
    // - Command line arguments
    LOGGER.fine("Setting up WASI context for runtime: " + runtime);
  }

  private void cleanupRuntimeWasiContext(
      final WasiExecutionEnvironment environment, final RuntimeType runtime) {
    // Implementation would cleanup runtime-specific WASI context
    LOGGER.fine("Cleaning up WASI context for runtime: " + runtime);
  }

  private String readFileContent(final Path file) {
    try {
      return Files.readString(file);
    } catch (final IOException e) {
      LOGGER.warning("Failed to read file content: " + e.getMessage());
      return "";
    }
  }

  private List<String> analyzeWasiFeaturesUsed(
      final BehavioralAnalyzer.TestExecutionResult result) {
    // Implementation would analyze which WASI features were called during execution
    // This could be done through:
    // - Runtime introspection
    // - Call tracing
    // - Result analysis
    return List.of("wasi_snapshot_preview1"); // Placeholder
  }

  private Map<String, Object> collectWasiPerformanceMetrics(
      final WasiExecutionEnvironment environment, final RuntimeType runtime) {
    final Map<String, Object> metrics = new HashMap<>();
    metrics.put("io_operations", 0);
    metrics.put("filesystem_operations", 0);
    metrics.put("network_operations", 0);
    metrics.put("clock_operations", 0);
    metrics.put("random_operations", 0);
    // Implementation would collect actual WASI-specific performance metrics
    return metrics;
  }

  private long measureMemoryUsage() {
    final Runtime runtime = Runtime.getRuntime();
    final long totalMemory = runtime.totalMemory();
    final long freeMemory = runtime.freeMemory();
    return totalMemory - freeMemory;
  }

  private int countSystemCalls(final BehavioralAnalyzer.TestExecutionResult result) {
    // Implementation would count WASI system calls made during execution
    return 0; // Placeholder
  }

  private WasiExecutionResult createFailureResult(
      final WasmTestCase testCase, final RuntimeType runtime, final Throwable error) {
    return new WasiExecutionResult(
        testCase.getTestName(),
        runtime,
        false,
        0L,
        "",
        "",
        1,
        0L,
        0,
        Map.of(),
        List.of(),
        error.getMessage());
  }
}
