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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentDebugInfo} interface.
 *
 * <p>ComponentDebugInfo provides debug information for WebAssembly components including symbols,
 * source maps, execution state, and memory layout.
 */
@DisplayName("ComponentDebugInfo Tests")
class ComponentDebugInfoTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentDebugInfo.class.getModifiers()),
          "ComponentDebugInfo should be public");
      assertTrue(
          ComponentDebugInfo.class.isInterface(), "ComponentDebugInfo should be an interface");
    }

    @Test
    @DisplayName("should have nested interfaces")
    void shouldHaveNestedInterfaces() {
      final var declaredClasses = ComponentDebugInfo.class.getDeclaredClasses();
      assertTrue(declaredClasses.length > 0, "Should have nested interfaces");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getComponentId method")
    void shouldHaveGetComponentIdMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.class.getMethod("getComponentId");
      assertNotNull(method, "getComponentId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getComponentName method")
    void shouldHaveGetComponentNameMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.class.getMethod("getComponentName");
      assertNotNull(method, "getComponentName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getSymbols method")
    void shouldHaveGetSymbolsMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.class.getMethod("getSymbols");
      assertNotNull(method, "getSymbols method should exist");
      assertEquals(
          ComponentDebugInfo.DebugSymbols.class,
          method.getReturnType(),
          "Should return DebugSymbols");
    }

    @Test
    @DisplayName("should have getSourceMaps method")
    void shouldHaveGetSourceMapsMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.class.getMethod("getSourceMaps");
      assertNotNull(method, "getSourceMaps method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getExecutionState method")
    void shouldHaveGetExecutionStateMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.class.getMethod("getExecutionState");
      assertNotNull(method, "getExecutionState method should exist");
    }

    @Test
    @DisplayName("should have getVariables method")
    void shouldHaveGetVariablesMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.class.getMethod("getVariables");
      assertNotNull(method, "getVariables method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getFunctions method")
    void shouldHaveGetFunctionsMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.class.getMethod("getFunctions");
      assertNotNull(method, "getFunctions method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getMemoryLayout method")
    void shouldHaveGetMemoryLayoutMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.class.getMethod("getMemoryLayout");
      assertNotNull(method, "getMemoryLayout method should exist");
      assertEquals(
          ComponentDebugInfo.MemoryLayout.class,
          method.getReturnType(),
          "Should return MemoryLayout");
    }

    @Test
    @DisplayName("should have getStackTrace method")
    void shouldHaveGetStackTraceMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.class.getMethod("getStackTrace");
      assertNotNull(method, "getStackTrace method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getBreakpoints method")
    void shouldHaveGetBreakpointsMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.class.getMethod("getBreakpoints");
      assertNotNull(method, "getBreakpoints method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("should have DebugSymbols interface")
    void shouldHaveDebugSymbolsInterface() {
      assertNotNull(ComponentDebugInfo.DebugSymbols.class, "DebugSymbols should exist");
      assertTrue(
          ComponentDebugInfo.DebugSymbols.class.isInterface(),
          "DebugSymbols should be an interface");
    }

    @Test
    @DisplayName("should have Symbol interface")
    void shouldHaveSymbolInterface() {
      assertNotNull(ComponentDebugInfo.Symbol.class, "Symbol should exist");
      assertTrue(ComponentDebugInfo.Symbol.class.isInterface(), "Symbol should be an interface");
    }

    @Test
    @DisplayName("should have SourceMap interface")
    void shouldHaveSourceMapInterface() {
      assertNotNull(ComponentDebugInfo.SourceMap.class, "SourceMap should exist");
      assertTrue(
          ComponentDebugInfo.SourceMap.class.isInterface(), "SourceMap should be an interface");
    }

    @Test
    @DisplayName("should have LineMapping interface")
    void shouldHaveLineMappingInterface() {
      assertNotNull(ComponentDebugInfo.LineMapping.class, "LineMapping should exist");
      assertTrue(
          ComponentDebugInfo.LineMapping.class.isInterface(), "LineMapping should be an interface");
    }

    @Test
    @DisplayName("should have VariableInfo interface")
    void shouldHaveVariableInfoInterface() {
      assertNotNull(ComponentDebugInfo.VariableInfo.class, "VariableInfo should exist");
      assertTrue(
          ComponentDebugInfo.VariableInfo.class.isInterface(),
          "VariableInfo should be an interface");
    }

    @Test
    @DisplayName("should have FunctionInfo interface")
    void shouldHaveFunctionInfoInterface() {
      assertNotNull(ComponentDebugInfo.FunctionInfo.class, "FunctionInfo should exist");
      assertTrue(
          ComponentDebugInfo.FunctionInfo.class.isInterface(),
          "FunctionInfo should be an interface");
    }

    @Test
    @DisplayName("should have MemoryLayout interface")
    void shouldHaveMemoryLayoutInterface() {
      assertNotNull(ComponentDebugInfo.MemoryLayout.class, "MemoryLayout should exist");
      assertTrue(
          ComponentDebugInfo.MemoryLayout.class.isInterface(),
          "MemoryLayout should be an interface");
    }

    @Test
    @DisplayName("should have StackFrame interface")
    void shouldHaveStackFrameInterface() {
      assertNotNull(ComponentDebugInfo.StackFrame.class, "StackFrame should exist");
      assertTrue(
          ComponentDebugInfo.StackFrame.class.isInterface(), "StackFrame should be an interface");
    }

    @Test
    @DisplayName("should have Breakpoint interface")
    void shouldHaveBreakpointInterface() {
      assertNotNull(ComponentDebugInfo.Breakpoint.class, "Breakpoint should exist");
      assertTrue(
          ComponentDebugInfo.Breakpoint.class.isInterface(), "Breakpoint should be an interface");
    }
  }

  @Nested
  @DisplayName("Enum Tests")
  class EnumTests {

    @Test
    @DisplayName("should have SymbolType enum")
    void shouldHaveSymbolTypeEnum() {
      assertNotNull(ComponentDebugInfo.SymbolType.class, "SymbolType should exist");
      assertTrue(ComponentDebugInfo.SymbolType.class.isEnum(), "SymbolType should be an enum");
    }

    @Test
    @DisplayName("SymbolType should have all expected values")
    void symbolTypeShouldHaveAllExpectedValues() {
      final var values = ComponentDebugInfo.SymbolType.values();
      assertEquals(5, values.length, "SymbolType should have 5 values");

      assertNotNull(ComponentDebugInfo.SymbolType.FUNCTION, "FUNCTION should exist");
      assertNotNull(ComponentDebugInfo.SymbolType.VARIABLE, "VARIABLE should exist");
      assertNotNull(ComponentDebugInfo.SymbolType.TYPE, "TYPE should exist");
      assertNotNull(ComponentDebugInfo.SymbolType.LABEL, "LABEL should exist");
      assertNotNull(ComponentDebugInfo.SymbolType.SECTION, "SECTION should exist");
    }

    @Test
    @DisplayName("should have VariableScope enum")
    void shouldHaveVariableScopeEnum() {
      assertNotNull(ComponentDebugInfo.VariableScope.class, "VariableScope should exist");
      assertTrue(
          ComponentDebugInfo.VariableScope.class.isEnum(), "VariableScope should be an enum");
    }

    @Test
    @DisplayName("VariableScope should have all expected values")
    void variableScopeShouldHaveAllExpectedValues() {
      final var values = ComponentDebugInfo.VariableScope.values();
      assertEquals(4, values.length, "VariableScope should have 4 values");

      assertNotNull(ComponentDebugInfo.VariableScope.GLOBAL, "GLOBAL should exist");
      assertNotNull(ComponentDebugInfo.VariableScope.FUNCTION, "FUNCTION should exist");
      assertNotNull(ComponentDebugInfo.VariableScope.BLOCK, "BLOCK should exist");
      assertNotNull(ComponentDebugInfo.VariableScope.PARAMETER, "PARAMETER should exist");
    }

    @Test
    @DisplayName("should have VariableLocation enum")
    void shouldHaveVariableLocationEnum() {
      assertNotNull(ComponentDebugInfo.VariableLocation.class, "VariableLocation should exist");
      assertTrue(
          ComponentDebugInfo.VariableLocation.class.isEnum(), "VariableLocation should be an enum");
    }

    @Test
    @DisplayName("VariableLocation should have all expected values")
    void variableLocationShouldHaveAllExpectedValues() {
      final var values = ComponentDebugInfo.VariableLocation.values();
      assertEquals(4, values.length, "VariableLocation should have 4 values");

      assertNotNull(ComponentDebugInfo.VariableLocation.REGISTER, "REGISTER should exist");
      assertNotNull(ComponentDebugInfo.VariableLocation.MEMORY, "MEMORY should exist");
      assertNotNull(ComponentDebugInfo.VariableLocation.STACK, "STACK should exist");
      assertNotNull(ComponentDebugInfo.VariableLocation.CONSTANT, "CONSTANT should exist");
    }

    @Test
    @DisplayName("should have SegmentType enum")
    void shouldHaveSegmentTypeEnum() {
      assertNotNull(ComponentDebugInfo.SegmentType.class, "SegmentType should exist");
      assertTrue(ComponentDebugInfo.SegmentType.class.isEnum(), "SegmentType should be an enum");
    }

    @Test
    @DisplayName("SegmentType should have all expected values")
    void segmentTypeShouldHaveAllExpectedValues() {
      final var values = ComponentDebugInfo.SegmentType.values();
      assertEquals(4, values.length, "SegmentType should have 4 values");

      assertNotNull(ComponentDebugInfo.SegmentType.CODE, "CODE should exist");
      assertNotNull(ComponentDebugInfo.SegmentType.DATA, "DATA should exist");
      assertNotNull(ComponentDebugInfo.SegmentType.HEAP, "HEAP should exist");
      assertNotNull(ComponentDebugInfo.SegmentType.STACK, "STACK should exist");
    }
  }

  @Nested
  @DisplayName("Symbol Interface Tests")
  class SymbolInterfaceTests {

    @Test
    @DisplayName("Symbol should have getName method")
    void symbolShouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.Symbol.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("Symbol should have getAddress method")
    void symbolShouldHaveGetAddressMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.Symbol.class.getMethod("getAddress");
      assertNotNull(method, "getAddress method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("Symbol should have getSize method")
    void symbolShouldHaveGetSizeMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.Symbol.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("Symbol should have getType method")
    void symbolShouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.Symbol.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(
          ComponentDebugInfo.SymbolType.class, method.getReturnType(), "Should return SymbolType");
    }
  }

  @Nested
  @DisplayName("StackFrame Interface Tests")
  class StackFrameInterfaceTests {

    @Test
    @DisplayName("StackFrame should have getFunctionName method")
    void stackFrameShouldHaveGetFunctionNameMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.StackFrame.class.getMethod("getFunctionName");
      assertNotNull(method, "getFunctionName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("StackFrame should have getInstructionPointer method")
    void stackFrameShouldHaveGetInstructionPointerMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.StackFrame.class.getMethod("getInstructionPointer");
      assertNotNull(method, "getInstructionPointer method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("StackFrame should have getDepth method")
    void stackFrameShouldHaveGetDepthMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.StackFrame.class.getMethod("getDepth");
      assertNotNull(method, "getDepth method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Breakpoint Interface Tests")
  class BreakpointInterfaceTests {

    @Test
    @DisplayName("Breakpoint should have getId method")
    void breakpointShouldHaveGetIdMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.Breakpoint.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("Breakpoint should have getAddress method")
    void breakpointShouldHaveGetAddressMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.Breakpoint.class.getMethod("getAddress");
      assertNotNull(method, "getAddress method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("Breakpoint should have getCondition method")
    void breakpointShouldHaveGetConditionMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.Breakpoint.class.getMethod("getCondition");
      assertNotNull(method, "getCondition method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("Breakpoint should have isEnabled method")
    void breakpointShouldHaveIsEnabledMethod() throws NoSuchMethodException {
      final Method method = ComponentDebugInfo.Breakpoint.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }
}
