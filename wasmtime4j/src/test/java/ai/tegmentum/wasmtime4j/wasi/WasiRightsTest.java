package ai.tegmentum.wasmtime4j.wasi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Test suite for WASI rights functionality.
 */
final class WasiRightsTest {

  @Test
  void testIndividualRights() {
    assertThat(WasiRights.FD_READ.getValue()).isEqualTo(0x0000000000000002L);
    assertThat(WasiRights.FD_WRITE.getValue()).isEqualTo(0x0000000000000040L);
    assertThat(WasiRights.FD_SEEK.getValue()).isEqualTo(0x0000000000000004L);
    assertThat(WasiRights.PATH_OPEN.getValue()).isEqualTo(0x0000000000002000L);
    assertThat(WasiRights.FD_READDIR.getValue()).isEqualTo(0x0000000000004000L);
  }

  @Test
  void testRightsCombination() {
    final WasiRights.WasiRightsSet readWrite = WasiRights.FD_READ.combine(WasiRights.FD_WRITE);

    assertThat(readWrite.getValue()).isEqualTo(0x0000000000000042L);
    assertThat(readWrite.contains(WasiRights.FD_READ)).isTrue();
    assertThat(readWrite.contains(WasiRights.FD_WRITE)).isTrue();
    assertThat(readWrite.contains(WasiRights.FD_SEEK)).isFalse();
  }

  @Test
  void testMultipleRightsCreation() {
    final WasiRights.WasiRightsSet fileRights = WasiRights.of(
        WasiRights.FD_READ,
        WasiRights.FD_WRITE,
        WasiRights.FD_SEEK,
        WasiRights.FD_TRUNCATE
    );

    assertThat(fileRights.contains(WasiRights.FD_READ)).isTrue();
    assertThat(fileRights.contains(WasiRights.FD_WRITE)).isTrue();
    assertThat(fileRights.contains(WasiRights.FD_SEEK)).isTrue();
    assertThat(fileRights.contains(WasiRights.FD_TRUNCATE)).isTrue();
    assertThat(fileRights.contains(WasiRights.PATH_OPEN)).isFalse();
  }

  @Test
  void testRightsSetCombination() {
    final WasiRights.WasiRightsSet readSeek = WasiRights.of(
        WasiRights.FD_READ,
        WasiRights.FD_SEEK
    );

    final WasiRights.WasiRightsSet readSeekWrite = readSeek.combine(WasiRights.FD_WRITE);

    assertThat(readSeekWrite.contains(WasiRights.FD_READ)).isTrue();
    assertThat(readSeekWrite.contains(WasiRights.FD_SEEK)).isTrue();
    assertThat(readSeekWrite.contains(WasiRights.FD_WRITE)).isTrue();
  }

  @Test
  void testContainsAll() {
    final WasiRights.WasiRightsSet allFileRights = WasiRights.of(
        WasiRights.FD_READ,
        WasiRights.FD_WRITE,
        WasiRights.FD_SEEK
    );

    final WasiRights.WasiRightsSet readOnly = WasiRights.of(WasiRights.FD_READ);
    final WasiRights.WasiRightsSet readWrite = WasiRights.of(
        WasiRights.FD_READ,
        WasiRights.FD_WRITE
    );

    assertThat(allFileRights.containsAll(readOnly)).isTrue();
    assertThat(allFileRights.containsAll(readWrite)).isTrue();
    assertThat(readOnly.containsAll(allFileRights)).isFalse();
  }

  @Test
  void testIsSetMethod() {
    final long rights = WasiRights.FD_READ.getValue() | WasiRights.FD_WRITE.getValue();

    assertThat(WasiRights.FD_READ.isSet(rights)).isTrue();
    assertThat(WasiRights.FD_WRITE.isSet(rights)).isTrue();
    assertThat(WasiRights.FD_SEEK.isSet(rights)).isFalse();
  }

