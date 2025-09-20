# Developer Contribution Guidelines

This guide provides comprehensive guidelines for developers contributing to the Wasmtime4j comparison testing framework. It covers development workflows, extension contribution processes, and best practices for maintaining code quality.

## Overview

The Wasmtime4j testing framework is designed to be extensible and welcomes contributions from the community. This document outlines:

- **Extension Development**: How to create and contribute custom analyzers, reporters, and integrators
- **Development Workflows**: Standard processes for development, testing, and review
- **Code Quality Standards**: Requirements for code style, testing, and documentation
- **Release Processes**: How extensions are reviewed, integrated, and released

## Contribution Types

### 1. Core Framework Contributions

Contributions to the core testing framework infrastructure:

- **Framework APIs**: Improvements to base interfaces and abstractions
- **Test Infrastructure**: Enhancements to test execution and management
- **Performance Optimizations**: Improvements to framework performance
- **Bug Fixes**: Fixes to existing functionality

### 2. Extension Contributions

New components that extend framework functionality:

- **Custom Analyzers**: New analysis capabilities (security, performance, compatibility)
- **Custom Reporters**: New output formats and visualization options
- **Test Integrators**: Support for new test suite formats and sources
- **Dashboard Components**: New visualization and reporting components

### 3. Documentation and Examples

Documentation and educational materials:

- **API Documentation**: Interface and usage documentation
- **Tutorial Content**: Step-by-step guides and examples
- **Best Practices**: Patterns and recommendations
- **Example Implementations**: Working code samples

## Development Workflow

### 1. Setup Development Environment

#### Prerequisites

```bash
# Required tools
- Java 8+ (for JNI development)
- Java 23+ (for Panama development)
- Maven 3.6+
- Git
- IDE (IntelliJ IDEA or Eclipse recommended)

# Optional tools for advanced development
- Docker (for cross-platform testing)
- JMH (for performance benchmarking)
- Checkstyle plugin
- SpotBugs plugin
```

#### Initial Setup

```bash
# Fork and clone the repository
git clone https://github.com/your-username/wasmtime4j.git
cd wasmtime4j

# Setup development branch
git checkout -b feature/your-feature-name

# Build and test
./mvnw clean compile
./mvnw test

# Verify code quality
./mvnw checkstyle:check spotless:check spotbugs:check
```

#### IDE Configuration

**IntelliJ IDEA Setup:**

1. Import as Maven project
2. Install required plugins:
   - Checkstyle-IDEA
   - SpotBugs
   - SonarLint (optional)
3. Configure code style:
   - Import `checkstyle.xml` as code style
   - Enable auto-formatting on save
4. Configure run configurations for tests

**Eclipse Setup:**

1. Import as Maven project
2. Install Eclipse plugins:
   - Checkstyle Plugin
   - SpotBugs Plugin
3. Configure code formatting with Google Java Style

### 2. Extension Development Process

#### Creating a New Analyzer

```bash
# 1. Create analyzer module structure
mkdir -p src/main/java/ai/tegmentum/wasmtime4j/comparison/analyzers/custom
mkdir -p src/test/java/ai/tegmentum/wasmtime4j/comparison/analyzers/custom

# 2. Implement analyzer interface
# See API Reference for interface details

# 3. Create test cases
# Follow existing test patterns

# 4. Add documentation
# Include Javadoc and usage examples

# 5. Register analyzer (if core contribution)
# Update component registry
```

#### Example: Security Analyzer Development

```java
// 1. Create analyzer class
package ai.tegmentum.wasmtime4j.comparison.analyzers.custom;

@AnalyzerComponent(
    name = "security",
    description = "Security vulnerability analysis",
    version = "1.0.0"
)
public final class SecurityAnalyzer implements TestResultAnalyzer {

    @Override
    public AnalysisResult analyze(final List<TestExecutionResult> results,
                                 final Map<String, Object> configuration) {
        // Implementation here
        return performSecurityAnalysis(results, configuration);
    }

    // Additional methods...
}

// 2. Create supporting classes
public final class SecurityAnalysisResult implements AnalysisResult {
    // Implementation...
}

public final class SecurityConfiguration {
    // Configuration handling...
}

// 3. Create comprehensive tests
public class SecurityAnalyzerTest extends AnalyzerTestBase {

    @Test
    void testMemoryVulnerabilityDetection() {
        // Test implementation
    }

    @Test
    void testPrivilegeEscalationDetection() {
        // Test implementation
    }

    @Test
    void testConfigurationValidation() {
        // Test implementation
    }
}

// 4. Create integration tests
public class SecurityAnalyzerIntegrationTest {

    @Test
    void testSecurityAnalyzerWithRealData() {
        // Integration test with actual test results
    }
}
```

