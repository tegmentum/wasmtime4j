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

import ai.tegmentum.wasmtime4j.Extern;
import ai.tegmentum.wasmtime4j.type.ExternType;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for JNI Extern type implementations. Tests JniExternFunc, JniExternGlobal,
 * JniExternMemory, and JniExternTable.
 */
@DisplayName("JNI Extern Types Tests")
class JniExternTypesTest {

  @Nested
  @DisplayName("JniExternFunc Tests")
  class JniExternFuncTests {

    @Test
    @DisplayName("JniExternFunc should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniExternFunc.class.getModifiers()), "JniExternFunc should be final");
    }

    @Test
    @DisplayName("JniExternFunc should be package-private")
    void shouldBePackagePrivate() {
      int modifiers = JniExternFunc.class.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "JniExternFunc should be package-private");
    }

    @Test
    @DisplayName("JniExternFunc should implement Extern")
    void shouldImplementExtern() {
      assertTrue(
          Extern.class.isAssignableFrom(JniExternFunc.class),
          "JniExternFunc should implement Extern");
    }

    @Test
    @DisplayName("JniExternFunc should have nativeHandle field")
    void shouldHaveNativeHandleField() throws NoSuchFieldException {
      Field field = JniExternFunc.class.getDeclaredField("nativeHandle");
      assertNotNull(field, "nativeHandle field should exist");
      assertEquals(long.class, field.getType(), "Should be long type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("JniExternFunc should have store field")
    void shouldHaveStoreField() throws NoSuchFieldException {
      Field field = JniExternFunc.class.getDeclaredField("store");
      assertNotNull(field, "store field should exist");
      assertEquals(JniStore.class, field.getType(), "Should be JniStore type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("JniExternFunc should have package-private constructor")
    void shouldHavePackagePrivateConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          JniExternFunc.class.getDeclaredConstructor(long.class, JniStore.class);
      assertNotNull(constructor, "Constructor should exist");
      int modifiers = constructor.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "Constructor should be package-private");
    }

    @Test
    @DisplayName("JniExternFunc getType should return ExternType")
    void getTypeShouldReturnExternType() throws NoSuchMethodException {
      Method method = JniExternFunc.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(ExternType.class, method.getReturnType(), "Should return ExternType");
    }

    @Test
    @DisplayName("JniExternFunc asFunction should return WasmFunction")
    void asFunctionShouldReturnWasmFunction() throws NoSuchMethodException {
      Method method = JniExternFunc.class.getMethod("asFunction");
      assertNotNull(method, "asFunction method should exist");
      assertEquals(WasmFunction.class, method.getReturnType(), "Should return WasmFunction");
    }

    @Test
    @DisplayName("JniExternFunc should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      Method method = JniExternFunc.class.getDeclaredMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      int modifiers = method.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "getNativeHandle should be package-private");
    }
  }

  @Nested
  @DisplayName("JniExternGlobal Tests")
  class JniExternGlobalTests {

    @Test
    @DisplayName("JniExternGlobal should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniExternGlobal.class.getModifiers()),
          "JniExternGlobal should be final");
    }

    @Test
    @DisplayName("JniExternGlobal should be package-private")
    void shouldBePackagePrivate() {
      int modifiers = JniExternGlobal.class.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "JniExternGlobal should be package-private");
    }

    @Test
    @DisplayName("JniExternGlobal should implement Extern")
    void shouldImplementExtern() {
      assertTrue(
          Extern.class.isAssignableFrom(JniExternGlobal.class),
          "JniExternGlobal should implement Extern");
    }

