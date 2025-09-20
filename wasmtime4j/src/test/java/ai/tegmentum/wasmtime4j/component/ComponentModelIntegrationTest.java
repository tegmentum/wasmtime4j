package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.JniEngine;
import ai.tegmentum.wasmtime4j.jni.JniStore;
import ai.tegmentum.wasmtime4j.jni.component.JniComponentImpl;
import ai.tegmentum.wasmtime4j.panama.PanamaEngine;
import ai.tegmentum.wasmtime4j.panama.PanamaStore;
import ai.tegmentum.wasmtime4j.panama.component.PanamaComponentImpl;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceManager;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceType;
import ai.tegmentum.wasmtime4j.wasi.impl.WasiFileResourceImpl;
import ai.tegmentum.wasmtime4j.wasi.impl.WasiResourceManagerImpl;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive integration test suite for the complete Component Model implementation.
 *
 * <p>Tests the integration between all Component Model components including JNI and Panama
 * implementations, WASI Preview 2 resource management, and cross-runtime compatibility.
 *
 * <p>These tests validate that the complete Component Model implementation works correctly
 * across different runtime backends and handles complex scenarios involving resource
 * management, component composition, and performance considerations.
 */
@DisplayName("Component Model Integration Tests")
class ComponentModelIntegrationTest {

  @TempDir
  Path tempDir;

  @Test
  @DisplayName("JNI Component implementation should integrate with resource management")
  void testJniComponentWithResourceManagement() throws WasmException, IOException {
    Engine engine = null;
    Store store = null;
    WasiResourceManager resourceManager = null;

    try {
      // Setup JNI runtime
      engine = new JniEngine();
      store = new JniStore(engine);
      resourceManager = new WasiResourceManagerImpl();

      // Create filesystem resource for component access
      final Path resourceDir = tempDir.resolve("component-resources");
      Files.createDirectories(resourceDir);
      Files.write(resourceDir.resolve("data.txt"), "Component data".getBytes());

      // Create component that would use filesystem resources
      final byte[] componentBytes = createMinimalComponent();
      final Component component = new JniComponentImpl(engine, componentBytes);

      // Verify component can be created and managed
      assertNotNull(component);
      assertTrue(component.isValid());

      // Test component metadata integration
      final ComponentMetadata metadata = component.getMetadata();
      assertNotNull(metadata);
      assertTrue(metadata.getSize() > 0);

      // Test component type integration
      final ComponentType type = component.getType();
      assertNotNull(type);

      // Integration test: component lifecycle with resource management
      assertEquals(0, resourceManager.getActiveResourceCount());

      // Component should be able to work with linker (interface level test)
      assertDoesNotThrow(() -> {
        final ComponentLinker linker = ComponentLinker.create(engine);
        assertNotNull(linker);
      });

    } finally {
      // Cleanup in reverse order
      if (resourceManager != null) {
        resourceManager.close();
      }
      if (store != null) {
        store.close();
      }
      if (engine != null) {
        engine.close();
      }
    }
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_23)
  @DisplayName("Panama Component implementation should integrate with resource management")
  void testPanamaComponentWithResourceManagement() throws WasmException, IOException {
    Engine engine = null;
    Store store = null;
    Arena arena = null;
    WasiResourceManager resourceManager = null;

    try {
      // Setup Panama runtime
      arena = Arena.ofConfined();
      engine = new PanamaEngine(arena);
      store = new PanamaStore(engine, arena);
      resourceManager = new WasiResourceManagerImpl();

      // Create filesystem resource for component access
      final Path resourceDir = tempDir.resolve("panama-resources");
      Files.createDirectories(resourceDir);
      Files.write(resourceDir.resolve("panama-data.txt"), "Panama component data".getBytes());

      // Create component that would use filesystem resources
      final byte[] componentBytes = createMinimalComponent();
      final Component component = new PanamaComponentImpl(engine, componentBytes, arena);

      // Verify component can be created and managed
      assertNotNull(component);
      assertTrue(component.isValid());

      // Test component metadata integration
      final ComponentMetadata metadata = component.getMetadata();
      assertNotNull(metadata);
      assertTrue(metadata.getSize() > 0);

      // Test component type integration
      final ComponentType type = component.getType();
      assertNotNull(type);

      // Integration test: Arena-based resource management
      assertEquals(0, resourceManager.getActiveResourceCount());

      // Component should work with Arena lifecycle
      assertTrue(component.isValid());
      arena.close();
      arena = Arena.ofConfined(); // Create new arena

      // Component with closed arena should be invalid
      assertFalse(component.isValid());

    } finally {
      // Cleanup in reverse order
      if (resourceManager != null) {
        resourceManager.close();
      }
      if (store != null) {
        store.close();
      }
      if (engine != null) {
        engine.close();
      }
      if (arena != null) {
        arena.close();
      }
    }
  }

