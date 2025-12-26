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

import ai.tegmentum.wasmtime4j.WitInterfaceDefinition;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniWitInterfaceDefinition} class.
 *
 * <p>JniWitInterfaceDefinition provides JNI implementation of WIT interface definitions.
 */
@DisplayName("JniWitInterfaceDefinition Tests")
class JniWitInterfaceDefinitionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public and final")
    void shouldBePublicAndFinal() {
      assertTrue(
          Modifier.isPublic(JniWitInterfaceDefinition.class.getModifiers()),
          "JniWitInterfaceDefinition should be public");
      assertTrue(
          Modifier.isFinal(JniWitInterfaceDefinition.class.getModifiers()),
          "JniWitInterfaceDefinition should be final");
    }

    @Test
    @DisplayName("should implement WitInterfaceDefinition interface")
    void shouldImplementWitInterfaceDefinitionInterface() {
      assertTrue(
          WitInterfaceDefinition.class.isAssignableFrom(JniWitInterfaceDefinition.class),
          "JniWitInterfaceDefinition should implement WitInterfaceDefinition");
    }

    @Test
    @DisplayName("should have constructor with name, version, packageName, exports, imports")
    void shouldHaveConstructorWithAllParameters() throws NoSuchMethodException {
      final Constructor<?> constructor =
          JniWitInterfaceDefinition.class.getConstructor(
              String.class, String.class, String.class, Set.class, Set.class);
      assertNotNull(constructor, "Constructor with all parameters should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = JniWitInterfaceDefinition.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "getName should return String");
    }

    @Test
    @DisplayName("should have getVersion method")
    void shouldHaveGetVersionMethod() throws NoSuchMethodException {
      final Method method = JniWitInterfaceDefinition.class.getMethod("getVersion");
      assertNotNull(method, "getVersion method should exist");
      assertEquals(String.class, method.getReturnType(), "getVersion should return String");
    }

    @Test
    @DisplayName("should have getPackageName method")
    void shouldHaveGetPackageNameMethod() throws NoSuchMethodException {
      final Method method = JniWitInterfaceDefinition.class.getMethod("getPackageName");
      assertNotNull(method, "getPackageName method should exist");
      assertEquals(String.class, method.getReturnType(), "getPackageName should return String");
    }

    @Test
    @DisplayName("should have getFunctionNames method")
    void shouldHaveGetFunctionNamesMethod() throws NoSuchMethodException {
      final Method method = JniWitInterfaceDefinition.class.getMethod("getFunctionNames");
      assertNotNull(method, "getFunctionNames method should exist");
      assertEquals(List.class, method.getReturnType(), "getFunctionNames should return List");
    }

    @Test
    @DisplayName("should have getTypeNames method")
    void shouldHaveGetTypeNamesMethod() throws NoSuchMethodException {
      final Method method = JniWitInterfaceDefinition.class.getMethod("getTypeNames");
      assertNotNull(method, "getTypeNames method should exist");
      assertEquals(List.class, method.getReturnType(), "getTypeNames should return List");
    }

    @Test
    @DisplayName("should have getDependencies method")
    void shouldHaveGetDependenciesMethod() throws NoSuchMethodException {
      final Method method = JniWitInterfaceDefinition.class.getMethod("getDependencies");
      assertNotNull(method, "getDependencies method should exist");
      assertEquals(Set.class, method.getReturnType(), "getDependencies should return Set");
    }

    @Test
    @DisplayName("should have getImportNames method")
    void shouldHaveGetImportNamesMethod() throws NoSuchMethodException {
      final Method method = JniWitInterfaceDefinition.class.getMethod("getImportNames");
      assertNotNull(method, "getImportNames method should exist");
      assertEquals(List.class, method.getReturnType(), "getImportNames should return List");
    }

    @Test
    @DisplayName("should have getExportNames method")
    void shouldHaveGetExportNamesMethod() throws NoSuchMethodException {
      final Method method = JniWitInterfaceDefinition.class.getMethod("getExportNames");
      assertNotNull(method, "getExportNames method should exist");
      assertEquals(List.class, method.getReturnType(), "getExportNames should return List");
    }
  }

  @Nested
  @DisplayName("Interface Methods Tests")
  class InterfaceMethodsTests {

    @Test
    @DisplayName("should have isCompatibleWith method")
    void shouldHaveIsCompatibleWithMethod() throws NoSuchMethodException {
      final Method method =
          JniWitInterfaceDefinition.class.getMethod(
              "isCompatibleWith", WitInterfaceDefinition.class);
      assertNotNull(method, "isCompatibleWith method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.WitCompatibilityResult.class,
          method.getReturnType(),
          "isCompatibleWith should return WitCompatibilityResult");
    }

    @Test
    @DisplayName("should have getWitText method")
    void shouldHaveGetWitTextMethod() throws NoSuchMethodException {
      final Method method = JniWitInterfaceDefinition.class.getMethod("getWitText");
      assertNotNull(method, "getWitText method should exist");
      assertEquals(String.class, method.getReturnType(), "getWitText should return String");
    }
  }

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    @Test
    @DisplayName("should create instance with all parameters")
    void shouldCreateInstanceWithAllParameters() {
      final Set<String> exports = new HashSet<>(Arrays.asList("export1", "export2"));
      final Set<String> imports = new HashSet<>(Arrays.asList("import1"));

      final JniWitInterfaceDefinition definition =
          new JniWitInterfaceDefinition(
              "test-interface", "1.0.0", "test-package", exports, imports);

      assertEquals("test-interface", definition.getName(), "Name should match");
      assertEquals("1.0.0", definition.getVersion(), "Version should match");
      assertEquals("test-package", definition.getPackageName(), "Package name should match");
    }

    @Test
    @DisplayName("should handle null parameters gracefully")
    void shouldHandleNullParametersGracefully() {
      final JniWitInterfaceDefinition definition =
          new JniWitInterfaceDefinition(null, null, null, null, null);

      assertNotNull(definition.getName(), "Name should not be null");
      assertNotNull(definition.getVersion(), "Version should not be null");
      assertNotNull(definition.getPackageName(), "Package name should not be null");
    }
  }
}
