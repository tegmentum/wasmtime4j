# WebAssembly Test Files

This directory contains WebAssembly (.wasm) files used for testing Wasmtime4j functionality.

## Test File Categories

### Basic Modules
- `simple.wasm` - Simple module with basic arithmetic functions
- `hello.wasm` - Module that demonstrates string/memory operations
- `fibonacci.wasm` - Recursive Fibonacci implementation

### Import/Export Tests
- `memory-import.wasm` - Module that imports memory
- `function-import.wasm` - Module that imports functions
- `multi-export.wasm` - Module with multiple exports

### Advanced Features
- `wasi-example.wasm` - WASI (WebAssembly System Interface) example
- `multi-memory.wasm` - Module with multiple memory instances
- `table-ops.wasm` - Module demonstrating table operations

### Error Cases
- `invalid-magic.wasm` - Invalid WebAssembly magic number
- `missing-sections.wasm` - Module with missing required sections
- `corrupted.wasm` - Intentionally corrupted module

## Generating Test Files

WebAssembly test files can be generated from WebAssembly Text (WAT) format using the WebAssembly Binary Toolkit (wabt):

```bash
# Install wabt
# On macOS: brew install wabt
# On Ubuntu: apt install wabt
# On Windows: download from https://github.com/WebAssembly/wabt/releases

# Compile WAT to WASM
wat2wasm input.wat -o output.wasm
```

## Sample WAT Files

### Simple Addition Function
```wat
(module
  (func $add (param $lhs i32) (param $rhs i32) (result i32)
    local.get $lhs
    local.get $rhs
    i32.add)
  (export "add" (func $add))
)
```

### Memory Import Example
```wat
(module
  (import "env" "memory" (memory 1))
  (func (export "load") (param i32) (result i32)
    local.get 0
    i32.load)
)
```

## Official Test Suites

For comprehensive testing, consider integrating official WebAssembly test suites:

- [WebAssembly Specification Tests](https://github.com/WebAssembly/spec/tree/main/test)
- [Wasmtime Test Suite](https://github.com/bytecodealliance/wasmtime/tree/main/tests)

## Notes

- Binary WebAssembly files (.wasm) are checked into version control for test stability
- Test files should be minimal and focused on specific functionality
- Large test files should be generated during build rather than stored in repository
- All test files should include corresponding documentation about their purpose