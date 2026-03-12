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
package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ArenaResourceManager}.
 *
 * <p>These tests exercise the Java-side logic of arena resource management without requiring the
 * native library to be loaded.
 */
@DisplayName("ArenaResourceManager Tests")
final class ArenaResourceManagerTest {

  private static final Logger LOGGER = Logger.getLogger(ArenaResourceManagerTest.class.getName());

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create manager with default constructor")
    void shouldCreateManagerWithDefaultConstructor() {
      try (ArenaResourceManager manager = new ArenaResourceManager()) {
        assertNotNull(manager, "Manager should not be null");
        assertTrue(manager.isValid(), "Manager should be valid after creation");
        LOGGER.info("Created ArenaResourceManager with default constructor");
      }
    }

    @Test
    @DisplayName("Should create manager with explicit arena and tracking enabled")
    void shouldCreateManagerWithExplicitArenaAndTrackingEnabled() {
      try (Arena arena = Arena.ofShared()) {
        try (ArenaResourceManager manager = new ArenaResourceManager(arena, true)) {
          assertNotNull(manager, "Manager should not be null");
          assertTrue(manager.isValid(), "Manager should be valid");
          LOGGER.info("Created ArenaResourceManager with tracking enabled");
        }
      }
    }

    @Test
    @DisplayName("Should create manager with tracking disabled")
    void shouldCreateManagerWithTrackingDisabled() {
      try (Arena arena = Arena.ofShared()) {
        try (ArenaResourceManager manager = new ArenaResourceManager(arena, false)) {
          assertNotNull(manager, "Manager should not be null");
          assertTrue(manager.isValid(), "Manager should be valid with tracking disabled");
          LOGGER.info("Created ArenaResourceManager with tracking disabled");
        }
      }
    }

