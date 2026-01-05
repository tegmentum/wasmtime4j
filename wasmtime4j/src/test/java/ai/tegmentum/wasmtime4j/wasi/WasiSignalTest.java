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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link WasiSignal} enum.
 *
 * <p>WasiSignal represents POSIX-like signals for process control in WASI.
 */
@DisplayName("WasiSignal Tests")
class WasiSignalTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should have all 31 POSIX signals")
    void shouldHaveAll31PosixSignals() {
      final WasiSignal[] values = WasiSignal.values();
      assertEquals(31, values.length, "WasiSignal should have 31 values");
    }

    @Test
    @DisplayName("should have commonly used signals")
    void shouldHaveCommonlyUsedSignals() {
      assertNotNull(WasiSignal.valueOf("SIGHUP"), "SIGHUP should exist");
      assertNotNull(WasiSignal.valueOf("SIGINT"), "SIGINT should exist");
      assertNotNull(WasiSignal.valueOf("SIGQUIT"), "SIGQUIT should exist");
      assertNotNull(WasiSignal.valueOf("SIGKILL"), "SIGKILL should exist");
      assertNotNull(WasiSignal.valueOf("SIGTERM"), "SIGTERM should exist");
      assertNotNull(WasiSignal.valueOf("SIGSTOP"), "SIGSTOP should exist");
      assertNotNull(WasiSignal.valueOf("SIGCONT"), "SIGCONT should exist");
    }
  }

  @Nested
  @DisplayName("getCode Method Tests")
  class GetCodeMethodTests {

    @Test
    @DisplayName("SIGHUP should have code 1")
    void sighupShouldHaveCode1() {
      assertEquals(1, WasiSignal.SIGHUP.getCode(), "SIGHUP code should be 1");
    }

    @Test
    @DisplayName("SIGINT should have code 2")
    void sigintShouldHaveCode2() {
      assertEquals(2, WasiSignal.SIGINT.getCode(), "SIGINT code should be 2");
    }

    @Test
    @DisplayName("SIGKILL should have code 9")
    void sigkillShouldHaveCode9() {
      assertEquals(9, WasiSignal.SIGKILL.getCode(), "SIGKILL code should be 9");
    }

    @Test
    @DisplayName("SIGTERM should have code 15")
    void sigtermShouldHaveCode15() {
      assertEquals(15, WasiSignal.SIGTERM.getCode(), "SIGTERM code should be 15");
    }

    @Test
    @DisplayName("SIGSTOP should have code 19")
    void sigstopShouldHaveCode19() {
      assertEquals(19, WasiSignal.SIGSTOP.getCode(), "SIGSTOP code should be 19");
    }

    @Test
    @DisplayName("SIGSYS should have code 31")
    void sigsysShouldHaveCode31() {
      assertEquals(31, WasiSignal.SIGSYS.getCode(), "SIGSYS code should be 31");
    }

    @ParameterizedTest
    @EnumSource(WasiSignal.class)
    @DisplayName("all signals should have codes from 1 to 31")
    void allSignalsShouldHaveCodesFrom1To31(final WasiSignal signal) {
      final int code = signal.getCode();
      assertTrue(code >= 1 && code <= 31, signal.name() + " code should be between 1 and 31");
    }
  }

  @Nested
  @DisplayName("fromCode Method Tests")
  class FromCodeMethodTests {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 9, 15, 19, 31})
    @DisplayName("fromCode should return correct signal for valid codes")
    void fromCodeShouldReturnCorrectSignalForValidCodes(final int code) {
      final WasiSignal signal = WasiSignal.fromCode(code);
      assertNotNull(signal, "Should return a signal for code " + code);
      assertEquals(code, signal.getCode(), "getCode should return original code");
    }

    @Test
    @DisplayName("fromCode should roundtrip for all signals")
    void fromCodeShouldRoundtripForAllSignals() {
      for (final WasiSignal signal : WasiSignal.values()) {
        final WasiSignal roundtrip = WasiSignal.fromCode(signal.getCode());
        assertEquals(signal, roundtrip, "fromCode should return same signal for " + signal.name());
      }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 32, 100, 999})
    @DisplayName("fromCode should throw for invalid codes")
    void fromCodeShouldThrowForInvalidCodes(final int code) {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> WasiSignal.fromCode(code),
              "Should throw for invalid code " + code);
      assertTrue(
          exception.getMessage().contains(String.valueOf(code)),
          "Exception should contain the invalid code");
    }
  }

  @Nested
  @DisplayName("Signal Code Uniqueness Tests")
  class SignalCodeUniquenessTests {

    @Test
    @DisplayName("all signal codes should be unique")
    void allSignalCodesShouldBeUnique() {
      final WasiSignal[] signals = WasiSignal.values();
      for (int i = 0; i < signals.length; i++) {
        for (int j = i + 1; j < signals.length; j++) {
          assertTrue(
              signals[i].getCode() != signals[j].getCode(),
              signals[i].name() + " and " + signals[j].name() + " should have different codes");
        }
      }
    }

    @Test
    @DisplayName("signal codes should cover 1 to 31")
    void signalCodesShouldCover1To31() {
      final boolean[] covered = new boolean[32];
      for (final WasiSignal signal : WasiSignal.values()) {
        covered[signal.getCode()] = true;
      }
      for (int i = 1; i <= 31; i++) {
        assertTrue(covered[i], "Code " + i + " should be covered by a signal");
      }
    }
  }

  @Nested
  @DisplayName("Standard Signal Codes Tests")
  class StandardSignalCodesTests {

    @Test
    @DisplayName("SIGQUIT should have code 3")
    void sigquitShouldHaveCode3() {
      assertEquals(3, WasiSignal.SIGQUIT.getCode(), "SIGQUIT code should be 3");
    }

    @Test
    @DisplayName("SIGILL should have code 4")
    void sigillShouldHaveCode4() {
      assertEquals(4, WasiSignal.SIGILL.getCode(), "SIGILL code should be 4");
    }

    @Test
    @DisplayName("SIGTRAP should have code 5")
    void sigtrapShouldHaveCode5() {
      assertEquals(5, WasiSignal.SIGTRAP.getCode(), "SIGTRAP code should be 5");
    }

    @Test
    @DisplayName("SIGABRT should have code 6")
    void sigabrtShouldHaveCode6() {
      assertEquals(6, WasiSignal.SIGABRT.getCode(), "SIGABRT code should be 6");
    }

    @Test
    @DisplayName("SIGBUS should have code 7")
    void sigbusShouldHaveCode7() {
      assertEquals(7, WasiSignal.SIGBUS.getCode(), "SIGBUS code should be 7");
    }

    @Test
    @DisplayName("SIGFPE should have code 8")
    void sigfpeShouldHaveCode8() {
      assertEquals(8, WasiSignal.SIGFPE.getCode(), "SIGFPE code should be 8");
    }

    @Test
    @DisplayName("SIGSEGV should have code 11")
    void sigsegvShouldHaveCode11() {
      assertEquals(11, WasiSignal.SIGSEGV.getCode(), "SIGSEGV code should be 11");
    }

    @Test
    @DisplayName("SIGPIPE should have code 13")
    void sigpipeShouldHaveCode13() {
      assertEquals(13, WasiSignal.SIGPIPE.getCode(), "SIGPIPE code should be 13");
    }

    @Test
    @DisplayName("SIGALRM should have code 14")
    void sigalrmShouldHaveCode14() {
      assertEquals(14, WasiSignal.SIGALRM.getCode(), "SIGALRM code should be 14");
    }

    @Test
    @DisplayName("SIGCHLD should have code 17")
    void sigchldShouldHaveCode17() {
      assertEquals(17, WasiSignal.SIGCHLD.getCode(), "SIGCHLD code should be 17");
    }

    @Test
    @DisplayName("SIGCONT should have code 18")
    void sigcontShouldHaveCode18() {
      assertEquals(18, WasiSignal.SIGCONT.getCode(), "SIGCONT code should be 18");
    }

    @Test
    @DisplayName("SIGUSR1 should have code 10")
    void sigusr1ShouldHaveCode10() {
      assertEquals(10, WasiSignal.SIGUSR1.getCode(), "SIGUSR1 code should be 10");
    }

    @Test
    @DisplayName("SIGUSR2 should have code 12")
    void sigusr2ShouldHaveCode12() {
      assertEquals(12, WasiSignal.SIGUSR2.getCode(), "SIGUSR2 code should be 12");
    }
  }
}
