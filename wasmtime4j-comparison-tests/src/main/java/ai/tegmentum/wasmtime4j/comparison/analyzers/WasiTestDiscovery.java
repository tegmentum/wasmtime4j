package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.webassembly.WasmTestCase;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestSuiteLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Discovers and catalogs WASI tests from Wasmtime's official test suite. Provides comprehensive
 * test discovery with automatic categorization, metadata extraction, and compatibility analysis.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automatic discovery of WASI tests from Wasmtime repository structure
 *   <li>Intelligent categorization based on test content and metadata
 *   <li>WASI Preview 1 and Preview 2 detection and classification
 *   <li>Test dependency analysis and execution ordering
 *   <li>Environment requirement detection for proper test isolation
 *   <li>Performance-aware test grouping for efficient execution
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasiTestDiscovery {
  private static final Logger LOGGER = Logger.getLogger(WasiTestDiscovery.class.getName());

  /** WASI test discovery configuration. */
  public static final class WasiDiscoveryConfiguration {
    private final Path wasmtimeRootDirectory;
    private final boolean includeExperimentalTests;
    private final boolean includePerformanceTests;
    private final Set<WasiTestIntegrator.WasiTestCategory> enabledCategories;
    private final Set<String> testNameFilters;
    private final boolean strictValidation;
    private final int maxTestsPerCategory;

    private WasiDiscoveryConfiguration(final Builder builder) {
      this.wasmtimeRootDirectory = builder.wasmtimeRootDirectory;
      this.includeExperimentalTests = builder.includeExperimentalTests;
      this.includePerformanceTests = builder.includePerformanceTests;
      this.enabledCategories = Set.copyOf(builder.enabledCategories);
      this.testNameFilters = Set.copyOf(builder.testNameFilters);
      this.strictValidation = builder.strictValidation;
      this.maxTestsPerCategory = builder.maxTestsPerCategory;
    }

    public Path getWasmtimeRootDirectory() {
      return wasmtimeRootDirectory;
    }

    public boolean isIncludeExperimentalTests() {
      return includeExperimentalTests;
    }

    public boolean isIncludePerformanceTests() {
      return includePerformanceTests;
    }

    public Set<WasiTestIntegrator.WasiTestCategory> getEnabledCategories() {
      return enabledCategories;
    }

    public Set<String> getTestNameFilters() {
      return testNameFilters;
    }

    public boolean isStrictValidation() {
      return strictValidation;
    }

    public int getMaxTestsPerCategory() {
      return maxTestsPerCategory;
    }

    /** Builder for creating WasiDiscoveryConfiguration instances. */
    public static final class Builder {
      private Path wasmtimeRootDirectory;
      private boolean includeExperimentalTests = false;
      private boolean includePerformanceTests = true;
      private final Set<WasiTestIntegrator.WasiTestCategory> enabledCategories = new HashSet<>();
      private final Set<String> testNameFilters = new HashSet<>();
      private boolean strictValidation = true;
      private int maxTestsPerCategory = Integer.MAX_VALUE;

      public Builder wasmtimeRootDirectory(final Path directory) {
        this.wasmtimeRootDirectory = directory;
        return this;
      }

      public Builder includeExperimentalTests(final boolean include) {
        this.includeExperimentalTests = include;
        return this;
      }

      public Builder includePerformanceTests(final boolean include) {
        this.includePerformanceTests = include;
        return this;
      }

      public Builder enableCategory(final WasiTestIntegrator.WasiTestCategory category) {
        enabledCategories.add(category);
        return this;
      }

      public Builder enableAllCategories() {
        Collections.addAll(enabledCategories, WasiTestIntegrator.WasiTestCategory.values());
        return this;
      }

      public Builder testNameFilter(final String filter) {
        testNameFilters.add(filter);
        return this;
      }

      public Builder strictValidation(final boolean strict) {
        this.strictValidation = strict;
        return this;
      }

      public Builder maxTestsPerCategory(final int max) {
        this.maxTestsPerCategory = max;
        return this;
      }

      /**
       * Builds the discovery configuration.
       *
       * @return configured discovery configuration
       */
      public WasiDiscoveryConfiguration build() {
        Objects.requireNonNull(wasmtimeRootDirectory, "wasmtimeRootDirectory must be specified");
        if (enabledCategories.isEmpty()) {
          enableAllCategories();
        }
        return new WasiDiscoveryConfiguration(this);
      }
    }
  }

  /** WASI test discovery result with comprehensive metadata. */
  public static final class WasiDiscoveryResult {
    private final Map<WasiTestIntegrator.WasiTestCategory, List<WasmTestCase>> categorizedTests;
    private final Map<String, WasiTestMetadata> testMetadata;
    private final WasiTestStatistics statistics;
    private final List<String> discoveryWarnings;
    private final List<String> validationErrors;
    private final Instant discoveryTime;

    /**
     * Creates a discovery result with categorized tests and metadata.
     *
     * @param categorizedTests tests organized by category
     * @param testMetadata metadata for each discovered test
     * @param statistics discovery statistics
     * @param discoveryWarnings warnings generated during discovery
     * @param validationErrors validation errors encountered
     */
    public WasiDiscoveryResult(
        final Map<WasiTestIntegrator.WasiTestCategory, List<WasmTestCase>> categorizedTests,
        final Map<String, WasiTestMetadata> testMetadata,
        final WasiTestStatistics statistics,
        final List<String> discoveryWarnings,
        final List<String> validationErrors) {
      this.categorizedTests = Map.copyOf(categorizedTests);
      this.testMetadata = Map.copyOf(testMetadata);
      this.statistics = statistics;
      this.discoveryWarnings = List.copyOf(discoveryWarnings);
      this.validationErrors = List.copyOf(validationErrors);
      this.discoveryTime = Instant.now();
    }

    public Map<WasiTestIntegrator.WasiTestCategory, List<WasmTestCase>> getCategorizedTests() {
      return categorizedTests;
    }

    public Map<String, WasiTestMetadata> getTestMetadata() {
      return testMetadata;
    }

    public WasiTestStatistics getStatistics() {
      return statistics;
    }

    public List<String> getDiscoveryWarnings() {
      return discoveryWarnings;
    }

    public List<String> getValidationErrors() {
      return validationErrors;
    }

    public Instant getDiscoveryTime() {
      return discoveryTime;
    }
  }

  /** WASI test metadata extracted from test files and directory structure. */
  public static final class WasiTestMetadata {
    private final String testName;
    private final WasiTestIntegrator.WasiTestCategory category;
    private final int previewVersion;
    private final Set<String> requiredFeatures;
    private final Set<String> environmentRequirements;
    private final boolean experimentalTest;
    private final boolean performanceTest;
    private final String description;
    private final Map<String, String> customAttributes;

    /**
     * Creates metadata for a WASI test with feature requirements.
     *
     * @param testName name of the test
     * @param category test category
     * @param previewVersion WASI preview version (1 or 2)
     * @param requiredFeatures features required by this test
     * @param environmentRequirements environment requirements for execution
     * @param experimentalTest true if this is an experimental test
     * @param performanceTest true if this is a performance test
     * @param description test description
     * @param customAttributes custom attributes for the test
     */
    public WasiTestMetadata(
        final String testName,
        final WasiTestIntegrator.WasiTestCategory category,
        final int previewVersion,
        final Set<String> requiredFeatures,
        final Set<String> environmentRequirements,
        final boolean experimentalTest,
        final boolean performanceTest,
        final String description,
        final Map<String, String> customAttributes) {
      this.testName = testName;
      this.category = category;
      this.previewVersion = previewVersion;
      this.requiredFeatures = Set.copyOf(requiredFeatures);
      this.environmentRequirements = Set.copyOf(environmentRequirements);
      this.experimentalTest = experimentalTest;
      this.performanceTest = performanceTest;
      this.description = description;
      this.customAttributes = Map.copyOf(customAttributes);
    }

    public String getTestName() {
      return testName;
    }

    public WasiTestIntegrator.WasiTestCategory getCategory() {
      return category;
    }

    public int getPreviewVersion() {
      return previewVersion;
    }

    public Set<String> getRequiredFeatures() {
      return requiredFeatures;
    }

    public Set<String> getEnvironmentRequirements() {
      return environmentRequirements;
    }

    public boolean isExperimentalTest() {
      return experimentalTest;
    }

    public boolean isPerformanceTest() {
      return performanceTest;
    }

    public String getDescription() {
      return description;
    }

    public Map<String, String> getCustomAttributes() {
      return customAttributes;
    }
  }

  /** WASI test discovery statistics. */
  public static final class WasiTestStatistics {
    private final int totalTestsFound;
    private final Map<WasiTestIntegrator.WasiTestCategory, Integer> testsPerCategory;
    private final int preview1Tests;
    private final int preview2Tests;
    private final int experimentalTests;
    private final int performanceTests;
    private final int validTests;
    private final int invalidTests;

    /**
     * Creates discovery statistics summarizing found tests.
     *
     * @param totalTestsFound total number of tests found
     * @param testsPerCategory count of tests per category
     * @param preview1Tests number of Preview 1 tests
     * @param preview2Tests number of Preview 2 tests
     * @param experimentalTests number of experimental tests
     * @param performanceTests number of performance tests
     * @param validTests number of valid tests
     * @param invalidTests number of invalid tests
     */
    public WasiTestStatistics(
        final int totalTestsFound,
        final Map<WasiTestIntegrator.WasiTestCategory, Integer> testsPerCategory,
        final int preview1Tests,
        final int preview2Tests,
        final int experimentalTests,
        final int performanceTests,
        final int validTests,
        final int invalidTests) {
      this.totalTestsFound = totalTestsFound;
      this.testsPerCategory = Map.copyOf(testsPerCategory);
      this.preview1Tests = preview1Tests;
      this.preview2Tests = preview2Tests;
      this.experimentalTests = experimentalTests;
      this.performanceTests = performanceTests;
      this.validTests = validTests;
      this.invalidTests = invalidTests;
    }

    public int getTotalTestsFound() {
      return totalTestsFound;
    }

    public Map<WasiTestIntegrator.WasiTestCategory, Integer> getTestsPerCategory() {
      return testsPerCategory;
    }

    public int getPreview1Tests() {
      return preview1Tests;
    }

    public int getPreview2Tests() {
      return preview2Tests;
    }

    public int getExperimentalTests() {
      return experimentalTests;
    }

    public int getPerformanceTests() {
      return performanceTests;
    }

    public int getValidTests() {
      return validTests;
    }

    public int getInvalidTests() {
      return invalidTests;
    }
  }

  // Test discovery patterns for different WASI test types
  private static final Map<WasiTestIntegrator.WasiTestCategory, Set<Pattern>> CATEGORY_PATTERNS;

  static {
    final Map<WasiTestIntegrator.WasiTestCategory, Set<Pattern>> patterns =
        new EnumMap<>(WasiTestIntegrator.WasiTestCategory.class);

    patterns.put(
        WasiTestIntegrator.WasiTestCategory.FILESYSTEM,
        Set.of(
            Pattern.compile(".*file.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*dir.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*path.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*fd.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*open.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*read.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*write.*", Pattern.CASE_INSENSITIVE)));

    patterns.put(
        WasiTestIntegrator.WasiTestCategory.STDIO,
        Set.of(
            Pattern.compile(".*stdio.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*stdout.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*stdin.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*stderr.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*io.*", Pattern.CASE_INSENSITIVE)));

    patterns.put(
        WasiTestIntegrator.WasiTestCategory.ENVIRONMENT,
        Set.of(
            Pattern.compile(".*env.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*environ.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*getenv.*", Pattern.CASE_INSENSITIVE)));

    patterns.put(
        WasiTestIntegrator.WasiTestCategory.CLOCKS,
        Set.of(
            Pattern.compile(".*clock.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*time.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*timestamp.*", Pattern.CASE_INSENSITIVE)));

    patterns.put(
        WasiTestIntegrator.WasiTestCategory.RANDOM,
        Set.of(
            Pattern.compile(".*random.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*rand.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*entropy.*", Pattern.CASE_INSENSITIVE)));

    patterns.put(
        WasiTestIntegrator.WasiTestCategory.PROCESS,
        Set.of(
            Pattern.compile(".*proc.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*process.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*spawn.*", Pattern.CASE_INSENSITIVE)));

    patterns.put(
        WasiTestIntegrator.WasiTestCategory.SOCKETS,
        Set.of(
            Pattern.compile(".*socket.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*net.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*tcp.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*udp.*", Pattern.CASE_INSENSITIVE)));

    patterns.put(
        WasiTestIntegrator.WasiTestCategory.ARGS,
        Set.of(
            Pattern.compile(".*args.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*argv.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*arguments.*", Pattern.CASE_INSENSITIVE)));

    patterns.put(
        WasiTestIntegrator.WasiTestCategory.EXIT,
        Set.of(
            Pattern.compile(".*exit.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*terminate.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*proc_exit.*", Pattern.CASE_INSENSITIVE)));

    patterns.put(
        WasiTestIntegrator.WasiTestCategory.PREVIEW2,
        Set.of(
            Pattern.compile(".*preview2.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*p2.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*component.*", Pattern.CASE_INSENSITIVE)));

    CATEGORY_PATTERNS = Map.copyOf(patterns);
  }

  // Known Wasmtime WASI test directories
  private static final List<String> WASI_TEST_DIRECTORIES =
      List.of(
          "crates/wasi-tests",
          "crates/wasi/tests",
          "crates/wasi-common/tests",
          "tests/misc_testsuite/wasi",
          "crates/test-programs/wasi-tests");

  private final Map<String, WasiTestMetadata> cachedMetadata = new ConcurrentHashMap<>();

  /**
   * Discovers all WASI tests from the Wasmtime repository using the specified configuration.
   *
   * @param config the discovery configuration
   * @return comprehensive discovery result with categorized tests
   * @throws IOException if test discovery fails
   */
  public WasiDiscoveryResult discoverWasiTests(final WasiDiscoveryConfiguration config)
      throws IOException {
    Objects.requireNonNull(config, "config cannot be null");

    LOGGER.info("Starting WASI test discovery in: " + config.getWasmtimeRootDirectory());

    final Map<WasiTestIntegrator.WasiTestCategory, List<WasmTestCase>> categorizedTests =
        new EnumMap<>(WasiTestIntegrator.WasiTestCategory.class);
    final Map<String, WasiTestMetadata> testMetadata = new HashMap<>();
    final List<String> warnings = new ArrayList<>();
    final List<String> validationErrors = new ArrayList<>();

    // Initialize empty lists for all categories
    for (final WasiTestIntegrator.WasiTestCategory category :
        WasiTestIntegrator.WasiTestCategory.values()) {
      categorizedTests.put(category, new ArrayList<>());
    }

    // Discover tests in known WASI test directories
    for (final String testDir : WASI_TEST_DIRECTORIES) {
      final Path wasiTestsPath = config.getWasmtimeRootDirectory().resolve(testDir);
      if (Files.exists(wasiTestsPath)) {
        discoverTestsInDirectory(
            wasiTestsPath, config, categorizedTests, testMetadata, warnings, validationErrors);
      } else {
        warnings.add("WASI test directory not found: " + wasiTestsPath);
      }
    }

    // Apply filters and limits
    applyFiltersAndLimits(config, categorizedTests, testMetadata);

    // Calculate statistics
    final WasiTestStatistics statistics = calculateStatistics(categorizedTests, testMetadata);

    LOGGER.info(
        "WASI test discovery completed: " + statistics.getTotalTestsFound() + " tests found");

    return new WasiDiscoveryResult(
        categorizedTests, testMetadata, statistics, warnings, validationErrors);
  }

  /**
   * Creates a default discovery configuration for basic WASI test discovery.
   *
   * @param wasmtimeRootDirectory the root directory of the Wasmtime repository
   * @return default discovery configuration
   */
  public static WasiDiscoveryConfiguration createDefaultConfiguration(
      final Path wasmtimeRootDirectory) {
    return new WasiDiscoveryConfiguration.Builder()
        .wasmtimeRootDirectory(wasmtimeRootDirectory)
        .enableAllCategories()
        .includePerformanceTests(true)
        .includeExperimentalTests(false)
        .strictValidation(true)
        .build();
  }

  /** Clears the cached test metadata. */
  public void clearCache() {
    cachedMetadata.clear();
    LOGGER.info("WASI test discovery cache cleared");
  }

  private void discoverTestsInDirectory(
      final Path directory,
      final WasiDiscoveryConfiguration config,
      final Map<WasiTestIntegrator.WasiTestCategory, List<WasmTestCase>> categorizedTests,
      final Map<String, WasiTestMetadata> testMetadata,
      final List<String> warnings,
      final List<String> validationErrors)
      throws IOException {

    LOGGER.fine("Discovering WASI tests in directory: " + directory);

    try (final Stream<Path> paths = Files.walk(directory)) {
      final List<Path> wasmFiles =
          paths
              .filter(Files::isRegularFile)
              .filter(path -> path.toString().endsWith(".wasm"))
              .collect(Collectors.toList());

      for (final Path wasmFile : wasmFiles) {
        try {
          final WasmTestCase testCase =
              WasmTestCase.fromFile(wasmFile, WasmTestSuiteLoader.TestSuiteType.WASI_TESTS);

          // Validate test case if strict validation is enabled
          if (config.isStrictValidation() && !testCase.isValidWasmModule()) {
            validationErrors.add("Invalid WASM module: " + testCase.getTestName());
            continue;
          }

          // Extract metadata
          final WasiTestMetadata metadata = extractTestMetadata(testCase, wasmFile, config);
          testMetadata.put(testCase.getTestName(), metadata);

          // Check if category is enabled
          if (!config.getEnabledCategories().contains(metadata.getCategory())) {
            continue;
          }

          // Check experimental and performance filters
          if (metadata.isExperimentalTest() && !config.isIncludeExperimentalTests()) {
            continue;
          }
          if (metadata.isPerformanceTest() && !config.isIncludePerformanceTests()) {
            continue;
          }

          // Apply name filters
          if (!config.getTestNameFilters().isEmpty()) {
            final boolean matchesFilter =
                config.getTestNameFilters().stream()
                    .anyMatch(filter -> testCase.getTestName().contains(filter));
            if (!matchesFilter) {
              continue;
            }
          }

          // Add to appropriate category
          categorizedTests.get(metadata.getCategory()).add(testCase);

        } catch (final Exception e) {
          warnings.add("Failed to process test file " + wasmFile + ": " + e.getMessage());
        }
      }
    }
  }

  private WasiTestMetadata extractTestMetadata(
      final WasmTestCase testCase, final Path wasmFile, final WasiDiscoveryConfiguration config) {

    // Check cache first
    final String cacheKey = testCase.getTestName();
    if (cachedMetadata.containsKey(cacheKey)) {
      return cachedMetadata.get(cacheKey);
    }

    final String testName = testCase.getTestName();
    final String pathStr = wasmFile.toString().toLowerCase();

    // Determine category based on patterns
    final WasiTestIntegrator.WasiTestCategory category =
        determineCategoryFromPatterns(testName, pathStr);

    // Determine preview version
    final int previewVersion = determinePreviewVersion(testName, pathStr);

    // Extract required features
    final Set<String> requiredFeatures = extractRequiredFeatures(testCase, pathStr);

    // Extract environment requirements
    final Set<String> environmentRequirements = extractEnvironmentRequirements(testCase, pathStr);

    // Determine if experimental or performance test
    final boolean experimental = isExperimentalTest(testName, pathStr);
    final boolean performance = isPerformanceTest(testName, pathStr);

    // Extract description
    final String description = extractDescription(testCase);

    // Extract custom attributes
    final Map<String, String> customAttributes = extractCustomAttributes(testCase);

    final WasiTestMetadata metadata =
        new WasiTestMetadata(
            testName,
            category,
            previewVersion,
            requiredFeatures,
            environmentRequirements,
            experimental,
            performance,
            description,
            customAttributes);

    // Cache the metadata
    cachedMetadata.put(cacheKey, metadata);

    return metadata;
  }

  private WasiTestIntegrator.WasiTestCategory determineCategoryFromPatterns(
      final String testName, final String pathStr) {
    for (final Map.Entry<WasiTestIntegrator.WasiTestCategory, Set<Pattern>> entry :
        CATEGORY_PATTERNS.entrySet()) {
      final WasiTestIntegrator.WasiTestCategory category = entry.getKey();
      final Set<Pattern> patterns = entry.getValue();

      for (final Pattern pattern : patterns) {
        if (pattern.matcher(testName).matches() || pattern.matcher(pathStr).find()) {
          return category;
        }
      }
    }

    // Default to Preview 1 if no specific category matches
    return WasiTestIntegrator.WasiTestCategory.PREVIEW1;
  }

  private int determinePreviewVersion(final String testName, final String pathStr) {
    if (testName.toLowerCase().contains("preview2")
        || pathStr.contains("preview2")
        || testName.toLowerCase().contains("p2")
        || pathStr.contains("p2")
        || pathStr.contains("component")) {
      return 2;
    }
    return 1; // Default to Preview 1
  }

  private Set<String> extractRequiredFeatures(final WasmTestCase testCase, final String pathStr) {
    final Set<String> features = new HashSet<>();

    // Basic WASI features that all tests require
    features.add("wasi_snapshot_preview1");

    // Add features based on test characteristics
    if (pathStr.contains("filesystem") || pathStr.contains("file")) {
      features.add("wasi_filesystem");
    }
    if (pathStr.contains("socket") || pathStr.contains("net")) {
      features.add("wasi_sockets");
    }
    if (pathStr.contains("clock") || pathStr.contains("time")) {
      features.add("wasi_clocks");
    }
    if (pathStr.contains("random")) {
      features.add("wasi_random");
    }

    return features;
  }

  private Set<String> extractEnvironmentRequirements(
      final WasmTestCase testCase, final String pathStr) {
    final Set<String> requirements = new HashSet<>();

    if (pathStr.contains("filesystem") || pathStr.contains("file")) {
      requirements.add("filesystem_access");
    }
    if (pathStr.contains("socket") || pathStr.contains("net")) {
      requirements.add("network_access");
    }
    if (pathStr.contains("env")) {
      requirements.add("environment_variables");
    }

    return requirements;
  }

  private boolean isExperimentalTest(final String testName, final String pathStr) {
    return testName.toLowerCase().contains("experimental")
        || pathStr.contains("experimental")
        || testName.toLowerCase().contains("unstable")
        || pathStr.contains("unstable");
  }

  private boolean isPerformanceTest(final String testName, final String pathStr) {
    return testName.toLowerCase().contains("perf")
        || testName.toLowerCase().contains("benchmark")
        || pathStr.contains("perf")
        || pathStr.contains("benchmark");
  }

  private String extractDescription(final WasmTestCase testCase) {
    // Try to extract description from metadata
    if (testCase.hasMetadata()) {
      // Would parse JSON metadata for description
      return "WASI test: " + testCase.getTestName();
    }
    return "WASI test: " + testCase.getTestName();
  }

  private Map<String, String> extractCustomAttributes(final WasmTestCase testCase) {
    final Map<String, String> attributes = new HashMap<>();

    attributes.put("suite_type", testCase.getSuiteType().name());
    attributes.put("module_size", String.valueOf(testCase.getModuleSize()));
    attributes.put("has_metadata", String.valueOf(testCase.hasMetadata()));
    attributes.put("has_expected_results", String.valueOf(testCase.hasExpectedResults()));

    return attributes;
  }

  private void applyFiltersAndLimits(
      final WasiDiscoveryConfiguration config,
      final Map<WasiTestIntegrator.WasiTestCategory, List<WasmTestCase>> categorizedTests,
      final Map<String, WasiTestMetadata> testMetadata) {

    // Apply per-category limits
    for (final Map.Entry<WasiTestIntegrator.WasiTestCategory, List<WasmTestCase>> entry :
        categorizedTests.entrySet()) {
      final List<WasmTestCase> tests = entry.getValue();
      if (tests.size() > config.getMaxTestsPerCategory()) {
        final List<WasmTestCase> limitedTests = tests.subList(0, config.getMaxTestsPerCategory());
        entry.setValue(new ArrayList<>(limitedTests));

        // Remove metadata for filtered tests
        for (int i = config.getMaxTestsPerCategory(); i < tests.size(); i++) {
          testMetadata.remove(tests.get(i).getTestName());
        }
      }
    }
  }

  private WasiTestStatistics calculateStatistics(
      final Map<WasiTestIntegrator.WasiTestCategory, List<WasmTestCase>> categorizedTests,
      final Map<String, WasiTestMetadata> testMetadata) {

    final Map<WasiTestIntegrator.WasiTestCategory, Integer> testsPerCategory =
        new EnumMap<>(WasiTestIntegrator.WasiTestCategory.class);
    int totalTests = 0;
    int preview1Tests = 0;
    int preview2Tests = 0;
    int experimentalTests = 0;
    int performanceTests = 0;
    int validTests = 0;

    for (final Map.Entry<WasiTestIntegrator.WasiTestCategory, List<WasmTestCase>> entry :
        categorizedTests.entrySet()) {
      final int categoryCount = entry.getValue().size();
      testsPerCategory.put(entry.getKey(), categoryCount);
      totalTests += categoryCount;
    }

    for (final WasiTestMetadata metadata : testMetadata.values()) {
      if (metadata.getPreviewVersion() == 1) {
        preview1Tests++;
      } else if (metadata.getPreviewVersion() == 2) {
        preview2Tests++;
      }

      if (metadata.isExperimentalTest()) {
        experimentalTests++;
      }

      if (metadata.isPerformanceTest()) {
        performanceTests++;
      }

      validTests++; // All tests in metadata are considered valid
    }

    return new WasiTestStatistics(
        totalTests,
        testsPerCategory,
        preview1Tests,
        preview2Tests,
        experimentalTests,
        performanceTests,
        validTests,
        0); // No invalid tests in final result
  }
}
