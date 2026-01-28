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

package ai.tegmentum.wasmtime4j.jni.wasi.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiFileOperation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for WASI permission classes: {@link WasiPermissionManager}, {@link WasiResourceLimits}, and
 * {@link WasiFileOperation}.
 *
 * <p>These tests verify the permission system works correctly for controlling WASI capabilities
 * including file system access, environment variable access, and resource limiting.
 */
@DisplayName("WASI Permission System Tests")
class WasiPermissionTest {

  // ============================================================================
  // WasiResourceLimits Tests
  // ============================================================================

  @Nested
  @DisplayName("WasiResourceLimits Tests")
  class WasiResourceLimitsTests {

    @Nested
    @DisplayName("UNLIMITED Constant Tests")
    class UnlimitedConstantTests {

      @Test
      @DisplayName("UNLIMITED constant should be -1")
      void unlimitedConstantShouldBeNegativeOne() {
        assertThat(WasiResourceLimits.UNLIMITED).isEqualTo(-1L);
      }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

      @Test
      @DisplayName("should create builder via static method")
      void shouldCreateBuilderViaStaticMethod() {
        final WasiResourceLimits.Builder builder = WasiResourceLimits.builder();
        assertThat(builder).isNotNull();
      }

      @Test
      @DisplayName("should build limits with all default values (unlimited)")
      void shouldBuildLimitsWithAllDefaultValues() {
        final WasiResourceLimits limits = WasiResourceLimits.builder().build();

        assertThat(limits.getMaxMemoryBytes()).isEqualTo(WasiResourceLimits.UNLIMITED);
        assertThat(limits.getMaxFileDescriptors()).isEqualTo((int) WasiResourceLimits.UNLIMITED);
        assertThat(limits.getMaxDiskReadsPerSecond()).isEqualTo(WasiResourceLimits.UNLIMITED);
        assertThat(limits.getMaxDiskWritesPerSecond()).isEqualTo(WasiResourceLimits.UNLIMITED);
        assertThat(limits.getMaxDiskReadBytesPerSecond()).isEqualTo(WasiResourceLimits.UNLIMITED);
        assertThat(limits.getMaxDiskWriteBytesPerSecond()).isEqualTo(WasiResourceLimits.UNLIMITED);
        assertThat(limits.getMaxNetworkConnections()).isEqualTo((int) WasiResourceLimits.UNLIMITED);
        assertThat(limits.getMaxExecutionTime()).isNull();
        assertThat(limits.getMaxCpuTime()).isNull();
        assertThat(limits.getMaxWallClockTime()).isNull();
        assertThat(limits.getMaxFileSize()).isEqualTo(WasiResourceLimits.UNLIMITED);
        assertThat(limits.getMaxDiskSpaceUsage()).isEqualTo(WasiResourceLimits.UNLIMITED);
      }

      @Test
      @DisplayName("should set max memory bytes")
      void shouldSetMaxMemoryBytes() {
        final long expectedBytes = 512L * 1024L * 1024L; // 512 MB
        final WasiResourceLimits limits =
            WasiResourceLimits.builder().withMaxMemoryBytes(expectedBytes).build();

        assertThat(limits.getMaxMemoryBytes()).isEqualTo(expectedBytes);
      }

      @Test
      @DisplayName("should set max file descriptors")
      void shouldSetMaxFileDescriptors() {
        final int expectedFds = 2048;
        final WasiResourceLimits limits =
            WasiResourceLimits.builder().withMaxFileDescriptors(expectedFds).build();

        assertThat(limits.getMaxFileDescriptors()).isEqualTo(expectedFds);
      }

      @Test
      @DisplayName("should set max disk reads per second")
      void shouldSetMaxDiskReadsPerSecond() {
        final long expectedReads = 5000L;
        final WasiResourceLimits limits =
            WasiResourceLimits.builder().withMaxDiskReadsPerSecond(expectedReads).build();

        assertThat(limits.getMaxDiskReadsPerSecond()).isEqualTo(expectedReads);
      }

      @Test
      @DisplayName("should set max disk writes per second")
      void shouldSetMaxDiskWritesPerSecond() {
        final long expectedWrites = 3000L;
        final WasiResourceLimits limits =
            WasiResourceLimits.builder().withMaxDiskWritesPerSecond(expectedWrites).build();

        assertThat(limits.getMaxDiskWritesPerSecond()).isEqualTo(expectedWrites);
      }

      @Test
      @DisplayName("should set max disk read bytes per second")
      void shouldSetMaxDiskReadBytesPerSecond() {
        final long expectedBytes = 50L * 1024L * 1024L; // 50 MB/s
        final WasiResourceLimits limits =
            WasiResourceLimits.builder().withMaxDiskReadBytesPerSecond(expectedBytes).build();

        assertThat(limits.getMaxDiskReadBytesPerSecond()).isEqualTo(expectedBytes);
      }

      @Test
      @DisplayName("should set max disk write bytes per second")
      void shouldSetMaxDiskWriteBytesPerSecond() {
        final long expectedBytes = 25L * 1024L * 1024L; // 25 MB/s
        final WasiResourceLimits limits =
            WasiResourceLimits.builder().withMaxDiskWriteBytesPerSecond(expectedBytes).build();

        assertThat(limits.getMaxDiskWriteBytesPerSecond()).isEqualTo(expectedBytes);
      }

      @Test
      @DisplayName("should set max network connections")
      void shouldSetMaxNetworkConnections() {
        final int expectedConnections = 500;
        final WasiResourceLimits limits =
            WasiResourceLimits.builder().withMaxNetworkConnections(expectedConnections).build();

        assertThat(limits.getMaxNetworkConnections()).isEqualTo(expectedConnections);
      }

