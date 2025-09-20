# Custom Test Integration Guide

This guide explains how to integrate custom WebAssembly test suites into the Wasmtime4j comparison testing framework. You'll learn how to create test integrators, define custom test formats, and integrate them into the analysis pipeline.

## Overview

The Wasmtime4j testing framework supports multiple types of test integration:

- **Standard Test Suites**: WebAssembly spec tests, Wasmtime tests
- **Custom Test Suites**: Your organization's specific test collections
- **Generated Tests**: Programmatically created test cases
- **External Test Sources**: Tests from third-party tools and frameworks

## Test Integration Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Test Integration Pipeline                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Test Sources │  │ Test Loaders │  │ Test Runners │          │
│  │              │  │              │  │              │          │
│  │ • Files      │  │ • JSON       │  │ • JNI        │          │
│  │ • APIs       │  │ • YAML       │  │ • Panama     │          │
│  │ • Generated  │  │ • Custom     │  │ • Both       │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│         │                   │                   │               │
│         └───────────────────┼───────────────────┘               │
│                             │                                   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Test Execution Results                     │   │
│  │                                                         │   │
│  │ • Status (Pass/Fail/Error)                             │   │
│  │ • Execution Time                                        │   │
│  │ • Memory Usage                                          │   │
│  │ • Output/Errors                                         │   │
│  │ • Runtime Comparison                                    │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Creating Custom Test Integrators

### 1. Basic Test Integrator

```java
package ai.tegmentum.wasmtime4j.comparison.integrators;

import ai.tegmentum.wasmtime4j.comparison.analyzers.TestExecutionResult;
import ai.tegmentum.wasmtime4j.RuntimeType;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Custom test integrator for organization-specific WebAssembly test suites.
 * Supports various test formats and execution strategies.
 */
public final class CustomTestIntegrator {
    private static final Logger LOGGER = Logger.getLogger(CustomTestIntegrator.class.getName());

    private final TestLoader testLoader;
    private final TestRunner testRunner;
    private final TestConfiguration configuration;

    public CustomTestIntegrator(final TestConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration cannot be null");
        this.testLoader = new TestLoader(configuration.getLoaderConfig());
        this.testRunner = new TestRunner(configuration.getRunnerConfig());
    }

    /**
     * Discovers and executes tests from the specified source.
     *
     * @param testSource the source containing tests to execute
     * @param runtimeTypes the runtime types to test against
     * @return future containing all test execution results
     */
    public CompletableFuture<List<TestExecutionResult>> executeTests(
            final TestSource testSource,
            final Set<RuntimeType> runtimeTypes) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Starting test execution for source: " + testSource.getName());

                // Load test cases from the source
                final List<TestCase> testCases = testLoader.loadTests(testSource);
                LOGGER.info("Loaded " + testCases.size() + " test cases");

                // Execute tests across all runtime types
                final List<TestExecutionResult> allResults = new ArrayList<>();

                for (final RuntimeType runtimeType : runtimeTypes) {
                    final List<TestExecutionResult> runtimeResults =
                        executeTestsForRuntime(testCases, runtimeType);
                    allResults.addAll(runtimeResults);
                }

                LOGGER.info("Test execution complete: " + allResults.size() + " total results");
                return allResults;

            } catch (Exception e) {
                LOGGER.severe("Test execution failed: " + e.getMessage());
                throw new TestIntegrationException("Failed to execute tests", e);
            }
        });
    }

    private List<TestExecutionResult> executeTestsForRuntime(
            final List<TestCase> testCases,
            final RuntimeType runtimeType) {

        final List<TestExecutionResult> results = new ArrayList<>();

        for (final TestCase testCase : testCases) {
            if (shouldSkipTest(testCase, runtimeType)) {
                results.add(createSkippedResult(testCase, runtimeType));
                continue;
            }

            final TestExecutionResult result = executeTestCase(testCase, runtimeType);
            results.add(result);

            // Handle fail-fast configuration
            if (configuration.isFailFast() && result.getStatus() == TestStatus.FAILED) {
                LOGGER.warning("Failing fast due to test failure: " + testCase.getName());
                break;
            }
        }

        return results;
    }

    private TestExecutionResult executeTestCase(final TestCase testCase,
                                              final RuntimeType runtimeType) {
        final Instant startTime = Instant.now();
        final TestExecutionContext context = createExecutionContext(testCase, runtimeType);

        try {
            LOGGER.fine("Executing test: " + testCase.getName() + " on " + runtimeType);

            final TestResult result = testRunner.executeTest(testCase, context);
            final Duration executionTime = Duration.between(startTime, Instant.now());

            return TestExecutionResult.builder()
                .testName(testCase.getName())
                .testType(testCase.getType())
                .runtime(runtimeType)
                .status(result.getStatus())
                .executionTime(executionTime)
                .memoryUsage(result.getMemoryUsage())
                .output(result.getOutput())
                .errorOutput(result.getErrorOutput())
                .metadata(createResultMetadata(testCase, result))
                .build();

        } catch (TestExecutionException e) {
            return createErrorResult(testCase, runtimeType, startTime, e);
        }
    }

    private TestExecutionContext createExecutionContext(final TestCase testCase,
                                                       final RuntimeType runtimeType) {
        return TestExecutionContext.builder()
            .runtimeType(runtimeType)
            .timeout(configuration.getExecutionTimeout())
            .memoryLimit(configuration.getMemoryLimit())
            .tempDirectory(configuration.getTempDirectory())
            .enableProfiling(configuration.isProfilingEnabled())
            .testMetadata(testCase.getMetadata())
            .build();
    }
}
```

