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
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentResourceSharingManager} interface.
 *
 * <p>ComponentResourceSharingManager provides advanced resource sharing and isolation for
 * components including shared resource pools, isolation, quotas, and cross-component communication.
 */
@DisplayName("ComponentResourceSharingManager Interface Tests")
class ComponentResourceSharingManagerTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ComponentResourceSharingManager.class.isInterface(),
          "ComponentResourceSharingManager should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      final Class<?>[] interfaces = ComponentResourceSharingManager.class.getInterfaces();
      boolean extendsAutoCloseable = false;
      for (final Class<?> iface : interfaces) {
        if (iface == AutoCloseable.class) {
          extendsAutoCloseable = true;
          break;
        }
      }
      assertTrue(extendsAutoCloseable, "Should extend AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Core Method Tests")
  class CoreMethodTests {

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
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceSharingManager.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Resource Pool Method Tests")
  class ResourcePoolMethodTests {

    @Test
    @DisplayName("should have createResourcePool method")
    void shouldHaveCreateResourcePoolMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod(
              "createResourcePool",
              String.class,
              ComponentResourceSharingManager.ResourceType.class,
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
      assertEquals(void.class, method.getReturnType(), "Should return void");
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
  @DisplayName("Resource Allocation Method Tests")
  class ResourceAllocationMethodTests {

    @Test
    @DisplayName("should have allocateResources method")
    void shouldHaveAllocateResourcesMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod(
              "allocateResources",
              Component.class,
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
  }

  @Nested
  @DisplayName("Resource Isolation Method Tests")
  class ResourceIsolationMethodTests {

    @Test
    @DisplayName("should have setupResourceIsolation method")
    void shouldHaveSetupResourceIsolationMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod(
              "setupResourceIsolation",
              Component.class,
              ComponentResourceSharingManager.ResourceIsolationConfig.class);
      assertNotNull(method, "setupResourceIsolation method should exist");
    }

    @Test
    @DisplayName("should have removeResourceIsolation method")
    void shouldHaveRemoveResourceIsolationMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod(
              "removeResourceIsolation", Component.class);
      assertNotNull(method, "removeResourceIsolation method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Resource Quota Method Tests")
  class ResourceQuotaMethodTests {

    @Test
    @DisplayName("should have setResourceQuotas method")
    void shouldHaveSetResourceQuotasMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod(
              "setResourceQuotas",
              Component.class,
              ComponentResourceSharingManager.ResourceQuotas.class);
      assertNotNull(method, "setResourceQuotas method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getResourceQuotas method")
    void shouldHaveGetResourceQuotasMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod(
              "getResourceQuotas", Component.class);
      assertNotNull(method, "getResourceQuotas method should exist");
    }

    @Test
    @DisplayName("should have updateResourceQuotas method")
    void shouldHaveUpdateResourceQuotasMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod(
              "updateResourceQuotas",
              Component.class,
              ComponentResourceSharingManager.ResourceQuotaUpdates.class);
      assertNotNull(method, "updateResourceQuotas method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getResourceUsage method")
    void shouldHaveGetResourceUsageMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod(
              "getResourceUsage", Component.class);
      assertNotNull(method, "getResourceUsage method should exist");
    }
  }

  @Nested
  @DisplayName("Monitoring Method Tests")
  class MonitoringMethodTests {

    @Test
    @DisplayName("should have startResourceMonitoring method")
    void shouldHaveStartResourceMonitoringMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod(
              "startResourceMonitoring",
              Component.class,
              ComponentResourceSharingManager.ResourceMonitoringConfig.class);
      assertNotNull(method, "startResourceMonitoring method should exist");
    }

    @Test
    @DisplayName("should have stopResourceMonitoring method")
    void shouldHaveStopResourceMonitoringMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod(
              "stopResourceMonitoring", ComponentResourceSharingManager.ResourceMonitor.class);
      assertNotNull(method, "stopResourceMonitoring method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceSharingManager.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
    }
  }

  @Nested
  @DisplayName("Event Listener Method Tests")
  class EventListenerMethodTests {

    @Test
    @DisplayName("should have setResourceEventListener method")
    void shouldHaveSetResourceEventListenerMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod(
              "setResourceEventListener",
              ComponentResourceSharingManager.ResourceEventListener.class);
      assertNotNull(method, "setResourceEventListener method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have removeResourceEventListener method")
    void shouldHaveRemoveResourceEventListenerMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod("removeResourceEventListener");
      assertNotNull(method, "removeResourceEventListener method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("should have ResourcePool nested interface")
    void shouldHaveResourcePoolNestedInterface() {
      final Class<?>[] declaredClasses = ComponentResourceSharingManager.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("ResourcePool")) {
          found = true;
          assertTrue(clazz.isInterface(), "ResourcePool should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have ResourcePool nested interface");
    }

    @Test
    @DisplayName("should have ResourceAllocation nested interface")
    void shouldHaveResourceAllocationNestedInterface() {
      final Class<?>[] declaredClasses = ComponentResourceSharingManager.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("ResourceAllocation")) {
          found = true;
          assertTrue(clazz.isInterface(), "ResourceAllocation should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have ResourceAllocation nested interface");
    }

    @Test
    @DisplayName("should have ResourceQuotas nested interface")
    void shouldHaveResourceQuotasNestedInterface() {
      final Class<?>[] declaredClasses = ComponentResourceSharingManager.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("ResourceQuotas")) {
          found = true;
          assertTrue(clazz.isInterface(), "ResourceQuotas should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have ResourceQuotas nested interface");
    }

    @Test
    @DisplayName("should have ResourceMonitor nested interface")
    void shouldHaveResourceMonitorNestedInterface() {
      final Class<?>[] declaredClasses = ComponentResourceSharingManager.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("ResourceMonitor")) {
          found = true;
          assertTrue(clazz.isInterface(), "ResourceMonitor should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have ResourceMonitor nested interface");
    }
  }

  @Nested
  @DisplayName("Nested Enum Tests")
  class NestedEnumTests {

    @Test
    @DisplayName("should have ResourceType enum")
    void shouldHaveResourceTypeEnum() {
      final Class<?>[] declaredClasses = ComponentResourceSharingManager.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("ResourceType")) {
          found = true;
          assertTrue(clazz.isEnum(), "ResourceType should be an enum");
          break;
        }
      }
      assertTrue(found, "Should have ResourceType enum");
    }

    @Test
    @DisplayName("ResourceType enum should have expected values")
    void resourceTypeEnumShouldHaveExpectedValues() {
      final ComponentResourceSharingManager.ResourceType[] values =
          ComponentResourceSharingManager.ResourceType.values();
      final Set<String> valueNames = new HashSet<>();
      for (final ComponentResourceSharingManager.ResourceType value : values) {
        valueNames.add(value.name());
      }

      assertTrue(valueNames.contains("MEMORY"), "Should have MEMORY");
      assertTrue(valueNames.contains("CPU"), "Should have CPU");
      assertTrue(valueNames.contains("NETWORK"), "Should have NETWORK");
      assertTrue(valueNames.contains("FILE_SYSTEM"), "Should have FILE_SYSTEM");
    }

    @Test
    @DisplayName("should have ResourceAllocationStatus enum")
    void shouldHaveResourceAllocationStatusEnum() {
      final Class<?>[] declaredClasses = ComponentResourceSharingManager.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("ResourceAllocationStatus")) {
          found = true;
          assertTrue(clazz.isEnum(), "ResourceAllocationStatus should be an enum");
          break;
        }
      }
      assertTrue(found, "Should have ResourceAllocationStatus enum");
    }

    @Test
    @DisplayName("ResourceAllocationStatus enum should have expected values")
    void resourceAllocationStatusEnumShouldHaveExpectedValues() {
      final ComponentResourceSharingManager.ResourceAllocationStatus[] values =
          ComponentResourceSharingManager.ResourceAllocationStatus.values();
      final Set<String> valueNames = new HashSet<>();
      for (final ComponentResourceSharingManager.ResourceAllocationStatus value : values) {
        valueNames.add(value.name());
      }

      assertTrue(valueNames.contains("ACTIVE"), "Should have ACTIVE");
      assertTrue(valueNames.contains("EXPIRED"), "Should have EXPIRED");
      assertTrue(valueNames.contains("RELEASED"), "Should have RELEASED");
      assertTrue(valueNames.contains("FAILED"), "Should have FAILED");
    }

    @Test
    @DisplayName("should have ResourceIsolationLevel enum")
    void shouldHaveResourceIsolationLevelEnum() {
      final Class<?>[] declaredClasses = ComponentResourceSharingManager.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("ResourceIsolationLevel")) {
          found = true;
          assertTrue(clazz.isEnum(), "ResourceIsolationLevel should be an enum");
          break;
        }
      }
      assertTrue(found, "Should have ResourceIsolationLevel enum");
    }

    @Test
    @DisplayName("should have AlertSeverity enum")
    void shouldHaveAlertSeverityEnum() {
      final Class<?>[] declaredClasses = ComponentResourceSharingManager.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("AlertSeverity")) {
          found = true;
          assertTrue(clazz.isEnum(), "AlertSeverity should be an enum");
          break;
        }
      }
      assertTrue(found, "Should have AlertSeverity enum");
    }

    @Test
    @DisplayName("AlertSeverity enum should have expected values")
    void alertSeverityEnumShouldHaveExpectedValues() {
      final ComponentResourceSharingManager.AlertSeverity[] values =
          ComponentResourceSharingManager.AlertSeverity.values();
      final String[] expectedValues = {"INFO", "WARNING", "ERROR", "CRITICAL"};
      final Set<String> valueNames = new HashSet<>();
      for (final ComponentResourceSharingManager.AlertSeverity value : values) {
        valueNames.add(value.name());
      }

      for (final String expected : expectedValues) {
        assertTrue(valueNames.contains(expected), "Should have " + expected);
      }
    }
  }

  @Nested
  @DisplayName("Snapshot and GC Method Tests")
  class SnapshotAndGcMethodTests {

    @Test
    @DisplayName("should have performGarbageCollection method")
    void shouldHavePerformGarbageCollectionMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod(
              "performGarbageCollection", ComponentResourceSharingManager.ResourceGCConfig.class);
      assertNotNull(method, "performGarbageCollection method should exist");
    }

    @Test
    @DisplayName("should have createResourceSnapshot method")
    void shouldHaveCreateResourceSnapshotMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod(
              "createResourceSnapshot",
              Component.class,
              ComponentResourceSharingManager.ResourceSnapshotConfig.class);
      assertNotNull(method, "createResourceSnapshot method should exist");
    }

    @Test
    @DisplayName("should have restoreResourceSnapshot method")
    void shouldHaveRestoreResourceSnapshotMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod(
              "restoreResourceSnapshot",
              Component.class,
              ComponentResourceSharingManager.ResourceSnapshot.class);
      assertNotNull(method, "restoreResourceSnapshot method should exist");
    }
  }

  @Nested
  @DisplayName("Communication Method Tests")
  class CommunicationMethodTests {

    @Test
    @DisplayName("should have setupResourceCommunication method")
    void shouldHaveSetupResourceCommunicationMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod(
              "setupResourceCommunication",
              Component.class,
              Component.class,
              ComponentResourceSharingManager.ResourceCommunicationConfig.class);
      assertNotNull(method, "setupResourceCommunication method should exist");
    }

    @Test
    @DisplayName("should have removeResourceCommunication method")
    void shouldHaveRemoveResourceCommunicationMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceSharingManager.class.getMethod(
              "removeResourceCommunication",
              ComponentResourceSharingManager.ResourceCommunicationChannel.class);
      assertNotNull(method, "removeResourceCommunication method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
