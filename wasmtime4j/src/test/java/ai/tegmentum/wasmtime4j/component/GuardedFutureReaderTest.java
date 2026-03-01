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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GuardedFutureReader} class.
 *
 * <p>GuardedFutureReader wraps a FutureReader with use-after-close protection.
 */
@DisplayName("GuardedFutureReader Tests")
class GuardedFutureReaderTest {

  /** Configurable FutureReader stub for testing delegation and lifecycle. */
  private static class TestFutureReader implements FutureReader {
    private final long handle;
    private boolean resolved;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicInteger readCount = new AtomicInteger(0);
    private final AtomicInteger cancelCount = new AtomicInteger(0);

    TestFutureReader(final long handle) {
      this.handle = handle;
    }

    @Override
    public long getHandle() {
      return handle;
    }

    @Override
    public CompletableFuture<Optional<ComponentVal>> readAsync() {
      readCount.incrementAndGet();
      return CompletableFuture.completedFuture(Optional.of(ComponentVal.s32(42)));
    }

    @Override
    public void cancelRead() {
      cancelCount.incrementAndGet();
    }

    @Override
    public boolean isResolved() {
      return resolved;
    }

    void setResolved(final boolean resolved) {
      this.resolved = resolved;
    }

    @Override
    public void close() {
      closed.set(true);
    }

    boolean isClosed() {
      return closed.get();
    }

    int getReadCount() {
      return readCount.get();
    }

    int getCancelCount() {
      return cancelCount.get();
    }
  }

  @Nested
  @DisplayName("Construction Tests")
  class ConstructionTests {

    @Test
    @DisplayName("should create guarded reader from delegate")
    void shouldCreateFromDelegate() {
      final TestFutureReader delegate = new TestFutureReader(100L);
      final GuardedFutureReader guarded = new GuardedFutureReader(delegate);
      assertFalse(guarded.isClosed(), "New guarded reader should not be closed");
    }

    @Test
    @DisplayName("should throw on null delegate")
    void shouldThrowOnNullDelegate() {
      assertThrows(IllegalArgumentException.class, () -> new GuardedFutureReader(null));
    }
  }

  @Nested
  @DisplayName("Delegation Tests")
  class DelegationTests {

    @Test
    @DisplayName("getHandle should delegate")
    void getHandleShouldDelegate() {
      final TestFutureReader delegate = new TestFutureReader(555L);
      final GuardedFutureReader guarded = new GuardedFutureReader(delegate);
      assertEquals(555L, guarded.getHandle());
    }

    @Test
    @DisplayName("readAsync should delegate")
    void readAsyncShouldDelegate() throws WasmException {
      final TestFutureReader delegate = new TestFutureReader(100L);
      final GuardedFutureReader guarded = new GuardedFutureReader(delegate);
      guarded.readAsync();
      assertEquals(1, delegate.getReadCount(), "readAsync should have been delegated");
    }

    @Test
    @DisplayName("cancelRead should delegate")
    void cancelReadShouldDelegate() throws WasmException {
      final TestFutureReader delegate = new TestFutureReader(100L);
      final GuardedFutureReader guarded = new GuardedFutureReader(delegate);
      guarded.cancelRead();
      assertEquals(1, delegate.getCancelCount(), "cancelRead should have been delegated");
    }

    @Test
    @DisplayName("isResolved should delegate")
    void isResolvedShouldDelegate() {
      final TestFutureReader delegate = new TestFutureReader(100L);
      delegate.setResolved(true);
      final GuardedFutureReader guarded = new GuardedFutureReader(delegate);
      assertTrue(guarded.isResolved(), "isResolved should delegate to underlying reader");
    }
  }

  @Nested
  @DisplayName("Use-After-Close Protection Tests")
  class UseAfterCloseProtectionTests {

    @Test
    @DisplayName("getHandle should throw after close")
    void getHandleShouldThrowAfterClose() {
      final TestFutureReader delegate = new TestFutureReader(100L);
      final GuardedFutureReader guarded = new GuardedFutureReader(delegate);
      guarded.close();
      assertThrows(IllegalStateException.class, guarded::getHandle);
    }

    @Test
    @DisplayName("readAsync should throw after close")
    void readAsyncShouldThrowAfterClose() {
      final TestFutureReader delegate = new TestFutureReader(100L);
      final GuardedFutureReader guarded = new GuardedFutureReader(delegate);
      guarded.close();
      assertThrows(IllegalStateException.class, guarded::readAsync);
    }

    @Test
    @DisplayName("cancelRead should throw after close")
    void cancelReadShouldThrowAfterClose() {
      final TestFutureReader delegate = new TestFutureReader(100L);
      final GuardedFutureReader guarded = new GuardedFutureReader(delegate);
      guarded.close();
      assertThrows(IllegalStateException.class, guarded::cancelRead);
    }

    @Test
    @DisplayName("isResolved should throw after close")
    void isResolvedShouldThrowAfterClose() {
      final TestFutureReader delegate = new TestFutureReader(100L);
      final GuardedFutureReader guarded = new GuardedFutureReader(delegate);
      guarded.close();
      assertThrows(IllegalStateException.class, guarded::isResolved);
    }
  }

  @Nested
  @DisplayName("Close Behavior Tests")
  class CloseBehaviorTests {

    @Test
    @DisplayName("close should delegate to underlying reader")
    void closeShouldDelegate() {
      final TestFutureReader delegate = new TestFutureReader(100L);
      final GuardedFutureReader guarded = new GuardedFutureReader(delegate);
      guarded.close();
      assertTrue(delegate.isClosed(), "Close should delegate to underlying reader");
      assertTrue(guarded.isClosed(), "Guarded reader should be marked closed");
    }

    @Test
    @DisplayName("double close should be safe")
    void doubleCloseShouldBeSafe() {
      final TestFutureReader delegate = new TestFutureReader(100L);
      final GuardedFutureReader guarded = new GuardedFutureReader(delegate);
      guarded.close();
      guarded.close(); // should not throw
      assertTrue(guarded.isClosed());
    }
  }
}
