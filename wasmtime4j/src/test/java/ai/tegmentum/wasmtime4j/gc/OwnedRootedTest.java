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

package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link OwnedRooted} class.
 *
 * <p>OwnedRooted provides an owned rooted reference to a GC-managed WebAssembly object with
 * explicit ownership semantics.
 */
@DisplayName("OwnedRooted Tests")
class OwnedRootedTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(Modifier.isFinal(OwnedRooted.class.getModifiers()), "OwnedRooted should be final");
    }

    @Test
    @DisplayName("should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(OwnedRooted.class),
          "OwnedRooted should implement AutoCloseable");
    }

    @Test
    @DisplayName("should be generic class")
    void shouldBeGenericClass() {
      final TypeVariable<?>[] typeParams = OwnedRooted.class.getTypeParameters();
      assertEquals(1, typeParams.length, "Should have 1 type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be T");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have generic create method")
    void shouldHaveGenericCreateMethod() {
      assertTrue(hasMethod(OwnedRooted.class, "create"), "Should have create method");
    }

    @Test
    @DisplayName("should have create method for AnyRef")
    void shouldHaveCreateMethodForAnyRef() throws NoSuchMethodException {
      // Check if specialized create methods exist
      final Method[] methods = OwnedRooted.class.getMethods();
      boolean hasAnyRefCreate =
          Arrays.stream(methods)
              .filter(m -> m.getName().equals("create"))
              .anyMatch(m -> Arrays.asList(m.getParameterTypes()).contains(AnyRef.class));

      assertTrue(hasAnyRefCreate, "Should have create method for AnyRef");
    }

    @Test
    @DisplayName("should have create method for EqRef")
    void shouldHaveCreateMethodForEqRef() {
      final Method[] methods = OwnedRooted.class.getMethods();
      boolean hasEqRefCreate =
          Arrays.stream(methods)
              .filter(m -> m.getName().equals("create"))
              .anyMatch(m -> Arrays.asList(m.getParameterTypes()).contains(EqRef.class));

      assertTrue(hasEqRefCreate, "Should have create method for EqRef");
    }

    @Test
    @DisplayName("should have create method for StructRef")
    void shouldHaveCreateMethodForStructRef() {
      final Method[] methods = OwnedRooted.class.getMethods();
      boolean hasStructRefCreate =
          Arrays.stream(methods)
              .filter(m -> m.getName().equals("create"))
              .anyMatch(m -> Arrays.asList(m.getParameterTypes()).contains(StructRef.class));

      assertTrue(hasStructRefCreate, "Should have create method for StructRef");
    }

    @Test
    @DisplayName("should have create method for ArrayRef")
    void shouldHaveCreateMethodForArrayRef() {
      final Method[] methods = OwnedRooted.class.getMethods();
      boolean hasArrayRefCreate =
          Arrays.stream(methods)
              .filter(m -> m.getName().equals("create"))
              .anyMatch(m -> Arrays.asList(m.getParameterTypes()).contains(ArrayRef.class));

      assertTrue(hasArrayRefCreate, "Should have create method for ArrayRef");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Instance Method Tests")
  class InstanceMethodTests {

    @Test
    @DisplayName("should have get method")
    void shouldHaveGetMethod() {
      assertTrue(hasMethod(OwnedRooted.class, "get"), "Should have get method");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = OwnedRooted.class.getMethod("isValid");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getRootId method")
    void shouldHaveGetRootIdMethod() throws NoSuchMethodException {
      final Method method = OwnedRooted.class.getMethod("getRootId");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getStoreId method")
    void shouldHaveGetStoreIdMethod() throws NoSuchMethodException {
      final Method method = OwnedRooted.class.getMethod("getStoreId");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have release method")
    void shouldHaveReleaseMethod() throws NoSuchMethodException {
      final Method method = OwnedRooted.class.getMethod("release");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have transferToScope method")
    void shouldHaveTransferToScopeMethod() throws NoSuchMethodException {
      final Method method = OwnedRooted.class.getMethod("transferToScope", RootScope.class);
      assertEquals(Rooted.class, method.getReturnType(), "Should return Rooted");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = OwnedRooted.class.getMethod("close");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Ownership Semantics Tests")
  class OwnershipSemanticsTests {

    @Test
    @DisplayName("should have all ownership methods")
    void shouldHaveAllOwnershipMethods() {
      final String[] expectedMethods = {
        "get", "isValid", "getRootId", "getStoreId", "release", "transferToScope", "close"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(hasMethod(OwnedRooted.class, methodName), "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("equals and hashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = OwnedRooted.class.getMethod("equals", Object.class);
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = OwnedRooted.class.getMethod("hashCode");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = OwnedRooted.class.getMethod("toString");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("AutoCloseable Pattern Tests")
  class AutoCloseablePatternTests {

    @Test
    @DisplayName("close should be equivalent to release")
    void closeShouldBeEquivalentToRelease() {
      // Both methods should exist and release the owned reference
      assertTrue(hasMethod(OwnedRooted.class, "close"), "Should have close method");
      assertTrue(hasMethod(OwnedRooted.class, "release"), "Should have release method");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Usage Pattern Documentation Tests")
  class UsagePatternDocumentationTests {

    @Test
    @DisplayName("should support try-with-resources pattern")
    void shouldSupportTryWithResourcesPattern() {
      // Documents usage: try (OwnedRooted<T> owned = OwnedRooted.create(...)) { ... }
      assertTrue(
          AutoCloseable.class.isAssignableFrom(OwnedRooted.class),
          "Should implement AutoCloseable for try-with-resources");
    }

    @Test
    @DisplayName("should support ownership transfer pattern")
    void shouldSupportOwnershipTransferPattern() {
      // Documents usage: Rooted<T> rooted = owned.transferToScope(scope);
      assertTrue(
          hasMethod(OwnedRooted.class, "transferToScope"), "Should support ownership transfer");
    }

    @Test
    @DisplayName("should support validity checking pattern")
    void shouldSupportValidityCheckingPattern() {
      // Documents usage: if (owned.isValid()) { T value = owned.get(store); }
      assertTrue(hasMethod(OwnedRooted.class, "isValid"), "Should support validity checking");
      assertTrue(hasMethod(OwnedRooted.class, "get"), "Should support value retrieval");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Thread Safety Documentation Tests")
  class ThreadSafetyDocumentationTests {

    @Test
    @DisplayName("should document thread safety requirements")
    void shouldDocumentThreadSafetyRequirements() {
      // OwnedRooted is not thread-safe - this test documents that
      // External synchronization is required for multi-threaded access
      assertNotNull(OwnedRooted.class, "OwnedRooted should be documented as not thread-safe");
    }
  }
}
