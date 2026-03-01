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
package ai.tegmentum.wasmtime4j.debug;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Callback interface for handling debug events during WebAssembly execution.
 *
 * <p>Implement this interface and register it with {@link
 * ai.tegmentum.wasmtime4j.Store#setDebugHandler(DebugHandler)} to receive notifications about debug
 * events such as breakpoints, traps, and exceptions.
 *
 * <p>The debug handler is invoked asynchronously and must return a {@link CompletableFuture} that
 * completes when the handler is done processing the event. The store requires async support to be
 * enabled (via {@code Config.asyncSupport(true)}).
 *
 * <p><strong>Important:</strong> The {@link FrameHandle} instances provided in the frames list are
 * only valid during the handler invocation. They become invalid after the returned future
 * completes.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * store.setDebugHandler((event, frames) -> {
 *     System.out.println("Debug event: " + event);
 *     for (FrameHandle frame : frames) {
 *         System.out.println("  Frame: func=" + frame.getFunctionIndex()
 *             + " pc=" + frame.getPc());
 *     }
 *     return CompletableFuture.completedFuture(null);
 * });
 * }</pre>
 *
 * @since 1.1.0
 */
@FunctionalInterface
public interface DebugHandler {

  /**
   * Handles a debug event.
   *
   * @param event the type of debug event that occurred
   * @param frames the current stack frames (innermost to outermost), valid only during this call
   * @return a future that completes when the handler is done
   */
  CompletableFuture<Void> handle(DebugEvent event, List<FrameHandle> frames);
}
