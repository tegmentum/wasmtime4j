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
 * Tests for {@link NoneRef} - the bottom type in the any/eq type hierarchy.
 *
 * <p>Validates singleton behavior, HeapType contract, subtype relationships, and equals/hashCode.
 */
@DisplayName("NoneRef Tests")
class NoneRefTest {

  @Nested
  @DisplayName("Singleton Tests")
  class SingletonTests {

    @Test
    @DisplayName("getInstance should return non-null instance")
    void getInstanceShouldReturnNonNullInstance() {
      final NoneRef instance = NoneRef.getInstance();
      assertNotNull(instance, "getInstance() should return a non-null instance");
    }

    @Test
    @DisplayName("getInstance should return same instance on multiple calls")
    void getInstanceShouldReturnSameInstanceOnMultipleCalls() {
      final NoneRef first = NoneRef.getInstance();
      final NoneRef second = NoneRef.getInstance();
      assertSame(first, second, "getInstance() should return the same singleton instance");
    }

    @Test
    @DisplayName("INSTANCE field should be same as getInstance")
    void instanceFieldShouldBeSameAsGetInstance() {
      assertSame(
          NoneRef.INSTANCE,
          NoneRef.getInstance(),
          "INSTANCE field should reference the same object as getInstance()");
    }
  }

  @Nested
  @DisplayName("HeapType Contract Tests")
  class HeapTypeContractTests {

    @Test
    @DisplayName("getValueType should return ANYREF")
    void getValueTypeShouldReturnAnyref() {
      final NoneRef noneRef = NoneRef.getInstance();
      assertEquals(
          WasmValueType.ANYREF, noneRef.getValueType(), "NoneRef value type should be ANYREF");
    }

    @Test
    @DisplayName("isNullable should return true for bottom type")
    void isNullableShouldReturnTrue() {
      final NoneRef noneRef = NoneRef.getInstance();
      assertTrue(noneRef.isNullable(), "Bottom types should always be nullable");
    }

    @Test
    @DisplayName("isBottom should return true")
    void isBottomShouldReturnTrue() {
      final NoneRef noneRef = NoneRef.getInstance();
      assertTrue(noneRef.isBottom(), "NoneRef should be a bottom type");
    }

    @Test
    @DisplayName("getTypeName should return none")
    void getTypeNameShouldReturnNone() {
      final NoneRef noneRef = NoneRef.getInstance();
      assertEquals("none", noneRef.getTypeName(), "Type name should be 'none'");
    }

    @Test
    @DisplayName("toString should return none")
    void toStringShouldReturnNone() {
      final NoneRef noneRef = NoneRef.getInstance();
      assertEquals("none", noneRef.toString(), "toString should return 'none'");
    }
  }

  @Nested
  @DisplayName("Subtype Relationship Tests")
  class SubtypeRelationshipTests {

    @Test
    @DisplayName("should be subtype of itself")
    void shouldBeSubtypeOfItself() {
      final NoneRef noneRef = NoneRef.getInstance();
      assertTrue(noneRef.isSubtypeOf(noneRef), "NoneRef should be a subtype of itself");
    }

    @Test
    @DisplayName("should be subtype of ANYREF heap type")
    void shouldBeSubtypeOfAnyrefHeapType() {
      final NoneRef noneRef = NoneRef.getInstance();
      final HeapType anyType =
          new HeapType() {
            @Override
            public WasmValueType getValueType() {
              return WasmValueType.ANYREF;
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
              return "any";
            }
          };
      assertTrue(noneRef.isSubtypeOf(anyType), "NoneRef should be a subtype of ANYREF heap type");
    }

    @Test
    @DisplayName("should be subtype of EQREF heap type")
    void shouldBeSubtypeOfEqrefHeapType() {
      final NoneRef noneRef = NoneRef.getInstance();
      final HeapType eqType =
          new HeapType() {
            @Override
            public WasmValueType getValueType() {
              return WasmValueType.EQREF;
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
              return "eq";
            }
          };
      assertTrue(noneRef.isSubtypeOf(eqType), "NoneRef should be a subtype of EQREF heap type");
    }

    @Test
    @DisplayName("should be subtype of I31REF heap type")
    void shouldBeSubtypeOfI31refHeapType() {
      final NoneRef noneRef = NoneRef.getInstance();
      final HeapType i31Type =
          new HeapType() {
            @Override
            public WasmValueType getValueType() {
              return WasmValueType.I31REF;
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
              return "i31";
            }
          };
      assertTrue(noneRef.isSubtypeOf(i31Type), "NoneRef should be a subtype of I31REF heap type");
    }