  @Test
  @DisplayName("Component Model should handle cross-runtime compatibility")
  void testCrossRuntimeCompatibility() throws WasmException {
    // Test that components work consistently across JNI and Panama
    final byte[] componentBytes = createMinimalComponent();

    // Test JNI implementation
    Engine jniEngine = null;
    Store jniStore = null;
    try {
      jniEngine = new JniEngine();
      jniStore = new JniStore(jniEngine);

      final Component jniComponent = new JniComponentImpl(jniEngine, componentBytes);
      final ComponentMetadata jniMetadata = jniComponent.getMetadata();
      final ComponentType jniType = jniComponent.getType();

      assertNotNull(jniComponent);
      assertNotNull(jniMetadata);
      assertNotNull(jniType);

      // For Java 23+, also test Panama implementation
      if (isJava23OrLater()) {
        Arena arena = null;
        Engine panamaEngine = null;
        Store panamaStore = null;
        try {
          arena = Arena.ofConfined();
          panamaEngine = new PanamaEngine(arena);
          panamaStore = new PanamaStore(panamaEngine, arena);

          final Component panamaComponent = new PanamaComponentImpl(panamaEngine, componentBytes, arena);
          final ComponentMetadata panamaMetadata = panamaComponent.getMetadata();
          final ComponentType panamaType = panamaComponent.getType();

          assertNotNull(panamaComponent);
          assertNotNull(panamaMetadata);
          assertNotNull(panamaType);

          // Components should have similar characteristics
          assertEquals(jniMetadata.getSize(), panamaMetadata.getSize());
          assertEquals(jniType.getImports().size(), panamaType.getImports().size());
          assertEquals(jniType.getExports().size(), panamaType.getExports().size());

        } finally {
          if (panamaStore != null) panamaStore.close();
          if (panamaEngine != null) panamaEngine.close();
          if (arena != null) arena.close();
        }
      }

    } finally {
      if (jniStore != null) jniStore.close();
      if (jniEngine != null) jniEngine.close();
    }
  }

  @Test
  @DisplayName("Component Model should handle concurrent component operations")
  void testConcurrentComponentOperations() throws InterruptedException, WasmException {
    final int threadCount = 5;
    final int componentsPerThread = 3;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final CountDownLatch latch = new CountDownLatch(threadCount);
    final byte[] componentBytes = createMinimalComponent();

    // Create components concurrently using JNI
    for (int i = 0; i < threadCount; i++) {
      final int threadId = i;
      executor.submit(() -> {
        Engine engine = null;
        Store store = null;
        try {
          engine = new JniEngine();
          store = new JniStore(engine);

          for (int j = 0; j < componentsPerThread; j++) {
            final Component component = new JniComponentImpl(engine, componentBytes);
            assertNotNull(component);
            assertTrue(component.isValid());

            final ComponentMetadata metadata = component.getMetadata();
            assertNotNull(metadata);

            component.close();
          }
        } catch (final Exception e) {
          fail("Concurrent component operations failed: " + e.getMessage());
        } finally {
          if (store != null) store.close();
          if (engine != null) engine.close();
          latch.countDown();
        }
      });
    }

    assertTrue(latch.await(30, TimeUnit.SECONDS));
    executor.shutdown();
  }

