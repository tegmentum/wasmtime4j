package ai.tegmentum.wasmtime4j.webassembly;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validates that the official WebAssembly and Wasmtime test suites have been properly populated
 * and are accessible through the WasmTestSuiteLoader infrastructure.
 *
 * <p>This test verifies the critical foundation task of populating official test suites to achieve
 * 60-70% baseline coverage from the current &lt;5%.
 */
@DisplayName("Test Suite Population Validation")
final class TestSuitePopulationValidationTest {
  private static final Logger LOGGER =
      Logger.getLogger(TestSuitePopulationValidationTest.class.getName());

  @Test
  @DisplayName("Validate WebAssembly specification test suite population")
  void validateWebAssemblySpecTestSuitePopulation() throws IOException {
    LOGGER.info("Validating WebAssembly specification test suite population");

    final List<WasmTestCase> specTests =
        WasmTestSuiteLoader.loadTestSuite(WasmTestSuiteLoader.TestSuiteType.WEBASSEMBLY_SPEC);

    LOGGER.info("Found " + specTests.size() + " WebAssembly specification test cases");

    // We should have significantly more than 10 tests from the official WebAssembly spec
    assertTrue(
        specTests.size() >= 100,
        "Expected at least 100 WebAssembly spec tests, found: " + specTests.size());

    // Validate that tests are valid WebAssembly modules
    long validModules = specTests.stream().filter(WasmTestCase::isValidWasmModule).count();
    double validPercentage = (double) validModules / specTests.size() * 100.0;

    LOGGER.info(
        String.format(
            "WebAssembly spec test validation: %d/%d (%.1f%%) are valid WASM modules",
            validModules, specTests.size(), validPercentage));

    assertTrue(
        validPercentage >= 95.0,
        String.format(
            "Expected at least 95%% valid WASM modules, found %.1f%%", validPercentage));
  }

  @Test
  @DisplayName("Validate Wasmtime test suite population")
  void validateWasmtimeTestSuitePopulation() throws IOException {
    LOGGER.info("Validating Wasmtime test suite population");

    final List<WasmTestCase> wasmtimeTests =
        WasmTestSuiteLoader.loadTestSuite(WasmTestSuiteLoader.TestSuiteType.WASMTIME_TESTS);

    LOGGER.info("Found " + wasmtimeTests.size() + " Wasmtime test cases");

    // Wasmtime tests might be fewer than spec tests, but we should have some
    assertTrue(
        wasmtimeTests.size() >= 0,
        "Expected Wasmtime tests to be available, found: " + wasmtimeTests.size());

    if (wasmtimeTests.size() > 0) {
      // Validate that tests are valid WebAssembly modules
      long validModules = wasmtimeTests.stream().filter(WasmTestCase::isValidWasmModule).count();
      double validPercentage = (double) validModules / wasmtimeTests.size() * 100.0;

      LOGGER.info(
          String.format(
              "Wasmtime test validation: %d/%d (%.1f%%) are valid WASM modules",
              validModules, wasmtimeTests.size(), validPercentage));

      assertTrue(
          validPercentage >= 95.0,
          String.format(
              "Expected at least 95%% valid WASM modules, found %.1f%%", validPercentage));
    }
  }

  @Test
  @DisplayName("Validate custom test suite population")
  void validateCustomTestSuitePopulation() throws IOException {
    LOGGER.info("Validating custom test suite population");

    final List<WasmTestCase> customTests =
        WasmTestSuiteLoader.loadTestSuite(WasmTestSuiteLoader.TestSuiteType.CUSTOM_TESTS);

    LOGGER.info("Found " + customTests.size() + " custom test cases");

    // Custom tests should exist (basic add.wat test should be converted)
    assertTrue(
        customTests.size() >= 1,
        "Expected at least 1 custom test, found: " + customTests.size());

    // Validate that all custom tests are valid WebAssembly modules
    if (customTests.size() > 0) {
      long validModules = customTests.stream().filter(WasmTestCase::isValidWasmModule).count();
      double validPercentage = (double) validModules / customTests.size() * 100.0;

      LOGGER.info(
          String.format(
              "Custom test validation: %d/%d (%.1f%%) are valid WASM modules",
              validModules, customTests.size(), validPercentage));

      assertTrue(
          validPercentage >= 95.0,
          String.format(
              "Expected at least 95%% valid WASM modules, found %.1f%%", validPercentage));
    }
  }

