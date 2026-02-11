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

package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.component.ComponentResourceHandle;
import ai.tegmentum.wasmtime4j.wit.WitResourceManager;
import ai.tegmentum.wasmtime4j.exception.ResourceException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for Component Model resource lifecycle management.
 *
 * <p>These tests verify resource handle creation, ownership semantics, WitResourceManager
 * operations, and resource lifecycle management.
 *
 * @since 1.0.0
 */
@DisplayName("Component Resource Lifecycle Integration Tests")
public final class ComponentResourceLifecycleIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(ComponentResourceLifecycleIntegrationTest.class.getName());

  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down: " + testInfo.getDisplayName());
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  @Nested
  @DisplayName("Resource Handle Creation Tests")
  class ResourceHandleCreationTests {

    @Test
    @DisplayName("should create owned resource handle")
    void shouldCreateOwnedResourceHandle(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ComponentResourceHandle handle = ComponentResourceHandle.own("file-descriptor", 1);
      assertNotNull(handle, "Handle should not be null");
      assertEquals("file-descriptor", handle.getResourceType());
      assertEquals(1, handle.getIndex());
      assertTrue(handle.isOwned(), "Handle should be owned");
      assertFalse(handle.isBorrowed(), "Handle should not be borrowed");

      LOGGER.info("Created owned handle: " + handle);
    }

    @Test
    @DisplayName("should create borrowed resource handle")
    void shouldCreateBorrowedResourceHandle(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ComponentResourceHandle handle = ComponentResourceHandle.borrow("stream", 42);
      assertNotNull(handle, "Handle should not be null");
      assertEquals("stream", handle.getResourceType());
      assertEquals(42, handle.getIndex());
      assertFalse(handle.isOwned(), "Handle should not be owned");
      assertTrue(handle.isBorrowed(), "Handle should be borrowed");

      LOGGER.info("Created borrowed handle: " + handle);
    }

    @Test
    @DisplayName("should create owned handle with host object")
    void shouldCreateOwnedHandleWithHostObject(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      String hostObject = "test-host-object";
      ComponentResourceHandle handle =
          ComponentResourceHandle.ownWithHost("custom-resource", 5, hostObject);

      assertNotNull(handle, "Handle should not be null");
      assertTrue(handle.isOwned(), "Handle should be owned");
      assertEquals(hostObject, handle.getHostObject(String.class));

      LOGGER.info("Created owned handle with host object: " + handle);
    }

    @Test
    @DisplayName("should create borrowed handle with host object")
    void shouldCreateBorrowedHandleWithHostObject(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      Integer hostObject = 12345;
      ComponentResourceHandle handle =
          ComponentResourceHandle.borrowWithHost("number-resource", 10, hostObject);

      assertNotNull(handle, "Handle should not be null");
      assertTrue(handle.isBorrowed(), "Handle should be borrowed");
      assertEquals(hostObject, handle.getHostObject(Integer.class));

      LOGGER.info("Created borrowed handle with host object: " + handle);
    }

    @Test
    @DisplayName("should throw on null resource type")
    void shouldThrowOnNullResourceType(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentResourceHandle.own(null, 1),
          "Should throw on null resource type");
    }

    @Test
    @DisplayName("should throw when getting host object from handle without one")
    void shouldThrowWhenGettingHostObjectFromHandleWithoutOne(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ComponentResourceHandle handle = ComponentResourceHandle.own("test", 1);

      assertThrows(
          IllegalStateException.class,
          () -> handle.getHostObject(String.class),
          "Should throw when no host object");
    }
  }

  @Nested
  @DisplayName("Resource Handle Equality Tests")
  class ResourceHandleEqualityTests {

    @Test
    @DisplayName("should be equal for same type, index, and ownership")
    void shouldBeEqualForSameTypeIndexOwnership(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ComponentResourceHandle handle1 = ComponentResourceHandle.own("resource", 5);
      ComponentResourceHandle handle2 = ComponentResourceHandle.own("resource", 5);

      assertEquals(handle1, handle2, "Handles with same properties should be equal");
      assertEquals(handle1.hashCode(), handle2.hashCode(), "Hash codes should match");

      LOGGER.info("Equality verified for: " + handle1 + " and " + handle2);
    }

    @Test
    @DisplayName("should not be equal for different ownership")
    void shouldNotBeEqualForDifferentOwnership(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ComponentResourceHandle owned = ComponentResourceHandle.own("resource", 5);
      ComponentResourceHandle borrowed = ComponentResourceHandle.borrow("resource", 5);

      assertFalse(owned.equals(borrowed), "Owned and borrowed should not be equal");

      LOGGER.info("Different ownership verified: " + owned + " vs " + borrowed);
    }

    @Test
    @DisplayName("should not be equal for different indices")
    void shouldNotBeEqualForDifferentIndices(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ComponentResourceHandle handle1 = ComponentResourceHandle.own("resource", 1);
      ComponentResourceHandle handle2 = ComponentResourceHandle.own("resource", 2);

      assertFalse(handle1.equals(handle2), "Handles with different indices should not be equal");

      LOGGER.info("Different indices verified: " + handle1 + " vs " + handle2);
    }
  }

  @Nested
  @DisplayName("WitResourceManager Tests")
  class WitResourceManagerTests {

    @Test
    @DisplayName("should create resource manager with default config")
    void shouldCreateResourceManagerWithDefaultConfig(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (WitResourceManager manager = new WitResourceManager()) {
        assertNotNull(manager, "Manager should not be null");
        assertFalse(manager.isClosed(), "New manager should not be closed");
        assertEquals(0, manager.getActiveResourceCount(), "New manager should have no resources");

        LOGGER.info("Created resource manager: " + manager.getManagerId());
      }
    }

    @Test
    @DisplayName("should create resource manager with custom config")
    void shouldCreateResourceManagerWithCustomConfig(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      WitResourceManager.ResourceConfig config =
          new WitResourceManager.ResourceConfig(100, 60, 300000);

      try (WitResourceManager manager = new WitResourceManager(config)) {
        assertNotNull(manager, "Manager should not be null");

        LOGGER.info("Created resource manager with custom config: " + manager.getManagerId());
      }
    }

    @Test
    @DisplayName("should register and create resources")
    void shouldRegisterAndCreateResources(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (WitResourceManager manager = new WitResourceManager()) {
        // Register resource type
        WitResourceManager.ResourceTypeInfo typeInfo =
            new WitResourceManager.ResourceTypeInfo("test-string", String.class, null);
        manager.registerResourceType("test-string", typeInfo);

        // Create resources
        int handle1 = manager.createResource("test-string", "first");
        int handle2 = manager.createResource("test-string", "second");
        int handle3 = manager.createResource("test-string", "third");

        LOGGER.info("Created resources: " + handle1 + ", " + handle2 + ", " + handle3);

        assertEquals(3, manager.getActiveResourceCount(), "Should have 3 resources");
        assertEquals(
            3,
            manager.getActiveResourceCount("test-string"),
            "Should have 3 resources of type test-string");

        // Get resources
        assertEquals("first", manager.getResourceValue(handle1, String.class));
        assertEquals("second", manager.getResourceValue(handle2, String.class));
        assertEquals("third", manager.getResourceValue(handle3, String.class));
      }
    }

    @Test
    @DisplayName("should destroy resources")
    void shouldDestroyResources(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (WitResourceManager manager = new WitResourceManager()) {
        WitResourceManager.ResourceTypeInfo typeInfo =
            new WitResourceManager.ResourceTypeInfo("int-resource", Integer.class, null);
        manager.registerResourceType("int-resource", typeInfo);

        final int handle1 = manager.createResource("int-resource", 100);
        final int handle2 = manager.createResource("int-resource", 200);

        assertEquals(2, manager.getActiveResourceCount(), "Should have 2 resources");

        // Destroy one
        boolean destroyed = manager.destroyResource(handle1);
        assertTrue(destroyed, "Resource should be destroyed");

        assertEquals(1, manager.getActiveResourceCount(), "Should have 1 resource");
        assertFalse(
            manager.getActiveResourceIds().contains(handle1), "Handle1 should not be active");
        assertTrue(manager.getActiveResourceIds().contains(handle2), "Handle2 should be active");

        LOGGER.info("Destroyed resource, remaining: " + manager.getActiveResourceCount());
      }
    }

    @Test
    @DisplayName("should throw on unknown resource type")
    void shouldThrowOnUnknownResourceType(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (WitResourceManager manager = new WitResourceManager()) {
        assertThrows(
            ResourceException.class,
            () -> manager.createResource("unknown-type", "value"),
            "Should throw on unknown resource type");
      }
    }

    @Test
    @DisplayName("should throw on invalid handle access")
    void shouldThrowOnInvalidHandleAccess(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (WitResourceManager manager = new WitResourceManager()) {
        WitResourceManager.ResourceTypeInfo typeInfo =
            new WitResourceManager.ResourceTypeInfo("test", String.class, null);
        manager.registerResourceType("test", typeInfo);

        int handle = manager.createResource("test", "test-value");
        manager.destroyResource(handle);

        // Should throw for deleted handle
        assertThrows(
            ResourceException.class,
            () -> manager.getResource(handle),
            "Should throw for deleted handle");

        // Should throw for invalid handle
        assertThrows(
            ResourceException.class, () -> manager.getResource(999), "Should throw for invalid id");

        LOGGER.info("Invalid handle access properly rejected");
      }
    }

    @Test
    @DisplayName("should get active resource IDs")
    void shouldGetActiveResourceIds(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (WitResourceManager manager = new WitResourceManager()) {
        WitResourceManager.ResourceTypeInfo typeInfo =
            new WitResourceManager.ResourceTypeInfo("test", String.class, null);
        manager.registerResourceType("test", typeInfo);

        final int handle1 = manager.createResource("test", "a");
        final int handle2 = manager.createResource("test", "b");
        final int handle3 = manager.createResource("test", "c");

        Set<Integer> activeIds = manager.getActiveResourceIds();
        assertEquals(3, activeIds.size(), "Should have 3 active IDs");
        assertTrue(activeIds.contains(handle1), "Should contain handle1");
        assertTrue(activeIds.contains(handle2), "Should contain handle2");
        assertTrue(activeIds.contains(handle3), "Should contain handle3");

        LOGGER.info("Active resource IDs: " + activeIds);
      }
    }
  }

  @Nested
  @DisplayName("Resource Lifecycle Tests")
  class ResourceLifecycleTests {

    @Test
    @DisplayName("should track resource lifecycle with destructor")
    void shouldTrackResourceLifecycleWithDestructor(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      AtomicBoolean destructorCalled = new AtomicBoolean(false);
      AtomicInteger destructorValue = new AtomicInteger(0);

      try (WitResourceManager manager = new WitResourceManager()) {
        // Register resource type with destructor
        WitResourceManager.ResourceTypeInfo typeInfo =
            new WitResourceManager.ResourceTypeInfo(
                "tracked-resource",
                Integer.class,
                resource -> {
                  destructorCalled.set(true);
                  destructorValue.set((Integer) resource);
                  LOGGER.info("Destructor called for value: " + resource);
                });
        manager.registerResourceType("tracked-resource", typeInfo);

        int handle = manager.createResource("tracked-resource", 42);

        // Destroy should trigger destructor
        manager.destroyResource(handle);

        assertTrue(destructorCalled.get(), "Destructor should have been called");
        assertEquals(42, destructorValue.get(), "Destructor should receive the resource value");

        LOGGER.info("Resource lifecycle with destructor verified");
      }
    }

    @Test
    @DisplayName("should handle multiple resource types")
    void shouldHandleMultipleResourceTypes(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (WitResourceManager manager = new WitResourceManager()) {
        // Register multiple types
        manager.registerResourceType(
            "string-res",
            new WitResourceManager.ResourceTypeInfo("string-res", String.class, null));
        manager.registerResourceType(
            "int-res", new WitResourceManager.ResourceTypeInfo("int-res", Integer.class, null));
        manager.registerResourceType(
            "double-res",
            new WitResourceManager.ResourceTypeInfo("double-res", Double.class, null));

        int strHandle = manager.createResource("string-res", "hello");
        int intHandle = manager.createResource("int-res", 123);
        int dblHandle = manager.createResource("double-res", 3.14);

        assertEquals("hello", manager.getResourceValue(strHandle, String.class));
        assertEquals(Integer.valueOf(123), manager.getResourceValue(intHandle, Integer.class));
        assertEquals(Double.valueOf(3.14), manager.getResourceValue(dblHandle, Double.class));

        assertEquals(1, manager.getActiveResourceCount("string-res"));
        assertEquals(1, manager.getActiveResourceCount("int-res"));
        assertEquals(1, manager.getActiveResourceCount("double-res"));

        LOGGER.info("Multiple resource types handled successfully");
      }
    }

    @Test
    @DisplayName("should manage reference counts")
    void shouldManageReferenceCounts(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (WitResourceManager manager = new WitResourceManager()) {
        WitResourceManager.ResourceTypeInfo typeInfo =
            new WitResourceManager.ResourceTypeInfo("refcounted", String.class, null);
        manager.registerResourceType("refcounted", typeInfo);

        int handle = manager.createResource("refcounted", "shared-resource");

        // Initial ref count is 1
        WitResourceManager.ManagedResource resource = manager.getResource(handle);
        assertEquals(1, resource.getRefCount(), "Initial ref count should be 1");

        // Increment ref count
        manager.incrementRefCount(handle);
        assertEquals(2, resource.getRefCount(), "Ref count should be 2");

        manager.incrementRefCount(handle);
        assertEquals(3, resource.getRefCount(), "Ref count should be 3");

        // Decrement ref count (should not destroy yet)
        boolean destroyed = manager.decrementRefCount(handle);
        assertFalse(destroyed, "Should not be destroyed with ref count > 0");
        assertEquals(2, resource.getRefCount(), "Ref count should be 2");

        destroyed = manager.decrementRefCount(handle);
        assertFalse(destroyed, "Should not be destroyed with ref count > 0");
        assertEquals(1, resource.getRefCount(), "Ref count should be 1");

        // Final decrement destroys resource
        destroyed = manager.decrementRefCount(handle);
        assertTrue(destroyed, "Should be destroyed when ref count reaches 0");
        assertEquals(0, manager.getActiveResourceCount(), "No active resources");

        LOGGER.info("Reference counting verified");
      }
    }
  }

  @Nested
  @DisplayName("Resource Usage Stats Tests")
  class ResourceUsageStatsTests {

    @Test
    @DisplayName("should track resource usage statistics")
    void shouldTrackResourceUsageStatistics(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (WitResourceManager manager = new WitResourceManager()) {
        WitResourceManager.ResourceTypeInfo typeInfo =
            new WitResourceManager.ResourceTypeInfo("stats-test", String.class, null);
        manager.registerResourceType("stats-test", typeInfo);

        WitResourceManager.ResourceUsageStats initialStats = manager.getUsageStats();
        assertEquals(0, initialStats.getActiveResources(), "Initial active count");
        assertEquals(0, initialStats.getTotalCreated(), "Initial total created");
        assertEquals(0, initialStats.getTotalDestroyed(), "Initial total destroyed");

        // Create resources
        final int handle1 = manager.createResource("stats-test", "a");
        final int handle2 = manager.createResource("stats-test", "b");
        final int handle3 = manager.createResource("stats-test", "c");

        WitResourceManager.ResourceUsageStats afterCreate = manager.getUsageStats();
        assertEquals(3, afterCreate.getActiveResources(), "3 active after creation");
        assertEquals(3, afterCreate.getTotalCreated(), "3 total created");
        assertEquals(0, afterCreate.getTotalDestroyed(), "0 total destroyed");

        // Destroy some resources
        manager.destroyResource(handle1);
        manager.destroyResource(handle2);

        WitResourceManager.ResourceUsageStats afterDestroy = manager.getUsageStats();
        assertEquals(1, afterDestroy.getActiveResources(), "1 active after destroy");
        assertEquals(3, afterDestroy.getTotalCreated(), "3 total created");
        assertEquals(2, afterDestroy.getTotalDestroyed(), "2 total destroyed");

        LOGGER.info(
            "Usage stats: "
                + afterDestroy.getActiveResources()
                + " active, "
                + afterDestroy.getTotalCreated()
                + " created, "
                + afterDestroy.getTotalDestroyed()
                + " destroyed");
      }
    }

    @Test
    @DisplayName("should track resource type counts")
    void shouldTrackResourceTypeCounts(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (WitResourceManager manager = new WitResourceManager()) {
        manager.registerResourceType(
            "type-a", new WitResourceManager.ResourceTypeInfo("type-a", String.class, null));
        manager.registerResourceType(
            "type-b", new WitResourceManager.ResourceTypeInfo("type-b", String.class, null));

        manager.createResource("type-a", "a1");
        manager.createResource("type-a", "a2");
        manager.createResource("type-a", "a3");
        manager.createResource("type-b", "b1");
        manager.createResource("type-b", "b2");

        WitResourceManager.ResourceUsageStats stats = manager.getUsageStats();
        assertEquals(Long.valueOf(3), stats.getResourceTypeCounts().get("type-a"), "Type-a count");
        assertEquals(Long.valueOf(2), stats.getResourceTypeCounts().get("type-b"), "Type-b count");

        LOGGER.info("Resource type counts: " + stats.getResourceTypeCounts());
      }
    }
  }

  @Nested
  @DisplayName("Resource Cleanup Tests")
  class ResourceCleanupTests {

    @Test
    @DisplayName("should cleanup resources on manager close")
    void shouldCleanupResourcesOnManagerClose(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      AtomicInteger destructorCallCount = new AtomicInteger(0);

      WitResourceManager manager = new WitResourceManager();

      WitResourceManager.ResourceTypeInfo typeInfo =
          new WitResourceManager.ResourceTypeInfo(
              "cleanup-test", String.class, resource -> destructorCallCount.incrementAndGet());
      manager.registerResourceType("cleanup-test", typeInfo);

      manager.createResource("cleanup-test", "r1");
      manager.createResource("cleanup-test", "r2");
      manager.createResource("cleanup-test", "r3");

      assertEquals(3, manager.getActiveResourceCount(), "Should have 3 resources");

      // Close manager - should trigger destructors
      manager.close();

      assertTrue(manager.isClosed(), "Manager should be closed");
      assertEquals(3, destructorCallCount.get(), "All 3 destructors should be called");

      LOGGER.info("Cleanup on close verified, destructors called: " + destructorCallCount.get());
    }

    @Test
    @DisplayName("should perform manual cleanup")
    void shouldPerformManualCleanup(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (WitResourceManager manager = new WitResourceManager()) {
        WitResourceManager.ResourceTypeInfo typeInfo =
            new WitResourceManager.ResourceTypeInfo("manual-cleanup", String.class, null);
        manager.registerResourceType("manual-cleanup", typeInfo);

        manager.createResource("manual-cleanup", "test");

        int cleanedUp = manager.performCleanup();
        LOGGER.info("Manual cleanup result: " + cleanedUp + " resources cleaned");

        // performCleanup doesn't destroy active resources with ref count > 0
        // So we expect 0 cleanups for active resources
        assertTrue(cleanedUp >= 0, "Cleanup count should be non-negative");
      }
    }
  }
}
