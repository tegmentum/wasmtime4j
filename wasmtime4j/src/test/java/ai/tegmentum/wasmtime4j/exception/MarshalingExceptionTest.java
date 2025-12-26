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

package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.MarshalingException.OperationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link MarshalingException} class.
 *
 * <p>This test class verifies the construction and behavior of marshaling exceptions, including
 * operation types, recovery hints, and factory methods.
 */
@DisplayName("MarshalingException Tests")
class MarshalingExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("MarshalingException should extend WasmException")
    void shouldExtendWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(MarshalingException.class),
          "MarshalingException should extend WasmException");
    }

    @Test
    @DisplayName("MarshalingException should be serializable")
    void shouldBeSerializable() {
      assertTrue(
          java.io.Serializable.class.isAssignableFrom(MarshalingException.class),
          "MarshalingException should be serializable");
    }
  }

  @Nested
  @DisplayName("OperationType Enum Tests")
  class OperationTypeEnumTests {

    @Test
    @DisplayName("Should have SERIALIZATION value")
    void shouldHaveSerializationValue() {
      assertNotNull(OperationType.valueOf("SERIALIZATION"), "Should have SERIALIZATION value");
    }

    @Test
    @DisplayName("Should have DESERIALIZATION value")
    void shouldHaveDeserializationValue() {
      assertNotNull(OperationType.valueOf("DESERIALIZATION"), "Should have DESERIALIZATION value");
    }

    @Test
    @DisplayName("Should have TYPE_CONVERSION value")
    void shouldHaveTypeConversionValue() {
      assertNotNull(OperationType.valueOf("TYPE_CONVERSION"), "Should have TYPE_CONVERSION value");
    }

    @Test
    @DisplayName("Should have MEMORY_ALLOCATION value")
    void shouldHaveMemoryAllocationValue() {
      assertNotNull(
          OperationType.valueOf("MEMORY_ALLOCATION"), "Should have MEMORY_ALLOCATION value");
    }

    @Test
    @DisplayName("Should have MEMORY_LAYOUT value")
    void shouldHaveMemoryLayoutValue() {
      assertNotNull(OperationType.valueOf("MEMORY_LAYOUT"), "Should have MEMORY_LAYOUT value");
    }

    @Test
    @DisplayName("Should have ARRAY_MARSHALING value")
    void shouldHaveArrayMarshalingValue() {
      assertNotNull(
          OperationType.valueOf("ARRAY_MARSHALING"), "Should have ARRAY_MARSHALING value");
    }

    @Test
    @DisplayName("Should have COLLECTION_MARSHALING value")
    void shouldHaveCollectionMarshalingValue() {
      assertNotNull(
          OperationType.valueOf("COLLECTION_MARSHALING"),
          "Should have COLLECTION_MARSHALING value");
    }

    @Test
    @DisplayName("Should have OBJECT_GRAPH_TRAVERSAL value")
    void shouldHaveObjectGraphTraversalValue() {
      assertNotNull(
          OperationType.valueOf("OBJECT_GRAPH_TRAVERSAL"),
          "Should have OBJECT_GRAPH_TRAVERSAL value");
    }

    @Test
    @DisplayName("Should have CIRCULAR_REFERENCE_DETECTION value")
    void shouldHaveCircularReferenceDetectionValue() {
      assertNotNull(
          OperationType.valueOf("CIRCULAR_REFERENCE_DETECTION"),
          "Should have CIRCULAR_REFERENCE_DETECTION value");
    }

    @Test
    @DisplayName("Should have STRATEGY_SELECTION value")
    void shouldHaveStrategySelectionValue() {
      assertNotNull(
          OperationType.valueOf("STRATEGY_SELECTION"), "Should have STRATEGY_SELECTION value");
    }

    @Test
    @DisplayName("Should have 10 operation types")
    void shouldHave10OperationTypes() {
      assertEquals(10, OperationType.values().length, "Should have 10 operation types");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor without cause should set all fields")
    void constructorWithoutCauseShouldSetAllFields() {
      final MarshalingException exception =
          new MarshalingException(
              "Serialization failed",
              OperationType.SERIALIZATION,
              "MyClass",
              1024,
              "Use custom serializer");

      assertTrue(
          exception.getMessage().contains("Serialization failed"),
          "Message should contain error text");
      assertEquals(
          OperationType.SERIALIZATION,
          exception.getOperationType(),
          "Operation type should be SERIALIZATION");
      assertEquals(
          "MyClass", exception.getObjectTypeName(), "Object type name should be 'MyClass'");
      assertEquals(1024, exception.getEstimatedSize(), "Estimated size should be 1024");
      assertEquals(
          "Use custom serializer", exception.getRecoveryHint(), "Recovery hint should match");
    }

    @Test
    @DisplayName("Constructor with cause should set all fields")
    void constructorWithCauseShouldSetAllFields() {
      final Throwable cause = new RuntimeException("Root cause");
      final MarshalingException exception =
          new MarshalingException(
              "Deserialization failed",
              cause,
              OperationType.DESERIALIZATION,
              "AnotherClass",
              2048,
              "Check format");

      assertTrue(
          exception.getMessage().contains("Deserialization failed"),
          "Message should contain error text");
      assertSame(cause, exception.getCause(), "Cause should be set");
      assertEquals(
          OperationType.DESERIALIZATION,
          exception.getOperationType(),
          "Operation type should be DESERIALIZATION");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("serializationFailure should create proper exception")
    void serializationFailureShouldCreateProperException() {
      final Throwable cause = new RuntimeException("Serialize error");
      final MarshalingException exception =
          MarshalingException.serializationFailure("CustomObject", cause);

      assertEquals(
          OperationType.SERIALIZATION,
          exception.getOperationType(),
          "Operation type should be SERIALIZATION");
      assertEquals("CustomObject", exception.getObjectTypeName(), "Object type name should match");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("deserializationFailure should create proper exception")
    void deserializationFailureShouldCreateProperException() {
      final Throwable cause = new RuntimeException("Deserialize error");
      final MarshalingException exception =
          MarshalingException.deserializationFailure("DataClass", cause);

      assertEquals(
          OperationType.DESERIALIZATION,
          exception.getOperationType(),
          "Operation type should be DESERIALIZATION");
      assertEquals("DataClass", exception.getObjectTypeName(), "Object type name should match");
    }

    @Test
    @DisplayName("typeConversionFailure should include both types")
    void typeConversionFailureShouldIncludeBothTypes() {
      final MarshalingException exception =
          MarshalingException.typeConversionFailure("String", "Integer");

      assertEquals(
          OperationType.TYPE_CONVERSION,
          exception.getOperationType(),
          "Operation type should be TYPE_CONVERSION");
      assertTrue(
          exception.getObjectTypeName().contains("String"),
          "Object type should contain source type");
      assertTrue(
          exception.getObjectTypeName().contains("Integer"),
          "Object type should contain target type");
    }

    @Test
    @DisplayName("memoryAllocationFailure should include size")
    void memoryAllocationFailureShouldIncludeSize() {
      final Throwable cause = new OutOfMemoryError("No memory");
      final MarshalingException exception =
          MarshalingException.memoryAllocationFailure(10000, cause);

      assertEquals(
          OperationType.MEMORY_ALLOCATION,
          exception.getOperationType(),
          "Operation type should be MEMORY_ALLOCATION");
      assertEquals(10000, exception.getEstimatedSize(), "Estimated size should be 10000");
    }

    @Test
    @DisplayName("arrayMarshalingFailure should include array info")
    void arrayMarshalingFailureShouldIncludeArrayInfo() {
      final Throwable cause = new RuntimeException("Array error");
      final MarshalingException exception =
          MarshalingException.arrayMarshalingFailure("int[]", 5000, cause);

      assertEquals(
          OperationType.ARRAY_MARSHALING,
          exception.getOperationType(),
          "Operation type should be ARRAY_MARSHALING");
      assertEquals("int[]", exception.getObjectTypeName(), "Object type should be 'int[]'");
      assertEquals(5000, exception.getEstimatedSize(), "Estimated size should be 5000");
    }

    @Test
    @DisplayName("collectionMarshalingFailure should include collection info")
    void collectionMarshalingFailureShouldIncludeCollectionInfo() {
      final Throwable cause = new RuntimeException("Collection error");
      final MarshalingException exception =
          MarshalingException.collectionMarshalingFailure("ArrayList", 1000, cause);

      assertEquals(
          OperationType.COLLECTION_MARSHALING,
          exception.getOperationType(),
          "Operation type should be COLLECTION_MARSHALING");
      assertEquals("ArrayList", exception.getObjectTypeName(), "Object type should be 'ArrayList'");
    }

    @Test
    @DisplayName("circularReferenceDetected should create proper exception")
    void circularReferenceDetectedShouldCreateProperException() {
      final MarshalingException exception = MarshalingException.circularReferenceDetected("Node");

      assertEquals(
          OperationType.CIRCULAR_REFERENCE_DETECTION,
          exception.getOperationType(),
          "Operation type should be CIRCULAR_REFERENCE_DETECTION");
      assertTrue(
          exception.getMessage().toLowerCase().contains("circular"),
          "Message should mention circular");
    }

    @Test
    @DisplayName("objectTooLarge should include size info")
    void objectTooLargeShouldIncludeSizeInfo() {
      final MarshalingException exception =
          MarshalingException.objectTooLarge("LargeObject", 5000, 1000);

      assertEquals(
          OperationType.STRATEGY_SELECTION,
          exception.getOperationType(),
          "Operation type should be STRATEGY_SELECTION");
      assertTrue(exception.getMessage().contains("5000"), "Message should contain actual size");
      assertTrue(exception.getMessage().contains("1000"), "Message should contain max size");
    }

    @Test
    @DisplayName("unsupportedObjectType should create proper exception")
    void unsupportedObjectTypeShouldCreateProperException() {
      final MarshalingException exception =
          MarshalingException.unsupportedObjectType("UnsupportedClass");

      assertEquals(
          OperationType.TYPE_CONVERSION,
          exception.getOperationType(),
          "Operation type should be TYPE_CONVERSION");
      assertEquals("UnsupportedClass", exception.getObjectTypeName(), "Object type should match");
    }
  }

  @Nested
  @DisplayName("isRecoverable Tests")
  class IsRecoverableTests {

    @Test
    @DisplayName("MEMORY_ALLOCATION should be recoverable")
    void memoryAllocationShouldBeRecoverable() {
      final MarshalingException exception =
          MarshalingException.memoryAllocationFailure(1000, new java.lang.RuntimeException());

      assertTrue(exception.isRecoverable(), "MEMORY_ALLOCATION should be recoverable");
    }

    @Test
    @DisplayName("SERIALIZATION should not be recoverable")
    void serializationShouldNotBeRecoverable() {
      final MarshalingException exception =
          MarshalingException.serializationFailure("Type", new java.lang.RuntimeException());

      assertFalse(exception.isRecoverable(), "SERIALIZATION should not be recoverable");
    }

    @Test
    @DisplayName("DESERIALIZATION should not be recoverable")
    void deserializationShouldNotBeRecoverable() {
      final MarshalingException exception =
          MarshalingException.deserializationFailure("Type", new java.lang.RuntimeException());

      assertFalse(exception.isRecoverable(), "DESERIALIZATION should not be recoverable");
    }

    @Test
    @DisplayName("CIRCULAR_REFERENCE_DETECTION should be recoverable")
    void circularReferenceShouldBeRecoverable() {
      final MarshalingException exception = MarshalingException.circularReferenceDetected("Type");

      assertTrue(exception.isRecoverable(), "CIRCULAR_REFERENCE_DETECTION should be recoverable");
    }

    @Test
    @DisplayName("ARRAY_MARSHALING should be recoverable")
    void arrayMarshalingShouldBeRecoverable() {
      final MarshalingException exception =
          MarshalingException.arrayMarshalingFailure(
              "int[]", 100, new java.lang.RuntimeException());

      assertTrue(exception.isRecoverable(), "ARRAY_MARSHALING should be recoverable");
    }
  }

  @Nested
  @DisplayName("getRetryStrategies Tests")
  class GetRetryStrategiesTests {

    @Test
    @DisplayName("Recoverable operations should have retry strategies")
    void recoverableOperationsShouldHaveRetryStrategies() {
      final MarshalingException exception =
          MarshalingException.memoryAllocationFailure(1000, new java.lang.RuntimeException());

      final String[] strategies = exception.getRetryStrategies();
      assertTrue(strategies.length > 0, "Recoverable operations should have retry strategies");
    }

    @Test
    @DisplayName("Non-recoverable operations should have empty retry strategies")
    void nonRecoverableOperationsShouldHaveEmptyRetryStrategies() {
      final MarshalingException exception =
          MarshalingException.serializationFailure("Type", new java.lang.RuntimeException());

      final String[] strategies = exception.getRetryStrategies();
      assertEquals(
          0, strategies.length, "Non-recoverable operations should have empty retry strategies");
    }
  }

  @Nested
  @DisplayName("Message Formatting Tests")
  class MessageFormattingTests {

    @Test
    @DisplayName("Message should include operation type")
    void messageShouldIncludeOperationType() {
      final MarshalingException exception =
          new MarshalingException("Error", OperationType.TYPE_CONVERSION, "Type", -1, "Hint");

      assertTrue(
          exception.getMessage().contains("TYPE_CONVERSION"),
          "Message should contain operation type");
    }

    @Test
    @DisplayName("Message should include object type name")
    void messageShouldIncludeObjectTypeName() {
      final MarshalingException exception =
          new MarshalingException(
              "Error", OperationType.SERIALIZATION, "MyCustomClass", -1, "Hint");

      assertTrue(
          exception.getMessage().contains("MyCustomClass"),
          "Message should contain object type name");
    }

    @Test
    @DisplayName("Message should include size when positive")
    void messageShouldIncludeSizeWhenPositive() {
      final MarshalingException exception =
          new MarshalingException("Error", OperationType.MEMORY_ALLOCATION, "Type", 4096, "Hint");

      assertTrue(exception.getMessage().contains("4096"), "Message should contain size");
    }

    @Test
    @DisplayName("Message should include recovery hint")
    void messageShouldIncludeRecoveryHint() {
      final MarshalingException exception =
          new MarshalingException(
              "Error", OperationType.SERIALIZATION, "Type", -1, "Try different approach");

      assertTrue(
          exception.getMessage().contains("Try different approach"),
          "Message should contain recovery hint");
    }
  }
}
