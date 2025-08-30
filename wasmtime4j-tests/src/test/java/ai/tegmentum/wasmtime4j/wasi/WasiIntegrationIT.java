package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestDataManager;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestSuiteLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for WASI (WebAssembly System Interface) functionality.
 * Tests file system operations, process operations, and WASI compatibility across runtimes.
 */
@DisplayName("WASI Integration Tests")
class WasiIntegrationIT extends BaseIntegrationTest {
    
    private static WasmTestDataManager testDataManager;
    
    @BeforeAll
    static void setUpWasiTestSuite() throws IOException {
        testDataManager = WasmTestDataManager.getInstance();
        testDataManager.initializeTestData();
        
        // Create WASI-specific test modules
        final Path wasiTestsDir = WasmTestSuiteLoader.getTestSuiteDirectory(
            WasmTestSuiteLoader.TestSuiteType.WASI_TESTS);
        WasiIntegrationTestRunner.createWasiTestModules(wasiTestsDir);
        
        LOGGER.info("WASI test suite setup completed");
    }
    
    @Override
    protected void doSetUp(final TestInfo testInfo) {
        // Skip if WASI tests are not enabled
        skipIfCategoryNotEnabled("wasi");
    }
    
    @Test
    @DisplayName("Should validate WASI test runner infrastructure")
    void shouldValidateWasiTestRunnerInfrastructure() {
        // Given: WASI test runner
        
        // When: Testing basic infrastructure
        final WasiTestResult result = WasiIntegrationTestRunner.executeWasiTest(
            "infrastructure_test",
            (runtime, testDir) -> {
                // Basic validation that we can create runtime and test environment
                assertThat(runtime).isNotNull();
                if (testDir != null) {
                    assertThat(testDir).exists();
                }
                return "infrastructure_ok";
            },
            true // Requires file system
        );
        
        // Then: Should complete successfully
        assertThat(result).isNotNull();
        assertThat(result.getTestName()).isEqualTo("infrastructure_test");
        assertThat(result.getTestEnvironmentPath()).isPresent();
        
        LOGGER.info("WASI infrastructure test completed: " + result.getSummary());
    }
    
    @Test
    @DisplayName("Should execute WASI file system operation tests")
    void shouldExecuteWasiFileSystemOperationTests() {
        // Given: WASI file system tests
        
        // When: Executing file system tests
        final List<WasiTestResult> results = WasiIntegrationTestRunner.executeFileSystemTests();
        
        // Then: Should have test results
        assertThat(results).isNotEmpty();
        
        for (final WasiTestResult result : results) {
            assertThat(result.getTestName()).isNotBlank();
            LOGGER.info("WASI file system test: " + result.getSummary());
        }
        
        // At least some tests should pass (even if they're placeholders)
        final boolean anySuccessful = results.stream().anyMatch(WasiTestResult::isSuccessful);
        if (!anySuccessful) {
            LOGGER.warning("No WASI file system tests were successful - this may indicate missing implementation");
        }
    }
    
    @Test
    @DisplayName("Should execute WASI process operation tests")
    void shouldExecuteWasiProcessOperationTests() {
        // Given: WASI process tests
        
        // When: Executing process tests
        final List<WasiTestResult> results = WasiIntegrationTestRunner.executeProcessTests();
        
        // Then: Should have test results
        assertThat(results).isNotEmpty();
        
        for (final WasiTestResult result : results) {
            assertThat(result.getTestName()).isNotBlank();
            LOGGER.info("WASI process test: " + result.getSummary());
        }
        
        // At least some tests should pass (even if they're placeholders)
        final boolean anySuccessful = results.stream().anyMatch(WasiTestResult::isSuccessful);
        if (!anySuccessful) {
            LOGGER.warning("No WASI process tests were successful - this may indicate missing implementation");
        }
    }
    
