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
package ai.tegmentum.wasmtime4j.wasmtime.generated.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Equivalent Java tests for Wasmtime store.rs tests.
 *
 * <p>Original source: store.rs - Tests store data handling and lifecycle.
 *
 * <p>This test validates that wasmtime4j store operations produce the same behavior as the upstream
 * Wasmtime implementation.
 */
public final class StoreDataTest extends DualRuntimeTest {

  @AfterEach
  void cleanupRuntime() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("store::basic_store_creation")
  public void testBasicStoreCreation(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final Engine engine = Engine.create()) {
      try (final Store store = engine.createStore()) {
        assertNotNull(store, "Store should be created successfully");
        assertTrue(store.isValid(), "Store should be valid after creation");
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("store::store_with_data")
  public void testStoreWithData(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final Engine engine = Engine.create()) {
      final String customData = "my custom data";
      try (final Store store = engine.createStore(customData)) {
        assertNotNull(store, "Store with data should be created");
        assertEquals(customData, store.getData(), "Store should contain custom data");
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("store::store_set_data")
  public void testStoreSetData(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final Engine engine = Engine.create()) {
      try (final Store store = engine.createStore()) {
        assertNull(store.getData(), "Store should have null data initially");

        final String newData = "updated data";
        store.setData(newData);
        assertEquals(newData, store.getData(), "Store should have updated data");
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("store::store_data_lifecycle")
  public void testStoreDataLifecycle(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Test that objects stored in the store are properly referenced
    final AtomicInteger accessCount = new AtomicInteger(0);

    // Create custom data that tracks access
    final Object customData =
        new Object() {
          @Override
          public String toString() {
            accessCount.incrementAndGet();
            return "custom-data";
          }
        };

    try (final Engine engine = Engine.create()) {
      try (final Store store = engine.createStore(customData)) {
        // Access the data multiple times
        store.getData().toString();
        store.getData().toString();
        store.getData().toString();

        assertEquals(3, accessCount.get(), "Data should be accessed 3 times");
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("store::multiple_stores_same_engine")
  public void testMultipleStoresSameEngine(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final Engine engine = Engine.create()) {
      try (final Store store1 = engine.createStore("data1");
          final Store store2 = engine.createStore("data2");
          final Store store3 = engine.createStore("data3")) {

        // Each store should have independent data
        assertEquals("data1", store1.getData());
        assertEquals("data2", store2.getData());
        assertEquals("data3", store3.getData());

        // All stores should reference the same engine
        assertEquals(engine, store1.getEngine());
        assertEquals(engine, store2.getEngine());
        assertEquals(engine, store3.getEngine());
      }
    }
  }
}
