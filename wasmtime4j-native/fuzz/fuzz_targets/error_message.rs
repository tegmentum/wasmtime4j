#![no_main]

use arbitrary::Arbitrary;
use libfuzzer_sys::fuzz_target;
use std::fmt::Write;

/// Structured input for error message formatting fuzzing.
///
/// Tests that WasmtimeError Display and Debug formatting never panics,
/// even with invalid UTF-8, embedded nulls, or extremely long strings.
#[derive(Debug, Arbitrary)]
struct ErrorMessageInput {
    /// Raw message bytes (may contain invalid UTF-8, nulls, etc.).
    message_bytes: Vec<u8>,
    /// Selects which error variant to construct (mod number of variants).
    error_category: u8,
    /// Whether to also test wrapping in anyhow::Error.
    include_context: bool,
    /// Additional context bytes for wrapped error chains.
    context_bytes: Vec<u8>,
}

fuzz_target!(|input: ErrorMessageInput| {
    // Cap message length to avoid OOM (10KB max)
    let message_bytes: Vec<u8> = input.message_bytes.iter().take(10240).copied().collect();

    // Convert bytes to String, replacing invalid UTF-8 with replacement characters
    let message = String::from_utf8_lossy(&message_bytes).into_owned();
    let context_msg = String::from_utf8_lossy(
        &input.context_bytes.iter().take(1024).copied().collect::<Vec<u8>>()
    ).into_owned();

    // Construct various error types based on category selector.
    // Covers all variants that take simple String fields.
    // Skipped: Io (requires io::Error), Multiple (requires Vec<WasmtimeError>),
    //          WasiExit (requires i32), WastExecutionError (tuple variant),
    //          JniError (tuple variant)
    let error = match input.error_category % 29 {
        0 => wasmtime4j::error::WasmtimeError::Compilation { message: message.clone() },
        1 => wasmtime4j::error::WasmtimeError::Validation { message: message.clone() },
        2 => wasmtime4j::error::WasmtimeError::Module { message: message.clone() },
        3 => wasmtime4j::error::WasmtimeError::Runtime {
            message: message.clone(),
            backtrace: None,
        },
        4 => wasmtime4j::error::WasmtimeError::EngineConfig { message: message.clone() },
        5 => wasmtime4j::error::WasmtimeError::Store { message: message.clone() },
        6 => wasmtime4j::error::WasmtimeError::Instance { message: message.clone() },
        7 => wasmtime4j::error::WasmtimeError::Function { message: message.clone() },
        8 => wasmtime4j::error::WasmtimeError::Memory { message: message.clone() },
        9 => wasmtime4j::error::WasmtimeError::Table { message: message.clone() },
        10 => wasmtime4j::error::WasmtimeError::Global { message: message.clone() },
        11 => wasmtime4j::error::WasmtimeError::Linker { message: message.clone() },
        12 => wasmtime4j::error::WasmtimeError::ImportExport { message: message.clone() },
        13 => wasmtime4j::error::WasmtimeError::Type { message: message.clone() },
        14 => wasmtime4j::error::WasmtimeError::Resource { message: message.clone() },
        15 => wasmtime4j::error::WasmtimeError::InvalidParameter { message: message.clone() },
        16 => wasmtime4j::error::WasmtimeError::Concurrency { message: message.clone() },
        17 => wasmtime4j::error::WasmtimeError::Wasi { message: message.clone() },
        18 => wasmtime4j::error::WasmtimeError::Security { message: message.clone() },
        19 => wasmtime4j::error::WasmtimeError::Component { message: message.clone() },
        20 => wasmtime4j::error::WasmtimeError::Interface { message: message.clone() },
        21 => wasmtime4j::error::WasmtimeError::Internal { message: message.clone() },
        22 => wasmtime4j::error::WasmtimeError::Execution { message: message.clone() },
        23 => wasmtime4j::error::WasmtimeError::Instantiation { message: message.clone() },
        24 => wasmtime4j::error::WasmtimeError::CallerContextError { message: message.clone() },
        25 => wasmtime4j::error::WasmtimeError::UnsupportedFeature { message: message.clone() },
        26 => wasmtime4j::error::WasmtimeError::TypeMismatch {
            expected: message.clone(),
            actual: context_msg.clone(),
        },
        27 => wasmtime4j::error::WasmtimeError::ExportNotFound { name: message.clone() },
        28 => wasmtime4j::error::WasmtimeError::Utf8Error { message: message.clone() },
        _ => unreachable!(),
    };

    // Test Display formatting - must produce valid UTF-8 and not panic
    let display_output = format!("{}", error);
    assert!(
        std::str::from_utf8(display_output.as_bytes()).is_ok(),
        "Display output is not valid UTF-8"
    );

    // Test Debug formatting - must produce valid UTF-8 and not panic
    let debug_output = format!("{:?}", error);
    assert!(
        std::str::from_utf8(debug_output.as_bytes()).is_ok(),
        "Debug output is not valid UTF-8"
    );

    // Test write! macro formatting
    let mut buf = String::new();
    let _ = write!(buf, "{}", error);
    let _ = write!(buf, "{:?}", error);

    // Test to_string()
    let _ = error.to_string();

    // Test wrapping in anyhow::Error
    if input.include_context {
        let anyhow_err: anyhow::Error = error.into();
        let anyhow_display = format!("{}", anyhow_err);
        assert!(
            std::str::from_utf8(anyhow_display.as_bytes()).is_ok(),
            "anyhow Display output is not valid UTF-8"
        );
        let anyhow_debug = format!("{:?}", anyhow_err);
        assert!(
            std::str::from_utf8(anyhow_debug.as_bytes()).is_ok(),
            "anyhow Debug output is not valid UTF-8"
        );
    }
});
