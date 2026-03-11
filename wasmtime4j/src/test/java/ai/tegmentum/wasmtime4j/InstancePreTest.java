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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.validation.ImportMap;
import ai.tegmentum.wasmtime4j.validation.PreInstantiationStatistics;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link InstancePre} interface.
 *
 * <p>Verifies the default {@code instantiateAsync} method behavior using a stub implementation.
 */
@DisplayName("InstancePre Tests")
class InstancePreTest {

  /**
   * Stub implementation that returns a sentinel object from instantiate. We use null as the
   * "instance" since creating a full Instance stub requires implementing many methods.
   */
  private static final class StubInstancePre implements InstancePre {

    private final WasmException error;
    private boolean instantiateCalled;

    StubInstancePre() {
      this.error = null;
    }

    StubInstancePre(final WasmException error) {
      this.error = error;
    }

    @Override
    public Instance instantiate(final Store store) throws WasmException {
      instantiateCalled = true;
      if (error != null) {
        throw error;
      }
      // Return null as a sentinel; the important thing is that instantiate was called
      return null;
    }

    @Override
    public Instance instantiate(final Store store, final ImportMap imports) throws WasmException {
      return instantiate(store);
    }

    @Override
    public Module getModule() {
      return null;
    }

    @Override
    public Engine getEngine() {
      return null;
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public long getInstanceCount() {
      return 0;
    }

    @Override
    public PreInstantiationStatistics getStatistics() {
      return null;
    }

    @Override
    public void close() {
      // no-op
    }
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(InstancePre.class.isInterface(), "InstancePre should be an interface");
    }

    @Test
    @DisplayName("should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(
          java.io.Closeable.class.isAssignableFrom(InstancePre.class),
          "InstancePre should extend Closeable");
    }
  }

  @Nested
  @DisplayName("instantiateAsync Default Method Tests")
  class InstantiateAsyncTests {

    @Test
    @DisplayName("should return a CompletableFuture")
    void shouldReturnCompletableFuture() {
      final StubInstancePre pre = new StubInstancePre();

      final CompletableFuture<Instance> future = pre.instantiateAsync(null);

      assertNotNull(future, "instantiateAsync should return a non-null future");
    }

    @Test
    @DisplayName("should delegate to synchronous instantiate")
    void shouldDelegateToSynchronousInstantiate() throws Exception {
      final StubInstancePre pre = new StubInstancePre();

      final CompletableFuture<Instance> future = pre.instantiateAsync(null);
      future.get(); // wait for completion

      assertTrue(pre.instantiateCalled, "instantiateAsync should delegate to instantiate");
    }

    @Test
    @DisplayName("should propagate WasmException through the future")
    void shouldPropagateWasmExceptionThroughFuture() {
      final WasmException wasmError = new WasmException("test error");
      final StubInstancePre pre = new StubInstancePre(wasmError);

      final CompletableFuture<Instance> future = pre.instantiateAsync(null);

      try {
        future.get();
        assertTrue(false, "Should have thrown ExecutionException");
      } catch (final ExecutionException e) {
        // CompletableFuture.supplyAsync unwraps CompletionException, so the cause
        // of ExecutionException is the original WasmException directly
        assertSame(
            wasmError, e.getCause(), "Cause should be the original WasmException");
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        assertTrue(false, "Should not be interrupted");
      }
    }
  }

  @Nested
  @DisplayName("Stub Behavior Tests")
  class StubBehaviorTests {

    @Test
    @DisplayName("getModule should return null for stub")
    void getModuleShouldReturnNull() {
      final StubInstancePre pre = new StubInstancePre();
      assertSame(null, pre.getModule(), "Stub getModule should return null");
    }

    @Test
    @DisplayName("getEngine should return null for stub")
    void getEngineShouldReturnNull() {
      final StubInstancePre pre = new StubInstancePre();
      assertSame(null, pre.getEngine(), "Stub getEngine should return null");
    }

    @Test
    @DisplayName("isValid should return true for stub")
    void isValidShouldReturnTrue() {
      final StubInstancePre pre = new StubInstancePre();
      assertTrue(pre.isValid(), "Stub isValid should return true");
    }
  }
}
