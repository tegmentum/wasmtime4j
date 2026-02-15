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

import ai.tegmentum.wasmtime4j.async.AsyncRuntimeFactory;
import ai.tegmentum.wasmtime4j.spi.CallerContextProvider;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for JNI Provider classes.
 *
 * <p>This test class verifies the structure and interface compliance of the JNI ServiceLoader
 * providers without loading native libraries.
 *
 * <p>Classes tested:
 *
 * <ul>
 *   <li>{@link JniAsyncRuntimeProvider}
 *   <li>{@link JniCallerContextProvider}
 * </ul>
 */
@DisplayName("JNI Provider Classes Tests")
class JniProviderClassesTest {

  // ===================================================================================
  // JniAsyncRuntimeProvider Tests
  // ===================================================================================

  @Nested
  @DisplayName("JniAsyncRuntimeProvider Tests")
  class JniAsyncRuntimeProviderTests {

    @Nested
    @DisplayName("Class Structure Tests")
    class ClassStructureTests {

      @Test
      @DisplayName("JniAsyncRuntimeProvider should be final class")
      void shouldBeFinalClass() {
        assertTrue(
            Modifier.isFinal(JniAsyncRuntimeProvider.class.getModifiers()),
            "JniAsyncRuntimeProvider should be final");
      }

      @Test
      @DisplayName("JniAsyncRuntimeProvider should be public")
      void shouldBePublic() {
        assertTrue(
            Modifier.isPublic(JniAsyncRuntimeProvider.class.getModifiers()),
            "JniAsyncRuntimeProvider should be public");
      }

      @Test
      @DisplayName("JniAsyncRuntimeProvider should implement AsyncRuntimeProvider")
      void shouldImplementAsyncRuntimeProvider() {
        assertTrue(
            AsyncRuntimeFactory.AsyncRuntimeProvider.class.isAssignableFrom(
                JniAsyncRuntimeProvider.class),
            "JniAsyncRuntimeProvider should implement AsyncRuntimeFactory.AsyncRuntimeProvider");
      }
    }

    @Nested
    @DisplayName("Field Tests")
    class FieldTests {

      @Test
      @DisplayName("Should have LOGGER field")
      void shouldHaveLoggerField() throws NoSuchFieldException {
        Field field = JniAsyncRuntimeProvider.class.getDeclaredField("LOGGER");
        assertNotNull(field, "LOGGER field should exist");
        assertEquals(Logger.class, field.getType(), "LOGGER should be Logger type");
        assertTrue(Modifier.isPrivate(field.getModifiers()), "LOGGER should be private");
        assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
        assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
      }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

      @Test
      @DisplayName("Should have public no-arg constructor for ServiceLoader")
      void shouldHavePublicNoArgConstructor() throws NoSuchMethodException {
        Constructor<?> constructor = JniAsyncRuntimeProvider.class.getDeclaredConstructor();
        assertNotNull(constructor, "No-arg constructor should exist");
        assertTrue(
            Modifier.isPublic(constructor.getModifiers()),
            "Constructor should be public for ServiceLoader");
      }

      @Test
      @DisplayName("No-arg constructor should be instantiable")
      void noArgConstructorShouldBeInstantiable() throws Exception {
        JniAsyncRuntimeProvider provider = new JniAsyncRuntimeProvider();
        assertNotNull(provider, "Provider should be instantiable");
      }
    }

    @Nested
    @DisplayName("Method Tests")
    class MethodTests {

      @Test
      @DisplayName("Should have create method returning AsyncRuntime")
      void shouldHaveCreateMethod() throws NoSuchMethodException {
        Method method = JniAsyncRuntimeProvider.class.getMethod("create");
        assertNotNull(method, "create method should exist");
        assertEquals(
            ai.tegmentum.wasmtime4j.async.AsyncRuntime.class,
            method.getReturnType(),
            "create should return AsyncRuntime");
        assertTrue(Modifier.isPublic(method.getModifiers()), "create should be public");
      }
    }

    @Nested
    @DisplayName("Package Location Tests")
    class PackageLocationTests {

      @Test
      @DisplayName("Class should be in correct package")
      void shouldBeInCorrectPackage() {
        assertEquals(
            "ai.tegmentum.wasmtime4j.jni",
            JniAsyncRuntimeProvider.class.getPackage().getName(),
            "Should be in ai.tegmentum.wasmtime4j.jni package");
      }
    }
  }

  // ===================================================================================
  // JniCallerContextProvider Tests
  // ===================================================================================

  @Nested
  @DisplayName("JniCallerContextProvider Tests")
  class JniCallerContextProviderTests {

    @Nested
    @DisplayName("Class Structure Tests")
    class ClassStructureTests {

      @Test
      @DisplayName("JniCallerContextProvider should be final class")
      void shouldBeFinalClass() {
        assertTrue(
            Modifier.isFinal(JniCallerContextProvider.class.getModifiers()),
            "JniCallerContextProvider should be final");
      }

