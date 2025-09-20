# API Reference for Extensibility

This document provides a comprehensive API reference for extending the Wasmtime4j comparison testing framework. It covers all interfaces, classes, and extension points available for custom implementations.

## Core Interfaces

### Analysis Framework Interfaces

#### Analyzer Interface Pattern

```java
package ai.tegmentum.wasmtime4j.comparison.analyzers;

/**
 * Base interface for all analysis components.
 * Implementations should follow this pattern for consistency.
 */
public interface AnalysisComponent {

    /**
     * Gets the name of this analysis component.
     *
     * @return the component name
     */
    String getName();

    /**
     * Gets the version of this analysis component.
     *
     * @return the component version
     */
    String getVersion();

    /**
     * Gets metadata about this analysis component.
     *
     * @return component metadata
     */
    Map<String, Object> getMetadata();

    /**
     * Checks if this component supports the given configuration.
     *
     * @param configuration the configuration to check
     * @return true if the configuration is supported
     */
    boolean supportsConfiguration(Map<String, Object> configuration);
}

/**
 * Interface for test result analyzers.
 * Implement this interface to create custom analysis components.
 */
public interface TestResultAnalyzer extends AnalysisComponent {

    /**
     * Analyzes the provided test execution results.
     *
     * @param results the test execution results to analyze
     * @param configuration analysis configuration parameters
     * @return analysis results
     * @throws AnalysisException if analysis fails
     */
    AnalysisResult analyze(List<TestExecutionResult> results,
                          Map<String, Object> configuration) throws AnalysisException;

    /**
     * Gets the supported analysis types for this analyzer.
     *
     * @return set of supported analysis types
     */
    Set<AnalysisType> getSupportedAnalysisTypes();

    /**
     * Validates the analysis configuration.
     *
     * @param configuration the configuration to validate
     * @return validation result
     */
    ValidationResult validateConfiguration(Map<String, Object> configuration);
}
```

#### Reporter Interface Pattern

```java
package ai.tegmentum.wasmtime4j.comparison.reporters;

/**
 * Interface for result reporters.
 * Implement this interface to create custom output formats.
 */
public interface ResultReporter extends AnalysisComponent {

    /**
     * Generates a report from the analysis results.
     *
     * @param results the analysis results to report
     * @param configuration reporter configuration
     * @param outputStream the output stream to write to
     * @throws ReportingException if report generation fails
     */
    void generateReport(List<AnalysisResult> results,
                       Map<String, Object> configuration,
                       OutputStream outputStream) throws ReportingException;

    /**
     * Gets the supported output formats for this reporter.
     *
     * @return set of supported output formats
     */
    Set<OutputFormat> getSupportedFormats();

    /**
     * Gets the MIME type for the output format.
     *
     * @param format the output format
     * @return the MIME type
     */
    String getMimeType(OutputFormat format);

    /**
     * Gets the file extension for the output format.
     *
     * @param format the output format
     * @return the file extension (including dot)
     */
    String getFileExtension(OutputFormat format);
}

/**
 * Interface for streaming reporters that can handle large datasets.
 */
public interface StreamingReporter extends ResultReporter {

    /**
     * Starts a streaming report session.
     *
     * @param configuration reporter configuration
     * @param outputStream the output stream
     * @return streaming session handle
     */
    StreamingSession startStreaming(Map<String, Object> configuration,
                                   OutputStream outputStream);

    /**
     * Writes analysis results to the streaming session.
     *
     * @param session the streaming session
     * @param results the results to write
     */
    void writeResults(StreamingSession session, List<AnalysisResult> results);

    /**
     * Completes the streaming session.
     *
     * @param session the streaming session to complete
     */
    void completeStreaming(StreamingSession session);
}
```

#### Test Integrator Interface

