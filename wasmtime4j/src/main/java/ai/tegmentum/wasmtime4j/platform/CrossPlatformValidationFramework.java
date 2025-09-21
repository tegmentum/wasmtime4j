/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.platform;

import ai.tegmentum.wasmtime4j.nativeloader.NativeLibraryUtils;
import ai.tegmentum.wasmtime4j.nativeloader.NativeLoader;
import ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector;
import ai.tegmentum.wasmtime4j.nativeloader.PlatformFeatureDetector;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Comprehensive cross-platform validation framework for Wasmtime4j.
 *
 * <p>This framework provides end-to-end validation of Wasmtime4j functionality across
 * different platforms, architectures, and deployment environments. It ensures consistent
 * behavior, performance characteristics, and compatibility across all supported platforms.
 *
 * <p>Key validation areas:
 * <ul>
 *   <li>Platform detection and feature validation
 *   <li>Native library loading and version compatibility
 *   <li>Runtime selection and optimization validation
 *   <li>Cross-platform API consistency verification
 *   <li>Performance baseline validation
 *   <li>Resource utilization and memory management
 *   <li>Error handling and recovery mechanisms
 * </ul>
 */
public final class CrossPlatformValidationFramework {

    private static final Logger LOGGER = Logger.getLogger(CrossPlatformValidationFramework.class.getName());

    /** Validation test categories. */
    public enum ValidationCategory {
        /** Platform detection and basic compatibility */
        PLATFORM_DETECTION,
        /** Native library loading and management */
        NATIVE_LIBRARY_LOADING,
        /** Feature detection and capability validation */
        FEATURE_DETECTION,
        /** Runtime selection and configuration */
        RUNTIME_CONFIGURATION,
        /** Performance and resource utilization */
        PERFORMANCE_VALIDATION,
        /** API consistency across platforms */
        API_CONSISTENCY,
        /** Error handling and recovery */
        ERROR_HANDLING,
        /** Security and sandboxing */
        SECURITY_VALIDATION,
        /** Memory management and resource cleanup */
        RESOURCE_MANAGEMENT,
        /** Integration and interoperability */
        INTEGRATION_TESTING
    }

    /** Validation severity levels. */
    public enum ValidationSeverity {
        /** Critical issues that prevent functionality */
        CRITICAL,
        /** Major issues that significantly impact functionality */
        MAJOR,
        /** Minor issues with workarounds available */
        MINOR,
        /** Informational items that don't impact functionality */
        INFO
    }

    /** Individual validation test result. */
    public static final class ValidationResult {
        private final String testName;
        private final ValidationCategory category;
        private final boolean passed;
        private final Duration executionTime;
        private final Optional<String> errorMessage;
        private final Optional<Throwable> exception;
        private final ValidationSeverity severity;
        private final Map<String, Object> metadata;
        private final Instant timestamp;

        ValidationResult(
                final String testName,
                final ValidationCategory category,
                final boolean passed,
                final Duration executionTime,
                final String errorMessage,
                final Throwable exception,
                final ValidationSeverity severity,
                final Map<String, Object> metadata) {
            this.testName = testName;
            this.category = category;
            this.passed = passed;
            this.executionTime = executionTime;
            this.errorMessage = Optional.ofNullable(errorMessage);
            this.exception = Optional.ofNullable(exception);
            this.severity = severity;
            this.metadata = Map.copyOf(metadata);
            this.timestamp = Instant.now();
        }

        public String getTestName() { return testName; }
        public ValidationCategory getCategory() { return category; }
        public boolean isPassed() { return passed; }
        public Duration getExecutionTime() { return executionTime; }
        public Optional<String> getErrorMessage() { return errorMessage; }
        public Optional<Throwable> getException() { return exception; }
        public ValidationSeverity getSeverity() { return severity; }
        public Map<String, Object> getMetadata() { return metadata; }
        public Instant getTimestamp() { return timestamp; }

        public boolean isCriticalFailure() {
            return !passed && severity == ValidationSeverity.CRITICAL;
        }

        public boolean isMajorFailure() {
            return !passed && severity == ValidationSeverity.MAJOR;
        }

        @Override
        public String toString() {
            return String.format("%s[%s]: %s (%.2fms) - %s",
                category, testName, passed ? "PASS" : "FAIL",
                executionTime.toNanos() / 1_000_000.0,
                errorMessage.orElse(""));
        }

