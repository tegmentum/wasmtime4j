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

package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniExnRef} class.
 *
 * <p>JniExnRef provides JNI implementation of WebAssembly exception reference handling.
 */
@DisplayName("JniExnRef Tests")
class JniExnRefTest {

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getTag method")
    void shouldHaveGetTagMethod() throws NoSuchMethodException {
      final Method method =
          JniExnRef.class.getMethod("getTag", ai.tegmentum.wasmtime4j.Store.class);
      assertNotNull(method, "getTag method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.memory.Tag.class,
          method.getReturnType(),
          "getTag should return Tag");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = JniExnRef.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }
  }

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should have inherited close method")
    void shouldHaveInheritedCloseMethod() throws NoSuchMethodException {
      final Method method = JniExnRef.class.getMethod("close");
      assertNotNull(method, "close method should exist (inherited from JniResource)");
    }

    @Test
    @DisplayName("should have inherited isClosed method")
    void shouldHaveInheritedIsClosedMethod() throws NoSuchMethodException {
      final Method method = JniExnRef.class.getMethod("isClosed");
      assertNotNull(method, "isClosed method should exist (inherited from JniResource)");
    }
  }
}
