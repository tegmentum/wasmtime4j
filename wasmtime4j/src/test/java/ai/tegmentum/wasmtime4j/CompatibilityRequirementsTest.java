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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link CompatibilityRequirements} compatibility checking. */
@DisplayName("CompatibilityRequirements")
final class CompatibilityRequirementsTest {

  @Nested
  @DisplayName("defaults factory method")
  final class DefaultsTests {

    @Test
    @DisplayName("should create defaults with PARTIAL minimum compatibility level")
    void shouldCreateDefaultsWithPartialLevel() {
      final CompatibilityRequirements req = CompatibilityRequirements.defaults();
      assertEquals(
          CompatibilityRequirements.CompatibilityLevel.PARTIAL,
          req.getMinimumCompatibilityLevel(),
          "Default compatibility level should be PARTIAL");
    }

    @Test
    @DisplayName("should disallow pre-release by default")
    void shouldDisallowPreReleaseByDefault() {
      final CompatibilityRequirements req = CompatibilityRequirements.defaults();
      assertFalse(
          req.isAllowPreRelease(), "Pre-release should be disallowed by default");
    }

    @Test
    @DisplayName("should allow build metadata by default")
    void shouldAllowBuildMetadataByDefault() {
      final CompatibilityRequirements req = CompatibilityRequirements.defaults();
      assertTrue(
          req.isAllowBuildMetadata(), "Build metadata should be allowed by default");
    }

    @Test
    @DisplayName("should have empty required features by default")
    void shouldHaveEmptyRequiredFeatures() {
      final CompatibilityRequirements req = CompatibilityRequirements.defaults();
      assertTrue(
          req.getRequiredFeatures().isEmpty(),
          "Required features should be empty by default");
    }

    @Test
    @DisplayName("should allow backward compatible by default")
    void shouldAllowBackwardCompatibleByDefault() {
      final CompatibilityRequirements req = CompatibilityRequirements.defaults();
      assertTrue(
          req.isAllowBackwardCompatible(),
          "Backward compatible should be allowed by default");
    }

    @Test
    @DisplayName("should disallow forward compatible by default")
    void shouldDisallowForwardCompatibleByDefault() {
      final CompatibilityRequirements req = CompatibilityRequirements.defaults();
      assertFalse(
          req.isAllowForwardCompatible(),
          "Forward compatible should be disallowed by default");
    }
  }

  @Nested
  @DisplayName("strict factory method")
  final class StrictTests {

    @Test
    @DisplayName("should create strict requirements with FULL compatibility level")
    void shouldHaveFullCompatibilityLevel() {
      final CompatibilityRequirements req = CompatibilityRequirements.strict();
      assertEquals(
          CompatibilityRequirements.CompatibilityLevel.FULL,
          req.getMinimumCompatibilityLevel(),
          "Strict compatibility level should be FULL");
    }

    @Test
    @DisplayName("should enforce strict function signatures")
    void shouldEnforceStrictFunctionSignatures() {
      final CompatibilityRequirements req = CompatibilityRequirements.strict();
      assertTrue(
          req.isStrictFunctionSignatures(),
          "Strict should enforce function signatures");
    }

    @Test
    @DisplayName("should enforce strict type definitions")
    void shouldEnforceStrictTypeDefinitions() {
      final CompatibilityRequirements req = CompatibilityRequirements.strict();
      assertTrue(
          req.isStrictTypeDefinitions(), "Strict should enforce type definitions");
    }

    @Test
    @DisplayName("should disallow pre-release in strict mode")
    void shouldDisallowPreRelease() {
      final CompatibilityRequirements req = CompatibilityRequirements.strict();
      assertFalse(
          req.isAllowPreRelease(), "Strict should disallow pre-release");
    }

    @Test
    @DisplayName("should have zero version differences in strict mode")
    void shouldHaveZeroVersionDifferences() {
      final CompatibilityRequirements req = CompatibilityRequirements.strict();
      assertEquals(0, req.getMaxMajorVersionDifference(), "Max major diff should be 0 in strict");
      assertEquals(0, req.getMaxMinorVersionDifference(), "Max minor diff should be 0 in strict");
    }
  }

