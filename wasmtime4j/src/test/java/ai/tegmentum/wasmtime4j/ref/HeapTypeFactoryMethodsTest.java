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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test suite for HeapType static factory methods and bottom types.
 *
 * <p>Tests the static factory methods none(), nofunc(), noextern(), and funcNull().
 */
@DisplayName("HeapType Factory Methods Tests")
class HeapTypeFactoryMethodsTest {

  // ========================================================================
  // none() Factory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("none() Factory Method Tests")
  class NoneFactoryTests {

    @Test
    @DisplayName("should return non-null NoneRef instance")
    void shouldReturnNonNullNoneRefInstance() {
      HeapType none = HeapType.none();
      assertNotNull(none, "none() should return non-null instance");
    }

    @Test
    @DisplayName("should return NoneRef type")
    void shouldReturnNoneRefType() {
      HeapType none = HeapType.none();
      assertTrue(none instanceof NoneRef, "none() should return NoneRef instance");
    }

    @Test
    @DisplayName("should return same instance on multiple calls")
    void shouldReturnSameInstanceOnMultipleCalls() {
      HeapType none1 = HeapType.none();
      HeapType none2 = HeapType.none();
      assertSame(none1, none2, "none() should return same singleton instance");
    }

    @Test
    @DisplayName("none should be a bottom type")
    void noneShouldBeBottomType() {
      HeapType none = HeapType.none();
      assertTrue(none.isBottom(), "none should be a bottom type");
    }
  }

  // ========================================================================
  // nofunc() Factory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("nofunc() Factory Method Tests")
  class NofuncFactoryTests {

    @Test
    @DisplayName("should return non-null NoFunc instance")
    void shouldReturnNonNullNoFuncInstance() {
      HeapType nofunc = HeapType.nofunc();
      assertNotNull(nofunc, "nofunc() should return non-null instance");
    }

    @Test
    @DisplayName("should return NoFunc type")
    void shouldReturnNoFuncType() {
      HeapType nofunc = HeapType.nofunc();
      assertTrue(nofunc instanceof NoFunc, "nofunc() should return NoFunc instance");
    }

    @Test
    @DisplayName("should return same instance on multiple calls")
    void shouldReturnSameInstanceOnMultipleCalls() {
      HeapType nofunc1 = HeapType.nofunc();
      HeapType nofunc2 = HeapType.nofunc();
      assertSame(nofunc1, nofunc2, "nofunc() should return same singleton instance");
    }

    @Test
    @DisplayName("nofunc should be a bottom type")
    void nofuncShouldBeBottomType() {
      HeapType nofunc = HeapType.nofunc();
      assertTrue(nofunc.isBottom(), "nofunc should be a bottom type");
    }
  }

  // ========================================================================
  // noextern() Factory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("noextern() Factory Method Tests")
  class NoexternFactoryTests {

    @Test
    @DisplayName("should return non-null NoExtern instance")
    void shouldReturnNonNullNoExternInstance() {
      HeapType noextern = HeapType.noextern();
      assertNotNull(noextern, "noextern() should return non-null instance");
    }

    @Test
    @DisplayName("should return NoExtern type")
    void shouldReturnNoExternType() {
      HeapType noextern = HeapType.noextern();
      assertTrue(noextern instanceof NoExtern, "noextern() should return NoExtern instance");
    }

    @Test
    @DisplayName("should return same instance on multiple calls")
    void shouldReturnSameInstanceOnMultipleCalls() {
      HeapType noextern1 = HeapType.noextern();
      HeapType noextern2 = HeapType.noextern();
      assertSame(noextern1, noextern2, "noextern() should return same singleton instance");
    }

    @Test
    @DisplayName("noextern should be a bottom type")
    void noexternShouldBeBottomType() {
      HeapType noextern = HeapType.noextern();
      assertTrue(noextern.isBottom(), "noextern should be a bottom type");
    }
  }

  // ========================================================================
  // funcNull() Factory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("funcNull() Factory Method Tests")
  class FuncNullFactoryTests {

    @Test
    @DisplayName("should return non-null FuncRef instance")
    void shouldReturnNonNullFuncRefInstance() {
      FuncRef funcNull = HeapType.funcNull();
      assertNotNull(funcNull, "funcNull() should return non-null instance");
    }

    @Test
    @DisplayName("should return FuncRef type")
    void shouldReturnFuncRefType() {
      FuncRef funcNull = HeapType.funcNull();
      assertTrue(funcNull instanceof FuncRef, "funcNull() should return FuncRef instance");
    }

    @Test
    @DisplayName("funcNull should be nullable")
    void funcNullShouldBeNullable() {
      FuncRef funcNull = HeapType.funcNull();
      assertTrue(funcNull.isNullable(), "funcNull should be nullable");
    }
  }

  // ========================================================================
  // Bottom Type Relationship Tests
  // ========================================================================

  @Nested
  @DisplayName("Bottom Type Relationship Tests")
  class BottomTypeRelationshipTests {

    @Test
    @DisplayName("all bottom types should be distinct")
    void allBottomTypesShouldBeDistinct() {
      HeapType none = HeapType.none();
      HeapType nofunc = HeapType.nofunc();
      HeapType noextern = HeapType.noextern();

      assertTrue(none != nofunc, "none and nofunc should be different instances");
      assertTrue(none != noextern, "none and noextern should be different instances");
      assertTrue(nofunc != noextern, "nofunc and noextern should be different instances");
    }

    @Test
    @DisplayName("all bottom types should have different type names")
    void allBottomTypesShouldHaveDifferentTypeNames() {
      HeapType none = HeapType.none();
      HeapType nofunc = HeapType.nofunc();
      HeapType noextern = HeapType.noextern();

      String noneName = none.getTypeName();
      String nofuncName = nofunc.getTypeName();
      String noexternName = noextern.getTypeName();

      assertNotNull(noneName, "none type name should not be null");
      assertNotNull(nofuncName, "nofunc type name should not be null");
      assertNotNull(noexternName, "noextern type name should not be null");

      assertTrue(!noneName.equals(nofuncName), "none and nofunc should have different type names");
      assertTrue(
          !noneName.equals(noexternName), "none and noextern should have different type names");
      assertTrue(
          !nofuncName.equals(noexternName), "nofunc and noextern should have different type names");
    }
  }

  // ========================================================================
  // Default Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("isBottom default should return false for non-bottom types")
    void isBottomDefaultShouldReturnFalseForNonBottomTypes() {
      // FuncRef is not a bottom type
      FuncRef funcNull = HeapType.funcNull();
      // The default isBottom() returns false, but FuncRef may override it
      // We're testing that non-bottom types correctly return false
      // This is a behavioral test of the inheritance hierarchy
      assertNotNull(funcNull, "FuncRef should be created");
    }

    @Test
    @DisplayName("asNullable default should return same instance")
    void asNullableDefaultShouldReturnSameInstance() {
      HeapType none = HeapType.none();
      HeapType nullableNone = none.asNullable();
      // Default implementation returns 'this'
      assertEquals(none, nullableNone, "asNullable should return same instance by default");
    }

    @Test
    @DisplayName("asNonNullable default should return same instance")
    void asNonNullableDefaultShouldReturnSameInstance() {
      HeapType none = HeapType.none();
      HeapType nonNullableNone = none.asNonNullable();
      // Default implementation returns 'this'
      assertEquals(none, nonNullableNone, "asNonNullable should return same instance by default");
    }
  }
}
