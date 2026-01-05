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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasiSocketAddress class.
 *
 * <p>WasiSocketAddress represents a WASI Preview 2 IP socket address (IP address and port number).
 */
@DisplayName("WasiSocketAddress Class Tests")
class WasiSocketAddressTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class, not an interface")
    void shouldBeAClass() {
      assertFalse(WasiSocketAddress.class.isInterface(), "WasiSocketAddress should be a class");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiSocketAddress.class.getModifiers()),
          "WasiSocketAddress should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(WasiSocketAddress.class.getModifiers()),
          "WasiSocketAddress should be final");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Object")
    void shouldExtendObject() {
      assertEquals(
          Object.class,
          WasiSocketAddress.class.getSuperclass(),
          "WasiSocketAddress should extend Object");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          WasiSocketAddress.class.getInterfaces().length,
          "WasiSocketAddress should not implement any interfaces");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have family field")
    void shouldHaveFamilyField() throws NoSuchFieldException {
      Field field = WasiSocketAddress.class.getDeclaredField("family");
      assertNotNull(field, "family field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(WasiAddressFamily.class, field.getType(), "Should be WasiAddressFamily");
    }

    @Test
    @DisplayName("should have address field")
    void shouldHaveAddressField() throws NoSuchFieldException {
      Field field = WasiSocketAddress.class.getDeclaredField("address");
      assertNotNull(field, "address field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(byte[].class, field.getType(), "Should be byte[]");
    }

    @Test
    @DisplayName("should have port field")
    void shouldHavePortField() throws NoSuchFieldException {
      Field field = WasiSocketAddress.class.getDeclaredField("port");
      assertNotNull(field, "port field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(int.class, field.getType(), "Should be int");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() {
      Constructor<?>[] constructors = WasiSocketAddress.class.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly 1 constructor");
      assertTrue(
          Modifier.isPrivate(constructors[0].getModifiers()), "Constructor should be private");
    }
  }

  // ========================================================================
  // Static Factory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have ipv4 factory method")
    void shouldHaveIpv4Method() throws NoSuchMethodException {
      Method method = WasiSocketAddress.class.getMethod("ipv4", byte[].class, int.class);
      assertNotNull(method, "ipv4 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          WasiSocketAddress.class, method.getReturnType(), "Should return WasiSocketAddress");
    }

    @Test
    @DisplayName("should have ipv6 factory method")
    void shouldHaveIpv6Method() throws NoSuchMethodException {
      Method method = WasiSocketAddress.class.getMethod("ipv6", byte[].class, int.class);
      assertNotNull(method, "ipv6 method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          WasiSocketAddress.class, method.getReturnType(), "Should return WasiSocketAddress");
    }

    @Test
    @DisplayName("should have fromInetSocketAddress factory method")
    void shouldHaveFromInetSocketAddressMethod() throws NoSuchMethodException {
      Method method =
          WasiSocketAddress.class.getMethod("fromInetSocketAddress", InetSocketAddress.class);
      assertNotNull(method, "fromInetSocketAddress method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          WasiSocketAddress.class, method.getReturnType(), "Should return WasiSocketAddress");
    }
  }

  // ========================================================================
  // Instance Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Instance Method Tests")
  class InstanceMethodTests {

    @Test
    @DisplayName("should have getFamily method")
    void shouldHaveGetFamilyMethod() throws NoSuchMethodException {
      Method method = WasiSocketAddress.class.getMethod("getFamily");
      assertNotNull(method, "getFamily method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          WasiAddressFamily.class, method.getReturnType(), "Should return WasiAddressFamily");
    }

    @Test
    @DisplayName("should have getAddress method")
    void shouldHaveGetAddressMethod() throws NoSuchMethodException {
      Method method = WasiSocketAddress.class.getMethod("getAddress");
      assertNotNull(method, "getAddress method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
    }

    @Test
    @DisplayName("should have getPort method")
    void shouldHaveGetPortMethod() throws NoSuchMethodException {
      Method method = WasiSocketAddress.class.getMethod("getPort");
      assertNotNull(method, "getPort method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have toInetSocketAddress method")
    void shouldHaveToInetSocketAddressMethod() throws NoSuchMethodException {
      Method method = WasiSocketAddress.class.getMethod("toInetSocketAddress");
      assertNotNull(method, "toInetSocketAddress method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          InetSocketAddress.class, method.getReturnType(), "Should return InetSocketAddress");
    }
  }

  // ========================================================================
  // Object Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Object Method Tests")
  class ObjectMethodTests {

    @Test
    @DisplayName("should override equals")
    void shouldOverrideEquals() throws NoSuchMethodException {
      Method method = WasiSocketAddress.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(
          WasiSocketAddress.class,
          method.getDeclaringClass(),
          "equals should be declared in WasiSocketAddress");
    }

    @Test
    @DisplayName("should override hashCode")
    void shouldOverrideHashCode() throws NoSuchMethodException {
      Method method = WasiSocketAddress.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
      assertEquals(
          WasiSocketAddress.class,
          method.getDeclaringClass(),
          "hashCode should be declared in WasiSocketAddress");
    }

    @Test
    @DisplayName("should override toString")
    void shouldOverrideToString() throws NoSuchMethodException {
      Method method = WasiSocketAddress.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(
          WasiSocketAddress.class,
          method.getDeclaringClass(),
          "toString should be declared in WasiSocketAddress");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have no nested classes")
    void shouldHaveNoNestedClasses() {
      assertEquals(
          0,
          WasiSocketAddress.class.getDeclaredClasses().length,
          "WasiSocketAddress should have no nested classes");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have expected public methods")
    void shouldHaveExpectedPublicMethods() {
      long publicMethodCount =
          java.util.Arrays.stream(WasiSocketAddress.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();
      assertTrue(publicMethodCount >= 7, "Should have at least 7 public methods");
    }
  }
}
