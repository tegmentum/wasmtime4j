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

package ai.tegmentum.wasmtime4j.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ExecutionState interface.
 *
 * <p>This test class verifies the interface structure, methods, and nested types for ExecutionState
 * using reflection-based testing.
 */
@DisplayName("ExecutionState Tests")
class ExecutionStateTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ExecutionState should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ExecutionState.class.isInterface(), "ExecutionState should be an interface");
    }

    @Test
    @DisplayName("ExecutionState should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionState.class.getModifiers()),
          "ExecutionState should be public");
    }

    @Test
    @DisplayName("ExecutionState should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = ExecutionState.class.getInterfaces();
      assertEquals(0, interfaces.length, "ExecutionState should not extend other interfaces");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getExecutionId method")
    void shouldHaveGetExecutionIdMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getExecutionId");
      assertNotNull(method, "getExecutionId method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getStatus method")
    void shouldHaveGetStatusMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getStatus");
      assertNotNull(method, "getStatus method should exist");
      assertEquals(
          ExecutionStatus.class, method.getReturnType(), "Return type should be ExecutionStatus");
    }

    @Test
    @DisplayName("should have getPhase method")
    void shouldHaveGetPhaseMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getPhase");
      assertNotNull(method, "getPhase method should exist");
      assertEquals(
          ExecutionState.ExecutionPhase.class,
          method.getReturnType(),
          "Return type should be ExecutionPhase");
    }

    @Test
    @DisplayName("should have getStartTime method")
    void shouldHaveGetStartTimeMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getStartTime");
      assertNotNull(method, "getStartTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getCurrentTime method")
    void shouldHaveGetCurrentTimeMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getCurrentTime");
      assertNotNull(method, "getCurrentTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getDuration method")
    void shouldHaveGetDurationMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getDuration");
      assertNotNull(method, "getDuration method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getContext method")
    void shouldHaveGetContextMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getContext");
      assertNotNull(method, "getContext method should exist");
      assertEquals(
          ExecutionContext.class, method.getReturnType(), "Return type should be ExecutionContext");
    }

    @Test
    @DisplayName("should have getQuotas method")
    void shouldHaveGetQuotasMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getQuotas");
      assertNotNull(method, "getQuotas method should exist");
      assertEquals(
          ExecutionQuotas.class, method.getReturnType(), "Return type should be ExecutionQuotas");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          ExecutionStatistics.class,
          method.getReturnType(),
          "Return type should be ExecutionStatistics");
    }

    @Test
    @DisplayName("should have getStackTrace method")
    void shouldHaveGetStackTraceMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getStackTrace");
      assertNotNull(method, "getStackTrace method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have getCurrentFunction method")
    void shouldHaveGetCurrentFunctionMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getCurrentFunction");
      assertNotNull(method, "getCurrentFunction method should exist");
      assertEquals(
          ExecutionState.CurrentFunctionInfo.class,
          method.getReturnType(),
          "Return type should be CurrentFunctionInfo");
    }

    @Test
    @DisplayName("should have getMetadata method")
    void shouldHaveGetMetadataMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getMetadata");
      assertNotNull(method, "getMetadata method should exist");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
    }

    @Test
    @DisplayName("should have getWarnings method")
    void shouldHaveGetWarningsMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getWarnings");
      assertNotNull(method, "getWarnings method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have getError method")
    void shouldHaveGetErrorMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getError");
      assertNotNull(method, "getError method should exist");
      assertEquals(Throwable.class, method.getReturnType(), "Return type should be Throwable");
    }

    @Test
    @DisplayName("should have boolean status methods")
    void shouldHaveBooleanStatusMethods() throws NoSuchMethodException {
      Method isActive = ExecutionState.class.getMethod("isActive");
      assertEquals(boolean.class, isActive.getReturnType(), "isActive should return boolean");

      Method isSuspended = ExecutionState.class.getMethod("isSuspended");
      assertEquals(boolean.class, isSuspended.getReturnType(), "isSuspended should return boolean");

      Method isCompleted = ExecutionState.class.getMethod("isCompleted");
      assertEquals(boolean.class, isCompleted.getReturnType(), "isCompleted should return boolean");

      Method hasFailed = ExecutionState.class.getMethod("hasFailed");
      assertEquals(boolean.class, hasFailed.getReturnType(), "hasFailed should return boolean");
    }

    @Test
    @DisplayName("should have getSuspensionReason method")
    void shouldHaveGetSuspensionReasonMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getSuspensionReason");
      assertNotNull(method, "getSuspensionReason method should exist");
      assertEquals(
          ExecutionState.SuspensionReason.class,
          method.getReturnType(),
          "Return type should be SuspensionReason");
    }

    @Test
    @DisplayName("should have getTerminationReason method")
    void shouldHaveGetTerminationReasonMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getTerminationReason");
      assertNotNull(method, "getTerminationReason method should exist");
      assertEquals(
          ExecutionState.TerminationReason.class,
          method.getReturnType(),
          "Return type should be TerminationReason");
    }

    @Test
    @DisplayName("should have getProgress method")
    void shouldHaveGetProgressMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getProgress");
      assertNotNull(method, "getProgress method should exist");
      assertEquals(
          ExecutionState.ProgressInfo.class,
          method.getReturnType(),
          "Return type should be ProgressInfo");
    }

    @Test
    @DisplayName("should have createSnapshot method")
    void shouldHaveCreateSnapshotMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("createSnapshot");
      assertNotNull(method, "createSnapshot method should exist");
      assertEquals(
          ExecutionState.ExecutionStateSnapshot.class,
          method.getReturnType(),
          "Return type should be ExecutionStateSnapshot");
    }
  }

  // ========================================================================
  // ExecutionPhase Nested Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ExecutionPhase Enum Tests")
  class ExecutionPhaseTests {

    @Test
    @DisplayName("ExecutionPhase should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(ExecutionState.ExecutionPhase.class.isEnum(), "ExecutionPhase should be an enum");
      assertTrue(
          ExecutionState.ExecutionPhase.class.isMemberClass(),
          "ExecutionPhase should be a member class");
    }

    @Test
    @DisplayName("ExecutionPhase should have 9 values")
    void shouldHaveNineValues() {
      ExecutionState.ExecutionPhase[] values = ExecutionState.ExecutionPhase.values();
      assertEquals(9, values.length, "ExecutionPhase should have 9 values");
    }

    @Test
    @DisplayName("ExecutionPhase should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of(
              "INITIALIZATION",
              "PRE_EXECUTION",
              "FUNCTION_EXECUTION",
              "HOST_FUNCTION_CALL",
              "MEMORY_OPERATION",
              "EXCEPTION_HANDLING",
              "POST_EXECUTION",
              "CLEANUP",
              "FINALIZATION");
      Set<String> actualNames = new HashSet<>();
      for (ExecutionState.ExecutionPhase phase : ExecutionState.ExecutionPhase.values()) {
        actualNames.add(phase.name());
      }
      assertEquals(expectedNames, actualNames, "ExecutionPhase should have expected values");
    }
  }

  // ========================================================================
  // SuspensionReason Nested Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("SuspensionReason Enum Tests")
  class SuspensionReasonTests {

    @Test
    @DisplayName("SuspensionReason should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ExecutionState.SuspensionReason.class.isEnum(), "SuspensionReason should be an enum");
    }

    @Test
    @DisplayName("SuspensionReason should have 6 values")
    void shouldHaveSixValues() {
      ExecutionState.SuspensionReason[] values = ExecutionState.SuspensionReason.values();
      assertEquals(6, values.length, "SuspensionReason should have 6 values");
    }

    @Test
    @DisplayName("SuspensionReason should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of(
              "USER_REQUESTED",
              "RESOURCE_LIMIT",
              "BREAKPOINT",
              "DEBUGGING",
              "SYSTEM_OVERLOAD",
              "WAITING_FOR_RESOURCE");
      Set<String> actualNames = new HashSet<>();
      for (ExecutionState.SuspensionReason reason : ExecutionState.SuspensionReason.values()) {
        actualNames.add(reason.name());
      }
      assertEquals(expectedNames, actualNames, "SuspensionReason should have expected values");
    }
  }

  // ========================================================================
  // TerminationReason Nested Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("TerminationReason Enum Tests")
  class TerminationReasonTests {

    @Test
    @DisplayName("TerminationReason should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ExecutionState.TerminationReason.class.isEnum(), "TerminationReason should be an enum");
    }

    @Test
    @DisplayName("TerminationReason should have 8 values")
    void shouldHaveEightValues() {
      ExecutionState.TerminationReason[] values = ExecutionState.TerminationReason.values();
      assertEquals(8, values.length, "TerminationReason should have 8 values");
    }

    @Test
    @DisplayName("TerminationReason should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of(
              "COMPLETED",
              "USER_TERMINATED",
              "TIMEOUT",
              "MEMORY_LIMIT",
              "FUEL_EXHAUSTED",
              "EXCEPTION",
              "SYSTEM_ERROR",
              "SECURITY_VIOLATION");
      Set<String> actualNames = new HashSet<>();
      for (ExecutionState.TerminationReason reason : ExecutionState.TerminationReason.values()) {
        actualNames.add(reason.name());
      }
      assertEquals(expectedNames, actualNames, "TerminationReason should have expected values");
    }
  }

  // ========================================================================
  // WarningType Nested Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("WarningType Enum Tests")
  class WarningTypeTests {

    @Test
    @DisplayName("WarningType should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(ExecutionState.WarningType.class.isEnum(), "WarningType should be an enum");
    }

    @Test
    @DisplayName("WarningType should have 7 values")
    void shouldHaveSevenValues() {
      ExecutionState.WarningType[] values = ExecutionState.WarningType.values();
      assertEquals(7, values.length, "WarningType should have 7 values");
    }

    @Test
    @DisplayName("WarningType should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of(
              "PERFORMANCE",
              "MEMORY_USAGE",
              "QUOTA_LIMIT",
              "RESOURCE_CONTENTION",
              "DEPRECATED_FEATURE",
              "SECURITY",
              "CUSTOM");
      Set<String> actualNames = new HashSet<>();
      for (ExecutionState.WarningType type : ExecutionState.WarningType.values()) {
        actualNames.add(type.name());
      }
      assertEquals(expectedNames, actualNames, "WarningType should have expected values");
    }
  }

  // ========================================================================
  // WarningSeverity Nested Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("WarningSeverity Enum Tests")
  class WarningSeverityTests {

    @Test
    @DisplayName("WarningSeverity should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ExecutionState.WarningSeverity.class.isEnum(), "WarningSeverity should be an enum");
    }

    @Test
    @DisplayName("WarningSeverity should have 4 values")
    void shouldHaveFourValues() {
      ExecutionState.WarningSeverity[] values = ExecutionState.WarningSeverity.values();
      assertEquals(4, values.length, "WarningSeverity should have 4 values");
    }

    @Test
    @DisplayName("WarningSeverity should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("INFO", "LOW", "MEDIUM", "HIGH");
      Set<String> actualNames = new HashSet<>();
      for (ExecutionState.WarningSeverity severity : ExecutionState.WarningSeverity.values()) {
        actualNames.add(severity.name());
      }
      assertEquals(expectedNames, actualNames, "WarningSeverity should have expected values");
    }
  }

  // ========================================================================
  // CurrentFunctionInfo Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("CurrentFunctionInfo Interface Tests")
  class CurrentFunctionInfoTests {

    @Test
    @DisplayName("CurrentFunctionInfo should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ExecutionState.CurrentFunctionInfo.class.isInterface(),
          "CurrentFunctionInfo should be an interface");
    }

    @Test
    @DisplayName("CurrentFunctionInfo should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      Method method = ExecutionState.CurrentFunctionInfo.class.getMethod("getName");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("CurrentFunctionInfo should have getModuleName method")
    void shouldHaveGetModuleNameMethod() throws NoSuchMethodException {
      Method method = ExecutionState.CurrentFunctionInfo.class.getMethod("getModuleName");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("CurrentFunctionInfo should have getIndex method")
    void shouldHaveGetIndexMethod() throws NoSuchMethodException {
      Method method = ExecutionState.CurrentFunctionInfo.class.getMethod("getIndex");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("CurrentFunctionInfo should have getInstructionPointer method")
    void shouldHaveGetInstructionPointerMethod() throws NoSuchMethodException {
      Method method = ExecutionState.CurrentFunctionInfo.class.getMethod("getInstructionPointer");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("CurrentFunctionInfo should have getParameters method")
    void shouldHaveGetParametersMethod() throws NoSuchMethodException {
      Method method = ExecutionState.CurrentFunctionInfo.class.getMethod("getParameters");
      assertEquals(Object[].class, method.getReturnType(), "Return type should be Object[]");
    }

    @Test
    @DisplayName("CurrentFunctionInfo should have getLocalVariables method")
    void shouldHaveGetLocalVariablesMethod() throws NoSuchMethodException {
      Method method = ExecutionState.CurrentFunctionInfo.class.getMethod("getLocalVariables");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
    }

    @Test
    @DisplayName("CurrentFunctionInfo should have getEntryTime method")
    void shouldHaveGetEntryTimeMethod() throws NoSuchMethodException {
      Method method = ExecutionState.CurrentFunctionInfo.class.getMethod("getEntryTime");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("CurrentFunctionInfo should have getExecutionDuration method")
    void shouldHaveGetExecutionDurationMethod() throws NoSuchMethodException {
      Method method = ExecutionState.CurrentFunctionInfo.class.getMethod("getExecutionDuration");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }
  }

  // ========================================================================
  // StackFrame Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("StackFrame Interface Tests")
  class StackFrameTests {

    @Test
    @DisplayName("StackFrame should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ExecutionState.StackFrame.class.isInterface(), "StackFrame should be an interface");
    }

    @Test
    @DisplayName("StackFrame should have getFunctionName method")
    void shouldHaveGetFunctionNameMethod() throws NoSuchMethodException {
      Method method = ExecutionState.StackFrame.class.getMethod("getFunctionName");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("StackFrame should have getModuleName method")
    void shouldHaveGetModuleNameMethod() throws NoSuchMethodException {
      Method method = ExecutionState.StackFrame.class.getMethod("getModuleName");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("StackFrame should have getInstructionPointer method")
    void shouldHaveGetInstructionPointerMethod() throws NoSuchMethodException {
      Method method = ExecutionState.StackFrame.class.getMethod("getInstructionPointer");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("StackFrame should have getDepth method")
    void shouldHaveGetDepthMethod() throws NoSuchMethodException {
      Method method = ExecutionState.StackFrame.class.getMethod("getDepth");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("StackFrame should have getLocalVariables method")
    void shouldHaveGetLocalVariablesMethod() throws NoSuchMethodException {
      Method method = ExecutionState.StackFrame.class.getMethod("getLocalVariables");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
    }
  }

  // ========================================================================
  // ProgressInfo Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("ProgressInfo Interface Tests")
  class ProgressInfoTests {

    @Test
    @DisplayName("ProgressInfo should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ExecutionState.ProgressInfo.class.isInterface(), "ProgressInfo should be an interface");
    }

    @Test
    @DisplayName("ProgressInfo should have getCompletionPercentage method")
    void shouldHaveGetCompletionPercentageMethod() throws NoSuchMethodException {
      Method method = ExecutionState.ProgressInfo.class.getMethod("getCompletionPercentage");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("ProgressInfo should have getEstimatedTimeRemaining method")
    void shouldHaveGetEstimatedTimeRemainingMethod() throws NoSuchMethodException {
      Method method = ExecutionState.ProgressInfo.class.getMethod("getEstimatedTimeRemaining");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("ProgressInfo should have getMilestones method")
    void shouldHaveGetMilestonesMethod() throws NoSuchMethodException {
      Method method = ExecutionState.ProgressInfo.class.getMethod("getMilestones");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("ProgressInfo should have getCurrentMilestone method")
    void shouldHaveGetCurrentMilestoneMethod() throws NoSuchMethodException {
      Method method = ExecutionState.ProgressInfo.class.getMethod("getCurrentMilestone");
      assertEquals(
          ExecutionState.ProgressMilestone.class,
          method.getReturnType(),
          "Return type should be ProgressMilestone");
    }
  }

  // ========================================================================
  // Nested Class Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Class Count Tests")
  class NestedClassCountTests {

    @Test
    @DisplayName("ExecutionState should have expected number of nested classes/interfaces")
    void shouldHaveExpectedNestedClassCount() {
      Class<?>[] nestedClasses = ExecutionState.class.getDeclaredClasses();
      // Expecting: ExecutionPhase, CurrentFunctionInfo, ExecutionWarning, ProgressInfo,
      // ProgressMilestone, ExecutionStateSnapshot, StackFrame, SuspensionReason,
      // TerminationReason, WarningType, WarningSeverity = 11 nested types
      assertTrue(
          nestedClasses.length >= 10,
          "ExecutionState should have at least 10 nested types, found: " + nestedClasses.length);
    }

    @Test
    @DisplayName("All nested enums should be public")
    void allNestedEnumsShouldBePublic() {
      Class<?>[] nestedClasses = ExecutionState.class.getDeclaredClasses();
      for (Class<?> nestedClass : nestedClasses) {
        if (nestedClass.isEnum()) {
          assertTrue(
              Modifier.isPublic(nestedClass.getModifiers()),
              nestedClass.getSimpleName() + " should be public");
        }
      }
    }

    @Test
    @DisplayName("All nested interfaces should be public")
    void allNestedInterfacesShouldBePublic() {
      Class<?>[] nestedClasses = ExecutionState.class.getDeclaredClasses();
      for (Class<?> nestedClass : nestedClasses) {
        if (nestedClass.isInterface() && !nestedClass.isAnnotation()) {
          assertTrue(
              Modifier.isPublic(nestedClass.getModifiers()),
              nestedClass.getSimpleName() + " should be public");
        }
      }
    }
  }
}
