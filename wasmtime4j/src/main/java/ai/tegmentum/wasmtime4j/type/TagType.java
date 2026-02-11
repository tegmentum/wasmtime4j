package ai.tegmentum.wasmtime4j.type;

import ai.tegmentum.wasmtime4j.DefaultTagType;
import ai.tegmentum.wasmtime4j.WasmValueType;

/**
 * Represents the type of a WebAssembly exception tag.
 *
 * <p>TagType wraps a {@link FunctionType} to describe the signature of exception payloads. The
 * parameters of the function type represent the types of values that can be attached to an
 * exception when it is thrown.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create a tag type that carries an i32 error code
 * FunctionType funcType = FunctionType.create(
 *     List.of(WasmValueType.I32),  // parameters (exception payload types)
 *     List.of()                     // no returns
 * );
 * TagType tagType = TagType.create(funcType);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface TagType {

  /**
   * Creates a new tag type from a function type.
   *
   * <p>The function type's parameters define the types of values that can be attached to exceptions
   * of this tag type. The function type's return values are ignored for tag types.
   *
   * @param funcType the function type describing the exception payload
   * @return a new TagType instance
   * @throws IllegalArgumentException if funcType is null
   */
  static TagType create(final FunctionType funcType) {
    return new DefaultTagType(funcType);
  }

  /**
   * Returns the underlying function type of this tag type.
   *
   * <p>The function type's parameters represent the payload types for exceptions thrown with this
   * tag. The return types are not used.
   *
   * @return the FunctionType describing this tag's signature
   */
  FunctionType getFunctionType();
}
