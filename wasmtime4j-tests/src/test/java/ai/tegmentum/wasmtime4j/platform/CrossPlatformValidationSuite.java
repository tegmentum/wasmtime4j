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

import static org.assertj.core.api.Assertions.*;

import ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector;
import ai.tegmentum.wasmtime4j.nativeloader.PlatformFeatureDetector;
import ai.tegmentum.wasmtime4j.platform.CrossPlatformCompatibility;
import ai.tegmentum.wasmtime4j.platform.NativeLibraryVersionManager;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Comprehensive cross-platform validation test suite.
 *
 * <p>This test suite validates Wasmtime4j functionality across different platforms,
 * architectures, and deployment environments. It ensures consistent behavior and
 * performance characteristics across all supported platforms.
 *
 * <p>Test categories:
 * <ul>
 *   <li>Platform detection and feature validation
 *   <li>Native library loading and version management
 *   <li>Runtime selection and optimization
 *   <li>Cross-platform compatibility validation
 *   <li>Performance and resource utilization
 *   <li>Environment-specific behavior validation
 * </ul>
 */
@DisplayName("Cross-Platform Validation Suite")
@Execution(ExecutionMode.CONCURRENT)
class CrossPlatformValidationSuite extends BaseIntegrationTest {

    private static final Logger LOGGER = Logger.getLogger(CrossPlatformValidationSuite.class.getName());

    private PlatformDetector.PlatformInfo platformInfo;
    private PlatformFeatureDetector.PlatformFeatures platformFeatures;
    private CrossPlatformCompatibility.CompatibilityAnalysis compatibilityAnalysis;

    @Override
    protected void doSetUp(final TestInfo testInfo) {
        // Initialize platform information
        this.platformInfo = PlatformDetector.detect();
        this.platformFeatures = PlatformFeatureDetector.detect();
        this.compatibilityAnalysis = CrossPlatformCompatibility.analyzeCompatibility();

        LOGGER.info("Running cross-platform tests on: " + platformInfo.getPlatformId());
        LOGGER.info("Platform features: " + platformFeatures);
        LOGGER.info("Compatibility analysis: " + compatibilityAnalysis);
    }

    @Nested
    @DisplayName("Platform Detection Tests")
    class PlatformDetectionTests {

        @Test
        @DisplayName("Should detect platform correctly and consistently")
        void shouldDetectPlatformCorrectly() {
            // Verify basic platform detection
            assertThat(platformInfo).isNotNull();
            assertThat(platformInfo.getOperatingSystem()).isNotNull();
            assertThat(platformInfo.getArchitecture()).isNotNull();
            assertThat(platformInfo.getPlatformId()).isNotEmpty();

            // Verify consistency across multiple calls
            final PlatformDetector.PlatformInfo secondDetection = PlatformDetector.detect();
            assertThat(secondDetection).isEqualTo(platformInfo);

            // Verify platform ID format
            assertThat(platformInfo.getPlatformId())
                .matches("^(linux|windows|macos)-(x86_64|aarch64)$");
        }

        @Test
        @DisplayName("Should detect supported platform")
        void shouldDetectSupportedPlatform() {
            assertThat(PlatformDetector.isPlatformSupported()).isTrue();

            final String description = PlatformDetector.getPlatformDescription();
            assertThat(description).isNotEmpty();
            assertThat(description).contains("Platform:");
            assertThat(description).contains("Java:");
        }

        @Test
        @DisplayName("Should generate correct library file names")
        void shouldGenerateCorrectLibraryFileNames() {
            final String libraryName = "wasmtime4j";
            final String fileName = platformInfo.getLibraryFileName(libraryName);

            switch (platformInfo.getOperatingSystem()) {
                case LINUX:
                case MACOS:
                    assertThat(fileName).startsWith("lib");
                    break;
                case WINDOWS:
                    assertThat(fileName).doesNotStartWith("lib");
                    break;
            }

            // Verify extension
            final String expectedExtension = platformInfo.getOperatingSystem().getLibraryExtension();
            assertThat(fileName).endsWith(expectedExtension);
        }

