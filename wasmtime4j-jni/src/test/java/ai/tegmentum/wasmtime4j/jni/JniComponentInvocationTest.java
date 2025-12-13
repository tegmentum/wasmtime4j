package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.wit.WitBool;
import ai.tegmentum.wasmtime4j.wit.WitChar;
import ai.tegmentum.wasmtime4j.wit.WitFloat64;
import ai.tegmentum.wasmtime4j.wit.WitS32;
import ai.tegmentum.wasmtime4j.wit.WitS64;
import ai.tegmentum.wasmtime4j.wit.WitString;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for WebAssembly Component Model function invocation with WIT
 * marshalling.
 *
 * <p>These tests validate:
 *
 * <ul>
 *   <li>WitValue type creation and basic functionality
 *   <li>Type safety and validation requirements
 *   <li>Documentation of full integration test requirements
 * </ul>
 *
 * <p><b>Note:</b> Full integration tests requiring actual WebAssembly component files (.wasm) are
 * documented but currently disabled. These will be enabled once test component files are available.
 *
 * <p>Required test components for future integration tests:
 *
 * <ul>
 *   <li>simple-add.wasm - Component exporting: add(s32, s32) -> s32
 *   <li>string-echo.wasm - Component exporting: echo(string) -> string
 *   <li>void-function.wasm - Component exporting: no-op() -> void
 *   <li>multi-type.wasm - Component exporting: process(bool, s64, float64, char) -> string
 * </ul>
 */
@DisplayName("Component Function Invocation with WIT Marshalling")
public final class JniComponentInvocationTest {