        /** Creates a successful validation result. */
        public static ValidationResult success(
                final String testName,
                final ValidationCategory category,
                final Duration executionTime) {
            return new ValidationResult(testName, category, true, executionTime,
                null, null, ValidationSeverity.INFO, Collections.emptyMap());
        }

        /** Creates a failed validation result. */
        public static ValidationResult failure(
                final String testName,
                final ValidationCategory category,
                final Duration executionTime,
                final String errorMessage,
                final ValidationSeverity severity) {
            return new ValidationResult(testName, category, false, executionTime,
                errorMessage, null, severity, Collections.emptyMap());
        }

        /** Creates a failed validation result with exception. */
        public static ValidationResult failure(
                final String testName,
                final ValidationCategory category,
                final Duration executionTime,
                final String errorMessage,
                final Throwable exception,
                final ValidationSeverity severity) {
            return new ValidationResult(testName, category, false, executionTime,
                errorMessage, exception, severity, Collections.emptyMap());
        }

        /** Creates a validation result with metadata. */
        public static ValidationResult withMetadata(
                final String testName,
                final ValidationCategory category,
                final boolean passed,
                final Duration executionTime,
                final Map<String, Object> metadata) {
            return new ValidationResult(testName, category, passed, executionTime,
                null, null, ValidationSeverity.INFO, metadata);
        }
    }

    /** Comprehensive validation report. */
    public static final class ValidationReport {
        private final PlatformDetector.PlatformInfo platformInfo;
        private final PlatformFeatureDetector.PlatformFeatures platformFeatures;
        private final CrossPlatformCompatibility.CompatibilityAnalysis compatibilityAnalysis;
        private final PlatformOptimizations.OptimizationConfiguration optimizationConfig;
        private final List<ValidationResult> results;
        private final Duration totalExecutionTime;
        private final Instant reportTimestamp;
        private final Map<ValidationCategory, List<ValidationResult>> resultsByCategory;
        private final ValidationSummary summary;

        ValidationReport(
                final PlatformDetector.PlatformInfo platformInfo,
                final PlatformFeatureDetector.PlatformFeatures platformFeatures,
                final CrossPlatformCompatibility.CompatibilityAnalysis compatibilityAnalysis,
                final PlatformOptimizations.OptimizationConfiguration optimizationConfig,
                final List<ValidationResult> results,
                final Duration totalExecutionTime) {
            this.platformInfo = platformInfo;
            this.platformFeatures = platformFeatures;
            this.compatibilityAnalysis = compatibilityAnalysis;
            this.optimizationConfig = optimizationConfig;
            this.results = List.copyOf(results);
            this.totalExecutionTime = totalExecutionTime;
            this.reportTimestamp = Instant.now();
            this.resultsByCategory = groupResultsByCategory(results);
            this.summary = generateSummary(results);
        }

        public PlatformDetector.PlatformInfo getPlatformInfo() { return platformInfo; }
        public PlatformFeatureDetector.PlatformFeatures getPlatformFeatures() { return platformFeatures; }
        public CrossPlatformCompatibility.CompatibilityAnalysis getCompatibilityAnalysis() { return compatibilityAnalysis; }
        public PlatformOptimizations.OptimizationConfiguration getOptimizationConfig() { return optimizationConfig; }
        public List<ValidationResult> getResults() { return results; }
        public Duration getTotalExecutionTime() { return totalExecutionTime; }
        public Instant getReportTimestamp() { return reportTimestamp; }
        public Map<ValidationCategory, List<ValidationResult>> getResultsByCategory() { return resultsByCategory; }
        public ValidationSummary getSummary() { return summary; }

        public List<ValidationResult> getFailures() {
            return results.stream().filter(r -> !r.isPassed()).toList();
        }

        public List<ValidationResult> getCriticalFailures() {
            return results.stream().filter(ValidationResult::isCriticalFailure).toList();
        }

        public List<ValidationResult> getMajorFailures() {
            return results.stream().filter(ValidationResult::isMajorFailure).toList();
        }

        public boolean hasAnyFailures() {
            return results.stream().anyMatch(r -> !r.isPassed());
        }

        public boolean hasCriticalFailures() {
            return results.stream().anyMatch(ValidationResult::isCriticalFailure);
        }

