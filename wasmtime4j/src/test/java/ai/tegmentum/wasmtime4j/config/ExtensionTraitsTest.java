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

package ai.tegmentum.wasmtime4j.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the extension trait interfaces: MemoryCreator, LinearMemory, StackCreator, StackMemory,
 * and CustomCodeMemory.
 *
 * <p>Validates the API surface, EngineConfig wiring, and basic implementations.
 *
 * @since 1.1.0
 */
@DisplayName("Extension Traits Tests")
class ExtensionTraitsTest {

  private static final Logger LOGGER = Logger.getLogger(ExtensionTraitsTest.class.getName());

  // ========== Test implementations ==========

  /** Simple LinearMemory implementation backed by tracking fields. */
  private static class TrackingLinearMemory implements LinearMemory {

    private long currentSize;
    private final long capacity;
    private final long basePtr;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    TrackingLinearMemory(final long initialSize, final long capacity, final long basePtr) {
      this.currentSize = initialSize;
      this.capacity = capacity;
      this.basePtr = basePtr;
    }

    @Override
    public long byteSize() {
      return currentSize;
    }

    @Override
    public long byteCapacity() {
      return capacity;
    }

    @Override
    public void growTo(final long newSize) throws WasmException {
      if (newSize > capacity) {
        throw new WasmException("Cannot grow beyond capacity: " + newSize + " > " + capacity);
      }
      LOGGER.fine("Growing memory from " + currentSize + " to " + newSize);
      currentSize = newSize;
    }

    @Override
    public long basePointer() {
      return basePtr;
    }

    @Override
    public void close() {
      closed.set(true);
      LOGGER.fine("LinearMemory closed");
    }

    boolean isClosed() {
      return closed.get();
    }
  }

  /** Simple MemoryCreator implementation that tracks invocations. */
  private static MemoryCreator createTrackingMemoryCreator(final AtomicLong invocationCount) {
    return (type, minimumBytes, maximumBytes, reservedSizeBytes, guardSizeBytes) -> {
      invocationCount.incrementAndGet();
      LOGGER.fine(
          "newMemory called: min="
              + minimumBytes
              + " max="
              + maximumBytes
              + " reserved="
              + reservedSizeBytes
              + " guard="
              + guardSizeBytes);
      final long capacity = maximumBytes.orElse(minimumBytes * 2);
      return new TrackingLinearMemory(minimumBytes, capacity, 0xDEADBEEFL);
    };
  }

  /** Simple StackMemory implementation. */
  private static class TrackingStackMemory implements StackMemory {

    private final long topAddr;
    private final long rangeStart;
    private final long rangeEnd;
    private final long guardStart;
    private final long guardEnd;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    TrackingStackMemory(
        final long topAddr,
        final long rangeStart,
        final long rangeEnd,
        final long guardStart,
        final long guardEnd) {
      this.topAddr = topAddr;
      this.rangeStart = rangeStart;
      this.rangeEnd = rangeEnd;
      this.guardStart = guardStart;
      this.guardEnd = guardEnd;
    }

    @Override
    public long top() {
      return topAddr;
    }

    @Override
    public long rangeStart() {
      return rangeStart;
    }

    @Override
    public long rangeEnd() {
      return rangeEnd;
    }

    @Override
    public long guardRangeStart() {
      return guardStart;
    }

    @Override
    public long guardRangeEnd() {
      return guardEnd;
    }

    @Override
    public void close() {
      closed.set(true);
      LOGGER.fine("StackMemory closed");
    }

    boolean isClosed() {
      return closed.get();
    }
  }

  /** Simple StackCreator implementation. */
  private static StackCreator createTrackingStackCreator(final AtomicLong invocationCount) {
    return (size, zeroed) -> {
      invocationCount.incrementAndGet();
      LOGGER.fine("newStack called: size=" + size + " zeroed=" + zeroed);
      final long guardSize = 4096;
      return new TrackingStackMemory(
          0x1000 + size, // top = base + size
          0x1000 + guardSize, // range starts after guard
          0x1000 + size, // range ends at top
          0x1000, // guard at bottom
          0x1000 + guardSize); // guard end
    };
  }

  /** Simple CustomCodeMemory implementation. */
  private static class TrackingCustomCodeMemory implements CustomCodeMemory {

