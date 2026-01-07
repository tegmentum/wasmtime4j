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

import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link JniComponent} and its inner classes.
 *
 * <p>Tests class structure, field definitions, method signatures, and access modifiers for
 * JniComponent and its inner classes: JniComponentEngine, JniComponentHandle, and
 * JniComponentInstanceHandle.
 */
@DisplayName("JniComponent Tests")
class JniComponentTest {

  @Nested
  @DisplayName("JniComponent Class Structure Tests")
  class JniComponentClassStructureTests {

    @Test
    @DisplayName("JniComponent should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isFinal(JniComponent.class.getModifiers()), "JniComponent should be final");
      assertTrue(
          Modifier.isPublic(JniComponent.class.getModifiers()), "JniComponent should be public");
    }

    @Test
    @DisplayName("JniComponent should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = JniComponent.class.getDeclaredConstructor();
      assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");
    }

    @Test
    @DisplayName("JniComponent should have createComponentEngine static factory method")
    void shouldHaveCreateComponentEngineMethod() throws NoSuchMethodException {
      Method method = JniComponent.class.getMethod("createComponentEngine");
      assertNotNull(method, "createComponentEngine method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "createComponentEngine should be static");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "createComponentEngine should be public");
      assertEquals(
          JniComponent.JniComponentEngine.class,
          method.getReturnType(),
          "Should return JniComponentEngine");
    }

    @Test
    @DisplayName("JniComponent should have LOGGER field")
    void shouldHaveLoggerField() throws NoSuchFieldException {
      Field field = JniComponent.class.getDeclaredField("LOGGER");
      assertNotNull(field, "LOGGER field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "LOGGER should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
    }

    @Test
    @DisplayName("JniComponent should have three public inner classes")
    void shouldHaveThreePublicInnerClasses() {
      Class<?>[] declaredClasses = JniComponent.class.getDeclaredClasses();
      int publicCount = 0;
      for (Class<?> innerClass : declaredClasses) {
        if (Modifier.isPublic(innerClass.getModifiers())) {
          publicCount++;
        }
      }
      assertEquals(3, publicCount, "Should have 3 public inner classes");
    }
  }

  @Nested
  @DisplayName("JniComponentEngine Inner Class Tests")
  class JniComponentEngineTests {

    @Test
    @DisplayName("JniComponentEngine should be public static final inner class")
    void shouldBePublicStaticFinalInnerClass() {
      assertTrue(
          Modifier.isPublic(JniComponent.JniComponentEngine.class.getModifiers()),
          "JniComponentEngine should be public");
      assertTrue(
          Modifier.isStatic(JniComponent.JniComponentEngine.class.getModifiers()),
          "JniComponentEngine should be static");
      assertTrue(
          Modifier.isFinal(JniComponent.JniComponentEngine.class.getModifiers()),
          "JniComponentEngine should be final");
    }

    @Test
    @DisplayName("JniComponentEngine should extend JniResource")
    void shouldExtendJniResource() {
      assertTrue(
          JniResource.class.isAssignableFrom(JniComponent.JniComponentEngine.class),
          "JniComponentEngine should extend JniResource");
    }

    @Test
    @DisplayName("JniComponentEngine should have package-private constructor")
    void shouldHavePackagePrivateConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          JniComponent.JniComponentEngine.class.getDeclaredConstructor(long.class);
      assertNotNull(constructor, "Constructor with long parameter should exist");
      int modifiers = constructor.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "Constructor should be package-private");
    }

    @Test
    @DisplayName("JniComponentEngine should have loadComponentFromBytes method")
    void shouldHaveLoadComponentFromBytesMethod() throws NoSuchMethodException {
      Method method =
          JniComponent.JniComponentEngine.class.getMethod("loadComponentFromBytes", byte[].class);
      assertNotNull(method, "loadComponentFromBytes method should exist");
      assertEquals(
          JniComponent.JniComponentHandle.class,
          method.getReturnType(),
          "Should return JniComponentHandle");
    }