```java
package ai.tegmentum.wasmtime4j.comparison.integrators;

/**
 * Interface for test suite integrators.
 * Implement this interface to integrate custom test suites.
 */
public interface TestSuiteIntegrator extends AnalysisComponent {

    /**
     * Discovers test sources from the given location.
     *
     * @param location the location to discover tests from
     * @param configuration discovery configuration
     * @return list of discovered test sources
     */
    List<TestSource> discoverTests(String location, Map<String, Object> configuration);

    /**
     * Executes tests from the specified test source.
     *
     * @param testSource the test source to execute
     * @param runtimeTypes the runtime types to test
     * @param configuration execution configuration
     * @return future containing test execution results
     */
    CompletableFuture<List<TestExecutionResult>> executeTests(
        TestSource testSource,
        Set<RuntimeType> runtimeTypes,
        Map<String, Object> configuration);

    /**
     * Gets the supported test source types.
     *
     * @return set of supported test source types
     */
    Set<TestSourceType> getSupportedSourceTypes();

    /**
     * Validates a test source for compatibility.
     *
     * @param testSource the test source to validate
     * @return validation result
     */
    ValidationResult validateTestSource(TestSource testSource);
}
```

## Core Data Types

### Test Execution Results

```java
package ai.tegmentum.wasmtime4j.comparison.analyzers;

/**
 * Represents the result of executing a single test case.
 */
public final class TestExecutionResult {
    private final String testName;
    private final String testType;
    private final RuntimeType runtime;
    private final TestStatus status;
    private final Duration executionTime;
    private final MemoryUsage memoryUsage;
    private final String output;
    private final String errorOutput;
    private final Map<String, Object> metadata;
    private final Optional<Throwable> exception;

    // Builder pattern implementation
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String testName;
        private String testType;
        private RuntimeType runtime;
        private TestStatus status;
        private Duration executionTime;
        private MemoryUsage memoryUsage;
        private String output = "";
        private String errorOutput = "";
        private Map<String, Object> metadata = new HashMap<>();
        private Throwable exception;

        public Builder testName(final String testName) {
            this.testName = testName;
            return this;
        }

        public Builder runtime(final RuntimeType runtime) {
            this.runtime = runtime;
            return this;
        }

        public Builder status(final TestStatus status) {
            this.status = status;
            return this;
        }

        public Builder executionTime(final Duration executionTime) {
            this.executionTime = executionTime;
            return this;
        }

        public Builder memoryUsage(final MemoryUsage memoryUsage) {
            this.memoryUsage = memoryUsage;
            return this;
        }

        public Builder metadata(final String key, final Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder metadata(final Map<String, Object> metadata) {
            this.metadata.putAll(metadata);
            return this;
        }

        public TestExecutionResult build() {
            Objects.requireNonNull(testName, "testName cannot be null");
            Objects.requireNonNull(runtime, "runtime cannot be null");
            Objects.requireNonNull(status, "status cannot be null");

            return new TestExecutionResult(
                testName, testType, runtime, status, executionTime,
                memoryUsage, output, errorOutput, metadata,
                Optional.ofNullable(exception)
            );
        }
    }

    // Getters
    public String getTestName() { return testName; }
    public RuntimeType getRuntime() { return runtime; }
    public TestStatus getStatus() { return status; }
    public Duration getExecutionTime() { return executionTime; }
    public MemoryUsage getMemoryUsage() { return memoryUsage; }
    public Map<String, Object> getMetadata() { return Collections.unmodifiableMap(metadata); }

    // Convenience methods
    public boolean isSuccessful() {
        return status == TestStatus.PASSED;
    }

    public boolean isFailed() {
        return status == TestStatus.FAILED || status == TestStatus.ERROR;
    }

    public Optional<Object> getMetadataValue(final String key) {
        return Optional.ofNullable(metadata.get(key));
    }
}

/**
 * Enumeration of test execution statuses.
 */
public enum TestStatus {
    PASSED,
    FAILED,
    ERROR,
    SKIPPED,
    TIMEOUT
}

/**
 * Represents memory usage during test execution.
 */
public final class MemoryUsage {
    private final long heapUsed;
    private final long heapMax;
    private final long nonHeapUsed;
    private final long nonHeapMax;
    private final Map<String, Long> customMetrics;

    public static Builder builder() {
        return new Builder();
    }

    // Implementation with getters and builder...
}
```

