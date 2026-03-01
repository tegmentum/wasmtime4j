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
package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for Component Model resource lifecycle management.
 *
 * <p>These tests verify resource handle creation and ownership semantics.
 *
 * @since 1.0.0
 */
@DisplayName("Component Resource Lifecycle Integration Tests")
public final class ComponentResourceLifecycleTest {

  private static final Logger LOGGER =
      Logger.getLogger(ComponentResourceLifecycleTest.class.getName());

  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down: " + testInfo.getDisplayName());
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  @Nested
  @DisplayName("Resource Handle Creation Tests")
  class ResourceHandleCreationTests {

    @Test
    @DisplayName("should create owned resource handle")
    void shouldCreateOwnedResourceHandle(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ComponentResourceHandle handle = ComponentResourceHandle.own("file-descriptor", 1);
      assertNotNull(handle, "Handle should not be null");
      assertEquals("file-descriptor", handle.getResourceType());
      assertEquals(1, handle.getIndex());
      assertTrue(handle.isOwned(), "Handle should be owned");
      assertFalse(handle.isBorrowed(), "Handle should not be borrowed");

      LOGGER.info("Created owned handle: " + handle);
    }

    @Test
    @DisplayName("should create borrowed resource handle")
    void shouldCreateBorrowedResourceHandle(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ComponentResourceHandle handle = ComponentResourceHandle.borrow("stream", 42);
      assertNotNull(handle, "Handle should not be null");
      assertEquals("stream", handle.getResourceType());
      assertEquals(42, handle.getIndex());
      assertFalse(handle.isOwned(), "Handle should not be owned");
      assertTrue(handle.isBorrowed(), "Handle should be borrowed");

      LOGGER.info("Created borrowed handle: " + handle);
    }

    @Test
    @DisplayName("should create owned handle with host object")
    void shouldCreateOwnedHandleWithHostObject(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      String hostObject = "test-host-object";
      ComponentResourceHandle handle =
          ComponentResourceHandle.ownWithHost("custom-resource", 5, hostObject);

      assertNotNull(handle, "Handle should not be null");
      assertTrue(handle.isOwned(), "Handle should be owned");
      assertEquals(hostObject, handle.getHostObject(String.class));

      LOGGER.info("Created owned handle with host object: " + handle);
    }

    @Test
    @DisplayName("should create borrowed handle with host object")
    void shouldCreateBorrowedHandleWithHostObject(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      Integer hostObject = 12345;
      ComponentResourceHandle handle =
          ComponentResourceHandle.borrowWithHost("number-resource", 10, hostObject);

      assertNotNull(handle, "Handle should not be null");
      assertTrue(handle.isBorrowed(), "Handle should be borrowed");
      assertEquals(hostObject, handle.getHostObject(Integer.class));

      LOGGER.info("Created borrowed handle with host object: " + handle);
    }

    @Test
    @DisplayName("should throw on null resource type")
    void shouldThrowOnNullResourceType(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentResourceHandle.own(null, 1),
          "Should throw on null resource type");
    }

    @Test
    @DisplayName("should throw when getting host object from handle without one")
    void shouldThrowWhenGettingHostObjectFromHandleWithoutOne(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ComponentResourceHandle handle = ComponentResourceHandle.own("test", 1);

      assertThrows(
          IllegalStateException.class,
          () -> handle.getHostObject(String.class),
          "Should throw when no host object");
    }
  }

  @Nested
  @DisplayName("Resource Handle Equality Tests")
  class ResourceHandleEqualityTests {

    @Test
    @DisplayName("should be equal for same type, index, and ownership")
    void shouldBeEqualForSameTypeIndexOwnership(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ComponentResourceHandle handle1 = ComponentResourceHandle.own("resource", 5);
      ComponentResourceHandle handle2 = ComponentResourceHandle.own("resource", 5);

      assertEquals(handle1, handle2, "Handles with same properties should be equal");
      assertEquals(handle1.hashCode(), handle2.hashCode(), "Hash codes should match");

      LOGGER.info("Equality verified for: " + handle1 + " and " + handle2);
    }

    @Test
    @DisplayName("should not be equal for different ownership")
    void shouldNotBeEqualForDifferentOwnership(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ComponentResourceHandle owned = ComponentResourceHandle.own("resource", 5);
      ComponentResourceHandle borrowed = ComponentResourceHandle.borrow("resource", 5);

      assertFalse(owned.equals(borrowed), "Owned and borrowed should not be equal");

      LOGGER.info("Different ownership verified: " + owned + " vs " + borrowed);
    }

    @Test
    @DisplayName("should not be equal for different indices")
    void shouldNotBeEqualForDifferentIndices(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ComponentResourceHandle handle1 = ComponentResourceHandle.own("resource", 1);
      ComponentResourceHandle handle2 = ComponentResourceHandle.own("resource", 2);

      assertFalse(handle1.equals(handle2), "Handles with different indices should not be equal");

      LOGGER.info("Different indices verified: " + handle1 + " vs " + handle2);
    }
  }
}
