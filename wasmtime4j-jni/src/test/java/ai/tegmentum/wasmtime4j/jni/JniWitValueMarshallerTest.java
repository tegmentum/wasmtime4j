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

package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wit.WitValue;
import ai.tegmentum.wasmtime4j.wit.WitValueMarshaller.MarshalledValue;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniWitValueMarshaller} class.
 *
 * <p>JniWitValueMarshaller provides JNI implementation of WIT value marshalling.
 */
@DisplayName("JniWitValueMarshaller Tests")
class JniWitValueMarshallerTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public and final")
    void shouldBePublicAndFinal() {
      assertTrue(
          Modifier.isPublic(JniWitValueMarshaller.class.getModifiers()),
          "JniWitValueMarshaller should be public");
      assertTrue(
          Modifier.isFinal(JniWitValueMarshaller.class.getModifiers()),
          "JniWitValueMarshaller should be final");
    }

    @Test
    @DisplayName("should have private constructor (utility class)")
    void shouldHavePrivateConstructor() {
      final Constructor<?>[] constructors = JniWitValueMarshaller.class.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly one constructor");
      assertTrue(
          Modifier.isPrivate(constructors[0].getModifiers()), "Constructor should be private");
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have marshal static method")
    void shouldHaveMarshalStaticMethod() throws NoSuchMethodException {
      final Method method = JniWitValueMarshaller.class.getMethod("marshal", WitValue.class);
      assertNotNull(method, "marshal method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "marshal should be static");
      assertEquals(
          MarshalledValue.class, method.getReturnType(), "marshal should return MarshalledValue");
    }

    @Test
    @DisplayName("should have unmarshal static method")
    void shouldHaveUnmarshalStaticMethod() throws NoSuchMethodException {
      final Method method =
          JniWitValueMarshaller.class.getMethod("unmarshal", int.class, byte[].class);
      assertNotNull(method, "unmarshal method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "unmarshal should be static");
      assertEquals(WitValue.class, method.getReturnType(), "unmarshal should return WitValue");
    }

    @Test
    @DisplayName("should have validateDiscriminator static method")
    void shouldHaveValidateDiscriminatorStaticMethod() throws NoSuchMethodException {
      final Method method =
          JniWitValueMarshaller.class.getMethod("validateDiscriminator", int.class);
      assertNotNull(method, "validateDiscriminator method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "validateDiscriminator should be static");
      assertEquals(
          boolean.class, method.getReturnType(), "validateDiscriminator should return boolean");
    }
  }
}
