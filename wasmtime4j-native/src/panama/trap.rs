//! Panama FFI bindings for trap introspection
//!
//! This module provides C-compatible functions for extracting detailed trap information
//! from WebAssembly runtime errors, enabling better error reporting and debugging.

use std::os::raw::{c_char, c_int, c_long};

/// Trap code constants matching Java TrapException.TrapType enum ordinals.
/// CRITICAL: These ordinals MUST match the Java enum declaration order exactly.
pub mod trap_codes {
    /// Stack overflow trap
    pub const STACK_OVERFLOW: i32 = 0;
    /// Memory out of bounds trap
    pub const MEMORY_OUT_OF_BOUNDS: i32 = 1;
    /// Heap misaligned (atomic operation) trap
    pub const HEAP_MISALIGNED: i32 = 2;
    /// Table out of bounds trap
    pub const TABLE_OUT_OF_BOUNDS: i32 = 3;
    /// Indirect call to null table entry trap
    pub const INDIRECT_CALL_TO_NULL: i32 = 4;
    /// Bad function signature trap
    pub const BAD_SIGNATURE: i32 = 5;
    /// Integer overflow trap
    pub const INTEGER_OVERFLOW: i32 = 6;
    /// Integer division by zero trap
    pub const INTEGER_DIVISION_BY_ZERO: i32 = 7;
    /// Bad conversion to integer trap
    pub const BAD_CONVERSION_TO_INTEGER: i32 = 8;
    /// Unreachable code reached trap
    pub const UNREACHABLE_CODE_REACHED: i32 = 9;
    /// Execution interrupted trap
    pub const INTERRUPT: i32 = 10;
    /// Component model always-trap adapter
    pub const ALWAYS_TRAP_ADAPTER: i32 = 11;
    /// Out of fuel trap
    pub const OUT_OF_FUEL: i32 = 12;
    /// Atomic wait on non-shared memory
    pub const ATOMIC_WAIT_NON_SHARED_MEMORY: i32 = 13;
    /// Null reference trap (GC proposal)
    pub const NULL_REFERENCE: i32 = 14;
    /// Array out of bounds trap (GC proposal)
    pub const ARRAY_OUT_OF_BOUNDS: i32 = 15;
    /// Allocation too large (GC proposal)
    pub const ALLOCATION_TOO_LARGE: i32 = 16;
    /// Cast failure (GC proposal)
    pub const CAST_FAILURE: i32 = 17;
    /// Cannot enter component (reentrance)
    pub const CANNOT_ENTER_COMPONENT: i32 = 18;
    /// Async export did not produce a result
    pub const NO_ASYNC_RESULT: i32 = 19;
    /// Unhandled tag during suspension
    pub const UNHANDLED_TAG: i32 = 20;
    /// Continuation already consumed
    pub const CONTINUATION_ALREADY_CONSUMED: i32 = 21;
    /// Disabled opcode executed
    pub const DISABLED_OPCODE: i32 = 22;
    /// Async event loop deadlocked
    pub const ASYNC_DEADLOCK: i32 = 23;
    /// Cannot leave component from current context
    pub const CANNOT_LEAVE_COMPONENT: i32 = 24;
    /// Synchronous task cannot make blocking call
    pub const CANNOT_BLOCK_SYNC_TASK: i32 = 25;
    /// Invalid character bit pattern
    pub const INVALID_CHAR: i32 = 26;
    /// String access out of bounds
    pub const STRING_OUT_OF_BOUNDS: i32 = 27;
    /// List access out of bounds
    pub const LIST_OUT_OF_BOUNDS: i32 = 28;
    /// Invalid discriminant for variant
    pub const INVALID_DISCRIMINANT: i32 = 29;
    /// Unaligned pointer in component operation
    pub const UNALIGNED_POINTER: i32 = 30;
    /// Unknown trap type
    pub const UNKNOWN: i32 = 31;
}