### 2. Test Source Abstraction

Create a flexible test source abstraction:

```java
/**
 * Represents a source of WebAssembly tests that can be loaded and executed.
 */
public interface TestSource {

    /**
     * Gets the name/identifier of this test source.
     */
    String getName();

    /**
     * Gets the type of this test source.
     */
    TestSourceType getType();

    /**
     * Gets metadata about this test source.
     */
    Map<String, Object> getMetadata();

    /**
     * Checks if this test source is available for loading.
     */
    boolean isAvailable();
}

/**
 * File-based test source implementation.
 */
public final class FileTestSource implements TestSource {
    private final String name;
    private final Path sourcePath;
    private final TestFileFormat format;
    private final Map<String, Object> metadata;

    public FileTestSource(final String name,
                         final Path sourcePath,
                         final TestFileFormat format) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.sourcePath = Objects.requireNonNull(sourcePath, "sourcePath cannot be null");
        this.format = Objects.requireNonNull(format, "format cannot be null");
        this.metadata = new HashMap<>();

        // Auto-detect metadata from file system
        this.metadata.put("path", sourcePath.toString());
        this.metadata.put("format", format.name());
        this.metadata.put("lastModified", getLastModified());
        this.metadata.put("size", getFileSize());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TestSourceType getType() {
        return TestSourceType.FILE;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    @Override
    public boolean isAvailable() {
        return Files.exists(sourcePath) && Files.isReadable(sourcePath);
    }

    public Path getPath() {
        return sourcePath;
    }

    public TestFileFormat getFormat() {
        return format;
    }
}

/**
 * API-based test source for dynamic test loading.
 */
public final class ApiTestSource implements TestSource {
    private final String name;
    private final URI apiEndpoint;
    private final Map<String, String> headers;
    private final TestApiFormat format;

    public ApiTestSource(final String name,
                        final URI apiEndpoint,
                        final TestApiFormat format) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.apiEndpoint = Objects.requireNonNull(apiEndpoint, "apiEndpoint cannot be null");
        this.format = Objects.requireNonNull(format, "format cannot be null");
        this.headers = new HashMap<>();
    }

    @Override
    public boolean isAvailable() {
        try {
            // Perform a lightweight check (HEAD request)
            final HttpURLConnection connection = (HttpURLConnection) apiEndpoint.toURL().openConnection();
            connection.setRequestMethod("HEAD");
            headers.forEach(connection::setRequestProperty);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            final int responseCode = connection.getResponseCode();
            return responseCode >= 200 && responseCode < 300;

        } catch (IOException e) {
            LOGGER.warning("API test source unavailable: " + e.getMessage());
            return false;
        }
    }
}
```

### 3. Test Loaders

Implement loaders for different test formats:

