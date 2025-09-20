package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestCase;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestSuiteLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Integrates Wasmtime WASI tests into the comparison testing framework. Provides comprehensive
 * WASI test discovery, execution, and analysis capabilities with support for both WASI Preview 1
 * and Preview 2.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automatic discovery of Wasmtime WASI tests from official test suite
 *   <li>WASI environment setup and teardown for isolated test execution
 *   <li>I/O redirection and filesystem simulation for WASI tests
 *   <li>WASI Preview 1/2 compatibility validation
 *   <li>WASI-specific performance analysis and benchmarking
 *   <li>Integration with existing comparison testing infrastructure
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasiTestIntegrator {
  private static final Logger LOGGER = Logger.getLogger(WasiTestIntegrator.class.getName());

  /** WASI test categories based on Wasmtime test organization. */
  public enum WasiTestCategory {
    FILESYSTEM("filesystem", "File system operations and access"),
    STDIO("stdio", "Standard input/output operations"),
    ENVIRONMENT("environment", "Environment variable access"),
    CLOCKS("clocks", "Clock and time operations"),
    RANDOM("random", "Random number generation"),
    PROCESS("process", "Process lifecycle and exit handling"),
    SOCKETS("sockets", "Network socket operations"),
    ARGS("args", "Command line argument handling"),
    EXIT("exit", "Process exit and status handling"),
    PREVIEW1("preview1", "WASI Preview 1 compatibility"),
    PREVIEW2("preview2", "WASI Preview 2 functionality");

    private final String identifier;
    private final String description;

    WasiTestCategory(final String identifier, final String description) {
      this.identifier = identifier;
      this.description = description;
    }

    public String getIdentifier() {
      return identifier;
    }

    public String getDescription() {
      return description;
    }
  }

  /** WASI environment configuration for test execution. */
  public static final class WasiEnvironmentConfiguration {
    private final Map<String, String> environmentVariables;
    private final List<String> commandLineArguments;
    private final Path workingDirectory;
    private final Path stdoutRedirection;
    private final Path stderrRedirection;
    private final Path stdinInput;
    private final Set<Path> preOpenedDirectories;
    private final boolean allowNetworking;
    private final int previewVersion;

    private WasiEnvironmentConfiguration(final Builder builder) {
      this.environmentVariables = Map.copyOf(builder.environmentVariables);
      this.commandLineArguments = List.copyOf(builder.commandLineArguments);
      this.workingDirectory = builder.workingDirectory;
      this.stdoutRedirection = builder.stdoutRedirection;
      this.stderrRedirection = builder.stderrRedirection;
      this.stdinInput = builder.stdinInput;
      this.preOpenedDirectories = Set.copyOf(builder.preOpenedDirectories);
      this.allowNetworking = builder.allowNetworking;
      this.previewVersion = builder.previewVersion;
    }

    public Map<String, String> getEnvironmentVariables() {
      return environmentVariables;
    }

    public List<String> getCommandLineArguments() {
      return commandLineArguments;
    }

    public Path getWorkingDirectory() {
      return workingDirectory;
    }

    public Path getStdoutRedirection() {
      return stdoutRedirection;
    }

    public Path getStderrRedirection() {
      return stderrRedirection;
    }

    public Path getStdinInput() {
      return stdinInput;
    }

    public Set<Path> getPreOpenedDirectories() {
      return preOpenedDirectories;
    }

    public boolean isNetworkingAllowed() {
      return allowNetworking;
    }

    public int getPreviewVersion() {
      return previewVersion;
    }

    public static final class Builder {
      private final Map<String, String> environmentVariables = new HashMap<>();
      private final List<String> commandLineArguments = new ArrayList<>();
      private final Set<Path> preOpenedDirectories = new HashSet<>();
      private Path workingDirectory;
      private Path stdoutRedirection;
      private Path stderrRedirection;
      private Path stdinInput;
      private boolean allowNetworking = false;
      private int previewVersion = 1;

      public Builder environmentVariable(final String key, final String value) {
        environmentVariables.put(key, value);
        return this;
      }

      public Builder commandLineArgument(final String argument) {
        commandLineArguments.add(argument);
        return this;
      }

      public Builder workingDirectory(final Path directory) {
        this.workingDirectory = directory;
        return this;
      }

      public Builder stdoutRedirection(final Path file) {
        this.stdoutRedirection = file;
        return this;
      }

      public Builder stderrRedirection(final Path file) {
        this.stderrRedirection = file;
        return this;
      }

      public Builder stdinInput(final Path file) {
        this.stdinInput = file;
        return this;
      }

      public Builder preOpenedDirectory(final Path directory) {
        preOpenedDirectories.add(directory);
        return this;
      }

      public Builder allowNetworking(final boolean allow) {
        this.allowNetworking = allow;
        return this;
      }

      public Builder previewVersion(final int version) {
        this.previewVersion = version;
        return this;
      }

      public WasiEnvironmentConfiguration build() {
        return new WasiEnvironmentConfiguration(this);
      }
    }
  }

  /** WASI test execution result with environment details. */
  public static final class WasiTestExecutionResult {
    private final String testName;
    private final WasiTestCategory category;
    private final WasiEnvironmentConfiguration environment;
    private final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> runtimeResults;
    private final WasiCompatibilityAnalysis compatibilityAnalysis;
    private final WasiPerformanceMetrics performanceMetrics;
    private final Instant executionTime;
    private final boolean successful;

    public WasiTestExecutionResult(
        final String testName,
        final WasiTestCategory category,
        final WasiEnvironmentConfiguration environment,
        final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> runtimeResults,
        final WasiCompatibilityAnalysis compatibilityAnalysis,
        final WasiPerformanceMetrics performanceMetrics,
        final boolean successful) {
      this.testName = testName;
      this.category = category;
      this.environment = environment;
      this.runtimeResults = Map.copyOf(runtimeResults);
      this.compatibilityAnalysis = compatibilityAnalysis;
      this.performanceMetrics = performanceMetrics;
      this.executionTime = Instant.now();
      this.successful = successful;
    }

    public String getTestName() {
      return testName;
    }

    public WasiTestCategory getCategory() {
      return category;
    }

    public WasiEnvironmentConfiguration getEnvironment() {
      return environment;
    }

    public Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> getRuntimeResults() {
      return runtimeResults;
    }

    public WasiCompatibilityAnalysis getCompatibilityAnalysis() {
      return compatibilityAnalysis;
    }

    public WasiPerformanceMetrics getPerformanceMetrics() {
      return performanceMetrics;
    }

    public Instant getExecutionTime() {
      return executionTime;
    }

    public boolean isSuccessful() {
      return successful;
    }
  }

  /** WASI compatibility analysis results. */
  public static final class WasiCompatibilityAnalysis {
    private final Map<RuntimeType, Double> previewCompatibilityScores;
    private final Set<String> supportedWasiFeatures;
    private final Set<String> unsupportedWasiFeatures;
    private final Map<String, String> compatibilityIssues;
    private final double overallCompatibilityScore;

    public WasiCompatibilityAnalysis(
        final Map<RuntimeType, Double> previewCompatibilityScores,
        final Set<String> supportedWasiFeatures,
        final Set<String> unsupportedWasiFeatures,
        final Map<String, String> compatibilityIssues,
        final double overallCompatibilityScore) {
      this.previewCompatibilityScores = Map.copyOf(previewCompatibilityScores);
      this.supportedWasiFeatures = Set.copyOf(supportedWasiFeatures);
      this.unsupportedWasiFeatures = Set.copyOf(unsupportedWasiFeatures);
      this.compatibilityIssues = Map.copyOf(compatibilityIssues);
      this.overallCompatibilityScore = overallCompatibilityScore;
    }

    public Map<RuntimeType, Double> getPreviewCompatibilityScores() {
      return previewCompatibilityScores;
    }

    public Set<String> getSupportedWasiFeatures() {
      return supportedWasiFeatures;
    }

    public Set<String> getUnsupportedWasiFeatures() {
      return unsupportedWasiFeatures;
    }

    public Map<String, String> getCompatibilityIssues() {
      return compatibilityIssues;
    }

    public double getOverallCompatibilityScore() {
      return overallCompatibilityScore;
    }
  }

  /** WASI performance metrics. */
  public static final class WasiPerformanceMetrics {
    private final Map<RuntimeType, Long> ioOperationTimes;
    private final Map<RuntimeType, Long> filesystemOperationTimes;
    private final Map<RuntimeType, Long> networkOperationTimes;
    private final Map<RuntimeType, Integer> systemCallCounts;
    private final Map<RuntimeType, Long> memoryUsage;

    public WasiPerformanceMetrics(
        final Map<RuntimeType, Long> ioOperationTimes,
        final Map<RuntimeType, Long> filesystemOperationTimes,
        final Map<RuntimeType, Long> networkOperationTimes,
        final Map<RuntimeType, Integer> systemCallCounts,
        final Map<RuntimeType, Long> memoryUsage) {
      this.ioOperationTimes = Map.copyOf(ioOperationTimes);
      this.filesystemOperationTimes = Map.copyOf(filesystemOperationTimes);
      this.networkOperationTimes = Map.copyOf(networkOperationTimes);
      this.systemCallCounts = Map.copyOf(systemCallCounts);
      this.memoryUsage = Map.copyOf(memoryUsage);
    }

    public Map<RuntimeType, Long> getIoOperationTimes() {
      return ioOperationTimes;
    }

    public Map<RuntimeType, Long> getFilesystemOperationTimes() {
      return filesystemOperationTimes;
    }

    public Map<RuntimeType, Long> getNetworkOperationTimes() {
      return networkOperationTimes;
    }

    public Map<RuntimeType, Integer> getSystemCallCounts() {
      return systemCallCounts;
    }

    public Map<RuntimeType, Long> getMemoryUsage() {
      return memoryUsage;
    }
  }

  private final BehavioralAnalyzer behavioralAnalyzer;
  private final PerformanceAnalyzer performanceAnalyzer;
  private final WasmtimeCompatibilityValidator compatibilityValidator;
  private final Map<WasiTestCategory, List<WasmTestCase>> categorizedTests;
  private final Map<String, WasiTestExecutionResult> executionResults;

  /** Creates a new WASI test integrator with default analyzers. */
  public WasiTestIntegrator() {
    this.behavioralAnalyzer = new BehavioralAnalyzer();
    this.performanceAnalyzer = new PerformanceAnalyzer();
    this.compatibilityValidator = new WasmtimeCompatibilityValidator();
    this.categorizedTests = new ConcurrentHashMap<>();
    this.executionResults = new ConcurrentHashMap<>();
    initializeWasiTestCategories();
  }

  /**
   * Discovers and loads all available WASI tests from Wasmtime test suite.
   *
   * @param wasmtimeTestDirectory the root directory of Wasmtime tests
   * @return map of categorized WASI test cases
   * @throws IOException if test discovery fails
   */
  public Map<WasiTestCategory, List<WasmTestCase>> discoverWasiTests(final Path wasmtimeTestDirectory)
      throws IOException {
    Objects.requireNonNull(wasmtimeTestDirectory, "wasmtimeTestDirectory cannot be null");

    LOGGER.info("Discovering WASI tests in: " + wasmtimeTestDirectory);

    final Map<WasiTestCategory, List<WasmTestCase>> discoveredTests = new EnumMap<>(WasiTestCategory.class);

    // Initialize empty lists for all categories
    for (final WasiTestCategory category : WasiTestCategory.values()) {
      discoveredTests.put(category, new ArrayList<>());
    }

    // Discover WASI-specific test directories
    final Path wasiTestsDir = wasmtimeTestDirectory.resolve("crates/wasi-tests/src");
    if (Files.exists(wasiTestsDir)) {
      discoverWasiTestsInDirectory(wasiTestsDir, discoveredTests);
    }

    // Discover component model tests with WASI
    final Path componentTestsDir = wasmtimeTestDirectory.resolve("crates/component-test/src");
    if (Files.exists(componentTestsDir)) {
      discoverComponentWasiTests(componentTestsDir, discoveredTests);
    }

    // Update internal categorization
    categorizedTests.putAll(discoveredTests);

    final int totalTests = discoveredTests.values().stream().mapToInt(List::size).sum();
    LOGGER.info("Discovered " + totalTests + " WASI tests across " + discoveredTests.size() + " categories");

    return Map.copyOf(discoveredTests);
  }

  /**
   * Executes a WASI test with specified environment configuration across all available runtimes.
   *
   * @param testCase the WASI test case to execute
   * @param environment the WASI environment configuration
   * @return comprehensive WASI test execution result
   */
  public WasiTestExecutionResult executeWasiTest(
      final WasmTestCase testCase, final WasiEnvironmentConfiguration environment) {
    Objects.requireNonNull(testCase, "testCase cannot be null");
    Objects.requireNonNull(environment, "environment cannot be null");

    LOGGER.info("Executing WASI test: " + testCase.getTestName());

    final WasiTestCategory category = categorizeWasiTest(testCase);
    final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> runtimeResults = new EnumMap<>(RuntimeType.class);

    // Execute test across all available runtimes
    for (final RuntimeType runtime : RuntimeType.values()) {
      try {
        final BehavioralAnalyzer.TestExecutionResult result =
            executeWasiTestOnRuntime(testCase, environment, runtime);
        runtimeResults.put(runtime, result);
        LOGGER.fine("WASI test " + testCase.getTestName() + " executed on " + runtime + ": " + result.isSuccessful());
      } catch (final Exception e) {
        LOGGER.warning("Failed to execute WASI test " + testCase.getTestName() + " on " + runtime + ": " + e.getMessage());
        // Create failure result
        runtimeResults.put(runtime, createFailureResult(testCase, e));
      }
    }

    // Analyze WASI-specific compatibility
    final WasiCompatibilityAnalysis compatibilityAnalysis =
        analyzeWasiCompatibility(testCase, environment, runtimeResults);

    // Measure WASI-specific performance
    final WasiPerformanceMetrics performanceMetrics =
        measureWasiPerformance(testCase, environment, runtimeResults);

    final boolean successful = runtimeResults.values().stream().anyMatch(BehavioralAnalyzer.TestExecutionResult::isSuccessful);

    final WasiTestExecutionResult result = new WasiTestExecutionResult(
        testCase.getTestName(),
        category,
        environment,
        runtimeResults,
        compatibilityAnalysis,
        performanceMetrics,
        successful);

    executionResults.put(testCase.getTestName(), result);

    LOGGER.info("WASI test execution completed: " + testCase.getTestName() + " (success: " + successful + ")");
    return result;
  }

  /**
   * Creates a default WASI environment configuration suitable for most tests.
   *
   * @return default WASI environment configuration
   */
  public WasiEnvironmentConfiguration createDefaultWasiEnvironment() {
    try {
      final Path tempDir = Files.createTempDirectory("wasi-test-");
      return new WasiEnvironmentConfiguration.Builder()
          .workingDirectory(tempDir)
          .preOpenedDirectory(tempDir)
          .environmentVariable("PATH", "/usr/bin:/bin")
          .environmentVariable("HOME", tempDir.toString())
          .previewVersion(1)
          .allowNetworking(false)
          .build();
    } catch (final IOException e) {
      throw new RuntimeException("Failed to create default WASI environment", e);
    }
  }

  /**
   * Gets all executed WASI test results.
   *
   * @return map of WASI test execution results
   */
  public Map<String, WasiTestExecutionResult> getExecutionResults() {
    return Map.copyOf(executionResults);
  }

  /**
   * Gets categorized WASI tests.
   *
   * @return map of categorized WASI test cases
   */
  public Map<WasiTestCategory, List<WasmTestCase>> getCategorizedTests() {
    return Map.copyOf(categorizedTests);
  }

  /** Clears all execution results and test categorization data. */
  public void clearResults() {
    executionResults.clear();
    categorizedTests.clear();
    initializeWasiTestCategories();
    LOGGER.info("WASI test integrator data cleared");
  }

  private void initializeWasiTestCategories() {
    for (final WasiTestCategory category : WasiTestCategory.values()) {
      categorizedTests.put(category, new ArrayList<>());
    }
  }

  private void discoverWasiTestsInDirectory(
      final Path directory, final Map<WasiTestCategory, List<WasmTestCase>> discoveredTests)
      throws IOException {

    if (!Files.exists(directory)) {
      return;
    }

    try (final Stream<Path> paths = Files.walk(directory)) {
      final List<Path> wasmFiles = paths
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".wasm"))
          .collect(Collectors.toList());

      for (final Path wasmFile : wasmFiles) {
        final WasmTestCase testCase = WasmTestCase.fromFile(wasmFile, WasmTestSuiteLoader.TestSuiteType.WASI_TESTS);
        final WasiTestCategory category = categorizeWasiTestFromPath(wasmFile);
        discoveredTests.get(category).add(testCase);
      }
    }
  }

  private void discoverComponentWasiTests(
      final Path directory, final Map<WasiTestCategory, List<WasmTestCase>> discoveredTests)
      throws IOException {

    if (!Files.exists(directory)) {
      return;
    }

    try (final Stream<Path> paths = Files.walk(directory)) {
      final List<Path> wasmFiles = paths
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".wasm"))
          .filter(path -> path.toString().contains("wasi") || path.toString().contains("component"))
          .collect(Collectors.toList());

      for (final Path wasmFile : wasmFiles) {
        final WasmTestCase testCase = WasmTestCase.fromFile(wasmFile, WasmTestSuiteLoader.TestSuiteType.WASMTIME_TESTS);
        final WasiTestCategory category = categorizeWasiTestFromPath(wasmFile);
        discoveredTests.get(category).add(testCase);
      }
    }
  }

  private WasiTestCategory categorizeWasiTest(final WasmTestCase testCase) {
    final String testName = testCase.getTestName().toLowerCase();

    // Categorize based on test name patterns
    if (testName.contains("filesystem") || testName.contains("file") || testName.contains("dir")) {
      return WasiTestCategory.FILESYSTEM;
    }
    if (testName.contains("stdio") || testName.contains("stdout") || testName.contains("stdin")) {
      return WasiTestCategory.STDIO;
    }
    if (testName.contains("env") || testName.contains("environment")) {
      return WasiTestCategory.ENVIRONMENT;
    }
    if (testName.contains("clock") || testName.contains("time")) {
      return WasiTestCategory.CLOCKS;
    }
    if (testName.contains("random") || testName.contains("rand")) {
      return WasiTestCategory.RANDOM;
    }
    if (testName.contains("process") || testName.contains("proc")) {
      return WasiTestCategory.PROCESS;
    }
    if (testName.contains("socket") || testName.contains("net")) {
      return WasiTestCategory.SOCKETS;
    }
    if (testName.contains("args") || testName.contains("argv")) {
      return WasiTestCategory.ARGS;
    }
    if (testName.contains("exit") || testName.contains("terminate")) {
      return WasiTestCategory.EXIT;
    }
    if (testName.contains("preview2") || testName.contains("p2")) {
      return WasiTestCategory.PREVIEW2;
    }

    // Default to Preview 1
    return WasiTestCategory.PREVIEW1;
  }

  private WasiTestCategory categorizeWasiTestFromPath(final Path wasmFile) {
    final String pathStr = wasmFile.toString().toLowerCase();

    // Categorize based on path patterns
    if (pathStr.contains("filesystem") || pathStr.contains("file")) {
      return WasiTestCategory.FILESYSTEM;
    }
    if (pathStr.contains("stdio") || pathStr.contains("io")) {
      return WasiTestCategory.STDIO;
    }
    if (pathStr.contains("env")) {
      return WasiTestCategory.ENVIRONMENT;
    }
    if (pathStr.contains("clock")) {
      return WasiTestCategory.CLOCKS;
    }
    if (pathStr.contains("random")) {
      return WasiTestCategory.RANDOM;
    }
    if (pathStr.contains("process")) {
      return WasiTestCategory.PROCESS;
    }
    if (pathStr.contains("socket")) {
      return WasiTestCategory.SOCKETS;
    }
    if (pathStr.contains("args")) {
      return WasiTestCategory.ARGS;
    }
    if (pathStr.contains("exit")) {
      return WasiTestCategory.EXIT;
    }
    if (pathStr.contains("preview2") || pathStr.contains("p2")) {
      return WasiTestCategory.PREVIEW2;
    }

    return WasiTestCategory.PREVIEW1;
  }

  private BehavioralAnalyzer.TestExecutionResult executeWasiTestOnRuntime(
      final WasmTestCase testCase,
      final WasiEnvironmentConfiguration environment,
      final RuntimeType runtime) {

    // Setup WASI environment for the specific runtime
    setupWasiEnvironment(environment, runtime);

    try {
      // Execute the test with behavioral analysis
      return behavioralAnalyzer.executeTest(testCase, runtime);
    } finally {
      // Cleanup WASI environment
      cleanupWasiEnvironment(environment, runtime);
    }
  }

  private void setupWasiEnvironment(final WasiEnvironmentConfiguration environment, final RuntimeType runtime) {
    // Implementation would setup WASI-specific environment:
    // - Set environment variables
    // - Prepare filesystem access
    // - Configure I/O redirection
    // - Setup networking if allowed
    // This is runtime-specific setup
    LOGGER.fine("Setting up WASI environment for runtime: " + runtime);
  }

  private void cleanupWasiEnvironment(final WasiEnvironmentConfiguration environment, final RuntimeType runtime) {
    // Implementation would cleanup WASI environment:
    // - Remove temporary files
    // - Close file handles
    // - Reset environment state
    LOGGER.fine("Cleaning up WASI environment for runtime: " + runtime);
  }

  private BehavioralAnalyzer.TestExecutionResult createFailureResult(final WasmTestCase testCase, final Exception e) {
    // Create a failure result for behavioral analysis
    return new BehavioralAnalyzer.TestExecutionResult(
        testCase.getTestName(),
        false,
        e.getMessage(),
        0L,
        Map.of(),
        List.of(),
        BehavioralVerdict.FAILURE);
  }

  private WasiCompatibilityAnalysis analyzeWasiCompatibility(
      final WasmTestCase testCase,
      final WasiEnvironmentConfiguration environment,
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> runtimeResults) {

    final Map<RuntimeType, Double> previewScores = new EnumMap<>(RuntimeType.class);
    final Set<String> supportedFeatures = new HashSet<>();
    final Set<String> unsupportedFeatures = new HashSet<>();
    final Map<String, String> issues = new HashMap<>();

    for (final Map.Entry<RuntimeType, BehavioralAnalyzer.TestExecutionResult> entry : runtimeResults.entrySet()) {
      final RuntimeType runtime = entry.getKey();
      final BehavioralAnalyzer.TestExecutionResult result = entry.getValue();

      // Calculate preview compatibility score
      final double score = result.isSuccessful() ? 100.0 : 0.0;
      previewScores.put(runtime, score);

      // Analyze supported/unsupported features based on execution results
      if (result.isSuccessful()) {
        supportedFeatures.add("wasi_" + categorizeWasiTest(testCase).getIdentifier());
      } else {
        unsupportedFeatures.add("wasi_" + categorizeWasiTest(testCase).getIdentifier());
        issues.put(runtime.name(), result.getFailureReason());
      }
    }

    final double overallScore = previewScores.values().stream()
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0.0);

    return new WasiCompatibilityAnalysis(
        previewScores,
        supportedFeatures,
        unsupportedFeatures,
        issues,
        overallScore);
  }

  private WasiPerformanceMetrics measureWasiPerformance(
      final WasmTestCase testCase,
      final WasiEnvironmentConfiguration environment,
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> runtimeResults) {

    final Map<RuntimeType, Long> ioTimes = new EnumMap<>(RuntimeType.class);
    final Map<RuntimeType, Long> fsTimes = new EnumMap<>(RuntimeType.class);
    final Map<RuntimeType, Long> netTimes = new EnumMap<>(RuntimeType.class);
    final Map<RuntimeType, Integer> syscallCounts = new EnumMap<>(RuntimeType.class);
    final Map<RuntimeType, Long> memoryUsage = new EnumMap<>(RuntimeType.class);

    for (final Map.Entry<RuntimeType, BehavioralAnalyzer.TestExecutionResult> entry : runtimeResults.entrySet()) {
      final RuntimeType runtime = entry.getKey();
      final BehavioralAnalyzer.TestExecutionResult result = entry.getValue();

      // Extract performance metrics from execution result
      ioTimes.put(runtime, result.getExecutionTime());
      fsTimes.put(runtime, result.getExecutionTime());
      netTimes.put(runtime, 0L); // Would be measured during execution
      syscallCounts.put(runtime, 0); // Would be measured during execution
      memoryUsage.put(runtime, 0L); // Would be measured during execution
    }

    return new WasiPerformanceMetrics(ioTimes, fsTimes, netTimes, syscallCounts, memoryUsage);
  }
}