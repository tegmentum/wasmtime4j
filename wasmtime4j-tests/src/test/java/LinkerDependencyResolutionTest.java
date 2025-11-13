package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test suite for Linker dependency resolution and instantiation planning features.
 *
 * <p>Tests resolveDependencies() and createInstantiationPlan() implementations.
 */
@DisplayName("Linker Dependency Resolution Tests")
class LinkerDependencyResolutionTest {

  private Engine engine;
  private Store store;
  private Linker<Void> linker;

  @BeforeEach
  void setUp() throws WasmException {
    engine = Engine.create();
    store = Store.create(engine);
    linker = Linker.create(engine);
  }

  @AfterEach
  void tearDown() {
    if (linker != null) {
      linker.close();
    }
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
  }

  @Test
  @DisplayName("resolveDependencies should throw for null parameter")
  void testResolveDependenciesThrowsForNull() {
    assertThrows(IllegalArgumentException.class, () -> linker.resolveDependencies((Module[]) null));
  }

  @Test
  @DisplayName("resolveDependencies should throw for empty module list")
  void testResolveDependenciesThrowsForEmptyList() {
    assertThrows(IllegalArgumentException.class, () -> linker.resolveDependencies());
  }

  @Test
  @DisplayName("resolveDependencies should handle single module")
  void testResolveDependenciesWithSingleModule() throws WasmException {
    // Create a simple empty WebAssembly module
    final byte[] wasmBytecode =
        new byte[] {
          0x00, 0x61, 0x73, 0x6D, // magic number "\0asm"
          0x01, 0x00, 0x00, 0x00 // version 1
        };

    final Module module = Module.compile(engine, wasmBytecode);
    final DependencyResolution resolution = linker.resolveDependencies(module);

    assertNotNull(resolution);
    assertEquals(1, resolution.getTotalModules());
    assertTrue(resolution.isResolutionSuccessful());
    assertFalse(resolution.hasCircularDependencies());
    assertTrue(resolution.getCircularDependencyChains().isEmpty());
    assertEquals(1, resolution.getInstantiationOrder().size());
    assertNotNull(resolution.getAnalysisTime());
    assertTrue(resolution.getAnalysisTime().compareTo(Duration.ZERO) >= 0);
  }

  @Test
  @DisplayName("resolveDependencies should handle multiple modules")
  void testResolveDependenciesWithMultipleModules() throws WasmException {
    final byte[] wasmBytecode =
        new byte[] {
          0x00, 0x61, 0x73, 0x6D, // magic number "\0asm"
          0x01, 0x00, 0x00, 0x00 // version 1
        };

    final Module module1 = Module.compile(engine, wasmBytecode);
    final Module module2 = Module.compile(engine, wasmBytecode);

    final DependencyResolution resolution = linker.resolveDependencies(module1, module2);

    assertNotNull(resolution);
    assertEquals(2, resolution.getTotalModules());
    assertEquals(2, resolution.getInstantiationOrder().size());
    assertNotNull(resolution.getDependencies());
    assertTrue(resolution.getResolutionRate() >= 0.0);
    assertTrue(resolution.getResolutionRate() <= 100.0);

    // Verify the toString method provides meaningful output
    final String resolutionString = resolution.toString();
    assertTrue(resolutionString.contains("modules=2"));
    assertTrue(resolutionString.contains("successful=" + resolution.isResolutionSuccessful()));
  }

  @Test
  @DisplayName("createInstantiationPlan should throw for null parameter")
  void testCreateInstantiationPlanThrowsForNull() {
    assertThrows(
        IllegalArgumentException.class, () -> linker.createInstantiationPlan((Module[]) null));
  }

  @Test
  @DisplayName("createInstantiationPlan should throw for empty module list")
  void testCreateInstantiationPlanThrowsForEmptyList() {
    assertThrows(IllegalArgumentException.class, () -> linker.createInstantiationPlan());
  }

