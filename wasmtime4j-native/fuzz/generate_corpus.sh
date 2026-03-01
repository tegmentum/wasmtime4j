#!/bin/bash
# Generate seed corpus for fuzzing from existing test resources
#
# This script creates initial corpus files for each fuzz target by:
# 1. Copying existing WASM test files
# 2. Generating minimal valid WASM modules
# 3. Creating valid value serialization samples

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CORPUS_DIR="$SCRIPT_DIR/corpus"

echo "Generating fuzz corpus in $CORPUS_DIR"

# Create corpus directories
mkdir -p "$CORPUS_DIR/module_parse"
mkdir -p "$CORPUS_DIR/wit_deserialize"
mkdir -p "$CORPUS_DIR/memory_access"
mkdir -p "$CORPUS_DIR/func_call"
mkdir -p "$CORPUS_DIR/module_serialize"
mkdir -p "$CORPUS_DIR/error_message"

# =============================================================================
# module_parse corpus
# =============================================================================
echo "Generating module_parse corpus..."

# Copy existing WASM files from test resources
WASM_SOURCES=(
    "../../../wasmtime4j-tests/src/test/resources"
    "../../../wasmtime4j-jni/src/test/resources"
    "../../../wasmtime4j-panama/src/test/resources"
)

for source_dir in "${WASM_SOURCES[@]}"; do
    if [ -d "$SCRIPT_DIR/$source_dir" ]; then
        find "$SCRIPT_DIR/$source_dir" -name "*.wasm" -exec cp {} "$CORPUS_DIR/module_parse/" \; 2>/dev/null || true
        find "$SCRIPT_DIR/$source_dir" -name "*.wat" -exec cp {} "$CORPUS_DIR/module_parse/" \; 2>/dev/null || true
    fi
done

# Generate minimal valid WASM modules
# Empty module
printf '\x00asm\x01\x00\x00\x00' > "$CORPUS_DIR/module_parse/empty.wasm"

# Module with empty type section
printf '\x00asm\x01\x00\x00\x00\x01\x01\x00' > "$CORPUS_DIR/module_parse/empty_type_section.wasm"

# Module with memory
printf '\x00asm\x01\x00\x00\x00\x05\x03\x01\x00\x01' > "$CORPUS_DIR/module_parse/with_memory.wasm"

# Module with function (minimal)
printf '\x00asm\x01\x00\x00\x00\x01\x04\x01\x60\x00\x00\x03\x02\x01\x00\x0a\x04\x01\x02\x00\x0b' > "$CORPUS_DIR/module_parse/with_func.wasm"

# WAT samples
cat > "$CORPUS_DIR/module_parse/simple.wat" << 'EOF'
(module)
EOF

cat > "$CORPUS_DIR/module_parse/with_func.wat" << 'EOF'
(module
  (func $add (param i32 i32) (result i32)
    local.get 0
    local.get 1
    i32.add))
EOF

cat > "$CORPUS_DIR/module_parse/with_memory.wat" << 'EOF'
(module
  (memory 1 10)
  (export "memory" (memory 0)))
EOF

cat > "$CORPUS_DIR/module_parse/with_table.wat" << 'EOF'
(module
  (table 10 funcref)
  (export "table" (table 0)))
EOF

cat > "$CORPUS_DIR/module_parse/with_global.wat" << 'EOF'
(module
  (global $g (mut i32) (i32.const 0))
  (export "g" (global $g)))
EOF

# =============================================================================
# wit_deserialize corpus
# =============================================================================
echo "Generating wit_deserialize corpus..."

# Create sample serialized value files
# Format: [count: u32 LE][type_tag: u8][value bytes]

# Single i32 value (42)
printf '\x01\x00\x00\x00\x01\x2a\x00\x00\x00' > "$CORPUS_DIR/wit_deserialize/i32_single.bin"

# Single i64 value (100)
printf '\x01\x00\x00\x00\x02\x64\x00\x00\x00\x00\x00\x00\x00' > "$CORPUS_DIR/wit_deserialize/i64_single.bin"

# Single f32 value (3.14)
printf '\x01\x00\x00\x00\x03\xc3\xf5\x48\x40' > "$CORPUS_DIR/wit_deserialize/f32_single.bin"

# Single f64 value (3.14159)
printf '\x01\x00\x00\x00\x04\x6e\x86\x1b\xf0\xf9\x21\x09\x40' > "$CORPUS_DIR/wit_deserialize/f64_single.bin"

# Multiple values
printf '\x03\x00\x00\x00\x01\x01\x00\x00\x00\x01\x02\x00\x00\x00\x01\x03\x00\x00\x00' > "$CORPUS_DIR/wit_deserialize/multiple_i32.bin"

# Empty (0 values)
printf '\x00\x00\x00\x00' > "$CORPUS_DIR/wit_deserialize/empty.bin"

# FuncRef null
printf '\x01\x00\x00\x00\x06\x00' > "$CORPUS_DIR/wit_deserialize/funcref_null.bin"

# ExternRef null
printf '\x01\x00\x00\x00\x07\x00' > "$CORPUS_DIR/wit_deserialize/externref_null.bin"

# Invalid type tag (for testing error handling)
printf '\x01\x00\x00\x00\xff\x00\x00\x00\x00' > "$CORPUS_DIR/wit_deserialize/invalid_type.bin"

