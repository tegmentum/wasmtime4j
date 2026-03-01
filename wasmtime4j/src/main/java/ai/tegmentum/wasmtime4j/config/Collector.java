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
package ai.tegmentum.wasmtime4j.config;

/**
 * GC collector implementation strategies for the WebAssembly engine.
 *
 * <p>Controls which garbage collector implementation is used by the engine for managing GC-managed
 * WebAssembly objects (structs, arrays, etc.). The collector choice affects both performance
 * characteristics and feature support.
 *
 * @since 1.1.0
 */
public enum Collector {

  /** Automatically select the best available collector (default). */
  AUTO("auto"),

  /**
   * Deferred reference counting collector.
   *
   * <p>Uses reference counting with deferred cycle collection. This is currently the only non-null
   * collector implementation in Wasmtime.
   */
  DEFERRED_REFERENCE_COUNTING("deferred_reference_counting"),

  /**
   * Null collector that never collects garbage.
   *
   * <p>Useful for short-lived instances where GC overhead is not desired. All allocations remain
   * until the store is dropped.
   */
  NULL("null");

  private final String rustName;

  Collector(final String rustName) {
    this.rustName = rustName;
  }

  /**
   * Gets the Rust-side configuration name for this collector.
   *
   * @return the Rust configuration string
   */
  public String getRustName() {
    return rustName;
  }

  /**
   * Parses a collector from its Rust configuration string.
   *
   * @param name the Rust configuration name
   * @return the corresponding Collector
   * @throws IllegalArgumentException if the name is not recognized
   */
  public static Collector fromString(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Collector name cannot be null");
    }
    for (final Collector collector : values()) {
      if (collector.rustName.equals(name)) {
        return collector;
      }
    }
    throw new IllegalArgumentException("Unknown collector: " + name);
  }
}
