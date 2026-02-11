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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ResourceException} class.
 *
 * <p>This test class verifies the construction and behavior of resource management exceptions.
 */
@DisplayName("ResourceException Tests")
class ResourceExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("ResourceException should extend WasmException")
    void shouldExtendWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(ResourceException.class),
          "ResourceException should extend WasmException");
    }

    @Test
    @DisplayName("ResourceException should be serializable")
    void shouldBeSerializable() {
      assertTrue(
          java.io.Serializable.class.isAssignableFrom(ResourceException.class),
          "ResourceException should be serializable");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with message only should set defaults")
    void constructorWithMessageOnlyShouldSetDefaults() {
      final ResourceException exception = new ResourceException("Resource error");

      assertEquals("Resource error", exception.getMessage(), "Message should be 'Resource error'");
      assertNull(exception.getResourceType(), "Resource type should be null");
      assertNull(exception.getResourceId(), "Resource ID should be null");
      assertFalse(exception.isCleanupRecommended(), "Cleanup should not be recommended by default");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("Constructor with message and cause should set both")
    void constructorWithMessageAndCauseShouldSetBoth() {
      final Throwable cause = new Exception("Root cause");
      final ResourceException exception = new ResourceException("Resource error", cause);

      assertEquals("Resource error", exception.getMessage(), "Message should be 'Resource error'");
      assertSame(cause, exception.getCause(), "Cause should be set");
      assertNull(exception.getResourceType(), "Resource type should be null");
    }

    @Test
    @DisplayName("Constructor with resource details should set cleanup recommended")
    void constructorWithResourceDetailsShouldSetCleanupRecommended() {
      final ResourceException exception =
          new ResourceException("Memory allocation failed", "memory", "heap_0x1234");

      assertEquals("Memory allocation failed", exception.getMessage(), "Message should match");
      assertEquals("memory", exception.getResourceType(), "Resource type should be 'memory'");
      assertEquals("heap_0x1234", exception.getResourceId(), "Resource ID should be 'heap_0x1234'");
      assertTrue(
          exception.isCleanupRecommended(),
          "Cleanup should be recommended when resource details provided");
    }

    @Test
    @DisplayName("Full constructor should set all fields")
    void fullConstructorShouldSetAllFields() {
      final Throwable cause = new Exception("Root cause");
      final ResourceException exception =
          new ResourceException("Handle error", "handle", "native_handle_42", true, cause);

      assertEquals("Handle error", exception.getMessage(), "Message should match");
      assertEquals("handle", exception.getResourceType(), "Resource type should be 'handle'");
      assertEquals(
          "native_handle_42",
          exception.getResourceId(),
          "Resource ID should be 'native_handle_42'");
      assertTrue(exception.isCleanupRecommended(), "Cleanup should be recommended");
      assertSame(cause, exception.getCause(), "Cause should be set");
    }

    @Test
    @DisplayName("Constructor should allow cleanup not recommended")
    void constructorShouldAllowCleanupNotRecommended() {
      final ResourceException exception = new ResourceException("Error", "type", "id", false, null);

      assertFalse(
          exception.isCleanupRecommended(), "Cleanup should not be recommended when set to false");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("getResourceType should return resource type")
    void getResourceTypeShouldReturnResourceType() {
      final ResourceException exception = new ResourceException("Error", "file_handle", "file.txt");

      assertEquals(
          "file_handle", exception.getResourceType(), "Resource type should be 'file_handle'");
    }

    @Test
    @DisplayName("getResourceId should return resource ID")
    void getResourceIdShouldReturnResourceId() {
      final ResourceException exception = new ResourceException("Error", "connection", "conn_123");

      assertEquals("conn_123", exception.getResourceId(), "Resource ID should be 'conn_123'");
    }

    @Test
    @DisplayName("isCleanupRecommended should return recommendation status")
    void isCleanupRecommendedShouldReturnRecommendationStatus() {
      final ResourceException withCleanup = new ResourceException("Error", "pool", "pool_1");
      final ResourceException withoutCleanup = new ResourceException("Error");

      assertTrue(
          withCleanup.isCleanupRecommended(),
          "Should recommend cleanup when resource details provided");
      assertFalse(
          withoutCleanup.isCleanupRecommended(), "Should not recommend cleanup when no details");
    }
  }

  @Nested
  @DisplayName("Resource Type Check Tests")
  class ResourceTypeCheckTests {

    @Test
    @DisplayName("isMemoryResourceError should return true for memory resources")
    void isMemoryResourceErrorShouldReturnTrueForMemoryResources() {
      final ResourceException lowerCase = new ResourceException("Error", "memory", "mem_1");
      final ResourceException mixed = new ResourceException("Error", "NativeMemory", "mem_2");
      final ResourceException upper = new ResourceException("Error", "MEMORY_BUFFER", "mem_3");

      assertTrue(lowerCase.isMemoryResourceError(), "lowercase 'memory' should be memory resource");
      assertTrue(mixed.isMemoryResourceError(), "'NativeMemory' should be memory resource");
      assertTrue(upper.isMemoryResourceError(), "'MEMORY_BUFFER' should be memory resource");
    }

    @Test
    @DisplayName("isMemoryResourceError should return false for non-memory resources")
    void isMemoryResourceErrorShouldReturnFalseForNonMemoryResources() {
      final ResourceException file = new ResourceException("Error", "file", "f1");
      final ResourceException none = new ResourceException("Error");

      assertFalse(file.isMemoryResourceError(), "'file' should not be memory resource");
      assertFalse(none.isMemoryResourceError(), "null type should not be memory resource");
    }

    @Test
    @DisplayName("isHandleResourceError should return true for handle resources")
    void isHandleResourceErrorShouldReturnTrueForHandleResources() {
      final ResourceException lowerCase = new ResourceException("Error", "handle", "h1");
      final ResourceException mixed = new ResourceException("Error", "FileHandle", "h2");
      final ResourceException upper = new ResourceException("Error", "NATIVE_HANDLE", "h3");

      assertTrue(lowerCase.isHandleResourceError(), "lowercase 'handle' should be handle resource");
      assertTrue(mixed.isHandleResourceError(), "'FileHandle' should be handle resource");
      assertTrue(upper.isHandleResourceError(), "'NATIVE_HANDLE' should be handle resource");
    }

    @Test
    @DisplayName("isHandleResourceError should return false for non-handle resources")
    void isHandleResourceErrorShouldReturnFalseForNonHandleResources() {
      final ResourceException memory = new ResourceException("Error", "memory", "m1");
      final ResourceException none = new ResourceException("Error");

      assertFalse(memory.isHandleResourceError(), "'memory' should not be handle resource");
      assertFalse(none.isHandleResourceError(), "null type should not be handle resource");
    }
  }

  @Nested
  @DisplayName("getResourceErrorDescription Tests")
  class GetResourceErrorDescriptionTests {

    @Test
    @DisplayName("Should include resource type when present")
    void shouldIncludeResourceTypeWhenPresent() {
      final ResourceException exception = new ResourceException("Error", "native_memory", "nm_1");

      final String description = exception.getResourceErrorDescription();

      assertTrue(
          description.contains("type: native_memory"), "Description should include resource type");
    }

    @Test
    @DisplayName("Should include resource ID when present")
    void shouldIncludeResourceIdWhenPresent() {
      final ResourceException exception = new ResourceException("Error", "buffer", "buf_456");

      final String description = exception.getResourceErrorDescription();

      assertTrue(description.contains("id: buf_456"), "Description should include resource ID");
    }

    @Test
    @DisplayName("Should include cleanup recommendation when true")
    void shouldIncludeCleanupRecommendationWhenTrue() {
      final ResourceException exception = new ResourceException("Error", "pool", "p1");

      final String description = exception.getResourceErrorDescription();

      assertTrue(
          description.contains("cleanup recommended"),
          "Description should include cleanup recommendation");
    }

    @Test
    @DisplayName("Should return basic description when no details")
    void shouldReturnBasicDescriptionWhenNoDetails() {
      final ResourceException exception = new ResourceException("Error");

      final String description = exception.getResourceErrorDescription();

      assertNotNull(description, "Description should not be null");
      assertTrue(
          description.contains("Resource error"), "Description should contain 'Resource error'");
      assertFalse(description.contains("type:"), "Description should not contain type when null");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle null resource type")
    void shouldHandleNullResourceType() {
      final ResourceException exception = new ResourceException("Error", null, "id_1", true, null);

      assertNull(exception.getResourceType(), "Resource type should be null");
      assertFalse(exception.isMemoryResourceError(), "Should not be memory error with null type");
      assertFalse(exception.isHandleResourceError(), "Should not be handle error with null type");
    }

    @Test
    @DisplayName("Should handle null resource ID")
    void shouldHandleNullResourceId() {
      final ResourceException exception = new ResourceException("Error", "type", null);

      assertNull(exception.getResourceId(), "Resource ID should be null");
    }
  }

  @Nested
  @DisplayName("Constructor Default Value Mutation Tests")
  class ConstructorDefaultValueMutationTests {

    @Test
    @DisplayName("Message+cause constructor should set cleanupRecommended to exactly false")
    void messageCauseConstructorShouldSetCleanupRecommendedToFalse() {
      // This kills line 59 mutation: Substituted 0 with 1 (false -> true)
      final Throwable cause = new RuntimeException("test cause");
      final ResourceException exception = new ResourceException("Error message", cause);

      // cleanupRecommended should be false, not true
      assertFalse(
          exception.isCleanupRecommended(),
          "Message+cause constructor should set cleanupRecommended to false, not true");
    }

    @Test
    @DisplayName("Message-only constructor should also set cleanupRecommended to false")
    void messageOnlyConstructorShouldSetCleanupRecommendedToFalse() {
      final ResourceException exception = new ResourceException("Error message");

      assertFalse(
          exception.isCleanupRecommended(),
          "Message-only constructor should set cleanupRecommended to false");
    }
  }

  @Nested
  @DisplayName("getResourceErrorDescription Conditional Mutation Tests")
  class GetResourceErrorDescriptionConditionalMutationTests {

    @Test
    @DisplayName("Description should NOT include id section when resourceId is null")
    void descriptionShouldNotIncludeIdSectionWhenResourceIdIsNull() {
      // This kills line 157 mutation: resourceId != null replaced with true
      // If mutated, it would try to append "[id: null]" to the description
      final ResourceException exception = new ResourceException("Error", "memory", null);

      final String description = exception.getResourceErrorDescription();

      // Should contain type but NOT id (since resourceId is null)
      assertTrue(description.contains("[type: memory]"), "Description should contain type section");
      assertFalse(
          description.contains("[id:"),
          "Description should NOT contain id section when resourceId is null");
      assertFalse(
          description.contains("null"),
          "Description should not contain 'null' as literal text for id");
    }

    @Test
    @DisplayName("Description should NOT include cleanup section when cleanupRecommended is false")
    void descriptionShouldNotIncludeCleanupSectionWhenCleanupRecommendedIsFalse() {
      // This kills line 161 mutation: cleanupRecommended replaced with true
      // If mutated, it would always append "[cleanup recommended]"
      final ResourceException exception =
          new ResourceException("Error", "handle", "h1", false, null);

      final String description = exception.getResourceErrorDescription();

      // Should contain type and id but NOT cleanup (since cleanupRecommended is false)
      assertTrue(description.contains("[type: handle]"), "Description should contain type section");
      assertTrue(description.contains("[id: h1]"), "Description should contain id section");
      assertFalse(
          description.contains("cleanup recommended"),
          "Description should NOT contain 'cleanup recommended' when cleanupRecommended is false");
    }

    @Test
    @DisplayName("Description with all null fields should be minimal")
    void descriptionWithAllNullFieldsShouldBeMinimal() {
      // Test both conditionals with null/false values
      final ResourceException exception = new ResourceException("Error");

      final String description = exception.getResourceErrorDescription();

      // Should only contain "Resource error" without any brackets
      assertEquals(
          "Resource error",
          description,
          "Description should be exactly 'Resource error' when all fields are null/false");
    }

    @Test
    @DisplayName("Description should include all sections when all fields are set")
    void descriptionShouldIncludeAllSectionsWhenAllFieldsAreSet() {
      final ResourceException exception =
          new ResourceException("Error", "file", "f123", true, null);

      final String description = exception.getResourceErrorDescription();

      // Should contain all three sections
      assertTrue(description.contains("[type: file]"), "Description should contain type section");
      assertTrue(description.contains("[id: f123]"), "Description should contain id section");
      assertTrue(
          description.contains("[cleanup recommended]"),
          "Description should contain cleanup section");
    }
  }
}
