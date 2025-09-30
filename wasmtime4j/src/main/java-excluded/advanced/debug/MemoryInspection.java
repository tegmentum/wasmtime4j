package ai.tegmentum.wasmtime4j.debug;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * Represents the result of memory inspection during debugging.
 */
public final class MemoryInspection {
    private final long address;
    private final byte[] data;

    public MemoryInspection(final long address, final byte[] data) {
        this.address = address;
        this.data = Objects.requireNonNull(data, "Data cannot be null").clone();
    }

    public long getAddress() { return address; }
    public byte[] getData() { return data.clone(); }
    public int getLength() { return data.length; }

    /**
     * Gets data as hexadecimal string.
     */
    public String toHexString() {
        final StringBuilder sb = new StringBuilder();
        for (final byte b : data) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }

    /**
     * Reads an integer from the memory data at the given offset.
     */
    public int readInt32(final int offset) {
        if (offset + 4 > data.length) {
            throw new IndexOutOfBoundsException("Not enough data for int32 at offset " + offset);
        }
        return ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    /**
     * Reads a long from the memory data at the given offset.
     */
    public long readInt64(final int offset) {
        if (offset + 8 > data.length) {
            throw new IndexOutOfBoundsException("Not enough data for int64 at offset " + offset);
        }
        return ByteBuffer.wrap(data, offset, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    /**
     * Reads a float from the memory data at the given offset.
     */
    public float readFloat32(final int offset) {
        if (offset + 4 > data.length) {
            throw new IndexOutOfBoundsException("Not enough data for float32 at offset " + offset);
        }
        return ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    /**
     * Reads a double from the memory data at the given offset.
     */
    public double readFloat64(final int offset) {
        if (offset + 8 > data.length) {
            throw new IndexOutOfBoundsException("Not enough data for float64 at offset " + offset);
        }
        return ByteBuffer.wrap(data, offset, 8).order(ByteOrder.LITTLE_ENDIAN).getDouble();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof MemoryInspection)) return false;
        final MemoryInspection that = (MemoryInspection) o;
        return address == that.address && java.util.Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(address);
        result = 31 * result + java.util.Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        return String.format("MemoryInspection{address=0x%X, length=%d, data=%s}",
                           address, data.length, toHexString());
    }
}