      @Test
      @DisplayName("should set max execution time")
      void shouldSetMaxExecutionTime() {
        final Duration expectedTime = Duration.ofMinutes(10);
        final WasiResourceLimits limits =
            WasiResourceLimits.builder().withMaxExecutionTime(expectedTime).build();

        assertThat(limits.getMaxExecutionTime()).isEqualTo(expectedTime);
      }

      @Test
      @DisplayName("should set max CPU time")
      void shouldSetMaxCpuTime() {
        final Duration expectedTime = Duration.ofSeconds(30);
        final WasiResourceLimits limits =
            WasiResourceLimits.builder().withMaxCpuTime(expectedTime).build();

        assertThat(limits.getMaxCpuTime()).isEqualTo(expectedTime);
      }

      @Test
      @DisplayName("should set max wall clock time")
      void shouldSetMaxWallClockTime() {
        final Duration expectedTime = Duration.ofHours(2);
        final WasiResourceLimits limits =
            WasiResourceLimits.builder().withMaxWallClockTime(expectedTime).build();

        assertThat(limits.getMaxWallClockTime()).isEqualTo(expectedTime);
      }

      @Test
      @DisplayName("should set max file size")
      void shouldSetMaxFileSize() {
        final long expectedSize = 500L * 1024L * 1024L; // 500 MB
        final WasiResourceLimits limits =
            WasiResourceLimits.builder().withMaxFileSize(expectedSize).build();

        assertThat(limits.getMaxFileSize()).isEqualTo(expectedSize);
      }

      @Test
      @DisplayName("should set max disk space usage")
      void shouldSetMaxDiskSpaceUsage() {
        final long expectedSpace = 5L * 1024L * 1024L * 1024L; // 5 GB
        final WasiResourceLimits limits =
            WasiResourceLimits.builder().withMaxDiskSpaceUsage(expectedSpace).build();

        assertThat(limits.getMaxDiskSpaceUsage()).isEqualTo(expectedSpace);
      }

      @Test
      @DisplayName("should support method chaining")
      void shouldSupportMethodChaining() {
        final WasiResourceLimits limits =
            WasiResourceLimits.builder()
                .withMaxMemoryBytes(100L * 1024L * 1024L)
                .withMaxFileDescriptors(512)
                .withMaxDiskReadsPerSecond(2000)
                .withMaxDiskWritesPerSecond(1000)
                .withMaxNetworkConnections(50)
                .withMaxExecutionTime(Duration.ofMinutes(3))
                .build();

        assertThat(limits.getMaxMemoryBytes()).isEqualTo(100L * 1024L * 1024L);
        assertThat(limits.getMaxFileDescriptors()).isEqualTo(512);
        assertThat(limits.getMaxDiskReadsPerSecond()).isEqualTo(2000);
        assertThat(limits.getMaxDiskWritesPerSecond()).isEqualTo(1000);
        assertThat(limits.getMaxNetworkConnections()).isEqualTo(50);
        assertThat(limits.getMaxExecutionTime()).isEqualTo(Duration.ofMinutes(3));
      }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

      @Test
      @DisplayName("defaultLimits should create moderate restrictions")
      void defaultLimitsShouldCreateModerateRestrictions() {
        final WasiResourceLimits limits = WasiResourceLimits.defaultLimits();

        assertThat(limits.getMaxMemoryBytes()).isEqualTo(256L * 1024L * 1024L); // 256 MB
        assertThat(limits.getMaxFileDescriptors()).isEqualTo(1024);
        assertThat(limits.getMaxDiskReadsPerSecond()).isEqualTo(1000);
        assertThat(limits.getMaxDiskWritesPerSecond()).isEqualTo(1000);
        assertThat(limits.getMaxDiskReadBytesPerSecond()).isEqualTo(10L * 1024L * 1024L); // 10 MB/s
        assertThat(limits.getMaxDiskWriteBytesPerSecond())
            .isEqualTo(10L * 1024L * 1024L); // 10 MB/s
        assertThat(limits.getMaxNetworkConnections()).isEqualTo(100);
        assertThat(limits.getMaxExecutionTime()).isEqualTo(Duration.ofMinutes(5));
        assertThat(limits.getMaxFileSize()).isEqualTo(100L * 1024L * 1024L); // 100 MB
        assertThat(limits.getMaxDiskSpaceUsage()).isEqualTo(1L * 1024L * 1024L * 1024L); // 1 GB
      }

      @Test
      @DisplayName("restrictiveLimits should create tight restrictions")
      void restrictiveLimitsShouldCreateTightRestrictions() {
        final WasiResourceLimits limits = WasiResourceLimits.restrictiveLimits();

        assertThat(limits.getMaxMemoryBytes()).isEqualTo(64L * 1024L * 1024L); // 64 MB
        assertThat(limits.getMaxFileDescriptors()).isEqualTo(256);
        assertThat(limits.getMaxDiskReadsPerSecond()).isEqualTo(100);
        assertThat(limits.getMaxDiskWritesPerSecond()).isEqualTo(100);
        assertThat(limits.getMaxDiskReadBytesPerSecond()).isEqualTo(1024L * 1024L); // 1 MB/s
        assertThat(limits.getMaxDiskWriteBytesPerSecond()).isEqualTo(1024L * 1024L); // 1 MB/s
        assertThat(limits.getMaxNetworkConnections()).isEqualTo(10);
        assertThat(limits.getMaxExecutionTime()).isEqualTo(Duration.ofMinutes(1));
        assertThat(limits.getMaxFileSize()).isEqualTo(10L * 1024L * 1024L); // 10 MB
        assertThat(limits.getMaxDiskSpaceUsage()).isEqualTo(100L * 1024L * 1024L); // 100 MB
      }

