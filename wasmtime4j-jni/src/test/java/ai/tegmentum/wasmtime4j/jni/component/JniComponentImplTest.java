package ai.tegmentum.wasmtime4j.jni.component;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentLinker;
import ai.tegmentum.wasmtime4j.component.ComponentMetadata;
import ai.tegmentum.wasmtime4j.component.ComponentType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.JniEngine;
import ai.tegmentum.wasmtime4j.jni.JniStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test suite for JNI-based Component implementation.
 *
 * <p>Tests the JniComponentImpl class to ensure proper component compilation, instantiation, and
 * lifecycle management through the JNI bridge to the native Wasmtime runtime.
 *
 * <p>These tests focus on validating the JNI bridge implementation and ensuring that defensive
 * programming patterns work correctly with real native resources.
 */
@DisplayName("JNI Component Implementation Tests")
class JniComponentImplTest {

  private Engine engine;
  private Store store;

  @BeforeEach
  void setUp() throws WasmException {
    // Create JNI-based engine and store for testing
    engine = new JniEngine();
    store = new JniStore(engine);
  }

  @AfterEach
  void tearDown() {
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
  }

  @Test
  @DisplayName("JniComponentImpl should handle component creation with validation")
  void testComponentCreationWithValidation() throws WasmException {
    // Test basic component creation with proper validation
    final byte[] validComponentBytes = createMinimalComponent();

    assertDoesNotThrow(
        () -> {
          final Component component = new JniComponentImpl(engine, validComponentBytes);
          assertNotNull(component);
          assertTrue(component.isValid());
        });
  }

  @Test
  @DisplayName("JniComponentImpl should reject invalid component bytes")
  void testComponentCreationWithInvalidBytes() {
    // Test that invalid bytes are properly rejected
    final byte[] invalidBytes = new byte[] {0x00, 0x01, 0x02, 0x03}; // Not a valid component

    assertThrows(
        WasmException.class,
        () -> {
          new JniComponentImpl(engine, invalidBytes);
        });
  }

