package ai.tegmentum.wasmtime4j.wasi.impl;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.WasiResource;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceLimits;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceManager;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceMetadata;
import ai.tegmentum.wasmtime4j.wasi.WasiResourcePermissions;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceType;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceUsageStats;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for WASI Resource Manager implementation.
 *
 * <p>Tests the WasiResourceManagerImpl class to ensure proper resource lifecycle management, thread
 * safety, permission enforcement, and integration with different resource types.
 *
 * <p>These tests validate both the basic functionality and advanced scenarios including concurrent
 * access, resource limits, and error handling.
 */
@DisplayName("WASI Resource Manager Implementation Tests")
class WasiResourceManagerImplTest {

  private WasiResourceManager resourceManager;
  private TestResourceConfig testConfig;
  private WasiResourceLimits defaultLimits;

  @BeforeEach
  void setUp() throws WasmException {
    // Create default resource limits for testing
    defaultLimits = createTestResourceLimits();

    // Create resource manager with test limits
    resourceManager = WasiResourceManager.create(defaultLimits);

    // Create test configuration
    testConfig = new TestResourceConfig();
  }

  @AfterEach
  void tearDown() {
    if (resourceManager != null) {
      resourceManager.close();
    }
  }

  @Test
  @DisplayName("Resource manager should create and track resources correctly")
  void testResourceCreationAndTracking() throws WasmException {
    // Create a filesystem resource
    final WasiResource resource =
        resourceManager.createResource(
            "test-filesystem", WasiResource.class, createFilesystemConfig());

    assertNotNull(resource);
    assertEquals("test-filesystem", ((WasiGenericResourceImpl) resource).getName());
    assertTrue(resource.isValid());

    // Verify resource is tracked
    assertTrue(resourceManager.hasResource("test-filesystem"));
    assertEquals(1, resourceManager.getActiveResourceCount());

    // Retrieve the resource
    final Optional<WasiResource> retrieved = resourceManager.getResource("test-filesystem");
    assertTrue(retrieved.isPresent());
    assertEquals(resource.getId(), retrieved.get().getId());
  }

  @Test
  @DisplayName("Resource manager should handle different resource types")
  void testDifferentResourceTypes() throws WasmException {
    // Create filesystem resource
    final WasiResource filesystemResource =
        resourceManager.createResource("filesystem", WasiResource.class, createFilesystemConfig());

    // Create socket resource
    final WasiResource socketResource =
        resourceManager.createResource("socket", WasiResource.class, createSocketConfig());

    // Create timer resource
    final WasiResource timerResource =
        resourceManager.createResource("timer", WasiResource.class, createTimerConfig());

    // Verify all resources are created and tracked
    assertEquals(3, resourceManager.getActiveResourceCount());
    assertTrue(resourceManager.hasResource("filesystem"));
    assertTrue(resourceManager.hasResource("socket"));
    assertTrue(resourceManager.hasResource("timer"));

    // Verify resource types
    assertEquals("filesystem", filesystemResource.getType());
    assertEquals("network", socketResource.getType());
    assertEquals("time", timerResource.getType());
  }

  @Test
  @DisplayName("Resource manager should enforce resource limits")
  void testResourceLimitsEnforcement() throws WasmException {
    // Create a resource manager with strict limits
    final WasiResourceLimits strictLimits = createStrictResourceLimits();
    final WasiResourceManager limitedManager = WasiResourceManager.create(strictLimits);

    try {
      // Should be able to create up to the limit
      for (int i = 0; i < strictLimits.getMaxResources(); i++) {
        limitedManager.createResource(
            "resource-" + i, WasiResource.class, createFilesystemConfig());
      }

      assertEquals(strictLimits.getMaxResources(), limitedManager.getActiveResourceCount());

      // Creating one more should fail
      assertThrows(
          WasmException.class,
          () -> {
            limitedManager.createResource(
                "resource-overflow", WasiResource.class, createFilesystemConfig());
          });
    } finally {
      limitedManager.close();
    }
  }

