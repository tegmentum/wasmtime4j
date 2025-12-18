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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for the execution package interfaces and enums.
 *
 * <p>This test class covers:
 * <ul>
 *   <li>ExecutionContext - Context management interface</li>
 *   <li>ExecutionPolicy - Policy management interface</li>
 *   <li>ExecutionQuotas - Quota management interface</li>
 *   <li>ExecutionRequest - Request definition interface</li>
 *   <li>ExecutionState - State tracking interface</li>
 *   <li>ExecutionStatistics - Statistics interface</li>
 *   <li>ExecutionResult - Result representation interface</li>
 * </ul>
 */
@DisplayName("Execution Package Tests")
class ExecutionPackageTest {

  // ============================================
  // ExecutionContext Tests
  // ============================================

  @Nested
  @DisplayName("ExecutionContext Interface Tests")
  class ExecutionContextTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ExecutionContext.class.isInterface(), "ExecutionContext should be an interface");
    }

    @Test
    @DisplayName("should have getContextId method")
    void shouldHaveGetContextIdMethod() throws NoSuchMethodException {
      Method method = ExecutionContext.class.getMethod("getContextId");
      assertEquals(String.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getEnvironment method")
    void shouldHaveGetEnvironmentMethod() throws NoSuchMethodException {
      Method method = ExecutionContext.class.getMethod("getEnvironment");
      assertEquals(ExecutionContext.ExecutionEnvironment.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getParameters method")
    void shouldHaveGetParametersMethod() throws NoSuchMethodException {
      Method method = ExecutionContext.class.getMethod("getParameters");
      assertEquals(Map.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have setParameter method")
    void shouldHaveSetParameterMethod() throws NoSuchMethodException {
      Method method = ExecutionContext.class.getMethod("setParameter", String.class, Object.class);
      assertEquals(void.class, method.getReturnType());
      assertEquals(2, method.getParameterCount());
    }

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      Method method = ExecutionContext.class.getMethod("getConfig");
      assertEquals(ExecutionContextConfig.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getSecurityContext method")
    void shouldHaveGetSecurityContextMethod() throws NoSuchMethodException {
      Method method = ExecutionContext.class.getMethod("getSecurityContext");
      assertEquals(Object.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getMetadata method")
    void shouldHaveGetMetadataMethod() throws NoSuchMethodException {
      Method method = ExecutionContext.class.getMethod("getMetadata");
      assertEquals(ExecutionContext.ExecutionMetadata.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = ExecutionContext.class.getMethod("isValid");
      assertEquals(boolean.class, method.getReturnType());
    }

    @Nested
    @DisplayName("ExecutionEnvironment Inner Interface Tests")
    class ExecutionEnvironmentTests {

      @Test
      @DisplayName("should be an inner interface")
      void shouldBeInnerInterface() {
        assertTrue(
            ExecutionContext.ExecutionEnvironment.class.isInterface(),
            "ExecutionEnvironment should be an interface");
        assertEquals(
            ExecutionContext.class,
            ExecutionContext.ExecutionEnvironment.class.getEnclosingClass());
      }

      @Test
      @DisplayName("should have getEnvironmentVariables method")
      void shouldHaveGetEnvironmentVariablesMethod() throws NoSuchMethodException {
        Method method =
            ExecutionContext.ExecutionEnvironment.class.getMethod("getEnvironmentVariables");
        assertEquals(Map.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getWorkingDirectory method")
      void shouldHaveGetWorkingDirectoryMethod() throws NoSuchMethodException {
        Method method =
            ExecutionContext.ExecutionEnvironment.class.getMethod("getWorkingDirectory");
        assertEquals(String.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getResourceLimits method")
      void shouldHaveGetResourceLimitsMethod() throws NoSuchMethodException {
        Method method = ExecutionContext.ExecutionEnvironment.class.getMethod("getResourceLimits");
        assertEquals(ExecutionContext.ResourceLimits.class, method.getReturnType());
      }
    }

    @Nested
    @DisplayName("ResourceLimits Inner Interface Tests")
    class ResourceLimitsTests {

      @Test
      @DisplayName("should be an inner interface")
      void shouldBeInnerInterface() {
        assertTrue(
            ExecutionContext.ResourceLimits.class.isInterface(),
            "ResourceLimits should be an interface");
      }

      @Test
      @DisplayName("should have getMaxMemory method")
      void shouldHaveGetMaxMemoryMethod() throws NoSuchMethodException {
        Method method = ExecutionContext.ResourceLimits.class.getMethod("getMaxMemory");
        assertEquals(long.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getMaxExecutionTime method")
      void shouldHaveGetMaxExecutionTimeMethod() throws NoSuchMethodException {
        Method method = ExecutionContext.ResourceLimits.class.getMethod("getMaxExecutionTime");
        assertEquals(long.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getMaxCpuTime method")
      void shouldHaveGetMaxCpuTimeMethod() throws NoSuchMethodException {
        Method method = ExecutionContext.ResourceLimits.class.getMethod("getMaxCpuTime");
        assertEquals(long.class, method.getReturnType());
      }
    }

    @Nested
    @DisplayName("ExecutionMetadata Inner Interface Tests")
    class ExecutionMetadataTests {

      @Test
      @DisplayName("should have getCreationTime method")
      void shouldHaveGetCreationTimeMethod() throws NoSuchMethodException {
        Method method = ExecutionContext.ExecutionMetadata.class.getMethod("getCreationTime");
        assertEquals(long.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getLastAccessTime method")
      void shouldHaveGetLastAccessTimeMethod() throws NoSuchMethodException {
        Method method = ExecutionContext.ExecutionMetadata.class.getMethod("getLastAccessTime");
        assertEquals(long.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getCreator method")
      void shouldHaveGetCreatorMethod() throws NoSuchMethodException {
        Method method = ExecutionContext.ExecutionMetadata.class.getMethod("getCreator");
        assertEquals(String.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getTags method")
      void shouldHaveGetTagsMethod() throws NoSuchMethodException {
        Method method = ExecutionContext.ExecutionMetadata.class.getMethod("getTags");
        assertEquals(Set.class, method.getReturnType());
      }
    }
  }

  // ============================================
  // ExecutionPolicy Tests
  // ============================================

  @Nested
  @DisplayName("ExecutionPolicy Interface Tests")
  class ExecutionPolicyTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ExecutionPolicy.class.isInterface(), "ExecutionPolicy should be an interface");
    }

    @Test
    @DisplayName("should have getPolicyName method")
    void shouldHaveGetPolicyNameMethod() throws NoSuchMethodException {
      Method method = ExecutionPolicy.class.getMethod("getPolicyName");
      assertEquals(String.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have isAllowed method")
    void shouldHaveIsAllowedMethod() throws NoSuchMethodException {
      Method method = ExecutionPolicy.class.getMethod("isAllowed", String.class);
      assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getMaxExecutionTime method")
    void shouldHaveGetMaxExecutionTimeMethod() throws NoSuchMethodException {
      Method method = ExecutionPolicy.class.getMethod("getMaxExecutionTime");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getMaxMemoryUsage method")
    void shouldHaveGetMaxMemoryUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionPolicy.class.getMethod("getMaxMemoryUsage");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getMaxCpuUsage method")
    void shouldHaveGetMaxCpuUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionPolicy.class.getMethod("getMaxCpuUsage");
      assertEquals(double.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getRestrictedOperations method")
    void shouldHaveGetRestrictedOperationsMethod() throws NoSuchMethodException {
      Method method = ExecutionPolicy.class.getMethod("getRestrictedOperations");
      assertEquals(Set.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getAllowedOperations method")
    void shouldHaveGetAllowedOperationsMethod() throws NoSuchMethodException {
      Method method = ExecutionPolicy.class.getMethod("getAllowedOperations");
      assertEquals(Set.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getEnforcementLevel method")
    void shouldHaveGetEnforcementLevelMethod() throws NoSuchMethodException {
      Method method = ExecutionPolicy.class.getMethod("getEnforcementLevel");
      assertEquals(ExecutionPolicy.EnforcementLevel.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getViolationHandling method")
    void shouldHaveGetViolationHandlingMethod() throws NoSuchMethodException {
      Method method = ExecutionPolicy.class.getMethod("getViolationHandling");
      assertEquals(ExecutionPolicy.ViolationHandling.class, method.getReturnType());
    }

    @Nested
    @DisplayName("EnforcementLevel Enum Tests")
    class EnforcementLevelTests {

      @Test
      @DisplayName("should have 4 enforcement levels")
      void shouldHaveFourEnforcementLevels() {
        assertEquals(4, ExecutionPolicy.EnforcementLevel.values().length);
      }

      @Test
      @DisplayName("should have ADVISORY level")
      void shouldHaveAdvisoryLevel() {
        assertNotNull(ExecutionPolicy.EnforcementLevel.ADVISORY);
        assertEquals("ADVISORY", ExecutionPolicy.EnforcementLevel.ADVISORY.name());
      }

      @Test
      @DisplayName("should have WARNING level")
      void shouldHaveWarningLevel() {
        assertNotNull(ExecutionPolicy.EnforcementLevel.WARNING);
        assertEquals("WARNING", ExecutionPolicy.EnforcementLevel.WARNING.name());
      }

      @Test
      @DisplayName("should have BLOCKING level")
      void shouldHaveBlockingLevel() {
        assertNotNull(ExecutionPolicy.EnforcementLevel.BLOCKING);
        assertEquals("BLOCKING", ExecutionPolicy.EnforcementLevel.BLOCKING.name());
      }

      @Test
      @DisplayName("should have STRICT level")
      void shouldHaveStrictLevel() {
        assertNotNull(ExecutionPolicy.EnforcementLevel.STRICT);
        assertEquals("STRICT", ExecutionPolicy.EnforcementLevel.STRICT.name());
      }

      @Test
      @DisplayName("should support valueOf")
      void shouldSupportValueOf() {
        assertEquals(
            ExecutionPolicy.EnforcementLevel.ADVISORY,
            ExecutionPolicy.EnforcementLevel.valueOf("ADVISORY"));
        assertEquals(
            ExecutionPolicy.EnforcementLevel.STRICT,
            ExecutionPolicy.EnforcementLevel.valueOf("STRICT"));
      }

      @Test
      @DisplayName("should throw for invalid valueOf")
      void shouldThrowForInvalidValueOf() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ExecutionPolicy.EnforcementLevel.valueOf("INVALID"));
      }

      @Test
      @DisplayName("should have correct ordinals")
      void shouldHaveCorrectOrdinals() {
        assertEquals(0, ExecutionPolicy.EnforcementLevel.ADVISORY.ordinal());
        assertEquals(1, ExecutionPolicy.EnforcementLevel.WARNING.ordinal());
        assertEquals(2, ExecutionPolicy.EnforcementLevel.BLOCKING.ordinal());
        assertEquals(3, ExecutionPolicy.EnforcementLevel.STRICT.ordinal());
      }
    }

    @Nested
    @DisplayName("ViolationHandling Enum Tests")
    class ViolationHandlingTests {

      @Test
      @DisplayName("should have 4 violation handling types")
      void shouldHaveFourViolationHandlingTypes() {
        assertEquals(4, ExecutionPolicy.ViolationHandling.values().length);
      }

      @Test
      @DisplayName("should have IGNORE handling")
      void shouldHaveIgnoreHandling() {
        assertNotNull(ExecutionPolicy.ViolationHandling.IGNORE);
      }

      @Test
      @DisplayName("should have LOG handling")
      void shouldHaveLogHandling() {
        assertNotNull(ExecutionPolicy.ViolationHandling.LOG);
      }

      @Test
      @DisplayName("should have EXCEPTION handling")
      void shouldHaveExceptionHandling() {
        assertNotNull(ExecutionPolicy.ViolationHandling.EXCEPTION);
      }

      @Test
      @DisplayName("should have TERMINATE handling")
      void shouldHaveTerminateHandling() {
        assertNotNull(ExecutionPolicy.ViolationHandling.TERMINATE);
      }

      @Test
      @DisplayName("should be comparable")
      void shouldBeComparable() {
        assertTrue(
            ExecutionPolicy.ViolationHandling.IGNORE.compareTo(
                    ExecutionPolicy.ViolationHandling.TERMINATE)
                < 0);
      }
    }
  }

  // ============================================
  // ExecutionQuotas Tests
  // ============================================

  @Nested
  @DisplayName("ExecutionQuotas Interface Tests")
  class ExecutionQuotasTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ExecutionQuotas.class.isInterface(), "ExecutionQuotas should be an interface");
    }

    @Test
    @DisplayName("should have getFuelQuota method")
    void shouldHaveGetFuelQuotaMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("getFuelQuota");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have setFuelQuota method")
    void shouldHaveSetFuelQuotaMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("setFuelQuota", long.class);
      assertEquals(void.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getRemainingFuel method")
    void shouldHaveGetRemainingFuelMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("getRemainingFuel");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getMemoryQuota method")
    void shouldHaveGetMemoryQuotaMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("getMemoryQuota");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have setMemoryQuota method")
    void shouldHaveSetMemoryQuotaMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("setMemoryQuota", long.class);
      assertEquals(void.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getTimeQuota method")
    void shouldHaveGetTimeQuotaMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("getTimeQuota");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have setTimeQuota method")
    void shouldHaveSetTimeQuotaMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("setTimeQuota", long.class);
      assertEquals(void.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getRemainingTime method")
    void shouldHaveGetRemainingTimeMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("getRemainingTime");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getInstructionQuota method")
    void shouldHaveGetInstructionQuotaMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("getInstructionQuota");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have setInstructionQuota method")
    void shouldHaveSetInstructionQuotaMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("setInstructionQuota", long.class);
      assertEquals(void.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getRemainingInstructions method")
    void shouldHaveGetRemainingInstructionsMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("getRemainingInstructions");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have isQuotaExceeded method")
    void shouldHaveIsQuotaExceededMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("isQuotaExceeded");
      assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getUsage method")
    void shouldHaveGetUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("getUsage");
      assertEquals(ExecutionQuotas.QuotaUsage.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have reset method")
    void shouldHaveResetMethod() throws NoSuchMethodException {
      Method method = ExecutionQuotas.class.getMethod("reset");
      assertEquals(void.class, method.getReturnType());
    }

    @Nested
    @DisplayName("QuotaUsage Inner Interface Tests")
    class QuotaUsageTests {

      @Test
      @DisplayName("should be an inner interface")
      void shouldBeInnerInterface() {
        assertTrue(ExecutionQuotas.QuotaUsage.class.isInterface());
        assertEquals(
            ExecutionQuotas.class, ExecutionQuotas.QuotaUsage.class.getEnclosingClass());
      }

      @Test
      @DisplayName("should have getFuelUsage method")
      void shouldHaveGetFuelUsageMethod() throws NoSuchMethodException {
        Method method = ExecutionQuotas.QuotaUsage.class.getMethod("getFuelUsage");
        assertEquals(double.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getMemoryUsage method")
      void shouldHaveGetMemoryUsageMethod() throws NoSuchMethodException {
        Method method = ExecutionQuotas.QuotaUsage.class.getMethod("getMemoryUsage");
        assertEquals(double.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getTimeUsage method")
      void shouldHaveGetTimeUsageMethod() throws NoSuchMethodException {
        Method method = ExecutionQuotas.QuotaUsage.class.getMethod("getTimeUsage");
        assertEquals(double.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getInstructionUsage method")
      void shouldHaveGetInstructionUsageMethod() throws NoSuchMethodException {
        Method method = ExecutionQuotas.QuotaUsage.class.getMethod("getInstructionUsage");
        assertEquals(double.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getMaxUsage method")
      void shouldHaveGetMaxUsageMethod() throws NoSuchMethodException {
        Method method = ExecutionQuotas.QuotaUsage.class.getMethod("getMaxUsage");
        assertEquals(double.class, method.getReturnType());
      }
    }
  }

  // ============================================
  // ExecutionRequest Tests
  // ============================================

  @Nested
  @DisplayName("ExecutionRequest Interface Tests")
  class ExecutionRequestTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ExecutionRequest.class.isInterface(), "ExecutionRequest should be an interface");
    }

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getId");
      assertEquals(String.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getFunctionName method")
    void shouldHaveGetFunctionNameMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getFunctionName");
      assertEquals(String.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getArguments method")
    void shouldHaveGetArgumentsMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getArguments");
      assertEquals(Object[].class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getContext method")
    void shouldHaveGetContextMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getContext");
      assertEquals(ExecutionContext.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getQuotas method")
    void shouldHaveGetQuotasMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getQuotas");
      assertEquals(ExecutionQuotas.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getPolicy method")
    void shouldHaveGetPolicyMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getPolicy");
      assertEquals(ExecutionPolicy.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getPriority method")
    void shouldHaveGetPriorityMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getPriority");
      assertEquals(ExecutionRequest.RequestPriority.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getTimeout method")
    void shouldHaveGetTimeoutMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getTimeout");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getMetadata method")
    void shouldHaveGetMetadataMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getMetadata");
      assertEquals(Map.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getTimestamp method")
    void shouldHaveGetTimestampMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getTimestamp");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getCallerInfo method")
    void shouldHaveGetCallerInfoMethod() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getCallerInfo");
      assertEquals(ExecutionRequest.CallerInfo.class, method.getReturnType());
    }

    @Nested
    @DisplayName("RequestPriority Enum Tests")
    class RequestPriorityTests {

      @Test
      @DisplayName("should have 4 priority levels")
      void shouldHaveFourPriorityLevels() {
        assertEquals(4, ExecutionRequest.RequestPriority.values().length);
      }

      @Test
      @DisplayName("should have LOW priority")
      void shouldHaveLowPriority() {
        assertNotNull(ExecutionRequest.RequestPriority.LOW);
        assertEquals(0, ExecutionRequest.RequestPriority.LOW.ordinal());
      }

      @Test
      @DisplayName("should have NORMAL priority")
      void shouldHaveNormalPriority() {
        assertNotNull(ExecutionRequest.RequestPriority.NORMAL);
        assertEquals(1, ExecutionRequest.RequestPriority.NORMAL.ordinal());
      }

      @Test
      @DisplayName("should have HIGH priority")
      void shouldHaveHighPriority() {
        assertNotNull(ExecutionRequest.RequestPriority.HIGH);
        assertEquals(2, ExecutionRequest.RequestPriority.HIGH.ordinal());
      }

      @Test
      @DisplayName("should have CRITICAL priority")
      void shouldHaveCriticalPriority() {
        assertNotNull(ExecutionRequest.RequestPriority.CRITICAL);
        assertEquals(3, ExecutionRequest.RequestPriority.CRITICAL.ordinal());
      }

      @Test
      @DisplayName("should be comparable by severity")
      void shouldBeComparableBySeverity() {
        assertTrue(
            ExecutionRequest.RequestPriority.LOW.compareTo(
                    ExecutionRequest.RequestPriority.CRITICAL)
                < 0);
        assertTrue(
            ExecutionRequest.RequestPriority.HIGH.compareTo(ExecutionRequest.RequestPriority.NORMAL)
                > 0);
      }

      @Test
      @DisplayName("should support valueOf")
      void shouldSupportValueOf() {
        assertEquals(
            ExecutionRequest.RequestPriority.LOW,
            ExecutionRequest.RequestPriority.valueOf("LOW"));
        assertEquals(
            ExecutionRequest.RequestPriority.CRITICAL,
            ExecutionRequest.RequestPriority.valueOf("CRITICAL"));
      }
    }

    @Nested
    @DisplayName("CallerInfo Inner Interface Tests")
    class CallerInfoTests {

      @Test
      @DisplayName("should be an inner interface")
      void shouldBeInnerInterface() {
        assertTrue(ExecutionRequest.CallerInfo.class.isInterface());
      }

      @Test
      @DisplayName("should have getId method")
      void shouldHaveGetIdMethod() throws NoSuchMethodException {
        Method method = ExecutionRequest.CallerInfo.class.getMethod("getId");
        assertEquals(String.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getType method")
      void shouldHaveGetTypeMethod() throws NoSuchMethodException {
        Method method = ExecutionRequest.CallerInfo.class.getMethod("getType");
        assertEquals(String.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getPermissions method")
      void shouldHaveGetPermissionsMethod() throws NoSuchMethodException {
        Method method = ExecutionRequest.CallerInfo.class.getMethod("getPermissions");
        assertEquals(Set.class, method.getReturnType());
      }
    }
  }

  // ============================================
  // ExecutionState Tests
  // ============================================

  @Nested
  @DisplayName("ExecutionState Interface Tests")
  class ExecutionStateTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ExecutionState.class.isInterface(), "ExecutionState should be an interface");
    }

    @Test
    @DisplayName("should have getExecutionId method")
    void shouldHaveGetExecutionIdMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getExecutionId");
      assertEquals(String.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getStatus method")
    void shouldHaveGetStatusMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getStatus");
      assertEquals(ExecutionStatus.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getPhase method")
    void shouldHaveGetPhaseMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getPhase");
      assertEquals(ExecutionState.ExecutionPhase.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getStartTime method")
    void shouldHaveGetStartTimeMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getStartTime");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getCurrentTime method")
    void shouldHaveGetCurrentTimeMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getCurrentTime");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getDuration method")
    void shouldHaveGetDurationMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getDuration");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getContext method")
    void shouldHaveGetContextMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getContext");
      assertEquals(ExecutionContext.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getQuotas method")
    void shouldHaveGetQuotasMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getQuotas");
      assertEquals(ExecutionQuotas.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getStatistics");
      assertEquals(ExecutionStatistics.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getStackTrace method")
    void shouldHaveGetStackTraceMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getStackTrace");
      assertEquals(List.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getCurrentFunction method")
    void shouldHaveGetCurrentFunctionMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getCurrentFunction");
      assertEquals(ExecutionState.CurrentFunctionInfo.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getMetadata method")
    void shouldHaveGetMetadataMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getMetadata");
      assertEquals(Map.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getWarnings method")
    void shouldHaveGetWarningsMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getWarnings");
      assertEquals(List.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getError method")
    void shouldHaveGetErrorMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getError");
      assertEquals(Throwable.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have isActive method")
    void shouldHaveIsActiveMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("isActive");
      assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have isSuspended method")
    void shouldHaveIsSuspendedMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("isSuspended");
      assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have isCompleted method")
    void shouldHaveIsCompletedMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("isCompleted");
      assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have hasFailed method")
    void shouldHaveHasFailedMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("hasFailed");
      assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getSuspensionReason method")
    void shouldHaveGetSuspensionReasonMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getSuspensionReason");
      assertEquals(ExecutionState.SuspensionReason.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getTerminationReason method")
    void shouldHaveGetTerminationReasonMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getTerminationReason");
      assertEquals(ExecutionState.TerminationReason.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getProgress method")
    void shouldHaveGetProgressMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getProgress");
      assertEquals(ExecutionState.ProgressInfo.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have createSnapshot method")
    void shouldHaveCreateSnapshotMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("createSnapshot");
      assertEquals(ExecutionState.ExecutionStateSnapshot.class, method.getReturnType());
    }

    @Nested
    @DisplayName("ExecutionPhase Enum Tests")
    class ExecutionPhaseTests {

      @Test
      @DisplayName("should have 9 execution phases")
      void shouldHaveNineExecutionPhases() {
        assertEquals(9, ExecutionState.ExecutionPhase.values().length);
      }

      @Test
      @DisplayName("should have all expected phases")
      void shouldHaveAllExpectedPhases() {
        Set<String> expectedPhases =
            new HashSet<>(
                Arrays.asList(
                    "INITIALIZATION",
                    "PRE_EXECUTION",
                    "FUNCTION_EXECUTION",
                    "HOST_FUNCTION_CALL",
                    "MEMORY_OPERATION",
                    "EXCEPTION_HANDLING",
                    "POST_EXECUTION",
                    "CLEANUP",
                    "FINALIZATION"));

        Set<String> actualPhases = new HashSet<>();
        for (ExecutionState.ExecutionPhase phase : ExecutionState.ExecutionPhase.values()) {
          actualPhases.add(phase.name());
        }

        assertEquals(expectedPhases, actualPhases);
      }

      @Test
      @DisplayName("should have INITIALIZATION as first phase")
      void shouldHaveInitializationAsFirstPhase() {
        assertEquals(0, ExecutionState.ExecutionPhase.INITIALIZATION.ordinal());
      }

      @Test
      @DisplayName("should have FINALIZATION as last phase")
      void shouldHaveFinalizationAsLastPhase() {
        assertEquals(8, ExecutionState.ExecutionPhase.FINALIZATION.ordinal());
      }
    }

    @Nested
    @DisplayName("SuspensionReason Enum Tests")
    class SuspensionReasonTests {

      @Test
      @DisplayName("should have 6 suspension reasons")
      void shouldHaveSixSuspensionReasons() {
        assertEquals(6, ExecutionState.SuspensionReason.values().length);
      }

      @Test
      @DisplayName("should have USER_REQUESTED reason")
      void shouldHaveUserRequestedReason() {
        assertNotNull(ExecutionState.SuspensionReason.USER_REQUESTED);
      }

      @Test
      @DisplayName("should have RESOURCE_LIMIT reason")
      void shouldHaveResourceLimitReason() {
        assertNotNull(ExecutionState.SuspensionReason.RESOURCE_LIMIT);
      }

      @Test
      @DisplayName("should have BREAKPOINT reason")
      void shouldHaveBreakpointReason() {
        assertNotNull(ExecutionState.SuspensionReason.BREAKPOINT);
      }

      @Test
      @DisplayName("should have DEBUGGING reason")
      void shouldHaveDebuggingReason() {
        assertNotNull(ExecutionState.SuspensionReason.DEBUGGING);
      }

      @Test
      @DisplayName("should have SYSTEM_OVERLOAD reason")
      void shouldHaveSystemOverloadReason() {
        assertNotNull(ExecutionState.SuspensionReason.SYSTEM_OVERLOAD);
      }

      @Test
      @DisplayName("should have WAITING_FOR_RESOURCE reason")
      void shouldHaveWaitingForResourceReason() {
        assertNotNull(ExecutionState.SuspensionReason.WAITING_FOR_RESOURCE);
      }
    }

    @Nested
    @DisplayName("TerminationReason Enum Tests")
    class TerminationReasonTests {

      @Test
      @DisplayName("should have 8 termination reasons")
      void shouldHaveEightTerminationReasons() {
        assertEquals(8, ExecutionState.TerminationReason.values().length);
      }

      @Test
      @DisplayName("should have all expected termination reasons")
      void shouldHaveAllExpectedTerminationReasons() {
        Set<String> expectedReasons =
            new HashSet<>(
                Arrays.asList(
                    "COMPLETED",
                    "USER_TERMINATED",
                    "TIMEOUT",
                    "MEMORY_LIMIT",
                    "FUEL_EXHAUSTED",
                    "EXCEPTION",
                    "SYSTEM_ERROR",
                    "SECURITY_VIOLATION"));

        Set<String> actualReasons = new HashSet<>();
        for (ExecutionState.TerminationReason reason : ExecutionState.TerminationReason.values()) {
          actualReasons.add(reason.name());
        }

        assertEquals(expectedReasons, actualReasons);
      }

      @Test
      @DisplayName("should have COMPLETED as first reason")
      void shouldHaveCompletedAsFirstReason() {
        assertEquals(0, ExecutionState.TerminationReason.COMPLETED.ordinal());
      }
    }

    @Nested
    @DisplayName("WarningType Enum Tests")
    class WarningTypeTests {

      @Test
      @DisplayName("should have 7 warning types")
      void shouldHaveSevenWarningTypes() {
        assertEquals(7, ExecutionState.WarningType.values().length);
      }

      @Test
      @DisplayName("should have PERFORMANCE warning type")
      void shouldHavePerformanceWarningType() {
        assertNotNull(ExecutionState.WarningType.PERFORMANCE);
      }

      @Test
      @DisplayName("should have MEMORY_USAGE warning type")
      void shouldHaveMemoryUsageWarningType() {
        assertNotNull(ExecutionState.WarningType.MEMORY_USAGE);
      }

      @Test
      @DisplayName("should have CUSTOM warning type")
      void shouldHaveCustomWarningType() {
        assertNotNull(ExecutionState.WarningType.CUSTOM);
      }
    }

    @Nested
    @DisplayName("WarningSeverity Enum Tests")
    class WarningSeverityTests {

      @Test
      @DisplayName("should have 4 severity levels")
      void shouldHaveFourSeverityLevels() {
        assertEquals(4, ExecutionState.WarningSeverity.values().length);
      }

      @Test
      @DisplayName("should have INFO as lowest severity")
      void shouldHaveInfoAsLowestSeverity() {
        assertEquals(0, ExecutionState.WarningSeverity.INFO.ordinal());
      }

      @Test
      @DisplayName("should have HIGH as highest severity")
      void shouldHaveHighAsHighestSeverity() {
        assertEquals(3, ExecutionState.WarningSeverity.HIGH.ordinal());
      }

      @Test
      @DisplayName("should be comparable by severity")
      void shouldBeComparableBySeverity() {
        assertTrue(
            ExecutionState.WarningSeverity.INFO.compareTo(ExecutionState.WarningSeverity.HIGH) < 0);
        assertTrue(
            ExecutionState.WarningSeverity.MEDIUM.compareTo(ExecutionState.WarningSeverity.LOW)
                > 0);
      }
    }

    @Nested
    @DisplayName("CurrentFunctionInfo Inner Interface Tests")
    class CurrentFunctionInfoTests {

      @Test
      @DisplayName("should be an inner interface")
      void shouldBeInnerInterface() {
        assertTrue(ExecutionState.CurrentFunctionInfo.class.isInterface());
      }

      @Test
      @DisplayName("should have getName method")
      void shouldHaveGetNameMethod() throws NoSuchMethodException {
        Method method = ExecutionState.CurrentFunctionInfo.class.getMethod("getName");
        assertEquals(String.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getModuleName method")
      void shouldHaveGetModuleNameMethod() throws NoSuchMethodException {
        Method method = ExecutionState.CurrentFunctionInfo.class.getMethod("getModuleName");
        assertEquals(String.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getIndex method")
      void shouldHaveGetIndexMethod() throws NoSuchMethodException {
        Method method = ExecutionState.CurrentFunctionInfo.class.getMethod("getIndex");
        assertEquals(int.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getInstructionPointer method")
      void shouldHaveGetInstructionPointerMethod() throws NoSuchMethodException {
        Method method = ExecutionState.CurrentFunctionInfo.class.getMethod("getInstructionPointer");
        assertEquals(int.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getParameters method")
      void shouldHaveGetParametersMethod() throws NoSuchMethodException {
        Method method = ExecutionState.CurrentFunctionInfo.class.getMethod("getParameters");
        assertEquals(Object[].class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getLocalVariables method")
      void shouldHaveGetLocalVariablesMethod() throws NoSuchMethodException {
        Method method = ExecutionState.CurrentFunctionInfo.class.getMethod("getLocalVariables");
        assertEquals(Map.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getEntryTime method")
      void shouldHaveGetEntryTimeMethod() throws NoSuchMethodException {
        Method method = ExecutionState.CurrentFunctionInfo.class.getMethod("getEntryTime");
        assertEquals(long.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getExecutionDuration method")
      void shouldHaveGetExecutionDurationMethod() throws NoSuchMethodException {
        Method method = ExecutionState.CurrentFunctionInfo.class.getMethod("getExecutionDuration");
        assertEquals(long.class, method.getReturnType());
      }
    }

    @Nested
    @DisplayName("ExecutionWarning Inner Interface Tests")
    class ExecutionWarningTests {

      @Test
      @DisplayName("should be an inner interface")
      void shouldBeInnerInterface() {
        assertTrue(ExecutionState.ExecutionWarning.class.isInterface());
      }

      @Test
      @DisplayName("should have getType method")
      void shouldHaveGetTypeMethod() throws NoSuchMethodException {
        Method method = ExecutionState.ExecutionWarning.class.getMethod("getType");
        assertEquals(ExecutionState.WarningType.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getMessage method")
      void shouldHaveGetMessageMethod() throws NoSuchMethodException {
        Method method = ExecutionState.ExecutionWarning.class.getMethod("getMessage");
        assertEquals(String.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getTimestamp method")
      void shouldHaveGetTimestampMethod() throws NoSuchMethodException {
        Method method = ExecutionState.ExecutionWarning.class.getMethod("getTimestamp");
        assertEquals(long.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getContext method")
      void shouldHaveGetContextMethod() throws NoSuchMethodException {
        Method method = ExecutionState.ExecutionWarning.class.getMethod("getContext");
        assertEquals(String.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getSeverity method")
      void shouldHaveGetSeverityMethod() throws NoSuchMethodException {
        Method method = ExecutionState.ExecutionWarning.class.getMethod("getSeverity");
        assertEquals(ExecutionState.WarningSeverity.class, method.getReturnType());
      }
    }

    @Nested
    @DisplayName("ProgressInfo Inner Interface Tests")
    class ProgressInfoTests {

      @Test
      @DisplayName("should have getCompletionPercentage method")
      void shouldHaveGetCompletionPercentageMethod() throws NoSuchMethodException {
        Method method = ExecutionState.ProgressInfo.class.getMethod("getCompletionPercentage");
        assertEquals(double.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getEstimatedTimeRemaining method")
      void shouldHaveGetEstimatedTimeRemainingMethod() throws NoSuchMethodException {
        Method method = ExecutionState.ProgressInfo.class.getMethod("getEstimatedTimeRemaining");
        assertEquals(long.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getMilestones method")
      void shouldHaveGetMilestonesMethod() throws NoSuchMethodException {
        Method method = ExecutionState.ProgressInfo.class.getMethod("getMilestones");
        assertEquals(List.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getCurrentMilestone method")
      void shouldHaveGetCurrentMilestoneMethod() throws NoSuchMethodException {
        Method method = ExecutionState.ProgressInfo.class.getMethod("getCurrentMilestone");
        assertEquals(ExecutionState.ProgressMilestone.class, method.getReturnType());
      }
    }

    @Nested
    @DisplayName("ProgressMilestone Inner Interface Tests")
    class ProgressMilestoneTests {

      @Test
      @DisplayName("should have getName method")
      void shouldHaveGetNameMethod() throws NoSuchMethodException {
        Method method = ExecutionState.ProgressMilestone.class.getMethod("getName");
        assertEquals(String.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getDescription method")
      void shouldHaveGetDescriptionMethod() throws NoSuchMethodException {
        Method method = ExecutionState.ProgressMilestone.class.getMethod("getDescription");
        assertEquals(String.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getCompletionPercentage method")
      void shouldHaveGetCompletionPercentageMethod() throws NoSuchMethodException {
        Method method = ExecutionState.ProgressMilestone.class.getMethod("getCompletionPercentage");
        assertEquals(double.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getTimestamp method")
      void shouldHaveGetTimestampMethod() throws NoSuchMethodException {
        Method method = ExecutionState.ProgressMilestone.class.getMethod("getTimestamp");
        assertEquals(long.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have isCompleted method")
      void shouldHaveIsCompletedMethod() throws NoSuchMethodException {
        Method method = ExecutionState.ProgressMilestone.class.getMethod("isCompleted");
        assertEquals(boolean.class, method.getReturnType());
      }
    }

    @Nested
    @DisplayName("StackFrame Inner Interface Tests")
    class StackFrameTests {

      @Test
      @DisplayName("should have getFunctionName method")
      void shouldHaveGetFunctionNameMethod() throws NoSuchMethodException {
        Method method = ExecutionState.StackFrame.class.getMethod("getFunctionName");
        assertEquals(String.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getModuleName method")
      void shouldHaveGetModuleNameMethod() throws NoSuchMethodException {
        Method method = ExecutionState.StackFrame.class.getMethod("getModuleName");
        assertEquals(String.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getInstructionPointer method")
      void shouldHaveGetInstructionPointerMethod() throws NoSuchMethodException {
        Method method = ExecutionState.StackFrame.class.getMethod("getInstructionPointer");
        assertEquals(int.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getDepth method")
      void shouldHaveGetDepthMethod() throws NoSuchMethodException {
        Method method = ExecutionState.StackFrame.class.getMethod("getDepth");
        assertEquals(int.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getLocalVariables method")
      void shouldHaveGetLocalVariablesMethod() throws NoSuchMethodException {
        Method method = ExecutionState.StackFrame.class.getMethod("getLocalVariables");
        assertEquals(Map.class, method.getReturnType());
      }
    }

    @Nested
    @DisplayName("ExecutionStateSnapshot Inner Interface Tests")
    class ExecutionStateSnapshotTests {

      @Test
      @DisplayName("should have getSnapshotTime method")
      void shouldHaveGetSnapshotTimeMethod() throws NoSuchMethodException {
        Method method = ExecutionState.ExecutionStateSnapshot.class.getMethod("getSnapshotTime");
        assertEquals(long.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getState method")
      void shouldHaveGetStateMethod() throws NoSuchMethodException {
        Method method = ExecutionState.ExecutionStateSnapshot.class.getMethod("getState");
        assertEquals(ExecutionState.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getMetadata method")
      void shouldHaveGetMetadataMethod() throws NoSuchMethodException {
        Method method = ExecutionState.ExecutionStateSnapshot.class.getMethod("getMetadata");
        assertEquals(Map.class, method.getReturnType());
      }
    }
  }

  // ============================================
  // ExecutionStatus Tests
  // ============================================

  @Nested
  @DisplayName("ExecutionStatus Enum Tests")
  class ExecutionStatusTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(ExecutionStatus.class.isEnum());
    }

    @Test
    @DisplayName("should have expected number of statuses")
    void shouldHaveExpectedNumberOfStatuses() {
      assertTrue(ExecutionStatus.values().length >= 4, "Should have at least 4 status values");
    }

    @Test
    @DisplayName("should support valueOf for all values")
    void shouldSupportValueOfForAllValues() {
      for (ExecutionStatus status : ExecutionStatus.values()) {
        assertEquals(status, ExecutionStatus.valueOf(status.name()));
      }
    }

    @Test
    @DisplayName("should throw for invalid valueOf")
    void shouldThrowForInvalidValueOf() {
      assertThrows(IllegalArgumentException.class, () -> ExecutionStatus.valueOf("INVALID_STATUS"));
    }
  }

  // ============================================
  // Cross-Interface Relationship Tests
  // ============================================

  @Nested
  @DisplayName("Interface Relationship Tests")
  class InterfaceRelationshipTests {

    @Test
    @DisplayName("ExecutionRequest should reference ExecutionContext")
    void executionRequestShouldReferenceExecutionContext() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getContext");
      assertEquals(ExecutionContext.class, method.getReturnType());
    }

    @Test
    @DisplayName("ExecutionRequest should reference ExecutionQuotas")
    void executionRequestShouldReferenceExecutionQuotas() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getQuotas");
      assertEquals(ExecutionQuotas.class, method.getReturnType());
    }

    @Test
    @DisplayName("ExecutionRequest should reference ExecutionPolicy")
    void executionRequestShouldReferenceExecutionPolicy() throws NoSuchMethodException {
      Method method = ExecutionRequest.class.getMethod("getPolicy");
      assertEquals(ExecutionPolicy.class, method.getReturnType());
    }

    @Test
    @DisplayName("ExecutionState should reference ExecutionContext")
    void executionStateShouldReferenceExecutionContext() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getContext");
      assertEquals(ExecutionContext.class, method.getReturnType());
    }

    @Test
    @DisplayName("ExecutionState should reference ExecutionQuotas")
    void executionStateShouldReferenceExecutionQuotas() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getQuotas");
      assertEquals(ExecutionQuotas.class, method.getReturnType());
    }

    @Test
    @DisplayName("ExecutionState should reference ExecutionStatistics")
    void executionStateShouldReferenceExecutionStatistics() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getStatistics");
      assertEquals(ExecutionStatistics.class, method.getReturnType());
    }

    @Test
    @DisplayName("ExecutionController should manage ExecutionQuotas")
    void executionControllerShouldManageExecutionQuotas() throws NoSuchMethodException {
      assertNotNull(ExecutionController.class.getMethod("getQuotas"));
      assertNotNull(ExecutionController.class.getMethod("setQuotas", ExecutionQuotas.class));
    }

    @Test
    @DisplayName("ExecutionController should manage ExecutionPolicy")
    void executionControllerShouldManageExecutionPolicy() throws NoSuchMethodException {
      assertNotNull(ExecutionController.class.getMethod("getPolicy"));
      assertNotNull(ExecutionController.class.getMethod("setPolicy", ExecutionPolicy.class));
    }
  }

  // ============================================
  // Method Count Verification Tests
  // ============================================

  @Nested
  @DisplayName("Interface Completeness Tests")
  class InterfaceCompletenessTests {

    @Test
    @DisplayName("ExecutionContext should have at least 8 methods")
    void executionContextShouldHaveAtLeastEightMethods() {
      int methodCount = countPublicAbstractMethods(ExecutionContext.class);
      assertTrue(
          methodCount >= 8,
          "ExecutionContext should have at least 8 methods, found: " + methodCount);
    }

    @Test
    @DisplayName("ExecutionPolicy should have at least 9 methods")
    void executionPolicyShouldHaveAtLeastNineMethods() {
      int methodCount = countPublicAbstractMethods(ExecutionPolicy.class);
      assertTrue(
          methodCount >= 9,
          "ExecutionPolicy should have at least 9 methods, found: " + methodCount);
    }

    @Test
    @DisplayName("ExecutionQuotas should have at least 14 methods")
    void executionQuotasShouldHaveAtLeastFourteenMethods() {
      int methodCount = countPublicAbstractMethods(ExecutionQuotas.class);
      assertTrue(
          methodCount >= 14,
          "ExecutionQuotas should have at least 14 methods, found: " + methodCount);
    }

    @Test
    @DisplayName("ExecutionRequest should have at least 11 methods")
    void executionRequestShouldHaveAtLeastElevenMethods() {
      int methodCount = countPublicAbstractMethods(ExecutionRequest.class);
      assertTrue(
          methodCount >= 11,
          "ExecutionRequest should have at least 11 methods, found: " + methodCount);
    }

    @Test
    @DisplayName("ExecutionState should have at least 18 methods")
    void executionStateShouldHaveAtLeastEighteenMethods() {
      int methodCount = countPublicAbstractMethods(ExecutionState.class);
      assertTrue(
          methodCount >= 18,
          "ExecutionState should have at least 18 methods, found: " + methodCount);
    }

    private int countPublicAbstractMethods(final Class<?> clazz) {
      int count = 0;
      for (Method method : clazz.getDeclaredMethods()) {
        if (Modifier.isPublic(method.getModifiers()) && Modifier.isAbstract(method.getModifiers())) {
          count++;
        }
      }
      return count;
    }
  }

  // ============================================
  // ExecutionStatistics Interface Tests
  // ============================================

  @Nested
  @DisplayName("ExecutionStatistics Interface Tests")
  class ExecutionStatisticsTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ExecutionStatistics.class.isInterface(), "ExecutionStatistics should be an interface");
    }

    @Test
    @DisplayName("should have getExecutionTime method")
    void shouldHaveGetExecutionTimeMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getExecutionTime");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getInstructionCount method")
    void shouldHaveGetInstructionCountMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getInstructionCount");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getFunctionCallCount method")
    void shouldHaveGetFunctionCallCountMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getFunctionCallCount");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getMemoryAllocations method")
    void shouldHaveGetMemoryAllocationsMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getMemoryAllocations");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getPeakMemoryUsage method")
    void shouldHaveGetPeakMemoryUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getPeakMemoryUsage");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getCurrentMemoryUsage method")
    void shouldHaveGetCurrentMemoryUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getCurrentMemoryUsage");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getInstructionsPerSecond method")
    void shouldHaveGetInstructionsPerSecondMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getInstructionsPerSecond");
      assertEquals(double.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getFunctionCallsPerSecond method")
    void shouldHaveGetFunctionCallsPerSecondMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getFunctionCallsPerSecond");
      assertEquals(double.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getCpuUsage method")
    void shouldHaveGetCpuUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getCpuUsage");
      assertEquals(ExecutionStatistics.CpuUsage.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getMemoryUsage method")
    void shouldHaveGetMemoryUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getMemoryUsage");
      assertEquals(ExecutionStatistics.MemoryUsage.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getPerformanceMetrics method")
    void shouldHaveGetPerformanceMetricsMethod() throws NoSuchMethodException {
      Method method = ExecutionStatistics.class.getMethod("getPerformanceMetrics");
      assertEquals(ExecutionStatistics.PerformanceMetrics.class, method.getReturnType());
    }

    @Nested
    @DisplayName("CpuUsage Inner Interface Tests")
    class CpuUsageInnerInterfaceTests {

      @Test
      @DisplayName("should be an inner interface")
      void shouldBeInnerInterface() {
        assertTrue(
            ExecutionStatistics.CpuUsage.class.isInterface(),
            "CpuUsage should be an interface");
        assertEquals(
            ExecutionStatistics.class, ExecutionStatistics.CpuUsage.class.getEnclosingClass());
      }

      @Test
      @DisplayName("should have getTotalCpuTime method")
      void shouldHaveGetTotalCpuTimeMethod() throws NoSuchMethodException {
        Method method = ExecutionStatistics.CpuUsage.class.getMethod("getTotalCpuTime");
        assertEquals(long.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getUserCpuTime method")
      void shouldHaveGetUserCpuTimeMethod() throws NoSuchMethodException {
        Method method = ExecutionStatistics.CpuUsage.class.getMethod("getUserCpuTime");
        assertEquals(long.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getSystemCpuTime method")
      void shouldHaveGetSystemCpuTimeMethod() throws NoSuchMethodException {
        Method method = ExecutionStatistics.CpuUsage.class.getMethod("getSystemCpuTime");
        assertEquals(long.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getCpuUsagePercentage method")
      void shouldHaveGetCpuUsagePercentageMethod() throws NoSuchMethodException {
        Method method = ExecutionStatistics.CpuUsage.class.getMethod("getCpuUsagePercentage");
        assertEquals(double.class, method.getReturnType());
      }
    }

    @Nested
    @DisplayName("MemoryUsage Inner Interface Tests")
    class MemoryUsageInnerInterfaceTests {

      @Test
      @DisplayName("should be an inner interface")
      void shouldBeInnerInterface() {
        assertTrue(
            ExecutionStatistics.MemoryUsage.class.isInterface(),
            "MemoryUsage should be an interface");
        assertEquals(
            ExecutionStatistics.class, ExecutionStatistics.MemoryUsage.class.getEnclosingClass());
      }

      @Test
      @DisplayName("should have getHeapUsage method")
      void shouldHaveGetHeapUsageMethod() throws NoSuchMethodException {
        Method method = ExecutionStatistics.MemoryUsage.class.getMethod("getHeapUsage");
        assertEquals(long.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getStackUsage method")
      void shouldHaveGetStackUsageMethod() throws NoSuchMethodException {
        Method method = ExecutionStatistics.MemoryUsage.class.getMethod("getStackUsage");
        assertEquals(long.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getTotalAllocated method")
      void shouldHaveGetTotalAllocatedMethod() throws NoSuchMethodException {
        Method method = ExecutionStatistics.MemoryUsage.class.getMethod("getTotalAllocated");
        assertEquals(long.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getTotalFreed method")
      void shouldHaveGetTotalFreedMethod() throws NoSuchMethodException {
        Method method = ExecutionStatistics.MemoryUsage.class.getMethod("getTotalFreed");
        assertEquals(long.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getGcCount method")
      void shouldHaveGetGcCountMethod() throws NoSuchMethodException {
        Method method = ExecutionStatistics.MemoryUsage.class.getMethod("getGcCount");
        assertEquals(int.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getGcTime method")
      void shouldHaveGetGcTimeMethod() throws NoSuchMethodException {
        Method method = ExecutionStatistics.MemoryUsage.class.getMethod("getGcTime");
        assertEquals(long.class, method.getReturnType());
      }
    }

    @Nested
    @DisplayName("PerformanceMetrics Inner Interface Tests")
    class PerformanceMetricsInnerInterfaceTests {

      @Test
      @DisplayName("should be an inner interface")
      void shouldBeInnerInterface() {
        assertTrue(
            ExecutionStatistics.PerformanceMetrics.class.isInterface(),
            "PerformanceMetrics should be an interface");
        assertEquals(
            ExecutionStatistics.class,
            ExecutionStatistics.PerformanceMetrics.class.getEnclosingClass());
      }

      @Test
      @DisplayName("should have getThroughput method")
      void shouldHaveGetThroughputMethod() throws NoSuchMethodException {
        Method method = ExecutionStatistics.PerformanceMetrics.class.getMethod("getThroughput");
        assertEquals(double.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getAverageLatency method")
      void shouldHaveGetAverageLatencyMethod() throws NoSuchMethodException {
        Method method = ExecutionStatistics.PerformanceMetrics.class.getMethod("getAverageLatency");
        assertEquals(double.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getP95Latency method")
      void shouldHaveGetP95LatencyMethod() throws NoSuchMethodException {
        Method method = ExecutionStatistics.PerformanceMetrics.class.getMethod("getP95Latency");
        assertEquals(double.class, method.getReturnType());
      }

      @Test
      @DisplayName("should have getP99Latency method")
      void shouldHaveGetP99LatencyMethod() throws NoSuchMethodException {
        Method method = ExecutionStatistics.PerformanceMetrics.class.getMethod("getP99Latency");
        assertEquals(double.class, method.getReturnType());
      }
    }
  }

  // ============================================
  // FuelCallbackHandler Interface Tests
  // ============================================

  @Nested
  @DisplayName("FuelCallbackHandler Interface Tests")
  class FuelCallbackHandlerTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          FuelCallbackHandler.class.isInterface(), "FuelCallbackHandler should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(FuelCallbackHandler.class),
          "FuelCallbackHandler should extend AutoCloseable");
    }

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      Method method = FuelCallbackHandler.class.getMethod("getId");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getStoreId method")
    void shouldHaveGetStoreIdMethod() throws NoSuchMethodException {
      Method method = FuelCallbackHandler.class.getMethod("getStoreId");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have handleExhaustion method")
    void shouldHaveHandleExhaustionMethod() throws NoSuchMethodException {
      Method method =
          FuelCallbackHandler.class.getMethod("handleExhaustion", FuelExhaustionContext.class);
      assertEquals(FuelExhaustionResult.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getStats method")
    void shouldHaveGetStatsMethod() throws NoSuchMethodException {
      Method method = FuelCallbackHandler.class.getMethod("getStats");
      assertEquals(FuelCallbackStats.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have resetStats method")
    void shouldHaveResetStatsMethod() throws NoSuchMethodException {
      Method method = FuelCallbackHandler.class.getMethod("resetStats");
      assertEquals(void.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = FuelCallbackHandler.class.getMethod("close");
      assertEquals(void.class, method.getReturnType());
    }
  }

  // ============================================
  // ResourceLimiter Interface Tests
  // ============================================

  @Nested
  @DisplayName("ResourceLimiter Interface Tests")
  class ResourceLimiterTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ResourceLimiter.class.isInterface(), "ResourceLimiter should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(ResourceLimiter.class),
          "ResourceLimiter should extend AutoCloseable");
    }

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      Method method = ResourceLimiter.class.getMethod("getId");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      Method method = ResourceLimiter.class.getMethod("getConfig");
      assertEquals(ResourceLimiterConfig.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have allowMemoryGrow method")
    void shouldHaveAllowMemoryGrowMethod() throws NoSuchMethodException {
      Method method =
          ResourceLimiter.class.getMethod("allowMemoryGrow", long.class, long.class);
      assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have allowTableGrow method")
    void shouldHaveAllowTableGrowMethod() throws NoSuchMethodException {
      Method method =
          ResourceLimiter.class.getMethod("allowTableGrow", long.class, long.class);
      assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getStats method")
    void shouldHaveGetStatsMethod() throws NoSuchMethodException {
      Method method = ResourceLimiter.class.getMethod("getStats");
      assertEquals(ResourceLimiterStats.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have resetStats method")
    void shouldHaveResetStatsMethod() throws NoSuchMethodException {
      Method method = ResourceLimiter.class.getMethod("resetStats");
      assertEquals(void.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = ResourceLimiter.class.getMethod("close");
      assertEquals(void.class, method.getReturnType());
    }
  }

  // ============================================
  // ResourceLimiterAsync Interface Tests
  // ============================================

  @Nested
  @DisplayName("ResourceLimiterAsync Interface Tests")
  class ResourceLimiterAsyncTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ResourceLimiterAsync.class.isInterface(), "ResourceLimiterAsync should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(ResourceLimiterAsync.class),
          "ResourceLimiterAsync should extend AutoCloseable");
    }

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      Method method = ResourceLimiterAsync.class.getMethod("getId");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getConfigAsync method")
    void shouldHaveGetConfigAsyncMethod() throws NoSuchMethodException {
      Method method = ResourceLimiterAsync.class.getMethod("getConfigAsync");
      assertEquals(java.util.concurrent.CompletableFuture.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have allowMemoryGrowAsync method")
    void shouldHaveAllowMemoryGrowAsyncMethod() throws NoSuchMethodException {
      Method method =
          ResourceLimiterAsync.class.getMethod("allowMemoryGrowAsync", long.class, long.class);
      assertEquals(java.util.concurrent.CompletableFuture.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have allowTableGrowAsync method")
    void shouldHaveAllowTableGrowAsyncMethod() throws NoSuchMethodException {
      Method method =
          ResourceLimiterAsync.class.getMethod("allowTableGrowAsync", long.class, long.class);
      assertEquals(java.util.concurrent.CompletableFuture.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getStatsAsync method")
    void shouldHaveGetStatsAsyncMethod() throws NoSuchMethodException {
      Method method = ResourceLimiterAsync.class.getMethod("getStatsAsync");
      assertEquals(java.util.concurrent.CompletableFuture.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have resetStatsAsync method")
    void shouldHaveResetStatsAsyncMethod() throws NoSuchMethodException {
      Method method = ResourceLimiterAsync.class.getMethod("resetStatsAsync");
      assertEquals(java.util.concurrent.CompletableFuture.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have default allowMemoryGrow method")
    void shouldHaveDefaultAllowMemoryGrowMethod() throws NoSuchMethodException {
      Method method =
          ResourceLimiterAsync.class.getMethod("allowMemoryGrow", long.class, long.class);
      assertTrue(method.isDefault(), "allowMemoryGrow should be a default method");
      assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have default allowTableGrow method")
    void shouldHaveDefaultAllowTableGrowMethod() throws NoSuchMethodException {
      Method method =
          ResourceLimiterAsync.class.getMethod("allowTableGrow", long.class, long.class);
      assertTrue(method.isDefault(), "allowTableGrow should be a default method");
      assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have static fromSync method")
    void shouldHaveStaticFromSyncMethod() throws NoSuchMethodException {
      Method method = ResourceLimiterAsync.class.getMethod("fromSync", ResourceLimiter.class);
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "fromSync should be a static method");
      assertEquals(ResourceLimiterAsync.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = ResourceLimiterAsync.class.getMethod("close");
      assertEquals(void.class, method.getReturnType());
    }
  }

  // ============================================
  // SyncToAsyncLimiterAdapter Class Tests
  // ============================================

  @Nested
  @DisplayName("SyncToAsyncLimiterAdapter Class Tests")
  class SyncToAsyncLimiterAdapterTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertFalse(
          SyncToAsyncLimiterAdapter.class.isInterface(),
          "SyncToAsyncLimiterAdapter should not be an interface");
    }

    @Test
    @DisplayName("should implement ResourceLimiterAsync")
    void shouldImplementResourceLimiterAsync() {
      assertTrue(
          ResourceLimiterAsync.class.isAssignableFrom(SyncToAsyncLimiterAdapter.class),
          "SyncToAsyncLimiterAdapter should implement ResourceLimiterAsync");
    }

    @Test
    @DisplayName("should have package-private constructor taking ResourceLimiter")
    void shouldHaveConstructorTakingResourceLimiter() throws NoSuchMethodException {
      // Constructor is package-private, so use getDeclaredConstructor
      assertNotNull(SyncToAsyncLimiterAdapter.class.getDeclaredConstructor(ResourceLimiter.class));
    }

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      Method method = SyncToAsyncLimiterAdapter.class.getMethod("getId");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getConfigAsync method")
    void shouldHaveGetConfigAsyncMethod() throws NoSuchMethodException {
      Method method = SyncToAsyncLimiterAdapter.class.getMethod("getConfigAsync");
      assertEquals(java.util.concurrent.CompletableFuture.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have allowMemoryGrowAsync method")
    void shouldHaveAllowMemoryGrowAsyncMethod() throws NoSuchMethodException {
      Method method =
          SyncToAsyncLimiterAdapter.class.getMethod(
              "allowMemoryGrowAsync", long.class, long.class);
      assertEquals(java.util.concurrent.CompletableFuture.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have allowTableGrowAsync method")
    void shouldHaveAllowTableGrowAsyncMethod() throws NoSuchMethodException {
      Method method =
          SyncToAsyncLimiterAdapter.class.getMethod("allowTableGrowAsync", long.class, long.class);
      assertEquals(java.util.concurrent.CompletableFuture.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getStatsAsync method")
    void shouldHaveGetStatsAsyncMethod() throws NoSuchMethodException {
      Method method = SyncToAsyncLimiterAdapter.class.getMethod("getStatsAsync");
      assertEquals(java.util.concurrent.CompletableFuture.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have resetStatsAsync method")
    void shouldHaveResetStatsAsyncMethod() throws NoSuchMethodException {
      Method method = SyncToAsyncLimiterAdapter.class.getMethod("resetStatsAsync");
      assertEquals(java.util.concurrent.CompletableFuture.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = SyncToAsyncLimiterAdapter.class.getMethod("close");
      assertEquals(void.class, method.getReturnType());
    }
  }

  // ============================================
  // ExecutionResult Interface Tests
  // ============================================

  @Nested
  @DisplayName("ExecutionResult Interface Tests")
  class ExecutionResultTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ExecutionResult.class.isInterface(), "ExecutionResult should be an interface");
    }

    @Test
    @DisplayName("should have isSuccessful method")
    void shouldHaveIsSuccessfulMethod() throws NoSuchMethodException {
      Method method = ExecutionResult.class.getMethod("isSuccessful");
      assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getResult method")
    void shouldHaveGetResultMethod() throws NoSuchMethodException {
      Method method = ExecutionResult.class.getMethod("getResult");
      assertEquals(Object.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getError method")
    void shouldHaveGetErrorMethod() throws NoSuchMethodException {
      Method method = ExecutionResult.class.getMethod("getError");
      assertEquals(Throwable.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getDuration method")
    void shouldHaveGetDurationMethod() throws NoSuchMethodException {
      Method method = ExecutionResult.class.getMethod("getDuration");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getStatus method")
    void shouldHaveGetStatusMethod() throws NoSuchMethodException {
      Method method = ExecutionResult.class.getMethod("getStatus");
      assertEquals(ExecutionStatus.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      Method method = ExecutionResult.class.getMethod("getStatistics");
      assertEquals(ExecutionStatistics.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have TerminationReason enum")
    void shouldHaveTerminationReasonEnum() {
      Class<?>[] declaredClasses = ExecutionResult.class.getDeclaredClasses();
      boolean hasTerminationReason = false;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.isEnum() && clazz.getSimpleName().equals("TerminationReason")) {
          hasTerminationReason = true;
          break;
        }
      }
      assertTrue(hasTerminationReason, "ExecutionResult should have TerminationReason enum");
    }
  }

  // ============================================
  // ExecutionAnalytics Interface Tests
  // ============================================

  @Nested
  @DisplayName("ExecutionAnalytics Interface Tests")
  class ExecutionAnalyticsTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ExecutionAnalytics.class.isInterface(), "ExecutionAnalytics should be an interface");
    }

    @Test
    @DisplayName("should have getPerformanceAnalytics method")
    void shouldHaveGetPerformanceAnalyticsMethod() throws NoSuchMethodException {
      Method method = ExecutionAnalytics.class.getMethod("getPerformanceAnalytics");
      assertEquals(ExecutionAnalytics.PerformanceAnalytics.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getResourceAnalytics method")
    void shouldHaveGetResourceAnalyticsMethod() throws NoSuchMethodException {
      Method method = ExecutionAnalytics.class.getMethod("getResourceAnalytics");
      assertEquals(ExecutionAnalytics.ResourceAnalytics.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getErrorAnalytics method")
    void shouldHaveGetErrorAnalyticsMethod() throws NoSuchMethodException {
      Method method = ExecutionAnalytics.class.getMethod("getErrorAnalytics");
      assertEquals(ExecutionAnalytics.ErrorAnalytics.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getTrendAnalytics method")
    void shouldHaveGetTrendAnalyticsMethod() throws NoSuchMethodException {
      Method method = ExecutionAnalytics.class.getMethod("getTrendAnalytics");
      assertEquals(ExecutionAnalytics.TrendAnalytics.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have ExportFormat enum")
    void shouldHaveExportFormatEnum() {
      Class<?>[] declaredClasses = ExecutionAnalytics.class.getDeclaredClasses();
      boolean hasExportFormat = false;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.isEnum() && clazz.getSimpleName().equals("ExportFormat")) {
          hasExportFormat = true;
          break;
        }
      }
      assertTrue(hasExportFormat, "ExecutionAnalytics should have ExportFormat enum");
    }
  }

  // ============================================
  // TraceFilter Interface Tests
  // ============================================

  @Nested
  @DisplayName("TraceFilter Interface Tests")
  class TraceFilterTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(TraceFilter.class.isInterface(), "TraceFilter should be an interface");
    }

    @Test
    @DisplayName("should have matches method with TraceEvent parameter")
    void shouldHaveMatchesMethod() throws NoSuchMethodException {
      Method method = TraceFilter.class.getMethod("matches", TraceFilter.TraceEvent.class);
      assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      Method method = TraceFilter.class.getMethod("getName");
      assertEquals(String.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      Method method = TraceFilter.class.getMethod("getType");
      assertEquals(TraceFilter.FilterType.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have FilterType enum")
    void shouldHaveFilterTypeEnum() {
      Class<?>[] declaredClasses = TraceFilter.class.getDeclaredClasses();
      boolean hasFilterType = false;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.isEnum() && clazz.getSimpleName().equals("FilterType")) {
          hasFilterType = true;
          break;
        }
      }
      assertTrue(hasFilterType, "TraceFilter should have FilterType enum");
    }

    @Test
    @DisplayName("should have FilterAction enum")
    void shouldHaveFilterActionEnum() {
      Class<?>[] declaredClasses = TraceFilter.class.getDeclaredClasses();
      boolean hasFilterAction = false;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.isEnum() && clazz.getSimpleName().equals("FilterAction")) {
          hasFilterAction = true;
          break;
        }
      }
      assertTrue(hasFilterAction, "TraceFilter should have FilterAction enum");
    }
  }

  // ============================================
  // FuelAdjustment Final Class Tests
  // ============================================

  @Nested
  @DisplayName("FuelAdjustment Final Class Tests")
  class FuelAdjustmentTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(FuelAdjustment.class.getModifiers()),
          "FuelAdjustment should be a final class");
      assertFalse(FuelAdjustment.class.isInterface(), "FuelAdjustment should not be an interface");
    }

    @Test
    @DisplayName("should have constructor with long and String parameters")
    void shouldHaveConstructor() throws NoSuchMethodException {
      assertNotNull(FuelAdjustment.class.getConstructor(long.class, String.class));
    }

    @Test
    @DisplayName("should have getAmount method")
    void shouldHaveGetAmountMethod() throws NoSuchMethodException {
      Method method = FuelAdjustment.class.getMethod("getAmount");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("should have getReason method")
    void shouldHaveGetReasonMethod() throws NoSuchMethodException {
      Method method = FuelAdjustment.class.getMethod("getReason");
      assertEquals(String.class, method.getReturnType());
    }
  }
}