### 3. Testing Requirements

#### Unit Testing Standards

```java
/**
 * Base test class for analyzer testing.
 */
public abstract class AnalyzerTestBase {

    // Test data creation utilities
    protected List<TestExecutionResult> createTestResults(TestResultTemplate... templates) {
        return Arrays.stream(templates)
            .map(this::createTestResult)
            .collect(Collectors.toList());
    }

    // Common assertions
    protected void assertAnalysisSuccessful(AnalysisResult result) {
        assertThat(result.getStatus()).isEqualTo(AnalysisStatus.SUCCESS);
        assertThat(result.getErrorMessage()).isEmpty();
    }

    // Performance testing utilities
    protected void assertAnalysisPerformance(Supplier<AnalysisResult> analysisOperation,
                                           Duration maxExecutionTime) {
        final Instant start = Instant.now();
        final AnalysisResult result = analysisOperation.get();
        final Duration executionTime = Duration.between(start, Instant.now());

        assertThat(executionTime).isLessThan(maxExecutionTime);
        assertAnalysisSuccessful(result);
    }
}

/**
 * Example analyzer test with comprehensive coverage.
 */
public class CustomAnalyzerTest extends AnalyzerTestBase {

    private CustomAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new CustomAnalyzer();
    }

    @Test
    void testAnalyzeEmptyResults() {
        // Test with empty input
        final List<TestExecutionResult> results = Collections.emptyList();
        final Map<String, Object> config = Map.of();

        assertThatThrownBy(() -> analyzer.analyze(results, config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("results cannot be empty");
    }

    @Test
    void testAnalyzeValidResults() {
        // Test with valid input
        final List<TestExecutionResult> results = createTestResults(
            TestResultTemplate.success("test1", RuntimeType.JNI),
            TestResultTemplate.success("test2", RuntimeType.PANAMA)
        );

        final AnalysisResult result = analyzer.analyze(results, Map.of());

        assertAnalysisSuccessful(result);
        assertThat(result.getAllResults()).containsKey("processedTests");
    }

    @Test
    void testAnalyzeWithInvalidConfiguration() {
        // Test configuration validation
        final List<TestExecutionResult> results = createTestResults(
            TestResultTemplate.success("test1", RuntimeType.JNI)
        );

        final Map<String, Object> invalidConfig = Map.of("invalidKey", "invalidValue");

        assertThatThrownBy(() -> analyzer.analyze(results, invalidConfig))
            .isInstanceOf(AnalysisException.class);
    }

    @Test
    void testAnalyzePerformance() {
        // Test performance with large dataset
        final List<TestExecutionResult> results = createLargeTestDataset(1000);

        assertAnalysisPerformance(
            () -> analyzer.analyze(results, Map.of()),
            Duration.ofSeconds(5)
        );
    }

    @Test
    void testAnalyzeThreadSafety() {
        // Test thread safety
        final List<TestExecutionResult> results = createTestResults(
            TestResultTemplate.success("test1", RuntimeType.JNI)
        );

        final List<CompletableFuture<AnalysisResult>> futures = IntStream.range(0, 10)
            .mapToObj(i -> CompletableFuture.supplyAsync(() ->
                analyzer.analyze(results, Map.of())))
            .collect(Collectors.toList());

        final List<AnalysisResult> analysisResults = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        analysisResults.forEach(this::assertAnalysisSuccessful);
    }
}
```

#### Integration Testing