    @Test
    @DisplayName("JniComponentEngine should have instantiateComponent method")
    void shouldHaveInstantiateComponentMethod() throws NoSuchMethodException {
      Method method =
          JniComponent.JniComponentEngine.class.getMethod(
              "instantiateComponent", JniComponent.JniComponentHandle.class);
      assertNotNull(method, "instantiateComponent method should exist");
      assertEquals(
          JniComponent.JniComponentInstanceHandle.class,
          method.getReturnType(),
          "Should return JniComponentInstanceHandle");
    }

    @Test
    @DisplayName("JniComponentEngine should have getActiveInstancesCount method")
    void shouldHaveGetActiveInstancesCountMethod() throws NoSuchMethodException {
      Method method = JniComponent.JniComponentEngine.class.getMethod("getActiveInstancesCount");
      assertNotNull(method, "getActiveInstancesCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("JniComponentEngine should have cleanupInstances method")
    void shouldHaveCleanupInstancesMethod() throws NoSuchMethodException {
      Method method = JniComponent.JniComponentEngine.class.getMethod("cleanupInstances");
      assertNotNull(method, "cleanupInstances method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("JniComponentEngine should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = JniComponent.JniComponentEngine.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("JniComponentEngine should override doClose method")
    void shouldOverrideDoCloseMethod() throws NoSuchMethodException {
      Method method = JniComponent.JniComponentEngine.class.getDeclaredMethod("doClose");
      assertNotNull(method, "doClose method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isProtected(method.getModifiers()), "doClose should be protected");
    }

    @Test
    @DisplayName("JniComponentEngine should override getResourceType method")
    void shouldOverrideGetResourceTypeMethod() throws NoSuchMethodException {
      Method method = JniComponent.JniComponentEngine.class.getDeclaredMethod("getResourceType");
      assertNotNull(method, "getResourceType method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertTrue(
          Modifier.isProtected(method.getModifiers()), "getResourceType should be protected");
    }
  }

  @Nested
  @DisplayName("JniComponentHandle Inner Class Tests")
  class JniComponentHandleTests {

    @Test
    @DisplayName("JniComponentHandle should be public static final inner class")
    void shouldBePublicStaticFinalInnerClass() {
      assertTrue(
          Modifier.isPublic(JniComponent.JniComponentHandle.class.getModifiers()),
          "JniComponentHandle should be public");
      assertTrue(
          Modifier.isStatic(JniComponent.JniComponentHandle.class.getModifiers()),
          "JniComponentHandle should be static");
      assertTrue(
          Modifier.isFinal(JniComponent.JniComponentHandle.class.getModifiers()),
          "JniComponentHandle should be final");
    }

    @Test
    @DisplayName("JniComponentHandle should extend JniResource")
    void shouldExtendJniResource() {
      assertTrue(
          JniResource.class.isAssignableFrom(JniComponent.JniComponentHandle.class),
          "JniComponentHandle should extend JniResource");
    }

    @Test
    @DisplayName("JniComponentHandle should have package-private constructor")
    void shouldHavePackagePrivateConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          JniComponent.JniComponentHandle.class.getDeclaredConstructor(long.class);
      assertNotNull(constructor, "Constructor with long parameter should exist");
      int modifiers = constructor.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "Constructor should be package-private");
    }

    @Test
    @DisplayName("JniComponentHandle should have getSize method")
    void shouldHaveGetSizeMethod() throws NoSuchMethodException {
      Method method = JniComponent.JniComponentHandle.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("JniComponentHandle should have exportsInterface method")
    void shouldHaveExportsInterfaceMethod() throws NoSuchMethodException {
      Method method =
          JniComponent.JniComponentHandle.class.getMethod("exportsInterface", String.class);
      assertNotNull(method, "exportsInterface method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("JniComponentHandle should have importsInterface method")
    void shouldHaveImportsInterfaceMethod() throws NoSuchMethodException {
      Method method =
          JniComponent.JniComponentHandle.class.getMethod("importsInterface", String.class);
      assertNotNull(method, "importsInterface method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("JniComponentHandle should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = JniComponent.JniComponentHandle.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("JniComponentHandle should override doClose method")
    void shouldOverrideDoCloseMethod() throws NoSuchMethodException {
      Method method = JniComponent.JniComponentHandle.class.getDeclaredMethod("doClose");
      assertNotNull(method, "doClose method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("JniComponentHandle should override getResourceType method")
    void shouldOverrideGetResourceTypeMethod() throws NoSuchMethodException {
      Method method = JniComponent.JniComponentHandle.class.getDeclaredMethod("getResourceType");
      assertNotNull(method, "getResourceType method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("JniComponentInstanceHandle Inner Class Tests")
  class JniComponentInstanceHandleTests {

    @Test
    @DisplayName("JniComponentInstanceHandle should be public static final inner class")
    void shouldBePublicStaticFinalInnerClass() {
      assertTrue(
          Modifier.isPublic(JniComponent.JniComponentInstanceHandle.class.getModifiers()),
          "JniComponentInstanceHandle should be public");
      assertTrue(
          Modifier.isStatic(JniComponent.JniComponentInstanceHandle.class.getModifiers()),
          "JniComponentInstanceHandle should be static");
      assertTrue(
          Modifier.isFinal(JniComponent.JniComponentInstanceHandle.class.getModifiers()),
          "JniComponentInstanceHandle should be final");
    }

    @Test
    @DisplayName("JniComponentInstanceHandle should extend JniResource")
    void shouldExtendJniResource() {
      assertTrue(
          JniResource.class.isAssignableFrom(JniComponent.JniComponentInstanceHandle.class),
          "JniComponentInstanceHandle should extend JniResource");
    }

    @Test
    @DisplayName("JniComponentInstanceHandle should have package-private constructor")
    void shouldHavePackagePrivateConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          JniComponent.JniComponentInstanceHandle.class.getDeclaredConstructor(long.class);
      assertNotNull(constructor, "Constructor with long parameter should exist");
      int modifiers = constructor.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "Constructor should be package-private");
    }

    @Test
    @DisplayName("JniComponentInstanceHandle should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = JniComponent.JniComponentInstanceHandle.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("JniComponentInstanceHandle should override doClose method")
    void shouldOverrideDoCloseMethod() throws NoSuchMethodException {
      Method method = JniComponent.JniComponentInstanceHandle.class.getDeclaredMethod("doClose");
      assertNotNull(method, "doClose method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("JniComponentInstanceHandle should override getResourceType method")
    void shouldOverrideGetResourceTypeMethod() throws NoSuchMethodException {
      Method method =
          JniComponent.JniComponentInstanceHandle.class.getDeclaredMethod("getResourceType");
      assertNotNull(method, "getResourceType method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Native Method Declarations Tests")
  class NativeMethodDeclarationsTests {

    @Test
    @DisplayName("JniComponent should have nativeCreateComponentEngine method")
    void shouldHaveNativeCreateComponentEngineMethod() throws NoSuchMethodException {
      Method method = JniComponent.class.getDeclaredMethod("nativeCreateComponentEngine");
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("JniComponent should have nativeLoadComponentFromBytes method")
    void shouldHaveNativeLoadComponentFromBytesMethod() throws NoSuchMethodException {
      Method method =
          JniComponent.class.getDeclaredMethod(
              "nativeLoadComponentFromBytes", long.class, byte[].class);
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("JniComponent should have nativeInstantiateComponent method")
    void shouldHaveNativeInstantiateComponentMethod() throws NoSuchMethodException {
      Method method =
          JniComponent.class.getDeclaredMethod(
              "nativeInstantiateComponent", long.class, long.class);
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("JniComponent should have nativeDestroyComponentEngine method")
    void shouldHaveNativeDestroyComponentEngineMethod() throws NoSuchMethodException {
      Method method =
          JniComponent.class.getDeclaredMethod("nativeDestroyComponentEngine", long.class);
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("JniComponent should have nativeDestroyComponent method")
    void shouldHaveNativeDestroyComponentMethod() throws NoSuchMethodException {
      Method method = JniComponent.class.getDeclaredMethod("nativeDestroyComponent", long.class);
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("JniComponent should have nativeDestroyComponentInstance method")
    void shouldHaveNativeDestroyComponentInstanceMethod() throws NoSuchMethodException {
      Method method =
          JniComponent.class.getDeclaredMethod(
              "nativeDestroyComponentInstance", long.class, long.class);
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("JniComponent should have nativeGetComponentSize method")
    void shouldHaveNativeGetComponentSizeMethod() throws NoSuchMethodException {
      Method method = JniComponent.class.getDeclaredMethod("nativeGetComponentSize", long.class);
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("JniComponent should have nativeExportsInterface method")
    void shouldHaveNativeExportsInterfaceMethod() throws NoSuchMethodException {
      Method method =
          JniComponent.class.getDeclaredMethod("nativeExportsInterface", long.class, String.class);
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("JniComponent should have nativeImportsInterface method")
    void shouldHaveNativeImportsInterfaceMethod() throws NoSuchMethodException {
      Method method =
          JniComponent.class.getDeclaredMethod("nativeImportsInterface", long.class, String.class);
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("JniComponent should have nativeGetActiveInstancesCount method")
    void shouldHaveNativeGetActiveInstancesCountMethod() throws NoSuchMethodException {
      Method method =
          JniComponent.class.getDeclaredMethod("nativeGetActiveInstancesCount", long.class);
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("JniComponent should have nativeCleanupInstances method")
    void shouldHaveNativeCleanupInstancesMethod() throws NoSuchMethodException {
      Method method = JniComponent.class.getDeclaredMethod("nativeCleanupInstances", long.class);
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("JniComponent should have nativeComponentInvokeFunction method")
    void shouldHaveNativeComponentInvokeFunctionMethod() throws NoSuchMethodException {
      Method method =
          JniComponent.class.getDeclaredMethod(
              "nativeComponentInvokeFunction",
              long.class,
              long.class,
              String.class,
              int[].class,
              byte[][].class);
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(Object[].class, method.getReturnType(), "Should return Object[]");
    }
  }

  @Nested
  @DisplayName("Package Location Tests")
  class PackageLocationTests {

    @Test
    @DisplayName("JniComponent should be in correct package")
    void shouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni",
          JniComponent.class.getPackage().getName(),
          "Should be in jni package");
    }

    @Test
    @DisplayName("JniComponentEngine should be in correct enclosing class")
    void engineShouldHaveCorrectEnclosingClass() {
      assertEquals(
          JniComponent.class,
          JniComponent.JniComponentEngine.class.getEnclosingClass(),
          "JniComponentEngine should be enclosed by JniComponent");
    }

    @Test
    @DisplayName("JniComponentHandle should be in correct enclosing class")
    void handleShouldHaveCorrectEnclosingClass() {
      assertEquals(
          JniComponent.class,
          JniComponent.JniComponentHandle.class.getEnclosingClass(),
          "JniComponentHandle should be enclosed by JniComponent");
    }

    @Test
    @DisplayName("JniComponentInstanceHandle should be in correct enclosing class")
    void instanceHandleShouldHaveCorrectEnclosingClass() {
      assertEquals(
          JniComponent.class,
          JniComponent.JniComponentInstanceHandle.class.getEnclosingClass(),
          "JniComponentInstanceHandle should be enclosed by JniComponent");
    }
  }

  @Nested
  @DisplayName("AutoCloseable Support Tests")
  class AutoCloseableSupportTests {

    @Test
    @DisplayName("JniComponentEngine should implement AutoCloseable via JniResource")
    void engineShouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(JniComponent.JniComponentEngine.class),
          "JniComponentEngine should implement AutoCloseable");
    }

    @Test
    @DisplayName("JniComponentHandle should implement AutoCloseable via JniResource")
    void handleShouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(JniComponent.JniComponentHandle.class),
          "JniComponentHandle should implement AutoCloseable");
    }

    @Test
    @DisplayName("JniComponentInstanceHandle should implement AutoCloseable via JniResource")
    void instanceHandleShouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(JniComponent.JniComponentInstanceHandle.class),
          "JniComponentInstanceHandle should implement AutoCloseable");
    }
  }
}
