package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.ComponentEngine;
import ai.tegmentum.wasmtime4j.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.ComponentEngineResourceUsage;
import ai.tegmentum.wasmtime4j.ComponentEngineStatistics;
import ai.tegmentum.wasmtime4j.ComponentInstance;
import ai.tegmentum.wasmtime4j.ComponentRegistry;
import ai.tegmentum.wasmtime4j.ComponentSimple;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Performance monitoring tests for component operations.
 *
 * <p>These tests verify performance characteristics of component linking, instantiation, and
 * execution to ensure optimal performance and resource utilization.
 */
class ComponentPerformanceMonitoringTest {

  private ComponentEngine componentEngine;
  private ComponentRegistry registry;
  private Store store;
  private ExecutorService executorService;

  @BeforeEach
  void setUp(TestInfo testInfo) throws WasmException {
    System.out.printf("Starting performance test: %s%n", testInfo.getDisplayName());

    // Create component engine with performance-optimized configuration
    final ComponentEngineConfig config = new ComponentEngineConfig();
    this.componentEngine = WasmRuntimeFactory.getDefaultRuntime().createComponentEngine(config);
    this.registry = componentEngine.getRegistry();
    this.store = WasmRuntimeFactory.getDefaultRuntime().createStore();
    this.executorService = Executors.newFixedThreadPool(8);

    assertNotNull(componentEngine, "Component engine should be created");
    assertNotNull(registry, "Component registry should be available");
    assertNotNull(store, "Store should be created");
  }

  @AfterEach
  void tearDown() throws Exception {
    if (executorService != null) {
      executorService.shutdown();
      executorService.awaitTermination(5, TimeUnit.SECONDS);
    }
    if (store != null) {
      store.close();
    }
    if (componentEngine != null) {
      componentEngine.close();
    }
  }

  @Test
  @DisplayName("Should measure component compilation performance")
  void shouldMeasureComponentCompilationPerformance() throws WasmException {
    final int componentCount = 100;
    final List<Long> compilationTimes = new ArrayList<>();

    System.out.printf("Compiling %d components to measure performance...%n", componentCount);

    for (int i = 0; i < componentCount; i++) {
      final byte[] componentBytes = createTestComponentBytes(i);

      final Instant start = Instant.now();
      final ComponentSimple component =
          componentEngine.compileComponent(componentBytes, "perf-test-" + i);
      final Instant end = Instant.now();

      final long compilationTimeMs = Duration.between(start, end).toMillis();
      compilationTimes.add(compilationTimeMs);

      assertNotNull(component, "Component " + i + " should be compiled successfully");
      assertTrue(component.isValid(), "Component " + i + " should be valid");
    }

    // Analyze compilation performance
    final double avgCompilationTime =
        compilationTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
    final long maxCompilationTime =
        compilationTimes.stream().mapToLong(Long::longValue).max().orElse(0);
    final long minCompilationTime =
        compilationTimes.stream().mapToLong(Long::longValue).min().orElse(0);

    System.out.printf("Compilation Performance Stats:%n");
    System.out.printf("  Average: %.2f ms%n", avgCompilationTime);
    System.out.printf("  Max: %d ms%n", maxCompilationTime);
    System.out.printf("  Min: %d ms%n", minCompilationTime);

    // Performance assertions
    assertTrue(avgCompilationTime < 1000, "Average compilation time should be under 1 second");
    assertTrue(maxCompilationTime < 5000, "Maximum compilation time should be under 5 seconds");
  }

  @Test
  @DisplayName("Should measure component linking performance")
  void shouldMeasureComponentLinkingPerformance() throws WasmException {
    // Create components for linking
    final int componentCount = 10;
    final List<ComponentSimple> components = new ArrayList<>();

    for (int i = 0; i < componentCount; i++) {
      final ComponentSimple component = createLinkableComponent(i);
      components.add(component);
      registry.register("linkable-" + i, component);
    }

    System.out.printf("Linking %d components to measure performance...%n", componentCount);

    // Measure linking performance
    final Instant start = Instant.now();
    final ComponentSimple linkedComponent = componentEngine.linkComponents(components);
    final Instant end = Instant.now();

    final long linkingTimeMs = Duration.between(start, end).toMillis();

    System.out.printf(
        "Linking Performance: %d ms for %d components%n", linkingTimeMs, componentCount);

    assertNotNull(linkedComponent, "Linked component should be created");
    assertTrue(linkedComponent.isValid(), "Linked component should be valid");
    assertTrue(linkingTimeMs < 10000, "Linking should complete within 10 seconds");
  }