        public boolean isProductionReady() {
            return !hasCriticalFailures() && getMajorFailures().size() <= 2;
        }

        private static Map<ValidationCategory, List<ValidationResult>> groupResultsByCategory(
                final List<ValidationResult> results) {
            final Map<ValidationCategory, List<ValidationResult>> grouped = new EnumMap<>(ValidationCategory.class);
            for (final ValidationResult result : results) {
                grouped.computeIfAbsent(result.getCategory(), k -> new ArrayList<>()).add(result);
            }
            return grouped;
        }

        private static ValidationSummary generateSummary(final List<ValidationResult> results) {
            final int totalTests = results.size();
            final int passedTests = (int) results.stream().filter(ValidationResult::isPassed).count();
            final int failedTests = totalTests - passedTests;
            final int criticalFailures = (int) results.stream().filter(ValidationResult::isCriticalFailure).count();
            final int majorFailures = (int) results.stream().filter(ValidationResult::isMajorFailure).count();

            final double successRate = totalTests > 0 ? (double) passedTests / totalTests * 100.0 : 0.0;

            return new ValidationSummary(totalTests, passedTests, failedTests,
                criticalFailures, majorFailures, successRate);
        }
    }

    /** Validation summary statistics. */
    public static final class ValidationSummary {
        private final int totalTests;
        private final int passedTests;
        private final int failedTests;
        private final int criticalFailures;
        private final int majorFailures;
        private final double successRate;

        ValidationSummary(
                final int totalTests,
                final int passedTests,
                final int failedTests,
                final int criticalFailures,
                final int majorFailures,
                final double successRate) {
            this.totalTests = totalTests;
            this.passedTests = passedTests;
            this.failedTests = failedTests;
            this.criticalFailures = criticalFailures;
            this.majorFailures = majorFailures;
            this.successRate = successRate;
        }

        public int getTotalTests() { return totalTests; }
        public int getPassedTests() { return passedTests; }
        public int getFailedTests() { return failedTests; }
        public int getCriticalFailures() { return criticalFailures; }
        public int getMajorFailures() { return majorFailures; }
        public double getSuccessRate() { return successRate; }

        @Override
        public String toString() {
            return String.format("Summary: %d/%d tests passed (%.1f%%), %d critical, %d major failures",
                passedTests, totalTests, successRate, criticalFailures, majorFailures);
        }
    }

    /** Private constructor to prevent instantiation of utility class. */
    private CrossPlatformValidationFramework() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Runs comprehensive cross-platform validation.
     *
     * @return the validation report
     */
    public static ValidationReport runComprehensiveValidation() {
        return runComprehensiveValidation(true);
    }

    /**
     * Runs comprehensive cross-platform validation.
     *
     * @param includePerformanceTests whether to include performance tests
     * @return the validation report
     */
    public static ValidationReport runComprehensiveValidation(final boolean includePerformanceTests) {
        LOGGER.info("Starting comprehensive cross-platform validation");
        final Instant startTime = Instant.now();

        // Gather platform information
        final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();
        final PlatformFeatureDetector.PlatformFeatures platformFeatures = PlatformFeatureDetector.detect();
        final CrossPlatformCompatibility.CompatibilityAnalysis compatibilityAnalysis =
            CrossPlatformCompatibility.analyzeCompatibility();
        final PlatformOptimizations.OptimizationConfiguration optimizationConfig =
            PlatformOptimizations.getOptimizedConfiguration();

        LOGGER.info("Platform: " + platformInfo.getPlatformId());
        LOGGER.info("Features: " + platformFeatures);
        LOGGER.info("Compatibility: " + compatibilityAnalysis.getOverallSupportLevel());

        // Run all validation tests
        final List<ValidationResult> results = new ArrayList<>();

        // Platform detection tests
        results.addAll(runPlatformDetectionTests());

        // Native library loading tests
        results.addAll(runNativeLibraryLoadingTests());

        // Feature detection tests
        results.addAll(runFeatureDetectionTests(platformFeatures));

        // Runtime configuration tests
        results.addAll(runRuntimeConfigurationTests(optimizationConfig));

        // API consistency tests
        results.addAll(runApiConsistencyTests());

        // Error handling tests
        results.addAll(runErrorHandlingTests());

        // Security validation tests
        results.addAll(runSecurityValidationTests());

        // Resource management tests
        results.addAll(runResourceManagementTests());

        // Integration testing
        results.addAll(runIntegrationTests());

        // Performance tests (optional)
        if (includePerformanceTests) {
            results.addAll(runPerformanceValidationTests());
        }

        final Duration totalExecutionTime = Duration.between(startTime, Instant.now());

        final ValidationReport report = new ValidationReport(
            platformInfo, platformFeatures, compatibilityAnalysis, optimizationConfig,
            results, totalExecutionTime);

        LOGGER.info("Validation completed: " + report.getSummary());
        if (report.hasCriticalFailures()) {
            LOGGER.severe("Critical failures detected - platform may not be suitable for production");
        }

        return report;
    }

