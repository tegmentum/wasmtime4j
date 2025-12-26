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

package ai.tegmentum.wasmtime4j.wasi.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CryptoKey} interface.
 *
 * <p>CryptoKey represents a cryptographic key in WASI-crypto.
 */
@DisplayName("CryptoKey Tests")
class CryptoKeyTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(CryptoKey.class.isInterface(), "CryptoKey should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(CryptoKey.class.getModifiers()), "CryptoKey should be public");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(CryptoKey.class),
          "CryptoKey should extend AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      final Method method = CryptoKey.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(String.class, method.getReturnType(), "getId should return String");
    }

    @Test
    @DisplayName("should have getKeyType method")
    void shouldHaveGetKeyTypeMethod() throws NoSuchMethodException {
      final Method method = CryptoKey.class.getMethod("getKeyType");
      assertNotNull(method, "getKeyType method should exist");
      assertEquals(
          CryptoKeyType.class, method.getReturnType(), "getKeyType should return CryptoKeyType");
    }

    @Test
    @DisplayName("should have getAlgorithm method")
    void shouldHaveGetAlgorithmMethod() throws NoSuchMethodException {
      final Method method = CryptoKey.class.getMethod("getAlgorithm");
      assertNotNull(method, "getAlgorithm method should exist");
      assertEquals(String.class, method.getReturnType(), "getAlgorithm should return String");
    }

    @Test
    @DisplayName("should have getKeySizeBits method")
    void shouldHaveGetKeySizeBitsMethod() throws NoSuchMethodException {
      final Method method = CryptoKey.class.getMethod("getKeySizeBits");
      assertNotNull(method, "getKeySizeBits method should exist");
      assertEquals(int.class, method.getReturnType(), "getKeySizeBits should return int");
    }

    @Test
    @DisplayName("should have isExportable method")
    void shouldHaveIsExportableMethod() throws NoSuchMethodException {
      final Method method = CryptoKey.class.getMethod("isExportable");
      assertNotNull(method, "isExportable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isExportable should return boolean");
    }

    @Test
    @DisplayName("should have exportPublicKey method")
    void shouldHaveExportPublicKeyMethod() throws NoSuchMethodException {
      final Method method = CryptoKey.class.getMethod("exportPublicKey");
      assertNotNull(method, "exportPublicKey method should exist");
      assertEquals(
          Optional.class, method.getReturnType(), "exportPublicKey should return Optional");
    }

    @Test
    @DisplayName("should have extractPublicKey method")
    void shouldHaveExtractPublicKeyMethod() throws NoSuchMethodException {
      final Method method = CryptoKey.class.getMethod("extractPublicKey");
      assertNotNull(method, "extractPublicKey method should exist");
      assertEquals(
          CryptoKey.class, method.getReturnType(), "extractPublicKey should return CryptoKey");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = CryptoKey.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }

    @Test
    @DisplayName("should have close method from AutoCloseable")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = CryptoKey.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }
  }
}