```java
/**
 * Integration test for custom components.
 */
public class CustomComponentIntegrationTest {

    @Test
    void testCustomAnalyzerIntegration() {
        // Register custom analyzer
        ComponentRegistry.getInstance().registerAnalyzer("custom", CustomAnalyzer.class);

        // Create framework configuration
        final AnalysisConfiguration config = AnalysisConfiguration.builder()
            .enableAnalyzer("custom")
            .addAnalyzerConfiguration("custom", Map.of(
                "parameter1", "value1",
                "parameter2", 42
            ))
            .build();

        // Execute analysis
        final AnalysisFramework framework = AnalysisFramework.getInstance();
        final List<TestExecutionResult> testResults = createRealTestResults();
        final List<AnalysisResult> results = framework.runAnalysis(testResults, config);

        // Verify integration
        assertThat(results).hasSize(1);
        final AnalysisResult customResult = results.get(0);
        assertThat(customResult.getAnalysisType()).isEqualTo(AnalysisType.CUSTOM);
        assertAnalysisSuccessful(customResult);
    }

    @Test
    void testCustomReporterIntegration() {
        // Test custom reporter integration
        ComponentRegistry.getInstance().registerReporter("custom-xml", CustomXmlReporter.class);

        final ReportingConfiguration config = ReportingConfiguration.builder()
            .enableReporter("custom-xml")
            .outputDirectory(Paths.get("target/test-reports"))
            .build();

        final ReportingFramework framework = ReportingFramework.getInstance();
        final List<AnalysisResult> analysisResults = createAnalysisResults();

        framework.generateReports(analysisResults, config);

        // Verify report generation
        final Path reportFile = Paths.get("target/test-reports/custom-report.xml");
        assertThat(reportFile).exists();
        assertThat(Files.size(reportFile)).isGreaterThan(0);
    }
}
```

### 4. Code Quality Requirements

#### Coding Standards

```java
/**
 * Example of properly documented and structured analyzer.
 *
 * Key requirements:
 * - Comprehensive Javadoc
 * - Defensive programming
 * - Proper error handling
 * - Resource management
 * - Thread safety
 */
@AnalyzerComponent(name = "example", description = "Example analyzer implementation")
public final class ExampleAnalyzer implements TestResultAnalyzer {
    private static final Logger LOGGER = Logger.getLogger(ExampleAnalyzer.class.getName());

    // Constants at the top
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);
    private static final int MAX_RESULTS_TO_PROCESS = 10000;

    // Thread-safe fields
    private final AtomicLong analysisCount = new AtomicLong();
    private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();

    /**
     * Creates a new example analyzer with default configuration.
     */
    public ExampleAnalyzer() {
        // Initialization if needed
    }

    @Override
    public String getName() {
        return "example";
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public Map<String, Object> getMetadata() {
        return Map.of(
            "description", "Example analyzer for demonstration",
            "author", "Development Team",
            "analysisCount", analysisCount.get()
        );
    }

    @Override
    public Set<AnalysisType> getSupportedAnalysisTypes() {
        return Set.of(AnalysisType.CUSTOM);
    }

    /**
     * Analyzes test execution results.
     *
     * @param results the test execution results to analyze (must not be null or empty)
     * @param configuration analysis configuration parameters (must not be null)
     * @return analysis results containing processed data and insights
     * @throws AnalysisException if analysis fails due to invalid input or processing error
     */
    @Override
    public AnalysisResult analyze(final List<TestExecutionResult> results,
                                 final Map<String, Object> configuration) throws AnalysisException {

        // Input validation
        Objects.requireNonNull(results, "results cannot be null");
        Objects.requireNonNull(configuration, "configuration cannot be null");

        if (results.isEmpty()) {
            throw new IllegalArgumentException("results cannot be empty");
        }

        if (results.size() > MAX_RESULTS_TO_PROCESS) {
            throw new IllegalArgumentException("too many results to process: " + results.size());
        }

        // Configuration validation
        final ValidationResult validationResult = validateConfiguration(configuration);
        if (!validationResult.isValid()) {
            throw new AnalysisException("Invalid configuration: " +
                String.join(", ", validationResult.getErrors()));
        }

        try {
            LOGGER.info("Starting analysis of " + results.size() + " test results");
            final long analysisId = analysisCount.incrementAndGet();

            final AnalysisResult result = performAnalysis(results, configuration, analysisId);

            LOGGER.info("Analysis complete (ID: " + analysisId + ")");
            return result;

        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (Exception e) {
            LOGGER.severe("Unexpected error during analysis: " + e.getMessage());
            throw new AnalysisException("Analysis failed due to unexpected error", e);
        }
    }

    @Override
    public ValidationResult validateConfiguration(final Map<String, Object> configuration) {
        final ValidationResult.Builder builder = ValidationResult.builder();

        // Validate required parameters
        if (!configuration.containsKey("requiredParam")) {
            builder.addError("requiredParam is missing");
        }

        // Validate parameter types and values
        final Object requiredParam = configuration.get("requiredParam");
        if (requiredParam != null && !(requiredParam instanceof String)) {
            builder.addError("requiredParam must be a string");
        }

        // Additional validation logic...

        return builder.build();
    }

    @Override
    public boolean supportsConfiguration(final Map<String, Object> configuration) {
        return validateConfiguration(configuration).isValid();
    }

    /**
     * Performs the actual analysis logic.
     */
    private AnalysisResult performAnalysis(final List<TestExecutionResult> results,
                                         final Map<String, Object> configuration,
                                         final long analysisId) {

        // Use try-with-resources for resource management
        try (AnalysisContext context = createAnalysisContext(configuration)) {

            final GenericAnalysisResult.Builder resultBuilder =
                GenericAnalysisResult.builder(AnalysisType.CUSTOM);

            // Process results efficiently
            final Map<String, Long> statusCounts = results.parallelStream()
                .collect(Collectors.groupingBy(
                    result -> result.getStatus().name(),
                    Collectors.counting()
                ));

            resultBuilder.result("statusCounts", statusCounts);
            resultBuilder.result("totalResults", results.size());
            resultBuilder.result("analysisId", analysisId);

            // Add analysis-specific results
            addCustomAnalysisResults(results, configuration, resultBuilder);

            return resultBuilder.build();
        }
    }

    private void addCustomAnalysisResults(final List<TestExecutionResult> results,
                                        final Map<String, Object> configuration,
                                        final GenericAnalysisResult.Builder resultBuilder) {
        // Custom analysis logic here
        // This is where the specific analyzer functionality would be implemented
    }

    private AnalysisContext createAnalysisContext(final Map<String, Object> configuration) {
        return new AnalysisContext(configuration);
    }

    /**
     * Resource management for analysis context.
     */
    private static final class AnalysisContext implements AutoCloseable {
        private final Map<String, Object> configuration;
        private volatile boolean closed = false;

        AnalysisContext(final Map<String, Object> configuration) {
            this.configuration = new HashMap<>(configuration);
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                // Cleanup resources
            }
        }
    }
}
```

