/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

import java.util.List;
import java.util.Objects;

/**
 * Represents a single API endpoint with its documentation and metadata.
 *
 * <p>An API endpoint encompasses methods, constructors, and fields that form the public interface
 * of the wasmtime4j library.
 *
 * @since 1.0.0
 */
public final class ApiEndpoint {

  private final String fullyQualifiedName;
  private final String className;
  private final String methodName;
  private final List<String> parameterTypes;
  private final String returnType;
  private final boolean isDocumented;
  private final DocumentationQuality quality;
  private final List<String> missingDocumentation;

  /**
   * Creates a new API endpoint representation.
   *
   * @param fullyQualifiedName the complete method signature including class and parameters
   * @param className the name of the containing class or interface
   * @param methodName the name of the method or constructor
   * @param parameterTypes list of parameter type names
   * @param returnType the return type name
   * @param isDocumented whether the endpoint has complete documentation
   * @param quality the quality assessment of existing documentation
   * @param missingDocumentation list of missing documentation elements
   */
  public ApiEndpoint(
      final String fullyQualifiedName,
      final String className,
      final String methodName,
      final List<String> parameterTypes,
      final String returnType,
      final boolean isDocumented,
      final DocumentationQuality quality,
      final List<String> missingDocumentation) {
    this.fullyQualifiedName = Objects.requireNonNull(fullyQualifiedName, "fullyQualifiedName");
    this.className = Objects.requireNonNull(className, "className");
    this.methodName = Objects.requireNonNull(methodName, "methodName");
    this.parameterTypes = List.copyOf(Objects.requireNonNull(parameterTypes, "parameterTypes"));
    this.returnType = Objects.requireNonNull(returnType, "returnType");
    this.isDocumented = isDocumented;
    this.quality = Objects.requireNonNull(quality, "quality");
    this.missingDocumentation =
        List.copyOf(Objects.requireNonNull(missingDocumentation, "missingDocumentation"));
  }

  /**
   * Returns the fully qualified method signature.
   *
   * @return complete method signature including class and parameters
   */
  public String getFullyQualifiedName() {
    return fullyQualifiedName;
  }

  /**
   * Returns the name of the containing class or interface.
   *
   * @return class or interface name
   */
  public String getClassName() {
    return className;
  }

  /**
   * Returns the method or constructor name.
   *
   * @return method or constructor name
   */
  public String getMethodName() {
    return methodName;
  }

  /**
   * Returns the list of parameter type names.
   *
   * @return immutable list of parameter types
   */
  public List<String> getParameterTypes() {
    return parameterTypes;
  }

  /**
   * Returns the return type name.
   *
   * @return return type name
   */
  public String getReturnType() {
    return returnType;
  }

  /**
   * Checks if the endpoint has complete documentation.
   *
   * @return {@code true} if fully documented, {@code false} otherwise
   */
  public boolean isDocumented() {
    return isDocumented;
  }

  /**
   * Returns the quality assessment of existing documentation.
   *
   * @return documentation quality evaluation
   */
  public DocumentationQuality getQuality() {
    return quality;
  }

  /**
   * Returns list of missing documentation elements.
   *
   * @return immutable list of missing documentation components
   */
  public List<String> getMissingDocumentation() {
    return missingDocumentation;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ApiEndpoint that = (ApiEndpoint) obj;
    return Objects.equals(fullyQualifiedName, that.fullyQualifiedName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fullyQualifiedName);
  }

  @Override
  public String toString() {
    return "ApiEndpoint{"
        + "fullyQualifiedName='"
        + fullyQualifiedName
        + '\''
        + ", documented="
        + isDocumented
        + ", quality="
        + quality
        + '}';
  }
}
