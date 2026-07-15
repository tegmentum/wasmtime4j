//! Svalinn file-stream-reader witness.
//!
//! Reads a granted file through Rust std's `std::fs::read`, which on wasm32-wasip2
//! is backed by `wasi:filesystem` `read-via-stream` + `wasi:io/streams`
//! `blocking-read` (a FILE-BACKED INPUT STREAM). This is the exact byte-transfer
//! mechanism that traps on the wasmtime4j-native COMPONENT instantiation path,
//! while the low-level positioned `descriptor.read` (used by the fsread witness)
//! works. The guest reports a clean `ERR:` on any Rust error; a host TRAP produces
//! no return at all, which is what we assert against.

wit_bindgen::generate!({
    world: "file-stream-reader",
    path: "wit",
});

struct Component;

impl Guest for Component {
    fn read_stream(path: String) -> String {
        // std::fs::read opens the file and reads it to EOF via read-via-stream /
        // wasi:io/streams blocking-read. This is the stream path under test.
        match std::fs::read(&path) {
            Ok(bytes) => format!("OK:\n{}", String::from_utf8_lossy(&bytes)),
            Err(e) => format!("ERR: read {path} -> {e}"),
        }
    }
}

export!(Component);
