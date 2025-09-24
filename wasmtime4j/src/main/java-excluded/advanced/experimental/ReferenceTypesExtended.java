/*
 * Copyright 2024 Tegmentum Technology, Inc.
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

package ai.tegmentum.wasmtime4j.experimental;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.Objects;

/**
 * Extended reference types support for WebAssembly.
 *
 * <p><strong>EXPERIMENTAL:</strong> This API is experimental and subject to change. It extends
 * WebAssembly reference types beyond the basic funcref and externref to support typed function
 * references and reference type subtyping.
 *
 * @since 1.0.0
 */
@ExperimentalApi(feature = ExperimentalFeatures.Feature.REFERENCE_TYPES_EXTENDED)
public final class ReferenceTypesExtended {

  /** Extended reference type definitions. */
  public enum ExtendedReferenceType {
    /** Typed function reference with specific signature. */
    TYPED_FUNCREF,

    /** Array reference type. */
    ARRAYREF,

    /** Struct reference type. */
    STRUCTREF,

    /** Interface reference type. */
    INTERFACEREF,

    /** Union reference type. */
    UNIONREF,

    /** Nullable reference wrapper. */
    NULLABLE_REF
  }

  /** Typed function reference. */
  public static final class TypedFunctionReference {
    private final FunctionType signature;
    private final long nativeHandle;
    private final boolean isNullable;

    /**
     * Creates a new typed function reference.
     *
     * @param signature the function signature
     * @param nativeHandle the native handle
     * @param isNullable true if the reference can be null
     * @throws IllegalArgumentException if signature is null
     */
    public TypedFunctionReference(
        final FunctionType signature, final long nativeHandle, final boolean isNullable) {
      if (signature == null) {
        throw new IllegalArgumentException("Function signature cannot be null");
      }

      this.signature = signature;
      this.nativeHandle = nativeHandle;
      this.isNullable = isNullable;
    }

    /**
     * Gets the function signature.
     *
     * @return the function signature
     */
    public FunctionType getSignature() {
      return signature;
    }

    /**
     * Gets the native handle for this function reference.
     *
     * @return the native handle
     */
    public long getNativeHandle() {
      return nativeHandle;
    }

    /**
     * Checks if this reference can be null.
     *
     * @return true if nullable, false otherwise
     */
    public boolean isNullable() {
      return isNullable;
    }

    /**
     * Checks if this reference is null.
     *
     * @return true if null, false otherwise
     */
    public boolean isNull() {
      return nativeHandle == 0;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final TypedFunctionReference that = (TypedFunctionReference) obj;
      return nativeHandle == that.nativeHandle
          && isNullable == that.isNullable
          && Objects.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
      return Objects.hash(signature, nativeHandle, isNullable);
    }

    @Override
    public String toString() {
      return "TypedFunctionReference{"
          + "signature="
          + signature
          + ", nativeHandle="
          + nativeHandle
          + ", isNullable="
          + isNullable
          + '}';
    }
  }

  /** Reference type subtyping validator. */
  public static final class ReferenceTypeValidator {
    private final long nativeHandle;

    /**
     * Creates a new reference type validator.
     *
     * @param nativeHandle the native handle
     */
    public ReferenceTypeValidator(final long nativeHandle) {
      this.nativeHandle = nativeHandle;
    }

    /**
     * Checks if one reference type is a subtype of another.
     *
     * @param subtype the potential subtype
     * @param supertype the potential supertype
     * @return true if subtype is assignable to supertype
     * @throws IllegalArgumentException if either type is null
     */
    public boolean isSubtype(
        final ExtendedReferenceType subtype, final ExtendedReferenceType supertype) {
      if (subtype == null) {
        throw new IllegalArgumentException("Subtype cannot be null");
      }
      if (supertype == null) {
        throw new IllegalArgumentException("Supertype cannot be null");
      }

      return checkSubtypeNative(nativeHandle, subtype, supertype);
    }