```java
/**
 * Loads tests from various sources and formats.
 */
public final class TestLoader {
    private static final Logger LOGGER = Logger.getLogger(TestLoader.class.getName());

    private final Map<TestFileFormat, TestFileLoader> fileLoaders;
    private final Map<TestApiFormat, TestApiLoader> apiLoaders;
    private final TestLoaderConfiguration configuration;

    public TestLoader(final TestLoaderConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration cannot be null");
        this.fileLoaders = createFileLoaders();
        this.apiLoaders = createApiLoaders();
    }

    /**
     * Loads test cases from the specified test source.
     *
     * @param testSource the source to load tests from
     * @return list of loaded test cases
     * @throws TestLoadingException if tests cannot be loaded
     */
    public List<TestCase> loadTests(final TestSource testSource) {
        if (!testSource.isAvailable()) {
            throw new TestLoadingException("Test source is not available: " + testSource.getName());
        }

        try {
            switch (testSource.getType()) {
                case FILE:
                    return loadFromFile((FileTestSource) testSource);
                case API:
                    return loadFromApi((ApiTestSource) testSource);
                case GENERATED:
                    return loadFromGenerator((GeneratedTestSource) testSource);
                default:
                    throw new TestLoadingException("Unsupported test source type: " + testSource.getType());
            }
        } catch (Exception e) {
            throw new TestLoadingException("Failed to load tests from " + testSource.getName(), e);
        }
    }

    private List<TestCase> loadFromFile(final FileTestSource source) {
        final TestFileFormat format = source.getFormat();
        final TestFileLoader loader = fileLoaders.get(format);

        if (loader == null) {
            throw new TestLoadingException("No loader available for format: " + format);
        }

        LOGGER.info("Loading tests from file: " + source.getPath() + " (format: " + format + ")");
        return loader.loadTests(source.getPath());
    }
}

/**
 * JSON test file loader implementation.
 */
public final class JsonTestFileLoader implements TestFileLoader {
    private final ObjectMapper objectMapper;

    public JsonTestFileLoader() {
        this.objectMapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
    }

    @Override
    public List<TestCase> loadTests(final Path filePath) {
        try {
            final JsonTestSuite testSuite = objectMapper.readValue(filePath.toFile(), JsonTestSuite.class);
            return convertToTestCases(testSuite);
        } catch (IOException e) {
            throw new TestLoadingException("Failed to load JSON test file: " + filePath, e);
        }
    }

    private List<TestCase> convertToTestCases(final JsonTestSuite testSuite) {
        return testSuite.getTests().stream()
            .map(this::convertToTestCase)
            .collect(Collectors.toList());
    }

    private TestCase convertToTestCase(final JsonTestDefinition jsonTest) {
        return TestCase.builder()
            .name(jsonTest.getName())
            .description(jsonTest.getDescription())
            .wasmModule(loadWasmModule(jsonTest.getWasmFile()))
            .expectedResults(jsonTest.getExpectedResults())
            .testType(TestType.valueOf(jsonTest.getType().toUpperCase()))
            .metadata(jsonTest.getMetadata())
            .timeout(Duration.ofSeconds(jsonTest.getTimeoutSeconds()))
            .build();
    }
}

/**
 * YAML test file loader implementation.
 */
public final class YamlTestFileLoader implements TestFileLoader {
    private final ObjectMapper yamlMapper;

    public YamlTestFileLoader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public List<TestCase> loadTests(final Path filePath) {
        try {
            final YamlTestSuite testSuite = yamlMapper.readValue(filePath.toFile(), YamlTestSuite.class);
            return convertToTestCases(testSuite);
        } catch (IOException e) {
            throw new TestLoadingException("Failed to load YAML test file: " + filePath, e);
        }
    }
}
```

## Test Format Definitions

### JSON Test Format

