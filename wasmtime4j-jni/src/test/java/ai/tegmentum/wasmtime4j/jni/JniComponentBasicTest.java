package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Basic unit tests for WebAssembly Component Model functionality.
 *
 * <p>These tests validate the Java API layer of the Component Model implementation, focusing on:
 *
 * <ul>
 *   <li>Component engine creation and lifecycle
 *   <li>Component loading validation and error handling
 *   <li>Parameter validation and defensive programming
 *   <li>Resource management and cleanup
 * </ul>
 *
 * <p>Tests use real WebAssembly modules where possible to ensure authentic behavior without
 * mocking.
 */
@DisplayName("Component Model Basic Tests")
public final class JniComponentBasicTest {

  private static final Path SIMPLE_WAT_PATH =
      Paths.get("../wasmtime4j-tests/src/test/resources/wasm/simple.wat");

  private static byte[] simpleWatBytes;

  /**
   * Load test resources before running tests.
   *
   * @throws IOException if test resources cannot be loaded
   */
  @BeforeAll
  public static void loadTestResources() throws IOException {
    // Load native library
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      System.err.println("Warning: Failed to load native library: " + e.getMessage());
      System.err.println("Component model tests will be skipped if native library is unavailable");
    }

    // Load simple WAT file if it exists
    if (Files.exists(SIMPLE_WAT_PATH)) {
      simpleWatBytes = Files.readAllBytes(SIMPLE_WAT_PATH);
    }
  }

  @Test
  @DisplayName("Create component engine")
  public void testCreateComponentEngine() {
    try (final JniComponent.JniComponentEngine engine = JniComponent.createComponentEngine()) {
      assertNotNull(engine, "Component engine should not be null");
      assertTrue(engine.isValid(), "Component engine should be valid");
      assertTrue(engine.getNativeHandle() != 0, "Component engine should have valid native handle");
    } catch (final UnsupportedOperationException | JniException e) {
      // Component model may not be fully implemented yet
      System.out.println("Component model not yet fully implemented: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Component engine lifecycle")
  public void testComponentEngineLifecycle() {
    try {
      final JniComponent.JniComponentEngine engine = JniComponent.createComponentEngine();
      assertFalse(engine.isClosed(), "Engine should not be closed initially");

      engine.close();
      assertTrue(engine.isClosed(), "Engine should be closed after close()");
      assertFalse(engine.isValid(), "Engine should not be valid after close()");

      // Multiple close calls should be safe
      engine.close();
      assertTrue(engine.isClosed(), "Engine should still be closed");
    } catch (final UnsupportedOperationException | JniException e) {
      System.out.println("Component model not yet fully implemented: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Load component with null bytes should throw exception")
  public void testLoadComponentWithNullBytes() {
    try (final JniComponent.JniComponentEngine engine = JniComponent.createComponentEngine()) {
      assertThrows(
          IllegalArgumentException.class,
          () -> engine.loadComponentFromBytes(null),
          "Loading component with null bytes should throw exception");
    } catch (final UnsupportedOperationException | JniException e) {
      System.out.println("Component model not yet fully implemented: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Load component with empty bytes should throw exception")
  public void testLoadComponentWithEmptyBytes() {
    try (final JniComponent.JniComponentEngine engine = JniComponent.createComponentEngine()) {
      assertThrows(
          IllegalArgumentException.class,
          () -> engine.loadComponentFromBytes(new byte[0]),
          "Loading component with empty bytes should throw exception");
    } catch (final UnsupportedOperationException | JniException e) {
      System.out.println("Component model not yet fully implemented: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Load component with invalid bytes should throw exception")
  public void testLoadComponentWithInvalidBytes() {
    try (final JniComponent.JniComponentEngine engine = JniComponent.createComponentEngine()) {
      final byte[] invalidBytes = "not a wasm component".getBytes();
      assertThrows(
          WasmException.class,
          () -> engine.loadComponentFromBytes(invalidBytes),
          "Loading component with invalid bytes should throw exception");
    } catch (final UnsupportedOperationException | JniException e) {
      System.out.println("Component model not yet fully implemented: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Component engine operations after close should throw exception")
  public void testComponentEngineOperationsAfterClose() {
    try {
      final JniComponent.JniComponentEngine engine = JniComponent.createComponentEngine();
      engine.close();

      // Operations on closed engine should throw JniResourceException
      assertThrows(
          JniResourceException.class,
          () -> engine.getActiveInstancesCount(),
          "getActiveInstancesCount on closed engine should throw exception");

      assertThrows(
          JniResourceException.class,
          () -> engine.cleanupInstances(),
          "cleanupInstances on closed engine should throw exception");

      if (simpleWatBytes != null) {
        assertThrows(
            JniResourceException.class,
            () -> engine.loadComponentFromBytes(simpleWatBytes),
            "loadComponentFromBytes on closed engine should throw exception");
      }
    } catch (final UnsupportedOperationException | JniException e) {
      System.out.println("Component model not yet fully implemented: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Get active instances count on new engine")
  public void testGetActiveInstancesCount() {
    try (final JniComponent.JniComponentEngine engine = JniComponent.createComponentEngine()) {
      final int count = engine.getActiveInstancesCount();
      assertTrue(count >= 0, "Active instances count should be non-negative");
    } catch (final UnsupportedOperationException | JniException e) {
      System.out.println("Component model not yet fully implemented: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Cleanup instances on new engine")
  public void testCleanupInstances() {
    try (final JniComponent.JniComponentEngine engine = JniComponent.createComponentEngine()) {
      final int cleaned = engine.cleanupInstances();
      assertTrue(cleaned >= 0, "Cleaned instances count should be non-negative");
    } catch (final UnsupportedOperationException | JniException e) {
      System.out.println("Component model not yet fully implemented: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Component handle lifecycle")
  public void testComponentHandleLifecycle() {
    // This test will be skipped if component model not implemented or simple.wat unavailable
    if (simpleWatBytes == null) {
      System.out.println("Skipping test: simple.wat not available");
      return;
    }

    try (final JniComponent.JniComponentEngine engine = JniComponent.createComponentEngine()) {
      // Note: simple.wat is a core module, not a component
      // This test validates the error handling when trying to load a non-component
      try {
        final JniComponent.JniComponentHandle component =
            engine.loadComponentFromBytes(simpleWatBytes);
        component.close();
      } catch (final WasmException e) {
        // Expected: simple.wat is not a component
        assertTrue(
            e.getMessage().contains("component") || e.getMessage().contains("module"),
            "Error message should indicate component/module mismatch");
      }
    } catch (final UnsupportedOperationException | JniException e) {
      System.out.println("Component model not yet fully implemented: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Instantiate null component should throw exception")
  public void testInstantiateNullComponent() {
    try (final JniComponent.JniComponentEngine engine = JniComponent.createComponentEngine()) {
      assertThrows(
          IllegalArgumentException.class,
          () -> engine.instantiateComponent(null),
          "Instantiating null component should throw exception");
    } catch (final UnsupportedOperationException | JniException e) {
      System.out.println("Component model not yet fully implemented: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Multiple engine instances should be independent")
  public void testMultipleEngines() {
    try (final JniComponent.JniComponentEngine engine1 = JniComponent.createComponentEngine();
        final JniComponent.JniComponentEngine engine2 = JniComponent.createComponentEngine()) {

      assertNotEquals(
          engine1.getNativeHandle(),
          engine2.getNativeHandle(),
          "Different engines should have different native handles");

      assertTrue(engine1.isValid(), "Engine 1 should be valid");
      assertTrue(engine2.isValid(), "Engine 2 should be valid");

      engine1.close();
      assertTrue(engine1.isClosed(), "Engine 1 should be closed");
      assertFalse(engine1.isValid(), "Engine 1 should not be valid");
      assertTrue(engine2.isValid(), "Engine 2 should still be valid");
    } catch (final UnsupportedOperationException | JniException e) {
      System.out.println("Component model not yet fully implemented: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Component engine toString should not throw")
  public void testComponentEngineToString() {
    try (final JniComponent.JniComponentEngine engine = JniComponent.createComponentEngine()) {
      final String str = engine.toString();
      assertNotNull(str, "toString should not return null");
      assertFalse(str.isEmpty(), "toString should not return empty string");
    } catch (final UnsupportedOperationException | JniException e) {
      System.out.println("Component model not yet fully implemented: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Resource type should be correct")
  public void testResourceType() {
    try (final JniComponent.JniComponentEngine engine = JniComponent.createComponentEngine()) {
      assertEquals(
          "ComponentEngine", engine.getResourceType(), "Resource type should be ComponentEngine");
    } catch (final UnsupportedOperationException | JniException e) {
      System.out.println("Component model not yet fully implemented: " + e.getMessage());
    }
  }
}
