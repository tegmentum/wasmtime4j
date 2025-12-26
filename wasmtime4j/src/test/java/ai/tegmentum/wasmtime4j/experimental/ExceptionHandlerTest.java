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

package ai.tegmentum.wasmtime4j.experimental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExceptionHandler} interface.
 *
 * <p>ExceptionHandler provides experimental exception handling for WebAssembly components.
 */
@DisplayName("ExceptionHandler Tests")
class ExceptionHandlerTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeInterface() {
      assertTrue(ExceptionHandler.class.isInterface(), "ExceptionHandler should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExceptionHandler.class.getModifiers()),
          "ExceptionHandler should be public");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(ExceptionHandler.class),
          "ExceptionHandler should extend AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have handle method")
    void shouldHaveHandleMethod() throws NoSuchMethodException {
      final Method method = ExceptionHandler.class.getMethod("handle", Throwable.class);
      assertNotNull(method, "handle method should exist");
      assertEquals(
          ExceptionHandler.HandlingResult.class,
          method.getReturnType(),
          "handle should return HandlingResult");
    }

    @Test
    @DisplayName("should have getHandlerName method")
    void shouldHaveGetHandlerNameMethod() throws NoSuchMethodException {
      final Method method = ExceptionHandler.class.getMethod("getHandlerName");
      assertNotNull(method, "getHandlerName method should exist");
      assertEquals(String.class, method.getReturnType(), "getHandlerName should return String");
    }

    @Test
    @DisplayName("should have isEnabled method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      final Method method = ExceptionHandler.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isEnabled should return boolean");
    }

    @Test
    @DisplayName("should have createExceptionTag method")
    void shouldHaveCreateExceptionTagMethod() throws NoSuchMethodException {
      final Method method =
          ExceptionHandler.class.getMethod("createExceptionTag", String.class, List.class);
      assertNotNull(method, "createExceptionTag method should exist");
      assertEquals(
          ExceptionHandler.ExceptionTag.class,
          method.getReturnType(),
          "createExceptionTag should return ExceptionTag");
    }

    @Test
    @DisplayName("should have getExceptionTag method")
    void shouldHaveGetExceptionTagMethod() throws NoSuchMethodException {
      final Method method = ExceptionHandler.class.getMethod("getExceptionTag", String.class);
      assertNotNull(method, "getExceptionTag method should exist");
      assertEquals(
          Optional.class, method.getReturnType(), "getExceptionTag should return Optional");
    }

    @Test
    @DisplayName("should have listExceptionTags method")
    void shouldHaveListExceptionTagsMethod() throws NoSuchMethodException {
      final Method method = ExceptionHandler.class.getMethod("listExceptionTags");
      assertNotNull(method, "listExceptionTags method should exist");
      assertEquals(List.class, method.getReturnType(), "listExceptionTags should return List");
    }

    @Test
    @DisplayName("should have captureStackTrace method")
    void shouldHaveCaptureStackTraceMethod() throws NoSuchMethodException {
      final Method method = ExceptionHandler.class.getMethod("captureStackTrace", long.class);
      assertNotNull(method, "captureStackTrace method should exist");
      assertEquals(String.class, method.getReturnType(), "captureStackTrace should return String");
    }

    @Test
    @DisplayName("should have performUnwinding method")
    void shouldHavePerformUnwindingMethod() throws NoSuchMethodException {
      final Method method = ExceptionHandler.class.getMethod("performUnwinding", int.class);
      assertNotNull(method, "performUnwinding method should exist");
      assertEquals(boolean.class, method.getReturnType(), "performUnwinding should return boolean");
    }

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      final Method method = ExceptionHandler.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(
          ExceptionHandler.ExceptionHandlingConfig.class,
          method.getReturnType(),
          "getConfig should return ExceptionHandlingConfig");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = ExceptionHandler.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }
  }

  @Nested
  @DisplayName("HandlingResult Enum Tests")
  class HandlingResultEnumTests {

    @Test
    @DisplayName("should have HANDLED value")
    void shouldHaveHandledValue() {
      assertNotNull(
          ExceptionHandler.HandlingResult.valueOf("HANDLED"), "Should have HANDLED enum value");
    }

    @Test
    @DisplayName("should have NOT_HANDLED value")
    void shouldHaveNotHandledValue() {
      assertNotNull(
          ExceptionHandler.HandlingResult.valueOf("NOT_HANDLED"),
          "Should have NOT_HANDLED enum value");
    }

    @Test
    @DisplayName("should have FAILED value")
    void shouldHaveFailedValue() {
      assertNotNull(
          ExceptionHandler.HandlingResult.valueOf("FAILED"), "Should have FAILED enum value");
    }

    @Test
    @DisplayName("should have exactly 3 values")
    void shouldHaveExactlyThreeValues() {
      assertEquals(
          3,
          ExceptionHandler.HandlingResult.values().length,
          "HandlingResult should have exactly 3 values");
    }
  }

  @Nested
  @DisplayName("ExceptionTag Interface Tests")
  class ExceptionTagInterfaceTests {

    @Test
    @DisplayName("should have ExceptionTag nested interface")
    void shouldHaveExceptionTagInterface() {
      final Class<?>[] declaredClasses = ExceptionHandler.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if ("ExceptionTag".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have ExceptionTag nested interface");
    }

    @Test
    @DisplayName("ExceptionTag should have getTagHandle method")
    void exceptionTagShouldHaveGetTagHandleMethod() throws NoSuchMethodException {
      final Method method = ExceptionHandler.ExceptionTag.class.getMethod("getTagHandle");
      assertNotNull(method, "getTagHandle method should exist");
      assertEquals(long.class, method.getReturnType(), "getTagHandle should return long");
    }

    @Test
    @DisplayName("ExceptionTag should have getTagName method")
    void exceptionTagShouldHaveGetTagNameMethod() throws NoSuchMethodException {
      final Method method = ExceptionHandler.ExceptionTag.class.getMethod("getTagName");
      assertNotNull(method, "getTagName method should exist");
      assertEquals(String.class, method.getReturnType(), "getTagName should return String");
    }

    @Test
    @DisplayName("ExceptionTag should have getTagType method")
    void exceptionTagShouldHaveGetTagTypeMethod() throws NoSuchMethodException {
      final Method method = ExceptionHandler.ExceptionTag.class.getMethod("getTagType");
      assertNotNull(method, "getTagType method should exist");
      assertEquals(String.class, method.getReturnType(), "getTagType should return String");
    }

    @Test
    @DisplayName("ExceptionTag should have getParameterTypes method")
    void exceptionTagShouldHaveGetParameterTypesMethod() throws NoSuchMethodException {
      final Method method = ExceptionHandler.ExceptionTag.class.getMethod("getParameterTypes");
      assertNotNull(method, "getParameterTypes method should exist");
      assertEquals(List.class, method.getReturnType(), "getParameterTypes should return List");
    }

    @Test
    @DisplayName("ExceptionTag should have isGcAware method")
    void exceptionTagShouldHaveIsGcAwareMethod() throws NoSuchMethodException {
      final Method method = ExceptionHandler.ExceptionTag.class.getMethod("isGcAware");
      assertNotNull(method, "isGcAware method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isGcAware should return boolean");
    }
  }

  @Nested
  @DisplayName("ExceptionHandlingConfig Interface Tests")
  class ExceptionHandlingConfigInterfaceTests {

    @Test
    @DisplayName("should have ExceptionHandlingConfig nested interface")
    void shouldHaveExceptionHandlingConfigInterface() {
      final Class<?>[] declaredClasses = ExceptionHandler.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if ("ExceptionHandlingConfig".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have ExceptionHandlingConfig nested interface");
    }

    @Test
    @DisplayName("ExceptionHandlingConfig should have isNestedTryCatchEnabled method")
    void shouldHaveIsNestedTryCatchEnabledMethod() throws NoSuchMethodException {
      final Method method =
          ExceptionHandler.ExceptionHandlingConfig.class.getMethod("isNestedTryCatchEnabled");
      assertNotNull(method, "isNestedTryCatchEnabled method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isNestedTryCatchEnabled should return boolean");
    }

    @Test
    @DisplayName("ExceptionHandlingConfig should have isExceptionUnwindingEnabled method")
    void shouldHaveIsExceptionUnwindingEnabledMethod() throws NoSuchMethodException {
      final Method method =
          ExceptionHandler.ExceptionHandlingConfig.class.getMethod("isExceptionUnwindingEnabled");
      assertNotNull(method, "isExceptionUnwindingEnabled method should exist");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isExceptionUnwindingEnabled should return boolean");
    }

    @Test
    @DisplayName("ExceptionHandlingConfig should have getMaxUnwindDepth method")
    void shouldHaveGetMaxUnwindDepthMethod() throws NoSuchMethodException {
      final Method method =
          ExceptionHandler.ExceptionHandlingConfig.class.getMethod("getMaxUnwindDepth");
      assertNotNull(method, "getMaxUnwindDepth method should exist");
      assertEquals(int.class, method.getReturnType(), "getMaxUnwindDepth should return int");
    }

    @Test
    @DisplayName("ExceptionHandlingConfig should have isExceptionTypeValidationEnabled method")
    void shouldHaveIsExceptionTypeValidationEnabledMethod() throws NoSuchMethodException {
      final Method method =
          ExceptionHandler.ExceptionHandlingConfig.class.getMethod(
              "isExceptionTypeValidationEnabled");
      assertNotNull(method, "isExceptionTypeValidationEnabled method should exist");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isExceptionTypeValidationEnabled should return boolean");
    }

    @Test
    @DisplayName("ExceptionHandlingConfig should have isStackTracesEnabled method")
    void shouldHaveIsStackTracesEnabledMethod() throws NoSuchMethodException {
      final Method method =
          ExceptionHandler.ExceptionHandlingConfig.class.getMethod("isStackTracesEnabled");
      assertNotNull(method, "isStackTracesEnabled method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isStackTracesEnabled should return boolean");
    }

    @Test
    @DisplayName("ExceptionHandlingConfig should have isExceptionPropagationEnabled method")
    void shouldHaveIsExceptionPropagationEnabledMethod() throws NoSuchMethodException {
      final Method method =
          ExceptionHandler.ExceptionHandlingConfig.class.getMethod("isExceptionPropagationEnabled");
      assertNotNull(method, "isExceptionPropagationEnabled method should exist");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isExceptionPropagationEnabled should return boolean");
    }

    @Test
    @DisplayName("ExceptionHandlingConfig should have isGcIntegrationEnabled method")
    void shouldHaveIsGcIntegrationEnabledMethod() throws NoSuchMethodException {
      final Method method =
          ExceptionHandler.ExceptionHandlingConfig.class.getMethod("isGcIntegrationEnabled");
      assertNotNull(method, "isGcIntegrationEnabled method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isGcIntegrationEnabled should return boolean");
    }
  }

  @Nested
  @DisplayName("ExceptionHandlingConfig Builder Tests")
  class ExceptionHandlingConfigBuilderTests {

    @Test
    @DisplayName("should have Builder nested interface")
    void shouldHaveBuilderInterface() {
      final Class<?>[] declaredClasses =
          ExceptionHandler.ExceptionHandlingConfig.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if ("Builder".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have Builder nested interface in ExceptionHandlingConfig");
    }

    @Test
    @DisplayName("Builder should have nestedTryCatch method")
    void builderShouldHaveNestedTryCatchMethod() throws NoSuchMethodException {
      final Method method =
          ExceptionHandler.ExceptionHandlingConfig.Builder.class.getMethod(
              "nestedTryCatch", boolean.class);
      assertNotNull(method, "nestedTryCatch method should exist");
      assertEquals(
          ExceptionHandler.ExceptionHandlingConfig.Builder.class,
          method.getReturnType(),
          "nestedTryCatch should return Builder");
    }

    @Test
    @DisplayName("Builder should have build method")
    void builderShouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method =
          ExceptionHandler.ExceptionHandlingConfig.Builder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(
          ExceptionHandler.ExceptionHandlingConfig.class,
          method.getReturnType(),
          "build should return ExceptionHandlingConfig");
    }
  }
}
