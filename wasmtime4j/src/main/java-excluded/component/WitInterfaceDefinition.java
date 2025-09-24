/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Advanced WIT (WebAssembly Interface Types) interface definition with comprehensive type system
 * support, versioning, compatibility checking, and migration capabilities.
 *
 * <p>This class provides enterprise-grade WIT interface management including:
 *
 * <ul>
 *   <li>Advanced WIT type system features and validation
 *   <li>Interface versioning and evolution support
 *   <li>Compatibility checking and migration planning
 *   <li>Interface documentation and introspection
 *   <li>Performance optimization and caching
 * </ul>
 *
 * @since 1.0.0
 */
public final class WitInterfaceDefinition {

  private final String interfaceName;
  private final WitInterfaceVersion version;
  private final Set<WitFunction> functions;
  private final Set<WitType> types;
  private final Set<WitResource> resources;
  private final Map<String, WitValue> constants;
  private final WitInterfaceMetadata metadata;
  private final WitInterfaceSchema schema;
  private final WitInterfaceDocumentation documentation;

  /**
   * Creates a new WIT interface definition.
   *
   * @param builder the interface definition builder
   */
  private WitInterfaceDefinition(final Builder builder) {
    this.interfaceName =
        Objects.requireNonNull(builder.interfaceName, "Interface name cannot be null");
    this.version = Objects.requireNonNull(builder.version, "Interface version cannot be null");
    this.functions = Set.copyOf(builder.functions);
    this.types = Set.copyOf(builder.types);
    this.resources = Set.copyOf(builder.resources);
    this.constants = Map.copyOf(builder.constants);
    this.metadata = builder.metadata;
    this.schema = builder.schema;
    this.documentation = builder.documentation;
  }

  /**
   * Creates a new interface definition builder.
   *
   * @param interfaceName the interface name
   * @param version the interface version
   * @return a new builder instance
   */
  public static Builder builder(final String interfaceName, final WitInterfaceVersion version) {
    return new Builder(interfaceName, version);
  }