    @Test
    @DisplayName("JniExternGlobal should have nativeHandle field")
    void shouldHaveNativeHandleField() throws NoSuchFieldException {
      Field field = JniExternGlobal.class.getDeclaredField("nativeHandle");
      assertNotNull(field, "nativeHandle field should exist");
      assertEquals(long.class, field.getType(), "Should be long type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("JniExternGlobal should have store field")
    void shouldHaveStoreField() throws NoSuchFieldException {
      Field field = JniExternGlobal.class.getDeclaredField("store");
      assertNotNull(field, "store field should exist");
      assertEquals(JniStore.class, field.getType(), "Should be JniStore type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("JniExternGlobal should have package-private constructor")
    void shouldHavePackagePrivateConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          JniExternGlobal.class.getDeclaredConstructor(long.class, JniStore.class);
      assertNotNull(constructor, "Constructor should exist");
      int modifiers = constructor.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "Constructor should be package-private");
    }

    @Test
    @DisplayName("JniExternGlobal getType should return ExternType")
    void getTypeShouldReturnExternType() throws NoSuchMethodException {
      Method method = JniExternGlobal.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(ExternType.class, method.getReturnType(), "Should return ExternType");
    }

    @Test
    @DisplayName("JniExternGlobal asGlobal should return WasmGlobal")
    void asGlobalShouldReturnWasmGlobal() throws NoSuchMethodException {
      Method method = JniExternGlobal.class.getMethod("asGlobal");
      assertNotNull(method, "asGlobal method should exist");
      assertEquals(WasmGlobal.class, method.getReturnType(), "Should return WasmGlobal");
    }

    @Test
    @DisplayName("JniExternGlobal should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      Method method = JniExternGlobal.class.getDeclaredMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      int modifiers = method.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "getNativeHandle should be package-private");
    }
  }

  @Nested
  @DisplayName("JniExternMemory Tests")
  class JniExternMemoryTests {

    @Test
    @DisplayName("JniExternMemory should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniExternMemory.class.getModifiers()),
          "JniExternMemory should be final");
    }

    @Test
    @DisplayName("JniExternMemory should be package-private")
    void shouldBePackagePrivate() {
      int modifiers = JniExternMemory.class.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "JniExternMemory should be package-private");
    }

    @Test
    @DisplayName("JniExternMemory should implement Extern")
    void shouldImplementExtern() {
      assertTrue(
          Extern.class.isAssignableFrom(JniExternMemory.class),
          "JniExternMemory should implement Extern");
    }

