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

import ai.tegmentum.wasmtime4j.ComponentResourceSharingManager.AlertSeverity;
import ai.tegmentum.wasmtime4j.ComponentResourceSharingManager.QuotaEnforcementPolicy;
import ai.tegmentum.wasmtime4j.ComponentResourceSharingManager.ResourceAllocationStatus;
import ai.tegmentum.wasmtime4j.ComponentResourceSharingManager.ResourceCommunicationType;
import ai.tegmentum.wasmtime4j.ComponentResourceSharingManager.ResourceIsolationLevel;
import ai.tegmentum.wasmtime4j.ComponentResourceSharingManager.ResourceMessageType;
import ai.tegmentum.wasmtime4j.ComponentResourceSharingManager.ResourceSharingPolicy;
import ai.tegmentum.wasmtime4j.ComponentResourceSharingManager.ResourceType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentResourceSharingManager} interface.
 *
 * <p>ComponentResourceSharingManager provides advanced resource sharing and isolation for
 * components.
 */
@DisplayName("ComponentResourceSharingManager Tests")
class ComponentResourceSharingManagerTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentResourceSharingManager.class.getModifiers()),
          "ComponentResourceSharingManager should be public");
      assertTrue(
          ComponentResourceSharingManager.class.isInterface(),
          "ComponentResourceSharingManager should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(ComponentResourceSharingManager.class),
          "ComponentResourceSharingManager should extend AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceSharingManager.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getConfiguration method")
    void shouldHaveGetConfigurationMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceSharingManager.class.getMethod("getConfiguration");
      assertNotNull(method, "getConfiguration method should exist");
    }

    @Test
    @DisplayName("should have createResourcePool method")
    void shouldHaveCreateResourcePoolMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod(
              "createResourcePool",
              String.class,
              ResourceType.class,
              ComponentResourceSharingManager.ResourcePoolConfig.class);
      assertNotNull(method, "createResourcePool method should exist");
    }

    @Test
    @DisplayName("should have getResourcePool method")
    void shouldHaveGetResourcePoolMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod("getResourcePool", String.class);
      assertNotNull(method, "getResourcePool method should exist");
    }

    @Test
    @DisplayName("should have removeResourcePool method")
    void shouldHaveRemoveResourcePoolMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod("removeResourcePool", String.class);
      assertNotNull(method, "removeResourcePool method should exist");
    }

    @Test
    @DisplayName("should have allocateResources method")
    void shouldHaveAllocateResourcesMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod(
              "allocateResources",
              ComponentSimple.class,
              String.class,
              ComponentResourceSharingManager.ResourceAllocationRequest.class);
      assertNotNull(method, "allocateResources method should exist");
    }

    @Test
    @DisplayName("should have deallocateResources method")
    void shouldHaveDeallocateResourcesMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod(
              "deallocateResources", ComponentResourceSharingManager.ResourceAllocation.class);
      assertNotNull(method, "deallocateResources method should exist");
    }

    @Test
    @DisplayName("should have start method")
    void shouldHaveStartMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceSharingManager.class.getMethod("start");
      assertNotNull(method, "start method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have stop method")
    void shouldHaveStopMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceSharingManager.class.getMethod("stop");
      assertNotNull(method, "stop method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceSharingManager.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
    }

    @Test
    @DisplayName("should have getActiveResourcePools method")
    void shouldHaveGetActiveResourcePoolsMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod("getActiveResourcePools");
      assertNotNull(method, "getActiveResourcePools method should exist");
    }
  }

  @Nested
  @DisplayName("ResourceType Enum Tests")
  class ResourceTypeEnumTests {

    @Test
    @DisplayName("should have all resource types")
    void shouldHaveAllResourceTypes() {
      final var types = ResourceType.values();
      assertEquals(8, types.length, "Should have 8 resource types");
    }

    @Test
    @DisplayName("should have MEMORY type")
    void shouldHaveMemoryType() {
      assertEquals(ResourceType.MEMORY, ResourceType.valueOf("MEMORY"));
    }

    @Test
    @DisplayName("should have CPU type")
    void shouldHaveCpuType() {
      assertEquals(ResourceType.CPU, ResourceType.valueOf("CPU"));
    }

    @Test
    @DisplayName("should have NETWORK type")
    void shouldHaveNetworkType() {
      assertEquals(ResourceType.NETWORK, ResourceType.valueOf("NETWORK"));
    }

    @Test
    @DisplayName("should have FILE_SYSTEM type")
    void shouldHaveFileSystemType() {
      assertEquals(ResourceType.FILE_SYSTEM, ResourceType.valueOf("FILE_SYSTEM"));
    }

    @Test
    @DisplayName("should have THREADS type")
    void shouldHaveThreadsType() {
      assertEquals(ResourceType.THREADS, ResourceType.valueOf("THREADS"));
    }

    @Test
    @DisplayName("should have SOCKETS type")
    void shouldHaveSocketsType() {
      assertEquals(ResourceType.SOCKETS, ResourceType.valueOf("SOCKETS"));
    }

    @Test
    @DisplayName("should have HANDLES type")
    void shouldHaveHandlesType() {
      assertEquals(ResourceType.HANDLES, ResourceType.valueOf("HANDLES"));
    }

    @Test
    @DisplayName("should have CUSTOM type")
    void shouldHaveCustomType() {
      assertEquals(ResourceType.CUSTOM, ResourceType.valueOf("CUSTOM"));
    }
  }

  @Nested
  @DisplayName("ResourceAllocationStatus Enum Tests")
  class ResourceAllocationStatusEnumTests {

    @Test
    @DisplayName("should have all allocation statuses")
    void shouldHaveAllAllocationStatuses() {
      final var statuses = ResourceAllocationStatus.values();
      assertEquals(4, statuses.length, "Should have 4 allocation statuses");
    }

    @Test
    @DisplayName("should have ACTIVE status")
    void shouldHaveActiveStatus() {
      assertEquals(ResourceAllocationStatus.ACTIVE, ResourceAllocationStatus.valueOf("ACTIVE"));
    }

    @Test
    @DisplayName("should have EXPIRED status")
    void shouldHaveExpiredStatus() {
      assertEquals(ResourceAllocationStatus.EXPIRED, ResourceAllocationStatus.valueOf("EXPIRED"));
    }

    @Test
    @DisplayName("should have RELEASED status")
    void shouldHaveReleasedStatus() {
      assertEquals(ResourceAllocationStatus.RELEASED, ResourceAllocationStatus.valueOf("RELEASED"));
    }

    @Test
    @DisplayName("should have FAILED status")
    void shouldHaveFailedStatus() {
      assertEquals(ResourceAllocationStatus.FAILED, ResourceAllocationStatus.valueOf("FAILED"));
    }
  }

  @Nested
  @DisplayName("ResourceIsolationLevel Enum Tests")
  class ResourceIsolationLevelEnumTests {

    @Test
    @DisplayName("should have all isolation levels")
    void shouldHaveAllIsolationLevels() {
      final var levels = ResourceIsolationLevel.values();
      assertEquals(5, levels.length, "Should have 5 isolation levels");
    }

    @Test
    @DisplayName("should have NONE level")
    void shouldHaveNoneLevel() {
      assertEquals(ResourceIsolationLevel.NONE, ResourceIsolationLevel.valueOf("NONE"));
    }

    @Test
    @DisplayName("should have BASIC level")
    void shouldHaveBasicLevel() {
      assertEquals(ResourceIsolationLevel.BASIC, ResourceIsolationLevel.valueOf("BASIC"));
    }

    @Test
    @DisplayName("should have MODERATE level")
    void shouldHaveModerateLevel() {
      assertEquals(ResourceIsolationLevel.MODERATE, ResourceIsolationLevel.valueOf("MODERATE"));
    }

    @Test
    @DisplayName("should have STRONG level")
    void shouldHaveStrongLevel() {
      assertEquals(ResourceIsolationLevel.STRONG, ResourceIsolationLevel.valueOf("STRONG"));
    }

    @Test
    @DisplayName("should have COMPLETE level")
    void shouldHaveCompleteLevel() {
      assertEquals(ResourceIsolationLevel.COMPLETE, ResourceIsolationLevel.valueOf("COMPLETE"));
    }
  }

  @Nested
  @DisplayName("QuotaEnforcementPolicy Enum Tests")
  class QuotaEnforcementPolicyEnumTests {

    @Test
    @DisplayName("should have all enforcement policies")
    void shouldHaveAllEnforcementPolicies() {
      final var policies = QuotaEnforcementPolicy.values();
      assertEquals(4, policies.length, "Should have 4 enforcement policies");
    }

    @Test
    @DisplayName("should have WARN_ONLY policy")
    void shouldHaveWarnOnlyPolicy() {
      assertEquals(QuotaEnforcementPolicy.WARN_ONLY, QuotaEnforcementPolicy.valueOf("WARN_ONLY"));
    }

    @Test
    @DisplayName("should have THROTTLE policy")
    void shouldHaveThrottlePolicy() {
      assertEquals(QuotaEnforcementPolicy.THROTTLE, QuotaEnforcementPolicy.valueOf("THROTTLE"));
    }

    @Test
    @DisplayName("should have REJECT policy")
    void shouldHaveRejectPolicy() {
      assertEquals(QuotaEnforcementPolicy.REJECT, QuotaEnforcementPolicy.valueOf("REJECT"));
    }

    @Test
    @DisplayName("should have TERMINATE policy")
    void shouldHaveTerminatePolicy() {
      assertEquals(QuotaEnforcementPolicy.TERMINATE, QuotaEnforcementPolicy.valueOf("TERMINATE"));
    }
  }

  @Nested
  @DisplayName("ResourceCommunicationType Enum Tests")
  class ResourceCommunicationTypeEnumTests {

    @Test
    @DisplayName("should have all communication types")
    void shouldHaveAllCommunicationTypes() {
      final var types = ResourceCommunicationType.values();
      assertEquals(4, types.length, "Should have 4 communication types");
    }

    @Test
    @DisplayName("should have MESSAGE_PASSING type")
    void shouldHaveMessagePassingType() {
      assertEquals(
          ResourceCommunicationType.MESSAGE_PASSING,
          ResourceCommunicationType.valueOf("MESSAGE_PASSING"));
    }

    @Test
    @DisplayName("should have SHARED_MEMORY type")
    void shouldHaveSharedMemoryType() {
      assertEquals(
          ResourceCommunicationType.SHARED_MEMORY,
          ResourceCommunicationType.valueOf("SHARED_MEMORY"));
    }

    @Test
    @DisplayName("should have EVENT_DRIVEN type")
    void shouldHaveEventDrivenType() {
      assertEquals(
          ResourceCommunicationType.EVENT_DRIVEN,
          ResourceCommunicationType.valueOf("EVENT_DRIVEN"));
    }

    @Test
    @DisplayName("should have STREAM type")
    void shouldHaveStreamType() {
      assertEquals(ResourceCommunicationType.STREAM, ResourceCommunicationType.valueOf("STREAM"));
    }
  }

  @Nested
  @DisplayName("AlertSeverity Enum Tests")
  class AlertSeverityEnumTests {

    @Test
    @DisplayName("should have all alert severities")
    void shouldHaveAllAlertSeverities() {
      final var severities = AlertSeverity.values();
      assertEquals(4, severities.length, "Should have 4 alert severities");
    }

    @Test
    @DisplayName("should have INFO severity")
    void shouldHaveInfoSeverity() {
      assertEquals(AlertSeverity.INFO, AlertSeverity.valueOf("INFO"));
    }

    @Test
    @DisplayName("should have WARNING severity")
    void shouldHaveWarningSeverity() {
      assertEquals(AlertSeverity.WARNING, AlertSeverity.valueOf("WARNING"));
    }

    @Test
    @DisplayName("should have ERROR severity")
    void shouldHaveErrorSeverity() {
      assertEquals(AlertSeverity.ERROR, AlertSeverity.valueOf("ERROR"));
    }

    @Test
    @DisplayName("should have CRITICAL severity")
    void shouldHaveCriticalSeverity() {
      assertEquals(AlertSeverity.CRITICAL, AlertSeverity.valueOf("CRITICAL"));
    }
  }

  @Nested
  @DisplayName("ResourceSharingPolicy Enum Tests")
  class ResourceSharingPolicyEnumTests {

    @Test
    @DisplayName("should have all sharing policies")
    void shouldHaveAllSharingPolicies() {
      final var policies = ResourceSharingPolicy.values();
      assertEquals(4, policies.length, "Should have 4 sharing policies");
    }

    @Test
    @DisplayName("should have EXCLUSIVE policy")
    void shouldHaveExclusivePolicy() {
      assertEquals(ResourceSharingPolicy.EXCLUSIVE, ResourceSharingPolicy.valueOf("EXCLUSIVE"));
    }

    @Test
    @DisplayName("should have SHARED policy")
    void shouldHaveSharedPolicy() {
      assertEquals(ResourceSharingPolicy.SHARED, ResourceSharingPolicy.valueOf("SHARED"));
    }

    @Test
    @DisplayName("should have ROUND_ROBIN policy")
    void shouldHaveRoundRobinPolicy() {
      assertEquals(ResourceSharingPolicy.ROUND_ROBIN, ResourceSharingPolicy.valueOf("ROUND_ROBIN"));
    }

    @Test
    @DisplayName("should have PRIORITY_BASED policy")
    void shouldHavePriorityBasedPolicy() {
      assertEquals(
          ResourceSharingPolicy.PRIORITY_BASED, ResourceSharingPolicy.valueOf("PRIORITY_BASED"));
    }
  }

  @Nested
  @DisplayName("ResourceMessageType Enum Tests")
  class ResourceMessageTypeEnumTests {

    @Test
    @DisplayName("should have all message types")
    void shouldHaveAllMessageTypes() {
      final var types = ResourceMessageType.values();
      assertEquals(4, types.length, "Should have 4 message types");
    }

    @Test
    @DisplayName("should have DATA type")
    void shouldHaveDataType() {
      assertEquals(ResourceMessageType.DATA, ResourceMessageType.valueOf("DATA"));
    }

    @Test
    @DisplayName("should have CONTROL type")
    void shouldHaveControlType() {
      assertEquals(ResourceMessageType.CONTROL, ResourceMessageType.valueOf("CONTROL"));
    }

    @Test
    @DisplayName("should have HEARTBEAT type")
    void shouldHaveHeartbeatType() {
      assertEquals(ResourceMessageType.HEARTBEAT, ResourceMessageType.valueOf("HEARTBEAT"));
    }

    @Test
    @DisplayName("should have ERROR type")
    void shouldHaveErrorType() {
      assertEquals(ResourceMessageType.ERROR, ResourceMessageType.valueOf("ERROR"));
    }
  }

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("should have ResourcePool interface")
    void shouldHaveResourcePoolInterface() {
      final var classes = ComponentResourceSharingManager.class.getDeclaredClasses();
      boolean found = false;
      for (final var clazz : classes) {
        if ("ResourcePool".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have ResourcePool nested interface");
    }

    @Test
    @DisplayName("should have ResourceAllocation interface")
    void shouldHaveResourceAllocationInterface() {
      final var classes = ComponentResourceSharingManager.class.getDeclaredClasses();
      boolean found = false;
      for (final var clazz : classes) {
        if ("ResourceAllocation".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have ResourceAllocation nested interface");
    }

    @Test
    @DisplayName("should have ResourceIsolationContext interface")
    void shouldHaveResourceIsolationContextInterface() {
      final var classes = ComponentResourceSharingManager.class.getDeclaredClasses();
      boolean found = false;
      for (final var clazz : classes) {
        if ("ResourceIsolationContext".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have ResourceIsolationContext nested interface");
    }

    @Test
    @DisplayName("should have ResourceQuotas interface")
    void shouldHaveResourceQuotasInterface() {
      final var classes = ComponentResourceSharingManager.class.getDeclaredClasses();
      boolean found = false;
      for (final var clazz : classes) {
        if ("ResourceQuotas".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have ResourceQuotas nested interface");
    }

    @Test
    @DisplayName("should have ResourceUsage interface")
    void shouldHaveResourceUsageInterface() {
      final var classes = ComponentResourceSharingManager.class.getDeclaredClasses();
      boolean found = false;
      for (final var clazz : classes) {
        if ("ResourceUsage".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have ResourceUsage nested interface");
    }

    @Test
    @DisplayName("should have ResourceMonitor interface")
    void shouldHaveResourceMonitorInterface() {
      final var classes = ComponentResourceSharingManager.class.getDeclaredClasses();
      boolean found = false;
      for (final var clazz : classes) {
        if ("ResourceMonitor".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have ResourceMonitor nested interface");
    }

    @Test
    @DisplayName("should have ResourceSnapshot interface")
    void shouldHaveResourceSnapshotInterface() {
      final var classes = ComponentResourceSharingManager.class.getDeclaredClasses();
      boolean found = false;
      for (final var clazz : classes) {
        if ("ResourceSnapshot".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have ResourceSnapshot nested interface");
    }

    @Test
    @DisplayName("should have ResourceEventListener interface")
    void shouldHaveResourceEventListenerInterface() {
      final var classes = ComponentResourceSharingManager.class.getDeclaredClasses();
      boolean found = false;
      for (final var clazz : classes) {
        if ("ResourceEventListener".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have ResourceEventListener nested interface");
    }
  }

  @Nested
  @DisplayName("Interface Completeness Tests")
  class InterfaceCompletenessTests {

    @Test
    @DisplayName("should have resource isolation methods")
    void shouldHaveResourceIsolationMethods() throws NoSuchMethodException {
      assertNotNull(
          ComponentResourceSharingManager.class.getMethod(
              "setupResourceIsolation",
              ComponentSimple.class,
              ComponentResourceSharingManager.ResourceIsolationConfig.class),
          "Should have setupResourceIsolation method");
      assertNotNull(
          ComponentResourceSharingManager.class.getMethod(
              "removeResourceIsolation", ComponentSimple.class),
          "Should have removeResourceIsolation method");
    }

    @Test
    @DisplayName("should have quota management methods")
    void shouldHaveQuotaManagementMethods() throws NoSuchMethodException {
      assertNotNull(
          ComponentResourceSharingManager.class.getMethod(
              "setResourceQuotas",
              ComponentSimple.class,
              ComponentResourceSharingManager.ResourceQuotas.class),
          "Should have setResourceQuotas method");
      assertNotNull(
          ComponentResourceSharingManager.class.getMethod(
              "getResourceQuotas", ComponentSimple.class),
          "Should have getResourceQuotas method");
      assertNotNull(
          ComponentResourceSharingManager.class.getMethod(
              "updateResourceQuotas",
              ComponentSimple.class,
              ComponentResourceSharingManager.ResourceQuotaUpdates.class),
          "Should have updateResourceQuotas method");
    }

    @Test
    @DisplayName("should have resource monitoring methods")
    void shouldHaveResourceMonitoringMethods() throws NoSuchMethodException {
      assertNotNull(
          ComponentResourceSharingManager.class.getMethod(
              "startResourceMonitoring",
              ComponentSimple.class,
              ComponentResourceSharingManager.ResourceMonitoringConfig.class),
          "Should have startResourceMonitoring method");
      assertNotNull(
          ComponentResourceSharingManager.class.getMethod(
              "stopResourceMonitoring", ComponentResourceSharingManager.ResourceMonitor.class),
          "Should have stopResourceMonitoring method");
    }

    @Test
    @DisplayName("should have garbage collection methods")
    void shouldHaveGarbageCollectionMethods() throws NoSuchMethodException {
      assertNotNull(
          ComponentResourceSharingManager.class.getMethod(
              "performGarbageCollection", ComponentResourceSharingManager.ResourceGCConfig.class),
          "Should have performGarbageCollection method");
    }

    @Test
    @DisplayName("should have snapshot methods")
    void shouldHaveSnapshotMethods() throws NoSuchMethodException {
      assertNotNull(
          ComponentResourceSharingManager.class.getMethod(
              "createResourceSnapshot",
              ComponentSimple.class,
              ComponentResourceSharingManager.ResourceSnapshotConfig.class),
          "Should have createResourceSnapshot method");
      assertNotNull(
          ComponentResourceSharingManager.class.getMethod(
              "restoreResourceSnapshot",
              ComponentSimple.class,
              ComponentResourceSharingManager.ResourceSnapshot.class),
          "Should have restoreResourceSnapshot method");
    }

    @Test
    @DisplayName("should have event listener methods")
    void shouldHaveEventListenerMethods() throws NoSuchMethodException {
      assertNotNull(
          ComponentResourceSharingManager.class.getMethod(
              "setResourceEventListener",
              ComponentResourceSharingManager.ResourceEventListener.class),
          "Should have setResourceEventListener method");
      assertNotNull(
          ComponentResourceSharingManager.class.getMethod("removeResourceEventListener"),
          "Should have removeResourceEventListener method");
    }
  }
}
