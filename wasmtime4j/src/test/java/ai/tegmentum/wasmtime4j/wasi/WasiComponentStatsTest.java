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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiComponentStats} interface.
 *
 * <p>WasiComponentStats provides comprehensive statistics for WASI components.
 */
@DisplayName("WasiComponentStats Tests")
class WasiComponentStatsTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(WasiComponentStats.class.getModifiers()),
          "WasiComponentStats should be public");
      assertTrue(
          WasiComponentStats.class.isInterface(), "WasiComponentStats should be an interface");
    }
  }

  @Nested
  @DisplayName("Metadata Method Tests")
  class MetadataMethodTests {

    @Test
    @DisplayName("should have getCollectedAt method")
    void shouldHaveGetCollectedAtMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getCollectedAt");
      assertNotNull(method, "getCollectedAt method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("should have getComponentName method")
    void shouldHaveGetComponentNameMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getComponentName");
      assertNotNull(method, "getComponentName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Size Method Tests")
  class SizeMethodTests {

    @Test
    @DisplayName("should have getBytecodeSize method")
    void shouldHaveGetBytecodeSizeMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getBytecodeSize");
      assertNotNull(method, "getBytecodeSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getCompiledSize method")
    void shouldHaveGetCompiledSizeMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getCompiledSize");
      assertNotNull(method, "getCompiledSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMemoryOverhead method")
    void shouldHaveGetMemoryOverheadMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getMemoryOverhead");
      assertNotNull(method, "getMemoryOverhead method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Interface Count Method Tests")
  class InterfaceCountMethodTests {

    @Test
    @DisplayName("should have getExportedInterfaceCount method")
    void shouldHaveGetExportedInterfaceCountMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getExportedInterfaceCount");
      assertNotNull(method, "getExportedInterfaceCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getImportedInterfaceCount method")
    void shouldHaveGetImportedInterfaceCountMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getImportedInterfaceCount");
      assertNotNull(method, "getImportedInterfaceCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getExportedFunctionCount method")
    void shouldHaveGetExportedFunctionCountMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getExportedFunctionCount");
      assertNotNull(method, "getExportedFunctionCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getImportedFunctionCount method")
    void shouldHaveGetImportedFunctionCountMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getImportedFunctionCount");
      assertNotNull(method, "getImportedFunctionCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Type Count Method Tests")
  class TypeCountMethodTests {

    @Test
    @DisplayName("should have getResourceTypeCount method")
    void shouldHaveGetResourceTypeCountMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getResourceTypeCount");
      assertNotNull(method, "getResourceTypeCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getCustomTypeCount method")
    void shouldHaveGetCustomTypeCountMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getCustomTypeCount");
      assertNotNull(method, "getCustomTypeCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Compilation Method Tests")
  class CompilationMethodTests {

    @Test
    @DisplayName("should have getCompilationTimeMs method")
    void shouldHaveGetCompilationTimeMsMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getCompilationTimeMs");
      assertNotNull(method, "getCompilationTimeMs method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Instance Count Method Tests")
  class InstanceCountMethodTests {

    @Test
    @DisplayName("should have getActiveInstanceCount method")
    void shouldHaveGetActiveInstanceCountMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getActiveInstanceCount");
      assertNotNull(method, "getActiveInstanceCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getTotalInstanceCount method")
    void shouldHaveGetTotalInstanceCountMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getTotalInstanceCount");
      assertNotNull(method, "getTotalInstanceCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Function Stats Method Tests")
  class FunctionStatsMethodTests {

    @Test
    @DisplayName("should have getTotalFunctionCalls method")
    void shouldHaveGetTotalFunctionCallsMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getTotalFunctionCalls");
      assertNotNull(method, "getTotalFunctionCalls method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getFunctionCallStats method")
    void shouldHaveGetFunctionCallStatsMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getFunctionCallStats");
      assertNotNull(method, "getFunctionCallStats method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getTotalExecutionTimeMs method")
    void shouldHaveGetTotalExecutionTimeMsMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getTotalExecutionTimeMs");
      assertNotNull(method, "getTotalExecutionTimeMs method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getFunctionExecutionTimeStats method")
    void shouldHaveGetFunctionExecutionTimeStatsMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getFunctionExecutionTimeStats");
      assertNotNull(method, "getFunctionExecutionTimeStats method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }
  }

  @Nested
  @DisplayName("Aggregated Stats Method Tests")
  class AggregatedStatsMethodTests {

    @Test
    @DisplayName("should have getErrorStats method")
    void shouldHaveGetErrorStatsMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getErrorStats");
      assertNotNull(method, "getErrorStats method should exist");
      assertEquals(WasiErrorStats.class, method.getReturnType(), "Should return WasiErrorStats");
    }

    @Test
    @DisplayName("should have getResourceUsageStats method")
    void shouldHaveGetResourceUsageStatsMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getResourceUsageStats");
      assertNotNull(method, "getResourceUsageStats method should exist");
      assertEquals(
          WasiResourceUsageStats.class,
          method.getReturnType(),
          "Should return WasiResourceUsageStats");
    }

    @Test
    @DisplayName("should have getPerformanceMetrics method")
    void shouldHaveGetPerformanceMetricsMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getPerformanceMetrics");
      assertNotNull(method, "getPerformanceMetrics method should exist");
      assertEquals(
          WasiPerformanceMetrics.class,
          method.getReturnType(),
          "Should return WasiPerformanceMetrics");
    }
  }

  @Nested
  @DisplayName("Interface List Method Tests")
  class InterfaceListMethodTests {

    @Test
    @DisplayName("should have getExportedInterfaces method")
    void shouldHaveGetExportedInterfacesMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getExportedInterfaces");
      assertNotNull(method, "getExportedInterfaces method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getImportedInterfaces method")
    void shouldHaveGetImportedInterfacesMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getImportedInterfaces");
      assertNotNull(method, "getImportedInterfaces method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("Utility Method Tests")
  class UtilityMethodTests {

    @Test
    @DisplayName("should have getCustomProperties method")
    void shouldHaveGetCustomPropertiesMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getCustomProperties");
      assertNotNull(method, "getCustomProperties method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getSummary method")
    void shouldHaveGetSummaryMethod() throws NoSuchMethodException {
      final Method method = WasiComponentStats.class.getMethod("getSummary");
      assertNotNull(method, "getSummary method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }
}
