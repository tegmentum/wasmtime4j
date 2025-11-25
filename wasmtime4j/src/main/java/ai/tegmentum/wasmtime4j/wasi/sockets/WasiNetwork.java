package ai.tegmentum.wasmtime4j.wasi.sockets;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Network resource representing an opaque handle to a network instance.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/network@0.2.0
 *
 * <p>This resource is used to group sockets into a "network" and is a prerequisite
 * for socket creation.
 */
public interface WasiNetwork {
    /**
     * Close and dispose of this network resource.
     *
     * @throws WasmException if closing fails
     */
    void close() throws WasmException;
}
