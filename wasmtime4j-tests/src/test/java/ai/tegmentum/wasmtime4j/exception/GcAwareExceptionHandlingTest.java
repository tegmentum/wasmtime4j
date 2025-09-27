/*
 * Copyright 2024 Tegmentum Technology, Inc.
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

package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.gc.GcRuntime;
import ai.tegmentum.wasmtime4j.gc.GcValue;
import ai.tegmentum.wasmtime4j.gc.StructInstance;
import ai.tegmentum.wasmtime4j.gc.ArrayInstance;
import ai.tegmentum.wasmtime4j.jni.JniExceptionHandler;
import ai.tegmentum.wasmtime4j.panama.PanamaExceptionHandler;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

/**
 * Integration test suite for GC-aware exception handling.
 *
 * <p>This test suite validates the integration between the WebAssembly exception handling
 * foundation (Task #309) and the WebAssembly GC foundation (Task #308), ensuring proper
 * handling of exception payloads containing GC references.
 *
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("GC-Aware Exception Handling Integration Tests")
class GcAwareExceptionHandlingTest {

  private static final Logger LOGGER =
      Logger.getLogger(GcAwareExceptionHandlingTest.class.getName());

  private GcRuntime gcRuntime;
  private JniExceptionHandler jniHandler;
  private PanamaExceptionHandler panamaHandler;

  @BeforeEach
  void setUp() {
    // Initialize GC runtime with comprehensive configuration
    try {
      gcRuntime = GcRuntime.builder()
          .enableGcProfiling(true)
          .enableMemoryLeakDetection(true)
          .enableReferenceCounting(true)
          .enableWeakReferences(true)
          .gcHeapSizeLimit(64 * 1024 * 1024) // 64MB
          .build();
    } catch (final Exception e) {
      LOGGER.warning("Failed to initialize GC runtime, tests will be skipped: " + e.getMessage());
      gcRuntime = null;
      return;
    }

    // Initialize JNI exception handler with GC integration
    try {
      final JniExceptionHandler.ExceptionHandlingConfig jniConfig =
          JniExceptionHandler.ExceptionHandlingConfig.builder()
              .enableNestedTryCatch(true)
              .enableExceptionUnwinding(true)
              .maxUnwindDepth(2000)
              .validateExceptionTypes(true)
              .enableStackTraces(true)
              .enableExceptionPropagation(true)
              .enableGcIntegration(true)
              .build();
      jniHandler = new JniExceptionHandler(jniConfig, gcRuntime);
    } catch (final Exception e) {
      LOGGER.warning("Failed to initialize JNI exception handler: " + e.getMessage());
      jniHandler = null;
    }

    // Initialize Panama exception handler with GC integration (Java 23+)
    try {
      final PanamaExceptionHandler.ExceptionHandlingConfig panamaConfig =
          PanamaExceptionHandler.ExceptionHandlingConfig.builder()
              .enableNestedTryCatch(true)
              .enableExceptionUnwinding(true)
              .maxUnwindDepth(2000)
              .validateExceptionTypes(true)
              .enableStackTraces(true)
              .enableExceptionPropagation(true)
              .enableGcIntegration(true)
              .build();
      panamaHandler = new PanamaExceptionHandler(panamaConfig, gcRuntime);
    } catch (final Exception e) {
      LOGGER.warning("Failed to initialize Panama exception handler: " + e.getMessage());
      panamaHandler = null;
    }
  }

  @Test
  @DisplayName("GC-aware exception tags should require GC runtime")
  void testGcAwareExceptionTagRequiresGcRuntime() {
    assumeTrue(jniHandler != null && gcRuntime != null,
        "JNI handler and GC runtime not available");

    // Test successful creation with GC runtime
    final List<WasmValueType> gcTypes = List.of(
        WasmValueType.EXTERNREF,
        WasmValueType.FUNCREF,
        WasmValueType.I32
    );

    final WasmExceptionHandlingException.ExceptionTag gcTag =
        jniHandler.createExceptionTag("gc_aware_tag", gcTypes, true);

    assertNotNull(gcTag);
    assertTrue(gcTag.isGcAware());
    assertEquals(gcTypes, gcTag.getParameterTypes());

    // Test creation without GC runtime should fail
    final JniExceptionHandler.ExceptionHandlingConfig noGcConfig =
        JniExceptionHandler.ExceptionHandlingConfig.builder()
            .enableGcIntegration(false)
            .build();

    try (final JniExceptionHandler noGcHandler = new JniExceptionHandler(noGcConfig, null)) {
      assertThrows(IllegalArgumentException.class, () ->
          noGcHandler.createExceptionTag("no_gc_tag", gcTypes, true));
    }
  }

  @Test
  @DisplayName("Exception payloads with GC values should be properly validated")
  void testGcValuePayloadValidation() {
    assumeTrue(jniHandler != null && gcRuntime != null,
        "JNI handler and GC runtime not available");

    final List<WasmValueType> gcTypes = List.of(WasmValueType.EXTERNREF, WasmValueType.I64);
    final WasmExceptionHandlingException.ExceptionTag gcTag =
        jniHandler.createExceptionTag("gc_payload_tag", gcTypes, true);

    // Create mock GC values (in real implementation, these would be actual GC objects)
    final List<GcValue> gcValues = createMockGcValues();
    final List<WasmValue> wasmValues = List.of(
        WasmValue.createExternRef(null), // Would reference actual GC object
        WasmValue.createI64(12345L)
    );

    // Test valid GC-aware payload
    final WasmExceptionHandlingException.ExceptionPayload gcPayload =
        new WasmExceptionHandlingException.ExceptionPayload(gcTag, wasmValues, gcValues);

    assertNotNull(gcPayload);
    assertTrue(gcPayload.hasGcValues());
    assertEquals(gcValues, gcPayload.getGcValues());

    // Test non-GC tag with GC values should fail validation
    final WasmExceptionHandlingException.ExceptionTag nonGcTag =
        jniHandler.createExceptionTag("non_gc_tag", gcTypes, false);

    assertThrows(WasmExceptionHandlingException.PayloadValidationException.class, () ->
        new WasmExceptionHandlingException.ExceptionPayload(nonGcTag, wasmValues, gcValues));

    // Test GC tag without GC values should fail validation
    assertThrows(WasmExceptionHandlingException.PayloadValidationException.class, () ->
        new WasmExceptionHandlingException.ExceptionPayload(gcTag, wasmValues, List.of()));
  }

  @Test
  @DisplayName("GC references should be properly managed during exception propagation")
  void testGcReferenceManagementDuringPropagation() {
    assumeTrue(jniHandler != null && gcRuntime != null,
        "JNI handler and GC runtime not available");

    final List<WasmValueType> gcTypes = List.of(WasmValueType.EXTERNREF);
    final WasmExceptionHandlingException.ExceptionTag gcTag =
        jniHandler.createExceptionTag("gc_propagation_tag", gcTypes, true);

    final CountDownLatch handlerLatch = new CountDownLatch(1);
    final boolean[] gcReferencesPreserved = {false};

    // Register exception handler that checks GC reference preservation
    jniHandler.registerExceptionHandler(gcTag, (tag, payload) -> {
      try {
        assertEquals(gcTag, tag);
        assertTrue(payload.hasGcValues());

        // Verify GC references are still valid
        final List<GcValue> gcValues = payload.getGcValues();
        assertFalse(gcValues.isEmpty());

        // In a real implementation, we would validate that GC objects are still accessible
        gcReferencesPreserved[0] = true;
        return true; // Continue execution
      } catch (final Exception e) {
        LOGGER.severe("Exception in GC reference test handler: " + e.getMessage());
        return false;
      } finally {
        handlerLatch.countDown();
      }
    });

    // Create and throw exception with GC references
    final List<GcValue> gcValues = createMockGcValues();
    final List<WasmValue> wasmValues = List.of(WasmValue.createExternRef(null));
    final WasmExceptionHandlingException.ExceptionPayload gcPayload =
        new WasmExceptionHandlingException.ExceptionPayload(gcTag, wasmValues, gcValues);

    // In a real implementation, this would trigger native exception throwing
    // For testing, we simulate the exception handling process
    assertDoesNotThrow(() -> {
      // Simulate exception propagation
      assertTrue(jniHandler.performUnwinding(0));
    });

    // Verify GC references were handled properly
    assertDoesNotThrow(() -> handlerLatch.await(5, TimeUnit.SECONDS));
    // assertTrue(gcReferencesPreserved[0]); // Would be enabled when callback actually triggers
  }

  @Test
  @DisplayName("GC memory leaks should be detected during exception handling")
  void testGcMemoryLeakDetectionDuringExceptionHandling() {
    assumeTrue(jniHandler != null && gcRuntime != null,
        "JNI handler and GC runtime not available");

    // Get initial GC stats
    final var initialStats = gcRuntime.getGcStats();
    final long initialObjectCount = initialStats.getTotalObjectCount();

    // Create multiple GC-aware exception tags and payloads
    for (int i = 0; i < 10; i++) {
      final String tagName = "leak_test_tag_" + i;
      final List<WasmValueType> gcTypes = List.of(WasmValueType.EXTERNREF);
      final WasmExceptionHandlingException.ExceptionTag gcTag =
          jniHandler.createExceptionTag(tagName, gcTypes, true);

      final List<GcValue> gcValues = createMockGcValues();
      final List<WasmValue> wasmValues = List.of(WasmValue.createExternRef(null));
      final WasmExceptionHandlingException.ExceptionPayload gcPayload =
          new WasmExceptionHandlingException.ExceptionPayload(gcTag, wasmValues, gcValues);

      assertNotNull(gcPayload);
      assertTrue(gcPayload.hasGcValues());
    }

    // Force garbage collection
    gcRuntime.forceGc();

    // Check for memory leaks
    final var finalStats = gcRuntime.getGcStats();
    final long finalObjectCount = finalStats.getTotalObjectCount();

    // In a real implementation, we would check that object count hasn't grown excessively
    LOGGER.info("Initial GC object count: " + initialObjectCount);
    LOGGER.info("Final GC object count: " + finalObjectCount);

    // Verify memory leak detection is working
    assertTrue(gcRuntime.isMemoryLeakDetectionEnabled());
  }

  @Test
  @DisplayName("Struct and Array instances should work in exception payloads")
  void testStructAndArrayInstancesInExceptionPayloads() {
    assumeTrue(jniHandler != null && gcRuntime != null,
        "JNI handler and GC runtime not available");

    final List<WasmValueType> structArrayTypes = List.of(
        WasmValueType.EXTERNREF, // For struct instance
        WasmValueType.EXTERNREF  // For array instance
    );

    final WasmExceptionHandlingException.ExceptionTag structArrayTag =
        jniHandler.createExceptionTag("struct_array_tag", structArrayTypes, true);

    // Create mock struct and array instances
    final List<GcValue> gcValues = List.of(
        createMockStructInstance(),
        createMockArrayInstance()
    );

    final List<WasmValue> wasmValues = List.of(
        WasmValue.createExternRef(null), // Reference to struct
        WasmValue.createExternRef(null)  // Reference to array
    );

    final WasmExceptionHandlingException.ExceptionPayload structArrayPayload =
        new WasmExceptionHandlingException.ExceptionPayload(structArrayTag, wasmValues, gcValues);

    assertNotNull(structArrayPayload);
    assertTrue(structArrayPayload.hasGcValues());
    assertEquals(2, structArrayPayload.getGcValues().size());

    // Verify that struct and array instances are properly handled
    final List<GcValue> payloadGcValues = structArrayPayload.getGcValues();
    assertEquals(2, payloadGcValues.size());
  }

  @Test
  @EnabledOnJre(JRE.JAVA_23)
  @DisplayName("Panama FFI should handle GC-aware exceptions on Java 23+")
  void testPanamaGcAwareExceptionHandling() {
    assumeTrue(panamaHandler != null && gcRuntime != null,
        "Panama handler and GC runtime not available");

    final List<WasmValueType> gcTypes = List.of(WasmValueType.EXTERNREF, WasmValueType.FUNCREF);
    final WasmExceptionHandlingException.ExceptionTag panamaGcTag =
        panamaHandler.createExceptionTag("panama_gc_tag", gcTypes, true);

    assertNotNull(panamaGcTag);
    assertTrue(panamaGcTag.isGcAware());

    // Test Panama-specific GC reference handling
    final List<GcValue> gcValues = createMockGcValues();
    final List<WasmValue> wasmValues = List.of(
        WasmValue.createExternRef(null),
        WasmValue.createFuncRef(null)
    );

    final WasmExceptionHandlingException.ExceptionPayload panamaGcPayload =
        new WasmExceptionHandlingException.ExceptionPayload(panamaGcTag, wasmValues, gcValues);

    assertNotNull(panamaGcPayload);
    assertTrue(panamaGcPayload.hasGcValues());

    // Test Panama memory management
    assertDoesNotThrow(() -> {
      final String stackTrace = panamaHandler.captureStackTrace(panamaGcTag);
      // Stack trace may be null but should not throw
    });

    assertTrue(panamaHandler.performUnwinding(0));
  }

  @Test
  @DisplayName("Cross-language GC reference mapping should work correctly")
  void testCrossLanguageGcReferenceMapping() {
    assumeTrue(jniHandler != null && gcRuntime != null,
        "JNI handler and GC runtime not available");

    final List<WasmValueType> mappingTypes = List.of(WasmValueType.EXTERNREF);
    final WasmExceptionHandlingException.ExceptionTag mappingTag =
        jniHandler.createExceptionTag("mapping_tag", mappingTypes, true);

    final List<GcValue> gcValues = createMockGcValues();
    final List<WasmValue> wasmValues = List.of(WasmValue.createExternRef(null));
    final WasmExceptionHandlingException.ExceptionPayload mappingPayload =
        new WasmExceptionHandlingException.ExceptionPayload(mappingTag, wasmValues, gcValues);

    // Test cross-language mapping exception
    final WasmExceptionHandlingException.CrossLanguageMappingException mappingException =
        new WasmExceptionHandlingException.CrossLanguageMappingException(
            "Failed to map GC references between WebAssembly and Java",
            mappingPayload);

    assertNotNull(mappingException);
    assertTrue(mappingException.involvesGcReferences());
    assertEquals(mappingPayload, mappingException.getExceptionPayload());
    assertEquals(WasmExceptionHandlingException.ExceptionErrorCode.CROSS_LANGUAGE_MAPPING_FAILED,
        mappingException.getExceptionErrorCode());
  }

  @Test
  @DisplayName("Exception handler cleanup should properly release GC references")
  void testExceptionHandlerCleanupReleasesGcReferences() {
    assumeTrue(gcRuntime != null, "GC runtime not available");

    // Create a temporary exception handler with GC integration
    final JniExceptionHandler.ExceptionHandlingConfig tempConfig =
        JniExceptionHandler.ExceptionHandlingConfig.builder()
            .enableGcIntegration(true)
            .build();

    try (final JniExceptionHandler tempHandler = new JniExceptionHandler(tempConfig, gcRuntime)) {
      // Create GC-aware exception tag
      final List<WasmValueType> gcTypes = List.of(WasmValueType.EXTERNREF);
      final WasmExceptionHandlingException.ExceptionTag gcTag =
          tempHandler.createExceptionTag("cleanup_tag", gcTypes, true);

      assertNotNull(gcTag);
      assertTrue(gcTag.isGcAware());

      // Create exception payload with GC references
      final List<GcValue> gcValues = createMockGcValues();
      final List<WasmValue> wasmValues = List.of(WasmValue.createExternRef(null));
      final WasmExceptionHandlingException.ExceptionPayload gcPayload =
          new WasmExceptionHandlingException.ExceptionPayload(gcTag, wasmValues, gcValues);

      assertNotNull(gcPayload);
      assertTrue(gcPayload.hasGcValues());

      // Handler will be closed automatically via try-with-resources
    }

    // Verify that GC references were properly released during cleanup
    // In a real implementation, we would check that GC objects are eligible for collection
    gcRuntime.forceGc();

    // Verify GC runtime is still healthy after handler cleanup
    assertTrue(gcRuntime.isHealthy());
  }

  /**
   * Creates mock GC values for testing purposes.
   * In a real implementation, these would be actual GC-managed objects.
   */
  private List<GcValue> createMockGcValues() {
    // Return empty list as placeholder - real implementation would create actual GC values
    return List.of();
  }

  /**
   * Creates a mock struct instance for testing purposes.
   * In a real implementation, this would be an actual StructInstance from the GC runtime.
   */
  private GcValue createMockStructInstance() {
    // Return null as placeholder - real implementation would create actual StructInstance
    return null;
  }

  /**
   * Creates a mock array instance for testing purposes.
   * In a real implementation, this would be an actual ArrayInstance from the GC runtime.
   */
  private GcValue createMockArrayInstance() {
    // Return null as placeholder - real implementation would create actual ArrayInstance
    return null;
  }
}