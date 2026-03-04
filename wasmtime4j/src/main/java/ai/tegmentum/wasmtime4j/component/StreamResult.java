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

/**
 * Result status for stream and future read/write operations in the async component model.
 *
 * <p>This enum maps to Wasmtime's {@code StreamResult} type, which indicates the outcome of a
 * stream or future poll operation. It is returned by read/write operations on streams and futures
 * to indicate whether more data may be available, or the channel has been closed.
 *
 * <p>The three states form a progression:
 *
 * <ul>
 *   <li>{@link #COMPLETED} - The operation succeeded and the stream/future may produce or consume
 *       more values. This is the normal "success, keep going" result.
 *   <li>{@link #CANCELLED} - The operation was interrupted (e.g., by a timeout or explicit
 *       cancellation), but the stream/future is still alive and may produce or consume more values.
 *   <li>{@link #DROPPED} - The operation completed and the producer or consumer has been dropped.
 *       No further values can be produced or consumed on this channel.
 * </ul>
 *
 * @since 1.1.0
 */
public enum StreamResult {

  /**
   * The operation completed successfully. The stream or future may produce or consume additional
   * values.
   */
  COMPLETED,

  /**
   * The operation was cancelled or interrupted. The stream or future is still alive and may produce
   * or consume additional values.
   */
  CANCELLED,

  /**
   * The operation completed and the producer or consumer has been dropped. No further values can be
   * produced or consumed on this channel.
   */
  DROPPED
}
