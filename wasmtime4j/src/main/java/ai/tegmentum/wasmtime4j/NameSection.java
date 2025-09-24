package ai.tegmentum.wasmtime4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a WebAssembly "name" custom section.
 *
 * <p>The name section provides human-readable names for various WebAssembly constructs including
 * modules, functions, locals, types, tables, memories, and globals. This information is primarily
 * used by debugging and development tools.
 *
 * @since 1.0.0
 */
public final class NameSection {

  private final String moduleName;
  private final Map<Integer, String> functionNames;
  private final Map<Integer, Map<Integer, String>> localNames;
  private final Map<Integer, String> typeNames;
  private final Map<Integer, String> tableNames;
  private final Map<Integer, String> memoryNames;
  private final Map<Integer, String> globalNames;
  private final Map<Integer, String> elementSegmentNames;
  private final Map<Integer, String> dataSegmentNames;
  private final Map<Integer, String> tagNames;

  private NameSection(final Builder builder) {
    this.moduleName = builder.moduleName;
    this.functionNames = builder.functionNames == null
        ? java.util.Collections.emptyMap()
        : java.util.Collections.unmodifiableMap(builder.functionNames);
    this.localNames = builder.localNames == null
        ? java.util.Collections.emptyMap()
        : java.util.Collections.unmodifiableMap(builder.localNames);
    this.typeNames = builder.typeNames == null
        ? java.util.Collections.emptyMap()
        : java.util.Collections.unmodifiableMap(builder.typeNames);
    this.tableNames = builder.tableNames == null
        ? java.util.Collections.emptyMap()
        : java.util.Collections.unmodifiableMap(builder.tableNames);
    this.memoryNames = builder.memoryNames == null
        ? java.util.Collections.emptyMap()
        : java.util.Collections.unmodifiableMap(builder.memoryNames);
    this.globalNames = builder.globalNames == null
        ? java.util.Collections.emptyMap()
        : java.util.Collections.unmodifiableMap(builder.globalNames);
    this.elementSegmentNames = builder.elementSegmentNames == null
        ? java.util.Collections.emptyMap()
        : java.util.Collections.unmodifiableMap(builder.elementSegmentNames);
    this.dataSegmentNames = builder.dataSegmentNames == null
        ? java.util.Collections.emptyMap()
        : java.util.Collections.unmodifiableMap(builder.dataSegmentNames);
    this.tagNames = builder.tagNames == null
        ? java.util.Collections.emptyMap()
        : java.util.Collections.unmodifiableMap(builder.tagNames);
  }

  /**
   * Gets the module name.
   *
   * @return the module name, or empty if not set
   */
  public Optional<String> getModuleName() {
    return Optional.ofNullable(moduleName);
  }

  /**
   * Gets the name of a function by its index.
   *
   * @param functionIndex the function index
   * @return the function name, or empty if not found
   */
  public Optional<String> getFunctionName(final int functionIndex) {
    return Optional.ofNullable(functionNames.get(functionIndex));
  }

  /**
   * Gets all function names.
   *
   * @return an immutable map of function indices to names
   */
  public Map<Integer, String> getFunctionNames() {
    return functionNames;
  }

  /**
   * Gets the name of a local variable by function index and local index.
   *
   * @param functionIndex the function index
   * @param localIndex the local variable index
   * @return the local variable name, or empty if not found
   */
  public Optional<String> getLocalName(final int functionIndex, final int localIndex) {
    final Map<Integer, String> locals = localNames.get(functionIndex);
    return locals != null ? Optional.ofNullable(locals.get(localIndex)) : Optional.empty();
  }

  /**
   * Gets all local variable names for a function.
   *
   * @param functionIndex the function index
   * @return an immutable map of local indices to names, or empty map if none found
   */
  public Map<Integer, String> getLocalNames(final int functionIndex) {
    final Map<Integer, String> locals = localNames.get(functionIndex);
    return locals != null ? locals : java.util.Collections.emptyMap();
  }

  /**
   * Gets all local variable names for all functions.
   *
   * @return an immutable map of function indices to local name maps
   */
  public Map<Integer, Map<Integer, String>> getAllLocalNames() {
    return localNames;
  }

