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

package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GcHeapInspection} interface.
 *
 * <p>GcHeapInspection provides detailed information about the current state of the WebAssembly GC
 * heap, including object counts, type distributions, and memory usage.
 */
@DisplayName("GcHeapInspection Tests")
class GcHeapInspectionTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(GcHeapInspection.class.isInterface(), "GcHeapInspection should be an interface");
    }
  }

  @Nested
  @DisplayName("Memory Statistics Method Tests")
  class MemoryStatisticsMethodTests {

    @Test
    @DisplayName("should have getTotalObjectCount method")
    void shouldHaveGetTotalObjectCountMethod() throws NoSuchMethodException {
      final Method method = GcHeapInspection.class.getMethod("getTotalObjectCount");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getTotalHeapSize method")
    void shouldHaveGetTotalHeapSizeMethod() throws NoSuchMethodException {
      final Method method = GcHeapInspection.class.getMethod("getTotalHeapSize");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getUsedHeapSize method")
    void shouldHaveGetUsedHeapSizeMethod() throws NoSuchMethodException {
      final Method method = GcHeapInspection.class.getMethod("getUsedHeapSize");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getFreeHeapSize method")
    void shouldHaveGetFreeHeapSizeMethod() throws NoSuchMethodException {
      final Method method = GcHeapInspection.class.getMethod("getFreeHeapSize");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Distribution Method Tests")
  class DistributionMethodTests {

    @Test
    @DisplayName("should have getObjectTypeDistribution method")
    void shouldHaveGetObjectTypeDistributionMethod() throws NoSuchMethodException {
      final Method method = GcHeapInspection.class.getMethod("getObjectTypeDistribution");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getMemoryUsageByType method")
    void shouldHaveGetMemoryUsageByTypeMethod() throws NoSuchMethodException {
      final Method method = GcHeapInspection.class.getMethod("getMemoryUsageByType");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }
  }

  @Nested
  @DisplayName("Instance Information Method Tests")
  class InstanceInformationMethodTests {

    @Test
    @DisplayName("should have getStructInstances method")
    void shouldHaveGetStructInstancesMethod() throws NoSuchMethodException {
      final Method method = GcHeapInspection.class.getMethod("getStructInstances");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getArrayInstances method")
    void shouldHaveGetArrayInstancesMethod() throws NoSuchMethodException {
      final Method method = GcHeapInspection.class.getMethod("getArrayInstances");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getI31Instances method")
    void shouldHaveGetI31InstancesMethod() throws NoSuchMethodException {
      final Method method = GcHeapInspection.class.getMethod("getI31Instances");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("Analysis Method Tests")
  class AnalysisMethodTests {

    @Test
    @DisplayName("should have getReferenceGraph method")
    void shouldHaveGetReferenceGraphMethod() throws NoSuchMethodException {
      final Method method = GcHeapInspection.class.getMethod("getReferenceGraph");
      assertEquals(ReferenceGraph.class, method.getReturnType(), "Should return ReferenceGraph");
    }

    @Test
    @DisplayName("should have getGcStats method")
    void shouldHaveGetGcStatsMethod() throws NoSuchMethodException {
      final Method method = GcHeapInspection.class.getMethod("getGcStats");
      assertEquals(GcStats.class, method.getReturnType(), "Should return GcStats");
    }

    @Test
    @DisplayName("should have getFragmentationInfo method")
    void shouldHaveGetFragmentationInfoMethod() throws NoSuchMethodException {
      final Method method = GcHeapInspection.class.getMethod("getFragmentationInfo");
      assertEquals(
          GcHeapInspection.HeapFragmentation.class,
          method.getReturnType(),
          "Should return HeapFragmentation");
    }

    @Test
    @DisplayName("should have getRootObjects method")
    void shouldHaveGetRootObjectsMethod() throws NoSuchMethodException {
      final Method method = GcHeapInspection.class.getMethod("getRootObjects");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("StructInstanceInfo Interface Tests")
  class StructInstanceInfoTests {

    @Test
    @DisplayName("should have all struct instance info methods")
    void shouldHaveAllStructInstanceInfoMethods() {
      final String[] expectedMethods = {
        "getObjectId",
        "getStructType",
        "getSize",
        "getFieldValues",
        "getIncomingReferences",
        "getOutgoingReferences"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(GcHeapInspection.StructInstanceInfo.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("ArrayInstanceInfo Interface Tests")
  class ArrayInstanceInfoTests {

    @Test
    @DisplayName("should have all array instance info methods")
    void shouldHaveAllArrayInstanceInfoMethods() {
      final String[] expectedMethods = {
        "getObjectId",
        "getArrayType",
        "getSize",
        "getLength",
        "getElements",
        "getIncomingReferences",
        "getOutgoingReferences"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(GcHeapInspection.ArrayInstanceInfo.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("I31InstanceInfo Interface Tests")
  class I31InstanceInfoTests {

    @Test
    @DisplayName("should have all I31 instance info methods")
    void shouldHaveAllI31InstanceInfoMethods() {
      final String[] expectedMethods = {"getObjectId", "getValue", "getIncomingReferences"};

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(GcHeapInspection.I31InstanceInfo.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("HeapFragmentation Interface Tests")
  class HeapFragmentationTests {

    @Test
    @DisplayName("should have all heap fragmentation methods")
    void shouldHaveAllHeapFragmentationMethods() {
      final String[] expectedMethods = {
        "getFragmentationRatio",
        "getFreeBlockCount",
        "getLargestFreeBlock",
        "getAverageFreeBlockSize"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(GcHeapInspection.HeapFragmentation.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("RootObjectInfo Interface Tests")
  class RootObjectInfoTests {

    @Test
    @DisplayName("should have all root object info methods")
    void shouldHaveAllRootObjectInfoMethods() {
      final String[] expectedMethods = {
        "getObjectId", "getRootType", "getRootLocation", "getObjectType"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(GcHeapInspection.RootObjectInfo.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Usage Pattern Documentation Tests")
  class UsagePatternDocumentationTests {

    @Test
    @DisplayName("should support heap size monitoring")
    void shouldSupportHeapSizeMonitoring() {
      // Documents usage:
      // long total = inspection.getTotalHeapSize();
      // long used = inspection.getUsedHeapSize();
      // double utilization = (double) used / total;
      assertTrue(
          hasMethod(GcHeapInspection.class, "getTotalHeapSize"), "Need getTotalHeapSize method");
      assertTrue(
          hasMethod(GcHeapInspection.class, "getUsedHeapSize"), "Need getUsedHeapSize method");
    }

    @Test
    @DisplayName("should support type distribution analysis")
    void shouldSupportTypeDistributionAnalysis() {
      // Documents usage:
      // Map<String, Long> typeDist = inspection.getObjectTypeDistribution();
      // for (Entry<String, Long> entry : typeDist.entrySet()) { ... }
      assertTrue(
          hasMethod(GcHeapInspection.class, "getObjectTypeDistribution"),
          "Need getObjectTypeDistribution method");
      assertTrue(
          hasMethod(GcHeapInspection.class, "getMemoryUsageByType"),
          "Need getMemoryUsageByType method");
    }

    @Test
    @DisplayName("should support fragmentation analysis")
    void shouldSupportFragmentationAnalysis() {
      // Documents usage:
      // HeapFragmentation frag = inspection.getFragmentationInfo();
      // if (frag.getFragmentationRatio() > 0.5) { ... }
      assertTrue(
          hasMethod(GcHeapInspection.class, "getFragmentationInfo"),
          "Need getFragmentationInfo method");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("WASM GC Specification Compliance Tests")
  class WasmGcSpecificationComplianceTests {

    @Test
    @DisplayName("should have all required heap inspection methods")
    void shouldHaveAllRequiredHeapInspectionMethods() {
      final String[] expectedMethods = {
        "getTotalObjectCount",
        "getTotalHeapSize",
        "getUsedHeapSize",
        "getFreeHeapSize",
        "getObjectTypeDistribution",
        "getMemoryUsageByType",
        "getStructInstances",
        "getArrayInstances",
        "getI31Instances",
        "getReferenceGraph",
        "getGcStats",
        "getFragmentationInfo",
        "getRootObjects"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(GcHeapInspection.class, methodName), "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }
}
