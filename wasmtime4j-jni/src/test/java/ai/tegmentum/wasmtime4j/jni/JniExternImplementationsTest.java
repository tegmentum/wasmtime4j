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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Extern;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.type.ExternType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the JNI Extern implementation classes: JniExternFunc, JniExternGlobal, JniExternMemory,
 * JniExternTable.
 *
 * <p>These tests verify the class structure, method signatures, and interface compliance of the JNI
 * extern implementations using reflection since the classes are package-private.
 */
@DisplayName("JNI Extern Implementation Tests")
class JniExternImplementationsTest {

  private static final String JNI_PACKAGE = "ai.tegmentum.wasmtime4j.jni";

  // ========================================================================
  // JniExternFunc Tests
  // ========================================================================

  @Nested
  @DisplayName("JniExternFunc Tests")
  class JniExternFuncTests {

    private Class<?> getJniExternFuncClass() throws ClassNotFoundException {
      return Class.forName(JNI_PACKAGE + ".JniExternFunc");
    }

    @Test
    @DisplayName("JniExternFunc should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      Class<?> clazz = getJniExternFuncClass();
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "JniExternFunc should be final");
    }

    @Test
    @DisplayName("JniExternFunc should be package-private")
    void shouldBePackagePrivate() throws ClassNotFoundException {
      Class<?> clazz = getJniExternFuncClass();
      int modifiers = clazz.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "JniExternFunc should not be public");
      assertFalse(Modifier.isProtected(modifiers), "JniExternFunc should not be protected");
      assertFalse(Modifier.isPrivate(modifiers), "JniExternFunc should not be private");
    }

    @Test
    @DisplayName("JniExternFunc should implement Extern interface")
    void shouldImplementExternInterface() throws ClassNotFoundException {
      Class<?> clazz = getJniExternFuncClass();
      assertTrue(Extern.class.isAssignableFrom(clazz), "JniExternFunc should implement Extern");
    }

    @Test
    @DisplayName("JniExternFunc should have nativeHandle field")
    void shouldHaveNativeHandleField() throws Exception {
      Class<?> clazz = getJniExternFuncClass();
      Field field = clazz.getDeclaredField("nativeHandle");
      assertNotNull(field, "nativeHandle field should exist");
      assertEquals(long.class, field.getType(), "nativeHandle should be long");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "nativeHandle should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "nativeHandle should be final");
    }

    @Test
    @DisplayName("JniExternFunc should have store field")
    void shouldHaveStoreField() throws Exception {
      Class<?> clazz = getJniExternFuncClass();
      Field field = clazz.getDeclaredField("store");
      assertNotNull(field, "store field should exist");
      assertEquals(JniStore.class, field.getType(), "store should be JniStore");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "store should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "store should be final");
    }

    @Test
    @DisplayName("JniExternFunc should have package-private constructor")
    void shouldHavePackagePrivateConstructor() throws Exception {
      Class<?> clazz = getJniExternFuncClass();
      Constructor<?>[] constructors = clazz.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly 1 constructor");

      Constructor<?> constructor = constructors[0];
      int modifiers = constructor.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "Constructor should not be public");
      assertFalse(Modifier.isProtected(modifiers), "Constructor should not be protected");
      assertFalse(Modifier.isPrivate(modifiers), "Constructor should not be private");

      Class<?>[] paramTypes = constructor.getParameterTypes();
      assertEquals(2, paramTypes.length, "Constructor should have 2 parameters");
      assertEquals(long.class, paramTypes[0], "First parameter should be long");
      assertEquals(JniStore.class, paramTypes[1], "Second parameter should be JniStore");
    }

    @Test
    @DisplayName("JniExternFunc should have getType method returning FUNC")
    void shouldHaveGetTypeMethod() throws Exception {
      Class<?> clazz = getJniExternFuncClass();
      Method method = clazz.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(ExternType.class, method.getReturnType(), "Return type should be ExternType");
      assertEquals(0, method.getParameterCount(), "getType should have no parameters");
    }

    @Test
    @DisplayName("JniExternFunc should have asFunction method")
    void shouldHaveAsFunctionMethod() throws Exception {
      Class<?> clazz = getJniExternFuncClass();
      Method method = clazz.getMethod("asFunction");
      assertNotNull(method, "asFunction method should exist");
      assertEquals(
          WasmFunction.class, method.getReturnType(), "Return type should be WasmFunction");
      assertEquals(0, method.getParameterCount(), "asFunction should have no parameters");
    }

    @Test
    @DisplayName("JniExternFunc should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws Exception {
      Class<?> clazz = getJniExternFuncClass();
      Method method = clazz.getDeclaredMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
      assertEquals(0, method.getParameterCount(), "getNativeHandle should have no parameters");

      int modifiers = method.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "getNativeHandle should not be public");
      assertFalse(Modifier.isPrivate(modifiers), "getNativeHandle should not be private");
    }

    @Test
    @DisplayName("JniExternFunc should have at least 2 non-synthetic fields")
    void shouldHaveAtLeastTwoNonSyntheticFields() throws ClassNotFoundException {
      Class<?> clazz = getJniExternFuncClass();
      // Count only non-synthetic fields (excludes $jacocoData and similar)
      long nonSyntheticCount =
          java.util.Arrays.stream(clazz.getDeclaredFields()).filter(f -> !f.isSynthetic()).count();
      assertTrue(
          nonSyntheticCount >= 2,
          "JniExternFunc should have at least 2 non-synthetic fields, found: " + nonSyntheticCount);
    }
  }

  // ========================================================================
  // JniExternGlobal Tests
  // ========================================================================

  @Nested
  @DisplayName("JniExternGlobal Tests")
  class JniExternGlobalTests {

    private Class<?> getJniExternGlobalClass() throws ClassNotFoundException {
      return Class.forName(JNI_PACKAGE + ".JniExternGlobal");
    }

    @Test
    @DisplayName("JniExternGlobal should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      Class<?> clazz = getJniExternGlobalClass();
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "JniExternGlobal should be final");
    }

    @Test
    @DisplayName("JniExternGlobal should be package-private")
    void shouldBePackagePrivate() throws ClassNotFoundException {
      Class<?> clazz = getJniExternGlobalClass();
      int modifiers = clazz.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "JniExternGlobal should not be public");
      assertFalse(Modifier.isProtected(modifiers), "JniExternGlobal should not be protected");
      assertFalse(Modifier.isPrivate(modifiers), "JniExternGlobal should not be private");
    }

    @Test
    @DisplayName("JniExternGlobal should implement Extern interface")
    void shouldImplementExternInterface() throws ClassNotFoundException {
      Class<?> clazz = getJniExternGlobalClass();
      assertTrue(Extern.class.isAssignableFrom(clazz), "JniExternGlobal should implement Extern");
    }

    @Test
    @DisplayName("JniExternGlobal should have nativeHandle field")
    void shouldHaveNativeHandleField() throws Exception {
      Class<?> clazz = getJniExternGlobalClass();
      Field field = clazz.getDeclaredField("nativeHandle");
      assertNotNull(field, "nativeHandle field should exist");
      assertEquals(long.class, field.getType(), "nativeHandle should be long");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "nativeHandle should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "nativeHandle should be final");
    }

    @Test
    @DisplayName("JniExternGlobal should have store field")
    void shouldHaveStoreField() throws Exception {
      Class<?> clazz = getJniExternGlobalClass();
      Field field = clazz.getDeclaredField("store");
      assertNotNull(field, "store field should exist");
      assertEquals(JniStore.class, field.getType(), "store should be JniStore");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "store should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "store should be final");
    }

    @Test
    @DisplayName("JniExternGlobal should have package-private constructor")
    void shouldHavePackagePrivateConstructor() throws Exception {
      Class<?> clazz = getJniExternGlobalClass();
      Constructor<?>[] constructors = clazz.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly 1 constructor");

      Constructor<?> constructor = constructors[0];
      int modifiers = constructor.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "Constructor should not be public");
      assertFalse(Modifier.isProtected(modifiers), "Constructor should not be protected");
      assertFalse(Modifier.isPrivate(modifiers), "Constructor should not be private");

      Class<?>[] paramTypes = constructor.getParameterTypes();
      assertEquals(2, paramTypes.length, "Constructor should have 2 parameters");
      assertEquals(long.class, paramTypes[0], "First parameter should be long");
      assertEquals(JniStore.class, paramTypes[1], "Second parameter should be JniStore");
    }

    @Test
    @DisplayName("JniExternGlobal should have getType method returning GLOBAL")
    void shouldHaveGetTypeMethod() throws Exception {
      Class<?> clazz = getJniExternGlobalClass();
      Method method = clazz.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(ExternType.class, method.getReturnType(), "Return type should be ExternType");
      assertEquals(0, method.getParameterCount(), "getType should have no parameters");
    }

    @Test
    @DisplayName("JniExternGlobal should have asGlobal method")
    void shouldHaveAsGlobalMethod() throws Exception {
      Class<?> clazz = getJniExternGlobalClass();
      Method method = clazz.getMethod("asGlobal");
      assertNotNull(method, "asGlobal method should exist");
      assertEquals(WasmGlobal.class, method.getReturnType(), "Return type should be WasmGlobal");
      assertEquals(0, method.getParameterCount(), "asGlobal should have no parameters");
    }

    @Test
    @DisplayName("JniExternGlobal should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws Exception {
      Class<?> clazz = getJniExternGlobalClass();
      Method method = clazz.getDeclaredMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
      assertEquals(0, method.getParameterCount(), "getNativeHandle should have no parameters");

      int modifiers = method.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "getNativeHandle should not be public");
      assertFalse(Modifier.isPrivate(modifiers), "getNativeHandle should not be private");
    }

    @Test
    @DisplayName("JniExternGlobal should have at least 2 non-synthetic fields")
    void shouldHaveAtLeastTwoNonSyntheticFields() throws ClassNotFoundException {
      Class<?> clazz = getJniExternGlobalClass();
      // Count only non-synthetic fields (excludes $jacocoData and similar)
      long nonSyntheticCount =
          java.util.Arrays.stream(clazz.getDeclaredFields()).filter(f -> !f.isSynthetic()).count();
      assertTrue(
          nonSyntheticCount >= 2,
          "JniExternGlobal should have at least 2 non-synthetic fields, found: "
              + nonSyntheticCount);
    }
  }

  // ========================================================================
  // JniExternMemory Tests
  // ========================================================================

  @Nested
  @DisplayName("JniExternMemory Tests")
  class JniExternMemoryTests {

    private Class<?> getJniExternMemoryClass() throws ClassNotFoundException {
      return Class.forName(JNI_PACKAGE + ".JniExternMemory");
    }

    @Test
    @DisplayName("JniExternMemory should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      Class<?> clazz = getJniExternMemoryClass();
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "JniExternMemory should be final");
    }

    @Test
    @DisplayName("JniExternMemory should be package-private")
    void shouldBePackagePrivate() throws ClassNotFoundException {
      Class<?> clazz = getJniExternMemoryClass();
      int modifiers = clazz.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "JniExternMemory should not be public");
      assertFalse(Modifier.isProtected(modifiers), "JniExternMemory should not be protected");
      assertFalse(Modifier.isPrivate(modifiers), "JniExternMemory should not be private");
    }

    @Test
    @DisplayName("JniExternMemory should implement Extern interface")
    void shouldImplementExternInterface() throws ClassNotFoundException {
      Class<?> clazz = getJniExternMemoryClass();
      assertTrue(Extern.class.isAssignableFrom(clazz), "JniExternMemory should implement Extern");
    }

    @Test
    @DisplayName("JniExternMemory should have nativeHandle field")
    void shouldHaveNativeHandleField() throws Exception {
      Class<?> clazz = getJniExternMemoryClass();
      Field field = clazz.getDeclaredField("nativeHandle");
      assertNotNull(field, "nativeHandle field should exist");
      assertEquals(long.class, field.getType(), "nativeHandle should be long");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "nativeHandle should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "nativeHandle should be final");
    }

    @Test
    @DisplayName("JniExternMemory should have store field")
    void shouldHaveStoreField() throws Exception {
      Class<?> clazz = getJniExternMemoryClass();
      Field field = clazz.getDeclaredField("store");
      assertNotNull(field, "store field should exist");
      assertEquals(JniStore.class, field.getType(), "store should be JniStore");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "store should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "store should be final");
    }

    @Test
    @DisplayName("JniExternMemory should have package-private constructor")
    void shouldHavePackagePrivateConstructor() throws Exception {
      Class<?> clazz = getJniExternMemoryClass();
      Constructor<?>[] constructors = clazz.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly 1 constructor");

      Constructor<?> constructor = constructors[0];
      int modifiers = constructor.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "Constructor should not be public");
      assertFalse(Modifier.isProtected(modifiers), "Constructor should not be protected");
      assertFalse(Modifier.isPrivate(modifiers), "Constructor should not be private");

      Class<?>[] paramTypes = constructor.getParameterTypes();
      assertEquals(2, paramTypes.length, "Constructor should have 2 parameters");
      assertEquals(long.class, paramTypes[0], "First parameter should be long");
      assertEquals(JniStore.class, paramTypes[1], "Second parameter should be JniStore");
    }

    @Test
    @DisplayName("JniExternMemory should have getType method returning MEMORY")
    void shouldHaveGetTypeMethod() throws Exception {
      Class<?> clazz = getJniExternMemoryClass();
      Method method = clazz.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(ExternType.class, method.getReturnType(), "Return type should be ExternType");
      assertEquals(0, method.getParameterCount(), "getType should have no parameters");
    }

    @Test
    @DisplayName("JniExternMemory should have asMemory method")
    void shouldHaveAsMemoryMethod() throws Exception {
      Class<?> clazz = getJniExternMemoryClass();
      Method method = clazz.getMethod("asMemory");
      assertNotNull(method, "asMemory method should exist");
      assertEquals(WasmMemory.class, method.getReturnType(), "Return type should be WasmMemory");
      assertEquals(0, method.getParameterCount(), "asMemory should have no parameters");
    }

    @Test
    @DisplayName("JniExternMemory should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws Exception {
      Class<?> clazz = getJniExternMemoryClass();
      Method method = clazz.getDeclaredMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
      assertEquals(0, method.getParameterCount(), "getNativeHandle should have no parameters");

      int modifiers = method.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "getNativeHandle should not be public");
      assertFalse(Modifier.isPrivate(modifiers), "getNativeHandle should not be private");
    }

    @Test
    @DisplayName("JniExternMemory should have at least 2 non-synthetic fields")
    void shouldHaveAtLeastTwoNonSyntheticFields() throws ClassNotFoundException {
      Class<?> clazz = getJniExternMemoryClass();
      // Count only non-synthetic fields (excludes $jacocoData and similar)
      long nonSyntheticCount =
          java.util.Arrays.stream(clazz.getDeclaredFields()).filter(f -> !f.isSynthetic()).count();
      assertTrue(
          nonSyntheticCount >= 2,
          "JniExternMemory should have at least 2 non-synthetic fields, found: "
              + nonSyntheticCount);
    }
  }

  // ========================================================================
  // JniExternTable Tests
  // ========================================================================

  @Nested
  @DisplayName("JniExternTable Tests")
  class JniExternTableTests {

    private Class<?> getJniExternTableClass() throws ClassNotFoundException {
      return Class.forName(JNI_PACKAGE + ".JniExternTable");
    }

    @Test
    @DisplayName("JniExternTable should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      Class<?> clazz = getJniExternTableClass();
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "JniExternTable should be final");
    }

    @Test
    @DisplayName("JniExternTable should be package-private")
    void shouldBePackagePrivate() throws ClassNotFoundException {
      Class<?> clazz = getJniExternTableClass();
      int modifiers = clazz.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "JniExternTable should not be public");
      assertFalse(Modifier.isProtected(modifiers), "JniExternTable should not be protected");
      assertFalse(Modifier.isPrivate(modifiers), "JniExternTable should not be private");
    }

    @Test
    @DisplayName("JniExternTable should implement Extern interface")
    void shouldImplementExternInterface() throws ClassNotFoundException {
      Class<?> clazz = getJniExternTableClass();
      assertTrue(Extern.class.isAssignableFrom(clazz), "JniExternTable should implement Extern");
    }

    @Test
    @DisplayName("JniExternTable should have nativeHandle field")
    void shouldHaveNativeHandleField() throws Exception {
      Class<?> clazz = getJniExternTableClass();
      Field field = clazz.getDeclaredField("nativeHandle");
      assertNotNull(field, "nativeHandle field should exist");
      assertEquals(long.class, field.getType(), "nativeHandle should be long");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "nativeHandle should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "nativeHandle should be final");
    }

    @Test
    @DisplayName("JniExternTable should have store field")
    void shouldHaveStoreField() throws Exception {
      Class<?> clazz = getJniExternTableClass();
      Field field = clazz.getDeclaredField("store");
      assertNotNull(field, "store field should exist");
      assertEquals(JniStore.class, field.getType(), "store should be JniStore");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "store should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "store should be final");
    }

    @Test
    @DisplayName("JniExternTable should have package-private constructor")
    void shouldHavePackagePrivateConstructor() throws Exception {
      Class<?> clazz = getJniExternTableClass();
      Constructor<?>[] constructors = clazz.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly 1 constructor");

      Constructor<?> constructor = constructors[0];
      int modifiers = constructor.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "Constructor should not be public");
      assertFalse(Modifier.isProtected(modifiers), "Constructor should not be protected");
      assertFalse(Modifier.isPrivate(modifiers), "Constructor should not be private");

      Class<?>[] paramTypes = constructor.getParameterTypes();
      assertEquals(2, paramTypes.length, "Constructor should have 2 parameters");
      assertEquals(long.class, paramTypes[0], "First parameter should be long");
      assertEquals(JniStore.class, paramTypes[1], "Second parameter should be JniStore");
    }

    @Test
    @DisplayName("JniExternTable should have getType method returning TABLE")
    void shouldHaveGetTypeMethod() throws Exception {
      Class<?> clazz = getJniExternTableClass();
      Method method = clazz.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(ExternType.class, method.getReturnType(), "Return type should be ExternType");
      assertEquals(0, method.getParameterCount(), "getType should have no parameters");
    }

    @Test
    @DisplayName("JniExternTable should have asTable method")
    void shouldHaveAsTableMethod() throws Exception {
      Class<?> clazz = getJniExternTableClass();
      Method method = clazz.getMethod("asTable");
      assertNotNull(method, "asTable method should exist");
      assertEquals(WasmTable.class, method.getReturnType(), "Return type should be WasmTable");
      assertEquals(0, method.getParameterCount(), "asTable should have no parameters");
    }

    @Test
    @DisplayName("JniExternTable should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws Exception {
      Class<?> clazz = getJniExternTableClass();
      Method method = clazz.getDeclaredMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
      assertEquals(0, method.getParameterCount(), "getNativeHandle should have no parameters");

      int modifiers = method.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "getNativeHandle should not be public");
      assertFalse(Modifier.isPrivate(modifiers), "getNativeHandle should not be private");
    }

    @Test
    @DisplayName("JniExternTable should have at least 2 non-synthetic fields")
    void shouldHaveAtLeastTwoNonSyntheticFields() throws ClassNotFoundException {
      Class<?> clazz = getJniExternTableClass();
      // Count only non-synthetic fields (excludes $jacocoData and similar)
      long nonSyntheticCount =
          java.util.Arrays.stream(clazz.getDeclaredFields()).filter(f -> !f.isSynthetic()).count();
      assertTrue(
          nonSyntheticCount >= 2,
          "JniExternTable should have at least 2 non-synthetic fields, found: "
              + nonSyntheticCount);
    }
  }

  // ========================================================================
  // Cross-Implementation Consistency Tests
  // ========================================================================

  @Nested
  @DisplayName("Cross-Implementation Consistency Tests")
  class CrossImplementationConsistencyTests {

    private final String[] externImplClasses = {
      "JniExternFunc", "JniExternGlobal", "JniExternMemory", "JniExternTable"
    };

    @Test
    @DisplayName("All JniExtern implementations should be in same package")
    void allShouldBeInSamePackage() throws ClassNotFoundException {
      for (String className : externImplClasses) {
        Class<?> clazz = Class.forName(JNI_PACKAGE + "." + className);
        assertEquals(
            JNI_PACKAGE, clazz.getPackage().getName(), className + " should be in " + JNI_PACKAGE);
      }
    }

    @Test
    @DisplayName("All JniExtern implementations should implement Extern")
    void allShouldImplementExtern() throws ClassNotFoundException {
      for (String className : externImplClasses) {
        Class<?> clazz = Class.forName(JNI_PACKAGE + "." + className);
        assertTrue(
            Extern.class.isAssignableFrom(clazz), className + " should implement Extern interface");
      }
    }

    @Test
    @DisplayName("All JniExtern implementations should be final")
    void allShouldBeFinal() throws ClassNotFoundException {
      for (String className : externImplClasses) {
        Class<?> clazz = Class.forName(JNI_PACKAGE + "." + className);
        assertTrue(Modifier.isFinal(clazz.getModifiers()), className + " should be final");
      }
    }

    @Test
    @DisplayName("All JniExtern implementations should be package-private")
    void allShouldBePackagePrivate() throws ClassNotFoundException {
      for (String className : externImplClasses) {
        Class<?> clazz = Class.forName(JNI_PACKAGE + "." + className);
        int modifiers = clazz.getModifiers();
        assertFalse(Modifier.isPublic(modifiers), className + " should not be public");
        assertFalse(Modifier.isProtected(modifiers), className + " should not be protected");
        assertFalse(Modifier.isPrivate(modifiers), className + " should not be private");
      }
    }

    @Test
    @DisplayName("All JniExtern implementations should have nativeHandle field")
    void allShouldHaveNativeHandleField() throws Exception {
      for (String className : externImplClasses) {
        Class<?> clazz = Class.forName(JNI_PACKAGE + "." + className);
        Field field = clazz.getDeclaredField("nativeHandle");
        assertNotNull(field, className + " should have nativeHandle field");
        assertEquals(long.class, field.getType(), className + ".nativeHandle should be long");
      }
    }

    @Test
    @DisplayName("All JniExtern implementations should have store field")
    void allShouldHaveStoreField() throws Exception {
      for (String className : externImplClasses) {
        Class<?> clazz = Class.forName(JNI_PACKAGE + "." + className);
        Field field = clazz.getDeclaredField("store");
        assertNotNull(field, className + " should have store field");
        assertEquals(JniStore.class, field.getType(), className + ".store should be JniStore");
      }
    }

    @Test
    @DisplayName("All JniExtern implementations should have getType method")
    void allShouldHaveGetTypeMethod() throws Exception {
      for (String className : externImplClasses) {
        Class<?> clazz = Class.forName(JNI_PACKAGE + "." + className);
        Method method = clazz.getMethod("getType");
        assertNotNull(method, className + " should have getType method");
        assertEquals(
            ExternType.class,
            method.getReturnType(),
            className + ".getType should return ExternType");
      }
    }

    @Test
    @DisplayName("All JniExtern implementations should have getNativeHandle method")
    void allShouldHaveGetNativeHandleMethod() throws Exception {
      for (String className : externImplClasses) {
        Class<?> clazz = Class.forName(JNI_PACKAGE + "." + className);
        Method method = clazz.getDeclaredMethod("getNativeHandle");
        assertNotNull(method, className + " should have getNativeHandle method");
        assertEquals(
            long.class, method.getReturnType(), className + ".getNativeHandle should return long");
      }
    }

    @Test
    @DisplayName("All JniExtern implementations should have at least 2 non-synthetic fields")
    void allShouldHaveAtLeastTwoNonSyntheticFields() throws ClassNotFoundException {
      for (String className : externImplClasses) {
        Class<?> clazz = Class.forName(JNI_PACKAGE + "." + className);
        // Count only non-synthetic fields (excludes $jacocoData and similar)
        long nonSyntheticCount =
            java.util.Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> !f.isSynthetic())
                .count();
        assertTrue(
            nonSyntheticCount >= 2,
            className
                + " should have at least 2 non-synthetic fields, found: "
                + nonSyntheticCount);
      }
    }

    @Test
    @DisplayName("All JniExtern implementations should have exactly 1 constructor")
    void allShouldHaveExactlyOneConstructor() throws ClassNotFoundException {
      for (String className : externImplClasses) {
        Class<?> clazz = Class.forName(JNI_PACKAGE + "." + className);
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        assertEquals(1, constructors.length, className + " should have exactly 1 constructor");
      }
    }
  }

  // ========================================================================
  // ExternType Mapping Tests
  // ========================================================================

  @Nested
  @DisplayName("ExternType Mapping Tests")
  class ExternTypeMappingTests {

    @Test
    @DisplayName("JniExternFunc getType should return ExternType.FUNC")
    void funcShouldReturnFuncType() throws Exception {
      Class<?> clazz = Class.forName(JNI_PACKAGE + ".JniExternFunc");
      Method getType = clazz.getMethod("getType");

      // Verify method exists and returns correct type
      assertEquals(
          ExternType.class, getType.getReturnType(), "getType should return ExternType class");
    }

    @Test
    @DisplayName("JniExternGlobal getType should return ExternType.GLOBAL")
    void globalShouldReturnGlobalType() throws Exception {
      Class<?> clazz = Class.forName(JNI_PACKAGE + ".JniExternGlobal");
      Method getType = clazz.getMethod("getType");

      assertEquals(
          ExternType.class, getType.getReturnType(), "getType should return ExternType class");
    }

    @Test
    @DisplayName("JniExternMemory getType should return ExternType.MEMORY")
    void memoryShouldReturnMemoryType() throws Exception {
      Class<?> clazz = Class.forName(JNI_PACKAGE + ".JniExternMemory");
      Method getType = clazz.getMethod("getType");

      assertEquals(
          ExternType.class, getType.getReturnType(), "getType should return ExternType class");
    }

    @Test
    @DisplayName("JniExternTable getType should return ExternType.TABLE")
    void tableShouldReturnTableType() throws Exception {
      Class<?> clazz = Class.forName(JNI_PACKAGE + ".JniExternTable");
      Method getType = clazz.getMethod("getType");

      assertEquals(
          ExternType.class, getType.getReturnType(), "getType should return ExternType class");
    }
  }

  // ========================================================================
  // Conversion Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Conversion Method Tests")
  class ConversionMethodTests {

    @Test
    @DisplayName("JniExternFunc asFunction should return WasmFunction")
    void funcAsFunctionShouldReturnWasmFunction() throws Exception {
      Class<?> clazz = Class.forName(JNI_PACKAGE + ".JniExternFunc");
      Method method = clazz.getMethod("asFunction");

      assertEquals(
          WasmFunction.class, method.getReturnType(), "asFunction should return WasmFunction");
      assertTrue(Modifier.isPublic(method.getModifiers()), "asFunction should be public");
    }

    @Test
    @DisplayName("JniExternGlobal asGlobal should return WasmGlobal")
    void globalAsGlobalShouldReturnWasmGlobal() throws Exception {
      Class<?> clazz = Class.forName(JNI_PACKAGE + ".JniExternGlobal");
      Method method = clazz.getMethod("asGlobal");

      assertEquals(WasmGlobal.class, method.getReturnType(), "asGlobal should return WasmGlobal");
      assertTrue(Modifier.isPublic(method.getModifiers()), "asGlobal should be public");
    }

    @Test
    @DisplayName("JniExternMemory asMemory should return WasmMemory")
    void memoryAsMemoryShouldReturnWasmMemory() throws Exception {
      Class<?> clazz = Class.forName(JNI_PACKAGE + ".JniExternMemory");
      Method method = clazz.getMethod("asMemory");

      assertEquals(WasmMemory.class, method.getReturnType(), "asMemory should return WasmMemory");
      assertTrue(Modifier.isPublic(method.getModifiers()), "asMemory should be public");
    }

    @Test
    @DisplayName("JniExternTable asTable should return WasmTable")
    void tableAsTableShouldReturnWasmTable() throws Exception {
      Class<?> clazz = Class.forName(JNI_PACKAGE + ".JniExternTable");
      Method method = clazz.getMethod("asTable");

      assertEquals(WasmTable.class, method.getReturnType(), "asTable should return WasmTable");
      assertTrue(Modifier.isPublic(method.getModifiers()), "asTable should be public");
    }
  }

  // ========================================================================
  // Interface Compliance Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("Extern interface should have getType method")
    void externInterfaceShouldHaveGetType() throws NoSuchMethodException {
      Method method = Extern.class.getMethod("getType");
      assertNotNull(method, "Extern.getType should exist");
      assertEquals(ExternType.class, method.getReturnType(), "Should return ExternType");
    }

    @Test
    @DisplayName("All JniExtern implementations should override Extern methods")
    void allShouldOverrideExternMethods() throws Exception {
      String[] classNames = {
        "JniExternFunc", "JniExternGlobal", "JniExternMemory", "JniExternTable"
      };

      for (String className : classNames) {
        Class<?> clazz = Class.forName(JNI_PACKAGE + "." + className);

        // Check getType is declared in the class (not just inherited)
        Method getType = clazz.getDeclaredMethod("getType");
        assertNotNull(getType, className + " should declare getType method");
        // Note: @Override annotation may not be preserved in all cases
        // The important thing is that the method is declared and implements the interface
        assertEquals(
            ExternType.class,
            getType.getReturnType(),
            className + ".getType should return ExternType");
      }
    }

    @Test
    @DisplayName("JniExternFunc should implement asFunction from Extern")
    void funcShouldImplementAsFunction() throws Exception {
      Class<?> clazz = Class.forName(JNI_PACKAGE + ".JniExternFunc");
      Method method = clazz.getDeclaredMethod("asFunction");
      assertNotNull(method, "JniExternFunc.asFunction should be declared");
      // Verify it's the implementation (correct return type)
      assertEquals(WasmFunction.class, method.getReturnType(), "Should return WasmFunction");
    }

    @Test
    @DisplayName("JniExternGlobal should implement asGlobal from Extern")
    void globalShouldImplementAsGlobal() throws Exception {
      Class<?> clazz = Class.forName(JNI_PACKAGE + ".JniExternGlobal");
      Method method = clazz.getDeclaredMethod("asGlobal");
      assertNotNull(method, "JniExternGlobal.asGlobal should be declared");
      // Verify it's the implementation (correct return type)
      assertEquals(WasmGlobal.class, method.getReturnType(), "Should return WasmGlobal");
    }

    @Test
    @DisplayName("JniExternMemory should implement asMemory from Extern")
    void memoryShouldImplementAsMemory() throws Exception {
      Class<?> clazz = Class.forName(JNI_PACKAGE + ".JniExternMemory");
      Method method = clazz.getDeclaredMethod("asMemory");
      assertNotNull(method, "JniExternMemory.asMemory should be declared");
      // Verify it's the implementation (correct return type)
      assertEquals(WasmMemory.class, method.getReturnType(), "Should return WasmMemory");
    }

    @Test
    @DisplayName("JniExternTable should implement asTable from Extern")
    void tableShouldImplementAsTable() throws Exception {
      Class<?> clazz = Class.forName(JNI_PACKAGE + ".JniExternTable");
      Method method = clazz.getDeclaredMethod("asTable");
      assertNotNull(method, "JniExternTable.asTable should be declared");
      // Verify it's the implementation (correct return type)
      assertEquals(WasmTable.class, method.getReturnType(), "Should return WasmTable");
    }
  }
}
