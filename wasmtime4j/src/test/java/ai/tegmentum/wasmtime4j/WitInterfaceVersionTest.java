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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wit.WitInterfaceVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitInterfaceVersion} class.
 *
 * <p>WitInterfaceVersion represents a semantic version for WIT interfaces.
 */
@DisplayName("WitInterfaceVersion Tests")
class WitInterfaceVersionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create version with major, minor, patch")
    void shouldCreateVersionWithMajorMinorPatch() {
      final WitInterfaceVersion version = new WitInterfaceVersion(1, 2, 3);

      assertEquals(1, version.getMajor());
      assertEquals(2, version.getMinor());
      assertEquals(3, version.getPatch());
      assertNull(version.getPreRelease());
    }

    @Test
    @DisplayName("should create version with pre-release")
    void shouldCreateVersionWithPreRelease() {
      final WitInterfaceVersion version = new WitInterfaceVersion(1, 0, 0, "alpha");

      assertEquals(1, version.getMajor());
      assertEquals(0, version.getMinor());
      assertEquals(0, version.getPatch());
      assertEquals("alpha", version.getPreRelease());
    }

    @Test
    @DisplayName("should handle null pre-release")
    void shouldHandleNullPreRelease() {
      final WitInterfaceVersion version = new WitInterfaceVersion(1, 0, 0, null);

      assertNull(version.getPreRelease());
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getMajor should return major version")
    void getMajorShouldReturnMajorVersion() {
      final WitInterfaceVersion version = new WitInterfaceVersion(2, 1, 0);
      assertEquals(2, version.getMajor());
    }

    @Test
    @DisplayName("getMinor should return minor version")
    void getMinorShouldReturnMinorVersion() {
      final WitInterfaceVersion version = new WitInterfaceVersion(1, 5, 0);
      assertEquals(5, version.getMinor());
    }

    @Test
    @DisplayName("getPatch should return patch version")
    void getPatchShouldReturnPatchVersion() {
      final WitInterfaceVersion version = new WitInterfaceVersion(1, 0, 7);
      assertEquals(7, version.getPatch());
    }

    @Test
    @DisplayName("getVersion should return formatted string")
    void getVersionShouldReturnFormattedString() {
      final WitInterfaceVersion version = new WitInterfaceVersion(1, 2, 3);
      assertEquals("1.2.3", version.getVersion());
    }

    @Test
    @DisplayName("getInterfaceName should return unknown by default")
    void getInterfaceNameShouldReturnUnknown() {
      final WitInterfaceVersion version = new WitInterfaceVersion(1, 0, 0);
      assertEquals("unknown", version.getInterfaceName());
    }

    @Test
    @DisplayName("getInterface should return null by default")
    void getInterfaceShouldReturnNull() {
      final WitInterfaceVersion version = new WitInterfaceVersion(1, 0, 0);
      assertNull(version.getInterface());
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should format version without pre-release")
    void toStringShouldFormatVersionWithoutPreRelease() {
      final WitInterfaceVersion version = new WitInterfaceVersion(1, 2, 3);
      assertEquals("1.2.3", version.toString());
    }

    @Test
    @DisplayName("toString should format version with pre-release")
    void toStringShouldFormatVersionWithPreRelease() {
      final WitInterfaceVersion version = new WitInterfaceVersion(1, 0, 0, "beta.1");
      assertEquals("1.0.0-beta.1", version.toString());
    }

    @Test
    @DisplayName("toString should handle zero versions")
    void toStringShouldHandleZeroVersions() {
      final WitInterfaceVersion version = new WitInterfaceVersion(0, 0, 0);
      assertEquals("0.0.0", version.toString());
    }
  }

  @Nested
  @DisplayName("Compatibility Tests")
  class CompatibilityTests {

    @Test
    @DisplayName("same major versions should be compatible")
    void sameMajorVersionsShouldBeCompatible() {
      final WitInterfaceVersion v1 = new WitInterfaceVersion(1, 0, 0);
      final WitInterfaceVersion v2 = new WitInterfaceVersion(1, 5, 0);

      assertTrue(v1.isCompatibleWith(v2));
      assertTrue(v2.isCompatibleWith(v1));
    }

    @Test
    @DisplayName("different major versions should not be compatible")
    void differentMajorVersionsShouldNotBeCompatible() {
      final WitInterfaceVersion v1 = new WitInterfaceVersion(1, 0, 0);
      final WitInterfaceVersion v2 = new WitInterfaceVersion(2, 0, 0);

      assertFalse(v1.isCompatibleWith(v2));
      assertFalse(v2.isCompatibleWith(v1));
    }

    @Test
    @DisplayName("pre-release should not affect compatibility")
    void preReleaseShouldNotAffectCompatibility() {
      final WitInterfaceVersion v1 = new WitInterfaceVersion(1, 0, 0, "alpha");
      final WitInterfaceVersion v2 = new WitInterfaceVersion(1, 0, 0, "beta");

      assertTrue(v1.isCompatibleWith(v2));
    }
  }

  @Nested
  @DisplayName("equals and hashCode Tests")
  class EqualsHashCodeTests {

    @Test
    @DisplayName("equal versions should be equal")
    void equalVersionsShouldBeEqual() {
      final WitInterfaceVersion v1 = new WitInterfaceVersion(1, 2, 3);
      final WitInterfaceVersion v2 = new WitInterfaceVersion(1, 2, 3);

      assertEquals(v1, v2);
      assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    @DisplayName("equal versions with pre-release should be equal")
    void equalVersionsWithPreReleaseShouldBeEqual() {
      final WitInterfaceVersion v1 = new WitInterfaceVersion(1, 0, 0, "alpha");
      final WitInterfaceVersion v2 = new WitInterfaceVersion(1, 0, 0, "alpha");

      assertEquals(v1, v2);
      assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    @DisplayName("different versions should not be equal")
    void differentVersionsShouldNotBeEqual() {
      final WitInterfaceVersion v1 = new WitInterfaceVersion(1, 2, 3);
      final WitInterfaceVersion v2 = new WitInterfaceVersion(1, 2, 4);

      assertNotEquals(v1, v2);
    }

    @Test
    @DisplayName("different pre-release should not be equal")
    void differentPreReleaseShouldNotBeEqual() {
      final WitInterfaceVersion v1 = new WitInterfaceVersion(1, 0, 0, "alpha");
      final WitInterfaceVersion v2 = new WitInterfaceVersion(1, 0, 0, "beta");

      assertNotEquals(v1, v2);
    }

    @Test
    @DisplayName("null should not be equal")
    void nullShouldNotBeEqual() {
      final WitInterfaceVersion v1 = new WitInterfaceVersion(1, 0, 0);

      assertNotEquals(v1, null);
    }

    @Test
    @DisplayName("same instance should be equal")
    void sameInstanceShouldBeEqual() {
      final WitInterfaceVersion v1 = new WitInterfaceVersion(1, 0, 0);

      assertEquals(v1, v1);
    }
  }

  @Nested
  @DisplayName("compareTo Tests")
  class CompareToTests {

    @Test
    @DisplayName("higher major should compare greater")
    void higherMajorShouldCompareGreater() {
      final WitInterfaceVersion v1 = new WitInterfaceVersion(2, 0, 0);
      final WitInterfaceVersion v2 = new WitInterfaceVersion(1, 0, 0);

      assertTrue(v1.compareTo(v2) > 0);
      assertTrue(v2.compareTo(v1) < 0);
    }

    @Test
    @DisplayName("higher minor should compare greater")
    void higherMinorShouldCompareGreater() {
      final WitInterfaceVersion v1 = new WitInterfaceVersion(1, 5, 0);
      final WitInterfaceVersion v2 = new WitInterfaceVersion(1, 3, 0);

      assertTrue(v1.compareTo(v2) > 0);
    }

    @Test
    @DisplayName("higher patch should compare greater")
    void higherPatchShouldCompareGreater() {
      final WitInterfaceVersion v1 = new WitInterfaceVersion(1, 0, 5);
      final WitInterfaceVersion v2 = new WitInterfaceVersion(1, 0, 3);

      assertTrue(v1.compareTo(v2) > 0);
    }

    @Test
    @DisplayName("equal versions should compare equal")
    void equalVersionsShouldCompareEqual() {
      final WitInterfaceVersion v1 = new WitInterfaceVersion(1, 2, 3);
      final WitInterfaceVersion v2 = new WitInterfaceVersion(1, 2, 3);

      assertEquals(0, v1.compareTo(v2));
    }

    @Test
    @DisplayName("release version should be greater than pre-release")
    void releaseVersionShouldBeGreaterThanPreRelease() {
      final WitInterfaceVersion release = new WitInterfaceVersion(1, 0, 0);
      final WitInterfaceVersion preRelease = new WitInterfaceVersion(1, 0, 0, "alpha");

      assertTrue(release.compareTo(preRelease) > 0);
      assertTrue(preRelease.compareTo(release) < 0);
    }

    @Test
    @DisplayName("pre-release versions should compare lexicographically")
    void preReleaseVersionsShouldCompareLexicographically() {
      final WitInterfaceVersion alpha = new WitInterfaceVersion(1, 0, 0, "alpha");
      final WitInterfaceVersion beta = new WitInterfaceVersion(1, 0, 0, "beta");

      assertTrue(alpha.compareTo(beta) < 0);
      assertTrue(beta.compareTo(alpha) > 0);
    }
  }

  @Nested
  @DisplayName("Comparable Contract Tests")
  class ComparableContractTests {

    @Test
    @DisplayName("should implement Comparable interface")
    void shouldImplementComparable() {
      assertTrue(Comparable.class.isAssignableFrom(WitInterfaceVersion.class));
    }

    @Test
    @DisplayName("compareTo should be consistent with equals")
    void compareToShouldBeConsistentWithEquals() {
      final WitInterfaceVersion v1 = new WitInterfaceVersion(1, 2, 3);
      final WitInterfaceVersion v2 = new WitInterfaceVersion(1, 2, 3);

      assertEquals(v1.equals(v2), v1.compareTo(v2) == 0);
    }
  }
}
