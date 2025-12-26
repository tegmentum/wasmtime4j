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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentLinker} interface.
 *
 * <p>ComponentLinker provides the mechanism to define host functions and bind imports before
 * instantiating WebAssembly components. It supports WIT interface definitions and the full
 * Component Model type system.
 */
@DisplayName("ComponentLinker Tests")
class ComponentLinkerTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentLinker.class.getModifiers()),
          "ComponentLinker should be public");
      assertTrue(ComponentLinker.class.isInterface(), "ComponentLinker should be an interface");
    }

    @Test
    @DisplayName("should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(
          Closeable.class.isAssignableFrom(ComponentLinker.class),
          "ComponentLinker should extend Closeable");
    }

    @Test
    @DisplayName("should be generic with type parameter T")
    void shouldBeGenericWithTypeParameterT() {
      final TypeVariable<?>[] typeParams = ComponentLinker.class.getTypeParameters();
      assertEquals(1, typeParams.length, "Should have exactly one type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be named T");
    }
  }

  @Nested
  @DisplayName("defineFunction Method Tests")
  class DefineFunctionMethodTests {

    @Test
    @DisplayName("should have defineFunction method with four parameters")
    void shouldHaveDefineFunctionMethodWithFourParameters() throws NoSuchMethodException {
      final Method method =
          ComponentLinker.class.getMethod(
              "defineFunction",
              String.class,
              String.class,
              String.class,
              ComponentHostFunction.class);
      assertNotNull(method, "defineFunction(4 params) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(declaresWasmException(method), "Should declare WasmException");
    }

    @Test
    @DisplayName("should have defineFunction method with WIT path")
    void shouldHaveDefineFunctionMethodWithWitPath() throws NoSuchMethodException {
      final Method method =
          ComponentLinker.class.getMethod(
              "defineFunction", String.class, ComponentHostFunction.class);
      assertNotNull(method, "defineFunction(witPath) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(declaresWasmException(method), "Should declare WasmException");
    }
  }

  @Nested
  @DisplayName("defineInterface Method Tests")
  class DefineInterfaceMethodTests {

    @Test
    @DisplayName("should have defineInterface method")
    void shouldHaveDefineInterfaceMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLinker.class.getMethod("defineInterface", String.class, String.class, Map.class);
      assertNotNull(method, "defineInterface method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(declaresWasmException(method), "Should declare WasmException");
    }
  }

  @Nested
  @DisplayName("defineResource Method Tests")
  class DefineResourceMethodTests {

    @Test
    @DisplayName("should have defineResource method")
    void shouldHaveDefineResourceMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLinker.class.getMethod(
              "defineResource",
              String.class,
              String.class,
              String.class,
              ComponentResourceDefinition.class);
      assertNotNull(method, "defineResource method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(declaresWasmException(method), "Should declare WasmException");
    }
  }

  @Nested
  @DisplayName("linkInstance Method Tests")
  class LinkInstanceMethodTests {

    @Test
    @DisplayName("should have linkInstance method")
    void shouldHaveLinkInstanceMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLinker.class.getMethod("linkInstance", ComponentInstance.class);
      assertNotNull(method, "linkInstance method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(declaresWasmException(method), "Should declare WasmException");
    }
  }

  @Nested
  @DisplayName("linkComponent Method Tests")
  class LinkComponentMethodTests {

    @Test
    @DisplayName("should have linkComponent method")
    void shouldHaveLinkComponentMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLinker.class.getMethod("linkComponent", Store.class, ComponentSimple.class);
      assertNotNull(method, "linkComponent method should exist");
      assertEquals(
          ComponentInstance.class, method.getReturnType(), "Should return ComponentInstance");
      assertTrue(declaresWasmException(method), "Should declare WasmException");
    }
  }

  @Nested
  @DisplayName("instantiate Method Tests")
  class InstantiateMethodTests {

    @Test
    @DisplayName("should have instantiate method")
    void shouldHaveInstantiateMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLinker.class.getMethod("instantiate", Store.class, ComponentSimple.class);
      assertNotNull(method, "instantiate method should exist");
      assertEquals(
          ComponentInstance.class, method.getReturnType(), "Should return ComponentInstance");
      assertTrue(declaresWasmException(method), "Should declare WasmException");
    }
  }

  @Nested
  @DisplayName("WASI Preview 2 Method Tests")
  class WasiPreview2MethodTests {

    @Test
    @DisplayName("should have enableWasiPreview2 method without parameters")
    void shouldHaveEnableWasiPreview2Method() throws NoSuchMethodException {
      final Method method = ComponentLinker.class.getMethod("enableWasiPreview2");
      assertNotNull(method, "enableWasiPreview2() method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(declaresWasmException(method), "Should declare WasmException");
    }

    @Test
    @DisplayName("should have enableWasiPreview2 method with config parameter")
    void shouldHaveEnableWasiPreview2MethodWithConfig() throws NoSuchMethodException {
      final Method method =
          ComponentLinker.class.getMethod("enableWasiPreview2", WasiPreview2Config.class);
      assertNotNull(method, "enableWasiPreview2(config) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(declaresWasmException(method), "Should declare WasmException");
    }
  }

  @Nested
  @DisplayName("Engine and State Method Tests")
  class EngineAndStateMethodTests {

    @Test
    @DisplayName("should have getEngine method")
    void shouldHaveGetEngineMethod() throws NoSuchMethodException {
      final Method method = ComponentLinker.class.getMethod("getEngine");
      assertNotNull(method, "getEngine method should exist");
      assertEquals(Engine.class, method.getReturnType(), "Should return Engine");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = ComponentLinker.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = ComponentLinker.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Interface Query Method Tests")
  class InterfaceQueryMethodTests {

    @Test
    @DisplayName("should have hasInterface method")
    void shouldHaveHasInterfaceMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLinker.class.getMethod("hasInterface", String.class, String.class);
      assertNotNull(method, "hasInterface method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have hasFunction method")
    void shouldHaveHasFunctionMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLinker.class.getMethod("hasFunction", String.class, String.class, String.class);
      assertNotNull(method, "hasFunction method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getDefinedInterfaces method")
    void shouldHaveGetDefinedInterfacesMethod() throws NoSuchMethodException {
      final Method method = ComponentLinker.class.getMethod("getDefinedInterfaces");
      assertNotNull(method, "getDefinedInterfaces method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have getDefinedFunctions method")
    void shouldHaveGetDefinedFunctionsMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLinker.class.getMethod("getDefinedFunctions", String.class, String.class);
      assertNotNull(method, "getDefinedFunctions method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }
  }

  @Nested
  @DisplayName("Validation Method Tests")
  class ValidationMethodTests {

    @Test
    @DisplayName("should have validateImports method")
    void shouldHaveValidateImportsMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLinker.class.getMethod("validateImports", ComponentSimple.class);
      assertNotNull(method, "validateImports method should exist");
      assertEquals(
          ComponentImportValidation.class,
          method.getReturnType(),
          "Should return ComponentImportValidation");
    }
  }

  @Nested
  @DisplayName("Alias Method Tests")
  class AliasMethodTests {

    @Test
    @DisplayName("should have aliasInterface method")
    void shouldHaveAliasInterfaceMethod() throws NoSuchMethodException {
      final Method method =
          ComponentLinker.class.getMethod(
              "aliasInterface", String.class, String.class, String.class, String.class);
      assertNotNull(method, "aliasInterface method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(declaresWasmException(method), "Should declare WasmException");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have static create factory method")
    void shouldHaveStaticCreateFactoryMethod() throws NoSuchMethodException {
      final Method method = ComponentLinker.class.getMethod("create", Engine.class);
      assertNotNull(method, "create static method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(ComponentLinker.class, method.getReturnType(), "Should return ComponentLinker");
      assertTrue(declaresWasmException(method), "Should declare WasmException");
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have expected number of methods")
    void shouldHaveExpectedNumberOfMethods() {
      final Method[] methods = ComponentLinker.class.getDeclaredMethods();
      // 2 defineFunction overloads + defineInterface + defineResource + linkInstance +
      // linkComponent + instantiate + 2 enableWasiPreview2 + getEngine + isValid + hasInterface +
      // hasFunction + getDefinedInterfaces + getDefinedFunctions + validateImports +
      // aliasInterface + close + create = 19+
      assertTrue(methods.length >= 17, "Should have at least 17 declared methods");
    }
  }

  /**
   * Helper to check if a method declares WasmException.
   *
   * @param method the method to check
   * @return true if the method declares WasmException
   */
  private boolean declaresWasmException(final Method method) {
    for (Class<?> exception : method.getExceptionTypes()) {
      if (exception.equals(WasmException.class)) {
        return true;
      }
    }
    return false;
  }
}
