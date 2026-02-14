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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wit.WitResourceManager;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitResourceManager} class.
 *
 * <p>WitResourceManager provides comprehensive resource management for WIT resources, including
 * lifecycle tracking, automatic cleanup, reference counting, and garbage collection integration.
 */
@DisplayName("WitResourceManager Tests")
class WitResourceManagerTest {

  private WitResourceManager manager;

  @BeforeEach
  void setUp() {
    manager = new WitResourceManager();
  }

  @AfterEach
  void tearDown() {
    if (manager != null && !manager.isClosed()) {
      manager.close();
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create manager with default configuration")
    void shouldCreateManagerWithDefaultConfiguration() {
      try (final WitResourceManager newManager = new WitResourceManager()) {
        assertNotNull(newManager);
        assertFalse(newManager.isClosed());
        assertNotNull(newManager.getManagerId());
        assertTrue(newManager.getManagerId().startsWith("wit-resource-manager-"));
      }
    }

    @Test
    @DisplayName("should create manager with custom configuration")
    void shouldCreateManagerWithCustomConfiguration() {
      final WitResourceManager.ResourceConfig config =
          new WitResourceManager.ResourceConfig(100, 10, 5000);
      try (final WitResourceManager customManager = new WitResourceManager(config)) {
        assertNotNull(customManager);
        assertFalse(customManager.isClosed());
      }
    }

    @Test
    @DisplayName("constructor should throw on null config")
    void constructorShouldThrowOnNullConfig() {
      assertThrows(NullPointerException.class, () -> new WitResourceManager(null));
    }
  }

  @Nested
  @DisplayName("ResourceConfig Tests")
  class ResourceConfigTests {

    @Test
    @DisplayName("should create config with default values")
    void shouldCreateConfigWithDefaultValues() {
      final WitResourceManager.ResourceConfig config = new WitResourceManager.ResourceConfig();

      assertEquals(10000, config.getMaxResources());
      assertEquals(30, config.getCleanupIntervalSeconds());
      assertEquals(0, config.getResourceExpirationMillis());
    }

    @Test
    @DisplayName("should create config with custom values")
    void shouldCreateConfigWithCustomValues() {
      final WitResourceManager.ResourceConfig config =
          new WitResourceManager.ResourceConfig(500, 60, 30000);

      assertEquals(500, config.getMaxResources());
      assertEquals(60, config.getCleanupIntervalSeconds());
      assertEquals(30000, config.getResourceExpirationMillis());
    }
  }

  @Nested
  @DisplayName("Resource Type Registration Tests")
  class ResourceTypeRegistrationTests {

    @Test
    @DisplayName("registerResourceType should throw on null typeName")
    void registerResourceTypeShouldThrowOnNullTypeName() {
      final WitResourceManager.ResourceTypeInfo typeInfo =
          new WitResourceManager.ResourceTypeInfo("test", Object.class, null);
      assertThrows(NullPointerException.class, () -> manager.registerResourceType(null, typeInfo));
    }

    @Test
    @DisplayName("registerResourceType should throw on null typeInfo")
    void registerResourceTypeShouldThrowOnNullTypeInfo() {
      assertThrows(NullPointerException.class, () -> manager.registerResourceType("test", null));
    }

    @Test
    @DisplayName("should register resource type successfully")
    void shouldRegisterResourceTypeSuccessfully() {
      final WitResourceManager.ResourceTypeInfo typeInfo =
          new WitResourceManager.ResourceTypeInfo("test-type", String.class, null);

      manager.registerResourceType("test-type", typeInfo);
      // Registration should not throw
      assertNotNull(manager);
    }
  }

  @Nested
  @DisplayName("ResourceTypeInfo Tests")
  class ResourceTypeInfoTests {

    @Test
    @DisplayName("should create ResourceTypeInfo with all fields")
    void shouldCreateResourceTypeInfoWithAllFields() {
      final WitResourceManager.ResourceDestructor destructor = resource -> {};
      final WitResourceManager.ResourceTypeInfo typeInfo =
          new WitResourceManager.ResourceTypeInfo("my-type", String.class, destructor);

      assertEquals("my-type", typeInfo.getTypeName());
      assertEquals(String.class, typeInfo.getResourceClass());
      assertEquals(destructor, typeInfo.getDestructor());
    }

    @Test
    @DisplayName("should create ResourceTypeInfo with null destructor")
    void shouldCreateResourceTypeInfoWithNullDestructor() {
      final WitResourceManager.ResourceTypeInfo typeInfo =
          new WitResourceManager.ResourceTypeInfo("my-type", Integer.class, null);

      assertEquals("my-type", typeInfo.getTypeName());
      assertEquals(Integer.class, typeInfo.getResourceClass());
      assertFalse(typeInfo.getDestructor() != null);
    }
  }

  @Nested
  @DisplayName("Resource Creation Tests")
  class ResourceCreationTests {

    @BeforeEach
    void registerTestType() {
      final WitResourceManager.ResourceTypeInfo typeInfo =
          new WitResourceManager.ResourceTypeInfo("test-type", String.class, null);
      manager.registerResourceType("test-type", typeInfo);
    }

    @Test
    @DisplayName("createResource should throw on null typeName")
    void createResourceShouldThrowOnNullTypeName() {
      assertThrows(NullPointerException.class, () -> manager.createResource(null, "resource"));
    }

    @Test
    @DisplayName("createResource should throw on null resource")
    void createResourceShouldThrowOnNullResource() {
      assertThrows(NullPointerException.class, () -> manager.createResource("test-type", null));
    }

    @Test
    @DisplayName("createResource should throw for unknown type")
    void createResourceShouldThrowForUnknownType() {
      assertThrows(
          WasmException.class, () -> manager.createResource("unknown-type", "resource"));
    }

    @Test
    @DisplayName("should create resource and return handle")
    void shouldCreateResourceAndReturnHandle() throws WasmException {
      final int resourceId = manager.createResource("test-type", "my-resource");

      assertTrue(resourceId > 0);
      assertEquals(1, manager.getActiveResourceCount());
    }

    @Test
    @DisplayName("should create multiple resources with unique handles")
    void shouldCreateMultipleResourcesWithUniqueHandles() throws WasmException {
      final int id1 = manager.createResource("test-type", "resource1");
      final int id2 = manager.createResource("test-type", "resource2");
      final int id3 = manager.createResource("test-type", "resource3");

      assertTrue(id1 != id2);
      assertTrue(id2 != id3);
      assertTrue(id1 != id3);
      assertEquals(3, manager.getActiveResourceCount());
    }

    @Test
    @DisplayName("should enforce max resources limit")
    void shouldEnforceMaxResourcesLimit() throws WasmException {
      final WitResourceManager.ResourceConfig config =
          new WitResourceManager.ResourceConfig(2, 30, 0);

      try (final WitResourceManager limitedManager = new WitResourceManager(config)) {
        final WitResourceManager.ResourceTypeInfo typeInfo =
            new WitResourceManager.ResourceTypeInfo("test-type", String.class, null);
        limitedManager.registerResourceType("test-type", typeInfo);

        limitedManager.createResource("test-type", "resource1");
        limitedManager.createResource("test-type", "resource2");

        assertThrows(
            WasmException.class, () -> limitedManager.createResource("test-type", "resource3"));
      }
    }
  }

  @Nested
  @DisplayName("Resource Retrieval Tests")
  class ResourceRetrievalTests {

    private int resourceId;

    @BeforeEach
    void createTestResource() throws WasmException {
      final WitResourceManager.ResourceTypeInfo typeInfo =
          new WitResourceManager.ResourceTypeInfo("test-type", String.class, null);
      manager.registerResourceType("test-type", typeInfo);
      resourceId = manager.createResource("test-type", "test-value");
    }

    @Test
    @DisplayName("getResource should return managed resource")
    void getResourceShouldReturnManagedResource() throws WasmException {
      final WitResourceManager.ManagedResource resource = manager.getResource(resourceId);

      assertNotNull(resource);
      assertEquals(resourceId, resource.getResourceId());
      assertEquals("test-type", resource.getTypeName());
      assertEquals("test-value", resource.getResource());
    }

    @Test
    @DisplayName("getResource should throw for unknown resource")
    void getResourceShouldThrowForUnknownResource() {
      assertThrows(WasmException.class, () -> manager.getResource(99999));
    }

    @Test
    @DisplayName("getResourceValue should return typed resource")
    void getResourceValueShouldReturnTypedResource() throws WasmException {
      final String value = manager.getResourceValue(resourceId, String.class);

      assertEquals("test-value", value);
    }

    @Test
    @DisplayName("getResourceValue should throw for wrong type")
    void getResourceValueShouldThrowForWrongType() {
      assertThrows(
          WasmException.class, () -> manager.getResourceValue(resourceId, Integer.class));
    }
  }

  @Nested
  @DisplayName("Reference Counting Tests")
  class ReferenceCountingTests {

    private int resourceId;

    @BeforeEach
    void createTestResource() throws WasmException {
      final WitResourceManager.ResourceTypeInfo typeInfo =
          new WitResourceManager.ResourceTypeInfo("test-type", String.class, null);
      manager.registerResourceType("test-type", typeInfo);
      resourceId = manager.createResource("test-type", "test-value");
    }

    @Test
    @DisplayName("incrementRefCount should increase count")
    void incrementRefCountShouldIncreaseCount() throws WasmException {
      manager.incrementRefCount(resourceId);

      final WitResourceManager.ManagedResource resource = manager.getResource(resourceId);
      assertEquals(2, resource.getRefCount());
    }

    @Test
    @DisplayName("decrementRefCount should decrease count")
    void decrementRefCountShouldDecreaseCount() throws WasmException {
      manager.incrementRefCount(resourceId);
      final boolean destroyed = manager.decrementRefCount(resourceId);

      assertFalse(destroyed);
      final WitResourceManager.ManagedResource resource = manager.getResource(resourceId);
      assertEquals(1, resource.getRefCount());
    }

    @Test
    @DisplayName("decrementRefCount should destroy when count reaches zero")
    void decrementRefCountShouldDestroyWhenCountReachesZero() throws WasmException {
      final boolean destroyed = manager.decrementRefCount(resourceId);

      assertTrue(destroyed);
      assertEquals(0, manager.getActiveResourceCount());
    }

    @Test
    @DisplayName("incrementRefCount should throw for unknown resource")
    void incrementRefCountShouldThrowForUnknownResource() {
      assertThrows(WasmException.class, () -> manager.incrementRefCount(99999));
    }

    @Test
    @DisplayName("decrementRefCount should throw for unknown resource")
    void decrementRefCountShouldThrowForUnknownResource() {
      assertThrows(WasmException.class, () -> manager.decrementRefCount(99999));
    }
  }

  @Nested
  @DisplayName("Resource Destruction Tests")
  class ResourceDestructionTests {

    @Test
    @DisplayName("destroyResource should remove resource")
    void destroyResourceShouldRemoveResource() throws WasmException {
      final WitResourceManager.ResourceTypeInfo typeInfo =
          new WitResourceManager.ResourceTypeInfo("test-type", String.class, null);
      manager.registerResourceType("test-type", typeInfo);
      final int resourceId = manager.createResource("test-type", "test-value");

      assertEquals(1, manager.getActiveResourceCount());

      final boolean destroyed = manager.destroyResource(resourceId);

      assertTrue(destroyed);
      assertEquals(0, manager.getActiveResourceCount());
    }

    @Test
    @DisplayName("destroyResource should call destructor")
    void destroyResourceShouldCallDestructor() throws WasmException {
      final AtomicBoolean destructorCalled = new AtomicBoolean(false);
      final WitResourceManager.ResourceDestructor destructor =
          resource -> destructorCalled.set(true);
      final WitResourceManager.ResourceTypeInfo typeInfo =
          new WitResourceManager.ResourceTypeInfo("test-type", String.class, destructor);
      manager.registerResourceType("test-type", typeInfo);
      final int resourceId = manager.createResource("test-type", "test-value");

      manager.destroyResource(resourceId);

      assertTrue(destructorCalled.get());
    }

    @Test
    @DisplayName("destroyResource should return false for non-existent resource")
    void destroyResourceShouldReturnFalseForNonExistentResource() throws WasmException {
      final boolean destroyed = manager.destroyResource(99999);

      assertFalse(destroyed);
    }
  }

  @Nested
  @DisplayName("Active Resources Tests")
  class ActiveResourcesTests {

    @BeforeEach
    void registerTestType() {
      final WitResourceManager.ResourceTypeInfo typeInfo =
          new WitResourceManager.ResourceTypeInfo("test-type", String.class, null);
      manager.registerResourceType("test-type", typeInfo);
    }

    @Test
    @DisplayName("getActiveResourceIds should return all active IDs")
    void getActiveResourceIdsShouldReturnAllActiveIds() throws WasmException {
      final int id1 = manager.createResource("test-type", "resource1");
      final int id2 = manager.createResource("test-type", "resource2");

      final Set<Integer> activeIds = manager.getActiveResourceIds();

      assertEquals(2, activeIds.size());
      assertTrue(activeIds.contains(id1));
      assertTrue(activeIds.contains(id2));
    }

    @Test
    @DisplayName("getActiveResourceCount should return correct count")
    void getActiveResourceCountShouldReturnCorrectCount() throws WasmException {
      assertEquals(0, manager.getActiveResourceCount());

      manager.createResource("test-type", "resource1");
      assertEquals(1, manager.getActiveResourceCount());

      manager.createResource("test-type", "resource2");
      assertEquals(2, manager.getActiveResourceCount());
    }

    @Test
    @DisplayName("getActiveResourceCount by type should return correct count")
    void getActiveResourceCountByTypeShouldReturnCorrectCount() throws WasmException {
      final WitResourceManager.ResourceTypeInfo typeInfo2 =
          new WitResourceManager.ResourceTypeInfo("other-type", Integer.class, null);
      manager.registerResourceType("other-type", typeInfo2);

      manager.createResource("test-type", "resource1");
      manager.createResource("test-type", "resource2");
      manager.createResource("other-type", 123);

      assertEquals(2, manager.getActiveResourceCount("test-type"));
      assertEquals(1, manager.getActiveResourceCount("other-type"));
      assertEquals(0, manager.getActiveResourceCount("unknown-type"));
    }
  }

  @Nested
  @DisplayName("ManagedResource Tests")
  class ManagedResourceTests {

    @Test
    @DisplayName("ManagedResource should have correct initial state")
    void managedResourceShouldHaveCorrectInitialState() throws WasmException {
      final WitResourceManager.ResourceTypeInfo typeInfo =
          new WitResourceManager.ResourceTypeInfo("test-type", String.class, null);
      manager.registerResourceType("test-type", typeInfo);
      final int resourceId = manager.createResource("test-type", "test-value");

      final WitResourceManager.ManagedResource resource = manager.getResource(resourceId);

      assertEquals(resourceId, resource.getResourceId());
      assertEquals("test-type", resource.getTypeName());
      assertEquals("test-value", resource.getResource());
      assertEquals(1, resource.getRefCount());
      assertTrue(resource.getCreatedTime() > 0);
      assertTrue(resource.getLastAccessedTime() > 0);
      assertNotNull(resource.getTypeInfo());
    }

    @Test
    @DisplayName("ManagedResource updateLastAccessed should update timestamp")
    void managedResourceUpdateLastAccessedShouldUpdateTimestamp()
        throws WasmException, InterruptedException {
      final WitResourceManager.ResourceTypeInfo typeInfo =
          new WitResourceManager.ResourceTypeInfo("test-type", String.class, null);
      manager.registerResourceType("test-type", typeInfo);
      final int resourceId = manager.createResource("test-type", "test-value");

      final WitResourceManager.ManagedResource resource = manager.getResource(resourceId);
      final long initialTime = resource.getLastAccessedTime();

      Thread.sleep(10);
      resource.updateLastAccessed();

      assertTrue(resource.getLastAccessedTime() > initialTime);
    }

    @Test
    @DisplayName("ManagedResource incrementRefCount should work")
    void managedResourceIncrementRefCountShouldWork() throws WasmException {
      final WitResourceManager.ResourceTypeInfo typeInfo =
          new WitResourceManager.ResourceTypeInfo("test-type", String.class, null);
      manager.registerResourceType("test-type", typeInfo);
      final int resourceId = manager.createResource("test-type", "test-value");

      final WitResourceManager.ManagedResource resource = manager.getResource(resourceId);
      assertEquals(1, resource.getRefCount());

      final int newCount = resource.incrementRefCount();
      assertEquals(2, newCount);
      assertEquals(2, resource.getRefCount());
    }

    @Test
    @DisplayName("ManagedResource decrementRefCount should work")
    void managedResourceDecrementRefCountShouldWork() throws WasmException {
      final WitResourceManager.ResourceTypeInfo typeInfo =
          new WitResourceManager.ResourceTypeInfo("test-type", String.class, null);
      manager.registerResourceType("test-type", typeInfo);
      final int resourceId = manager.createResource("test-type", "test-value");

      final WitResourceManager.ManagedResource resource = manager.getResource(resourceId);
      resource.incrementRefCount();
      assertEquals(2, resource.getRefCount());

      final int newCount = resource.decrementRefCount();
      assertEquals(1, newCount);
      assertEquals(1, resource.getRefCount());
    }
  }

  @Nested
  @DisplayName("ResourceUsageStats Tests")
  class ResourceUsageStatsTests {

    @Test
    @DisplayName("should create ResourceUsageStats with all fields")
    void shouldCreateResourceUsageStatsWithAllFields() {
      final Map<String, Long> typeCounts = Map.of("type1", 5L, "type2", 3L);
      final WitResourceManager.ResourceUsageStats stats =
          new WitResourceManager.ResourceUsageStats(8, 15, 7, 100, typeCounts);

      assertEquals(8, stats.getActiveResources());
      assertEquals(15, stats.getTotalCreated());
      assertEquals(7, stats.getTotalDestroyed());
      assertEquals(100, stats.getMaxResources());
      assertEquals(2, stats.getResourceTypeCounts().size());
      assertEquals(5L, stats.getResourceTypeCounts().get("type1"));
      assertEquals(3L, stats.getResourceTypeCounts().get("type2"));
    }

    @Test
    @DisplayName("getUsageStats should return current statistics")
    void getUsageStatsShouldReturnCurrentStatistics() throws WasmException {
      final WitResourceManager.ResourceTypeInfo typeInfo =
          new WitResourceManager.ResourceTypeInfo("test-type", String.class, null);
      manager.registerResourceType("test-type", typeInfo);
      manager.createResource("test-type", "resource1");
      manager.createResource("test-type", "resource2");

      final WitResourceManager.ResourceUsageStats stats = manager.getUsageStats();

      assertEquals(2, stats.getActiveResources());
      assertEquals(2, stats.getTotalCreated());
      assertEquals(0, stats.getTotalDestroyed());
      assertEquals(10000, stats.getMaxResources());
      assertEquals(1, stats.getResourceTypeCounts().size());
      assertEquals(2L, stats.getResourceTypeCounts().get("test-type"));
    }
  }

  @Nested
  @DisplayName("Cleanup Tests")
  class CleanupTests {

    @Test
    @DisplayName("performCleanup should return cleaned up count")
    void performCleanupShouldReturnCleanedUpCount() {
      final int cleanedUp = manager.performCleanup();

      // Initially should be 0 since no orphaned resources
      assertEquals(0, cleanedUp);
    }

    @Test
    @DisplayName("performCleanup should not fail on empty manager")
    void performCleanupShouldNotFailOnEmptyManager() {
      // Should not throw
      final int cleanedUp = manager.performCleanup();
      assertTrue(cleanedUp >= 0);
    }
  }

  @Nested
  @DisplayName("Close Tests")
  class CloseTests {

    @Test
    @DisplayName("close should mark manager as closed")
    void closeShouldMarkManagerAsClosed() {
      assertFalse(manager.isClosed());

      manager.close();

      assertTrue(manager.isClosed());
    }

    @Test
    @DisplayName("close should be idempotent")
    void closeShouldBeIdempotent() {
      manager.close();
      assertTrue(manager.isClosed());

      // Second close should not throw
      manager.close();
      assertTrue(manager.isClosed());
    }

    @Test
    @DisplayName("close should destroy all resources")
    void closeShouldDestroyAllResources() throws WasmException {
      final AtomicBoolean destroyed1 = new AtomicBoolean(false);
      final AtomicBoolean destroyed2 = new AtomicBoolean(false);

      final WitResourceManager.ResourceTypeInfo typeInfo =
          new WitResourceManager.ResourceTypeInfo(
              "test-type",
              String.class,
              resource -> {
                if ("resource1".equals(resource)) {
                  destroyed1.set(true);
                } else if ("resource2".equals(resource)) {
                  destroyed2.set(true);
                }
              });
      manager.registerResourceType("test-type", typeInfo);
      manager.createResource("test-type", "resource1");
      manager.createResource("test-type", "resource2");

      manager.close();

      assertTrue(destroyed1.get());
      assertTrue(destroyed2.get());
    }

    @Test
    @DisplayName("operations should throw after close")
    void operationsShouldThrowAfterClose() {
      final WitResourceManager.ResourceTypeInfo typeInfo =
          new WitResourceManager.ResourceTypeInfo("test-type", String.class, null);
      manager.registerResourceType("test-type", typeInfo);

      manager.close();

      assertThrows(
          IllegalStateException.class, () -> manager.createResource("test-type", "resource"));
    }
  }

  @Nested
  @DisplayName("ResourceDestructor Interface Tests")
  class ResourceDestructorInterfaceTests {

    @Test
    @DisplayName("ResourceDestructor should be a functional interface")
    void resourceDestructorShouldBeFunctionalInterface() {
      assertTrue(WitResourceManager.ResourceDestructor.class.isInterface());
      assertTrue(
          WitResourceManager.ResourceDestructor.class.isAnnotationPresent(
              FunctionalInterface.class));
    }

    @Test
    @DisplayName("ResourceDestructor should have destroy method")
    void resourceDestructorShouldHaveDestroyMethod() throws NoSuchMethodException {
      assertNotNull(WitResourceManager.ResourceDestructor.class.getMethod("destroy", Object.class));
    }

    @Test
    @DisplayName("should use lambda as ResourceDestructor")
    void shouldUseLambdaAsResourceDestructor() throws WasmException {
      final AtomicBoolean destroyed = new AtomicBoolean(false);

      final WitResourceManager.ResourceDestructor destructor = resource -> destroyed.set(true);

      final WitResourceManager.ResourceTypeInfo typeInfo =
          new WitResourceManager.ResourceTypeInfo("test-type", String.class, destructor);
      manager.registerResourceType("test-type", typeInfo);
      final int resourceId = manager.createResource("test-type", "test-value");

      manager.destroyResource(resourceId);

      assertTrue(destroyed.get());
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WitResourceManager should be final")
    void witResourceManagerShouldBeFinal() {
      assertTrue(java.lang.reflect.Modifier.isFinal(WitResourceManager.class.getModifiers()));
    }

    @Test
    @DisplayName("WitResourceManager should implement AutoCloseable")
    void witResourceManagerShouldImplementAutoCloseable() {
      assertTrue(AutoCloseable.class.isAssignableFrom(WitResourceManager.class));
    }

    @Test
    @DisplayName("ManagedResource should be final")
    void managedResourceShouldBeFinal() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(
              WitResourceManager.ManagedResource.class.getModifiers()));
    }

    @Test
    @DisplayName("ResourceConfig should be final")
    void resourceConfigShouldBeFinal() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(
              WitResourceManager.ResourceConfig.class.getModifiers()));
    }

    @Test
    @DisplayName("ResourceTypeInfo should be final")
    void resourceTypeInfoShouldBeFinal() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(
              WitResourceManager.ResourceTypeInfo.class.getModifiers()));
    }

    @Test
    @DisplayName("ResourceUsageStats should be final")
    void resourceUsageStatsShouldBeFinal() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(
              WitResourceManager.ResourceUsageStats.class.getModifiers()));
    }
  }
}
