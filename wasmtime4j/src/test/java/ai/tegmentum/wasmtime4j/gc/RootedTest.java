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
 * Tests for {@link Rooted} class.
 *
 * <p>Rooted represents a rooted reference to a GC-managed WebAssembly object.
 */
@DisplayName("Rooted Tests")
class RootedTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(Modifier.isPublic(Rooted.class.getModifiers()), "Rooted should be public");
      assertTrue(Modifier.isFinal(Rooted.class.getModifiers()), "Rooted should be final");
    }

    @Test
    @DisplayName("should have public constructor")
    void shouldHavePublicConstructor() throws NoSuchMethodException {
      final var constructor = Rooted.class.getConstructor(Object.class, long.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have get method")
    void shouldHaveGetMethod() throws NoSuchMethodException {
      final Method method = Rooted.class.getMethod("get", ai.tegmentum.wasmtime4j.Store.class);
      assertNotNull(method, "get method should exist");
      assertEquals(Object.class, method.getReturnType(), "get should return generic type");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = Rooted.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }

    @Test
    @DisplayName("should have getRootId method")
    void shouldHaveGetRootIdMethod() throws NoSuchMethodException {
      final Method method = Rooted.class.getMethod("getRootId");
      assertNotNull(method, "getRootId method should exist");
      assertEquals(long.class, method.getReturnType(), "getRootId should return long");
    }

    @Test
    @DisplayName("should have unroot method")
    void shouldHaveUnrootMethod() throws NoSuchMethodException {
      final Method method = Rooted.class.getMethod("unroot", ai.tegmentum.wasmtime4j.Store.class);
      assertNotNull(method, "unroot method should exist");
    }

    @Test
    @DisplayName("should have reroot method")
    void shouldHaveRerootMethod() throws NoSuchMethodException {
      final Method method =
          Rooted.class.getMethod("reroot", ai.tegmentum.wasmtime4j.Store.class, Object.class);
      assertNotNull(method, "reroot method should exist");
      assertEquals(Rooted.class, method.getReturnType(), "reroot should return Rooted");
    }

    @Test
    @DisplayName("should have toManualRoot method")
    void shouldHaveToManualRootMethod() throws NoSuchMethodException {
      final Method method =
          Rooted.class.getMethod("toManualRoot", ai.tegmentum.wasmtime4j.Store.class);
      assertNotNull(method, "toManualRoot method should exist");
      assertEquals(
          Rooted.ManualRoot.class, method.getReturnType(), "toManualRoot should return ManualRoot");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create rooted with value and root id")
    void shouldCreateRootedWithValueAndRootId() {
      final String testValue = "test";
      final long rootId = 42L;
      final Rooted<String> rooted = new Rooted<>(testValue, rootId);

      assertNotNull(rooted, "Rooted should be created");
      assertEquals(rootId, rooted.getRootId(), "Root ID should match");
      assertTrue(rooted.isValid(), "Rooted should be valid initially");
    }

    @Test
    @DisplayName("should throw NPE for null value")
    void shouldThrowNpeForNullValue() {
      assertThrows(
          NullPointerException.class,
          () -> new Rooted<>(null, 1L),
          "Constructor should throw NPE for null value");
    }
  }

  @Nested
  @DisplayName("Validity Tests")
  class ValidityTests {

    @Test
    @DisplayName("new rooted should be valid")
    void newRootedShouldBeValid() {
      final Rooted<String> rooted = new Rooted<>("test", 1L);
      assertTrue(rooted.isValid(), "New rooted should be valid");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same value and rootId should be equal")
    void sameValueAndRootIdShouldBeEqual() {
      final Rooted<String> rooted1 = new Rooted<>("test", 1L);
      final Rooted<String> rooted2 = new Rooted<>("test", 1L);
      assertEquals(rooted1, rooted2, "Same value and rootId should be equal");
    }

    @Test
    @DisplayName("different rootId should not be equal")
    void differentRootIdShouldNotBeEqual() {
      final Rooted<String> rooted1 = new Rooted<>("test", 1L);
      final Rooted<String> rooted2 = new Rooted<>("test", 2L);
      assertNotEquals(rooted1, rooted2, "Different rootId should not be equal");
    }

    @Test
    @DisplayName("different value should not be equal")
    void differentValueShouldNotBeEqual() {
      final Rooted<String> rooted1 = new Rooted<>("test1", 1L);
      final Rooted<String> rooted2 = new Rooted<>("test2", 1L);
      assertNotEquals(rooted1, rooted2, "Different value should not be equal");
    }

    @Test
    @DisplayName("equals with non-Rooted should return false")
    void equalsWithNonRootedShouldReturnFalse() {
      final Rooted<String> rooted = new Rooted<>("test", 1L);
      assertFalse(rooted.equals("not a Rooted"), "equals with non-Rooted should return false");
    }

    @Test
    @DisplayName("equal objects should have same hashCode")
    void equalObjectsShouldHaveSameHashCode() {
      final Rooted<String> rooted1 = new Rooted<>("test", 1L);
      final Rooted<String> rooted2 = new Rooted<>("test", 1L);
      assertEquals(
          rooted1.hashCode(), rooted2.hashCode(), "Equal objects should have same hashCode");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain 'Rooted'")
    void toStringShouldContainRooted() {
      final Rooted<String> rooted = new Rooted<>("test", 1L);
      final String str = rooted.toString();
      assertTrue(str.contains("Rooted"), "toString should contain 'Rooted': " + str);
    }

    @Test
    @DisplayName("toString should contain rootId")
    void toStringShouldContainRootId() {
      final Rooted<String> rooted = new Rooted<>("test", 42L);
      final String str = rooted.toString();
      assertTrue(str.contains("42"), "toString should contain rootId: " + str);
    }
  }

  @Nested
  @DisplayName("ManualRoot Nested Class Tests")
  class ManualRootNestedClassTests {

    @Test
    @DisplayName("ManualRoot should be public static final class")
    void manualRootShouldBePublicStaticFinalClass() {
      assertTrue(
          Modifier.isPublic(Rooted.ManualRoot.class.getModifiers()), "ManualRoot should be public");
      assertTrue(
          Modifier.isStatic(Rooted.ManualRoot.class.getModifiers()), "ManualRoot should be static");
      assertTrue(
          Modifier.isFinal(Rooted.ManualRoot.class.getModifiers()), "ManualRoot should be final");
    }

    @Test
    @DisplayName("ManualRoot should have get method")
    void manualRootShouldHaveGetMethod() throws NoSuchMethodException {
      final Method method = Rooted.ManualRoot.class.getMethod("get");
      assertNotNull(method, "get method should exist");
      assertEquals(Object.class, method.getReturnType(), "get should return generic type");
    }

    @Test
    @DisplayName("ManualRoot should have release method")
    void manualRootShouldHaveReleaseMethod() throws NoSuchMethodException {
      final Method method = Rooted.ManualRoot.class.getMethod("release");
      assertNotNull(method, "release method should exist");
    }

    @Test
    @DisplayName("ManualRoot should have isValid method")
    void manualRootShouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = Rooted.ManualRoot.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }

    @Test
    @DisplayName("ManualRoot should have getRootId method")
    void manualRootShouldHaveGetRootIdMethod() throws NoSuchMethodException {
      final Method method = Rooted.ManualRoot.class.getMethod("getRootId");
      assertNotNull(method, "getRootId method should exist");
      assertEquals(long.class, method.getReturnType(), "getRootId should return long");
    }
  }
}
