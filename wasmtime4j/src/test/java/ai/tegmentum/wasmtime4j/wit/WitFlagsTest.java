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

  @Nested
  @DisplayName("Mutation Killing Tests")
  class MutationKillingTests {

    @Test
    @DisplayName("isEmpty must return true for empty and false for non-empty")
    void isEmptyMutationTest() {
      final WitType ft = createFlagsType();

      // Empty flags must return true for isEmpty()
      final WitFlags emptyFlags = WitFlags.empty(ft);
      assertTrue(emptyFlags.isEmpty(), "isEmpty() on empty flags must return exactly true");
      assertFalse(!emptyFlags.isEmpty(), "isEmpty() result must be true, not false");

      // Non-empty flags must return false for isEmpty()
      final WitFlags nonEmpty = WitFlags.of(ft, "read");
      assertFalse(nonEmpty.isEmpty(), "isEmpty() on non-empty flags must return exactly false");
      assertTrue(!nonEmpty.isEmpty(), "isEmpty() result must be false, not true");
    }

    @Test
    @DisplayName("isSet must return true for set flag and false for unset flag")
    void isSetMutationTest() {
      final WitType ft = createFlagsType();
      final WitFlags flags = WitFlags.of(ft, "read", "write");

      // Set flag must return true
      assertTrue(flags.isSet("read"), "isSet() for set flag must return exactly true");
      assertFalse(!flags.isSet("read"), "isSet() for set flag must not return false");
      assertTrue(flags.isSet("write"), "isSet() for set flag must return exactly true");

      // Unset flag must return false
      assertFalse(flags.isSet("execute"), "isSet() for unset flag must return exactly false");
      assertTrue(!flags.isSet("execute"), "isSet() for unset flag must not return true");
    }

    @Test
    @DisplayName("size must return exact count - 0, 1, and multiple")
    void sizeMutationTest() {
      final WitType ft = createFlagsType();

      // Empty flags: size must be exactly 0
      final WitFlags empty = WitFlags.empty(ft);
      assertEquals(0, empty.size(), "Empty flags size must be exactly 0");
      assertTrue(empty.size() == 0, "Empty flags size == 0 must be true");
      assertFalse(empty.size() != 0, "Empty flags size != 0 must be false");

      // One flag: size must be exactly 1
      final WitFlags one = WitFlags.of(ft, "read");
      assertEquals(1, one.size(), "One flag size must be exactly 1");
      assertTrue(one.size() == 1, "One flag size == 1 must be true");
      assertFalse(one.size() == 0, "One flag size == 0 must be false");

      // Three flags: size must be exactly 3
      final WitFlags three = WitFlags.of(ft, "read", "write", "execute");
      assertEquals(3, three.size(), "Three flags size must be exactly 3");
      assertTrue(three.size() == 3, "Three flags size == 3 must be true");
    }

    @Test
    @DisplayName("validation must reject empty flag names")
    void validationEmptyFlagNameMutationTest() {
      final WitType ft = createFlagsType();

      // Empty string flag name must be rejected
      assertThrows(
          IllegalArgumentException.class,
          () -> WitFlags.of(ft, ""),
          "Empty flag name must be rejected");

      // Null flag in varargs must be rejected
      assertThrows(
          IllegalArgumentException.class,
          () -> WitFlags.of(ft, new String[] {null}),
          "Null flag name must be rejected");
    }

    @Test
    @DisplayName("validation must reject invalid flag names")
    void validationInvalidFlagNameMutationTest() {
      final WitType ft = createFlagsType();

      // Invalid flag name not in type must be rejected
      assertThrows(
          IllegalArgumentException.class,
          () -> WitFlags.of(ft, "invalid_flag"),
          "Flag name not in type must be rejected");

      // Mix of valid and invalid must be rejected
      assertThrows(
          IllegalArgumentException.class,
          () -> WitFlags.of(ft, "read", "nonexistent"),
          "Invalid flag in mix must be rejected");
    }

    @Test
    @DisplayName("builder set must reject null and empty flag names")
    void builderValidationMutationTest() {
      final WitType ft = createFlagsType();

      // Builder with null flag must throw
      assertThrows(
          IllegalArgumentException.class,
          () -> WitFlags.builder(ft).set(null),
          "Builder.set(null) must throw IllegalArgumentException");

      // Builder with empty flag must throw
      assertThrows(
          IllegalArgumentException.class,
          () -> WitFlags.builder(ft).set(""),
          "Builder.set(\"\") must throw IllegalArgumentException");

      // Builder with null type must throw
      assertThrows(
          IllegalArgumentException.class,
          () -> WitFlags.builder(null),
          "Builder(null) must throw IllegalArgumentException");
    }

    @Test
    @DisplayName("toJava must return mutable copy of set flags")
    void toJavaMutationTest() {
      final WitType ft = createFlagsType();
      final WitFlags flags = WitFlags.of(ft, "read", "write");

      // toJava must return a Set containing the same flags
      final Set<String> javaSet = flags.toJava();
      assertEquals(2, javaSet.size(), "toJava Set must have same size");
      assertTrue(javaSet.contains("read"), "toJava Set must contain 'read'");
      assertTrue(javaSet.contains("write"), "toJava Set must contain 'write'");

      // toJava must return a mutable copy (not the internal set)
      javaSet.add("execute");
      assertFalse(flags.isSet("execute"), "Modifying toJava result must not affect original");
    }

    @Test
    @DisplayName("equals must handle edge cases correctly")
    void equalsMutationTest() {
      final WitType ft = createFlagsType();
      final WitFlags flags = WitFlags.of(ft, "read");

      // Reflexive - same object
      assertTrue(flags.equals(flags), "equals(self) must return true");

      // Null comparison
      assertFalse(flags.equals(null), "equals(null) must return false");

      // Different type
      assertFalse(flags.equals("read"), "equals(String) must return false");
      assertFalse(flags.equals(42), "equals(Integer) must return false");

      // Empty vs non-empty
      final WitFlags empty = WitFlags.empty(ft);
      assertFalse(flags.equals(empty), "non-empty.equals(empty) must return false");
      assertFalse(empty.equals(flags), "empty.equals(non-empty) must return false");
    }
  }
}
