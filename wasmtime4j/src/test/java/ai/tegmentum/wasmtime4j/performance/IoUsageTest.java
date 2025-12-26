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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IoUsage} class.
 *
 * <p>IoUsage provides detailed I/O usage information for monitoring disk operations.
 */
@DisplayName("IoUsage Tests")
class IoUsageTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(IoUsage.class.getModifiers()), "IoUsage should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(IoUsage.class.getModifiers()), "IoUsage should be public");
    }

    @Test
    @DisplayName("should have constructor with all parameters")
    void shouldHaveConstructorWithAllParameters() throws NoSuchMethodException {
      final Constructor<?> constructor =
          IoUsage.class.getConstructor(
              long.class, long.class, long.class, long.class, Duration.class, double.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getBytesRead method")
    void shouldHaveGetBytesReadMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("getBytesRead");
      assertNotNull(method, "getBytesRead method should exist");
      assertEquals(long.class, method.getReturnType(), "getBytesRead should return long");
    }

    @Test
    @DisplayName("should have getBytesWritten method")
    void shouldHaveGetBytesWrittenMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("getBytesWritten");
      assertNotNull(method, "getBytesWritten method should exist");
      assertEquals(long.class, method.getReturnType(), "getBytesWritten should return long");
    }

    @Test
    @DisplayName("should have getReadOperations method")
    void shouldHaveGetReadOperationsMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("getReadOperations");
      assertNotNull(method, "getReadOperations method should exist");
      assertEquals(long.class, method.getReturnType(), "getReadOperations should return long");
    }

    @Test
    @DisplayName("should have getWriteOperations method")
    void shouldHaveGetWriteOperationsMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("getWriteOperations");
      assertNotNull(method, "getWriteOperations method should exist");
      assertEquals(long.class, method.getReturnType(), "getWriteOperations should return long");
    }

    @Test
    @DisplayName("should have getTotalIoTime method")
    void shouldHaveGetTotalIoTimeMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("getTotalIoTime");
      assertNotNull(method, "getTotalIoTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "getTotalIoTime should return Duration");
    }

    @Test
    @DisplayName("should have getAverageLatency method")
    void shouldHaveGetAverageLatencyMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("getAverageLatency");
      assertNotNull(method, "getAverageLatency method should exist");
      assertEquals(double.class, method.getReturnType(), "getAverageLatency should return double");
    }
  }

  @Nested
  @DisplayName("Derived Metric Method Tests")
  class DerivedMetricMethodTests {

    @Test
    @DisplayName("should have getTotalOperations method")
    void shouldHaveGetTotalOperationsMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("getTotalOperations");
      assertNotNull(method, "getTotalOperations method should exist");
      assertEquals(long.class, method.getReturnType(), "getTotalOperations should return long");
    }

    @Test
    @DisplayName("should have getTotalBytesTransferred method")
    void shouldHaveGetTotalBytesTransferredMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("getTotalBytesTransferred");
      assertNotNull(method, "getTotalBytesTransferred method should exist");
      assertEquals(
          long.class, method.getReturnType(), "getTotalBytesTransferred should return long");
    }

    @Test
    @DisplayName("should have getReadThroughput method")
    void shouldHaveGetReadThroughputMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("getReadThroughput", Duration.class);
      assertNotNull(method, "getReadThroughput method should exist");
      assertEquals(double.class, method.getReturnType(), "getReadThroughput should return double");
    }

    @Test
    @DisplayName("should have getWriteThroughput method")
    void shouldHaveGetWriteThroughputMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("getWriteThroughput", Duration.class);
      assertNotNull(method, "getWriteThroughput method should exist");
      assertEquals(double.class, method.getReturnType(), "getWriteThroughput should return double");
    }

    @Test
    @DisplayName("should have getOperationsPerSecond method")
    void shouldHaveGetOperationsPerSecondMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("getOperationsPerSecond", Duration.class);
      assertNotNull(method, "getOperationsPerSecond method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getOperationsPerSecond should return double");
    }

    @Test
    @DisplayName("should have getAverageBytesPerOperation method")
    void shouldHaveGetAverageBytesPerOperationMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("getAverageBytesPerOperation");
      assertNotNull(method, "getAverageBytesPerOperation method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getAverageBytesPerOperation should return double");
    }

    @Test
    @DisplayName("should have getReadWriteRatio method")
    void shouldHaveGetReadWriteRatioMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("getReadWriteRatio");
      assertNotNull(method, "getReadWriteRatio method should exist");
      assertEquals(double.class, method.getReturnType(), "getReadWriteRatio should return double");
    }
  }

  @Nested
  @DisplayName("Analysis Method Tests")
  class AnalysisMethodTests {

    @Test
    @DisplayName("should have isHighIoLoad method")
    void shouldHaveIsHighIoLoadMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("isHighIoLoad");
      assertNotNull(method, "isHighIoLoad method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isHighIoLoad should return boolean");
    }

    @Test
    @DisplayName("should have isSequentialIo method")
    void shouldHaveIsSequentialIoMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("isSequentialIo");
      assertNotNull(method, "isSequentialIo method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isSequentialIo should return boolean");
    }

    @Test
    @DisplayName("should have getUtilization method")
    void shouldHaveGetUtilizationMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("getUtilization");
      assertNotNull(method, "getUtilization method should exist");
      assertEquals(double.class, method.getReturnType(), "getUtilization should return double");
    }

    @Test
    @DisplayName("should have getEfficiencyScore method")
    void shouldHaveGetEfficiencyScoreMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("getEfficiencyScore", Duration.class);
      assertNotNull(method, "getEfficiencyScore method should exist");
      assertEquals(double.class, method.getReturnType(), "getEfficiencyScore should return double");
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have formatBytes static method")
    void shouldHaveFormatBytesStaticMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("formatBytes", long.class);
      assertNotNull(method, "formatBytes method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "formatBytes should be static");
      assertEquals(String.class, method.getReturnType(), "formatBytes should return String");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = IoUsage.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
    }
  }

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    @Test
    @DisplayName("should create instance with valid parameters")
    void shouldCreateInstanceWithValidParameters() {
      final IoUsage ioUsage =
          new IoUsage(
              1024L * 1024L, // bytesRead
              512L * 1024L, // bytesWritten
              100L, // readOperations
              50L, // writeOperations
              Duration.ofSeconds(10), // totalIoTime
              25.0); // averageLatency

      assertEquals(1024L * 1024L, ioUsage.getBytesRead(), "Bytes read should match");
      assertEquals(512L * 1024L, ioUsage.getBytesWritten(), "Bytes written should match");
      assertEquals(100L, ioUsage.getReadOperations(), "Read operations should match");
      assertEquals(50L, ioUsage.getWriteOperations(), "Write operations should match");
      assertEquals(Duration.ofSeconds(10), ioUsage.getTotalIoTime(), "Total I/O time should match");
      assertEquals(25.0, ioUsage.getAverageLatency(), 0.001, "Average latency should match");
    }

    @Test
    @DisplayName("should calculate total operations correctly")
    void shouldCalculateTotalOperationsCorrectly() {
      final IoUsage ioUsage = new IoUsage(1024L, 512L, 100L, 50L, Duration.ofSeconds(10), 25.0);

      assertEquals(150L, ioUsage.getTotalOperations(), "Total operations should be 150");
    }

    @Test
    @DisplayName("should calculate total bytes transferred correctly")
    void shouldCalculateTotalBytesTransferredCorrectly() {
      final IoUsage ioUsage = new IoUsage(1024L, 512L, 100L, 50L, Duration.ofSeconds(10), 25.0);

      assertEquals(1536L, ioUsage.getTotalBytesTransferred(), "Total bytes should be 1536");
    }
  }
}