  /**
   * Parses a WIT interface definition from its textual representation.
   *
   * @param witSource the WIT interface source code
   * @param parseConfig the parsing configuration
   * @return a future that completes with the parsed interface definition
   * @throws WasmException if parsing fails
   */
  public static CompletableFuture<WitInterfaceDefinition> parseFromSource(
      final String witSource, final WitParseConfig parseConfig) throws WasmException {
    Objects.requireNonNull(witSource, "WIT source cannot be null");
    Objects.requireNonNull(parseConfig, "Parse config cannot be null");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return WitInterfaceParser.parse(witSource, parseConfig);
          } catch (final Exception e) {
            throw new RuntimeException("Failed to parse WIT interface", e);
          }
        });
  }

  /**
   * Gets the interface name.
   *
   * @return the interface name
   */
  public String getInterfaceName() {
    return interfaceName;
  }

  /**
   * Gets the interface version.
   *
   * @return the interface version
   */
  public WitInterfaceVersion getVersion() {
    return version;
  }

  /**
   * Gets all functions defined in this interface.
   *
   * @return the set of interface functions
   */
  public Set<WitFunction> getFunctions() {
    return functions;
  }

  /**
   * Gets a specific function by name.
   *
   * @param functionName the function name
   * @return the function, or null if not found
   */
  public WitFunction getFunction(final String functionName) {
    Objects.requireNonNull(functionName, "Function name cannot be null");
    return functions.stream()
        .filter(func -> func.getName().equals(functionName))
        .findFirst()
        .orElse(null);
  }

  /**
   * Gets all types defined in this interface.
   *
   * @return the set of interface types
   */
  public Set<WitType> getTypes() {
    return types;
  }

  /**
   * Gets a specific type by name.
   *
   * @param typeName the type name
   * @return the type, or null if not found
   */
  public WitType getType(final String typeName) {
    Objects.requireNonNull(typeName, "Type name cannot be null");
    return types.stream().filter(type -> type.getName().equals(typeName)).findFirst().orElse(null);
  }

  /**
   * Gets all resources defined in this interface.
   *
   * @return the set of interface resources
   */
  public Set<WitResource> getResources() {
    return resources;
  }

  /**
   * Gets a specific resource by name.
   *
   * @param resourceName the resource name
   * @return the resource, or null if not found
   */
  public WitResource getResource(final String resourceName) {
    Objects.requireNonNull(resourceName, "Resource name cannot be null");
    return resources.stream()
        .filter(resource -> resource.getName().equals(resourceName))
        .findFirst()
        .orElse(null);
  }

  /**
   * Gets all constants defined in this interface.
   *
   * @return the map of constant names to values
   */
  public Map<String, WitValue> getConstants() {
    return constants;
  }

  /**
   * Gets a specific constant by name.
   *
   * @param constantName the constant name
   * @return the constant value, or null if not found
   */
  public WitValue getConstant(final String constantName) {
    Objects.requireNonNull(constantName, "Constant name cannot be null");
    return constants.get(constantName);
  }

  /**
   * Gets the interface metadata.
   *
   * @return the interface metadata
   */
  public WitInterfaceMetadata getMetadata() {
    return metadata;
  }

  /**
   * Gets the interface schema.
   *
   * @return the interface schema
   */
  public WitInterfaceSchema getSchema() {
    return schema;
  }

  /**
   * Gets the interface documentation.
   *
   * @return the interface documentation
   */
  public WitInterfaceDocumentation getDocumentation() {
    return documentation;
  }

  // Compatibility and Validation

  /**
   * Checks compatibility with another WIT interface.
   *
   * @param other the other interface to check compatibility with
   * @param compatibilityConfig the compatibility checking configuration
   * @return the compatibility result
   * @throws WasmException if compatibility check fails
   */
  public WitCompatibilityResult checkCompatibility(
      final WitInterfaceDefinition other, final WitCompatibilityConfig compatibilityConfig)
      throws WasmException {
    Objects.requireNonNull(other, "Other interface cannot be null");
    Objects.requireNonNull(compatibilityConfig, "Compatibility config cannot be null");

    return WitCompatibilityChecker.checkCompatibility(this, other, compatibilityConfig);
  }

  /**
   * Validates this interface definition for correctness and completeness.
   *
   * @param validationConfig the validation configuration
   * @return the validation result
   * @throws WasmException if validation fails
   */
  public WitInterfaceValidationResult validate(final WitInterfaceValidationConfig validationConfig)
      throws WasmException {
    Objects.requireNonNull(validationConfig, "Validation config cannot be null");

    return WitInterfaceValidator.validate(this, validationConfig);
  }

  /**
   * Checks if this interface is backward compatible with another version.
   *
   * @param olderVersion the older interface version
   * @return true if backward compatible
   * @throws WasmException if compatibility check fails
   */
  public boolean isBackwardCompatibleWith(final WitInterfaceDefinition olderVersion)
      throws WasmException {
    Objects.requireNonNull(olderVersion, "Older version cannot be null");

    if (!this.interfaceName.equals(olderVersion.interfaceName)) {
      return false;
    }

    final WitCompatibilityConfig config =
        WitCompatibilityConfig.builder()
            .strictTypeChecking(true)
            .allowFunctionAdditions(true)
            .allowFunctionRemovals(false)
            .build();

    final WitCompatibilityResult result = checkCompatibility(olderVersion, config);
    return result.isCompatible();
  }

  // Evolution and Migration

  /**
   * Creates a migration plan to evolve to a newer interface version.
   *
   * @param targetVersion the target interface version
   * @param migrationConfig the migration configuration
   * @return a future that completes with the migration plan
   * @throws WasmException if migration planning fails
   */
  public CompletableFuture<WitInterfaceMigrationPlan> createMigrationPlan(
      final WitInterfaceDefinition targetVersion, final WitMigrationConfig migrationConfig)
      throws WasmException {
    Objects.requireNonNull(targetVersion, "Target version cannot be null");
    Objects.requireNonNull(migrationConfig, "Migration config cannot be null");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return WitInterfaceMigrationPlanner.createMigrationPlan(
                this, targetVersion, migrationConfig);
          } catch (final Exception e) {
            throw new RuntimeException("Failed to create migration plan", e);
          }
        });
  }

  /**
   * Applies an interface evolution to create a new version.
   *
   * @param evolution the interface evolution to apply
   * @return the evolved interface definition
   * @throws WasmException if evolution application fails
   */
  public WitInterfaceDefinition applyEvolution(final WitInterfaceEvolution evolution)
      throws WasmException {
    Objects.requireNonNull(evolution, "Evolution cannot be null");

    return WitInterfaceEvolutionEngine.applyEvolution(this, evolution);
  }

  // Introspection and Analysis

  /**
   * Gets introspection information about this interface.
   *
   * @param introspectionConfig the introspection configuration
   * @return the interface introspection data
   * @throws WasmException if introspection fails
   */
  public WitInterfaceIntrospection getIntrospection(
      final WitIntrospectionConfig introspectionConfig) throws WasmException {
    Objects.requireNonNull(introspectionConfig, "Introspection config cannot be null");

    return WitInterfaceIntrospector.introspect(this, introspectionConfig);
  }

  /**
   * Analyzes the complexity and performance characteristics of this interface.
   *
   * @param analysisConfig the analysis configuration
   * @return the interface analysis result
   * @throws WasmException if analysis fails
   */
  public WitInterfaceAnalysisResult analyze(final WitInterfaceAnalysisConfig analysisConfig)
      throws WasmException {
    Objects.requireNonNull(analysisConfig, "Analysis config cannot be null");

    return WitInterfaceAnalyzer.analyze(this, analysisConfig);
  }

  /**
   * Gets a dependency graph of types used in this interface.
   *
   * @return the type dependency graph
   * @throws WasmException if dependency analysis fails
   */
  public WitTypeDependencyGraph getTypeDependencyGraph() throws WasmException {
    return WitTypeDependencyAnalyzer.analyze(this);
  }

  // Serialization and Representation

  /**
   * Serializes this interface definition to its WIT textual representation.
   *
   * @param serializationConfig the serialization configuration
   * @return the WIT interface source code
   * @throws WasmException if serialization fails
   */
  public String toWitSource(final WitSerializationConfig serializationConfig) throws WasmException {
    Objects.requireNonNull(serializationConfig, "Serialization config cannot be null");

    return WitInterfaceSerializer.serialize(this, serializationConfig);
  }

  /**
   * Serializes this interface definition to binary format.
   *
   * @param binaryConfig the binary serialization configuration
   * @return the binary representation
   * @throws WasmException if binary serialization fails
   */
  public byte[] toBinary(final WitBinarySerializationConfig binaryConfig) throws WasmException {
    Objects.requireNonNull(binaryConfig, "Binary config cannot be null");

    return WitInterfaceBinarySerializer.serialize(this, binaryConfig);
  }

  /**
   * Creates a JSON representation of this interface definition.
   *
   * @param jsonConfig the JSON serialization configuration
   * @return the JSON representation
   * @throws WasmException if JSON serialization fails
   */
  public String toJson(final WitJsonSerializationConfig jsonConfig) throws WasmException {
    Objects.requireNonNull(jsonConfig, "JSON config cannot be null");

    return WitInterfaceJsonSerializer.serialize(this, jsonConfig);
  }

  // Caching and Optimization

  /**
   * Gets a cached and optimized version of this interface definition.
   *
   * @param cacheConfig the caching configuration
   * @return the cached interface definition
   * @throws WasmException if caching fails
   */
  public WitInterfaceDefinition getCachedVersion(final WitInterfaceCacheConfig cacheConfig)
      throws WasmException {
    Objects.requireNonNull(cacheConfig, "Cache config cannot be null");

    return WitInterfaceCache.getCachedInterface(this, cacheConfig);
  }

  /**
   * Optimizes this interface definition for performance.
   *
   * @param optimizationConfig the optimization configuration
   * @return the optimized interface definition
   * @throws WasmException if optimization fails
   */
  public WitInterfaceDefinition optimize(final WitInterfaceOptimizationConfig optimizationConfig)
      throws WasmException {
    Objects.requireNonNull(optimizationConfig, "Optimization config cannot be null");

    return WitInterfaceOptimizer.optimize(this, optimizationConfig);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final WitInterfaceDefinition that = (WitInterfaceDefinition) obj;
    return Objects.equals(interfaceName, that.interfaceName)
        && Objects.equals(version, that.version)
        && Objects.equals(functions, that.functions)
        && Objects.equals(types, that.types)
        && Objects.equals(resources, that.resources)
        && Objects.equals(constants, that.constants);
  }

  @Override
  public int hashCode() {
    return Objects.hash(interfaceName, version, functions, types, resources, constants);
  }

  @Override
  public String toString() {
    return String.format(
        "WitInterfaceDefinition{name='%s', version=%s, functions=%d, types=%d}",
        interfaceName, version, functions.size(), types.size());
  }

  /** Builder for WIT interface definitions. */
  public static final class Builder {
    private final String interfaceName;
    private final WitInterfaceVersion version;

    private Set<WitFunction> functions = Set.of();
    private Set<WitType> types = Set.of();
    private Set<WitResource> resources = Set.of();
    private Map<String, WitValue> constants = Map.of();
    private WitInterfaceMetadata metadata;
    private WitInterfaceSchema schema;
    private WitInterfaceDocumentation documentation;

    private Builder(final String interfaceName, final WitInterfaceVersion version) {
      this.interfaceName = Objects.requireNonNull(interfaceName, "Interface name cannot be null");
      this.version = Objects.requireNonNull(version, "Interface version cannot be null");
    }

    public Builder functions(final Set<WitFunction> functions) {
      this.functions = Objects.requireNonNull(functions, "Functions cannot be null");
      return this;
    }

    public Builder types(final Set<WitType> types) {
      this.types = Objects.requireNonNull(types, "Types cannot be null");
      return this;
    }

    public Builder resources(final Set<WitResource> resources) {
      this.resources = Objects.requireNonNull(resources, "Resources cannot be null");
      return this;
    }

    public Builder constants(final Map<String, WitValue> constants) {
      this.constants = Objects.requireNonNull(constants, "Constants cannot be null");
      return this;
    }

    public Builder metadata(final WitInterfaceMetadata metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder schema(final WitInterfaceSchema schema) {
      this.schema = schema;
      return this;
    }

    public Builder documentation(final WitInterfaceDocumentation documentation) {
      this.documentation = documentation;
      return this;
    }

    public WitInterfaceDefinition build() {
      return new WitInterfaceDefinition(this);
    }
  }
}