      @Test
      @DisplayName("JniCallerContextProvider should be public")
      void shouldBePublic() {
        assertTrue(
            Modifier.isPublic(JniCallerContextProvider.class.getModifiers()),
            "JniCallerContextProvider should be public");
      }

      @Test
      @DisplayName("JniCallerContextProvider should implement CallerContextProvider")
      void shouldImplementCallerContextProvider() {
        assertTrue(
            CallerContextProvider.class.isAssignableFrom(JniCallerContextProvider.class),
            "JniCallerContextProvider should implement CallerContextProvider");
      }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

      @Test
      @DisplayName("Should have public no-arg constructor for ServiceLoader")
      void shouldHavePublicNoArgConstructor() throws NoSuchMethodException {
        Constructor<?> constructor = JniCallerContextProvider.class.getDeclaredConstructor();
        assertNotNull(constructor, "No-arg constructor should exist");
        assertTrue(
            Modifier.isPublic(constructor.getModifiers()),
            "Constructor should be public for ServiceLoader");
      }

      @Test
      @DisplayName("No-arg constructor should be instantiable")
      void noArgConstructorShouldBeInstantiable() throws Exception {
        JniCallerContextProvider provider = new JniCallerContextProvider();
        assertNotNull(provider, "Provider should be instantiable");
      }
    }

    @Nested
    @DisplayName("Method Tests")
    class MethodTests {

      @Test
      @DisplayName("Should have getCurrentCaller method returning Caller")
      void shouldHaveGetCurrentCallerMethod() throws NoSuchMethodException {
        Method method = JniCallerContextProvider.class.getMethod("getCurrentCaller");
        assertNotNull(method, "getCurrentCaller method should exist");
        assertEquals(
            ai.tegmentum.wasmtime4j.func.Caller.class,
            method.getReturnType(),
            "getCurrentCaller should return Caller");
        assertTrue(Modifier.isPublic(method.getModifiers()), "getCurrentCaller should be public");
      }
    }

    @Nested
    @DisplayName("Package Location Tests")
    class PackageLocationTests {

      @Test
      @DisplayName("Class should be in correct package")
      void shouldBeInCorrectPackage() {
        assertEquals(
            "ai.tegmentum.wasmtime4j.jni",
            JniCallerContextProvider.class.getPackage().getName(),
            "Should be in ai.tegmentum.wasmtime4j.jni package");
      }
    }

    @Nested
    @DisplayName("Minimal Footprint Tests")
    class MinimalFootprintTests {

      @Test
      @DisplayName("Should have no non-synthetic fields (delegates to JniHostFunction)")
      void shouldHaveNoNonSyntheticFields() {
        // Count only non-synthetic fields (excludes $jacocoData and similar)
        long nonSyntheticCount =
            java.util.Arrays.stream(JniCallerContextProvider.class.getDeclaredFields())
                .filter(f -> !f.isSynthetic())
                .count();
        assertEquals(
            0, nonSyntheticCount, "JniCallerContextProvider should have no non-synthetic fields");
      }

      @Test
      @DisplayName("Should have only declared methods (excluding synthetic)")
      void shouldHaveExpectedMethods() {
        // Count only non-synthetic methods (excludes $jacocoInit and similar)
        long nonSyntheticCount =
            java.util.Arrays.stream(JniCallerContextProvider.class.getDeclaredMethods())
                .filter(m -> !m.isSynthetic())
                .count();
        assertTrue(
            nonSyntheticCount >= 1,
            "Should have at least one non-synthetic method, found: " + nonSyntheticCount);
      }
    }
  }

  // ===================================================================================
  // Cross-Provider Consistency Tests
  // ===================================================================================

  @Nested
  @DisplayName("Cross-Provider Consistency Tests")
  class CrossProviderConsistencyTests {

    @Test
    @DisplayName("All providers should be final classes")
    void allProvidersShouldBeFinal() {
      assertTrue(
          Modifier.isFinal(JniAsyncRuntimeProvider.class.getModifiers()),
          "JniAsyncRuntimeProvider should be final");
      assertTrue(
          Modifier.isFinal(JniCallerContextProvider.class.getModifiers()),
          "JniCallerContextProvider should be final");
    }

    @Test
    @DisplayName("All providers should be public")
    void allProvidersShouldBePublic() {
      assertTrue(
          Modifier.isPublic(JniAsyncRuntimeProvider.class.getModifiers()),
          "JniAsyncRuntimeProvider should be public");
      assertTrue(
          Modifier.isPublic(JniCallerContextProvider.class.getModifiers()),
          "JniCallerContextProvider should be public");
    }