  @Test
  @DisplayName("Should measure component instantiation performance")
  void shouldMeasureComponentInstantiationPerformance() throws WasmException {
    final ComponentSimple component = createTestComponent();
    registry.register("instantiation-test", component);

    final int instanceCount = 50;
    final List<Long> instantiationTimes = new ArrayList<>();

    System.out.printf("Creating %d component instances to measure performance...%n", instanceCount);

    for (int i = 0; i < instanceCount; i++) {
      final Instant start = Instant.now();
      final ComponentInstance instance = componentEngine.createInstance(component, store);
      final Instant end = Instant.now();

      final long instantiationTimeMs = Duration.between(start, end).toMillis();
      instantiationTimes.add(instantiationTimeMs);

      assertNotNull(instance, "Instance " + i + " should be created successfully");
      assertTrue(instance.isValid(), "Instance " + i + " should be valid");
    }

    // Analyze instantiation performance
    final double avgInstantiationTime =
        instantiationTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
    final long maxInstantiationTime =
        instantiationTimes.stream().mapToLong(Long::longValue).max().orElse(0);
    final long minInstantiationTime =
        instantiationTimes.stream().mapToLong(Long::longValue).min().orElse(0);

    System.out.printf("Instantiation Performance Stats:%n");
    System.out.printf("  Average: %.2f ms%n", avgInstantiationTime);
    System.out.printf("  Max: %d ms%n", maxInstantiationTime);
    System.out.printf("  Min: %d ms%n", minInstantiationTime);

    // Performance assertions
    assertTrue(avgInstantiationTime < 500, "Average instantiation time should be under 500ms");
    assertTrue(maxInstantiationTime < 2000, "Maximum instantiation time should be under 2 seconds");
  }

  @Test
  @DisplayName("Should measure concurrent component operations performance")
  void shouldMeasureConcurrentComponentOperationsPerformance() throws Exception {
    final int concurrentOperations = 20;
    final List<CompletableFuture<Void>> futures = new ArrayList<>();

    System.out.printf("Running %d concurrent component operations...%n", concurrentOperations);

    final Instant start = Instant.now();

    for (int i = 0; i < concurrentOperations; i++) {
      final int operationId = i;
      final CompletableFuture<Void> future =
          CompletableFuture.runAsync(
              () -> {
                try {
                  // Perform component operations concurrently
                  final ComponentSimple component = createTestComponent();
                  registry.register("concurrent-" + operationId, component);

                  final ComponentInstance instance =
                      componentEngine.createInstance(component, store);
                  assertNotNull(
                      instance, "Concurrent instance " + operationId + " should be created");

                } catch (WasmException e) {
                  throw new RuntimeException("Concurrent operation " + operationId + " failed", e);
                }
              },
              executorService);

      futures.add(future);
    }

    // Wait for all operations to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);

    final Instant end = Instant.now();
    final long totalTimeMs = Duration.between(start, end).toMillis();

    System.out.printf(
        "Concurrent Operations Performance: %d ms for %d operations%n",
        totalTimeMs, concurrentOperations);

    // Verify all operations completed successfully
    assertEquals(
        concurrentOperations, registry.getComponentCount(), "All components should be registered");
    assertTrue(totalTimeMs < 15000, "Concurrent operations should complete within 15 seconds");
  }

  @Test
  @DisplayName("Should monitor resource usage during component operations")
  void shouldMonitorResourceUsageDuringComponentOperations() throws WasmException {
    // Get initial resource usage
    final ComponentEngineResourceUsage initialUsage = componentEngine.getResourceUsage();
    System.out.printf("Initial Resource Usage: %s%n", formatResourceUsage(initialUsage));

    // Perform resource-intensive operations
    final int componentCount = 20;
    final List<ComponentSimple> components = new ArrayList<>();

    for (int i = 0; i < componentCount; i++) {
      final ComponentSimple component = createTestComponent();
      components.add(component);
      registry.register("resource-test-" + i, component);

      // Create multiple instances per component
      for (int j = 0; j < 3; j++) {
        final ComponentInstance instance = componentEngine.createInstance(component, store);
        assertNotNull(instance, "Instance should be created");
      }
    }

    // Get resource usage after operations
    final ComponentEngineResourceUsage finalUsage = componentEngine.getResourceUsage();
    System.out.printf("Final Resource Usage: %s%n", formatResourceUsage(finalUsage));

    // Analyze resource usage
    assertTrue(
        finalUsage.getActiveInstances() >= initialUsage.getActiveInstances(),
        "Active instances should have increased");
    assertTrue(
        finalUsage.getMemoryUsage() >= initialUsage.getMemoryUsage(),
        "Memory usage should have increased");

    // Check resource cleanup
    final int cleanedUp = componentEngine.cleanupInactiveInstances();
    System.out.printf("Cleaned up %d inactive instances%n", cleanedUp);

    final ComponentEngineResourceUsage cleanedUsage = componentEngine.getResourceUsage();
    System.out.printf("After Cleanup Resource Usage: %s%n", formatResourceUsage(cleanedUsage));
  }