```json
{
  "testSuite": {
    "name": "Custom WebAssembly Tests",
    "version": "1.0.0",
    "description": "Organization-specific WebAssembly validation tests",
    "metadata": {
      "author": "Test Team",
      "created": "2024-01-15",
      "tags": ["validation", "performance", "security"]
    }
  },
  "configuration": {
    "defaultTimeout": 30,
    "memoryLimit": "128MB",
    "enableProfiling": true
  },
  "tests": [
    {
      "name": "memory_bounds_test",
      "description": "Tests memory boundary access patterns",
      "type": "functional",
      "wasmFile": "tests/wasm/memory_bounds.wasm",
      "expectedResults": {
        "exitCode": 0,
        "output": "Memory bounds test passed",
        "memoryUsage": {
          "max": "64MB",
          "average": "32MB"
        }
      },
      "runtimeSpecific": {
        "jni": {
          "expectedPerformance": {
            "executionTime": "< 100ms"
          }
        },
        "panama": {
          "expectedPerformance": {
            "executionTime": "< 50ms"
          }
        }
      },
      "metadata": {
        "category": "memory",
        "priority": "high",
        "tags": ["bounds", "safety"]
      },
      "timeoutSeconds": 15
    },
    {
      "name": "performance_benchmark",
      "description": "Measures execution performance across runtimes",
      "type": "performance",
      "wasmFile": "tests/wasm/fibonacci.wasm",
      "inputs": [
        {"function": "fibonacci", "args": [20]},
        {"function": "fibonacci", "args": [30]}
      ],
      "expectedResults": {
        "outputs": [6765, 832040],
        "performanceThresholds": {
          "maxExecutionTime": "500ms",
          "maxMemoryUsage": "16MB"
        }
      },
      "iterations": 100,
      "warmupIterations": 10
    }
  ]
}
```

### YAML Test Format

```yaml
testSuite:
  name: "Security Test Suite"
  version: "2.0.0"
  description: "Security-focused WebAssembly tests"

configuration:
  defaultTimeout: 60
  memoryLimit: "256MB"
  securityLevel: "strict"

tests:
  - name: "buffer_overflow_prevention"
    description: "Verifies buffer overflow protection"
    type: "security"
    wasmFile: "security/buffer_overflow.wasm"

    expectedResults:
      exitCode: 1
      errorPattern: ".*buffer overflow.*"

    securityChecks:
      - type: "memory_bounds"
        enabled: true
      - type: "stack_overflow"
        enabled: true

    metadata:
      severity: "critical"
      cve: "CVE-2024-XXXX"

  - name: "privilege_escalation_test"
    description: "Tests for privilege escalation vulnerabilities"
    type: "security"
    wasmFile: "security/privilege_test.wasm"

    hostFunctions:
      - name: "restricted_operation"
        expectFailure: true

    expectedResults:
      exitCode: 1
      errorPattern: ".*access denied.*"
```

## Advanced Integration Patterns

### 1. Test Generation

Create tests programmatically:

```java
/**
 * Generates WebAssembly tests programmatically for comprehensive coverage.
 */
public final class TestGenerator {

    /**
     * Generates memory access pattern tests.
     */
    public List<TestCase> generateMemoryTests(final MemoryTestConfiguration config) {
        final List<TestCase> tests = new ArrayList<>();

        // Generate boundary tests
        for (int size : config.getMemorySizes()) {
            tests.add(generateBoundaryTest(size));
            tests.add(generateOverflowTest(size));
            tests.add(generateUnderflowTest(size));
        }

        // Generate alignment tests
        for (int alignment : config.getAlignments()) {
            tests.add(generateAlignmentTest(alignment));
        }

        return tests;
    }

    private TestCase generateBoundaryTest(final int memorySize) {
        final String wasmCode = generateBoundaryTestWasm(memorySize);
        final byte[] wasmBytes = WasmCompiler.compile(wasmCode);

        return TestCase.builder()
            .name("memory_boundary_" + memorySize)
            .description("Tests memory boundary access for " + memorySize + " bytes")
            .wasmModule(wasmBytes)
            .testType(TestType.MEMORY)
            .expectedResults(ExpectedResults.success())
            .metadata(Map.of(
                "generated", true,
                "memorySize", memorySize,
                "category", "boundary"
            ))
            .build();
    }
}
```

### 2. Parameterized Test Execution

Support parameterized tests:

```java
/**
 * Executes parameterized tests with multiple input combinations.
 */
public final class ParameterizedTestRunner {

    public List<TestExecutionResult> executeParameterizedTest(
            final ParameterizedTestCase testCase,
            final RuntimeType runtime) {

        final List<TestExecutionResult> results = new ArrayList<>();

        for (final TestParameters parameters : testCase.getParameterSets()) {
            final TestExecutionResult result = executeWithParameters(
                testCase, parameters, runtime
            );
            results.add(result);
        }

        return results;
    }

    private TestExecutionResult executeWithParameters(
            final ParameterizedTestCase testCase,
            final TestParameters parameters,
            final RuntimeType runtime) {

        // Create test instance with specific parameters
        final TestCase instance = testCase.createInstance(parameters);

        // Execute the test
        return testRunner.executeTest(instance, createContext(runtime, parameters));
    }
}

/**
 * Represents a test case with multiple parameter sets.
 */
public final class ParameterizedTestCase {
    private final String name;
    private final TestCaseTemplate template;
    private final List<TestParameters> parameterSets;

    /**
     * Creates a specific test instance for the given parameters.
     */
    public TestCase createInstance(final TestParameters parameters) {
        return template.instantiate(parameters);
    }
}
```

