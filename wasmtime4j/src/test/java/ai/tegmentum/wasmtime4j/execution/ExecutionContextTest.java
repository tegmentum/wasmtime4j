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

package ai.tegmentum.wasmtime4j.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExecutionContext} interface.
 *
 * <p>ExecutionContext provides execution context for WebAssembly components.
 */
@DisplayName("ExecutionContext Tests")
class ExecutionContextTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ExecutionContext.class.getModifiers()),
          "ExecutionContext should be public");
      assertTrue(ExecutionContext.class.isInterface(), "ExecutionContext should be an interface");
    }

    @Test
    @DisplayName("should have getContextId method")
    void shouldHaveGetContextIdMethod() throws NoSuchMethodException {
      final Method method = ExecutionContext.class.getMethod("getContextId");
      assertNotNull(method, "getContextId method should exist");
      assertEquals(String.class, method.getReturnType(), "getContextId should return String");
    }

    @Test
    @DisplayName("should have getEnvironment method")
    void shouldHaveGetEnvironmentMethod() throws NoSuchMethodException {
      final Method method = ExecutionContext.class.getMethod("getEnvironment");
      assertNotNull(method, "getEnvironment method should exist");
      assertEquals(
          ExecutionContext.ExecutionEnvironment.class,
          method.getReturnType(),
          "getEnvironment should return ExecutionEnvironment");
    }

    @Test
    @DisplayName("should have getParameters method")
    void shouldHaveGetParametersMethod() throws NoSuchMethodException {
      final Method method = ExecutionContext.class.getMethod("getParameters");
      assertNotNull(method, "getParameters method should exist");
      assertEquals(Map.class, method.getReturnType(), "getParameters should return Map");
    }

    @Test
    @DisplayName("should have setParameter method")
    void shouldHaveSetParameterMethod() throws NoSuchMethodException {
      final Method method =
          ExecutionContext.class.getMethod("setParameter", String.class, Object.class);
      assertNotNull(method, "setParameter method should exist");
      assertEquals(void.class, method.getReturnType(), "setParameter should return void");
    }

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      final Method method = ExecutionContext.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(
          ExecutionContextConfig.class,
          method.getReturnType(),
          "getConfig should return ExecutionContextConfig");
    }

    @Test
    @DisplayName("should have getSecurityContext method")
    void shouldHaveGetSecurityContextMethod() throws NoSuchMethodException {
      final Method method = ExecutionContext.class.getMethod("getSecurityContext");
      assertNotNull(method, "getSecurityContext method should exist");
      assertEquals(Object.class, method.getReturnType(), "getSecurityContext should return Object");
    }

    @Test
    @DisplayName("should have getMetadata method")
    void shouldHaveGetMetadataMethod() throws NoSuchMethodException {
      final Method method = ExecutionContext.class.getMethod("getMetadata");
      assertNotNull(method, "getMetadata method should exist");
      assertEquals(
          ExecutionContext.ExecutionMetadata.class,
          method.getReturnType(),
          "getMetadata should return ExecutionMetadata");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = ExecutionContext.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }
  }

  @Nested
  @DisplayName("ExecutionEnvironment Inner Interface Tests")
  class ExecutionEnvironmentTests {

    @Test
    @DisplayName("ExecutionEnvironment should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ExecutionContext.ExecutionEnvironment.class.getModifiers()),
          "ExecutionEnvironment should be public");
      assertTrue(
          ExecutionContext.ExecutionEnvironment.class.isInterface(),
          "ExecutionEnvironment should be an interface");
    }

    @Test
    @DisplayName("should have getEnvironmentVariables method")
    void shouldHaveGetEnvironmentVariablesMethod() throws NoSuchMethodException {
      final Method method =
          ExecutionContext.ExecutionEnvironment.class.getMethod("getEnvironmentVariables");
      assertNotNull(method, "getEnvironmentVariables method should exist");
      assertEquals(Map.class, method.getReturnType(), "getEnvironmentVariables should return Map");
    }

    @Test
    @DisplayName("should have getWorkingDirectory method")
    void shouldHaveGetWorkingDirectoryMethod() throws NoSuchMethodException {
      final Method method =
          ExecutionContext.ExecutionEnvironment.class.getMethod("getWorkingDirectory");
      assertNotNull(method, "getWorkingDirectory method should exist");
      assertEquals(
          String.class, method.getReturnType(), "getWorkingDirectory should return String");
    }

    @Test
    @DisplayName("should have getResourceLimits method")
    void shouldHaveGetResourceLimitsMethod() throws NoSuchMethodException {
      final Method method =
          ExecutionContext.ExecutionEnvironment.class.getMethod("getResourceLimits");
      assertNotNull(method, "getResourceLimits method should exist");
      assertEquals(
          ExecutionContext.ResourceLimits.class,
          method.getReturnType(),
          "getResourceLimits should return ResourceLimits");
    }
  }

  @Nested
  @DisplayName("ResourceLimits Inner Interface Tests")
  class ResourceLimitsTests {

    @Test
    @DisplayName("ResourceLimits should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ExecutionContext.ResourceLimits.class.getModifiers()),
          "ResourceLimits should be public");
      assertTrue(
          ExecutionContext.ResourceLimits.class.isInterface(),
          "ResourceLimits should be an interface");
    }

    @Test
    @DisplayName("should have getMaxMemory method")
    void shouldHaveGetMaxMemoryMethod() throws NoSuchMethodException {
      final Method method = ExecutionContext.ResourceLimits.class.getMethod("getMaxMemory");
      assertNotNull(method, "getMaxMemory method should exist");
      assertEquals(long.class, method.getReturnType(), "getMaxMemory should return long");
    }

    @Test
    @DisplayName("should have getMaxExecutionTime method")
    void shouldHaveGetMaxExecutionTimeMethod() throws NoSuchMethodException {
      final Method method = ExecutionContext.ResourceLimits.class.getMethod("getMaxExecutionTime");
      assertNotNull(method, "getMaxExecutionTime method should exist");
      assertEquals(long.class, method.getReturnType(), "getMaxExecutionTime should return long");
    }

    @Test
    @DisplayName("should have getMaxCpuTime method")
    void shouldHaveGetMaxCpuTimeMethod() throws NoSuchMethodException {
      final Method method = ExecutionContext.ResourceLimits.class.getMethod("getMaxCpuTime");
      assertNotNull(method, "getMaxCpuTime method should exist");
      assertEquals(long.class, method.getReturnType(), "getMaxCpuTime should return long");
    }
  }

  @Nested
  @DisplayName("ExecutionMetadata Inner Interface Tests")
  class ExecutionMetadataTests {

    @Test
    @DisplayName("ExecutionMetadata should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ExecutionContext.ExecutionMetadata.class.getModifiers()),
          "ExecutionMetadata should be public");
      assertTrue(
          ExecutionContext.ExecutionMetadata.class.isInterface(),
          "ExecutionMetadata should be an interface");
    }

    @Test
    @DisplayName("should have getCreationTime method")
    void shouldHaveGetCreationTimeMethod() throws NoSuchMethodException {
      final Method method = ExecutionContext.ExecutionMetadata.class.getMethod("getCreationTime");
      assertNotNull(method, "getCreationTime method should exist");
      assertEquals(long.class, method.getReturnType(), "getCreationTime should return long");
    }

    @Test
    @DisplayName("should have getLastAccessTime method")
    void shouldHaveGetLastAccessTimeMethod() throws NoSuchMethodException {
      final Method method = ExecutionContext.ExecutionMetadata.class.getMethod("getLastAccessTime");
      assertNotNull(method, "getLastAccessTime method should exist");
      assertEquals(long.class, method.getReturnType(), "getLastAccessTime should return long");
    }

    @Test
    @DisplayName("should have getCreator method")
    void shouldHaveGetCreatorMethod() throws NoSuchMethodException {
      final Method method = ExecutionContext.ExecutionMetadata.class.getMethod("getCreator");
      assertNotNull(method, "getCreator method should exist");
      assertEquals(String.class, method.getReturnType(), "getCreator should return String");
    }

    @Test
    @DisplayName("should have getTags method")
    void shouldHaveGetTagsMethod() throws NoSuchMethodException {
      final Method method = ExecutionContext.ExecutionMetadata.class.getMethod("getTags");
      assertNotNull(method, "getTags method should exist");
      assertEquals(Set.class, method.getReturnType(), "getTags should return Set");
    }
  }

  @Nested
  @DisplayName("Interface Nesting Tests")
  class InterfaceNestingTests {

    @Test
    @DisplayName("ExecutionEnvironment should be nested in ExecutionContext")
    void executionEnvironmentShouldBeNested() {
      assertTrue(
          ExecutionContext.ExecutionEnvironment.class.isMemberClass(),
          "ExecutionEnvironment should be a member class");
      assertEquals(
          ExecutionContext.class,
          ExecutionContext.ExecutionEnvironment.class.getDeclaringClass(),
          "ExecutionEnvironment should be declared in ExecutionContext");
    }

    @Test
    @DisplayName("ResourceLimits should be nested in ExecutionContext")
    void resourceLimitsShouldBeNested() {
      assertTrue(
          ExecutionContext.ResourceLimits.class.isMemberClass(),
          "ResourceLimits should be a member class");
      assertEquals(
          ExecutionContext.class,
          ExecutionContext.ResourceLimits.class.getDeclaringClass(),
          "ResourceLimits should be declared in ExecutionContext");
    }

    @Test
    @DisplayName("ExecutionMetadata should be nested in ExecutionContext")
    void executionMetadataShouldBeNested() {
      assertTrue(
          ExecutionContext.ExecutionMetadata.class.isMemberClass(),
          "ExecutionMetadata should be a member class");
      assertEquals(
          ExecutionContext.class,
          ExecutionContext.ExecutionMetadata.class.getDeclaringClass(),
          "ExecutionMetadata should be declared in ExecutionContext");
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("ExecutionContext should have 8 declared methods")
    void executionContextShouldHave8Methods() {
      final Method[] methods = ExecutionContext.class.getDeclaredMethods();
      assertEquals(8, methods.length, "ExecutionContext should have 8 declared methods");
    }

    @Test
    @DisplayName("ExecutionEnvironment should have 3 declared methods")
    void executionEnvironmentShouldHave3Methods() {
      final Method[] methods = ExecutionContext.ExecutionEnvironment.class.getDeclaredMethods();
      assertEquals(3, methods.length, "ExecutionEnvironment should have 3 declared methods");
    }

    @Test
    @DisplayName("ResourceLimits should have 3 declared methods")
    void resourceLimitsShouldHave3Methods() {
      final Method[] methods = ExecutionContext.ResourceLimits.class.getDeclaredMethods();
      assertEquals(3, methods.length, "ResourceLimits should have 3 declared methods");
    }

    @Test
    @DisplayName("ExecutionMetadata should have 4 declared methods")
    void executionMetadataShouldHave4Methods() {
      final Method[] methods = ExecutionContext.ExecutionMetadata.class.getDeclaredMethods();
      assertEquals(4, methods.length, "ExecutionMetadata should have 4 declared methods");
    }
  }

  @Nested
  @DisplayName("Interface Modifier Tests")
  class InterfaceModifierTests {

    @Test
    @DisplayName("all inner interfaces should be static")
    void allInnerInterfacesShouldBeStatic() {
      assertTrue(
          Modifier.isStatic(ExecutionContext.ExecutionEnvironment.class.getModifiers()),
          "ExecutionEnvironment should be static");
      assertTrue(
          Modifier.isStatic(ExecutionContext.ResourceLimits.class.getModifiers()),
          "ResourceLimits should be static");
      assertTrue(
          Modifier.isStatic(ExecutionContext.ExecutionMetadata.class.getModifiers()),
          "ExecutionMetadata should be static");
    }

    @Test
    @DisplayName("all interface methods should be public")
    void allMethodsShouldBePublic() {
      for (final Method method : ExecutionContext.class.getDeclaredMethods()) {
        assertTrue(
            Modifier.isPublic(method.getModifiers()),
            "Method " + method.getName() + " should be public");
      }
    }

    @Test
    @DisplayName("all interface methods should be abstract")
    void allMethodsShouldBeAbstract() {
      for (final Method method : ExecutionContext.class.getDeclaredMethods()) {
        assertTrue(
            Modifier.isAbstract(method.getModifiers()),
            "Method " + method.getName() + " should be abstract");
      }
    }
  }
}