    /**
     * Validates reference type compatibility for assignment.
     *
     * @param sourceType the source reference type
     * @param targetType the target reference type
     * @return true if assignment is valid
     * @throws IllegalArgumentException if either type is null
     */
    public boolean isAssignmentValid(
        final ExtendedReferenceType sourceType, final ExtendedReferenceType targetType) {
      if (sourceType == null) {
        throw new IllegalArgumentException("Source type cannot be null");
      }
      if (targetType == null) {
        throw new IllegalArgumentException("Target type cannot be null");
      }

      return validateAssignmentNative(nativeHandle, sourceType, targetType);
    }

    // Native method declarations
    private static native boolean checkSubtypeNative(
        long validatorHandle, ExtendedReferenceType subtype, ExtendedReferenceType supertype);

    private static native boolean validateAssignmentNative(
        long validatorHandle, ExtendedReferenceType sourceType, ExtendedReferenceType targetType);
  }

  /** Reference type configuration. */
  public static final class ReferenceTypeConfig {
    private final boolean enableSubtyping;
    private final boolean validateNullability;
    private final boolean strictTypeChecking;
    private final int maxReferenceDepth;

    private ReferenceTypeConfig(final Builder builder) {
      this.enableSubtyping = builder.enableSubtyping;
      this.validateNullability = builder.validateNullability;
      this.strictTypeChecking = builder.strictTypeChecking;
      this.maxReferenceDepth = builder.maxReferenceDepth;
    }

    /**
     * Checks if reference type subtyping is enabled.
     *
     * @return true if subtyping is enabled
     */
    public boolean isSubtypingEnabled() {
      return enableSubtyping;
    }

    /**
     * Checks if nullability validation is enabled.
     *
     * @return true if nullability validation is enabled
     */
    public boolean isNullabilityValidationEnabled() {
      return validateNullability;
    }

    /**
     * Checks if strict type checking is enabled.
     *
     * @return true if strict type checking is enabled
     */
    public boolean isStrictTypeCheckingEnabled() {
      return strictTypeChecking;
    }

    /**
     * Gets the maximum reference depth allowed.
     *
     * @return the maximum reference depth
     */
    public int getMaxReferenceDepth() {
      return maxReferenceDepth;
    }

    /**
     * Creates a new builder for reference type configuration.
     *
     * @return a new builder
     */
    public static Builder builder() {
      return new Builder();
    }

    /** Builder for reference type configuration. */
    public static final class Builder {
      private boolean enableSubtyping = true;
      private boolean validateNullability = true;
      private boolean strictTypeChecking = true;
      private int maxReferenceDepth = 100;

      private Builder() {}

      /**
       * Enables or disables reference type subtyping.
       *
       * @param enable true to enable, false to disable
       * @return this builder
       */
      public Builder enableSubtyping(final boolean enable) {
        this.enableSubtyping = enable;
        return this;
      }

      /**
       * Enables or disables nullability validation.
       *
       * @param enable true to enable, false to disable
       * @return this builder
       */
      public Builder validateNullability(final boolean enable) {
        this.validateNullability = enable;
        return this;
      }

      /**
       * Enables or disables strict type checking.
       *
       * @param enable true to enable, false to disable
       * @return this builder
       */
      public Builder strictTypeChecking(final boolean enable) {
        this.strictTypeChecking = enable;
        return this;
      }

      /**
       * Sets the maximum reference depth.
       *
       * @param depth the maximum reference depth (must be positive)
       * @return this builder
       * @throws IllegalArgumentException if depth is not positive
       */
      public Builder maxReferenceDepth(final int depth) {
        if (depth <= 0) {
          throw new IllegalArgumentException("Max reference depth must be positive");
        }
        this.maxReferenceDepth = depth;
        return this;
      }

      /**
       * Builds the reference type configuration.
       *
       * @return the configuration
       */
      public ReferenceTypeConfig build() {
        return new ReferenceTypeConfig(this);
      }
    }
  }

  private final ReferenceTypeConfig config;
  private final long nativeHandle;
  private final ReferenceTypeValidator validator;

