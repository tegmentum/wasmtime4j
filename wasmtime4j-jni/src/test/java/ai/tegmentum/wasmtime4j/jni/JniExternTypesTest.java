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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Extern;
import ai.tegmentum.wasmtime4j.type.ExternType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for JNI Extern type implementations. Tests JniExternFunc, JniExternGlobal,
 * JniExternMemory, and JniExternTable.
 */
@DisplayName("JNI Extern Types Tests")
class JniExternTypesTest {

  private static final long TEST_HANDLE = 0xDEADBEEFL;

  @Nested
  @DisplayName("JniExternFunc Behavioral Tests")
  class JniExternFuncBehavioralTests {

    @Test
    @DisplayName("getType returns FUNC")
    void getTypeReturnsFuncType() {
      final JniExternFunc extern = new JniExternFunc(TEST_HANDLE, null);
      assertEquals(
          ExternType.FUNC,
          extern.getType(),
          "JniExternFunc.getType() should return ExternType.FUNC");
    }

    @Test
    @DisplayName("getNativeHandle returns stored handle")
    void getNativeHandleReturnsStoredHandle() {
      final JniExternFunc extern = new JniExternFunc(TEST_HANDLE, null);
      assertEquals(
          TEST_HANDLE,
          extern.getNativeHandle(),
          "getNativeHandle() should return the handle passed to constructor");
    }

    @Test
    @DisplayName("implements Extern interface")
    void implementsExternInterface() {
      final JniExternFunc extern = new JniExternFunc(TEST_HANDLE, null);
      assertTrue(extern instanceof Extern, "JniExternFunc should implement Extern interface");
    }

    @Test
    @DisplayName("different handles produce different instances")
    void differentHandlesProduceDifferentInstances() {
      final JniExternFunc extern1 = new JniExternFunc(1L, null);
      final JniExternFunc extern2 = new JniExternFunc(2L, null);
      assertEquals(1L, extern1.getNativeHandle(), "First extern should have handle 1");
      assertEquals(2L, extern2.getNativeHandle(), "Second extern should have handle 2");
    }
  }

  @Nested
  @DisplayName("JniExternGlobal Behavioral Tests")
  class JniExternGlobalBehavioralTests {

    @Test
    @DisplayName("getType returns GLOBAL")
    void getTypeReturnsGlobalType() {
      final JniExternGlobal extern = new JniExternGlobal(TEST_HANDLE, null);
      assertEquals(
          ExternType.GLOBAL,
          extern.getType(),
          "JniExternGlobal.getType() should return ExternType.GLOBAL");
    }

    @Test
    @DisplayName("getNativeHandle returns stored handle")
    void getNativeHandleReturnsStoredHandle() {
      final JniExternGlobal extern = new JniExternGlobal(TEST_HANDLE, null);
      assertEquals(
          TEST_HANDLE,
          extern.getNativeHandle(),
          "getNativeHandle() should return the handle passed to constructor");
    }

    @Test
    @DisplayName("implements Extern interface")
    void implementsExternInterface() {
      final JniExternGlobal extern = new JniExternGlobal(TEST_HANDLE, null);
      assertTrue(extern instanceof Extern, "JniExternGlobal should implement Extern interface");
    }
  }

  @Nested
  @DisplayName("JniExternMemory Behavioral Tests")
  class JniExternMemoryBehavioralTests {

    @Test
    @DisplayName("getType returns MEMORY")
    void getTypeReturnsMemoryType() {
      final JniExternMemory extern = new JniExternMemory(TEST_HANDLE, null);
      assertEquals(
          ExternType.MEMORY,
          extern.getType(),
          "JniExternMemory.getType() should return ExternType.MEMORY");
    }

    @Test
    @DisplayName("getNativeHandle returns stored handle")
    void getNativeHandleReturnsStoredHandle() {
      final JniExternMemory extern = new JniExternMemory(TEST_HANDLE, null);
      assertEquals(
          TEST_HANDLE,
          extern.getNativeHandle(),
          "getNativeHandle() should return the handle passed to constructor");
    }

    @Test
    @DisplayName("implements Extern interface")
    void implementsExternInterface() {
      final JniExternMemory extern = new JniExternMemory(TEST_HANDLE, null);
      assertTrue(extern instanceof Extern, "JniExternMemory should implement Extern interface");
    }
  }

  @Nested
  @DisplayName("JniExternTable Behavioral Tests")
  class JniExternTableBehavioralTests {

    @Test
    @DisplayName("getType returns TABLE")
    void getTypeReturnsTableType() {
      final JniExternTable extern = new JniExternTable(TEST_HANDLE, null);
      assertEquals(
          ExternType.TABLE,
          extern.getType(),
          "JniExternTable.getType() should return ExternType.TABLE");
    }

    @Test
    @DisplayName("getNativeHandle returns stored handle")
    void getNativeHandleReturnsStoredHandle() {
      final JniExternTable extern = new JniExternTable(TEST_HANDLE, null);
      assertEquals(
          TEST_HANDLE,
          extern.getNativeHandle(),
          "getNativeHandle() should return the handle passed to constructor");
    }

    @Test
    @DisplayName("implements Extern interface")
    void implementsExternInterface() {
      final JniExternTable extern = new JniExternTable(TEST_HANDLE, null);
      assertTrue(extern instanceof Extern, "JniExternTable should implement Extern interface");
    }
  }
}