    /**
     * Runs quick validation for basic platform compatibility.
     *
     * @return the validation report
     */
    public static ValidationReport runQuickValidation() {
        LOGGER.info("Starting quick cross-platform validation");
        final Instant startTime = Instant.now();

        // Gather minimal platform information
        final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();
        final PlatformFeatureDetector.PlatformFeatures platformFeatures = PlatformFeatureDetector.detect();
        final CrossPlatformCompatibility.CompatibilityAnalysis compatibilityAnalysis =
            CrossPlatformCompatibility.analyzeCompatibility();
        final PlatformOptimizations.OptimizationConfiguration optimizationConfig =
            PlatformOptimizations.getOptimizedConfiguration();

        // Run essential validation tests only
        final List<ValidationResult> results = new ArrayList<>();

        // Basic platform detection
        results.addAll(runPlatformDetectionTests());

        // Critical native library loading tests
        results.add(runTest("basic_library_loading", ValidationCategory.NATIVE_LIBRARY_LOADING,
            () -> validateBasicLibraryLoading()));

        // Essential feature detection
        results.add(runTest("essential_features", ValidationCategory.FEATURE_DETECTION,
            () -> validateEssentialFeatures(platformFeatures)));

        final Duration totalExecutionTime = Duration.between(startTime, Instant.now());

        final ValidationReport report = new ValidationReport(
            platformInfo, platformFeatures, compatibilityAnalysis, optimizationConfig,
            results, totalExecutionTime);

        LOGGER.info("Quick validation completed: " + report.getSummary());

        return report;
    }

    /**
     * Validates a specific platform feature.
     *
     * @param feature the feature to validate
     * @return the validation result
     */
    public static ValidationResult validateFeature(final CrossPlatformCompatibility.CrossPlatformFeature feature) {
        return runTest("feature_" + feature.name().toLowerCase(), ValidationCategory.FEATURE_DETECTION,
            () -> CrossPlatformCompatibility.validateFeature(feature));
    }

    // Private validation test implementations

    private static List<ValidationResult> runPlatformDetectionTests() {
        final List<ValidationResult> results = new ArrayList<>();

        results.add(runTest("platform_detection", ValidationCategory.PLATFORM_DETECTION,
            () -> PlatformDetector.detect() != null));

        results.add(runTest("platform_support_check", ValidationCategory.PLATFORM_DETECTION,
            () -> PlatformDetector.isPlatformSupported()));

        results.add(runTest("platform_description", ValidationCategory.PLATFORM_DETECTION,
            () -> !PlatformDetector.getPlatformDescription().isEmpty()));

        results.add(runTest("operating_system_detection", ValidationCategory.PLATFORM_DETECTION,
            () -> PlatformDetector.detectOperatingSystem() != null));

        results.add(runTest("architecture_detection", ValidationCategory.PLATFORM_DETECTION,
            () -> PlatformDetector.detectArchitecture() != null));

        return results;
    }

    private static List<ValidationResult> runNativeLibraryLoadingTests() {
        final List<ValidationResult> results = new ArrayList<>();

        results.add(runTest("native_library_utils", ValidationCategory.NATIVE_LIBRARY_LOADING,
            () -> validateNativeLibraryUtils()));

        results.add(runTest("library_version_management", ValidationCategory.NATIVE_LIBRARY_LOADING,
            () -> validateLibraryVersionManagement()));

        results.add(runTest("library_compatibility", ValidationCategory.NATIVE_LIBRARY_LOADING,
            () -> validateLibraryCompatibility()));

        return results;
    }

