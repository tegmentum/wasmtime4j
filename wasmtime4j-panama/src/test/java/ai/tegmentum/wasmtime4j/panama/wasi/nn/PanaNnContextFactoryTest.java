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

package ai.tegmentum.wasmtime4j.panama.wasi.nn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.nn.NnContext;
import ai.tegmentum.wasmtime4j.wasi.nn.NnContextFactory;
import ai.tegmentum.wasmtime4j.wasi.nn.NnExecutionTarget;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanaNnContextFactory} class.
 *
 * <p>PanaNnContextFactory provides factory for creating Panama WASI-NN contexts.
 */
@DisplayName("PanaNnContextFactory Tests")
class PanaNnContextFactoryTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanaNnContextFactory.class.getModifiers()),
          "PanaNnContextFactory should be public");
      assertTrue(
          Modifier.isFinal(PanaNnContextFactory.class.getModifiers()),
          "PanaNnContextFactory should be final");
    }

    @Test
    @DisplayName("should implement NnContextFactory interface")
    void shouldImplementNnContextFactoryInterface() {
      assertTrue(
          NnContextFactory.class.isAssignableFrom(PanaNnContextFactory.class),
          "PanaNnContextFactory should implement NnContextFactory");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = PanaNnContextFactory.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should have createNnContext method")
    void shouldHaveCreateNnContextMethod() throws NoSuchMethodException {
      final Method method = PanaNnContextFactory.class.getMethod("createNnContext");
      assertNotNull(method, "createNnContext method should exist");
      assertEquals(NnContext.class, method.getReturnType(), "Should return NnContext");
    }
  }

  @Nested
  @DisplayName("Availability Method Tests")
  class AvailabilityMethodTests {

    @Test
    @DisplayName("should have isNnAvailable method")
    void shouldHaveIsNnAvailableMethod() throws NoSuchMethodException {
      final Method method = PanaNnContextFactory.class.getMethod("isNnAvailable");
      assertNotNull(method, "isNnAvailable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Target Method Tests")
  class TargetMethodTests {

    @Test
    @DisplayName("should have getDefaultExecutionTarget method")
    void shouldHaveGetDefaultExecutionTargetMethod() throws NoSuchMethodException {
      final Method method = PanaNnContextFactory.class.getMethod("getDefaultExecutionTarget");
      assertNotNull(method, "getDefaultExecutionTarget method should exist");
      assertEquals(
          NnExecutionTarget.class, method.getReturnType(), "Should return NnExecutionTarget");
    }
  }
}
