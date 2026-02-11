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

package ai.tegmentum.wasmtime4j.panama.adapter;

import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.memory.Memory;
import java.nio.ByteBuffer;

/**
 * Adapter class that wraps a WasmMemory instance to implement the Memory interface.
 *
 * <p>This adapter bridges the gap between the comprehensive WasmMemory interface and the simpler
 * Memory interface used by the Caller interface. It handles type conversions and method signature
 * differences between the two interfaces.
 *
 * @since 1.0.0
 */
public final class WasmMemoryToMemoryAdapter implements Memory {

  private final WasmMemory delegate;

  /**
   * Creates a new adapter wrapping the given WasmMemory instance.
   *
   * @param delegate the WasmMemory instance to wrap
   * @throws IllegalArgumentException if delegate is null
   */
  public WasmMemoryToMemoryAdapter(final WasmMemory delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException("WasmMemory delegate cannot be null");
    }
    this.delegate = delegate;
  }

  @Override
  public long getSize() {
    return delegate.getSize();
  }

  @Override
  public long getSizeInBytes() {
    return delegate.getSize() * 65536L; // 64KB per page
  }

  @Override
  public long grow(final long deltaPages) throws WasmException {
    if (deltaPages > Integer.MAX_VALUE) {
      throw new WasmException("Cannot grow by more than " + Integer.MAX_VALUE + " pages");
    }
    final int result = delegate.grow((int) deltaPages);
    return result;
  }

  @Override
  public int read(final long offset, final ByteBuffer buffer) throws WasmException {
    if (buffer == null) {
      throw new IllegalArgumentException("Buffer cannot be null");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("Offset cannot be negative");
    }

    try {
      final int remaining = buffer.remaining();
      final byte[] dest = new byte[remaining];
      if (offset > Integer.MAX_VALUE) {
        delegate.readBytes64(offset, dest, 0, remaining);
      } else {
        delegate.readBytes((int) offset, dest, 0, remaining);
      }
      buffer.put(dest);
      return remaining;
    } catch (final IndexOutOfBoundsException e) {
      throw new WasmException("Read operation out of bounds: " + e.getMessage(), e);
    } catch (final RuntimeException e) {
      throw new WasmException("Read operation failed: " + e.getMessage(), e);
    }
  }

  @Override
  public int write(final long offset, final ByteBuffer buffer) throws WasmException {
    if (buffer == null) {
      throw new IllegalArgumentException("Buffer cannot be null");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("Offset cannot be negative");
    }

    try {
      final int remaining = buffer.remaining();
      final byte[] src = new byte[remaining];
      buffer.get(src);
      if (offset > Integer.MAX_VALUE) {
        delegate.writeBytes64(offset, src, 0, remaining);
      } else {
        delegate.writeBytes((int) offset, src, 0, remaining);
      }
      return remaining;
    } catch (final IndexOutOfBoundsException e) {
      throw new WasmException("Write operation out of bounds: " + e.getMessage(), e);
    } catch (final RuntimeException e) {
      throw new WasmException("Write operation failed: " + e.getMessage(), e);
    }
  }

  @Override
  public byte readByte(final long offset) throws WasmException {
    if (offset < 0) {
      throw new IllegalArgumentException("Offset cannot be negative");
    }

    try {
      if (offset > Integer.MAX_VALUE) {
        return delegate.readByte64(offset);
      } else {
        return delegate.readByte((int) offset);
      }
    } catch (final IndexOutOfBoundsException e) {
      throw new WasmException("Read byte out of bounds: " + e.getMessage(), e);
    } catch (final RuntimeException e) {
      throw new WasmException("Read byte failed: " + e.getMessage(), e);
    }
  }

  @Override
  public void writeByte(final long offset, final byte value) throws WasmException {
    if (offset < 0) {
      throw new IllegalArgumentException("Offset cannot be negative");
    }

    try {
      if (offset > Integer.MAX_VALUE) {
        delegate.writeByte64(offset, value);
      } else {
        delegate.writeByte((int) offset, value);
      }
    } catch (final IndexOutOfBoundsException e) {
      throw new WasmException("Write byte out of bounds: " + e.getMessage(), e);
    } catch (final RuntimeException e) {
      throw new WasmException("Write byte failed: " + e.getMessage(), e);
    }
  }

  @Override
  public int readInt32(final long offset) throws WasmException {
    if (offset < 0) {
      throw new IllegalArgumentException("Offset cannot be negative");
    }

    try {
      return delegate.readInt32(offset);
    } catch (final IndexOutOfBoundsException e) {
      throw new WasmException("Read int32 out of bounds: " + e.getMessage(), e);
    } catch (final RuntimeException e) {
      throw new WasmException("Read int32 failed: " + e.getMessage(), e);
    }
  }

  @Override
  public void writeInt32(final long offset, final int value) throws WasmException {
    if (offset < 0) {
      throw new IllegalArgumentException("Offset cannot be negative");
    }

    try {
      delegate.writeInt32(offset, value);
    } catch (final IndexOutOfBoundsException e) {
      throw new WasmException("Write int32 out of bounds: " + e.getMessage(), e);
    } catch (final RuntimeException e) {
      throw new WasmException("Write int32 failed: " + e.getMessage(), e);
    }
  }

  @Override
  public long readInt64(final long offset) throws WasmException {
    if (offset < 0) {
      throw new IllegalArgumentException("Offset cannot be negative");
    }

    try {
      return delegate.readInt64(offset);
    } catch (final IndexOutOfBoundsException e) {
      throw new WasmException("Read int64 out of bounds: " + e.getMessage(), e);
    } catch (final RuntimeException e) {
      throw new WasmException("Read int64 failed: " + e.getMessage(), e);
    }
  }

  @Override
  public void writeInt64(final long offset, final long value) throws WasmException {
    if (offset < 0) {
      throw new IllegalArgumentException("Offset cannot be negative");
    }

    try {
      delegate.writeInt64(offset, value);
    } catch (final IndexOutOfBoundsException e) {
      throw new WasmException("Write int64 out of bounds: " + e.getMessage(), e);
    } catch (final RuntimeException e) {
      throw new WasmException("Write int64 failed: " + e.getMessage(), e);
    }
  }

  @Override
  public long getMaxSize() {
    return delegate.getMaxSize();
  }

  @Override
  public boolean isValid() {
    // WasmMemory doesn't have a direct isValid method, so we try a safe operation
    try {
      delegate.getSize();
      return true;
    } catch (final Exception e) {
      return false;
    }
  }

  /**
   * Gets the underlying WasmMemory delegate.
   *
   * @return the delegate WasmMemory instance
   */
  public WasmMemory getDelegate() {
    return delegate;
  }
}