  @Test
  @DisplayName("Resource manager should handle resource release correctly")
  void testResourceRelease() throws WasmException {
    // Create multiple resources
    final WasiResource resource1 =
        resourceManager.createResource("resource1", WasiResource.class, createFilesystemConfig());

    final WasiResource resource2 =
        resourceManager.createResource("resource2", WasiResource.class, createSocketConfig());

    assertEquals(2, resourceManager.getActiveResourceCount());

    // Release first resource by name
    resourceManager.releaseResource("resource1");
    assertEquals(1, resourceManager.getActiveResourceCount());
    assertFalse(resourceManager.hasResource("resource1"));
    assertTrue(resourceManager.hasResource("resource2"));

    // Release second resource by reference
    resourceManager.releaseResource(resource2);
    assertEquals(0, resourceManager.getActiveResourceCount());
    assertFalse(resourceManager.hasResource("resource2"));
  }

  @Test
  @DisplayName("Resource manager should provide accurate usage statistics")
  void testUsageStatistics() throws WasmException {
    final WasiResourceUsageStats initialStats = resourceManager.getUsageStats();
    assertEquals(0, initialStats.getTotalResourcesCreated());
    assertEquals(0, initialStats.getCurrentActiveResources());

    // Create some resources
    resourceManager.createResource("fs1", WasiResource.class, createFilesystemConfig());
    resourceManager.createResource("fs2", WasiResource.class, createFilesystemConfig());
    resourceManager.createResource("sock1", WasiResource.class, createSocketConfig());

    final WasiResourceUsageStats afterCreation = resourceManager.getUsageStats();
    assertEquals(3, afterCreation.getTotalResourcesCreated());
    assertEquals(3, afterCreation.getCurrentActiveResources());

    // Release one resource
    resourceManager.releaseResource("fs1");

    final WasiResourceUsageStats afterRelease = resourceManager.getUsageStats();
    assertEquals(3, afterRelease.getTotalResourcesCreated());
    assertEquals(1, afterRelease.getTotalResourcesReleased());
    assertEquals(2, afterRelease.getCurrentActiveResources());
  }

  @Test
  @DisplayName("Resource manager should handle concurrent access safely")
  void testConcurrentAccess() throws InterruptedException, WasmException {
    final int threadCount = 10;
    final int resourcesPerThread = 5;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final CountDownLatch latch = new CountDownLatch(threadCount);

    // Create resources concurrently
    for (int i = 0; i < threadCount; i++) {
      final int threadId = i;
      executor.submit(
          () -> {
            try {
              for (int j = 0; j < resourcesPerThread; j++) {
                final String resourceName = "thread-" + threadId + "-resource-" + j;
                resourceManager.createResource(
                    resourceName, WasiResource.class, createFilesystemConfig());
              }
            } catch (final Exception e) {
              fail("Concurrent resource creation failed: " + e.getMessage());
            } finally {
              latch.countDown();
            }
          });
    }

    // Wait for all threads to complete
    assertTrue(latch.await(10, TimeUnit.SECONDS));
    executor.shutdown();

    // Verify all resources were created
    assertEquals(threadCount * resourcesPerThread, resourceManager.getActiveResourceCount());

    // Verify resource manager state is consistent
    final WasiResourceUsageStats stats = resourceManager.getUsageStats();
    assertEquals(threadCount * resourcesPerThread, stats.getTotalResourcesCreated());
    assertEquals(threadCount * resourcesPerThread, stats.getCurrentActiveResources());
  }

  @Test
  @DisplayName("Resource manager should validate resources correctly")
  void testResourceValidation() throws WasmException {
    // Create valid resources
    resourceManager.createResource("valid1", WasiResource.class, createFilesystemConfig());
    resourceManager.createResource("valid2", WasiResource.class, createSocketConfig());

    // Validation should pass for valid resources
    assertDoesNotThrow(
        () -> {
          resourceManager.validateResources();
        });

    // Close one resource externally
    final Optional<WasiResource> resource = resourceManager.getResource("valid1");
    assertTrue(resource.isPresent());
    resource.get().close();

    // Validation should detect the invalid resource
    assertThrows(
        WasmException.class,
        () -> {
          resourceManager.validateResources();
        });
  }

  @Test
  @DisplayName("Resource manager should cleanup inactive resources")
  void testResourceCleanup() throws WasmException {
    // Create resources
    resourceManager.createResource("resource1", WasiResource.class, createFilesystemConfig());
    resourceManager.createResource("resource2", WasiResource.class, createSocketConfig());

    assertEquals(2, resourceManager.getActiveResourceCount());

    // Close one resource externally to make it invalid
    final Optional<WasiResource> resource = resourceManager.getResource("resource1");
    assertTrue(resource.isPresent());
    resource.get().close();

    // Cleanup should remove the invalid resource
    final int cleanedUp = resourceManager.cleanupResources();
    assertEquals(1, cleanedUp);
    assertEquals(1, resourceManager.getActiveResourceCount());
    assertFalse(resourceManager.hasResource("resource1"));
    assertTrue(resourceManager.hasResource("resource2"));
  }

