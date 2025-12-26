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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DebugOptions} interface.
 *
 * <p>DebugOptions provides configuration options for WebAssembly debugging including enabling/
 * disabling various debug features, log levels, and output directories.
 */
@DisplayName("DebugOptions Tests")
class DebugOptionsTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(DebugOptions.class.isInterface(), "DebugOptions should be an interface");
    }

    @Test
    @DisplayName("should have isDebugEnabled method")
    void shouldHaveIsDebugEnabledMethod() throws NoSuchMethodException {
      final Method method = DebugOptions.class.getMethod("isDebugEnabled");
      assertNotNull(method, "isDebugEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isProfilingEnabled method")
    void shouldHaveIsProfilingEnabledMethod() throws NoSuchMethodException {
      final Method method = DebugOptions.class.getMethod("isProfilingEnabled");
      assertNotNull(method, "isProfilingEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isTracingEnabled method")
    void shouldHaveIsTracingEnabledMethod() throws NoSuchMethodException {
      final Method method = DebugOptions.class.getMethod("isTracingEnabled");
      assertNotNull(method, "isTracingEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getLogLevel method")
    void shouldHaveGetLogLevelMethod() throws NoSuchMethodException {
      final Method method = DebugOptions.class.getMethod("getLogLevel");
      assertNotNull(method, "getLogLevel method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getOutputDirectory method")
    void shouldHaveGetOutputDirectoryMethod() throws NoSuchMethodException {
      final Method method = DebugOptions.class.getMethod("getOutputDirectory");
      assertNotNull(method, "getOutputDirectory method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have isSourceMapEnabled method")
    void shouldHaveIsSourceMapEnabledMethod() throws NoSuchMethodException {
      final Method method = DebugOptions.class.getMethod("isSourceMapEnabled");
      assertNotNull(method, "isSourceMapEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isDwarfEnabled method")
    void shouldHaveIsDwarfEnabledMethod() throws NoSuchMethodException {
      final Method method = DebugOptions.class.getMethod("isDwarfEnabled");
      assertNotNull(method, "isDwarfEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getMaxTraceBufferSize method")
    void shouldHaveGetMaxTraceBufferSizeMethod() throws NoSuchMethodException {
      final Method method = DebugOptions.class.getMethod("getMaxTraceBufferSize");
      assertNotNull(method, "getMaxTraceBufferSize method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getDebugTimeout method")
    void shouldHaveGetDebugTimeoutMethod() throws NoSuchMethodException {
      final Method method = DebugOptions.class.getMethod("getDebugTimeout");
      assertNotNull(method, "getDebugTimeout method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have isMemoryDumpEnabled method")
    void shouldHaveIsMemoryDumpEnabledMethod() throws NoSuchMethodException {
      final Method method = DebugOptions.class.getMethod("isMemoryDumpEnabled");
      assertNotNull(method, "isMemoryDumpEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have exactly ten methods")
    void shouldHaveExactlyTenMethods() {
      final Method[] methods = DebugOptions.class.getDeclaredMethods();
      assertEquals(10, methods.length, "DebugOptions should have exactly 10 methods");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock should return debug enabled status")
    void mockShouldReturnDebugEnabledStatus() {
      final MockDebugOptions enabledOptions =
          new MockDebugOptions.Builder().debugEnabled(true).build();
      final MockDebugOptions disabledOptions =
          new MockDebugOptions.Builder().debugEnabled(false).build();

      assertTrue(enabledOptions.isDebugEnabled(), "Debug should be enabled");
      assertFalse(disabledOptions.isDebugEnabled(), "Debug should be disabled");
    }

    @Test
    @DisplayName("mock should return profiling enabled status")
    void mockShouldReturnProfilingEnabledStatus() {
      final MockDebugOptions options =
          new MockDebugOptions.Builder().profilingEnabled(true).build();

      assertTrue(options.isProfilingEnabled(), "Profiling should be enabled");
    }

    @Test
    @DisplayName("mock should return tracing enabled status")
    void mockShouldReturnTracingEnabledStatus() {
      final MockDebugOptions options = new MockDebugOptions.Builder().tracingEnabled(true).build();

      assertTrue(options.isTracingEnabled(), "Tracing should be enabled");
    }

    @Test
    @DisplayName("mock should return log level")
    void mockShouldReturnLogLevel() {
      final MockDebugOptions options = new MockDebugOptions.Builder().logLevel("DEBUG").build();

      assertEquals("DEBUG", options.getLogLevel(), "Log level should be DEBUG");
    }

    @Test
    @DisplayName("mock should return output directory")
    void mockShouldReturnOutputDirectory() {
      final MockDebugOptions options =
          new MockDebugOptions.Builder().outputDirectory("/tmp/debug").build();

      assertEquals("/tmp/debug", options.getOutputDirectory(), "Output directory should match");
    }

    @Test
    @DisplayName("mock should return source map enabled status")
    void mockShouldReturnSourceMapEnabledStatus() {
      final MockDebugOptions options =
          new MockDebugOptions.Builder().sourceMapEnabled(true).build();

      assertTrue(options.isSourceMapEnabled(), "Source map should be enabled");
    }

    @Test
    @DisplayName("mock should return DWARF enabled status")
    void mockShouldReturnDwarfEnabledStatus() {
      final MockDebugOptions options = new MockDebugOptions.Builder().dwarfEnabled(true).build();

      assertTrue(options.isDwarfEnabled(), "DWARF should be enabled");
    }

    @Test
    @DisplayName("mock should return max trace buffer size")
    void mockShouldReturnMaxTraceBufferSize() {
      final MockDebugOptions options =
          new MockDebugOptions.Builder().maxTraceBufferSize(8192).build();

      assertEquals(8192, options.getMaxTraceBufferSize(), "Max trace buffer size should match");
    }

    @Test
    @DisplayName("mock should return debug timeout")
    void mockShouldReturnDebugTimeout() {
      final MockDebugOptions options = new MockDebugOptions.Builder().debugTimeout(30000L).build();

      assertEquals(30000L, options.getDebugTimeout(), "Debug timeout should be 30 seconds");
    }

    @Test
    @DisplayName("mock should return memory dump enabled status")
    void mockShouldReturnMemoryDumpEnabledStatus() {
      final MockDebugOptions options =
          new MockDebugOptions.Builder().memoryDumpEnabled(true).build();

      assertTrue(options.isMemoryDumpEnabled(), "Memory dump should be enabled");
    }

    @Test
    @DisplayName("mock builder should use sensible defaults")
    void mockBuilderShouldUseSensibleDefaults() {
      final MockDebugOptions options = new MockDebugOptions.Builder().build();

      assertFalse(options.isDebugEnabled(), "Debug should be disabled by default");
      assertFalse(options.isProfilingEnabled(), "Profiling should be disabled by default");
      assertFalse(options.isTracingEnabled(), "Tracing should be disabled by default");
      assertEquals("INFO", options.getLogLevel(), "Default log level should be INFO");
      assertEquals("", options.getOutputDirectory(), "Default output directory should be empty");
      assertFalse(options.isSourceMapEnabled(), "Source map should be disabled by default");
      assertFalse(options.isDwarfEnabled(), "DWARF should be disabled by default");
      assertEquals(4096, options.getMaxTraceBufferSize(), "Default buffer size should be 4096");
      assertEquals(10000L, options.getDebugTimeout(), "Default timeout should be 10 seconds");
      assertFalse(options.isMemoryDumpEnabled(), "Memory dump should be disabled by default");
    }
  }

  /** Mock implementation of DebugOptions for testing. */
  private static class MockDebugOptions implements DebugOptions {
    private final boolean debugEnabled;
    private final boolean profilingEnabled;
    private final boolean tracingEnabled;
    private final String logLevel;
    private final String outputDirectory;
    private final boolean sourceMapEnabled;
    private final boolean dwarfEnabled;
    private final int maxTraceBufferSize;
    private final long debugTimeout;
    private final boolean memoryDumpEnabled;

    private MockDebugOptions(final Builder builder) {
      this.debugEnabled = builder.debugEnabled;
      this.profilingEnabled = builder.profilingEnabled;
      this.tracingEnabled = builder.tracingEnabled;
      this.logLevel = builder.logLevel;
      this.outputDirectory = builder.outputDirectory;
      this.sourceMapEnabled = builder.sourceMapEnabled;
      this.dwarfEnabled = builder.dwarfEnabled;
      this.maxTraceBufferSize = builder.maxTraceBufferSize;
      this.debugTimeout = builder.debugTimeout;
      this.memoryDumpEnabled = builder.memoryDumpEnabled;
    }

    @Override
    public boolean isDebugEnabled() {
      return debugEnabled;
    }

    @Override
    public boolean isProfilingEnabled() {
      return profilingEnabled;
    }

    @Override
    public boolean isTracingEnabled() {
      return tracingEnabled;
    }

    @Override
    public String getLogLevel() {
      return logLevel;
    }

    @Override
    public String getOutputDirectory() {
      return outputDirectory;
    }

    @Override
    public boolean isSourceMapEnabled() {
      return sourceMapEnabled;
    }

    @Override
    public boolean isDwarfEnabled() {
      return dwarfEnabled;
    }

    @Override
    public int getMaxTraceBufferSize() {
      return maxTraceBufferSize;
    }

    @Override
    public long getDebugTimeout() {
      return debugTimeout;
    }

    @Override
    public boolean isMemoryDumpEnabled() {
      return memoryDumpEnabled;
    }

    /** Builder for MockDebugOptions. */
    static class Builder {
      private boolean debugEnabled = false;
      private boolean profilingEnabled = false;
      private boolean tracingEnabled = false;
      private String logLevel = "INFO";
      private String outputDirectory = "";
      private boolean sourceMapEnabled = false;
      private boolean dwarfEnabled = false;
      private int maxTraceBufferSize = 4096;
      private long debugTimeout = 10000L;
      private boolean memoryDumpEnabled = false;

      Builder debugEnabled(final boolean enabled) {
        this.debugEnabled = enabled;
        return this;
      }

      Builder profilingEnabled(final boolean enabled) {
        this.profilingEnabled = enabled;
        return this;
      }

      Builder tracingEnabled(final boolean enabled) {
        this.tracingEnabled = enabled;
        return this;
      }

      Builder logLevel(final String level) {
        this.logLevel = level;
        return this;
      }

      Builder outputDirectory(final String directory) {
        this.outputDirectory = directory;
        return this;
      }

      Builder sourceMapEnabled(final boolean enabled) {
        this.sourceMapEnabled = enabled;
        return this;
      }

      Builder dwarfEnabled(final boolean enabled) {
        this.dwarfEnabled = enabled;
        return this;
      }

      Builder maxTraceBufferSize(final int size) {
        this.maxTraceBufferSize = size;
        return this;
      }

      Builder debugTimeout(final long timeout) {
        this.debugTimeout = timeout;
        return this;
      }

      Builder memoryDumpEnabled(final boolean enabled) {
        this.memoryDumpEnabled = enabled;
        return this;
      }

      MockDebugOptions build() {
        return new MockDebugOptions(this);
      }
    }
  }
}