  @Test
  void testEmptyRightsSet() {
    final WasiRights.WasiRightsSet empty = WasiRights.of();

    assertThat(empty.isEmpty()).isTrue();
    assertThat(empty.getValue()).isEqualTo(0L);
  }

  @Test
  void testNullHandling() {
    assertThatThrownBy(() -> WasiRights.FD_READ.combine(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Other right cannot be null");

    assertThatThrownBy(() -> WasiRights.of((WasiRights[]) null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Rights array cannot be null or empty");

    assertThatThrownBy(() -> WasiRights.of())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Rights array cannot be null or empty");

    final WasiRights.WasiRightsSet set = WasiRights.of(WasiRights.FD_READ);

    assertThatThrownBy(() -> set.contains(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Right cannot be null");

    assertThatThrownBy(() -> set.containsAll(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Other rights set cannot be null");
  }

  @Test
  void testRightsSetEqualsAndHashCode() {
    final WasiRights.WasiRightsSet set1 = WasiRights.of(
        WasiRights.FD_READ,
        WasiRights.FD_WRITE
    );
    final WasiRights.WasiRightsSet set2 = WasiRights.of(
        WasiRights.FD_READ,
        WasiRights.FD_WRITE
    );
    final WasiRights.WasiRightsSet set3 = WasiRights.of(
        WasiRights.FD_READ,
        WasiRights.FD_SEEK
    );

    assertThat(set1).isEqualTo(set2);
    assertThat(set1).isNotEqualTo(set3);
    assertThat(set1.hashCode()).isEqualTo(set2.hashCode());
    assertThat(set1.hashCode()).isNotEqualTo(set3.hashCode());
  }

  @Test
  void testRightsSetToString() {
    final WasiRights.WasiRightsSet readWrite = WasiRights.of(
        WasiRights.FD_READ,
        WasiRights.FD_WRITE
    );

    final String str = readWrite.toString();

    assertThat(str).contains("WasiRightsSet");
    assertThat(str).contains("FD_READ");
    assertThat(str).contains("FD_WRITE");
  }

  @Test
  void testEmptyRightsSetToString() {
    final WasiRights.WasiRightsSet empty = new WasiRights.WasiRightsSet(0L);

    assertThat(empty.toString()).isEqualTo("WasiRightsSet[]");
  }

  @Test
  void testDirectoryRights() {
    final WasiRights.WasiRightsSet dirRights = WasiRights.of(
        WasiRights.PATH_OPEN,
        WasiRights.FD_READDIR,
        WasiRights.PATH_CREATE_DIRECTORY,
        WasiRights.PATH_REMOVE_DIRECTORY
    );

    assertThat(dirRights.contains(WasiRights.PATH_OPEN)).isTrue();
    assertThat(dirRights.contains(WasiRights.FD_READDIR)).isTrue();
    assertThat(dirRights.contains(WasiRights.PATH_CREATE_DIRECTORY)).isTrue();
    assertThat(dirRights.contains(WasiRights.PATH_REMOVE_DIRECTORY)).isTrue();
    assertThat(dirRights.contains(WasiRights.FD_READ)).isFalse();
  }

  @Test
  void testFileStatRights() {
    final WasiRights.WasiRightsSet statRights = WasiRights.of(
        WasiRights.PATH_FILESTAT_GET,
        WasiRights.FD_FILESTAT_GET,
        WasiRights.FD_FILESTAT_SET_SIZE,
        WasiRights.FD_FILESTAT_SET_TIMES
    );

    assertThat(statRights.contains(WasiRights.PATH_FILESTAT_GET)).isTrue();
    assertThat(statRights.contains(WasiRights.FD_FILESTAT_GET)).isTrue();
    assertThat(statRights.contains(WasiRights.FD_FILESTAT_SET_SIZE)).isTrue();
    assertThat(statRights.contains(WasiRights.FD_FILESTAT_SET_TIMES)).isTrue();
  }
}