  @Test
  @DisplayName("Resource manager should provide resource metadata")
  void testResourceMetadata() throws WasmException {
    // Create resources with different configurations
    resourceManager.createResource("fs-resource", WasiResource.class, createFilesystemConfig());
    resourceManager.createResource("net-resource", WasiResource.class, createSocketConfig());

    final Map<String, WasiResourceMetadata> metadata = resourceManager.getResourceMetadata();
    assertEquals(2, metadata.size());

    assertTrue(metadata.containsKey("fs-resource"));
    assertTrue(metadata.containsKey("net-resource"));

    // Verify metadata content
    final WasiResourceMetadata fsMetadata = metadata.get("fs-resource");
    assertEquals("fs-resource", fsMetadata.getName());
    assertEquals("filesystem", fsMetadata.getType());
    assertTrue(fsMetadata.getSize() > 0);

    final WasiResourceMetadata netMetadata = metadata.get("net-resource");
    assertEquals("net-resource", netMetadata.getName());
    assertEquals("network", netMetadata.getType());
    assertTrue(netMetadata.getSize() > 0);
  }

  @Test
  @DisplayName("Resource manager should handle parameter validation")
  void testParameterValidation() {
    // Test null parameters
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          resourceManager.createResource(null, WasiResource.class, testConfig);
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          resourceManager.createResource("", WasiResource.class, testConfig);
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          resourceManager.createResource("test", null, testConfig);
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          resourceManager.createResource("test", WasiResource.class, null);
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          resourceManager.getResource(null);
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          resourceManager.releaseResource((String) null);
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          resourceManager.releaseResource((WasiResource) null);
        });
  }

  @Test
  @DisplayName("Resource manager should handle duplicate resource names")
  void testDuplicateResourceNames() throws WasmException {
    // Create first resource
    resourceManager.createResource("duplicate", WasiResource.class, createFilesystemConfig());

    // Creating another resource with the same name should fail
    assertThrows(
        WasmException.class,
        () -> {
          resourceManager.createResource("duplicate", WasiResource.class, createSocketConfig());
        });
  }

  @Test
  @DisplayName("Resource manager should handle resource type filtering")
  void testResourceTypeFiltering() throws WasmException {
    // Create different types of resources
    resourceManager.createResource("fs1", WasiResource.class, createFilesystemConfig());
    resourceManager.createResource("fs2", WasiResource.class, createFilesystemConfig());
    resourceManager.createResource("sock1", WasiResource.class, createSocketConfig());

    // Get all resources
    final List<WasiResource> allResources = resourceManager.getActiveResources();
    assertEquals(3, allResources.size());

    // Get resources by type (this is tested through the interface, actual filtering
    // depends on implementation details)
    final List<WasiResource> typedResources =
        resourceManager.getActiveResources(WasiResource.class);
    assertEquals(3, typedResources.size()); // All are WasiResource instances
  }

  @Test
  @DisplayName("Resource manager should handle close properly")
  void testResourceManagerClose() throws WasmException {
    // Create resources
    resourceManager.createResource("resource1", WasiResource.class, createFilesystemConfig());
    resourceManager.createResource("resource2", WasiResource.class, createSocketConfig());

    assertTrue(resourceManager.isValid());
    assertEquals(2, resourceManager.getActiveResourceCount());

    // Close the manager
    resourceManager.close();

    // Manager should be invalid
    assertFalse(resourceManager.isValid());

    // Operations should fail
    assertThrows(
        IllegalStateException.class,
        () -> {
          resourceManager.createResource("new", WasiResource.class, testConfig);
        });

    assertThrows(
        IllegalStateException.class,
        () -> {
          resourceManager.getActiveResources();
        });
  }

  /** Creates test resource limits for testing. */
  private WasiResourceLimits createTestResourceLimits() {
    return new WasiResourceLimits() {
      @Override
      public int getMaxResources() {
        return 100;
      }

      @Override
      public long getMaxMemoryPerResource() {
        return 1024 * 1024; // 1MB
      }

      @Override
      public long getTotalMaxMemory() {
        return 100 * 1024 * 1024; // 100MB
      }

      @Override
      public int getMaxResourcesPerType() {
        return 50;
      }

      @Override
      public Map<String, Object> getCustomLimits() {
        return new HashMap<>();
      }
    };
  }

  /** Creates strict resource limits for testing limit enforcement. */
  private WasiResourceLimits createStrictResourceLimits() {
    return new WasiResourceLimits() {
      @Override
      public int getMaxResources() {
        return 3; // Very low limit for testing
      }

      @Override
      public long getMaxMemoryPerResource() {
        return 1024;
      }

      @Override
      public long getTotalMaxMemory() {
        return 3 * 1024;
      }

      @Override
      public int getMaxResourcesPerType() {
        return 2;
      }

      @Override
      public Map<String, Object> getCustomLimits() {
        return new HashMap<>();
      }
    };
  }

  /** Creates filesystem resource configuration for testing. */
  private WasiResourceConfig createFilesystemConfig() {
    return new TestResourceConfig(WasiResourceType.FILESYSTEM, "/tmp");
  }

  /** Creates socket resource configuration for testing. */
  private WasiResourceConfig createSocketConfig() {
    return new TestResourceConfig(WasiResourceType.NETWORK, "localhost:8080");
  }

  /** Creates timer resource configuration for testing. */
  private WasiResourceConfig createTimerConfig() {
    return new TestResourceConfig(WasiResourceType.TIME, "monotonic");
  }

  /** Test implementation of WasiResourceConfig for testing purposes. */
  private static class TestResourceConfig implements WasiResourceConfig {
    private final WasiResourceType resourceType;
    private final String name;
    private final Map<String, Object> properties = new HashMap<>();

    public TestResourceConfig() {
      this(WasiResourceType.CUSTOM, "test-resource");
    }

    public TestResourceConfig(final WasiResourceType resourceType, final String configValue) {
      this.resourceType = resourceType;
      this.name = "test-" + resourceType.getName();

      // Set type-specific properties
      switch (resourceType) {
        case FILESYSTEM:
          properties.put("root_path", configValue);
          break;
        case NETWORK:
          final String[] hostPort = configValue.split(":");
          properties.put("host", hostPort[0]);
          properties.put("port", Integer.parseInt(hostPort[1]));
          properties.put("socket_type", "TCP");
          break;
        case TIME:
          properties.put("timer_type", configValue);
          properties.put("resolution_ms", 1);
          properties.put("allow_scheduling", true);
          break;
        default:
          properties.put("value", configValue);
          break;
      }
    }

    @Override
    public WasiResourceType getResourceType() {
      return resourceType;
    }

    @Override
    public Optional<String> getName() {
      return Optional.of(name);
    }

    @Override
    public Optional<Object> getProperty(final String name) {
      return Optional.ofNullable(properties.get(name));
    }

    @Override
    public Map<String, Object> getProperties() {
      return new HashMap<>(properties);
    }

    @Override
    public boolean hasProperty(final String name) {
      return properties.containsKey(name);
    }

    @Override
    public WasiResourcePermissions getPermissions() {
      return resourceType.getDefaultPermissions();
    }

    @Override
    public WasiResourceLimits getResourceLimits() {
      return new WasiResourceLimits() {
        @Override
        public int getMaxResources() {
          return 10;
        }

        @Override
        public long getMaxMemoryPerResource() {
          return 1024 * 1024;
        }

        @Override
        public long getTotalMaxMemory() {
          return 10 * 1024 * 1024;
        }

        @Override
        public int getMaxResourcesPerType() {
          return 5;
        }

        @Override
        public Map<String, Object> getCustomLimits() {
          return new HashMap<>();
        }
      };
    }

    @Override
    public Map<String, Object> getMetadata() {
      final Map<String, Object> metadata = new HashMap<>();
      metadata.put("test_config", true);
      metadata.put("resource_type", resourceType.getName());
      return metadata;
    }

    @Override
    public void validate() {
      if (resourceType == null) {
        throw new IllegalArgumentException("Resource type cannot be null");
      }
      // Additional validation would go here
    }

    @Override
    public boolean isCompatibleWith(final WasiResourceConfig other) {
      if (other == null) {
        throw new IllegalArgumentException("Other config cannot be null");
      }
      return resourceType == other.getResourceType();
    }
  }
}
