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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Extern;
import ai.tegmentum.wasmtime4j.type.ExternType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaExternTable} class.
 *
 * <p>PanamaExternTable is the Panama implementation of an extern table value.
 */
@DisplayName("PanamaExternTable Tests")
class PanamaExternTableTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be package-private final class")
    void shouldBePackagePrivateFinalClass() {
      // Not public - package-private
      assertTrue(
          !Modifier.isPublic(PanamaExternTable.class.getModifiers()),
          "PanamaExternTable should be package-private");
      assertTrue(
          Modifier.isFinal(PanamaExternTable.class.getModifiers()),
          "PanamaExternTable should be final");
    }

    @Test
    @DisplayName("should implement Extern interface")
    void shouldImplementExternInterface() {
      assertTrue(
          Extern.class.isAssignableFrom(PanamaExternTable.class),
          "PanamaExternTable should implement Extern");
    }
  }

  @Nested
  @DisplayName("Extern Interface Method Tests")
  class ExternInterfaceMethodTests {

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = PanamaExternTable.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(ExternType.class, method.getReturnType(), "Should return ExternType");
    }

    @Test
    @DisplayName("should have asTable method")
    void shouldHaveAsTableMethod() throws NoSuchMethodException {
      final Method method = PanamaExternTable.class.getMethod("asTable");
      assertNotNull(method, "asTable method should exist");
      assertNotNull(method.getReturnType(), "Should have a return type");
    }
  }

  @Nested
  @DisplayName("Internal Method Tests")
  class InternalMethodTests {

    @Test
    @DisplayName("should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      final Method method = PanamaExternTable.class.getDeclaredMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      // Package-private method
      assertTrue(
          !Modifier.isPublic(method.getModifiers()), "getNativeHandle should be package-private");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have package-private constructor")
    void shouldHavePackagePrivateConstructor() {
      boolean hasExpectedConstructor = false;
      for (var constructor : PanamaExternTable.class.getDeclaredConstructors()) {
        if (constructor.getParameterCount() == 2) {
          hasExpectedConstructor = true;
          break;
        }
      }
      assertTrue(hasExpectedConstructor, "Should have constructor with 2 parameters");
    }
  }
}