    private final long alignment;
    private final AtomicLong publishCount = new AtomicLong(0);
    private final AtomicLong unpublishCount = new AtomicLong(0);

    TrackingCustomCodeMemory(final long alignment) {
      this.alignment = alignment;
    }

    @Override
    public long requiredAlignment() {
      return alignment;
    }

    @Override
    public void publishExecutable(final long ptr, final long len) {
      publishCount.incrementAndGet();
      LOGGER.fine("publishExecutable: ptr=" + ptr + " len=" + len);
    }

    @Override
    public void unpublishExecutable(final long ptr, final long len) {
      unpublishCount.incrementAndGet();
      LOGGER.fine("unpublishExecutable: ptr=" + ptr + " len=" + len);
    }

    long getPublishCount() {
      return publishCount.get();
    }

    long getUnpublishCount() {
      return unpublishCount.get();
    }
  }

  // ========== LinearMemory Tests ==========

  @Nested
  @DisplayName("LinearMemory Interface Tests")
  class LinearMemoryTests {

    @Test
    @DisplayName("byteSize returns initial size")
    void byteSizeReturnsInitialSize() {
      final TrackingLinearMemory mem = new TrackingLinearMemory(65536, 1048576, 0x1000);
      assertEquals(65536, mem.byteSize(), "Initial byte size should be 65536");
      LOGGER.info("PASS: byteSize returns initial size correctly");
    }

    @Test
    @DisplayName("byteCapacity returns configured capacity")
    void byteCapacityReturnsCapacity() {
      final TrackingLinearMemory mem = new TrackingLinearMemory(65536, 1048576, 0x1000);
      assertEquals(1048576, mem.byteCapacity(), "Capacity should be 1048576");
      LOGGER.info("PASS: byteCapacity returns configured capacity");
    }

    @Test
    @DisplayName("growTo increases size within capacity")
    void growToIncreasesSize() throws WasmException {
      final TrackingLinearMemory mem = new TrackingLinearMemory(65536, 1048576, 0x1000);
      mem.growTo(131072);
      assertEquals(131072, mem.byteSize(), "Size should be updated after growTo");
      LOGGER.info("PASS: growTo increases size within capacity");
    }

    @Test
    @DisplayName("growTo throws when exceeding capacity")
    void growToThrowsWhenExceedingCapacity() {
      final TrackingLinearMemory mem = new TrackingLinearMemory(65536, 1048576, 0x1000);
      assertThrows(
          WasmException.class,
          () -> mem.growTo(2097152),
          "growTo should throw when exceeding capacity");
      LOGGER.info("PASS: growTo throws when exceeding capacity");
    }

    @Test
    @DisplayName("basePointer returns configured address")
    void basePointerReturnsAddress() {
      final TrackingLinearMemory mem = new TrackingLinearMemory(65536, 1048576, 0xDEADBEEFL);
      assertEquals(0xDEADBEEFL, mem.basePointer(), "Base pointer should match configured value");
      LOGGER.info("PASS: basePointer returns configured address");
    }

    @Test
    @DisplayName("close marks memory as closed")
    void closeMarksMemoryAsClosed() {
      final TrackingLinearMemory mem = new TrackingLinearMemory(65536, 1048576, 0x1000);
      mem.close();
      assertTrue(mem.isClosed(), "Memory should be marked as closed after close()");
      LOGGER.info("PASS: close marks memory as closed");
    }

    @Test
    @DisplayName("LinearMemory is AutoCloseable")
    void linearMemoryIsAutoCloseable() {
      final TrackingLinearMemory mem;
      try (TrackingLinearMemory m = new TrackingLinearMemory(65536, 1048576, 0x1000)) {
        mem = m;
      }
      assertTrue(mem.isClosed(), "Memory should be closed by try-with-resources");
      LOGGER.info("PASS: LinearMemory works with try-with-resources");
    }
  }

  // ========== MemoryCreator Tests ==========

  @Nested
  @DisplayName("MemoryCreator Interface Tests")
  class MemoryCreatorTests {

    @Test
    @DisplayName("newMemory returns non-null LinearMemory")
    void newMemoryReturnsNonNull() {
      final AtomicLong count = new AtomicLong(0);
      final MemoryCreator creator = createTrackingMemoryCreator(count);
      final LinearMemory mem =
          creator.newMemory(null, 65536, OptionalLong.of(1048576), OptionalLong.empty(), 4096);
      assertNotNull(mem, "newMemory should return a non-null LinearMemory");
      assertEquals(1, count.get(), "Creator should have been invoked once");
      LOGGER.info("PASS: newMemory returns non-null LinearMemory");
    }