  @Test
  @DisplayName("createInstantiationPlan should handle single module")
  void testCreateInstantiationPlanWithSingleModule() throws WasmException {
    final byte[] wasmBytecode =
        new byte[] {
          0x00, 0x61, 0x73, 0x6D, // magic number "\0asm"
          0x01, 0x00, 0x00, 0x00 // version 1
        };

    final Module module = Module.compile(engine, wasmBytecode);
    final InstantiationPlan plan = linker.createInstantiationPlan(module);

    assertNotNull(plan);
    assertTrue(plan.isExecutable());
    assertEquals(1, plan.getStepCount());
    assertNotNull(plan.getSteps());
    assertEquals(1, plan.getSteps().size());
    assertNotNull(plan.getDependencyResolution());
    assertNotNull(plan.getPlanningTime());
    assertTrue(plan.getPlanningTime().compareTo(Duration.ZERO) >= 0);

    // Test plan validation
    assertTrue(plan.isValidFor(linker));

    // Test toString method
    final String planString = plan.toString();
    assertTrue(planString.contains("steps=1"));
    assertTrue(planString.contains("executable=true"));
  }

  @Test
  @DisplayName("createInstantiationPlan should create proper steps")
  void testCreateInstantiationPlanSteps() throws WasmException {
    final byte[] wasmBytecode =
        new byte[] {
          0x00, 0x61, 0x73, 0x6D, // magic number "\0asm"
          0x01, 0x00, 0x00, 0x00 // version 1
        };

    final Module module = Module.compile(engine, wasmBytecode);
    final InstantiationPlan plan = linker.createInstantiationPlan(module);

    final List<InstantiationStep> steps = plan.getSteps();
    assertEquals(1, steps.size());

    final InstantiationStep step = steps.get(0);
    assertEquals(1, step.getStepNumber());
    assertEquals(module, step.getModule());
    assertTrue(step.getInstanceName().isPresent());
    assertNotNull(step.getRequiredImports());
    assertNotNull(step.getProvidedExports());
    assertNotNull(step.getDescription());

    // Test step execution capability check
    assertTrue(step.canExecuteWith(linker));

    // Test toString method
    final String stepString = step.toString();
    assertTrue(stepString.contains("step=1"));
  }

  @Test
  @DisplayName("createInstantiationPlan should execute successfully")
  void testInstantiationPlanExecution() throws WasmException {
    final byte[] wasmBytecode =
        new byte[] {
          0x00, 0x61, 0x73, 0x6D, // magic number "\0asm"
          0x01, 0x00, 0x00, 0x00 // version 1
        };

    final Module module = Module.compile(engine, wasmBytecode);
    final InstantiationPlan plan = linker.createInstantiationPlan(module);

    // Execute the plan
    final List<Instance> instances = plan.execute(linker, store);
    assertNotNull(instances);
    assertEquals(1, instances.size());

    final Instance instance = instances.get(0);
    assertNotNull(instance);
    assertTrue(instance.isValid());

    // Clean up
    instance.close();
  }

  @Test
  @DisplayName("DependencyEdge should work correctly")
  void testDependencyEdge() throws WasmException {
    final byte[] wasmBytecode =
        new byte[] {
          0x00, 0x61, 0x73, 0x6D,
          0x01, 0x00, 0x00, 0x00
        };

    final Module module1 = Module.compile(engine, wasmBytecode);
    final Module module2 = Module.compile(engine, wasmBytecode);

    final DependencyEdge edge =
        new DependencyEdge(
            module1, module2, "env", "test_function", DependencyEdge.DependencyType.FUNCTION, true);

    assertEquals(module1, edge.getDependent());
    assertEquals(module2, edge.getDependency());
    assertEquals("env", edge.getImportModule());
    assertEquals("test_function", edge.getImportName());
    assertEquals(DependencyEdge.DependencyType.FUNCTION, edge.getDependencyType());
    assertTrue(edge.isResolved());

    final String depString = edge.getDependencyString();
    assertNotNull(depString);
    assertTrue(depString.contains("env::test_function"));

    final String toString = edge.toString();
    assertNotNull(toString);
    assertTrue(toString.contains("FUNCTION"));
    assertTrue(toString.contains("resolved=true"));
  }

  @Test
  @DisplayName("methods should handle closed linker gracefully")
  void testClosedLinkerHandling() throws WasmException {
    final byte[] wasmBytecode =
        new byte[] {
          0x00, 0x61, 0x73, 0x6D, // magic number "\0asm"
          0x01, 0x00, 0x00, 0x00 // version 1
        };

    final Module module = Module.compile(engine, wasmBytecode);
    linker.close();

    assertThrows(IllegalStateException.class, () -> linker.resolveDependencies(module));
    assertThrows(IllegalStateException.class, () -> linker.createInstantiationPlan(module));
  }
}