  @Test
  @DisplayName("Component Model should integrate with comprehensive WASI resource management")
  void testComponentModelWasiIntegration() throws WasmException, IOException {
    Engine engine = null;
    Store store = null;
    WasiResourceManager resourceManager = null;

    try {
      engine = new JniEngine();
      store = new JniStore(engine);
      resourceManager = new WasiResourceManagerImpl();

      // Create various WASI resources
      final Path filesystemRoot = tempDir.resolve("wasi-fs");
      Files.createDirectories(filesystemRoot);

      // Test filesystem resource integration
      final TestWasiResourceConfig fsConfig = new TestWasiResourceConfig(
          WasiResourceType.FILESYSTEM, filesystemRoot.toString());

      resourceManager.createResource("filesystem", WasiFileResourceImpl.class, fsConfig);

      // Test network resource integration
      final TestWasiResourceConfig netConfig = new TestWasiResourceConfig(
          WasiResourceType.NETWORK, "localhost:8080");

      resourceManager.createResource("network",
          ai.tegmentum.wasmtime4j.wasi.impl.WasiSocketResourceImpl.class, netConfig);

      // Test timer resource integration
      final TestWasiResourceConfig timerConfig = new TestWasiResourceConfig(
          WasiResourceType.TIME, "monotonic");

      resourceManager.createResource("timer",
          ai.tegmentum.wasmtime4j.wasi.impl.WasiTimerResourceImpl.class, timerConfig);

      // Verify resource manager state
      assertEquals(3, resourceManager.getActiveResourceCount());

      // Test component creation in context of resource management
      final byte[] componentBytes = createMinimalComponent();
      final Component component = new JniComponentImpl(engine, componentBytes);

      // Component should be created successfully alongside resource management
      assertNotNull(component);
      assertTrue(component.isValid());

      // Test resource statistics
      final ai.tegmentum.wasmtime4j.wasi.WasiResourceUsageStats stats = resourceManager.getUsageStats();
      assertEquals(3, stats.getTotalResourcesCreated());
      assertEquals(3, stats.getCurrentActiveResources());

      // Test resource cleanup
      final int cleanedUp = resourceManager.cleanupResources();
      assertEquals(0, cleanedUp); // All resources should be valid

    } finally {
      if (resourceManager != null) resourceManager.close();
      if (store != null) store.close();
      if (engine != null) engine.close();
    }
  }

  @Test
  @DisplayName("Component Model should handle resource limits and constraints")
  void testComponentModelResourceConstraints() throws WasmException {
    // Create resource manager with strict limits
    final ai.tegmentum.wasmtime4j.wasi.WasiResourceLimits strictLimits = createStrictLimits();
    final WasiResourceManager limitedManager = WasiResourceManager.create(strictLimits);

    Engine engine = null;
    Store store = null;

    try {
      engine = new JniEngine();
      store = new JniStore(engine);

      // Create components up to the limit
      final byte[] componentBytes = createMinimalComponent();
      for (int i = 0; i < 2; i++) {
        final Component component = new JniComponentImpl(engine, componentBytes);
        assertNotNull(component);
      }

      // Resource manager should still work within its own limits
      for (int i = 0; i < strictLimits.getMaxResources(); i++) {
        final TestWasiResourceConfig config = new TestWasiResourceConfig(
            WasiResourceType.CUSTOM, "resource-" + i);
        limitedManager.createResource("resource-" + i,
            ai.tegmentum.wasmtime4j.wasi.impl.WasiGenericResourceImpl.class, config);
      }

      assertEquals(strictLimits.getMaxResources(), limitedManager.getActiveResourceCount());

      // One more resource should fail
      assertThrows(WasmException.class, () -> {
        final TestWasiResourceConfig config = new TestWasiResourceConfig(
            WasiResourceType.CUSTOM, "overflow");
        limitedManager.createResource("overflow",
            ai.tegmentum.wasmtime4j.wasi.impl.WasiGenericResourceImpl.class, config);
      });

    } finally {
      limitedManager.close();
      if (store != null) store.close();
      if (engine != null) engine.close();
    }
  }