### Analysis Results

```java
package ai.tegmentum.wasmtime4j.comparison.analyzers;

/**
 * Base interface for all analysis results.
 */
public interface AnalysisResult {

    /**
     * Gets the type of analysis that produced this result.
     */
    AnalysisType getAnalysisType();

    /**
     * Gets the timestamp when this analysis was performed.
     */
    Instant getAnalysisTimestamp();

    /**
     * Gets metadata about this analysis result.
     */
    Map<String, Object> getMetadata();

    /**
     * Gets the status of this analysis.
     */
    AnalysisStatus getStatus();

    /**
     * Gets any error message if the analysis failed.
     */
    Optional<String> getErrorMessage();
}

/**
 * Generic analysis result implementation.
 */
public final class GenericAnalysisResult implements AnalysisResult {
    private final AnalysisType analysisType;
    private final Instant analysisTimestamp;
    private final AnalysisStatus status;
    private final Map<String, Object> results;
    private final Map<String, Object> metadata;
    private final String errorMessage;

    public static Builder builder(final AnalysisType analysisType) {
        return new Builder(analysisType);
    }

    // Builder implementation...

    /**
     * Gets a result value by key.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getResult(final String key, final Class<T> type) {
        final Object value = results.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    /**
     * Gets all result data.
     */
    public Map<String, Object> getAllResults() {
        return Collections.unmodifiableMap(results);
    }
}

/**
 * Enumeration of analysis types.
 */
public enum AnalysisType {
    COVERAGE("Coverage Analysis"),
    BEHAVIORAL("Behavioral Analysis"),
    PERFORMANCE("Performance Analysis"),
    SECURITY("Security Analysis"),
    COMPATIBILITY("Compatibility Analysis"),
    CUSTOM("Custom Analysis");

    private final String displayName;

    AnalysisType(final String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

/**
 * Enumeration of analysis statuses.
 */
public enum AnalysisStatus {
    SUCCESS,
    WARNING,
    ERROR,
    PARTIAL
}
```

### Configuration Types

```java
package ai.tegmentum.wasmtime4j.comparison.configuration;

/**
 * Base interface for all configuration objects.
 */
public interface Configuration {

    /**
     * Validates this configuration.
     */
    ValidationResult validate();

    /**
     * Gets configuration as a map.
     */
    Map<String, Object> asMap();

    /**
     * Gets a configuration value by key.
     */
    <T> Optional<T> getValue(String key, Class<T> type);
}

/**
 * Analysis configuration implementation.
 */
public final class AnalysisConfiguration implements Configuration {
    private final Map<String, Object> properties;
    private final Set<String> enabledAnalyzers;
    private final Set<String> enabledReporters;
    private final Duration timeout;
    private final int maxConcurrency;

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, Object> properties = new HashMap<>();
        private final Set<String> enabledAnalyzers = new HashSet<>();
        private final Set<String> enabledReporters = new HashSet<>();
        private Duration timeout = Duration.ofMinutes(30);
        private int maxConcurrency = Runtime.getRuntime().availableProcessors();

        public Builder enableAnalyzer(final String analyzerName) {
            enabledAnalyzers.add(analyzerName);
            return this;
        }

        public Builder enableReporter(final String reporterName) {
            enabledReporters.add(reporterName);
            return this;
        }

        public Builder timeout(final Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder maxConcurrency(final int maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
            return this;
        }

        public Builder property(final String key, final Object value) {
            properties.put(key, value);
            return this;
        }

        public Builder addAnalyzerConfiguration(final String analyzerName,
                                              final Map<String, Object> config) {
            properties.put("analyzer." + analyzerName, config);
            return this;
        }

        public AnalysisConfiguration build() {
            return new AnalysisConfiguration(
                new HashMap<>(properties),
                new HashSet<>(enabledAnalyzers),
                new HashSet<>(enabledReporters),
                timeout,
                maxConcurrency
            );
        }
    }

    // Getters and implementation...
}
```

