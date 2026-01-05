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

package ai.tegmentum.wasmtime4j.panama.wit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wit.WitValue;
import ai.tegmentum.wasmtime4j.wit.WitValueMarshaller.MarshalledValue;
import java.lang.foreign.Arena;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWitValueMarshaller} class.
 *
 * <p>PanamaWitValueMarshaller provides Panama implementation of WIT value marshalling.
 */
@DisplayName("PanamaWitValueMarshaller Tests")
class PanamaWitValueMarshallerTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWitValueMarshaller.class.getModifiers()),
          "PanamaWitValueMarshaller should be public");
      assertTrue(
          Modifier.isFinal(PanamaWitValueMarshaller.class.getModifiers()),
          "PanamaWitValueMarshaller should be final");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() {
      Constructor<?>[] constructors = PanamaWitValueMarshaller.class.getDeclaredConstructors();
      assertTrue(constructors.length > 0, "Should have at least one constructor");
      for (Constructor<?> constructor : constructors) {
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");
      }
    }
  }

  @Nested
  @DisplayName("Marshal Method Tests")
  class MarshalMethodTests {

    @Test
    @DisplayName("should have marshal static method")
    void shouldHaveMarshalMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWitValueMarshaller.class.getMethod("marshal", WitValue.class, Arena.class);
      assertNotNull(method, "marshal method should exist");
      assertEquals(MarshalledValue.class, method.getReturnType(), "Should return MarshalledValue");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Unmarshal Method Tests")
  class UnmarshalMethodTests {

    @Test
    @DisplayName("should have unmarshal static method")
    void shouldHaveUnmarshalMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWitValueMarshaller.class.getMethod(
              "unmarshal", int.class, byte[].class, Arena.class);
      assertNotNull(method, "unmarshal method should exist");
      assertEquals(WitValue.class, method.getReturnType(), "Should return WitValue");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Validation Method Tests")
  class ValidationMethodTests {

    @Test
    @DisplayName("should have validateDiscriminator static method")
    void shouldHaveValidateDiscriminatorMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWitValueMarshaller.class.getMethod("validateDiscriminator", int.class);
      assertNotNull(method, "validateDiscriminator method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }
}
