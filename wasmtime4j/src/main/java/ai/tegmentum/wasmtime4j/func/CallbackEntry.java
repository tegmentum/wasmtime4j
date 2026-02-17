/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.func;

/**
 * Internal callback entry for managing callback state within a {@link CallbackRegistry}.
 *
 * <p>Each entry associates a {@link CallbackRegistry.CallbackHandle} with its sync/async callback
 * implementation and the corresponding {@link FunctionReference}.
 *
 * @since 1.0.0
 */
public final class CallbackEntry {

  private final CallbackRegistry.CallbackHandle handle;
  private final HostFunction syncCallback;
  private final CallbackRegistry.AsyncHostFunction asyncCallback;
  private final FunctionReference functionReference;

  /**
   * Creates a new callback entry.
   *
   * @param handle the callback handle
   * @param syncCallback the synchronous callback, or null for async-only
   * @param asyncCallback the asynchronous callback, or null for sync-only
   * @param functionReference the function reference for the callback
   */
  public CallbackEntry(
      final CallbackRegistry.CallbackHandle handle,
      final HostFunction syncCallback,
      final CallbackRegistry.AsyncHostFunction asyncCallback,
      final FunctionReference functionReference) {
    this.handle = handle;
    this.syncCallback = syncCallback;
    this.asyncCallback = asyncCallback;
    this.functionReference = functionReference;
  }

  /**
   * Gets the callback handle.
   *
   * @return the callback handle
   */
  public CallbackRegistry.CallbackHandle getHandle() {
    return handle;
  }

  /**
   * Gets the synchronous callback.
   *
   * @return the synchronous callback, or null if this is an async-only entry
   */
  public HostFunction getSyncCallback() {
    return syncCallback;
  }

  /**
   * Gets the asynchronous callback.
   *
   * @return the asynchronous callback, or null if this is a sync-only entry
   */
  public CallbackRegistry.AsyncHostFunction getAsyncCallback() {
    return asyncCallback;
  }

  /**
   * Gets the function reference.
   *
   * @return the function reference
   */
  public FunctionReference getFunctionReference() {
    return functionReference;
  }

  /**
   * Checks if this entry is for an asynchronous callback.
   *
   * @return true if the async callback is non-null
   */
  public boolean isAsync() {
    return asyncCallback != null;
  }
}
