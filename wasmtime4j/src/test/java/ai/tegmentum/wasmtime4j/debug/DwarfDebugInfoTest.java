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

import ai.tegmentum.wasmtime4j.debug.DwarfDebugInfo.CompilationUnit;
import ai.tegmentum.wasmtime4j.debug.DwarfDebugInfo.FunctionInfo;
import ai.tegmentum.wasmtime4j.debug.DwarfDebugInfo.LineNumberInfo;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DwarfDebugInfo} interface.
 *
 * <p>DwarfDebugInfo provides DWARF debugging information for WebAssembly modules, enabling source
 * level debugging with compilation units, function info, and line number mappings.
 */
@DisplayName("DwarfDebugInfo Tests")
class DwarfDebugInfoTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(DwarfDebugInfo.class.isInterface(), "DwarfDebugInfo should be an interface");
    }

    @Test
    @DisplayName("should have getVersion method")
    void shouldHaveGetVersionMethod() throws NoSuchMethodException {
      final Method method = DwarfDebugInfo.class.getMethod("getVersion");
      assertNotNull(method, "getVersion method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getCompilationUnitCount method")
    void shouldHaveGetCompilationUnitCountMethod() throws NoSuchMethodException {
      final Method method = DwarfDebugInfo.class.getMethod("getCompilationUnitCount");
      assertNotNull(method, "getCompilationUnitCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getCompilationUnits method")
    void shouldHaveGetCompilationUnitsMethod() throws NoSuchMethodException {
      final Method method = DwarfDebugInfo.class.getMethod("getCompilationUnits");
      assertNotNull(method, "getCompilationUnits method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have findFunction method")
    void shouldHaveFindFunctionMethod() throws NoSuchMethodException {
      final Method method = DwarfDebugInfo.class.getMethod("findFunction", long.class);
      assertNotNull(method, "findFunction method should exist");
      assertEquals(FunctionInfo.class, method.getReturnType(), "Should return FunctionInfo");
    }

    @Test
    @DisplayName("should have getLineNumberInfo method")
    void shouldHaveGetLineNumberInfoMethod() throws NoSuchMethodException {
      final Method method = DwarfDebugInfo.class.getMethod("getLineNumberInfo", long.class);
      assertNotNull(method, "getLineNumberInfo method should exist");
      assertEquals(LineNumberInfo.class, method.getReturnType(), "Should return LineNumberInfo");
    }

    @Test
    @DisplayName("should have exactly five methods")
    void shouldHaveExactlyFiveMethods() {
      final Method[] methods = DwarfDebugInfo.class.getDeclaredMethods();
      assertEquals(5, methods.length, "DwarfDebugInfo should have exactly 5 methods");
    }
  }

  @Nested
  @DisplayName("CompilationUnit Nested Interface Tests")
  class CompilationUnitInterfaceTests {

    @Test
    @DisplayName("should have CompilationUnit nested interface")
    void shouldHaveCompilationUnitNestedInterface() {
      boolean found = false;
      for (final Class<?> inner : DwarfDebugInfo.class.getDeclaredClasses()) {
        if ("CompilationUnit".equals(inner.getSimpleName()) && inner.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have CompilationUnit nested interface");
    }

    @Test
    @DisplayName("CompilationUnit should have getName method")
    void compilationUnitShouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = CompilationUnit.class.getMethod("getName");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("CompilationUnit should have getProducer method")
    void compilationUnitShouldHaveGetProducerMethod() throws NoSuchMethodException {
      final Method method = CompilationUnit.class.getMethod("getProducer");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("CompilationUnit should have getBaseAddress method")
    void compilationUnitShouldHaveGetBaseAddressMethod() throws NoSuchMethodException {
      final Method method = CompilationUnit.class.getMethod("getBaseAddress");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("FunctionInfo Nested Interface Tests")
  class FunctionInfoInterfaceTests {

    @Test
    @DisplayName("should have FunctionInfo nested interface")
    void shouldHaveFunctionInfoNestedInterface() {
      boolean found = false;
      for (final Class<?> inner : DwarfDebugInfo.class.getDeclaredClasses()) {
        if ("FunctionInfo".equals(inner.getSimpleName()) && inner.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have FunctionInfo nested interface");
    }

    @Test
    @DisplayName("FunctionInfo should have getName method")
    void functionInfoShouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = FunctionInfo.class.getMethod("getName");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("FunctionInfo should have getStartAddress method")
    void functionInfoShouldHaveGetStartAddressMethod() throws NoSuchMethodException {
      final Method method = FunctionInfo.class.getMethod("getStartAddress");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("FunctionInfo should have getEndAddress method")
    void functionInfoShouldHaveGetEndAddressMethod() throws NoSuchMethodException {
      final Method method = FunctionInfo.class.getMethod("getEndAddress");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("LineNumberInfo Nested Interface Tests")
  class LineNumberInfoInterfaceTests {

    @Test
    @DisplayName("should have LineNumberInfo nested interface")
    void shouldHaveLineNumberInfoNestedInterface() {
      boolean found = false;
      for (final Class<?> inner : DwarfDebugInfo.class.getDeclaredClasses()) {
        if ("LineNumberInfo".equals(inner.getSimpleName()) && inner.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have LineNumberInfo nested interface");
    }

    @Test
    @DisplayName("LineNumberInfo should have getFileName method")
    void lineNumberInfoShouldHaveGetFileNameMethod() throws NoSuchMethodException {
      final Method method = LineNumberInfo.class.getMethod("getFileName");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("LineNumberInfo should have getLineNumber method")
    void lineNumberInfoShouldHaveGetLineNumberMethod() throws NoSuchMethodException {
      final Method method = LineNumberInfo.class.getMethod("getLineNumber");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("LineNumberInfo should have getColumnNumber method")
    void lineNumberInfoShouldHaveGetColumnNumberMethod() throws NoSuchMethodException {
      final Method method = LineNumberInfo.class.getMethod("getColumnNumber");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock should return DWARF version")
    void mockShouldReturnDwarfVersion() {
      final MockDwarfDebugInfo info = new MockDwarfDebugInfo(4);

      assertEquals(4, info.getVersion(), "Version should be 4");
    }

    @Test
    @DisplayName("mock should return compilation unit count")
    void mockShouldReturnCompilationUnitCount() {
      final MockDwarfDebugInfo info = new MockDwarfDebugInfo(4);
      info.addCompilationUnit(new MockCompilationUnit("main.c", "clang 12.0", 0x1000L));
      info.addCompilationUnit(new MockCompilationUnit("utils.c", "clang 12.0", 0x2000L));

      assertEquals(2, info.getCompilationUnitCount(), "Should have 2 compilation units");
    }

    @Test
    @DisplayName("mock should return compilation units list")
    void mockShouldReturnCompilationUnitsList() {
      final MockDwarfDebugInfo info = new MockDwarfDebugInfo(4);
      info.addCompilationUnit(new MockCompilationUnit("main.c", "clang 12.0", 0x1000L));

      final List<CompilationUnit> units = info.getCompilationUnits();

      assertEquals(1, units.size(), "Should have 1 unit");
      assertEquals("main.c", units.get(0).getName(), "Unit name should match");
    }

    @Test
    @DisplayName("mock should find function by address")
    void mockShouldFindFunctionByAddress() {
      final MockDwarfDebugInfo info = new MockDwarfDebugInfo(4);
      info.addFunction(0x1000L, new MockFunctionInfo("main", 0x1000L, 0x1100L));

      final FunctionInfo func = info.findFunction(0x1050L);

      assertNotNull(func, "Function should be found");
      assertEquals("main", func.getName(), "Function name should match");
    }

    @Test
    @DisplayName("mock should return null for address outside functions")
    void mockShouldReturnNullForUnknownAddress() {
      final MockDwarfDebugInfo info = new MockDwarfDebugInfo(4);
      info.addFunction(0x1000L, new MockFunctionInfo("main", 0x1000L, 0x1100L));

      assertNull(info.findFunction(0x5000L), "Should return null for unknown address");
    }

    @Test
    @DisplayName("mock should return line number info")
    void mockShouldReturnLineNumberInfo() {
      final MockDwarfDebugInfo info = new MockDwarfDebugInfo(4);
      info.addLineInfo(0x1000L, new MockLineNumberInfo("main.c", 10, 5));

      final LineNumberInfo lineInfo = info.getLineNumberInfo(0x1000L);

      assertNotNull(lineInfo, "Line info should be found");
      assertEquals("main.c", lineInfo.getFileName(), "File name should match");
      assertEquals(10, lineInfo.getLineNumber(), "Line number should match");
      assertEquals(5, lineInfo.getColumnNumber(), "Column number should match");
    }

    @Test
    @DisplayName("compilation unit should return all properties")
    void compilationUnitShouldReturnAllProperties() {
      final MockCompilationUnit unit = new MockCompilationUnit("test.c", "gcc 11.0", 0x3000L);

      assertEquals("test.c", unit.getName(), "Name should match");
      assertEquals("gcc 11.0", unit.getProducer(), "Producer should match");
      assertEquals(0x3000L, unit.getBaseAddress(), "Base address should match");
    }

    @Test
    @DisplayName("function info should return address range")
    void functionInfoShouldReturnAddressRange() {
      final MockFunctionInfo func = new MockFunctionInfo("helper", 0x2000L, 0x2500L);

      assertEquals("helper", func.getName(), "Name should match");
      assertEquals(0x2000L, func.getStartAddress(), "Start address should match");
      assertEquals(0x2500L, func.getEndAddress(), "End address should match");
    }
  }

  /** Mock implementation of DwarfDebugInfo for testing. */
  private static class MockDwarfDebugInfo implements DwarfDebugInfo {
    private final int version;
    private final List<CompilationUnit> compilationUnits = new ArrayList<>();
    private final Map<Long, FunctionInfo> functions = new HashMap<>();
    private final Map<Long, LineNumberInfo> lineInfo = new HashMap<>();

    MockDwarfDebugInfo(final int version) {
      this.version = version;
    }

    @Override
    public int getVersion() {
      return version;
    }

    @Override
    public int getCompilationUnitCount() {
      return compilationUnits.size();
    }

    @Override
    public List<CompilationUnit> getCompilationUnits() {
      return new ArrayList<>(compilationUnits);
    }

    @Override
    public FunctionInfo findFunction(final long address) {
      for (final FunctionInfo func : functions.values()) {
        if (address >= func.getStartAddress() && address < func.getEndAddress()) {
          return func;
        }
      }
      return null;
    }

    @Override
    public LineNumberInfo getLineNumberInfo(final long address) {
      return lineInfo.get(address);
    }

    public void addCompilationUnit(final CompilationUnit unit) {
      compilationUnits.add(unit);
    }

    public void addFunction(final long address, final FunctionInfo func) {
      functions.put(address, func);
    }

    public void addLineInfo(final long address, final LineNumberInfo info) {
      lineInfo.put(address, info);
    }
  }

  /** Mock implementation of CompilationUnit for testing. */
  private static class MockCompilationUnit implements CompilationUnit {
    private final String name;
    private final String producer;
    private final long baseAddress;

    MockCompilationUnit(final String name, final String producer, final long baseAddress) {
      this.name = name;
      this.producer = producer;
      this.baseAddress = baseAddress;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getProducer() {
      return producer;
    }

    @Override
    public long getBaseAddress() {
      return baseAddress;
    }
  }

  /** Mock implementation of FunctionInfo for testing. */
  private static class MockFunctionInfo implements FunctionInfo {
    private final String name;
    private final long startAddress;
    private final long endAddress;

    MockFunctionInfo(final String name, final long startAddress, final long endAddress) {
      this.name = name;
      this.startAddress = startAddress;
      this.endAddress = endAddress;
    }

    @Override
    public String getName() {
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
  }

  /** Mock implementation of LineNumberInfo for testing. */
  private static class MockLineNumberInfo implements LineNumberInfo {
    private final String fileName;
    private final int lineNumber;
    private final int columnNumber;

    MockLineNumberInfo(final String fileName, final int lineNumber, final int columnNumber) {
      this.fileName = fileName;
      this.lineNumber = lineNumber;
      this.columnNumber = columnNumber;
    }

    @Override
    public String getFileName() {
      return fileName;
    }

    @Override
    public int getLineNumber() {
      return lineNumber;
    }

    @Override
    public int getColumnNumber() {
      return columnNumber;
    }
  }
}
