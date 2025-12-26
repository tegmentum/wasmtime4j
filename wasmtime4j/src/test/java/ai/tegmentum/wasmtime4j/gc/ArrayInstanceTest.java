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
 * Tests for {@link ArrayInstance} interface.
 *
 * <p>ArrayInstance represents a WebAssembly GC array instance with element values, providing
 * indexed access to elements.
 */
@DisplayName("ArrayInstance Tests")
class ArrayInstanceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ArrayInstance.class.isInterface(), "ArrayInstance should be an interface");
    }

    @Test
    @DisplayName("should extend GcObject")
    void shouldExtendGcObject() {
      assertTrue(
          GcObject.class.isAssignableFrom(ArrayInstance.class),
          "ArrayInstance should extend GcObject");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = ArrayInstance.class.getMethod("getType");
      assertEquals(ArrayType.class, method.getReturnType(), "Should return ArrayType");
    }

    @Test
    @DisplayName("should have getLength method")
    void shouldHaveGetLengthMethod() throws NoSuchMethodException {
      final Method method = ArrayInstance.class.getMethod("getLength");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getElement method")
    void shouldHaveGetElementMethod() throws NoSuchMethodException {
      final Method method = ArrayInstance.class.getMethod("getElement", int.class);
      assertEquals(GcValue.class, method.getReturnType(), "Should return GcValue");
    }

    @Test
    @DisplayName("should have setElement method")
    void shouldHaveSetElementMethod() throws NoSuchMethodException {
      final Method method = ArrayInstance.class.getMethod("setElement", int.class, GcValue.class);
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("WASM GC Specification Compliance Tests")
  class WasmGcSpecificationComplianceTests {

    @Test
    @DisplayName("should have all required array methods")
    void shouldHaveAllRequiredArrayMethods() {
      final String[] expectedMethods = {"getType", "getLength", "getElement", "setElement"};

      for (final String methodName : expectedMethods) {
        assertTrue(hasMethod(ArrayInstance.class, methodName), "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("getElement should take int parameter")
    void getElementShouldTakeIntParameter() throws NoSuchMethodException {
      final Method method = ArrayInstance.class.getMethod("getElement", int.class);
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
      assertEquals(int.class, method.getParameterTypes()[0], "Parameter should be int");
    }

    @Test
    @DisplayName("setElement should take int and GcValue parameters")
    void setElementShouldTakeIntAndGcValueParameters() throws NoSuchMethodException {
      final Method method = ArrayInstance.class.getMethod("setElement", int.class, GcValue.class);
      assertEquals(2, method.getParameterCount(), "Should have 2 parameters");
      assertEquals(int.class, method.getParameterTypes()[0], "First parameter should be int");
      assertEquals(
          GcValue.class, method.getParameterTypes()[1], "Second parameter should be GcValue");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("getElement should declare GcException")
    void getElementShouldDeclareGcException() throws NoSuchMethodException {
      final Method method = ArrayInstance.class.getMethod("getElement", int.class);
      final Class<?>[] exceptions = method.getExceptionTypes();

      boolean throwsGcException = false;
      for (final Class<?> ex : exceptions) {
        if (ex == GcException.class) {
          throwsGcException = true;
          break;
        }
      }

      assertTrue(throwsGcException, "getElement should declare GcException");
    }

    @Test
    @DisplayName("setElement should declare GcException")
    void setElementShouldDeclareGcException() throws NoSuchMethodException {
      final Method method = ArrayInstance.class.getMethod("setElement", int.class, GcValue.class);
      final Class<?>[] exceptions = method.getExceptionTypes();

      boolean throwsGcException = false;
      for (final Class<?> ex : exceptions) {
        if (ex == GcException.class) {
          throwsGcException = true;
          break;
        }
      }

      assertTrue(throwsGcException, "setElement should declare GcException");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Documentation Tests")
  class UsagePatternDocumentationTests {

    @Test
    @DisplayName("should support reading array elements")
    void shouldSupportReadingArrayElements() {
      // Documents usage: GcValue value = array.getElement(0);
      assertTrue(hasMethod(ArrayInstance.class, "getElement"), "Need getElement method");
    }

    @Test
    @DisplayName("should support writing array elements")
    void shouldSupportWritingArrayElements() {
      // Documents usage: array.setElement(0, value);
      assertTrue(hasMethod(ArrayInstance.class, "setElement"), "Need setElement method");
    }

    @Test
    @DisplayName("should support getting array length")
    void shouldSupportGettingArrayLength() {
      // Documents usage: int length = array.getLength();
      assertTrue(hasMethod(ArrayInstance.class, "getLength"), "Need getLength method");
    }

    @Test
    @DisplayName("should support getting type")
    void shouldSupportGettingType() {
      // Documents usage: ArrayType type = array.getType();
      assertTrue(hasMethod(ArrayInstance.class, "getType"), "Need getType method");
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
      final Class<?>[] interfaces = ArrayInstance.class.getInterfaces();
      boolean extendsGcObject = false;

      for (final Class<?> iface : interfaces) {
        if (iface == GcObject.class) {
          extendsGcObject = true;
          break;
        }
      }

      assertTrue(extendsGcObject, "ArrayInstance should extend GcObject");
    }
  }
}
