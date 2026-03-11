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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ConcurrentTask} functional interface.
 *
 * <p>Verifies that ConcurrentTask can be used as a lambda, method reference, and anonymous class,
 * and that it correctly propagates exceptions.
 */
@DisplayName("ConcurrentTask Tests")
class ConcurrentTaskTest {

  @Nested
  @DisplayName("Functional Interface Contract Tests")
  class FunctionalInterfaceContractTests {

    @Test
    @DisplayName("should be annotated as FunctionalInterface")
    void shouldBeFunctionalInterface() {
      assertTrue(
          ConcurrentTask.class.isAnnotationPresent(FunctionalInterface.class),
          "ConcurrentTask should be annotated with @FunctionalInterface");
    }

    @Test
    @DisplayName("should have exactly one abstract method")
    void shouldHaveExactlyOneAbstractMethod() {
      long abstractMethodCount = 0;
      for (final java.lang.reflect.Method method : ConcurrentTask.class.getMethods()) {
        if (method.getDeclaringClass() == ConcurrentTask.class
            && java.lang.reflect.Modifier.isAbstract(method.getModifiers())) {
          abstractMethodCount++;
        }
      }
      assertEquals(1, abstractMethodCount, "Should have exactly one abstract method");
    }
  }

  @Nested
  @DisplayName("Lambda Usage Tests")
  class LambdaUsageTests {

    @Test
    @DisplayName("should work as a lambda returning a value")
    void shouldWorkAsLambdaReturningValue() throws WasmException {
      final ConcurrentTask<String> task = store -> "hello";

      final String result = task.execute(null);

      assertEquals("hello", result, "Lambda should return the expected value");
    }

    @Test
    @DisplayName("should work as a lambda returning an integer")
    void shouldWorkAsLambdaReturningInteger() throws WasmException {
      final ConcurrentTask<Integer> task = store -> 42;

      final Integer result = task.execute(null);

      assertEquals(42, result, "Lambda should return the expected integer");
    }

    @Test
    @DisplayName("should work as a lambda returning null")
    void shouldWorkAsLambdaReturningNull() throws WasmException {
      final ConcurrentTask<Object> task = store -> null;

      final Object result = task.execute(null);

      assertEquals(null, result, "Lambda should return null");
    }
  }

  @Nested
  @DisplayName("Exception Propagation Tests")
  class ExceptionPropagationTests {

    @Test
    @DisplayName("should propagate WasmException from execute")
    void shouldPropagateWasmException() {
      final ConcurrentTask<String> task =
          store -> {
            throw new WasmException("task failed");
          };

      final WasmException thrown =
          assertThrows(
              WasmException.class,
              () -> task.execute(null),
              "execute should propagate WasmException");

      assertEquals("task failed", thrown.getMessage(), "Exception message should match");
    }
  }

  @Nested
  @DisplayName("Anonymous Class Tests")
  class AnonymousClassTests {

    @Test
    @DisplayName("should work as an anonymous class")
    void shouldWorkAsAnonymousClass() throws WasmException {
      final ConcurrentTask<String> task =
          new ConcurrentTask<String>() {
            @Override
            public String execute(final Store store) throws WasmException {
              return "anonymous";
            }
          };

      final String result = task.execute(null);

      assertNotNull(result, "Anonymous class should return a non-null result");
      assertEquals("anonymous", result, "Anonymous class should return the expected value");
    }
  }

  @Nested
  @DisplayName("Type Parameter Tests")
  class TypeParameterTests {

    @Test
    @DisplayName("should support complex type parameters")
    void shouldSupportComplexTypeParameters() throws WasmException {
      final ConcurrentTask<java.util.List<String>> task =
          store -> java.util.Arrays.asList("a", "b", "c");

      final java.util.List<String> result = task.execute(null);

      assertNotNull(result, "Result should not be null");
      assertEquals(3, result.size(), "Result should have 3 elements");
      assertEquals("a", result.get(0), "First element should be 'a'");
    }
  }
}
