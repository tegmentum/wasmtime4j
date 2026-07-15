//! Reproduces the wasi:io/streams@0.2.12 read-via-stream trap. Uses the `wasi` crate (which pins the
//! component's WASI imports to 0.2.12) and reads the granted file EXPLICITLY through
//! descriptor.read-via-stream -> input-stream.blocking-read — the exact 0.2.12 host capability the
//! shipped dylib fails to service. The 0.2.6 sibling (pure std::fs) returns the bytes; this traps.
wit_bindgen::generate!({ world: "file-stream-reader-newwasi", path: "wit" });

use wasi::filesystem::preopens::get_directories;
use wasi::filesystem::types::{DescriptorFlags, OpenFlags, PathFlags};
use wasi::io::streams::StreamError;

struct Component;

impl Guest for Component {
    fn read_stream(path: String) -> String {
        for (dir, mount) in get_directories() {
            let prefix = if mount.ends_with('/') { mount.clone() } else { format!("{mount}/") };
            let rel = match path.strip_prefix(&prefix) {
                Some(r) if !r.is_empty() => r.to_string(),
                _ => continue,
            };
            let fd = match dir.open_at(PathFlags::empty(), &rel, OpenFlags::empty(), DescriptorFlags::READ) {
                Ok(fd) => fd,
                Err(e) => return format!("ERR: open {path} -> {e:?}"),
            };
            // The 0.2.12 read-via-stream path: get a file-backed input stream, then blocking-read it.
            let stream = match fd.read_via_stream(0) {
                Ok(s) => s,
                Err(e) => return format!("ERR: read-via-stream {path} -> {e:?}"),
            };
            let mut out: Vec<u8> = Vec::new();
            loop {
                match stream.blocking_read(65536) {
                    Ok(chunk) if chunk.is_empty() => break,
                    Ok(chunk) => out.extend_from_slice(&chunk),
                    Err(StreamError::Closed) => break,
                    Err(e) => return format!("ERR: blocking-read {path} -> {e:?}"),
                }
            }
            return format!("OK:\n{}", String::from_utf8_lossy(&out));
        }
        format!("ERR: no preopen covers {path}")
    }
}

export!(Component);
