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
package ai.tegmentum.wasmtime4j.wasi.nn;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Operator-supplied configuration for enabling WASI-NN on a component linker via {@link
 * ai.tegmentum.wasmtime4j.component.ComponentLinker#enableWasiNn(WasiNnConfig)}.
 *
 * <p>An empty config (the {@link #defaults()} instance) matches the behaviour of the zero-arg
 * overload: backends are auto-detected from the compiled feature set of {@code wasmtime-wasi-nn} —
 * under this repo's pin that yields the single ORT-backed ONNX backend, mirroring the Rust engines
 * (oxigraph-wf / qlever-wf) so byte-identical tensor output across JVM and native hosts is
 * achievable.
 *
 * <p><b>Extensibility.</b> The configuration surface is deliberately minimal. As of {@code
 * 46.0.1-1.4.1} the named-model registry {@link Builder#registerModel(String, byte[])
 * registerModel} is wired through the native binding. Future additions will land as additive
 * setters/builder methods (preferred backend enumeration ordering, per-graph execution target
 * hints). Guests that already work under the default config will continue to work; callers that
 * need finer control opt in.
 *
 * <p><b>Thread safety.</b> Instances are immutable after {@link Builder#build()}. Share freely
 * across threads.
 *
 * @since 1.4.0
 */
public final class WasiNnConfig {

  private static final WasiNnConfig DEFAULTS = new WasiNnConfig(Collections.emptyMap());

  /**
   * Named-model registry entries. Wired end-to-end since {@code 46.0.1-1.4.1}: each entry is
   * decoded once against the compiled wasi-nn backend (ONNX/ORT under this repo's workspace pin) at
   * {@code enableWasiNn(WasiNnConfig)} time and the resulting {@code Arc<Graph>} is carried into
   * every store built off this linker, so guests calling {@code wasi:nn/graph.load-by-name} resolve
   * without re-decoding. Empty map = auto-decode-only ({@code graph.load(builders, encoding,
   * target)} still works and is unaffected).
   */
  private final Map<String, byte[]> namedModels;

  private WasiNnConfig(final Map<String, byte[]> namedModels) {
    // Defensive copy plus unmodifiable wrapper — a caller mutating their
    // builder after build() must not silently change linker behaviour.
    this.namedModels = Collections.unmodifiableMap(new LinkedHashMap<>(namedModels));
  }

  /**
   * Returns a shared, empty configuration equivalent to {@link
   * ai.tegmentum.wasmtime4j.component.ComponentLinker#enableWasiNn()}.
   */
  public static WasiNnConfig defaults() {
    return DEFAULTS;
  }

  /** Starts a builder for a customised configuration. */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns the (read-only) named-model registry entries supplied to this config. Since {@code
   * 46.0.1-1.4.1} each entry is consumed by the native binding at {@link
   * ai.tegmentum.wasmtime4j.component.ComponentLinker#enableWasiNn(WasiNnConfig)} time and becomes
   * resolvable via {@code wasi:nn/graph.load-by-name}.
   */
  public Map<String, byte[]> namedModels() {
    return namedModels;
  }

  /** Builder for {@link WasiNnConfig}. */
  public static final class Builder {
    private final Map<String, byte[]> namedModels = new LinkedHashMap<>();

    private Builder() {}

    /**
     * Register a model by name so guests can look it up via {@code wasi:nn/graph.load-by-name}.
     * Model bytes are copied at build time; the caller's array may be mutated afterwards without
     * affecting linker behaviour.
     *
     * <p><b>Wired through in 46.0.1-1.4.1.</b> Each entry is decoded once against the compiled
     * wasi-nn backend (ONNX/ORT under this repo's workspace pin) at {@link
     * ai.tegmentum.wasmtime4j.component.ComponentLinker#enableWasiNn(WasiNnConfig)} time. Under the
     * ONNX-only backend the model bytes are treated as an ONNX file; other encodings are not yet
     * selectable.
     */
    public Builder registerModel(final String name, final byte[] modelBytes) {
      if (name == null || name.isEmpty()) {
        throw new IllegalArgumentException("model name cannot be null or empty");
      }
      if (modelBytes == null) {
        throw new IllegalArgumentException("modelBytes cannot be null");
      }
      namedModels.put(name, modelBytes.clone());
      return this;
    }

    public WasiNnConfig build() {
      return new WasiNnConfig(namedModels);
    }
  }
}
