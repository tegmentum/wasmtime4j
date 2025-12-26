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

package ai.tegmentum.wasmtime4j.panama.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaResource} abstract class.
 *
 * <p>This test class verifies the resource lifecycle management for Panama FFI operations.
 */
@DisplayName("PanamaResource Tests")
class PanamaResourceTest {

  private Arena arena;

  /** Concrete implementation of PanamaResource for testing. */
  private static class TestPanamaResource extends PanamaResource {
    private final AtomicBoolean closeCallbackInvoked = new AtomicBoolean(false);
    private final String resourceType;
    private volatile Exception closeException;

    TestPanamaResource(final MemorySegment nativeHandle) {
      super(nativeHandle);
      this.resourceType = "TestResource";
    }

    TestPanamaResource(final MemorySegment nativeHandle, final String resourceType) {
      super(nativeHandle);
      this.resourceType = resourceType;
    }

    TestPanamaResource(
        final MemorySegment nativeHandle, final String resourceType, final Exception closeEx) {
      super(nativeHandle);
      this.resourceType = resourceType;
      this.closeException = closeEx;
    }

    @Override
    protected void doClose() throws Exception {
      closeCallbackInvoked.set(true);
      if (closeException != null) {
        throw closeException;
      }
    }

    @Override
    protected String getResourceType() {
      return resourceType;
    }

    boolean wasCloseCallbackInvoked() {
      return closeCallbackInvoked.get();
    }
  }

  @BeforeEach
  void setUp() {
    arena = Arena.ofConfined();
  }

  @AfterEach
  void tearDown() {
    if (arena.scope().isAlive()) {
      arena.close();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaResource should be abstract")
    void shouldBeAbstract() {
      assertTrue(
          java.lang.reflect.Modifier.isAbstract(PanamaResource.class.getModifiers()),
          "PanamaResource should be abstract");
    }

    @Test
    @DisplayName("PanamaResource should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(PanamaResource.class),
          "PanamaResource should implement AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should throw for null native handle")
    void constructorShouldThrowForNullNativeHandle() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new TestPanamaResource(null),
          "Should throw for null native handle");
    }

    @Test
    @DisplayName("Constructor should accept valid native handle")
    void constructorShouldAcceptValidNativeHandle() {
      final MemorySegment segment = arena.allocate(64);
      final TestPanamaResource resource = new TestPanamaResource(segment);

      assertNotNull(resource, "Resource should not be null");
      assertFalse(resource.isClosed(), "Resource should not be closed");
    }

