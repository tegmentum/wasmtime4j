package ai.tegmentum.wasmtime4j.utils;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.util.logging.Logger;

/**
 * Utility class for running Wasmtime4j test suites programmatically.
 * Provides methods to execute specific test categories and generate reports.
 */
public final class TestRunner {
    private static final Logger LOGGER = Logger.getLogger(TestRunner.class.getName());
    
    private TestRunner() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Runs all integration tests for a specific category.
     *
     * @param category the test category to run
     * @return the test execution summary
     */
    public static TestExecutionSummary runTestCategory(final String category) {
        LOGGER.info("Running test category: " + category);
        
        // Set system property to enable the category
        System.setProperty("wasmtime4j.test." + category + ".enabled", "true");
        
        try {
            final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectPackage("ai.tegmentum.wasmtime4j"))
                .build();
            
            final Launcher launcher = LauncherFactory.create();
            final SummaryGeneratingListener listener = new SummaryGeneratingListener();
            
            launcher.registerTestExecutionListeners(listener);
            launcher.execute(request);
            
            final TestExecutionSummary summary = listener.getSummary();
            logTestSummary(category, summary);
            
            return summary;
        } finally {
            // Clean up system property
            System.clearProperty("wasmtime4j.test." + category + ".enabled");
        }
    }
    
    /**
     * Runs all Wasmtime4j integration tests.
     *
     * @return the test execution summary
     */
    public static TestExecutionSummary runAllTests() {
        LOGGER.info("Running all Wasmtime4j integration tests");
        
        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectPackage("ai.tegmentum.wasmtime4j"))
            .build();
        
        final Launcher launcher = LauncherFactory.create();
        final SummaryGeneratingListener listener = new SummaryGeneratingListener();
        
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);
        
        final TestExecutionSummary summary = listener.getSummary();
        logTestSummary("all", summary);
        
        return summary;
    }
    
    /**
     * Runs tests for the current platform only.
     *
     * @return the test execution summary
     */
    public static TestExecutionSummary runPlatformTests() {
        LOGGER.info("Running platform-specific tests for: " + 
                   TestUtils.getOperatingSystem() + " on " + TestUtils.getSystemArchitecture());
        
        return runTestCategory(TestCategories.PLATFORM);
    }
    
    /**
     * Runs runtime selection tests.
     *
     * @return the test execution summary
     */
    public static TestExecutionSummary runRuntimeTests() {
        LOGGER.info("Running runtime selection tests");
        return runTestCategory(TestCategories.RUNTIME);
    }
    
    /**
     * Runs WebAssembly test suite.
     *
     * @return the test execution summary
     */
    public static TestExecutionSummary runWebAssemblyTests() {
        LOGGER.info("Running WebAssembly test suite");
        return runTestCategory(TestCategories.WASM_SUITE);
    }
    
    /**
     * Runs native library tests.
     *
     * @return the test execution summary
     */
    public static TestExecutionSummary runNativeTests() {
        LOGGER.info("Running native library tests");
        return runTestCategory(TestCategories.NATIVE);
    }
    
    /**
     * Logs a test execution summary.
     *
     * @param category the test category
     * @param summary the execution summary
     */
    private static void logTestSummary(final String category, final TestExecutionSummary summary) {
        LOGGER.info("Test summary for category '" + category + "':");
        LOGGER.info("  Tests found: " + summary.getTestsFoundCount());
        LOGGER.info("  Tests started: " + summary.getTestsStartedCount());
        LOGGER.info("  Tests successful: " + summary.getTestsSucceededCount());
        LOGGER.info("  Tests failed: " + summary.getTestsFailedCount());
        LOGGER.info("  Tests skipped: " + summary.getTestsSkippedCount());
        LOGGER.info("  Tests aborted: " + summary.getTestsAbortedCount());
        
        if (summary.getTestsFailedCount() > 0) {
            LOGGER.severe("Failed tests:");
            summary.getFailures().forEach(failure -> {
                LOGGER.severe("  - " + failure.getTestIdentifier().getDisplayName() + 
                             ": " + failure.getException().getMessage());
            });
        }
    }
    
    /**
     * Main method to run tests from command line.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        if (args.length == 0) {
            runAllTests();
        } else {
            final String category = args[0];
            runTestCategory(category);
        }
    }
}