      @Test
      @DisplayName("permissiveLimits should create generous restrictions")
      void permissiveLimitsShouldCreateGenerousRestrictions() {
        final WasiResourceLimits limits = WasiResourceLimits.permissiveLimits();

        assertThat(limits.getMaxMemoryBytes()).isEqualTo(2L * 1024L * 1024L * 1024L); // 2 GB
        assertThat(limits.getMaxFileDescriptors()).isEqualTo(8192);
        assertThat(limits.getMaxDiskReadsPerSecond()).isEqualTo(10000);
        assertThat(limits.getMaxDiskWritesPerSecond()).isEqualTo(10000);
        assertThat(limits.getMaxDiskReadBytesPerSecond())
            .isEqualTo(100L * 1024L * 1024L); // 100 MB/s
        assertThat(limits.getMaxDiskWriteBytesPerSecond())
            .isEqualTo(100L * 1024L * 1024L); // 100 MB/s
        assertThat(limits.getMaxNetworkConnections()).isEqualTo(1000);
        assertThat(limits.getMaxExecutionTime()).isEqualTo(Duration.ofHours(1));
        assertThat(limits.getMaxFileSize()).isEqualTo(1L * 1024L * 1024L * 1024L); // 1 GB
        assertThat(limits.getMaxDiskSpaceUsage()).isEqualTo(10L * 1024L * 1024L * 1024L); // 10 GB
      }

      @Test
      @DisplayName("unlimitedLimits should create no restrictions")
      void unlimitedLimitsShouldCreateNoRestrictions() {
        final WasiResourceLimits limits = WasiResourceLimits.unlimitedLimits();

        assertThat(limits.getMaxMemoryBytes()).isEqualTo(WasiResourceLimits.UNLIMITED);
        assertThat(limits.getMaxFileDescriptors()).isEqualTo((int) WasiResourceLimits.UNLIMITED);
        assertThat(limits.getMaxDiskReadsPerSecond()).isEqualTo(WasiResourceLimits.UNLIMITED);
        assertThat(limits.getMaxDiskWritesPerSecond()).isEqualTo(WasiResourceLimits.UNLIMITED);
        assertThat(limits.getMaxDiskReadBytesPerSecond()).isEqualTo(WasiResourceLimits.UNLIMITED);
        assertThat(limits.getMaxDiskWriteBytesPerSecond()).isEqualTo(WasiResourceLimits.UNLIMITED);
        assertThat(limits.getMaxNetworkConnections()).isEqualTo((int) WasiResourceLimits.UNLIMITED);
        assertThat(limits.getMaxExecutionTime()).isNull();
        assertThat(limits.getMaxCpuTime()).isNull();
        assertThat(limits.getMaxWallClockTime()).isNull();
        assertThat(limits.getMaxFileSize()).isEqualTo(WasiResourceLimits.UNLIMITED);
        assertThat(limits.getMaxDiskSpaceUsage()).isEqualTo(WasiResourceLimits.UNLIMITED);
      }
    }

    @Nested
    @DisplayName("isLimited Methods Tests")
    class IsLimitedMethodsTests {

      @Test
      @DisplayName("isMemoryLimited should return false when unlimited")
      void isMemoryLimitedShouldReturnFalseWhenUnlimited() {
        final WasiResourceLimits limits = WasiResourceLimits.unlimitedLimits();
        assertThat(limits.isMemoryLimited()).isFalse();
      }

      @Test
      @DisplayName("isMemoryLimited should return true when limited")
      void isMemoryLimitedShouldReturnTrueWhenLimited() {
        final WasiResourceLimits limits = WasiResourceLimits.defaultLimits();
        assertThat(limits.isMemoryLimited()).isTrue();
      }

      @Test
      @DisplayName("areFileDescriptorsLimited should return false when unlimited")
      void areFileDescriptorsLimitedShouldReturnFalseWhenUnlimited() {
        final WasiResourceLimits limits = WasiResourceLimits.unlimitedLimits();
        assertThat(limits.areFileDescriptorsLimited()).isFalse();
      }

      @Test
      @DisplayName("areFileDescriptorsLimited should return true when limited")
      void areFileDescriptorsLimitedShouldReturnTrueWhenLimited() {
        final WasiResourceLimits limits = WasiResourceLimits.defaultLimits();
        assertThat(limits.areFileDescriptorsLimited()).isTrue();
      }

      @Test
      @DisplayName("isDiskIoLimited should return false when all unlimited")
      void isDiskIoLimitedShouldReturnFalseWhenAllUnlimited() {
        final WasiResourceLimits limits = WasiResourceLimits.unlimitedLimits();
        assertThat(limits.isDiskIoLimited()).isFalse();
      }

      @Test
      @DisplayName("isDiskIoLimited should return true when any disk limit set")
      void isDiskIoLimitedShouldReturnTrueWhenAnyDiskLimitSet() {
        final WasiResourceLimits limits =
            WasiResourceLimits.builder().withMaxDiskReadsPerSecond(100).build();
        assertThat(limits.isDiskIoLimited()).isTrue();
      }

      @Test
      @DisplayName("areNetworkConnectionsLimited should return false when unlimited")
      void areNetworkConnectionsLimitedShouldReturnFalseWhenUnlimited() {
        final WasiResourceLimits limits = WasiResourceLimits.unlimitedLimits();
        assertThat(limits.areNetworkConnectionsLimited()).isFalse();
      }

      @Test
      @DisplayName("areNetworkConnectionsLimited should return true when limited")
      void areNetworkConnectionsLimitedShouldReturnTrueWhenLimited() {
        final WasiResourceLimits limits = WasiResourceLimits.defaultLimits();
        assertThat(limits.areNetworkConnectionsLimited()).isTrue();
      }

      @Test
      @DisplayName("isExecutionTimeLimited should return false when all null")
      void isExecutionTimeLimitedShouldReturnFalseWhenAllNull() {
        final WasiResourceLimits limits = WasiResourceLimits.unlimitedLimits();
        assertThat(limits.isExecutionTimeLimited()).isFalse();
      }