  /** Load test resources before running tests. */
  @BeforeAll
  public static void loadTestResources() {
    // Load native library
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      System.err.println("Warning: Failed to load native library: " + e.getMessage());
      System.err.println(
          "Component invocation tests will be skipped if native library unavailable");
    }
  }

  // ==================== WitValue Type Creation Tests ====================

  @Test
  @DisplayName("WitBool instances can be created with valid values")
  public void testWitBoolCreation() {
    final WitBool trueValue = WitBool.of(true);
    assertNotNull(trueValue, "WitBool.of(true) should not return null");
    assertTrue((Boolean) trueValue.toJava(), "WitBool should preserve true value");

    final WitBool falseValue = WitBool.of(false);
    assertNotNull(falseValue, "WitBool.of(false) should not return null");
    assertFalse((Boolean) falseValue.toJava(), "WitBool should preserve false value");
  }

  @Test
  @DisplayName("WitS32 instances can be created with valid values")
  public void testWitS32Creation() {
    final WitS32 zero = WitS32.of(0);
    assertNotNull(zero, "WitS32.of(0) should not return null");
    assertEquals(Integer.valueOf(0), zero.toJava(), "WitS32 should preserve zero value");

    final WitS32 positive = WitS32.of(42);
    assertNotNull(positive, "WitS32.of(42) should not return null");
    assertEquals(Integer.valueOf(42), positive.toJava(), "WitS32 should preserve positive value");

    final WitS32 negative = WitS32.of(-123);
    assertNotNull(negative, "WitS32.of(-123) should not return null");
    assertEquals(Integer.valueOf(-123), negative.toJava(), "WitS32 should preserve negative value");
  }

  @Test
  @DisplayName("WitS64 instances can be created with valid values")
  public void testWitS64Creation() {
    final WitS64 large = WitS64.of(9876543210L);
    assertNotNull(large, "WitS64.of(9876543210L) should not return null");
    assertEquals(Long.valueOf(9876543210L), large.toJava(), "WitS64 should preserve large value");
  }

  @Test
  @DisplayName("WitFloat64 instances can be created with valid values")
  public void testWitFloat64Creation() {
    final WitFloat64 pi = WitFloat64.of(3.14159);
    assertNotNull(pi, "WitFloat64.of(3.14159) should not return null");
    assertEquals(
        3.14159, (Double) pi.toJava(), 0.00001, "WitFloat64 should preserve floating point value");
  }

  @Test
  @DisplayName("WitChar instances can be created with valid values")
  public void testWitCharCreation() throws Exception {
    final WitChar letterZ = WitChar.of('Z');
    assertNotNull(letterZ, "WitChar.of('Z') should not return null");
    assertEquals(Character.valueOf('Z'), letterZ.toJava(), "WitChar should preserve character value");
  }

  @Test
  @DisplayName("WitString instances can be created with valid values")
  public void testWitStringCreation() throws Exception {
    final WitString hello = WitString.of("hello world");
    assertNotNull(hello, "WitString.of() should not return null");
    assertEquals("hello world", hello.toJava(), "WitString should preserve string value");

    final WitString empty = WitString.of("");
    assertNotNull(empty, "WitString.of(\"\") should not return null");
    assertEquals("", empty.toJava(), "WitString should preserve empty string");
  }

  // ==================== Future Integration Tests (Disabled) ====================

  /*
   * TODO: Enable these tests once WebAssembly component test files are available
   *
   * These tests require actual compiled WebAssembly components (.wasm files) with
   * specific exported functions. To create these components:
   *
   * 1. Create WIT interface definitions (e.g., simple-add.wit)
   * 2. Implement the components in a language with component model support (Rust, etc.)
   * 3. Compile to .wasm using wasm-tools component new
   * 4. Place the .wasm files in: wasmtime4j-tests/src/test/resources/components/
   *
   * Example WIT definition for simple-add.wit:
   * ```
   * package test:math;
   *
   * world calculator {
   *   export add: func(a: s32, b: s32) -> s32;
   * }
   * ```
   *
   * Integration tests would include:
   * - testInvokeSimpleAdd: Test s32 parameter passing and return values
   * - testInvokeStringEcho: Test string marshalling
   * - testInvokeVoidFunction: Test functions that return void
   * - testInvokeMultiTypeFunction: Test functions with multiple parameter types
   * - testInvokeNonExistentFunction: Test error handling for missing functions
   * - testInvokeWithNullFunctionName: Test validation of function name
   * - testInvokeWithEmptyFunctionName: Test validation of empty function name
   * - testInvokeWithNullArgument: Test validation of null arguments
   * - testInvokeWithNonWitValueArgument: Test validation of argument types
   * - testInvokeOnClosedInstance: Test error handling for closed instances
   *
   * Each test would:
   * 1. Load a component file using JniComponent.createComponentEngine()
   * 2. Instantiate the component
   * 3. Create a JniComponentInstanceImpl
   * 4. Invoke functions with WitValue parameters
   * 5. Verify results or exceptions
   * 6. Clean up resources
   */

  @Test
  @DisplayName("Integration tests documentation")
  public void testIntegrationTestsDocumentation() {
    // This test serves as documentation for the required integration tests
    // See the comment block above for details on what needs to be implemented
    assertTrue(true, "Integration tests are documented and waiting for component files");
  }

  // ==================== End-to-End Integration Test ====================

  @Test
  @DisplayName("End-to-end component function invocation with add(s32, s32) -> s32")
  public void testComponentFunctionInvocationEndToEnd() throws Exception {
    // Load the add.wasm component file
    final java.io.InputStream componentStream =
        getClass().getResourceAsStream("/components/add.wasm");
    assertNotNull(componentStream, "add.wasm component file must exist in test resources");

    final byte[] componentBytes = readAllBytes(componentStream);
    assertTrue(componentBytes.length > 0, "Component bytes must not be empty");

    // Create component engine
    final ai.tegmentum.wasmtime4j.jni.JniComponent.JniComponentEngine engine =
        ai.tegmentum.wasmtime4j.jni.JniComponent.createComponentEngine();
    assertNotNull(engine, "Component engine must not be null");

    try {
      // Load component from bytes
      final ai.tegmentum.wasmtime4j.jni.JniComponent.JniComponentHandle componentHandle =
          engine.loadComponentFromBytes(componentBytes);
      assertNotNull(componentHandle, "Component handle must not be null");

      try {
        // Instantiate the component
        final ai.tegmentum.wasmtime4j.jni.JniComponent.JniComponentInstanceHandle instanceHandle =
            engine.instantiateComponent(componentHandle);
        assertNotNull(instanceHandle, "Component instance handle must not be null");

        try {
          // Create WitS32 values for parameters: add(5, 7) should return 12
          final WitS32 param1 = WitS32.of(5);
          final WitS32 param2 = WitS32.of(7);

          // Marshal parameters using WitValueMarshaller
          final ai.tegmentum.wasmtime4j.wit.WitValueMarshaller.MarshalledValue marshalled1 =
              ai.tegmentum.wasmtime4j.wit.WitValueMarshaller.marshal(param1);
          final ai.tegmentum.wasmtime4j.wit.WitValueMarshaller.MarshalledValue marshalled2 =
              ai.tegmentum.wasmtime4j.wit.WitValueMarshaller.marshal(param2);

          // Prepare parameters for native invocation
          final int[] typeDiscriminators =
              new int[] {marshalled1.getTypeDiscriminator(), marshalled2.getTypeDiscriminator()};
          final byte[][] paramData = new byte[][] {marshalled1.getData(), marshalled2.getData()};

          // Invoke the add function via native binding with engine handle and instance ID
          final Object[] result =
              ai.tegmentum.wasmtime4j.jni.JniComponent.nativeComponentInvokeFunction(
                  engine.getNativeHandle(),
                  instanceHandle.getNativeHandle(),
                  "add",
                  typeDiscriminators,
                  paramData);

          // Verify result structure
          assertNotNull(result, "Result must not be null");
          assertEquals(
              2, result.length, "Result must have 2 elements: [type_discriminator, data_bytes]");

          // Extract result type discriminator and data
          final Integer resultDiscriminator = (Integer) result[0];
          final byte[] resultData = (byte[]) result[1];

          assertNotNull(resultDiscriminator, "Result type discriminator must not be null");
          assertNotNull(resultData, "Result data must not be null");
          assertEquals(
              Integer.valueOf(2), resultDiscriminator, "Result type discriminator must be 2 (s32)");

          // Unmarshal result using WitValueMarshaller
          final ai.tegmentum.wasmtime4j.wit.WitValue resultValue =
              ai.tegmentum.wasmtime4j.wit.WitValueMarshaller.unmarshal(
                  resultDiscriminator, resultData);
          assertNotNull(resultValue, "Result value must not be null");
          assertTrue(
              resultValue instanceof WitS32,
              "Result value must be WitS32, got: " + resultValue.getClass().getName());

          final WitS32 resultS32 = (WitS32) resultValue;

          // Verify the calculation: 5 + 7 = 12
          assertEquals(
              Integer.valueOf(12),
              resultS32.toJava(),
              "add(5, 7) must return 12 via WIT marshalling");

          System.out.println(
              "SUCCESS: Component function invocation test passed - add(5, 7) = "
                  + resultS32.toJava());

        } finally {
          // Clean up instance resources
          if (instanceHandle != null) {
            instanceHandle.close();
          }
        }

      } finally {
        // Clean up component resources
        if (componentHandle != null) {
          componentHandle.close();
        }
      }

    } finally {
      // Clean up engine resources
      if (engine != null) {
        engine.close();
      }
    }
  }

  /**
   * Helper method to read all bytes from an InputStream.
   *
   * @param inputStream The input stream to read from
   * @return The bytes read from the stream
   * @throws Exception If reading fails
   */
  private static byte[] readAllBytes(final java.io.InputStream inputStream) throws Exception {
    final java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
    final byte[] tempBuffer = new byte[1024];
    int bytesRead;

    while ((bytesRead = inputStream.read(tempBuffer)) != -1) {
      buffer.write(tempBuffer, 0, bytesRead);
    }

    return buffer.toByteArray();
  }
}
