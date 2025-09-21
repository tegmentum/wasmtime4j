package ai.tegmentum.wasmtime4j.wasi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Test suite for WASI file permissions functionality. */
final class WasiPermissionsTest {

  @Test
  void testOctalPermissions() {
    final WasiPermissions perms644 = WasiPermissions.of(0644);

    assertThat(perms644.getMode()).isEqualTo(0644);
    assertThat(perms644.isOwnerRead()).isTrue();
    assertThat(perms644.isOwnerWrite()).isTrue();
    assertThat(perms644.isOwnerExecute()).isFalse();
    assertThat(perms644.isGroupRead()).isTrue();
    assertThat(perms644.isGroupWrite()).isFalse();
    assertThat(perms644.isGroupExecute()).isFalse();
    assertThat(perms644.isOtherRead()).isTrue();
    assertThat(perms644.isOtherWrite()).isFalse();
    assertThat(perms644.isOtherExecute()).isFalse();
  }

  @Test
  void testExecutablePermissions() {
    final WasiPermissions perms755 = WasiPermissions.of(0755);

    assertThat(perms755.getMode()).isEqualTo(0755);
    assertThat(perms755.isOwnerRead()).isTrue();
    assertThat(perms755.isOwnerWrite()).isTrue();
    assertThat(perms755.isOwnerExecute()).isTrue();
    assertThat(perms755.isGroupRead()).isTrue();
    assertThat(perms755.isGroupWrite()).isFalse();
    assertThat(perms755.isGroupExecute()).isTrue();
    assertThat(perms755.isOtherRead()).isTrue();
    assertThat(perms755.isOtherWrite()).isFalse();
    assertThat(perms755.isOtherExecute()).isTrue();
  }

  @Test
  void testSpecialBits() {
    final WasiPermissions perms4755 = WasiPermissions.of(04755);

    assertThat(perms4755.isSetuid()).isTrue();
    assertThat(perms4755.isSetgid()).isFalse();
    assertThat(perms4755.isSticky()).isFalse();

    final WasiPermissions perms6755 = WasiPermissions.of(06755);

    assertThat(perms6755.isSetuid()).isTrue();
    assertThat(perms6755.isSetgid()).isTrue();
    assertThat(perms6755.isSticky()).isFalse();

    final WasiPermissions perms1755 = WasiPermissions.of(01755);

    assertThat(perms1755.isSetuid()).isFalse();
    assertThat(perms1755.isSetgid()).isFalse();
    assertThat(perms1755.isSticky()).isTrue();
  }

  @Test
  void testBuilderPattern() {
    final WasiPermissions perms =
        WasiPermissions.builder()
            .ownerRead(true)
            .ownerWrite(true)
            .ownerExecute(false)
            .groupRead(true)
            .groupWrite(false)
            .groupExecute(false)
            .otherRead(true)
            .otherWrite(false)
            .otherExecute(false)
            .build();

    assertThat(perms.getMode()).isEqualTo(0644);
    assertThat(perms.isOwnerRead()).isTrue();
    assertThat(perms.isOwnerWrite()).isTrue();
    assertThat(perms.isOwnerExecute()).isFalse();
  }

  @Test
  void testBuilderWithSpecialBits() {
    final WasiPermissions perms =
        WasiPermissions.builder().mode(0755).setuid(true).setgid(true).build();

    assertThat(perms.getMode()).isEqualTo(06755);
    assertThat(perms.isSetuid()).isTrue();
    assertThat(perms.isSetgid()).isTrue();
    assertThat(perms.isSticky()).isFalse();
  }

  @Test
  void testDefaultPermissions() {
    final WasiPermissions filePerms = WasiPermissions.defaultFilePermissions();
    assertThat(filePerms.getMode()).isEqualTo(0644);

    final WasiPermissions dirPerms = WasiPermissions.defaultDirectoryPermissions();
    assertThat(dirPerms.getMode()).isEqualTo(0755);
  }

  @Test
  void testInvalidPermissions() {
    assertThatThrownBy(() -> WasiPermissions.of(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid permission mode");

    assertThatThrownBy(() -> WasiPermissions.of(010000))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid permission mode");
  }

  @Test
  void testEqualsAndHashCode() {
    final WasiPermissions perms1 = WasiPermissions.of(0644);
    final WasiPermissions perms2 = WasiPermissions.of(0644);
    final WasiPermissions perms3 = WasiPermissions.of(0755);

    assertThat(perms1).isEqualTo(perms2);
    assertThat(perms1).isNotEqualTo(perms3);
    assertThat(perms1.hashCode()).isEqualTo(perms2.hashCode());
    assertThat(perms1.hashCode()).isNotEqualTo(perms3.hashCode());
  }

  @Test
  void testToString() {
    final WasiPermissions perms = WasiPermissions.of(0644);
    final String str = perms.toString();

    assertThat(str).contains("WasiPermissions");
    assertThat(str).contains("644");
  }
}
