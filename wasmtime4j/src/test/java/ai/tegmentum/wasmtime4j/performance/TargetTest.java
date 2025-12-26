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

package ai.tegmentum.wasmtime4j.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Target} class.
 *
 * <p>Target provides target platform information for WebAssembly compilation.
 */
@DisplayName("Target Tests")
class TargetTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(Target.class.getModifiers()), "Target should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(Target.class.getModifiers()), "Target should be public");
    }

    @Test
    @DisplayName("should have constructor with all parameters")
    void shouldHaveConstructorWithAllParameters() throws NoSuchMethodException {
      final Constructor<?> constructor =
          Target.class.getConstructor(
              String.class, String.class, String.class, Set.class, boolean.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getArchitecture method")
    void shouldHaveGetArchitectureMethod() throws NoSuchMethodException {
      final Method method = Target.class.getMethod("getArchitecture");
      assertNotNull(method, "getArchitecture method should exist");
      assertEquals(String.class, method.getReturnType(), "getArchitecture should return String");
    }

    @Test
    @DisplayName("should have getOperatingSystem method")
    void shouldHaveGetOperatingSystemMethod() throws NoSuchMethodException {
      final Method method = Target.class.getMethod("getOperatingSystem");
      assertNotNull(method, "getOperatingSystem method should exist");
      assertEquals(String.class, method.getReturnType(), "getOperatingSystem should return String");
    }

    @Test
    @DisplayName("should have getAbi method")
    void shouldHaveGetAbiMethod() throws NoSuchMethodException {
      final Method method = Target.class.getMethod("getAbi");
      assertNotNull(method, "getAbi method should exist");
      assertEquals(String.class, method.getReturnType(), "getAbi should return String");
    }

    @Test
    @DisplayName("should have getCpuFeatures method")
    void shouldHaveGetCpuFeaturesMethod() throws NoSuchMethodException {
      final Method method = Target.class.getMethod("getCpuFeatures");
      assertNotNull(method, "getCpuFeatures method should exist");
      assertEquals(Set.class, method.getReturnType(), "getCpuFeatures should return Set");
    }

    @Test
    @DisplayName("should have is64Bit method")
    void shouldHaveIs64BitMethod() throws NoSuchMethodException {
      final Method method = Target.class.getMethod("is64Bit");
      assertNotNull(method, "is64Bit method should exist");
      assertEquals(boolean.class, method.getReturnType(), "is64Bit should return boolean");
    }
  }

  @Nested
  @DisplayName("Query Method Tests")
  class QueryMethodTests {

    @Test
    @DisplayName("should have hasCpuFeature method")
    void shouldHaveHasCpuFeatureMethod() throws NoSuchMethodException {
      final Method method = Target.class.getMethod("hasCpuFeature", String.class);
      assertNotNull(method, "hasCpuFeature method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasCpuFeature should return boolean");
    }

    @Test
    @DisplayName("should have getTargetTriple method")
    void shouldHaveGetTargetTripleMethod() throws NoSuchMethodException {
      final Method method = Target.class.getMethod("getTargetTriple");
      assertNotNull(method, "getTargetTriple method should exist");
      assertEquals(String.class, method.getReturnType(), "getTargetTriple should return String");
    }

    @Test
    @DisplayName("should have isX86 method")
    void shouldHaveIsX86Method() throws NoSuchMethodException {
      final Method method = Target.class.getMethod("isX86");
      assertNotNull(method, "isX86 method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isX86 should return boolean");
    }

    @Test
    @DisplayName("should have isArm method")
    void shouldHaveIsArmMethod() throws NoSuchMethodException {
      final Method method = Target.class.getMethod("isArm");
      assertNotNull(method, "isArm method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isArm should return boolean");
    }

    @Test
    @DisplayName("should have hasSimdSupport method")
    void shouldHaveHasSimdSupportMethod() throws NoSuchMethodException {
      final Method method = Target.class.getMethod("hasSimdSupport");
      assertNotNull(method, "hasSimdSupport method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasSimdSupport should return boolean");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = Target.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = Target.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
    }
  }

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    @Test
    @DisplayName("should create instance with valid parameters")
    void shouldCreateInstanceWithValidParameters() {
      final Set<String> cpuFeatures = Set.of("sse2", "avx", "avx2");
      final Target target = new Target("x86_64", "linux", "gnu", cpuFeatures, true);

      assertEquals("x86_64", target.getArchitecture(), "Architecture should match");
      assertEquals("linux", target.getOperatingSystem(), "OS should match");
      assertEquals("gnu", target.getAbi(), "ABI should match");
      assertEquals(cpuFeatures, target.getCpuFeatures(), "CPU features should match");
      assertTrue(target.is64Bit(), "Should be 64-bit");
    }

    @Test
    @DisplayName("should throw exception for null architecture")
    void shouldThrowExceptionForNullArchitecture() {
      assertThrows(
          NullPointerException.class,
          () -> new Target(null, "linux", "gnu", Set.of(), true),
          "Should throw exception for null architecture");
    }

    @Test
    @DisplayName("should throw exception for null operating system")
    void shouldThrowExceptionForNullOperatingSystem() {
      assertThrows(
          NullPointerException.class,
          () -> new Target("x86_64", null, "gnu", Set.of(), true),
          "Should throw exception for null operating system");
    }

    @Test
    @DisplayName("should throw exception for null abi")
    void shouldThrowExceptionForNullAbi() {
      assertThrows(
          NullPointerException.class,
          () -> new Target("x86_64", "linux", null, Set.of(), true),
          "Should throw exception for null abi");
    }

    @Test
    @DisplayName("should generate target triple correctly")
    void shouldGenerateTargetTripleCorrectly() {
      final Target target = new Target("x86_64", "linux", "gnu", Set.of(), true);

      assertEquals("x86_64-linux-gnu", target.getTargetTriple(), "Target triple should match");
    }

    @Test
    @DisplayName("should detect x86 architecture")
    void shouldDetectX86Architecture() {
      final Target x86Target = new Target("x86", "linux", "gnu", Set.of(), false);
      assertTrue(x86Target.isX86(), "x86 should be detected");
      assertFalse(x86Target.isArm(), "x86 should not be ARM");

      final Target x86_64Target = new Target("x86_64", "linux", "gnu", Set.of(), true);
      assertTrue(x86_64Target.isX86(), "x86_64 should be detected");
      assertFalse(x86_64Target.isArm(), "x86_64 should not be ARM");
    }

    @Test
    @DisplayName("should detect ARM architecture")
    void shouldDetectArmArchitecture() {
      final Target armTarget = new Target("arm", "linux", "gnu", Set.of(), false);
      assertTrue(armTarget.isArm(), "ARM should be detected");
      assertFalse(armTarget.isX86(), "ARM should not be x86");

      final Target aarch64Target = new Target("aarch64", "linux", "gnu", Set.of(), true);
      assertTrue(aarch64Target.isArm(), "AArch64 should be detected");
      assertFalse(aarch64Target.isX86(), "AArch64 should not be x86");
    }

    @Test
    @DisplayName("should check CPU feature")
    void shouldCheckCpuFeature() {
      final Set<String> cpuFeatures = Set.of("sse2", "avx");
      final Target target = new Target("x86_64", "linux", "gnu", cpuFeatures, true);

      assertTrue(target.hasCpuFeature("sse2"), "Should detect SSE2");
      assertTrue(target.hasCpuFeature("avx"), "Should detect AVX");
      assertFalse(target.hasCpuFeature("avx512"), "Should not detect AVX512");
    }

    @Test
    @DisplayName("should detect SIMD support with SSE2")
    void shouldDetectSimdSupportWithSse2() {
      final Target target = new Target("x86_64", "linux", "gnu", Set.of("sse2"), true);
      assertTrue(target.hasSimdSupport(), "Should detect SIMD with SSE2");
    }

    @Test
    @DisplayName("should detect SIMD support with AVX")
    void shouldDetectSimdSupportWithAvx() {
      final Target target = new Target("x86_64", "linux", "gnu", Set.of("avx"), true);
      assertTrue(target.hasSimdSupport(), "Should detect SIMD with AVX");
    }

    @Test
    @DisplayName("should detect SIMD support with NEON")
    void shouldDetectSimdSupportWithNeon() {
      final Target target = new Target("aarch64", "linux", "gnu", Set.of("neon"), true);
      assertTrue(target.hasSimdSupport(), "Should detect SIMD with NEON");
    }

    @Test
    @DisplayName("should not detect SIMD without SIMD features")
    void shouldNotDetectSimdWithoutSimdFeatures() {
      final Target target = new Target("x86_64", "linux", "gnu", Set.of(), true);
      assertFalse(target.hasSimdSupport(), "Should not detect SIMD without SIMD features");
    }

    @Test
    @DisplayName("should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      final Target target1 = new Target("x86_64", "linux", "gnu", Set.of("sse2"), true);
      final Target target2 = new Target("x86_64", "linux", "gnu", Set.of("sse2"), true);
      final Target target3 = new Target("aarch64", "linux", "gnu", Set.of("neon"), true);

      assertEquals(target1, target2, "Identical targets should be equal");
      assertFalse(target1.equals(target3), "Different targets should not be equal");
    }

    @Test
    @DisplayName("should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      final Target target1 = new Target("x86_64", "linux", "gnu", Set.of("sse2"), true);
      final Target target2 = new Target("x86_64", "linux", "gnu", Set.of("sse2"), true);

      assertEquals(
          target1.hashCode(), target2.hashCode(), "Identical targets should have same hash code");
    }
  }
}
