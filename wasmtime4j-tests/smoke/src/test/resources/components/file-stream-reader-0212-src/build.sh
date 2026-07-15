#!/usr/bin/env bash
# Build the 0.2.12 read-via-stream reproducer to a Component-Model .wasm.
# The `wasi` crate pins the component's WASI imports to 0.2.12; the guest reads via
# descriptor.read-via-stream + input-stream.blocking-read (wasi:io/streams@0.2.12).
set -euo pipefail
cd "$(dirname "$0")"
cargo build --release --target wasm32-wasip2
cp target/wasm32-wasip2/release/svalinn_file_stream_reader_0212.wasm ../file-stream-reader-0212.component.wasm
echo "built: ../file-stream-reader-0212.component.wasm"
