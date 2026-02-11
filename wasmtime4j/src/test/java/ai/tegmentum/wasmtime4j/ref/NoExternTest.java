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

package ai.tegmentum.wasmtime4j.ref;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValueType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NoExtern} - the bottom type in the extern type hierarchy.
 *
 * <p>Validates singleton behavior, HeapType contract, subtype relationships, and equals/hashCode.
 */
@DisplayName("NoExtern Tests")
class NoExternTest {

  @Nested
  @DisplayName("Singleton Tests")
  class SingletonTests {

    @Test
    @DisplayName("getInstance should return non-null instance")
    void getInstanceShouldReturnNonNullInstance() {
      final NoExtern instance = NoExtern.getInstance();
      assertNotNull(instance, "getInstance() should return a non-null instance");
    }

    @Test
    @DisplayName("getInstance should return same instance on multiple calls")
    void getInstanceShouldReturnSameInstanceOnMultipleCalls() {
      final NoExtern first = NoExtern.getInstance();
      final NoExtern second = NoExtern.getInstance();
      assertSame(first, second, "getInstance() should return the same singleton instance");
    }

    @Test
    @DisplayName("INSTANCE field should be same as getInstance")
    void instanceFieldShouldBeSameAsGetInstance() {
      assertSame(
          NoExtern.INSTANCE,
          NoExtern.getInstance(),
          "INSTANCE field should reference the same object as getInstance()");
    }
  }

  @Nested
  @DisplayName("HeapType Contract Tests")
  class HeapTypeContractTests {

    @Test
    @DisplayName("getValueType should return EXTERNREF")
    void getValueTypeShouldReturnExternref() {
      final NoExtern noExtern = NoExtern.getInstance();
      assertEquals(
          WasmValueType.EXTERNREF,
          noExtern.getValueType(),
          "NoExtern value type should be EXTERNREF");
    }

    @Test
    @DisplayName("isNullable should return true for bottom type")
    void isNullableShouldReturnTrue() {
      final NoExtern noExtern = NoExtern.getInstance();
      assertTrue(noExtern.isNullable(), "Bottom types should always be nullable");
    }

    @Test
    @DisplayName("isBottom should return true")
    void isBottomShouldReturnTrue() {
      final NoExtern noExtern = NoExtern.getInstance();
      assertTrue(noExtern.isBottom(), "NoExtern should be a bottom type");
    }

    @Test
    @DisplayName("getTypeName should return noextern")
    void getTypeNameShouldReturnNoextern() {
      final NoExtern noExtern = NoExtern.getInstance();
      assertEquals("noextern", noExtern.getTypeName(), "Type name should be 'noextern'");
    }

    @Test
    @DisplayName("toString should return noextern")
    void toStringShouldReturnNoextern() {
      final NoExtern noExtern = NoExtern.getInstance();
      assertEquals("noextern", noExtern.toString(), "toString should return 'noextern'");
    }
  }

  @Nested
  @DisplayName("Subtype Relationship Tests")
  class SubtypeRelationshipTests {

    @Test
    @DisplayName("should be subtype of itself")
    void shouldBeSubtypeOfItself() {
      final NoExtern noExtern = NoExtern.getInstance();
      assertTrue(noExtern.isSubtypeOf(noExtern), "NoExtern should be a subtype of itself");
    }

    @Test
    @DisplayName("should be subtype of any EXTERNREF heap type")
    void shouldBeSubtypeOfExternrefHeapType() {
      final NoExtern noExtern = NoExtern.getInstance();
      final HeapType externType =
          new HeapType() {
            @Override
            public WasmValueType getValueType() {
              return WasmValueType.EXTERNREF;
            }

            @Override
            public boolean isNullable() {
              return false;
            }

            @Override
            public boolean isSubtypeOf(final HeapType other) {
              return false;
            }

            @Override
            public String getTypeName() {
              return "extern";
            }
          };
      assertTrue(
          noExtern.isSubtypeOf(externType),
          "NoExtern should be a subtype of any EXTERNREF heap type");
    }

    @Test
    @DisplayName("should not be subtype of FUNCREF heap type")
    void shouldNotBeSubtypeOfFuncrefHeapType() {
      final NoExtern noExtern = NoExtern.getInstance();
      final NoFunc noFunc = NoFunc.getInstance();
      assertFalse(
          noExtern.isSubtypeOf(noFunc),
          "NoExtern should not be a subtype of NoFunc (different hierarchy)");
    }

    @Test
    @DisplayName("should not be subtype of ANYREF heap type")
    void shouldNotBeSubtypeOfAnyrefHeapType() {
      final NoExtern noExtern = NoExtern.getInstance();
      final NoneRef noneRef = NoneRef.getInstance();
      assertFalse(
          noExtern.isSubtypeOf(noneRef),
          "NoExtern should not be a subtype of NoneRef (different hierarchy)");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for same type")
    void equalsShouldReturnTrueForSameType() {
      final NoExtern a = NoExtern.getInstance();
      final NoExtern b = NoExtern.getInstance();
      assertEquals(a, b, "Two NoExtern instances should be equal");
    }

    @Test
    @DisplayName("equals should return false for different types")
    void equalsShouldReturnFalseForDifferentTypes() {
      final NoExtern noExtern = NoExtern.getInstance();
      final NoFunc noFunc = NoFunc.getInstance();
      assertNotEquals(noExtern, noFunc, "NoExtern should not equal NoFunc");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final NoExtern noExtern = NoExtern.getInstance();
      assertNotEquals(null, noExtern, "NoExtern should not equal null");
    }

    @Test
    @DisplayName("hashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final NoExtern a = NoExtern.getInstance();
      final NoExtern b = NoExtern.getInstance();
      assertEquals(a.hashCode(), b.hashCode(), "Equal objects should have same hashCode");
    }

    @Test
    @DisplayName("hashCode should be based on class")
    void hashCodeShouldBeBasedOnClass() {
      final NoExtern noExtern = NoExtern.getInstance();
      assertEquals(
          NoExtern.class.hashCode(),
          noExtern.hashCode(),
          "NoExtern hashCode should be based on its class");
    }
  }

  @Nested
  @DisplayName("HeapType Interface Conformance Tests")
  class HeapTypeInterfaceConformanceTests {

    @Test
    @DisplayName("should implement HeapType interface")
    void shouldImplementHeapTypeInterface() {
      final NoExtern noExtern = NoExtern.getInstance();
      assertTrue(noExtern instanceof HeapType, "NoExtern should implement HeapType interface");
    }

    @Test
    @DisplayName("asNullable should return same instance by default")
    void asNullableShouldReturnSameInstance() {
      final NoExtern noExtern = NoExtern.getInstance();
      final HeapType nullable = noExtern.asNullable();
      assertSame(noExtern, nullable, "asNullable should return the same instance for bottom types");
    }

    @Test
    @DisplayName("asNonNullable should return same instance by default")
    void asNonNullableShouldReturnSameInstance() {
      final NoExtern noExtern = NoExtern.getInstance();
      final HeapType nonNullable = noExtern.asNonNullable();
      assertSame(
          noExtern, nonNullable, "asNonNullable should return the same instance for bottom types");
    }
  }
}
