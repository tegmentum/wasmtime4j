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

package ai.tegmentum.wasmtime4j.jni.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive package-level tests for the JNI factory package.
 *
 * <p>This test class verifies the API contracts, class structure, and method signatures of all
 * factory-related classes in the jni.factory package using reflection-based testing to avoid native
 * library loading.
 *
 * @since 1.0.0
 */
@DisplayName("JNI Factory Package Tests")
public class JniFactoryPackageTest {

  private static final Logger LOGGER = Logger.getLogger(JniFactoryPackageTest.class.getName());

  private static final String PACKAGE_PREFIX = "ai.tegmentum.wasmtime4j.jni.factory.";

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
  @DisplayName("JniRuntimeFactory Tests")
  class JniRuntimeFactoryTests {

    @Test
    @DisplayName("Should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "JniRuntimeFactory");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "JniRuntimeFactory should be final");
    }

    @Test
    @DisplayName("Should have private constructor")
    void shouldHavePrivateConstructor() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "JniRuntimeFactory");
      boolean hasPrivateConstructor = false;

      for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
        if (Modifier.isPrivate(constructor.getModifiers())) {
          hasPrivateConstructor = true;
          break;
        }
      }

      assertTrue(hasPrivateConstructor, "Should have private constructor (utility class pattern)");
    }

    @Test
    @DisplayName("Should have createRuntime static factory method")
    void shouldHaveCreateRuntimeMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "JniRuntimeFactory");
      boolean hasCreateRuntime = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("createRuntime".equals(method.getName()) && Modifier.isStatic(method.getModifiers())) {
          hasCreateRuntime = true;
          assertEquals(0, method.getParameterCount(), "createRuntime should have no parameters");
          break;
        }
      }

      assertTrue(hasCreateRuntime, "Should have static createRuntime method");
    }

    @Test
    @DisplayName("Should have isAvailable static method")
    void shouldHaveIsAvailableMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "JniRuntimeFactory");
      boolean hasIsAvailable = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("isAvailable".equals(method.getName()) && Modifier.isStatic(method.getModifiers())) {
          hasIsAvailable = true;
          assertEquals(boolean.class, method.getReturnType(), "isAvailable should return boolean");
          assertEquals(0, method.getParameterCount(), "isAvailable should have no parameters");
          break;
        }
      }

      assertTrue(hasIsAvailable, "Should have static isAvailable method");
    }

    @Test
    @DisplayName("Should have getImplementationName static method")
    void shouldHaveGetImplementationNameMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "JniRuntimeFactory");
      boolean hasGetImplementationName = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("getImplementationName".equals(method.getName())
            && Modifier.isStatic(method.getModifiers())) {
          hasGetImplementationName = true;
          assertEquals(
              String.class, method.getReturnType(), "getImplementationName should return String");
          break;
        }
      }

      assertTrue(hasGetImplementationName, "Should have static getImplementationName method");
    }

    @Test
    @DisplayName("Should have getWasmtimeVersion static method")
    void shouldHaveGetWasmtimeVersionMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "JniRuntimeFactory");
      boolean hasGetWasmtimeVersion = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("getWasmtimeVersion".equals(method.getName())
            && Modifier.isStatic(method.getModifiers())) {
          hasGetWasmtimeVersion = true;
          assertEquals(
              String.class, method.getReturnType(), "getWasmtimeVersion should return String");
          break;
        }
      }

      assertTrue(hasGetWasmtimeVersion, "Should have static getWasmtimeVersion method");
    }

    @Test
    @DisplayName("Should have getFactoryInfo static method")
    void shouldHaveGetFactoryInfoMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "JniRuntimeFactory");
      boolean hasGetFactoryInfo = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("getFactoryInfo".equals(method.getName()) && Modifier.isStatic(method.getModifiers())) {
          hasGetFactoryInfo = true;
          assertEquals(String.class, method.getReturnType(), "getFactoryInfo should return String");
          break;
        }
      }

      assertTrue(hasGetFactoryInfo, "Should have static getFactoryInfo method");
    }

    @Test
    @DisplayName("Should have validateEnvironment static method")
    void shouldHaveValidateEnvironmentMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "JniRuntimeFactory");
      boolean hasValidateEnvironment = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("validateEnvironment".equals(method.getName())
            && Modifier.isStatic(method.getModifiers())) {
          hasValidateEnvironment = true;
          assertEquals(
              void.class, method.getReturnType(), "validateEnvironment should return void");
          break;
        }
      }

      assertTrue(hasValidateEnvironment, "Should have static validateEnvironment method");
    }

    @Test
    @DisplayName("Should have Java version compatibility methods")
    void shouldHaveJavaVersionMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "JniRuntimeFactory");
      boolean hasMinVersion = false;
      boolean hasMaxVersion = false;
      boolean hasIsCompatible = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        final String methodName = method.getName();
        if ("getMinimumJavaVersion".equals(methodName)
            && Modifier.isStatic(method.getModifiers())) {
          hasMinVersion = true;
          assertEquals(
              String.class, method.getReturnType(), "getMinimumJavaVersion should return String");
        }
        if ("getMaximumJavaVersion".equals(methodName)
            && Modifier.isStatic(method.getModifiers())) {
          hasMaxVersion = true;
          assertEquals(
              String.class, method.getReturnType(), "getMaximumJavaVersion should return String");
        }
        if ("isJavaVersionCompatible".equals(methodName)
            && Modifier.isStatic(method.getModifiers())) {
          hasIsCompatible = true;
          assertEquals(
              boolean.class,
              method.getReturnType(),
              "isJavaVersionCompatible should return boolean");
        }
      }

      assertTrue(hasMinVersion, "Should have static getMinimumJavaVersion method");
      assertTrue(hasMaxVersion, "Should have static getMaximumJavaVersion method");
      assertTrue(hasIsCompatible, "Should have static isJavaVersionCompatible method");
    }
  }

  @Nested
  @DisplayName("Package Completeness Tests")
  class PackageCompletenessTests {

    @Test
    @DisplayName("All expected factory classes should exist")
    void allExpectedFactoryClassesShouldExist() {
      final String[] expectedClasses = {"JniRuntimeFactory"};

      for (final String className : expectedClasses) {
        try {
          final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
          assertNotNull(clazz, className + " should exist");
          LOGGER.fine("Found factory class: " + className);
        } catch (final ClassNotFoundException e) {
          throw new AssertionError("Expected class not found: " + className, e);
        }
      }
    }

    @Test
    @DisplayName("All factory classes should be final")
    void allFactoryClassesShouldBeFinal() throws ClassNotFoundException {
      final String[] factoryClasses = {"JniRuntimeFactory"};

      for (final String className : factoryClasses) {
        final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
        assertTrue(Modifier.isFinal(clazz.getModifiers()), className + " should be final");
      }
    }

    @Test
    @DisplayName("All factory classes should have private constructors")
    void allFactoryClassesShouldHavePrivateConstructors() throws ClassNotFoundException {
      final String[] factoryClasses = {"JniRuntimeFactory"};

      for (final String className : factoryClasses) {
        final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
        boolean hasPrivateConstructor = false;

        for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
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
