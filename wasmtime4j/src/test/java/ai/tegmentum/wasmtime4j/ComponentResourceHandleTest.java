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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentResourceHandle} interface.
 *
 * <p>ComponentResourceHandle represents a Component Model resource handle (own or borrow).
 */
@DisplayName("ComponentResourceHandle Tests")
class ComponentResourceHandleTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentResourceHandle.class.getModifiers()),
          "ComponentResourceHandle should be public");
      assertTrue(
          ComponentResourceHandle.class.isInterface(),
          "ComponentResourceHandle should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getResourceType method")
    void shouldHaveGetResourceTypeMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceHandle.class.getMethod("getResourceType");
      assertNotNull(method, "getResourceType method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getIndex method")
    void shouldHaveGetIndexMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceHandle.class.getMethod("getIndex");
      assertNotNull(method, "getIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have isOwned method")
    void shouldHaveIsOwnedMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceHandle.class.getMethod("isOwned");
      assertNotNull(method, "isOwned method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isBorrowed method")
    void shouldHaveIsBorrowedMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceHandle.class.getMethod("isBorrowed");
      assertNotNull(method, "isBorrowed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getHostObject method")
    void shouldHaveGetHostObjectMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceHandle.class.getMethod("getHostObject", Class.class);
      assertNotNull(method, "getHostObject method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have own static method")
    void shouldHaveOwnStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceHandle.class.getMethod("own", String.class, int.class);
      assertNotNull(method, "own method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "own should be static");
      assertEquals(
          ComponentResourceHandle.class,
          method.getReturnType(),
          "Should return ComponentResourceHandle");
    }

    @Test
    @DisplayName("should have borrow static method")
    void shouldHaveBorrowStaticMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceHandle.class.getMethod("borrow", String.class, int.class);
      assertNotNull(method, "borrow method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "borrow should be static");
      assertEquals(
          ComponentResourceHandle.class,
          method.getReturnType(),
          "Should return ComponentResourceHandle");
    }

    @Test
    @DisplayName("should have ownWithHost static method")
    void shouldHaveOwnWithHostStaticMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceHandle.class.getMethod(
              "ownWithHost", String.class, int.class, Object.class);
      assertNotNull(method, "ownWithHost method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "ownWithHost should be static");
      assertEquals(
          ComponentResourceHandle.class,
          method.getReturnType(),
          "Should return ComponentResourceHandle");
    }

    @Test
    @DisplayName("should have borrowWithHost static method")
    void shouldHaveBorrowWithHostStaticMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceHandle.class.getMethod(
              "borrowWithHost", String.class, int.class, Object.class);
      assertNotNull(method, "borrowWithHost method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "borrowWithHost should be static");
      assertEquals(
          ComponentResourceHandle.class,
          method.getReturnType(),
          "Should return ComponentResourceHandle");
    }
  }

  @Nested
  @DisplayName("Impl Nested Class Tests")
  class ImplNestedClassTests {

    @Test
    @DisplayName("should have Impl nested class")
    void shouldHaveImplNestedClass() {
      final var nestedClasses = ComponentResourceHandle.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("Impl")) {
          found = true;
          assertFalse(nestedClass.isInterface(), "Impl should be a class");
          assertTrue(
              ComponentResourceHandle.class.isAssignableFrom(nestedClass),
              "Impl should implement ComponentResourceHandle");
          assertTrue(Modifier.isFinal(nestedClass.getModifiers()), "Impl should be final");
          break;
        }
      }
      assertTrue(found, "Should have Impl nested class");
    }
  }

  @Nested
  @DisplayName("Owned Handle Behavior Tests")
  class OwnedHandleBehaviorTests {

    @Test
    @DisplayName("own should create owned handle")
    void ownShouldCreateOwnedHandle() {
      final ComponentResourceHandle handle = ComponentResourceHandle.own("file", 42);

      assertNotNull(handle, "Should create handle");
      assertEquals("file", handle.getResourceType(), "Resource type should match");
      assertEquals(42, handle.getIndex(), "Index should match");
      assertTrue(handle.isOwned(), "Should be owned");
      assertFalse(handle.isBorrowed(), "Should not be borrowed");
    }

    @Test
    @DisplayName("ownWithHost should create owned handle with host object")
    void ownWithHostShouldCreateOwnedHandleWithHostObject() {
      final Object hostObject = "test-host-object";
      final ComponentResourceHandle handle =
          ComponentResourceHandle.ownWithHost("file", 42, hostObject);

      assertNotNull(handle, "Should create handle");
      assertTrue(handle.isOwned(), "Should be owned");
      assertEquals(hostObject, handle.getHostObject(String.class), "Host object should match");
    }

    @Test
    @DisplayName("own should reject null resource type")
    void ownShouldRejectNullResourceType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentResourceHandle.own(null, 42),
          "Should throw for null resource type");
    }
  }

  @Nested
  @DisplayName("Borrowed Handle Behavior Tests")
  class BorrowedHandleBehaviorTests {

    @Test
    @DisplayName("borrow should create borrowed handle")
    void borrowShouldCreateBorrowedHandle() {
      final ComponentResourceHandle handle = ComponentResourceHandle.borrow("file", 42);

      assertNotNull(handle, "Should create handle");
      assertEquals("file", handle.getResourceType(), "Resource type should match");
      assertEquals(42, handle.getIndex(), "Index should match");
      assertFalse(handle.isOwned(), "Should not be owned");
      assertTrue(handle.isBorrowed(), "Should be borrowed");
    }

    @Test
    @DisplayName("borrowWithHost should create borrowed handle with host object")
    void borrowWithHostShouldCreateBorrowedHandleWithHostObject() {
      final Object hostObject = "test-host-object";
      final ComponentResourceHandle handle =
          ComponentResourceHandle.borrowWithHost("file", 42, hostObject);

      assertNotNull(handle, "Should create handle");
      assertTrue(handle.isBorrowed(), "Should be borrowed");
      assertEquals(hostObject, handle.getHostObject(String.class), "Host object should match");
    }
  }

  @Nested
  @DisplayName("Host Object Tests")
  class HostObjectTests {

    @Test
    @DisplayName("getHostObject should throw for handle without host object")
    void getHostObjectShouldThrowForHandleWithoutHostObject() {
      final ComponentResourceHandle handle = ComponentResourceHandle.own("file", 42);

      assertThrows(
          IllegalStateException.class,
          () -> handle.getHostObject(String.class),
          "Should throw for handle without host object");
    }

    @Test
    @DisplayName("getHostObject should throw for wrong type")
    void getHostObjectShouldThrowForWrongType() {
      final String hostObject = "test-string";
      final ComponentResourceHandle handle =
          ComponentResourceHandle.ownWithHost("file", 42, hostObject);

      assertThrows(
          ClassCastException.class,
          () -> handle.getHostObject(Integer.class),
          "Should throw for wrong type");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("two owned handles with same type and index should be equal")
    void twoOwnedHandlesWithSameTypeAndIndexShouldBeEqual() {
      final ComponentResourceHandle h1 = ComponentResourceHandle.own("file", 42);
      final ComponentResourceHandle h2 = ComponentResourceHandle.own("file", 42);

      assertEquals(h1, h2, "Handles should be equal");
      assertEquals(h1.hashCode(), h2.hashCode(), "Hash codes should be equal");
    }

    @Test
    @DisplayName("two borrowed handles with same type and index should be equal")
    void twoBorrowedHandlesWithSameTypeAndIndexShouldBeEqual() {
      final ComponentResourceHandle h1 = ComponentResourceHandle.borrow("file", 42);
      final ComponentResourceHandle h2 = ComponentResourceHandle.borrow("file", 42);

      assertEquals(h1, h2, "Handles should be equal");
      assertEquals(h1.hashCode(), h2.hashCode(), "Hash codes should be equal");
    }

    @Test
    @DisplayName("owned and borrowed handles with same type and index should not be equal")
    void ownedAndBorrowedHandlesShouldNotBeEqual() {
      final ComponentResourceHandle owned = ComponentResourceHandle.own("file", 42);
      final ComponentResourceHandle borrowed = ComponentResourceHandle.borrow("file", 42);

      assertNotEquals(owned, borrowed, "Owned and borrowed handles should not be equal");
    }

    @Test
    @DisplayName("handles with different types should not be equal")
    void handlesWithDifferentTypesShouldNotBeEqual() {
      final ComponentResourceHandle h1 = ComponentResourceHandle.own("file", 42);
      final ComponentResourceHandle h2 = ComponentResourceHandle.own("socket", 42);

      assertNotEquals(h1, h2, "Handles with different types should not be equal");
    }

    @Test
    @DisplayName("handles with different indices should not be equal")
    void handlesWithDifferentIndicesShouldNotBeEqual() {
      final ComponentResourceHandle h1 = ComponentResourceHandle.own("file", 42);
      final ComponentResourceHandle h2 = ComponentResourceHandle.own("file", 43);

      assertNotEquals(h1, h2, "Handles with different indices should not be equal");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("owned handle toString should contain own")
    void ownedHandleToStringShouldContainOwn() {
      final ComponentResourceHandle handle = ComponentResourceHandle.own("file", 42);
      final String str = handle.toString();

      assertTrue(str.contains("own"), "Should contain 'own'");
      assertTrue(str.contains("file"), "Should contain resource type");
      assertTrue(str.contains("42"), "Should contain index");
    }

    @Test
    @DisplayName("borrowed handle toString should contain borrow")
    void borrowedHandleToStringShouldContainBorrow() {
      final ComponentResourceHandle handle = ComponentResourceHandle.borrow("file", 42);
      final String str = handle.toString();

      assertTrue(str.contains("borrow"), "Should contain 'borrow'");
      assertTrue(str.contains("file"), "Should contain resource type");
      assertTrue(str.contains("42"), "Should contain index");
    }
  }
}