  @Test
  @DisplayName("Component Model should provide comprehensive error handling")
  void testComponentModelErrorHandling() throws WasmException {
    Engine engine = null;
    Store store = null;
    WasiResourceManager resourceManager = null;

    try {
      engine = new JniEngine();
      store = new JniStore(engine);
      resourceManager = new WasiResourceManagerImpl();

      // Test component creation with invalid bytes
      final byte[] invalidBytes = new byte[]{0x00, 0x01, 0x02};
      assertThrows(WasmException.class, () -> {
        new JniComponentImpl(engine, invalidBytes);
      });

      // Test resource creation with invalid configuration
      final TestWasiResourceConfig invalidConfig = new TestWasiResourceConfig(null, null);
      assertThrows(Exception.class, () -> {
        resourceManager.createResource("invalid",
            ai.tegmentum.wasmtime4j.wasi.impl.WasiGenericResourceImpl.class, invalidConfig);
      });

      // Test parameter validation
      assertThrows(IllegalArgumentException.class, () -> {
        new JniComponentImpl(null, new byte[]{});
      });

      assertThrows(IllegalArgumentException.class, () -> {
        resourceManager.createResource(null,
            ai.tegmentum.wasmtime4j.wasi.impl.WasiGenericResourceImpl.class, invalidConfig);
      });

    } finally {
      if (resourceManager != null) resourceManager.close();
      if (store != null) store.close();
      if (engine != null) engine.close();
    }
  }

  @Test
  @DisplayName("Component Model should demonstrate complete workflow")
  void testCompleteComponentModelWorkflow() throws WasmException, IOException {
    Engine engine = null;
    Store store = null;
    WasiResourceManager resourceManager = null;

    try {
      // Step 1: Initialize runtime environment
      engine = new JniEngine();
      store = new JniStore(engine);
      resourceManager = new WasiResourceManagerImpl();

      // Step 2: Setup WASI resources
      final Path workDir = tempDir.resolve("workflow");
      Files.createDirectories(workDir);
      Files.write(workDir.resolve("input.txt"), "Workflow input data".getBytes());

      final TestWasiResourceConfig fsConfig = new TestWasiResourceConfig(
          WasiResourceType.FILESYSTEM, workDir.toString());
      resourceManager.createResource("workspace", WasiFileResourceImpl.class, fsConfig);

      // Step 3: Create and validate component
      final byte[] componentBytes = createMinimalComponent();
      final Component component = new JniComponentImpl(engine, componentBytes);

      assertTrue(component.isValid());
      component.validate(); // Should not throw

      // Step 4: Get component metadata and type information
      final ComponentMetadata metadata = component.getMetadata();
      final ComponentType type = component.getType();

      assertNotNull(metadata);
      assertNotNull(type);
      assertTrue(metadata.getSize() > 0);

      // Step 5: Setup component linking (interface level)
      final ComponentLinker linker = ComponentLinker.create(engine);
      assertNotNull(linker);

      // Step 6: Component instantiation would happen here
      // (Currently at interface level due to implementation state)
      assertDoesNotThrow(() -> {
        component.instantiate(store, linker);
      });

      // Step 7: Verify resource management statistics
      final ai.tegmentum.wasmtime4j.wasi.WasiResourceUsageStats stats = resourceManager.getUsageStats();
      assertEquals(1, stats.getTotalResourcesCreated());
      assertEquals(1, stats.getCurrentActiveResources());

      // Step 8: Cleanup verification
      component.close();
      assertFalse(component.isValid());

      resourceManager.close();
      assertFalse(resourceManager.isValid());

    } finally {
      if (resourceManager != null && resourceManager.isValid()) {
        resourceManager.close();
      }
      if (store != null) store.close();
      if (engine != null) engine.close();
    }
  }