      @Test
      @DisplayName("isExecutionTimeLimited should return true when execution time set")
      void isExecutionTimeLimitedShouldReturnTrueWhenExecutionTimeSet() {
        final WasiResourceLimits limits =
            WasiResourceLimits.builder().withMaxExecutionTime(Duration.ofMinutes(5)).build();
        assertThat(limits.isExecutionTimeLimited()).isTrue();
      }

      @Test
      @DisplayName("isExecutionTimeLimited should return true when CPU time set")
      void isExecutionTimeLimitedShouldReturnTrueWhenCpuTimeSet() {
        final WasiResourceLimits limits =
            WasiResourceLimits.builder().withMaxCpuTime(Duration.ofSeconds(30)).build();
        assertThat(limits.isExecutionTimeLimited()).isTrue();
      }

      @Test
      @DisplayName("isExecutionTimeLimited should return true when wall clock time set")
      void isExecutionTimeLimitedShouldReturnTrueWhenWallClockTimeSet() {
        final WasiResourceLimits limits =
            WasiResourceLimits.builder().withMaxWallClockTime(Duration.ofHours(1)).build();
        assertThat(limits.isExecutionTimeLimited()).isTrue();
      }

      @Test
      @DisplayName("areFileOperationsLimited should return false when all unlimited")
      void areFileOperationsLimitedShouldReturnFalseWhenAllUnlimited() {
        final WasiResourceLimits limits = WasiResourceLimits.unlimitedLimits();
        assertThat(limits.areFileOperationsLimited()).isFalse();
      }

      @Test
      @DisplayName("areFileOperationsLimited should return true when file size limited")
      void areFileOperationsLimitedShouldReturnTrueWhenFileSizeLimited() {
        final WasiResourceLimits limits =
            WasiResourceLimits.builder().withMaxFileSize(100L * 1024L * 1024L).build();
        assertThat(limits.areFileOperationsLimited()).isTrue();
      }

      @Test
      @DisplayName("areFileOperationsLimited should return true when disk space limited")
      void areFileOperationsLimitedShouldReturnTrueWhenDiskSpaceLimited() {
        final WasiResourceLimits limits =
            WasiResourceLimits.builder().withMaxDiskSpaceUsage(1L * 1024L * 1024L * 1024L).build();
        assertThat(limits.areFileOperationsLimited()).isTrue();
      }
    }

    @Nested
    @DisplayName("toString Tests")
    class ToStringTests {

      @Test
      @DisplayName("toString should include all resource fields")
      void toStringShouldIncludeAllResourceFields() {
        final WasiResourceLimits limits = WasiResourceLimits.defaultLimits();
        final String str = limits.toString();

        assertThat(str).contains("WasiResourceLimits");
        assertThat(str).contains("memory=");
        assertThat(str).contains("fileDescriptors=");
        assertThat(str).contains("diskReads=");
        assertThat(str).contains("diskWrites=");
        assertThat(str).contains("networkConnections=");
        assertThat(str).contains("executionTime=");
      }

      @Test
      @DisplayName("toString should show unlimited for unlimited values")
      void toStringShouldShowUnlimitedForUnlimitedValues() {
        final WasiResourceLimits limits = WasiResourceLimits.unlimitedLimits();
        final String str = limits.toString();

        assertThat(str).contains("unlimited");
      }

      @Test
      @DisplayName("toString should format bytes with units")
      void toStringShouldFormatBytesWithUnits() {
        final WasiResourceLimits limits = WasiResourceLimits.defaultLimits();
        final String str = limits.toString();

        // Default limits should show MB for memory
        assertThat(str).containsPattern("\\d+\\.\\d+ [KMGT]?B");
      }
    }
  }

  // ============================================================================
  // WasiFileOperation Tests
  // ============================================================================

  @Nested
  @DisplayName("WasiFileOperation Tests")
  class WasiFileOperationTests {

    @Nested
    @DisplayName("Enum Value Tests")
    class EnumValueTests {

      @Test
      @DisplayName("should have expected number of values")
      void shouldHaveExpectedNumberOfValues() {
        final WasiFileOperation[] values = WasiFileOperation.values();
        assertThat(values.length).isEqualTo(23);
      }

      @Test
      @DisplayName("should contain READ operation")
      void shouldContainReadOperation() {
        assertThat(WasiFileOperation.valueOf("READ")).isEqualTo(WasiFileOperation.READ);
      }

      @Test
      @DisplayName("should contain WRITE operation")
      void shouldContainWriteOperation() {
        assertThat(WasiFileOperation.valueOf("WRITE")).isEqualTo(WasiFileOperation.WRITE);
      }

      @Test
      @DisplayName("should contain EXECUTE operation")
      void shouldContainExecuteOperation() {
        assertThat(WasiFileOperation.valueOf("EXECUTE")).isEqualTo(WasiFileOperation.EXECUTE);
      }

      @Test
      @DisplayName("should contain DELETE operation")
      void shouldContainDeleteOperation() {
        assertThat(WasiFileOperation.valueOf("DELETE")).isEqualTo(WasiFileOperation.DELETE);
      }
    }

    @Nested
    @DisplayName("Operation ID Tests")
    class OperationIdTests {

      @Test
      @DisplayName("READ should have correct operation ID")
      void readShouldHaveCorrectOperationId() {
        assertThat(WasiFileOperation.READ.getOperationId()).isEqualTo("read");
      }

      @Test
      @DisplayName("WRITE should have correct operation ID")
      void writeShouldHaveCorrectOperationId() {
        assertThat(WasiFileOperation.WRITE.getOperationId()).isEqualTo("write");
      }

      @Test
      @DisplayName("EXECUTE should have correct operation ID")
      void executeShouldHaveCorrectOperationId() {
        assertThat(WasiFileOperation.EXECUTE.getOperationId()).isEqualTo("execute");
      }