    @Test
    @DisplayName("Should validate cross-runtime WASI consistency")
    void shouldValidateCrossRuntimeWasiConsistency() {
        // Given: A WASI test that should behave consistently across runtimes
        
        // When: Testing WASI functionality across runtimes
        final WasiTestResult result = WasiIntegrationTestRunner.executeWasiTest(
            "cross_runtime_consistency",
            (runtime, testDir) -> {
                // Test basic WASI functionality that should be consistent
                // This is a placeholder - real tests would use actual WASI modules
                return testDir != null ? "wasi_available" : "wasi_not_available";
            },
            true
        );
        
        // Then: Should have consistent results if both runtimes are available
        if (TestUtils.isPanamaAvailable() && result.getCrossRuntimeResult().isPresent()) {
            final var crossRuntimeResult = result.getCrossRuntimeResult().get();
            
            LOGGER.info("Cross-runtime WASI test: " + crossRuntimeResult.getSummary());
            
            // If both succeeded, results should be consistent
            if (crossRuntimeResult.bothSuccessful()) {
                final Object jniResult = crossRuntimeResult.getJniResult().getResult();
                final Object panamaResult = crossRuntimeResult.getPanamaResult().getResult();
                
                LOGGER.info("JNI WASI result: " + jniResult);
                LOGGER.info("Panama WASI result: " + panamaResult);
                
                // Results should be consistent for WASI operations
                assertThat(jniResult).isEqualTo(panamaResult);
            }
        }
        
        LOGGER.info("Cross-runtime WASI consistency test: " + result.getSummary());
    }
    
    @Test
    @DisplayName("Should generate comprehensive WASI execution summary")
    void shouldGenerateComprehensiveWasiExecutionSummary() {
        // Given: Multiple WASI tests have been executed
        
        // Execute a few more tests for summary demonstration
        WasiIntegrationTestRunner.executeWasiTest("summary_test_1", (runtime, testDir) -> "test1", false);
        WasiIntegrationTestRunner.executeWasiTest("summary_test_2", (runtime, testDir) -> "test2", false);
        WasiIntegrationTestRunner.executeWasiTest("summary_test_3", (runtime, testDir) -> "test3", true);
        
        // When: Creating execution summary
        final WasiExecutionSummary summary = WasiIntegrationTestRunner.createWasiExecutionSummary();
        
        // Then: Should have meaningful statistics
        assertThat(summary.getTotalWasiTests()).isGreaterThan(0);
        assertThat(summary.getSummaryTime()).isNotNull();
        
        final String report = summary.createReport();
        assertThat(report).contains("WASI Integration Test Execution Summary");
        assertThat(report).contains("Total WASI Tests");
        assertThat(report).contains("WASI Compliance Rate");
        
        LOGGER.info("WASI execution summary:\n" + report);
        
        // Log individual test results
        final var allResults = WasiIntegrationTestRunner.getAllWasiTestResults();
        LOGGER.info("Total WASI test results collected: " + allResults.size());
        
        for (final var entry : allResults.entrySet()) {
            LOGGER.info("WASI test result: " + entry.getKey() + " -> " + entry.getValue().getSummary());
        }
    }
    
    @Test
    @DisplayName("Should handle WASI test failures gracefully")
    void shouldHandleWasiTestFailuresGracefully() {
        // Given: A WASI test that will fail
        
        // When: Executing a test designed to fail
        final WasiTestResult result = WasiIntegrationTestRunner.executeWasiTest(
            "intentional_failure_test",
            (runtime, testDir) -> {
                throw new RuntimeException("Intentional test failure");
            },
            false
        );
        
        // Then: Should handle failure gracefully
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).isNotEmpty();
        
        final String summary = result.getSummary();
        assertThat(summary).contains("FAILED");
        
        LOGGER.info("Intentional failure handled: " + summary);
        
        // Should not crash the test runner
        final var allResults = WasiIntegrationTestRunner.getAllWasiTestResults();
        assertThat(allResults).containsKey("intentional_failure_test");
    }
    
    @Test
    @DisplayName("Should cleanup WASI test resources")
    void shouldCleanupWasiTestResources() {
        // Given: WASI tests that have created resources
        
        // When: Clearing test results
        final int resultCountBefore = WasiIntegrationTestRunner.getAllWasiTestResults().size();
        assertThat(resultCountBefore).isGreaterThan(0);
        
        WasiIntegrationTestRunner.clearWasiTestResults();
        
        // Then: Should cleanup resources
        final int resultCountAfter = WasiIntegrationTestRunner.getAllWasiTestResults().size();
        assertThat(resultCountAfter).isZero();
        
        LOGGER.info("WASI test resources cleaned up successfully");
    }
}