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
package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown when a component model resource table operation fails.
 *
 * <p>Resource tables track component model resources (handles). Operations on the table can fail
 * when the table is full, a resource is not present, the wrong type is specified, or a resource
 * still has child references.
 *
 * @since 1.1.0
 */
public class ResourceTableException extends ResourceException {

  private static final long serialVersionUID = 1L;

  /** Kinds of resource table errors. */
  public enum ErrorKind {
    /** The resource table is full and cannot accept new entries. */
    FULL("Resource table is full"),
    /** The specified resource handle is not present in the table. */
    NOT_PRESENT("Resource not present in table"),
    /** The resource handle refers to a different type than expected. */
    WRONG_TYPE("Resource type mismatch"),
    /** The resource cannot be removed because it has outstanding child references. */
    HAS_CHILDREN("Resource has outstanding child references"),
    /** The child resource already has a parent. */
    HAS_PARENT("Child resource already has a parent");

    private final String description;

    ErrorKind(final String description) {
      this.description = description;
    }

    /**
     * Gets a human-readable description of this error kind.
     *
     * @return the error description
     */
    public String getDescription() {
      return description;
    }
  }

  private final ErrorKind errorKind;

  /**
   * Creates a new resource table exception with the specified kind and message.
   *
   * @param errorKind the kind of resource table error
   * @param message the error message
   */
  public ResourceTableException(final ErrorKind errorKind, final String message) {
    super(message);
    this.errorKind = errorKind;
  }

  /**
   * Creates a new resource table exception with the specified kind, message, and cause.
   *
   * @param errorKind the kind of resource table error
   * @param message the error message
   * @param cause the underlying cause
   */
  public ResourceTableException(
      final ErrorKind errorKind, final String message, final Throwable cause) {
    super(message, cause);
    this.errorKind = errorKind;
  }

  /**
   * Gets the kind of resource table error.
   *
   * @return the error kind
   */
  public ErrorKind getErrorKind() {
    return errorKind;
  }
}
