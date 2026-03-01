/*
 * Copyright 2025 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.coredump;

import ai.tegmentum.wasmtime4j.WasmValueType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a global variable snapshot in a WebAssembly core dump.
 *
 * @since 1.0.0
 */
public final class CoreDumpGlobal {

  private final int instanceIndex;
  private final int globalIndex;
  private final String name;
  private final WasmValueType valueType;
  private final boolean mutable;
  private final byte[] rawValue;

  private CoreDumpGlobal(final Builder builder) {
    this.instanceIndex = builder.instanceIndex;
    this.globalIndex = builder.globalIndex;
    this.name = builder.name;
    this.valueType = Objects.requireNonNull(builder.valueType, "Value type cannot be null");
    this.mutable = builder.mutable;
    this.rawValue = builder.rawValue != null ? builder.rawValue.clone() : new byte[0];
  }

  /**
   * Creates a new builder for constructing a CoreDumpGlobal.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  public int getInstanceIndex() {
    return instanceIndex;
  }

  public int getGlobalIndex() {
    return globalIndex;
  }

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public WasmValueType getValueType() {
    return valueType;
  }

  public boolean isMutable() {
    return mutable;
  }

  public byte[] getRawValue() {
    return rawValue.clone();
  }

  /**
   * Returns the global's value interpreted as a 32-bit integer.
   *
   * @return the i32 value
   * @throws IllegalStateException if the value type is not {@link WasmValueType#I32}
   */
  public int getI32Value() {
    if (valueType != WasmValueType.I32) {
      throw new IllegalStateException("Value type is not I32: " + valueType);
    }
    return ByteBuffer.wrap(rawValue).order(ByteOrder.LITTLE_ENDIAN).getInt();
  }

  /**
   * Returns the global's value interpreted as a 64-bit integer.
   *
   * @return the i64 value
   * @throws IllegalStateException if the value type is not {@link WasmValueType#I64}
   */
  public long getI64Value() {
    if (valueType != WasmValueType.I64) {
      throw new IllegalStateException("Value type is not I64: " + valueType);
    }
    return ByteBuffer.wrap(rawValue).order(ByteOrder.LITTLE_ENDIAN).getLong();
  }

  /**
   * Returns the global's value interpreted as a 32-bit float.
   *
   * @return the f32 value
   * @throws IllegalStateException if the value type is not {@link WasmValueType#F32}
   */
  public float getF32Value() {
    if (valueType != WasmValueType.F32) {
      throw new IllegalStateException("Value type is not F32: " + valueType);
    }
    return ByteBuffer.wrap(rawValue).order(ByteOrder.LITTLE_ENDIAN).getFloat();
  }

  /**
   * Returns the global's value interpreted as a 64-bit float.
   *
   * @return the f64 value
   * @throws IllegalStateException if the value type is not {@link WasmValueType#F64}
   */
  public double getF64Value() {
    if (valueType != WasmValueType.F64) {
      throw new IllegalStateException("Value type is not F64: " + valueType);
    }
    return ByteBuffer.wrap(rawValue).order(ByteOrder.LITTLE_ENDIAN).getDouble();
  }

  @Override
  public String toString() {
    return "CoreDumpGlobal{"
        + "instanceIndex="
        + instanceIndex
        + ", globalIndex="
        + globalIndex
        + ", name='"
        + name
        + '\''
        + ", valueType="
        + valueType
        + ", mutable="
        + mutable
        + '}';
  }

  /** Builder for constructing {@link CoreDumpGlobal} instances. */
  public static final class Builder {

    private int instanceIndex;
    private int globalIndex;
    private String name;
    private WasmValueType valueType;
    private boolean mutable;
    private byte[] rawValue;

    private Builder() {}

    public Builder instanceIndex(final int instanceIndex) {
      this.instanceIndex = instanceIndex;
      return this;
    }

    public Builder globalIndex(final int globalIndex) {
      this.globalIndex = globalIndex;
      return this;
    }

    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    public Builder valueType(final WasmValueType valueType) {
      this.valueType = valueType;
      return this;
    }

    public Builder mutable(final boolean mutable) {
      this.mutable = mutable;
      return this;
    }

    public Builder rawValue(final byte[] rawValue) {
      this.rawValue = rawValue != null ? rawValue.clone() : null;
      return this;
    }

    /**
     * Sets the global value as an i32.
     *
     * @param value the i32 value
     * @return this builder
     */
    public Builder i32Value(final int value) {
      this.valueType = WasmValueType.I32;
      this.rawValue = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
      return this;
    }

    /**
     * Sets the global value as an i64.
     *
     * @param value the i64 value
     * @return this builder
     */
    public Builder i64Value(final long value) {
      this.valueType = WasmValueType.I64;
      this.rawValue = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array();
      return this;
    }

    /**
     * Sets the global value as an f32.
     *
     * @param value the f32 value
     * @return this builder
     */
    public Builder f32Value(final float value) {
      this.valueType = WasmValueType.F32;
      this.rawValue = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array();
      return this;
    }

    /**
     * Sets the global value as an f64.
     *
     * @param value the f64 value
     * @return this builder
     */
    public Builder f64Value(final double value) {
      this.valueType = WasmValueType.F64;
      this.rawValue =
          ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(value).array();
      return this;
    }

    public CoreDumpGlobal build() {
      return new CoreDumpGlobal(this);
    }
  }
}
