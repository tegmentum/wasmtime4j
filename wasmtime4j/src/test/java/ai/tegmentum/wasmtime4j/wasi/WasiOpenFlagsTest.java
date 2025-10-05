package ai.tegmentum.wasmtime4j.wasi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Test suite for WASI open flags functionality. */
final class WasiOpenFlagsTest {

  @Test
  void testIndividualFlags() {
    assertThat(WasiOpenFlags.READ.getValue()).isEqualTo(0x01);
    assertThat(WasiOpenFlags.WRITE.getValue()).isEqualTo(0x02);
    assertThat(WasiOpenFlags.CREATE.getValue()).isEqualTo(0x04);
    assertThat(WasiOpenFlags.EXCLUSIVE.getValue()).isEqualTo(0x08);
    assertThat(WasiOpenFlags.TRUNCATE.getValue()).isEqualTo(0x10);
    assertThat(WasiOpenFlags.APPEND.getValue()).isEqualTo(0x20);
  }

  @Test
  void testFlagCombination() {
    final WasiOpenFlags.WasiOpenFlagsSet readWrite =
        WasiOpenFlags.READ.combine(WasiOpenFlags.WRITE);

    assertThat(readWrite.getValue()).isEqualTo(0x03);
    assertThat(readWrite.contains(WasiOpenFlags.READ)).isTrue();
    assertThat(readWrite.contains(WasiOpenFlags.WRITE)).isTrue();
    assertThat(readWrite.contains(WasiOpenFlags.CREATE)).isFalse();
  }

  @Test
  void testMultipleFlagCreation() {
    final WasiOpenFlags.WasiOpenFlagsSet writeCreateTruncate =
        WasiOpenFlags.of(WasiOpenFlags.WRITE, WasiOpenFlags.CREATE, WasiOpenFlags.TRUNCATE);

    assertThat(writeCreateTruncate.getValue()).isEqualTo(0x16);
    assertThat(writeCreateTruncate.contains(WasiOpenFlags.WRITE)).isTrue();
    assertThat(writeCreateTruncate.contains(WasiOpenFlags.CREATE)).isTrue();
    assertThat(writeCreateTruncate.contains(WasiOpenFlags.TRUNCATE)).isTrue();
    assertThat(writeCreateTruncate.contains(WasiOpenFlags.READ)).isFalse();
  }

  @Test
  void testFlagsSetCombination() {
    final WasiOpenFlags.WasiOpenFlagsSet readWrite =
        WasiOpenFlags.of(WasiOpenFlags.READ, WasiOpenFlags.WRITE);

    final WasiOpenFlags.WasiOpenFlagsSet readWriteCreate = readWrite.combine(WasiOpenFlags.CREATE);

    assertThat(readWriteCreate.getValue()).isEqualTo(0x07);
    assertThat(readWriteCreate.contains(WasiOpenFlags.READ)).isTrue();
    assertThat(readWriteCreate.contains(WasiOpenFlags.WRITE)).isTrue();
    assertThat(readWriteCreate.contains(WasiOpenFlags.CREATE)).isTrue();
  }

  @Test
  void testIsSetMethod() {
    final int flags = WasiOpenFlags.READ.getValue() | WasiOpenFlags.WRITE.getValue();

    assertThat(WasiOpenFlags.READ.isSet(flags)).isTrue();
    assertThat(WasiOpenFlags.WRITE.isSet(flags)).isTrue();
    assertThat(WasiOpenFlags.CREATE.isSet(flags)).isFalse();
  }

  @Test
  void testEmptyFlagsSet() {
    final WasiOpenFlags.WasiOpenFlagsSet empty = WasiOpenFlags.of();

    assertThat(empty.isEmpty()).isTrue();
    assertThat(empty.getValue()).isEqualTo(0);
  }

  @Test
  void testNullHandling() {
    assertThatThrownBy(() -> WasiOpenFlags.READ.combine(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Other flag cannot be null");

    assertThatThrownBy(() -> WasiOpenFlags.of((WasiOpenFlags[]) null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Flags array cannot be null");
  }

  @Test
  void testFlagsSetEqualsAndHashCode() {
    final WasiOpenFlags.WasiOpenFlagsSet set1 =
        WasiOpenFlags.of(WasiOpenFlags.READ, WasiOpenFlags.WRITE);
    final WasiOpenFlags.WasiOpenFlagsSet set2 =
        WasiOpenFlags.of(WasiOpenFlags.READ, WasiOpenFlags.WRITE);
    final WasiOpenFlags.WasiOpenFlagsSet set3 =
        WasiOpenFlags.of(WasiOpenFlags.READ, WasiOpenFlags.CREATE);

    assertThat(set1).isEqualTo(set2);
    assertThat(set1).isNotEqualTo(set3);
    assertThat(set1.hashCode()).isEqualTo(set2.hashCode());
    assertThat(set1.hashCode()).isNotEqualTo(set3.hashCode());
  }

  @Test
  void testFlagsSetToString() {
    final WasiOpenFlags.WasiOpenFlagsSet readWrite =
        WasiOpenFlags.of(WasiOpenFlags.READ, WasiOpenFlags.WRITE);

    final String str = readWrite.toString();

    assertThat(str).contains("WasiOpenFlagsSet");
    assertThat(str).contains("READ");
    assertThat(str).contains("WRITE");
  }

  @Test
  void testEmptyFlagsSetToString() {
    final WasiOpenFlags.WasiOpenFlagsSet empty = new WasiOpenFlags.WasiOpenFlagsSet(0);

    assertThat(empty.toString()).isEqualTo("WasiOpenFlagsSet[]");
  }
}
