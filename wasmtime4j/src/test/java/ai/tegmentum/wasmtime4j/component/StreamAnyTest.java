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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StreamAny} interface and its default implementation.
 *
 * <p>StreamAny is a type-erased handle for Component Model async streams.
 */
@DisplayName("StreamAny Tests")
class StreamAnyTest {

  /** A minimal StreamReader stub for testing StreamAny wrappers. */
  private static class StubStreamReader implements StreamReader {
    private final long handle;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    StubStreamReader(final long handle) {
      this.handle = handle;
    }

    @Override
    public long getHandle() {
      return handle;
    }

    @Override
    public CompletableFuture<StreamResult> readAsync(final int maxCount) {
      return CompletableFuture.completedFuture(new StreamResult(Collections.emptyList(), false));
    }

    @Override
    public void cancelRead() {
      // no-op for stub
    }

    @Override
    public boolean isClosed() {
      return closed.get();
    }

    @Override
    public void close() {
      closed.set(true);
    }
  }

  @Nested
  @DisplayName("tryFromStreamReader Tests")
  class TryFromStreamReaderTests {

    @Test
    @DisplayName("should create StreamAny from StreamReader")
    void shouldCreateFromStreamReader() {
      final StubStreamReader reader = new StubStreamReader(123L);
      final StreamAny streamAny = StreamAny.tryFromStreamReader(reader);
      assertNotNull(streamAny);
      assertEquals(123L, streamAny.getHandle());
    }

    @Test
    @DisplayName("should throw on null reader")
    void shouldThrowOnNullReader() {
      assertThrows(IllegalArgumentException.class, () -> StreamAny.tryFromStreamReader(null));
    }
  }

  @Nested
  @DisplayName("DefaultStreamAny Tests")
  class DefaultStreamAnyTests {

    @Test
    @DisplayName("should convert back to StreamReader")
    void shouldConvertBackToStreamReader() throws WasmException {
      final StubStreamReader reader = new StubStreamReader(456L);
      final StreamAny streamAny = StreamAny.tryFromStreamReader(reader);
      final StreamReader recovered = streamAny.tryIntoStreamReader();
      assertEquals(reader, recovered, "Should return the original reader");
    }

    @Test
    @DisplayName("should throw on tryIntoStreamReader after close")
    void shouldThrowOnTryIntoStreamReaderAfterClose() {
      final StubStreamReader reader = new StubStreamReader(789L);
      final StreamAny streamAny = StreamAny.tryFromStreamReader(reader);
      streamAny.close();
      assertThrows(WasmException.class, streamAny::tryIntoStreamReader);
    }

    @Test
    @DisplayName("close should delegate to reader")
    void closeShouldDelegateToReader() {
      final StubStreamReader reader = new StubStreamReader(100L);
      final StreamAny streamAny = StreamAny.tryFromStreamReader(reader);
      streamAny.close();
      assertTrue(reader.isClosed(), "Reader should be closed after StreamAny.close()");
    }

    @Test
    @DisplayName("double close should be safe")
    void doubleCloseShouldBeSafe() {
      final StubStreamReader reader = new StubStreamReader(200L);
      final StreamAny streamAny = StreamAny.tryFromStreamReader(reader);
      streamAny.close();
      streamAny.close(); // should not throw
      assertTrue(reader.isClosed());
    }
  }
}
