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

    @Test
    @DisplayName("COLLECTION_MARSHALING should be recoverable")
    void collectionMarshalingShouldBeRecoverable() {
      final MarshalingException exception =
          MarshalingException.collectionMarshalingFailure(
              "List", 100, new java.lang.RuntimeException());

      assertTrue(exception.isRecoverable(), "COLLECTION_MARSHALING should be recoverable");
    }

    @Test
    @DisplayName("TYPE_CONVERSION should not be recoverable")
    void typeConversionShouldNotBeRecoverable() {
      final MarshalingException exception =
          MarshalingException.typeConversionFailure("String", "Integer");

      assertFalse(exception.isRecoverable(), "TYPE_CONVERSION should not be recoverable");
    }

    @Test
    @DisplayName("MEMORY_LAYOUT should not be recoverable")
    void memoryLayoutShouldNotBeRecoverable() {
      final MarshalingException exception =
          new MarshalingException(
              "Memory layout error", OperationType.MEMORY_LAYOUT, "Struct", 256, "Check alignment");

      assertFalse(exception.isRecoverable(), "MEMORY_LAYOUT should not be recoverable");
    }

    @Test
    @DisplayName("OBJECT_GRAPH_TRAVERSAL should be recoverable")
    void objectGraphTraversalShouldBeRecoverable() {
      final MarshalingException exception =
          new MarshalingException(
              "Object graph too deep",
              OperationType.OBJECT_GRAPH_TRAVERSAL,
              "ComplexObject",
              -1,
              "Reduce depth");

      assertTrue(exception.isRecoverable(), "OBJECT_GRAPH_TRAVERSAL should be recoverable");
    }

    @Test
    @DisplayName("STRATEGY_SELECTION should be recoverable")
    void strategySelectionShouldBeRecoverable() {
      final MarshalingException exception =
          MarshalingException.objectTooLarge("LargeObject", 5000, 1000);

      assertTrue(exception.isRecoverable(), "STRATEGY_SELECTION should be recoverable");
    }

    @Test
    @DisplayName("Should have exactly 6 recoverable operation types")
    void shouldHaveExactly6RecoverableTypes() {
      int recoverableCount = 0;
      int nonRecoverableCount = 0;

      for (OperationType type : OperationType.values()) {
        final MarshalingException exception =
            new MarshalingException("Test", type, "Type", -1, "Hint");
        if (exception.isRecoverable()) {
          recoverableCount++;
        } else {
          nonRecoverableCount++;
        }
      }

      assertEquals(
          6,
          recoverableCount,
          "Should have exactly 6 recoverable types: "
              + "MEMORY_ALLOCATION, OBJECT_GRAPH_TRAVERSAL, STRATEGY_SELECTION, "
              + "CIRCULAR_REFERENCE_DETECTION, ARRAY_MARSHALING, COLLECTION_MARSHALING");
      assertEquals(
          4,
          nonRecoverableCount,
          "Should have exactly 4 non-recoverable types: "
              + "SERIALIZATION, DESERIALIZATION, TYPE_CONVERSION, MEMORY_LAYOUT");
    }

    @Test
    @DisplayName("All operation types should have defined recoverability")
    void allOperationTypesShouldHaveDefinedRecoverability() {
      // Verify no exception is thrown for any operation type
      for (OperationType type : OperationType.values()) {
        final MarshalingException exception =
            new MarshalingException("Test", type, "Type", -1, "Hint");
        // Should not throw - just verify it returns a boolean
        boolean result = exception.isRecoverable();
        assertNotNull(Boolean.valueOf(result), "isRecoverable should return a value for " + type);
      }
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

    @Test
    @DisplayName("MEMORY_ALLOCATION should have specific retry strategies")
    void memoryAllocationShouldHaveSpecificStrategies() {
      final MarshalingException exception =
          MarshalingException.memoryAllocationFailure(1000, new java.lang.RuntimeException());

      final String[] strategies = exception.getRetryStrategies();
      assertEquals(3, strategies.length, "MEMORY_ALLOCATION should have 3 strategies");
      assertEquals("Reduce object size", strategies[0], "First strategy should match");
      assertEquals("Use memory-based marshaling", strategies[1], "Second strategy should match");
      assertEquals("Increase available memory", strategies[2], "Third strategy should match");
    }

    @Test
    @DisplayName("OBJECT_GRAPH_TRAVERSAL should have specific retry strategies")
    void objectGraphTraversalShouldHaveSpecificStrategies() {
      final MarshalingException exception =
          new MarshalingException(
              "Graph too deep",
              OperationType.OBJECT_GRAPH_TRAVERSAL,
              "ComplexObject",
              -1,
              "Reduce depth");

      final String[] strategies = exception.getRetryStrategies();
      assertEquals(3, strategies.length, "OBJECT_GRAPH_TRAVERSAL should have 3 strategies");
      assertEquals("Reduce object graph depth", strategies[0], "First strategy should match");
      assertEquals(
          "Break complex objects into simpler parts",
          strategies[1],
          "Second strategy should match");
      assertEquals("Use custom serialization", strategies[2], "Third strategy should match");
    }

    @Test
    @DisplayName("CIRCULAR_REFERENCE_DETECTION should have specific retry strategies")
    void circularReferenceDetectionShouldHaveSpecificStrategies() {
      final MarshalingException exception = MarshalingException.circularReferenceDetected("Node");

      final String[] strategies = exception.getRetryStrategies();
      assertEquals(3, strategies.length, "CIRCULAR_REFERENCE_DETECTION should have 3 strategies");
      assertEquals(
          "Disable circular reference detection", strategies[0], "First strategy should match");
      assertEquals(
          "Break circular references manually", strategies[1], "Second strategy should match");
      assertEquals(
          "Use object references instead of embedded objects",
          strategies[2],
          "Third strategy should match");
    }

    @Test
    @DisplayName("STRATEGY_SELECTION should have specific retry strategies")
    void strategySelectionShouldHaveSpecificStrategies() {
      final MarshalingException exception =
          MarshalingException.objectTooLarge("LargeObject", 5000, 1000);

      final String[] strategies = exception.getRetryStrategies();
      assertEquals(3, strategies.length, "STRATEGY_SELECTION should have 3 strategies");
      assertEquals("Use memory-based marshaling", strategies[0], "First strategy should match");
      assertEquals("Enable streaming marshaling", strategies[1], "Second strategy should match");
      assertEquals(
          "Break object into smaller chunks", strategies[2], "Third strategy should match");
    }

    @Test
    @DisplayName("ARRAY_MARSHALING should have specific retry strategies")
    void arrayMarshalingShouldHaveSpecificStrategies() {
      final MarshalingException exception =
          MarshalingException.arrayMarshalingFailure(
              "int[]", 1000, new java.lang.RuntimeException());

      final String[] strategies = exception.getRetryStrategies();
      assertEquals(3, strategies.length, "ARRAY_MARSHALING should have 3 strategies");
      assertEquals("Use smaller data structures", strategies[0], "First strategy should match");
      assertEquals("Enable memory-based marshaling", strategies[1], "Second strategy should match");
      assertEquals(
          "Use lazy loading for large collections", strategies[2], "Third strategy should match");
    }

    @Test
    @DisplayName("COLLECTION_MARSHALING should have same strategies as ARRAY_MARSHALING")
    void collectionMarshalingShouldHaveSameStrategiesAsArrayMarshaling() {
      final MarshalingException arrayException =
          MarshalingException.arrayMarshalingFailure(
              "int[]", 1000, new java.lang.RuntimeException());
      final MarshalingException collectionException =
          MarshalingException.collectionMarshalingFailure(
              "List", 1000, new java.lang.RuntimeException());

      final String[] arrayStrategies = arrayException.getRetryStrategies();
      final String[] collectionStrategies = collectionException.getRetryStrategies();

      assertEquals(
          arrayStrategies.length,
          collectionStrategies.length,
          "ARRAY and COLLECTION marshaling should have same number of strategies");
      for (int i = 0; i < arrayStrategies.length; i++) {
        assertEquals(
            arrayStrategies[i],
            collectionStrategies[i],
            "Strategy " + i + " should match between ARRAY and COLLECTION marshaling");
      }
    }

    @Test
    @DisplayName("TYPE_CONVERSION should have empty retry strategies")
    void typeConversionShouldHaveEmptyStrategies() {
      final MarshalingException exception =
          MarshalingException.typeConversionFailure("String", "Integer");

      final String[] strategies = exception.getRetryStrategies();
      assertEquals(0, strategies.length, "TYPE_CONVERSION should have empty strategies");
    }

    @Test
    @DisplayName("DESERIALIZATION should have empty retry strategies")
    void deserializationShouldHaveEmptyStrategies() {
      final MarshalingException exception =
          MarshalingException.deserializationFailure("Type", new java.lang.RuntimeException());

      final String[] strategies = exception.getRetryStrategies();
      assertEquals(0, strategies.length, "DESERIALIZATION should have empty strategies");
    }

    @Test
    @DisplayName("MEMORY_LAYOUT should have empty retry strategies")
    void memoryLayoutShouldHaveEmptyStrategies() {
      final MarshalingException exception =
          new MarshalingException(
              "Layout error", OperationType.MEMORY_LAYOUT, "Struct", 256, "Check alignment");

      final String[] strategies = exception.getRetryStrategies();
      assertEquals(0, strategies.length, "MEMORY_LAYOUT should have empty strategies");
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

  @Nested
  @DisplayName("formatMessage Edge Case Tests for Mutation Coverage")
  class FormatMessageEdgeCaseTests {

    @Test
    @DisplayName("Message should not include operation section when operationType is null")
    void messageShouldNotIncludeOperationWhenNull() {
      final MarshalingException exception =
          new MarshalingException("Error message", null, "Type", -1, "Hint");

      assertFalse(
          exception.getMessage().contains("[Operation:"),
          "Message should not contain operation section when operationType is null");
      assertTrue(
          exception.getMessage().startsWith("Error message"),
          "Message should start with base message");
    }

    @Test
    @DisplayName("Message should not include type section when objectTypeName is empty")
    void messageShouldNotIncludeTypeWhenEmpty() {
      final MarshalingException exception =
          new MarshalingException("Error message", OperationType.SERIALIZATION, "", -1, "Hint");

      assertFalse(
          exception.getMessage().contains("[Type:"),
          "Message should not contain type section when objectTypeName is empty");
      assertTrue(
          exception.getMessage().contains("[Operation: SERIALIZATION]"),
          "Message should still contain operation section");
    }

    @Test
    @DisplayName("Message should not include type section when objectTypeName is null")
    void messageShouldNotIncludeTypeWhenNull() {
      final MarshalingException exception =
          new MarshalingException("Error message", OperationType.SERIALIZATION, null, -1, "Hint");

      assertFalse(
          exception.getMessage().contains("[Type:"),
          "Message should not contain type section when objectTypeName is null");
    }

    @Test
    @DisplayName("Message should include size section when estimatedSize is zero")
    void messageShouldIncludeSizeWhenZero() {
      final MarshalingException exception =
          new MarshalingException(
              "Error message", OperationType.MEMORY_ALLOCATION, "Type", 0, "Hint");

      assertTrue(
          exception.getMessage().contains("[Size: 0 bytes]"),
          "Message should contain size section when estimatedSize is 0 (boundary case)");
    }

    @Test
    @DisplayName("Message should not include size section when estimatedSize is negative")
    void messageShouldNotIncludeSizeWhenNegative() {
      final MarshalingException exception =
          new MarshalingException("Error message", OperationType.SERIALIZATION, "Type", -1, "Hint");

      assertFalse(
          exception.getMessage().contains("[Size:"),
          "Message should not contain size section when estimatedSize is negative");
    }

    @Test
    @DisplayName("Message should not include hint section when recoveryHint is empty")
    void messageShouldNotIncludeHintWhenEmpty() {
      final MarshalingException exception =
          new MarshalingException("Error message", OperationType.SERIALIZATION, "Type", -1, "");

      assertFalse(
          exception.getMessage().contains("[Hint:"),
          "Message should not contain hint section when recoveryHint is empty");
    }

    @Test
    @DisplayName("Message should not include hint section when recoveryHint is null")
    void messageShouldNotIncludeHintWhenNull() {
      final MarshalingException exception =
          new MarshalingException("Error message", OperationType.SERIALIZATION, "Type", -1, null);

      assertFalse(
          exception.getMessage().contains("[Hint:"),
          "Message should not contain hint section when recoveryHint is null");
    }

    @Test
    @DisplayName("Message with all null/empty optional fields should only contain base message")
    void messageWithAllNullFieldsShouldOnlyContainBaseMessage() {
      final MarshalingException exception =
          new MarshalingException("Base error only", null, "", -1, "");

      assertEquals(
          "Base error only",
          exception.getMessage(),
          "Message should only contain base message when all optional fields are null/empty");
    }

    @Test
    @DisplayName("Message with all valid fields should contain all sections")
    void messageWithAllValidFieldsShouldContainAllSections() {
      final MarshalingException exception =
          new MarshalingException(
              "Full error", OperationType.TYPE_CONVERSION, "MyClass", 1024, "Try again");

      final String message = exception.getMessage();
      assertTrue(message.contains("Full error"), "Message should contain base message");
      assertTrue(
          message.contains("[Operation: TYPE_CONVERSION]"),
          "Message should contain operation section");
      assertTrue(message.contains("[Type: MyClass]"), "Message should contain type section");
      assertTrue(message.contains("[Size: 1024 bytes]"), "Message should contain size section");
      assertTrue(message.contains("[Hint: Try again]"), "Message should contain hint section");
    }

    @Test
    @DisplayName("Message sections should appear in correct order")
    void messageSectionsShouldAppearInCorrectOrder() {
      final MarshalingException exception =
          new MarshalingException(
              "Order test", OperationType.SERIALIZATION, "TypeName", 512, "Recovery hint");

      final String message = exception.getMessage();
      final int operationIndex = message.indexOf("[Operation:");
      final int typeIndex = message.indexOf("[Type:");
      final int sizeIndex = message.indexOf("[Size:");
      final int hintIndex = message.indexOf("[Hint:");

      assertTrue(operationIndex < typeIndex, "Operation section should appear before type section");
      assertTrue(typeIndex < sizeIndex, "Type section should appear before size section");
      assertTrue(sizeIndex < hintIndex, "Size section should appear before hint section");
    }
  }

  @Nested
  @DisplayName("Constructor With Cause Mutation Tests")
  class ConstructorWithCauseMutationTests {

    @Test
    @DisplayName("Constructor with cause should store cause properly")
    void constructorWithCauseShouldStoreCauseProperly() {
      final Throwable cause = new IllegalArgumentException("Root cause");
      final MarshalingException exception =
          new MarshalingException(
              "Error with cause", cause, OperationType.TYPE_CONVERSION, "Type", 100, "Hint");

      assertSame(cause, exception.getCause(), "Cause should be stored properly");
    }

    @Test
    @DisplayName("Constructor with cause should format message same as without cause")
    void constructorWithCauseShouldFormatMessageSameAsWithoutCause() {
      final Throwable cause = new RuntimeException("Cause");
      final MarshalingException withCause =
          new MarshalingException(
              "Same message", cause, OperationType.SERIALIZATION, "SameType", 256, "Same hint");
      final MarshalingException withoutCause =
          new MarshalingException(
              "Same message", OperationType.SERIALIZATION, "SameType", 256, "Same hint");

      assertEquals(
          withoutCause.getMessage(),
          withCause.getMessage(),
          "Messages should be identical regardless of cause");
    }

    @Test
    @DisplayName("Constructor with null cause should not throw")
    void constructorWithNullCauseShouldNotThrow() {
      final MarshalingException exception =
          new MarshalingException("Error", null, OperationType.DESERIALIZATION, "Type", -1, "Hint");

      assertNotNull(exception, "Exception should be created with null cause");
      // null is a valid operationType parameter, not cause
    }
  }

  @Nested
  @DisplayName("Getter Methods Mutation Tests")
  class GetterMethodsMutationTests {

    @Test
    @DisplayName("getOperationType should return exact operation type passed to constructor")
    void getOperationTypeShouldReturnExactOperationType() {
      for (OperationType type : OperationType.values()) {
        final MarshalingException exception =
            new MarshalingException("Test", type, "Type", -1, "Hint");
        assertEquals(type, exception.getOperationType(), "getOperationType should return " + type);
      }
    }

    @Test
    @DisplayName("getObjectTypeName should return exact object type name passed to constructor")
    void getObjectTypeNameShouldReturnExactObjectTypeName() {
      final String[] testNames = {"MyClass", "int[]", "List<String>", "", null};
      for (String name : testNames) {
        final MarshalingException exception =
            new MarshalingException("Test", OperationType.SERIALIZATION, name, -1, "Hint");
        assertEquals(
            name, exception.getObjectTypeName(), "getObjectTypeName should return: " + name);
      }
    }

    @Test
    @DisplayName("getEstimatedSize should return exact size passed to constructor")
    void getEstimatedSizeShouldReturnExactSize() {
      final long[] testSizes = {-1, 0, 1, 100, 1024, Long.MAX_VALUE};
      for (long size : testSizes) {
        final MarshalingException exception =
            new MarshalingException("Test", OperationType.MEMORY_ALLOCATION, "Type", size, "Hint");
        assertEquals(size, exception.getEstimatedSize(), "getEstimatedSize should return: " + size);
      }
    }

    @Test
    @DisplayName("getRecoveryHint should return exact hint passed to constructor")
    void getRecoveryHintShouldReturnExactHint() {
      final String[] testHints = {"Try again", "Use different approach", "", null};
      for (String hint : testHints) {
        final MarshalingException exception =
            new MarshalingException("Test", OperationType.SERIALIZATION, "Type", -1, hint);
        assertEquals(hint, exception.getRecoveryHint(), "getRecoveryHint should return: " + hint);
      }
    }
  }

  @Nested
  @DisplayName("Factory Method Estimated Size Mutation Tests")
  class FactoryMethodEstimatedSizeMutationTests {

    @Test
    @DisplayName("serializationFailure should set estimatedSize to -1 (unknown)")
    void serializationFailureShouldSetEstimatedSizeToNegativeOne() {
      final MarshalingException exception =
          MarshalingException.serializationFailure("Type", new java.lang.RuntimeException());

      assertEquals(
          -1,
          exception.getEstimatedSize(),
          "serializationFailure should set estimatedSize to -1 to indicate unknown size");
      // Verify it's specifically -1, not 0 (the mutation would change -1 to 0)
      assertTrue(
          exception.getEstimatedSize() < 0,
          "serializationFailure estimatedSize should be negative");
    }

    @Test
    @DisplayName("deserializationFailure should set estimatedSize to -1 (unknown)")
    void deserializationFailureShouldSetEstimatedSizeToNegativeOne() {
      final MarshalingException exception =
          MarshalingException.deserializationFailure("Type", new java.lang.RuntimeException());

      assertEquals(
          -1,
          exception.getEstimatedSize(),
          "deserializationFailure should set estimatedSize to -1 to indicate unknown size");
      assertTrue(
          exception.getEstimatedSize() < 0,
          "deserializationFailure estimatedSize should be negative");
    }

    @Test
    @DisplayName("typeConversionFailure should set estimatedSize to -1 (unknown)")
    void typeConversionFailureShouldSetEstimatedSizeToNegativeOne() {
      final MarshalingException exception =
          MarshalingException.typeConversionFailure("String", "Integer");

      assertEquals(
          -1,
          exception.getEstimatedSize(),
          "typeConversionFailure should set estimatedSize to -1 to indicate unknown size");
      assertTrue(
          exception.getEstimatedSize() < 0,
          "typeConversionFailure estimatedSize should be negative");
    }

    @Test
    @DisplayName("circularReferenceDetected should set estimatedSize to -1 (unknown)")
    void circularReferenceDetectedShouldSetEstimatedSizeToNegativeOne() {
      final MarshalingException exception = MarshalingException.circularReferenceDetected("Node");

      assertEquals(
          -1,
          exception.getEstimatedSize(),
          "circularReferenceDetected should set estimatedSize to -1 to indicate unknown size");
      assertTrue(
          exception.getEstimatedSize() < 0,
          "circularReferenceDetected estimatedSize should be negative");
    }

    @Test
    @DisplayName("unsupportedObjectType should set estimatedSize to -1 (unknown)")
    void unsupportedObjectTypeShouldSetEstimatedSizeToNegativeOne() {
      final MarshalingException exception =
          MarshalingException.unsupportedObjectType("UnsupportedClass");

      assertEquals(
          -1,
          exception.getEstimatedSize(),
          "unsupportedObjectType should set estimatedSize to -1 to indicate unknown size");
      assertTrue(
          exception.getEstimatedSize() < 0,
          "unsupportedObjectType estimatedSize should be negative");
    }

    @Test
    @DisplayName("Factory methods with known size should NOT set -1")
    void factoryMethodsWithKnownSizeShouldNotSetNegativeOne() {
      // memoryAllocationFailure passes the requested size
      final MarshalingException memException =
          MarshalingException.memoryAllocationFailure(1000, new java.lang.RuntimeException());
      assertEquals(
          1000,
          memException.getEstimatedSize(),
          "memoryAllocationFailure should use provided size");
      assertFalse(
          memException.getEstimatedSize() < 0,
          "memoryAllocationFailure should have non-negative size");

      // arrayMarshalingFailure passes the array size
      final MarshalingException arrayException =
          MarshalingException.arrayMarshalingFailure(
              "int[]", 500, new java.lang.RuntimeException());
      assertEquals(
          500,
          arrayException.getEstimatedSize(),
          "arrayMarshalingFailure should use provided size");

      // collectionMarshalingFailure passes the collection size
      final MarshalingException collException =
          MarshalingException.collectionMarshalingFailure(
              "List", 750, new java.lang.RuntimeException());
      assertEquals(
          750,
          collException.getEstimatedSize(),
          "collectionMarshalingFailure should use provided size");

      // objectTooLarge passes the actual size
      final MarshalingException largeException =
          MarshalingException.objectTooLarge("LargeObject", 5000, 1000);
      assertEquals(
          5000, largeException.getEstimatedSize(), "objectTooLarge should use actual size");
    }
  }

  @Nested
  @DisplayName("objectTooLarge Format String Mutation Tests")
  class ObjectTooLargeFormatStringMutationTests {

    @Test
    @DisplayName("objectTooLarge message should contain actual size before max size")
    void objectTooLargeMessageShouldHaveCorrectOrder() {
      final MarshalingException exception =
          MarshalingException.objectTooLarge("LargeObject", 5000, 1000);

      final String message = exception.getMessage();
      // The message format is: "Object size (%d bytes) exceeds maximum allowed (%d bytes)"
      // First %d is actualSize (5000), second %d is maxAllowedSize (1000)
      final int actualSizeIndex = message.indexOf("5000");
      final int maxSizeIndex = message.indexOf("1000");

      assertTrue(actualSizeIndex >= 0, "Message should contain actual size 5000");
      assertTrue(maxSizeIndex >= 0, "Message should contain max size 1000");
      assertTrue(
          actualSizeIndex < maxSizeIndex,
          "Actual size (5000) should appear before max size (1000) in message");
    }

    @Test
    @DisplayName("objectTooLarge should use actualSize as first format argument")
    void objectTooLargeShouldUseActualSizeAsFirstArgument() {
      // Test with different values to ensure we're checking the right positions
      final MarshalingException exception = MarshalingException.objectTooLarge("Type", 12345, 500);

      final String message = exception.getMessage();
      // The pattern "Object size (12345 bytes)" proves actualSize is first argument
      assertTrue(
          message.contains("12345 bytes) exceeds"),
          "Message should have actualSize before 'exceeds': " + message);
    }

    @Test
    @DisplayName("objectTooLarge should use maxAllowedSize as second format argument")
    void objectTooLargeShouldUseMaxAllowedSizeAsSecondArgument() {
      final MarshalingException exception = MarshalingException.objectTooLarge("Type", 10000, 2500);

      final String message = exception.getMessage();
      // The pattern "maximum allowed (2500 bytes)" proves maxAllowedSize is second argument
      assertTrue(
          message.contains("maximum allowed (2500 bytes)"),
          "Message should have maxAllowedSize after 'maximum allowed': " + message);
    }

    @Test
    @DisplayName("objectTooLarge format arguments should not be swapped")
    void objectTooLargeFormatArgumentsShouldNotBeSwapped() {
      // Use distinct values that would produce different messages if swapped
      final MarshalingException exception = MarshalingException.objectTooLarge("Type", 9999, 1111);

      final String message = exception.getMessage();
      // Correct format: "Object size (9999 bytes) exceeds maximum allowed (1111 bytes)"
      // Swapped would be: "Object size (1111 bytes) exceeds maximum allowed (9999 bytes)"
      assertTrue(
          message.contains("(9999 bytes) exceeds"),
          "actualSize should be in 'Object size' position");
      assertTrue(
          message.contains("allowed (1111 bytes)"),
          "maxAllowedSize should be in 'maximum allowed' position");
    }

    @Test
    @DisplayName("objectTooLarge with equal values should still work correctly")
    void objectTooLargeWithEqualValuesShouldWork() {
      // Edge case: when actualSize equals maxAllowedSize
      final MarshalingException exception = MarshalingException.objectTooLarge("Type", 1000, 1000);

      final String message = exception.getMessage();
      // Should have "1000" twice in the message
      final int firstIndex = message.indexOf("1000");
      final int secondIndex = message.indexOf("1000", firstIndex + 1);
      assertTrue(firstIndex >= 0, "Should have first occurrence of size");
      assertTrue(secondIndex > firstIndex, "Should have second occurrence of size");
    }
  }
}
