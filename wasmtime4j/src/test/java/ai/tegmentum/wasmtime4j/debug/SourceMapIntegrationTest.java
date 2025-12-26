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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.SourceMapIntegration.SourceLocation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SourceMapIntegration} interface.
 *
 * <p>SourceMapIntegration provides source map support for debugging WebAssembly modules, enabling
 * mapping between WebAssembly addresses and original source locations.
 */
@DisplayName("SourceMapIntegration Tests")
class SourceMapIntegrationTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          SourceMapIntegration.class.isInterface(), "SourceMapIntegration should be an interface");
    }

    @Test
    @DisplayName("should have loadSourceMap method")
    void shouldHaveLoadSourceMapMethod() throws NoSuchMethodException {
      final Method method = SourceMapIntegration.class.getMethod("loadSourceMap", String.class);
      assertNotNull(method, "loadSourceMap method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
    }

    @Test
    @DisplayName("should have mapToSource method")
    void shouldHaveMapToSourceMethod() throws NoSuchMethodException {
      final Method method = SourceMapIntegration.class.getMethod("mapToSource", long.class);
      assertNotNull(method, "mapToSource method should exist");
      assertEquals(SourceLocation.class, method.getReturnType(), "Should return SourceLocation");
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
    }

    @Test
    @DisplayName("should have mapToAddress method")
    void shouldHaveMapToAddressMethod() throws NoSuchMethodException {
      final Method method =
          SourceMapIntegration.class.getMethod("mapToAddress", SourceLocation.class);
      assertNotNull(method, "mapToAddress method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
    }

    @Test
    @DisplayName("should have getSourceFiles method")
    void shouldHaveGetSourceFilesMethod() throws NoSuchMethodException {
      final Method method = SourceMapIntegration.class.getMethod("getSourceFiles");
      assertNotNull(method, "getSourceFiles method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have isLoaded method")
    void shouldHaveIsLoadedMethod() throws NoSuchMethodException {
      final Method method = SourceMapIntegration.class.getMethod("isLoaded");
      assertNotNull(method, "isLoaded method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getSourceContent method")
    void shouldHaveGetSourceContentMethod() throws NoSuchMethodException {
      final Method method = SourceMapIntegration.class.getMethod("getSourceContent", String.class);
      assertNotNull(method, "getSourceContent method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
    }

    @Test
    @DisplayName("should have exactly six methods")
    void shouldHaveExactlySixMethods() {
      final Method[] methods = SourceMapIntegration.class.getDeclaredMethods();
      assertEquals(6, methods.length, "SourceMapIntegration should have exactly 6 methods");
    }
  }

  @Nested
  @DisplayName("SourceLocation Nested Interface Tests")
  class SourceLocationInterfaceTests {

    @Test
    @DisplayName("should have SourceLocation nested interface")
    void shouldHaveSourceLocationNestedInterface() {
      boolean hasSourceLocation = false;
      for (final Class<?> inner : SourceMapIntegration.class.getDeclaredClasses()) {
        if ("SourceLocation".equals(inner.getSimpleName()) && inner.isInterface()) {
          hasSourceLocation = true;
          break;
        }
      }
      assertTrue(hasSourceLocation, "Should have SourceLocation nested interface");
    }

    @Test
    @DisplayName("SourceLocation should have getFileName method")
    void sourceLocationShouldHaveGetFileNameMethod() throws NoSuchMethodException {
      final Method method = SourceLocation.class.getMethod("getFileName");
      assertNotNull(method, "getFileName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("SourceLocation should have getLineNumber method")
    void sourceLocationShouldHaveGetLineNumberMethod() throws NoSuchMethodException {
      final Method method = SourceLocation.class.getMethod("getLineNumber");
      assertNotNull(method, "getLineNumber method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("SourceLocation should have getColumnNumber method")
    void sourceLocationShouldHaveGetColumnNumberMethod() throws NoSuchMethodException {
      final Method method = SourceLocation.class.getMethod("getColumnNumber");
      assertNotNull(method, "getColumnNumber method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("SourceLocation should have getFunctionName method")
    void sourceLocationShouldHaveGetFunctionNameMethod() throws NoSuchMethodException {
      final Method method = SourceLocation.class.getMethod("getFunctionName");
      assertNotNull(method, "getFunctionName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("SourceLocation should have exactly four methods")
    void sourceLocationShouldHaveExactlyFourMethods() {
      final Method[] methods = SourceLocation.class.getDeclaredMethods();
      assertEquals(4, methods.length, "SourceLocation should have exactly 4 methods");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock should not be loaded initially")
    void mockShouldNotBeLoadedInitially() {
      final MockSourceMapIntegration integration = new MockSourceMapIntegration();

      assertFalse(integration.isLoaded(), "Should not be loaded initially");
    }

    @Test
    @DisplayName("mock should load source map")
    void mockShouldLoadSourceMap() {
      final MockSourceMapIntegration integration = new MockSourceMapIntegration();

      assertTrue(integration.loadSourceMap("test.map"), "Should load successfully");
      assertTrue(integration.isLoaded(), "Should be loaded after loadSourceMap");
    }

    @Test
    @DisplayName("mock should map address to source location")
    void mockShouldMapAddressToSourceLocation() {
      final MockSourceMapIntegration integration = new MockSourceMapIntegration();
      integration.loadSourceMap("test.map");
      integration.addMapping(0x100L, new MockSourceLocation("main.rs", 10, 5, "main"));

      final SourceLocation location = integration.mapToSource(0x100L);

      assertNotNull(location, "Location should not be null");
      assertEquals("main.rs", location.getFileName(), "File name should match");
      assertEquals(10, location.getLineNumber(), "Line number should match");
      assertEquals(5, location.getColumnNumber(), "Column number should match");
      assertEquals("main", location.getFunctionName(), "Function name should match");
    }

    @Test
    @DisplayName("mock should return null for unmapped address")
    void mockShouldReturnNullForUnmappedAddress() {
      final MockSourceMapIntegration integration = new MockSourceMapIntegration();
      integration.loadSourceMap("test.map");

      assertNull(integration.mapToSource(0x999L), "Should return null for unmapped address");
    }

    @Test
    @DisplayName("mock should map source location to address")
    void mockShouldMapSourceLocationToAddress() {
      final MockSourceMapIntegration integration = new MockSourceMapIntegration();
      integration.loadSourceMap("test.map");
      final MockSourceLocation location = new MockSourceLocation("lib.rs", 20, 0, "helper");
      integration.addMapping(0x200L, location);

      assertEquals(0x200L, integration.mapToAddress(location), "Address should match");
    }

    @Test
    @DisplayName("mock should return -1 for unmapped location")
    void mockShouldReturnNegativeOneForUnmappedLocation() {
      final MockSourceMapIntegration integration = new MockSourceMapIntegration();
      integration.loadSourceMap("test.map");
      final MockSourceLocation unknownLocation = new MockSourceLocation("unknown.rs", 1, 0, null);

      assertEquals(
          -1L, integration.mapToAddress(unknownLocation), "Should return -1 for unmapped location");
    }

    @Test
    @DisplayName("mock should return source files")
    void mockShouldReturnSourceFiles() {
      final MockSourceMapIntegration integration = new MockSourceMapIntegration();
      integration.loadSourceMap("test.map");
      integration.addSourceFile("main.rs");
      integration.addSourceFile("lib.rs");

      final List<String> files = integration.getSourceFiles();

      assertEquals(2, files.size(), "Should have 2 source files");
      assertTrue(files.contains("main.rs"), "Should contain main.rs");
      assertTrue(files.contains("lib.rs"), "Should contain lib.rs");
    }

    @Test
    @DisplayName("mock should return source content")
    void mockShouldReturnSourceContent() {
      final MockSourceMapIntegration integration = new MockSourceMapIntegration();
      integration.loadSourceMap("test.map");
      integration.setSourceContent("main.rs", "fn main() { println!(\"Hello\"); }");

      assertEquals(
          "fn main() { println!(\"Hello\"); }",
          integration.getSourceContent("main.rs"),
          "Source content should match");
    }

    @Test
    @DisplayName("mock should return null for unknown source file")
    void mockShouldReturnNullForUnknownSourceFile() {
      final MockSourceMapIntegration integration = new MockSourceMapIntegration();
      integration.loadSourceMap("test.map");

      assertNull(
          integration.getSourceContent("nonexistent.rs"), "Should return null for unknown file");
    }

    @Test
    @DisplayName("source location should handle null function name")
    void sourceLocationShouldHandleNullFunctionName() {
      final MockSourceLocation location = new MockSourceLocation("test.rs", 1, 0, null);

      assertNull(location.getFunctionName(), "Function name can be null");
    }
  }

  /** Mock implementation of SourceMapIntegration for testing. */
  private static class MockSourceMapIntegration implements SourceMapIntegration {
    private boolean loaded;
    private final Map<Long, SourceLocation> addressToLocation = new HashMap<>();
    private final Map<SourceLocation, Long> locationToAddress = new HashMap<>();
    private final List<String> sourceFiles = new ArrayList<>();
    private final Map<String, String> sourceContents = new HashMap<>();

    @Override
    public boolean loadSourceMap(final String filePath) {
      loaded = true;
      return true;
    }

    @Override
    public SourceLocation mapToSource(final long address) {
      return addressToLocation.get(address);
    }

    @Override
    public long mapToAddress(final SourceLocation location) {
      final Long address = locationToAddress.get(location);
      return address != null ? address : -1L;
    }

    @Override
    public List<String> getSourceFiles() {
      return new ArrayList<>(sourceFiles);
    }

    @Override
    public boolean isLoaded() {
      return loaded;
    }

    @Override
    public String getSourceContent(final String fileName) {
      return sourceContents.get(fileName);
    }

    public void addMapping(final long address, final SourceLocation location) {
      addressToLocation.put(address, location);
      locationToAddress.put(location, address);
    }

    public void addSourceFile(final String fileName) {
      sourceFiles.add(fileName);
    }

    public void setSourceContent(final String fileName, final String content) {
      sourceContents.put(fileName, content);
    }
  }

  /** Mock implementation of SourceLocation for testing. */
  private static class MockSourceLocation implements SourceLocation {
    private final String fileName;
    private final int lineNumber;
    private final int columnNumber;
    private final String functionName;

    MockSourceLocation(
        final String fileName,
        final int lineNumber,
        final int columnNumber,
        final String functionName) {
      this.fileName = fileName;
      this.lineNumber = lineNumber;
      this.columnNumber = columnNumber;
      this.functionName = functionName;
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

    @Override
    public String getFunctionName() {
      return functionName;
    }
  }
}