## Registry and Discovery

### Component Registry

```java
package ai.tegmentum.wasmtime4j.comparison.registry;

/**
 * Registry for analysis framework components.
 */
public final class ComponentRegistry {
    private static final ComponentRegistry INSTANCE = new ComponentRegistry();

    private final Map<String, Class<? extends TestResultAnalyzer>> analyzers = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends ResultReporter>> reporters = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends TestSuiteIntegrator>> integrators = new ConcurrentHashMap<>();

    public static ComponentRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a custom analyzer.
     */
    public void registerAnalyzer(final String name,
                                final Class<? extends TestResultAnalyzer> analyzerClass) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(analyzerClass, "analyzerClass cannot be null");

        // Validate the analyzer class
        validateAnalyzerClass(analyzerClass);

        analyzers.put(name, analyzerClass);
        LOGGER.info("Registered analyzer: " + name + " -> " + analyzerClass.getName());
    }

    /**
     * Registers a custom reporter.
     */
    public void registerReporter(final String name,
                               final Class<? extends ResultReporter> reporterClass) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(reporterClass, "reporterClass cannot be null");

        validateReporterClass(reporterClass);

        reporters.put(name, reporterClass);
        LOGGER.info("Registered reporter: " + name + " -> " + reporterClass.getName());
    }

    /**
     * Creates an analyzer instance by name.
     */
    public Optional<TestResultAnalyzer> createAnalyzer(final String name) {
        final Class<? extends TestResultAnalyzer> analyzerClass = analyzers.get(name);
        if (analyzerClass == null) {
            return Optional.empty();
        }

        try {
            final Constructor<? extends TestResultAnalyzer> constructor =
                analyzerClass.getDeclaredConstructor();
            return Optional.of(constructor.newInstance());
        } catch (Exception e) {
            LOGGER.severe("Failed to create analyzer instance: " + name + " - " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Creates a reporter instance by name.
     */
    public Optional<ResultReporter> createReporter(final String name) {
        final Class<? extends ResultReporter> reporterClass = reporters.get(name);
        if (reporterClass == null) {
            return Optional.empty();
        }

        try {
            final Constructor<? extends ResultReporter> constructor =
                reporterClass.getDeclaredConstructor();
            return Optional.of(constructor.newInstance());
        } catch (Exception e) {
            LOGGER.severe("Failed to create reporter instance: " + name + " - " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Gets all registered analyzer names.
     */
    public Set<String> getRegisteredAnalyzers() {
        return Collections.unmodifiableSet(analyzers.keySet());
    }

    /**
     * Gets all registered reporter names.
     */
    public Set<String> getRegisteredReporters() {
        return Collections.unmodifiableSet(reporters.keySet());
    }

    private void validateAnalyzerClass(final Class<? extends TestResultAnalyzer> analyzerClass) {
        // Check for default constructor
        try {
            analyzerClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                "Analyzer class must have a default constructor: " + analyzerClass.getName()
            );
        }

        // Check for required annotations or interfaces if needed
        // Additional validation logic...
    }
}
```

### Component Discovery

```java
package ai.tegmentum.wasmtime4j.comparison.discovery;

/**
 * Automatic component discovery using annotations and classpath scanning.
 */
public final class ComponentDiscovery {

    /**
     * Discovers and registers all components on the classpath.
     */
    public static void discoverAndRegister() {
        discoverAnalyzers();
        discoverReporters();
        discoverIntegrators();
    }

    private static void discoverAnalyzers() {
        final Reflections reflections = new Reflections("ai.tegmentum.wasmtime4j");
        final Set<Class<? extends TestResultAnalyzer>> analyzerClasses =
            reflections.getSubTypesOf(TestResultAnalyzer.class);

        for (final Class<? extends TestResultAnalyzer> analyzerClass : analyzerClasses) {
            final AnalyzerComponent annotation = analyzerClass.getAnnotation(AnalyzerComponent.class);
            if (annotation != null) {
                ComponentRegistry.getInstance().registerAnalyzer(annotation.name(), analyzerClass);
            }
        }
    }
}

/**
 * Annotation for marking analyzer components.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AnalyzerComponent {
    String name();
    String description() default "";
    String version() default "1.0.0";
}

/**
 * Annotation for marking reporter components.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ReporterComponent {
    String name();
    String description() default "";
    String[] supportedFormats() default {};
}
```