#### Documentation Requirements

```java
/**
 * Custom reporter for generating specialized analysis reports.
 *
 * <p>This reporter provides the following features:
 * <ul>
 *   <li>Multiple output formats (JSON, XML, HTML)</li>
 *   <li>Configurable detail levels</li>
 *   <li>Template-based customization</li>
 *   <li>Streaming support for large datasets</li>
 * </ul>
 *
 * <h3>Configuration Parameters</h3>
 * <table>
 *   <tr><th>Parameter</th><th>Type</th><th>Description</th><th>Default</th></tr>
 *   <tr><td>format</td><td>String</td><td>Output format (json, xml, html)</td><td>json</td></tr>
 *   <tr><td>detailLevel</td><td>String</td><td>Detail level (summary, detailed, verbose)</td><td>detailed</td></tr>
 *   <tr><td>templatePath</td><td>String</td><td>Custom template path (optional)</td><td>null</td></tr>
 * </table>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Register the reporter
 * ComponentRegistry.getInstance().registerReporter("custom", CustomReporter.class);
 *
 * // Configure and use
 * ReportingConfiguration config = ReportingConfiguration.builder()
 *     .enableReporter("custom")
 *     .addReporterConfiguration("custom", Map.of(
 *         "format", "html",
 *         "detailLevel", "verbose"
 *     ))
 *     .build();
 *
 * ReportingFramework framework = ReportingFramework.getInstance();
 * framework.generateReports(analysisResults, config);
 * }</pre>
 *
 * @since 1.0.0
 * @author Development Team
 * @see ResultReporter
 * @see ReportingFramework
 */
@ReporterComponent(
    name = "custom",
    description = "Custom multi-format reporter",
    supportedFormats = {"json", "xml", "html"}
)
public final class CustomReporter implements ResultReporter {
    // Implementation...
}
```

