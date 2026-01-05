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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentVersion} class.
 *
 * <p>ComponentVersion represents a semantic version for WebAssembly components.
 */
@DisplayName("ComponentVersion Tests")
class ComponentVersionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentVersion.class.getModifiers()),
          "ComponentVersion should be public");
      assertTrue(
          Modifier.isFinal(ComponentVersion.class.getModifiers()),
          "ComponentVersion should be final");
    }

    @Test
    @DisplayName("should implement Comparable")
    void shouldImplementComparable() {
      assertTrue(
          Comparable.class.isAssignableFrom(ComponentVersion.class),
          "ComponentVersion should implement Comparable");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create version with major, minor, patch")
    void shouldCreateVersionWithMajorMinorPatch() {
      final ComponentVersion version = new ComponentVersion(1, 2, 3);

      assertEquals(1, version.getMajor(), "Major should be 1");
      assertEquals(2, version.getMinor(), "Minor should be 2");
      assertEquals(3, version.getPatch(), "Patch should be 3");
      assertNull(version.getPreRelease(), "PreRelease should be null");
      assertNull(version.getBuildMetadata(), "BuildMetadata should be null");
    }

    @Test
    @DisplayName("should create version with pre-release")
    void shouldCreateVersionWithPreRelease() {
      final ComponentVersion version = new ComponentVersion(1, 0, 0, "alpha");

      assertEquals(1, version.getMajor(), "Major should be 1");
      assertEquals(0, version.getMinor(), "Minor should be 0");
      assertEquals(0, version.getPatch(), "Patch should be 0");
      assertEquals("alpha", version.getPreRelease(), "PreRelease should be alpha");
      assertNull(version.getBuildMetadata(), "BuildMetadata should be null");
    }

    @Test
    @DisplayName("should create version with pre-release and build metadata")
    void shouldCreateVersionWithPreReleaseAndBuildMetadata() {
      final ComponentVersion version = new ComponentVersion(2, 1, 0, "beta.1", "build.123");

      assertEquals(2, version.getMajor(), "Major should be 2");
      assertEquals(1, version.getMinor(), "Minor should be 1");
      assertEquals(0, version.getPatch(), "Patch should be 0");
      assertEquals("beta.1", version.getPreRelease(), "PreRelease should be beta.1");
      assertEquals("build.123", version.getBuildMetadata(), "BuildMetadata should be build.123");
    }

    @Test
    @DisplayName("should throw exception for negative major version")
    void shouldThrowExceptionForNegativeMajor() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ComponentVersion(-1, 0, 0),
          "Should throw for negative major version");
    }

    @Test
    @DisplayName("should throw exception for negative minor version")
    void shouldThrowExceptionForNegativeMinor() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ComponentVersion(1, -1, 0),
          "Should throw for negative minor version");
    }

    @Test
    @DisplayName("should throw exception for negative patch version")
    void shouldThrowExceptionForNegativePatch() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ComponentVersion(1, 0, -1),
          "Should throw for negative patch version");
    }

    @Test
    @DisplayName("should allow zero versions")
    void shouldAllowZeroVersions() {
      final ComponentVersion version = new ComponentVersion(0, 0, 0);

      assertEquals(0, version.getMajor(), "Major should be 0");
      assertEquals(0, version.getMinor(), "Minor should be 0");
      assertEquals(0, version.getPatch(), "Patch should be 0");
    }
  }

  @Nested
  @DisplayName("Parse Tests")
  class ParseTests {

    @Test
    @DisplayName("should parse simple version")
    void shouldParseSimpleVersion() {
      final ComponentVersion version = ComponentVersion.parse("1.2.3");

      assertEquals(1, version.getMajor(), "Major should be 1");
      assertEquals(2, version.getMinor(), "Minor should be 2");
      assertEquals(3, version.getPatch(), "Patch should be 3");
    }

    @Test
    @DisplayName("should parse version with pre-release")
    void shouldParseVersionWithPreRelease() {
      final ComponentVersion version = ComponentVersion.parse("1.0.0-alpha");

      assertEquals(1, version.getMajor(), "Major should be 1");
      assertEquals("alpha", version.getPreRelease(), "PreRelease should be alpha");
    }

    @Test
    @DisplayName("should parse version with pre-release and build metadata")
    void shouldParseVersionWithPreReleaseAndBuildMetadata() {
      final ComponentVersion version = ComponentVersion.parse("1.0.0-beta+build.123");

      assertEquals("beta", version.getPreRelease(), "PreRelease should be beta");
      assertEquals("build.123", version.getBuildMetadata(), "BuildMetadata should be build.123");
    }

    @Test
    @DisplayName("should parse version with only build metadata")
    void shouldParseVersionWithOnlyBuildMetadata() {
      final ComponentVersion version = ComponentVersion.parse("1.0.0+build.456");

      assertNull(version.getPreRelease(), "PreRelease should be null");
      assertEquals("build.456", version.getBuildMetadata(), "BuildMetadata should be build.456");
    }

    @Test
    @DisplayName("should throw exception for null version string")
    void shouldThrowExceptionForNullVersionString() {
      assertThrows(
          NullPointerException.class,
          () -> ComponentVersion.parse(null),
          "Should throw for null version string");
    }

    @Test
    @DisplayName("should throw exception for invalid version string")
    void shouldThrowExceptionForInvalidVersionString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentVersion.parse("invalid"),
          "Should throw for invalid version string");
    }

    @Test
    @DisplayName("should throw exception for version string exceeding max length")
    void shouldThrowExceptionForTooLongVersionString() {
      final String longVersion = "1.0.0-" + "a".repeat(260);
      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentVersion.parse(longVersion),
          "Should throw for version string exceeding max length");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getMajor should return major version")
    void getMajorShouldReturnMajorVersion() {
      final ComponentVersion version = new ComponentVersion(5, 3, 1);
      assertEquals(5, version.getMajor(), "Should return correct major version");
    }

    @Test
    @DisplayName("getMinor should return minor version")
    void getMinorShouldReturnMinorVersion() {
      final ComponentVersion version = new ComponentVersion(5, 3, 1);
      assertEquals(3, version.getMinor(), "Should return correct minor version");
    }

    @Test
    @DisplayName("getPatch should return patch version")
    void getPatchShouldReturnPatchVersion() {
      final ComponentVersion version = new ComponentVersion(5, 3, 1);
      assertEquals(1, version.getPatch(), "Should return correct patch version");
    }

    @Test
    @DisplayName("getPreRelease should return pre-release")
    void getPreReleaseShouldReturnPreRelease() {
      final ComponentVersion version = new ComponentVersion(1, 0, 0, "rc.1");
      assertEquals("rc.1", version.getPreRelease(), "Should return correct pre-release");
    }

    @Test
    @DisplayName("getBuildMetadata should return build metadata")
    void getBuildMetadataShouldReturnBuildMetadata() {
      final ComponentVersion version = new ComponentVersion(1, 0, 0, null, "20240101");
      assertEquals("20240101", version.getBuildMetadata(), "Should return correct build metadata");
    }
  }

  @Nested
  @DisplayName("isPreRelease Tests")
  class IsPreReleaseTests {

    @Test
    @DisplayName("should return true for pre-release version")
    void shouldReturnTrueForPreReleaseVersion() {
      final ComponentVersion version = new ComponentVersion(1, 0, 0, "alpha");
      assertTrue(version.isPreRelease(), "Should be a pre-release version");
    }

    @Test
    @DisplayName("should return false for release version")
    void shouldReturnFalseForReleaseVersion() {
      final ComponentVersion version = new ComponentVersion(1, 0, 0);
      assertFalse(version.isPreRelease(), "Should not be a pre-release version");
    }

    @Test
    @DisplayName("should return false for empty pre-release")
    void shouldReturnFalseForEmptyPreRelease() {
      final ComponentVersion version = new ComponentVersion(1, 0, 0, "");
      assertFalse(version.isPreRelease(), "Should not be a pre-release version with empty string");
    }
  }

  @Nested
  @DisplayName("isCompatibleWith Tests")
  class IsCompatibleWithTests {

    @Test
    @DisplayName("should be compatible with same major version")
    void shouldBeCompatibleWithSameMajorVersion() {
      final ComponentVersion v1 = new ComponentVersion(2, 1, 0);
      final ComponentVersion v2 = new ComponentVersion(2, 0, 0);

      assertTrue(v1.isCompatibleWith(v2), "2.1.0 should be compatible with 2.0.0");
    }

    @Test
    @DisplayName("should not be compatible with different major version")
    void shouldNotBeCompatibleWithDifferentMajorVersion() {
      final ComponentVersion v1 = new ComponentVersion(2, 0, 0);
      final ComponentVersion v2 = new ComponentVersion(1, 0, 0);

      assertFalse(v1.isCompatibleWith(v2), "2.0.0 should not be compatible with 1.0.0");
    }

    @Test
    @DisplayName("should not be compatible with higher version")
    void shouldNotBeCompatibleWithHigherVersion() {
      final ComponentVersion v1 = new ComponentVersion(2, 0, 0);
      final ComponentVersion v2 = new ComponentVersion(2, 1, 0);

      assertFalse(v1.isCompatibleWith(v2), "2.0.0 should not be compatible with 2.1.0");
    }
  }

  @Nested
  @DisplayName("isBackwardCompatibleWith Tests")
  class IsBackwardCompatibleWithTests {

    @Test
    @DisplayName("should be backward compatible with lower minor version")
    void shouldBeBackwardCompatibleWithLowerMinorVersion() {
      final ComponentVersion v1 = new ComponentVersion(2, 2, 0);
      final ComponentVersion v2 = new ComponentVersion(2, 1, 0);

      assertTrue(v1.isBackwardCompatibleWith(v2), "2.2.0 should be backward compatible with 2.1.0");
    }

    @Test
    @DisplayName("should be backward compatible with lower patch version")
    void shouldBeBackwardCompatibleWithLowerPatchVersion() {
      final ComponentVersion v1 = new ComponentVersion(2, 1, 5);
      final ComponentVersion v2 = new ComponentVersion(2, 1, 3);

      assertTrue(v1.isBackwardCompatibleWith(v2), "2.1.5 should be backward compatible with 2.1.3");
    }

    @Test
    @DisplayName("should not be backward compatible with different major version")
    void shouldNotBeBackwardCompatibleWithDifferentMajorVersion() {
      final ComponentVersion v1 = new ComponentVersion(3, 0, 0);
      final ComponentVersion v2 = new ComponentVersion(2, 0, 0);

      assertFalse(
          v1.isBackwardCompatibleWith(v2), "3.0.0 should not be backward compatible with 2.0.0");
    }
  }

  @Nested
  @DisplayName("nextMajor Tests")
  class NextMajorTests {

    @Test
    @DisplayName("should increment major version")
    void shouldIncrementMajorVersion() {
      final ComponentVersion version = new ComponentVersion(1, 2, 3);
      final ComponentVersion next = version.nextMajor();

      assertEquals(2, next.getMajor(), "Major should be 2");
      assertEquals(0, next.getMinor(), "Minor should be 0");
      assertEquals(0, next.getPatch(), "Patch should be 0");
    }
  }

  @Nested
  @DisplayName("nextMinor Tests")
  class NextMinorTests {

    @Test
    @DisplayName("should increment minor version")
    void shouldIncrementMinorVersion() {
      final ComponentVersion version = new ComponentVersion(1, 2, 3);
      final ComponentVersion next = version.nextMinor();

      assertEquals(1, next.getMajor(), "Major should be 1");
      assertEquals(3, next.getMinor(), "Minor should be 3");
      assertEquals(0, next.getPatch(), "Patch should be 0");
    }
  }

  @Nested
  @DisplayName("nextPatch Tests")
  class NextPatchTests {

    @Test
    @DisplayName("should increment patch version")
    void shouldIncrementPatchVersion() {
      final ComponentVersion version = new ComponentVersion(1, 2, 3);
      final ComponentVersion next = version.nextPatch();

      assertEquals(1, next.getMajor(), "Major should be 1");
      assertEquals(2, next.getMinor(), "Minor should be 2");
      assertEquals(4, next.getPatch(), "Patch should be 4");
    }
  }

  @Nested
  @DisplayName("compareTo Tests")
  class CompareToTests {

    @Test
    @DisplayName("should compare major versions")
    void shouldCompareMajorVersions() {
      final ComponentVersion v1 = new ComponentVersion(2, 0, 0);
      final ComponentVersion v2 = new ComponentVersion(1, 0, 0);

      assertTrue(v1.compareTo(v2) > 0, "2.0.0 should be greater than 1.0.0");
      assertTrue(v2.compareTo(v1) < 0, "1.0.0 should be less than 2.0.0");
    }

    @Test
    @DisplayName("should compare minor versions")
    void shouldCompareMinorVersions() {
      final ComponentVersion v1 = new ComponentVersion(1, 2, 0);
      final ComponentVersion v2 = new ComponentVersion(1, 1, 0);

      assertTrue(v1.compareTo(v2) > 0, "1.2.0 should be greater than 1.1.0");
    }

    @Test
    @DisplayName("should compare patch versions")
    void shouldComparePatchVersions() {
      final ComponentVersion v1 = new ComponentVersion(1, 1, 2);
      final ComponentVersion v2 = new ComponentVersion(1, 1, 1);

      assertTrue(v1.compareTo(v2) > 0, "1.1.2 should be greater than 1.1.1");
    }

    @Test
    @DisplayName("should compare pre-release versions")
    void shouldComparePreReleaseVersions() {
      final ComponentVersion release = new ComponentVersion(1, 0, 0);
      final ComponentVersion preRelease = new ComponentVersion(1, 0, 0, "alpha");

      assertTrue(release.compareTo(preRelease) > 0, "1.0.0 should be greater than 1.0.0-alpha");
    }

    @Test
    @DisplayName("should return zero for equal versions")
    void shouldReturnZeroForEqualVersions() {
      final ComponentVersion v1 = new ComponentVersion(1, 0, 0);
      final ComponentVersion v2 = new ComponentVersion(1, 0, 0);

      assertEquals(0, v1.compareTo(v2), "Equal versions should compare to 0");
    }
  }

  @Nested
  @DisplayName("Equals Tests")
  class EqualsTests {

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final ComponentVersion version = new ComponentVersion(1, 0, 0);
      assertEquals(version, version, "Should be equal to itself");
    }

    @Test
    @DisplayName("should be equal to same version")
    void shouldBeEqualToSameVersion() {
      final ComponentVersion v1 = new ComponentVersion(1, 2, 3);
      final ComponentVersion v2 = new ComponentVersion(1, 2, 3);

      assertEquals(v1, v2, "Should be equal to same version");
    }

    @Test
    @DisplayName("should not be equal to different version")
    void shouldNotBeEqualToDifferentVersion() {
      final ComponentVersion v1 = new ComponentVersion(1, 0, 0);
      final ComponentVersion v2 = new ComponentVersion(2, 0, 0);

      assertNotEquals(v1, v2, "Should not be equal to different version");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final ComponentVersion version = new ComponentVersion(1, 0, 0);
      assertNotEquals(null, version, "Should not be equal to null");
    }

    @Test
    @DisplayName("should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      final ComponentVersion version = new ComponentVersion(1, 0, 0);
      assertNotEquals("1.0.0", version, "Should not be equal to String");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("should have same hashCode for equal objects")
    void shouldHaveSameHashCodeForEqualObjects() {
      final ComponentVersion v1 = new ComponentVersion(1, 2, 3);
      final ComponentVersion v2 = new ComponentVersion(1, 2, 3);

      assertEquals(v1.hashCode(), v2.hashCode(), "Hash codes should match for equal objects");
    }

    @Test
    @DisplayName("should have consistent hashCode")
    void shouldHaveConsistentHashCode() {
      final ComponentVersion version = new ComponentVersion(1, 0, 0);

      final int hash1 = version.hashCode();
      final int hash2 = version.hashCode();

      assertEquals(hash1, hash2, "Hash code should be consistent");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return simple version string")
    void shouldReturnSimpleVersionString() {
      final ComponentVersion version = new ComponentVersion(1, 2, 3);
      assertEquals("1.2.3", version.toString(), "Should return 1.2.3");
    }

    @Test
    @DisplayName("should return version string with pre-release")
    void shouldReturnVersionStringWithPreRelease() {
      final ComponentVersion version = new ComponentVersion(1, 0, 0, "alpha");
      assertEquals("1.0.0-alpha", version.toString(), "Should return 1.0.0-alpha");
    }

    @Test
    @DisplayName("should return version string with pre-release and build metadata")
    void shouldReturnVersionStringWithPreReleaseAndBuildMetadata() {
      final ComponentVersion version = new ComponentVersion(1, 0, 0, "beta", "build.123");
      assertEquals("1.0.0-beta+build.123", version.toString(), "Should include build metadata");
    }
  }
}
