package ai.tegmentum.wasmtime4j.performance.microbench;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Generates JMH micro-benchmarks from Wasmtime test cases for detailed performance analysis.
 *
 * <p>This framework automatically converts Wasmtime test cases into standardized JMH benchmarks,
 * enabling precise performance measurement and comparison across different runtime implementations.
 *
 * <p>Features include:
 *
 * <ul>
 *   <li>Automatic test case discovery and analysis
 *   <li>JMH benchmark code generation with proper annotations
 *   <li>Runtime-specific benchmark variants (JNI vs Panama)
 *   <li>Wasmtime-specific performance metric collection
 *   <li>Benchmark suite organization and management
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * MicroBenchmarkGenerator generator = MicroBenchmarkGenerator.builder()
 *     .packageName("ai.tegmentum.wasmtime4j.benchmarks.generated")
 *     .outputDirectory(Paths.get("target/generated-benchmarks"))
 *     .includeRuntimes(RuntimeType.JNI, RuntimeType.PANAMA)
 *     .build();
 *
 * // Generate benchmarks from test directory
 * BenchmarkSuite suite = generator.generateFromTestDirectory(testDir);
 * suite.writeToDisk();
 *
 * // Run the generated benchmarks
 * BenchmarkResults results = suite.runBenchmarks();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class MicroBenchmarkGenerator {
  private static final Logger LOGGER = Logger.getLogger(MicroBenchmarkGenerator.class.getName());

  private static final String JMH_VERSION = "1.37";
  private static final Pattern WASM_FILE_PATTERN = Pattern.compile(".*\\.(wasm|wat)$");
  private static final Pattern TEST_METHOD_PATTERN = Pattern.compile("@Test\\s+[^{]*\\{");

  private final String packageName;
  private final Path outputDirectory;
  private final List<String> includeRuntimes;
  private final BenchmarkConfig benchmarkConfig;
  private final boolean generateRuntimeComparisons;
  private final boolean includeWasmtimeMetrics;

  private MicroBenchmarkGenerator(final Builder builder) {
    this.packageName = builder.packageName;
    this.outputDirectory = builder.outputDirectory;
    this.includeRuntimes = List.copyOf(builder.includeRuntimes);
    this.benchmarkConfig = builder.benchmarkConfig;
    this.generateRuntimeComparisons = builder.generateRuntimeComparisons;
    this.includeWasmtimeMetrics = builder.includeWasmtimeMetrics;
  }

  /**
   * Creates a new micro-benchmark generator builder.
   *
   * @return builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Generates micro-benchmarks from a test directory containing Wasmtime tests.
   *
   * @param testDirectory directory containing test files
   * @return benchmark suite
   * @throws BenchmarkGenerationException if generation fails
   */
  public BenchmarkSuite generateFromTestDirectory(final Path testDirectory)
      throws BenchmarkGenerationException {
    Objects.requireNonNull(testDirectory, "testDirectory cannot be null");

    if (!Files.isDirectory(testDirectory)) {
      throw new BenchmarkGenerationException("Test directory does not exist: " + testDirectory);
    }

    LOGGER.info("Generating micro-benchmarks from test directory: " + testDirectory);

    try {
      final List<TestCase> testCases = discoverTestCases(testDirectory);
      final List<BenchmarkClass> benchmarkClasses = generateBenchmarkClasses(testCases);

      return new BenchmarkSuite(packageName, outputDirectory, benchmarkClasses, benchmarkConfig);

    } catch (final IOException e) {
      throw new BenchmarkGenerationException("Failed to generate benchmarks", e);
    }
  }

  /**
   * Generates micro-benchmarks from specific test cases.
   *
   * @param testCases list of test cases to convert
   * @return benchmark suite
   * @throws BenchmarkGenerationException if generation fails
   */
  public BenchmarkSuite generateFromTestCases(final List<TestCase> testCases)
      throws BenchmarkGenerationException {
    Objects.requireNonNull(testCases, "testCases cannot be null");

    LOGGER.info("Generating micro-benchmarks from " + testCases.size() + " test cases");

    final List<BenchmarkClass> benchmarkClasses = generateBenchmarkClasses(testCases);
    return new BenchmarkSuite(packageName, outputDirectory, benchmarkClasses, benchmarkConfig);
  }

  /**
   * Analyzes test complexity to recommend benchmark configuration.
   *
   * @param testCases test cases to analyze
   * @return recommended benchmark configuration
   */
  public BenchmarkConfig analyzeAndRecommendConfig(final List<TestCase> testCases) {
    final var complexityCounts = new HashMap<TestComplexity, Integer>();
    for (final TestCase testCase : testCases) {
      final TestComplexity complexity = analyzeTestComplexity(testCase);
      complexityCounts.merge(complexity, 1, Integer::sum);
    }

    // Recommend configuration based on test complexity distribution
    if (complexityCounts.getOrDefault(TestComplexity.HIGH, 0) > testCases.size() / 3) {
      return BenchmarkConfig.forHighComplexityTests();
    } else if (complexityCounts.getOrDefault(TestComplexity.LOW, 0) > testCases.size() / 2) {
      return BenchmarkConfig.forLowComplexityTests();
    } else {
      return BenchmarkConfig.balanced();
    }
  }

  private List<TestCase> discoverTestCases(final Path testDirectory) throws IOException {
    final List<TestCase> testCases = new ArrayList<>();

    Files.walk(testDirectory)
        .filter(Files::isRegularFile)
        .filter(
            path ->
                WASM_FILE_PATTERN.matcher(path.getFileName().toString()).matches()
                    || path.getFileName().toString().endsWith(".java"))
        .forEach(
            path -> {
              try {
                final TestCase testCase = parseTestFile(path);
                if (testCase != null) {
                  testCases.add(testCase);
                }
              } catch (final IOException e) {
                LOGGER.warning("Failed to parse test file: " + path + " - " + e.getMessage());
              }
            });

    LOGGER.info("Discovered " + testCases.size() + " test cases");
    return testCases;
  }

  private TestCase parseTestFile(final Path testFile) throws IOException {
    final String filename = testFile.getFileName().toString();

    if (filename.endsWith(".wasm") || filename.endsWith(".wat")) {
      return parseWasmTestFile(testFile);
    } else if (filename.endsWith(".java")) {
      return parseJavaTestFile(testFile);
    }

    return null;
  }

  private TestCase parseWasmTestFile(final Path wasmFile) throws IOException {
    final byte[] wasmBytes = Files.readAllBytes(wasmFile);
    final String testName = extractTestNameFromPath(wasmFile);

    return TestCase.builder()
        .name(testName)
        .wasmModule(wasmBytes)
        .sourceFile(wasmFile)
        .complexity(analyzeWasmComplexity(wasmBytes))
        .build();
  }

  private TestCase parseJavaTestFile(final Path javaFile) throws IOException {
    final String content = Files.readString(javaFile);
    final String testName = extractTestNameFromPath(javaFile);

    // Look for embedded WASM modules or external references
    final byte[] wasmModule = extractWasmModuleFromJavaTest(content, javaFile.getParent());

    if (wasmModule != null) {
      return TestCase.builder()
          .name(testName)
          .wasmModule(wasmModule)
          .sourceFile(javaFile)
          .testMethod(extractTestMethod(content))
          .complexity(analyzeWasmComplexity(wasmModule))
          .build();
    }

    return null;
  }

  private byte[] extractWasmModuleFromJavaTest(final String javaContent, final Path testDir)
      throws IOException {
    // Look for byte array declarations (embedded WASM)
    if (javaContent.contains("byte[]") && javaContent.contains("0x")) {
      return parseInlineByteArray(javaContent);
    }

    // Look for file references
    final Pattern filePattern = Pattern.compile("\"([^\"]*\\.wasm)\"");
    final var matcher = filePattern.matcher(javaContent);
    if (matcher.find()) {
      final Path wasmFile = testDir.resolve(matcher.group(1));
      if (Files.exists(wasmFile)) {
        return Files.readAllBytes(wasmFile);
      }
    }

    return null;
  }

  private byte[] parseInlineByteArray(final String javaContent) {
    // Simplified parser for byte arrays in Java code
    final Pattern byteArrayPattern = Pattern.compile("\\{([^}]+)\\}");
    final var matcher = byteArrayPattern.matcher(javaContent);

    if (matcher.find()) {
      final String arrayContent = matcher.group(1);
      final String[] hexValues = arrayContent.split(",");
      final byte[] bytes = new byte[hexValues.length];

      for (int i = 0; i < hexValues.length; i++) {
        final String hex = hexValues[i].trim();
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
          bytes[i] = (byte) Integer.parseInt(hex.substring(2), 16);
        } else {
          bytes[i] = (byte) Integer.parseInt(hex);
        }
      }
      return bytes;
    }

    return null;
  }

  private String extractTestMethod(final String javaContent) {
    final var matcher = TEST_METHOD_PATTERN.matcher(javaContent);
    if (matcher.find()) {
      // Extract method body (simplified)
      final int start = matcher.start();
      final int openBrace = javaContent.indexOf('{', start);
      final int closeBrace = findMatchingBrace(javaContent, openBrace);
      return javaContent.substring(start, closeBrace + 1);
    }
    return null;
  }

  private int findMatchingBrace(final String content, final int openBrace) {
    int braceCount = 0;
    for (int i = openBrace; i < content.length(); i++) {
      if (content.charAt(i) == '{') {
        braceCount++;
      } else if (content.charAt(i) == '}') {
        braceCount--;
        if (braceCount == 0) {
          return i;
        }
      }
    }
    return content.length() - 1;
  }

  private String extractTestNameFromPath(final Path path) {
    String filename = path.getFileName().toString();
    final int lastDot = filename.lastIndexOf('.');
    if (lastDot > 0) {
      filename = filename.substring(0, lastDot);
    }
    return sanitizeTestName(filename);
  }

  private String sanitizeTestName(final String name) {
    return name.replaceAll("[^a-zA-Z0-9_]", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
  }

  private TestComplexity analyzeTestComplexity(final TestCase testCase) {
    return analyzeWasmComplexity(testCase.getWasmModule());
  }

  private TestComplexity analyzeWasmComplexity(final byte[] wasmBytes) {
    if (wasmBytes == null || wasmBytes.length == 0) {
      return TestComplexity.LOW;
    }

    // Simple heuristics based on module size and structure
    if (wasmBytes.length < 1000) {
      return TestComplexity.LOW;
    } else if (wasmBytes.length < 10000) {
      return TestComplexity.MEDIUM;
    } else {
      return TestComplexity.HIGH;
    }
  }

  private List<BenchmarkClass> generateBenchmarkClasses(final List<TestCase> testCases) {
    final Map<String, List<TestCase>> groupedByCategory = groupTestCasesByCategory(testCases);
    final List<BenchmarkClass> benchmarkClasses = new ArrayList<>();

    for (final Map.Entry<String, List<TestCase>> entry : groupedByCategory.entrySet()) {
      final String category = entry.getKey();
      final List<TestCase> categoryTests = entry.getValue();

      final BenchmarkClass benchmarkClass = generateBenchmarkClass(category, categoryTests);
      benchmarkClasses.add(benchmarkClass);
    }

    // Generate runtime comparison benchmark if requested
    if (generateRuntimeComparisons) {
      final BenchmarkClass comparisonClass = generateRuntimeComparisonClass(testCases);
      benchmarkClasses.add(comparisonClass);
    }

    return benchmarkClasses;
  }

  private Map<String, List<TestCase>> groupTestCasesByCategory(final List<TestCase> testCases) {
    return testCases.stream().collect(Collectors.groupingBy(this::categorizeTestCase));
  }

  private String categorizeTestCase(final TestCase testCase) {
    final String name = testCase.getName().toLowerCase();

    if (name.contains("memory") || name.contains("mem")) {
      return "Memory";
    } else if (name.contains("function") || name.contains("call")) {
      return "Functions";
    } else if (name.contains("compile") || name.contains("instantiate")) {
      return "Compilation";
    } else if (name.contains("math") || name.contains("arithmetic")) {
      return "Math";
    } else if (name.contains("control") || name.contains("branch")) {
      return "Control";
    } else {
      return "General";
    }
  }

  private BenchmarkClass generateBenchmarkClass(
      final String category, final List<TestCase> testCases) {
    final String className = category + "Benchmark";
    final StringBuilder classCode = new StringBuilder();

    // Generate class header
    generateClassHeader(classCode, className);

    // Generate setup methods
    generateSetupMethods(classCode, testCases);

    // Generate benchmark methods
    for (final TestCase testCase : testCases) {
      generateBenchmarkMethod(classCode, testCase);
    }

    // Generate teardown methods
    generateTeardownMethods(classCode);

    // Close class
    classCode.append("}\n");

    return new BenchmarkClass(className, classCode.toString(), testCases);
  }

  private BenchmarkClass generateRuntimeComparisonClass(final List<TestCase> testCases) {
    final String className = "RuntimeComparisonBenchmark";
    final StringBuilder classCode = new StringBuilder();

    generateClassHeader(classCode, className);

    // Add runtime parameter
    classCode.append("    @Param({");
    for (int i = 0; i < includeRuntimes.size(); i++) {
      if (i > 0) classCode.append(", ");
      classCode.append("\"").append(includeRuntimes.get(i)).append("\"");
    }
    classCode.append("})\n");
    classCode.append("    public String runtime;\n\n");

    // Generate comparison benchmark methods
    for (final TestCase testCase : testCases.subList(0, Math.min(5, testCases.size()))) {
      generateComparisonBenchmarkMethod(classCode, testCase);
    }

    classCode.append("}\n");

    return new BenchmarkClass(className, classCode.toString(), testCases);
  }

  private void generateClassHeader(final StringBuilder code, final String className) {
    code.append("package ").append(packageName).append(";\n\n");

    // Add imports
    code.append("import org.openjdk.jmh.annotations.*;\n");
    code.append("import org.openjdk.jmh.infra.Blackhole;\n");
    code.append("import ai.tegmentum.wasmtime4j.*;\n");
    code.append("import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;\n");
    code.append("import java.util.concurrent.TimeUnit;\n\n");

    // Add class annotations
    code.append("@State(Scope.Benchmark)\n");
    code.append("@BenchmarkMode(Mode.").append(benchmarkConfig.getBenchmarkMode()).append(")\n");
    code.append("@OutputTimeUnit(TimeUnit.").append(benchmarkConfig.getTimeUnit()).append(")\n");
    code.append("@Warmup(iterations = ")
        .append(benchmarkConfig.getWarmupIterations())
        .append(", time = 1)\n");
    code.append("@Measurement(iterations = ")
        .append(benchmarkConfig.getMeasurementIterations())
        .append(", time = 1)\n");
    code.append("@Fork(").append(benchmarkConfig.getForkCount()).append(")\n");
    code.append("public class ").append(className).append(" {\n\n");
  }

  private void generateSetupMethods(final StringBuilder code, final List<TestCase> testCases) {
    code.append("    private WasmRuntime runtime;\n");
    code.append("    private Engine engine;\n");
    code.append("    private Store store;\n\n");

    // Add WASM module constants
    for (int i = 0; i < testCases.size(); i++) {
      final TestCase testCase = testCases.get(i);
      code.append("    private static final byte[] WASM_MODULE_").append(i).append(" = {\n");
      final byte[] wasmBytes = testCase.getWasmModule();
      for (int j = 0; j < wasmBytes.length; j++) {
        if (j % 16 == 0) code.append("        ");
        code.append(String.format("(byte)0x%02X", wasmBytes[j] & 0xFF));
        if (j < wasmBytes.length - 1) code.append(", ");
        if (j % 16 == 15 || j == wasmBytes.length - 1) code.append("\n");
      }
      code.append("    };\n\n");
    }

    // Setup method
    code.append("    @Setup\n");
    code.append("    public void setup() throws Exception {\n");
    code.append("        runtime = WasmRuntimeFactory.create();\n");
    code.append("        engine = runtime.createEngine();\n");
    code.append("        store = engine.createStore();\n");
    code.append("    }\n\n");
  }

  private void generateBenchmarkMethod(final StringBuilder code, final TestCase testCase) {
    final String methodName = "benchmark" + capitalize(testCase.getName());

    code.append("    @Benchmark\n");
    code.append("    public void ")
        .append(methodName)
        .append("(Blackhole bh) throws Exception {\n");

    if (includeWasmtimeMetrics) {
      code.append("        long startTime = System.nanoTime();\n");
    }

    code.append("        Module module = engine.compileModule(WASM_MODULE_")
        .append(0)
        .append(");\n");
    code.append("        Instance instance = module.instantiate(store);\n");
    code.append("        \n");
    code.append("        // Execute test logic\n");
    code.append("        Object result = executeTestLogic(instance);\n");

    if (includeWasmtimeMetrics) {
      code.append("        long endTime = System.nanoTime();\n");
      code.append("        bh.consume(endTime - startTime);\n");
    }

    code.append("        bh.consume(result);\n");
    code.append("    }\n\n");
  }

  private void generateComparisonBenchmarkMethod(
      final StringBuilder code, final TestCase testCase) {
    final String methodName = "compare" + capitalize(testCase.getName());

    code.append("    @Benchmark\n");
    code.append("    public void ")
        .append(methodName)
        .append("(Blackhole bh) throws Exception {\n");
    code.append(
        "        WasmRuntime runtimeImpl ="
            + " WasmRuntimeFactory.create(RuntimeType.valueOf(runtime));\n");
    code.append("        Engine engineImpl = runtimeImpl.createEngine();\n");
    code.append("        Store storeImpl = engineImpl.createStore();\n");
    code.append("        \n");
    code.append("        long startTime = System.nanoTime();\n");
    code.append("        Module module = engineImpl.compileModule(WASM_MODULE_0);\n");
    code.append("        Instance instance = module.instantiate(storeImpl);\n");
    code.append("        Object result = executeTestLogic(instance);\n");
    code.append("        long endTime = System.nanoTime();\n");
    code.append("        \n");
    code.append("        bh.consume(endTime - startTime);\n");
    code.append("        bh.consume(result);\n");
    code.append("    }\n\n");
  }

  private void generateTeardownMethods(final StringBuilder code) {
    code.append("    private Object executeTestLogic(Instance instance) {\n");
    code.append("        // Placeholder for test-specific logic\n");
    code.append("        return instance;\n");
    code.append("    }\n\n");

    code.append("    @TearDown\n");
    code.append("    public void teardown() {\n");
    code.append("        if (store != null) store.close();\n");
    code.append("        if (engine != null) engine.close();\n");
    code.append("        if (runtime != null) runtime.close();\n");
    code.append("    }\n\n");
  }

  private String capitalize(final String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1);
  }

  /** Builder for MicroBenchmarkGenerator. */
  public static final class Builder {
    private String packageName = "ai.tegmentum.wasmtime4j.benchmarks.generated";
    private Path outputDirectory = Path.of("target/generated-benchmarks");
    private List<String> includeRuntimes = List.of("JNI", "PANAMA");
    private BenchmarkConfig benchmarkConfig = BenchmarkConfig.defaultConfig();
    private boolean generateRuntimeComparisons = true;
    private boolean includeWasmtimeMetrics = true;

    public Builder packageName(final String packageName) {
      this.packageName = packageName;
      return this;
    }

    public Builder outputDirectory(final Path outputDirectory) {
      this.outputDirectory = outputDirectory;
      return this;
    }

    public Builder includeRuntimes(final String... runtimes) {
      this.includeRuntimes = List.of(runtimes);
      return this;
    }

    public Builder benchmarkConfig(final BenchmarkConfig config) {
      this.benchmarkConfig = config;
      return this;
    }

    public Builder generateRuntimeComparisons(final boolean generate) {
      this.generateRuntimeComparisons = generate;
      return this;
    }

    public Builder includeWasmtimeMetrics(final boolean include) {
      this.includeWasmtimeMetrics = include;
      return this;
    }

    public MicroBenchmarkGenerator build() {
      return new MicroBenchmarkGenerator(this);
    }
  }

  /** Represents a test case to be converted to a benchmark. */
  public static final class TestCase {
    private final String name;
    private final byte[] wasmModule;
    private final Path sourceFile;
    private final String testMethod;
    private final TestComplexity complexity;

    private TestCase(final Builder builder) {
      this.name = builder.name;
      this.wasmModule = builder.wasmModule.clone();
      this.sourceFile = builder.sourceFile;
      this.testMethod = builder.testMethod;
      this.complexity = builder.complexity;
    }

    public static Builder builder() {
      return new Builder();
    }

    // Getters
    public String getName() {
      return name;
    }

    public byte[] getWasmModule() {
      return wasmModule.clone();
    }

    public Path getSourceFile() {
      return sourceFile;
    }

    public String getTestMethod() {
      return testMethod;
    }

    public TestComplexity getComplexity() {
      return complexity;
    }

    public static final class Builder {
      private String name;
      private byte[] wasmModule;
      private Path sourceFile;
      private String testMethod;
      private TestComplexity complexity = TestComplexity.MEDIUM;

      public Builder name(final String name) {
        this.name = name;
        return this;
      }

      public Builder wasmModule(final byte[] wasmModule) {
        this.wasmModule = wasmModule;
        return this;
      }

      public Builder sourceFile(final Path sourceFile) {
        this.sourceFile = sourceFile;
        return this;
      }

      public Builder testMethod(final String testMethod) {
        this.testMethod = testMethod;
        return this;
      }

      public Builder complexity(final TestComplexity complexity) {
        this.complexity = complexity;
        return this;
      }

      public TestCase build() {
        return new TestCase(this);
      }
    }
  }

  /** Test complexity levels for benchmark optimization. */
  public enum TestComplexity {
    LOW("Low complexity - simple operations"),
    MEDIUM("Medium complexity - moderate operations"),
    HIGH("High complexity - complex operations");

    private final String description;

    TestComplexity(final String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /** Represents a generated benchmark class. */
  public static final class BenchmarkClass {
    private final String className;
    private final String sourceCode;
    private final List<TestCase> testCases;

    public BenchmarkClass(
        final String className, final String sourceCode, final List<TestCase> testCases) {
      this.className = className;
      this.sourceCode = sourceCode;
      this.testCases = List.copyOf(testCases);
    }

    public String getClassName() {
      return className;
    }

    public String getSourceCode() {
      return sourceCode;
    }

    public List<TestCase> getTestCases() {
      return testCases;
    }
  }

  /** Configuration for generated benchmarks. */
  public static final class BenchmarkConfig {
    private final String benchmarkMode;
    private final String timeUnit;
    private final int warmupIterations;
    private final int measurementIterations;
    private final int forkCount;

    private BenchmarkConfig(
        final String benchmarkMode,
        final String timeUnit,
        final int warmupIterations,
        final int measurementIterations,
        final int forkCount) {
      this.benchmarkMode = benchmarkMode;
      this.timeUnit = timeUnit;
      this.warmupIterations = warmupIterations;
      this.measurementIterations = measurementIterations;
      this.forkCount = forkCount;
    }

    public static BenchmarkConfig defaultConfig() {
      return new BenchmarkConfig("Throughput", "SECONDS", 3, 5, 2);
    }

    public static BenchmarkConfig forHighComplexityTests() {
      return new BenchmarkConfig("AverageTime", "MILLISECONDS", 2, 3, 1);
    }

    public static BenchmarkConfig forLowComplexityTests() {
      return new BenchmarkConfig("Throughput", "SECONDS", 5, 10, 3);
    }

    public static BenchmarkConfig balanced() {
      return new BenchmarkConfig("Throughput", "SECONDS", 3, 5, 2);
    }

    // Getters
    public String getBenchmarkMode() {
      return benchmarkMode;
    }

    public String getTimeUnit() {
      return timeUnit;
    }

    public int getWarmupIterations() {
      return warmupIterations;
    }

    public int getMeasurementIterations() {
      return measurementIterations;
    }

    public int getForkCount() {
      return forkCount;
    }
  }

  /** Exception thrown when benchmark generation fails. */
  public static final class BenchmarkGenerationException extends Exception {
    public BenchmarkGenerationException(final String message) {
      super(message);
    }

    public BenchmarkGenerationException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