# Truncated data
printf '\x01\x00\x00\x00\x01' > "$CORPUS_DIR/wit_deserialize/truncated.bin"

# =============================================================================
# memory_access corpus
# =============================================================================
echo "Generating memory_access corpus..."

# These are arbitrary-derived struct inputs
# Format depends on the Arbitrary derive but we provide some reasonable defaults

# Small offset, small length, read operation
printf '\x00\x00\x00\x00\x00\x00\x00\x00\x10\x00\x00\x00\x00\x01' > "$CORPUS_DIR/memory_access/small_read.bin"

# Zero offset, zero length
printf '\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x01' > "$CORPUS_DIR/memory_access/zero.bin"

# Large offset (boundary testing)
printf '\xff\xff\xff\xff\x00\x00\x00\x00\x10\x00\x00\x00\x00\x01' > "$CORPUS_DIR/memory_access/large_offset.bin"

# Write operation with data
printf '\x00\x00\x00\x00\x00\x00\x00\x00\x04\x00\x00\x00\x01\x01\x41\x42\x43\x44' > "$CORPUS_DIR/memory_access/write.bin"

# =============================================================================
# func_call corpus
# =============================================================================
echo "Generating func_call corpus..."

# Function 0 (identity_i32), 1 i32 arg
printf '\x00\x01\x00\x00\x00\x2a\x00\x00\x00\x01\x00\x00\x00' > "$CORPUS_DIR/func_call/identity_i32.bin"

# Function 2 (add), 2 i32 args
printf '\x02\x08\x00\x00\x00\x05\x00\x00\x00\x03\x00\x00\x00\x02\x00\x00\x00' > "$CORPUS_DIR/func_call/add.bin"

# Function 3 (const), no args
printf '\x03\x00\x00\x00\x00\x00' > "$CORPUS_DIR/func_call/const.bin"

# Type mismatch - i64 to i32 function
printf '\x00\x01\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x01\x00\x00\x00' > "$CORPUS_DIR/func_call/type_mismatch.bin"

# =============================================================================
# module_serialize corpus
# =============================================================================
echo "Generating module_serialize corpus..."

# module_serialize uses Arbitrary-derived struct input. The fuzzer compiles its own
# module from VALID_MODULE_WAT and serializes it internally. libfuzzer generates
# the Arbitrary struct automatically, so no binary seeds are strictly needed.
# We provide a few minimal seeds to give the fuzzer starting points for each operation.

# Operation 0: bit flip (mutation_offset=0, mutation_byte=0xFF, truncate_len=0, operation=0)
printf '\x00\x00\xff\x00\x00\x00\x00' > "$CORPUS_DIR/module_serialize/bit_flip.bin"

# Operation 1: truncate (mutation_offset=0, mutation_byte=0, truncate_len=16, operation=1)
printf '\x00\x00\x00\x10\x00\x01\x00' > "$CORPUS_DIR/module_serialize/truncate.bin"

# Operation 2: append garbage (mutation_offset=0, mutation_byte=0, truncate_len=0, operation=2)
printf '\x00\x00\x00\x00\x00\x02\xde\xad\xbe\xef' > "$CORPUS_DIR/module_serialize/append.bin"

# Operation 3: zero out range (mutation_offset=0, mutation_byte=8, truncate_len=0, operation=3)
printf '\x00\x00\x08\x00\x00\x03\x00' > "$CORPUS_DIR/module_serialize/zero_range.bin"

# =============================================================================
# error_message corpus
# =============================================================================
echo "Generating error_message corpus..."

# error_message uses Arbitrary-derived struct with message_bytes, error_category,
# include_context, and context_bytes. We provide seeds with varying categories.

# Category 0 (Compilation), short message, no context
printf '\x05hello\x00\x00\x00' > "$CORPUS_DIR/error_message/compilation.bin"

# Category 3 (Runtime with backtrace), include context
printf '\x07runtime\x03\x01\x03ctx' > "$CORPUS_DIR/error_message/runtime.bin"

# Category 26 (TypeMismatch), with expected/actual strings
printf '\x08expected\x1a\x01\x06actual' > "$CORPUS_DIR/error_message/type_mismatch.bin"

# Empty message (edge case)
printf '\x00\x00\x00\x00' > "$CORPUS_DIR/error_message/empty.bin"

# Large category value (tests modulo behavior)
printf '\x03abc\xff\x00\x00' > "$CORPUS_DIR/error_message/large_category.bin"

# =============================================================================
# Summary
# =============================================================================
echo ""
echo "Corpus generation complete!"
echo "  module_parse:      $(ls -1 "$CORPUS_DIR/module_parse" 2>/dev/null | wc -l) files"
echo "  wit_deserialize:   $(ls -1 "$CORPUS_DIR/wit_deserialize" 2>/dev/null | wc -l) files"
echo "  memory_access:     $(ls -1 "$CORPUS_DIR/memory_access" 2>/dev/null | wc -l) files"
echo "  func_call:         $(ls -1 "$CORPUS_DIR/func_call" 2>/dev/null | wc -l) files"
echo "  module_serialize:  $(ls -1 "$CORPUS_DIR/module_serialize" 2>/dev/null | wc -l) files"
echo "  error_message:     $(ls -1 "$CORPUS_DIR/error_message" 2>/dev/null | wc -l) files"
