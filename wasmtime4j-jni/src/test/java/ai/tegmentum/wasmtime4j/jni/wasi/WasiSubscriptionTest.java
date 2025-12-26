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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiSubscription} class.
 *
 * <p>WasiSubscription represents an event subscription for WASI polling operations.
 */
@DisplayName("WasiSubscription Class Tests")
class WasiSubscriptionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create subscription with default constructor")
    void shouldCreateSubscriptionWithDefaultConstructor() {
      final WasiSubscription subscription = new WasiSubscription();
      assertNotNull(subscription, "WasiSubscription should be created");
    }

    @Test
    @DisplayName("should have default zero values")
    void shouldHaveDefaultZeroValues() {
      final WasiSubscription subscription = new WasiSubscription();
      assertEquals(0L, subscription.getUserData(), "userData should default to 0");
      assertEquals(0, subscription.getType(), "type should default to 0");
      assertEquals(0, subscription.getFd(), "fd should default to 0");
      assertEquals(0, subscription.getFlags(), "flags should default to 0");
    }
  }

  @Nested
  @DisplayName("UserData Tests")
  class UserDataTests {

    @Test
    @DisplayName("should set and get user data")
    void shouldSetAndGetUserData() {
      final WasiSubscription subscription = new WasiSubscription();
      subscription.setUserData(12345L);
      assertEquals(12345L, subscription.getUserData());
    }

    @Test
    @DisplayName("should handle Long.MAX_VALUE for user data")
    void shouldHandleMaxValueForUserData() {
      final WasiSubscription subscription = new WasiSubscription();
      subscription.setUserData(Long.MAX_VALUE);
      assertEquals(Long.MAX_VALUE, subscription.getUserData());
    }

    @Test
    @DisplayName("should handle negative user data")
    void shouldHandleNegativeUserData() {
      final WasiSubscription subscription = new WasiSubscription();
      subscription.setUserData(-1L);
      assertEquals(-1L, subscription.getUserData());
    }
  }

  @Nested
  @DisplayName("Type Tests")
  class TypeTests {

    @Test
    @DisplayName("should set and get type")
    void shouldSetAndGetType() {
      final WasiSubscription subscription = new WasiSubscription();
      subscription.setType(1);
      assertEquals(1, subscription.getType());
    }

    @Test
    @DisplayName("should set type 0 for CLOCK")
    void shouldSetTypeZeroForClock() {
      final WasiSubscription subscription = new WasiSubscription();
      subscription.setType(0); // CLOCK
      assertEquals(0, subscription.getType());
    }

    @Test
    @DisplayName("should set type 1 for FD_READ")
    void shouldSetTypeOneForFdRead() {
      final WasiSubscription subscription = new WasiSubscription();
      subscription.setType(1); // FD_READ
      assertEquals(1, subscription.getType());
    }

    @Test
    @DisplayName("should set type 2 for FD_WRITE")
    void shouldSetTypeTwoForFdWrite() {
      final WasiSubscription subscription = new WasiSubscription();
      subscription.setType(2); // FD_WRITE
      assertEquals(2, subscription.getType());
    }
  }

  @Nested
  @DisplayName("File Descriptor Tests")
  class FileDescriptorTests {

    @Test
    @DisplayName("should set and get file descriptor")
    void shouldSetAndGetFileDescriptor() {
      final WasiSubscription subscription = new WasiSubscription();
      subscription.setFd(3);
      assertEquals(3, subscription.getFd());
    }

    @Test
    @DisplayName("should set standard input file descriptor")
    void shouldSetStandardInputFileDescriptor() {
      final WasiSubscription subscription = new WasiSubscription();
      subscription.setFd(0); // stdin
      assertEquals(0, subscription.getFd());
    }

    @Test
    @DisplayName("should set standard output file descriptor")
    void shouldSetStandardOutputFileDescriptor() {
      final WasiSubscription subscription = new WasiSubscription();
      subscription.setFd(1); // stdout
      assertEquals(1, subscription.getFd());
    }

    @Test
    @DisplayName("should set standard error file descriptor")
    void shouldSetStandardErrorFileDescriptor() {
      final WasiSubscription subscription = new WasiSubscription();
      subscription.setFd(2); // stderr
      assertEquals(2, subscription.getFd());
    }
  }

  @Nested
  @DisplayName("Flags Tests")
  class FlagsTests {

    @Test
    @DisplayName("should set and get flags")
    void shouldSetAndGetFlags() {
      final WasiSubscription subscription = new WasiSubscription();
      subscription.setFlags(1);
      assertEquals(1, subscription.getFlags());
    }

    @Test
    @DisplayName("should handle multiple flag bits")
    void shouldHandleMultipleFlagBits() {
      final WasiSubscription subscription = new WasiSubscription();
      subscription.setFlags(0x0F);
      assertEquals(15, subscription.getFlags());
    }
  }

  @Nested
  @DisplayName("SetFdReadwrite Tests")
  class SetFdReadwriteTests {

    @Test
    @DisplayName("should set fd and flags via setFdReadwrite")
    void shouldSetFdAndFlagsViaSetFdReadwrite() {
      final WasiSubscription subscription = new WasiSubscription();
      subscription.setFdReadwrite(5, 1);

      assertEquals(5, subscription.getFd(), "fd should be set");
      assertEquals(1, subscription.getFlags(), "flags should be set");
    }

    @Test
    @DisplayName("setFdReadwrite should work for stdin read")
    void setFdReadwriteShouldWorkForStdinRead() {
      final WasiSubscription subscription = new WasiSubscription();
      subscription.setUserData(100L);
      subscription.setType(1); // FD_READ
      subscription.setFdReadwrite(0, 0); // stdin, no special flags

      assertEquals(100L, subscription.getUserData());
      assertEquals(1, subscription.getType());
      assertEquals(0, subscription.getFd());
      assertEquals(0, subscription.getFlags());
    }

    @Test
    @DisplayName("setFdReadwrite should work for stdout write")
    void setFdReadwriteShouldWorkForStdoutWrite() {
      final WasiSubscription subscription = new WasiSubscription();
      subscription.setUserData(200L);
      subscription.setType(2); // FD_WRITE
      subscription.setFdReadwrite(1, 0); // stdout, no special flags

      assertEquals(200L, subscription.getUserData());
      assertEquals(2, subscription.getType());
      assertEquals(1, subscription.getFd());
      assertEquals(0, subscription.getFlags());
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include all fields")
    void toStringShouldIncludeAllFields() {
      final WasiSubscription subscription = new WasiSubscription();
      subscription.setUserData(12345L);
      subscription.setType(1);
      subscription.setFd(3);
      subscription.setFlags(1);

      final String str = subscription.toString();

      assertTrue(str.contains("WasiSubscription"), "Should contain class name");
      assertTrue(str.contains("userData=12345"), "Should contain userData");
      assertTrue(str.contains("type=1"), "Should contain type");
      assertTrue(str.contains("fd=3"), "Should contain fd");
      assertTrue(str.contains("flags=1"), "Should contain flags");
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiSubscription should be final class")
    void wasiSubscriptionShouldBeFinalClass() {
      assertTrue(java.lang.reflect.Modifier.isFinal(WasiSubscription.class.getModifiers()),
          "WasiSubscription should be final");
    }

    @Test
    @DisplayName("should have getter and setter for all fields")
    void shouldHaveGetterAndSetterForAllFields() throws NoSuchMethodException {
      assertNotNull(WasiSubscription.class.getMethod("getUserData"),
          "Should have getUserData method");
      assertNotNull(WasiSubscription.class.getMethod("setUserData", long.class),
          "Should have setUserData method");
      assertNotNull(WasiSubscription.class.getMethod("getType"),
          "Should have getType method");
      assertNotNull(WasiSubscription.class.getMethod("setType", int.class),
          "Should have setType method");
      assertNotNull(WasiSubscription.class.getMethod("getFd"),
          "Should have getFd method");
      assertNotNull(WasiSubscription.class.getMethod("setFd", int.class),
          "Should have setFd method");
      assertNotNull(WasiSubscription.class.getMethod("getFlags"),
          "Should have getFlags method");
      assertNotNull(WasiSubscription.class.getMethod("setFlags", int.class),
          "Should have setFlags method");
    }

    @Test
    @DisplayName("should have setFdReadwrite convenience method")
    void shouldHaveSetFdReadwriteConvenienceMethod() throws NoSuchMethodException {
      assertNotNull(WasiSubscription.class.getMethod("setFdReadwrite", int.class, int.class),
          "Should have setFdReadwrite method");
    }
  }
}
