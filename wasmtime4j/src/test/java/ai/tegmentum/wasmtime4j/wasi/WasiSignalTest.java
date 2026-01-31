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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiSignal} enum.
 *
 * <p>Verifies signal values, code mappings, and fromCode conversion for all 31 POSIX-like signals.
 */
@DisplayName("WasiSignal Tests")
class WasiSignalTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("WasiSignal should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiSignal.class.isEnum(), "WasiSignal should be an enum");
    }

    @Test
    @DisplayName("WasiSignal should have exactly 31 values")
    void shouldHaveExactlyThirtyOneValues() {
      assertEquals(31, WasiSignal.values().length, "Should have exactly 31 signal values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have SIGHUP value")
    void shouldHaveSighupValue() {
      assertNotNull(WasiSignal.SIGHUP, "SIGHUP should exist");
      assertEquals("SIGHUP", WasiSignal.SIGHUP.name(), "Name should be SIGHUP");
    }

    @Test
    @DisplayName("should have SIGINT value")
    void shouldHaveSigintValue() {
      assertNotNull(WasiSignal.SIGINT, "SIGINT should exist");
      assertEquals("SIGINT", WasiSignal.SIGINT.name(), "Name should be SIGINT");
    }

    @Test
    @DisplayName("should have SIGKILL value")
    void shouldHaveSigkillValue() {
      assertNotNull(WasiSignal.SIGKILL, "SIGKILL should exist");
      assertEquals("SIGKILL", WasiSignal.SIGKILL.name(), "Name should be SIGKILL");
    }

    @Test
    @DisplayName("should have SIGTERM value")
    void shouldHaveSigtermValue() {
      assertNotNull(WasiSignal.SIGTERM, "SIGTERM should exist");
      assertEquals("SIGTERM", WasiSignal.SIGTERM.name(), "Name should be SIGTERM");
    }

    @Test
    @DisplayName("should have SIGSYS value")
    void shouldHaveSigsysValue() {
      assertNotNull(WasiSignal.SIGSYS, "SIGSYS should exist");
      assertEquals("SIGSYS", WasiSignal.SIGSYS.name(), "Name should be SIGSYS");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("ordinals should be unique")
    void ordinalsShouldBeUnique() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final WasiSignal signal : WasiSignal.values()) {
        assertTrue(
            ordinals.add(signal.ordinal()), "Ordinal should be unique: " + signal.ordinal());
      }
    }

    @Test
    @DisplayName("ordinals should be sequential starting at 0")
    void ordinalsShouldBeSequential() {
      final WasiSignal[] values = WasiSignal.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(), "Ordinal should match index for " + values[i]);
      }
    }
  }

  @Nested
  @DisplayName("valueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("valueOf should return correct constant for representative names")
    void valueOfShouldReturnCorrectConstant() {
      assertEquals(
          WasiSignal.SIGHUP, WasiSignal.valueOf("SIGHUP"), "Should return SIGHUP");
      assertEquals(
          WasiSignal.SIGINT, WasiSignal.valueOf("SIGINT"), "Should return SIGINT");
      assertEquals(
          WasiSignal.SIGQUIT, WasiSignal.valueOf("SIGQUIT"), "Should return SIGQUIT");
      assertEquals(
          WasiSignal.SIGKILL, WasiSignal.valueOf("SIGKILL"), "Should return SIGKILL");
      assertEquals(
          WasiSignal.SIGTERM, WasiSignal.valueOf("SIGTERM"), "Should return SIGTERM");
      assertEquals(
          WasiSignal.SIGSYS, WasiSignal.valueOf("SIGSYS"), "Should return SIGSYS");
    }

    @Test
    @DisplayName("valueOf should throw for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiSignal.valueOf("INVALID"),
          "Should throw for invalid enum name");
    }
  }

  @Nested
  @DisplayName("values() Tests")
  class ValuesTests {

    @Test
    @DisplayName("values() should return all enum constants")
    void valuesShouldReturnAllEnumConstants() {
      final WasiSignal[] values = WasiSignal.values();
      final Set<WasiSignal> valueSet = new HashSet<>(Arrays.asList(values));

      assertTrue(valueSet.contains(WasiSignal.SIGHUP), "Should contain SIGHUP");
      assertTrue(valueSet.contains(WasiSignal.SIGINT), "Should contain SIGINT");
      assertTrue(valueSet.contains(WasiSignal.SIGQUIT), "Should contain SIGQUIT");
      assertTrue(valueSet.contains(WasiSignal.SIGILL), "Should contain SIGILL");
      assertTrue(valueSet.contains(WasiSignal.SIGTRAP), "Should contain SIGTRAP");
      assertTrue(valueSet.contains(WasiSignal.SIGABRT), "Should contain SIGABRT");
      assertTrue(valueSet.contains(WasiSignal.SIGBUS), "Should contain SIGBUS");
      assertTrue(valueSet.contains(WasiSignal.SIGFPE), "Should contain SIGFPE");
      assertTrue(valueSet.contains(WasiSignal.SIGKILL), "Should contain SIGKILL");
      assertTrue(valueSet.contains(WasiSignal.SIGUSR1), "Should contain SIGUSR1");
      assertTrue(valueSet.contains(WasiSignal.SIGSEGV), "Should contain SIGSEGV");
      assertTrue(valueSet.contains(WasiSignal.SIGUSR2), "Should contain SIGUSR2");
      assertTrue(valueSet.contains(WasiSignal.SIGPIPE), "Should contain SIGPIPE");
      assertTrue(valueSet.contains(WasiSignal.SIGALRM), "Should contain SIGALRM");
      assertTrue(valueSet.contains(WasiSignal.SIGTERM), "Should contain SIGTERM");
      assertTrue(valueSet.contains(WasiSignal.SIGSTKFLT), "Should contain SIGSTKFLT");
      assertTrue(valueSet.contains(WasiSignal.SIGCHLD), "Should contain SIGCHLD");
      assertTrue(valueSet.contains(WasiSignal.SIGCONT), "Should contain SIGCONT");
      assertTrue(valueSet.contains(WasiSignal.SIGSTOP), "Should contain SIGSTOP");
      assertTrue(valueSet.contains(WasiSignal.SIGTSTP), "Should contain SIGTSTP");
      assertTrue(valueSet.contains(WasiSignal.SIGTTIN), "Should contain SIGTTIN");
      assertTrue(valueSet.contains(WasiSignal.SIGTTOU), "Should contain SIGTTOU");
      assertTrue(valueSet.contains(WasiSignal.SIGURG), "Should contain SIGURG");
      assertTrue(valueSet.contains(WasiSignal.SIGXCPU), "Should contain SIGXCPU");
      assertTrue(valueSet.contains(WasiSignal.SIGXFSZ), "Should contain SIGXFSZ");
      assertTrue(valueSet.contains(WasiSignal.SIGVTALRM), "Should contain SIGVTALRM");
      assertTrue(valueSet.contains(WasiSignal.SIGPROF), "Should contain SIGPROF");
      assertTrue(valueSet.contains(WasiSignal.SIGWINCH), "Should contain SIGWINCH");
      assertTrue(valueSet.contains(WasiSignal.SIGIO), "Should contain SIGIO");
      assertTrue(valueSet.contains(WasiSignal.SIGPWR), "Should contain SIGPWR");
      assertTrue(valueSet.contains(WasiSignal.SIGSYS), "Should contain SIGSYS");
    }

    @Test
    @DisplayName("values() should return new array each time")
    void valuesShouldReturnNewArrayEachTime() {
      final WasiSignal[] first = WasiSignal.values();
      final WasiSignal[] second = WasiSignal.values();

      assertTrue(first != second, "Should return new array each time");
      assertEquals(first.length, second.length, "Arrays should have same length");
    }
  }

  @Nested
  @DisplayName("GetCode Tests")
  class GetCodeTests {

    @Test
    @DisplayName("SIGHUP should have code 1")
    void sighupShouldHaveCodeOne() {
      assertEquals(1, WasiSignal.SIGHUP.getCode(), "SIGHUP code should be 1");
    }

    @Test
    @DisplayName("SIGINT should have code 2")
    void sigintShouldHaveCodeTwo() {
      assertEquals(2, WasiSignal.SIGINT.getCode(), "SIGINT code should be 2");
    }

    @Test
    @DisplayName("SIGQUIT should have code 3")
    void sigquitShouldHaveCodeThree() {
      assertEquals(3, WasiSignal.SIGQUIT.getCode(), "SIGQUIT code should be 3");
    }

    @Test
    @DisplayName("SIGILL should have code 4")
    void sigillShouldHaveCodeFour() {
      assertEquals(4, WasiSignal.SIGILL.getCode(), "SIGILL code should be 4");
    }

    @Test
    @DisplayName("SIGTRAP should have code 5")
    void sigtrapShouldHaveCodeFive() {
      assertEquals(5, WasiSignal.SIGTRAP.getCode(), "SIGTRAP code should be 5");
    }

    @Test
    @DisplayName("SIGABRT should have code 6")
    void sigabrtShouldHaveCodeSix() {
      assertEquals(6, WasiSignal.SIGABRT.getCode(), "SIGABRT code should be 6");
    }

    @Test
    @DisplayName("SIGBUS should have code 7")
    void sigbusShouldHaveCodeSeven() {
      assertEquals(7, WasiSignal.SIGBUS.getCode(), "SIGBUS code should be 7");
    }

    @Test
    @DisplayName("SIGFPE should have code 8")
    void sigfpeShouldHaveCodeEight() {
      assertEquals(8, WasiSignal.SIGFPE.getCode(), "SIGFPE code should be 8");
    }

    @Test
    @DisplayName("SIGKILL should have code 9")
    void sigkillShouldHaveCodeNine() {
      assertEquals(9, WasiSignal.SIGKILL.getCode(), "SIGKILL code should be 9");
    }

    @Test
    @DisplayName("SIGUSR1 should have code 10")
    void sigusr1ShouldHaveCodeTen() {
      assertEquals(10, WasiSignal.SIGUSR1.getCode(), "SIGUSR1 code should be 10");
    }

    @Test
    @DisplayName("SIGSEGV should have code 11")
    void sigsegvShouldHaveCodeEleven() {
      assertEquals(11, WasiSignal.SIGSEGV.getCode(), "SIGSEGV code should be 11");
    }

    @Test
    @DisplayName("SIGUSR2 should have code 12")
    void sigusr2ShouldHaveCodeTwelve() {
      assertEquals(12, WasiSignal.SIGUSR2.getCode(), "SIGUSR2 code should be 12");
    }

    @Test
    @DisplayName("SIGPIPE should have code 13")
    void sigpipeShouldHaveCodeThirteen() {
      assertEquals(13, WasiSignal.SIGPIPE.getCode(), "SIGPIPE code should be 13");
    }

    @Test
    @DisplayName("SIGALRM should have code 14")
    void sigalrmShouldHaveCodeFourteen() {
      assertEquals(14, WasiSignal.SIGALRM.getCode(), "SIGALRM code should be 14");
    }

    @Test
    @DisplayName("SIGTERM should have code 15")
    void sigtermShouldHaveCodeFifteen() {
      assertEquals(15, WasiSignal.SIGTERM.getCode(), "SIGTERM code should be 15");
    }

    @Test
    @DisplayName("SIGSTKFLT should have code 16")
    void sigstkfltShouldHaveCodeSixteen() {
      assertEquals(16, WasiSignal.SIGSTKFLT.getCode(), "SIGSTKFLT code should be 16");
    }

    @Test
    @DisplayName("SIGCHLD should have code 17")
    void sigchldShouldHaveCodeSeventeen() {
      assertEquals(17, WasiSignal.SIGCHLD.getCode(), "SIGCHLD code should be 17");
    }

    @Test
    @DisplayName("SIGCONT should have code 18")
    void sigcontShouldHaveCodeEighteen() {
      assertEquals(18, WasiSignal.SIGCONT.getCode(), "SIGCONT code should be 18");
    }

    @Test
    @DisplayName("SIGSTOP should have code 19")
    void sigstopShouldHaveCodeNineteen() {
      assertEquals(19, WasiSignal.SIGSTOP.getCode(), "SIGSTOP code should be 19");
    }

    @Test
    @DisplayName("SIGTSTP should have code 20")
    void sigtstpShouldHaveCodeTwenty() {
      assertEquals(20, WasiSignal.SIGTSTP.getCode(), "SIGTSTP code should be 20");
    }

    @Test
    @DisplayName("SIGTTIN should have code 21")
    void sigttinShouldHaveCodeTwentyOne() {
      assertEquals(21, WasiSignal.SIGTTIN.getCode(), "SIGTTIN code should be 21");
    }

    @Test
    @DisplayName("SIGTTOU should have code 22")
    void sigttouShouldHaveCodeTwentyTwo() {
      assertEquals(22, WasiSignal.SIGTTOU.getCode(), "SIGTTOU code should be 22");
    }

    @Test
    @DisplayName("SIGURG should have code 23")
    void sigurgShouldHaveCodeTwentyThree() {
      assertEquals(23, WasiSignal.SIGURG.getCode(), "SIGURG code should be 23");
    }

    @Test
    @DisplayName("SIGXCPU should have code 24")
    void sigxcpuShouldHaveCodeTwentyFour() {
      assertEquals(24, WasiSignal.SIGXCPU.getCode(), "SIGXCPU code should be 24");
    }

    @Test
    @DisplayName("SIGXFSZ should have code 25")
    void sigxfszShouldHaveCodeTwentyFive() {
      assertEquals(25, WasiSignal.SIGXFSZ.getCode(), "SIGXFSZ code should be 25");
    }

    @Test
    @DisplayName("SIGVTALRM should have code 26")
    void sigvtalrmShouldHaveCodeTwentySix() {
      assertEquals(26, WasiSignal.SIGVTALRM.getCode(), "SIGVTALRM code should be 26");
    }

    @Test
    @DisplayName("SIGPROF should have code 27")
    void sigprofShouldHaveCodeTwentySeven() {
      assertEquals(27, WasiSignal.SIGPROF.getCode(), "SIGPROF code should be 27");
    }

    @Test
    @DisplayName("SIGWINCH should have code 28")
    void sigwinchShouldHaveCodeTwentyEight() {
      assertEquals(28, WasiSignal.SIGWINCH.getCode(), "SIGWINCH code should be 28");
    }

    @Test
    @DisplayName("SIGIO should have code 29")
    void sigioShouldHaveCodeTwentyNine() {
      assertEquals(29, WasiSignal.SIGIO.getCode(), "SIGIO code should be 29");
    }

    @Test
    @DisplayName("SIGPWR should have code 30")
    void sigpwrShouldHaveCodeThirty() {
      assertEquals(30, WasiSignal.SIGPWR.getCode(), "SIGPWR code should be 30");
    }

    @Test
    @DisplayName("SIGSYS should have code 31")
    void sigsysShouldHaveCodeThirtyOne() {
      assertEquals(31, WasiSignal.SIGSYS.getCode(), "SIGSYS code should be 31");
    }
  }

  @Nested
  @DisplayName("FromCode Tests")
  class FromCodeTests {

    @Test
    @DisplayName("should resolve valid codes 1 through 31")
    void shouldResolveValidCodes() {
      assertEquals(WasiSignal.SIGHUP, WasiSignal.fromCode(1), "Code 1 should be SIGHUP");
      assertEquals(WasiSignal.SIGINT, WasiSignal.fromCode(2), "Code 2 should be SIGINT");
      assertEquals(WasiSignal.SIGQUIT, WasiSignal.fromCode(3), "Code 3 should be SIGQUIT");
      assertEquals(WasiSignal.SIGILL, WasiSignal.fromCode(4), "Code 4 should be SIGILL");
      assertEquals(WasiSignal.SIGTRAP, WasiSignal.fromCode(5), "Code 5 should be SIGTRAP");
      assertEquals(WasiSignal.SIGABRT, WasiSignal.fromCode(6), "Code 6 should be SIGABRT");
      assertEquals(WasiSignal.SIGBUS, WasiSignal.fromCode(7), "Code 7 should be SIGBUS");
      assertEquals(WasiSignal.SIGFPE, WasiSignal.fromCode(8), "Code 8 should be SIGFPE");
      assertEquals(WasiSignal.SIGKILL, WasiSignal.fromCode(9), "Code 9 should be SIGKILL");
      assertEquals(WasiSignal.SIGUSR1, WasiSignal.fromCode(10), "Code 10 should be SIGUSR1");
      assertEquals(WasiSignal.SIGSEGV, WasiSignal.fromCode(11), "Code 11 should be SIGSEGV");
      assertEquals(WasiSignal.SIGUSR2, WasiSignal.fromCode(12), "Code 12 should be SIGUSR2");
      assertEquals(WasiSignal.SIGPIPE, WasiSignal.fromCode(13), "Code 13 should be SIGPIPE");
      assertEquals(WasiSignal.SIGALRM, WasiSignal.fromCode(14), "Code 14 should be SIGALRM");
      assertEquals(WasiSignal.SIGTERM, WasiSignal.fromCode(15), "Code 15 should be SIGTERM");
      assertEquals(WasiSignal.SIGSTKFLT, WasiSignal.fromCode(16), "Code 16 should be SIGSTKFLT");
      assertEquals(WasiSignal.SIGCHLD, WasiSignal.fromCode(17), "Code 17 should be SIGCHLD");
      assertEquals(WasiSignal.SIGCONT, WasiSignal.fromCode(18), "Code 18 should be SIGCONT");
      assertEquals(WasiSignal.SIGSTOP, WasiSignal.fromCode(19), "Code 19 should be SIGSTOP");
      assertEquals(WasiSignal.SIGTSTP, WasiSignal.fromCode(20), "Code 20 should be SIGTSTP");
      assertEquals(WasiSignal.SIGTTIN, WasiSignal.fromCode(21), "Code 21 should be SIGTTIN");
      assertEquals(WasiSignal.SIGTTOU, WasiSignal.fromCode(22), "Code 22 should be SIGTTOU");
      assertEquals(WasiSignal.SIGURG, WasiSignal.fromCode(23), "Code 23 should be SIGURG");
      assertEquals(WasiSignal.SIGXCPU, WasiSignal.fromCode(24), "Code 24 should be SIGXCPU");
      assertEquals(WasiSignal.SIGXFSZ, WasiSignal.fromCode(25), "Code 25 should be SIGXFSZ");
      assertEquals(WasiSignal.SIGVTALRM, WasiSignal.fromCode(26), "Code 26 should be SIGVTALRM");
      assertEquals(WasiSignal.SIGPROF, WasiSignal.fromCode(27), "Code 27 should be SIGPROF");
      assertEquals(WasiSignal.SIGWINCH, WasiSignal.fromCode(28), "Code 28 should be SIGWINCH");
      assertEquals(WasiSignal.SIGIO, WasiSignal.fromCode(29), "Code 29 should be SIGIO");
      assertEquals(WasiSignal.SIGPWR, WasiSignal.fromCode(30), "Code 30 should be SIGPWR");
      assertEquals(WasiSignal.SIGSYS, WasiSignal.fromCode(31), "Code 31 should be SIGSYS");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for code 0")
    void shouldThrowForCodeZero() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> WasiSignal.fromCode(0),
              "Should throw for code 0");
      assertTrue(
          exception.getMessage().contains("0"),
          "Exception message should mention the invalid code: " + exception.getMessage());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for negative code")
    void shouldThrowForNegativeCode() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiSignal.fromCode(-1),
          "Should throw for negative code -1");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for code 32")
    void shouldThrowForCodeThirtyTwo() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiSignal.fromCode(32),
          "Should throw for out-of-range code 32");
    }

    @Test
    @DisplayName("should round-trip all signals through code")
    void shouldRoundTripAllSignalsThroughCode() {
      for (final WasiSignal signal : WasiSignal.values()) {
        final int code = signal.getCode();
        final WasiSignal resolved = WasiSignal.fromCode(code);
        assertSame(
            signal, resolved, "Round-trip should preserve signal: " + signal + " (code=" + code + ")");
      }
    }
  }
}
