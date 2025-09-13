package ai.tegmentum.wasmtime4j.function;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import java.util.Arrays;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Advanced parameter marshaling tests for V128, funcref, and externref WebAssembly value types.
 *
 * <p>This test class validates:
 *
 * <ul>
 *   <li>V128 (SIMD vector) parameter handling and marshaling
 *   <li>Function reference (funcref) parameter and return value handling
 *   <li>External reference (externref) parameter and return value handling
 *   <li>Complex parameter combinations
 *   <li>Edge cases and boundary conditions for advanced types
 *   <li>Type safety validation for complex types
 * </ul>
 */
@DisplayName("Advanced Parameter Marshaling Tests")
public final class AdvancedParameterMarshalingIT extends BaseIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(AdvancedParameterMarshalingIT.class.getName());

  /**
   * Tests V128 (SIMD vector) parameter marshaling and handling.
   *
   * <p>Note: V128 support depends on SIMD instruction set availability in Wasmtime.
   */
  @Test
  @DisplayName("V128 SIMD vector parameter marshaling")
  void testV128VectorParameterMarshaling() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing V128 vector parameter marshaling with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine)) {

            registerForCleanup(engine);
            registerForCleanup(store);

            // Create a V128 value with test data
            final byte[] vectorData = new byte[16];
            for (int i = 0; i < 16; i++) {
              vectorData[i] = (byte) (i * 8); // Pattern: 0, 8, 16, 24, ...
            }

            final WasmValue v128Value = WasmValue.v128(vectorData);

            // Validate V128 value creation
            assertEquals(WasmValueType.V128, v128Value.getType(), "Should be V128 type");
            assertArrayEquals(vectorData, v128Value.asV128(), "V128 data should match input");

            // Test V128 value immutability
            final byte[] retrievedData = v128Value.asV128();
            retrievedData[0] = (byte) 0xFF; // Modify retrieved data
            assertNotEquals(
                (byte) 0xFF, v128Value.asV128()[0], "Original V128 data should not be modified");

            LOGGER.info("V128 vector parameter marshaling test completed for " + runtimeType);
          }
        });
  }

  /** Tests V128 edge cases and boundary conditions. */
  @Test
  @DisplayName("V128 edge cases and validation")
  void testV128EdgeCasesAndValidation() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing V128 edge cases and validation with " + runtimeType + " runtime");

          // Test V128 with all zeros
          final byte[] zeroVector = new byte[16];
          final WasmValue zeroV128 = WasmValue.v128(zeroVector);
          assertEquals(WasmValueType.V128, zeroV128.getType());
          assertArrayEquals(zeroVector, zeroV128.asV128());

          // Test V128 with all ones
          final byte[] onesVector = new byte[16];
          Arrays.fill(onesVector, (byte) 0xFF);
          final WasmValue onesV128 = WasmValue.v128(onesVector);
          assertArrayEquals(onesVector, onesV128.asV128());

          // Test V128 with alternating pattern
          final byte[] patternVector = new byte[16];
          for (int i = 0; i < 16; i++) {
            patternVector[i] = (byte) (i % 2 == 0 ? 0xAA : 0x55);
          }
          final WasmValue patternV128 = WasmValue.v128(patternVector);
          assertArrayEquals(patternVector, patternV128.asV128());

          // Test invalid V128 size
          assertThrows(
              IllegalArgumentException.class,
              () -> WasmValue.v128(new byte[15]), // 15 bytes instead of 16
              "Should throw exception for invalid V128 size");

          assertThrows(
              IllegalArgumentException.class,
              () -> WasmValue.v128(new byte[17]), // 17 bytes instead of 16
              "Should throw exception for invalid V128 size");

          assertThrows(
              IllegalArgumentException.class,
              () -> WasmValue.v128(null), // null array
              "Should throw exception for null V128 data");

          LOGGER.info("V128 edge cases and validation test completed for " + runtimeType);
        });
  }

  /** Tests function reference (funcref) parameter handling. */
  @Test
  @DisplayName("Function reference (funcref) parameter handling")
  void testFunctionReferenceParameterHandling() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info(
              "Testing function reference parameter handling with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine)) {

            registerForCleanup(engine);
            registerForCleanup(store);

            // Test null funcref
            final WasmValue nullFuncref = WasmValue.funcref(null);
            assertEquals(WasmValueType.FUNCREF, nullFuncref.getType(), "Should be FUNCREF type");
            assertNull(nullFuncref.asFuncref(), "Null funcref should be null");

            // Test funcref type validation
            assertThrows(
                ClassCastException.class,
                () -> nullFuncref.asI32(),
                "Should throw ClassCastException when accessing funcref as I32");

            assertThrows(
                ClassCastException.class,
                () -> nullFuncref.asExternref(),
                "Should throw ClassCastException when accessing funcref as externref");

            LOGGER.info("Function reference parameter handling test completed for " + runtimeType);
          }
        });
  }

  /** Tests external reference (externref) parameter handling. */
  @Test
  @DisplayName("External reference (externref) parameter handling")
  void testExternalReferenceParameterHandling() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info(
              "Testing external reference parameter handling with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine)) {

            registerForCleanup(engine);
            registerForCleanup(store);

            // Test null externref
            final WasmValue nullExternref = WasmValue.externref(null);
            assertEquals(
                WasmValueType.EXTERNREF, nullExternref.getType(), "Should be EXTERNREF type");
            assertNull(nullExternref.asExternref(), "Null externref should be null");

            // Test externref with Java object
            final String testObject = "Test External Reference";
            final WasmValue objectExternref = WasmValue.externref(testObject);
            assertEquals(WasmValueType.EXTERNREF, objectExternref.getType());
            assertEquals(
                testObject, objectExternref.asExternref(), "Should return the same object");

            // Test externref with complex object
            final java.util.List<String> listObject = Arrays.asList("item1", "item2", "item3");
            final WasmValue listExternref = WasmValue.externref(listObject);
            assertEquals(listObject, listExternref.asExternref(), "Should return the same list");

            // Test externref type validation
            assertThrows(
                ClassCastException.class,
                () -> nullExternref.asI32(),
                "Should throw ClassCastException when accessing externref as I32");

            assertThrows(
                ClassCastException.class,
                () -> nullExternref.asFuncref(),
                "Should throw ClassCastException when accessing externref as funcref");

            LOGGER.info("External reference parameter handling test completed for " + runtimeType);
          }
        });
  }

  /** Tests complex parameter combinations with multiple value types. */
  @Test
  @DisplayName("Complex parameter type combinations")
  void testComplexParameterTypeCombinations() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing complex parameter combinations with " + runtimeType + " runtime");

          try (final Engine engine = runtime.createEngine();
              final Store store = runtime.createStore(engine)) {

            registerForCleanup(engine);
            registerForCleanup(store);

            // Create values of different types
            final WasmValue i32Value = WasmValue.i32(42);
            final WasmValue i64Value = WasmValue.i64(1234567890L);
            final WasmValue f32Value = WasmValue.f32(3.14159f);
            final WasmValue f64Value = WasmValue.f64(2.718281828);

            final byte[] vectorData = {
              0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E,
              0x0F, 0x10
            };
            final WasmValue v128Value = WasmValue.v128(vectorData);

            final WasmValue funcrefValue = WasmValue.funcref(null);
            final WasmValue externrefValue = WasmValue.externref("External Reference Test");

            // Validate all types are created correctly
            assertEquals(WasmValueType.I32, i32Value.getType());
            assertEquals(WasmValueType.I64, i64Value.getType());
            assertEquals(WasmValueType.F32, f32Value.getType());
            assertEquals(WasmValueType.F64, f64Value.getType());
            assertEquals(WasmValueType.V128, v128Value.getType());
            assertEquals(WasmValueType.FUNCREF, funcrefValue.getType());
            assertEquals(WasmValueType.EXTERNREF, externrefValue.getType());

            // Validate values are accessible through appropriate methods
            assertEquals(42, i32Value.asI32());
            assertEquals(1234567890L, i64Value.asI64());
            assertEquals(3.14159f, f32Value.asF32(), 0.00001f);
            assertEquals(2.718281828, f64Value.asF64(), 0.00000001);
            assertArrayEquals(vectorData, v128Value.asV128());
            assertNull(funcrefValue.asFuncref());
            assertEquals("External Reference Test", externrefValue.asExternref());

            // Test toString() methods for debugging
            assertNotNull(i32Value.toString(), "I32 toString should not be null");
            assertNotNull(v128Value.toString(), "V128 toString should not be null");
            assertNotNull(funcrefValue.toString(), "Funcref toString should not be null");
            assertNotNull(externrefValue.toString(), "Externref toString should not be null");

            LOGGER.info("Complex parameter combinations test completed for " + runtimeType);
          }
        });
  }

  /** Tests type safety validation for all parameter types. */
  @Test
  @DisplayName("Parameter type safety validation")
  void testParameterTypeSafetyValidation() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing parameter type safety validation with " + runtimeType + " runtime");

          // Create values of each type
          final WasmValue i32Value = WasmValue.i32(42);
          final WasmValue i64Value = WasmValue.i64(1000L);
          final WasmValue f32Value = WasmValue.f32(3.14f);
          final WasmValue f64Value = WasmValue.f64(2.718);
          final WasmValue v128Value = WasmValue.v128(new byte[16]);
          final WasmValue funcrefValue = WasmValue.funcref(null);
          final WasmValue externrefValue = WasmValue.externref(new Object());

          // Test I32 type safety
          assertEquals(42, i32Value.asI32());
          assertEquals(42, i32Value.asInt()); // Alias
          assertThrows(ClassCastException.class, () -> i32Value.asLong());
          assertThrows(ClassCastException.class, () -> i32Value.asFloat());
          assertThrows(ClassCastException.class, () -> i32Value.asDouble());
          assertThrows(ClassCastException.class, () -> i32Value.asV128());
          assertThrows(ClassCastException.class, () -> i32Value.asFuncref());
          assertThrows(ClassCastException.class, () -> i32Value.asExternref());

          // Test I64 type safety
          assertEquals(1000L, i64Value.asI64());
          assertEquals(1000L, i64Value.asLong()); // Alias
          assertThrows(ClassCastException.class, () -> i64Value.asInt());
          assertThrows(ClassCastException.class, () -> i64Value.asFloat());
          assertThrows(ClassCastException.class, () -> i64Value.asDouble());

          // Test F32 type safety
          assertEquals(3.14f, f32Value.asF32(), 0.001f);
          assertEquals(3.14f, f32Value.asFloat(), 0.001f); // Alias
          assertThrows(ClassCastException.class, () -> f32Value.asInt());
          assertThrows(ClassCastException.class, () -> f32Value.asLong());
          assertThrows(ClassCastException.class, () -> f32Value.asDouble());

          // Test F64 type safety
          assertEquals(2.718, f64Value.asF64(), 0.001);
          assertEquals(2.718, f64Value.asDouble(), 0.001); // Alias
          assertThrows(ClassCastException.class, () -> f64Value.asInt());
          assertThrows(ClassCastException.class, () -> f64Value.asLong());
          assertThrows(ClassCastException.class, () -> f64Value.asFloat());

          // Test V128 type safety
          assertEquals(16, v128Value.asV128().length);
          assertThrows(ClassCastException.class, () -> v128Value.asInt());
          assertThrows(ClassCastException.class, () -> v128Value.asLong());
          assertThrows(ClassCastException.class, () -> v128Value.asFloat());
          assertThrows(ClassCastException.class, () -> v128Value.asDouble());

          // Test funcref type safety
          assertNull(funcrefValue.asFuncref());
          // Note: Type checking is performed by the asFuncref() method itself

          // Test externref type safety
          assertNotNull(externrefValue.asExternref());
          // Note: Type checking is performed by the asExternref() method itself

          LOGGER.info("Parameter type safety validation test completed for " + runtimeType);
        });
  }

  /** Tests parameter marshaling performance for complex types. */
  @Test
  @DisplayName("Complex parameter marshaling performance")
  void testComplexParameterMarshalingPerformance() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info(
              "Testing complex parameter marshaling performance with " + runtimeType + " runtime");

          // Measure V128 creation and access performance
          measureExecutionTime(
              "V128 creation and access (1000 iterations) with " + runtimeType,
              () -> {
                final byte[] testData = new byte[16];
                for (int i = 0; i < 1000; i++) {
                  // Fill with pattern
                  for (int j = 0; j < 16; j++) {
                    testData[j] = (byte) ((i + j) % 256);
                  }

                  final WasmValue v128 = WasmValue.v128(testData);
                  final byte[] retrieved = v128.asV128();

                  // Verify data integrity
                  if (retrieved.length != 16) {
                    throw new RuntimeException("V128 size mismatch");
                  }
                }
              });

          // Measure externref creation and access performance
          measureExecutionTime(
              "Externref creation and access (1000 iterations) with " + runtimeType,
              () -> {
                for (int i = 0; i < 1000; i++) {
                  final String testObject = "Test Object " + i;
                  final WasmValue externref = WasmValue.externref(testObject);
                  final Object retrieved = externref.asExternref();

                  // Verify object integrity
                  if (!testObject.equals(retrieved)) {
                    throw new RuntimeException("Externref object mismatch");
                  }
                }
              });

          LOGGER.info("Complex parameter marshaling performance test completed for " + runtimeType);
        });
  }

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    LOGGER.info("Setting up advanced parameter marshaling test: " + testInfo.getDisplayName());
  }
}