/// Parse a trap code from an error message string
///
/// This function analyzes the error message to determine the trap type.
/// It returns the trap code constant that matches the Java TrapType enum.
///
/// # Parameters
/// - error_message: Pointer to null-terminated C string containing the error message
///
/// # Returns
/// The trap code constant, or UNKNOWN if the trap type cannot be determined
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_trap_parse_code(error_message: *const c_char) -> c_int {
    if error_message.is_null() {
        return trap_codes::UNKNOWN;
    }

    let msg = match unsafe { std::ffi::CStr::from_ptr(error_message) }.to_str() {
        Ok(s) => s.to_lowercase(),
        Err(_) => return trap_codes::UNKNOWN,
    };

    // Parse trap type from message content using Wasmtime's error messages.
    // Order matters: more specific patterns must come before more general ones.
    if msg.contains("stack overflow") || msg.contains("call stack exhausted") {
        trap_codes::STACK_OVERFLOW
    } else if msg.contains("out of bounds memory") || msg.contains("memory access out of bounds") {
        trap_codes::MEMORY_OUT_OF_BOUNDS
    } else if msg.contains("misaligned") && (msg.contains("atomic") || msg.contains("heap")) {
        trap_codes::HEAP_MISALIGNED
    } else if msg.contains("out of bounds table") || msg.contains("table access out of bounds") {
        trap_codes::TABLE_OUT_OF_BOUNDS
    } else if msg.contains("indirect call") && msg.contains("null") {
        trap_codes::INDIRECT_CALL_TO_NULL
    } else if msg.contains("signature mismatch") || msg.contains("indirect call type mismatch") {
        trap_codes::BAD_SIGNATURE
    } else if msg.contains("integer overflow") {
        trap_codes::INTEGER_OVERFLOW
    } else if msg.contains("integer divide by zero") || msg.contains("division by zero") {
        trap_codes::INTEGER_DIVISION_BY_ZERO
    } else if msg.contains("invalid conversion") || msg.contains("bad conversion") {
        trap_codes::BAD_CONVERSION_TO_INTEGER
    } else if msg.contains("unreachable") {
        trap_codes::UNREACHABLE_CODE_REACHED
    } else if msg.contains("interrupt") || msg.contains("epoch") {
        trap_codes::INTERRUPT
    } else if msg.contains("always trap adapter") || msg.contains("alwaystrapadapter") {
        trap_codes::ALWAYS_TRAP_ADAPTER
    } else if msg.contains("fuel") && (msg.contains("out of") || msg.contains("ran out")) {
        trap_codes::OUT_OF_FUEL
    } else if msg.contains("atomic wait") && msg.contains("non-shared") {
        trap_codes::ATOMIC_WAIT_NON_SHARED_MEMORY
    } else if msg.contains("null reference") || msg.contains("null funcref") {
        trap_codes::NULL_REFERENCE
    } else if msg.contains("array") && msg.contains("out of bounds") {
        trap_codes::ARRAY_OUT_OF_BOUNDS
    } else if msg.contains("allocation too large") {
        trap_codes::ALLOCATION_TOO_LARGE
    } else if msg.contains("cast failure") || msg.contains("cast mismatch") {
        trap_codes::CAST_FAILURE
    } else if msg.contains("cannot enter component") {
        trap_codes::CANNOT_ENTER_COMPONENT
    } else if msg.contains("no async result") || msg.contains("task.return") {
        trap_codes::NO_ASYNC_RESULT
    } else if msg.contains("unhandled tag") {
        trap_codes::UNHANDLED_TAG
    } else if msg.contains("continuation") && msg.contains("consumed") {
        trap_codes::CONTINUATION_ALREADY_CONSUMED
    } else if msg.contains("disabled opcode") {
        trap_codes::DISABLED_OPCODE
    } else if msg.contains("async") && msg.contains("deadlock") {
        trap_codes::ASYNC_DEADLOCK
    } else if msg.contains("cannot leave component") {
        trap_codes::CANNOT_LEAVE_COMPONENT
    } else if msg.contains("cannot block") && msg.contains("sync") {
        trap_codes::CANNOT_BLOCK_SYNC_TASK
    } else if msg.contains("invalid char") {
        trap_codes::INVALID_CHAR
    } else if msg.contains("string") && msg.contains("out of bounds") {
        trap_codes::STRING_OUT_OF_BOUNDS
    } else if msg.contains("list") && msg.contains("out of bounds") {
        trap_codes::LIST_OUT_OF_BOUNDS
    } else if msg.contains("invalid discriminant") {
        trap_codes::INVALID_DISCRIMINANT
    } else if msg.contains("unaligned pointer") {
        trap_codes::UNALIGNED_POINTER
    } else {
        trap_codes::UNKNOWN
    }
}