  /**
   * Gets the name of a type by its index.
   *
   * @param typeIndex the type index
   * @return the type name, or empty if not found
   */
  public Optional<String> getTypeName(final int typeIndex) {
    return Optional.ofNullable(typeNames.get(typeIndex));
  }

  /**
   * Gets all type names.
   *
   * @return an immutable map of type indices to names
   */
  public Map<Integer, String> getTypeNames() {
    return typeNames;
  }

  /**
   * Gets the name of a table by its index.
   *
   * @param tableIndex the table index
   * @return the table name, or empty if not found
   */
  public Optional<String> getTableName(final int tableIndex) {
    return Optional.ofNullable(tableNames.get(tableIndex));
  }

  /**
   * Gets all table names.
   *
   * @return an immutable map of table indices to names
   */
  public Map<Integer, String> getTableNames() {
    return tableNames;
  }

  /**
   * Gets the name of a memory by its index.
   *
   * @param memoryIndex the memory index
   * @return the memory name, or empty if not found
   */
  public Optional<String> getMemoryName(final int memoryIndex) {
    return Optional.ofNullable(memoryNames.get(memoryIndex));
  }

  /**
   * Gets all memory names.
   *
   * @return an immutable map of memory indices to names
   */
  public Map<Integer, String> getMemoryNames() {
    return memoryNames;
  }

  /**
   * Gets the name of a global by its index.
   *
   * @param globalIndex the global index
   * @return the global name, or empty if not found
   */
  public Optional<String> getGlobalName(final int globalIndex) {
    return Optional.ofNullable(globalNames.get(globalIndex));
  }

  /**
   * Gets all global names.
   *
   * @return an immutable map of global indices to names
   */
  public Map<Integer, String> getGlobalNames() {
    return globalNames;
  }

  /**
   * Gets the name of an element segment by its index.
   *
   * @param elementIndex the element segment index
   * @return the element segment name, or empty if not found
   */
  public Optional<String> getElementSegmentName(final int elementIndex) {
    return Optional.ofNullable(elementSegmentNames.get(elementIndex));
  }

  /**
   * Gets all element segment names.
   *
   * @return an immutable map of element segment indices to names
   */
  public Map<Integer, String> getElementSegmentNames() {
    return elementSegmentNames;
  }

  /**
   * Gets the name of a data segment by its index.
   *
   * @param dataIndex the data segment index
   * @return the data segment name, or empty if not found
   */
  public Optional<String> getDataSegmentName(final int dataIndex) {
    return Optional.ofNullable(dataSegmentNames.get(dataIndex));
  }

  /**
   * Gets all data segment names.
   *
   * @return an immutable map of data segment indices to names
   */
  public Map<Integer, String> getDataSegmentNames() {
    return dataSegmentNames;
  }

  /**
   * Gets the name of a tag by its index.
   *
   * @param tagIndex the tag index
   * @return the tag name, or empty if not found
   */
  public Optional<String> getTagName(final int tagIndex) {
    return Optional.ofNullable(tagNames.get(tagIndex));
  }

  /**
   * Gets all tag names.
   *
   * @return an immutable map of tag indices to names
   */
  public Map<Integer, String> getTagNames() {
    return tagNames;
  }

  /**
   * Checks if this name section is empty.
   *
   * @return true if no names are defined
   */
  public boolean isEmpty() {
    return moduleName == null
        && functionNames.isEmpty()
        && localNames.isEmpty()
        && typeNames.isEmpty()
        && tableNames.isEmpty()
        && memoryNames.isEmpty()
        && globalNames.isEmpty()
        && elementSegmentNames.isEmpty()
        && dataSegmentNames.isEmpty()
        && tagNames.isEmpty();
  }

  /**
   * Gets a summary of this name section.
   *
   * @return a human-readable summary
   */
  public String getSummary() {
    final StringBuilder sb = new StringBuilder();
    sb.append("NameSection{");

    if (moduleName != null) {
      sb.append("module='").append(moduleName).append("', ");
    }

    sb.append("functions=").append(functionNames.size())
        .append(", locals=").append(localNames.size())
        .append(", types=").append(typeNames.size())
        .append(", tables=").append(tableNames.size())
        .append(", memories=").append(memoryNames.size())
        .append(", globals=").append(globalNames.size())
        .append(", elements=").append(elementSegmentNames.size())
        .append(", data=").append(dataSegmentNames.size())
        .append(", tags=").append(tagNames.size())
        .append("}");

    return sb.toString();
  }