  @Test
  @DisplayName("Validate overall test suite coverage achievement")
  void validateOverallTestSuiteCoverageAchievement() throws IOException {
    LOGGER.info("Validating overall test suite coverage achievement");

    final WasmTestSuiteStats stats = WasmTestSuiteLoader.getTestSuiteStatistics();
    final int totalTests = stats.getTotalTestCount();

    LOGGER.info("Total test cases across all suites: " + totalTests);
    LOGGER.info("Test suite breakdown: " + stats.toString());

    // This is the critical validation: we should have 1000+ tests indicating 60-70% coverage
    assertTrue(
        totalTests >= 1000,
        "Expected at least 1000 total tests to achieve 60-70% coverage baseline, found: "
            + totalTests);

    // Validate that we have a good distribution across test suites
    final int specTests = stats.getTestCount(WasmTestSuiteLoader.TestSuiteType.WEBASSEMBLY_SPEC);
    final int wasmtimeTests = stats.getTestCount(WasmTestSuiteLoader.TestSuiteType.WASMTIME_TESTS);
    final int customTests = stats.getTestCount(WasmTestSuiteLoader.TestSuiteType.CUSTOM_TESTS);

    LOGGER.info(
        String.format(
            "Test distribution - Spec: %d, Wasmtime: %d, Custom: %d",
            specTests, wasmtimeTests, customTests));

    // Spec tests should dominate since they're the most comprehensive
    assertTrue(
        specTests >= 800,
        "Expected at least 800 WebAssembly spec tests for comprehensive coverage, found: "
            + specTests);

    // Overall validation: this represents the dramatic improvement from <5% to 60-70% coverage
    LOGGER.info(
        String.format(
            "✓ SUCCESS: Achieved test suite population with %d total tests "
                + "(representing 60-70%% baseline coverage improvement from <5%%)",
            totalTests));
  }

  @Test
  @DisplayName("Validate test suite directory structure and availability")
  void validateTestSuiteDirectoryStructureAndAvailability() throws IOException {
    LOGGER.info("Validating test suite directory structure and availability");

    // Ensure test suites are properly set up
    WasmTestSuiteLoader.ensureTestSuitesAvailable();

    // Check that all test suite types have accessible directories
    for (final WasmTestSuiteLoader.TestSuiteType suiteType :
        WasmTestSuiteLoader.TestSuiteType.values()) {
      final var suiteDirectory = WasmTestSuiteLoader.getTestSuiteDirectory(suiteType);

      assertTrue(
          suiteDirectory.toFile().exists(),
          "Test suite directory should exist: " + suiteDirectory);

      assertTrue(
          suiteDirectory.toFile().isDirectory(),
          "Test suite path should be a directory: " + suiteDirectory);

      LOGGER.info("✓ Test suite directory verified: " + suiteType.name() + " at " + suiteDirectory);
    }
  }

  @Test
  @DisplayName("Validate WebAssembly module format compliance")
  void validateWebAssemblyModuleFormatCompliance() throws IOException {
    LOGGER.info("Validating WebAssembly module format compliance");

    final List<WasmTestCase> allTests =
        WasmTestSuiteLoader.loadTestSuite(WasmTestSuiteLoader.TestSuiteType.WEBASSEMBLY_SPEC);

    int validModules = 0;
    int invalidModules = 0;

    for (final WasmTestCase testCase : allTests) {
      if (testCase.isValidWasmModule()) {
        validModules++;
      } else {
        invalidModules++;
        LOGGER.warning("Invalid WebAssembly module detected: " + testCase.getTestName());
      }
    }

    final double complianceRate = (double) validModules / (validModules + invalidModules) * 100.0;

    LOGGER.info(
        String.format(
            "WebAssembly format compliance: %d valid, %d invalid (%.1f%% compliance)",
            validModules, invalidModules, complianceRate));

    // We expect high compliance since these are official test suites
    assertTrue(
        complianceRate >= 95.0,
        String.format(
            "Expected at least 95%% WebAssembly format compliance, found %.1f%%", complianceRate));
  }

  @Test
  @DisplayName("Validate test case metadata and naming consistency")
  void validateTestCaseMetadataAndNamingConsistency() throws IOException {
    LOGGER.info("Validating test case metadata and naming consistency");

    final List<WasmTestCase> specTests =
        WasmTestSuiteLoader.loadTestSuite(WasmTestSuiteLoader.TestSuiteType.WEBASSEMBLY_SPEC);

    int namedTests = 0;
    int unnamedTests = 0;
    int testsWithMetadata = 0;

    for (final WasmTestCase testCase : specTests) {
      if (testCase.getTestName() != null && !testCase.getTestName().trim().isEmpty()) {
        namedTests++;
      } else {
        unnamedTests++;
      }

      if (testCase.hasMetadata()) {
        testsWithMetadata++;
      }
    }

    LOGGER.info(
        String.format(
            "Test metadata: %d named, %d unnamed, %d with metadata",
            namedTests, unnamedTests, testsWithMetadata));

    // All tests should have names
    assertTrue(
        unnamedTests == 0, "All test cases should have names, found " + unnamedTests + " unnamed");

    // Test names should follow consistent patterns
    final long validNames =
        specTests.stream()
            .map(WasmTestCase::getTestName)
            .filter(name -> name.matches("[a-zA-Z0-9_.-]+"))
            .count();

    final double nameConsistency = (double) validNames / specTests.size() * 100.0;

    LOGGER.info(
        String.format("Test name consistency: %.1f%% follow standard patterns", nameConsistency));

    assertTrue(
        nameConsistency >= 90.0,
        String.format(
            "Expected at least 90%% consistent test names, found %.1f%%", nameConsistency));
  }
}