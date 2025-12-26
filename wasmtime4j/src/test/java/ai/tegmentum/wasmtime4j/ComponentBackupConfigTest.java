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

import ai.tegmentum.wasmtime4j.ComponentBackupConfig.BackupFrequency;
import ai.tegmentum.wasmtime4j.ComponentBackupConfig.BackupStrategy;
import ai.tegmentum.wasmtime4j.ComponentBackupConfig.BackupTrigger;
import ai.tegmentum.wasmtime4j.ComponentBackupConfig.ChecksumAlgorithm;
import ai.tegmentum.wasmtime4j.ComponentBackupConfig.CleanupStrategy;
import ai.tegmentum.wasmtime4j.ComponentBackupConfig.CompressionAlgorithm;
import ai.tegmentum.wasmtime4j.ComponentBackupConfig.EncryptionAlgorithm;
import ai.tegmentum.wasmtime4j.ComponentBackupConfig.VerificationFrequency;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentBackupConfig} interface.
 *
 * <p>ComponentBackupConfig provides configuration for component backup operations.
 */
@DisplayName("ComponentBackupConfig Tests")
class ComponentBackupConfigTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentBackupConfig.class.getModifiers()),
          "ComponentBackupConfig should be public");
      assertTrue(
          ComponentBackupConfig.class.isInterface(),
          "ComponentBackupConfig should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have isEnabled method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      final Method method = ComponentBackupConfig.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have setEnabled method")
    void shouldHaveSetEnabledMethod() throws NoSuchMethodException {
      final Method method = ComponentBackupConfig.class.getMethod("setEnabled", boolean.class);
      assertNotNull(method, "setEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getStrategy method")
    void shouldHaveGetStrategyMethod() throws NoSuchMethodException {
      final Method method = ComponentBackupConfig.class.getMethod("getStrategy");
      assertNotNull(method, "getStrategy method should exist");
      assertEquals(BackupStrategy.class, method.getReturnType(), "Should return BackupStrategy");
    }

    @Test
    @DisplayName("should have getBackupLocation method")
    void shouldHaveGetBackupLocationMethod() throws NoSuchMethodException {
      final Method method = ComponentBackupConfig.class.getMethod("getBackupLocation");
      assertNotNull(method, "getBackupLocation method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getFrequency method")
    void shouldHaveGetFrequencyMethod() throws NoSuchMethodException {
      final Method method = ComponentBackupConfig.class.getMethod("getFrequency");
      assertNotNull(method, "getFrequency method should exist");
      assertEquals(BackupFrequency.class, method.getReturnType(), "Should return BackupFrequency");
    }

    @Test
    @DisplayName("should have getTriggers method")
    void shouldHaveGetTriggersMethod() throws NoSuchMethodException {
      final Method method = ComponentBackupConfig.class.getMethod("getTriggers");
      assertNotNull(method, "getTriggers method should exist");
    }
  }

  @Nested
  @DisplayName("BackupStrategy Enum Tests")
  class BackupStrategyEnumTests {

    @Test
    @DisplayName("should have all backup strategies")
    void shouldHaveAllBackupStrategies() {
      final var strategies = BackupStrategy.values();
      assertEquals(5, strategies.length, "Should have 5 backup strategies");
    }

    @Test
    @DisplayName("should have FULL_ONLY strategy")
    void shouldHaveFullOnlyStrategy() {
      assertEquals(BackupStrategy.FULL_ONLY, BackupStrategy.valueOf("FULL_ONLY"));
    }

    @Test
    @DisplayName("should have INCREMENTAL strategy")
    void shouldHaveIncrementalStrategy() {
      assertEquals(BackupStrategy.INCREMENTAL, BackupStrategy.valueOf("INCREMENTAL"));
    }

    @Test
    @DisplayName("should have DIFFERENTIAL strategy")
    void shouldHaveDifferentialStrategy() {
      assertEquals(BackupStrategy.DIFFERENTIAL, BackupStrategy.valueOf("DIFFERENTIAL"));
    }

    @Test
    @DisplayName("should have MIXED strategy")
    void shouldHaveMixedStrategy() {
      assertEquals(BackupStrategy.MIXED, BackupStrategy.valueOf("MIXED"));
    }

    @Test
    @DisplayName("should have SNAPSHOT strategy")
    void shouldHaveSnapshotStrategy() {
      assertEquals(BackupStrategy.SNAPSHOT, BackupStrategy.valueOf("SNAPSHOT"));
    }
  }

  @Nested
  @DisplayName("BackupFrequency Enum Tests")
  class BackupFrequencyEnumTests {

    @Test
    @DisplayName("should have all backup frequencies")
    void shouldHaveAllBackupFrequencies() {
      final var frequencies = BackupFrequency.values();
      assertEquals(6, frequencies.length, "Should have 6 backup frequencies");
    }

    @Test
    @DisplayName("should have MANUAL frequency")
    void shouldHaveManualFrequency() {
      assertEquals(BackupFrequency.MANUAL, BackupFrequency.valueOf("MANUAL"));
    }

    @Test
    @DisplayName("should have ON_CHANGE frequency")
    void shouldHaveOnChangeFrequency() {
      assertEquals(BackupFrequency.ON_CHANGE, BackupFrequency.valueOf("ON_CHANGE"));
    }

    @Test
    @DisplayName("should have HOURLY frequency")
    void shouldHaveHourlyFrequency() {
      assertEquals(BackupFrequency.HOURLY, BackupFrequency.valueOf("HOURLY"));
    }

    @Test
    @DisplayName("should have DAILY frequency")
    void shouldHaveDailyFrequency() {
      assertEquals(BackupFrequency.DAILY, BackupFrequency.valueOf("DAILY"));
    }

    @Test
    @DisplayName("should have WEEKLY frequency")
    void shouldHaveWeeklyFrequency() {
      assertEquals(BackupFrequency.WEEKLY, BackupFrequency.valueOf("WEEKLY"));
    }

    @Test
    @DisplayName("should have MONTHLY frequency")
    void shouldHaveMonthlyFrequency() {
      assertEquals(BackupFrequency.MONTHLY, BackupFrequency.valueOf("MONTHLY"));
    }
  }

  @Nested
  @DisplayName("BackupTrigger Enum Tests")
  class BackupTriggerEnumTests {

    @Test
    @DisplayName("should have all backup triggers")
    void shouldHaveAllBackupTriggers() {
      final var triggers = BackupTrigger.values();
      assertEquals(6, triggers.length, "Should have 6 backup triggers");
    }

    @Test
    @DisplayName("should have BEFORE_UPDATE trigger")
    void shouldHaveBeforeUpdateTrigger() {
      assertEquals(BackupTrigger.BEFORE_UPDATE, BackupTrigger.valueOf("BEFORE_UPDATE"));
    }

    @Test
    @DisplayName("should have AFTER_UPDATE trigger")
    void shouldHaveAfterUpdateTrigger() {
      assertEquals(BackupTrigger.AFTER_UPDATE, BackupTrigger.valueOf("AFTER_UPDATE"));
    }

    @Test
    @DisplayName("should have ON_INSTANTIATION trigger")
    void shouldHaveOnInstantiationTrigger() {
      assertEquals(BackupTrigger.ON_INSTANTIATION, BackupTrigger.valueOf("ON_INSTANTIATION"));
    }

    @Test
    @DisplayName("should have ON_STATE_CHANGE trigger")
    void shouldHaveOnStateChangeTrigger() {
      assertEquals(BackupTrigger.ON_STATE_CHANGE, BackupTrigger.valueOf("ON_STATE_CHANGE"));
    }

    @Test
    @DisplayName("should have ON_ERROR trigger")
    void shouldHaveOnErrorTrigger() {
      assertEquals(BackupTrigger.ON_ERROR, BackupTrigger.valueOf("ON_ERROR"));
    }

    @Test
    @DisplayName("should have SCHEDULED trigger")
    void shouldHaveScheduledTrigger() {
      assertEquals(BackupTrigger.SCHEDULED, BackupTrigger.valueOf("SCHEDULED"));
    }
  }

  @Nested
  @DisplayName("CleanupStrategy Enum Tests")
  class CleanupStrategyEnumTests {

    @Test
    @DisplayName("should have all cleanup strategies")
    void shouldHaveAllCleanupStrategies() {
      final var strategies = CleanupStrategy.values();
      assertEquals(4, strategies.length, "Should have 4 cleanup strategies");
    }

    @Test
    @DisplayName("should have OLDEST_FIRST strategy")
    void shouldHaveOldestFirstStrategy() {
      assertEquals(CleanupStrategy.OLDEST_FIRST, CleanupStrategy.valueOf("OLDEST_FIRST"));
    }

    @Test
    @DisplayName("should have LARGEST_FIRST strategy")
    void shouldHaveLargestFirstStrategy() {
      assertEquals(CleanupStrategy.LARGEST_FIRST, CleanupStrategy.valueOf("LARGEST_FIRST"));
    }

    @Test
    @DisplayName("should have LEAST_USED_FIRST strategy")
    void shouldHaveLeastUsedFirstStrategy() {
      assertEquals(CleanupStrategy.LEAST_USED_FIRST, CleanupStrategy.valueOf("LEAST_USED_FIRST"));
    }

    @Test
    @DisplayName("should have KEEP_FULL_BACKUPS strategy")
    void shouldHaveKeepFullBackupsStrategy() {
      assertEquals(CleanupStrategy.KEEP_FULL_BACKUPS, CleanupStrategy.valueOf("KEEP_FULL_BACKUPS"));
    }
  }

  @Nested
  @DisplayName("CompressionAlgorithm Enum Tests")
  class CompressionAlgorithmEnumTests {

    @Test
    @DisplayName("should have all compression algorithms")
    void shouldHaveAllCompressionAlgorithms() {
      final var algorithms = CompressionAlgorithm.values();
      assertEquals(5, algorithms.length, "Should have 5 compression algorithms");
    }

    @Test
    @DisplayName("should have GZIP algorithm")
    void shouldHaveGzipAlgorithm() {
      assertEquals(CompressionAlgorithm.GZIP, CompressionAlgorithm.valueOf("GZIP"));
    }

    @Test
    @DisplayName("should have ZIP algorithm")
    void shouldHaveZipAlgorithm() {
      assertEquals(CompressionAlgorithm.ZIP, CompressionAlgorithm.valueOf("ZIP"));
    }

    @Test
    @DisplayName("should have BZIP2 algorithm")
    void shouldHaveBzip2Algorithm() {
      assertEquals(CompressionAlgorithm.BZIP2, CompressionAlgorithm.valueOf("BZIP2"));
    }

    @Test
    @DisplayName("should have LZ4 algorithm")
    void shouldHaveLz4Algorithm() {
      assertEquals(CompressionAlgorithm.LZ4, CompressionAlgorithm.valueOf("LZ4"));
    }

    @Test
    @DisplayName("should have ZSTD algorithm")
    void shouldHaveZstdAlgorithm() {
      assertEquals(CompressionAlgorithm.ZSTD, CompressionAlgorithm.valueOf("ZSTD"));
    }
  }

  @Nested
  @DisplayName("EncryptionAlgorithm Enum Tests")
  class EncryptionAlgorithmEnumTests {

    @Test
    @DisplayName("should have all encryption algorithms")
    void shouldHaveAllEncryptionAlgorithms() {
      final var algorithms = EncryptionAlgorithm.values();
      assertEquals(3, algorithms.length, "Should have 3 encryption algorithms");
    }

    @Test
    @DisplayName("should have AES_256_GCM algorithm")
    void shouldHaveAes256GcmAlgorithm() {
      assertEquals(EncryptionAlgorithm.AES_256_GCM, EncryptionAlgorithm.valueOf("AES_256_GCM"));
    }

    @Test
    @DisplayName("should have AES_128_GCM algorithm")
    void shouldHaveAes128GcmAlgorithm() {
      assertEquals(EncryptionAlgorithm.AES_128_GCM, EncryptionAlgorithm.valueOf("AES_128_GCM"));
    }

    @Test
    @DisplayName("should have CHACHA20_POLY1305 algorithm")
    void shouldHaveChaCha20Poly1305Algorithm() {
      assertEquals(
          EncryptionAlgorithm.CHACHA20_POLY1305, EncryptionAlgorithm.valueOf("CHACHA20_POLY1305"));
    }
  }

  @Nested
  @DisplayName("ChecksumAlgorithm Enum Tests")
  class ChecksumAlgorithmEnumTests {

    @Test
    @DisplayName("should have all checksum algorithms")
    void shouldHaveAllChecksumAlgorithms() {
      final var algorithms = ChecksumAlgorithm.values();
      assertEquals(4, algorithms.length, "Should have 4 checksum algorithms");
    }

    @Test
    @DisplayName("should have SHA256 algorithm")
    void shouldHaveSha256Algorithm() {
      assertEquals(ChecksumAlgorithm.SHA256, ChecksumAlgorithm.valueOf("SHA256"));
    }

    @Test
    @DisplayName("should have SHA512 algorithm")
    void shouldHaveSha512Algorithm() {
      assertEquals(ChecksumAlgorithm.SHA512, ChecksumAlgorithm.valueOf("SHA512"));
    }

    @Test
    @DisplayName("should have BLAKE2B algorithm")
    void shouldHaveBlake2bAlgorithm() {
      assertEquals(ChecksumAlgorithm.BLAKE2B, ChecksumAlgorithm.valueOf("BLAKE2B"));
    }

    @Test
    @DisplayName("should have CRC32 algorithm")
    void shouldHaveCrc32Algorithm() {
      assertEquals(ChecksumAlgorithm.CRC32, ChecksumAlgorithm.valueOf("CRC32"));
    }
  }

  @Nested
  @DisplayName("VerificationFrequency Enum Tests")
  class VerificationFrequencyEnumTests {

    @Test
    @DisplayName("should have all verification frequencies")
    void shouldHaveAllVerificationFrequencies() {
      final var frequencies = VerificationFrequency.values();
      assertEquals(4, frequencies.length, "Should have 4 verification frequencies");
    }

    @Test
    @DisplayName("should have NEVER frequency")
    void shouldHaveNeverFrequency() {
      assertEquals(VerificationFrequency.NEVER, VerificationFrequency.valueOf("NEVER"));
    }

    @Test
    @DisplayName("should have ON_CREATION frequency")
    void shouldHaveOnCreationFrequency() {
      assertEquals(VerificationFrequency.ON_CREATION, VerificationFrequency.valueOf("ON_CREATION"));
    }

    @Test
    @DisplayName("should have ON_RESTORE frequency")
    void shouldHaveOnRestoreFrequency() {
      assertEquals(VerificationFrequency.ON_RESTORE, VerificationFrequency.valueOf("ON_RESTORE"));
    }

    @Test
    @DisplayName("should have PERIODIC frequency")
    void shouldHavePeriodicFrequency() {
      assertEquals(VerificationFrequency.PERIODIC, VerificationFrequency.valueOf("PERIODIC"));
    }
  }

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("should have RetentionPolicy interface")
    void shouldHaveRetentionPolicyInterface() {
      final var classes = ComponentBackupConfig.class.getDeclaredClasses();
      boolean found = false;
      for (final var clazz : classes) {
        if ("RetentionPolicy".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have RetentionPolicy nested interface");
    }

    @Test
    @DisplayName("should have CompressionSettings interface")
    void shouldHaveCompressionSettingsInterface() {
      final var classes = ComponentBackupConfig.class.getDeclaredClasses();
      boolean found = false;
      for (final var clazz : classes) {
        if ("CompressionSettings".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have CompressionSettings nested interface");
    }

    @Test
    @DisplayName("should have EncryptionSettings interface")
    void shouldHaveEncryptionSettingsInterface() {
      final var classes = ComponentBackupConfig.class.getDeclaredClasses();
      boolean found = false;
      for (final var clazz : classes) {
        if ("EncryptionSettings".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have EncryptionSettings nested interface");
    }

    @Test
    @DisplayName("should have VerificationSettings interface")
    void shouldHaveVerificationSettingsInterface() {
      final var classes = ComponentBackupConfig.class.getDeclaredClasses();
      boolean found = false;
      for (final var clazz : classes) {
        if ("VerificationSettings".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have VerificationSettings nested interface");
    }
  }
}