      @Test
      @DisplayName("CREATE_DIRECTORY should have correct operation ID")
      void createDirectoryShouldHaveCorrectOperationId() {
        assertThat(WasiFileOperation.CREATE_DIRECTORY.getOperationId())
            .isEqualTo("create_directory");
      }
    }

    @Nested
    @DisplayName("Description Tests")
    class DescriptionTests {

      @Test
      @DisplayName("READ should have description")
      void readShouldHaveDescription() {
        assertThat(WasiFileOperation.READ.getDescription())
            .isEqualTo("Reading file or directory contents");
      }

      @Test
      @DisplayName("WRITE should have description")
      void writeShouldHaveDescription() {
        assertThat(WasiFileOperation.WRITE.getDescription())
            .isEqualTo("Writing file contents or creating files");
      }

      @Test
      @DisplayName("all operations should have non-empty descriptions")
      void allOperationsShouldHaveNonEmptyDescriptions() {
        for (final WasiFileOperation op : WasiFileOperation.values()) {
          assertThat(op.getDescription()).as("Description for %s", op.name()).isNotEmpty();
        }
      }
    }

    @Nested
    @DisplayName("requiresReadAccess Tests")
    class RequiresReadAccessTests {

      @Test
      @DisplayName("READ should require read access")
      void readShouldRequireReadAccess() {
        assertThat(WasiFileOperation.READ.requiresReadAccess()).isTrue();
      }

      @Test
      @DisplayName("METADATA should require read access")
      void metadataShouldRequireReadAccess() {
        assertThat(WasiFileOperation.METADATA.requiresReadAccess()).isTrue();
      }

      @Test
      @DisplayName("LIST_DIRECTORY should require read access")
      void listDirectoryShouldRequireReadAccess() {
        assertThat(WasiFileOperation.LIST_DIRECTORY.requiresReadAccess()).isTrue();
      }

      @Test
      @DisplayName("READ_ONLY should require read access")
      void readOnlyShouldRequireReadAccess() {
        assertThat(WasiFileOperation.READ_ONLY.requiresReadAccess()).isTrue();
      }

      @Test
      @DisplayName("READ_WRITE should require read access")
      void readWriteShouldRequireReadAccess() {
        assertThat(WasiFileOperation.READ_WRITE.requiresReadAccess()).isTrue();
      }

      @Test
      @DisplayName("WRITE should not require read access")
      void writeShouldNotRequireReadAccess() {
        assertThat(WasiFileOperation.WRITE.requiresReadAccess()).isFalse();
      }

      @Test
      @DisplayName("DELETE should not require read access")
      void deleteShouldNotRequireReadAccess() {
        assertThat(WasiFileOperation.DELETE.requiresReadAccess()).isFalse();
      }
    }

    @Nested
    @DisplayName("requiresWriteAccess Tests")
    class RequiresWriteAccessTests {

      @Test
      @DisplayName("WRITE should require write access")
      void writeShouldRequireWriteAccess() {
        assertThat(WasiFileOperation.WRITE.requiresWriteAccess()).isTrue();
      }

      @Test
      @DisplayName("CREATE_DIRECTORY should require write access")
      void createDirectoryShouldRequireWriteAccess() {
        assertThat(WasiFileOperation.CREATE_DIRECTORY.requiresWriteAccess()).isTrue();
      }

      @Test
      @DisplayName("DELETE should require write access")
      void deleteShouldRequireWriteAccess() {
        assertThat(WasiFileOperation.DELETE.requiresWriteAccess()).isTrue();
      }

      @Test
      @DisplayName("TRUNCATE should require write access")
      void truncateShouldRequireWriteAccess() {
        assertThat(WasiFileOperation.TRUNCATE.requiresWriteAccess()).isTrue();
      }

      @Test
      @DisplayName("READ should not require write access")
      void readShouldNotRequireWriteAccess() {
        assertThat(WasiFileOperation.READ.requiresWriteAccess()).isFalse();
      }

      @Test
      @DisplayName("METADATA should not require write access")
      void metadataShouldNotRequireWriteAccess() {
        assertThat(WasiFileOperation.METADATA.requiresWriteAccess()).isFalse();
      }
    }

    @Nested
    @DisplayName("requiresExecuteAccess Tests")
    class RequiresExecuteAccessTests {

      @Test
      @DisplayName("EXECUTE should require execute access")
      void executeShouldRequireExecuteAccess() {
        assertThat(WasiFileOperation.EXECUTE.requiresExecuteAccess()).isTrue();
      }

      @Test
      @DisplayName("READ should not require execute access")
      void readShouldNotRequireExecuteAccess() {
        assertThat(WasiFileOperation.READ.requiresExecuteAccess()).isFalse();
      }

      @Test
      @DisplayName("WRITE should not require execute access")
      void writeShouldNotRequireExecuteAccess() {
        assertThat(WasiFileOperation.WRITE.requiresExecuteAccess()).isFalse();
      }
    }

    @Nested
    @DisplayName("isModifyingOperation Tests")
    class IsModifyingOperationTests {

      @Test
      @DisplayName("WRITE should be modifying operation")
      void writeShouldBeModifyingOperation() {
        assertThat(WasiFileOperation.WRITE.isModifyingOperation()).isTrue();
      }

      @Test
      @DisplayName("CREATE_DIRECTORY should be modifying operation")
      void createDirectoryShouldBeModifyingOperation() {
        assertThat(WasiFileOperation.CREATE_DIRECTORY.isModifyingOperation()).isTrue();
      }

      @Test
      @DisplayName("DELETE should be modifying operation")
      void deleteShouldBeModifyingOperation() {
        assertThat(WasiFileOperation.DELETE.isModifyingOperation()).isTrue();
      }

      @Test
      @DisplayName("RENAME should be modifying operation")
      void renameShouldBeModifyingOperation() {
        assertThat(WasiFileOperation.RENAME.isModifyingOperation()).isTrue();
      }

