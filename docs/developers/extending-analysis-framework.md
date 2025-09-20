# Extending the Analysis Framework

This guide provides comprehensive information for developers who want to extend Wasmtime4j's full comparison test coverage analysis framework. The framework is designed with extensibility in mind, offering multiple extension points for custom analyzers, reporters, and test integrations.

## Overview

The analysis framework consists of several extensible components:

- **Analyzers**: Process test results and generate insights
- **Reporters**: Format and output analysis results
- **Test Integrators**: Execute and integrate custom test suites
- **Dashboard Components**: Visualize results in interactive formats

## Architecture Overview

The framework follows a plugin-style architecture where components implement specific interfaces and can be dynamically registered and discovered.

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│    Analyzers    │    │   Reporters     │    │  Integrators    │
│                 │    │                 │    │                 │
│ • Coverage      │    │ • HTML          │    │ • WASI          │
│ • Behavioral    │    │ • JSON          │    │ • Custom Tests  │
│ • Performance   │    │ • CSV           │    │ • Wasmtime      │
│ • Custom        │    │ • Custom        │    │ • Custom        │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │  Core Framework │
                    │                 │
                    │ • Registry      │
                    │ • Pipeline      │
                    │ • Configuration │
                    └─────────────────┘
```

## Extension Points

### 1. Custom Analyzers

Create custom analyzers by implementing the analyzer interface pattern. Here's how to create a custom analyzer:

#### Basic Analyzer Structure

```java
package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Custom analyzer for specialized test result analysis.
 * This example shows how to create a security-focused analyzer.
 */
public final class SecurityAnalyzer {
    private static final Logger LOGGER = Logger.getLogger(SecurityAnalyzer.class.getName());

    /**
     * Analyzes test results for security-related patterns and vulnerabilities.
     *
     * @param testResults the test execution results to analyze
     * @param configuration analysis configuration parameters
     * @return security analysis results
     */
    public SecurityAnalysisResult analyzeSecurityPatterns(
            final List<TestExecutionResult> testResults,
            final SecurityAnalysisConfiguration configuration) {

        LOGGER.info("Starting security analysis on " + testResults.size() + " test results");

        final SecurityAnalysisResult.Builder resultBuilder = SecurityAnalysisResult.builder();

        // Analyze memory safety patterns
        analyzeMemorySafety(testResults, resultBuilder);

        // Analyze privilege escalation risks
        analyzePrivilegeEscalation(testResults, resultBuilder);

        // Analyze resource exhaustion patterns
        analyzeResourceExhaustion(testResults, resultBuilder);

        final SecurityAnalysisResult result = resultBuilder.build();
        LOGGER.info("Security analysis complete: found " + result.getVulnerabilityCount() + " potential issues");

        return result;
    }

    private void analyzeMemorySafety(final List<TestExecutionResult> results,
                                   final SecurityAnalysisResult.Builder builder) {
        // Implementation for memory safety analysis
        results.stream()
            .filter(result -> result.getTestType().equals("memory"))
            .forEach(result -> {
                if (hasMemorySafetyIssue(result)) {
                    builder.addVulnerability(
                        SecurityVulnerability.memoryCorruption(
                            result.getTestName(),
                            result.getFailureDetails()
                        )
                    );
                }
            });
    }

    private boolean hasMemorySafetyIssue(final TestExecutionResult result) {
        // Custom logic to detect memory safety issues
        return result.getErrorOutput().contains("memory corruption") ||
               result.getErrorOutput().contains("buffer overflow");
    }

    // Additional analyzer methods...
}
```

#### Supporting Classes

Create supporting result and configuration classes:

```java
/**
 * Results from security analysis.
 */
public final class SecurityAnalysisResult {
    private final List<SecurityVulnerability> vulnerabilities;
    private final SecurityMetrics metrics;
    private final List<SecurityRecommendation> recommendations;

    // Constructor, getters, builder pattern implementation...

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<SecurityVulnerability> vulnerabilities = new ArrayList<>();
        private SecurityMetrics metrics;
        private final List<SecurityRecommendation> recommendations = new ArrayList<>();

        public Builder addVulnerability(final SecurityVulnerability vulnerability) {
            vulnerabilities.add(vulnerability);
            return this;
        }

        public SecurityAnalysisResult build() {
            return new SecurityAnalysisResult(vulnerabilities, metrics, recommendations);
        }
    }
}

/**
 * Configuration for security analysis.
 */
public final class SecurityAnalysisConfiguration {
    private final SecurityLevel securityLevel;
    private final Set<String> enabledChecks;
    private final Map<String, Object> customParameters;