    @Test
    @DisplayName("JniExternMemory should have nativeHandle field")
    void shouldHaveNativeHandleField() throws NoSuchFieldException {
      Field field = JniExternMemory.class.getDeclaredField("nativeHandle");
      assertNotNull(field, "nativeHandle field should exist");
      assertEquals(long.class, field.getType(), "Should be long type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("JniExternMemory should have store field")
    void shouldHaveStoreField() throws NoSuchFieldException {
      Field field = JniExternMemory.class.getDeclaredField("store");
      assertNotNull(field, "store field should exist");
      assertEquals(JniStore.class, field.getType(), "Should be JniStore type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("JniExternMemory should have package-private constructor")
    void shouldHavePackagePrivateConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          JniExternMemory.class.getDeclaredConstructor(long.class, JniStore.class);
      assertNotNull(constructor, "Constructor should exist");
      int modifiers = constructor.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "Constructor should be package-private");
    }

    @Test
    @DisplayName("JniExternMemory getType should return ExternType")
    void getTypeShouldReturnExternType() throws NoSuchMethodException {
      Method method = JniExternMemory.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(ExternType.class, method.getReturnType(), "Should return ExternType");
    }

    @Test
    @DisplayName("JniExternMemory asMemory should return WasmMemory")
    void asMemoryShouldReturnWasmMemory() throws NoSuchMethodException {
      Method method = JniExternMemory.class.getMethod("asMemory");
      assertNotNull(method, "asMemory method should exist");
      assertEquals(WasmMemory.class, method.getReturnType(), "Should return WasmMemory");
    }

    @Test
    @DisplayName("JniExternMemory should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      Method method = JniExternMemory.class.getDeclaredMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      int modifiers = method.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "getNativeHandle should be package-private");
    }
  }

  @Nested
  @DisplayName("JniExternTable Tests")
  class JniExternTableTests {

    @Test
    @DisplayName("JniExternTable should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniExternTable.class.getModifiers()), "JniExternTable should be final");
    }

    @Test
    @DisplayName("JniExternTable should be package-private")
    void shouldBePackagePrivate() {
      int modifiers = JniExternTable.class.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "JniExternTable should be package-private");
    }

    @Test
    @DisplayName("JniExternTable should implement Extern")
    void shouldImplementExtern() {
      assertTrue(
          Extern.class.isAssignableFrom(JniExternTable.class),
          "JniExternTable should implement Extern");
    }

    @Test
    @DisplayName("JniExternTable should have nativeHandle field")
    void shouldHaveNativeHandleField() throws NoSuchFieldException {
      Field field = JniExternTable.class.getDeclaredField("nativeHandle");
      assertNotNull(field, "nativeHandle field should exist");
      assertEquals(long.class, field.getType(), "Should be long type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("JniExternTable should have store field")
    void shouldHaveStoreField() throws NoSuchFieldException {
      Field field = JniExternTable.class.getDeclaredField("store");
      assertNotNull(field, "store field should exist");
      assertEquals(JniStore.class, field.getType(), "Should be JniStore type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("JniExternTable should have package-private constructor")
    void shouldHavePackagePrivateConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          JniExternTable.class.getDeclaredConstructor(long.class, JniStore.class);
      assertNotNull(constructor, "Constructor should exist");
      int modifiers = constructor.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "Constructor should be package-private");
    }

    @Test
    @DisplayName("JniExternTable getType should return ExternType")
    void getTypeShouldReturnExternType() throws NoSuchMethodException {
      Method method = JniExternTable.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(ExternType.class, method.getReturnType(), "Should return ExternType");
    }

    @Test
    @DisplayName("JniExternTable asTable should return WasmTable")
    void asTableShouldReturnWasmTable() throws NoSuchMethodException {
      Method method = JniExternTable.class.getMethod("asTable");
      assertNotNull(method, "asTable method should exist");
      assertEquals(WasmTable.class, method.getReturnType(), "Should return WasmTable");
    }

    @Test
    @DisplayName("JniExternTable should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      Method method = JniExternTable.class.getDeclaredMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      int modifiers = method.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "getNativeHandle should be package-private");
    }
  }

  @Nested
  @DisplayName("Cross-Type Consistency Tests")
  class CrossTypeConsistencyTests {

    @Test
    @DisplayName("All Extern types should be in same package")
    void allExternTypesShouldBeInSamePackage() {
      String funcPackage = JniExternFunc.class.getPackage().getName();
      String globalPackage = JniExternGlobal.class.getPackage().getName();
      String memoryPackage = JniExternMemory.class.getPackage().getName();
      String tablePackage = JniExternTable.class.getPackage().getName();

      assertEquals(funcPackage, globalPackage, "Func and Global should be in same package");
      assertEquals(globalPackage, memoryPackage, "Global and Memory should be in same package");
      assertEquals(memoryPackage, tablePackage, "Memory and Table should be in same package");
      assertEquals("ai.tegmentum.wasmtime4j.jni", funcPackage, "Should be in jni package");
    }

    @Test
    @DisplayName("All Extern types should implement Extern interface")
    void allExternTypesShouldImplementExtern() {
      assertTrue(
          Extern.class.isAssignableFrom(JniExternFunc.class),
          "JniExternFunc should implement Extern");
      assertTrue(
          Extern.class.isAssignableFrom(JniExternGlobal.class),
          "JniExternGlobal should implement Extern");
      assertTrue(
          Extern.class.isAssignableFrom(JniExternMemory.class),
          "JniExternMemory should implement Extern");
      assertTrue(
          Extern.class.isAssignableFrom(JniExternTable.class),
          "JniExternTable should implement Extern");
    }

    @Test
    @DisplayName("All Extern types should be final")
    void allExternTypesShouldBeFinal() {
      assertTrue(
          Modifier.isFinal(JniExternFunc.class.getModifiers()), "JniExternFunc should be final");
      assertTrue(
          Modifier.isFinal(JniExternGlobal.class.getModifiers()),
          "JniExternGlobal should be final");
      assertTrue(
          Modifier.isFinal(JniExternMemory.class.getModifiers()),
          "JniExternMemory should be final");
      assertTrue(
          Modifier.isFinal(JniExternTable.class.getModifiers()), "JniExternTable should be final");
    }

    @Test
    @DisplayName("All Extern types should have consistent field structure")
    void allExternTypesShouldHaveConsistentFieldStructure() throws NoSuchFieldException {
      // Verify nativeHandle field exists in all types
      assertNotNull(JniExternFunc.class.getDeclaredField("nativeHandle"));
      assertNotNull(JniExternGlobal.class.getDeclaredField("nativeHandle"));
      assertNotNull(JniExternMemory.class.getDeclaredField("nativeHandle"));
      assertNotNull(JniExternTable.class.getDeclaredField("nativeHandle"));

      // Verify store field exists in all types
      assertNotNull(JniExternFunc.class.getDeclaredField("store"));
      assertNotNull(JniExternGlobal.class.getDeclaredField("store"));
      assertNotNull(JniExternMemory.class.getDeclaredField("store"));
      assertNotNull(JniExternTable.class.getDeclaredField("store"));
    }

    @Test
    @DisplayName("All Extern types should have consistent constructor signature")
    void allExternTypesShouldHaveConsistentConstructorSignature() throws NoSuchMethodException {
      assertNotNull(JniExternFunc.class.getDeclaredConstructor(long.class, JniStore.class));
      assertNotNull(JniExternGlobal.class.getDeclaredConstructor(long.class, JniStore.class));
      assertNotNull(JniExternMemory.class.getDeclaredConstructor(long.class, JniStore.class));
      assertNotNull(JniExternTable.class.getDeclaredConstructor(long.class, JniStore.class));
    }

    @Test
    @DisplayName("All Extern types should have getType method")
    void allExternTypesShouldHaveGetTypeMethod() throws NoSuchMethodException {
      assertNotNull(JniExternFunc.class.getMethod("getType"));
      assertNotNull(JniExternGlobal.class.getMethod("getType"));
      assertNotNull(JniExternMemory.class.getMethod("getType"));
      assertNotNull(JniExternTable.class.getMethod("getType"));
    }

    @Test
    @DisplayName("All Extern types should have getNativeHandle method")
    void allExternTypesShouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      assertNotNull(JniExternFunc.class.getDeclaredMethod("getNativeHandle"));
      assertNotNull(JniExternGlobal.class.getDeclaredMethod("getNativeHandle"));
      assertNotNull(JniExternMemory.class.getDeclaredMethod("getNativeHandle"));
      assertNotNull(JniExternTable.class.getDeclaredMethod("getNativeHandle"));
    }
  }

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("JniExternFunc should implement all Extern interface methods")
    void jniExternFuncShouldImplementAllExternMethods() {
      for (Method interfaceMethod : Extern.class.getMethods()) {
        if (!interfaceMethod.isDefault() && !Modifier.isStatic(interfaceMethod.getModifiers())) {
          boolean found = false;
          for (Method implMethod : JniExternFunc.class.getMethods()) {
            if (implMethod.getName().equals(interfaceMethod.getName())
                && arrayEquals(
                    implMethod.getParameterTypes(), interfaceMethod.getParameterTypes())) {
              found = true;
              break;
            }
          }
          assertTrue(found, "JniExternFunc should implement: " + interfaceMethod.getName());
        }
      }
    }

    @Test
    @DisplayName("JniExternGlobal should implement all Extern interface methods")
    void jniExternGlobalShouldImplementAllExternMethods() {
      for (Method interfaceMethod : Extern.class.getMethods()) {
        if (!interfaceMethod.isDefault() && !Modifier.isStatic(interfaceMethod.getModifiers())) {
          boolean found = false;
          for (Method implMethod : JniExternGlobal.class.getMethods()) {
            if (implMethod.getName().equals(interfaceMethod.getName())
                && arrayEquals(
                    implMethod.getParameterTypes(), interfaceMethod.getParameterTypes())) {
              found = true;
              break;
            }
          }
          assertTrue(found, "JniExternGlobal should implement: " + interfaceMethod.getName());
        }
      }
    }

    @Test
    @DisplayName("JniExternMemory should implement all Extern interface methods")
    void jniExternMemoryShouldImplementAllExternMethods() {
      for (Method interfaceMethod : Extern.class.getMethods()) {
        if (!interfaceMethod.isDefault() && !Modifier.isStatic(interfaceMethod.getModifiers())) {
          boolean found = false;
          for (Method implMethod : JniExternMemory.class.getMethods()) {
            if (implMethod.getName().equals(interfaceMethod.getName())
                && arrayEquals(
                    implMethod.getParameterTypes(), interfaceMethod.getParameterTypes())) {
              found = true;
              break;
            }
          }
          assertTrue(found, "JniExternMemory should implement: " + interfaceMethod.getName());
        }
      }
    }

    @Test
    @DisplayName("JniExternTable should implement all Extern interface methods")
    void jniExternTableShouldImplementAllExternMethods() {
      for (Method interfaceMethod : Extern.class.getMethods()) {
        if (!interfaceMethod.isDefault() && !Modifier.isStatic(interfaceMethod.getModifiers())) {
          boolean found = false;
          for (Method implMethod : JniExternTable.class.getMethods()) {
            if (implMethod.getName().equals(interfaceMethod.getName())
                && arrayEquals(
                    implMethod.getParameterTypes(), interfaceMethod.getParameterTypes())) {
              found = true;
              break;
            }
          }
          assertTrue(found, "JniExternTable should implement: " + interfaceMethod.getName());
        }
      }
    }

    private boolean arrayEquals(final Class<?>[] a, final Class<?>[] b) {
      if (a.length != b.length) {
        return false;
      }
      for (int i = 0; i < a.length; i++) {
        if (!a[i].equals(b[i])) {
          return false;
        }
      }
      return true;
    }
  }

