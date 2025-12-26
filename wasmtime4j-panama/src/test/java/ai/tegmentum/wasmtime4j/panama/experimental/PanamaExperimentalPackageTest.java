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

package ai.tegmentum.wasmtime4j.panama.experimental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Comprehensive package-level tests for the Panama experimental package.
 *
 * <p>This test class verifies the API contracts, class structure, and method signatures of all
 * experimental classes in the panama.experimental package using reflection-based testing to avoid
 * native library loading.
 *
 * @since 1.0.0
 */
@DisplayName("Panama Experimental Package Tests")
public class PanamaExperimentalPackageTest {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaExperimentalPackageTest.class.getName());

  private static final String PACKAGE_PREFIX = "ai.tegmentum.wasmtime4j.panama.experimental.";

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
  @DisplayName("PanamaExceptionHandler Tests")
  class PanamaExceptionHandlerTests {

    @Test
    @DisplayName("Should be a final class implementing ExceptionHandler")
    void shouldBeFinalAndImplementExceptionHandler() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaExceptionHandler");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "PanamaExceptionHandler should be final");

      // Check that it implements ExceptionHandler interface
      boolean implementsExceptionHandler = false;
      for (final Class<?> iface : clazz.getInterfaces()) {
        if ("ExceptionHandler".equals(iface.getSimpleName())) {
          implementsExceptionHandler = true;
          break;
        }
      }
      assertTrue(
          implementsExceptionHandler, "PanamaExceptionHandler should implement ExceptionHandler");
    }

    @Test
    @DisplayName("Should have state fields")
    void shouldHaveStateFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaExceptionHandler");
      boolean hasTagsByName = false;
      boolean hasTagsByHandle = false;
      boolean hasArena = false;
      boolean hasNativeHandle = false;
      boolean hasConfig = false;
      boolean hasEnabled = false;
      boolean hasClosed = false;

      for (final Field field : clazz.getDeclaredFields()) {
        final String fieldName = field.getName();
        if ("tagsByName".equals(fieldName)) {
          hasTagsByName = true;
        }
        if ("tagsByHandle".equals(fieldName)) {
          hasTagsByHandle = true;
        }
        if ("arena".equals(fieldName)) {
          hasArena = true;
        }
        if ("nativeHandle".equals(fieldName)) {
          hasNativeHandle = true;
        }
        if ("config".equals(fieldName)) {
          hasConfig = true;
        }
        if ("enabled".equals(fieldName)) {
          hasEnabled = true;
        }
        if ("closed".equals(fieldName)) {
          hasClosed = true;
        }
      }

      assertTrue(hasTagsByName, "Should have tagsByName field");
      assertTrue(hasTagsByHandle, "Should have tagsByHandle field");
      assertTrue(hasArena, "Should have arena field");
      assertTrue(hasNativeHandle, "Should have nativeHandle field");
      assertTrue(hasConfig, "Should have config field");
      assertTrue(hasEnabled, "Should have enabled field");
      assertTrue(hasClosed, "Should have closed field");
    }

    @Test
    @DisplayName("Should have static method handle fields")
    void shouldHaveStaticMethodHandleFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaExceptionHandler");
      boolean hasLinker = false;
      boolean hasSymbolLookup = false;
      boolean hasCreateHandler = false;
      boolean hasCreateTag = false;
      boolean hasCaptureStackTrace = false;
      boolean hasPerformUnwinding = false;
      boolean hasCloseHandler = false;
      boolean hasFreeString = false;

      for (final Field field : clazz.getDeclaredFields()) {
        final String fieldName = field.getName();
        final int modifiers = field.getModifiers();

        if ("LINKER".equals(fieldName) && Modifier.isStatic(modifiers)) {
          hasLinker = true;
        }
        if ("SYMBOL_LOOKUP".equals(fieldName) && Modifier.isStatic(modifiers)) {
          hasSymbolLookup = true;
        }
        if ("CREATE_HANDLER".equals(fieldName) && Modifier.isStatic(modifiers)) {
          hasCreateHandler = true;
        }
        if ("CREATE_TAG".equals(fieldName) && Modifier.isStatic(modifiers)) {
          hasCreateTag = true;
        }
        if ("CAPTURE_STACK_TRACE".equals(fieldName) && Modifier.isStatic(modifiers)) {
          hasCaptureStackTrace = true;
        }
        if ("PERFORM_UNWINDING".equals(fieldName) && Modifier.isStatic(modifiers)) {
          hasPerformUnwinding = true;
        }
        if ("CLOSE_HANDLER".equals(fieldName) && Modifier.isStatic(modifiers)) {
          hasCloseHandler = true;
        }
        if ("FREE_STRING".equals(fieldName) && Modifier.isStatic(modifiers)) {
          hasFreeString = true;
        }
      }

      assertTrue(hasLinker, "Should have LINKER field");
      assertTrue(hasSymbolLookup, "Should have SYMBOL_LOOKUP field");
      assertTrue(hasCreateHandler, "Should have CREATE_HANDLER method handle");
      assertTrue(hasCreateTag, "Should have CREATE_TAG method handle");
      assertTrue(hasCaptureStackTrace, "Should have CAPTURE_STACK_TRACE method handle");
      assertTrue(hasPerformUnwinding, "Should have PERFORM_UNWINDING method handle");
      assertTrue(hasCloseHandler, "Should have CLOSE_HANDLER method handle");
      assertTrue(hasFreeString, "Should have FREE_STRING method handle");
    }

    @Test
    @DisplayName("Should have static factory methods")
    void shouldHaveStaticFactoryMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaExceptionHandler");
      int createCount = 0;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("create".equals(method.getName()) && Modifier.isStatic(method.getModifiers())) {
          createCount++;
        }
      }

      assertTrue(
          createCount >= 2,
          "Should have at least 2 create factory methods (found: " + createCount + ")");
    }

    @Test
    @DisplayName("Should have ExceptionHandler interface methods")
    void shouldHaveExceptionHandlerInterfaceMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaExceptionHandler");
      boolean hasHandle = false;
      boolean hasGetHandlerName = false;
      boolean hasIsEnabled = false;
      boolean hasCreateExceptionTag = false;
      boolean hasGetExceptionTag = false;
      boolean hasListExceptionTags = false;
      boolean hasCaptureStackTrace = false;
      boolean hasPerformUnwinding = false;
      boolean hasGetConfig = false;
      boolean hasClose = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        final String methodName = method.getName();
        if ("handle".equals(methodName)) {
          hasHandle = true;
        }
        if ("getHandlerName".equals(methodName)) {
          hasGetHandlerName = true;
          assertEquals(String.class, method.getReturnType(), "getHandlerName should return String");
        }
        if ("isEnabled".equals(methodName)) {
          hasIsEnabled = true;
          assertEquals(boolean.class, method.getReturnType(), "isEnabled should return boolean");
        }
        if ("createExceptionTag".equals(methodName)) {
          hasCreateExceptionTag = true;
        }
        if ("getExceptionTag".equals(methodName)) {
          hasGetExceptionTag = true;
        }
        if ("listExceptionTags".equals(methodName)) {
          hasListExceptionTags = true;
        }
        if ("captureStackTrace".equals(methodName)) {
          hasCaptureStackTrace = true;
        }
        if ("performUnwinding".equals(methodName)) {
          hasPerformUnwinding = true;
          assertEquals(
              boolean.class, method.getReturnType(), "performUnwinding should return boolean");
        }
        if ("getConfig".equals(methodName)) {
          hasGetConfig = true;
        }
        if ("close".equals(methodName)) {
          hasClose = true;
        }
      }

      assertTrue(hasHandle, "Should have handle method");
      assertTrue(hasGetHandlerName, "Should have getHandlerName method");
      assertTrue(hasIsEnabled, "Should have isEnabled method");
      assertTrue(hasCreateExceptionTag, "Should have createExceptionTag method");
      assertTrue(hasGetExceptionTag, "Should have getExceptionTag method");
      assertTrue(hasListExceptionTags, "Should have listExceptionTags method");
      assertTrue(hasCaptureStackTrace, "Should have captureStackTrace method");
      assertTrue(hasPerformUnwinding, "Should have performUnwinding method");
      assertTrue(hasGetConfig, "Should have getConfig method");
      assertTrue(hasClose, "Should have close method");
    }

    @Test
    @DisplayName("Should have additional accessor methods")
    void shouldHaveAdditionalAccessorMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaExceptionHandler");
      boolean hasSetEnabled = false;
      boolean hasGetTagByHandle = false;
      boolean hasGetNativeHandle = false;
      boolean hasIsClosed = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        final String methodName = method.getName();
        if ("setEnabled".equals(methodName)) {
          hasSetEnabled = true;
          assertEquals(void.class, method.getReturnType(), "setEnabled should return void");
          assertEquals(1, method.getParameterCount(), "setEnabled should have 1 parameter");
        }
        if ("getTagByHandle".equals(methodName)) {
          hasGetTagByHandle = true;
        }
        if ("getNativeHandle".equals(methodName)) {
          hasGetNativeHandle = true;
        }
        if ("isClosed".equals(methodName)) {
          hasIsClosed = true;
          assertEquals(boolean.class, method.getReturnType(), "isClosed should return boolean");
        }
      }

      assertTrue(hasSetEnabled, "Should have setEnabled method");
      assertTrue(hasGetTagByHandle, "Should have getTagByHandle method");
      assertTrue(hasGetNativeHandle, "Should have getNativeHandle method");
      assertTrue(hasIsClosed, "Should have isClosed method");
    }

    @Test
    @DisplayName("Should have private constructor")
    void shouldHavePrivateConstructor() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaExceptionHandler");
      boolean hasPrivateConstructor = false;

      for (final var constructor : clazz.getDeclaredConstructors()) {
        if (Modifier.isPrivate(constructor.getModifiers())) {
          hasPrivateConstructor = true;
          break;
        }
      }

      assertTrue(hasPrivateConstructor, "Should have private constructor (factory pattern)");
    }
  }

  @Nested
  @DisplayName("Package Completeness Tests")
  class PackageCompletenessTests {

    @Test
    @DisplayName("All expected experimental classes should exist")
    void allExpectedExperimentalClassesShouldExist() {
      final String[] expectedClasses = {"PanamaExceptionHandler"};

      for (final String className : expectedClasses) {
        try {
          final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
          assertNotNull(clazz, className + " should exist");
          LOGGER.fine("Found experimental class: " + className);
        } catch (final ClassNotFoundException e) {
          throw new AssertionError("Expected class not found: " + className, e);
        }
      }
    }

    @Test
    @DisplayName("All experimental classes should be final")
    void allExperimentalClassesShouldBeFinal() throws ClassNotFoundException {
      final String[] experimentalClasses = {"PanamaExceptionHandler"};

      for (final String className : experimentalClasses) {
        final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
        assertTrue(Modifier.isFinal(clazz.getModifiers()), className + " should be final");
      }
    }

    @Test
    @DisplayName("All experimental classes should not be interfaces")
    void allExperimentalClassesShouldNotBeInterfaces() throws ClassNotFoundException {
      final String[] experimentalClasses = {"PanamaExceptionHandler"};

      for (final String className : experimentalClasses) {
        final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
        assertFalse(clazz.isInterface(), className + " should not be an interface");
      }
    }

    @Test
    @DisplayName("All experimental classes should have close method for resource management")
    void allExperimentalClassesShouldHaveCloseMethod() throws ClassNotFoundException {
      final String[] experimentalClasses = {"PanamaExceptionHandler"};

      for (final String className : experimentalClasses) {
        final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
        boolean hasClose = false;

        for (final Method method : clazz.getDeclaredMethods()) {
          if ("close".equals(method.getName())) {
            hasClose = true;
            break;
          }
        }

        assertTrue(hasClose, className + " should have close method");
      }
    }

    @Test
    @DisplayName("All experimental classes should implement AutoCloseable")
    void allExperimentalClassesShouldImplementAutoCloseable() throws ClassNotFoundException {
      final String[] experimentalClasses = {"PanamaExceptionHandler"};

      for (final String className : experimentalClasses) {
        final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);

        // Check through interface hierarchy
        boolean implementsAutoCloseable = false;
        for (final Class<?> iface : clazz.getInterfaces()) {
          if (AutoCloseable.class.isAssignableFrom(iface)
              || "AutoCloseable".equals(iface.getSimpleName())) {
            implementsAutoCloseable = true;
            break;
          }
          // Check interfaces of the interface
          for (final Class<?> superIface : iface.getInterfaces()) {
            if (AutoCloseable.class.isAssignableFrom(superIface)) {
              implementsAutoCloseable = true;
              break;
            }
          }
        }

        assertTrue(
            implementsAutoCloseable,
            className + " should implement AutoCloseable (directly or through parent interface)");
      }
    }
  }
}
