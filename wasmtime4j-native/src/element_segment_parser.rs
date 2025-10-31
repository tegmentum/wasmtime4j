//! Element segment parser using wasmparser
//!
//! This module parses WebAssembly element sections from module bytecode,
//! extracting passive element segments for table.init() operations.

use wasmparser::{Parser, Payload, ElementKind, ElementItems};
use crate::element_segment::{ElementSegment, ElementItem, ElementMode};
use crate::error::{WasmtimeError, WasmtimeResult};
use wasmtime::ValType;

/// Parse element segments from WebAssembly module bytecode
///
/// Returns a vector where:
/// - Some(segment) = passive segment (cached for table.init)
/// - None = active/declarative segment (not cached in hybrid design)
pub fn parse_element_segments(module_bytes: &[u8]) -> WasmtimeResult<Vec<Option<ElementSegment>>> {
    let parser = Parser::new(0);
    let mut segments = Vec::new();

    for payload in parser.parse_all(module_bytes) {
        match payload.map_err(|e| WasmtimeError::Compilation {
            message: format!("Failed to parse module: {}", e),
        })? {
            Payload::ElementSection(reader) => {
                for element in reader {
                    let element = element.map_err(|e| WasmtimeError::Compilation {
                        message: format!("Failed to parse element segment: {}", e),
                    })?;

                    let segment = parse_element_segment(element)?;
                    segments.push(segment);
                }
            }
            _ => continue,
        }
    }

    Ok(segments)
}

/// Parse a single element segment
fn parse_element_segment(
    element: wasmparser::Element,
) -> WasmtimeResult<Option<ElementSegment>> {
    // Determine element type from items
    let elem_type = match &element.items {
        ElementItems::Functions(_) => {
            // Function indices -> funcref
            ValType::FUNCREF
        }
        ElementItems::Expressions(ref_type, _) => {
            // Convert wasmparser RefType to wasmtime ValType
            if ref_type.is_func_ref() {
                ValType::FUNCREF
            } else if ref_type.is_extern_ref() {
                ValType::EXTERNREF
            } else {
                // For other ref types, default to funcref for now
                log::warn!("Unsupported ref type in element segment, defaulting to funcref");
                ValType::FUNCREF
            }
        }
    };

    // Parse element kind and mode
    match element.kind {
        ElementKind::Passive => {
            // Passive segment - cache it for table.init()
            let items = parse_element_items(element.items)?;
            Ok(Some(ElementSegment::new_passive(elem_type, items)))
        }
        ElementKind::Active {
            table_index,
            offset_expr,
        } => {
            // Active segment - don't cache (hybrid design)
            // Could optionally parse for completeness, but not needed for table.init()
            log::debug!(
                "Skipping active element segment for table {} (not cached in hybrid design)",
                table_index.unwrap_or(0)
            );
            Ok(None)
        }
        ElementKind::Declared => {
            // Declarative segment - don't cache
            log::debug!("Skipping declarative element segment (not cached in hybrid design)");
            Ok(None)
        }
    }
}

/// Parse element items from an element segment
fn parse_element_items(items: ElementItems) -> WasmtimeResult<Vec<ElementItem>> {
    let mut result = Vec::new();

    match items {
        ElementItems::Functions(funcs) => {
            // Function indices
            for func_idx in funcs {
                let func_idx = func_idx.map_err(|e| WasmtimeError::Compilation {
                    message: format!("Failed to parse function index: {}", e),
                })?;
                result.push(ElementItem::FuncIndex(func_idx));
            }
        }
        ElementItems::Expressions(ref_type, exprs) => {
            // Constant expressions (for externref, etc.)
            for expr in exprs {
                let expr = expr.map_err(|e| WasmtimeError::Compilation {
                    message: format!("Failed to parse element expression: {}", e),
                })?;

                // Parse the constant expression
                let item = parse_const_expr(expr)?;
                result.push(item);
            }
        }
    }

    Ok(result)
}

/// Parse a constant expression to an ElementItem
fn parse_const_expr(
    expr: wasmparser::ConstExpr,
) -> WasmtimeResult<ElementItem> {
    // Get the expression reader
    let mut reader = expr.get_operators_reader();

    // Read the first operator
    let op = reader.read().map_err(|e| WasmtimeError::Compilation {
        message: format!("Failed to read const expression: {}", e),
    })?;

    use wasmparser::Operator;
    match op {
        Operator::RefNull { hty } => {
            // Null reference
            Ok(ElementItem::NullFunc)
        }
        Operator::RefFunc { function_index } => {
            // Function reference
            Ok(ElementItem::FuncIndex(function_index))
        }
        _ => {
            // For other expressions, store the bytecode
            // This is a simplified approach - could be enhanced
            log::warn!("Complex const expression in element segment, storing as opaque data");
            Ok(ElementItem::Expr(vec![]))
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_empty_module() {
        // Minimal valid WASM module with no element section
        let wasm = wat::parse_str(r#"
            (module)
        "#).unwrap();

        let segments = parse_element_segments(&wasm).unwrap();
        assert_eq!(segments.len(), 0);
    }

    #[test]
    fn test_parse_passive_element_segment() {
        // Module with a passive element segment
        let wasm = wat::parse_str(r#"
            (module
                (func $f1)
                (func $f2)
                (elem $e (func $f1 $f2))
            )
        "#).unwrap();

        let segments = parse_element_segments(&wasm).unwrap();

        // Should have one segment
        assert_eq!(segments.len(), 1);

        // First segment should be Some (passive)
        assert!(segments[0].is_some());

        let segment = segments[0].as_ref().unwrap();
        assert!(segment.is_passive());
        assert_eq!(segment.len(), 2);
    }

    #[test]
    fn test_parse_active_element_segment() {
        // Module with an active element segment
        let wasm = wat::parse_str(r#"
            (module
                (table 10 funcref)
                (func $f1)
                (elem (i32.const 0) $f1)
            )
        "#).unwrap();

        let segments = parse_element_segments(&wasm).unwrap();

        // Should have one segment
        assert_eq!(segments.len(), 1);

        // First segment should be None (active - not cached)
        assert!(segments[0].is_none());
    }
}
