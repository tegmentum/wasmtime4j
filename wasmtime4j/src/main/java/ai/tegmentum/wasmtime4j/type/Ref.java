package ai.tegmentum.wasmtime4j.type;

import ai.tegmentum.wasmtime4j.ContRef;
import ai.tegmentum.wasmtime4j.ExnRef;
import ai.tegmentum.wasmtime4j.ExternRef;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.gc.AnyRef;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a WebAssembly reference value.
 *
 * <p>This is the Java equivalent of Wasmtime's {@code Ref} enum, providing a unified tagged union
 * for all WebAssembly reference types: function references, extern references, any references,
 * exception references, and continuation references.
 *
 * <p>A {@code Ref} can be either non-null (carrying an actual reference) or null (representing the
 * null value for that reference kind). Use {@link #isNull()} and {@link #isNonNull()} to check, and
 * the type-specific extractors ({@link #asFunc()}, {@link #asExtern()}, etc.) to retrieve the
 * underlying value.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Ref funcRef = Ref.fromFunc(wasmFunction);
 * if (funcRef.isFunc() && funcRef.isNonNull()) {
 *     WasmFunction func = funcRef.asFunc().orElseThrow();
 *     func.call();
 * }
 *
 * Ref nullRef = Ref.nullExternRef();
 * assert nullRef.isNull();
 * assert nullRef.isExtern();
 * }</pre>
 *
 * @since 1.1.0
 */
public final class Ref {

  /**
   * The kind of reference this {@code Ref} holds.
   *
   * @since 1.1.0
   */
  public enum Kind {
    /** A function reference ({@code funcref}). */
    FUNC,
    /** An external reference ({@code externref}). */
    EXTERN,
    /** An any reference ({@code anyref}), part of the GC proposal. */
    ANY,
    /** An exception reference ({@code exnref}), part of the exception handling proposal. */
    EXN,
    /** A continuation reference ({@code contref}), part of the stack switching proposal. */
    CONT
  }

  private final Kind kind;
  private final Object value;

  private Ref(final Kind kind, final Object value) {
    this.kind = Objects.requireNonNull(kind, "kind cannot be null");
    this.value = value;
  }

  // --- Non-null reference factories ---

  /**
   * Creates a non-null function reference.
   *
   * @param func the function
   * @return a non-null func ref
   * @throws IllegalArgumentException if func is null
   */
  public static Ref fromFunc(final WasmFunction func) {
    if (func == null) {
      throw new IllegalArgumentException("func cannot be null");
    }
    return new Ref(Kind.FUNC, func);
  }

  /**
   * Creates a non-null extern reference.
   *
   * @param ref the extern reference
   * @return a non-null extern ref
   * @throws IllegalArgumentException if ref is null
   */
  public static Ref fromExtern(final ExternRef<?> ref) {
    if (ref == null) {
      throw new IllegalArgumentException("ref cannot be null");
    }
    return new Ref(Kind.EXTERN, ref);
  }

  /**
   * Creates a non-null any reference.
   *
   * @param ref the any reference
   * @return a non-null any ref
   * @throws IllegalArgumentException if ref is null
   */
  public static Ref fromAny(final AnyRef ref) {
    if (ref == null) {
      throw new IllegalArgumentException("ref cannot be null");
    }
    return new Ref(Kind.ANY, ref);
  }

  /**
   * Creates a non-null exception reference.
   *
   * @param ref the exception reference
   * @return a non-null exn ref
   * @throws IllegalArgumentException if ref is null
   */
  public static Ref fromExn(final ExnRef ref) {
    if (ref == null) {
      throw new IllegalArgumentException("ref cannot be null");
    }
    return new Ref(Kind.EXN, ref);
  }

  /**
   * Creates a non-null continuation reference.
   *
   * @param ref the continuation reference
   * @return a non-null cont ref
   * @throws IllegalArgumentException if ref is null
   */
  public static Ref fromCont(final ContRef ref) {
    if (ref == null) {
      throw new IllegalArgumentException("ref cannot be null");
    }
    return new Ref(Kind.CONT, ref);
  }

  // --- Null reference factories ---

  /**
   * Creates a null function reference.
   *
   * @return a null funcref
   */
  public static Ref nullFuncRef() {
    return new Ref(Kind.FUNC, null);
  }

  /**
   * Creates a null extern reference.
   *
   * @return a null externref
   */
  public static Ref nullExternRef() {
    return new Ref(Kind.EXTERN, null);
  }

  /**
   * Creates a null any reference.
   *
   * @return a null anyref
   */
  public static Ref nullAnyRef() {
    return new Ref(Kind.ANY, null);
  }

  /**
   * Creates a null exception reference.
   *
   * @return a null exnref
   */
  public static Ref nullExnRef() {
    return new Ref(Kind.EXN, null);
  }

  /**
   * Creates a null continuation reference.
   *
   * @return a null contref
   */
  public static Ref nullContRef() {
    return new Ref(Kind.CONT, null);
  }

  // --- Kind and nullability queries ---

  /**
   * Gets the kind of this reference.
   *
   * @return the reference kind
   */
  public Kind getKind() {
    return kind;
  }

  /**
   * Checks if this reference is null.
   *
   * @return true if this is a null reference
   */
  public boolean isNull() {
    return value == null;
  }

  /**
   * Checks if this reference is non-null.
   *
   * @return true if this reference carries a value
   */
  public boolean isNonNull() {
    return value != null;
  }

  // --- Type checks ---

  /**
   * Checks if this is a function reference.
   *
   * @return true if this is a funcref (null or non-null)
   */
  public boolean isFunc() {
    return kind == Kind.FUNC;
  }

  /**
   * Checks if this is an extern reference.
   *
   * @return true if this is an externref (null or non-null)
   */
  public boolean isExtern() {
    return kind == Kind.EXTERN;
  }

  /**
   * Checks if this is an any reference.
   *
   * @return true if this is an anyref (null or non-null)
   */
  public boolean isAny() {
    return kind == Kind.ANY;
  }

  /**
   * Checks if this is an exception reference.
   *
   * @return true if this is an exnref (null or non-null)
   */
  public boolean isExn() {
    return kind == Kind.EXN;
  }

  /**
   * Checks if this is a continuation reference.
   *
   * @return true if this is a contref (null or non-null)
   */
  public boolean isCont() {
    return kind == Kind.CONT;
  }

  // --- Extractors ---

  /**
   * Extracts the function if this is a non-null function reference.
   *
   * @return the function, or empty if this is null or not a funcref
   */
  public Optional<WasmFunction> asFunc() {
    if (kind == Kind.FUNC && value instanceof WasmFunction) {
      return Optional.of((WasmFunction) value);
    }
    return Optional.empty();
  }

  /**
   * Extracts the extern reference if this is a non-null extern reference.
   *
   * @return the extern reference, or empty if this is null or not an externref
   */
  @SuppressWarnings("unchecked")
  public Optional<ExternRef<?>> asExtern() {
    if (kind == Kind.EXTERN && value instanceof ExternRef) {
      return Optional.of((ExternRef<?>) value);
    }
    return Optional.empty();
  }

  /**
   * Extracts the any reference if this is a non-null any reference.
   *
   * @return the any reference, or empty if this is null or not an anyref
   */
  public Optional<AnyRef> asAny() {
    if (kind == Kind.ANY && value instanceof AnyRef) {
      return Optional.of((AnyRef) value);
    }
    return Optional.empty();
  }

  /**
   * Extracts the exception reference if this is a non-null exception reference.
   *
   * @return the exception reference, or empty if this is null or not an exnref
   */
  public Optional<ExnRef> asExn() {
    if (kind == Kind.EXN && value instanceof ExnRef) {
      return Optional.of((ExnRef) value);
    }
    return Optional.empty();
  }

  /**
   * Extracts the continuation reference if this is a non-null continuation reference.
   *
   * @return the continuation reference, or empty if this is null or not a contref
   */
  public Optional<ContRef> asCont() {
    if (kind == Kind.CONT && value instanceof ContRef) {
      return Optional.of((ContRef) value);
    }
    return Optional.empty();
  }

  // --- Type operations ---

  /**
   * Gets the {@link RefType} for this reference.
   *
   * <p>For non-null references, returns a non-nullable ref type. For null references, returns a
   * nullable ref type. The heap type is derived from the reference {@link Kind}.
   *
   * <p>This method does not require a store for abstract reference types. When concrete GC types
   * are involved, a store-aware variant may be needed.
   *
   * @return the reference type
   */
  public RefType ty() {
    final boolean nullable = value == null;
    final HeapType heapType;
    switch (kind) {
      case FUNC:
        heapType = HeapType.FUNC;
        break;
      case EXTERN:
        heapType = HeapType.EXTERN;
        break;
      case ANY:
        heapType = HeapType.ANY;
        break;
      case EXN:
        heapType = HeapType.EXN;
        break;
      case CONT:
        heapType = HeapType.CONT;
        break;
      default:
        heapType = HeapType.FUNC;
        break;
    }
    return nullable ? RefType.of(true, heapType) : RefType.nonNull(heapType);
  }

  /**
   * Gets the {@link RefType} for this reference within a store context.
   *
   * <p>This matches Wasmtime's {@code Ref::ty(&self, store)} signature. For abstract reference
   * types, the store is not needed and this delegates to {@link #ty()}. Future implementations with
   * concrete GC types may use the store for type resolution.
   *
   * @param store the store context
   * @return the reference type
   * @throws IllegalArgumentException if store is null
   */
  public RefType ty(final Store store) {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    return ty();
  }

  /**
   * Checks if this reference matches the given type.
   *
   * @param store the store context
   * @param expected the expected reference type
   * @return true if this reference is compatible with the expected type
   * @throws IllegalArgumentException if store or expected is null
   */
  public boolean matchesTy(final Store store, final RefType expected) {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    if (expected == null) {
      throw new IllegalArgumentException("expected cannot be null");
    }
    return ty().matches(expected);
  }

  /**
   * Gets the raw underlying value held by this reference, if any.
   *
   * @return the underlying value, or empty if this is a null reference
   */
  public Optional<Object> getRawValue() {
    return Optional.ofNullable(value);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Ref)) {
      return false;
    }
    final Ref other = (Ref) obj;
    return kind == other.kind && Objects.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, value);
  }

  @Override
  public String toString() {
    if (value == null) {
      return "Ref{kind=" + kind + ", null}";
    }
    return "Ref{kind=" + kind + ", value=" + value + "}";
  }
}
