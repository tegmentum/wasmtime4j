package ai.tegmentum.wasmtime4j;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CustomSectionType functionality.
 */
final class CustomSectionTypeTest {

  @Test
  void testStandardSectionTypes() {
    assertEquals("name", CustomSectionType.NAME.getPrimaryName());
    assertEquals("producers", CustomSectionType.PRODUCERS.getPrimaryName());
    assertEquals("target_features", CustomSectionType.TARGET_FEATURES.getPrimaryName());
    assertNull(CustomSectionType.UNKNOWN.getPrimaryName());
  }

  @Test
  void testSectionTypeMatching() {
    assertTrue(CustomSectionType.NAME.matches("name"));
    assertFalse(CustomSectionType.NAME.matches("names"));
    assertFalse(CustomSectionType.NAME.matches("producer"));

    assertTrue(CustomSectionType.DWARF.matches(".debug_info"));
    assertTrue(CustomSectionType.DWARF.matches(".debug_line"));
    assertTrue(CustomSectionType.DWARF.matches(".debug_str"));
    assertFalse(CustomSectionType.DWARF.matches("debug_info"));

    assertTrue(CustomSectionType.RELOC.matches("reloc.CODE"));
    assertTrue(CustomSectionType.RELOC.matches("reloc.DATA"));
    assertFalse(CustomSectionType.RELOC.matches("reloc"));

    assertFalse(CustomSectionType.UNKNOWN.matches("anything"));
  }

  @Test
  void testFromName() {
    assertEquals(CustomSectionType.NAME, CustomSectionType.fromName("name"));
    assertEquals(CustomSectionType.PRODUCERS, CustomSectionType.fromName("producers"));
    assertEquals(CustomSectionType.TARGET_FEATURES, CustomSectionType.fromName("target_features"));
    assertEquals(CustomSectionType.DWARF, CustomSectionType.fromName(".debug_info"));
    assertEquals(CustomSectionType.DWARF, CustomSectionType.fromName(".debug_line"));
    assertEquals(CustomSectionType.LINKING, CustomSectionType.fromName("linking"));
    assertEquals(CustomSectionType.RELOC, CustomSectionType.fromName("reloc.CODE"));
    assertEquals(CustomSectionType.SOURCE_MAP, CustomSectionType.fromName("sourceMappingURL"));
    assertEquals(CustomSectionType.UNKNOWN, CustomSectionType.fromName("custom_section"));
    assertEquals(CustomSectionType.UNKNOWN, CustomSectionType.fromName("unknown"));
  }

  @Test
  void testFromNameInvalidArgument() {
    assertThrows(IllegalArgumentException.class, () -> CustomSectionType.fromName(null));
  }

  @Test
  void testMatchesInvalidArgument() {
    assertThrows(IllegalArgumentException.class, () -> CustomSectionType.NAME.matches(null));
  }

  @Test
  void testSectionTypeCategories() {
    // Debugging sections
    assertTrue(CustomSectionType.NAME.isDebuggingSection());
    assertTrue(CustomSectionType.DWARF.isDebuggingSection());
    assertTrue(CustomSectionType.SOURCE_MAP.isDebuggingSection());
    assertFalse(CustomSectionType.PRODUCERS.isDebuggingSection());
    assertFalse(CustomSectionType.TARGET_FEATURES.isDebuggingSection());
    assertFalse(CustomSectionType.UNKNOWN.isDebuggingSection());

    // Toolchain sections
    assertTrue(CustomSectionType.PRODUCERS.isToolchainSection());
    assertTrue(CustomSectionType.TARGET_FEATURES.isToolchainSection());
    assertFalse(CustomSectionType.NAME.isToolchainSection());
    assertFalse(CustomSectionType.DWARF.isToolchainSection());
    assertFalse(CustomSectionType.UNKNOWN.isToolchainSection());

    // Linking sections
    assertTrue(CustomSectionType.LINKING.isLinkingSection());
    assertTrue(CustomSectionType.RELOC.isLinkingSection());
    assertFalse(CustomSectionType.NAME.isLinkingSection());
    assertFalse(CustomSectionType.PRODUCERS.isLinkingSection());
    assertFalse(CustomSectionType.UNKNOWN.isLinkingSection());
  }

  @Test
  void testGetNames() {
    final String[] nameNames = CustomSectionType.NAME.getNames();
    assertEquals(1, nameNames.length);
    assertEquals("name", nameNames[0]);

    final String[] dwarfNames = CustomSectionType.DWARF.getNames();
    assertTrue(dwarfNames.length > 1);
    assertTrue(java.util.Arrays.asList(dwarfNames).contains(".debug_info"));
    assertTrue(java.util.Arrays.asList(dwarfNames).contains(".debug_line"));

    final String[] unknownNames = CustomSectionType.UNKNOWN.getNames();
    assertEquals(0, unknownNames.length);
  }

  @Test
  void testComprehensiveSectionTypeMapping() {
    // Test all standard section types can be recognized
    final String[] testSections = {
        "name",
        "producers",
        "target_features",
        ".debug_info",
        ".debug_line",
        ".debug_abbrev",
        ".debug_str",
        ".debug_ranges",
        ".debug_loc",
        "sourceMappingURL",
        "linking",
        "reloc.CODE",
        "reloc.DATA"
    };

    for (final String sectionName : testSections) {
      final CustomSectionType type = CustomSectionType.fromName(sectionName);
      assertNotEquals(CustomSectionType.UNKNOWN, type,
          "Section '" + sectionName + "' should be recognized");
      assertTrue(type.matches(sectionName),
          "Section type should match its name: " + sectionName);
    }
  }

  @Test
  void testUnknownSectionHandling() {
    final String[] unknownSections = {
        "my_custom_section",
        "application_data",
        "metadata",
        "custom",
        ""
    };

    for (final String sectionName : unknownSections) {
      final CustomSectionType type = CustomSectionType.fromName(sectionName);
      assertEquals(CustomSectionType.UNKNOWN, type,
          "Section '" + sectionName + "' should be UNKNOWN");
    }
  }
}