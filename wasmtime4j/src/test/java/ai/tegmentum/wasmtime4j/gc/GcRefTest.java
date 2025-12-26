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

package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GcRef} interface.
 *
 * <p>GcRef is the base interface for all WebAssembly GC reference types.
 */
@DisplayName("GcRef Interface Tests")
class GcRefTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(Modifier.isPublic(GcRef.class.getModifiers()), "GcRef should be public");
      assertTrue(GcRef.class.isInterface(), "GcRef should be an interface");
    }

    @Test
    @DisplayName("should have isNull method")
    void shouldHaveIsNullMethod() throws NoSuchMethodException {
      final Method method = GcRef.class.getMethod("isNull");
      assertNotNull(method, "isNull method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isNull should return boolean");
    }

    @Test
    @DisplayName("should have getReferenceType method")
    void shouldHaveGetReferenceTypeMethod() throws NoSuchMethodException {
      final Method method = GcRef.class.getMethod("getReferenceType");
      assertNotNull(method, "getReferenceType method should exist");
      assertEquals(
          GcReferenceType.class,
          method.getReturnType(),
          "getReferenceType should return GcReferenceType");
    }

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      final Method method = GcRef.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(long.class, method.getReturnType(), "getId should return long");
    }

    @Test
    @DisplayName("should have exactly three methods")
    void shouldHaveExactlyThreeMethods() {
      final Method[] methods = GcRef.class.getDeclaredMethods();
      assertEquals(3, methods.length, "GcRef should have exactly 3 methods");
    }
  }

  @Nested
  @DisplayName("Implementation Verification Tests")
  class ImplementationVerificationTests {

    @Test
    @DisplayName("AnyRef should implement GcRef")
    void anyRefShouldImplementGcRef() {
      assertTrue(GcRef.class.isAssignableFrom(AnyRef.class), "AnyRef should implement GcRef");
    }

    @Test
    @DisplayName("EqRef should implement GcRef")
    void eqRefShouldImplementGcRef() {
      assertTrue(GcRef.class.isAssignableFrom(EqRef.class), "EqRef should implement GcRef");
    }

    @Test
    @DisplayName("StructRef should implement GcRef")
    void structRefShouldImplementGcRef() {
      assertTrue(GcRef.class.isAssignableFrom(StructRef.class), "StructRef should implement GcRef");
    }

    @Test
    @DisplayName("ArrayRef should implement GcRef")
    void arrayRefShouldImplementGcRef() {
      assertTrue(GcRef.class.isAssignableFrom(ArrayRef.class), "ArrayRef should implement GcRef");
    }
  }
}
