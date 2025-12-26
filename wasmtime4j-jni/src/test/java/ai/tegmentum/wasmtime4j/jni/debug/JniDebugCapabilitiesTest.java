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

package ai.tegmentum.wasmtime4j.jni.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.DebugCapabilities;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link JniDebugCapabilities}. */
@DisplayName("JniDebugCapabilities Tests")
class JniDebugCapabilitiesTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniDebugCapabilities should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(JniDebugCapabilities.class.getModifiers()),
          "JniDebugCapabilities should be final");
    }

    @Test
    @DisplayName("JniDebugCapabilities should implement DebugCapabilities")
    void shouldImplementDebugCapabilities() {
      assertTrue(
          DebugCapabilities.class.isAssignableFrom(JniDebugCapabilities.class),
          "JniDebugCapabilities should implement DebugCapabilities");
    }
  }

  @Nested
  @DisplayName("getDefault() Tests")
  class GetDefaultTests {

    @Test
    @DisplayName("getDefault should return capabilities with all features enabled")
    void getDefaultShouldReturnCapabilitiesWithAllFeaturesEnabled() {
      final JniDebugCapabilities caps = JniDebugCapabilities.getDefault();

      assertNotNull(caps, "Default capabilities should not be null");
      assertTrue(caps.supportsBreakpoints(), "Breakpoints should be supported");
      assertTrue(caps.supportsStepDebugging(), "Step debugging should be supported");
      assertTrue(caps.supportsVariableInspection(), "Variable inspection should be supported");
      assertTrue(caps.supportsMemoryInspection(), "Memory inspection should be supported");
      assertTrue(caps.supportsProfiling(), "Profiling should be supported");
      assertTrue(caps.supportsSourceMap(), "Source map should be supported");
      assertTrue(caps.supportsDwarf(), "DWARF should be supported");
    }

    @Test
    @DisplayName("getDefault should return max breakpoints of 1024")
    void getDefaultShouldReturnMaxBreakpoints1024() {
      final JniDebugCapabilities caps = JniDebugCapabilities.getDefault();

      assertEquals(1024, caps.getMaxBreakpoints(), "Default max breakpoints should be 1024");
    }

    @Test
    @DisplayName("getDefault should return supported formats")
    void getDefaultShouldReturnSupportedFormats() {
      final JniDebugCapabilities caps = JniDebugCapabilities.getDefault();

      final List<String> formats = caps.getSupportedFormats();
      assertNotNull(formats, "Supported formats should not be null");
      assertEquals(2, formats.size(), "Should have 2 supported formats");
      assertTrue(formats.contains("DWARF"), "Should support DWARF");
      assertTrue(formats.contains("SourceMap"), "Should support SourceMap");
    }

    @Test
    @DisplayName("getDefault should return debugger version 1.0.0")
    void getDefaultShouldReturnDebuggerVersion() {
      final JniDebugCapabilities caps = JniDebugCapabilities.getDefault();

      assertEquals("1.0.0", caps.getDebuggerVersion(), "Debugger version should be 1.0.0");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("Builder should create capabilities with all custom values")
    void builderShouldCreateCapabilitiesWithAllCustomValues() {
      final JniDebugCapabilities caps =
          JniDebugCapabilities.builder()
              .breakpointsSupported(false)
              .stepDebuggingSupported(false)
              .variableInspectionSupported(false)
              .memoryInspectionSupported(false)
              .profilingSupported(false)
              .sourceMapSupported(false)
              .dwarfSupported(false)
              .maxBreakpoints(256)
              .supportedFormats(Collections.singletonList("Custom"))
              .debuggerVersion("2.0.0")
              .build();

      assertFalse(caps.supportsBreakpoints(), "Breakpoints should not be supported");
      assertFalse(caps.supportsStepDebugging(), "Step debugging should not be supported");
      assertFalse(caps.supportsVariableInspection(), "Variable inspection should not be supported");
      assertFalse(caps.supportsMemoryInspection(), "Memory inspection should not be supported");
      assertFalse(caps.supportsProfiling(), "Profiling should not be supported");
      assertFalse(caps.supportsSourceMap(), "Source map should not be supported");
      assertFalse(caps.supportsDwarf(), "DWARF should not be supported");
      assertEquals(256, caps.getMaxBreakpoints(), "Max breakpoints should be 256");
      assertEquals(1, caps.getSupportedFormats().size(), "Should have 1 format");
      assertEquals("Custom", caps.getSupportedFormats().get(0), "Format should be Custom");
      assertEquals("2.0.0", caps.getDebuggerVersion(), "Version should be 2.0.0");
    }

    @Test
    @DisplayName("Builder should use default values when not set")
    void builderShouldUseDefaultValuesWhenNotSet() {
      final JniDebugCapabilities caps = JniDebugCapabilities.builder().build();

      assertTrue(caps.supportsBreakpoints(), "Default breakpoints support should be true");
      assertTrue(caps.supportsStepDebugging(), "Default step debugging should be true");
      assertTrue(caps.supportsVariableInspection(), "Default variable inspection should be true");
      assertTrue(caps.supportsMemoryInspection(), "Default memory inspection should be true");
      assertTrue(caps.supportsProfiling(), "Default profiling should be true");
      assertTrue(caps.supportsSourceMap(), "Default source map should be true");
      assertTrue(caps.supportsDwarf(), "Default DWARF should be true");
      assertEquals(1024, caps.getMaxBreakpoints(), "Default max breakpoints should be 1024");
      assertEquals("1.0.0", caps.getDebuggerVersion(), "Default version should be 1.0.0");
    }

    @Test
    @DisplayName("Builder should allow enabling individual features")
    void builderShouldAllowEnablingIndividualFeatures() {
      final JniDebugCapabilities caps =
          JniDebugCapabilities.builder()
              .breakpointsSupported(true)
              .stepDebuggingSupported(false)
              .variableInspectionSupported(true)
              .memoryInspectionSupported(false)
              .profilingSupported(true)
              .sourceMapSupported(false)
              .dwarfSupported(true)
              .build();

      assertTrue(caps.supportsBreakpoints(), "Breakpoints should be enabled");
      assertFalse(caps.supportsStepDebugging(), "Step debugging should be disabled");
      assertTrue(caps.supportsVariableInspection(), "Variable inspection should be enabled");
      assertFalse(caps.supportsMemoryInspection(), "Memory inspection should be disabled");
      assertTrue(caps.supportsProfiling(), "Profiling should be enabled");
      assertFalse(caps.supportsSourceMap(), "Source map should be disabled");
      assertTrue(caps.supportsDwarf(), "DWARF should be enabled");
    }

    @Test
    @DisplayName("Builder supportedFormats should create defensive copy")
    void builderSupportedFormatsShouldCreateDefensiveCopy() {
      final List<String> formats = new ArrayList<>();
      formats.add("DWARF");

      final JniDebugCapabilities.Builder builder =
          JniDebugCapabilities.builder().supportedFormats(formats);

      // Modify original list
      formats.add("SourceMap");

      final JniDebugCapabilities caps = builder.build();
      assertEquals(1, caps.getSupportedFormats().size(), "Should have 1 format (defensive copy)");
    }

    @Test
    @DisplayName("Builder supportedFormats should handle null")
    void builderSupportedFormatsShouldHandleNull() {
      final JniDebugCapabilities caps =
          JniDebugCapabilities.builder().supportedFormats(null).build();

      assertNotNull(caps.getSupportedFormats(), "Formats should not be null");
      assertTrue(caps.getSupportedFormats().isEmpty(), "Formats should be empty");
    }

    @Test
    @DisplayName("Builder should allow chaining all methods")
    void builderShouldAllowChainingAllMethods() {
      final JniDebugCapabilities caps =
          JniDebugCapabilities.builder()
              .breakpointsSupported(true)
              .stepDebuggingSupported(true)
              .variableInspectionSupported(true)
              .memoryInspectionSupported(true)
              .profilingSupported(true)
              .sourceMapSupported(true)
              .dwarfSupported(true)
              .maxBreakpoints(512)
              .supportedFormats(Arrays.asList("A", "B", "C"))
              .debuggerVersion("3.0.0")
              .build();

      assertNotNull(caps, "Chained builder should produce capabilities");
    }
  }

  @Nested
  @DisplayName("getSupportedFormats Tests")
  class GetSupportedFormatsTests {

    @Test
    @DisplayName("getSupportedFormats should return unmodifiable list")
    void getSupportedFormatsShouldReturnUnmodifiableList() {
      final JniDebugCapabilities caps = JniDebugCapabilities.getDefault();

      assertThrows(
          UnsupportedOperationException.class,
          () -> caps.getSupportedFormats().add("NewFormat"),
          "Should not be able to modify returned list");
    }

    @Test
    @DisplayName("getSupportedFormats should return empty list when none set")
    void getSupportedFormatsShouldReturnEmptyListWhenNoneSet() {
      final JniDebugCapabilities caps =
          JniDebugCapabilities.builder().supportedFormats(Collections.emptyList()).build();

      assertTrue(caps.getSupportedFormats().isEmpty(), "Should return empty list");
    }

    @Test
    @DisplayName("getSupportedFormats should preserve order")
    void getSupportedFormatsShouldPreserveOrder() {
      final JniDebugCapabilities caps =
          JniDebugCapabilities.builder()
              .supportedFormats(Arrays.asList("First", "Second", "Third"))
              .build();

      final List<String> formats = caps.getSupportedFormats();
      assertEquals("First", formats.get(0), "First element should match");
      assertEquals("Second", formats.get(1), "Second element should match");
      assertEquals("Third", formats.get(2), "Third element should match");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include all capability flags")
    void toStringShouldIncludeAllCapabilityFlags() {
      final JniDebugCapabilities caps =
          JniDebugCapabilities.builder()
              .breakpointsSupported(true)
              .stepDebuggingSupported(true)
              .variableInspectionSupported(true)
              .memoryInspectionSupported(true)
              .profilingSupported(true)
              .sourceMapSupported(true)
              .dwarfSupported(true)
              .maxBreakpoints(1024)
              .supportedFormats(Arrays.asList("DWARF", "SourceMap"))
              .debuggerVersion("1.0.0")
              .build();

      final String str = caps.toString();

      assertTrue(str.contains("breakpoints=true"), "Should contain breakpoints");
      assertTrue(str.contains("step=true"), "Should contain step");
      assertTrue(str.contains("variables=true"), "Should contain variables");
      assertTrue(str.contains("memory=true"), "Should contain memory");
      assertTrue(str.contains("profiling=true"), "Should contain profiling");
      assertTrue(str.contains("sourceMap=true"), "Should contain sourceMap");
      assertTrue(str.contains("dwarf=true"), "Should contain dwarf");
      assertTrue(str.contains("maxBreakpoints=1024"), "Should contain maxBreakpoints");
      assertTrue(str.contains("formats="), "Should contain formats");
      assertTrue(str.contains("version='1.0.0'"), "Should contain version");
    }

    @Test
    @DisplayName("toString should show disabled features as false")
    void toStringShouldShowDisabledFeaturesAsFalse() {
      final JniDebugCapabilities caps =
          JniDebugCapabilities.builder()
              .breakpointsSupported(false)
              .stepDebuggingSupported(false)
              .build();

      final String str = caps.toString();

      assertTrue(str.contains("breakpoints=false"), "Should show breakpoints=false");
      assertTrue(str.contains("step=false"), "Should show step=false");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle zero max breakpoints")
    void shouldHandleZeroMaxBreakpoints() {
      final JniDebugCapabilities caps = JniDebugCapabilities.builder().maxBreakpoints(0).build();

      assertEquals(0, caps.getMaxBreakpoints(), "Should handle zero max breakpoints");
    }

    @Test
    @DisplayName("Should handle negative max breakpoints")
    void shouldHandleNegativeMaxBreakpoints() {
      final JniDebugCapabilities caps = JniDebugCapabilities.builder().maxBreakpoints(-1).build();

      assertEquals(-1, caps.getMaxBreakpoints(), "Should handle negative max breakpoints");
    }

    @Test
    @DisplayName("Should handle empty debugger version")
    void shouldHandleEmptyDebuggerVersion() {
      final JniDebugCapabilities caps = JniDebugCapabilities.builder().debuggerVersion("").build();

      assertEquals("", caps.getDebuggerVersion(), "Should handle empty version");
    }

    @Test
    @DisplayName("Should handle null debugger version")
    void shouldHandleNullDebuggerVersion() {
      final JniDebugCapabilities caps =
          JniDebugCapabilities.builder().debuggerVersion(null).build();

      // Note: The builder doesn't prevent null, so we just verify it's stored
      assertEquals(null, caps.getDebuggerVersion(), "Should handle null version");
    }

    @Test
    @DisplayName("Should handle all features disabled")
    void shouldHandleAllFeaturesDisabled() {
      final JniDebugCapabilities caps =
          JniDebugCapabilities.builder()
              .breakpointsSupported(false)
              .stepDebuggingSupported(false)
              .variableInspectionSupported(false)
              .memoryInspectionSupported(false)
              .profilingSupported(false)
              .sourceMapSupported(false)
              .dwarfSupported(false)
              .build();

      assertFalse(caps.supportsBreakpoints(), "Breakpoints should be disabled");
      assertFalse(caps.supportsStepDebugging(), "Step debugging should be disabled");
      assertFalse(caps.supportsVariableInspection(), "Variable inspection should be disabled");
      assertFalse(caps.supportsMemoryInspection(), "Memory inspection should be disabled");
      assertFalse(caps.supportsProfiling(), "Profiling should be disabled");
      assertFalse(caps.supportsSourceMap(), "Source map should be disabled");
      assertFalse(caps.supportsDwarf(), "DWARF should be disabled");
    }
  }
}