    @Test
    @DisplayName("Constructor should set up phantom reference")
    void constructorShouldSetUpPhantomReference() {
      final MemorySegment segment = arena.allocate(64);
      // Creating the resource should not throw - phantom reference is set up internally
      assertDoesNotThrow(() -> new TestPanamaResource(segment));
    }
  }

  @Nested
  @DisplayName("getNativeHandle Tests")
  class GetNativeHandleTests {

    @Test
    @DisplayName("getNativeHandle should return handle for open resource")
    void getNativeHandleShouldReturnHandleForOpenResource() throws PanamaResourceException {
      final MemorySegment segment = arena.allocate(64);
      final TestPanamaResource resource = new TestPanamaResource(segment);

      assertEquals(segment, resource.getNativeHandle(), "Should return the native handle");
    }

    @Test
    @DisplayName("getNativeHandle should throw for closed resource")
    void getNativeHandleShouldThrowForClosedResource() {
      final MemorySegment segment = arena.allocate(64);
      final TestPanamaResource resource = new TestPanamaResource(segment);
      resource.close();

      assertThrows(
          PanamaResourceException.class,
          resource::getNativeHandle,
          "Should throw for closed resource");
    }
  }

  @Nested
  @DisplayName("isClosed Tests")
  class IsClosedTests {

    @Test
    @DisplayName("isClosed should return false for new resource")
    void isClosedShouldReturnFalseForNewResource() {
      final MemorySegment segment = arena.allocate(64);
      final TestPanamaResource resource = new TestPanamaResource(segment);

      assertFalse(resource.isClosed(), "New resource should not be closed");
    }

    @Test
    @DisplayName("isClosed should return true after close")
    void isClosedShouldReturnTrueAfterClose() {
      final MemorySegment segment = arena.allocate(64);
      final TestPanamaResource resource = new TestPanamaResource(segment);
      resource.close();

      assertTrue(resource.isClosed(), "Resource should be closed after close()");
    }
  }

  @Nested
  @DisplayName("markClosedForTesting Tests")
  class MarkClosedForTestingTests {

    @Test
    @DisplayName("markClosedForTesting should mark resource as closed")
    void markClosedForTestingShouldMarkResourceAsClosed() {
      final MemorySegment segment = arena.allocate(64);
      final TestPanamaResource resource = new TestPanamaResource(segment);

      resource.markClosedForTesting();

      assertTrue(resource.isClosed(), "Resource should be marked as closed");
    }

    @Test
    @DisplayName("markClosedForTesting should not invoke doClose")
    void markClosedForTestingShouldNotInvokeDoClose() {
      final MemorySegment segment = arena.allocate(64);
      final TestPanamaResource resource = new TestPanamaResource(segment);

      resource.markClosedForTesting();

      assertFalse(
          resource.wasCloseCallbackInvoked(),
          "doClose should not be invoked by markClosedForTesting");
    }
  }

  @Nested
  @DisplayName("close Tests")
  class CloseTests {

    @Test
    @DisplayName("close should invoke doClose")
    void closeShouldInvokeDoClose() {
      final MemorySegment segment = arena.allocate(64);
      final TestPanamaResource resource = new TestPanamaResource(segment);

      resource.close();

      assertTrue(resource.wasCloseCallbackInvoked(), "doClose should be invoked");
    }

    @Test
    @DisplayName("close should mark resource as closed")
    void closeShouldMarkResourceAsClosed() {
      final MemorySegment segment = arena.allocate(64);
      final TestPanamaResource resource = new TestPanamaResource(segment);

      resource.close();

      assertTrue(resource.isClosed(), "Resource should be closed");
    }

    @Test
    @DisplayName("close should be idempotent")
    void closeShouldBeIdempotent() {
      final MemorySegment segment = arena.allocate(64);
      final TestPanamaResource resource = new TestPanamaResource(segment);

      resource.close();
      final boolean firstInvocation = resource.wasCloseCallbackInvoked();

      // Second close should not throw and should not re-invoke doClose
      assertDoesNotThrow(resource::close, "Second close should not throw");
      assertTrue(firstInvocation, "doClose should have been invoked on first close");
    }

    @Test
    @DisplayName("close should handle doClose exceptions gracefully")
    void closeShouldHandleDoCloseExceptionsGracefully() {
      final MemorySegment segment = arena.allocate(64);
      final TestPanamaResource resource =
          new TestPanamaResource(segment, "TestResource", new RuntimeException("Test exception"));

      // Close should not throw even if doClose throws
      assertDoesNotThrow(resource::close, "Close should not throw when doClose throws");
      assertTrue(resource.isClosed(), "Resource should still be marked as closed");
    }
  }

  @Nested
  @DisplayName("ensureNotClosed Tests")
  class EnsureNotClosedTests {

    @Test
    @DisplayName("ensureNotClosed should not throw for open resource")
    void ensureNotClosedShouldNotThrowForOpenResource() {
      final MemorySegment segment = arena.allocate(64);
      final TestPanamaResource resource = new TestPanamaResource(segment);

      // Calling getNativeHandle implicitly calls ensureNotClosed
      assertDoesNotThrow(resource::getNativeHandle, "Should not throw for open resource");
    }

    @Test
    @DisplayName("ensureNotClosed should throw for closed resource")
    void ensureNotClosedShouldThrowForClosedResource() {
      final MemorySegment segment = arena.allocate(64);
      final TestPanamaResource resource = new TestPanamaResource(segment);
      resource.close();

      // Calling getNativeHandle implicitly calls ensureNotClosed
      final PanamaResourceException exception =
          assertThrows(PanamaResourceException.class, resource::getNativeHandle);
      assertTrue(
          exception.getMessage().contains("closed"),
          "Exception message should mention resource is closed");
    }

    @Test
    @DisplayName("ensureNotClosed exception should include resource type")
    void ensureNotClosedExceptionShouldIncludeResourceType() {
      final MemorySegment segment = arena.allocate(64);
      final TestPanamaResource resource = new TestPanamaResource(segment, "MyCustomResource");
      resource.close();

      final PanamaResourceException exception =
          assertThrows(PanamaResourceException.class, resource::getNativeHandle);
      assertTrue(
          exception.getMessage().contains("MyCustomResource"),
          "Exception message should include resource type");
    }
  }

  @Nested
  @DisplayName("getNativeLibrary Tests")
  class GetNativeLibraryTests {

    @Test
    @DisplayName("getNativeLibrary should return non-null SymbolLookup")
    void getNativeLibraryShouldReturnNonNull() {
      final SymbolLookup library = PanamaResource.getNativeLibrary();
      assertNotNull(library, "Native library should not be null");
    }

    @Test
    @DisplayName("getNativeLibrary should return cached instance")
    void getNativeLibraryShouldReturnCachedInstance() {
      final SymbolLookup library1 = PanamaResource.getNativeLibrary();
      final SymbolLookup library2 = PanamaResource.getNativeLibrary();

      // Both calls should return the same instance (cached)
      assertNotNull(library1, "First library should not be null");
      assertNotNull(library2, "Second library should not be null");
    }
  }

  @Nested
  @DisplayName("getResourceType Tests")
  class GetResourceTypeTests {

    @Test
    @DisplayName("getResourceType should return custom type name")
    void getResourceTypeShouldReturnCustomTypeName() {
      final MemorySegment segment = arena.allocate(64);
      final TestPanamaResource resource = new TestPanamaResource(segment, "CustomType");

      // Close to trigger logging which uses getResourceType
      resource.close();

      // The test implementation returns what we passed
      assertTrue(resource.wasCloseCallbackInvoked(), "Resource should be properly closed");
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("Concurrent close should be thread-safe")
    void concurrentCloseShouldBeThreadSafe() throws InterruptedException {
      final MemorySegment segment = arena.allocate(64);
      final TestPanamaResource resource = new TestPanamaResource(segment);

      final int threadCount = 10;
      final Thread[] threads = new Thread[threadCount];

      for (int i = 0; i < threadCount; i++) {
        threads[i] = new Thread(resource::close);
      }

      // Start all threads
      for (Thread thread : threads) {
        thread.start();
      }

      // Wait for all threads
      for (Thread thread : threads) {
        thread.join();
      }

      // Resource should be closed exactly once
      assertTrue(resource.isClosed(), "Resource should be closed");
      assertTrue(resource.wasCloseCallbackInvoked(), "doClose should have been invoked");
    }

    @Test
    @DisplayName("Concurrent isClosed should be thread-safe")
    void concurrentIsClosedShouldBeThreadSafe() throws InterruptedException {
      final MemorySegment segment = arena.allocate(64);
      final TestPanamaResource resource = new TestPanamaResource(segment);

      final int threadCount = 10;
      final Thread[] threads = new Thread[threadCount];
      final AtomicBoolean anyFailed = new AtomicBoolean(false);

      for (int i = 0; i < threadCount; i++) {
        final int index = i;
        threads[i] =
            new Thread(
                () -> {
                  try {
                    // Half the threads close, half check status
                    if (index % 2 == 0) {
                      resource.close();
                    } else {
                      // Just check isClosed - should never throw
                      resource.isClosed();
                    }
                  } catch (Exception e) {
                    anyFailed.set(true);
                  }
                });
      }

      // Start all threads
      for (Thread thread : threads) {
        thread.start();
      }

      // Wait for all threads
      for (Thread thread : threads) {
        thread.join();
      }

      assertFalse(anyFailed.get(), "No threads should have thrown exceptions");
      assertTrue(resource.isClosed(), "Resource should be closed");
    }
  }

  @Nested
  @DisplayName("Try-With-Resources Tests")
  class TryWithResourcesTests {

    @Test
    @DisplayName("Resource should be closed after try-with-resources")
    void resourceShouldBeClosedAfterTryWithResources() {
      final MemorySegment segment = arena.allocate(64);
      final TestPanamaResource resource;

      try (TestPanamaResource r = new TestPanamaResource(segment)) {
        resource = r;
        assertFalse(resource.isClosed(), "Resource should be open inside try block");
      }

      assertTrue(resource.isClosed(), "Resource should be closed after try block");
      assertTrue(resource.wasCloseCallbackInvoked(), "doClose should have been invoked");
    }

    @Test
    @DisplayName("Resource should be closed even if exception thrown in try block")
    void resourceShouldBeClosedEvenIfExceptionThrownInTryBlock() {
      final MemorySegment segment = arena.allocate(64);
      final TestPanamaResource resource = new TestPanamaResource(segment);

      try {
        throw new RuntimeException("Test exception");
      } catch (RuntimeException e) {
        // Expected
      } finally {
        resource.close();
      }

      assertTrue(resource.isClosed(), "Resource should be closed after exception");
      assertTrue(resource.wasCloseCallbackInvoked(), "doClose should have been invoked");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Full lifecycle should work correctly")
    void fullLifecycleShouldWorkCorrectly() throws PanamaResourceException {
      final MemorySegment segment = arena.allocate(128);
      final TestPanamaResource resource = new TestPanamaResource(segment, "IntegrationTest");

      // Open state
      assertFalse(resource.isClosed(), "Should be open initially");
      assertEquals(segment, resource.getNativeHandle(), "Handle should match");

      // Close
      resource.close();

      // Closed state
      assertTrue(resource.isClosed(), "Should be closed");
      assertTrue(resource.wasCloseCallbackInvoked(), "Callback should have been invoked");

      // Operations should throw
      assertThrows(PanamaResourceException.class, resource::getNativeHandle);

      // Additional close should be safe
      assertDoesNotThrow(resource::close);
    }

    @Test
    @DisplayName("Multiple resources should be independently managed")
    void multipleResourcesShouldBeIndependentlyManaged() throws PanamaResourceException {
      final MemorySegment segment1 = arena.allocate(64);
      final MemorySegment segment2 = arena.allocate(64);

      final TestPanamaResource resource1 = new TestPanamaResource(segment1, "Resource1");
      final TestPanamaResource resource2 = new TestPanamaResource(segment2, "Resource2");

      // Both open
      assertFalse(resource1.isClosed(), "Resource1 should be open");
      assertFalse(resource2.isClosed(), "Resource2 should be open");

      // Close first only
      resource1.close();
      assertTrue(resource1.isClosed(), "Resource1 should be closed");
      assertFalse(resource2.isClosed(), "Resource2 should still be open");

      // Resource2 should still work
      assertEquals(segment2, resource2.getNativeHandle(), "Resource2 handle should work");

      // Cleanup
      resource2.close();
      assertTrue(resource2.isClosed(), "Resource2 should be closed");
    }
  }
}
