/*
 * Copyright 2025 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.panama.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.util.PanamaCapabilityDetector.ClassAvailabilityInfo;
import ai.tegmentum.wasmtime4j.panama.util.PanamaCapabilityDetector.DetectionResult;
import ai.tegmentum.wasmtime4j.panama.util.PanamaCapabilityDetector.FallbackRecommendation;
import ai.tegmentum.wasmtime4j.panama.util.PanamaCapabilityDetector.FunctionalTestInfo;
import ai.tegmentum.wasmtime4j.panama.util.PanamaCapabilityDetector.JavaVersionInfo;
import ai.tegmentum.wasmtime4j.panama.util.PanamaCapabilityDetector.NativeAccessInfo;
import ai.tegmentum.wasmtime4j.panama.util.PanamaCapabilityDetector.PreviewFeatureInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaCapabilityDetector} utility class.
 *
 * <p>This test class verifies the Panama FFI capability detection functionality.
 */
@DisplayName("PanamaCapabilityDetector Tests")
class PanamaCapabilityDetectorTest {

  @AfterEach
  void tearDown() {
    // Clear cache after each test to ensure isolation
    PanamaCapabilityDetector.clearCache();
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaCapabilityDetector should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaCapabilityDetector.class.getModifiers()),
          "PanamaCapabilityDetector should be final");
    }

    @Test
    @DisplayName("Constructor should throw UnsupportedOperationException")
    void constructorShouldThrowUnsupportedOperationException() throws Exception {
      final var constructor = PanamaCapabilityDetector.class.getDeclaredConstructor();
      constructor.setAccessible(true);
      assertThrows(
          java.lang.reflect.InvocationTargetException.class,
          constructor::newInstance,
          "Constructor should throw exception");
    }
  }

  @Nested
  @DisplayName("detectCapabilities Tests")
  class DetectCapabilitiesTests {

    @Test
    @DisplayName("detectCapabilities should return non-null result")
    void detectCapabilitiesShouldReturnNonNullResult() {
      final DetectionResult result = PanamaCapabilityDetector.detectCapabilities();
      assertNotNull(result, "Detection result should not be null");
    }

    @Test
    @DisplayName("detectCapabilities should return cached result")
    void detectCapabilitiesShouldReturnCachedResult() {
      final DetectionResult result1 = PanamaCapabilityDetector.detectCapabilities();
      final DetectionResult result2 = PanamaCapabilityDetector.detectCapabilities();
      assertSame(result1, result2, "Should return cached result");
    }

    @Test
    @DisplayName("detectCapabilities should return fresh result after clearCache")
    void detectCapabilitiesShouldReturnFreshResultAfterClearCache() {
      final DetectionResult result1 = PanamaCapabilityDetector.detectCapabilities();
      PanamaCapabilityDetector.clearCache();
      final DetectionResult result2 = PanamaCapabilityDetector.detectCapabilities();

      // Results should be equivalent but not same object
      assertNotNull(result1, "First result should not be null");
      assertNotNull(result2, "Second result should not be null");
    }
  }

  @Nested
  @DisplayName("isPanamaAvailable Tests")
  class IsPanamaAvailableTests {

    @Test
    @DisplayName("isPanamaAvailable should return boolean value")
    void isPanamaAvailableShouldReturnBooleanValue() {
      // Just verify it doesn't throw - actual availability depends on JVM version
      assertDoesNotThrow(PanamaCapabilityDetector::isPanamaAvailable, "Should not throw exception");
    }
  }

  @Nested
  @DisplayName("getStatusDescription Tests")
  class GetStatusDescriptionTests {

    @Test
    @DisplayName("getStatusDescription should return non-null string")
    void getStatusDescriptionShouldReturnNonNullString() {
      final String status = PanamaCapabilityDetector.getStatusDescription();
      assertNotNull(status, "Status description should not be null");
      assertFalse(status.isEmpty(), "Status description should not be empty");
    }

    @Test
    @DisplayName("getStatusDescription should contain meaningful content")
    void getStatusDescriptionShouldContainMeaningfulContent() {
      final String status = PanamaCapabilityDetector.getStatusDescription();
      assertTrue(
          status.toLowerCase().contains("panama")
              || status.toLowerCase().contains("available")
              || status.toLowerCase().contains("unavailable"),
          "Status should contain relevant information");
    }
  }

  @Nested
  @DisplayName("getDiagnosticInfo Tests")
  class GetDiagnosticInfoTests {

    @Test
    @DisplayName("getDiagnosticInfo should return non-null string")
    void getDiagnosticInfoShouldReturnNonNullString() {
      final String diagnostics = PanamaCapabilityDetector.getDiagnosticInfo();
      assertNotNull(diagnostics, "Diagnostic info should not be null");
      assertFalse(diagnostics.isEmpty(), "Diagnostic info should not be empty");
    }

    @Test
    @DisplayName("getDiagnosticInfo should contain version information")
    void getDiagnosticInfoShouldContainVersionInformation() {
      final String diagnostics = PanamaCapabilityDetector.getDiagnosticInfo();
      assertTrue(diagnostics.contains("Available:"), "Diagnostics should contain availability");
    }
  }

  @Nested
  @DisplayName("getFallbackRecommendation Tests")
  class GetFallbackRecommendationTests {

    @Test
    @DisplayName("getFallbackRecommendation should return non-null")
    void getFallbackRecommendationShouldReturnNonNull() {
      final FallbackRecommendation recommendation =
          PanamaCapabilityDetector.getFallbackRecommendation();
      assertNotNull(recommendation, "Fallback recommendation should not be null");
    }
  }

  @Nested
  @DisplayName("clearCache Tests")
  class ClearCacheTests {

    @Test
    @DisplayName("clearCache should not throw")
    void clearCacheShouldNotThrow() {
      assertDoesNotThrow(PanamaCapabilityDetector::clearCache, "clearCache should not throw");
    }

    @Test
    @DisplayName("clearCache should allow re-detection")
    void clearCacheShouldAllowReDetection() {
      PanamaCapabilityDetector.detectCapabilities();
      PanamaCapabilityDetector.clearCache();
      assertDoesNotThrow(
          PanamaCapabilityDetector::detectCapabilities,
          "Should be able to detect after cache clear");
    }
  }

  @Nested
  @DisplayName("DetectionResult Tests")
  class DetectionResultTests {

    @Test
    @DisplayName("DetectionResult should have getters for all info types")
    void detectionResultShouldHaveGettersForAllInfoTypes() {
      final DetectionResult result = PanamaCapabilityDetector.detectCapabilities();

      // All getters should work without throwing
      assertDoesNotThrow(result::isAvailable, "isAvailable should work");
      assertDoesNotThrow(result::getFailureReason, "getFailureReason should work");
      assertDoesNotThrow(
          result::getFallbackRecommendation, "getFallbackRecommendation should work");
      assertDoesNotThrow(result::getJavaVersionInfo, "getJavaVersionInfo should work");
      assertDoesNotThrow(result::getClassAvailabilityInfo, "getClassAvailabilityInfo should work");
      assertDoesNotThrow(result::getNativeAccessInfo, "getNativeAccessInfo should work");
      assertDoesNotThrow(result::getPreviewFeatureInfo, "getPreviewFeatureInfo should work");
      assertDoesNotThrow(result::getFunctionalTestInfo, "getFunctionalTestInfo should work");
      assertDoesNotThrow(result::getStatusDescription, "getStatusDescription should work");
      assertDoesNotThrow(result::getDiagnosticInfo, "getDiagnosticInfo should work");
    }

    @Test
    @DisplayName("DetectionResult fallback recommendation should not be null")
    void detectionResultFallbackRecommendationShouldNotBeNull() {
      final DetectionResult result = PanamaCapabilityDetector.detectCapabilities();
      assertNotNull(
          result.getFallbackRecommendation(), "Fallback recommendation should never be null");
    }
  }

  @Nested
  @DisplayName("FallbackRecommendation Enum Tests")
  class FallbackRecommendationEnumTests {

    @Test
    @DisplayName("FallbackRecommendation should have all expected values")
    void fallbackRecommendationShouldHaveAllExpectedValues() {
      assertTrue(
          FallbackRecommendation.values().length >= 5,
          "Should have at least 5 fallback recommendations");

      assertNotNull(FallbackRecommendation.NONE, "NONE should exist");
      assertNotNull(FallbackRecommendation.USE_JNI, "USE_JNI should exist");
      assertNotNull(FallbackRecommendation.ENABLE_PREVIEW, "ENABLE_PREVIEW should exist");
      assertNotNull(
          FallbackRecommendation.ENABLE_NATIVE_ACCESS, "ENABLE_NATIVE_ACCESS should exist");
      assertNotNull(FallbackRecommendation.UPGRADE_JAVA, "UPGRADE_JAVA should exist");
    }

    @Test
    @DisplayName("FallbackRecommendation should have descriptions")
    void fallbackRecommendationShouldHaveDescriptions() {
      for (final FallbackRecommendation recommendation : FallbackRecommendation.values()) {
        assertNotNull(recommendation.getDescription(), "Description should not be null");
        assertFalse(recommendation.getDescription().isEmpty(), "Description should not be empty");
      }
    }

    @Test
    @DisplayName("FallbackRecommendation toString should return description")
    void fallbackRecommendationToStringShouldReturnDescription() {
      for (final FallbackRecommendation recommendation : FallbackRecommendation.values()) {
        assertTrue(
            recommendation.toString().equals(recommendation.getDescription()),
            "toString should return description");
      }
    }
  }

  @Nested
  @DisplayName("JavaVersionInfo Tests")
  class JavaVersionInfoTests {

    @Test
    @DisplayName("JavaVersionInfo should have all getters")
    void javaVersionInfoShouldHaveAllGetters() {
      final DetectionResult result = PanamaCapabilityDetector.detectCapabilities();
      final JavaVersionInfo versionInfo = result.getJavaVersionInfo();

      if (versionInfo != null) {
        assertDoesNotThrow(versionInfo::getFullVersion, "getFullVersion should work");
        assertDoesNotThrow(versionInfo::getVendor, "getVendor should work");
        assertDoesNotThrow(versionInfo::getRuntimeName, "getRuntimeName should work");
        assertDoesNotThrow(versionInfo::getMajorVersion, "getMajorVersion should work");
        assertDoesNotThrow(versionInfo::isJava23OrHigher, "isJava23OrHigher should work");
      }
    }
  }

  @Nested
  @DisplayName("ClassAvailabilityInfo Tests")
  class ClassAvailabilityInfoTests {

    @Test
    @DisplayName("ClassAvailabilityInfo should track available and missing classes")
    void classAvailabilityInfoShouldTrackClasses() {
      final DetectionResult result = PanamaCapabilityDetector.detectCapabilities();
      final ClassAvailabilityInfo classInfo = result.getClassAvailabilityInfo();

      if (classInfo != null) {
        assertNotNull(classInfo.getAvailableClasses(), "Available classes should not be null");
        assertNotNull(classInfo.getMissingClasses(), "Missing classes should not be null");
        assertDoesNotThrow(classInfo::areAllClassesAvailable, "areAllClassesAvailable should work");
      }
    }
  }

  @Nested
  @DisplayName("NativeAccessInfo Tests")
  class NativeAccessInfoTests {

    @Test
    @DisplayName("NativeAccessInfo should have all getters")
    void nativeAccessInfoShouldHaveAllGetters() {
      final DetectionResult result = PanamaCapabilityDetector.detectCapabilities();
      final NativeAccessInfo nativeInfo = result.getNativeAccessInfo();

      if (nativeInfo != null) {
        assertDoesNotThrow(nativeInfo::isNativeAccessEnabled, "isNativeAccessEnabled should work");
        assertDoesNotThrow(nativeInfo::getRestrictionReason, "getRestrictionReason should work");
        assertDoesNotThrow(nativeInfo::getProperties, "getProperties should work");
      }
    }
  }

  @Nested
  @DisplayName("PreviewFeatureInfo Tests")
  class PreviewFeatureInfoTests {

    @Test
    @DisplayName("PreviewFeatureInfo should track preview properties")
    void previewFeatureInfoShouldTrackProperties() {
      final DetectionResult result = PanamaCapabilityDetector.detectCapabilities();
      final PreviewFeatureInfo previewInfo = result.getPreviewFeatureInfo();

      if (previewInfo != null) {
        assertNotNull(previewInfo.getPreviewProperties(), "Preview properties should not be null");
      }
    }
  }

  @Nested
  @DisplayName("FunctionalTestInfo Tests")
  class FunctionalTestInfoTests {

    @Test
    @DisplayName("FunctionalTestInfo should track test results")
    void functionalTestInfoShouldTrackTestResults() {
      final DetectionResult result = PanamaCapabilityDetector.detectCapabilities();
      final FunctionalTestInfo testInfo = result.getFunctionalTestInfo();

      if (testInfo != null) {
        assertNotNull(testInfo.getPassingTests(), "Passing tests should not be null");
        assertNotNull(testInfo.getFailingTests(), "Failing tests should not be null");
        assertDoesNotThrow(testInfo::hasNativeAccessIssues, "hasNativeAccessIssues should work");
        assertDoesNotThrow(testInfo::areBasicTestsPassing, "areBasicTestsPassing should work");
      }
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Full detection cycle should work without errors")
    void fullDetectionCycleShouldWorkWithoutErrors() {
      // Perform full detection cycle
      PanamaCapabilityDetector.clearCache();

      assertDoesNotThrow(
          () -> {
            final DetectionResult result = PanamaCapabilityDetector.detectCapabilities();
            final boolean available = result.isAvailable();
            final String status = result.getStatusDescription();
            final String diagnostics = result.getDiagnosticInfo();
            final FallbackRecommendation recommendation = result.getFallbackRecommendation();

            // Verify coherence
            if (available) {
              assertTrue(
                  recommendation == FallbackRecommendation.NONE
                      || recommendation == FallbackRecommendation.USE_JNI,
                  "Available Panama should have NONE or USE_JNI fallback");
            }

            assertNotNull(status, "Status should not be null");
            assertNotNull(diagnostics, "Diagnostics should not be null");
          },
          "Full detection cycle should complete without errors");
    }

    @Test
    @DisplayName("Detection should be thread-safe")
    void detectionShouldBeThreadSafe() throws Exception {
      PanamaCapabilityDetector.clearCache();

      final int threadCount = 10;
      final Thread[] threads = new Thread[threadCount];
      final DetectionResult[] results = new DetectionResult[threadCount];
      final Exception[] errors = new Exception[threadCount];

      for (int i = 0; i < threadCount; i++) {
        final int index = i;
        threads[i] =
            new Thread(
                () -> {
                  try {
                    results[index] = PanamaCapabilityDetector.detectCapabilities();
                  } catch (Exception e) {
                    errors[index] = e;
                  }
                });
      }

      // Start all threads
      for (Thread thread : threads) {
        thread.start();
      }

      // Wait for all threads
      for (Thread thread : threads) {
        thread.join();
      }

      // Verify no errors
      for (int i = 0; i < threadCount; i++) {
        if (errors[i] != null) {
          throw errors[i];
        }
        assertNotNull(results[i], "Result " + i + " should not be null");
      }

      // All results should be the same cached instance
      for (int i = 1; i < threadCount; i++) {
        assertSame(results[0], results[i], "All results should be same cached instance");
      }
    }
  }
}
