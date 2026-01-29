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

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for WASI Operations classes.
 *
 * <p>This test class covers:
 *
 * <ul>
 *   <li>{@link WasiTimeOperations} - Time and clock operations
 *   <li>{@link WasiRandomOperations} - Random number generation
 *   <li>{@link WasiNetworkOperations} - Network socket operations
 *   <li>{@link WasiProcessOperations} - Process and environment operations
 * </ul>
 */
@DisplayName("WASI Operations Tests")
class WasiOperationsTest {

  @Nested
  @DisplayName("WasiTimeOperations Tests")
  class WasiTimeOperationsTests {

    @Nested
    @DisplayName("Class Structure Tests")
    class ClassStructureTests {

      @Test
      @DisplayName("should be a final class")
      void shouldBeFinalClass() {
        assertThat(Modifier.isFinal(WasiTimeOperations.class.getModifiers()))
            .as("WasiTimeOperations should be final")
            .isTrue();
      }

      @Test
      @DisplayName("should be public")
      void shouldBePublic() {
        assertThat(Modifier.isPublic(WasiTimeOperations.class.getModifiers()))
            .as("WasiTimeOperations should be public")
            .isTrue();
      }

      @Test
      @DisplayName("should have private LOGGER field")
      void shouldHaveLoggerField() throws NoSuchFieldException {
        final Field field = WasiTimeOperations.class.getDeclaredField("LOGGER");
        assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Clock Constants Tests")
    class ClockConstantsTests {

      @Test
      @DisplayName("should have WASI_CLOCK_REALTIME constant")
      void shouldHaveRealtimeConstant() throws NoSuchFieldException {
        final Field field = WasiTimeOperations.class.getField("WASI_CLOCK_REALTIME");
        assertThat(Modifier.isPublic(field.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
        assertThat(field.getType()).isEqualTo(int.class);
      }

      @Test
      @DisplayName("should have WASI_CLOCK_MONOTONIC constant")
      void shouldHaveMonotonicConstant() throws NoSuchFieldException {
        final Field field = WasiTimeOperations.class.getField("WASI_CLOCK_MONOTONIC");
        assertThat(Modifier.isPublic(field.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
        assertThat(field.getType()).isEqualTo(int.class);
      }

      @Test
      @DisplayName("should have WASI_CLOCK_PROCESS_CPUTIME_ID constant")
      void shouldHaveProcessCputimeConstant() throws NoSuchFieldException {
        final Field field = WasiTimeOperations.class.getField("WASI_CLOCK_PROCESS_CPUTIME_ID");
        assertThat(Modifier.isPublic(field.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
        assertThat(field.getType()).isEqualTo(int.class);
      }

      @Test
      @DisplayName("should have WASI_CLOCK_THREAD_CPUTIME_ID constant")
      void shouldHaveThreadCputimeConstant() throws NoSuchFieldException {
        final Field field = WasiTimeOperations.class.getField("WASI_CLOCK_THREAD_CPUTIME_ID");
        assertThat(Modifier.isPublic(field.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
        assertThat(field.getType()).isEqualTo(int.class);
      }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

      @Test
      @DisplayName("should have constructor with WasiContext parameter")
      void shouldHaveWasiContextConstructor() throws NoSuchMethodException {
        final Constructor<?> constructor =
            WasiTimeOperations.class.getConstructor(WasiContext.class);
        assertThat(constructor).isNotNull();
        assertThat(Modifier.isPublic(constructor.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Method Tests")
    class MethodTests {

      @Test
      @DisplayName("should have getClockResolution method")
      void shouldHaveGetClockResolutionMethod() throws NoSuchMethodException {
        final Method method = WasiTimeOperations.class.getMethod("getClockResolution", int.class);
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have getCurrentTime method with precision")
      void shouldHaveGetCurrentTimeWithPrecision() throws NoSuchMethodException {
        final Method method =
            WasiTimeOperations.class.getMethod("getCurrentTime", int.class, long.class);
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have getCurrentTime method without precision")
      void shouldHaveGetCurrentTimeWithoutPrecision() throws NoSuchMethodException {
        final Method method = WasiTimeOperations.class.getMethod("getCurrentTime", int.class);
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have getRealtime method")
      void shouldHaveGetRealtimeMethod() throws NoSuchMethodException {
        final Method method = WasiTimeOperations.class.getMethod("getRealtime");
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have getMonotonicTime method")
      void shouldHaveGetMonotonicTimeMethod() throws NoSuchMethodException {
        final Method method = WasiTimeOperations.class.getMethod("getMonotonicTime");
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have getProcessCpuTime method")
      void shouldHaveGetProcessCpuTimeMethod() throws NoSuchMethodException {
        final Method method = WasiTimeOperations.class.getMethod("getProcessCpuTime");
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have getThreadCpuTime method")
      void shouldHaveGetThreadCpuTimeMethod() throws NoSuchMethodException {
        final Method method = WasiTimeOperations.class.getMethod("getThreadCpuTime");
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Static Method Tests")
    class StaticMethodTests {

      @Test
      @DisplayName("should have convertTime static method")
      void shouldHaveConvertTimeMethod() throws NoSuchMethodException {
        final Method method =
            WasiTimeOperations.class.getMethod("convertTime", long.class, TimeUnit.class);
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have isClockSupported static method")
      void shouldHaveIsClockSupportedMethod() throws NoSuchMethodException {
        final Method method = WasiTimeOperations.class.getMethod("isClockSupported", int.class);
        assertThat(method.getReturnType()).isEqualTo(boolean.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have getClockName static method")
      void shouldHaveGetClockNameMethod() throws NoSuchMethodException {
        final Method method = WasiTimeOperations.class.getMethod("getClockName", int.class);
        assertThat(method.getReturnType()).isEqualTo(String.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
      }
    }
  }

  @Nested
  @DisplayName("WasiRandomOperations Tests")
  class WasiRandomOperationsTests {

    @Nested
    @DisplayName("Class Structure Tests")
    class ClassStructureTests {

      @Test
      @DisplayName("should be a final class")
      void shouldBeFinalClass() {
        assertThat(Modifier.isFinal(WasiRandomOperations.class.getModifiers()))
            .as("WasiRandomOperations should be final")
            .isTrue();
      }

      @Test
      @DisplayName("should be public")
      void shouldBePublic() {
        assertThat(Modifier.isPublic(WasiRandomOperations.class.getModifiers()))
            .as("WasiRandomOperations should be public")
            .isTrue();
      }

      @Test
      @DisplayName("should have private LOGGER field")
      void shouldHaveLoggerField() throws NoSuchFieldException {
        final Field field = WasiRandomOperations.class.getDeclaredField("LOGGER");
        assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

      @Test
      @DisplayName("should have constructor with WasiContext parameter")
      void shouldHaveWasiContextConstructor() throws NoSuchMethodException {
        final Constructor<?> constructor =
            WasiRandomOperations.class.getConstructor(WasiContext.class);
        assertThat(constructor).isNotNull();
        assertThat(Modifier.isPublic(constructor.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Method Tests")
    class MethodTests {

      @Test
      @DisplayName("should have getRandomBytes method")
      void shouldHaveGetRandomBytesMethod() throws NoSuchMethodException {
        final Method method =
            WasiRandomOperations.class.getMethod("getRandomBytes", ByteBuffer.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have generateRandomBytes method")
      void shouldHaveGenerateRandomBytesMethod() throws NoSuchMethodException {
        final Method method =
            WasiRandomOperations.class.getMethod("generateRandomBytes", int.class);
        assertThat(method.getReturnType()).isEqualTo(byte[].class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have generateRandomInt method without bound")
      void shouldHaveGenerateRandomIntMethod() throws NoSuchMethodException {
        final Method method = WasiRandomOperations.class.getMethod("generateRandomInt");
        assertThat(method.getReturnType()).isEqualTo(int.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have generateRandomInt method with bound")
      void shouldHaveGenerateRandomIntWithBoundMethod() throws NoSuchMethodException {
        final Method method = WasiRandomOperations.class.getMethod("generateRandomInt", int.class);
        assertThat(method.getReturnType()).isEqualTo(int.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have generateRandomLong method")
      void shouldHaveGenerateRandomLongMethod() throws NoSuchMethodException {
        final Method method = WasiRandomOperations.class.getMethod("generateRandomLong");
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have generateRandomDouble method")
      void shouldHaveGenerateRandomDoubleMethod() throws NoSuchMethodException {
        final Method method = WasiRandomOperations.class.getMethod("generateRandomDouble");
        assertThat(method.getReturnType()).isEqualTo(double.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have getRandomBytesFallback method")
      void shouldHaveGetRandomBytesFallbackMethod() throws NoSuchMethodException {
        final Method method =
            WasiRandomOperations.class.getMethod("getRandomBytesFallback", ByteBuffer.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }
  }

  @Nested
  @DisplayName("WasiNetworkOperations Tests")
  class WasiNetworkOperationsTests {

    @Nested
    @DisplayName("Class Structure Tests")
    class ClassStructureTests {

      @Test
      @DisplayName("should be a final class")
      void shouldBeFinalClass() {
        assertThat(Modifier.isFinal(WasiNetworkOperations.class.getModifiers()))
            .as("WasiNetworkOperations should be final")
            .isTrue();
      }

      @Test
      @DisplayName("should be public")
      void shouldBePublic() {
        assertThat(Modifier.isPublic(WasiNetworkOperations.class.getModifiers()))
            .as("WasiNetworkOperations should be public")
            .isTrue();
      }

      @Test
      @DisplayName("should have private LOGGER field")
      void shouldHaveLoggerField() throws NoSuchFieldException {
        final Field field = WasiNetworkOperations.class.getDeclaredField("LOGGER");
        assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Address Family Constants Tests")
    class AddressFamilyConstantsTests {

      @Test
      @DisplayName("should have AF_INET constant")
      void shouldHaveAfInetConstant() throws NoSuchFieldException {
        final Field field = WasiNetworkOperations.class.getField("AF_INET");
        assertThat(Modifier.isPublic(field.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
        assertThat(field.getType()).isEqualTo(int.class);
      }

      @Test
      @DisplayName("should have AF_INET6 constant")
      void shouldHaveAfInet6Constant() throws NoSuchFieldException {
        final Field field = WasiNetworkOperations.class.getField("AF_INET6");
        assertThat(Modifier.isPublic(field.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
        assertThat(field.getType()).isEqualTo(int.class);
      }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

      @Test
      @DisplayName("should have constructor with WasiContext and ExecutorService")
      void shouldHaveWasiContextExecutorServiceConstructor() throws NoSuchMethodException {
        final Constructor<?> constructor =
            WasiNetworkOperations.class.getConstructor(WasiContext.class, ExecutorService.class);
        assertThat(constructor).isNotNull();
        assertThat(Modifier.isPublic(constructor.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("TCP Method Tests")
    class TcpMethodTests {

      @Test
      @DisplayName("should have createTcpSocket method")
      void shouldHaveCreateTcpSocketMethod() throws NoSuchMethodException {
        final Method method = WasiNetworkOperations.class.getMethod("createTcpSocket", int.class);
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have bindTcp method")
      void shouldHaveBindTcpMethod() throws NoSuchMethodException {
        final Method method =
            WasiNetworkOperations.class.getMethod("bindTcp", long.class, String.class, int.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have listenTcp method")
      void shouldHaveListenTcpMethod() throws NoSuchMethodException {
        final Method method =
            WasiNetworkOperations.class.getMethod("listenTcp", long.class, int.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have acceptTcp method")
      void shouldHaveAcceptTcpMethod() throws NoSuchMethodException {
        final Method method = WasiNetworkOperations.class.getMethod("acceptTcp", long.class);
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have connectTcp method")
      void shouldHaveConnectTcpMethod() throws NoSuchMethodException {
        final Method method =
            WasiNetworkOperations.class.getMethod(
                "connectTcp", long.class, String.class, int.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have sendTcp method")
      void shouldHaveSendTcpMethod() throws NoSuchMethodException {
        final Method method =
            WasiNetworkOperations.class.getMethod("sendTcp", long.class, ByteBuffer.class);
        assertThat(method.getReturnType()).isEqualTo(int.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have receiveTcp method")
      void shouldHaveReceiveTcpMethod() throws NoSuchMethodException {
        final Method method =
            WasiNetworkOperations.class.getMethod("receiveTcp", long.class, ByteBuffer.class);
        assertThat(method.getReturnType()).isEqualTo(int.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("UDP Method Tests")
    class UdpMethodTests {

      @Test
      @DisplayName("should have createUdpSocket method")
      void shouldHaveCreateUdpSocketMethod() throws NoSuchMethodException {
        final Method method = WasiNetworkOperations.class.getMethod("createUdpSocket", int.class);
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have sendUdp method")
      void shouldHaveSendUdpMethod() throws NoSuchMethodException {
        final Method method =
            WasiNetworkOperations.class.getMethod(
                "sendUdp", long.class, ByteBuffer.class, String.class, int.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have receiveUdp method")
      void shouldHaveReceiveUdpMethod() throws NoSuchMethodException {
        final Method method =
            WasiNetworkOperations.class.getMethod("receiveUdp", long.class, ByteBuffer.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("HTTP Method Tests")
    class HttpMethodTests {

      @Test
      @DisplayName("should have httpRequest method")
      void shouldHaveHttpRequestMethod() throws NoSuchMethodException {
        final Method method =
            WasiNetworkOperations.class.getMethod(
                "httpRequest", String.class, String.class, Map.class, ByteBuffer.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Lifecycle Method Tests")
    class LifecycleMethodTests {

      @Test
      @DisplayName("should have closeSocket method")
      void shouldHaveCloseSocketMethod() throws NoSuchMethodException {
        final Method method = WasiNetworkOperations.class.getMethod("closeSocket", long.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have close method")
      void shouldHaveCloseMethod() throws NoSuchMethodException {
        final Method method = WasiNetworkOperations.class.getMethod("close");
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Inner Type Tests")
    class InnerTypeTests {

      @Test
      @DisplayName("should have SocketType enum")
      void shouldHaveSocketTypeEnum() {
        Class<?>[] declaredClasses = WasiNetworkOperations.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> declaredClass : declaredClasses) {
          if (declaredClass.getSimpleName().equals("SocketType")) {
            found = true;
            assertThat(declaredClass.isEnum()).isTrue();
            assertThat(Modifier.isPublic(declaredClass.getModifiers())).isTrue();
            break;
          }
        }
        assertThat(found).as("SocketType enum should exist").isTrue();
      }

      @Test
      @DisplayName("should have SocketState enum")
      void shouldHaveSocketStateEnum() {
        Class<?>[] declaredClasses = WasiNetworkOperations.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> declaredClass : declaredClasses) {
          if (declaredClass.getSimpleName().equals("SocketState")) {
            found = true;
            assertThat(declaredClass.isEnum()).isTrue();
            assertThat(Modifier.isPublic(declaredClass.getModifiers())).isTrue();
            break;
          }
        }
        assertThat(found).as("SocketState enum should exist").isTrue();
      }

      @Test
      @DisplayName("should have SocketInfo inner class")
      void shouldHaveSocketInfoClass() {
        Class<?>[] declaredClasses = WasiNetworkOperations.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> declaredClass : declaredClasses) {
          if (declaredClass.getSimpleName().equals("SocketInfo")) {
            found = true;
            assertThat(Modifier.isPublic(declaredClass.getModifiers())).isTrue();
            assertThat(Modifier.isStatic(declaredClass.getModifiers())).isTrue();
            assertThat(Modifier.isFinal(declaredClass.getModifiers())).isTrue();
            break;
          }
        }
        assertThat(found).as("SocketInfo inner class should exist").isTrue();
      }

      @Test
      @DisplayName("should have UdpDatagram inner class")
      void shouldHaveUdpDatagramClass() {
        Class<?>[] declaredClasses = WasiNetworkOperations.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> declaredClass : declaredClasses) {
          if (declaredClass.getSimpleName().equals("UdpDatagram")) {
            found = true;
            assertThat(Modifier.isPublic(declaredClass.getModifiers())).isTrue();
            assertThat(Modifier.isStatic(declaredClass.getModifiers())).isTrue();
            assertThat(Modifier.isFinal(declaredClass.getModifiers())).isTrue();
            break;
          }
        }
        assertThat(found).as("UdpDatagram inner class should exist").isTrue();
      }
    }
  }

  @Nested
  @DisplayName("WasiProcessOperations Tests")
  class WasiProcessOperationsTests {

    @Nested
    @DisplayName("Class Structure Tests")
    class ClassStructureTests {

      @Test
      @DisplayName("should be a final class")
      void shouldBeFinalClass() {
        assertThat(Modifier.isFinal(WasiProcessOperations.class.getModifiers()))
            .as("WasiProcessOperations should be final")
            .isTrue();
      }

      @Test
      @DisplayName("should be public")
      void shouldBePublic() {
        assertThat(Modifier.isPublic(WasiProcessOperations.class.getModifiers()))
            .as("WasiProcessOperations should be public")
            .isTrue();
      }

      @Test
      @DisplayName("should have private LOGGER field")
      void shouldHaveLoggerField() throws NoSuchFieldException {
        final Field field = WasiProcessOperations.class.getDeclaredField("LOGGER");
        assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

      @Test
      @DisplayName("should have constructor with WasiContext parameter")
      void shouldHaveWasiContextConstructor() throws NoSuchMethodException {
        final Constructor<?> constructor =
            WasiProcessOperations.class.getConstructor(WasiContext.class);
        assertThat(constructor).isNotNull();
        assertThat(Modifier.isPublic(constructor.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Process Method Tests")
    class ProcessMethodTests {

      @Test
      @DisplayName("should have getCurrentProcessId method")
      void shouldHaveGetCurrentProcessIdMethod() throws NoSuchMethodException {
        final Method method = WasiProcessOperations.class.getMethod("getCurrentProcessId");
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have spawnProcess method")
      void shouldHaveSpawnProcessMethod() throws NoSuchMethodException {
        final Method method =
            WasiProcessOperations.class.getMethod(
                "spawnProcess", String.class, List.class, Map.class, String.class);
        assertThat(method.getReturnType()).isEqualTo(CompletableFuture.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have waitForProcess method")
      void shouldHaveWaitForProcessMethod() throws NoSuchMethodException {
        final Method method =
            WasiProcessOperations.class.getMethod("waitForProcess", long.class, int.class);
        assertThat(method.getReturnType()).isEqualTo(CompletableFuture.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have terminateProcess method")
      void shouldHaveTerminateProcessMethod() throws NoSuchMethodException {
        final Method method =
            WasiProcessOperations.class.getMethod("terminateProcess", long.class, int.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have getProcessInfo method")
      void shouldHaveGetProcessInfoMethod() throws NoSuchMethodException {
        final Method method = WasiProcessOperations.class.getMethod("getProcessInfo", long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have getAllChildProcesses method")
      void shouldHaveGetAllChildProcessesMethod() throws NoSuchMethodException {
        final Method method = WasiProcessOperations.class.getMethod("getAllChildProcesses");
        assertThat(method.getReturnType()).isEqualTo(List.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Environment Method Tests")
    class EnvironmentMethodTests {

      @Test
      @DisplayName("should have getEnvironmentVariable method")
      void shouldHaveGetEnvironmentVariableMethod() throws NoSuchMethodException {
        final Method method =
            WasiProcessOperations.class.getMethod("getEnvironmentVariable", String.class);
        assertThat(method.getReturnType()).isEqualTo(String.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have setEnvironmentVariable method")
      void shouldHaveSetEnvironmentVariableMethod() throws NoSuchMethodException {
        final Method method =
            WasiProcessOperations.class.getMethod(
                "setEnvironmentVariable", String.class, String.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have getAllEnvironmentVariables method")
      void shouldHaveGetAllEnvironmentVariablesMethod() throws NoSuchMethodException {
        final Method method = WasiProcessOperations.class.getMethod("getAllEnvironmentVariables");
        assertThat(method.getReturnType()).isEqualTo(Map.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Signal Method Tests")
    class SignalMethodTests {

      @Test
      @DisplayName("should have raiseSignal method")
      void shouldHaveRaiseSignalMethod() throws NoSuchMethodException {
        final Method method = WasiProcessOperations.class.getMethod("raiseSignal", int.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Lifecycle Method Tests")
    class LifecycleMethodTests {

      @Test
      @DisplayName("should have close method")
      void shouldHaveCloseMethod() throws NoSuchMethodException {
        final Method method = WasiProcessOperations.class.getMethod("close");
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("ProcessInfo Inner Class Tests")
    class ProcessInfoInnerClassTests {

      @Test
      @DisplayName("should have ProcessInfo inner class")
      void shouldHaveProcessInfoClass() {
        Class<?>[] declaredClasses = WasiProcessOperations.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> declaredClass : declaredClasses) {
          if (declaredClass.getSimpleName().equals("ProcessInfo")) {
            found = true;
            assertThat(Modifier.isPublic(declaredClass.getModifiers())).isTrue();
            assertThat(Modifier.isStatic(declaredClass.getModifiers())).isTrue();
            assertThat(Modifier.isFinal(declaredClass.getModifiers())).isTrue();
            break;
          }
        }
        assertThat(found).as("ProcessInfo inner class should exist").isTrue();
      }

      @Test
      @DisplayName("ProcessInfo should have isAlive method")
      void processInfoShouldHaveIsAliveMethod() throws NoSuchMethodException {
        Class<?> processInfoClass = null;
        for (Class<?> declaredClass : WasiProcessOperations.class.getDeclaredClasses()) {
          if (declaredClass.getSimpleName().equals("ProcessInfo")) {
            processInfoClass = declaredClass;
            break;
          }
        }
        assertThat(processInfoClass).isNotNull();

        final Method method = processInfoClass.getMethod("isAlive");
        assertThat(method.getReturnType()).isEqualTo(boolean.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("ProcessInfo should have getPid method")
      void processInfoShouldHaveGetPidMethod() throws NoSuchMethodException {
        Class<?> processInfoClass = null;
        for (Class<?> declaredClass : WasiProcessOperations.class.getDeclaredClasses()) {
          if (declaredClass.getSimpleName().equals("ProcessInfo")) {
            processInfoClass = declaredClass;
            break;
          }
        }
        assertThat(processInfoClass).isNotNull();

        final Method method = processInfoClass.getMethod("getPid");
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }
  }

  @Nested
  @DisplayName("Method Count Verification Tests")
  class MethodCountTests {

    @Test
    @DisplayName("WasiTimeOperations should have expected number of public methods")
    void timeOperationsShouldHaveExpectedPublicMethods() {
      long publicMethodCount =
          java.util.Arrays.stream(WasiTimeOperations.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();

      // Expected: getClockResolution, getCurrentTime x2, getRealtime, getMonotonicTime,
      // getProcessCpuTime, getThreadCpuTime, convertTime (static), isClockSupported (static),
      // getClockName (static)
      assertThat(publicMethodCount).isGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("WasiRandomOperations should have expected number of public methods")
    void randomOperationsShouldHaveExpectedPublicMethods() {
      long publicMethodCount =
          java.util.Arrays.stream(WasiRandomOperations.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();

      // Expected: getRandomBytes, generateRandomBytes, generateRandomInt x2, generateRandomLong,
      // generateRandomDouble, getRandomBytesFallback
      assertThat(publicMethodCount).isGreaterThanOrEqualTo(7);
    }

    @Test
    @DisplayName("WasiNetworkOperations should have expected number of public methods")
    void networkOperationsShouldHaveExpectedPublicMethods() {
      long publicMethodCount =
          java.util.Arrays.stream(WasiNetworkOperations.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();

      // Expected: TCP (7), UDP (3), HTTP (1), Lifecycle (2)
      assertThat(publicMethodCount).isGreaterThanOrEqualTo(13);
    }

    @Test
    @DisplayName("WasiProcessOperations should have expected number of public methods")
    void processOperationsShouldHaveExpectedPublicMethods() {
      long publicMethodCount =
          java.util.Arrays.stream(WasiProcessOperations.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();

      // Expected: Process (6), Environment (3), Signal (1), Lifecycle (1)
      assertThat(publicMethodCount).isGreaterThanOrEqualTo(11);
    }
  }
}