## Extension Examples

### Custom Security Analyzer

```java
package ai.tegmentum.wasmtime4j.comparison.analyzers.custom;

@AnalyzerComponent(
    name = "security",
    description = "Security vulnerability analysis for WebAssembly modules",
    version = "1.0.0"
)
public final class SecurityAnalyzer implements TestResultAnalyzer {

    @Override
    public String getName() {
        return "security";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public Map<String, Object> getMetadata() {
        return Map.of(
            "description", "Security vulnerability analysis",
            "author", "Security Team",
            "supportedTests", Set.of("security", "memory", "privilege")
        );
    }

    @Override
    public Set<AnalysisType> getSupportedAnalysisTypes() {
        return Set.of(AnalysisType.SECURITY, AnalysisType.BEHAVIORAL);
    }

    @Override
    public AnalysisResult analyze(final List<TestExecutionResult> results,
                                 final Map<String, Object> configuration) throws AnalysisException {

        final SecurityAnalysisConfiguration securityConfig =
            SecurityAnalysisConfiguration.fromMap(configuration);

        try {
            final SecurityAnalysisData analysisData = performSecurityAnalysis(results, securityConfig);

            return GenericAnalysisResult.builder(AnalysisType.SECURITY)
                .status(AnalysisStatus.SUCCESS)
                .result("vulnerabilities", analysisData.getVulnerabilities())
                .result("securityScore", analysisData.getSecurityScore())
                .result("recommendations", analysisData.getRecommendations())
                .metadata("analysisTime", analysisData.getAnalysisTime())
                .metadata("testCount", results.size())
                .build();

        } catch (Exception e) {
            throw new AnalysisException("Security analysis failed", e);
        }
    }

    @Override
    public ValidationResult validateConfiguration(final Map<String, Object> configuration) {
        final ValidationResult.Builder builder = ValidationResult.builder();

        // Validate required configuration
        if (!configuration.containsKey("securityLevel")) {
            builder.addError("securityLevel is required");
        }

        // Validate security level value
        final Object securityLevel = configuration.get("securityLevel");
        if (securityLevel != null && !isValidSecurityLevel(securityLevel.toString())) {
            builder.addError("Invalid securityLevel: " + securityLevel);
        }

        return builder.build();
    }

    @Override
    public boolean supportsConfiguration(final Map<String, Object> configuration) {
        return validateConfiguration(configuration).isValid();
    }

    private SecurityAnalysisData performSecurityAnalysis(
            final List<TestExecutionResult> results,
            final SecurityAnalysisConfiguration config) {

        // Perform actual security analysis
        // This is where the real analysis logic would go

        return SecurityAnalysisData.builder()
            .vulnerabilities(findVulnerabilities(results, config))
            .securityScore(calculateSecurityScore(results))
            .recommendations(generateRecommendations(results, config))
            .analysisTime(Duration.ofSeconds(5)) // Example timing
            .build();
    }
}
```

### Custom XML Reporter