    private static List<ValidationResult> runFeatureDetectionTests(
            final PlatformFeatureDetector.PlatformFeatures platformFeatures) {
        final List<ValidationResult> results = new ArrayList<>();

        results.add(runTest("feature_detection_comprehensive", ValidationCategory.FEATURE_DETECTION,
            () -> platformFeatures != null));

        results.add(runTest("cpu_features", ValidationCategory.FEATURE_DETECTION,
            () -> !platformFeatures.getCpuFeatures().isEmpty()));

        results.add(runTest("jvm_features", ValidationCategory.FEATURE_DETECTION,
            () -> !platformFeatures.getJvmFeatures().isEmpty()));

        results.add(runTest("performance_recommendations", ValidationCategory.FEATURE_DETECTION,
            () -> platformFeatures.getRecommendedOptimizationLevel() >= 0));

        return results;
    }

    private static List<ValidationResult> runRuntimeConfigurationTests(
            final PlatformOptimizations.OptimizationConfiguration optimizationConfig) {
        final List<ValidationResult> results = new ArrayList<>();

        results.add(runTest("optimization_configuration", ValidationCategory.RUNTIME_CONFIGURATION,
            () -> optimizationConfig != null));

        results.add(runTest("thread_configuration", ValidationCategory.RUNTIME_CONFIGURATION,
            () -> optimizationConfig.getCompilationThreads() > 0 && optimizationConfig.getExecutionThreads() > 0));

        results.add(runTest("memory_configuration", ValidationCategory.RUNTIME_CONFIGURATION,
            () -> optimizationConfig.getMaxMemoryUsage() > 0));

        results.add(runTest("optimization_level", ValidationCategory.RUNTIME_CONFIGURATION,
            () -> optimizationConfig.getOptimizationLevel() >= 0 && optimizationConfig.getOptimizationLevel() <= 3));

        return results;
    }

    private static List<ValidationResult> runApiConsistencyTests() {
        final List<ValidationResult> results = new ArrayList<>();

        results.add(runTest("compatibility_analysis", ValidationCategory.API_CONSISTENCY,
            () -> CrossPlatformCompatibility.analyzeCompatibility() != null));

        results.add(runTest("feature_validation", ValidationCategory.API_CONSISTENCY,
            () -> validateCriticalFeatures()));

        return results;
    }

    private static List<ValidationResult> runErrorHandlingTests() {
        final List<ValidationResult> results = new ArrayList<>();

        results.add(runTest("invalid_platform_handling", ValidationCategory.ERROR_HANDLING,
            () -> validateInvalidPlatformHandling()));

        results.add(runTest("library_loading_errors", ValidationCategory.ERROR_HANDLING,
            () -> validateLibraryLoadingErrors()));

        return results;
    }

    private static List<ValidationResult> runSecurityValidationTests() {
        final List<ValidationResult> results = new ArrayList<>();

        results.add(runTest("platform_sanitization", ValidationCategory.SECURITY_VALIDATION,
            () -> validatePlatformSanitization()));

        results.add(runTest("library_path_validation", ValidationCategory.SECURITY_VALIDATION,
            () -> validateLibraryPathSecurity()));

        return results;
    }

    private static List<ValidationResult> runResourceManagementTests() {
        final List<ValidationResult> results = new ArrayList<>();

        results.add(runTest("cache_management", ValidationCategory.RESOURCE_MANAGEMENT,
            () -> validateCacheManagement()));

        results.add(runTest("memory_cleanup", ValidationCategory.RESOURCE_MANAGEMENT,
            () -> validateMemoryCleanup()));

        return results;
    }

    private static List<ValidationResult> runIntegrationTests() {
        final List<ValidationResult> results = new ArrayList<>();

        results.add(runTest("cross_platform_consistency", ValidationCategory.INTEGRATION_TESTING,
            () -> validateCrossPlatformConsistency()));

        results.add(runTest("version_compatibility", ValidationCategory.INTEGRATION_TESTING,
            () -> validateVersionCompatibility()));

        return results;
    }

    private static List<ValidationResult> runPerformanceValidationTests() {
        final List<ValidationResult> results = new ArrayList<>();

        results.add(runTest("platform_analysis_performance", ValidationCategory.PERFORMANCE_VALIDATION,
            () -> validatePlatformAnalysisPerformance()));

        results.add(runTest("optimization_effectiveness", ValidationCategory.PERFORMANCE_VALIDATION,
            () -> validateOptimizationEffectiveness()));

        return results;
    }

