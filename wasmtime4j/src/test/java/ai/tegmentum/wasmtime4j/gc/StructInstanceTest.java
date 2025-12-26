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
 * Tests for {@link StructInstance} interface.
 *
 * <p>StructInstance represents a WebAssembly GC struct instance with field values, providing access
 * to individual fields by index.
 */
@DisplayName("StructInstance Tests")
class StructInstanceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(StructInstance.class.isInterface(), "StructInstance should be an interface");
    }

    @Test
    @DisplayName("should extend GcObject")
    void shouldExtendGcObject() {
      assertTrue(
          GcObject.class.isAssignableFrom(StructInstance.class),
          "StructInstance should extend GcObject");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = StructInstance.class.getMethod("getType");
      assertEquals(StructType.class, method.getReturnType(), "Should return StructType");
    }

    @Test
    @DisplayName("should have getFieldCount method")
    void shouldHaveGetFieldCountMethod() throws NoSuchMethodException {
      final Method method = StructInstance.class.getMethod("getFieldCount");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getField method")
    void shouldHaveGetFieldMethod() throws NoSuchMethodException {
      final Method method = StructInstance.class.getMethod("getField", int.class);
      assertEquals(GcValue.class, method.getReturnType(), "Should return GcValue");
    }

    @Test
    @DisplayName("should have setField method")
    void shouldHaveSetFieldMethod() throws NoSuchMethodException {
      final Method method = StructInstance.class.getMethod("setField", int.class, GcValue.class);
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("WASM GC Specification Compliance Tests")
  class WasmGcSpecificationComplianceTests {

    @Test
    @DisplayName("should have all required struct methods")
    void shouldHaveAllRequiredStructMethods() {
      final String[] expectedMethods = {"getType", "getFieldCount", "getField", "setField"};

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(StructInstance.class, methodName), "Should have method: " + methodName);
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
    @DisplayName("getField should take int parameter")
    void getFieldShouldTakeIntParameter() throws NoSuchMethodException {
      final Method method = StructInstance.class.getMethod("getField", int.class);
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
      assertEquals(int.class, method.getParameterTypes()[0], "Parameter should be int");
    }

    @Test
    @DisplayName("setField should take int and GcValue parameters")
    void setFieldShouldTakeIntAndGcValueParameters() throws NoSuchMethodException {
      final Method method = StructInstance.class.getMethod("setField", int.class, GcValue.class);
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
    @DisplayName("getField should declare GcException")
    void getFieldShouldDeclareGcException() throws NoSuchMethodException {
      final Method method = StructInstance.class.getMethod("getField", int.class);
      final Class<?>[] exceptions = method.getExceptionTypes();

      boolean throwsGcException = false;
      for (final Class<?> ex : exceptions) {
        if (ex == GcException.class) {
          throwsGcException = true;
          break;
        }
      }

      assertTrue(throwsGcException, "getField should declare GcException");
    }

    @Test
    @DisplayName("setField should declare GcException")
    void setFieldShouldDeclareGcException() throws NoSuchMethodException {
      final Method method = StructInstance.class.getMethod("setField", int.class, GcValue.class);
      final Class<?>[] exceptions = method.getExceptionTypes();

      boolean throwsGcException = false;
      for (final Class<?> ex : exceptions) {
        if (ex == GcException.class) {
          throwsGcException = true;
          break;
        }
      }

      assertTrue(throwsGcException, "setField should declare GcException");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Documentation Tests")
  class UsagePatternDocumentationTests {

    @Test
    @DisplayName("should support reading struct fields")
    void shouldSupportReadingStructFields() {
      // Documents usage: GcValue value = struct.getField(0);
      assertTrue(hasMethod(StructInstance.class, "getField"), "Need getField method");
    }

    @Test
    @DisplayName("should support writing struct fields")
    void shouldSupportWritingStructFields() {
      // Documents usage: struct.setField(0, value);
      assertTrue(hasMethod(StructInstance.class, "setField"), "Need setField method");
    }

    @Test
    @DisplayName("should support getting field count")
    void shouldSupportGettingFieldCount() {
      // Documents usage: int count = struct.getFieldCount();
      assertTrue(hasMethod(StructInstance.class, "getFieldCount"), "Need getFieldCount method");
    }

    @Test
    @DisplayName("should support getting type")
    void shouldSupportGettingType() {
      // Documents usage: StructType type = struct.getType();
      assertTrue(hasMethod(StructInstance.class, "getType"), "Need getType method");
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
      final Class<?>[] interfaces = StructInstance.class.getInterfaces();
      boolean extendsGcObject = false;

      for (final Class<?> iface : interfaces) {
        if (iface == GcObject.class) {
          extendsGcObject = true;
          break;
        }
      }

      assertTrue(extendsGcObject, "StructInstance should extend GcObject");
    }
  }
}