  @Nested
  @DisplayName("lenient factory method")
  final class LenientTests {

    @Test
    @DisplayName("should create lenient requirements with PARTIAL compatibility level")
    void shouldHavePartialCompatibilityLevel() {
      final CompatibilityRequirements req = CompatibilityRequirements.lenient();
      assertEquals(
          CompatibilityRequirements.CompatibilityLevel.PARTIAL,
          req.getMinimumCompatibilityLevel(),
          "Lenient compatibility level should be PARTIAL");
    }

    @Test
    @DisplayName("should allow pre-release in lenient mode")
    void shouldAllowPreRelease() {
      final CompatibilityRequirements req = CompatibilityRequirements.lenient();
      assertTrue(req.isAllowPreRelease(), "Lenient should allow pre-release");
    }

    @Test
    @DisplayName("should allow forward and backward compatible")
    void shouldAllowBothDirections() {
      final CompatibilityRequirements req = CompatibilityRequirements.lenient();
      assertTrue(req.isAllowBackwardCompatible(), "Lenient should allow backward compatible");
      assertTrue(req.isAllowForwardCompatible(), "Lenient should allow forward compatible");
    }

    @Test
    @DisplayName("should have relaxed version differences")
    void shouldHaveRelaxedVersionDifferences() {
      final CompatibilityRequirements req = CompatibilityRequirements.lenient();
      assertEquals(
          1, req.getMaxMajorVersionDifference(), "Lenient max major diff should be 1");
      assertEquals(
          5, req.getMaxMinorVersionDifference(), "Lenient max minor diff should be 5");
    }

    @Test
    @DisplayName("should not enforce strict signatures in lenient mode")
    void shouldNotEnforceStrictSignatures() {
      final CompatibilityRequirements req = CompatibilityRequirements.lenient();
      assertFalse(
          req.isStrictFunctionSignatures(),
          "Lenient should not enforce strict function signatures");
      assertFalse(
          req.isStrictTypeDefinitions(),
          "Lenient should not enforce strict type definitions");
    }
  }

  @Nested
  @DisplayName("builder methods")
  final class BuilderTests {

    @Test
    @DisplayName("should set required features")
    void shouldSetRequiredFeatures() {
      final Set<String> features = Set.of("feature-a", "feature-b");
      final CompatibilityRequirements req =
          CompatibilityRequirements.builder().requiredFeatures(features).build();
      assertEquals(2, req.getRequiredFeatures().size(), "Should have 2 required features");
      assertTrue(
          req.getRequiredFeatures().contains("feature-a"),
          "Should contain feature-a");
    }

    @Test
    @DisplayName("should set optional features")
    void shouldSetOptionalFeatures() {
      final Set<String> features = Set.of("opt-x");
      final CompatibilityRequirements req =
          CompatibilityRequirements.builder().optionalFeatures(features).build();
      assertEquals(1, req.getOptionalFeatures().size(), "Should have 1 optional feature");
    }

    @Test
    @DisplayName("should set excluded versions")
    void shouldSetExcludedVersions() {
      final Set<String> excluded = Set.of("1.0.0-beta", "2.0.0-rc1");
      final CompatibilityRequirements req =
          CompatibilityRequirements.builder().excludedVersions(excluded).build();
      assertEquals(2, req.getExcludedVersions().size(), "Should have 2 excluded versions");
    }

    @Test
    @DisplayName("should handle null required features gracefully")
    void shouldHandleNullRequiredFeatures() {
      final CompatibilityRequirements req =
          CompatibilityRequirements.builder().requiredFeatures(null).build();
      assertTrue(
          req.getRequiredFeatures().isEmpty(),
          "Null required features should default to empty");
    }