    @Test
    @DisplayName("newMemory passes maximum bytes correctly")
    void newMemoryPassesMaximum() {
      final AtomicLong count = new AtomicLong(0);
      final MemoryCreator creator = createTrackingMemoryCreator(count);
      final LinearMemory mem =
          creator.newMemory(null, 65536, OptionalLong.of(524288), OptionalLong.empty(), 0);
      assertEquals(524288, mem.byteCapacity(), "Capacity should match maximum bytes");
      LOGGER.info("PASS: newMemory passes maximum bytes correctly");
    }

    @Test
    @DisplayName("newMemory handles empty maximum correctly")
    void newMemoryHandlesEmptyMaximum() {
      final AtomicLong count = new AtomicLong(0);
      final MemoryCreator creator = createTrackingMemoryCreator(count);
      final LinearMemory mem =
          creator.newMemory(null, 65536, OptionalLong.empty(), OptionalLong.empty(), 0);
      assertEquals(
          65536 * 2, mem.byteCapacity(), "Capacity should be 2x minimum when max is empty");
      LOGGER.info("PASS: newMemory handles empty maximum correctly");
    }
  }

  // ========== StackMemory Tests ==========

  @Nested
  @DisplayName("StackMemory Interface Tests")
  class StackMemoryTests {

    @Test
    @DisplayName("top returns stack top address")
    void topReturnsAddress() {
      final TrackingStackMemory stack =
          new TrackingStackMemory(0x2000, 0x1100, 0x2000, 0x1000, 0x1100);
      assertEquals(0x2000, stack.top(), "Top should be at configured address");
      LOGGER.info("PASS: top returns stack top address");
    }

    @Test
    @DisplayName("rangeStart and rangeEnd define usable range")
    void rangeDefinesUsableRegion() {
      final TrackingStackMemory stack =
          new TrackingStackMemory(0x2000, 0x1100, 0x2000, 0x1000, 0x1100);
      assertEquals(0x1100, stack.rangeStart(), "Range start should skip guard pages");
      assertEquals(0x2000, stack.rangeEnd(), "Range end should equal top");
      assertTrue(stack.rangeEnd() > stack.rangeStart(), "Range end should be after range start");
      LOGGER.info("PASS: rangeStart and rangeEnd define usable range");
    }

    @Test
    @DisplayName("guardRangeStart and guardRangeEnd define guard region")
    void guardRangeDefinesGuardRegion() {
      final TrackingStackMemory stack =
          new TrackingStackMemory(0x2000, 0x1100, 0x2000, 0x1000, 0x1100);
      assertEquals(0x1000, stack.guardRangeStart(), "Guard range should start at base");
      assertEquals(0x1100, stack.guardRangeEnd(), "Guard range end should match range start");
      LOGGER.info("PASS: guard range defines guard region correctly");
    }

    @Test
    @DisplayName("close marks stack as closed")
    void closeMarksStackClosed() {
      final TrackingStackMemory stack =
          new TrackingStackMemory(0x2000, 0x1100, 0x2000, 0x1000, 0x1100);
      stack.close();
      assertTrue(stack.isClosed(), "Stack should be closed after close()");
      LOGGER.info("PASS: close marks stack as closed");
    }

    @Test
    @DisplayName("StackMemory is AutoCloseable")
    void stackMemoryIsAutoCloseable() {
      final TrackingStackMemory stack;
      try (TrackingStackMemory s =
          new TrackingStackMemory(0x2000, 0x1100, 0x2000, 0x1000, 0x1100)) {
        stack = s;
      }
      assertTrue(stack.isClosed(), "Stack should be closed by try-with-resources");
      LOGGER.info("PASS: StackMemory works with try-with-resources");
    }
  }

  // ========== StackCreator Tests ==========

  @Nested
  @DisplayName("StackCreator Interface Tests")
  class StackCreatorTests {

    @Test
    @DisplayName("newStack returns non-null StackMemory")
    void newStackReturnsNonNull() {
      final AtomicLong count = new AtomicLong(0);
      final StackCreator creator = createTrackingStackCreator(count);
      final StackMemory stack = creator.newStack(65536, true);
      assertNotNull(stack, "newStack should return a non-null StackMemory");
      assertEquals(1, count.get(), "Creator should have been invoked once");
      LOGGER.info("PASS: newStack returns non-null StackMemory");
    }

