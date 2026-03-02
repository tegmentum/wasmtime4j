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
 * Tests for {@link FilePerms} class.
 *
 * <p>Verifies factory methods, bit operations, permission checks, and equals/hashCode/toString.
 */
@DisplayName("FilePerms Tests")
class FilePermsTest {

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("readOnly should have only READ permission")
    void readOnlyShouldHaveReadOnly() {
      final FilePerms perms = FilePerms.readOnly();

      assertTrue(perms.canRead(), "readOnly should allow read");
      assertFalse(perms.canWrite(), "readOnly should not allow write");
      assertEquals(FilePerms.READ, perms.getBits(), "readOnly bits should be READ");
    }

    @Test
    @DisplayName("all should have READ and WRITE permissions")
    void allShouldHaveAllPermissions() {
      final FilePerms perms = FilePerms.all();

      assertTrue(perms.canRead(), "all should allow read");
      assertTrue(perms.canWrite(), "all should allow write");
      assertEquals(FilePerms.ALL, perms.getBits(), "all bits should be ALL");
    }

    @Test
    @DisplayName("none should have no permissions")
    void noneShouldHaveNoPermissions() {
      final FilePerms perms = FilePerms.none();

      assertFalse(perms.canRead(), "none should not allow read");
      assertFalse(perms.canWrite(), "none should not allow write");
      assertEquals(FilePerms.NONE, perms.getBits(), "none bits should be NONE (0)");
    }
  }

  @Nested
  @DisplayName("Bit Masking Tests")
  class BitMaskingTests {

    @Test
    @DisplayName("should mask out invalid bits")
    void shouldMaskOutInvalidBits() {
      final FilePerms perms = new FilePerms(0xFF);

      assertEquals(
          FilePerms.ALL,
          perms.getBits(),
          "Should mask out bits beyond READ|WRITE, got: " + perms.getBits());
    }

    @Test
    @DisplayName("should allow constructing with individual bits")
    void shouldAllowIndividualBits() {
      final FilePerms readPerms = new FilePerms(FilePerms.READ);
      final FilePerms writePerms = new FilePerms(FilePerms.WRITE);

      assertTrue(readPerms.canRead(), "READ bit should enable canRead");
      assertFalse(readPerms.canWrite(), "READ bit should not enable canWrite");
      assertFalse(writePerms.canRead(), "WRITE bit should not enable canRead");
      assertTrue(writePerms.canWrite(), "WRITE bit should enable canWrite");
    }
  }

  @Nested
  @DisplayName("Constants Tests")
  class ConstantsTests {

    @Test
    @DisplayName("READ constant should be 0x1")
    void readShouldBe1() {
      assertEquals(0x1, FilePerms.READ, "READ should be 0x1");
    }

    @Test
    @DisplayName("WRITE constant should be 0x2")
    void writeShouldBe2() {
      assertEquals(0x2, FilePerms.WRITE, "WRITE should be 0x2");
    }

    @Test
    @DisplayName("ALL should be READ | WRITE")
    void allShouldBeReadOrWrite() {
      assertEquals(FilePerms.READ | FilePerms.WRITE, FilePerms.ALL, "ALL should be READ | WRITE");
    }

    @Test
    @DisplayName("NONE should be 0")
    void noneShouldBeZero() {
      assertEquals(0, FilePerms.NONE, "NONE should be 0");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsHashCodeTests {

    @Test
    @DisplayName("should be equal for same permission bits")
    void shouldBeEqualForSameBits() {
      final FilePerms perms1 = FilePerms.all();
      final FilePerms perms2 = FilePerms.all();

      assertEquals(perms1, perms2, "Same permission bits should be equal");
      assertEquals(perms1.hashCode(), perms2.hashCode(), "Equal perms should have same hash code");
    }

    @Test
    @DisplayName("should not be equal for different bits")
    void shouldNotBeEqualForDifferentBits() {
      assertNotEquals(FilePerms.readOnly(), FilePerms.all(), "Different bits should not be equal");
      assertNotEquals(FilePerms.none(), FilePerms.readOnly(), "NONE and READ should not be equal");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      assertFalse(FilePerms.all().equals(null), "Should not equal null");
    }

    @Test
    @DisplayName("should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      assertFalse(FilePerms.all().equals("not perms"), "Should not equal a String");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should show read and write flags")
    void shouldShowFlags() {
      final String allStr = FilePerms.all().toString();

      assertTrue(
          allStr.contains("read=true"), "all toString should show read=true, got: " + allStr);
      assertTrue(
          allStr.contains("write=true"), "all toString should show write=true, got: " + allStr);
    }

    @Test
    @DisplayName("none toString should show false flags")
    void noneShouldShowFalseFlags() {
      final String noneStr = FilePerms.none().toString();

      assertTrue(
          noneStr.contains("read=false"), "none toString should show read=false, got: " + noneStr);
      assertTrue(
          noneStr.contains("write=false"),
          "none toString should show write=false, got: " + noneStr);
    }
  }
}