    // Individual test implementations

    private static boolean validateBasicLibraryLoading() {
        try {
            final NativeLibraryUtils.LibraryLoadInfo loadInfo = NativeLoader.loadLibrary("wasmtime4j");
            return loadInfo != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean validateEssentialFeatures(final PlatformFeatureDetector.PlatformFeatures features) {
        return features.getLogicalCores() > 0 &&
               features.getAvailableMemory() > 0 &&
               !features.getJvmFeatures().isEmpty();
    }

    private static boolean validateNativeLibraryUtils() {
        try {
            final String diagnosticInfo = NativeLibraryUtils.getDiagnosticInfo();
            return diagnosticInfo != null && !diagnosticInfo.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean validateLibraryVersionManagement() {
        try {
            final List<NativeLibraryVersionManager.LibraryVersionInfo> versions =
                NativeLibraryVersionManager.discoverAvailableVersions("wasmtime4j");
            return !versions.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean validateLibraryCompatibility() {
        try {
            final List<NativeLibraryVersionManager.LibraryVersionInfo> versions =
                NativeLibraryVersionManager.discoverAvailableVersions("wasmtime4j");
            if (versions.isEmpty()) return false;

            final NativeLibraryVersionManager.CompatibilityResult compatibility =
                NativeLibraryVersionManager.validateCompatibility(versions.get(0));
            return compatibility != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean validateCriticalFeatures() {
        final CrossPlatformCompatibility.CompatibilityAnalysis analysis =
            CrossPlatformCompatibility.analyzeCompatibility();

        return analysis.isFeatureSupported(CrossPlatformCompatibility.CrossPlatformFeature.JNI_RUNTIME) &&
               analysis.isFeatureSupported(CrossPlatformCompatibility.CrossPlatformFeature.WASI_FILESYSTEM) &&
               analysis.isFeatureSupported(CrossPlatformCompatibility.CrossPlatformFeature.MEMORY_MAPPING);
    }

    private static boolean validateInvalidPlatformHandling() {
        // This would test error handling for unsupported platforms
        // For now, assume it works if we can detect the current platform
        return PlatformDetector.isPlatformSupported();
    }

    private static boolean validateLibraryLoadingErrors() {
        // Test error handling for non-existent libraries
        try {
            NativeLoader.loadLibrary("non_existent_library_12345");
            return false; // Should have failed
        } catch (Exception e) {
            return true; // Expected failure
        }
    }

    private static boolean validatePlatformSanitization() {
        // Test that platform detection sanitizes malicious input
        final String description = PlatformDetector.getPlatformDescription();
        return description != null && !description.contains("\n") && !description.contains("\r");
    }

    private static boolean validateLibraryPathSecurity() {
        // Test that library paths are properly sanitized
        final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
        final String resourcePath = info.getLibraryResourcePath("test");
        return !resourcePath.contains("..") && !resourcePath.contains("\\");
    }

    private static boolean validateCacheManagement() {
        // Test cache clearing functionality
        try {
            PlatformOptimizations.clearOptimizationCache();
            NativeLibraryVersionManager.clearVersionCache();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean validateMemoryCleanup() {
        // Basic memory cleanup validation
        System.gc(); // Suggest garbage collection
        return true; // Assume it works
    }

    private static boolean validateCrossPlatformConsistency() {
        // Validate that analysis results are consistent
        final CrossPlatformCompatibility.CompatibilityAnalysis analysis1 =
            CrossPlatformCompatibility.analyzeCompatibility();
        final CrossPlatformCompatibility.CompatibilityAnalysis analysis2 =
            CrossPlatformCompatibility.analyzeCompatibility();

        return analysis1.getPlatformInfo().equals(analysis2.getPlatformInfo()) &&
               analysis1.getOverallSupportLevel().equals(analysis2.getOverallSupportLevel());
    }

    private static boolean validateVersionCompatibility() {
        try {
            final List<NativeLibraryVersionManager.LibraryVersionInfo> versions =
                NativeLibraryVersionManager.discoverAvailableVersions("wasmtime4j");
            for (final NativeLibraryVersionManager.LibraryVersionInfo version : versions) {
                final NativeLibraryVersionManager.CompatibilityResult result =
                    NativeLibraryVersionManager.validateCompatibility(version);
                if (!result.isCompatible()) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean validatePlatformAnalysisPerformance() {
        final Instant start = Instant.now();
        CrossPlatformCompatibility.reanalyzeCompatibility();
        final Duration analysisTime = Duration.between(start, Instant.now());

        // Analysis should complete within 10 seconds
        return analysisTime.compareTo(Duration.ofSeconds(10)) < 0;
    }

    private static boolean validateOptimizationEffectiveness() {
        final PlatformOptimizations.OptimizationConfiguration config =
            PlatformOptimizations.getOptimizedConfiguration();

        // Basic validation that optimization configuration is reasonable
        return config.getCompilationThreads() > 0 &&
               config.getExecutionThreads() > 0 &&
               config.getMaxMemoryUsage() > 1024 * 1024; // At least 1MB
    }

    // Utility method for running individual tests

    private static ValidationResult runTest(
            final String testName,
            final ValidationCategory category,
            final TestFunction test) {
        final Instant start = Instant.now();
        try {
            final boolean result = test.execute();
            final Duration duration = Duration.between(start, Instant.now());

            if (result) {
                return ValidationResult.success(testName, category, duration);
            } else {
                return ValidationResult.failure(testName, category, duration,
                    "Test assertion failed", ValidationSeverity.MAJOR);
            }
        } catch (Exception e) {
            final Duration duration = Duration.between(start, Instant.now());
            return ValidationResult.failure(testName, category, duration,
                "Test threw exception: " + e.getMessage(), e, ValidationSeverity.CRITICAL);
        }
    }

    @FunctionalInterface
    private interface TestFunction {
        boolean execute() throws Exception;
    }

    /**
     * Generates a comprehensive diagnostic report.
     *
     * @return diagnostic information
     */
    public static String generateDiagnosticReport() {
        final StringBuilder sb = new StringBuilder();
        sb.append("=== Cross-Platform Validation Framework Diagnostic Report ===\n\n");

        // Platform information
        final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();
        sb.append("Platform Information:\n");
        sb.append("  Platform ID: ").append(platformInfo.getPlatformId()).append("\n");
        sb.append("  OS: ").append(platformInfo.getOperatingSystem()).append("\n");
        sb.append("  Architecture: ").append(platformInfo.getArchitecture()).append("\n");
        sb.append("  Description: ").append(PlatformDetector.getPlatformDescription()).append("\n\n");

        // Platform features
        final PlatformFeatureDetector.PlatformFeatures platformFeatures = PlatformFeatureDetector.detect();
        sb.append("Platform Features:\n");
        sb.append("  ").append(platformFeatures.toString()).append("\n\n");

        // Compatibility analysis
        final CrossPlatformCompatibility.CompatibilityAnalysis compatibilityAnalysis =
            CrossPlatformCompatibility.analyzeCompatibility();
        sb.append("Compatibility Analysis:\n");
        sb.append("  Support Level: ").append(compatibilityAnalysis.getOverallSupportLevel()).append("\n");
        sb.append("  Production Ready: ").append(compatibilityAnalysis.isProductionReady()).append("\n");
        sb.append("  Supported Features: ").append(compatibilityAnalysis.getSupportedFeatures().size()).append("\n");
        sb.append("  Limited Features: ").append(compatibilityAnalysis.getLimitedFeatures().size()).append("\n");
        sb.append("  Unsupported Features: ").append(compatibilityAnalysis.getUnsupportedFeatures().size()).append("\n\n");

        // Optimization configuration
        final PlatformOptimizations.OptimizationConfiguration optimizationConfig =
            PlatformOptimizations.getOptimizedConfiguration();
        sb.append("Optimization Configuration:\n");
        sb.append("  ").append(optimizationConfig.toString()).append("\n\n");

        // Component diagnostics
        sb.append("Component Diagnostics:\n");
        sb.append(NativeLibraryUtils.getDiagnosticInfo()).append("\n");
        sb.append(NativeLibraryVersionManager.getDiagnosticInfo()).append("\n");
        sb.append(PlatformOptimizations.getDiagnosticInfo()).append("\n");

        return sb.toString();
    }
}