    @Test
    @DisplayName("All providers should have public no-arg constructors")
    void allProvidersShouldHaveNoArgConstructors() throws NoSuchMethodException {
      final Constructor<?> c1 = JniAsyncRuntimeProvider.class.getDeclaredConstructor();
      final Constructor<?> c2 = JniCallerContextProvider.class.getDeclaredConstructor();

      assertTrue(Modifier.isPublic(c1.getModifiers()), "JniAsyncRuntimeProvider constructor");
      assertTrue(Modifier.isPublic(c2.getModifiers()), "JniCallerContextProvider constructor");
    }

    @Test
    @DisplayName("All providers should be in same package")
    void allProvidersShouldBeInSamePackage() {
      String expectedPackage = "ai.tegmentum.wasmtime4j.jni";
      assertEquals(expectedPackage, JniAsyncRuntimeProvider.class.getPackage().getName());
      assertEquals(expectedPackage, JniCallerContextProvider.class.getPackage().getName());
    }

    @Test
    @DisplayName("All providers should be instantiable")
    void allProvidersShouldBeInstantiable() throws Exception {
      assertNotNull(
          new JniAsyncRuntimeProvider(), "JniAsyncRuntimeProvider should be instantiable");
      assertNotNull(
          new JniCallerContextProvider(), "JniCallerContextProvider should be instantiable");
    }
  }

  // ===================================================================================
  // Interface Compliance Tests
  // ===================================================================================

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("JniAsyncRuntimeProvider should implement all interface methods")
    void asyncRuntimeProviderShouldImplementAllMethods() {
      Class<?> providerInterface = AsyncRuntimeFactory.AsyncRuntimeProvider.class;
      for (Method interfaceMethod : providerInterface.getMethods()) {
        if (!interfaceMethod.isDefault() && !Modifier.isStatic(interfaceMethod.getModifiers())) {
          boolean found = false;
          for (Method implMethod : JniAsyncRuntimeProvider.class.getMethods()) {
            if (methodsMatch(interfaceMethod, implMethod)) {
              found = true;
              break;
            }
          }
          assertTrue(found, "Should implement: " + interfaceMethod.getName());
        }
      }
    }

    @Test
    @DisplayName("JniCallerContextProvider should implement all interface methods")
    void callerContextProviderShouldImplementAllMethods() {
      Class<?> providerInterface = CallerContextProvider.class;
      for (Method interfaceMethod : providerInterface.getMethods()) {
        if (!interfaceMethod.isDefault() && !Modifier.isStatic(interfaceMethod.getModifiers())) {
          boolean found = false;
          for (Method implMethod : JniCallerContextProvider.class.getMethods()) {
            if (methodsMatch(interfaceMethod, implMethod)) {
              found = true;
              break;
            }
          }
          assertTrue(found, "Should implement: " + interfaceMethod.getName());
        }
      }
    }

    private boolean methodsMatch(final Method a, final Method b) {
      if (!a.getName().equals(b.getName())) {
        return false;
      }
      final Class<?>[] paramsA = a.getParameterTypes();
      final Class<?>[] paramsB = b.getParameterTypes();
      if (paramsA.length != paramsB.length) {
        return false;
      }
      for (int i = 0; i < paramsA.length; i++) {
        if (!paramsA[i].equals(paramsB[i])) {
          return false;
        }
      }
      return true;
    }
  }

  // ===================================================================================
  // ServiceLoader Pattern Tests
  // ===================================================================================

  @Nested
  @DisplayName("ServiceLoader Pattern Tests")
  class ServiceLoaderPatternTests {

    @Test
    @DisplayName("Providers follow ServiceLoader instantiation requirements")
    void providersShouldFollowServiceLoaderPattern() throws Exception {
      // ServiceLoader requires:
      // 1. Public no-arg constructor
      // 2. Class implements the service interface
      // 3. Class is concrete (not abstract)

      // JniAsyncRuntimeProvider
      assertProviderFollowsServiceLoaderPattern(
          JniAsyncRuntimeProvider.class, AsyncRuntimeFactory.AsyncRuntimeProvider.class);

      // JniCallerContextProvider
      assertProviderFollowsServiceLoaderPattern(
          JniCallerContextProvider.class, CallerContextProvider.class);
    }

    private void assertProviderFollowsServiceLoaderPattern(
        final Class<?> provider, final Class<?> serviceInterface) throws Exception {
      // Must not be abstract
      assertTrue(
          !Modifier.isAbstract(provider.getModifiers()),
          provider.getSimpleName() + " should not be abstract");

      // Must implement service interface
      assertTrue(
          serviceInterface.isAssignableFrom(provider),
          provider.getSimpleName() + " should implement " + serviceInterface.getSimpleName());

      // Must have public no-arg constructor
      Constructor<?> constructor = provider.getDeclaredConstructor();
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()),
          provider.getSimpleName() + " should have public no-arg constructor");

      // Constructor must be callable
      Object instance = constructor.newInstance();
      assertNotNull(instance, provider.getSimpleName() + " should be instantiable");
    }
  }
}