    @Test
    @DisplayName("newStack creates stack with correct top")
    void newStackHasCorrectTop() {
      final AtomicLong count = new AtomicLong(0);
      final StackCreator creator = createTrackingStackCreator(count);
      final StackMemory stack = creator.newStack(65536, false);
      assertEquals(0x1000 + 65536, stack.top(), "Top should be base + requested size");
      LOGGER.info("PASS: newStack creates stack with correct top");
    }
  }

  // ========== CustomCodeMemory Tests ==========

  @Nested
  @DisplayName("CustomCodeMemory Interface Tests")
  class CustomCodeMemoryTests {

    @Test
    @DisplayName("requiredAlignment returns configured value")
    void requiredAlignmentReturnsValue() {
      final TrackingCustomCodeMemory ccm = new TrackingCustomCodeMemory(4096);
      assertEquals(4096, ccm.requiredAlignment(), "Alignment should match configured value");
      LOGGER.info("PASS: requiredAlignment returns configured value");
    }

    @Test
    @DisplayName("publishExecutable increments counter")
    void publishExecutableIncrementsCounter() throws WasmException {
      final TrackingCustomCodeMemory ccm = new TrackingCustomCodeMemory(4096);
      ccm.publishExecutable(0x10000, 4096);
      assertEquals(1, ccm.getPublishCount(), "Publish count should be 1");
      LOGGER.info("PASS: publishExecutable increments counter");
    }

    @Test
    @DisplayName("unpublishExecutable increments counter")
    void unpublishExecutableIncrementsCounter() throws WasmException {
      final TrackingCustomCodeMemory ccm = new TrackingCustomCodeMemory(4096);
      ccm.unpublishExecutable(0x10000, 4096);
      assertEquals(1, ccm.getUnpublishCount(), "Unpublish count should be 1");
      LOGGER.info("PASS: unpublishExecutable increments counter");
    }

    @Test
    @DisplayName("publish and unpublish can be called multiple times")
    void publishUnpublishMultipleTimes() throws WasmException {
      final TrackingCustomCodeMemory ccm = new TrackingCustomCodeMemory(4096);
      ccm.publishExecutable(0x10000, 4096);
      ccm.unpublishExecutable(0x10000, 4096);
      ccm.publishExecutable(0x10000, 4096);
      assertEquals(2, ccm.getPublishCount(), "Publish count should be 2");
      assertEquals(1, ccm.getUnpublishCount(), "Unpublish count should be 1");
      LOGGER.info("PASS: publish/unpublish can be called multiple times");
    }
  }

  // ========== EngineConfig Integration Tests ==========

  @Nested
  @DisplayName("EngineConfig Extension Trait Integration Tests")
  class EngineConfigIntegrationTests {

    @Test
    @DisplayName("withHostMemory stores MemoryCreator reference")
    void withHostMemoryStoresReference() {
      final EngineConfig config = new EngineConfig();
      assertNull(config.getMemoryCreator(), "Default should be null");
      final MemoryCreator creator = createTrackingMemoryCreator(new AtomicLong(0));
      config.withHostMemory(creator);
      assertSame(creator, config.getMemoryCreator(), "Should store the MemoryCreator reference");
      LOGGER.info("PASS: withHostMemory stores MemoryCreator reference");
    }

    @Test
    @DisplayName("withHostStack stores StackCreator reference")
    void withHostStackStoresReference() {
      final EngineConfig config = new EngineConfig();
      assertNull(config.getStackCreator(), "Default should be null");
      final StackCreator creator = createTrackingStackCreator(new AtomicLong(0));
      config.withHostStack(creator);
      assertSame(creator, config.getStackCreator(), "Should store the StackCreator reference");
      LOGGER.info("PASS: withHostStack stores StackCreator reference");
    }

    @Test
    @DisplayName("withCustomCodeMemory stores CustomCodeMemory reference")
    void withCustomCodeMemoryStoresReference() {
      final EngineConfig config = new EngineConfig();
      assertNull(config.getCustomCodeMemory(), "Default should be null");
      final CustomCodeMemory ccm = new TrackingCustomCodeMemory(4096);
      config.withCustomCodeMemory(ccm);
      assertSame(ccm, config.getCustomCodeMemory(), "Should store the CustomCodeMemory reference");
      LOGGER.info("PASS: withCustomCodeMemory stores CustomCodeMemory reference");
    }