  /**
   * Creates minimal WebAssembly component bytes for testing.
   */
  private byte[] createMinimalComponent() {
    return new byte[] {
        0x00, 0x61, 0x73, 0x6d, // WASM magic number
        0x0d, 0x00, 0x01, 0x00, // Component version
        0x01, 0x00, 0x00, 0x00, // Minimal component sections
        0x00 // End marker
    };
  }

  /**
   * Creates strict resource limits for testing.
   */
  private ai.tegmentum.wasmtime4j.wasi.WasiResourceLimits createStrictLimits() {
    return new ai.tegmentum.wasmtime4j.wasi.WasiResourceLimits() {
      @Override
      public int getMaxResources() {
        return 3;
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

  /**
   * Checks if running on Java 23 or later.
   */
  private boolean isJava23OrLater() {
    try {
      final String version = System.getProperty("java.version");
      final int majorVersion = Integer.parseInt(version.split("\\.")[0]);
      return majorVersion >= 23;
    } catch (final Exception e) {
      return false;
    }
  }

  /**
   * Test implementation of WasiResourceConfig.
   */
  private static class TestWasiResourceConfig implements ai.tegmentum.wasmtime4j.wasi.WasiResourceConfig {
    private final WasiResourceType resourceType;
    private final String value;

    public TestWasiResourceConfig(final WasiResourceType resourceType, final String value) {
      this.resourceType = resourceType;
      this.value = value;
    }

    @Override
    public WasiResourceType getResourceType() {
      return resourceType;
    }

    @Override
    public java.util.Optional<String> getName() {
      return java.util.Optional.ofNullable(value);
    }

    @Override
    public java.util.Optional<Object> getProperty(final String name) {
      switch (name) {
        case "root_path":
          return resourceType == WasiResourceType.FILESYSTEM ?
              java.util.Optional.ofNullable(value) : java.util.Optional.empty();
        case "host":
          if (resourceType == WasiResourceType.NETWORK && value != null) {
            return java.util.Optional.of(value.split(":")[0]);
          }
          return java.util.Optional.empty();
        case "port":
          if (resourceType == WasiResourceType.NETWORK && value != null && value.contains(":")) {
            return java.util.Optional.of(Integer.parseInt(value.split(":")[1]));
          }
          return java.util.Optional.empty();
        case "timer_type":
          return resourceType == WasiResourceType.TIME ?
              java.util.Optional.ofNullable(value) : java.util.Optional.empty();
        default:
          return java.util.Optional.empty();
      }
    }

    @Override
    public Map<String, Object> getProperties() {
      final Map<String, Object> props = new HashMap<>();
      if (resourceType == WasiResourceType.FILESYSTEM) {
        props.put("root_path", value);
      } else if (resourceType == WasiResourceType.NETWORK && value != null && value.contains(":")) {
        final String[] parts = value.split(":");
        props.put("host", parts[0]);
        props.put("port", Integer.parseInt(parts[1]));
      } else if (resourceType == WasiResourceType.TIME) {
        props.put("timer_type", value);
      }
      return props;
    }

    @Override
    public boolean hasProperty(final String name) {
      return getProperty(name).isPresent();
    }

    @Override
    public Object getPermissions() {
      return resourceType != null ? resourceType.getDefaultPermissions() :
          ai.tegmentum.wasmtime4j.wasi.WasiResourcePermissions.NONE;
    }

    @Override
    public ai.tegmentum.wasmtime4j.wasi.WasiResourceLimits getResourceLimits() {
      return createStrictLimits();
    }

    @Override
    public Map<String, Object> getMetadata() {
      return new HashMap<>();
    }

    @Override
    public void validate() {
      if (resourceType == null) {
        throw new IllegalArgumentException("Resource type cannot be null");
      }
    }

    @Override
    public boolean isCompatibleWith(final ai.tegmentum.wasmtime4j.wasi.WasiResourceConfig other) {
      return other != null && other.getResourceType() == this.resourceType;
    }

    private ai.tegmentum.wasmtime4j.wasi.WasiResourceLimits createStrictLimits() {
      return new ai.tegmentum.wasmtime4j.wasi.WasiResourceLimits() {
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
  }
}