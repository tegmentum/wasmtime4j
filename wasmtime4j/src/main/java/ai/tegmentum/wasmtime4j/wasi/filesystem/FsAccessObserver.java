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
package ai.tegmentum.wasmtime4j.wasi.filesystem;

/**
 * Observe-only callback notified when a WASI Preview 2 component's path-based filesystem access is
 * DENIED on the capability-confined (preopen) instantiation path.
 *
 * <p>This callback is a pure observability hook. It CANNOT change enforcement: by the time it
 * fires, {@code wasmtime-wasi} has already performed the real {@code open-at} / {@code stat-at} and
 * produced the real error, which flows back to the guest unchanged regardless of what this method
 * does. Its sole purpose is to surface the raw guest-supplied path together with the classified
 * failure reason so the host can log, meter, or alert on denied filesystem access that would
 * otherwise be invisible on the component/preopen path.
 *
 * <p>The observer is invoked synchronously on the guest's calling thread while the denied operation
 * is being serviced. Implementations should be fast and must not assume they can influence the
 * outcome. Exceptions thrown from {@link #onDenied} are logged and swallowed by the native bridge
 * and never propagate into the guest.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * FsAccessObserver observer = (path, operation, reason, errorCode) ->
 *     LOGGER.warning("fs denial: " + operation + " " + path + " -> " + reason);
 *
 * WasiPreview2Config config = WasiPreview2Config.builder()
 *     .preopenDir(sandboxDir, "/data", false)
 *     .fsAccessObserver(observer)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface FsAccessObserver {

  /**
   * Called when a path-based filesystem operation was refused by WASI enforcement.
   *
   * @param path the raw guest-supplied path the operation targeted (relative to the descriptor it
   *     was resolved against; never {@code null})
   * @param operation the operation that was denied, either {@code "open-at"} or {@code "stat-at"}
   * @param reason the classified failure reason: the kebab-case {@code wasi:filesystem/types}
   *     {@code error-code} name, e.g. {@code "not-permitted"}, {@code "no-entry"}, {@code
   *     "access"}; {@code "unknown"} if the failure could not be classified to an error code
   * @param errorCode the numeric discriminant of {@code reason} within the {@code error-code} enum,
   *     or {@code -1} when unclassified
   */
  void onDenied(String path, String operation, String reason, int errorCode);
}