/// Get the trap code name as a string
///
/// # Parameters
/// - trap_code: The trap code constant
///
/// # Returns
/// Pointer to a static null-terminated string with the trap code name,
/// or null if the code is invalid
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_trap_code_name(trap_code: c_int) -> *const c_char {
    static STACK_OVERFLOW: &[u8] = b"STACK_OVERFLOW\0";
    static MEMORY_OUT_OF_BOUNDS: &[u8] = b"MEMORY_OUT_OF_BOUNDS\0";
    static HEAP_MISALIGNED: &[u8] = b"HEAP_MISALIGNED\0";
    static TABLE_OUT_OF_BOUNDS: &[u8] = b"TABLE_OUT_OF_BOUNDS\0";
    static INDIRECT_CALL_TO_NULL: &[u8] = b"INDIRECT_CALL_TO_NULL\0";
    static BAD_SIGNATURE: &[u8] = b"BAD_SIGNATURE\0";
    static INTEGER_OVERFLOW: &[u8] = b"INTEGER_OVERFLOW\0";
    static INTEGER_DIVISION_BY_ZERO: &[u8] = b"INTEGER_DIVISION_BY_ZERO\0";
    static BAD_CONVERSION_TO_INTEGER: &[u8] = b"BAD_CONVERSION_TO_INTEGER\0";
    static UNREACHABLE_CODE_REACHED: &[u8] = b"UNREACHABLE_CODE_REACHED\0";
    static INTERRUPT: &[u8] = b"INTERRUPT\0";
    static ALWAYS_TRAP_ADAPTER: &[u8] = b"ALWAYS_TRAP_ADAPTER\0";
    static OUT_OF_FUEL: &[u8] = b"OUT_OF_FUEL\0";
    static ATOMIC_WAIT_NON_SHARED_MEMORY: &[u8] = b"ATOMIC_WAIT_NON_SHARED_MEMORY\0";
    static NULL_REFERENCE: &[u8] = b"NULL_REFERENCE\0";
    static ARRAY_OUT_OF_BOUNDS: &[u8] = b"ARRAY_OUT_OF_BOUNDS\0";
    static ALLOCATION_TOO_LARGE: &[u8] = b"ALLOCATION_TOO_LARGE\0";
    static CAST_FAILURE: &[u8] = b"CAST_FAILURE\0";
    static CANNOT_ENTER_COMPONENT: &[u8] = b"CANNOT_ENTER_COMPONENT\0";
    static NO_ASYNC_RESULT: &[u8] = b"NO_ASYNC_RESULT\0";
    static UNHANDLED_TAG: &[u8] = b"UNHANDLED_TAG\0";
    static CONTINUATION_ALREADY_CONSUMED: &[u8] = b"CONTINUATION_ALREADY_CONSUMED\0";
    static DISABLED_OPCODE: &[u8] = b"DISABLED_OPCODE\0";
    static ASYNC_DEADLOCK: &[u8] = b"ASYNC_DEADLOCK\0";
    static CANNOT_LEAVE_COMPONENT: &[u8] = b"CANNOT_LEAVE_COMPONENT\0";
    static CANNOT_BLOCK_SYNC_TASK: &[u8] = b"CANNOT_BLOCK_SYNC_TASK\0";
    static INVALID_CHAR: &[u8] = b"INVALID_CHAR\0";
    static STRING_OUT_OF_BOUNDS: &[u8] = b"STRING_OUT_OF_BOUNDS\0";
    static LIST_OUT_OF_BOUNDS: &[u8] = b"LIST_OUT_OF_BOUNDS\0";
    static INVALID_DISCRIMINANT: &[u8] = b"INVALID_DISCRIMINANT\0";
    static UNALIGNED_POINTER: &[u8] = b"UNALIGNED_POINTER\0";
    static UNKNOWN: &[u8] = b"UNKNOWN\0";

    let name = match trap_code {
        trap_codes::STACK_OVERFLOW => STACK_OVERFLOW,
        trap_codes::MEMORY_OUT_OF_BOUNDS => MEMORY_OUT_OF_BOUNDS,
        trap_codes::HEAP_MISALIGNED => HEAP_MISALIGNED,
        trap_codes::TABLE_OUT_OF_BOUNDS => TABLE_OUT_OF_BOUNDS,
        trap_codes::INDIRECT_CALL_TO_NULL => INDIRECT_CALL_TO_NULL,
        trap_codes::BAD_SIGNATURE => BAD_SIGNATURE,
        trap_codes::INTEGER_OVERFLOW => INTEGER_OVERFLOW,
        trap_codes::INTEGER_DIVISION_BY_ZERO => INTEGER_DIVISION_BY_ZERO,
        trap_codes::BAD_CONVERSION_TO_INTEGER => BAD_CONVERSION_TO_INTEGER,
        trap_codes::UNREACHABLE_CODE_REACHED => UNREACHABLE_CODE_REACHED,
        trap_codes::INTERRUPT => INTERRUPT,
        trap_codes::ALWAYS_TRAP_ADAPTER => ALWAYS_TRAP_ADAPTER,
        trap_codes::OUT_OF_FUEL => OUT_OF_FUEL,
        trap_codes::ATOMIC_WAIT_NON_SHARED_MEMORY => ATOMIC_WAIT_NON_SHARED_MEMORY,
        trap_codes::NULL_REFERENCE => NULL_REFERENCE,
        trap_codes::ARRAY_OUT_OF_BOUNDS => ARRAY_OUT_OF_BOUNDS,
        trap_codes::ALLOCATION_TOO_LARGE => ALLOCATION_TOO_LARGE,
        trap_codes::CAST_FAILURE => CAST_FAILURE,
        trap_codes::CANNOT_ENTER_COMPONENT => CANNOT_ENTER_COMPONENT,
        trap_codes::NO_ASYNC_RESULT => NO_ASYNC_RESULT,
        trap_codes::UNHANDLED_TAG => UNHANDLED_TAG,
        trap_codes::CONTINUATION_ALREADY_CONSUMED => CONTINUATION_ALREADY_CONSUMED,
        trap_codes::DISABLED_OPCODE => DISABLED_OPCODE,
        trap_codes::ASYNC_DEADLOCK => ASYNC_DEADLOCK,
        trap_codes::CANNOT_LEAVE_COMPONENT => CANNOT_LEAVE_COMPONENT,
        trap_codes::CANNOT_BLOCK_SYNC_TASK => CANNOT_BLOCK_SYNC_TASK,
        trap_codes::INVALID_CHAR => INVALID_CHAR,
        trap_codes::STRING_OUT_OF_BOUNDS => STRING_OUT_OF_BOUNDS,
        trap_codes::LIST_OUT_OF_BOUNDS => LIST_OUT_OF_BOUNDS,
        trap_codes::INVALID_DISCRIMINANT => INVALID_DISCRIMINANT,
        trap_codes::UNALIGNED_POINTER => UNALIGNED_POINTER,
        _ => UNKNOWN,
    };

    name.as_ptr() as *const c_char
}

