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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentBackup} interface.
 *
 * <p>ComponentBackup provides backup functionality for WebAssembly components.
 */
@DisplayName("ComponentBackup Tests")
class ComponentBackupTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentBackup.class.getModifiers()),
          "ComponentBackup should be public");
      assertTrue(ComponentBackup.class.isInterface(), "ComponentBackup should be an interface");
    }

    @Test
    @DisplayName("should have BackupType nested enum")
    void shouldHaveBackupTypeNestedEnum() {
      final var nestedClasses = ComponentBackup.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("BackupType")) {
          found = true;
          assertTrue(nestedClass.isEnum(), "BackupType should be an enum");
          break;
        }
      }
      assertTrue(found, "Should have BackupType nested enum");
    }

    @Test
    @DisplayName("should have BackupStatus nested enum")
    void shouldHaveBackupStatusNestedEnum() {
      final var nestedClasses = ComponentBackup.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("BackupStatus")) {
          found = true;
          assertTrue(nestedClass.isEnum(), "BackupStatus should be an enum");
          break;
        }
      }
      assertTrue(found, "Should have BackupStatus nested enum");
    }

    @Test
    @DisplayName("should have VerificationResult nested interface")
    void shouldHaveVerificationResultNestedInterface() {
      final var nestedClasses = ComponentBackup.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("VerificationResult")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "VerificationResult should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have VerificationResult nested interface");
    }

    @Test
    @DisplayName("should have RestoreResult nested interface")
    void shouldHaveRestoreResultNestedInterface() {
      final var nestedClasses = ComponentBackup.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("RestoreResult")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "RestoreResult should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have RestoreResult nested interface");
    }

    @Test
    @DisplayName("should have CompressionInfo nested interface")
    void shouldHaveCompressionInfoNestedInterface() {
      final var nestedClasses = ComponentBackup.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("CompressionInfo")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "CompressionInfo should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have CompressionInfo nested interface");
    }

    @Test
    @DisplayName("should have EncryptionInfo nested interface")
    void shouldHaveEncryptionInfoNestedInterface() {
      final var nestedClasses = ComponentBackup.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("EncryptionInfo")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "EncryptionInfo should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have EncryptionInfo nested interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getBackupId method")
    void shouldHaveGetBackupIdMethod() throws NoSuchMethodException {
      final Method method = ComponentBackup.class.getMethod("getBackupId");
      assertNotNull(method, "getBackupId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getComponentId method")
    void shouldHaveGetComponentIdMethod() throws NoSuchMethodException {
      final Method method = ComponentBackup.class.getMethod("getComponentId");
      assertNotNull(method, "getComponentId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getTimestamp method")
    void shouldHaveGetTimestampMethod() throws NoSuchMethodException {
      final Method method = ComponentBackup.class.getMethod("getTimestamp");
      assertNotNull(method, "getTimestamp method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = ComponentBackup.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
    }

    @Test
    @DisplayName("should have getSize method")
    void shouldHaveGetSizeMethod() throws NoSuchMethodException {
      final Method method = ComponentBackup.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getChecksum method")
    void shouldHaveGetChecksumMethod() throws NoSuchMethodException {
      final Method method = ComponentBackup.class.getMethod("getChecksum");
      assertNotNull(method, "getChecksum method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getMetadata method")
    void shouldHaveGetMetadataMethod() throws NoSuchMethodException {
      final Method method = ComponentBackup.class.getMethod("getMetadata");
      assertNotNull(method, "getMetadata method should exist");
    }

    @Test
    @DisplayName("should have getStatus method")
    void shouldHaveGetStatusMethod() throws NoSuchMethodException {
      final Method method = ComponentBackup.class.getMethod("getStatus");
      assertNotNull(method, "getStatus method should exist");
    }

    @Test
    @DisplayName("should have getLocation method")
    void shouldHaveGetLocationMethod() throws NoSuchMethodException {
      final Method method = ComponentBackup.class.getMethod("getLocation");
      assertNotNull(method, "getLocation method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have verify method")
    void shouldHaveVerifyMethod() throws NoSuchMethodException {
      final Method method = ComponentBackup.class.getMethod("verify");
      assertNotNull(method, "verify method should exist");
    }

    @Test
    @DisplayName("should have restore method")
    void shouldHaveRestoreMethod() throws NoSuchMethodException {
      final Method method =
          ComponentBackup.class.getMethod("restore", ComponentRestoreOptions.class);
      assertNotNull(method, "restore method should exist");
    }

    @Test
    @DisplayName("should have delete method")
    void shouldHaveDeleteMethod() throws NoSuchMethodException {
      final Method method = ComponentBackup.class.getMethod("delete");
      assertNotNull(method, "delete method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getCompressionInfo method")
    void shouldHaveGetCompressionInfoMethod() throws NoSuchMethodException {
      final Method method = ComponentBackup.class.getMethod("getCompressionInfo");
      assertNotNull(method, "getCompressionInfo method should exist");
    }

    @Test
    @DisplayName("should have getEncryptionInfo method")
    void shouldHaveGetEncryptionInfoMethod() throws NoSuchMethodException {
      final Method method = ComponentBackup.class.getMethod("getEncryptionInfo");
      assertNotNull(method, "getEncryptionInfo method should exist");
    }
  }

  @Nested
  @DisplayName("BackupType Enum Tests")
  class BackupTypeEnumTests {

    @Test
    @DisplayName("should have all backup types")
    void shouldHaveAllBackupTypes() {
      final var types = ComponentBackup.BackupType.values();
      assertEquals(4, types.length, "Should have 4 backup types");
    }

    @Test
    @DisplayName("should have FULL type")
    void shouldHaveFullType() {
      assertEquals(ComponentBackup.BackupType.FULL, ComponentBackup.BackupType.valueOf("FULL"));
    }

    @Test
    @DisplayName("should have INCREMENTAL type")
    void shouldHaveIncrementalType() {
      assertEquals(
          ComponentBackup.BackupType.INCREMENTAL,
          ComponentBackup.BackupType.valueOf("INCREMENTAL"));
    }

    @Test
    @DisplayName("should have DIFFERENTIAL type")
    void shouldHaveDifferentialType() {
      assertEquals(
          ComponentBackup.BackupType.DIFFERENTIAL,
          ComponentBackup.BackupType.valueOf("DIFFERENTIAL"));
    }

    @Test
    @DisplayName("should have SNAPSHOT type")
    void shouldHaveSnapshotType() {
      assertEquals(
          ComponentBackup.BackupType.SNAPSHOT, ComponentBackup.BackupType.valueOf("SNAPSHOT"));
    }
  }

  @Nested
  @DisplayName("BackupStatus Enum Tests")
  class BackupStatusEnumTests {

    @Test
    @DisplayName("should have all backup statuses")
    void shouldHaveAllBackupStatuses() {
      final var statuses = ComponentBackup.BackupStatus.values();
      assertEquals(5, statuses.length, "Should have 5 backup statuses");
    }

    @Test
    @DisplayName("should have IN_PROGRESS status")
    void shouldHaveInProgressStatus() {
      assertEquals(
          ComponentBackup.BackupStatus.IN_PROGRESS,
          ComponentBackup.BackupStatus.valueOf("IN_PROGRESS"));
    }

    @Test
    @DisplayName("should have COMPLETED status")
    void shouldHaveCompletedStatus() {
      assertEquals(
          ComponentBackup.BackupStatus.COMPLETED,
          ComponentBackup.BackupStatus.valueOf("COMPLETED"));
    }

    @Test
    @DisplayName("should have FAILED status")
    void shouldHaveFailedStatus() {
      assertEquals(
          ComponentBackup.BackupStatus.FAILED, ComponentBackup.BackupStatus.valueOf("FAILED"));
    }

    @Test
    @DisplayName("should have CORRUPTED status")
    void shouldHaveCorruptedStatus() {
      assertEquals(
          ComponentBackup.BackupStatus.CORRUPTED,
          ComponentBackup.BackupStatus.valueOf("CORRUPTED"));
    }

    @Test
    @DisplayName("should have ARCHIVED status")
    void shouldHaveArchivedStatus() {
      assertEquals(
          ComponentBackup.BackupStatus.ARCHIVED, ComponentBackup.BackupStatus.valueOf("ARCHIVED"));
    }
  }

  @Nested
  @DisplayName("VerificationResult Interface Tests")
  class VerificationResultInterfaceTests {

    @Test
    @DisplayName("VerificationResult should have required methods")
    void verificationResultShouldHaveRequiredMethods() throws NoSuchMethodException {
      final Class<?> verificationResultClass = ComponentBackup.VerificationResult.class;

      assertNotNull(verificationResultClass.getMethod("isValid"), "Should have isValid");
      assertNotNull(verificationResultClass.getMethod("getErrors"), "Should have getErrors");
      assertNotNull(verificationResultClass.getMethod("getTimestamp"), "Should have getTimestamp");
      assertNotNull(
          verificationResultClass.getMethod("isChecksumValid"), "Should have isChecksumValid");
    }
  }

  @Nested
  @DisplayName("RestoreResult Interface Tests")
  class RestoreResultInterfaceTests {

    @Test
    @DisplayName("RestoreResult should have required methods")
    void restoreResultShouldHaveRequiredMethods() throws NoSuchMethodException {
      final Class<?> restoreResultClass = ComponentBackup.RestoreResult.class;

      assertNotNull(restoreResultClass.getMethod("isSuccessful"), "Should have isSuccessful");
      assertNotNull(restoreResultClass.getMethod("getErrors"), "Should have getErrors");
      assertNotNull(restoreResultClass.getMethod("getTimestamp"), "Should have getTimestamp");
      assertNotNull(
          restoreResultClass.getMethod("getRestoredComponent"), "Should have getRestoredComponent");
    }
  }

  @Nested
  @DisplayName("CompressionInfo Interface Tests")
  class CompressionInfoInterfaceTests {

    @Test
    @DisplayName("CompressionInfo should have required methods")
    void compressionInfoShouldHaveRequiredMethods() throws NoSuchMethodException {
      final Class<?> compressionInfoClass = ComponentBackup.CompressionInfo.class;

      assertNotNull(compressionInfoClass.getMethod("getAlgorithm"), "Should have getAlgorithm");
      assertNotNull(
          compressionInfoClass.getMethod("getOriginalSize"), "Should have getOriginalSize");
      assertNotNull(
          compressionInfoClass.getMethod("getCompressedSize"), "Should have getCompressedSize");
      assertNotNull(
          compressionInfoClass.getMethod("getCompressionRatio"), "Should have getCompressionRatio");
    }
  }

  @Nested
  @DisplayName("EncryptionInfo Interface Tests")
  class EncryptionInfoInterfaceTests {

    @Test
    @DisplayName("EncryptionInfo should have required methods")
    void encryptionInfoShouldHaveRequiredMethods() throws NoSuchMethodException {
      final Class<?> encryptionInfoClass = ComponentBackup.EncryptionInfo.class;

      assertNotNull(encryptionInfoClass.getMethod("getAlgorithm"), "Should have getAlgorithm");
      assertNotNull(
          encryptionInfoClass.getMethod("getKeyDerivationFunction"),
          "Should have getKeyDerivationFunction");
      assertNotNull(encryptionInfoClass.getMethod("isEncrypted"), "Should have isEncrypted");
      assertNotNull(encryptionInfoClass.getMethod("getParameters"), "Should have getParameters");
    }
  }
}
