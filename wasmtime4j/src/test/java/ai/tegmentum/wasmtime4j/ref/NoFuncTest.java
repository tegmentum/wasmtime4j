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
 * Tests for {@link NoFunc} - the bottom type in the func type hierarchy.
 *
 * <p>Validates singleton behavior, HeapType contract, subtype relationships, and equals/hashCode.
 */
@DisplayName("NoFunc Tests")
class NoFuncTest {

  @Nested
  @DisplayName("Singleton Tests")
  class SingletonTests {

    @Test
    @DisplayName("getInstance should return non-null instance")
    void getInstanceShouldReturnNonNullInstance() {
      final NoFunc instance = NoFunc.getInstance();
      assertNotNull(instance, "getInstance() should return a non-null instance");
    }

    @Test
    @DisplayName("getInstance should return same instance on multiple calls")
    void getInstanceShouldReturnSameInstanceOnMultipleCalls() {
      final NoFunc first = NoFunc.getInstance();
      final NoFunc second = NoFunc.getInstance();
      assertSame(first, second, "getInstance() should return the same singleton instance");
    }

    @Test
    @DisplayName("INSTANCE field should be same as getInstance")
    void instanceFieldShouldBeSameAsGetInstance() {
      assertSame(
          NoFunc.INSTANCE,
          NoFunc.getInstance(),
          "INSTANCE field should reference the same object as getInstance()");
    }
  }

  @Nested
  @DisplayName("HeapType Contract Tests")
  class HeapTypeContractTests {

    @Test
    @DisplayName("getValueType should return FUNCREF")
    void getValueTypeShouldReturnFuncref() {
      final NoFunc noFunc = NoFunc.getInstance();
      assertEquals(
          WasmValueType.FUNCREF, noFunc.getValueType(), "NoFunc value type should be FUNCREF");
    }

    @Test
    @DisplayName("isNullable should return true for bottom type")
    void isNullableShouldReturnTrue() {
      final NoFunc noFunc = NoFunc.getInstance();
      assertTrue(noFunc.isNullable(), "Bottom types should always be nullable");
    }

    @Test
    @DisplayName("isBottom should return true")
    void isBottomShouldReturnTrue() {
      final NoFunc noFunc = NoFunc.getInstance();
      assertTrue(noFunc.isBottom(), "NoFunc should be a bottom type");
    }

    @Test
    @DisplayName("getTypeName should return nofunc")
    void getTypeNameShouldReturnNofunc() {
      final NoFunc noFunc = NoFunc.getInstance();
      assertEquals("nofunc", noFunc.getTypeName(), "Type name should be 'nofunc'");
    }

    @Test
    @DisplayName("toString should return nofunc")
    void toStringShouldReturnNofunc() {
      final NoFunc noFunc = NoFunc.getInstance();
      assertEquals("nofunc", noFunc.toString(), "toString should return 'nofunc'");
    }
  }

  @Nested
  @DisplayName("Subtype Relationship Tests")
  class SubtypeRelationshipTests {

    @Test
    @DisplayName("should be subtype of itself")
    void shouldBeSubtypeOfItself() {
      final NoFunc noFunc = NoFunc.getInstance();
      assertTrue(noFunc.isSubtypeOf(noFunc), "NoFunc should be a subtype of itself");
    }

    @Test
    @DisplayName("should be subtype of any FUNCREF heap type")
    void shouldBeSubtypeOfFuncrefHeapType() {
      final NoFunc noFunc = NoFunc.getInstance();
      final HeapType funcType =
          new HeapType() {
            @Override
            public WasmValueType getValueType() {
              return WasmValueType.FUNCREF;
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
              return "func";
            }
          };
      assertTrue(
          noFunc.isSubtypeOf(funcType), "NoFunc should be a subtype of any FUNCREF heap type");
    }

    @Test
    @DisplayName("should not be subtype of EXTERNREF heap type")
    void shouldNotBeSubtypeOfExternrefHeapType() {
      final NoFunc noFunc = NoFunc.getInstance();
      final NoExtern noExtern = NoExtern.getInstance();
      assertFalse(
          noFunc.isSubtypeOf(noExtern),
          "NoFunc should not be a subtype of NoExtern (different hierarchy)");
    }

    @Test
    @DisplayName("should not be subtype of ANYREF heap type")
    void shouldNotBeSubtypeOfAnyrefHeapType() {
      final NoFunc noFunc = NoFunc.getInstance();
      final NoneRef noneRef = NoneRef.getInstance();
      assertFalse(
          noFunc.isSubtypeOf(noneRef),
          "NoFunc should not be a subtype of NoneRef (different hierarchy)");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for same type")
    void equalsShouldReturnTrueForSameType() {
      final NoFunc a = NoFunc.getInstance();
      final NoFunc b = NoFunc.getInstance();
      assertEquals(a, b, "Two NoFunc instances should be equal");
    }

    @Test
    @DisplayName("equals should return false for different types")
    void equalsShouldReturnFalseForDifferentTypes() {
      final NoFunc noFunc = NoFunc.getInstance();
      final NoneRef noneRef = NoneRef.getInstance();
      assertNotEquals(noFunc, noneRef, "NoFunc should not equal NoneRef");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final NoFunc noFunc = NoFunc.getInstance();
      assertNotEquals(null, noFunc, "NoFunc should not equal null");
    }

    @Test
    @DisplayName("hashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final NoFunc a = NoFunc.getInstance();
      final NoFunc b = NoFunc.getInstance();
      assertEquals(a.hashCode(), b.hashCode(), "Equal objects should have same hashCode");
    }

    @Test
    @DisplayName("hashCode should be based on class")
    void hashCodeShouldBeBasedOnClass() {
      final NoFunc noFunc = NoFunc.getInstance();
      assertEquals(
          NoFunc.class.hashCode(),
          noFunc.hashCode(),
          "NoFunc hashCode should be based on its class");
    }
  }

  @Nested
  @DisplayName("HeapType Interface Conformance Tests")
  class HeapTypeInterfaceConformanceTests {

    @Test
    @DisplayName("should implement HeapType interface")
    void shouldImplementHeapTypeInterface() {
      final NoFunc noFunc = NoFunc.getInstance();
      assertTrue(noFunc instanceof HeapType, "NoFunc should implement HeapType interface");
    }

    @Test
    @DisplayName("asNullable should return same instance by default")
    void asNullableShouldReturnSameInstance() {
      final NoFunc noFunc = NoFunc.getInstance();
      final HeapType nullable = noFunc.asNullable();
      assertSame(noFunc, nullable, "asNullable should return the same instance for bottom types");
    }

    @Test
    @DisplayName("asNonNullable should return same instance by default")
    void asNonNullableShouldReturnSameInstance() {
      final NoFunc noFunc = NoFunc.getInstance();
      final HeapType nonNullable = noFunc.asNonNullable();
      assertSame(
          noFunc, nonNullable, "asNonNullable should return the same instance for bottom types");
    }
  }
}