    @Test
    @DisplayName("withHostMemory supports method chaining")
    void withHostMemoryChaining() {
      final EngineConfig config = new EngineConfig();
      final EngineConfig result =
          config.withHostMemory(createTrackingMemoryCreator(new AtomicLong(0)));
      assertSame(config, result, "withHostMemory should return this for chaining");
      LOGGER.info("PASS: withHostMemory supports method chaining");
    }

    @Test
    @DisplayName("withHostStack supports method chaining")
    void withHostStackChaining() {
      final EngineConfig config = new EngineConfig();
      final EngineConfig result =
          config.withHostStack(createTrackingStackCreator(new AtomicLong(0)));
      assertSame(config, result, "withHostStack should return this for chaining");
      LOGGER.info("PASS: withHostStack supports method chaining");
    }

    @Test
    @DisplayName("withCustomCodeMemory supports method chaining")
    void withCustomCodeMemoryChaining() {
      final EngineConfig config = new EngineConfig();
      final EngineConfig result = config.withCustomCodeMemory(new TrackingCustomCodeMemory(4096));
      assertSame(config, result, "withCustomCodeMemory should return this for chaining");
      LOGGER.info("PASS: withCustomCodeMemory supports method chaining");
    }

    @Test
    @DisplayName("copy preserves all extension trait references")
    void copyPreservesExtensionTraits() {
      final EngineConfig config = new EngineConfig();
      final MemoryCreator mc = createTrackingMemoryCreator(new AtomicLong(0));
      final StackCreator sc = createTrackingStackCreator(new AtomicLong(0));
      final CustomCodeMemory ccm = new TrackingCustomCodeMemory(4096);
      config.withHostMemory(mc).withHostStack(sc).withCustomCodeMemory(ccm);

      final EngineConfig copy = config.copy();
      assertSame(mc, copy.getMemoryCreator(), "Copy should preserve MemoryCreator");
      assertSame(sc, copy.getStackCreator(), "Copy should preserve StackCreator");
      assertSame(ccm, copy.getCustomCodeMemory(), "Copy should preserve CustomCodeMemory");
      LOGGER.info("PASS: copy preserves all extension trait references");
    }

    @Test
    @DisplayName("toJson excludes transient extension trait fields")
    void toJsonExcludesExtensions() {
      final EngineConfig config = new EngineConfig();
      config
          .withHostMemory(createTrackingMemoryCreator(new AtomicLong(0)))
          .withHostStack(createTrackingStackCreator(new AtomicLong(0)))
          .withCustomCodeMemory(new TrackingCustomCodeMemory(4096));

      final String json = new String(config.toJson());
      assertTrue(!json.contains("memoryCreator"), "JSON should not contain memoryCreator");
      assertTrue(!json.contains("stackCreator"), "JSON should not contain stackCreator");
      assertTrue(!json.contains("customCodeMemory"), "JSON should not contain customCodeMemory");
      LOGGER.info("PASS: toJson excludes transient extension trait fields");
    }

    @Test
    @DisplayName("withHostMemory rejects null")
    void withHostMemoryRejectsNull() {
      final EngineConfig config = new EngineConfig();
      assertThrows(
          IllegalArgumentException.class,
          () -> config.withHostMemory(null),
          "Should reject null MemoryCreator");
      LOGGER.info("PASS: withHostMemory rejects null");
    }

    @Test
    @DisplayName("withHostStack rejects null")
    void withHostStackRejectsNull() {
      final EngineConfig config = new EngineConfig();
      assertThrows(
          IllegalArgumentException.class,
          () -> config.withHostStack(null),
          "Should reject null StackCreator");
      LOGGER.info("PASS: withHostStack rejects null");
    }

    @Test
    @DisplayName("withCustomCodeMemory rejects null")
    void withCustomCodeMemoryRejectsNull() {
      final EngineConfig config = new EngineConfig();
      assertThrows(
          IllegalArgumentException.class,
          () -> config.withCustomCodeMemory(null),
          "Should reject null CustomCodeMemory");
      LOGGER.info("PASS: withCustomCodeMemory rejects null");
    }
  }
}
