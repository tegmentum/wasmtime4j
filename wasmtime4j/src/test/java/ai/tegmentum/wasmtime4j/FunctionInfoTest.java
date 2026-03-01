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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.func.FunctionInfo;
import ai.tegmentum.wasmtime4j.type.FuncType;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FunctionInfo} class.
 *
 * <p>FunctionInfo provides metadata about functions including their index, name, type, and whether
 * they are imported or defined locally.
 */
@DisplayName("FunctionInfo Tests")
class FunctionInfoTest {

  /** Simple test implementation of FuncType for testing purposes. */
  private static class TestFuncType implements FuncType {
    private final List<WasmValueType> params;
    private final List<WasmValueType> results;

    TestFuncType(final List<WasmValueType> params, final List<WasmValueType> results) {
      this.params = params;
      this.results = results;
    }

    @Override
    public List<WasmValueType> getParams() {
      return params;
    }

    @Override
    public List<WasmValueType> getResults() {
      return results;
    }

    @Override
    public String toString() {
      return "TestFuncType{params=" + params + ", results=" + results + "}";
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create instance with all parameters for imported function")
    void shouldCreateInstanceForImportedFunction() {
      final FuncType type =
          new TestFuncType(
              List.of(WasmValueType.I32, WasmValueType.I32), List.of(WasmValueType.I32));
      final FunctionInfo info = new FunctionInfo(0, "add", type, true);

      assertNotNull(info, "FunctionInfo instance should not be null");
      assertEquals(0, info.getIndex(), "index should be 0");
      assertEquals("add", info.getName(), "name should be 'add'");
      assertEquals(type, info.getFuncType(), "funcType should match");
      assertTrue(info.isImport(), "isImport should be true");
      assertFalse(info.isLocal(), "isLocal should be false");
    }

    @Test
    @DisplayName("should create instance for locally defined function")
    void shouldCreateInstanceForLocalFunction() {
      final FuncType type = new TestFuncType(Collections.emptyList(), List.of(WasmValueType.I64));
      final FunctionInfo info = new FunctionInfo(5, "compute", type, false);

      assertEquals(5, info.getIndex(), "index should be 5");
      assertEquals("compute", info.getName(), "name should be 'compute'");
      assertFalse(info.isImport(), "isImport should be false");
      assertTrue(info.isLocal(), "isLocal should be true");
    }

    @Test
    @DisplayName("should allow null name for unnamed functions")
    void shouldAllowNullName() {
      final FuncType type = new TestFuncType(Collections.emptyList(), Collections.emptyList());
      final FunctionInfo info = new FunctionInfo(10, null, type, false);

      assertNull(info.getName(), "name should be null for unnamed functions");
    }

    @Test
    @DisplayName("should allow null funcType")
    void shouldAllowNullFuncType() {
      final FunctionInfo info = new FunctionInfo(0, "test", null, false);

      assertNull(info.getFuncType(), "funcType should be null");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getIndex should return the function index")
    void getIndexShouldReturnFunctionIndex() {
      final FunctionInfo info = new FunctionInfo(42, "fn", null, false);

      assertEquals(42, info.getIndex(), "getIndex should return 42");
    }

    @Test
    @DisplayName("getName should return the function name")
    void getNameShouldReturnFunctionName() {
      final FunctionInfo info = new FunctionInfo(0, "multiply", null, false);

      assertEquals("multiply", info.getName(), "getName should return 'multiply'");
    }

    @Test
    @DisplayName("getFuncType should return the function type")
    void getFuncTypeShouldReturnFunctionType() {
      final FuncType type =
          new TestFuncType(List.of(WasmValueType.F64), List.of(WasmValueType.F64));
      final FunctionInfo info = new FunctionInfo(0, "sqrt", type, false);

      assertEquals(type, info.getFuncType(), "getFuncType should return the type");
    }
  }

  @Nested
  @DisplayName("isImport and isLocal Tests")
  class ImportLocalTests {

    @Test
    @DisplayName("isImport should return true for imported functions")
    void isImportShouldReturnTrueForImported() {
      final FunctionInfo info = new FunctionInfo(0, "env_log", null, true);

      assertTrue(info.isImport(), "isImport should return true");
    }

    @Test
    @DisplayName("isImport should return false for local functions")
    void isImportShouldReturnFalseForLocal() {
      final FunctionInfo info = new FunctionInfo(0, "internal", null, false);

      assertFalse(info.isImport(), "isImport should return false");
    }

    @Test
    @DisplayName("isLocal should return inverse of isImport")
    void isLocalShouldReturnInverseOfIsImport() {
      final FunctionInfo imported = new FunctionInfo(0, "fn", null, true);
      final FunctionInfo local = new FunctionInfo(1, "fn", null, false);

      assertFalse(imported.isLocal(), "isLocal should be false when isImport is true");
      assertTrue(local.isLocal(), "isLocal should be true when isImport is false");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain FunctionInfo prefix")
    void toStringShouldContainFunctionInfoPrefix() {
      final FunctionInfo info = new FunctionInfo(0, "fn", null, false);

      final String result = info.toString();

      assertTrue(result.contains("FunctionInfo{"), "toString should contain 'FunctionInfo{'");
    }

    @Test
    @DisplayName("toString should contain index")
    void toStringShouldContainIndex() {
      final FunctionInfo info = new FunctionInfo(7, "fn", null, false);

      final String result = info.toString();

      assertTrue(result.contains("index=7"), "toString should contain index=7");
    }

    @Test
    @DisplayName("toString should contain name")
    void toStringShouldContainName() {
      final FunctionInfo info = new FunctionInfo(0, "add", null, false);

      final String result = info.toString();

      assertTrue(result.contains("name='add'"), "toString should contain name='add'");
    }

    @Test
    @DisplayName("toString should contain isImport flag")
    void toStringShouldContainIsImportFlag() {
      final FunctionInfo imported = new FunctionInfo(0, "fn", null, true);

      final String result = imported.toString();

      assertTrue(result.contains("isImport=true"), "toString should contain isImport=true");
    }

    @Test
    @DisplayName("toString should handle null name")
    void toStringShouldHandleNullName() {
      final FunctionInfo info = new FunctionInfo(0, null, null, false);

      final String result = info.toString();

      assertNotNull(result, "toString should not throw for null name");
      assertTrue(result.contains("name='null'"), "toString should contain name='null'");
    }
  }
}