    // Implementation...
}
```

### 2. Custom Reporters

Create custom output formats by implementing reporter interfaces:

#### Custom JSON Reporter

```java
package ai.tegmentum.wasmtime4j.comparison.reporters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Custom JSON reporter with specialized formatting for security analysis results.
 */
public final class SecurityJsonReporter {
    private static final Logger LOGGER = Logger.getLogger(SecurityJsonReporter.class.getName());
    private final ObjectMapper objectMapper;

    public SecurityJsonReporter() {
        this.objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Generates a JSON security report.
     *
     * @param securityResults the security analysis results
     * @param comparisonReport the overall comparison report
     * @param outputStream the output stream to write to
     * @throws IOException if the report cannot be written
     */
    public void generateSecurityReport(
            final SecurityAnalysisResult securityResults,
            final ComparisonReport comparisonReport,
            final OutputStream outputStream) throws IOException {

        final SecurityReportData reportData = SecurityReportData.builder()
            .timestamp(Instant.now())
            .vulnerabilities(securityResults.getVulnerabilities())
            .metrics(securityResults.getMetrics())
            .recommendations(securityResults.getRecommendations())
            .comparisonSummary(createComparisonSummary(comparisonReport))
            .build();

        objectMapper.writeValue(outputStream, reportData);
        LOGGER.info("Security JSON report generated successfully");
    }

    private ComparisonSummary createComparisonSummary(final ComparisonReport report) {
        return ComparisonSummary.builder()
            .totalTests(report.getTotalTestCount())
            .passedTests(report.getPassedTestCount())
            .failedTests(report.getFailedTestCount())
            .runtimeComparisons(report.getRuntimeComparisons())
            .build();
    }
}
```

#### Custom HTML Dashboard Component

```java
/**
 * Custom dashboard component for security visualizations.
 */
public final class SecurityDashboardComponent {
    private final TemplateEngine templateEngine;

    public SecurityDashboardComponent() {
        this.templateEngine = TemplateEngine.createDefault();
    }

    /**
     * Generates HTML content for security dashboard section.
     *
     * @param securityResults the security analysis results
     * @return HTML content for the security dashboard
     */
    public String generateSecurityDashboard(final SecurityAnalysisResult securityResults) {
        final Map<String, Object> templateContext = new HashMap<>();
        templateContext.put("vulnerabilities", securityResults.getVulnerabilities());
        templateContext.put("metrics", securityResults.getMetrics());
        templateContext.put("recommendations", securityResults.getRecommendations());
        templateContext.put("chartData", generateChartData(securityResults));

        return templateEngine.process("security-dashboard.html", templateContext);
    }

    private Map<String, Object> generateChartData(final SecurityAnalysisResult results) {
        // Generate data for JavaScript charts
        return Map.of(
            "vulnerabilityBySeverity", groupVulnerabilitiesBySeverity(results),
            "vulnerabilityTrends", calculateVulnerabilityTrends(results),
            "securityScore", calculateSecurityScore(results)
        );
    }
}
```

### 3. Custom Test Integrators

Integrate custom test suites into the analysis pipeline:

#### Custom Test Suite Integration

```java
package ai.tegmentum.wasmtime4j.comparison.integrators;

import ai.tegmentum.wasmtime4j.comparison.analyzers.TestExecutionResult;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Integrator for custom WebAssembly test suites.
 */
public final class CustomTestSuiteIntegrator {
    private static final Logger LOGGER = Logger.getLogger(CustomTestSuiteIntegrator.class.getName());

    /**
     * Executes a custom test suite and returns results for analysis.
     *
     * @param testSuitePath path to the custom test suite
     * @param configuration execution configuration
     * @return future containing test execution results
     */
    public CompletableFuture<List<TestExecutionResult>> executeTestSuite(
            final Path testSuitePath,
            final TestExecutionConfiguration configuration) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Executing custom test suite: " + testSuitePath);

                final List<TestCase> testCases = loadTestCases(testSuitePath);
                final List<TestExecutionResult> results = new ArrayList<>();

                for (final TestCase testCase : testCases) {
                    final TestExecutionResult result = executeTestCase(testCase, configuration);
                    results.add(result);

                    if (configuration.isFailFast() && result.getStatus() == TestStatus.FAILED) {
                        break;
                    }
                }

