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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link RuntimeInfo} class.
 *
 * <p>This test class verifies the construction and behavior of the RuntimeInfo value object, which
 * provides metadata about the WebAssembly runtime implementation.
 */
@DisplayName("RuntimeInfo Tests")
class RuntimeInfoTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should set all fields")
    void constructorShouldSetAllFields() {
      final RuntimeInfo info =
          new RuntimeInfo(
              "wasmtime4j", "1.0.0", "36.0.2", RuntimeType.JNI, "21.0.1", "linux-x86_64");

      assertEquals("wasmtime4j", info.getRuntimeName(), "Runtime name should match");
      assertEquals("1.0.0", info.getRuntimeVersion(), "Runtime version should match");
      assertEquals("36.0.2", info.getWasmtimeVersion(), "Wasmtime version should match");
      assertEquals(RuntimeType.JNI, info.getRuntimeType(), "Runtime type should be JNI");
      assertEquals("21.0.1", info.getJavaVersion(), "Java version should match");
      assertEquals("linux-x86_64", info.getPlatformInfo(), "Platform info should match");
    }

    @Test
    @DisplayName("Constructor should accept Panama runtime type")
    void constructorShouldAcceptPanamaRuntimeType() {
      final RuntimeInfo info =
          new RuntimeInfo(
              "wasmtime4j-panama",
              "1.0.0",
              "36.0.2",
              RuntimeType.PANAMA,
              "23.0.1",
              "darwin-aarch64");

      assertEquals(RuntimeType.PANAMA, info.getRuntimeType(), "Runtime type should be PANAMA");
    }

    @Test
    @DisplayName("Constructor should accept null values")
    void constructorShouldAcceptNullValues() {
      final RuntimeInfo info = new RuntimeInfo(null, null, null, null, null, null);

      assertEquals(null, info.getRuntimeName(), "Runtime name should be null");
      assertEquals(null, info.getRuntimeVersion(), "Runtime version should be null");
      assertEquals(null, info.getWasmtimeVersion(), "Wasmtime version should be null");
      assertEquals(null, info.getRuntimeType(), "Runtime type should be null");
      assertEquals(null, info.getJavaVersion(), "Java version should be null");
      assertEquals(null, info.getPlatformInfo(), "Platform info should be null");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("getRuntimeName should return runtime name")
    void getRuntimeNameShouldReturnRuntimeName() {
      final RuntimeInfo info = createTestRuntimeInfo();

      assertEquals(
          "wasmtime4j-jni", info.getRuntimeName(), "getRuntimeName should return 'wasmtime4j-jni'");
    }

    @Test
    @DisplayName("getRuntimeVersion should return runtime version")
    void getRuntimeVersionShouldReturnRuntimeVersion() {
      final RuntimeInfo info = createTestRuntimeInfo();

      assertEquals(
          "1.0.0-SNAPSHOT",
          info.getRuntimeVersion(),
          "getRuntimeVersion should return '1.0.0-SNAPSHOT'");
    }

    @Test
    @DisplayName("getWasmtimeVersion should return Wasmtime version")
    void getWasmtimeVersionShouldReturnWasmtimeVersion() {
      final RuntimeInfo info = createTestRuntimeInfo();

      assertEquals(
          "36.0.2", info.getWasmtimeVersion(), "getWasmtimeVersion should return '36.0.2'");
    }

    @Test
    @DisplayName("getRuntimeType should return runtime type")
    void getRuntimeTypeShouldReturnRuntimeType() {
      final RuntimeInfo info = createTestRuntimeInfo();

      assertEquals(RuntimeType.JNI, info.getRuntimeType(), "getRuntimeType should return JNI");
    }

    @Test
    @DisplayName("getJavaVersion should return Java version")
    void getJavaVersionShouldReturnJavaVersion() {
      final RuntimeInfo info = createTestRuntimeInfo();

      assertEquals("17.0.9", info.getJavaVersion(), "getJavaVersion should return '17.0.9'");
    }

    @Test
    @DisplayName("getPlatformInfo should return platform info")
    void getPlatformInfoShouldReturnPlatformInfo() {
      final RuntimeInfo info = createTestRuntimeInfo();

      assertEquals(
          "windows-x86_64",
          info.getPlatformInfo(),
          "getPlatformInfo should return 'windows-x86_64'");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include runtime name")
    void toStringShouldIncludeRuntimeName() {
      final RuntimeInfo info = createTestRuntimeInfo();

      final String result = info.toString();

      assertTrue(result.contains("wasmtime4j-jni"), "toString should contain runtime name");
    }

    @Test
    @DisplayName("toString should include runtime version")
    void toStringShouldIncludeRuntimeVersion() {
      final RuntimeInfo info = createTestRuntimeInfo();

      final String result = info.toString();

      assertTrue(result.contains("1.0.0-SNAPSHOT"), "toString should contain runtime version");
    }

    @Test
    @DisplayName("toString should include Wasmtime version")
    void toStringShouldIncludeWasmtimeVersion() {
      final RuntimeInfo info = createTestRuntimeInfo();

      final String result = info.toString();

      assertTrue(result.contains("36.0.2"), "toString should contain Wasmtime version");
    }

    @Test
    @DisplayName("toString should include runtime type")
    void toStringShouldIncludeRuntimeType() {
      final RuntimeInfo info = createTestRuntimeInfo();

      final String result = info.toString();

      assertTrue(result.contains("JNI"), "toString should contain runtime type");
    }

    @Test
    @DisplayName("toString should include Java version")
    void toStringShouldIncludeJavaVersion() {
      final RuntimeInfo info = createTestRuntimeInfo();

      final String result = info.toString();

      assertTrue(result.contains("17.0.9"), "toString should contain Java version");
    }

    @Test
    @DisplayName("toString should include platform info")
    void toStringShouldIncludePlatformInfo() {
      final RuntimeInfo info = createTestRuntimeInfo();

      final String result = info.toString();

      assertTrue(result.contains("windows-x86_64"), "toString should contain platform info");
    }

    @Test
    @DisplayName("toString should return non-null value")
    void toStringShouldReturnNonNullValue() {
      final RuntimeInfo info = createTestRuntimeInfo();

      final String result = info.toString();

      assertNotNull(result, "toString should not return null");
    }

    @Test
    @DisplayName("toString should contain RuntimeInfo identifier")
    void toStringShouldContainRuntimeInfoIdentifier() {
      final RuntimeInfo info = createTestRuntimeInfo();

      final String result = info.toString();

      assertTrue(result.contains("RuntimeInfo"), "toString should contain 'RuntimeInfo'");
    }
  }

  @Nested
  @DisplayName("Usage Tests")
  class UsageTests {

    @Test
    @DisplayName("Should be able to compare runtime types")
    void shouldBeAbleToCompareRuntimeTypes() {
      final RuntimeInfo jniInfo =
          new RuntimeInfo("jni-runtime", "1.0", "36.0", RuntimeType.JNI, "17", "linux");
      final RuntimeInfo panamaInfo =
          new RuntimeInfo("panama-runtime", "1.0", "36.0", RuntimeType.PANAMA, "23", "linux");

      assertEquals(RuntimeType.JNI, jniInfo.getRuntimeType(), "JNI runtime should have JNI type");
      assertEquals(
          RuntimeType.PANAMA,
          panamaInfo.getRuntimeType(),
          "Panama runtime should have PANAMA type");
    }

    @Test
    @DisplayName("Should support different platform configurations")
    void shouldSupportDifferentPlatformConfigurations() {
      final RuntimeInfo linuxX64 =
          new RuntimeInfo("rt", "1.0", "36.0", RuntimeType.JNI, "17", "linux-x86_64");
      final RuntimeInfo linuxArm =
          new RuntimeInfo("rt", "1.0", "36.0", RuntimeType.JNI, "17", "linux-aarch64");
      final RuntimeInfo macosArm =
          new RuntimeInfo("rt", "1.0", "36.0", RuntimeType.PANAMA, "23", "darwin-aarch64");
      final RuntimeInfo windowsX64 =
          new RuntimeInfo("rt", "1.0", "36.0", RuntimeType.JNI, "17", "windows-x86_64");

      assertEquals("linux-x86_64", linuxX64.getPlatformInfo(), "Linux x64 platform should match");
      assertEquals("linux-aarch64", linuxArm.getPlatformInfo(), "Linux ARM platform should match");
      assertEquals("darwin-aarch64", macosArm.getPlatformInfo(), "macOS ARM platform should match");
      assertEquals(
          "windows-x86_64", windowsX64.getPlatformInfo(), "Windows x64 platform should match");
    }
  }

  /**
   * Creates a test RuntimeInfo instance with common values.
   *
   * @return a RuntimeInfo for testing
   */
  private RuntimeInfo createTestRuntimeInfo() {
    return new RuntimeInfo(
        "wasmtime4j-jni", "1.0.0-SNAPSHOT", "36.0.2", RuntimeType.JNI, "17.0.9", "windows-x86_64");
  }
}