/// Check if an error message indicates a trap condition
///
/// # Parameters
/// - error_message: Pointer to null-terminated C string containing the error message
///
/// # Returns
/// 1 if the message indicates a trap, 0 otherwise
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_trap_is_trap(error_message: *const c_char) -> c_int {
    if error_message.is_null() {
        return 0;
    }

    let msg = match unsafe { std::ffi::CStr::from_ptr(error_message) }.to_str() {
        Ok(s) => s.to_lowercase(),
        Err(_) => return 0,
    };

    // Check for common trap indicators in Wasmtime error messages
    let is_trap = msg.contains("wasm trap")
        || msg.contains("stack overflow")
        || msg.contains("out of bounds")
        || msg.contains("unreachable")
        || msg.contains("divide by zero")
        || msg.contains("division by zero")
        || msg.contains("integer overflow")
        || msg.contains("signature mismatch")
        || msg.contains("indirect call")
        || msg.contains("out of fuel")
        || msg.contains("epoch")
        || msg.contains("null reference")
        || msg.contains("misaligned")
        || msg.contains("always trap adapter")
        || msg.contains("atomic wait")
        || msg.contains("allocation too large")
        || msg.contains("cast failure")
        || msg.contains("cannot enter component")
        || msg.contains("cannot leave component")
        || (msg.contains("continuation") && msg.contains("consumed"))
        || msg.contains("disabled opcode")
        || (msg.contains("async") && msg.contains("deadlock"))
        || msg.contains("unhandled tag")
        || msg.contains("invalid discriminant")
        || msg.contains("unaligned pointer");

    if is_trap {
        1
    } else {
        0
    }
}