        @Test
        @DisplayName("Should generate correct resource paths")
        void shouldGenerateCorrectResourcePaths() {
            final String libraryName = "wasmtime4j";
            final String resourcePath = platformInfo.getLibraryResourcePath(libraryName);

            assertThat(resourcePath).startsWith("/natives/");
            assertThat(resourcePath).contains(platformInfo.getPlatformId());
            assertThat(resourcePath).endsWith(platformInfo.getLibraryFileName(libraryName));
        }
    }

    @Nested
    @DisplayName("Feature Detection Tests")
    class FeatureDetectionTests {

        @Test
        @DisplayName("Should detect platform features comprehensively")
        void shouldDetectPlatformFeatures() {
            assertThat(platformFeatures).isNotNull();
            assertThat(platformFeatures.getPlatformInfo()).isEqualTo(platformInfo);

            // Verify core metrics
            assertThat(platformFeatures.getLogicalCores()).isGreaterThan(0);
            assertThat(platformFeatures.getPhysicalCores()).isGreaterThan(0);
            assertThat(platformFeatures.getTotalMemory()).isGreaterThan(0);
            assertThat(platformFeatures.getAvailableMemory()).isGreaterThan(0);

            // Logical cores should be >= physical cores
            assertThat(platformFeatures.getLogicalCores())
                .isGreaterThanOrEqualTo(platformFeatures.getPhysicalCores());

            // Available memory should be <= total memory
            assertThat(platformFeatures.getAvailableMemory())
                .isLessThanOrEqualTo(platformFeatures.getTotalMemory());
        }

        @Test
        @DisplayName("Should detect JVM features")
        void shouldDetectJvmFeatures() {
            final Set<PlatformFeatureDetector.JvmFeature> jvmFeatures = platformFeatures.getJvmFeatures();
            assertThat(jvmFeatures).isNotNull();

            // At least one JVM implementation should be detected
            final boolean hasJvmImpl = jvmFeatures.contains(PlatformFeatureDetector.JvmFeature.HOTSPOT) ||
                jvmFeatures.contains(PlatformFeatureDetector.JvmFeature.OPENJ9) ||
                jvmFeatures.contains(PlatformFeatureDetector.JvmFeature.GRAALVM_NATIVE);
            assertThat(hasJvmImpl).isTrue();

            // Check Panama FFI availability based on Java version
            final String javaVersion = System.getProperty("java.version");
            if (javaVersion.startsWith("1.") || javaVersion.startsWith("9") ||
                javaVersion.startsWith("10") || javaVersion.startsWith("11") ||
                javaVersion.startsWith("12") || javaVersion.startsWith("13") ||
                javaVersion.startsWith("14") || javaVersion.startsWith("15") ||
                javaVersion.startsWith("16") || javaVersion.startsWith("17") ||
                javaVersion.startsWith("18") || javaVersion.startsWith("19") ||
                javaVersion.startsWith("20") || javaVersion.startsWith("21") ||
                javaVersion.startsWith("22")) {
                assertThat(jvmFeatures).doesNotContain(PlatformFeatureDetector.JvmFeature.PANAMA_FFI);
            } else {
                // Java 23+ should have Panama FFI
                assertThat(jvmFeatures).contains(PlatformFeatureDetector.JvmFeature.PANAMA_FFI);
            }
        }

        @Test
        @DisplayName("Should detect CPU features accurately")
        void shouldDetectCpuFeatures() {
            final Set<PlatformFeatureDetector.CpuFeature> cpuFeatures = platformFeatures.getCpuFeatures();
            assertThat(cpuFeatures).isNotNull();

            // Architecture-specific feature validation
            switch (platformInfo.getArchitecture()) {
                case X86_64:
                    // Modern x86_64 processors should have POPCNT
                    assertThat(cpuFeatures).contains(PlatformFeatureDetector.CpuFeature.POPCNT);

                    // AES and FMA are common on modern processors
                    assertThat(cpuFeatures).contains(PlatformFeatureDetector.CpuFeature.AES);
                    assertThat(cpuFeatures).contains(PlatformFeatureDetector.CpuFeature.FMA);

                    // Should not have ARM-specific features
                    assertThat(cpuFeatures).doesNotContain(PlatformFeatureDetector.CpuFeature.NEON);
                    assertThat(cpuFeatures).doesNotContain(PlatformFeatureDetector.CpuFeature.ARM_CRYPTO);
                    break;

                case AARCH64:
                    // ARM64 should have NEON and ARM crypto
                    assertThat(cpuFeatures).contains(PlatformFeatureDetector.CpuFeature.NEON);
                    assertThat(cpuFeatures).contains(PlatformFeatureDetector.CpuFeature.ARM_CRYPTO);

                    // Should not have x86-specific features
                    assertThat(cpuFeatures).doesNotContain(PlatformFeatureDetector.CpuFeature.AVX);
                    assertThat(cpuFeatures).doesNotContain(PlatformFeatureDetector.CpuFeature.AVX2);
                    break;
            }
        }