  /**
   * Creates a new builder for constructing a NameSection.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for constructing NameSection instances.
   */
  public static final class Builder {
    private String moduleName;
    private Map<Integer, String> functionNames;
    private Map<Integer, Map<Integer, String>> localNames;
    private Map<Integer, String> typeNames;
    private Map<Integer, String> tableNames;
    private Map<Integer, String> memoryNames;
    private Map<Integer, String> globalNames;
    private Map<Integer, String> elementSegmentNames;
    private Map<Integer, String> dataSegmentNames;
    private Map<Integer, String> tagNames;

    private Builder() {}

    /**
     * Sets the module name.
     *
     * @param moduleName the module name
     * @return this builder
     */
    public Builder setModuleName(final String moduleName) {
      this.moduleName = moduleName;
      return this;
    }

    /**
     * Sets function names.
     *
     * @param functionNames map of function indices to names
     * @return this builder
     */
    public Builder setFunctionNames(final Map<Integer, String> functionNames) {
      this.functionNames = functionNames == null ? null : new java.util.HashMap<>(functionNames);
      return this;
    }

    /**
     * Sets local variable names.
     *
     * @param localNames map of function indices to local name maps
     * @return this builder
     */
    public Builder setLocalNames(final Map<Integer, Map<Integer, String>> localNames) {
      this.localNames = localNames == null ? null : new java.util.HashMap<>(localNames);
      return this;
    }

    /**
     * Sets type names.
     *
     * @param typeNames map of type indices to names
     * @return this builder
     */
    public Builder setTypeNames(final Map<Integer, String> typeNames) {
      this.typeNames = typeNames == null ? null : new java.util.HashMap<>(typeNames);
      return this;
    }

    /**
     * Sets table names.
     *
     * @param tableNames map of table indices to names
     * @return this builder
     */
    public Builder setTableNames(final Map<Integer, String> tableNames) {
      this.tableNames = tableNames == null ? null : new java.util.HashMap<>(tableNames);
      return this;
    }

    /**
     * Sets memory names.
     *
     * @param memoryNames map of memory indices to names
     * @return this builder
     */
    public Builder setMemoryNames(final Map<Integer, String> memoryNames) {
      this.memoryNames = memoryNames == null ? null : new java.util.HashMap<>(memoryNames);
      return this;
    }

    /**
     * Sets global names.
     *
     * @param globalNames map of global indices to names
     * @return this builder
     */
    public Builder setGlobalNames(final Map<Integer, String> globalNames) {
      this.globalNames = globalNames == null ? null : new java.util.HashMap<>(globalNames);
      return this;
    }

    /**
     * Sets element segment names.
     *
     * @param elementSegmentNames map of element segment indices to names
     * @return this builder
     */
    public Builder setElementSegmentNames(final Map<Integer, String> elementSegmentNames) {
      this.elementSegmentNames = elementSegmentNames == null ? null : new java.util.HashMap<>(elementSegmentNames);
      return this;
    }

    /**
     * Sets data segment names.
     *
     * @param dataSegmentNames map of data segment indices to names
     * @return this builder
     */
    public Builder setDataSegmentNames(final Map<Integer, String> dataSegmentNames) {
      this.dataSegmentNames = dataSegmentNames == null ? null : new java.util.HashMap<>(dataSegmentNames);
      return this;
    }

    /**
     * Sets tag names.
     *
     * @param tagNames map of tag indices to names
     * @return this builder
     */
    public Builder setTagNames(final Map<Integer, String> tagNames) {
      this.tagNames = tagNames == null ? null : new java.util.HashMap<>(tagNames);
      return this;
    }

    /**
     * Builds the NameSection.
     *
     * @return a new NameSection instance
     */
    public NameSection build() {
      return new NameSection(this);
    }
  }

  @Override
  public String toString() {
    return getSummary();
  }
}