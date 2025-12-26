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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link I31Instance} interface.
 *
 * <p>I31Instance represents a WebAssembly GC I31 value, which is an immediate 31-bit signed integer
 * stored as a reference.
 */
@DisplayName("I31Instance Tests")
class I31InstanceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(I31Instance.class.isInterface(), "I31Instance should be an interface");
    }

    @Test
    @DisplayName("should extend GcObject")
    void shouldExtendGcObject() {
      assertTrue(
          GcObject.class.isAssignableFrom(I31Instance.class), "I31Instance should extend GcObject");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = I31Instance.class.getMethod("getType");
      assertEquals(I31Type.class, method.getReturnType(), "Should return I31Type");
    }

    @Test
    @DisplayName("should have getValue method")
    void shouldHaveGetValueMethod() throws NoSuchMethodException {
      final Method method = I31Instance.class.getMethod("getValue");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getSignedValue method")
    void shouldHaveGetSignedValueMethod() throws NoSuchMethodException {
      final Method method = I31Instance.class.getMethod("getSignedValue");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getUnsignedValue method")
    void shouldHaveGetUnsignedValueMethod() throws NoSuchMethodException {
      final Method method = I31Instance.class.getMethod("getUnsignedValue");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("WASM GC Specification Compliance Tests")
  class WasmGcSpecificationComplianceTests {

    @Test
    @DisplayName("should have all required I31 methods")
    void shouldHaveAllRequiredI31Methods() {
      final String[] expectedMethods = {
        "getType", "getValue", "getSignedValue", "getUnsignedValue"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(hasMethod(I31Instance.class, methodName), "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Usage Pattern Documentation Tests")
  class UsagePatternDocumentationTests {

    @Test
    @DisplayName("should support getting signed value")
    void shouldSupportGettingSignedValue() {
      // Documents usage: int signed = instance.getSignedValue();
      assertTrue(hasMethod(I31Instance.class, "getSignedValue"), "Need getSignedValue method");
    }

    @Test
    @DisplayName("should support getting unsigned value")
    void shouldSupportGettingUnsignedValue() {
      // Documents usage: int unsigned = instance.getUnsignedValue();
      assertTrue(hasMethod(I31Instance.class, "getUnsignedValue"), "Need getUnsignedValue method");
    }

    @Test
    @DisplayName("should support getting type")
    void shouldSupportGettingType() {
      // Documents usage: I31Type type = instance.getType();
      assertTrue(hasMethod(I31Instance.class, "getType"), "Need getType method");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("GcObject Inheritance Tests")
  class GcObjectInheritanceTests {

    @Test
    @DisplayName("should inherit from GcObject")
    void shouldInheritFromGcObject() {
      final Class<?>[] interfaces = I31Instance.class.getInterfaces();
      boolean extendsGcObject = false;

      for (final Class<?> iface : interfaces) {
        if (iface == GcObject.class) {
          extendsGcObject = true;
          break;
        }
      }

      assertTrue(extendsGcObject, "I31Instance should extend GcObject");
    }

    @Test
    @DisplayName("should inherit GcObject methods")
    void shouldInheritGcObjectMethods() {
      // GcObject methods should be available
      assertTrue(
          hasMethod(I31Instance.class, "refEquals") || hasMethod(I31Instance.class, "isNull"),
          "Should inherit GcObject methods");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }
}
