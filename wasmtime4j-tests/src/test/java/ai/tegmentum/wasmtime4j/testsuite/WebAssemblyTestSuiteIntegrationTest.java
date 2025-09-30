package ai.tegmentum.wasmtime4j.testsuite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Integration test for WebAssembly test suite integration. */
final class WebAssemblyTestSuiteIntegrationTest {

  @TempDir private Path tempDir;

  private TestSuiteConfiguration configuration;
  private WebAssemblyTestSuiteIntegration testSuite;

  @BeforeEach
  void setUp() throws Exception {
    // Create test suite directory structure
    final Path testSuiteDir = tempDir.resolve("test-suite");
    final Path outputDir = tempDir.resolve("output");
    Files.createDirectories(testSuiteDir);
    Files.createDirectories(outputDir);

    // Create minimal test structure
    createTestStructure(testSuiteDir);

    // Configure test suite
    configuration =
        TestSuiteConfiguration.builder()
            .enableOfficialTests(true)
            .enableWasmtimeTests(false) // Skip for unit tests
            .enableCustomTests(true)
            .enabledRuntimes(EnumSet.of(TestRuntime.JNI)) // Test with JNI only for simplicity
            .maxConcurrentTests(1)
            .testTimeoutMinutes(1)
            .enableCrossRuntimeComparison(false) // Single runtime
            .enablePerformanceAnalysis(false) // Skip for unit tests
            .enableCoverageAnalysis(true)
            .enableConsoleReport(false) // Avoid console noise
            .enableHtmlReport(false)
            .enableJsonReport(true)
            .enableXmlReport(true)
            .testSuiteBaseDirectory(testSuiteDir)
            .outputDirectory(outputDir)
            .testFilters(TestFilterConfiguration.noFiltering())
            .regressionConfig(RegressionDetectionConfiguration.builder().enabled(false).build())
            .build();

    testSuite = new WebAssemblyTestSuiteIntegration(configuration);
  }

  private void createTestStructure(final Path testSuiteDir) throws Exception {
    // Create spec-tests directory with minimal test case
    final Path specTestsDir = testSuiteDir.resolve("spec-tests/core");
    Files.createDirectories(specTestsDir);

    // Create a minimal spec test JSON file
    final String specTestJson =
        """
            {
              "source_filename": "test.wast",
              "commands": [
                {
                  "type": "module",
                  "line": 1,
                  "filename": "test.0.wasm"
                },
                {
                  "type": "assert_return",
                  "line": 2,
                  "action": {
                    "type": "invoke",
                    "field": "test",
                    "args": []
                  },
                  "expected": [
                    {
                      "type": "i32",
                      "value": "42"
                    }
                  ]
                }
              ]
            }
            """;

    Files.writeString(specTestsDir.resolve("test.json"), specTestJson);

    // Create custom-tests directory with minimal test case
    final Path customTestsDir = testSuiteDir.resolve("custom-tests/jni");
    Files.createDirectories(customTestsDir);

    // Create a minimal WASM binary (empty module)
    final byte[] minimalWasm = {
      0x00, 0x61, 0x73, 0x6d, // Magic number
      0x01, 0x00, 0x00, 0x00 // Version
    };
    Files.write(customTestsDir.resolve("minimal.wasm"), minimalWasm);
  }

  @Test
  void testTestDiscovery() throws Exception {
    final Collection<WebAssemblyTestCase> testCases = testSuite.discoverTests();

    assertThat(testCases).isNotEmpty();
    assertThat(testCases).anyMatch(tc -> tc.getCategory() == TestCategory.SPEC_CORE);
    assertThat(testCases).anyMatch(tc -> tc.getCategory() == TestCategory.JAVA_JNI);
  }

  @Test
  void testTestExecution() throws Exception {
    final Collection<WebAssemblyTestCase> testCases = testSuite.discoverTests();
    assertThat(testCases).isNotEmpty();

    // Execute a small subset of tests
    final var limitedTestCases = testCases.stream().limit(2).toList();

    final TestExecutionResults results = testSuite.executeTests(limitedTestCases);

    assertThat(results).isNotNull();
    assertThat(results.getTotalTestCount()).isPositive();
    assertThat(results.getRuntimeResults()).containsKey(TestRuntime.JNI);
  }