### 3. Test Discovery

Implement automatic test discovery:

```java
/**
 * Discovers tests from various sources automatically.
 */
public final class TestDiscovery {

    /**
     * Discovers all tests in a directory structure.
     */
    public List<TestSource> discoverInDirectory(final Path directory) {
        final List<TestSource> sources = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile)
                 .filter(this::isTestFile)
                 .forEach(path -> {
                     final TestSource source = createTestSource(path);
                     if (source != null) {
                         sources.add(source);
                     }
                 });
        } catch (IOException e) {
            LOGGER.warning("Failed to discover tests in directory: " + directory);
        }

        return sources;
    }

    private boolean isTestFile(final Path path) {
        final String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".json") ||
               fileName.endsWith(".yaml") ||
               fileName.endsWith(".yml") ||
               fileName.endsWith(".wasm");
    }

    private TestSource createTestSource(final Path path) {
        final String fileName = path.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".json")) {
            return new FileTestSource(path.toString(), path, TestFileFormat.JSON);
        } else if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            return new FileTestSource(path.toString(), path, TestFileFormat.YAML);
        } else if (fileName.endsWith(".wasm")) {
            return new FileTestSource(path.toString(), path, TestFileFormat.WASM_BINARY);
        }

        return null;
    }
}
```

## Configuration and Integration

### Maven Integration

Add custom test integration to your Maven build:

```xml
<plugin>
    <groupId>ai.tegmentum</groupId>
    <artifactId>wasmtime4j-test-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <testSources>
            <testSource>
                <name>organization-tests</name>
                <type>file</type>
                <path>src/test/resources/custom-tests.json</path>
                <format>json</format>
            </testSource>
            <testSource>
                <name>security-tests</name>
                <type>file</type>
                <path>src/test/resources/security-tests.yaml</path>
                <format>yaml</format>
            </testSource>
        </testSources>
        <runtimeTypes>
            <runtimeType>JNI</runtimeType>
            <runtimeType>PANAMA</runtimeType>
        </runtimeTypes>
        <outputDirectory>${project.build.directory}/test-results</outputDirectory>
        <generateReports>true</generateReports>
    </configuration>
    <executions>
        <execution>
            <id>run-custom-tests</id>
            <phase>test</phase>
            <goals>
                <goal>run-custom-tests</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Programmatic Integration

Integrate custom tests programmatically:

```java
public class CustomTestIntegrationExample {

    public static void main(String[] args) {
        // Create test configuration
        final TestConfiguration config = TestConfiguration.builder()
            .executionTimeout(Duration.ofMinutes(5))
            .memoryLimit("512MB")
            .failFast(false)
            .enableProfiling(true)
            .tempDirectory(Paths.get("target/test-temp"))
            .build();

        // Create test integrator
        final CustomTestIntegrator integrator = new CustomTestIntegrator(config);

        // Define test sources
        final List<TestSource> testSources = Arrays.asList(
            new FileTestSource("custom-tests",
                              Paths.get("tests/custom-tests.json"),
                              TestFileFormat.JSON),
            new ApiTestSource("remote-tests",
                             URI.create("https://api.example.com/tests"),
                             TestApiFormat.JSON)
        );

        // Execute tests
        final Set<RuntimeType> runtimes = Set.of(RuntimeType.JNI, RuntimeType.PANAMA);

        for (final TestSource source : testSources) {
            final CompletableFuture<List<TestExecutionResult>> future =
                integrator.executeTests(source, runtimes);

            try {
                final List<TestExecutionResult> results = future.get(10, TimeUnit.MINUTES);
                processResults(results);
            } catch (Exception e) {
                System.err.println("Test execution failed: " + e.getMessage());
            }
        }
    }

