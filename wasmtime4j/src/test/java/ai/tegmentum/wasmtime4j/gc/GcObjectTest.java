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

import ai.tegmentum.wasmtime4j.WasmValue;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GcObject} interface.
 *
 * <p>GcObject is the base interface for all WebAssembly GC objects.
 */
@DisplayName("GcObject Interface Tests")
class GcObjectTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(Modifier.isPublic(GcObject.class.getModifiers()), "GcObject should be public");
      assertTrue(GcObject.class.isInterface(), "GcObject should be an interface");
    }

    @Test
    @DisplayName("should have getObjectId method")
    void shouldHaveGetObjectIdMethod() throws NoSuchMethodException {
      final Method method = GcObject.class.getMethod("getObjectId");
      assertNotNull(method, "getObjectId method should exist");
      assertEquals(long.class, method.getReturnType(), "getObjectId should return long");
    }

    @Test
    @DisplayName("should have getReferenceType method")
    void shouldHaveGetReferenceTypeMethod() throws NoSuchMethodException {
      final Method method = GcObject.class.getMethod("getReferenceType");
      assertNotNull(method, "getReferenceType method should exist");
      assertEquals(
          GcReferenceType.class,
          method.getReturnType(),
          "getReferenceType should return GcReferenceType");
    }

    @Test
    @DisplayName("should have isNull method")
    void shouldHaveIsNullMethod() throws NoSuchMethodException {
      final Method method = GcObject.class.getMethod("isNull");
      assertNotNull(method, "isNull method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isNull should return boolean");
    }

    @Test
    @DisplayName("should have isOfType method")
    void shouldHaveIsOfTypeMethod() throws NoSuchMethodException {
      final Method method = GcObject.class.getMethod("isOfType", GcReferenceType.class);
      assertNotNull(method, "isOfType method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isOfType should return boolean");
    }

    @Test
    @DisplayName("should have castTo method")
    void shouldHaveCastToMethod() throws NoSuchMethodException {
      final Method method = GcObject.class.getMethod("castTo", GcReferenceType.class);
      assertNotNull(method, "castTo method should exist");
      assertEquals(GcObject.class, method.getReturnType(), "castTo should return GcObject");
    }

    @Test
    @DisplayName("should have refEquals method")
    void shouldHaveRefEqualsMethod() throws NoSuchMethodException {
      final Method method = GcObject.class.getMethod("refEquals", GcObject.class);
      assertNotNull(method, "refEquals method should exist");
      assertEquals(boolean.class, method.getReturnType(), "refEquals should return boolean");
    }

    @Test
    @DisplayName("should have getSizeBytes method")
    void shouldHaveGetSizeBytesMethod() throws NoSuchMethodException {
      final Method method = GcObject.class.getMethod("getSizeBytes");
      assertNotNull(method, "getSizeBytes method should exist");
      assertEquals(int.class, method.getReturnType(), "getSizeBytes should return int");
    }

    @Test
    @DisplayName("should have toWasmValue method")
    void shouldHaveToWasmValueMethod() throws NoSuchMethodException {
      final Method method = GcObject.class.getMethod("toWasmValue");
      assertNotNull(method, "toWasmValue method should exist");
      assertEquals(WasmValue.class, method.getReturnType(), "toWasmValue should return WasmValue");
    }

    @Test
    @DisplayName("should have exactly eight methods")
    void shouldHaveExactlyEightMethods() {
      final Method[] methods = GcObject.class.getDeclaredMethods();
      assertEquals(8, methods.length, "GcObject should have exactly 8 methods");
    }
  }
}