  @Test
  void testAnalysis() throws Exception {
    final Collection<WebAssemblyTestCase> testCases = testSuite.discoverTests();
    final var limitedTestCases = testCases.stream().limit(1).toList();

    final TestExecutionResults results = testSuite.executeTests(limitedTestCases);
    final TestAnalysisReport analysisReport = testSuite.analyzeResults(results);

    assertThat(analysisReport).isNotNull();
    assertThat(analysisReport.getCoverageAnalysis()).isNotNull();
    assertThat(analysisReport.getInsights()).isNotNull();
  }

  @Test
  void testReportGeneration() throws Exception {
    final Collection<WebAssemblyTestCase> testCases = testSuite.discoverTests();
    final var limitedTestCases = testCases.stream().limit(1).toList();

    final TestExecutionResults results = testSuite.executeTests(limitedTestCases);
    final TestAnalysisReport analysisReport = testSuite.analyzeResults(results);

    assertDoesNotThrow(() -> testSuite.generateReports(results, analysisReport));

    // Verify reports were generated
    final Path outputDir = configuration.getOutputDirectory();
    assertThat(Files.exists(outputDir.resolve("test-report.json"))).isTrue();
    assertThat(Files.exists(outputDir.resolve("test-report.xml"))).isTrue();
  }

  @Test
  void testCompleteWorkflow() throws Exception {
    final ComprehensiveTestResults results = testSuite.runCompleteTestSuite();

    assertThat(results).isNotNull();
    assertThat(results.getExecutionResults()).isNotNull();
    assertThat(results.getAnalysisReport()).isNotNull();
    assertThat(results.getExecutionTimeMs()).isPositive();
  }

  @Test
  void testFilterConfiguration() throws Exception {
    // Test with filters
    final TestFilterConfiguration filters =
        TestFilterConfiguration.builder()
            .includedCategories(EnumSet.of(TestCategory.SPEC_CORE))
            .build();

    final TestSuiteConfiguration filteredConfig =
        TestSuiteConfiguration.builder()
            .enableOfficialTests(true)
            .enableCustomTests(false)
            .enabledRuntimes(EnumSet.of(TestRuntime.JNI))
            .testSuiteBaseDirectory(configuration.getTestSuiteBaseDirectory())
            .outputDirectory(configuration.getOutputDirectory())
            .testFilters(filters)
            .build();

    final WebAssemblyTestSuiteIntegration filteredTestSuite =
        new WebAssemblyTestSuiteIntegration(filteredConfig);

    final Collection<WebAssemblyTestCase> testCases = filteredTestSuite.discoverTests();

    // All test cases should be SPEC_CORE category
    assertThat(testCases).allMatch(tc -> tc.getCategory() == TestCategory.SPEC_CORE);
  }

  @Test
  void testCiConfiguration() {
    final TestSuiteConfiguration ciConfig = WebAssemblyTestSuiteIntegration.createCIConfiguration();

    assertThat(ciConfig).isNotNull();
    assertThat(ciConfig.isOfficialTestsEnabled()).isTrue();
    assertThat(ciConfig.isWasmtimeTestsEnabled()).isFalse();
    assertThat(ciConfig.getMaxConcurrentTests()).isEqualTo(2);
    assertThat(ciConfig.getTestTimeoutMinutes()).isEqualTo(15);
    assertThat(ciConfig.isPerformanceAnalysisEnabled()).isFalse();
    assertThat(ciConfig.isJsonReportEnabled()).isTrue();
  }