    @Test
    @DisplayName("should clamp negative max version differences to zero")
    void shouldClampNegativeVersionDifferences() {
      final CompatibilityRequirements req =
          CompatibilityRequirements.builder()
              .maxMajorVersionDifference(-5)
              .maxMinorVersionDifference(-10)
              .build();
      assertEquals(0, req.getMaxMajorVersionDifference(), "Negative major diff should clamp to 0");
      assertEquals(0, req.getMaxMinorVersionDifference(), "Negative minor diff should clamp to 0");
    }
  }

  @Nested
  @DisplayName("VersionRange")
  final class VersionRangeTests {

    @Test
    @DisplayName("any() should accept any version")
    void anyShouldAcceptAnyVersion() {
      final CompatibilityRequirements.VersionRange range =
          CompatibilityRequirements.VersionRange.any();
      final WitInterfaceVersion version = new WitInterfaceVersion(1, 0, 0);
      assertTrue(range.contains(version), "any() should contain version 1.0.0");
    }

    @Test
    @DisplayName("any() should reject null version")
    void anyShouldRejectNull() {
      final CompatibilityRequirements.VersionRange range =
          CompatibilityRequirements.VersionRange.any();
      assertFalse(range.contains(null), "any() should not contain null version");
    }

    @Test
    @DisplayName("between() should accept versions within range")
    void betweenShouldAcceptVersionsWithinRange() {
      final WitInterfaceVersion min = new WitInterfaceVersion(1, 0, 0);
      final WitInterfaceVersion max = new WitInterfaceVersion(2, 0, 0);
      final CompatibilityRequirements.VersionRange range =
          CompatibilityRequirements.VersionRange.between(min, max);
      final WitInterfaceVersion mid = new WitInterfaceVersion(1, 5, 0);
      assertTrue(range.contains(mid), "between() should contain version within range");
      assertTrue(range.contains(min), "between() should contain minimum version (inclusive)");
      assertTrue(range.contains(max), "between() should contain maximum version (inclusive)");
    }

    @Test
    @DisplayName("atLeast() should accept versions at or above minimum")
    void atLeastShouldAcceptVersionsAboveMinimum() {
      final WitInterfaceVersion min = new WitInterfaceVersion(2, 0, 0);
      final CompatibilityRequirements.VersionRange range =
          CompatibilityRequirements.VersionRange.atLeast(min);
      assertTrue(
          range.contains(new WitInterfaceVersion(3, 0, 0)),
          "atLeast() should contain version above minimum");
      assertTrue(
          range.contains(min), "atLeast() should contain the minimum version itself");
    }

    @Test
    @DisplayName("atMost() should accept versions at or below maximum")
    void atMostShouldAcceptVersionsBelowMaximum() {
      final WitInterfaceVersion max = new WitInterfaceVersion(3, 0, 0);
      final CompatibilityRequirements.VersionRange range =
          CompatibilityRequirements.VersionRange.atMost(max);
      assertTrue(
          range.contains(new WitInterfaceVersion(1, 0, 0)),
          "atMost() should contain version below maximum");
    }

    @Test
    @DisplayName("should have getter methods for bounds")
    void shouldHaveGetterMethods() {
      final WitInterfaceVersion min = new WitInterfaceVersion(1, 0, 0);
      final WitInterfaceVersion max = new WitInterfaceVersion(2, 0, 0);
      final CompatibilityRequirements.VersionRange range =
          CompatibilityRequirements.VersionRange.between(min, max);
      assertNotNull(range.getMinimum(), "Minimum should not be null");
      assertNotNull(range.getMaximum(), "Maximum should not be null");
      assertTrue(range.isIncludeMinimum(), "includeMinimum should be true");
      assertTrue(range.isIncludeMaximum(), "includeMaximum should be true");
    }
  }

  @Nested
  @DisplayName("CompatibilityLevel enum")
  final class CompatibilityLevelTests {

    @Test
    @DisplayName("should have four levels")
    void shouldHaveFourLevels() {
      assertEquals(
          4,
          CompatibilityRequirements.CompatibilityLevel.values().length,
          "Should have 4 compatibility levels: FULL, PARTIAL, LIMITED, ANY");
    }
  }
}