      @Test
      @DisplayName("TRUNCATE should be modifying operation")
      void truncateShouldBeModifyingOperation() {
        assertThat(WasiFileOperation.TRUNCATE.isModifyingOperation()).isTrue();
      }

      @Test
      @DisplayName("READ should not be modifying operation")
      void readShouldNotBeModifyingOperation() {
        assertThat(WasiFileOperation.READ.isModifyingOperation()).isFalse();
      }

      @Test
      @DisplayName("METADATA should not be modifying operation")
      void metadataShouldNotBeModifyingOperation() {
        assertThat(WasiFileOperation.METADATA.isModifyingOperation()).isFalse();
      }
    }

    @Nested
    @DisplayName("isDangerous Tests")
    class IsDangerousTests {

      @Test
      @DisplayName("EXECUTE should be dangerous")
      void executeShouldBeDangerous() {
        assertThat(WasiFileOperation.EXECUTE.isDangerous()).isTrue();
      }

      @Test
      @DisplayName("DELETE should be dangerous")
      void deleteShouldBeDangerous() {
        assertThat(WasiFileOperation.DELETE.isDangerous()).isTrue();
      }

      @Test
      @DisplayName("RENAME should be dangerous")
      void renameShouldBeDangerous() {
        assertThat(WasiFileOperation.RENAME.isDangerous()).isTrue();
      }

      @Test
      @DisplayName("CHANGE_PERMISSIONS should be dangerous")
      void changePermissionsShouldBeDangerous() {
        assertThat(WasiFileOperation.CHANGE_PERMISSIONS.isDangerous()).isTrue();
      }

      @Test
      @DisplayName("CREATE_LINK should be dangerous")
      void createLinkShouldBeDangerous() {
        assertThat(WasiFileOperation.CREATE_LINK.isDangerous()).isTrue();
      }

      @Test
      @DisplayName("READ should not be dangerous")
      void readShouldNotBeDangerous() {
        assertThat(WasiFileOperation.READ.isDangerous()).isFalse();
      }

      @Test
      @DisplayName("WRITE should not be dangerous")
      void writeShouldNotBeDangerous() {
        assertThat(WasiFileOperation.WRITE.isDangerous()).isFalse();
      }
    }

    @Nested
    @DisplayName("fromOperationId Tests")
    class FromOperationIdTests {

      @Test
      @DisplayName("should find operation by ID")
      void shouldFindOperationById() {
        assertThat(WasiFileOperation.fromOperationId("read")).isEqualTo(WasiFileOperation.READ);
        assertThat(WasiFileOperation.fromOperationId("write")).isEqualTo(WasiFileOperation.WRITE);
        assertThat(WasiFileOperation.fromOperationId("execute"))
            .isEqualTo(WasiFileOperation.EXECUTE);
        assertThat(WasiFileOperation.fromOperationId("delete")).isEqualTo(WasiFileOperation.DELETE);
      }

