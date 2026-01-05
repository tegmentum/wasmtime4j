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
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the CallSite interface.
 *
 * <p>CallSite represents a function call site for optimization analysis in WebAssembly. This test
 * verifies the interface structure, nested types, and method signatures.
 */
@DisplayName("CallSite Interface Tests")
class CallSiteTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(CallSite.class.isInterface(), "CallSite should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(CallSite.class.getModifiers()), "CallSite should be public");
    }

    @Test
    @DisplayName("should not extend any interface")
    void shouldNotExtendAnyInterface() {
      assertEquals(
          0, CallSite.class.getInterfaces().length, "CallSite should not extend any interface");
    }
  }

  // ========================================================================
  // Abstract Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Methods Tests")
  class AbstractMethodsTests {

    @Test
    @DisplayName("should have getInstructionOffset method")
    void shouldHaveGetInstructionOffsetMethod() throws NoSuchMethodException {
      final Method method = CallSite.class.getMethod("getInstructionOffset");
      assertNotNull(method, "getInstructionOffset method should exist");
      assertEquals(long.class, method.getReturnType(), "getInstructionOffset should return long");
    }

    @Test
    @DisplayName("should have getSourceLocation method")
    void shouldHaveGetSourceLocationMethod() throws NoSuchMethodException {
      final Method method = CallSite.class.getMethod("getSourceLocation");
      assertNotNull(method, "getSourceLocation method should exist");
      assertEquals(
          CallSite.SourceLocation.class,
          method.getReturnType(),
          "getSourceLocation should return SourceLocation");
    }

    @Test
    @DisplayName("should have isInTailPosition method")
    void shouldHaveIsInTailPositionMethod() throws NoSuchMethodException {
      final Method method = CallSite.class.getMethod("isInTailPosition");
      assertNotNull(method, "isInTailPosition method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isInTailPosition should return boolean");
    }

    @Test
    @DisplayName("should have getTargetFunction method")
    void shouldHaveGetTargetFunctionMethod() throws NoSuchMethodException {
      final Method method = CallSite.class.getMethod("getTargetFunction");
      assertNotNull(method, "getTargetFunction method should exist");
      assertEquals(
          FunctionContext.class,
          method.getReturnType(),
          "getTargetFunction should return FunctionContext");
    }

    @Test
    @DisplayName("should have getCallingFunction method")
    void shouldHaveGetCallingFunctionMethod() throws NoSuchMethodException {
      final Method method = CallSite.class.getMethod("getCallingFunction");
      assertNotNull(method, "getCallingFunction method should exist");
      assertEquals(
          FunctionContext.class,
          method.getReturnType(),
          "getCallingFunction should return FunctionContext");
    }

    @Test
    @DisplayName("should have getCallType method")
    void shouldHaveGetCallTypeMethod() throws NoSuchMethodException {
      final Method method = CallSite.class.getMethod("getCallType");
      assertNotNull(method, "getCallType method should exist");
      assertEquals(
          CallSite.CallType.class, method.getReturnType(), "getCallType should return CallType");
    }

    @Test
    @DisplayName("should have getFrequency method")
    void shouldHaveGetFrequencyMethod() throws NoSuchMethodException {
      final Method method = CallSite.class.getMethod("getFrequency");
      assertNotNull(method, "getFrequency method should exist");
      assertEquals(
          CallSite.CallFrequency.class,
          method.getReturnType(),
          "getFrequency should return CallFrequency");
    }

    @Test
    @DisplayName("should have getOptimization method")
    void shouldHaveGetOptimizationMethod() throws NoSuchMethodException {
      final Method method = CallSite.class.getMethod("getOptimization");
      assertNotNull(method, "getOptimization method should exist");
      assertEquals(
          CallSite.CallSiteOptimization.class,
          method.getReturnType(),
          "getOptimization should return CallSiteOptimization");
    }
  }

  // ========================================================================
  // Default Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Methods Tests")
  class DefaultMethodsTests {

    @Test
    @DisplayName("should have isHotSpot default method")
    void shouldHaveIsHotSpotMethod() throws NoSuchMethodException {
      final Method method = CallSite.class.getMethod("isHotSpot");
      assertNotNull(method, "isHotSpot method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isHotSpot should return boolean");
      assertTrue(method.isDefault(), "isHotSpot should be a default method");
    }
  }

  // ========================================================================
  // Static Factory Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Methods Tests")
  class StaticFactoryMethodsTests {

    @Test
    @DisplayName("should have create method with 4 parameters")
    void shouldHaveCreateMethodWith4Parameters() throws NoSuchMethodException {
      final Method method =
          CallSite.class.getMethod(
              "create", long.class, FunctionContext.class, CallSite.CallType.class, boolean.class);
      assertNotNull(method, "create method with 4 parameters should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(CallSite.class, method.getReturnType(), "create should return CallSite");
    }

    @Test
    @DisplayName("should have create method with 6 parameters")
    void shouldHaveCreateMethodWith6Parameters() throws NoSuchMethodException {
      final Method method =
          CallSite.class.getMethod(
              "create",
              long.class,
              FunctionContext.class,
              FunctionContext.class,
              CallSite.CallType.class,
              boolean.class,
              CallSite.SourceLocation.class);
      assertNotNull(method, "create method with 6 parameters should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(CallSite.class, method.getReturnType(), "create should return CallSite");
    }
  }

  // ========================================================================
  // Nested CallType Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested CallType Enum Tests")
  class NestedCallTypeEnumTests {

    @Test
    @DisplayName("CallType should be a nested enum")
    void callTypeShouldBeNestedEnum() {
      Class<?>[] declaredClasses = CallSite.class.getDeclaredClasses();
      boolean hasCallType = false;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("CallType") && clazz.isEnum()) {
          hasCallType = true;
          break;
        }
      }
      assertTrue(hasCallType, "CallSite should have a nested CallType enum");
    }

    @Test
    @DisplayName("CallType should be public")
    void callTypeShouldBePublic() {
      assertTrue(
          Modifier.isPublic(CallSite.CallType.class.getModifiers()), "CallType should be public");
    }

    @Test
    @DisplayName("CallType should have expected enum values")
    void callTypeShouldHaveExpectedEnumValues() {
      Set<String> expectedValues =
          Set.of(
              "DIRECT",
              "INDIRECT",
              "TAIL_CALL",
              "TAIL_CALL_INDIRECT",
              "RETURN_CALL",
              "RETURN_CALL_INDIRECT");

      Set<String> actualValues =
          Arrays.stream(CallSite.CallType.values()).map(Enum::name).collect(Collectors.toSet());

      assertEquals(expectedValues, actualValues, "CallType should have all expected values");
    }

    @Test
    @DisplayName("CallType should have 6 enum constants")
    void callTypeShouldHave6EnumConstants() {
      assertEquals(
          6, CallSite.CallType.values().length, "CallType should have exactly 6 constants");
    }
  }

  // ========================================================================
  // Nested SourceLocation Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested SourceLocation Interface Tests")
  class NestedSourceLocationInterfaceTests {

    @Test
    @DisplayName("SourceLocation should be a nested interface")
    void sourceLocationShouldBeNestedInterface() {
      Class<?>[] declaredClasses = CallSite.class.getDeclaredClasses();
      boolean hasSourceLocation = false;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("SourceLocation") && clazz.isInterface()) {
          hasSourceLocation = true;
          break;
        }
      }
      assertTrue(hasSourceLocation, "CallSite should have a nested SourceLocation interface");
    }

    @Test
    @DisplayName("SourceLocation should be public")
    void sourceLocationShouldBePublic() {
      assertTrue(
          Modifier.isPublic(CallSite.SourceLocation.class.getModifiers()),
          "SourceLocation should be public");
    }

    @Test
    @DisplayName("SourceLocation should have getFileName method")
    void sourceLocationShouldHaveGetFileNameMethod() throws NoSuchMethodException {
      final Method method = CallSite.SourceLocation.class.getMethod("getFileName");
      assertNotNull(method, "getFileName method should exist");
      assertEquals(String.class, method.getReturnType(), "getFileName should return String");
    }

    @Test
    @DisplayName("SourceLocation should have getLineNumber method")
    void sourceLocationShouldHaveGetLineNumberMethod() throws NoSuchMethodException {
      final Method method = CallSite.SourceLocation.class.getMethod("getLineNumber");
      assertNotNull(method, "getLineNumber method should exist");
      assertEquals(int.class, method.getReturnType(), "getLineNumber should return int");
    }

    @Test
    @DisplayName("SourceLocation should have getColumnNumber method")
    void sourceLocationShouldHaveGetColumnNumberMethod() throws NoSuchMethodException {
      final Method method = CallSite.SourceLocation.class.getMethod("getColumnNumber");
      assertNotNull(method, "getColumnNumber method should exist");
      assertEquals(int.class, method.getReturnType(), "getColumnNumber should return int");
    }

    @Test
    @DisplayName("SourceLocation should have getFunctionName method")
    void sourceLocationShouldHaveGetFunctionNameMethod() throws NoSuchMethodException {
      final Method method = CallSite.SourceLocation.class.getMethod("getFunctionName");
      assertNotNull(method, "getFunctionName method should exist");
      assertEquals(String.class, method.getReturnType(), "getFunctionName should return String");
    }
  }

  // ========================================================================
  // Nested CallFrequency Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested CallFrequency Interface Tests")
  class NestedCallFrequencyInterfaceTests {

    @Test
    @DisplayName("CallFrequency should be a nested interface")
    void callFrequencyShouldBeNestedInterface() {
      Class<?>[] declaredClasses = CallSite.class.getDeclaredClasses();
      boolean hasCallFrequency = false;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("CallFrequency") && clazz.isInterface()) {
          hasCallFrequency = true;
          break;
        }
      }
      assertTrue(hasCallFrequency, "CallSite should have a nested CallFrequency interface");
    }

    @Test
    @DisplayName("CallFrequency should have getCallCount method")
    void callFrequencyShouldHaveGetCallCountMethod() throws NoSuchMethodException {
      final Method method = CallSite.CallFrequency.class.getMethod("getCallCount");
      assertNotNull(method, "getCallCount method should exist");
      assertEquals(long.class, method.getReturnType(), "getCallCount should return long");
    }

    @Test
    @DisplayName("CallFrequency should have getTotalExecutionTime method")
    void callFrequencyShouldHaveGetTotalExecutionTimeMethod() throws NoSuchMethodException {
      final Method method = CallSite.CallFrequency.class.getMethod("getTotalExecutionTime");
      assertNotNull(method, "getTotalExecutionTime method should exist");
      assertEquals(long.class, method.getReturnType(), "getTotalExecutionTime should return long");
    }

    @Test
    @DisplayName("CallFrequency should have getAverageExecutionTime method")
    void callFrequencyShouldHaveGetAverageExecutionTimeMethod() throws NoSuchMethodException {
      final Method method = CallSite.CallFrequency.class.getMethod("getAverageExecutionTime");
      assertNotNull(method, "getAverageExecutionTime method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getAverageExecutionTime should return double");
    }

    @Test
    @DisplayName("CallFrequency should have getExecutionTimePercentage method")
    void callFrequencyShouldHaveGetExecutionTimePercentageMethod() throws NoSuchMethodException {
      final Method method = CallSite.CallFrequency.class.getMethod("getExecutionTimePercentage");
      assertNotNull(method, "getExecutionTimePercentage method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getExecutionTimePercentage should return double");
    }

    @Test
    @DisplayName("CallFrequency should have isRecursive method")
    void callFrequencyShouldHaveIsRecursiveMethod() throws NoSuchMethodException {
      final Method method = CallSite.CallFrequency.class.getMethod("isRecursive");
      assertNotNull(method, "isRecursive method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isRecursive should return boolean");
    }

    @Test
    @DisplayName("CallFrequency should have getAverageRecursionDepth method")
    void callFrequencyShouldHaveGetAverageRecursionDepthMethod() throws NoSuchMethodException {
      final Method method = CallSite.CallFrequency.class.getMethod("getAverageRecursionDepth");
      assertNotNull(method, "getAverageRecursionDepth method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getAverageRecursionDepth should return double");
    }
  }

  // ========================================================================
  // Nested CallSiteOptimization Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested CallSiteOptimization Interface Tests")
  class NestedCallSiteOptimizationInterfaceTests {

    @Test
    @DisplayName("CallSiteOptimization should be a nested interface")
    void callSiteOptimizationShouldBeNestedInterface() {
      Class<?>[] declaredClasses = CallSite.class.getDeclaredClasses();
      boolean hasCallSiteOptimization = false;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("CallSiteOptimization") && clazz.isInterface()) {
          hasCallSiteOptimization = true;
          break;
        }
      }
      assertTrue(
          hasCallSiteOptimization, "CallSite should have a nested CallSiteOptimization interface");
    }

    @Test
    @DisplayName("CallSiteOptimization should have isInlined method")
    void callSiteOptimizationShouldHaveIsInlinedMethod() throws NoSuchMethodException {
      final Method method = CallSite.CallSiteOptimization.class.getMethod("isInlined");
      assertNotNull(method, "isInlined method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isInlined should return boolean");
    }

    @Test
    @DisplayName("CallSiteOptimization should have hasTailCallOptimization method")
    void callSiteOptimizationShouldHaveHasTailCallOptimizationMethod()
        throws NoSuchMethodException {
      final Method method =
          CallSite.CallSiteOptimization.class.getMethod("hasTailCallOptimization");
      assertNotNull(method, "hasTailCallOptimization method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "hasTailCallOptimization should return boolean");
    }

    @Test
    @DisplayName("CallSiteOptimization should have isInliningCandidate method")
    void callSiteOptimizationShouldHaveIsInliningCandidateMethod() throws NoSuchMethodException {
      final Method method = CallSite.CallSiteOptimization.class.getMethod("isInliningCandidate");
      assertNotNull(method, "isInliningCandidate method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isInliningCandidate should return boolean");
    }

    @Test
    @DisplayName("CallSiteOptimization should have isTailCallCandidate method")
    void callSiteOptimizationShouldHaveIsTailCallCandidateMethod() throws NoSuchMethodException {
      final Method method = CallSite.CallSiteOptimization.class.getMethod("isTailCallCandidate");
      assertNotNull(method, "isTailCallCandidate method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isTailCallCandidate should return boolean");
    }

    @Test
    @DisplayName("CallSiteOptimization should have getCallCost method")
    void callSiteOptimizationShouldHaveGetCallCostMethod() throws NoSuchMethodException {
      final Method method = CallSite.CallSiteOptimization.class.getMethod("getCallCost");
      assertNotNull(method, "getCallCost method should exist");
      assertEquals(int.class, method.getReturnType(), "getCallCost should return int");
    }

    @Test
    @DisplayName("CallSiteOptimization should have getOptimizationBenefit method")
    void callSiteOptimizationShouldHaveGetOptimizationBenefitMethod() throws NoSuchMethodException {
      final Method method = CallSite.CallSiteOptimization.class.getMethod("getOptimizationBenefit");
      assertNotNull(method, "getOptimizationBenefit method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getOptimizationBenefit should return double");
    }

    @Test
    @DisplayName("CallSiteOptimization should have getOptimizationReason method")
    void callSiteOptimizationShouldHaveGetOptimizationReasonMethod() throws NoSuchMethodException {
      final Method method = CallSite.CallSiteOptimization.class.getMethod("getOptimizationReason");
      assertNotNull(method, "getOptimizationReason method should exist");
      assertEquals(
          String.class, method.getReturnType(), "getOptimizationReason should return String");
    }
  }

  // ========================================================================
  // Nested Implementation Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Implementation Classes Tests")
  class NestedImplementationClassesTests {

    @Test
    @DisplayName("should have CallFrequencyImpl nested class")
    void shouldHaveCallFrequencyImplNestedClass() {
      Class<?>[] declaredClasses = CallSite.class.getDeclaredClasses();
      boolean hasCallFrequencyImpl = false;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("CallFrequencyImpl")) {
          hasCallFrequencyImpl = true;
          assertTrue(
              CallSite.CallFrequency.class.isAssignableFrom(clazz),
              "CallFrequencyImpl should implement CallFrequency");
          break;
        }
      }
      assertTrue(hasCallFrequencyImpl, "CallSite should have a nested CallFrequencyImpl class");
    }

    @Test
    @DisplayName("should have CallSiteOptimizationImpl nested class")
    void shouldHaveCallSiteOptimizationImplNestedClass() {
      Class<?>[] declaredClasses = CallSite.class.getDeclaredClasses();
      boolean hasCallSiteOptimizationImpl = false;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("CallSiteOptimizationImpl")) {
          hasCallSiteOptimizationImpl = true;
          assertTrue(
              CallSite.CallSiteOptimization.class.isAssignableFrom(clazz),
              "CallSiteOptimizationImpl should implement CallSiteOptimization");
          break;
        }
      }
      assertTrue(
          hasCallSiteOptimizationImpl,
          "CallSite should have a nested CallSiteOptimizationImpl class");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          Set.of(
              "getInstructionOffset",
              "getSourceLocation",
              "isInTailPosition",
              "getTargetFunction",
              "getCallingFunction",
              "getCallType",
              "getFrequency",
              "isHotSpot",
              "getOptimization",
              "create");

      Set<String> actualMethods =
          Arrays.stream(CallSite.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "CallSite should have method: " + expected);
      }
    }
  }

  // ========================================================================
  // Nested Class Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Class Count Tests")
  class NestedClassCountTests {

    @Test
    @DisplayName("should have at least 5 nested types")
    void shouldHaveAtLeast5NestedTypes() {
      // CallType enum, SourceLocation, CallFrequency, CallSiteOptimization interfaces
      // CallFrequencyImpl, CallSiteOptimizationImpl classes
      assertTrue(
          CallSite.class.getDeclaredClasses().length >= 5,
          "CallSite should have at least 5 nested types");
    }

    @Test
    @DisplayName("should have exactly one nested enum")
    void shouldHaveExactlyOneNestedEnum() {
      long enumCount =
          Arrays.stream(CallSite.class.getDeclaredClasses()).filter(Class::isEnum).count();
      assertEquals(1, enumCount, "CallSite should have exactly 1 nested enum (CallType)");
    }

    @Test
    @DisplayName("should have at least 3 nested interfaces")
    void shouldHaveAtLeast3NestedInterfaces() {
      long interfaceCount =
          Arrays.stream(CallSite.class.getDeclaredClasses()).filter(Class::isInterface).count();
      assertTrue(interfaceCount >= 3, "CallSite should have at least 3 nested interfaces");
    }
  }
}