    private static void processResults(final List<TestExecutionResult> results) {
        // Analyze and report results
        final long passedTests = results.stream()
            .filter(r -> r.getStatus() == TestStatus.PASSED)
            .count();

        System.out.println("Test Results:");
        System.out.println("  Total: " + results.size());
        System.out.println("  Passed: " + passedTests);
        System.out.println("  Failed: " + (results.size() - passedTests));
    }
}
```

## Best Practices

### 1. Error Handling and Resilience

```java
public class ResilientTestIntegrator {

    public List<TestExecutionResult> executeWithRetry(
            final TestCase testCase,
            final RuntimeType runtime) {

        int attempts = 0;
        final int maxAttempts = configuration.getMaxRetryAttempts();

        while (attempts < maxAttempts) {
            try {
                return List.of(executeTestCase(testCase, runtime));
            } catch (TransientTestException e) {
                attempts++;
                if (attempts >= maxAttempts) {
                    return List.of(createRetryExhaustedResult(testCase, runtime, e));
                }

                final Duration backoff = calculateBackoff(attempts);
                LOGGER.warning("Test failed (attempt " + attempts + "), retrying in " +
                              backoff + ": " + e.getMessage());

                try {
                    Thread.sleep(backoff.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return List.of(createInterruptedResult(testCase, runtime));
                }
            }
        }

        throw new IllegalStateException("Should not reach here");
    }
}
```

### 2. Resource Management

```java
public class ResourceManagedTestRunner implements AutoCloseable {
    private final ExecutorService executorService;
    private final Path tempDirectory;
    private volatile boolean closed = false;

    public ResourceManagedTestRunner(final TestConfiguration config) {
        this.executorService = Executors.newFixedThreadPool(config.getConcurrency());
        this.tempDirectory = Files.createTempDirectory("wasmtime4j-tests");
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;

        // Shutdown executor
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Clean up temporary files
        try {
            FileUtils.deleteRecursively(tempDirectory);
        } catch (IOException e) {
            LOGGER.warning("Failed to clean up temp directory: " + e.getMessage());
        }
    }
}
```

### 3. Performance Optimization

```java
public class OptimizedTestExecution {

    /**
     * Executes tests in parallel with optimal resource utilization.
     */
    public CompletableFuture<List<TestExecutionResult>> executeParallel(
            final List<TestCase> testCases,
            final Set<RuntimeType> runtimes) {

        // Group tests by resource requirements
        final Map<ResourceProfile, List<TestCase>> testGroups =
            testCases.stream().collect(Collectors.groupingBy(this::getResourceProfile));

        // Execute each group with appropriate parallelism
        final List<CompletableFuture<List<TestExecutionResult>>> futures = new ArrayList<>();

        for (final Map.Entry<ResourceProfile, List<TestCase>> entry : testGroups.entrySet()) {
            final ResourceProfile profile = entry.getKey();
            final List<TestCase> tests = entry.getValue();

            final CompletableFuture<List<TestExecutionResult>> future =
                executeTestGroup(tests, runtimes, profile);
            futures.add(future);
        }

        // Combine all results
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .flatMap(f -> f.join().stream())
                .collect(Collectors.toList()));
    }
}
```

## Troubleshooting

### Common Issues

1. **Test Loading Failures**
   - Verify file paths and permissions
   - Check test format validity
   - Ensure required dependencies are available

2. **Execution Timeouts**
   - Increase timeout configuration
   - Check for infinite loops in test code
   - Monitor system resource usage

3. **Memory Issues**
   - Adjust memory limits in configuration
   - Monitor for memory leaks in custom code
   - Use appropriate garbage collection settings

4. **Runtime Compatibility**
   - Verify WebAssembly module compatibility
   - Check for runtime-specific features
   - Test with different Java versions

### Debugging Tips

```java
// Enable detailed logging
System.setProperty("wasmtime4j.debug", "true");
System.setProperty("java.util.logging.config.file", "logging.properties");

// Use test execution listeners for debugging
testRunner.addListener(new DebugTestExecutionListener());

// Capture detailed timing information
testRunner.enableDetailedTiming(true);
```

## Next Steps

- Explore [WASI Integration Patterns](wasi-integration-patterns.md)
- Check out [Example Custom Test Suites](../examples/custom-test-suites/)
- Review [API Reference Documentation](../reference/api-documentation.md)
- Learn about [Performance Testing Best Practices](../guides/performance.md)