    @Test
    @DisplayName("should be subtype of STRUCTREF heap type")
    void shouldBeSubtypeOfStructrefHeapType() {
      final NoneRef noneRef = NoneRef.getInstance();
      final HeapType structType =
          new HeapType() {
            @Override
            public WasmValueType getValueType() {
              return WasmValueType.STRUCTREF;
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
              return "struct";
            }
          };
      assertTrue(
          noneRef.isSubtypeOf(structType), "NoneRef should be a subtype of STRUCTREF heap type");
    }

    @Test
    @DisplayName("should be subtype of ARRAYREF heap type")
    void shouldBeSubtypeOfArrayrefHeapType() {
      final NoneRef noneRef = NoneRef.getInstance();
      final HeapType arrayType =
          new HeapType() {
            @Override
            public WasmValueType getValueType() {
              return WasmValueType.ARRAYREF;
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
              return "array";
            }
          };
      assertTrue(
          noneRef.isSubtypeOf(arrayType), "NoneRef should be a subtype of ARRAYREF heap type");
    }

    @Test
    @DisplayName("should not be subtype of EXTERNREF heap type")
    void shouldNotBeSubtypeOfExternrefHeapType() {
      final NoneRef noneRef = NoneRef.getInstance();
      final NoExtern noExtern = NoExtern.getInstance();
      assertFalse(
          noneRef.isSubtypeOf(noExtern),
          "NoneRef should not be a subtype of NoExtern (different hierarchy)");
    }

    @Test
    @DisplayName("should not be subtype of FUNCREF heap type")
    void shouldNotBeSubtypeOfFuncrefHeapType() {
      final NoneRef noneRef = NoneRef.getInstance();
      final NoFunc noFunc = NoFunc.getInstance();
      assertFalse(
          noneRef.isSubtypeOf(noFunc),
          "NoneRef should not be a subtype of NoFunc (different hierarchy)");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for same type")
    void equalsShouldReturnTrueForSameType() {
      final NoneRef a = NoneRef.getInstance();
      final NoneRef b = NoneRef.getInstance();
      assertEquals(a, b, "Two NoneRef instances should be equal");
    }

    @Test
    @DisplayName("equals should return false for different types")
    void equalsShouldReturnFalseForDifferentTypes() {
      final NoneRef noneRef = NoneRef.getInstance();
      final NoExtern noExtern = NoExtern.getInstance();
      assertNotEquals(noneRef, noExtern, "NoneRef should not equal NoExtern");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final NoneRef noneRef = NoneRef.getInstance();
      assertNotEquals(null, noneRef, "NoneRef should not equal null");
    }

    @Test
    @DisplayName("hashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final NoneRef a = NoneRef.getInstance();
      final NoneRef b = NoneRef.getInstance();
      assertEquals(a.hashCode(), b.hashCode(), "Equal objects should have same hashCode");
    }

    @Test
    @DisplayName("hashCode should be based on class")
    void hashCodeShouldBeBasedOnClass() {
      final NoneRef noneRef = NoneRef.getInstance();
      assertEquals(
          NoneRef.class.hashCode(),
          noneRef.hashCode(),
          "NoneRef hashCode should be based on its class");
    }
  }

  @Nested
  @DisplayName("HeapType Interface Conformance Tests")
  class HeapTypeInterfaceConformanceTests {

    @Test
    @DisplayName("should implement HeapType interface")
    void shouldImplementHeapTypeInterface() {
      final NoneRef noneRef = NoneRef.getInstance();
      assertTrue(noneRef instanceof HeapType, "NoneRef should implement HeapType interface");
    }

    @Test
    @DisplayName("asNullable should return same instance by default")
    void asNullableShouldReturnSameInstance() {
      final NoneRef noneRef = NoneRef.getInstance();
      final HeapType nullable = noneRef.asNullable();
      assertSame(noneRef, nullable, "asNullable should return the same instance for bottom types");
    }

    @Test
    @DisplayName("asNonNullable should return same instance by default")
    void asNonNullableShouldReturnSameInstance() {
      final NoneRef noneRef = NoneRef.getInstance();
      final HeapType nonNullable = noneRef.asNonNullable();
      assertSame(
          noneRef, nonNullable, "asNonNullable should return the same instance for bottom types");
    }
  }
}