  // --- Behavioral Tests ---
  // These test actual method behavior using null store references.
  // The constructors and getType()/getNativeHandle() methods are pure Java
  // and do not touch the store reference, so null is safe here.

  private static final long TEST_HANDLE = 0xDEADBEEFL;

  @Nested
  @DisplayName("JniExternFunc Behavioral Tests")
  class JniExternFuncBehavioralTests {

    @Test
    @DisplayName("getType returns FUNC")
    void getTypeReturnsFuncType() {
      final JniExternFunc extern = new JniExternFunc(TEST_HANDLE, null);
      assertEquals(ExternType.FUNC, extern.getType(),
          "JniExternFunc.getType() should return ExternType.FUNC");
    }

    @Test
    @DisplayName("getNativeHandle returns stored handle")
    void getNativeHandleReturnsStoredHandle() {
      final JniExternFunc extern = new JniExternFunc(TEST_HANDLE, null);
      assertEquals(TEST_HANDLE, extern.getNativeHandle(),
          "getNativeHandle() should return the handle passed to constructor");
    }

    @Test
    @DisplayName("implements Extern interface")
    void implementsExternInterface() {
      final JniExternFunc extern = new JniExternFunc(TEST_HANDLE, null);
      assertTrue(extern instanceof Extern,
          "JniExternFunc should implement Extern interface");
    }

