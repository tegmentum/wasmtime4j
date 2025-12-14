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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;

/**
 * Adapter that wraps a synchronous ResourceLimiter as ResourceLimiterAsync.
 *
 * @since 1.0.0
 */
final class SyncToAsyncLimiterAdapter implements ResourceLimiterAsync {

  private final ResourceLimiter delegate;

  SyncToAsyncLimiterAdapter(final ResourceLimiter delegate) {
    this.delegate = delegate;
  }

  @Override
  public long getId() {
    return delegate.getId();
  }

  @Override
  public CompletableFuture<ResourceLimiterConfig> getConfigAsync() {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return delegate.getConfig();
          } catch (WasmException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Override
  public CompletableFuture<Boolean> allowMemoryGrowAsync(
      final long currentPages, final long requestedPages) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return delegate.allowMemoryGrow(currentPages, requestedPages);
          } catch (WasmException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Override
  public CompletableFuture<Boolean> allowTableGrowAsync(
      final long currentElements, final long requestedElements) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return delegate.allowTableGrow(currentElements, requestedElements);
          } catch (WasmException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Override
  public CompletableFuture<ResourceLimiterStats> getStatsAsync() {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return delegate.getStats();
          } catch (WasmException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Override
  public CompletableFuture<Void> resetStatsAsync() {
    return CompletableFuture.runAsync(
        () -> {
          try {
            delegate.resetStats();
          } catch (WasmException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Override
  public void close() throws WasmException {
    delegate.close();
  }
}
