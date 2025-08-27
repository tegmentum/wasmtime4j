package ai.tegmentum.wasmtime4j.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Base class for integration tests providing common setup and teardown functionality.
 */
@Timeout(value = 5, unit = TimeUnit.MINUTES)
public abstract class BaseIntegrationTest {
    protected static final Logger LOGGER = Logger.getLogger(BaseIntegrationTest.class.getName());
    
    protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(TestUtils.getTestTimeoutSeconds());
    
    /**
     * Setup method executed before each test.
     *
     * @param testInfo information about the current test
     */
    @BeforeEach
    void setUp(final TestInfo testInfo) {
        LOGGER.info("Starting test: " + testInfo.getDisplayName());
        
        // Log system information for debugging
        LOGGER.info("Java version: " + TestUtils.getJavaVersion());
        LOGGER.info("Operating System: " + TestUtils.getOperatingSystem());
        LOGGER.info("Architecture: " + TestUtils.getSystemArchitecture());
        LOGGER.info("Panama available: " + TestUtils.isPanamaAvailable());
        
        // Perform any additional setup
        doSetUp(testInfo);
    }
    
    /**
     * Teardown method executed after each test.
     *
     * @param testInfo information about the current test
     */
    @AfterEach
    void tearDown(final TestInfo testInfo) {
        try {
            // Perform cleanup
            doTearDown(testInfo);
        } finally {
            LOGGER.info("Completed test: " + testInfo.getDisplayName());
        }
    }
    
    /**
     * Hook method for subclass-specific setup.
     *
     * @param testInfo information about the current test
     */
    protected void doSetUp(final TestInfo testInfo) {
        // Override in subclasses if needed
    }
    
    /**
     * Hook method for subclass-specific teardown.
     *
     * @param testInfo information about the current test
     */
    protected void doTearDown(final TestInfo testInfo) {
        // Override in subclasses if needed
    }
    
    /**
     * Skips the current test if the specified condition is not met.
     *
     * @param condition the condition that must be true for the test to run
     * @param message the message to display when skipping the test
     */
    protected void skipIfNot(final boolean condition, final String message) {
        org.junit.jupiter.api.Assumptions.assumeTrue(condition, message);
    }
    
    /**
     * Skips the current test if running on Windows.
     */
    protected void skipOnWindows() {
        skipIfNot(!TestUtils.isWindows(), "Test skipped on Windows platform");
    }
    
    /**
     * Skips the current test if running on Linux.
     */
    protected void skipOnLinux() {
        skipIfNot(!TestUtils.isLinux(), "Test skipped on Linux platform");
    }
    
    /**
     * Skips the current test if running on macOS.
     */
    protected void skipOnMacOS() {
        skipIfNot(!TestUtils.isMacOS(), "Test skipped on macOS platform");
    }
    
    /**
     * Skips the current test if Panama is not available.
     */
    protected void skipIfPanamaNotAvailable() {
        skipIfNot(TestUtils.isPanamaAvailable(), "Test requires Panama FFI (Java 23+)");
    }
    
    /**
     * Skips the current test if running on ARM64 architecture.
     */
    protected void skipOnARM64() {
        skipIfNot(!TestUtils.isARM64(), "Test skipped on ARM64 architecture");
    }
    
    /**
     * Skips the current test if running on x86_64 architecture.
     */
    protected void skipOnX86_64() {
        skipIfNot(!TestUtils.isX86_64(), "Test skipped on x86_64 architecture");
    }
    
    /**
     * Skips the current test if the specified test category is not enabled.
     *
     * @param category the test category to check
     */
    protected void skipIfCategoryNotEnabled(final String category) {
        skipIfNot(TestUtils.isTestCategoryEnabled(category), 
                 "Test category '" + category + "' is not enabled");
    }
}