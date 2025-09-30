package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown during WebAssembly exception handling operations.
 *
 * @since 1.0.0
 */
public class WasmExceptionHandlingException extends WasmtimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Creates a new exception with the specified message.
   *
   * @param message the exception message
   */
  public WasmExceptionHandlingException(final String message) {
    super(message);
  }

  /**
   * Creates a new exception with the specified message and cause.
   *
   * @param message the exception message
   * @param cause the underlying cause
   */
  public WasmExceptionHandlingException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new exception with the specified cause.
   *
   * @param cause the underlying cause
   */
  public WasmExceptionHandlingException(final Throwable cause) {
    super(cause.getMessage(), cause);
  }

  /**
   * WebAssembly exception tag.
   *
   * @since 1.0.0
   */
  public static final class ExceptionTag {
    private final String tagName;
    private final int tagId;
    private final String signature;

    /**
     * Creates a new exception tag.
     *
     * @param tagName the tag name
     * @param tagId the tag ID
     * @param signature the tag signature
     */
    public ExceptionTag(final String tagName, final int tagId, final String signature) {
      this.tagName = tagName;
      this.tagId = tagId;
      this.signature = signature;
    }

    /**
     * Gets the tag name.
     *
     * @return the tag name
     */
    public String getTagName() {
      return tagName;
    }

    /**
     * Gets the tag ID.
     *
     * @return the tag ID
     */
    public int getTagId() {
      return tagId;
    }

    /**
     * Gets the tag signature.
     *
     * @return the tag signature
     */
    public String getSignature() {
      return signature;
    }
  }

  /**
   * WebAssembly exception payload.
   *
   * @since 1.0.0
   */
  public static final class ExceptionPayload {
    private final byte[] payloadData;
    private final String payloadType;

    /**
     * Creates a new exception payload.
     *
     * @param payloadData the payload data
     * @param payloadType the payload type
     */
    public ExceptionPayload(final byte[] payloadData, final String payloadType) {
      this.payloadData = payloadData != null ? payloadData.clone() : new byte[0];
      this.payloadType = payloadType;
    }

    /**
     * Gets the payload data.
     *
     * @return the payload data as byte array
     */
    public byte[] getPayloadData() {
      return payloadData.clone();
    }

    /**
     * Gets the payload size in bytes.
     *
     * @return the payload size
     */
    public int getPayloadSize() {
      return payloadData.length;
    }

    /**
     * Gets the payload type.
     *
     * @return the payload type
     */
    public String getPayloadType() {
      return payloadType;
    }
  }
}