  @Test
  void testDefaultConfiguration() {
    final TestSuiteConfiguration defaultConfig =
        WebAssemblyTestSuiteIntegration.createDefaultConfiguration();

    assertThat(defaultConfig).isNotNull();
    assertThat(defaultConfig.isOfficialTestsEnabled()).isTrue();
    assertThat(defaultConfig.isWasmtimeTestsEnabled()).isTrue();
    assertThat(defaultConfig.isCustomTestsEnabled()).isTrue();
    assertThat(defaultConfig.getEnabledRuntimes()).containsAll(EnumSet.allOf(TestRuntime.class));
    assertThat(defaultConfig.isCrossRuntimeComparisonEnabled()).isTrue();
    assertThat(defaultConfig.isPerformanceAnalysisEnabled()).isTrue();
    assertThat(defaultConfig.isCoverageAnalysisEnabled()).isTrue();
  }

  @Test
  void testTestCaseBuilder() {
    final WebAssemblyTestCase testCase =
        WebAssemblyTestCase.builder()
            .testId("test_builder")
            .testName("Test Builder")
            .category(TestCategory.CUSTOM)
            .description("Test case for builder pattern")
            .expected(TestExpectedResult.PASS)
            .tags(List.of("unit", "builder"))
            .complexity(TestComplexity.SIMPLE)
            .estimatedExecutionTimeMs(1000)
            .build();

    assertThat(testCase).isNotNull();
    assertThat(testCase.getTestId()).isEqualTo("test_builder");
    assertThat(testCase.getTestName()).isEqualTo("Test Builder");
    assertThat(testCase.getCategory()).isEqualTo(TestCategory.CUSTOM);
    assertThat(testCase.getExpected()).isEqualTo(TestExpectedResult.PASS);
    assertThat(testCase.getTags()).containsExactly("unit", "builder");
    assertThat(testCase.getComplexity()).isEqualTo(TestComplexity.SIMPLE);
    assertThat(testCase.hasTag("unit")).isTrue();
    assertThat(testCase.hasTag("nonexistent")).isFalse();
  }

  @Test
  void testTestResultBuilder() {
    final WebAssemblyTestCase testCase =
        WebAssemblyTestCase.builder().testId("test_result").testName("Test Result").build();

    final TestResult result =
        TestResult.builder()
            .testCase(testCase)
            .runtime(TestRuntime.JNI)
            .status(TestStatus.PASSED)
            .executionTimeMs(500)
            .output("Test completed successfully")
            .build();

    assertThat(result).isNotNull();
    assertThat(result.getTestCase()).isEqualTo(testCase);
    assertThat(result.getRuntime()).isEqualTo(TestRuntime.JNI);
    assertThat(result.getStatus()).isEqualTo(TestStatus.PASSED);
    assertThat(result.getExecutionTimeMs()).isEqualTo(500);
    assertThat(result.getOutput()).isEqualTo("Test completed successfully");
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.isFailure()).isFalse();
  }

  @Test
  void testTestFilterMatching() {
    final WebAssemblyTestCase testCase =
        WebAssemblyTestCase.builder()
            .testId("test_filter")
            .testName("Test Filter")
            .category(TestCategory.SPEC_CORE)
            .tags(List.of("spec", "core"))
            .complexity(TestComplexity.SIMPLE)
            .estimatedExecutionTimeMs(2000)
            .build();

    // Test category inclusion
    final TestFilterConfiguration categoryFilter =
        TestFilterConfiguration.builder()
            .includedCategories(EnumSet.of(TestCategory.SPEC_CORE))
            .build();
    assertThat(categoryFilter.matches(testCase)).isTrue();

    // Test tag inclusion
    final TestFilterConfiguration tagFilter =
        TestFilterConfiguration.builder().includedTags(List.of("spec")).build();
    assertThat(tagFilter.matches(testCase)).isTrue();

    // Test complexity filter
    final TestFilterConfiguration complexityFilter =
        TestFilterConfiguration.builder()
            .allowedComplexities(EnumSet.of(TestComplexity.SIMPLE))
            .build();
    assertThat(complexityFilter.matches(testCase)).isTrue();

    // Test exclusion
    final TestFilterConfiguration exclusionFilter =
        TestFilterConfiguration.builder()
            .excludedCategories(EnumSet.of(TestCategory.SPEC_CORE))
            .build();
    assertThat(exclusionFilter.matches(testCase)).isFalse();
  }
}