/// Extract function name from a backtrace line if present
///
/// # Parameters
/// - backtrace_line: Pointer to null-terminated C string containing a backtrace line
/// - out_buffer: Buffer to write the function name to
/// - buffer_size: Size of the output buffer
///
/// # Returns
/// The length of the function name written (excluding null terminator),
/// or -1 if no function name was found or an error occurred
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_trap_extract_function_name(
    backtrace_line: *const c_char,
    out_buffer: *mut c_char,
    buffer_size: usize,
) -> c_int {
    if backtrace_line.is_null() || out_buffer.is_null() || buffer_size == 0 {
        return -1;
    }

    let line = match unsafe { std::ffi::CStr::from_ptr(backtrace_line) }.to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    // Try to extract function name from patterns like:
    // "  0: 0x123 - <function_name>" or "function_name()"
    let func_name = if let Some(idx) = line.find(" - ") {
        let after = &line[idx + 3..];
        if let Some(end) = after.find(|c: char| c == '(' || c.is_whitespace()) {
            &after[..end]
        } else {
            after.trim()
        }
    } else if let Some(idx) = line.find("!") {
        // Pattern: "module!function_name"
        let after = &line[idx + 1..];
        if let Some(end) = after.find(|c: char| c == '(' || c.is_whitespace() || c == '+') {
            &after[..end]
        } else {
            after.trim()
        }
    } else {
        return -1;
    };

    if func_name.is_empty() {
        return -1;
    }

    // Copy to output buffer
    let bytes = func_name.as_bytes();
    let copy_len = std::cmp::min(bytes.len(), buffer_size - 1);

    unsafe {
        std::ptr::copy_nonoverlapping(bytes.as_ptr(), out_buffer as *mut u8, copy_len);
        *out_buffer.add(copy_len) = 0; // Null terminator
    }

    copy_len as c_int
}

/// Extract instruction offset from an error message or backtrace
///
/// # Parameters
/// - error_message: Pointer to null-terminated C string containing the error message
///
/// # Returns
/// The instruction offset if found, or -1 if not found
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_trap_extract_offset(error_message: *const c_char) -> c_long {
    if error_message.is_null() {
        return -1;
    }

    let msg = match unsafe { std::ffi::CStr::from_ptr(error_message) }.to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    // Look for patterns like "offset 0x123" or "at 0x123" or just "0x123"
    let offset_patterns = ["offset 0x", "at 0x", ": 0x"];

    for pattern in &offset_patterns {
        if let Some(idx) = msg.find(pattern) {
            let hex_start = idx + pattern.len();
            let hex_str = &msg[hex_start..];
            if let Some(end) = hex_str.find(|c: char| !c.is_ascii_hexdigit()) {
                let hex = &hex_str[..end];
                if let Ok(offset) = u64::from_str_radix(hex, 16) {
                    return offset as c_long;
                }
            } else if let Ok(offset) = u64::from_str_radix(hex_str.trim(), 16) {
                return offset as c_long;
            }
        }
    }

    -1
}

/// Structure to hold extracted trap information for FFI
#[repr(C)]
pub struct TrapInfo {
    /// The trap code constant
    pub trap_code: c_int,
    /// Instruction offset (-1 if not available)
    pub instruction_offset: c_long,
    /// Whether this is definitely a trap (1) or unknown (0)
    pub is_trap: c_int,
}

/// Extract comprehensive trap information from an error message
///
/// # Parameters
/// - error_message: Pointer to null-terminated C string containing the error message
/// - out_info: Pointer to TrapInfo structure to populate
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_trap_extract_info(
    error_message: *const c_char,
    out_info: *mut TrapInfo,
) -> c_int {
    if error_message.is_null() || out_info.is_null() {
        return -1;
    }

    let info = TrapInfo {
        trap_code: wasmtime4j_panama_trap_parse_code(error_message),
        instruction_offset: wasmtime4j_panama_trap_extract_offset(error_message),
        is_trap: wasmtime4j_panama_trap_is_trap(error_message),
    };

    unsafe {
        *out_info = info;
    }

    0
}