  /**
   * Creates a new extended reference types handler.
   *
   * @param config the reference type configuration
   * @throws IllegalArgumentException if config is null
   * @throws UnsupportedOperationException if extended reference types feature is not enabled
   */
  public ReferenceTypesExtended(final ReferenceTypeConfig config) {
    ExperimentalFeatures.validateFeatureSupport(
        ExperimentalFeatures.Feature.REFERENCE_TYPES_EXTENDED);

    if (config == null) {
      throw new IllegalArgumentException("Reference type config cannot be null");
    }

    this.config = config;
    this.nativeHandle = createNativeReferenceTypeHandler(config);
    this.validator = new ReferenceTypeValidator(nativeHandle);
  }

  /**
   * Creates a typed function reference.
   *
   * @param signature the function signature
   * @param isNullable true if the reference can be null
   * @return the typed function reference
   * @throws IllegalArgumentException if signature is null
   */
  public TypedFunctionReference createTypedFunctionReference(
      final FunctionType signature, final boolean isNullable) {
    if (signature == null) {
      throw new IllegalArgumentException("Function signature cannot be null");
    }

    final long refHandle = createTypedFunctionReferenceNative(nativeHandle, signature, isNullable);
    return new TypedFunctionReference(signature, refHandle, isNullable);
  }

  /**
   * Validates reference type assignment.
   *
   * @param sourceRef the source reference
   * @param targetType the target reference type
   * @return true if assignment is valid
   * @throws IllegalArgumentException if any parameter is null
   */
  public boolean validateReferenceAssignment(
      final WasmValue sourceRef, final ExtendedReferenceType targetType) {
    if (sourceRef == null) {
      throw new IllegalArgumentException("Source reference cannot be null");
    }
    if (targetType == null) {
      throw new IllegalArgumentException("Target type cannot be null");
    }

    return validateReferenceAssignmentNative(nativeHandle, sourceRef, targetType);
  }

  /**
   * Performs reference type casting with validation.
   *
   * @param reference the reference to cast
   * @param targetType the target reference type
   * @return the cast reference
   * @throws IllegalArgumentException if any parameter is null
   * @throws ClassCastException if cast is invalid
   */
  public WasmValue castReference(
      final WasmValue reference, final ExtendedReferenceType targetType) {
    if (reference == null) {
      throw new IllegalArgumentException("Reference cannot be null");
    }
    if (targetType == null) {
      throw new IllegalArgumentException("Target type cannot be null");
    }

    if (config.isStrictTypeCheckingEnabled()
        && !validateReferenceAssignment(reference, targetType)) {
      throw new ClassCastException("Cannot cast reference to " + targetType);
    }

    return castReferenceNative(nativeHandle, reference, targetType);
  }

  /**
   * Gets the reference type validator.
   *
   * @return the reference type validator
   */
  public ReferenceTypeValidator getValidator() {
    return validator;
  }

  /**
   * Gets the reference type configuration.
   *
   * @return the configuration
   */
  public ReferenceTypeConfig getConfig() {
    return config;
  }

  /**
   * Gets the native handle for this reference types handler.
   *
   * @return the native handle
   */
  public long getNativeHandle() {
    return nativeHandle;
  }

  /** Closes this reference types handler and releases native resources. */
  public void close() {
    if (nativeHandle != 0) {
      closeNativeReferenceTypeHandler(nativeHandle);
    }
  }

  // Native method declarations - implementations in wasmtime4j-native
  private static native long createNativeReferenceTypeHandler(ReferenceTypeConfig config);

  private static native long createTypedFunctionReferenceNative(
      long handlerHandle, FunctionType signature, boolean isNullable);

  private static native boolean validateReferenceAssignmentNative(
      long handlerHandle, WasmValue sourceRef, ExtendedReferenceType targetType);

  private static native WasmValue castReferenceNative(
      long handlerHandle, WasmValue reference, ExtendedReferenceType targetType);

  private static native void closeNativeReferenceTypeHandler(long handle);

  @Override
  protected void finalize() throws Throwable {
    try {
      close();
    } finally {
      super.finalize();
    }
  }
}
