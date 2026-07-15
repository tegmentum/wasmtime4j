#!/usr/bin/env bash
# Build the file-stream-reader witness component to a Component-Model .wasm.
# Requires: rustup target wasm32-wasip2.
#
# The guest reads a granted file with std::fs::read, which on wasm32-wasip2 is backed by
# wasi:filesystem read-via-stream + wasi:io/streams blocking-read (a FILE-BACKED INPUT stream).
# This is the byte-transfer path exercised by ComponentFileInputStreamReadSmokeTest.
set -euo pipefail
cd "$(dirname "$0")"
cargo build --release --target wasm32-wasip2
cp target/wasm32-wasip2/release/svalinn_file_stream_reader.wasm ../file-stream-reader.component.wasm
echo "built: ../file-stream-reader.component.wasm"
