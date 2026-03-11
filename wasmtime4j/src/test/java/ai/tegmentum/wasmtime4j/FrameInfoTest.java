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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.FrameInfo;
import ai.tegmentum.wasmtime4j.debug.FrameSymbol;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link FrameInfo} class.
 *
 * <p>FrameInfo provides details about a single frame in a WebAssembly call stack.
 */
@DisplayName("FrameInfo Tests")
class FrameInfoTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create instance with all non-null parameters")
    void shouldCreateInstanceWithAllNonNullParameters() {
      final List<FrameSymbol> symbols = List.of(new FrameSymbol("main", "test.c", 10, 5));
      final FrameInfo info = new FrameInfo(0, null, "myFunc", 100, 42, symbols);

      assertNotNull(info, "FrameInfo instance should not be null");
      assertEquals(0, info.getFuncIndex(), "funcIndex should be 0");
      assertNull(info.getModule(), "module should be null");
      assertTrue(info.getFuncName().isPresent(), "funcName should be present");
      assertEquals("myFunc", info.getFuncName().get(), "funcName should match");
      assertTrue(info.getModuleOffset().isPresent(), "moduleOffset should be present");
      assertEquals(100, info.getModuleOffset().get(), "moduleOffset should be 100");
      assertTrue(info.getFuncOffset().isPresent(), "funcOffset should be present");
      assertEquals(42, info.getFuncOffset().get(), "funcOffset should be 42");
      assertEquals(1, info.getSymbols().size(), "symbols should have 1 element");
    }

    @Test
    @DisplayName("should create instance with all nullable fields set to null")
    void shouldCreateInstanceWithAllNullableFieldsNull() {
      final FrameInfo info = new FrameInfo(5, null, null, null, null, null);

      assertEquals(5, info.getFuncIndex(), "funcIndex should be 5");
      assertNull(info.getModule(), "module should be null");
      assertFalse(info.getFuncName().isPresent(), "funcName should be empty");
      assertFalse(info.getModuleOffset().isPresent(), "moduleOffset should be empty");
      assertFalse(info.getFuncOffset().isPresent(), "funcOffset should be empty");
      assertTrue(info.getSymbols().isEmpty(), "symbols should be empty when null passed");
    }

    @Test
    @DisplayName("should create instance with empty symbol list")
    void shouldCreateInstanceWithEmptySymbolList() {
      final FrameInfo info = new FrameInfo(2, null, "fn", 50, 10, Collections.emptyList());

      assertTrue(info.getSymbols().isEmpty(), "symbols should be empty");
    }

    @ParameterizedTest(name = "funcIndex={0} should be stored and retrieved correctly")
    @ValueSource(ints = {0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE, 42, 1000000})
    @DisplayName("FrameInfo should handle boundary funcIndex values")
    void shouldHandleBoundaryFuncIndexValues(int funcIndex) {
      final FrameInfo info = new FrameInfo(funcIndex, null, null, null, null, null);
      assertEquals(funcIndex, info.getFuncIndex(), "getFuncIndex should return " + funcIndex);
    }

    @ParameterizedTest(name = "moduleOffset={0} should be stored and retrieved correctly")
    @ValueSource(ints = {0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE})
    @DisplayName("FrameInfo should handle boundary moduleOffset values")
    void shouldHandleBoundaryModuleOffsetValues(int offset) {
      final FrameInfo info = new FrameInfo(0, null, null, offset, null, null);
      assertTrue(info.getModuleOffset().isPresent(), "moduleOffset should be present");
      assertEquals(offset, info.getModuleOffset().get(), "getModuleOffset should return " + offset);
    }

    @ParameterizedTest(name = "funcOffset={0} should be stored and retrieved correctly")
    @ValueSource(ints = {0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE})
    @DisplayName("FrameInfo should handle boundary funcOffset values")
    void shouldHandleBoundaryFuncOffsetValues(int offset) {
      final FrameInfo info = new FrameInfo(0, null, null, null, offset, null);
      assertTrue(info.getFuncOffset().isPresent(), "funcOffset should be present");
      assertEquals(offset, info.getFuncOffset().get(), "getFuncOffset should return " + offset);
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getFuncIndex should return the function index")
    void getFuncIndexShouldReturnFunctionIndex() {
      final FrameInfo info = new FrameInfo(42, null, null, null, null, null);

      assertEquals(42, info.getFuncIndex(), "getFuncIndex should return 42");
    }

    @Test
    @DisplayName("getModule should return null when module is null")
    void getModuleShouldReturnNullWhenModuleIsNull() {
      final FrameInfo info = new FrameInfo(0, null, null, null, null, null);

      assertNull(info.getModule(), "getModule should return null");
    }

    @Test
    @DisplayName("getFuncName should return Optional.empty for null name")
    void getFuncNameShouldReturnEmptyForNullName() {
      final FrameInfo info = new FrameInfo(0, null, null, null, null, null);

      assertEquals(
          Optional.empty(), info.getFuncName(), "getFuncName should return empty Optional");
    }

    @Test
    @DisplayName("getFuncName should return Optional with value for non-null name")
    void getFuncNameShouldReturnOptionalWithValue() {
      final FrameInfo info = new FrameInfo(0, null, "compute", null, null, null);

      assertEquals(
          Optional.of("compute"),
          info.getFuncName(),
          "getFuncName should return Optional containing 'compute'");
    }

    @Test
    @DisplayName("getModuleOffset should return Optional.empty for null offset")
    void getModuleOffsetShouldReturnEmptyForNullOffset() {
      final FrameInfo info = new FrameInfo(0, null, null, null, null, null);

      assertEquals(
          Optional.empty(), info.getModuleOffset(), "getModuleOffset should return empty Optional");
    }

    @Test
    @DisplayName("getModuleOffset should return Optional with value for non-null offset")
    void getModuleOffsetShouldReturnOptionalWithValue() {
      final FrameInfo info = new FrameInfo(0, null, null, 256, null, null);

      assertEquals(
          Optional.of(256),
          info.getModuleOffset(),
          "getModuleOffset should return Optional containing 256");
    }

    @Test
    @DisplayName("getFuncOffset should return Optional.empty for null offset")
    void getFuncOffsetShouldReturnEmptyForNullOffset() {
      final FrameInfo info = new FrameInfo(0, null, null, null, null, null);

      assertEquals(
          Optional.empty(), info.getFuncOffset(), "getFuncOffset should return empty Optional");
    }

    @Test
    @DisplayName("getFuncOffset should return Optional with value for non-null offset")
    void getFuncOffsetShouldReturnOptionalWithValue() {
      final FrameInfo info = new FrameInfo(0, null, null, null, 64, null);

      assertEquals(
          Optional.of(64),
          info.getFuncOffset(),
          "getFuncOffset should return Optional containing 64");
    }
  }

  @Nested
  @DisplayName("Symbols Defensive Copy Tests")
  class SymbolsDefensiveCopyTests {

    @Test
    @DisplayName("getSymbols should return a defensive copy")
    void getSymbolsShouldReturnDefensiveCopy() {
      final List<FrameSymbol> original = new ArrayList<>();
      original.add(new FrameSymbol("sym1", "file1.c", 1, 1));
      final FrameInfo info = new FrameInfo(0, null, null, null, null, original);

      final List<FrameSymbol> returned = info.getSymbols();
      assertNotSame(returned, info.getSymbols(), "getSymbols should return a new list each time");
      assertEquals(1, returned.size(), "returned list should have 1 element");
    }

    @Test
    @DisplayName("modifying returned symbols list should not affect FrameInfo")
    void modifyingReturnedSymbolsListShouldNotAffectFrameInfo() {
      final List<FrameSymbol> original = new ArrayList<>();
      original.add(new FrameSymbol("sym1", "file1.c", 1, 1));
      final FrameInfo info = new FrameInfo(0, null, null, null, null, original);

      final List<FrameSymbol> returned = info.getSymbols();
      returned.add(new FrameSymbol("sym2", "file2.c", 2, 2));

      assertEquals(
          1,
          info.getSymbols().size(),
          "FrameInfo symbols should not be affected by external modification");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for identical instances")
    void equalsShouldReturnTrueForIdenticalInstances() {
      final FrameInfo info1 = new FrameInfo(1, null, "func", 10, 5, Collections.emptyList());
      final FrameInfo info2 = new FrameInfo(1, null, "func", 10, 5, Collections.emptyList());

      assertEquals(info1, info2, "Two FrameInfo with same values should be equal");
    }

    @Test
    @DisplayName("equals should return false for different funcIndex")
    void equalsShouldReturnFalseForDifferentFuncIndex() {
      final FrameInfo info1 = new FrameInfo(1, null, "func", 10, 5, null);
      final FrameInfo info2 = new FrameInfo(2, null, "func", 10, 5, null);

      assertNotEquals(info1, info2, "FrameInfo with different funcIndex should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different funcName")
    void equalsShouldReturnFalseForDifferentFuncName() {
      final FrameInfo info1 = new FrameInfo(1, null, "funcA", null, null, null);
      final FrameInfo info2 = new FrameInfo(1, null, "funcB", null, null, null);

      assertNotEquals(info1, info2, "FrameInfo with different funcName should not be equal");
    }

    @Test
    @DisplayName("equals should return true for same object")
    void equalsShouldReturnTrueForSameObject() {
      final FrameInfo info = new FrameInfo(0, null, null, null, null, null);

      assertEquals(info, info, "Same object should be equal to itself");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final FrameInfo info = new FrameInfo(0, null, null, null, null, null);

      assertNotEquals(null, info, "FrameInfo should not be equal to null");
    }

    @Test
    @DisplayName("equals should return false for different type")
    void equalsShouldReturnFalseForDifferentType() {
      final FrameInfo info = new FrameInfo(0, null, null, null, null, null);

      assertNotEquals("not a FrameInfo", info, "FrameInfo should not be equal to a String");
    }

    @Test
    @DisplayName("hashCode should be consistent for equal instances")
    void hashCodeShouldBeConsistentForEqualInstances() {
      final FrameInfo info1 = new FrameInfo(1, null, "fn", 10, 5, Collections.emptyList());
      final FrameInfo info2 = new FrameInfo(1, null, "fn", 10, 5, Collections.emptyList());

      assertEquals(
          info1.hashCode(),
          info2.hashCode(),
          "Equal FrameInfo instances should have same hashCode");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain FrameInfo prefix")
    void toStringShouldContainFrameInfoPrefix() {
      final FrameInfo info = new FrameInfo(0, null, null, null, null, null);

      final String result = info.toString();

      assertTrue(result.startsWith("FrameInfo{"), "toString should start with 'FrameInfo{'");
    }

    @Test
    @DisplayName("toString should include funcIndex")
    void toStringShouldIncludeFuncIndex() {
      final FrameInfo info = new FrameInfo(7, null, null, null, null, null);

      final String result = info.toString();

      assertTrue(result.contains("funcIndex=7"), "toString should include funcIndex=7");
    }

    @Test
    @DisplayName("toString should include funcName when present")
    void toStringShouldIncludeFuncNameWhenPresent() {
      final FrameInfo info = new FrameInfo(0, null, "add", null, null, null);

      final String result = info.toString();

      assertTrue(result.contains("funcName='add'"), "toString should include funcName");
    }

    @Test
    @DisplayName("toString should not include funcName when null")
    void toStringShouldNotIncludeFuncNameWhenNull() {
      final FrameInfo info = new FrameInfo(0, null, null, null, null, null);

      final String result = info.toString();

      assertFalse(result.contains("funcName"), "toString should not include funcName when null");
    }

    @Test
    @DisplayName("toString should include moduleOffset when present")
    void toStringShouldIncludeModuleOffsetWhenPresent() {
      final FrameInfo info = new FrameInfo(0, null, null, 200, null, null);

      final String result = info.toString();

      assertTrue(
          result.contains("moduleOffset=200"), "toString should include moduleOffset when present");
    }

    @Test
    @DisplayName("toString should include funcOffset when present")
    void toStringShouldIncludeFuncOffsetWhenPresent() {
      final FrameInfo info = new FrameInfo(0, null, null, null, 30, null);

      final String result = info.toString();

      assertTrue(
          result.contains("funcOffset=30"), "toString should include funcOffset when present");
    }

    @Test
    @DisplayName("toString should include symbols when non-empty")
    void toStringShouldIncludeSymbolsWhenNonEmpty() {
      final List<FrameSymbol> symbols = List.of(new FrameSymbol("sym", null, null, null));
      final FrameInfo info = new FrameInfo(0, null, null, null, null, symbols);

      final String result = info.toString();

      assertTrue(result.contains("symbols="), "toString should include symbols when non-empty");
    }
  }
}
