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

package ai.tegmentum.wasmtime4j.jni.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.MemoryType;
import ai.tegmentum.wasmtime4j.WasmTypeKind;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link JniMemoryType} class.
 *
 * <p>This test class verifies the JNI implementation of MemoryType interface for WebAssembly memory
 * types.
 */
@DisplayName("JniMemoryType Tests")
class JniMemoryTypeTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniMemoryType should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(JniMemoryType.class.getModifiers()),
          "JniMemoryType should be final");
    }

    @Test
    @DisplayName("JniMemoryType should implement MemoryType")
    void shouldImplementMemoryType() {
      assertTrue(
          MemoryType.class.isAssignableFrom(JniMemoryType.class),
          "JniMemoryType should implement MemoryType");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should create with minimum only")
    void constructorShouldCreateWithMinimumOnly() {
      final JniMemoryType memoryType = new JniMemoryType(1, null, false, false);
      assertNotNull(memoryType, "MemoryType should not be null");
      assertEquals(1, memoryType.getMinimum(), "Minimum should be 1");
      assertFalse(memoryType.getMaximum().isPresent(), "Maximum should be empty");
    }

    @Test
    @DisplayName("Constructor should create with minimum and maximum")
    void constructorShouldCreateWithMinimumAndMaximum() {
      final JniMemoryType memoryType = new JniMemoryType(1, 10L, false, false);
      assertEquals(1, memoryType.getMinimum(), "Minimum should be 1");
      assertTrue(memoryType.getMaximum().isPresent(), "Maximum should be present");
      assertEquals(10L, memoryType.getMaximum().get(), "Maximum should be 10");
    }

    @Test
    @DisplayName("Constructor should accept zero minimum")
    void constructorShouldAcceptZeroMinimum() {
      final JniMemoryType memoryType = new JniMemoryType(0, null, false, false);
      assertEquals(0, memoryType.getMinimum(), "Minimum should be 0");
    }

    @Test
    @DisplayName("Constructor should throw for negative minimum")
    void constructorShouldThrowForNegativeMinimum() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniMemoryType(-1, null, false, false),
          "Should throw for negative minimum");
    }

    @Test
    @DisplayName("Constructor should throw when maximum less than minimum")
    void constructorShouldThrowWhenMaximumLessThanMinimum() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniMemoryType(10, 5L, false, false),
          "Should throw when maximum < minimum");
    }

    @Test
    @DisplayName("Constructor should accept maximum equal to minimum")
    void constructorShouldAcceptMaximumEqualToMinimum() {
      final JniMemoryType memoryType = new JniMemoryType(5, 5L, false, false);
      assertEquals(5, memoryType.getMinimum(), "Minimum should be 5");
      assertEquals(5L, memoryType.getMaximum().get(), "Maximum should be 5");
    }

    @Test
    @DisplayName("Constructor should create 64-bit memory")
    void constructorShouldCreate64BitMemory() {
      final JniMemoryType memoryType = new JniMemoryType(1, null, true, false);
      assertTrue(memoryType.is64Bit(), "Memory should be 64-bit");
    }

    @Test
    @DisplayName("Constructor should create shared memory")
    void constructorShouldCreateSharedMemory() {
      final JniMemoryType memoryType = new JniMemoryType(1, null, false, true);
      assertTrue(memoryType.isShared(), "Memory should be shared");
    }

    @Test
    @DisplayName("Constructor should create 64-bit shared memory")
    void constructorShouldCreate64BitSharedMemory() {
      final JniMemoryType memoryType = new JniMemoryType(1, 100L, true, true);
      assertTrue(memoryType.is64Bit(), "Memory should be 64-bit");
      assertTrue(memoryType.isShared(), "Memory should be shared");
    }
  }

  @Nested
  @DisplayName("getKind Tests")
  class GetKindTests {

    @Test
    @DisplayName("getKind should return MEMORY")
    void getKindShouldReturnMemory() {
      final JniMemoryType memoryType = new JniMemoryType(1, null, false, false);
      assertEquals(WasmTypeKind.MEMORY, memoryType.getKind(), "Kind should be MEMORY");
    }
  }

  @Nested
  @DisplayName("Equals Tests")
  class EqualsTests {

    @Test
    @DisplayName("Same instance should be equal")
    void sameInstanceShouldBeEqual() {
      final JniMemoryType memoryType = new JniMemoryType(1, 10L, false, false);
      assertEquals(memoryType, memoryType, "Same instance should be equal");
    }

    @Test
    @DisplayName("Equal values should be equal")
    void equalValuesShouldBeEqual() {
      final JniMemoryType memoryType1 = new JniMemoryType(1, 10L, false, false);
      final JniMemoryType memoryType2 = new JniMemoryType(1, 10L, false, false);
      assertEquals(memoryType1, memoryType2, "Equal values should be equal");
    }

    @Test
    @DisplayName("Different minimum should not be equal")
    void differentMinimumShouldNotBeEqual() {
      final JniMemoryType memoryType1 = new JniMemoryType(1, null, false, false);
      final JniMemoryType memoryType2 = new JniMemoryType(2, null, false, false);
      assertNotEquals(memoryType1, memoryType2, "Different minimum should not be equal");
    }

    @Test
    @DisplayName("Different maximum should not be equal")
    void differentMaximumShouldNotBeEqual() {
      final JniMemoryType memoryType1 = new JniMemoryType(1, 10L, false, false);
      final JniMemoryType memoryType2 = new JniMemoryType(1, 20L, false, false);
      assertNotEquals(memoryType1, memoryType2, "Different maximum should not be equal");
    }

    @Test
    @DisplayName("With and without maximum should not be equal")
    void withAndWithoutMaximumShouldNotBeEqual() {
      final JniMemoryType memoryType1 = new JniMemoryType(1, 10L, false, false);
      final JniMemoryType memoryType2 = new JniMemoryType(1, null, false, false);
      assertNotEquals(memoryType1, memoryType2, "With and without maximum should not be equal");
    }

    @Test
    @DisplayName("Different 64-bit flag should not be equal")
    void different64BitFlagShouldNotBeEqual() {
      final JniMemoryType memoryType1 = new JniMemoryType(1, null, true, false);
      final JniMemoryType memoryType2 = new JniMemoryType(1, null, false, false);
      assertNotEquals(memoryType1, memoryType2, "Different 64-bit flag should not be equal");
    }

    @Test
    @DisplayName("Different shared flag should not be equal")
    void differentSharedFlagShouldNotBeEqual() {
      final JniMemoryType memoryType1 = new JniMemoryType(1, null, false, true);
      final JniMemoryType memoryType2 = new JniMemoryType(1, null, false, false);
      assertNotEquals(memoryType1, memoryType2, "Different shared flag should not be equal");
    }

    @Test
    @DisplayName("Should not be equal to null")
    void shouldNotBeEqualToNull() {
      final JniMemoryType memoryType = new JniMemoryType(1, null, false, false);
      assertFalse(memoryType.equals(null), "Should not be equal to null");
    }

    @Test
    @DisplayName("Should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      final JniMemoryType memoryType = new JniMemoryType(1, null, false, false);
      assertFalse(memoryType.equals("string"), "Should not be equal to different type");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("Equal objects should have equal hashCodes")
    void equalObjectsShouldHaveEqualHashCodes() {
      final JniMemoryType memoryType1 = new JniMemoryType(1, 10L, true, true);
      final JniMemoryType memoryType2 = new JniMemoryType(1, 10L, true, true);
      assertEquals(
          memoryType1.hashCode(),
          memoryType2.hashCode(),
          "Equal objects should have equal hashCodes");
    }

    @Test
    @DisplayName("HashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final JniMemoryType memoryType = new JniMemoryType(1, 10L, false, false);
      final int hash1 = memoryType.hashCode();
      final int hash2 = memoryType.hashCode();
      assertEquals(hash1, hash2, "HashCode should be consistent");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include minimum")
    void toStringShouldIncludeMinimum() {
      final JniMemoryType memoryType = new JniMemoryType(5, null, false, false);
      final String str = memoryType.toString();
      assertTrue(str.contains("5") || str.contains("min"), "toString should include minimum");
    }

    @Test
    @DisplayName("toString should include maximum when present")
    void toStringShouldIncludeMaximumWhenPresent() {
      final JniMemoryType memoryType = new JniMemoryType(1, 100L, false, false);
      final String str = memoryType.toString();
      assertTrue(str.contains("100") || str.contains("max"), "toString should include maximum");
    }

    @Test
    @DisplayName("toString should indicate unlimited when no maximum")
    void toStringShouldIndicateUnlimitedWhenNoMaximum() {
      final JniMemoryType memoryType = new JniMemoryType(1, null, false, false);
      final String str = memoryType.toString();
      assertTrue(
          str.contains("unlimited") || str.contains("null") || str.contains("max="),
          "toString should indicate unlimited");
    }

    @Test
    @DisplayName("toString should include 64bit flag")
    void toStringShouldInclude64BitFlag() {
      final JniMemoryType memoryType = new JniMemoryType(1, null, true, false);
      final String str = memoryType.toString();
      assertTrue(str.contains("64") || str.contains("bit"), "toString should include 64bit flag");
    }

    @Test
    @DisplayName("toString should include shared flag")
    void toStringShouldIncludeSharedFlag() {
      final JniMemoryType memoryType = new JniMemoryType(1, null, false, true);
      final String str = memoryType.toString();
      assertTrue(str.contains("shared"), "toString should include shared flag");
    }
  }

  @Nested
  @DisplayName("fromNative Tests")
  class FromNativeTests {

    @Test
    @DisplayName("fromNative should throw for zero handle")
    void fromNativeShouldThrowForZeroHandle() {
      assertThrows(
          JniException.class, () -> JniMemoryType.fromNative(0), "Should throw for zero handle");
    }

    @Test
    @DisplayName("fromNative should throw for negative handle")
    void fromNativeShouldThrowForNegativeHandle() {
      assertThrows(
          JniException.class,
          () -> JniMemoryType.fromNative(-1),
          "Should throw for negative handle");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Large memory should work")
    void largeMemoryShouldWork() {
      final JniMemoryType memoryType = new JniMemoryType(1000, 65536L, false, false);
      assertEquals(1000, memoryType.getMinimum(), "Large minimum should work");
      assertEquals(65536L, memoryType.getMaximum().get(), "Large maximum should work");
    }

    @Test
    @DisplayName("Memory with all flags should work")
    void memoryWithAllFlagsShouldWork() {
      final JniMemoryType memoryType = new JniMemoryType(1, 100L, true, true);
      assertEquals(1, memoryType.getMinimum(), "Minimum should be 1");
      assertEquals(100L, memoryType.getMaximum().get(), "Maximum should be 100");
      assertTrue(memoryType.is64Bit(), "Should be 64-bit");
      assertTrue(memoryType.isShared(), "Should be shared");
      assertEquals(WasmTypeKind.MEMORY, memoryType.getKind(), "Kind should be MEMORY");
    }

    @Test
    @DisplayName("Standard 32-bit non-shared memory should work")
    void standard32BitNonSharedMemoryShouldWork() {
      final JniMemoryType memoryType = new JniMemoryType(1, 256L, false, false);
      assertEquals(1, memoryType.getMinimum(), "Minimum should be 1");
      assertEquals(256L, memoryType.getMaximum().get(), "Maximum should be 256 pages");
      assertFalse(memoryType.is64Bit(), "Should not be 64-bit");
      assertFalse(memoryType.isShared(), "Should not be shared");
    }
  }
}