```java
package ai.tegmentum.wasmtime4j.comparison.reporters.custom;

@ReporterComponent(
    name = "xml",
    description = "XML format reporter with custom schema",
    supportedFormats = {"xml", "application/xml"}
)
public final class XmlReporter implements ResultReporter {

    private final DocumentBuilderFactory documentBuilderFactory;
    private final TransformerFactory transformerFactory;

    public XmlReporter() {
        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        this.transformerFactory = TransformerFactory.newInstance();
    }

    @Override
    public String getName() {
        return "xml";
    }

    @Override
    public Set<OutputFormat> getSupportedFormats() {
        return Set.of(OutputFormat.XML);
    }

    @Override
    public String getMimeType(final OutputFormat format) {
        return "application/xml";
    }

    @Override
    public String getFileExtension(final OutputFormat format) {
        return ".xml";
    }

    @Override
    public void generateReport(final List<AnalysisResult> results,
                              final Map<String, Object> configuration,
                              final OutputStream outputStream) throws ReportingException {

        try {
            final DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            final Document document = builder.newDocument();

            // Create root element
            final Element root = document.createElement("testAnalysisReport");
            root.setAttribute("timestamp", Instant.now().toString());
            root.setAttribute("version", "1.0");
            document.appendChild(root);

            // Add metadata
            final Element metadata = createMetadataElement(document, configuration);
            root.appendChild(metadata);

            // Add analysis results
            for (final AnalysisResult result : results) {
                final Element resultElement = createResultElement(document, result);
                root.appendChild(resultElement);
            }

            // Write to output stream
            final Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            final DOMSource source = new DOMSource(document);
            final StreamResult streamResult = new StreamResult(outputStream);
            transformer.transform(source, streamResult);

        } catch (Exception e) {
            throw new ReportingException("Failed to generate XML report", e);
        }
    }

    private Element createResultElement(final Document document, final AnalysisResult result) {
        final Element resultElement = document.createElement("analysisResult");
        resultElement.setAttribute("type", result.getAnalysisType().name());
        resultElement.setAttribute("status", result.getStatus().name());
        resultElement.setAttribute("timestamp", result.getAnalysisTimestamp().toString());

        // Add result data based on type
        if (result instanceof GenericAnalysisResult) {
            final GenericAnalysisResult genericResult = (GenericAnalysisResult) result;
            addResultData(document, resultElement, genericResult.getAllResults());
        }

        return resultElement;
    }
}
```

## Testing Extensions

### Unit Testing Framework

```java
package ai.tegmentum.wasmtime4j.comparison.testing;

/**
 * Base class for testing custom analyzers.
 */
public abstract class AnalyzerTestBase {

    protected List<TestExecutionResult> createTestResults(final TestResultTemplate... templates) {
        return Arrays.stream(templates)
            .map(this::createTestResult)
            .collect(Collectors.toList());
    }

    protected TestExecutionResult createTestResult(final TestResultTemplate template) {
        return TestExecutionResult.builder()
            .testName(template.testName())
            .runtime(template.runtime())
            .status(template.status())
            .executionTime(template.executionTime())
            .output(template.output())
            .errorOutput(template.errorOutput())
            .build();
    }

    protected void assertAnalysisSuccessful(final AnalysisResult result) {
        assertThat(result.getStatus()).isEqualTo(AnalysisStatus.SUCCESS);
        assertThat(result.getErrorMessage()).isEmpty();
    }

    protected void assertAnalysisContains(final AnalysisResult result,
                                        final String key,
                                        final Object expectedValue) {
        if (result instanceof GenericAnalysisResult) {
            final GenericAnalysisResult genericResult = (GenericAnalysisResult) result;
            final Optional<Object> actualValue = genericResult.getResult(key, Object.class);
            assertThat(actualValue).hasValue(expectedValue);
        } else {
            fail("Result type not supported for assertion: " + result.getClass());
        }
    }
}

/**
 * Test template for creating test execution results.
 */
public record TestResultTemplate(
    String testName,
    RuntimeType runtime,
    TestStatus status,
    Duration executionTime,
    String output,
    String errorOutput
) {
    public static TestResultTemplate success(final String testName, final RuntimeType runtime) {
        return new TestResultTemplate(
            testName, runtime, TestStatus.PASSED,
            Duration.ofMillis(100), "Success", ""
        );
    }

    public static TestResultTemplate failure(final String testName, final RuntimeType runtime, final String error) {
        return new TestResultTemplate(
            testName, runtime, TestStatus.FAILED,
            Duration.ofMillis(150), "", error
        );
    }
}
```

