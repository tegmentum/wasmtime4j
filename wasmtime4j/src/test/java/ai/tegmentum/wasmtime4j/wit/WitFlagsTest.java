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

package ai.tegmentum.wasmtime4j.wit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WitType;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitFlags} class.
 *
 * <p>WitFlags represents a WIT flags value (bitset type) with named boolean flags.
 */
@DisplayName("WitFlags Tests")
class WitFlagsTest {

  private WitType createFlagsType() {
    return WitType.flags("permissions", Arrays.asList("read", "write", "execute"));
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WitFlags.class.getModifiers()), "WitFlags should be final");
    }

    @Test
    @DisplayName("should extend WitValue")
    void shouldExtendWitValue() {
      assertTrue(
          WitValue.class.isAssignableFrom(WitFlags.class),
          "WitFlags should extend WitValue");
    }
  }

  @Nested
  @DisplayName("Creation Tests")
  class CreationTests {

    @Test
    @DisplayName("of with flag names should create flags")
    void ofWithFlagNamesShouldCreateFlags() {
      final WitType ft = createFlagsType();
      final WitFlags flags = WitFlags.of(ft, "read", "write");
      assertNotNull(flags, "WitFlags.of should create non-null flags");
      assertEquals(2, flags.size(), "Two flags should be set");
    }

    @Test
    @DisplayName("of with Set should create flags")
    void ofWithSetShouldCreateFlags() {
      final WitType ft = createFlagsType();
      final WitFlags flags = WitFlags.of(ft, Set.of("read"));
      assertNotNull(flags, "WitFlags.of(Set) should create non-null flags");
      assertEquals(1, flags.size(), "One flag should be set");
    }

    @Test
    @DisplayName("empty should create flags with no flags set")
    void emptyShouldCreateEmptyFlags() {
      final WitType ft = createFlagsType();
      final WitFlags flags = WitFlags.empty(ft);
      assertNotNull(flags, "WitFlags.empty should create non-null flags");
      assertTrue(flags.isEmpty(), "Empty flags should be empty");
      assertEquals(0, flags.size(), "Empty flags size should be 0");
    }

    @Test
    @DisplayName("builder should create flags")
    void builderShouldCreateFlags() {
      final WitType ft = createFlagsType();
      final WitFlags flags = WitFlags.builder(ft)
          .set("read")
          .set("execute")
          .build();
      assertNotNull(flags, "Builder should create non-null flags");
      assertEquals(2, flags.size(), "Builder flags size should be 2");
    }

    @Test
    @DisplayName("builder setAll should set multiple flags")
    void builderSetAllShouldSetMultiple() {
      final WitType ft = createFlagsType();
      final WitFlags flags = WitFlags.builder(ft)
          .setAll("read", "write", "execute")
          .build();
      assertEquals(3, flags.size(), "setAll should set all three flags");
    }

    @Test
    @DisplayName("of with null set should throw")
    void ofWithNullSetShouldThrow() {
      final WitType ft = createFlagsType();
      assertThrows(
          IllegalArgumentException.class,
          () -> WitFlags.of(ft, (Set<String>) null),
          "of with null set should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Flag Access Tests")
  class FlagAccessTests {

    @Test
    @DisplayName("isSet should return true for set flag")
    void isSetShouldReturnTrueForSetFlag() {
      final WitType ft = createFlagsType();
      final WitFlags flags = WitFlags.of(ft, "read", "write");
      assertTrue(flags.isSet("read"), "read should be set");
      assertTrue(flags.isSet("write"), "write should be set");
    }

    @Test
    @DisplayName("isSet should return false for unset flag")
    void isSetShouldReturnFalseForUnsetFlag() {
      final WitType ft = createFlagsType();
      final WitFlags flags = WitFlags.of(ft, "read");
      assertFalse(flags.isSet("execute"), "execute should not be set");
    }

    @Test
    @DisplayName("getSetFlags should return unmodifiable set")
    void getSetFlagsShouldReturnUnmodifiable() {
      final WitType ft = createFlagsType();
      final WitFlags flags = WitFlags.of(ft, "read");
      final Set<String> setFlags = flags.getSetFlags();
      assertThrows(
          UnsupportedOperationException.class,
          () -> setFlags.add("write"),
          "getSetFlags should return unmodifiable set");
    }
  }

  @Nested
  @DisplayName("ToJava Tests")
  class ToJavaTests {

    @Test
    @DisplayName("toJava should return Set")
    void toJavaShouldReturnSet() {
      final WitType ft = createFlagsType();
      final WitFlags flags = WitFlags.of(ft, "read");
      final Object javaValue = flags.toJava();
      assertTrue(javaValue instanceof Set, "toJava should return a Set");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same flags should be equal")
    void sameFlagsShouldBeEqual() {
      final WitType ft = createFlagsType();
      final WitFlags f1 = WitFlags.of(ft, "read", "write");
      final WitFlags f2 = WitFlags.of(ft, "read", "write");
      assertEquals(f1, f2, "Flags with same set flags should be equal");
    }

    @Test
    @DisplayName("different flags should not be equal")
    void differentFlagsShouldNotBeEqual() {
      final WitType ft = createFlagsType();
      final WitFlags f1 = WitFlags.of(ft, "read");
      final WitFlags f2 = WitFlags.of(ft, "write");
      assertNotEquals(f1, f2, "Flags with different set flags should not be equal");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same flags should have same hash code")
    void sameFlagsShouldHaveSameHashCode() {
      final WitType ft = createFlagsType();
      final WitFlags f1 = WitFlags.of(ft, "read", "write");
      final WitFlags f2 = WitFlags.of(ft, "read", "write");
      assertEquals(
          f1.hashCode(), f2.hashCode(),
          "Same flags should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain WitFlags")
    void toStringShouldContainClassName() {
      final WitType ft = createFlagsType();
      final WitFlags flags = WitFlags.of(ft, "read");
      final String str = flags.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("WitFlags"), "toString should contain 'WitFlags'");
    }
  }

  @Nested
  @DisplayName("Type Tests")
  class TypeTests {

    @Test
    @DisplayName("should have WitType")
    void shouldHaveWitType() {
      final WitType ft = createFlagsType();
      final WitFlags flags = WitFlags.of(ft, "read");
      assertNotNull(flags.getType(), "Should have WitType");
    }
  }
}