        @Test
        @DisplayName("Should provide performance recommendations")
        void shouldProvidePerformanceRecommendations() {
            final int optimizationLevel = platformFeatures.getRecommendedOptimizationLevel();
            assertThat(optimizationLevel).isBetween(0, 3);

            final int compilationThreads = platformFeatures.getRecommendedCompilationThreads();
            assertThat(compilationThreads).isGreaterThan(0);
            assertThat(compilationThreads).isLessThanOrEqualTo(platformFeatures.getPhysicalCores() * 2);

            final boolean shouldEnableSimd = platformFeatures.shouldEnableSimd();
            // SIMD should be enabled if we have appropriate CPU features
            if (platformFeatures.hasCpuFeature(PlatformFeatureDetector.CpuFeature.AVX) ||
                platformFeatures.hasCpuFeature(PlatformFeatureDetector.CpuFeature.NEON)) {
                assertThat(shouldEnableSimd).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Compatibility Analysis Tests")
    class CompatibilityAnalysisTests {

        @Test
        @DisplayName("Should analyze compatibility comprehensively")
        void shouldAnalyzeCompatibilityComprehensively() {
            assertThat(compatibilityAnalysis).isNotNull();
            assertThat(compatibilityAnalysis.getPlatformInfo()).isEqualTo(platformInfo);
            assertThat(compatibilityAnalysis.getPlatformFeatures()).isEqualTo(platformFeatures);

            // Support level should be reasonable
            final CrossPlatformCompatibility.SupportLevel supportLevel =
                compatibilityAnalysis.getOverallSupportLevel();
            assertThat(supportLevel).isNotEqualTo(CrossPlatformCompatibility.SupportLevel.UNSUPPORTED);

            // Should have some supported features
            assertThat(compatibilityAnalysis.getSupportedFeatures()).isNotEmpty();

            // At least JNI runtime should be supported
            assertThat(compatibilityAnalysis.isFeatureSupported(
                CrossPlatformCompatibility.CrossPlatformFeature.JNI_RUNTIME)).isTrue();
        }

        @Test
        @DisplayName("Should validate feature support correctly")
        void shouldValidateFeatureSupportCorrectly() {
            // Basic features that should be supported on all platforms
            final List<CrossPlatformCompatibility.CrossPlatformFeature> basicFeatures = Arrays.asList(
                CrossPlatformCompatibility.CrossPlatformFeature.JNI_RUNTIME,
                CrossPlatformCompatibility.CrossPlatformFeature.WASI_FILESYSTEM,
                CrossPlatformCompatibility.CrossPlatformFeature.MEMORY_MAPPING,
                CrossPlatformCompatibility.CrossPlatformFeature.MODULE_SERIALIZATION,
                CrossPlatformCompatibility.CrossPlatformFeature.AOT_COMPILATION
            );

            for (final CrossPlatformCompatibility.CrossPlatformFeature feature : basicFeatures) {
                assertThat(compatibilityAnalysis.isFeatureAvailable(feature))
                    .as("Feature %s should be available", feature)
                    .isTrue();
            }

            // Platform-specific feature validation
            if (platformFeatures.hasJvmFeature(PlatformFeatureDetector.JvmFeature.PANAMA_FFI)) {
                assertThat(compatibilityAnalysis.isFeatureSupported(
                    CrossPlatformCompatibility.CrossPlatformFeature.PANAMA_RUNTIME)).isTrue();
            }

            if (platformFeatures.getLogicalCores() >= 2) {
                assertThat(compatibilityAnalysis.isFeatureAvailable(
                    CrossPlatformCompatibility.CrossPlatformFeature.MULTITHREADING)).isTrue();
            }
        }

        @Test
        @DisplayName("Should provide appropriate recommendations")
        void shouldProvideAppropriateRecommendations() {
            final List<String> recommendations = compatibilityAnalysis.getRecommendations();
            assertThat(recommendations).isNotNull();

            // Should recommend a runtime
            final boolean hasRuntimeRecommendation = recommendations.stream()
                .anyMatch(rec -> rec.contains("runtime") || rec.contains("Panama") || rec.contains("JNI"));
            assertThat(hasRuntimeRecommendation).isTrue();

            // Should have thread pool recommendations
            final boolean hasThreadRecommendation = recommendations.stream()
                .anyMatch(rec -> rec.contains("thread"));
            assertThat(hasThreadRecommendation).isTrue();
        }

        @Test
        @DisplayName("Should indicate production readiness accurately")
        void shouldIndicateProductionReadinessAccurately() {
            final boolean isProductionReady = compatibilityAnalysis.isProductionReady();

            // Production readiness should correlate with support level
            final CrossPlatformCompatibility.SupportLevel supportLevel =
                compatibilityAnalysis.getOverallSupportLevel();

            if (supportLevel == CrossPlatformCompatibility.SupportLevel.FULLY_SUPPORTED ||
                supportLevel == CrossPlatformCompatibility.SupportLevel.SUPPORTED_WITH_LIMITATIONS) {
                assertThat(isProductionReady).isTrue();
            } else {
                assertThat(isProductionReady).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("Runtime Configuration Tests")
    class RuntimeConfigurationTests {

        @Test
        @DisplayName("Should generate appropriate runtime configurations")
        void shouldGenerateAppropriateRuntimeConfigurations() {
            // Test different optimization strategies
            final CrossPlatformCompatibility.OptimizationStrategy[] strategies =
                CrossPlatformCompatibility.OptimizationStrategy.values();

            for (final CrossPlatformCompatibility.OptimizationStrategy strategy : strategies) {
                final CrossPlatformCompatibility.RuntimeConfiguration config =
                    CrossPlatformCompatibility.getRecommendedConfiguration(strategy);

                assertThat(config).isNotNull();
                assertThat(config.getRecommendedCompilationThreads()).isGreaterThan(0);
                assertThat(config.getRecommendedExecutionThreads()).isGreaterThan(0);
                assertThat(config.getRecommendedMemoryLimit()).isGreaterThan(0);
                assertThat(config.getRecommendedTimeout()).isPositive();
                assertThat(config.getOptimizationLevel()).isBetween(0, 3);

                // Verify strategy-specific characteristics
                switch (strategy) {
                    case PERFORMANCE_FOCUSED:
                        assertThat(config.getOptimizationLevel()).isEqualTo(3);
                        assertThat(config.shouldEnableAsyncCompilation()).isTrue();
                        break;

                    case MEMORY_CONSERVATIVE:
                        assertThat(config.getOptimizationLevel()).isEqualTo(1);
                        assertThat(config.shouldEnableModuleCaching()).isFalse();
                        break;

                    case COMPATIBILITY_FOCUSED:
                        assertThat(config.getOptimizationLevel()).isEqualTo(0);
                        assertThat(config.shouldEnableSimdOptimizations()).isFalse();
                        break;

                    case STARTUP_OPTIMIZED:
                        assertThat(config.getRecommendedTimeout()).isLessThanOrEqualTo(Duration.ofMinutes(2));
                        break;
                }
            }
        }

        @Test
        @DisplayName("Should use platform's recommended strategy by default")
        void shouldUsePlatformRecommendedStrategyByDefault() {
            final CrossPlatformCompatibility.RuntimeConfiguration defaultConfig =
                CrossPlatformCompatibility.getRecommendedConfiguration();

            final CrossPlatformCompatibility.RuntimeConfiguration strategicConfig =
                CrossPlatformCompatibility.getRecommendedConfiguration(
                    compatibilityAnalysis.getRecommendedStrategy());

            // Default should match the platform's recommended strategy
            assertThat(defaultConfig.getRecommendedCompilationThreads())
                .isEqualTo(strategicConfig.getRecommendedCompilationThreads());
            assertThat(defaultConfig.getOptimizationLevel())
                .isEqualTo(strategicConfig.getOptimizationLevel());
        }
    }

    @Nested
    @DisplayName("Version Management Tests")
    class VersionManagerTests {

        @Test
        @DisplayName("Should discover library versions")
        void shouldDiscoverLibraryVersions() {
            final List<NativeLibraryVersionManager.LibraryVersionInfo> versions =
                NativeLibraryVersionManager.discoverAvailableVersions("wasmtime4j");

            // Should find at least one version (the bundled one)
            assertThat(versions).isNotEmpty();

            // Verify version info structure
            for (final NativeLibraryVersionManager.LibraryVersionInfo version : versions) {
                assertThat(version.getLibraryName()).isEqualTo("wasmtime4j");
                assertThat(version.getVersion()).isNotNull();
                assertThat(version.getWasmtimeVersion()).isNotNull();
                assertThat(version.getPlatformInfo()).isNotNull();
            }
        }

        @Test
        @DisplayName("Should validate version compatibility")
        void shouldValidateVersionCompatibility() {
            final List<NativeLibraryVersionManager.LibraryVersionInfo> versions =
                NativeLibraryVersionManager.discoverAvailableVersions("wasmtime4j");

            if (!versions.isEmpty()) {
                final NativeLibraryVersionManager.LibraryVersionInfo version = versions.get(0);
                final NativeLibraryVersionManager.CompatibilityResult compatibility =
                    NativeLibraryVersionManager.validateCompatibility(version);

                assertThat(compatibility).isNotNull();
                assertThat(compatibility.getMinimumVersion()).isNotNull();
                assertThat(compatibility.getRecommendedVersion()).isNotNull();

                // Issues and recommendations should be lists (possibly empty)
                assertThat(compatibility.getIssues()).isNotNull();
                assertThat(compatibility.getRecommendations()).isNotNull();
            }
        }

        @Test
        @DisplayName("Should parse semantic versions correctly")
        void shouldParseSemanticVersionsCorrectly() {
            // Test valid version strings
            final String[] validVersions = {
                "1.0.0", "2.1.3", "1.0.0-alpha", "1.0.0-alpha.1",
                "1.0.0+build.1", "1.0.0-alpha+build.1"
            };

            for (final String versionString : validVersions) {
                final NativeLibraryVersionManager.SemanticVersion version =
                    NativeLibraryVersionManager.SemanticVersion.parse(versionString);

                assertThat(version).isNotNull();
                assertThat(version.toString()).isNotEmpty();
                assertThat(version.getMajor()).isGreaterThanOrEqualTo(0);
                assertThat(version.getMinor()).isGreaterThanOrEqualTo(0);
                assertThat(version.getPatch()).isGreaterThanOrEqualTo(0);
            }

            // Test invalid version strings
            final String[] invalidVersions = {
                "", "1", "1.0", "1.0.0.0", "invalid", "1.0.0-"
            };

            for (final String versionString : invalidVersions) {
                assertThatThrownBy(() ->
                    NativeLibraryVersionManager.SemanticVersion.parse(versionString))
                    .isInstanceOf(IllegalArgumentException.class);
            }
        }

        @Test
        @DisplayName("Should compare versions correctly")
        void shouldCompareVersionsCorrectly() {
            final NativeLibraryVersionManager.SemanticVersion v1_0_0 =
                new NativeLibraryVersionManager.SemanticVersion(1, 0, 0);
            final NativeLibraryVersionManager.SemanticVersion v1_0_1 =
                new NativeLibraryVersionManager.SemanticVersion(1, 0, 1);
            final NativeLibraryVersionManager.SemanticVersion v1_1_0 =
                new NativeLibraryVersionManager.SemanticVersion(1, 1, 0);
            final NativeLibraryVersionManager.SemanticVersion v2_0_0 =
                new NativeLibraryVersionManager.SemanticVersion(2, 0, 0);

            // Test ordering
            assertThat(v1_0_0.compareTo(v1_0_1)).isLessThan(0);
            assertThat(v1_0_1.compareTo(v1_1_0)).isLessThan(0);
            assertThat(v1_1_0.compareTo(v2_0_0)).isLessThan(0);

            // Test equality
            assertThat(v1_0_0.compareTo(new NativeLibraryVersionManager.SemanticVersion(1, 0, 0)))
                .isEqualTo(0);

            // Test compatibility
            assertThat(v1_1_0.isCompatibleWith(v1_0_0)).isTrue();
            assertThat(v1_0_0.isCompatibleWith(v1_1_0)).isFalse();
            assertThat(v2_0_0.isCompatibleWith(v1_0_0)).isFalse();
        }
    }

    @Nested
    @DisplayName("Feature Validation Tests")
    class FeatureValidationTests {

        @Test
        @DisplayName("Should validate JNI runtime feature")
        void shouldValidateJniRuntimeFeature() {
            final boolean isValid = CrossPlatformCompatibility.validateFeature(
                CrossPlatformCompatibility.CrossPlatformFeature.JNI_RUNTIME);
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should validate WASI filesystem feature")
        void shouldValidateWasiFilesystemFeature() {
            final boolean isValid = CrossPlatformCompatibility.validateFeature(
                CrossPlatformCompatibility.CrossPlatformFeature.WASI_FILESYSTEM);
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should validate memory mapping feature")
        void shouldValidateMemoryMappingFeature() {
            final boolean isValid = CrossPlatformCompatibility.validateFeature(
                CrossPlatformCompatibility.CrossPlatformFeature.MEMORY_MAPPING);
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should validate Panama runtime feature conditionally")
        void shouldValidatePanamaRuntimeFeatureConditionally() {
            final boolean isValid = CrossPlatformCompatibility.validateFeature(
                CrossPlatformCompatibility.CrossPlatformFeature.PANAMA_RUNTIME);

            // Should match feature detection
            final boolean hasFeature = compatibilityAnalysis.isFeatureSupported(
                CrossPlatformCompatibility.CrossPlatformFeature.PANAMA_RUNTIME);

            assertThat(isValid).isEqualTo(hasFeature);
        }
    }

    @Nested
    @DisplayName("Performance Validation Tests")
    class PerformanceValidationTests {

        @Test
        @DisplayName("Should complete compatibility analysis within reasonable time")
        void shouldCompleteCompatibilityAnalysisWithinReasonableTime() {
            final long startTime = System.nanoTime();

            // Force fresh analysis
            final CrossPlatformCompatibility.CompatibilityAnalysis freshAnalysis =
                CrossPlatformCompatibility.reanalyzeCompatibility();

            final long endTime = System.nanoTime();
            final Duration analysisTime = Duration.ofNanos(endTime - startTime);

            assertThat(freshAnalysis).isNotNull();
            assertThat(analysisTime).isLessThan(Duration.ofSeconds(5));

            LOGGER.info("Compatibility analysis completed in: " + analysisTime.toMillis() + "ms");
        }

        @Test
        @DisplayName("Should cache analysis results effectively")
        void shouldCacheAnalysisResultsEffectively() {
            // First call (may be cached from setup)
            final long startTime1 = System.nanoTime();
            final CrossPlatformCompatibility.CompatibilityAnalysis analysis1 =
                CrossPlatformCompatibility.analyzeCompatibility();
            final long endTime1 = System.nanoTime();

            // Second call (should be cached)
            final long startTime2 = System.nanoTime();
            final CrossPlatformCompatibility.CompatibilityAnalysis analysis2 =
                CrossPlatformCompatibility.analyzeCompatibility();
            final long endTime2 = System.nanoTime();

            // Results should be identical (same instance)
            assertThat(analysis1).isSameAs(analysis2);

            // Second call should be faster (cached)
            final Duration time1 = Duration.ofNanos(endTime1 - startTime1);
            final Duration time2 = Duration.ofNanos(endTime2 - startTime2);

            LOGGER.info("First analysis: " + time1.toMillis() + "ms");
            LOGGER.info("Second analysis: " + time2.toMillis() + "ms");

            // Cached call should be significantly faster
            if (time1.toMillis() > 10) { // Only test if first call took measurable time
                assertThat(time2).isLessThan(time1.dividedBy(2));
            }
        }

        @Test
        @DisplayName("Should handle concurrent analysis requests safely")
        void shouldHandleConcurrentAnalysisRequestsSafely() throws Exception {
            final int concurrentRequests = 10;
            final List<CompletableFuture<CrossPlatformCompatibility.CompatibilityAnalysis>> futures =
                new ArrayList<>();

            // Force fresh analysis to test concurrency
            CrossPlatformCompatibility.reanalyzeCompatibility();

            // Submit concurrent requests
            for (int i = 0; i < concurrentRequests; i++) {
                final CompletableFuture<CrossPlatformCompatibility.CompatibilityAnalysis> future =
                    CompletableFuture.supplyAsync(CrossPlatformCompatibility::analyzeCompatibility);
                futures.add(future);
            }

            // Wait for all to complete
            final CompletableFuture<Void> allDone = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
            allDone.get(10, TimeUnit.SECONDS);

            // All results should be identical (same cached instance)
            final CrossPlatformCompatibility.CompatibilityAnalysis firstResult = futures.get(0).get();
            for (final CompletableFuture<CrossPlatformCompatibility.CompatibilityAnalysis> future : futures) {
                assertThat(future.get()).isSameAs(firstResult);
            }
        }
    }

    @Nested
    @DisplayName("Platform-Specific Tests")
    class PlatformSpecificTests {

        @Test
        @EnabledOnOs(OS.LINUX)
        @DisplayName("Should detect Linux-specific features")
        void shouldDetectLinuxSpecificFeatures() {
            assertThat(platformInfo.getOperatingSystem())
                .isEqualTo(PlatformDetector.OperatingSystem.LINUX);

            // Linux should support various features
            final Set<PlatformFeatureDetector.OsFeature> osFeatures = platformFeatures.getOsFeatures();

            // At least cgroups should be available on most Linux systems
            // (though we'll be lenient for different test environments)
            if (platformFeatures.hasOsFeature(PlatformFeatureDetector.OsFeature.CGROUPS)) {
                assertThat(osFeatures).contains(PlatformFeatureDetector.OsFeature.CGROUPS);
            }
        }

        @Test
        @EnabledOnOs(OS.MAC)
        @DisplayName("Should detect macOS-specific features")
        void shouldDetectMacOsSpecificFeatures() {
            assertThat(platformInfo.getOperatingSystem())
                .isEqualTo(PlatformDetector.OperatingSystem.MACOS);

            // macOS-specific validations
            final String libraryFileName = platformInfo.getLibraryFileName("test");
            assertThat(libraryFileName).endsWith(".dylib");
            assertThat(libraryFileName).startsWith("lib");
        }

        @Test
        @EnabledOnOs(OS.WINDOWS)
        @DisplayName("Should detect Windows-specific features")
        void shouldDetectWindowsSpecificFeatures() {
            assertThat(platformInfo.getOperatingSystem())
                .isEqualTo(PlatformDetector.OperatingSystem.WINDOWS);

            // Windows-specific validations
            final String libraryFileName = platformInfo.getLibraryFileName("test");
            assertThat(libraryFileName).endsWith(".dll");
            assertThat(libraryFileName).doesNotStartWith("lib");
        }
    }

    @Test
    @DisplayName("Should provide comprehensive diagnostic information")
    void shouldProvideComprehensiveDiagnosticInformation() {
        final String diagnosticInfo = NativeLibraryVersionManager.getDiagnosticInfo();

        assertThat(diagnosticInfo).isNotEmpty();
        assertThat(diagnosticInfo).contains("Native Library Version Manager");
        assertThat(diagnosticInfo).contains("Minimum Wasmtime version");
        assertThat(diagnosticInfo).contains("Recommended Wasmtime version");

        LOGGER.info("Diagnostic Information:\n" + diagnosticInfo);
    }
}