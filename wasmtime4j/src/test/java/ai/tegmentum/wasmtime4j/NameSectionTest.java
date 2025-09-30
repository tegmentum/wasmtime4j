package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests for NameSection functionality. */
final class NameSectionTest {

  @Test
  void testEmptyNameSection() {
    final NameSection nameSection = NameSection.builder().build();

    assertTrue(nameSection.isEmpty());
    assertFalse(nameSection.getModuleName().isPresent());
    assertTrue(nameSection.getFunctionNames().isEmpty());
    assertTrue(nameSection.getAllLocalNames().isEmpty());
    assertTrue(nameSection.getTypeNames().isEmpty());
    assertTrue(nameSection.getTableNames().isEmpty());
    assertTrue(nameSection.getMemoryNames().isEmpty());
    assertTrue(nameSection.getGlobalNames().isEmpty());
    assertTrue(nameSection.getElementSegmentNames().isEmpty());
    assertTrue(nameSection.getDataSegmentNames().isEmpty());
    assertTrue(nameSection.getTagNames().isEmpty());
  }

  @Test
  void testNameSectionWithModuleName() {
    final NameSection nameSection = NameSection.builder().setModuleName("test_module").build();

    assertFalse(nameSection.isEmpty());
    assertTrue(nameSection.getModuleName().isPresent());
    assertEquals("test_module", nameSection.getModuleName().get());
  }

  @Test
  void testNameSectionWithFunctionNames() {
    final Map<Integer, String> functionNames = new HashMap<>();
    functionNames.put(0, "main");
    functionNames.put(1, "helper");

    final NameSection nameSection = NameSection.builder().setFunctionNames(functionNames).build();

    assertFalse(nameSection.isEmpty());
    assertEquals(2, nameSection.getFunctionNames().size());
    assertEquals("main", nameSection.getFunctionName(0).orElse(null));
    assertEquals("helper", nameSection.getFunctionName(1).orElse(null));
    assertFalse(nameSection.getFunctionName(2).isPresent());
  }

  @Test
  void testNameSectionWithLocalNames() {
    final Map<Integer, Map<Integer, String>> localNames = new HashMap<>();
    final Map<Integer, String> function0Locals = new HashMap<>();
    function0Locals.put(0, "param1");
    function0Locals.put(1, "param2");
    function0Locals.put(2, "local1");
    localNames.put(0, function0Locals);

    final Map<Integer, String> function1Locals = new HashMap<>();
    function1Locals.put(0, "value");
    localNames.put(1, function1Locals);

    final NameSection nameSection = NameSection.builder().setLocalNames(localNames).build();

    assertFalse(nameSection.isEmpty());
    assertEquals(2, nameSection.getAllLocalNames().size());

    assertEquals("param1", nameSection.getLocalName(0, 0).orElse(null));
    assertEquals("param2", nameSection.getLocalName(0, 1).orElse(null));
    assertEquals("local1", nameSection.getLocalName(0, 2).orElse(null));
    assertEquals("value", nameSection.getLocalName(1, 0).orElse(null));

    assertFalse(nameSection.getLocalName(0, 3).isPresent());
    assertFalse(nameSection.getLocalName(2, 0).isPresent());

    assertEquals(3, nameSection.getLocalNames(0).size());
    assertEquals(1, nameSection.getLocalNames(1).size());
    assertTrue(nameSection.getLocalNames(2).isEmpty());
  }

  @Test
  void testNameSectionWithAllTypes() {
    final Map<Integer, String> functionNames = new HashMap<>();
    functionNames.put(0, "main");

    final Map<Integer, String> typeNames = new HashMap<>();
    typeNames.put(0, "i32_to_i32");

    final Map<Integer, String> tableNames = new HashMap<>();
    tableNames.put(0, "function_table");

    final Map<Integer, String> memoryNames = new HashMap<>();
    memoryNames.put(0, "linear_memory");

    final Map<Integer, String> globalNames = new HashMap<>();
    globalNames.put(0, "global_counter");

    final Map<Integer, String> elementNames = new HashMap<>();
    elementNames.put(0, "init_functions");

    final Map<Integer, String> dataNames = new HashMap<>();
    dataNames.put(0, "string_data");

    final Map<Integer, String> tagNames = new HashMap<>();
    tagNames.put(0, "error_tag");

    final NameSection nameSection =
        NameSection.builder()
            .setModuleName("comprehensive_module")
            .setFunctionNames(functionNames)
            .setTypeNames(typeNames)
            .setTableNames(tableNames)
            .setMemoryNames(memoryNames)
            .setGlobalNames(globalNames)
            .setElementSegmentNames(elementNames)
            .setDataSegmentNames(dataNames)
            .setTagNames(tagNames)
            .build();

    assertFalse(nameSection.isEmpty());

    assertEquals("comprehensive_module", nameSection.getModuleName().get());
    assertEquals("main", nameSection.getFunctionName(0).get());
    assertEquals("i32_to_i32", nameSection.getTypeName(0).get());
    assertEquals("function_table", nameSection.getTableName(0).get());
    assertEquals("linear_memory", nameSection.getMemoryName(0).get());
    assertEquals("global_counter", nameSection.getGlobalName(0).get());
    assertEquals("init_functions", nameSection.getElementSegmentName(0).get());
    assertEquals("string_data", nameSection.getDataSegmentName(0).get());
    assertEquals("error_tag", nameSection.getTagName(0).get());
  }

  @Test
  void testNameSectionSummary() {
    final Map<Integer, String> functionNames = new HashMap<>();
    functionNames.put(0, "main");
    functionNames.put(1, "helper");

    final NameSection nameSection =
        NameSection.builder().setModuleName("test_module").setFunctionNames(functionNames).build();

    final String summary = nameSection.getSummary();
    assertTrue(summary.contains("test_module"));
    assertTrue(summary.contains("functions=2"));
  }

  @Test
  void testNameSectionToString() {
    final NameSection nameSection = NameSection.builder().setModuleName("test").build();

    final String str = nameSection.toString();
    assertTrue(str.contains("test"));
  }

  @Test
  void testNameSectionBuilderDefensiveCopy() {
    final Map<Integer, String> originalFunctionNames = new HashMap<>();
    originalFunctionNames.put(0, "main");

    final NameSection.Builder builder =
        NameSection.builder().setFunctionNames(originalFunctionNames);

    // Modify original map
    originalFunctionNames.put(1, "added_after");

    final NameSection nameSection = builder.build();

    // Should not contain the modification
    assertEquals(1, nameSection.getFunctionNames().size());
    assertFalse(nameSection.getFunctionName(1).isPresent());
  }

  @Test
  void testNameSectionImmutability() {
    final Map<Integer, String> functionNames = new HashMap<>();
    functionNames.put(0, "main");

    final NameSection nameSection = NameSection.builder().setFunctionNames(functionNames).build();

    final Map<Integer, String> retrievedNames = nameSection.getFunctionNames();

    assertThrows(UnsupportedOperationException.class, () -> retrievedNames.put(1, "hacker"));
  }
}
