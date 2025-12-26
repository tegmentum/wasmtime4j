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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DebugCapabilities} interface.
 *
 * <p>DebugCapabilities provides information about supported debugging features.
 */
@DisplayName("DebugCapabilities Tests")
class DebugCapabilitiesTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(DebugCapabilities.class.isInterface(), "DebugCapabilities should be an interface");
    }

    @Test
    @DisplayName("should have supportsBreakpoints method")
    void shouldHaveSupportsBreakpointsMethod() throws NoSuchMethodException {
      final Method method = DebugCapabilities.class.getMethod("supportsBreakpoints");
      assertNotNull(method, "supportsBreakpoints method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have supportsStepDebugging method")
    void shouldHaveSupportsStepDebuggingMethod() throws NoSuchMethodException {
      final Method method = DebugCapabilities.class.getMethod("supportsStepDebugging");
      assertNotNull(method, "supportsStepDebugging method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have supportsVariableInspection method")
    void shouldHaveSupportsVariableInspectionMethod() throws NoSuchMethodException {
      final Method method = DebugCapabilities.class.getMethod("supportsVariableInspection");
      assertNotNull(method, "supportsVariableInspection method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have supportsMemoryInspection method")
    void shouldHaveSupportsMemoryInspectionMethod() throws NoSuchMethodException {
      final Method method = DebugCapabilities.class.getMethod("supportsMemoryInspection");
      assertNotNull(method, "supportsMemoryInspection method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have supportsProfiling method")
    void shouldHaveSupportsProfilingMethod() throws NoSuchMethodException {
      final Method method = DebugCapabilities.class.getMethod("supportsProfiling");
      assertNotNull(method, "supportsProfiling method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have supportsSourceMap method")
    void shouldHaveSupportsSourceMapMethod() throws NoSuchMethodException {
      final Method method = DebugCapabilities.class.getMethod("supportsSourceMap");
      assertNotNull(method, "supportsSourceMap method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have supportsDwarf method")
    void shouldHaveSupportsDwarfMethod() throws NoSuchMethodException {
      final Method method = DebugCapabilities.class.getMethod("supportsDwarf");
      assertNotNull(method, "supportsDwarf method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getMaxBreakpoints method")
    void shouldHaveGetMaxBreakpointsMethod() throws NoSuchMethodException {
      final Method method = DebugCapabilities.class.getMethod("getMaxBreakpoints");
      assertNotNull(method, "getMaxBreakpoints method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getSupportedFormats method")
    void shouldHaveGetSupportedFormatsMethod() throws NoSuchMethodException {
      final Method method = DebugCapabilities.class.getMethod("getSupportedFormats");
      assertNotNull(method, "getSupportedFormats method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getDebuggerVersion method")
    void shouldHaveGetDebuggerVersionMethod() throws NoSuchMethodException {
      final Method method = DebugCapabilities.class.getMethod("getDebuggerVersion");
      assertNotNull(method, "getDebuggerVersion method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock capabilities should report breakpoint support")
    void mockCapabilitiesShouldReportBreakpointSupport() {
      final MockDebugCapabilities capabilities = new MockDebugCapabilities();
      assertTrue(capabilities.supportsBreakpoints(), "Should support breakpoints");
    }

    @Test
    @DisplayName("mock capabilities should report step debugging support")
    void mockCapabilitiesShouldReportStepDebuggingSupport() {
      final MockDebugCapabilities capabilities = new MockDebugCapabilities();
      assertTrue(capabilities.supportsStepDebugging(), "Should support step debugging");
    }

    @Test
    @DisplayName("mock capabilities should report variable inspection support")
    void mockCapabilitiesShouldReportVariableInspectionSupport() {
      final MockDebugCapabilities capabilities = new MockDebugCapabilities();
      assertTrue(capabilities.supportsVariableInspection(), "Should support variable inspection");
    }

    @Test
    @DisplayName("mock capabilities should report memory inspection support")
    void mockCapabilitiesShouldReportMemoryInspectionSupport() {
      final MockDebugCapabilities capabilities = new MockDebugCapabilities();
      assertTrue(capabilities.supportsMemoryInspection(), "Should support memory inspection");
    }

    @Test
    @DisplayName("mock capabilities should report profiling support")
    void mockCapabilitiesShouldReportProfilingSupport() {
      final MockDebugCapabilities capabilities = new MockDebugCapabilities();
      assertTrue(capabilities.supportsProfiling(), "Should support profiling");
    }

    @Test
    @DisplayName("mock capabilities should report source map support")
    void mockCapabilitiesShouldReportSourceMapSupport() {
      final MockDebugCapabilities capabilities = new MockDebugCapabilities();
      assertFalse(capabilities.supportsSourceMap(), "Should not support source maps by default");
    }

    @Test
    @DisplayName("mock capabilities should report DWARF support")
    void mockCapabilitiesShouldReportDwarfSupport() {
      final MockDebugCapabilities capabilities = new MockDebugCapabilities();
      assertTrue(capabilities.supportsDwarf(), "Should support DWARF");
    }

    @Test
    @DisplayName("mock capabilities should return max breakpoints")
    void mockCapabilitiesShouldReturnMaxBreakpoints() {
      final MockDebugCapabilities capabilities = new MockDebugCapabilities();
      assertEquals(100, capabilities.getMaxBreakpoints(), "Should return 100 max breakpoints");
    }

    @Test
    @DisplayName("mock capabilities should return supported formats")
    void mockCapabilitiesShouldReturnSupportedFormats() {
      final MockDebugCapabilities capabilities = new MockDebugCapabilities();
      final List<String> formats = capabilities.getSupportedFormats();
      assertNotNull(formats, "Supported formats should not be null");
      assertEquals(2, formats.size(), "Should have 2 supported formats");
      assertTrue(formats.contains("DWARF"), "Should include DWARF");
      assertTrue(formats.contains("WASM"), "Should include WASM");
    }

    @Test
    @DisplayName("mock capabilities should return debugger version")
    void mockCapabilitiesShouldReturnDebuggerVersion() {
      final MockDebugCapabilities capabilities = new MockDebugCapabilities();
      assertEquals("1.0.0", capabilities.getDebuggerVersion(), "Should return version 1.0.0");
    }
  }

  /** Mock implementation of DebugCapabilities for testing. */
  private static class MockDebugCapabilities implements DebugCapabilities {

    @Override
    public boolean supportsBreakpoints() {
      return true;
    }

    @Override
    public boolean supportsStepDebugging() {
      return true;
    }

    @Override
    public boolean supportsVariableInspection() {
      return true;
    }

    @Override
    public boolean supportsMemoryInspection() {
      return true;
    }

    @Override
    public boolean supportsProfiling() {
      return true;
    }

    @Override
    public boolean supportsSourceMap() {
      return false;
    }

    @Override
    public boolean supportsDwarf() {
      return true;
    }

    @Override
    public int getMaxBreakpoints() {
      return 100;
    }

    @Override
    public List<String> getSupportedFormats() {
      return Arrays.asList("DWARF", "WASM");
    }

    @Override
    public String getDebuggerVersion() {
      return "1.0.0";
    }
  }
}