### Integration Testing

```java
/**
 * Integration test for custom components.
 */
public class CustomComponentIntegrationTest {

    @Test
    void testSecurityAnalyzerIntegration() {
        // Register custom analyzer
        ComponentRegistry.getInstance().registerAnalyzer("security", SecurityAnalyzer.class);

        // Create test configuration
        final AnalysisConfiguration config = AnalysisConfiguration.builder()
            .enableAnalyzer("security")
            .addAnalyzerConfiguration("security", Map.of(
                "securityLevel", "HIGH",
                "enabledChecks", Set.of("memory", "privilege")
            ))
            .build();

        // Create test results
        final List<TestExecutionResult> results = createSecurityTestResults();

        // Run analysis
        final AnalysisFramework framework = AnalysisFramework.getInstance();
        final List<AnalysisResult> analysisResults = framework.runAnalysis(results, config);

        // Verify results
        assertThat(analysisResults).hasSize(1);
        final AnalysisResult securityResult = analysisResults.get(0);
        assertThat(securityResult.getAnalysisType()).isEqualTo(AnalysisType.SECURITY);
        assertAnalysisSuccessful(securityResult);
    }
}
```

## Best Practices

### 1. Thread Safety

```java
// Always make custom components thread-safe
public final class ThreadSafeAnalyzer implements TestResultAnalyzer {
    private final AtomicLong analysisCount = new AtomicLong();
    private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();

    @Override
    public AnalysisResult analyze(final List<TestExecutionResult> results,
                                 final Map<String, Object> configuration) {
        final long currentCount = analysisCount.incrementAndGet();

        // Thread-safe implementation
        return performThreadSafeAnalysis(results, configuration, currentCount);
    }
}
```

### 2. Resource Management

```java
// Implement AutoCloseable for resource management
public final class ResourceManagedReporter implements ResultReporter, AutoCloseable {
    private final OutputStream outputStream;
    private volatile boolean closed = false;

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    @Override
    public void generateReport(final List<AnalysisResult> results,
                              final Map<String, Object> configuration,
                              final OutputStream outputStream) throws ReportingException {
        if (closed) {
            throw new ReportingException("Reporter has been closed");
        }
        // Implementation...
    }
}
```

### 3. Error Handling

```java
// Comprehensive error handling
public final class RobustAnalyzer implements TestResultAnalyzer {

    @Override
    public AnalysisResult analyze(final List<TestExecutionResult> results,
                                 final Map<String, Object> configuration) throws AnalysisException {
        try {
            // Validate inputs
            validateInputs(results, configuration);

            // Perform analysis with error handling
            return performAnalysisWithErrorHandling(results, configuration);

        } catch (IllegalArgumentException e) {
            throw new AnalysisException("Invalid input parameters", e);
        } catch (Exception e) {
            LOGGER.severe("Unexpected error during analysis: " + e.getMessage());
            throw new AnalysisException("Analysis failed due to unexpected error", e);
        }
    }

    private void validateInputs(final List<TestExecutionResult> results,
                               final Map<String, Object> configuration) {
        Objects.requireNonNull(results, "results cannot be null");
        Objects.requireNonNull(configuration, "configuration cannot be null");

        if (results.isEmpty()) {
            throw new IllegalArgumentException("results cannot be empty");
        }

        final ValidationResult validationResult = validateConfiguration(configuration);
        if (!validationResult.isValid()) {
            throw new IllegalArgumentException("Invalid configuration: " +
                String.join(", ", validationResult.getErrors()));
        }
    }
}
```

## Next Steps

- Explore the [Extension Guide](extending-analysis-framework.md) for implementation examples
- Check out [WASI Integration Patterns](wasi-integration-patterns.md) for WASI-specific APIs
- Review [Custom Test Integration](custom-test-integration.md) for test suite APIs
- See [Example Implementations](../examples/) for working code samples