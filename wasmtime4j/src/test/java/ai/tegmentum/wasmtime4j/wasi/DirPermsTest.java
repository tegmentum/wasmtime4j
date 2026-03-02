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
package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DirPerms} class.
 *
 * <p>Verifies factory methods, bit operations, permission checks, and equals/hashCode/toString.
 */
@DisplayName("DirPerms Tests")
class DirPermsTest {

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("readOnly should have only READ permission")
    void readOnlyShouldHaveReadOnly() {
      final DirPerms perms = DirPerms.readOnly();

      assertTrue(perms.canRead(), "readOnly should allow read");
      assertFalse(perms.canMutate(), "readOnly should not allow mutate");
      assertEquals(DirPerms.READ, perms.getBits(), "readOnly bits should be READ");
    }

    @Test
    @DisplayName("all should have READ and MUTATE permissions")
    void allShouldHaveAllPermissions() {
      final DirPerms perms = DirPerms.all();

      assertTrue(perms.canRead(), "all should allow read");
      assertTrue(perms.canMutate(), "all should allow mutate");
      assertEquals(DirPerms.ALL, perms.getBits(), "all bits should be ALL");
    }

    @Test
    @DisplayName("none should have no permissions")
    void noneShouldHaveNoPermissions() {
      final DirPerms perms = DirPerms.none();

      assertFalse(perms.canRead(), "none should not allow read");
      assertFalse(perms.canMutate(), "none should not allow mutate");
      assertEquals(DirPerms.NONE, perms.getBits(), "none bits should be NONE (0)");
    }
  }

  @Nested
  @DisplayName("Bit Masking Tests")
  class BitMaskingTests {

    @Test
    @DisplayName("should mask out invalid bits")
    void shouldMaskOutInvalidBits() {
      final DirPerms perms = new DirPerms(0xFF);

      assertEquals(
          DirPerms.ALL,
          perms.getBits(),
          "Should mask out bits beyond READ|MUTATE, got: " + perms.getBits());
    }

    @Test
    @DisplayName("should allow constructing with individual bits")
    void shouldAllowIndividualBits() {
      final DirPerms readPerms = new DirPerms(DirPerms.READ);
      final DirPerms mutatePerms = new DirPerms(DirPerms.MUTATE);

      assertTrue(readPerms.canRead(), "READ bit should enable canRead");
      assertFalse(readPerms.canMutate(), "READ bit should not enable canMutate");
      assertFalse(mutatePerms.canRead(), "MUTATE bit should not enable canRead");
      assertTrue(mutatePerms.canMutate(), "MUTATE bit should enable canMutate");
    }
  }

  @Nested
  @DisplayName("Constants Tests")
  class ConstantsTests {

    @Test
    @DisplayName("READ constant should be 0x1")
    void readShouldBe1() {
      assertEquals(0x1, DirPerms.READ, "READ should be 0x1");
    }

    @Test
    @DisplayName("MUTATE constant should be 0x2")
    void mutateShouldBe2() {
      assertEquals(0x2, DirPerms.MUTATE, "MUTATE should be 0x2");
    }

    @Test
    @DisplayName("ALL should be READ | MUTATE")
    void allShouldBeReadOrMutate() {
      assertEquals(DirPerms.READ | DirPerms.MUTATE, DirPerms.ALL, "ALL should be READ | MUTATE");
    }

    @Test
    @DisplayName("NONE should be 0")
    void noneShouldBeZero() {
      assertEquals(0, DirPerms.NONE, "NONE should be 0");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsHashCodeTests {

    @Test
    @DisplayName("should be equal for same permission bits")
    void shouldBeEqualForSameBits() {
      final DirPerms perms1 = DirPerms.all();
      final DirPerms perms2 = DirPerms.all();

      assertEquals(perms1, perms2, "Same permission bits should be equal");
      assertEquals(perms1.hashCode(), perms2.hashCode(), "Equal perms should have same hash code");
    }

    @Test
    @DisplayName("should not be equal for different bits")
    void shouldNotBeEqualForDifferentBits() {
      assertNotEquals(DirPerms.readOnly(), DirPerms.all(), "Different bits should not be equal");
      assertNotEquals(DirPerms.none(), DirPerms.readOnly(), "NONE and READ should not be equal");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      assertFalse(DirPerms.all().equals(null), "Should not equal null");
    }

    @Test
    @DisplayName("should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      assertFalse(DirPerms.all().equals("not perms"), "Should not equal a String");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should show read and mutate flags")
    void shouldShowFlags() {
      final String allStr = DirPerms.all().toString();

      assertTrue(
          allStr.contains("read=true"), "all toString should show read=true, got: " + allStr);
      assertTrue(
          allStr.contains("mutate=true"), "all toString should show mutate=true, got: " + allStr);
    }

    @Test
    @DisplayName("none toString should show false flags")
    void noneShouldShowFalseFlags() {
      final String noneStr = DirPerms.none().toString();

      assertTrue(
          noneStr.contains("read=false"), "none toString should show read=false, got: " + noneStr);
      assertTrue(
          noneStr.contains("mutate=false"),
          "none toString should show mutate=false, got: " + noneStr);
    }
  }
}