                LOGGER.info("Custom test suite execution complete: " + results.size() + " tests");
                return results;

            } catch (Exception e) {
                LOGGER.severe("Failed to execute custom test suite: " + e.getMessage());
                throw new RuntimeException("Test suite execution failed", e);
            }
        });
    }

    private List<TestCase> loadTestCases(final Path testSuitePath) {
        // Implementation to load test cases from custom format
        // This could support various formats: JSON, YAML, custom DSL, etc.
        try {
            final List<TestCase> testCases = new ArrayList<>();

            // Example: Load from JSON test definition
            if (testSuitePath.toString().endsWith(".json")) {
                testCases.addAll(loadJsonTestCases(testSuitePath));
            }
            // Example: Load from YAML test definition
            else if (testSuitePath.toString().endsWith(".yaml")) {
                testCases.addAll(loadYamlTestCases(testSuitePath));
            }
            // Example: Load from directory structure
            else if (Files.isDirectory(testSuitePath)) {
                testCases.addAll(loadDirectoryTestCases(testSuitePath));
            }

            return testCases;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test cases", e);
        }
    }

    private TestExecutionResult executeTestCase(final TestCase testCase,
                                              final TestExecutionConfiguration config) {
        final Instant startTime = Instant.now();

        try {
            // Execute the test case using the appropriate runtime
            final WasmRuntime runtime = WasmRuntimeFactory.create(config.getRuntimeType());
            final TestResult result = testCase.execute(runtime);

            return TestExecutionResult.builder()
                .testName(testCase.getName())
                .runtime(config.getRuntimeType())
                .status(result.isSuccess() ? TestStatus.PASSED : TestStatus.FAILED)
                .executionTime(Duration.between(startTime, Instant.now()))
                .output(result.getOutput())
                .errorOutput(result.getErrorOutput())
                .metadata(testCase.getMetadata())
                .build();

        } catch (Exception e) {
            return TestExecutionResult.builder()
                .testName(testCase.getName())
                .runtime(config.getRuntimeType())
                .status(TestStatus.ERROR)
                .executionTime(Duration.between(startTime, Instant.now()))
                .errorOutput(e.getMessage())
                .exception(e)
                .build();
        }
    }
}
```

## Framework Integration

### Registering Custom Components

Use the component registry to register your custom extensions:

```java
/**
 * Custom extension registration example.
 */
public final class CustomExtensionRegistry {

    public static void registerCustomComponents() {
        final AnalysisFramework framework = AnalysisFramework.getInstance();

        // Register custom analyzer
        framework.registerAnalyzer("security", SecurityAnalyzer.class);

        // Register custom reporter
        framework.registerReporter("security-json", SecurityJsonReporter.class);

        // Register custom test integrator
        framework.registerTestIntegrator("custom-suite", CustomTestSuiteIntegrator.class);

        // Register custom dashboard component
        framework.registerDashboardComponent("security", SecurityDashboardComponent.class);
    }
}
```

### Configuration Integration

Add configuration support for your extensions:

```java
/**
 * Configuration provider for custom extensions.
 */
public final class CustomAnalysisConfiguration {

    /**
     * Creates configuration for custom security analysis.
     */
    public static AnalysisConfiguration createSecurityAnalysisConfiguration() {
        return AnalysisConfiguration.builder()
            .enableAnalyzer("security")
            .enableReporter("security-json")
            .addAnalyzerConfiguration("security", Map.of(
                "securityLevel", "HIGH",
                "enabledChecks", Set.of("memory", "privilege", "resources"),
                "failOnVulnerabilities", true
            ))
            .addReporterConfiguration("security-json", Map.of(
                "includeStackTraces", true,
                "prettyPrint", true,
                "timestampFormat", "ISO_INSTANT"
            ))
            .build();
    }
}
```

## Best Practices

### 1. Error Handling

Always implement robust error handling:

```java
public class RobustAnalyzer {

    public AnalysisResult analyze(List<TestExecutionResult> results) {
        try {
            // Analysis logic
            return performAnalysis(results);
        } catch (AnalysisException e) {
            LOGGER.warning("Analysis failed: " + e.getMessage());
            return AnalysisResult.failed(e.getMessage());
        } catch (Exception e) {
            LOGGER.severe("Unexpected error during analysis: " + e.getMessage());
            return AnalysisResult.error("Internal error: " + e.getClass().getSimpleName());
        }
    }
}
```

### 2. Resource Management

Use proper resource management patterns:

```java
public class ResourceManagedReporter implements AutoCloseable {
    private final OutputStream outputStream;
    private final Writer writer;

    public ResourceManagedReporter(Path outputPath) throws IOException {
        this.outputStream = Files.newOutputStream(outputPath);
        this.writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
    }