    @Test
    @DisplayName("Should throw NullPointerException for null arena")
    void shouldThrowNullPointerExceptionForNullArena() {
      assertThrows(
          NullPointerException.class,
          () -> new ArenaResourceManager(null, true),
          "Should throw NullPointerException for null arena");
      LOGGER.info("Correctly threw NullPointerException for null arena");
    }
  }

  @Nested
  @DisplayName("Arena Access Tests")
  class ArenaAccessTests {

    @Test
    @DisplayName("Should return non-null arena from getArena")
    void shouldReturnNonNullArena() {
      try (ArenaResourceManager manager = new ArenaResourceManager()) {
        Arena arena = manager.getArena();
        assertNotNull(arena, "getArena() should return non-null arena");
        LOGGER.info("getArena() returned non-null arena");
      }
    }

    @Test
    @DisplayName("Should return the provided arena from getArena")
    void shouldReturnProvidedArena() {
      try (Arena arena = Arena.ofShared()) {
        try (ArenaResourceManager manager = new ArenaResourceManager(arena, true)) {
          Arena result = manager.getArena();
          assertTrue(arena == result, "getArena() should return the same arena instance");
          LOGGER.info("getArena() returned the provided arena instance");
        }
      }
    }

    @Test
    @DisplayName("Should throw IllegalStateException when accessing arena after close")
    void shouldThrowWhenAccessingArenaAfterClose() {
      ArenaResourceManager manager = new ArenaResourceManager();
      manager.close();

      assertThrows(
          IllegalStateException.class,
          manager::getArena,
          "Should throw IllegalStateException when accessing arena after close");
      LOGGER.info("Correctly threw IllegalStateException on getArena() after close");
    }
  }

  @Nested
  @DisplayName("Close and Lifecycle Tests")
  class CloseAndLifecycleTests {

    @Test
    @DisplayName("Should transition isValid from true to false on close")
    void shouldTransitionIsValidOnClose() {
      ArenaResourceManager manager = new ArenaResourceManager();
      assertTrue(manager.isValid(), "Manager should be valid before close");

      manager.close();
      assertFalse(manager.isValid(), "Manager should not be valid after close");
      LOGGER.info("isValid() transitioned from true to false on close");
    }

    @Test
    @DisplayName("Should handle double close safely")
    void shouldHandleDoubleCloseSafely() {
      ArenaResourceManager manager = new ArenaResourceManager();

      assertDoesNotThrow(manager::close, "First close should not throw");
      assertDoesNotThrow(manager::close, "Second close should not throw");
      assertFalse(manager.isValid(), "Manager should not be valid after double close");
      LOGGER.info("Double close handled safely");
    }

    @Test
    @DisplayName("Should not close arena when not owned")
    void shouldNotCloseArenaWhenNotOwned() {
      try (Arena arena = Arena.ofShared()) {
        ArenaResourceManager manager = new ArenaResourceManager(arena, true);
        manager.close();

        // The arena should still be alive because the manager does not own it
        assertTrue(arena.scope().isAlive(), "Arena should still be alive after manager close");
        LOGGER.info("Arena remains alive after non-owning manager is closed");
      }
    }
  }

  @Nested
  @DisplayName("Resource Management Tests")
  class ResourceManagementTests {

    @Test
    @DisplayName("Should manage native resource with tracking enabled")
    void shouldManageNativeResourceWithTrackingEnabled() {
      try (Arena arena = Arena.ofShared()) {
        try (ArenaResourceManager manager = new ArenaResourceManager(arena, true)) {
          AtomicBoolean cleanupCalled = new AtomicBoolean(false);
          MemorySegment segment = arena.allocate(8);

          ArenaResourceManager.ManagedNativeResource resource =
              manager.manageNativeResource(segment, () -> cleanupCalled.set(true), "test-resource");

          assertNotNull(resource, "Managed resource should not be null");
          assertTrue(resource.isValid(), "Resource should be valid");
          assertNotNull(resource.resource(), "resource() should return the segment");
          LOGGER.info("Created managed native resource with tracking enabled");
        }
      }
    }

    @Test
    @DisplayName("Should manage native resource with tracking disabled")
    void shouldManageNativeResourceWithTrackingDisabled() {
      try (Arena arena = Arena.ofShared()) {
        try (ArenaResourceManager manager = new ArenaResourceManager(arena, false)) {
          AtomicBoolean cleanupCalled = new AtomicBoolean(false);
          MemorySegment segment = arena.allocate(8);

          ArenaResourceManager.ManagedNativeResource resource =
              manager.manageNativeResource(segment, () -> cleanupCalled.set(true), "test-resource");

          assertNotNull(resource, "Managed resource should not be null");
          assertTrue(resource.isValid(), "Resource should be valid");
          LOGGER.info("Created managed native resource with tracking disabled");
        }
      }
    }

    @Test
    @DisplayName("Should throw when managing resource after close")
    void shouldThrowWhenManagingResourceAfterClose() {
      Arena arena = Arena.ofShared();
      ArenaResourceManager manager = new ArenaResourceManager(arena, true);
      MemorySegment segment = arena.allocate(8);
      manager.close();

      assertThrows(
          IllegalStateException.class,
          () -> manager.manageNativeResource(segment, () -> {}, "too-late"),
          "Should throw IllegalStateException when managing resource after close");
      arena.close();
      LOGGER.info("Correctly rejected resource management after close");
    }

    @Test
    @DisplayName("Should throw NullPointerException for null native pointer")
    void shouldThrowForNullNativePointer() {
      try (Arena arena = Arena.ofShared()) {
        try (ArenaResourceManager manager = new ArenaResourceManager(arena, true)) {
          assertThrows(
              NullPointerException.class,
              () -> manager.manageNativeResource(null, () -> {}, "null-ptr"),
              "Should throw NullPointerException for null pointer");
          LOGGER.info("Correctly threw NullPointerException for null native pointer");
        }
      }
    }

    @Test
    @DisplayName("Should throw NullPointerException for null cleanup action")
    void shouldThrowForNullCleanupAction() {
      try (Arena arena = Arena.ofShared()) {
        try (ArenaResourceManager manager = new ArenaResourceManager(arena, true)) {
          MemorySegment segment = arena.allocate(8);
          assertThrows(
              NullPointerException.class,
              () -> manager.manageNativeResource(segment, null, "null-cleanup"),
              "Should throw NullPointerException for null cleanup");
          LOGGER.info("Correctly threw NullPointerException for null cleanup action");
        }
      }
    }

    @Test
    @DisplayName("Should throw NullPointerException for null description")
    void shouldThrowForNullDescription() {
      try (Arena arena = Arena.ofShared()) {
        try (ArenaResourceManager manager = new ArenaResourceManager(arena, true)) {
          MemorySegment segment = arena.allocate(8);
          assertThrows(
              NullPointerException.class,
              () -> manager.manageNativeResource(segment, () -> {}, null),
              "Should throw NullPointerException for null description");
          LOGGER.info("Correctly threw NullPointerException for null description");
        }
      }
    }
  }

  @Nested
  @DisplayName("Register Managed Native Resource Tests")
  class RegisterManagedNativeResourceTests {

    @Test
    @DisplayName("Should register managed native resource")
    void shouldRegisterManagedNativeResource() {
      try (Arena arena = Arena.ofShared()) {
        try (ArenaResourceManager manager = new ArenaResourceManager(arena, true)) {
          MemorySegment segment = arena.allocate(8);
          AtomicBoolean cleanupCalled = new AtomicBoolean(false);

          ArenaResourceManager.ManagedNativeResource resource =
              manager.registerManagedNativeResource(this, segment, () -> cleanupCalled.set(true));

          assertNotNull(resource, "Registered resource should not be null");
          assertTrue(resource.isValid(), "Registered resource should be valid");
          LOGGER.info("Registered managed native resource successfully");
        }
      }
    }

    @Test
    @DisplayName("Should throw for null owner")
    void shouldThrowForNullOwner() {
      try (Arena arena = Arena.ofShared()) {
        try (ArenaResourceManager manager = new ArenaResourceManager(arena, true)) {
          MemorySegment segment = arena.allocate(8);
          assertThrows(
              IllegalArgumentException.class,
              () -> manager.registerManagedNativeResource(null, segment, () -> {}),
              "Should throw IllegalArgumentException for null owner");
          LOGGER.info("Correctly rejected null owner");
        }
      }
    }

    @Test
    @DisplayName("Should throw for null native handle")
    void shouldThrowForNullNativeHandle() {
      try (Arena arena = Arena.ofShared()) {
        try (ArenaResourceManager manager = new ArenaResourceManager(arena, true)) {
          assertThrows(
              IllegalArgumentException.class,
              () -> manager.registerManagedNativeResource(this, null, () -> {}),
              "Should throw IllegalArgumentException for null native handle");
          LOGGER.info("Correctly rejected null native handle");
        }
      }
    }

    @Test
    @DisplayName("Should throw for NULL memory segment handle")
    void shouldThrowForNullMemorySegmentHandle() {
      try (Arena arena = Arena.ofShared()) {
        try (ArenaResourceManager manager = new ArenaResourceManager(arena, true)) {
          assertThrows(
              IllegalArgumentException.class,
              () -> manager.registerManagedNativeResource(this, MemorySegment.NULL, () -> {}),
              "Should throw IllegalArgumentException for NULL MemorySegment");
          LOGGER.info("Correctly rejected NULL MemorySegment handle");
        }
      }
    }

    @Test
    @DisplayName("Should throw for null cleanup action in register")
    void shouldThrowForNullCleanupActionInRegister() {
      try (Arena arena = Arena.ofShared()) {
        try (ArenaResourceManager manager = new ArenaResourceManager(arena, true)) {
          MemorySegment segment = arena.allocate(8);
          assertThrows(
              IllegalArgumentException.class,
              () -> manager.registerManagedNativeResource(this, segment, null),
              "Should throw IllegalArgumentException for null cleanup action");
          LOGGER.info("Correctly rejected null cleanup action");
        }
      }
    }

    @Test
    @DisplayName("Should throw when registering after close")
    void shouldThrowWhenRegisteringAfterClose() {
      Arena arena = Arena.ofShared();
      ArenaResourceManager manager = new ArenaResourceManager(arena, true);
      MemorySegment segment = arena.allocate(8);
      manager.close();

      assertThrows(
          IllegalStateException.class,
          () -> manager.registerManagedNativeResource(this, segment, () -> {}),
          "Should throw IllegalStateException when registering after close");
      arena.close();
      LOGGER.info("Correctly rejected registration after close");
    }
  }

  @Nested
  @DisplayName("ManagedNativeResource Tests")
  class ManagedNativeResourceTests {

    @Test
    @DisplayName("Should run cleanup on resource close")
    void shouldRunCleanupOnResourceClose() {
      try (Arena arena = Arena.ofShared()) {
        try (ArenaResourceManager manager = new ArenaResourceManager(arena, true)) {
          AtomicBoolean cleanupCalled = new AtomicBoolean(false);
          MemorySegment segment = arena.allocate(8);

          ArenaResourceManager.ManagedNativeResource resource =
              manager.manageNativeResource(segment, () -> cleanupCalled.set(true), "cleanup-test");

          assertTrue(resource.isValid(), "Resource should be valid before close");
          resource.close();
          assertFalse(resource.isValid(), "Resource should not be valid after close");
          assertTrue(cleanupCalled.get(), "Cleanup action should have been called");
          LOGGER.info("Resource cleanup executed on close");
        }
      }
    }

    @Test
    @DisplayName("Should handle double close on managed resource")
    void shouldHandleDoubleCloseOnManagedResource() {
      try (Arena arena = Arena.ofShared()) {
        try (ArenaResourceManager manager = new ArenaResourceManager(arena, true)) {
          AtomicBoolean cleanupCalled = new AtomicBoolean(false);
          MemorySegment segment = arena.allocate(8);

          ArenaResourceManager.ManagedNativeResource resource =
              manager.manageNativeResource(
                  segment,
                  () -> {
                    if (cleanupCalled.get()) {
                      throw new IllegalStateException("Cleanup called twice");
                    }
                    cleanupCalled.set(true);
                  },
                  "double-close-test");

          assertDoesNotThrow(resource::close, "First close should not throw");
          assertDoesNotThrow(resource::close, "Second close should not throw");
          assertTrue(cleanupCalled.get(), "Cleanup should have been called exactly once");
          LOGGER.info("Double close on managed resource handled safely");
        }
      }
    }

    @Test
    @DisplayName("Should throw when accessing resource after close")
    void shouldThrowWhenAccessingResourceAfterClose() {
      try (Arena arena = Arena.ofShared()) {
        try (ArenaResourceManager manager = new ArenaResourceManager(arena, true)) {
          MemorySegment segment = arena.allocate(8);

          ArenaResourceManager.ManagedNativeResource resource =
              manager.manageNativeResource(segment, () -> {}, "access-after-close");

          resource.close();
          assertThrows(
              IllegalStateException.class,
              resource::resource,
              "Should throw IllegalStateException when accessing closed resource");
          LOGGER.info("Correctly threw IllegalStateException on accessing closed resource");
        }
      }
    }

    @Test
    @DisplayName("Should return description from managed resource")
    void shouldReturnDescriptionFromManagedResource() {
      try (Arena arena = Arena.ofShared()) {
        try (ArenaResourceManager manager = new ArenaResourceManager(arena, true)) {
          MemorySegment segment = arena.allocate(8);

          ArenaResourceManager.ManagedNativeResource resource =
              manager.manageNativeResource(segment, () -> {}, "my-description");

          assertTrue(
              "my-description".equals(resource.getDescription()),
              "Description should match what was provided");
          LOGGER.info("Resource description: " + resource.getDescription());
        }
      }
    }

    @Test
    @DisplayName("Should include description and closed state in toString")
    void shouldIncludeDescriptionAndClosedStateInToString() {
      try (Arena arena = Arena.ofShared()) {
        try (ArenaResourceManager manager = new ArenaResourceManager(arena, true)) {
          MemorySegment segment = arena.allocate(8);

          ArenaResourceManager.ManagedNativeResource resource =
              manager.manageNativeResource(segment, () -> {}, "to-string-test");

          String str = resource.toString();
          assertTrue(str.contains("to-string-test"), "toString should contain description");
          assertTrue(str.contains("false"), "toString should show closed=false before close");

          resource.close();
          String closedStr = resource.toString();
          assertTrue(closedStr.contains("true"), "toString should show closed=true after close");
          LOGGER.info("toString output: " + closedStr);
        }
      }
    }
  }

  @Nested
  @DisplayName("Unregister Tests")
  class UnregisterTests {

    @Test
    @DisplayName("Should handle unregister with null owner gracefully")
    void shouldHandleUnregisterWithNullOwner() {
      try (Arena arena = Arena.ofShared()) {
        try (ArenaResourceManager manager = new ArenaResourceManager(arena, true)) {
          assertDoesNotThrow(
              () -> manager.unregisterManagedResource(null),
              "Unregistering null owner should not throw");
          LOGGER.info("Null owner unregister handled gracefully");
        }
      }
    }

    @Test
    @DisplayName("Should handle unregister when tracking is disabled")
    void shouldHandleUnregisterWhenTrackingDisabled() {
      try (Arena arena = Arena.ofShared()) {
        try (ArenaResourceManager manager = new ArenaResourceManager(arena, false)) {
          assertDoesNotThrow(
              () -> manager.unregisterManagedResource(this),
              "Unregistering with tracking disabled should not throw");
          LOGGER.info("Unregister with tracking disabled handled gracefully");
        }
      }
    }
  }
}
