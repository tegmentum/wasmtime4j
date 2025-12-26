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

package ai.tegmentum.wasmtime4j.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.DebugInfo.AddressDebugInfo;
import ai.tegmentum.wasmtime4j.debug.DebugInfo.FunctionDebugInfo;
import ai.tegmentum.wasmtime4j.debug.DebugInfo.GlobalVariableInfo;
import ai.tegmentum.wasmtime4j.debug.DebugInfo.TypeInfo;
import ai.tegmentum.wasmtime4j.debug.DebugInfo.TypeKind;
import ai.tegmentum.wasmtime4j.debug.DebugInfo.VariableDebugInfo;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DebugInfo} interface.
 *
 * <p>DebugInfo provides comprehensive debug information for WebAssembly modules including function
 * info, global variables, type information, and address-to-source mappings.
 */
@DisplayName("DebugInfo Tests")
class DebugInfoTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(DebugInfo.class.isInterface(), "DebugInfo should be an interface");
    }

    @Test
    @DisplayName("should have getModuleName method")
    void shouldHaveGetModuleNameMethod() throws NoSuchMethodException {
      final Method method = DebugInfo.class.getMethod("getModuleName");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getFunctionDebugInfo method")
    void shouldHaveGetFunctionDebugInfoMethod() throws NoSuchMethodException {
      final Method method = DebugInfo.class.getMethod("getFunctionDebugInfo");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getGlobalVariables method")
    void shouldHaveGetGlobalVariablesMethod() throws NoSuchMethodException {
      final Method method = DebugInfo.class.getMethod("getGlobalVariables");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getTypeInfo method")
    void shouldHaveGetTypeInfoMethod() throws NoSuchMethodException {
      final Method method = DebugInfo.class.getMethod("getTypeInfo");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getDebugInfoAtAddress method")
    void shouldHaveGetDebugInfoAtAddressMethod() throws NoSuchMethodException {
      final Method method = DebugInfo.class.getMethod("getDebugInfoAtAddress", long.class);
      assertEquals(
          AddressDebugInfo.class, method.getReturnType(), "Should return AddressDebugInfo");
    }

    @Test
    @DisplayName("should have exactly five methods")
    void shouldHaveExactlyFiveMethods() {
      final Method[] methods = DebugInfo.class.getDeclaredMethods();
      assertEquals(5, methods.length, "DebugInfo should have exactly 5 methods");
    }
  }

  @Nested
  @DisplayName("TypeKind Enum Tests")
  class TypeKindEnumTests {

    @Test
    @DisplayName("should have PRIMITIVE value")
    void shouldHavePrimitiveValue() {
      assertNotNull(TypeKind.valueOf("PRIMITIVE"), "PRIMITIVE should exist");
    }

    @Test
    @DisplayName("should have STRUCT value")
    void shouldHaveStructValue() {
      assertNotNull(TypeKind.valueOf("STRUCT"), "STRUCT should exist");
    }

    @Test
    @DisplayName("should have ARRAY value")
    void shouldHaveArrayValue() {
      assertNotNull(TypeKind.valueOf("ARRAY"), "ARRAY should exist");
    }

    @Test
    @DisplayName("should have POINTER value")
    void shouldHavePointerValue() {
      assertNotNull(TypeKind.valueOf("POINTER"), "POINTER should exist");
    }

    @Test
    @DisplayName("should have FUNCTION value")
    void shouldHaveFunctionValue() {
      assertNotNull(TypeKind.valueOf("FUNCTION"), "FUNCTION should exist");
    }

    @Test
    @DisplayName("should have exactly five values")
    void shouldHaveExactlyFiveValues() {
      assertEquals(5, TypeKind.values().length, "TypeKind should have exactly 5 values");
    }
  }

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("should have FunctionDebugInfo nested interface")
    void shouldHaveFunctionDebugInfoNestedInterface() {
      boolean found = false;
      for (final Class<?> inner : DebugInfo.class.getDeclaredClasses()) {
        if ("FunctionDebugInfo".equals(inner.getSimpleName()) && inner.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have FunctionDebugInfo nested interface");
    }

    @Test
    @DisplayName("should have VariableDebugInfo nested interface")
    void shouldHaveVariableDebugInfoNestedInterface() {
      boolean found = false;
      for (final Class<?> inner : DebugInfo.class.getDeclaredClasses()) {
        if ("VariableDebugInfo".equals(inner.getSimpleName()) && inner.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have VariableDebugInfo nested interface");
    }

    @Test
    @DisplayName("should have GlobalVariableInfo nested interface")
    void shouldHaveGlobalVariableInfoNestedInterface() {
      boolean found = false;
      for (final Class<?> inner : DebugInfo.class.getDeclaredClasses()) {
        if ("GlobalVariableInfo".equals(inner.getSimpleName()) && inner.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have GlobalVariableInfo nested interface");
    }

    @Test
    @DisplayName("should have TypeInfo nested interface")
    void shouldHaveTypeInfoNestedInterface() {
      boolean found = false;
      for (final Class<?> inner : DebugInfo.class.getDeclaredClasses()) {
        if ("TypeInfo".equals(inner.getSimpleName()) && inner.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have TypeInfo nested interface");
    }

    @Test
    @DisplayName("should have AddressDebugInfo nested interface")
    void shouldHaveAddressDebugInfoNestedInterface() {
      boolean found = false;
      for (final Class<?> inner : DebugInfo.class.getDeclaredClasses()) {
        if ("AddressDebugInfo".equals(inner.getSimpleName()) && inner.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have AddressDebugInfo nested interface");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock should return module name")
    void mockShouldReturnModuleName() {
      final MockDebugInfo info = new MockDebugInfo("test_module");

      assertEquals("test_module", info.getModuleName(), "Module name should match");
    }

    @Test
    @DisplayName("mock should return function debug info list")
    void mockShouldReturnFunctionDebugInfoList() {
      final MockDebugInfo info = new MockDebugInfo("module");
      info.addFunction(new MockFunctionDebugInfo("main", 0x1000L, 0x1100L));
      info.addFunction(new MockFunctionDebugInfo("helper", 0x1100L, 0x1200L));

      final List<FunctionDebugInfo> functions = info.getFunctionDebugInfo();

      assertEquals(2, functions.size(), "Should have 2 functions");
      assertEquals("main", functions.get(0).getFunctionName(), "First function name should match");
    }

    @Test
    @DisplayName("mock should return global variables")
    void mockShouldReturnGlobalVariables() {
      final MockDebugInfo info = new MockDebugInfo("module");
      info.addGlobal(new MockGlobalVariableInfo("counter", "i32", 0x2000L));

      final List<GlobalVariableInfo> globals = info.getGlobalVariables();

      assertEquals(1, globals.size(), "Should have 1 global");
      assertEquals("counter", globals.get(0).getName(), "Global name should match");
      assertEquals("i32", globals.get(0).getType(), "Global type should match");
      assertEquals(0x2000L, globals.get(0).getAddress(), "Global address should match");
    }

    @Test
    @DisplayName("mock should return type info")
    void mockShouldReturnTypeInfo() {
      final MockDebugInfo info = new MockDebugInfo("module");
      info.addType(new MockTypeInfo("i32", 4, TypeKind.PRIMITIVE));
      info.addType(new MockTypeInfo("Point", 8, TypeKind.STRUCT));

      final List<TypeInfo> types = info.getTypeInfo();

      assertEquals(2, types.size(), "Should have 2 types");
      assertEquals(TypeKind.PRIMITIVE, types.get(0).getTypeKind(), "First type kind should match");
      assertEquals(TypeKind.STRUCT, types.get(1).getTypeKind(), "Second type kind should match");
    }

    @Test
    @DisplayName("mock should return address debug info")
    void mockShouldReturnAddressDebugInfo() {
      final MockDebugInfo info = new MockDebugInfo("module");
      info.addAddressInfo(0x1050L, new MockAddressDebugInfo("main.c", 42, 10, "main"));

      final AddressDebugInfo addrInfo = info.getDebugInfoAtAddress(0x1050L);

      assertNotNull(addrInfo, "Address debug info should not be null");
      assertEquals("main.c", addrInfo.getSourceFile(), "Source file should match");
      assertEquals(42, addrInfo.getLineNumber(), "Line number should match");
      assertEquals(10, addrInfo.getColumnNumber(), "Column number should match");
      assertEquals("main", addrInfo.getFunctionName(), "Function name should match");
    }

    @Test
    @DisplayName("mock should return null for unmapped address")
    void mockShouldReturnNullForUnmappedAddress() {
      final MockDebugInfo info = new MockDebugInfo("module");

      assertNull(info.getDebugInfoAtAddress(0x9999L), "Should return null for unmapped address");
    }

    @Test
    @DisplayName("function debug info should have locals and parameters")
    void functionDebugInfoShouldHaveLocalsAndParameters() {
      final MockFunctionDebugInfo func = new MockFunctionDebugInfo("test", 0L, 100L);
      func.addLocal(new MockVariableDebugInfo("x", "i32", "local0"));
      func.addParameter(new MockVariableDebugInfo("arg", "i64", "param0"));

      assertEquals(1, func.getLocalVariables().size(), "Should have 1 local");
      assertEquals(1, func.getParameters().size(), "Should have 1 parameter");
      assertEquals("x", func.getLocalVariables().get(0).getName(), "Local name should match");
      assertEquals("arg", func.getParameters().get(0).getName(), "Parameter name should match");
    }

    @Test
    @DisplayName("variable debug info should have all properties")
    void variableDebugInfoShouldHaveAllProperties() {
      final MockVariableDebugInfo var = new MockVariableDebugInfo("count", "i32", "stack+4");

      assertEquals("count", var.getName(), "Name should match");
      assertEquals("i32", var.getType(), "Type should match");
      assertEquals("stack+4", var.getLocation(), "Location should match");
    }
  }

  /** Mock implementation of DebugInfo for testing. */
  private static class MockDebugInfo implements DebugInfo {
    private final String moduleName;
    private final List<FunctionDebugInfo> functions = new ArrayList<>();
    private final List<GlobalVariableInfo> globals = new ArrayList<>();
    private final List<TypeInfo> types = new ArrayList<>();
    private final Map<Long, AddressDebugInfo> addressInfo = new HashMap<>();

    MockDebugInfo(final String moduleName) {
      this.moduleName = moduleName;
    }

    @Override
    public String getModuleName() {
      return moduleName;
    }

    @Override
    public List<FunctionDebugInfo> getFunctionDebugInfo() {
      return new ArrayList<>(functions);
    }

    @Override
    public List<GlobalVariableInfo> getGlobalVariables() {
      return new ArrayList<>(globals);
    }

    @Override
    public List<TypeInfo> getTypeInfo() {
      return new ArrayList<>(types);
    }

    @Override
    public AddressDebugInfo getDebugInfoAtAddress(final long address) {
      return addressInfo.get(address);
    }

    public void addFunction(final FunctionDebugInfo func) {
      functions.add(func);
    }

    public void addGlobal(final GlobalVariableInfo global) {
      globals.add(global);
    }

    public void addType(final TypeInfo type) {
      types.add(type);
    }

    public void addAddressInfo(final long address, final AddressDebugInfo info) {
      addressInfo.put(address, info);
    }
  }

  /** Mock implementation of FunctionDebugInfo for testing. */
  private static class MockFunctionDebugInfo implements FunctionDebugInfo {
    private final String name;
    private final long startAddress;
    private final long endAddress;
    private final List<VariableDebugInfo> locals = new ArrayList<>();
    private final List<VariableDebugInfo> parameters = new ArrayList<>();

    MockFunctionDebugInfo(final String name, final long startAddress, final long endAddress) {
      this.name = name;
      this.startAddress = startAddress;
      this.endAddress = endAddress;
    }

    @Override
    public String getFunctionName() {
      return name;
    }

    @Override
    public long getStartAddress() {
      return startAddress;
    }

    @Override
    public long getEndAddress() {
      return endAddress;
    }

    @Override
    public List<VariableDebugInfo> getLocalVariables() {
      return new ArrayList<>(locals);
    }

    @Override
    public List<VariableDebugInfo> getParameters() {
      return new ArrayList<>(parameters);
    }

    public void addLocal(final VariableDebugInfo var) {
      locals.add(var);
    }

    public void addParameter(final VariableDebugInfo var) {
      parameters.add(var);
    }
  }

  /** Mock implementation of VariableDebugInfo for testing. */
  private static class MockVariableDebugInfo implements VariableDebugInfo {
    private final String name;
    private final String type;
    private final String location;

    MockVariableDebugInfo(final String name, final String type, final String location) {
      this.name = name;
      this.type = type;
      this.location = location;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getType() {
      return type;
    }

    @Override
    public String getLocation() {
      return location;
    }
  }

  /** Mock implementation of GlobalVariableInfo for testing. */
  private static class MockGlobalVariableInfo implements GlobalVariableInfo {
    private final String name;
    private final String type;
    private final long address;

    MockGlobalVariableInfo(final String name, final String type, final long address) {
      this.name = name;
      this.type = type;
      this.address = address;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getType() {
      return type;
    }

    @Override
    public long getAddress() {
      return address;
    }
  }

  /** Mock implementation of TypeInfo for testing. */
  private static class MockTypeInfo implements TypeInfo {
    private final String typeName;
    private final int size;
    private final TypeKind typeKind;

    MockTypeInfo(final String typeName, final int size, final TypeKind typeKind) {
      this.typeName = typeName;
      this.size = size;
      this.typeKind = typeKind;
    }

    @Override
    public String getTypeName() {
      return typeName;
    }

    @Override
    public int getSize() {
      return size;
    }

    @Override
    public TypeKind getTypeKind() {
      return typeKind;
    }
  }

  /** Mock implementation of AddressDebugInfo for testing. */
  private static class MockAddressDebugInfo implements AddressDebugInfo {
    private final String sourceFile;
    private final int lineNumber;
    private final int columnNumber;
    private final String functionName;

    MockAddressDebugInfo(
        final String sourceFile,
        final int lineNumber,
        final int columnNumber,
        final String functionName) {
      this.sourceFile = sourceFile;
      this.lineNumber = lineNumber;
      this.columnNumber = columnNumber;
      this.functionName = functionName;
    }

    @Override
    public String getSourceFile() {
      return sourceFile;
    }

    @Override
    public int getLineNumber() {
      return lineNumber;
    }

    @Override
    public int getColumnNumber() {
      return columnNumber;
    }

    @Override
    public String getFunctionName() {
      return functionName;
    }
  }
}
