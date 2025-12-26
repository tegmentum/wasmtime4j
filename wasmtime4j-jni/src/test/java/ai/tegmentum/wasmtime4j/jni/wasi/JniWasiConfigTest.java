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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniWasiConfig} class.
 *
 * <p>JniWasiConfig represents WASI configuration for JNI implementation.
 */
@DisplayName("JniWasiConfig Class Tests")
class JniWasiConfigTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public class")
    void shouldBePublicClass() {
      assertTrue(Modifier.isPublic(JniWasiConfig.class.getModifiers()),
          "JniWasiConfig should be public");
    }

    @Test
    @DisplayName("should have builder method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      final Method method = JniWasiConfig.class.getMethod("builder");
      assertNotNull(method, "Should have builder method");
    }
  }

  @Nested
  @DisplayName("AutoCloseable Tests")
  class AutoCloseableTests {

    @Test
    @DisplayName("should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(AutoCloseable.class.isAssignableFrom(JniWasiConfig.class),
          "JniWasiConfig should implement AutoCloseable");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      assertNotNull(JniWasiConfig.class.getMethod("close"),
          "Should have close method");
    }
  }

  @Nested
  @DisplayName("Native Handle Tests")
  class NativeHandleTests {

    @Test
    @DisplayName("should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      assertNotNull(JniWasiConfig.class.getMethod("getNativeHandle"),
          "Should have getNativeHandle method");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      assertNotNull(JniWasiConfig.class.getMethod("isValid"),
          "Should have isValid method");
    }
  }
}