### 5. Review Process

#### Pull Request Requirements

**Before Submitting:**

1. **Code Quality Checks:**
   ```bash
   # Run all quality checks
   ./mvnw clean compile
   ./mvnw test
   ./mvnw checkstyle:check
   ./mvnw spotless:check
   ./mvnw spotbugs:check

   # Run integration tests
   ./mvnw test -P integration-tests
   ```

2. **Documentation Updates:**
   - Update API documentation for new interfaces
   - Add usage examples for new features
   - Update README if needed

3. **Test Coverage:**
   ```bash
   # Generate coverage report
   ./mvnw test jacoco:report

   # Check coverage requirements
   # - Unit tests: minimum 80% line coverage
   # - Integration tests: all happy paths covered
   # - Error conditions: all exception paths tested
   ```

**Pull Request Template:**

```markdown
## Summary
Brief description of the changes made.

## Type of Change
- [ ] Bug fix (non-breaking change that fixes an issue)
- [ ] New feature (non-breaking change that adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update
- [ ] Extension contribution (new analyzer, reporter, or integrator)

## Changes Made
- List specific changes
- Include any breaking changes
- Mention new dependencies

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing performed
- [ ] Performance impact assessed

## Documentation
- [ ] Code is self-documenting with appropriate comments
- [ ] Javadoc updated for public APIs
- [ ] README updated (if needed)
- [ ] Examples updated (if needed)

## Quality Checklist
- [ ] Code follows style guidelines
- [ ] No new warnings or errors
- [ ] Performance impact is acceptable
- [ ] Security considerations addressed

## Related Issues
- Fixes #123
- Related to #456

## Screenshots (if applicable)
Include screenshots for UI changes or new visualizations.
```

#### Review Criteria

**Functional Review:**
- Code correctly implements intended functionality
- All edge cases are handled appropriately
- Error handling is comprehensive and appropriate
- Performance characteristics are acceptable

**Code Quality Review:**
- Code follows established style guidelines
- Code is readable and well-documented
- Code follows SOLID principles and best practices
- No code duplication or unnecessary complexity

**Test Quality Review:**
- Test coverage meets requirements (minimum 80% line coverage)
- Tests are meaningful and test real functionality
- Tests include both positive and negative cases
- Integration tests cover end-to-end scenarios

**Documentation Review:**
- Public APIs are fully documented with Javadoc
- Usage examples are provided and accurate
- Documentation is clear and helpful
- Breaking changes are clearly documented

#### Reviewer Guidelines

**For Core Team Members:**

```markdown
## Review Checklist

### Functionality
- [ ] Does the code solve the problem it claims to solve?
- [ ] Are all requirements from the issue addressed?
- [ ] Are edge cases handled appropriately?
- [ ] Is error handling comprehensive?

### Code Quality
- [ ] Does the code follow project style guidelines?
- [ ] Is the code readable and maintainable?
- [ ] Are there any code smells or anti-patterns?
- [ ] Is the code properly structured and organized?

### Tests
- [ ] Do tests actually test the functionality?
- [ ] Is test coverage adequate?
- [ ] Are tests clear and maintainable?
- [ ] Do integration tests cover realistic scenarios?

### Documentation
- [ ] Are public APIs documented with Javadoc?
- [ ] Are usage examples clear and correct?
- [ ] Is documentation up to date?
- [ ] Are breaking changes documented?

### Performance
- [ ] Has performance impact been considered?
- [ ] Are there any obvious performance issues?
- [ ] Are benchmarks needed/provided?

### Security
- [ ] Are there any security implications?
- [ ] Is input validation appropriate?
- [ ] Are security best practices followed?
```

### 6. Release Process

#### Extension Release Workflow

1. **Development Phase:**
   - Feature development in feature branch
   - Comprehensive testing and documentation
   - Code review and approval

2. **Integration Phase:**
   - Merge to development branch
   - Integration testing with framework
   - Performance validation

3. **Release Candidate:**
   - Create release branch
   - Final testing and validation
   - Documentation review