      @Test
      @DisplayName("should throw for null operation ID")
      void shouldThrowForNullOperationId() {
        assertThatThrownBy(() -> WasiFileOperation.fromOperationId(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be null or empty");
      }

      @Test
      @DisplayName("should throw for empty operation ID")
      void shouldThrowForEmptyOperationId() {
        assertThatThrownBy(() -> WasiFileOperation.fromOperationId(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be null or empty");
      }

      @Test
      @DisplayName("should throw for unknown operation ID")
      void shouldThrowForUnknownOperationId() {
        assertThatThrownBy(() -> WasiFileOperation.fromOperationId("unknown"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown WASI file operation");
      }
    }

    @Nested
    @DisplayName("toString Tests")
    class ToStringTests {

      @Test
      @DisplayName("toString should include name and description")
      void toStringShouldIncludeNameAndDescription() {
        final String str = WasiFileOperation.READ.toString();
        assertThat(str).contains("READ");
        assertThat(str).contains("Reading file or directory contents");
      }
    }
  }

  // ============================================================================
  // WasiPermissionManager Tests
  // ============================================================================

  @Nested
  @DisplayName("WasiPermissionManager Tests")
  class WasiPermissionManagerTests {

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

      @Test
      @DisplayName("should create builder via static method")
      void shouldCreateBuilderViaStaticMethod() {
        final WasiPermissionManager.Builder builder = WasiPermissionManager.builder();
        assertThat(builder).isNotNull();
      }

      @Test
      @DisplayName("should build permission manager with defaults")
      void shouldBuildPermissionManagerWithDefaults() {
        final WasiPermissionManager manager = WasiPermissionManager.builder().build();
        assertThat(manager).isNotNull();
        assertThat(manager.areDangerousOperationsAllowed()).isFalse();
        assertThat(manager.isStrictPathValidationEnabled()).isFalse();
      }

      @Test
      @DisplayName("should set global permission")
      void shouldSetGlobalPermission() {
        final WasiPermissionManager manager =
            WasiPermissionManager.builder().withGlobalPermission(WasiFileOperation.READ).build();
        assertThat(manager).isNotNull();
      }

      @Test
      @DisplayName("should set multiple global permissions")
      void shouldSetMultipleGlobalPermissions() {
        final WasiPermissionManager manager =
            WasiPermissionManager.builder()
                .withGlobalPermissions(WasiFileOperation.READ, WasiFileOperation.METADATA)
                .build();
        assertThat(manager).isNotNull();
      }

      @Test
      @DisplayName("should set all file operations")
      void shouldSetAllFileOperations() {
        final WasiPermissionManager manager =
            WasiPermissionManager.builder().withAllFileOperations().build();
        assertThat(manager).isNotNull();
      }

      @Test
      @DisplayName("should set path permission")
      void shouldSetPathPermission() {
        final Path path = Paths.get("/tmp");
        final WasiPermissionManager manager =
            WasiPermissionManager.builder()
                .withPathPermission(path, WasiFileOperation.READ)
                .build();
        assertThat(manager).isNotNull();
      }

      @Test
      @DisplayName("should set multiple path permissions")
      void shouldSetMultiplePathPermissions() {
        final Path path = Paths.get("/tmp");
        final WasiPermissionManager manager =
            WasiPermissionManager.builder()
                .withPathPermissions(path, WasiFileOperation.READ, WasiFileOperation.WRITE)
                .build();
        assertThat(manager).isNotNull();
      }

      @Test
      @DisplayName("should set environment variable")
      void shouldSetEnvironmentVariable() {
        final WasiPermissionManager manager =
            WasiPermissionManager.builder().withEnvironmentVariable("PATH").build();
        assertThat(manager).isNotNull();
      }

      @Test
      @DisplayName("should set all environment variables")
      void shouldSetAllEnvironmentVariables() {
        final WasiPermissionManager manager =
            WasiPermissionManager.builder().withAllEnvironmentVariables().build();
        assertThat(manager).isNotNull();
      }

      @Test
      @DisplayName("should set denied environment variable")
      void shouldSetDeniedEnvironmentVariable() {
        final WasiPermissionManager manager =
            WasiPermissionManager.builder().withDeniedEnvironmentVariable("SECRET_KEY").build();
        assertThat(manager).isNotNull();
      }

      @Test
      @DisplayName("should set resource limits")
      void shouldSetResourceLimits() {
        final WasiResourceLimits limits = WasiResourceLimits.defaultLimits();
        final WasiPermissionManager manager =
            WasiPermissionManager.builder().withResourceLimits(limits).build();
        assertThat(manager.getResourceLimits()).isSameAs(limits);
      }

      @Test
      @DisplayName("should set dangerous operations flag")
      void shouldSetDangerousOperationsFlag() {
        final WasiPermissionManager manager =
            WasiPermissionManager.builder().withDangerousOperations(true).build();
        assertThat(manager.areDangerousOperationsAllowed()).isTrue();
      }

      @Test
      @DisplayName("should set strict path validation flag")
      void shouldSetStrictPathValidationFlag() {
        final WasiPermissionManager manager =
            WasiPermissionManager.builder().withStrictPathValidation(true).build();
        assertThat(manager.isStrictPathValidationEnabled()).isTrue();
      }

      @Test
      @DisplayName("should support method chaining")
      void shouldSupportMethodChaining() {
        final WasiPermissionManager manager =
            WasiPermissionManager.builder()
                .withGlobalPermission(WasiFileOperation.READ)
                .withPathPermission(Paths.get("/tmp"), WasiFileOperation.WRITE)
                .withEnvironmentVariable("HOME")
                .withResourceLimits(WasiResourceLimits.defaultLimits())
                .withDangerousOperations(false)
                .withStrictPathValidation(false)
                .build();
        assertThat(manager).isNotNull();
      }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

      @Test
      @DisplayName("defaultManager should create manager with read permission")
      void defaultManagerShouldCreateManagerWithReadPermission() {
        final WasiPermissionManager manager = WasiPermissionManager.defaultManager();
        assertThat(manager).isNotNull();
        assertThat(manager.areDangerousOperationsAllowed()).isFalse();
        assertThat(manager.getResourceLimits()).isNotNull();
      }

      @Test
      @DisplayName("restrictiveManager should create manager with no permissions")
      void restrictiveManagerShouldCreateManagerWithNoPermissions() {
        final WasiPermissionManager manager = WasiPermissionManager.restrictiveManager();
        assertThat(manager).isNotNull();
        assertThat(manager.isStrictPathValidationEnabled()).isTrue();
        assertThat(manager.areDangerousOperationsAllowed()).isFalse();
      }

      @Test
      @DisplayName("permissiveManager should create manager with broad permissions")
      void permissiveManagerShouldCreateManagerWithBroadPermissions() {
        final WasiPermissionManager manager = WasiPermissionManager.permissiveManager();
        assertThat(manager).isNotNull();
        assertThat(manager.areDangerousOperationsAllowed()).isTrue();
      }
    }

    @Nested
    @DisplayName("File System Access Validation Tests")
    class FileSystemAccessValidationTests {

      @Test
      @DisplayName("should allow access with global read permission")
      void shouldAllowAccessWithGlobalReadPermission() {
        final WasiPermissionManager manager = WasiPermissionManager.defaultManager();
        final Path path = Paths.get("/tmp/test.txt");

        // Should not throw - global READ permission is granted
        manager.validateFileSystemAccess(path, WasiFileOperation.READ);
      }

      @Test
      @DisplayName("should deny access without permission")
      void shouldDenyAccessWithoutPermission() {
        final WasiPermissionManager manager = WasiPermissionManager.restrictiveManager();
        final Path path = Paths.get("/tmp/test.txt");

        assertThatThrownBy(() -> manager.validateFileSystemAccess(path, WasiFileOperation.READ))
            .isInstanceOf(JniException.class)
            .hasMessageContaining("File system access denied");
      }

      @Test
      @DisplayName("should allow access with path permission")
      void shouldAllowAccessWithPathPermission() {
        final Path tmpDir = Paths.get("/tmp");
        final WasiPermissionManager manager =
            WasiPermissionManager.builder()
                .withPathPermission(tmpDir, WasiFileOperation.WRITE)
                .build();

        // Should not throw - path permission grants WRITE
        manager.validateFileSystemAccess(Paths.get("/tmp/test.txt"), WasiFileOperation.WRITE);
      }

      @Test
      @DisplayName("should deny dangerous operation when not allowed")
      void shouldDenyDangerousOperationWhenNotAllowed() {
        final WasiPermissionManager manager =
            WasiPermissionManager.builder()
                .withGlobalPermission(WasiFileOperation.DELETE)
                .withDangerousOperations(false)
                .build();
        final Path path = Paths.get("/tmp/test.txt");

        assertThatThrownBy(() -> manager.validateFileSystemAccess(path, WasiFileOperation.DELETE))
            .isInstanceOf(JniException.class)
            .hasMessageContaining("Dangerous file operation");
      }

      @Test
      @DisplayName("should allow dangerous operation when explicitly allowed")
      void shouldAllowDangerousOperationWhenExplicitlyAllowed() {
        final WasiPermissionManager manager =
            WasiPermissionManager.builder()
                .withGlobalPermission(WasiFileOperation.DELETE)
                .withDangerousOperations(true)
                .build();
        final Path path = Paths.get("/tmp/test.txt");

        // Should not throw
        manager.validateFileSystemAccess(path, WasiFileOperation.DELETE);
      }

      @Test
      @DisplayName("should validate general access with global permissions")
      void shouldValidateGeneralAccessWithGlobalPermissions() {
        final WasiPermissionManager manager = WasiPermissionManager.defaultManager();
        final Path path = Paths.get("/tmp/test.txt");

        // Should not throw - has global permissions
        manager.validateFileSystemAccess(path);
      }

      @Test
      @DisplayName("should deny general access without permissions")
      void shouldDenyGeneralAccessWithoutPermissions() {
        final WasiPermissionManager manager = WasiPermissionManager.restrictiveManager();
        final Path path = Paths.get("/tmp/test.txt");

        assertThatThrownBy(() -> manager.validateFileSystemAccess(path))
            .isInstanceOf(JniException.class)
            .hasMessageContaining("File system access denied");
      }
    }

    @Nested
    @DisplayName("Environment Access Validation Tests")
    class EnvironmentAccessValidationTests {

      @Test
      @DisplayName("should allow access to explicitly allowed variable")
      void shouldAllowAccessToExplicitlyAllowedVariable() {
        final WasiPermissionManager manager =
            WasiPermissionManager.builder().withEnvironmentVariable("PATH").build();

        // Should not throw
        manager.validateEnvironmentAccess("PATH");
      }

      @Test
      @DisplayName("should deny access to explicitly denied variable")
      void shouldDenyAccessToExplicitlyDeniedVariable() {
        final WasiPermissionManager manager =
            WasiPermissionManager.builder()
                .withAllEnvironmentVariables()
                .withDeniedEnvironmentVariable("SECRET_KEY")
                .build();

        assertThatThrownBy(() -> manager.validateEnvironmentAccess("SECRET_KEY"))
            .isInstanceOf(JniException.class)
            .hasMessageContaining("Environment variable access denied");
      }

      @Test
      @DisplayName("should allow access with wildcard pattern")
      void shouldAllowAccessWithWildcardPattern() {
        final WasiPermissionManager manager =
            WasiPermissionManager.builder().withEnvironmentVariable("MY_APP_*").build();

        // Should not throw - matches pattern
        manager.validateEnvironmentAccess("MY_APP_CONFIG");
      }

      @Test
      @DisplayName("should deny access when not in allow list")
      void shouldDenyAccessWhenNotInAllowList() {
        final WasiPermissionManager manager =
            WasiPermissionManager.builder().withEnvironmentVariable("PATH").build();

        assertThatThrownBy(() -> manager.validateEnvironmentAccess("HOME"))
            .isInstanceOf(JniException.class)
            .hasMessageContaining("Environment variable access denied");
      }

      @Test
      @DisplayName("should allow all access when no allow list configured")
      void shouldAllowAllAccessWhenNoAllowListConfigured() {
        final WasiPermissionManager manager = WasiPermissionManager.builder().build();

        // Should not throw - permissive mode when no allow list
        manager.validateEnvironmentAccess("ANY_VARIABLE");
      }
    }

    @Nested
    @DisplayName("Resource Limits Tests")
    class ResourceLimitsTests {

      @Test
      @DisplayName("should return configured resource limits")
      void shouldReturnConfiguredResourceLimits() {
        final WasiResourceLimits limits = WasiResourceLimits.restrictiveLimits();
        final WasiPermissionManager manager =
            WasiPermissionManager.builder().withResourceLimits(limits).build();

        assertThat(manager.getResourceLimits()).isSameAs(limits);
      }

      @Test
      @DisplayName("should have default resource limits if not configured")
      void shouldHaveDefaultResourceLimitsIfNotConfigured() {
        final WasiPermissionManager manager = WasiPermissionManager.builder().build();
        assertThat(manager.getResourceLimits()).isNotNull();
      }
    }

    @Nested
    @DisplayName("Configuration Flag Tests")
    class ConfigurationFlagTests {

      @Test
      @DisplayName("areDangerousOperationsAllowed should return configured value")
      void areDangerousOperationsAllowedShouldReturnConfiguredValue() {
        final WasiPermissionManager managerFalse =
            WasiPermissionManager.builder().withDangerousOperations(false).build();
        final WasiPermissionManager managerTrue =
            WasiPermissionManager.builder().withDangerousOperations(true).build();

        assertThat(managerFalse.areDangerousOperationsAllowed()).isFalse();
        assertThat(managerTrue.areDangerousOperationsAllowed()).isTrue();
      }

      @Test
      @DisplayName("isStrictPathValidationEnabled should return configured value")
      void isStrictPathValidationEnabledShouldReturnConfiguredValue() {
        final WasiPermissionManager managerFalse =
            WasiPermissionManager.builder().withStrictPathValidation(false).build();
        final WasiPermissionManager managerTrue =
            WasiPermissionManager.builder().withStrictPathValidation(true).build();

        assertThat(managerFalse.isStrictPathValidationEnabled()).isFalse();
        assertThat(managerTrue.isStrictPathValidationEnabled()).isTrue();
      }
    }
  }
}
