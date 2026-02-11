package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import ai.tegmentum.wasmtime4j.metadata.CustomSection;
import ai.tegmentum.wasmtime4j.metadata.CustomSectionType;

/** Tests for CustomSection functionality. */
final class CustomSectionTest {

  @Test
  void testCustomSectionCreation() {
    final byte[] data = {0x01, 0x02, 0x03};
    final CustomSection section = new CustomSection("test", data, CustomSectionType.UNKNOWN);

    assertEquals("test", section.getName());
    assertArrayEquals(data, section.getData());
    assertEquals(CustomSectionType.UNKNOWN, section.getType());
    assertEquals(3, section.getSize());
    assertFalse(section.isEmpty());
  }

  @Test
  void testCustomSectionEmpty() {
    final byte[] data = {};
    final CustomSection section = new CustomSection("empty", data, CustomSectionType.UNKNOWN);

    assertEquals("empty", section.getName());
    assertArrayEquals(data, section.getData());
    assertEquals(0, section.getSize());
    assertTrue(section.isEmpty());
  }

  @Test
  void testCustomSectionDefensiveCopy() {
    final byte[] originalData = {0x01, 0x02, 0x03};
    final CustomSection section =
        new CustomSection("test", originalData, CustomSectionType.UNKNOWN);

    // Modify original data
    originalData[0] = (byte) 0x99;

    // Section data should not be affected
    final byte[] sectionData = section.getData();
    assertEquals(0x01, sectionData[0]);

    // Modify returned data
    sectionData[1] = (byte) 0x88;

    // Getting data again should return unmodified copy
    final byte[] freshData = section.getData();
    assertEquals(0x02, freshData[1]);
  }

  @Test
  void testCustomSectionCreateUnknown() {
    final byte[] data = {0x01, 0x02, 0x03};
    final CustomSection section = CustomSection.createUnknown("test", data);

    assertEquals("test", section.getName());
    assertEquals(CustomSectionType.UNKNOWN, section.getType());
    assertArrayEquals(data, section.getData());
  }

  @Test
  void testCustomSectionInvalidArguments() {
    final byte[] data = {0x01, 0x02, 0x03};

    assertThrows(
        IllegalArgumentException.class,
        () -> new CustomSection(null, data, CustomSectionType.UNKNOWN));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CustomSection("", data, CustomSectionType.UNKNOWN));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CustomSection("  ", data, CustomSectionType.UNKNOWN));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CustomSection("test", null, CustomSectionType.UNKNOWN));
    assertThrows(IllegalArgumentException.class, () -> new CustomSection("test", data, null));
  }

  @Test
  void testCustomSectionEquals() {
    final byte[] data1 = {0x01, 0x02, 0x03};
    final byte[] data2 = {0x01, 0x02, 0x03};
    final byte[] data3 = {0x01, 0x02, 0x04};

    final CustomSection section1 = new CustomSection("test", data1, CustomSectionType.NAME);
    final CustomSection section2 = new CustomSection("test", data2, CustomSectionType.NAME);
    final CustomSection section3 = new CustomSection("test", data3, CustomSectionType.NAME);
    final CustomSection section4 = new CustomSection("other", data1, CustomSectionType.NAME);
    final CustomSection section5 = new CustomSection("test", data1, CustomSectionType.PRODUCERS);

    assertEquals(section1, section2);
    assertNotEquals(section1, section3);
    assertNotEquals(section1, section4);
    assertNotEquals(section1, section5);
    assertNotEquals(section1, null);
    assertNotEquals(section1, "not a custom section");
  }

  @Test
  void testCustomSectionHashCode() {
    final byte[] data1 = {0x01, 0x02, 0x03};
    final byte[] data2 = {0x01, 0x02, 0x03};

    final CustomSection section1 = new CustomSection("test", data1, CustomSectionType.NAME);
    final CustomSection section2 = new CustomSection("test", data2, CustomSectionType.NAME);

    assertEquals(section1.hashCode(), section2.hashCode());
  }

  @Test
  void testCustomSectionToString() {
    final byte[] data = {0x01, 0x02, 0x03};
    final CustomSection section = new CustomSection("test", data, CustomSectionType.NAME);

    final String str = section.toString();
    assertTrue(str.contains("test"));
    assertTrue(str.contains("NAME"));
    assertTrue(str.contains("3"));
  }
}