    @Override
    public void close() throws IOException {
        try (Writer w = writer; OutputStream os = outputStream) {
            // Resources will be closed automatically
        }
    }
}
```

### 3. Performance Considerations

Optimize for performance when processing large datasets:

```java
public class OptimizedAnalyzer {

    public AnalysisResult analyzeParallel(List<TestExecutionResult> results) {
        // Use parallel streams for CPU-intensive operations
        final Map<String, Long> testCounts = results.parallelStream()
            .collect(Collectors.groupingBy(
                TestExecutionResult::getTestType,
                Collectors.counting()
            ));

        // Use CompletableFuture for independent async operations
        final CompletableFuture<Map<String, Double>> performanceMetrics =
            CompletableFuture.supplyAsync(() -> calculatePerformanceMetrics(results));

        final CompletableFuture<List<String>> recommendations =
            CompletableFuture.supplyAsync(() -> generateRecommendations(results));

        // Combine results
        return CompletableFuture.allOf(performanceMetrics, recommendations)
            .thenApply(v -> AnalysisResult.builder()
                .testCounts(testCounts)
                .performanceMetrics(performanceMetrics.join())
                .recommendations(recommendations.join())
                .build())
            .join();
    }
}
```

### 4. Testing Your Extensions

Always write comprehensive tests:

```java
public class SecurityAnalyzerTest {

    @Test
    void testMemorySafetyAnalysis() {
        // Given
        final List<TestExecutionResult> results = Arrays.asList(
            createTestResult("memory_test_1", "buffer overflow detected"),
            createTestResult("memory_test_2", "normal execution"),
            createTestResult("memory_test_3", "memory corruption found")
        );

        final SecurityAnalyzer analyzer = new SecurityAnalyzer();

        // When
        final SecurityAnalysisResult result = analyzer.analyzeSecurityPatterns(
            results, SecurityAnalysisConfiguration.createDefault()
        );

        // Then
        assertThat(result.getVulnerabilityCount()).isEqualTo(2);
        assertThat(result.getVulnerabilities())
            .extracting(SecurityVulnerability::getType)
            .containsExactly(
                VulnerabilityType.MEMORY_CORRUPTION,
                VulnerabilityType.MEMORY_CORRUPTION
            );
    }
}
```

## Examples and Templates

### Maven Plugin Integration

Create a Maven plugin to integrate your custom analyzers:

```xml
<plugin>
    <groupId>ai.tegmentum</groupId>
    <artifactId>wasmtime4j-analysis-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <analysisConfiguration>
            <analyzers>
                <analyzer>
                    <name>security</name>
                    <enabled>true</enabled>
                    <configuration>
                        <securityLevel>HIGH</securityLevel>
                        <failOnVulnerabilities>true</failOnVulnerabilities>
                    </configuration>
                </analyzer>
            </analyzers>
            <reporters>
                <reporter>
                    <name>security-json</name>
                    <outputFile>${project.build.directory}/security-report.json</outputFile>
                </reporter>
            </reporters>
        </analysisConfiguration>
    </configuration>
    <executions>
        <execution>
            <id>security-analysis</id>
            <phase>test</phase>
            <goals>
                <goal>analyze</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### CLI Integration

Integrate with the command-line interface:

```bash
# Run analysis with custom extensions
./mvnw wasmtime4j:analyze \
    -Dwasmtime4j.analysis.analyzers=coverage,behavioral,security \
    -Dwasmtime4j.analysis.reporters=html,json,security-json \
    -Dwasmtime4j.analysis.security.level=HIGH \
    -Dwasmtime4j.analysis.output.dir=target/analysis-reports
```

## Migration and Compatibility

When updating the framework, ensure your extensions remain compatible:

1. **Version Compatibility**: Check framework version compatibility
2. **API Changes**: Monitor breaking changes in core interfaces
3. **Configuration Updates**: Update configuration schemas as needed
4. **Testing**: Re-run extension tests with new framework versions

## Getting Help

- **Documentation**: Check the [API reference](../reference/api-documentation.md) for interface details
- **Examples**: See [example implementations](../examples/custom-analyzers/) for reference patterns
- **Community**: Join discussions in [GitHub Discussions](https://github.com/tegmentum/wasmtime4j/discussions)
- **Issues**: Report bugs or request features in [GitHub Issues](https://github.com/tegmentum/wasmtime4j/issues)

## Next Steps

- Read the [Custom Test Integration Guide](custom-test-integration.md)
- Explore [WASI Integration Patterns](wasi-integration-patterns.md)
- Check out [Example Custom Analyzers](../examples/custom-analyzers/)
- Review [API Reference Documentation](../reference/api-documentation.md)