  @Test
  @DisplayName("JniComponentImpl should reject null parameters")
  void testComponentCreationWithNullParameters() {
    final byte[] validBytes = createMinimalComponent();

    // Test null engine
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new JniComponentImpl(null, validBytes);
        });

    // Test null bytes
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new JniComponentImpl(engine, null);
        });
  }

  @Test
  @DisplayName("JniComponentImpl should reject empty component bytes")
  void testComponentCreationWithEmptyBytes() {
    // Test that empty bytes are properly rejected
    final byte[] emptyBytes = new byte[0];

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new JniComponentImpl(engine, emptyBytes);
        });
  }

  @Test
  @DisplayName("JniComponentImpl should provide component metadata")
  void testComponentMetadata() throws WasmException {
    final byte[] componentBytes = createMinimalComponent();
    final Component component = new JniComponentImpl(engine, componentBytes);

    assertDoesNotThrow(
        () -> {
          final ComponentMetadata metadata = component.getMetadata();
          assertNotNull(metadata);
          assertTrue(metadata.getSize() > 0);
          assertTrue(metadata.getComplexityScore() >= 0);
        });
  }

  @Test
  @DisplayName("JniComponentImpl should provide component type information")
  void testComponentType() throws WasmException {
    final byte[] componentBytes = createMinimalComponent();
    final Component component = new JniComponentImpl(engine, componentBytes);

    assertDoesNotThrow(
        () -> {
          final ComponentType type = component.getType();
          assertNotNull(type);
          // The type should be valid even for minimal components
          assertTrue(type.getImports().size() >= 0);
          assertTrue(type.getExports().size() >= 0);
        });
  }

  @Test
  @DisplayName("JniComponentImpl should support component instantiation")
  void testComponentInstantiation() throws WasmException {
    final byte[] componentBytes = createMinimalComponent();
    final Component component = new JniComponentImpl(engine, componentBytes);

    assertDoesNotThrow(
        () -> {
          final ComponentLinker linker = ComponentLinker.create(engine);
          final ComponentInstance instance = component.instantiate(store, linker);
          assertNotNull(instance);
        });
  }

  @Test
  @DisplayName("JniComponentImpl instantiate should validate parameters")
  void testComponentInstantiationParameterValidation() throws WasmException {
    final byte[] componentBytes = createMinimalComponent();
    final Component component = new JniComponentImpl(engine, componentBytes);
    final ComponentLinker linker = ComponentLinker.create(engine);

    // Test null store
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          component.instantiate(null, linker);
        });

    // Test null linker
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          component.instantiate(store, null);
        });
  }

  @Test
  @DisplayName("JniComponentImpl should handle component serialization")
  void testComponentSerialization() throws WasmException {
    final byte[] componentBytes = createMinimalComponent();
    final Component component = new JniComponentImpl(engine, componentBytes);

    assertDoesNotThrow(
        () -> {
          final byte[] serialized = component.serialize();
          assertNotNull(serialized);
          assertTrue(serialized.length > 0);

          // Serialized bytes should be valid for creating a new component
          final Component deserializedComponent = new JniComponentImpl(engine, serialized);
          assertNotNull(deserializedComponent);
        });
  }

  @Test
  @DisplayName("JniComponentImpl should support component validation")
  void testComponentValidation() throws WasmException {
    final byte[] componentBytes = createMinimalComponent();
    final Component component = new JniComponentImpl(engine, componentBytes);

    assertDoesNotThrow(
        () -> {
          component.validate();
          // If validation doesn't throw, the component is valid
          assertTrue(component.isValid());
        });
  }

  @Test
  @DisplayName("JniComponentImpl should handle resource cleanup properly")
  void testComponentResourceCleanup() throws WasmException {
    final byte[] componentBytes = createMinimalComponent();
    Component component = new JniComponentImpl(engine, componentBytes);

    assertTrue(component.isValid());

    // Close the component
    component.close();

    // Component should no longer be valid
    assertFalse(component.isValid());

    // Operations on closed component should fail
    assertThrows(
        IllegalStateException.class,
        () -> {
          component.getType();
        });

    assertThrows(
        IllegalStateException.class,
        () -> {
          component.getMetadata();
        });
  }

  @Test
  @DisplayName("JniComponentImpl should handle concurrent access safely")
  void testComponentConcurrentAccess() throws WasmException, InterruptedException {
    final byte[] componentBytes = createMinimalComponent();
    final Component component = new JniComponentImpl(engine, componentBytes);

    // Test concurrent access to component metadata
    final int threadCount = 10;
    final Thread[] threads = new Thread[threadCount];
    final boolean[] results = new boolean[threadCount];

    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] =
          new Thread(
              () -> {
                try {
                  final ComponentMetadata metadata = component.getMetadata();
                  final ComponentType type = component.getType();
                  results[index] = metadata != null && type != null;
                } catch (final Exception e) {
                  results[index] = false;
                }
              });
      threads[i].start();
    }

    // Wait for all threads to complete
    for (final Thread thread : threads) {
      thread.join();
    }

    // All threads should have succeeded
    for (final boolean result : results) {
      assertTrue(result, "Concurrent access to component should be safe");
    }
  }

  @Test
  @DisplayName("JniComponentImpl should handle engine lifecycle properly")
  void testComponentEngineLifecycle() throws WasmException {
    final byte[] componentBytes = createMinimalComponent();
    final Component component = new JniComponentImpl(engine, componentBytes);

    assertTrue(component.isValid());

    // Close the engine
    engine.close();

    // Component should detect engine closure and become invalid
    assertFalse(component.isValid());

    // Operations should fail gracefully
    assertThrows(
        IllegalStateException.class,
        () -> {
          component.getType();
        });
  }

  @Test
  @DisplayName("JniComponentImpl should provide meaningful error messages")
  void testComponentErrorMessages() {
    final byte[] invalidBytes = new byte[] {0x00, 0x61, 0x73, 0x6d}; // Partial WASM header

    final WasmException exception =
        assertThrows(
            WasmException.class,
            () -> {
              new JniComponentImpl(engine, invalidBytes);
            });

    // Error message should be informative
    assertNotNull(exception.getMessage());
    assertFalse(exception.getMessage().isEmpty());
    assertTrue(
        exception.getMessage().toLowerCase().contains("component")
            || exception.getMessage().toLowerCase().contains("compilation")
            || exception.getMessage().toLowerCase().contains("invalid"));
  }

  @Test
  @DisplayName("JniComponentImpl should handle large component bytes")
  void testComponentWithLargeBytes() throws WasmException {
    // Create a larger component with some padding
    final byte[] baseComponent = createMinimalComponent();
    final byte[] largeComponent = new byte[baseComponent.length + 1024 * 1024]; // Add 1MB padding
    System.arraycopy(baseComponent, 0, largeComponent, 0, baseComponent.length);

    // This should still work but might take more time
    assertDoesNotThrow(
        () -> {
          final Component component = new JniComponentImpl(engine, largeComponent);
          assertNotNull(component);
        });
  }

  /**
   * Creates a minimal valid WebAssembly component for testing.
   *
   * <p>In a real implementation, this would create an actual valid component. For now, we'll
   * simulate this by creating a minimal WASM module structure.
   */
  private byte[] createMinimalComponent() {
    // This is a simplified representation of a minimal WebAssembly component
    // In reality, this would be a properly formatted component binary
    return new byte[] {
      0x00, 0x61, 0x73, 0x6d, // WASM magic number
      0x0d, 0x00, 0x01, 0x00, // Component version (hypothetical)
      0x01, 0x00, 0x00, 0x00, // Minimal component sections
      0x00 // End marker
    };
  }

  /** Creates a component from a file if available for more realistic testing. */
  private byte[] loadComponentFromFile(final String filename) {
    try {
      final Path path = Paths.get("src/test/resources/components/" + filename);
      if (Files.exists(path)) {
        return Files.readAllBytes(path);
      }
    } catch (final IOException e) {
      // Fall back to minimal component
    }
    return createMinimalComponent();
  }
}
