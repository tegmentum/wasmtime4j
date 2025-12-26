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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EncryptionMode} enum.
 *
 * <p>EncryptionMode defines block cipher encryption modes for symmetric encryption.
 */
@DisplayName("EncryptionMode Tests")
class EncryptionModeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeEnum() {
      assertTrue(EncryptionMode.class.isEnum(), "EncryptionMode should be an enum");
    }

    @Test
    @DisplayName("should have ECB constant")
    void shouldHaveEcbConstant() {
      assertNotNull(EncryptionMode.ECB, "ECB constant should exist");
    }

    @Test
    @DisplayName("should have CBC constant")
    void shouldHaveCbcConstant() {
      assertNotNull(EncryptionMode.CBC, "CBC constant should exist");
    }

    @Test
    @DisplayName("should have CTR constant")
    void shouldHaveCtrConstant() {
      assertNotNull(EncryptionMode.CTR, "CTR constant should exist");
    }

    @Test
    @DisplayName("should have GCM constant")
    void shouldHaveGcmConstant() {
      assertNotNull(EncryptionMode.GCM, "GCM constant should exist");
    }

    @Test
    @DisplayName("should have CCM constant")
    void shouldHaveCcmConstant() {
      assertNotNull(EncryptionMode.CCM, "CCM constant should exist");
    }

    @Test
    @DisplayName("should have 8 encryption modes")
    void shouldHave8EncryptionModes() {
      assertEquals(8, EncryptionMode.values().length, "Should have 8 encryption modes");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getModeName method")
    void shouldHaveGetModeNameMethod() throws NoSuchMethodException {
      final Method method = EncryptionMode.class.getMethod("getModeName");
      assertNotNull(method, "getModeName method should exist");
      assertEquals(String.class, method.getReturnType(), "getModeName should return String");
    }

    @Test
    @DisplayName("should have requiresIv method")
    void shouldHaveRequiresIvMethod() throws NoSuchMethodException {
      final Method method = EncryptionMode.class.getMethod("requiresIv");
      assertNotNull(method, "requiresIv method should exist");
      assertEquals(boolean.class, method.getReturnType(), "requiresIv should return boolean");
    }

    @Test
    @DisplayName("should have providesAuthentication method")
    void shouldHaveProvidesAuthenticationMethod() throws NoSuchMethodException {
      final Method method = EncryptionMode.class.getMethod("providesAuthentication");
      assertNotNull(method, "providesAuthentication method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "providesAuthentication should return boolean");
    }
  }

  @Nested
  @DisplayName("Mode Properties Tests")
  class ModePropertiesTests {

    @Test
    @DisplayName("ECB should have correct properties")
    void ecbShouldHaveCorrectProperties() {
      assertEquals("ECB", EncryptionMode.ECB.getModeName(), "Mode name should match");
      assertFalse(EncryptionMode.ECB.requiresIv(), "ECB should not require IV");
      assertFalse(
          EncryptionMode.ECB.providesAuthentication(), "ECB should not provide authentication");
    }

    @Test
    @DisplayName("CBC should have correct properties")
    void cbcShouldHaveCorrectProperties() {
      assertEquals("CBC", EncryptionMode.CBC.getModeName(), "Mode name should match");
      assertTrue(EncryptionMode.CBC.requiresIv(), "CBC should require IV");
      assertFalse(
          EncryptionMode.CBC.providesAuthentication(), "CBC should not provide authentication");
    }

    @Test
    @DisplayName("CTR should have correct properties")
    void ctrShouldHaveCorrectProperties() {
      assertEquals("CTR", EncryptionMode.CTR.getModeName(), "Mode name should match");
      assertTrue(EncryptionMode.CTR.requiresIv(), "CTR should require IV");
      assertFalse(
          EncryptionMode.CTR.providesAuthentication(), "CTR should not provide authentication");
    }

    @Test
    @DisplayName("GCM should have correct properties")
    void gcmShouldHaveCorrectProperties() {
      assertEquals("GCM", EncryptionMode.GCM.getModeName(), "Mode name should match");
      assertTrue(EncryptionMode.GCM.requiresIv(), "GCM should require IV");
      assertTrue(EncryptionMode.GCM.providesAuthentication(), "GCM should provide authentication");
    }

    @Test
    @DisplayName("CCM should have correct properties")
    void ccmShouldHaveCorrectProperties() {
      assertEquals("CCM", EncryptionMode.CCM.getModeName(), "Mode name should match");
      assertTrue(EncryptionMode.CCM.requiresIv(), "CCM should require IV");
      assertTrue(EncryptionMode.CCM.providesAuthentication(), "CCM should provide authentication");
    }

    @Test
    @DisplayName("OCB should have correct properties")
    void ocbShouldHaveCorrectProperties() {
      assertEquals("OCB", EncryptionMode.OCB.getModeName(), "Mode name should match");
      assertTrue(EncryptionMode.OCB.requiresIv(), "OCB should require IV");
      assertTrue(EncryptionMode.OCB.providesAuthentication(), "OCB should provide authentication");
    }

    @Test
    @DisplayName("SIV should have correct properties")
    void sivShouldHaveCorrectProperties() {
      assertEquals("SIV", EncryptionMode.SIV.getModeName(), "Mode name should match");
      assertTrue(EncryptionMode.SIV.requiresIv(), "SIV should require IV");
      assertTrue(EncryptionMode.SIV.providesAuthentication(), "SIV should provide authentication");
    }

    @Test
    @DisplayName("GCM_SIV should have correct properties")
    void gcmSivShouldHaveCorrectProperties() {
      assertEquals("GCM-SIV", EncryptionMode.GCM_SIV.getModeName(), "Mode name should match");
      assertTrue(EncryptionMode.GCM_SIV.requiresIv(), "GCM-SIV should require IV");
      assertTrue(
          EncryptionMode.GCM_SIV.providesAuthentication(), "GCM-SIV should provide authentication");
    }
  }
}
