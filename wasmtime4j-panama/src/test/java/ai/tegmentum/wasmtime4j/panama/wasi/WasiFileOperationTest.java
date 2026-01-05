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

package ai.tegmentum.wasmtime4j.panama.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiFileOperation} enum.
 *
 * <p>WasiFileOperation defines the types of file system operations that can be performed through
 * WASI, allowing for fine-grained permission control.
 */
@DisplayName("WasiFileOperation Tests")
class WasiFileOperationTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public enum")
    void shouldBePublicEnum() {
      assertTrue(
          Modifier.isPublic(WasiFileOperation.class.getModifiers()),
          "WasiFileOperation should be public");
      assertTrue(WasiFileOperation.class.isEnum(), "WasiFileOperation should be an enum");
    }
  }

  @Nested
  @DisplayName("Enum Constant Tests")
  class EnumConstantTests {

    @Test
    @DisplayName("should have READ constant")
    void shouldHaveReadConstant() {
      assertNotNull(WasiFileOperation.valueOf("READ"), "READ constant should exist");
    }

    @Test
    @DisplayName("should have WRITE constant")
    void shouldHaveWriteConstant() {
      assertNotNull(WasiFileOperation.valueOf("WRITE"), "WRITE constant should exist");
    }

    @Test
    @DisplayName("should have EXECUTE constant")
    void shouldHaveExecuteConstant() {
      assertNotNull(WasiFileOperation.valueOf("EXECUTE"), "EXECUTE constant should exist");
    }

    @Test
    @DisplayName("should have CREATE_DIRECTORY constant")
    void shouldHaveCreateDirectoryConstant() {
      assertNotNull(
          WasiFileOperation.valueOf("CREATE_DIRECTORY"), "CREATE_DIRECTORY constant should exist");
    }

    @Test
    @DisplayName("should have DELETE constant")
    void shouldHaveDeleteConstant() {
      assertNotNull(WasiFileOperation.valueOf("DELETE"), "DELETE constant should exist");
    }

    @Test
    @DisplayName("should have RENAME constant")
    void shouldHaveRenameConstant() {
      assertNotNull(WasiFileOperation.valueOf("RENAME"), "RENAME constant should exist");
    }

    @Test
    @DisplayName("should have METADATA constant")
    void shouldHaveMetadataConstant() {
      assertNotNull(WasiFileOperation.valueOf("METADATA"), "METADATA constant should exist");
    }

    @Test
    @DisplayName("should have OPEN constant")
    void shouldHaveOpenConstant() {
      assertNotNull(WasiFileOperation.valueOf("OPEN"), "OPEN constant should exist");
    }

    @Test
    @DisplayName("should have CLOSE constant")
    void shouldHaveCloseConstant() {
      assertNotNull(WasiFileOperation.valueOf("CLOSE"), "CLOSE constant should exist");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getOperationId method")
    void shouldHaveGetOperationIdMethod() throws NoSuchMethodException {
      final Method method = WasiFileOperation.class.getMethod("getOperationId");
      assertNotNull(method, "getOperationId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getDescription method")
    void shouldHaveGetDescriptionMethod() throws NoSuchMethodException {
      final Method method = WasiFileOperation.class.getMethod("getDescription");
      assertNotNull(method, "getDescription method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Permission Check Method Tests")
  class PermissionCheckMethodTests {

    @Test
    @DisplayName("should have requiresReadAccess method")
    void shouldHaveRequiresReadAccessMethod() throws NoSuchMethodException {
      final Method method = WasiFileOperation.class.getMethod("requiresReadAccess");
      assertNotNull(method, "requiresReadAccess method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have requiresWriteAccess method")
    void shouldHaveRequiresWriteAccessMethod() throws NoSuchMethodException {
      final Method method = WasiFileOperation.class.getMethod("requiresWriteAccess");
      assertNotNull(method, "requiresWriteAccess method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have requiresExecuteAccess method")
    void shouldHaveRequiresExecuteAccessMethod() throws NoSuchMethodException {
      final Method method = WasiFileOperation.class.getMethod("requiresExecuteAccess");
      assertNotNull(method, "requiresExecuteAccess method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isModifyingOperation method")
    void shouldHaveIsModifyingOperationMethod() throws NoSuchMethodException {
      final Method method = WasiFileOperation.class.getMethod("isModifyingOperation");
      assertNotNull(method, "isModifyingOperation method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isDangerous method")
    void shouldHaveIsDangerousMethod() throws NoSuchMethodException {
      final Method method = WasiFileOperation.class.getMethod("isDangerous");
      assertNotNull(method, "isDangerous method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isWriteOperation method")
    void shouldHaveIsWriteOperationMethod() throws NoSuchMethodException {
      final Method method = WasiFileOperation.class.getMethod("isWriteOperation");
      assertNotNull(method, "isWriteOperation method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have fromOperationId static method")
    void shouldHaveFromOperationIdMethod() throws NoSuchMethodException {
      final Method method = WasiFileOperation.class.getMethod("fromOperationId", String.class);
      assertNotNull(method, "fromOperationId method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasiFileOperation.class, method.getReturnType(), "Should return WasiFileOperation");
    }
  }
}
