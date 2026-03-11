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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link JoinHandle} interface.
 *
 * <p>Verifies that JoinHandle is an interface with the expected method signatures and that
 * implementations fulfill the contract.
 */
@DisplayName("JoinHandle Tests")
class JoinHandleTest {

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(JoinHandle.class.isInterface(), "JoinHandle should be an interface");
    }

    @Test
    @DisplayName("should declare join method")
    void shouldDeclareJoinMethod() throws NoSuchMethodException {
      assertNotNull(
          JoinHandle.class.getMethod("join"), "JoinHandle should declare a join() method");
    }

    @Test
    @DisplayName("should declare toFuture method")
    void shouldDeclareToFutureMethod() throws NoSuchMethodException {
      assertNotNull(
          JoinHandle.class.getMethod("toFuture"), "JoinHandle should declare a toFuture() method");
    }

    @Test
    @DisplayName("should declare cancel method")
    void shouldDeclareCancelMethod() throws NoSuchMethodException {
      assertNotNull(
          JoinHandle.class.getMethod("cancel"), "JoinHandle should declare a cancel() method");
    }

    @Test
    @DisplayName("should declare isDone method")
    void shouldDeclareIsDoneMethod() throws NoSuchMethodException {
      assertNotNull(
          JoinHandle.class.getMethod("isDone"), "JoinHandle should declare an isDone() method");
    }
  }

  @Nested
  @DisplayName("Implementation Tests")
  class ImplementationTests {

    @Test
    @DisplayName("DefaultJoinHandle should implement JoinHandle")
    void defaultJoinHandleShouldImplementJoinHandle() {
      final CompletableFuture<String> future = CompletableFuture.completedFuture("test");
      final JoinHandle<String> handle = new DefaultJoinHandle<>(future);

      assertNotNull(handle, "DefaultJoinHandle should be assignable to JoinHandle");
    }

    @Test
    @DisplayName("should be usable through interface reference")
    void shouldBeUsableThroughInterfaceReference() throws WasmException, InterruptedException {
      final JoinHandle<Integer> handle =
          new DefaultJoinHandle<>(CompletableFuture.completedFuture(42));

      assertEquals(42, handle.join(), "Should return value through interface reference");
      assertTrue(handle.isDone(), "isDone should return true through interface reference");
      assertFalse(handle.cancel(), "cancel should return false for completed handle");
      assertNotNull(handle.toFuture(), "toFuture should return non-null through interface");
    }
  }
}