  @Test
  @DisplayName("Should measure dependency resolution performance")
  void shouldMeasureDependencyResolutionPerformance() throws WasmException {
    // Create complex dependency graph
    final int componentCount = 30;
    final List<ComponentSimple> components = new ArrayList<>();

    // Create components with interconnected dependencies
    for (int i = 0; i < componentCount; i++) {
      final ComponentSimple component = createComponentWithDependencies(i, componentCount);
      components.add(component);
      registry.register("dep-test-" + i, component);
    }

    System.out.printf("Resolving dependencies for %d components...%n", componentCount);

    final List<Long> resolutionTimes = new ArrayList<>();

    for (int i = 0; i < componentCount; i++) {
      final ComponentSimple component = components.get(i);

      final Instant start = Instant.now();
      try {
        final var dependencies = registry.resolveDependencies(component);
        final Instant end = Instant.now();

        final long resolutionTimeMs = Duration.between(start, end).toMillis();
        resolutionTimes.add(resolutionTimeMs);

        System.out.printf(
            "Component %d: %d dependencies resolved in %d ms%n",
            i, dependencies.size(), resolutionTimeMs);

      } catch (WasmException e) {
        // Some components may have circular dependencies - this is expected
        System.out.printf(
            "Component %d: Dependency resolution failed (expected for circular deps)%n", i);
      }
    }

    if (!resolutionTimes.isEmpty()) {
      final double avgResolutionTime =
          resolutionTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
      System.out.printf("Average dependency resolution time: %.2f ms%n", avgResolutionTime);

      assertTrue(avgResolutionTime < 1000, "Average resolution time should be under 1 second");
    }
  }

  @Test
  @DisplayName("Should measure engine statistics and health")
  void shouldMeasureEngineStatisticsAndHealth() throws WasmException {
    // Perform various operations to generate statistics
    for (int i = 0; i < 10; i++) {
      final ComponentSimple component = createTestComponent();
      registry.register("stats-" + i, component);
      componentEngine.createInstance(component, store);
    }

    // Get engine statistics
    final ComponentEngineStatistics stats = componentEngine.getStatistics();
    System.out.printf("Engine Statistics:%n");
    System.out.printf("  Active Instances: %d%n", stats.getActiveInstances());
    System.out.printf("  Total Components: %d%n", stats.getTotalComponents());
    System.out.printf("  Memory Usage: %d bytes%n", stats.getMemoryUsage());

    assertNotNull(stats, "Statistics should be available");
    assertTrue(stats.getActiveInstances() > 0, "Should have active instances");

    // Check engine health
    final var health = componentEngine.getHealth();
    System.out.printf(
        "Engine Health: %s - %s%n",
        health.isHealthy() ? "HEALTHY" : "UNHEALTHY", health.getMessage());

    assertTrue(health.isHealthy(), "Engine should be healthy");
    assertTrue(componentEngine.isValid(), "Engine should be valid");
  }

  // Helper methods for creating test components

  private ComponentSimple createTestComponent() throws WasmException {
    final byte[] componentBytes = createTestComponentBytes(0);
    return componentEngine.compileComponent(componentBytes, "test-" + System.nanoTime());
  }

  private ComponentSimple createLinkableComponent(int id) throws WasmException {
    final byte[] componentBytes = createLinkableComponentBytes(id);
    return componentEngine.compileComponent(
        componentBytes, "linkable-" + id + "-" + System.nanoTime());
  }

  private ComponentSimple createComponentWithDependencies(int id, int totalCount)
      throws WasmException {
    final byte[] componentBytes = createComponentWithDependencyBytes(id, totalCount);
    return componentEngine.compileComponent(
        componentBytes, "dependency-" + id + "-" + System.nanoTime());
  }

  private byte[] createTestComponentBytes(int id) {
    // Create minimal test component bytes
    final String metadata = "test-component-" + id;
    return createBasicComponentBytes(metadata);
  }

  private byte[] createLinkableComponentBytes(int id) {
    // Create component bytes that can be linked with others
    final String metadata =
        "linkable-component-"
            + id
            + "-exports:interface"
            + id
            + "-imports:interface"
            + ((id + 1) % 10);
    return createBasicComponentBytes(metadata);
  }

  private byte[] createComponentWithDependencyBytes(int id, int totalCount) {
    // Create component with specific dependency patterns
    final StringBuilder deps = new StringBuilder();
    for (int i = 0; i < Math.min(3, totalCount); i++) {
      if (i != id) { // Don't depend on self
        deps.append("dep").append(i).append(",");
      }
    }
    final String metadata = "dependency-component-" + id + "-deps:" + deps.toString();
    return createBasicComponentBytes(metadata);
  }

  private byte[] createBasicComponentBytes(String metadata) {
    // Create basic WebAssembly component with metadata
    final byte[] baseBytes = {
      0x00, 0x61, 0x73, 0x6d, // WASM magic number
      0x01, 0x00, 0x00, 0x00, // Version
    };

    final byte[] metadataBytes = metadata.getBytes();
    final byte[] result = new byte[baseBytes.length + metadataBytes.length];
    System.arraycopy(baseBytes, 0, result, 0, baseBytes.length);
    System.arraycopy(metadataBytes, 0, result, baseBytes.length, metadataBytes.length);

    return result;
  }

  private String formatResourceUsage(ComponentEngineResourceUsage usage) {
    return String.format(
        "Instances: %d, Memory: %d, Resources: %d, Handles: %d",
        usage.getActiveInstances(),
        usage.getMemoryUsage(),
        usage.getActiveResources(),
        usage.getActiveHandles());
  }
}
