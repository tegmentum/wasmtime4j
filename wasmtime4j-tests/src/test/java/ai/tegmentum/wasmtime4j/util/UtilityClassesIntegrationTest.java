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

package ai.tegmentum.wasmtime4j.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.AdvancedWasmFeature;
import ai.tegmentum.wasmtime4j.CallbackRegistry;
import ai.tegmentum.wasmtime4j.CustomSection;
import ai.tegmentum.wasmtime4j.CustomSectionType;
import ai.tegmentum.wasmtime4j.RuntimeInfo;
import ai.tegmentum.wasmtime4j.RuntimeType;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for utility classes in the root package.
 *
 * <p>Tests cover: AdvancedWasmFeature, CustomSection, CustomSectionType, RuntimeInfo, RuntimeType,
 * CallbackRegistry interfaces.
 */
@DisplayName("Utility Classes Integration Tests")
public class UtilityClassesIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(UtilityClassesIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting Utility Classes Integration Tests");
  }

  @Nested
  @DisplayName("AdvancedWasmFeature Enum Tests")
  class AdvancedWasmFeatureTests {

    @Test
    @DisplayName("should have all expected feature values")
    void shouldHaveAllExpectedFeatureValues(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      AdvancedWasmFeature[] features = AdvancedWasmFeature.values();
      assertEquals(15, features.length, "Should have 15 advanced features");

      assertNotNull(AdvancedWasmFeature.valueOf("EXCEPTIONS"));
      assertNotNull(AdvancedWasmFeature.valueOf("SIMD_ARITHMETIC"));
      assertNotNull(AdvancedWasmFeature.valueOf("SIMD_MEMORY"));
      assertNotNull(AdvancedWasmFeature.valueOf("SIMD_MANIPULATION"));
      assertNotNull(AdvancedWasmFeature.valueOf("ATOMIC_OPERATIONS"));
      assertNotNull(AdvancedWasmFeature.valueOf("ATOMIC_CAS"));
      assertNotNull(AdvancedWasmFeature.valueOf("SHARED_MEMORY"));
      assertNotNull(AdvancedWasmFeature.valueOf("MEMORY_ORDERING"));
      assertNotNull(AdvancedWasmFeature.valueOf("CROSS_MODULE_EXCEPTIONS"));
      assertNotNull(AdvancedWasmFeature.valueOf("NESTED_EXCEPTIONS"));
      assertNotNull(AdvancedWasmFeature.valueOf("EXCEPTION_TYPES"));
      assertNotNull(AdvancedWasmFeature.valueOf("THREAD_SAFETY"));
      assertNotNull(AdvancedWasmFeature.valueOf("SIMD_PERFORMANCE"));
      assertNotNull(AdvancedWasmFeature.valueOf("ATOMIC_PERFORMANCE"));
      assertNotNull(AdvancedWasmFeature.valueOf("EXCEPTION_PERFORMANCE"));

      LOGGER.info("Found " + features.length + " advanced WASM features");
    }

    @Test
    @DisplayName("should have correct ordinal values")
    void shouldHaveCorrectOrdinalValues(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertEquals(0, AdvancedWasmFeature.EXCEPTIONS.ordinal());
      assertEquals(1, AdvancedWasmFeature.SIMD_ARITHMETIC.ordinal());
      assertEquals(2, AdvancedWasmFeature.SIMD_MEMORY.ordinal());

      LOGGER.info("Ordinal values verified");
    }

    @Test
    @DisplayName("should categorize SIMD features correctly")
    void shouldCategorizeSimdFeaturesCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertTrue(AdvancedWasmFeature.SIMD_ARITHMETIC.name().contains("SIMD"));
      assertTrue(AdvancedWasmFeature.SIMD_MEMORY.name().contains("SIMD"));
      assertTrue(AdvancedWasmFeature.SIMD_MANIPULATION.name().contains("SIMD"));
      assertTrue(AdvancedWasmFeature.SIMD_PERFORMANCE.name().contains("SIMD"));

      LOGGER.info("SIMD feature categorization verified");
    }

    @Test
    @DisplayName("should categorize atomic features correctly")
    void shouldCategorizeAtomicFeaturesCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      String atomicOps = AdvancedWasmFeature.ATOMIC_OPERATIONS.name();
      String atomicCas = AdvancedWasmFeature.ATOMIC_CAS.name();
      String atomicPerf = AdvancedWasmFeature.ATOMIC_PERFORMANCE.name();

      assertTrue(atomicOps.contains("ATOMIC"));
      assertTrue(atomicCas.contains("ATOMIC"));
      assertTrue(atomicPerf.contains("ATOMIC"));

      LOGGER.info("Atomic feature categorization verified");
    }

    @Test
    @DisplayName("should categorize exception features correctly")
    void shouldCategorizeExceptionFeaturesCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertTrue(AdvancedWasmFeature.EXCEPTIONS.name().contains("EXCEPTION"));
      assertTrue(AdvancedWasmFeature.CROSS_MODULE_EXCEPTIONS.name().contains("EXCEPTION"));
      assertTrue(AdvancedWasmFeature.NESTED_EXCEPTIONS.name().contains("EXCEPTION"));
      assertTrue(AdvancedWasmFeature.EXCEPTION_TYPES.name().contains("EXCEPTION"));
      assertTrue(AdvancedWasmFeature.EXCEPTION_PERFORMANCE.name().contains("EXCEPTION"));

      LOGGER.info("Exception feature categorization verified");
    }
  }

  @Nested
  @DisplayName("CustomSectionType Enum Tests")
  class CustomSectionTypeTests {

    @Test
    @DisplayName("should have all expected section types")
    void shouldHaveAllExpectedSectionTypes(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      CustomSectionType[] types = CustomSectionType.values();
      assertEquals(8, types.length, "Should have 8 custom section types");

      assertNotNull(CustomSectionType.valueOf("NAME"));
      assertNotNull(CustomSectionType.valueOf("PRODUCERS"));
      assertNotNull(CustomSectionType.valueOf("TARGET_FEATURES"));
      assertNotNull(CustomSectionType.valueOf("DWARF"));
      assertNotNull(CustomSectionType.valueOf("SOURCE_MAP"));
      assertNotNull(CustomSectionType.valueOf("LINKING"));
      assertNotNull(CustomSectionType.valueOf("RELOC"));
      assertNotNull(CustomSectionType.valueOf("UNKNOWN"));

      LOGGER.info("Found " + types.length + " custom section types");
    }

    @Test
    @DisplayName("should return correct primary names")
    void shouldReturnCorrectPrimaryNames(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertEquals("name", CustomSectionType.NAME.getPrimaryName());
      assertEquals("producers", CustomSectionType.PRODUCERS.getPrimaryName());
      assertEquals("target_features", CustomSectionType.TARGET_FEATURES.getPrimaryName());
      assertEquals(".debug_info", CustomSectionType.DWARF.getPrimaryName());
      assertEquals("sourceMappingURL", CustomSectionType.SOURCE_MAP.getPrimaryName());
      assertEquals("linking", CustomSectionType.LINKING.getPrimaryName());
      assertEquals("reloc.CODE", CustomSectionType.RELOC.getPrimaryName());
      assertNull(CustomSectionType.UNKNOWN.getPrimaryName());

      LOGGER.info("Primary names verified");
    }

    @Test
    @DisplayName("should match section names correctly")
    void shouldMatchSectionNamesCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertTrue(CustomSectionType.NAME.matches("name"));
      assertTrue(CustomSectionType.PRODUCERS.matches("producers"));
      assertTrue(CustomSectionType.DWARF.matches(".debug_info"));
      assertTrue(CustomSectionType.DWARF.matches(".debug_line"));
      assertTrue(CustomSectionType.RELOC.matches("reloc.CODE"));
      assertTrue(CustomSectionType.RELOC.matches("reloc.DATA"));

      assertFalse(CustomSectionType.NAME.matches("producers"));
      assertFalse(CustomSectionType.UNKNOWN.matches("anything"));

      LOGGER.info("Section name matching verified");
    }

    @Test
    @DisplayName("should throw on null section name in matches")
    void shouldThrowOnNullSectionNameInMatches(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class,
          () -> CustomSectionType.NAME.matches(null),
          "matches should throw on null");

      LOGGER.info("Null handling verified");
    }

    @Test
    @DisplayName("should resolve type from name correctly")
    void shouldResolveTypeFromNameCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertEquals(CustomSectionType.NAME, CustomSectionType.fromName("name"));
      assertEquals(CustomSectionType.PRODUCERS, CustomSectionType.fromName("producers"));
      assertEquals(CustomSectionType.DWARF, CustomSectionType.fromName(".debug_info"));
      assertEquals(CustomSectionType.DWARF, CustomSectionType.fromName(".debug_str"));
      assertEquals(CustomSectionType.UNKNOWN, CustomSectionType.fromName("unknown_section"));

      LOGGER.info("Type resolution verified");
    }

    @Test
    @DisplayName("should throw on null section name in fromName")
    void shouldThrowOnNullSectionNameInFromName(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class,
          () -> CustomSectionType.fromName(null),
          "fromName should throw on null");

      LOGGER.info("Null handling in fromName verified");
    }

    @Test
    @DisplayName("should identify debugging sections correctly")
    void shouldIdentifyDebuggingSectionsCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertTrue(CustomSectionType.DWARF.isDebuggingSection());
      assertTrue(CustomSectionType.SOURCE_MAP.isDebuggingSection());
      assertTrue(CustomSectionType.NAME.isDebuggingSection());

      assertFalse(CustomSectionType.PRODUCERS.isDebuggingSection());
      assertFalse(CustomSectionType.LINKING.isDebuggingSection());
      assertFalse(CustomSectionType.UNKNOWN.isDebuggingSection());

      LOGGER.info("Debugging section identification verified");
    }

    @Test
    @DisplayName("should identify toolchain sections correctly")
    void shouldIdentifyToolchainSectionsCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertTrue(CustomSectionType.PRODUCERS.isToolchainSection());
      assertTrue(CustomSectionType.TARGET_FEATURES.isToolchainSection());

      assertFalse(CustomSectionType.NAME.isToolchainSection());
      assertFalse(CustomSectionType.DWARF.isToolchainSection());
      assertFalse(CustomSectionType.UNKNOWN.isToolchainSection());

      LOGGER.info("Toolchain section identification verified");
    }

    @Test
    @DisplayName("should identify linking sections correctly")
    void shouldIdentifyLinkingSectionsCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertTrue(CustomSectionType.LINKING.isLinkingSection());
      assertTrue(CustomSectionType.RELOC.isLinkingSection());

      assertFalse(CustomSectionType.NAME.isLinkingSection());
      assertFalse(CustomSectionType.PRODUCERS.isLinkingSection());
      assertFalse(CustomSectionType.UNKNOWN.isLinkingSection());

      LOGGER.info("Linking section identification verified");
    }

    @Test
    @DisplayName("should return names array correctly")
    void shouldReturnNamesArrayCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      String[] dwarfNames = CustomSectionType.DWARF.getNames();
      assertTrue(dwarfNames.length > 1, "DWARF should have multiple names");

      String[] relocNames = CustomSectionType.RELOC.getNames();
      assertEquals(2, relocNames.length, "RELOC should have 2 names");

      String[] unknownNames = CustomSectionType.UNKNOWN.getNames();
      assertEquals(0, unknownNames.length, "UNKNOWN should have no names");

      LOGGER.info("Names array verified");
    }
  }

  @Nested
  @DisplayName("CustomSection Class Tests")
  class CustomSectionTests {

    @Test
    @DisplayName("should create custom section with valid parameters")
    void shouldCreateCustomSectionWithValidParameters(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      byte[] data = {1, 2, 3, 4, 5};
      CustomSection section = new CustomSection("test", data, CustomSectionType.UNKNOWN);

      assertEquals("test", section.getName());
      assertArrayEquals(data, section.getData());
      assertEquals(CustomSectionType.UNKNOWN, section.getType());
      assertEquals(5, section.getSize());
      assertFalse(section.isEmpty());

      LOGGER.info("Custom section creation verified");
    }

    @Test
    @DisplayName("should create empty custom section")
    void shouldCreateEmptyCustomSection(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      byte[] emptyData = new byte[0];
      CustomSection section = new CustomSection("empty", emptyData, CustomSectionType.UNKNOWN);

      assertEquals(0, section.getSize());
      assertTrue(section.isEmpty());

      LOGGER.info("Empty custom section creation verified");
    }

    @Test
    @DisplayName("should throw on null name")
    void shouldThrowOnNullName(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      byte[] data = {1, 2, 3};
      assertThrows(
          IllegalArgumentException.class,
          () -> new CustomSection(null, data, CustomSectionType.UNKNOWN),
          "Should throw on null name");

      LOGGER.info("Null name handling verified");
    }

    @Test
    @DisplayName("should throw on empty name")
    void shouldThrowOnEmptyName(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      byte[] data = {1, 2, 3};
      assertThrows(
          IllegalArgumentException.class,
          () -> new CustomSection("", data, CustomSectionType.UNKNOWN),
          "Should throw on empty name");

      assertThrows(
          IllegalArgumentException.class,
          () -> new CustomSection("   ", data, CustomSectionType.UNKNOWN),
          "Should throw on whitespace-only name");

      LOGGER.info("Empty name handling verified");
    }

    @Test
    @DisplayName("should throw on null data")
    void shouldThrowOnNullData(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class,
          () -> new CustomSection("test", null, CustomSectionType.UNKNOWN),
          "Should throw on null data");

      LOGGER.info("Null data handling verified");
    }

    @Test
    @DisplayName("should throw on null type")
    void shouldThrowOnNullType(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      byte[] data = {1, 2, 3};
      assertThrows(
          IllegalArgumentException.class,
          () -> new CustomSection("test", data, null),
          "Should throw on null type");

      LOGGER.info("Null type handling verified");
    }

    @Test
    @DisplayName("should create unknown section via factory method")
    void shouldCreateUnknownSectionViaFactoryMethod(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      byte[] data = {1, 2, 3};
      CustomSection section = CustomSection.createUnknown("custom", data);

      assertEquals("custom", section.getName());
      assertEquals(CustomSectionType.UNKNOWN, section.getType());

      LOGGER.info("Factory method verified");
    }

    @Test
    @DisplayName("should implement equals correctly")
    void shouldImplementEqualsCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      byte[] data = {1, 2, 3};
      CustomSection s1 = new CustomSection("test", data, CustomSectionType.NAME);
      CustomSection s2 = new CustomSection("test", data, CustomSectionType.NAME);
      CustomSection s3 = new CustomSection("other", data, CustomSectionType.NAME);

      assertEquals(s1, s2);
      assertNotEquals(s1, s3);
      assertNotEquals(s1, null);
      assertNotEquals(s1, "string");

      LOGGER.info("Equals implementation verified");
    }

    @Test
    @DisplayName("should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      byte[] data = {1, 2, 3};
      CustomSection s1 = new CustomSection("test", data, CustomSectionType.NAME);
      CustomSection s2 = new CustomSection("test", data, CustomSectionType.NAME);

      assertEquals(s1.hashCode(), s2.hashCode());

      LOGGER.info("HashCode implementation verified");
    }

    @Test
    @DisplayName("should format toString correctly")
    void shouldFormatToStringCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      byte[] data = {1, 2, 3, 4, 5};
      CustomSection section = new CustomSection("test", data, CustomSectionType.NAME);
      String str = section.toString();

      assertTrue(str.contains("test"));
      assertTrue(str.contains("NAME"));
      assertTrue(str.contains("5"));

      LOGGER.info("ToString format verified");
    }

    @Test
    @DisplayName("should return defensive copy of data")
    void shouldReturnDefensiveCopyOfData(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      byte[] originalData = {1, 2, 3};
      CustomSection section = new CustomSection("test", originalData, CustomSectionType.UNKNOWN);

      byte[] retrievedData = section.getData();
      retrievedData[0] = 99;

      byte[] secondRetrieval = section.getData();
      assertEquals(1, secondRetrieval[0], "Original data should not be modified");

      LOGGER.info("Defensive copy verified");
    }
  }

  @Nested
  @DisplayName("RuntimeType Enum Tests")
  class RuntimeTypeTests {

    @Test
    @DisplayName("should have all expected runtime types")
    void shouldHaveAllExpectedRuntimeTypes(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      RuntimeType[] types = RuntimeType.values();
      assertEquals(2, types.length, "Should have 2 runtime types");

      assertNotNull(RuntimeType.valueOf("JNI"));
      assertNotNull(RuntimeType.valueOf("PANAMA"));

      LOGGER.info("Found " + types.length + " runtime types");
    }

    @Test
    @DisplayName("should have correct ordinal values")
    void shouldHaveCorrectOrdinalValues(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertEquals(0, RuntimeType.JNI.ordinal());
      assertEquals(1, RuntimeType.PANAMA.ordinal());

      LOGGER.info("Ordinal values verified");
    }
  }

  @Nested
  @DisplayName("RuntimeInfo Class Tests")
  class RuntimeInfoTests {

    @Test
    @DisplayName("should create RuntimeInfo with all parameters")
    void shouldCreateRuntimeInfoWithAllParameters(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      RuntimeInfo info =
          new RuntimeInfo(
              "wasmtime4j", "1.0.0", "22.0.0", RuntimeType.PANAMA, "23.0.1", "macOS-aarch64");

      assertEquals("wasmtime4j", info.getRuntimeName());
      assertEquals("1.0.0", info.getRuntimeVersion());
      assertEquals("22.0.0", info.getWasmtimeVersion());
      assertEquals(RuntimeType.PANAMA, info.getRuntimeType());
      assertEquals("23.0.1", info.getJavaVersion());
      assertEquals("macOS-aarch64", info.getPlatformInfo());

      LOGGER.info("RuntimeInfo creation verified");
    }

    @Test
    @DisplayName("should format toString correctly")
    void shouldFormatToStringCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      RuntimeInfo info =
          new RuntimeInfo("wasmtime4j", "1.0.0", "22.0.0", RuntimeType.JNI, "21.0.0", "linux-x64");
      String str = info.toString();

      assertTrue(str.contains("wasmtime4j"));
      assertTrue(str.contains("1.0.0"));
      assertTrue(str.contains("22.0.0"));
      assertTrue(str.contains("JNI"));
      assertTrue(str.contains("21.0.0"));
      assertTrue(str.contains("linux-x64"));

      LOGGER.info("ToString format verified");
    }
  }

  @Nested
  @DisplayName("CallbackRegistry Interface Tests")
  class CallbackRegistryInterfaceTests {

    @Test
    @DisplayName("should verify CallbackRegistry interface exists")
    void shouldVerifyCallbackRegistryInterfaceExists(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertTrue(
          CallbackRegistry.class.isInterface(), "CallbackRegistry should be an interface");

      LOGGER.info("CallbackRegistry interface verified");
    }

    @Test
    @DisplayName("should have required methods")
    void shouldHaveRequiredMethods(final TestInfo testInfo) throws Exception {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      Method registerCallback =
          CallbackRegistry.class.getMethod(
              "registerCallback",
              String.class,
              ai.tegmentum.wasmtime4j.HostFunction.class,
              ai.tegmentum.wasmtime4j.FunctionType.class);
      assertNotNull(registerCallback, "registerCallback method should exist");

      Method unregisterCallback =
          CallbackRegistry.class.getMethod(
              "unregisterCallback", CallbackRegistry.CallbackHandle.class);
      assertNotNull(unregisterCallback, "unregisterCallback method should exist");

      Method getMetrics = CallbackRegistry.class.getMethod("getMetrics");
      assertNotNull(getMetrics, "getMetrics method should exist");

      Method getCallbackCount = CallbackRegistry.class.getMethod("getCallbackCount");
      assertNotNull(getCallbackCount, "getCallbackCount method should exist");
      assertEquals(int.class, getCallbackCount.getReturnType());

      Method hasCallback = CallbackRegistry.class.getMethod("hasCallback", String.class);
      assertNotNull(hasCallback, "hasCallback method should exist");
      assertEquals(boolean.class, hasCallback.getReturnType());

      Method close = CallbackRegistry.class.getMethod("close");
      assertNotNull(close, "close method should exist");

      LOGGER.info("CallbackRegistry methods verified");
    }

    @Test
    @DisplayName("should have CallbackHandle nested interface")
    void shouldHaveCallbackHandleNestedInterface(final TestInfo testInfo) throws Exception {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertTrue(
          CallbackRegistry.CallbackHandle.class.isInterface(),
          "CallbackHandle should be an interface");

      Method getId = CallbackRegistry.CallbackHandle.class.getMethod("getId");
      assertNotNull(getId, "getId method should exist");
      assertEquals(long.class, getId.getReturnType());

      Method getName = CallbackRegistry.CallbackHandle.class.getMethod("getName");
      assertNotNull(getName, "getName method should exist");

      Method isValid = CallbackRegistry.CallbackHandle.class.getMethod("isValid");
      assertNotNull(isValid, "isValid method should exist");
      assertEquals(boolean.class, isValid.getReturnType());

      LOGGER.info("CallbackHandle interface verified");
    }

    @Test
    @DisplayName("should have AsyncCallbackHandle nested interface")
    void shouldHaveAsyncCallbackHandleNestedInterface(final TestInfo testInfo) throws Exception {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertTrue(
          CallbackRegistry.AsyncCallbackHandle.class.isInterface(),
          "AsyncCallbackHandle should be an interface");
      assertTrue(
          CallbackRegistry.CallbackHandle.class.isAssignableFrom(
              CallbackRegistry.AsyncCallbackHandle.class),
          "AsyncCallbackHandle should extend CallbackHandle");

      Method getTimeoutMillis =
          CallbackRegistry.AsyncCallbackHandle.class.getMethod("getTimeoutMillis");
      assertNotNull(getTimeoutMillis, "getTimeoutMillis method should exist");
      assertEquals(long.class, getTimeoutMillis.getReturnType());

      Method setTimeoutMillis =
          CallbackRegistry.AsyncCallbackHandle.class.getMethod("setTimeoutMillis", long.class);
      assertNotNull(setTimeoutMillis, "setTimeoutMillis method should exist");

      LOGGER.info("AsyncCallbackHandle interface verified");
    }

    @Test
    @DisplayName("should have AsyncHostFunction nested interface")
    void shouldHaveAsyncHostFunctionNestedInterface(final TestInfo testInfo) throws Exception {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertTrue(
          CallbackRegistry.AsyncHostFunction.class.isInterface(),
          "AsyncHostFunction should be an interface");

      Method executeAsync =
          CallbackRegistry.AsyncHostFunction.class.getMethod(
              "executeAsync", ai.tegmentum.wasmtime4j.WasmValue[].class);
      assertNotNull(executeAsync, "executeAsync method should exist");

      LOGGER.info("AsyncHostFunction interface verified");
    }

    @Test
    @DisplayName("should have CallbackMetrics nested interface")
    void shouldHaveCallbackMetricsNestedInterface(final TestInfo testInfo) throws Exception {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertTrue(
          CallbackRegistry.CallbackMetrics.class.isInterface(),
          "CallbackMetrics should be an interface");

      Method getTotalInvocations =
          CallbackRegistry.CallbackMetrics.class.getMethod("getTotalInvocations");
      assertNotNull(getTotalInvocations, "getTotalInvocations method should exist");
      assertEquals(long.class, getTotalInvocations.getReturnType());

      Method getAverageExecutionTimeNanos =
          CallbackRegistry.CallbackMetrics.class.getMethod("getAverageExecutionTimeNanos");
      assertNotNull(
          getAverageExecutionTimeNanos, "getAverageExecutionTimeNanos method should exist");
      assertEquals(double.class, getAverageExecutionTimeNanos.getReturnType());

      Method getFailureCount =
          CallbackRegistry.CallbackMetrics.class.getMethod("getFailureCount");
      assertNotNull(getFailureCount, "getFailureCount method should exist");

      Method getTimeoutCount =
          CallbackRegistry.CallbackMetrics.class.getMethod("getTimeoutCount");
      assertNotNull(getTimeoutCount, "getTimeoutCount method should exist");

      LOGGER.info("CallbackMetrics interface verified");
    }
  }
}
