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
package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Default implementation of {@link JoinHandle} backed by a {@link CompletableFuture}.
 *
 * @param <T> the result type of the spawned task
 */
final class DefaultJoinHandle<T> implements JoinHandle<T> {

  private final CompletableFuture<T> future;

  DefaultJoinHandle(final CompletableFuture<T> future) {
    if (future == null) {
      throw new IllegalArgumentException("future cannot be null");
    }
    this.future = future;
  }

  @Override
  public T join() throws WasmException, InterruptedException {
    try {
      return future.get();
    } catch (final ExecutionException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof WasmException) {
        throw (WasmException) cause;
      }
      if (cause instanceof CompletionException) {
        final Throwable innerCause = cause.getCause();
        if (innerCause instanceof WasmException) {
          throw (WasmException) innerCause;
        }
      }
      throw new WasmException("Concurrent task failed: " + cause.getMessage(), cause);
    } catch (final CancellationException e) {
      throw new WasmException("Concurrent task was cancelled", e);
    }
  }

  @Override
  public CompletableFuture<T> toFuture() {
    return future;
  }

  @Override
  public boolean cancel() {
    return future.cancel(true);
  }

  @Override
  public boolean isDone() {
    return future.isDone();
  }
}