4. **Release:**
   - Tag release version
   - Update component registry
   - Publish documentation
   - Announce to community

#### Versioning Strategy

**Semantic Versioning for Extensions:**
- **MAJOR**: Breaking API changes
- **MINOR**: New features, backward compatible
- **PATCH**: Bug fixes, backward compatible

**Framework Compatibility:**
- Extensions specify compatible framework versions
- Framework maintains backward compatibility for extensions
- Breaking changes in framework trigger major version bump

### 7. Community Guidelines

#### Communication Channels

- **GitHub Issues**: Bug reports and feature requests
- **GitHub Discussions**: General questions and community discussion
- **Pull Request Comments**: Code review and technical discussion
- **Email**: Private communication for security issues

#### Code of Conduct

All contributors are expected to:
- Be respectful and inclusive in all interactions
- Provide constructive feedback during code reviews
- Help newcomers and answer questions when possible
- Follow project guidelines and best practices
- Report security issues responsibly

#### Recognition

Contributors are recognized through:
- Listing in CONTRIBUTORS.md file
- Mention in release notes for significant contributions
- Attribution in documentation and examples
- Optional blog posts about major features

## Best Practices Summary

### 1. Development Best Practices

```java
// Always validate inputs
private void validateInputs(Object... inputs) {
    Arrays.stream(inputs).forEach(input ->
        Objects.requireNonNull(input, "Input cannot be null"));
}

// Use defensive programming
public AnalysisResult analyze(List<TestExecutionResult> results) {
    if (results.isEmpty()) {
        return AnalysisResult.empty();
    }
    // Continue with analysis...
}

// Implement proper resource management
try (ResourceManager manager = createResourceManager()) {
    return performAnalysis(manager);
}

// Use thread-safe patterns
private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();
private final AtomicLong counter = new AtomicLong();
```

### 2. Testing Best Practices

```java
// Create meaningful test names
@Test
void testAnalyzeWithEmptyResultsList_ThrowsIllegalArgumentException() {
    // Test implementation
}

// Use parametrized tests for multiple scenarios
@ParameterizedTest
@ValueSource(strings = {"json", "xml", "html"})
void testReporterSupportsFormat(String format) {
    // Test implementation
}

// Test error conditions
@Test
void testAnalyzeWithInvalidConfiguration_ThrowsAnalysisException() {
    assertThatThrownBy(() -> analyzer.analyze(results, invalidConfig))
        .isInstanceOf(AnalysisException.class)
        .hasMessageContaining("Invalid configuration");
}
```

### 3. Documentation Best Practices

```java
/**
 * Analyzes test execution results to identify patterns and generate insights.
 *
 * <p>This method processes the provided test results and applies various
 * analysis algorithms based on the configuration parameters. The analysis
 * can identify performance bottlenecks, behavioral discrepancies, and
 * coverage gaps.
 *
 * @param results the test execution results to analyze (must not be null or empty)
 * @param configuration configuration parameters for the analysis (must not be null)
 * @return analysis results containing insights and recommendations
 * @throws AnalysisException if the analysis fails due to invalid input or processing errors
 * @throws IllegalArgumentException if the results list is empty or configuration is invalid
 *
 * @since 1.0.0
 * @see TestExecutionResult
 * @see AnalysisConfiguration
 */
public AnalysisResult analyze(List<TestExecutionResult> results,
                            Map<String, Object> configuration) {
    // Implementation...
}
```

## Getting Help

### Resources
- **API Documentation**: Complete interface documentation
- **Examples Repository**: Working code samples and templates
- **Developer Guide**: Comprehensive development information
- **Community Forum**: Ask questions and get help from other developers

### Contact
- **Technical Questions**: GitHub Discussions
- **Bug Reports**: GitHub Issues
- **Security Issues**: Email to security@tegmentum.ai
- **General Inquiries**: Email to wasmtime4j@tegmentum.ai

## Next Steps

- Read the [Extension Development Guide](extending-analysis-framework.md) for detailed implementation patterns
- Explore [API Reference](api-reference.md) for complete interface documentation
- Check out [Example Implementations](../examples/) for working code samples
- Review [WASI Integration Patterns](wasi-integration-patterns.md) for WASI-specific development