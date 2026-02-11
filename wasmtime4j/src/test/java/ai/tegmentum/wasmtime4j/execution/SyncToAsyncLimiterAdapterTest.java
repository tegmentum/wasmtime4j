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

package ai.tegmentum.wasmtime4j.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SyncToAsyncLimiterAdapter} - package-private adapter wrapping ResourceLimiter.
 *
 * <p>Tests are in the same package to access the package-private class. Uses a stub
 * ResourceLimiter.
 */
@DisplayName("SyncToAsyncLimiterAdapter Tests")
class SyncToAsyncLimiterAdapterTest {

  private static final long TIMEOUT_SECONDS = 5;
  private StubResourceLimiter delegate;
  private SyncToAsyncLimiterAdapter adapter;

  @BeforeEach
  void setUp() {
    delegate = new StubResourceLimiter();
    adapter = new SyncToAsyncLimiterAdapter(delegate);
  }

  @Nested
  @DisplayName("GetId Tests")
  class GetIdTests {

    @Test
    @DisplayName("getId should delegate to underlying limiter")
    void getIdShouldDelegateToUnderlyingLimiter() {
      assertEquals(delegate.getId(), adapter.getId(), "getId should return the delegate's ID");
    }

    @Test
    @DisplayName("getId should return correct value")
    void getIdShouldReturnCorrectValue() {
      assertEquals(42L, adapter.getId(), "getId should return 42 from the stub");
    }
  }

  @Nested
  @DisplayName("GetConfigAsync Tests")
  class GetConfigAsyncTests {

    @Test
    @DisplayName("getConfigAsync should return non-null future")
    void getConfigAsyncShouldReturnNonNullFuture() {
      final CompletableFuture<ResourceLimiterConfig> future = adapter.getConfigAsync();
      assertNotNull(future, "getConfigAsync should return a non-null future");
    }

    @Test
    @DisplayName("getConfigAsync should complete with config")
    void getConfigAsyncShouldCompleteWithConfig()
        throws ExecutionException, InterruptedException, TimeoutException {
      final ResourceLimiterConfig config =
          adapter.getConfigAsync().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
      assertNotNull(config, "getConfigAsync should complete with non-null config");
    }
  }

  @Nested
  @DisplayName("AllowMemoryGrowAsync Tests")
  class AllowMemoryGrowAsyncTests {

    @Test
    @DisplayName("allowMemoryGrowAsync should return non-null future")
    void allowMemoryGrowAsyncShouldReturnNonNullFuture() {
      final CompletableFuture<Boolean> future = adapter.allowMemoryGrowAsync(10, 20);
      assertNotNull(future, "allowMemoryGrowAsync should return a non-null future");
    }

    @Test
    @DisplayName("allowMemoryGrowAsync should complete with delegate result")
    void allowMemoryGrowAsyncShouldCompleteWithDelegateResult()
        throws ExecutionException, InterruptedException, TimeoutException {
      final Boolean result =
          adapter.allowMemoryGrowAsync(10, 20).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
      assertTrue(result, "allowMemoryGrowAsync should return true from stub delegate");
    }
  }

  @Nested
  @DisplayName("AllowTableGrowAsync Tests")
  class AllowTableGrowAsyncTests {

    @Test
    @DisplayName("allowTableGrowAsync should return non-null future")
    void allowTableGrowAsyncShouldReturnNonNullFuture() {
      final CompletableFuture<Boolean> future = adapter.allowTableGrowAsync(5, 10);
      assertNotNull(future, "allowTableGrowAsync should return a non-null future");
    }

    @Test
    @DisplayName("allowTableGrowAsync should complete with delegate result")
    void allowTableGrowAsyncShouldCompleteWithDelegateResult()
        throws ExecutionException, InterruptedException, TimeoutException {
      final Boolean result =
          adapter.allowTableGrowAsync(5, 10).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
      assertTrue(result, "allowTableGrowAsync should return true from stub delegate");
    }
  }

  @Nested
  @DisplayName("GetStatsAsync Tests")
  class GetStatsAsyncTests {

    @Test
    @DisplayName("getStatsAsync should return non-null future")
    void getStatsAsyncShouldReturnNonNullFuture() {
      final CompletableFuture<ResourceLimiterStats> future = adapter.getStatsAsync();
      assertNotNull(future, "getStatsAsync should return a non-null future");
    }

    @Test
    @DisplayName("getStatsAsync should complete with stats")
    void getStatsAsyncShouldCompleteWithStats()
        throws ExecutionException, InterruptedException, TimeoutException {
      final ResourceLimiterStats stats =
          adapter.getStatsAsync().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
      assertNotNull(stats, "getStatsAsync should complete with non-null stats");
    }
  }

  @Nested
  @DisplayName("ResetStatsAsync Tests")
  class ResetStatsAsyncTests {

    @Test
    @DisplayName("resetStatsAsync should return non-null future")
    void resetStatsAsyncShouldReturnNonNullFuture() {
      final CompletableFuture<Void> future = adapter.resetStatsAsync();
      assertNotNull(future, "resetStatsAsync should return a non-null future");
    }

    @Test
    @DisplayName("resetStatsAsync should complete without error")
    void resetStatsAsyncShouldCompleteWithoutError()
        throws ExecutionException, InterruptedException, TimeoutException {
      adapter.resetStatsAsync().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
      assertTrue(delegate.resetStatsCalled, "resetStats should have been called on delegate");
    }
  }

  @Nested
  @DisplayName("Close Tests")
  class CloseTests {

    @Test
    @DisplayName("close should delegate to underlying limiter")
    void closeShouldDelegateToUnderlyingLimiter() throws WasmException {
      adapter.close();
      assertTrue(delegate.closeCalled, "close() should delegate to the underlying limiter");
    }
  }

  @Nested
  @DisplayName("ResourceLimiterAsync Interface Conformance Tests")
  class InterfaceConformanceTests {

    @Test
    @DisplayName("should implement ResourceLimiterAsync")
    void shouldImplementResourceLimiterAsync() {
      assertTrue(
          adapter instanceof ResourceLimiterAsync,
          "SyncToAsyncLimiterAdapter should implement ResourceLimiterAsync");
    }

    @Test
    @DisplayName("fromSync factory should return SyncToAsyncLimiterAdapter")
    void fromSyncFactoryShouldReturnAdapter() {
      final ResourceLimiterAsync asyncLimiter = ResourceLimiterAsync.fromSync(delegate);
      assertNotNull(asyncLimiter, "fromSync should return non-null async limiter");
      assertTrue(
          asyncLimiter instanceof SyncToAsyncLimiterAdapter,
          "fromSync should return a SyncToAsyncLimiterAdapter");
    }
  }

  /** Stub ResourceLimiter for testing the adapter without native runtime. */
  private static final class StubResourceLimiter implements ResourceLimiter {
    boolean resetStatsCalled = false;
    boolean closeCalled = false;

    @Override
    public long getId() {
      return 42L;
    }

    @Override
    public ResourceLimiterConfig getConfig() {
      return ResourceLimiterConfig.builder().build();
    }

    @Override
    public boolean allowMemoryGrow(final long currentPages, final long requestedPages) {
      return true;
    }

    @Override
    public boolean allowTableGrow(final long currentElements, final long requestedElements) {
      return true;
    }

    @Override
    public ResourceLimiterStats getStats() {
      return new ResourceLimiterStats(0L, 0L, 0L, 0L, 0L, 0L);
    }

    @Override
    public void resetStats() {
      resetStatsCalled = true;
    }

    @Override
    public void close() {
      closeCalled = true;
    }
  }
}
