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

package ai.tegmentum.wasmtime4j.jni.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive package-level tests for the JNI execution package.
 *
 * <p>This test class verifies the API contracts, class structure, and method signatures of all
 * execution-related classes in the jni.execution package using reflection-based testing to avoid
 * native library loading.
 *
 * @since 1.0.0
 */
@DisplayName("JNI Execution Package Tests")
public class JniExecutionPackageTest {

  private static final Logger LOGGER = Logger.getLogger(JniExecutionPackageTest.class.getName());

  private static final String PACKAGE_PREFIX = "ai.tegmentum.wasmtime4j.jni.execution.";

  /**
   * Loads a class without triggering static initialization.
   *
   * @param className the fully qualified class name
   * @return the loaded class
   * @throws ClassNotFoundException if the class cannot be found
   */
  private Class<?> loadClassWithoutInit(final String className) throws ClassNotFoundException {
    return Class.forName(className, false, getClass().getClassLoader());
  }

  @Nested
  @DisplayName("JniResourceLimiter Tests")
  class JniResourceLimiterTests {

    @Test
    @DisplayName("Should be a final class implementing ResourceLimiter")
    void shouldBeFinalAndImplementResourceLimiter() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "JniResourceLimiter");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "JniResourceLimiter should be final");

      // Check that it implements ResourceLimiter interface
      boolean implementsResourceLimiter = false;
      for (final Class<?> iface : clazz.getInterfaces()) {
        if ("ResourceLimiter".equals(iface.getSimpleName())) {
          implementsResourceLimiter = true;
          break;
        }
      }
      // May extend JniResource which implements AutoCloseable
      Class<?> superClass = clazz.getSuperclass();
      while (superClass != null) {
        for (final Class<?> iface : superClass.getInterfaces()) {
          if ("ResourceLimiter".equals(iface.getSimpleName())) {
            implementsResourceLimiter = true;
            break;
          }
        }
        superClass = superClass.getSuperclass();
      }

      assertTrue(implementsResourceLimiter, "JniResourceLimiter should implement ResourceLimiter");
    }

    @Test
    @DisplayName("Should have config field")
    void shouldHaveConfigField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "JniResourceLimiter");
      boolean hasConfig = false;

      for (final Field field : clazz.getDeclaredFields()) {
        if ("config".equals(field.getName())) {
          hasConfig = true;
          assertTrue(
              Modifier.isPrivate(field.getModifiers()) && Modifier.isFinal(field.getModifiers()),
              "config should be private final");
          break;
        }
      }

      assertTrue(hasConfig, "Should have config field");
    }

    @Test
    @DisplayName("Should have static factory methods")
    void shouldHaveStaticFactoryMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "JniResourceLimiter");
      boolean hasCreate = false;
      boolean hasCreateDefault = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("create".equals(method.getName()) && Modifier.isStatic(method.getModifiers())) {
          hasCreate = true;
          assertEquals(1, method.getParameterCount(), "create should have 1 parameter");
        }
        if ("createDefault".equals(method.getName()) && Modifier.isStatic(method.getModifiers())) {
          hasCreateDefault = true;
          assertEquals(0, method.getParameterCount(), "createDefault should have no parameters");
        }
      }

      assertTrue(hasCreate, "Should have static create method");
      assertTrue(hasCreateDefault, "Should have static createDefault method");
    }

    @Test
    @DisplayName("Should have resource limit check methods")
    void shouldHaveResourceLimitCheckMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "JniResourceLimiter");
      boolean hasAllowMemoryGrow = false;
      boolean hasAllowTableGrow = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        final String methodName = method.getName();
        if ("allowMemoryGrow".equals(methodName)) {
          hasAllowMemoryGrow = true;
          assertEquals(2, method.getParameterCount(), "allowMemoryGrow should have 2 parameters");
        }
        if ("allowTableGrow".equals(methodName)) {
          hasAllowTableGrow = true;
          assertEquals(2, method.getParameterCount(), "allowTableGrow should have 2 parameters");
        }
      }

      assertTrue(hasAllowMemoryGrow, "Should have allowMemoryGrow method");
      assertTrue(hasAllowTableGrow, "Should have allowTableGrow method");
    }

    @Test
    @DisplayName("Should have getId method")
    void shouldHaveGetIdMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "JniResourceLimiter");
      boolean hasGetId = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("getId".equals(method.getName())) {
          hasGetId = true;
          assertEquals(long.class, method.getReturnType(), "getId should return long");
          break;
        }
      }

      assertTrue(hasGetId, "Should have getId method");
    }

    @Test
    @DisplayName("Should have getConfig method")
    void shouldHaveGetConfigMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "JniResourceLimiter");
      boolean hasGetConfig = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("getConfig".equals(method.getName())) {
          hasGetConfig = true;
          break;
        }
      }

      assertTrue(hasGetConfig, "Should have getConfig method");
    }

    @Test
    @DisplayName("Should have getStats method")
    void shouldHaveGetStatsMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "JniResourceLimiter");
      boolean hasGetStats = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("getStats".equals(method.getName())) {
          hasGetStats = true;
          break;
        }
      }

      assertTrue(hasGetStats, "Should have getStats method");
    }

    @Test
    @DisplayName("Should have private constructor")
    void shouldHavePrivateConstructor() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "JniResourceLimiter");
      boolean hasPrivateConstructor = false;

      for (final java.lang.reflect.Constructor<?> constructor : clazz.getDeclaredConstructors()) {
        if (Modifier.isPrivate(constructor.getModifiers())) {
          hasPrivateConstructor = true;
          break;
        }
      }

      assertTrue(hasPrivateConstructor, "Should have private constructor (factory pattern)");
    }

    @Test
    @DisplayName("Should extend JniResource")
    void shouldExtendJniResource() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "JniResourceLimiter");
      final Class<?> superClass = clazz.getSuperclass();

      assertNotNull(superClass, "Should have a superclass");
      assertEquals("JniResource", superClass.getSimpleName(), "Should extend JniResource");
    }
  }

  @Nested
  @DisplayName("Package Completeness Tests")
  class PackageCompletenessTests {

    @Test
    @DisplayName("All expected execution classes should exist")
    void allExpectedExecutionClassesShouldExist() {
      final String[] expectedClasses = {"JniResourceLimiter"};

      for (final String className : expectedClasses) {
        try {
          final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
          assertNotNull(clazz, className + " should exist");
          LOGGER.fine("Found execution class: " + className);
        } catch (final ClassNotFoundException e) {
          throw new AssertionError("Expected class not found: " + className, e);
        }
      }
    }

    @Test
    @DisplayName("All execution classes should be final")
    void allExecutionClassesShouldBeFinal() throws ClassNotFoundException {
      final String[] executionClasses = {"JniResourceLimiter"};

      for (final String className : executionClasses) {
        final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
        assertTrue(Modifier.isFinal(clazz.getModifiers()), className + " should be final");
      }
    }

    @Test
    @DisplayName("All execution classes should have private constructors")
    void allExecutionClassesShouldHavePrivateConstructors() throws ClassNotFoundException {
      final String[] executionClasses = {"JniResourceLimiter"};

      for (final String className : executionClasses) {
        final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
        boolean hasPrivateConstructor = false;

        for (final java.lang.reflect.Constructor<?> constructor : clazz.getDeclaredConstructors()) {
          if (Modifier.isPrivate(constructor.getModifiers())) {
            hasPrivateConstructor = true;
            break;
          }
        }

        assertTrue(hasPrivateConstructor, className + " should have private constructor");
      }
    }
  }
}