    @Test
    @DisplayName("different handles produce different instances")
    void differentHandlesProduceDifferentInstances() {
      final JniExternFunc extern1 = new JniExternFunc(1L, null);
      final JniExternFunc extern2 = new JniExternFunc(2L, null);
      assertEquals(1L, extern1.getNativeHandle(),
          "First extern should have handle 1");
      assertEquals(2L, extern2.getNativeHandle(),
          "Second extern should have handle 2");
    }
  }

  @Nested
  @DisplayName("JniExternGlobal Behavioral Tests")
  class JniExternGlobalBehavioralTests {

    @Test
    @DisplayName("getType returns GLOBAL")
    void getTypeReturnsGlobalType() {
      final JniExternGlobal extern = new JniExternGlobal(TEST_HANDLE, null);
      assertEquals(ExternType.GLOBAL, extern.getType(),
          "JniExternGlobal.getType() should return ExternType.GLOBAL");
    }

    @Test
    @DisplayName("getNativeHandle returns stored handle")
    void getNativeHandleReturnsStoredHandle() {
      final JniExternGlobal extern = new JniExternGlobal(TEST_HANDLE, null);
      assertEquals(TEST_HANDLE, extern.getNativeHandle(),
          "getNativeHandle() should return the handle passed to constructor");
    }

    @Test
    @DisplayName("implements Extern interface")
    void implementsExternInterface() {
      final JniExternGlobal extern = new JniExternGlobal(TEST_HANDLE, null);
      assertTrue(extern instanceof Extern,
          "JniExternGlobal should implement Extern interface");
    }
  }

  @Nested
  @DisplayName("JniExternMemory Behavioral Tests")
  class JniExternMemoryBehavioralTests {

    @Test
    @DisplayName("getType returns MEMORY")
    void getTypeReturnsMemoryType() {
      final JniExternMemory extern = new JniExternMemory(TEST_HANDLE, null);
      assertEquals(ExternType.MEMORY, extern.getType(),
          "JniExternMemory.getType() should return ExternType.MEMORY");
    }

    @Test
    @DisplayName("getNativeHandle returns stored handle")
    void getNativeHandleReturnsStoredHandle() {
      final JniExternMemory extern = new JniExternMemory(TEST_HANDLE, null);
      assertEquals(TEST_HANDLE, extern.getNativeHandle(),
          "getNativeHandle() should return the handle passed to constructor");
    }

    @Test
    @DisplayName("implements Extern interface")
    void implementsExternInterface() {
      final JniExternMemory extern = new JniExternMemory(TEST_HANDLE, null);
      assertTrue(extern instanceof Extern,
          "JniExternMemory should implement Extern interface");
    }
  }

  @Nested
  @DisplayName("JniExternTable Behavioral Tests")
  class JniExternTableBehavioralTests {

    @Test
    @DisplayName("getType returns TABLE")
    void getTypeReturnsTableType() {
      final JniExternTable extern = new JniExternTable(TEST_HANDLE, null);
      assertEquals(ExternType.TABLE, extern.getType(),
          "JniExternTable.getType() should return ExternType.TABLE");
    }

    @Test
    @DisplayName("getNativeHandle returns stored handle")
    void getNativeHandleReturnsStoredHandle() {
      final JniExternTable extern = new JniExternTable(TEST_HANDLE, null);
      assertEquals(TEST_HANDLE, extern.getNativeHandle(),
          "getNativeHandle() should return the handle passed to constructor");
    }

    @Test
    @DisplayName("implements Extern interface")
    void implementsExternInterface() {
      final JniExternTable extern = new JniExternTable(TEST_HANDLE, null);
      assertTrue(extern instanceof Extern,
          "JniExternTable should implement Extern interface");
    }
  }
}
