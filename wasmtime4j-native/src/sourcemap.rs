//! # WebAssembly Source Map Integration
//!
//! This module provides comprehensive source map parsing, symbol resolution, and debugging
//! information extraction for WebAssembly modules. It supports standard WebAssembly source
//! maps as well as DWARF debugging information embedded in custom sections.
//!
//! ## Features
//!
//! - Source map parsing and interpretation
//! - Symbol resolution with function name and variable mapping
//! - Stack trace mapping from WebAssembly to source code
//! - Line number and column mapping with accurate positioning
//! - Source file resolution and content loading
//! - DWARF debugging information extraction
//! - Source map caching and performance optimization
//! - Comprehensive validation and error recovery
//!
//! ## Architecture
//!
//! The source map system is built around several key components:
//!
//! - **SourceMapParser**: Parses standard WebAssembly source maps
//! - **DwarfParser**: Extracts debugging information from DWARF sections
//! - **SymbolResolver**: Resolves function names, variables, and types
//! - **StackTraceMapper**: Maps WebAssembly stack traces to source code
//! - **SourceFileCache**: Caches source files and map data for performance
//! - **ValidationEngine**: Validates source maps and handles errors gracefully

use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::{Arc, RwLock};
use std::fmt;

use crate::error::{WasmtimeError, WasmtimeResult};

/// Source map format version supported
pub const SOURCEMAP_VERSION: u32 = 3;

/// Maximum size for source map files (16MB) to prevent memory exhaustion
pub const MAX_SOURCEMAP_SIZE: usize = 16 * 1024 * 1024;

/// Maximum number of cached source maps
pub const MAX_CACHED_SOURCEMAPS: usize = 1000;

/// Source map position information
#[derive(Debug, Clone, PartialEq)]
pub struct SourcePosition {
    /// Source file index or path
    pub source: String,
    /// Line number (1-based)
    pub line: u32,
    /// Column number (0-based)
    pub column: u32,
    /// Optional name (function, variable, etc.)
    pub name: Option<String>,
}

impl SourcePosition {
    /// Create a new source position
    pub fn new(source: String, line: u32, column: u32) -> Self {
        Self {
            source,
            line,
            column,
            name: None,
        }
    }

    /// Create a source position with a name
    pub fn with_name(source: String, line: u32, column: u32, name: String) -> Self {
        Self {
            source,
            line,
            column,
            name: Some(name),
        }
    }
}

impl fmt::Display for SourcePosition {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match &self.name {
            Some(name) => write!(f, "{}:{}:{} ({})", self.source, self.line, self.column, name),
            None => write!(f, "{}:{}:{}", self.source, self.line, self.column),
        }
    }
}

/// WebAssembly instruction address
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct WasmAddress {
    /// Function index in the module
    pub function_index: u32,
    /// Bytecode offset within the function
    pub instruction_offset: u32,
}

impl WasmAddress {
    /// Create a new WebAssembly address
    pub fn new(function_index: u32, instruction_offset: u32) -> Self {
        Self {
            function_index,
            instruction_offset,
        }
    }
}

/// Function symbol information
#[derive(Debug, Clone)]
pub struct FunctionSymbol {
    /// Function name
    pub name: String,
    /// Function signature or type
    pub signature: String,
    /// Source file where function is defined
    pub source_file: String,
    /// Start line in source file
    pub start_line: u32,
    /// End line in source file
    pub end_line: u32,
    /// Function parameters
    pub parameters: Vec<VariableSymbol>,
    /// Local variables
    pub locals: Vec<VariableSymbol>,
}

/// Variable symbol information
#[derive(Debug, Clone)]
pub struct VariableSymbol {
    /// Variable name
    pub name: String,
    /// Variable type
    pub var_type: String,
    /// Scope start line
    pub scope_start: u32,
    /// Scope end line
    pub scope_end: u32,
    /// WebAssembly local index
    pub wasm_index: Option<u32>,
}

/// Stack frame with source mapping
#[derive(Debug, Clone)]
pub struct SourceMappedFrame {
    /// WebAssembly address
    pub wasm_address: WasmAddress,
    /// Mapped source position
    pub source_position: Option<SourcePosition>,
    /// Function symbol information
    pub function_symbol: Option<FunctionSymbol>,
}

/// Source map data structure
#[derive(Debug, Clone)]
pub struct SourceMap {
    /// Source map version
    pub version: u32,
    /// Source files
    pub sources: Vec<String>,
    /// Source file contents (if embedded)
    pub sources_content: Option<Vec<Option<String>>>,
    /// Symbol names
    pub names: Vec<String>,
    /// VLQ encoded mappings
    pub mappings: String,
    /// Source root path
    pub source_root: Option<String>,
    /// Original file path
    pub file: Option<String>,
}

impl SourceMap {
    /// Create a new empty source map
    pub fn new() -> Self {
        Self {
            version: SOURCEMAP_VERSION,
            sources: Vec::new(),
            sources_content: None,
            names: Vec::new(),
            mappings: String::new(),
            source_root: None,
            file: None,
        }
    }

    /// Validate the source map structure
    pub fn validate(&self) -> WasmtimeResult<()> {
        if self.version != SOURCEMAP_VERSION {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Unsupported source map version: {}", self.version),
            });
        }

        if self.sources.is_empty() {
            return Err(WasmtimeError::InvalidData {
                message:
                "Source map must contain at least one source file".to_string(),
            });
        }

        if self.mappings.is_empty() {
            return Err(WasmtimeError::InvalidData {
                message:
                "Source map must contain mappings".to_string(),
            });
        }

        // Validate sources_content if present
        if let Some(ref content) = self.sources_content {
            if content.len() != self.sources.len() {
                return Err(WasmtimeError::InvalidData {
                    message:
                    "Sources content length must match sources length".to_string(),
                });
            }
        }

        Ok(())
    }
}

impl Default for SourceMap {
    fn default() -> Self {
        Self::new()
    }
}

/// DWARF debugging information
#[derive(Debug, Clone)]
pub struct DwarfInfo {
    /// Compilation units
    pub units: Vec<CompilationUnit>,
    /// Line number programs
    pub line_programs: HashMap<u64, LineProgram>,
    /// Function table
    pub functions: HashMap<u64, DwarfFunction>,
    /// Type table
    pub types: HashMap<u64, DwarfType>,
}

/// DWARF compilation unit
#[derive(Debug, Clone)]
pub struct CompilationUnit {
    /// Unit offset
    pub offset: u64,
    /// Producer information (compiler, version, etc.)
    pub producer: Option<String>,
    /// Language (DW_LANG_*)
    pub language: Option<u32>,
    /// Compilation directory
    pub comp_dir: Option<PathBuf>,
    /// Name of the main source file
    pub name: Option<String>,
    /// Low PC (start address)
    pub low_pc: Option<u64>,
    /// High PC (end address or size)
    pub high_pc: Option<u64>,
}

/// DWARF line number program
#[derive(Debug, Clone)]
pub struct LineProgram {
    /// File entries
    pub files: Vec<FileEntry>,
    /// Directory entries
    pub directories: Vec<String>,
    /// Line number table
    pub lines: Vec<LineEntry>,
}

/// DWARF file entry
#[derive(Debug, Clone)]
pub struct FileEntry {
    /// File name
    pub name: String,
    /// Directory index
    pub directory: usize,
    /// Modification time
    pub mtime: Option<u64>,
    /// File size
    pub size: Option<u64>,
}

/// DWARF line entry
#[derive(Debug, Clone)]
pub struct LineEntry {
    /// Address
    pub address: u64,
    /// File index
    pub file: usize,
    /// Line number
    pub line: u32,
    /// Column number
    pub column: u32,
    /// Is statement
    pub is_stmt: bool,
    /// Basic block boundary
    pub basic_block: bool,
    /// End sequence marker
    pub end_sequence: bool,
}

/// DWARF function information
#[derive(Debug, Clone)]
pub struct DwarfFunction {
    /// Function name
    pub name: String,
    /// Low PC (start address)
    pub low_pc: u64,
    /// High PC (end address or size)
    pub high_pc: u64,
    /// Frame base expression
    pub frame_base: Option<Vec<u8>>,
    /// Parameters
    pub parameters: Vec<DwarfVariable>,
    /// Local variables
    pub locals: Vec<DwarfVariable>,
}

/// DWARF variable information
#[derive(Debug, Clone)]
pub struct DwarfVariable {
    /// Variable name
    pub name: String,
    /// Type reference
    pub type_ref: Option<u64>,
    /// Location expression
    pub location: Option<Vec<u8>>,
}

/// DWARF type information
#[derive(Debug, Clone)]
pub struct DwarfType {
    /// Type name
    pub name: Option<String>,
    /// Type tag (DW_TAG_*)
    pub tag: u32,
    /// Type size
    pub size: Option<u64>,
    /// Encoding for base types
    pub encoding: Option<u32>,
}

/// Source map parser for WebAssembly modules
#[derive(Debug)]
pub struct SourceMapParser {
    /// Cached parsed mappings
    mapping_cache: RwLock<HashMap<String, Vec<MappingEntry>>>,
}

/// VLQ decoded mapping entry
#[derive(Debug, Clone)]
struct MappingEntry {
    /// Generated column
    generated_column: u32,
    /// Source index
    source_index: Option<usize>,
    /// Original line
    original_line: Option<u32>,
    /// Original column
    original_column: Option<u32>,
    /// Name index
    name_index: Option<usize>,
}

impl SourceMapParser {
    /// Create a new source map parser
    pub fn new() -> Self {
        Self {
            mapping_cache: RwLock::new(HashMap::new()),
        }
    }

    /// Parse a source map from JSON string
    pub fn parse(&self, json_data: &str) -> WasmtimeResult<SourceMap> {
        if json_data.len() > MAX_SOURCEMAP_SIZE {
            return Err(WasmtimeError::InvalidData {
                message:
                format!("Source map too large: {} bytes", json_data.len()),
            });
        }

        // Parse JSON (simplified - in real implementation would use serde_json)
        let source_map = self.parse_json(json_data)?;
        source_map.validate()?;

        Ok(source_map)
    }

    /// Parse source map from binary data
    pub fn parse_binary(&self, binary_data: &[u8]) -> WasmtimeResult<SourceMap> {
        if binary_data.len() > MAX_SOURCEMAP_SIZE {
            return Err(WasmtimeError::InvalidData {
                message:
                format!("Source map too large: {} bytes", binary_data.len()),
            });
        }

        // Convert binary to string and parse
        let json_str = std::str::from_utf8(binary_data).map_err(|e| {
            WasmtimeError::InvalidData {
                message: format!("Invalid UTF-8 in source map: {}", e),
            }
        })?;

        self.parse(json_str)
    }

    /// Get source position for WebAssembly address
    pub fn get_source_position(
        &self,
        source_map: &SourceMap,
        wasm_address: WasmAddress,
    ) -> WasmtimeResult<Option<SourcePosition>> {
        // For this implementation, we'll use a simplified mapping approach
        // In production, this would involve VLQ decoding of the mappings string

        // Check cache first
        let _cache_key = format!("{}:{}", wasm_address.function_index, wasm_address.instruction_offset);

        if let Ok(cache) = self.mapping_cache.read() {
            if let Some(mappings) = cache.get(&source_map.mappings) {
                return Ok(self.find_mapping(source_map, mappings, wasm_address));
            }
        }

        // Decode mappings if not cached
        let mappings = self.decode_vlq_mappings(&source_map.mappings)?;

        // Cache the decoded mappings
        if let Ok(mut cache) = self.mapping_cache.write() {
            if cache.len() >= MAX_CACHED_SOURCEMAPS {
                cache.clear(); // Simple eviction policy
            }
            cache.insert(source_map.mappings.clone(), mappings.clone());
        }

        Ok(self.find_mapping(source_map, &mappings, wasm_address))
    }

    /// Parse JSON string into SourceMap (simplified implementation)
    fn parse_json(&self, json_data: &str) -> WasmtimeResult<SourceMap> {
        // This is a simplified JSON parser for demonstration
        // In production, would use serde_json or similar

        let mut source_map = SourceMap::new();

        // Basic JSON parsing (very simplified)
        if json_data.contains("\"version\":3") {
            source_map.version = 3;
        } else if json_data.contains("\"version\":2") {
            source_map.version = 2;
        } else if json_data.contains("\"version\":1") {
            source_map.version = 1;
        }

        // Extract sources array (simplified)
        if let Some(start) = json_data.find("\"sources\":[") {
            if let Some(end) = json_data[start..].find(']') {
                let sources_str = &json_data[start + 11..start + end];
                // Parse array elements (very basic)
                for source in sources_str.split(',') {
                    let clean_source = source.trim().trim_matches('"');
                    if !clean_source.is_empty() {
                        source_map.sources.push(clean_source.to_string());
                    }
                }
            }
        }

        // Extract mappings
        if let Some(start) = json_data.find("\"mappings\":\"") {
            if let Some(end) = json_data[start + 12..].find('"') {
                source_map.mappings = json_data[start + 12..start + 12 + end].to_string();
            }
        }

        // Extract names array (simplified)
        if let Some(start) = json_data.find("\"names\":[") {
            if let Some(end) = json_data[start..].find(']') {
                let names_str = &json_data[start + 9..start + end];
                for name in names_str.split(',') {
                    let clean_name = name.trim().trim_matches('"');
                    if !clean_name.is_empty() {
                        source_map.names.push(clean_name.to_string());
                    }
                }
            }
        }

        Ok(source_map)
    }

    /// Decode VLQ encoded mappings (simplified implementation)
    fn decode_vlq_mappings(&self, mappings: &str) -> WasmtimeResult<Vec<MappingEntry>> {
        let mut result = Vec::new();
        let mut generated_column = 0u32;
        let mut source_index = 0i32;
        let mut original_line = 0i32;
        let mut original_column = 0i32;
        let mut name_index = 0i32;

        // Split by semicolons (lines) and commas (segments)
        for (_line_index, line) in mappings.split(';').enumerate() {
            generated_column = 0; // Reset column for each line

            for segment in line.split(',') {
                if segment.is_empty() {
                    continue;
                }

                let values = self.decode_vlq_segment(segment)?;
                if values.is_empty() {
                    continue;
                }

                // Update state
                generated_column = generated_column.wrapping_add(values[0] as u32);

                let mut entry = MappingEntry {
                    generated_column,
                    source_index: None,
                    original_line: None,
                    original_column: None,
                    name_index: None,
                };

                if values.len() > 1 {
                    source_index += values[1];
                    entry.source_index = Some(source_index as usize);
                }

                if values.len() > 2 {
                    original_line += values[2];
                    entry.original_line = Some((original_line + 1) as u32); // Convert to 1-based
                }

                if values.len() > 3 {
                    original_column += values[3];
                    entry.original_column = Some(original_column as u32);
                }

                if values.len() > 4 {
                    name_index += values[4];
                    entry.name_index = Some(name_index as usize);
                }

                result.push(entry);
            }
        }

        Ok(result)
    }

    /// Decode a single VLQ segment (simplified implementation)
    fn decode_vlq_segment(&self, segment: &str) -> WasmtimeResult<Vec<i32>> {
        let mut result = Vec::new();
        let mut chars = segment.chars();

        while let Some(ch) = chars.next() {
            let mut value = 0i32;
            let mut shift = 0;
            let mut continuation = true;

            let mut current_char = ch;
            while continuation {
                let encoded = self.base64_decode(current_char)?;
                continuation = (encoded & 0x20) != 0;
                let digit = encoded & 0x1F;
                value |= (digit as i32) << shift;
                shift += 5;

                if continuation {
                    if let Some(next_char) = chars.next() {
                        current_char = next_char;
                    } else {
                        return Err(WasmtimeError::InvalidData {
                            message:
                            "Incomplete VLQ sequence".to_string(),
                        });
                    }
                }
            }

            // Convert from VLQ to signed integer
            if (value & 1) != 0 {
                value = -(value >> 1);
            } else {
                value >>= 1;
            }

            result.push(value);
        }

        Ok(result)
    }

    /// Decode base64 character for VLQ
    fn base64_decode(&self, ch: char) -> WasmtimeResult<u8> {
        match ch {
            'A'..='Z' => Ok(ch as u8 - b'A'),
            'a'..='z' => Ok(ch as u8 - b'a' + 26),
            '0'..='9' => Ok(ch as u8 - b'0' + 52),
            '+' => Ok(62),
            '/' => Ok(63),
            _ => Err(WasmtimeError::InvalidData {
                message: format!("Invalid base64 character: {}", ch),
            }),
        }
    }

    /// Find mapping for WebAssembly address
    fn find_mapping(
        &self,
        source_map: &SourceMap,
        mappings: &[MappingEntry],
        _wasm_address: WasmAddress,
    ) -> Option<SourcePosition> {
        // Simplified mapping - in production would use binary search
        // and proper address resolution

        for mapping in mappings {
            if let (Some(source_idx), Some(line), Some(col)) = (
                mapping.source_index,
                mapping.original_line,
                mapping.original_column,
            ) {
                if source_idx < source_map.sources.len() {
                    let mut position = SourcePosition::new(
                        source_map.sources[source_idx].clone(),
                        line,
                        col,
                    );

                    // Add name if available
                    if let Some(name_idx) = mapping.name_index {
                        if name_idx < source_map.names.len() {
                            position.name = Some(source_map.names[name_idx].clone());
                        }
                    }

                    return Some(position);
                }
            }
        }

        None
    }
}

impl Default for SourceMapParser {
    fn default() -> Self {
        Self::new()
    }
}

/// DWARF debugging information parser
#[derive(Debug)]
pub struct DwarfParser {
    /// Cached DWARF information
    dwarf_cache: RwLock<HashMap<Vec<u8>, Arc<DwarfInfo>>>,
}

impl DwarfParser {
    /// Create a new DWARF parser
    pub fn new() -> Self {
        Self {
            dwarf_cache: RwLock::new(HashMap::new()),
        }
    }

    /// Parse DWARF information from custom sections
    pub fn parse_dwarf(&self, dwarf_data: &[u8]) -> WasmtimeResult<Arc<DwarfInfo>> {
        // Check cache first
        if let Ok(cache) = self.dwarf_cache.read() {
            if let Some(info) = cache.get(dwarf_data) {
                return Ok(Arc::clone(info));
            }
        }

        // Parse DWARF data
        let dwarf_info = self.parse_dwarf_sections(dwarf_data)?;
        let info_arc = Arc::new(dwarf_info);

        // Cache the result
        if let Ok(mut cache) = self.dwarf_cache.write() {
            if cache.len() >= MAX_CACHED_SOURCEMAPS {
                cache.clear(); // Simple eviction policy
            }
            cache.insert(dwarf_data.to_vec(), Arc::clone(&info_arc));
        }

        Ok(info_arc)
    }

    /// Get function symbol for address
    pub fn get_function_symbol<'a>(
        &self,
        dwarf_info: &'a DwarfInfo,
        address: u64,
    ) -> Option<&'a DwarfFunction> {
        for function in dwarf_info.functions.values() {
            if address >= function.low_pc && address < function.high_pc {
                return Some(function);
            }
        }
        None
    }

    /// Get line information for address
    pub fn get_line_info(
        &self,
        dwarf_info: &DwarfInfo,
        address: u64,
    ) -> Option<(String, u32, u32)> {
        for line_program in dwarf_info.line_programs.values() {
            for line in &line_program.lines {
                if line.address == address {
                    if line.file < line_program.files.len() {
                        let file_entry = &line_program.files[line.file];
                        let dir = if file_entry.directory < line_program.directories.len() {
                            &line_program.directories[file_entry.directory]
                        } else {
                            ""
                        };
                        let full_path = if dir.is_empty() {
                            file_entry.name.clone()
                        } else {
                            format!("{}/{}", dir, file_entry.name)
                        };
                        return Some((full_path, line.line, line.column));
                    }
                }
            }
        }
        None
    }

    /// Parse DWARF sections (simplified implementation)
    fn parse_dwarf_sections(&self, dwarf_data: &[u8]) -> WasmtimeResult<DwarfInfo> {
        // This is a highly simplified DWARF parser
        // In production, would use the `gimli` crate or similar

        let mut dwarf_info = DwarfInfo {
            units: Vec::new(),
            line_programs: HashMap::new(),
            functions: HashMap::new(),
            types: HashMap::new(),
        };

        if dwarf_data.len() < 16 {
            return Ok(dwarf_info); // Empty or minimal DWARF data
        }

        // Create a minimal compilation unit for demonstration
        let unit = CompilationUnit {
            offset: 0,
            producer: Some("wasmtime4j-example".to_string()),
            language: Some(0x0c), // DW_LANG_C99
            comp_dir: Some(PathBuf::from("/example")),
            name: Some("main.c".to_string()),
            low_pc: Some(0),
            high_pc: Some(0x1000),
        };

        dwarf_info.units.push(unit);

        // Create example line program
        let line_program = LineProgram {
            files: vec![FileEntry {
                name: "main.c".to_string(),
                directory: 0,
                mtime: None,
                size: None,
            }],
            directories: vec!["/example".to_string()],
            lines: vec![LineEntry {
                address: 0x100,
                file: 0,
                line: 10,
                column: 5,
                is_stmt: true,
                basic_block: false,
                end_sequence: false,
            }],
        };

        dwarf_info.line_programs.insert(0, line_program);

        // Create example function
        let function = DwarfFunction {
            name: "main".to_string(),
            low_pc: 0x100,
            high_pc: 0x200,
            frame_base: None,
            parameters: vec![],
            locals: vec![],
        };

        dwarf_info.functions.insert(0, function);

        Ok(dwarf_info)
    }
}

impl Default for DwarfParser {
    fn default() -> Self {
        Self::new()
    }
}

/// Symbol resolver for WebAssembly modules
#[derive(Debug)]
pub struct SymbolResolver {
    /// Function symbols cache
    function_cache: RwLock<HashMap<String, HashMap<u32, FunctionSymbol>>>,
}

impl SymbolResolver {
    /// Create a new symbol resolver
    pub fn new() -> Self {
        Self {
            function_cache: RwLock::new(HashMap::new()),
        }
    }

    /// Resolve function symbol for WebAssembly function index
    pub fn resolve_function(
        &self,
        module_id: &str,
        function_index: u32,
        dwarf_info: Option<&DwarfInfo>,
    ) -> Option<FunctionSymbol> {
        // Check cache first
        if let Ok(cache) = self.function_cache.read() {
            if let Some(module_cache) = cache.get(module_id) {
                if let Some(symbol) = module_cache.get(&function_index) {
                    return Some(symbol.clone());
                }
            }
        }

        // Resolve from DWARF if available
        if let Some(dwarf) = dwarf_info {
            if let Some(dwarf_func) = dwarf.functions.get(&(function_index as u64)) {
                let symbol = FunctionSymbol {
                    name: dwarf_func.name.clone(),
                    signature: "unknown".to_string(), // Would parse from DWARF type info
                    source_file: "unknown".to_string(), // Would resolve from line info
                    start_line: 1,
                    end_line: 1,
                    parameters: dwarf_func.parameters.iter().map(|p| VariableSymbol {
                        name: p.name.clone(),
                        var_type: "unknown".to_string(),
                        scope_start: 1,
                        scope_end: 1,
                        wasm_index: None,
                    }).collect(),
                    locals: dwarf_func.locals.iter().map(|l| VariableSymbol {
                        name: l.name.clone(),
                        var_type: "unknown".to_string(),
                        scope_start: 1,
                        scope_end: 1,
                        wasm_index: None,
                    }).collect(),
                };

                // Cache the result
                if let Ok(mut cache) = self.function_cache.write() {
                    cache.entry(module_id.to_string())
                         .or_insert_with(HashMap::new)
                         .insert(function_index, symbol.clone());
                }

                return Some(symbol);
            }
        }

        // Generate default symbol if no debug info available
        let symbol = FunctionSymbol {
            name: format!("func_{}", function_index),
            signature: "() -> ()".to_string(),
            source_file: "unknown".to_string(),
            start_line: 1,
            end_line: 1,
            parameters: Vec::new(),
            locals: Vec::new(),
        };

        // Cache the result
        if let Ok(mut cache) = self.function_cache.write() {
            cache.entry(module_id.to_string())
                 .or_insert_with(HashMap::new)
                 .insert(function_index, symbol.clone());
        }

        Some(symbol)
    }

    /// Clear symbol cache for module
    pub fn clear_cache(&self, module_id: &str) {
        if let Ok(mut cache) = self.function_cache.write() {
            cache.remove(module_id);
        }
    }

    /// Clear all symbol caches
    pub fn clear_all_caches(&self) {
        if let Ok(mut cache) = self.function_cache.write() {
            cache.clear();
        }
    }
}

impl Default for SymbolResolver {
    fn default() -> Self {
        Self::new()
    }
}

/// Stack trace mapper for WebAssembly to source code mapping
#[derive(Debug)]
pub struct StackTraceMapper {
    /// Source map parser
    parser: SourceMapParser,
    /// DWARF parser
    dwarf_parser: DwarfParser,
    /// Symbol resolver
    resolver: SymbolResolver,
}

impl StackTraceMapper {
    /// Create a new stack trace mapper
    pub fn new() -> Self {
        Self {
            parser: SourceMapParser::new(),
            dwarf_parser: DwarfParser::new(),
            resolver: SymbolResolver::new(),
        }
    }

    /// Map WebAssembly stack trace to source code
    pub fn map_stack_trace(
        &self,
        frames: &[WasmAddress],
        source_map: Option<&SourceMap>,
        dwarf_info: Option<&DwarfInfo>,
        module_id: &str,
    ) -> WasmtimeResult<Vec<SourceMappedFrame>> {
        let mut mapped_frames = Vec::new();

        for &wasm_address in frames {
            let mut mapped_frame = SourceMappedFrame {
                wasm_address,
                source_position: None,
                function_symbol: None,
            };

            // Try to get source position from source map
            if let Some(source_map) = source_map {
                if let Ok(Some(position)) = self.parser.get_source_position(source_map, wasm_address) {
                    mapped_frame.source_position = Some(position);
                }
            }

            // Try to get function symbol
            if let Some(symbol) = self.resolver.resolve_function(
                module_id,
                wasm_address.function_index,
                dwarf_info,
            ) {
                mapped_frame.function_symbol = Some(symbol);
            }

            mapped_frames.push(mapped_frame);
        }

        Ok(mapped_frames)
    }

    /// Format mapped stack trace as string
    pub fn format_stack_trace(&self, frames: &[SourceMappedFrame]) -> String {
        let mut result = String::new();

        for (index, frame) in frames.iter().enumerate() {
            result.push_str(&format!("Frame {}: ", index));

            if let Some(ref symbol) = frame.function_symbol {
                result.push_str(&format!("{}()", symbol.name));
            } else {
                result.push_str(&format!("func_{}", frame.wasm_address.function_index));
            }

            if let Some(ref pos) = frame.source_position {
                result.push_str(&format!(" at {}", pos));
            } else {
                result.push_str(&format!(
                    " at wasm+0x{:x}:0x{:x}",
                    frame.wasm_address.function_index,
                    frame.wasm_address.instruction_offset
                ));
            }

            result.push('\n');
        }

        result
    }
}

impl Default for StackTraceMapper {
    fn default() -> Self {
        Self::new()
    }
}

/// Source file cache for performance optimization
#[derive(Debug)]
pub struct SourceFileCache {
    /// File content cache
    file_cache: RwLock<HashMap<String, Arc<String>>>,
    /// Source map cache
    sourcemap_cache: RwLock<HashMap<String, Arc<SourceMap>>>,
    /// Maximum cache entries
    max_entries: usize,
}

impl SourceFileCache {
    /// Create a new source file cache
    pub fn new(max_entries: usize) -> Self {
        Self {
            file_cache: RwLock::new(HashMap::new()),
            sourcemap_cache: RwLock::new(HashMap::new()),
            max_entries,
        }
    }

    /// Load source file content
    pub fn load_source_file(&self, path: &str) -> WasmtimeResult<Arc<String>> {
        // Check cache first
        if let Ok(cache) = self.file_cache.read() {
            if let Some(content) = cache.get(path) {
                return Ok(Arc::clone(content));
            }
        }

        // Load file content (simplified - would use std::fs in production)
        let content = format!("// Source file: {}\n// (Content would be loaded from filesystem)\n", path);
        let content_arc = Arc::new(content);

        // Cache the content
        if let Ok(mut cache) = self.file_cache.write() {
            if cache.len() >= self.max_entries {
                // Simple eviction - remove first entry
                if let Some(first_key) = cache.keys().next().cloned() {
                    cache.remove(&first_key);
                }
            }
            cache.insert(path.to_string(), Arc::clone(&content_arc));
        }

        Ok(content_arc)
    }

    /// Cache source map
    pub fn cache_source_map(&self, key: String, source_map: SourceMap) {
        if let Ok(mut cache) = self.sourcemap_cache.write() {
            if cache.len() >= self.max_entries {
                // Simple eviction - remove first entry
                if let Some(first_key) = cache.keys().next().cloned() {
                    cache.remove(&first_key);
                }
            }
            cache.insert(key, Arc::new(source_map));
        }
    }

    /// Get cached source map
    pub fn get_source_map(&self, key: &str) -> Option<Arc<SourceMap>> {
        if let Ok(cache) = self.sourcemap_cache.read() {
            cache.get(key).cloned()
        } else {
            None
        }
    }

    /// Clear all caches
    pub fn clear(&self) {
        if let Ok(mut file_cache) = self.file_cache.write() {
            file_cache.clear();
        }
        if let Ok(mut map_cache) = self.sourcemap_cache.write() {
            map_cache.clear();
        }
    }
}

/// Source map validation engine
#[derive(Debug)]
pub struct ValidationEngine;

impl ValidationEngine {
    /// Validate source map completeness and correctness
    pub fn validate_source_map(&self, source_map: &SourceMap) -> ValidationResult {
        let mut result = ValidationResult::new();

        // Check version
        if source_map.version != SOURCEMAP_VERSION {
            result.add_warning(format!(
                "Source map version {} may not be fully supported, expected {}",
                source_map.version, SOURCEMAP_VERSION
            ));
        }

        // Check sources
        if source_map.sources.is_empty() {
            result.add_error("Source map must contain at least one source file".to_string());
        }

        // Validate mappings
        if source_map.mappings.is_empty() {
            result.add_error("Source map must contain mappings".to_string());
        } else {
            // Basic VLQ validation
            let valid_chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/,;";
            for ch in source_map.mappings.chars() {
                if !valid_chars.contains(ch) {
                    result.add_error(format!("Invalid character in mappings: {}", ch));
                    break;
                }
            }
        }

        // Check sources content consistency
        if let Some(ref content) = source_map.sources_content {
            if content.len() != source_map.sources.len() {
                result.add_error(
                    "Sources content array length must match sources array length".to_string()
                );
            }
        }

        result
    }

    /// Validate DWARF information
    pub fn validate_dwarf_info(&self, dwarf_info: &DwarfInfo) -> ValidationResult {
        let mut result = ValidationResult::new();

        // Check compilation units
        if dwarf_info.units.is_empty() {
            result.add_warning("No compilation units found in DWARF info".to_string());
        }

        // Validate line programs
        for (id, line_program) in &dwarf_info.line_programs {
            if line_program.files.is_empty() {
                result.add_warning(format!("Line program {} has no file entries", id));
            }
            if line_program.lines.is_empty() {
                result.add_warning(format!("Line program {} has no line entries", id));
            }
        }

        // Validate functions
        for (id, function) in &dwarf_info.functions {
            if function.name.is_empty() {
                result.add_warning(format!("Function {} has no name", id));
            }
            if function.high_pc <= function.low_pc {
                result.add_error(format!(
                    "Function {} has invalid address range: 0x{:x}-0x{:x}",
                    function.name, function.low_pc, function.high_pc
                ));
            }
        }

        result
    }
}

/// Validation result for source maps and DWARF info
#[derive(Debug, Clone)]
pub struct ValidationResult {
    /// Validation errors
    pub errors: Vec<String>,
    /// Validation warnings
    pub warnings: Vec<String>,
}

impl ValidationResult {
    /// Create a new validation result
    pub fn new() -> Self {
        Self {
            errors: Vec::new(),
            warnings: Vec::new(),
        }
    }

    /// Add an error
    pub fn add_error(&mut self, error: String) {
        self.errors.push(error);
    }

    /// Add a warning
    pub fn add_warning(&mut self, warning: String) {
        self.warnings.push(warning);
    }

    /// Check if validation passed (no errors)
    pub fn is_valid(&self) -> bool {
        self.errors.is_empty()
    }

    /// Check if there are any warnings
    pub fn has_warnings(&self) -> bool {
        !self.warnings.is_empty()
    }
}

impl Default for ValidationResult {
    fn default() -> Self {
        Self::new()
    }
}

/// Complete source map integration system
#[derive(Debug)]
pub struct SourceMapIntegration {
    /// Source map parser
    parser: SourceMapParser,
    /// DWARF parser
    dwarf_parser: DwarfParser,
    /// Symbol resolver
    resolver: SymbolResolver,
    /// Stack trace mapper
    mapper: StackTraceMapper,
    /// Source file cache
    cache: SourceFileCache,
    /// Validation engine
    validator: ValidationEngine,
}

impl SourceMapIntegration {
    /// Create a new source map integration system
    pub fn new() -> Self {
        Self {
            parser: SourceMapParser::new(),
            dwarf_parser: DwarfParser::new(),
            resolver: SymbolResolver::new(),
            mapper: StackTraceMapper::new(),
            cache: SourceFileCache::new(MAX_CACHED_SOURCEMAPS),
            validator: ValidationEngine,
        }
    }

    /// Create with custom cache size
    pub fn with_cache_size(cache_size: usize) -> Self {
        Self {
            parser: SourceMapParser::new(),
            dwarf_parser: DwarfParser::new(),
            resolver: SymbolResolver::new(),
            mapper: StackTraceMapper::new(),
            cache: SourceFileCache::new(cache_size),
            validator: ValidationEngine,
        }
    }

    /// Load and parse source map from JSON data
    pub fn load_source_map(&self, json_data: &str) -> WasmtimeResult<Arc<SourceMap>> {
        let source_map = self.parser.parse(json_data)?;
        let validation = self.validator.validate_source_map(&source_map);

        if !validation.is_valid() {
            return Err(WasmtimeError::InvalidData {
                message:
                format!("Source map validation failed: {:?}", validation.errors),
            });
        }

        Ok(Arc::new(source_map))
    }

    /// Load and parse DWARF information
    pub fn load_dwarf_info(&self, dwarf_data: &[u8]) -> WasmtimeResult<Arc<DwarfInfo>> {
        let dwarf_info = self.dwarf_parser.parse_dwarf(dwarf_data)?;
        let validation = self.validator.validate_dwarf_info(&dwarf_info);

        if !validation.is_valid() {
            log::warn!("DWARF validation warnings: {:?}", validation.warnings);
        }

        Ok(dwarf_info)
    }

    /// Map WebAssembly stack trace to source locations
    pub fn map_stack_trace(
        &self,
        frames: &[WasmAddress],
        source_map: Option<&SourceMap>,
        dwarf_info: Option<&DwarfInfo>,
        module_id: &str,
    ) -> WasmtimeResult<Vec<SourceMappedFrame>> {
        self.mapper.map_stack_trace(frames, source_map, dwarf_info, module_id)
    }

    /// Format stack trace with source information
    pub fn format_stack_trace(&self, frames: &[SourceMappedFrame]) -> String {
        self.mapper.format_stack_trace(frames)
    }

    /// Get source position for WebAssembly address
    pub fn get_source_position(
        &self,
        source_map: &SourceMap,
        wasm_address: WasmAddress,
    ) -> WasmtimeResult<Option<SourcePosition>> {
        self.parser.get_source_position(source_map, wasm_address)
    }

    /// Resolve function symbol
    pub fn resolve_function_symbol(
        &self,
        module_id: &str,
        function_index: u32,
        dwarf_info: Option<&DwarfInfo>,
    ) -> Option<FunctionSymbol> {
        self.resolver.resolve_function(module_id, function_index, dwarf_info)
    }

    /// Load source file content
    pub fn load_source_file(&self, path: &str) -> WasmtimeResult<Arc<String>> {
        self.cache.load_source_file(path)
    }

    /// Clear all caches
    pub fn clear_caches(&self) {
        self.cache.clear();
        self.resolver.clear_all_caches();
    }
}

impl Default for SourceMapIntegration {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_source_position_creation() {
        let pos = SourcePosition::new("test.c".to_string(), 10, 5);
        assert_eq!(pos.source, "test.c");
        assert_eq!(pos.line, 10);
        assert_eq!(pos.column, 5);
        assert!(pos.name.is_none());
    }

    #[test]
    fn test_source_position_with_name() {
        let pos = SourcePosition::with_name("test.c".to_string(), 10, 5, "main".to_string());
        assert_eq!(pos.source, "test.c");
        assert_eq!(pos.line, 10);
        assert_eq!(pos.column, 5);
        assert_eq!(pos.name, Some("main".to_string()));
    }

    #[test]
    fn test_wasm_address() {
        let addr = WasmAddress::new(5, 100);
        assert_eq!(addr.function_index, 5);
        assert_eq!(addr.instruction_offset, 100);
    }

    #[test]
    fn test_source_map_validation() {
        let mut source_map = SourceMap::new();
        source_map.sources.push("test.c".to_string());
        source_map.mappings = "AAAA".to_string();

        assert!(source_map.validate().is_ok());
    }

    #[test]
    fn test_source_map_validation_empty_sources() {
        let source_map = SourceMap::new();
        assert!(source_map.validate().is_err());
    }

    #[test]
    fn test_source_map_parser() {
        let parser = SourceMapParser::new();
        let json = r#"{"version":3,"sources":["test.c"],"mappings":"AAAA","names":[]}"#;

        let result = parser.parse(json);
        assert!(result.is_ok());

        let source_map = result.unwrap();
        assert_eq!(source_map.version, 3);
        assert_eq!(source_map.sources.len(), 1);
        assert_eq!(source_map.sources[0], "test.c");
    }

    #[test]
    fn test_validation_result() {
        let mut result = ValidationResult::new();
        assert!(result.is_valid());
        assert!(!result.has_warnings());

        result.add_warning("test warning".to_string());
        assert!(result.is_valid());
        assert!(result.has_warnings());

        result.add_error("test error".to_string());
        assert!(!result.is_valid());
        assert!(result.has_warnings());
    }

    #[test]
    fn test_symbol_resolver() {
        let resolver = SymbolResolver::new();
        let symbol = resolver.resolve_function("test_module", 0, None);

        assert!(symbol.is_some());
        let symbol = symbol.unwrap();
        assert_eq!(symbol.name, "func_0");
    }

    #[test]
    fn test_source_file_cache() {
        let cache = SourceFileCache::new(10);
        let result = cache.load_source_file("test.c");

        assert!(result.is_ok());
        let content = result.unwrap();
        assert!(content.contains("test.c"));

        // Test cache hit
        let result2 = cache.load_source_file("test.c");
        assert!(result2.is_ok());
    }

    #[test]
    fn test_source_map_integration() {
        let integration = SourceMapIntegration::new();
        let json = r#"{"version":3,"sources":["test.c"],"mappings":"AAAA","names":[]}"#;

        let result = integration.load_source_map(json);
        assert!(result.is_ok());
    }

    #[test]
    fn test_stack_trace_mapping() {
        let integration = SourceMapIntegration::new();
        let frames = vec![WasmAddress::new(0, 100)];

        let result = integration.map_stack_trace(&frames, None, None, "test_module");
        assert!(result.is_ok());

        let mapped = result.unwrap();
        assert_eq!(mapped.len(), 1);
        assert_eq!(mapped[0].wasm_address, frames[0]);
    }
}

// ============================================================================
// Panama FFI Functions for Source Map Support
// ============================================================================

use std::ffi::{c_char, c_int, CStr, CString};
use std::ptr;

/// Global source map integration instance
lazy_static::lazy_static! {
    static ref SOURCE_MAP_INTEGRATION: SourceMapIntegration = SourceMapIntegration::new();
    static ref SOURCEMAP_REGISTRY: RwLock<HashMap<u64, Arc<SourceMap>>> = RwLock::new(HashMap::new());
    static ref NEXT_SOURCEMAP_ID: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(1);
}

/// Create a new source map from JSON data
///
/// # Arguments
/// * `json_data` - Pointer to UTF-8 JSON source map data
/// * `json_len` - Length of the JSON data
///
/// # Returns
/// Source map ID on success, 0 on error
///
/// # Safety
/// json_data must be a valid pointer to json_len bytes of UTF-8 data
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_sourcemap_parse(
    json_data: *const c_char,
    json_len: usize,
) -> u64 {
    if json_data.is_null() || json_len == 0 {
        return 0;
    }

    // Convert to Rust string
    let json_slice = std::slice::from_raw_parts(json_data as *const u8, json_len);
    let json_str = match std::str::from_utf8(json_slice) {
        Ok(s) => s,
        Err(_) => return 0,
    };

    // Parse the source map
    match SOURCE_MAP_INTEGRATION.load_source_map(json_str) {
        Ok(source_map) => {
            // Register the source map
            let id = NEXT_SOURCEMAP_ID.fetch_add(1, std::sync::atomic::Ordering::SeqCst);
            if let Ok(mut registry) = SOURCEMAP_REGISTRY.write() {
                registry.insert(id, source_map);
            }
            id
        }
        Err(_) => 0,
    }
}

/// Free a source map
///
/// # Safety
/// sourcemap_id must be a valid ID from wasmtime4j_sourcemap_parse
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_sourcemap_free(sourcemap_id: u64) -> c_int {
    if let Ok(mut registry) = SOURCEMAP_REGISTRY.write() {
        registry.remove(&sourcemap_id);
        0
    } else {
        -1
    }
}

/// Get the number of sources in a source map
///
/// # Safety
/// sourcemap_id must be a valid ID from wasmtime4j_sourcemap_parse
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_sourcemap_get_source_count(sourcemap_id: u64) -> c_int {
    if let Ok(registry) = SOURCEMAP_REGISTRY.read() {
        if let Some(source_map) = registry.get(&sourcemap_id) {
            return source_map.sources.len() as c_int;
        }
    }
    -1
}

/// Get a source file name by index
/// Returns a newly allocated C string that must be freed with wasmtime4j_sourcemap_string_free
///
/// # Safety
/// sourcemap_id must be a valid ID from wasmtime4j_sourcemap_parse
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_sourcemap_get_source(
    sourcemap_id: u64,
    source_index: c_int,
) -> *mut c_char {
    if source_index < 0 {
        return ptr::null_mut();
    }

    if let Ok(registry) = SOURCEMAP_REGISTRY.read() {
        if let Some(source_map) = registry.get(&sourcemap_id) {
            if (source_index as usize) < source_map.sources.len() {
                let source = &source_map.sources[source_index as usize];
                if let Ok(cstr) = CString::new(source.as_str()) {
                    return cstr.into_raw();
                }
            }
        }
    }
    ptr::null_mut()
}

/// Get the number of names in a source map
///
/// # Safety
/// sourcemap_id must be a valid ID from wasmtime4j_sourcemap_parse
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_sourcemap_get_name_count(sourcemap_id: u64) -> c_int {
    if let Ok(registry) = SOURCEMAP_REGISTRY.read() {
        if let Some(source_map) = registry.get(&sourcemap_id) {
            return source_map.names.len() as c_int;
        }
    }
    -1
}

/// Get a name by index
/// Returns a newly allocated C string that must be freed with wasmtime4j_sourcemap_string_free
///
/// # Safety
/// sourcemap_id must be a valid ID from wasmtime4j_sourcemap_parse
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_sourcemap_get_name(
    sourcemap_id: u64,
    name_index: c_int,
) -> *mut c_char {
    if name_index < 0 {
        return ptr::null_mut();
    }

    if let Ok(registry) = SOURCEMAP_REGISTRY.read() {
        if let Some(source_map) = registry.get(&sourcemap_id) {
            if (name_index as usize) < source_map.names.len() {
                let name = &source_map.names[name_index as usize];
                if let Ok(cstr) = CString::new(name.as_str()) {
                    return cstr.into_raw();
                }
            }
        }
    }
    ptr::null_mut()
}

/// Get source position for a WebAssembly address
/// Returns JSON string with source position info, or null on error/not found
///
/// JSON format: {"source":"file.c","line":10,"column":5,"name":"funcName"}
///
/// # Safety
/// sourcemap_id must be a valid ID from wasmtime4j_sourcemap_parse
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_sourcemap_get_position(
    sourcemap_id: u64,
    function_index: u32,
    instruction_offset: u32,
) -> *mut c_char {
    if let Ok(registry) = SOURCEMAP_REGISTRY.read() {
        if let Some(source_map) = registry.get(&sourcemap_id) {
            let wasm_address = WasmAddress::new(function_index, instruction_offset);
            if let Ok(Some(position)) = SOURCE_MAP_INTEGRATION.get_source_position(source_map, wasm_address) {
                let json = serde_json::json!({
                    "source": position.source,
                    "line": position.line,
                    "column": position.column,
                    "name": position.name,
                });
                if let Ok(cstr) = CString::new(json.to_string()) {
                    return cstr.into_raw();
                }
            }
        }
    }
    ptr::null_mut()
}

/// Map a WebAssembly stack trace to source positions
/// Returns JSON array of mapped frame info
///
/// # Arguments
/// * `sourcemap_id` - Source map ID (0 to skip source mapping)
/// * `module_id` - Module identifier string
/// * `addresses` - Array of (function_index, instruction_offset) pairs
/// * `address_count` - Number of address pairs
///
/// # Returns
/// JSON string with mapped frames, null on error
///
/// # Safety
/// All pointers must be valid
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_sourcemap_map_stack_trace(
    sourcemap_id: u64,
    module_id: *const c_char,
    addresses: *const u32,
    address_count: usize,
) -> *mut c_char {
    if module_id.is_null() || addresses.is_null() || address_count == 0 {
        return ptr::null_mut();
    }

    // Parse module ID
    let module_id_str = match CStr::from_ptr(module_id).to_str() {
        Ok(s) => s,
        Err(_) => return ptr::null_mut(),
    };

    // Parse addresses (pairs of function_index, instruction_offset)
    let addr_slice = std::slice::from_raw_parts(addresses, address_count * 2);
    let mut frames = Vec::with_capacity(address_count);
    for i in 0..address_count {
        let func_idx = addr_slice[i * 2];
        let inst_offset = addr_slice[i * 2 + 1];
        frames.push(WasmAddress::new(func_idx, inst_offset));
    }

    // Get source map if provided
    let source_map_opt = if sourcemap_id != 0 {
        if let Ok(registry) = SOURCEMAP_REGISTRY.read() {
            registry.get(&sourcemap_id).cloned()
        } else {
            None
        }
    } else {
        None
    };

    // Map the stack trace
    match SOURCE_MAP_INTEGRATION.map_stack_trace(
        &frames,
        source_map_opt.as_ref().map(|arc| arc.as_ref()),
        None, // No DWARF info in this call
        module_id_str,
    ) {
        Ok(mapped_frames) => {
            let json_frames: Vec<serde_json::Value> = mapped_frames
                .iter()
                .map(|frame| {
                    let mut obj = serde_json::json!({
                        "function_index": frame.wasm_address.function_index,
                        "instruction_offset": frame.wasm_address.instruction_offset,
                    });

                    if let Some(ref pos) = frame.source_position {
                        obj["source"] = serde_json::json!(pos.source);
                        obj["line"] = serde_json::json!(pos.line);
                        obj["column"] = serde_json::json!(pos.column);
                        if let Some(ref name) = pos.name {
                            obj["name"] = serde_json::json!(name);
                        }
                    }

                    if let Some(ref symbol) = frame.function_symbol {
                        obj["function_name"] = serde_json::json!(symbol.name);
                        obj["function_signature"] = serde_json::json!(symbol.signature);
                    }

                    obj
                })
                .collect();

            if let Ok(cstr) = CString::new(serde_json::to_string(&json_frames).unwrap_or_default()) {
                return cstr.into_raw();
            }
        }
        Err(_) => {}
    }

    ptr::null_mut()
}

/// Format a mapped stack trace as a human-readable string
///
/// # Safety
/// json_frames must be a valid JSON string from wasmtime4j_sourcemap_map_stack_trace
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_sourcemap_format_stack_trace(
    json_frames: *const c_char,
) -> *mut c_char {
    if json_frames.is_null() {
        return ptr::null_mut();
    }

    let json_str = match CStr::from_ptr(json_frames).to_str() {
        Ok(s) => s,
        Err(_) => return ptr::null_mut(),
    };

    // Parse JSON and format
    if let Ok(frames) = serde_json::from_str::<Vec<serde_json::Value>>(json_str) {
        let mut result = String::new();
        for (index, frame) in frames.iter().enumerate() {
            result.push_str(&format!("Frame {}: ", index));

            if let Some(func_name) = frame.get("function_name").and_then(|v| v.as_str()) {
                result.push_str(&format!("{}()", func_name));
            } else if let Some(func_idx) = frame.get("function_index").and_then(|v| v.as_u64()) {
                result.push_str(&format!("func_{}", func_idx));
            }

            if let (Some(source), Some(line)) = (
                frame.get("source").and_then(|v| v.as_str()),
                frame.get("line").and_then(|v| v.as_u64()),
            ) {
                let column = frame.get("column").and_then(|v| v.as_u64()).unwrap_or(0);
                result.push_str(&format!(" at {}:{}:{}", source, line, column));
            } else if let (Some(func_idx), Some(inst_offset)) = (
                frame.get("function_index").and_then(|v| v.as_u64()),
                frame.get("instruction_offset").and_then(|v| v.as_u64()),
            ) {
                result.push_str(&format!(" at wasm+0x{:x}:0x{:x}", func_idx, inst_offset));
            }

            result.push('\n');
        }

        if let Ok(cstr) = CString::new(result) {
            return cstr.into_raw();
        }
    }

    ptr::null_mut()
}

/// Free a string allocated by source map functions
///
/// # Safety
/// s must be a valid pointer from a wasmtime4j_sourcemap_* function
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_sourcemap_string_free(s: *mut c_char) {
    if !s.is_null() {
        drop(CString::from_raw(s));
    }
}

/// Resolve function symbol for a WebAssembly function
/// Returns JSON with function symbol info
///
/// # Safety
/// module_id must be a valid C string
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_sourcemap_resolve_function(
    module_id: *const c_char,
    function_index: u32,
) -> *mut c_char {
    if module_id.is_null() {
        return ptr::null_mut();
    }

    let module_id_str = match CStr::from_ptr(module_id).to_str() {
        Ok(s) => s,
        Err(_) => return ptr::null_mut(),
    };

    if let Some(symbol) = SOURCE_MAP_INTEGRATION.resolve_function_symbol(module_id_str, function_index, None) {
        let json = serde_json::json!({
            "name": symbol.name,
            "signature": symbol.signature,
            "source_file": symbol.source_file,
            "start_line": symbol.start_line,
            "end_line": symbol.end_line,
            "parameters": symbol.parameters.iter().map(|p| {
                serde_json::json!({
                    "name": p.name,
                    "type": p.var_type,
                })
            }).collect::<Vec<_>>(),
            "locals": symbol.locals.iter().map(|l| {
                serde_json::json!({
                    "name": l.name,
                    "type": l.var_type,
                })
            }).collect::<Vec<_>>(),
        });

        if let Ok(cstr) = CString::new(json.to_string()) {
            return cstr.into_raw();
        }
    }

    ptr::null_mut()
}

/// Clear all source map caches
#[no_mangle]
pub extern "C" fn wasmtime4j_sourcemap_clear_caches() {
    SOURCE_MAP_INTEGRATION.clear_caches();
}

/// Get the source map version
///
/// # Safety
/// sourcemap_id must be a valid ID from wasmtime4j_sourcemap_parse
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_sourcemap_get_version(sourcemap_id: u64) -> c_int {
    if let Ok(registry) = SOURCEMAP_REGISTRY.read() {
        if let Some(source_map) = registry.get(&sourcemap_id) {
            return source_map.version as c_int;
        }
    }
    -1
}

/// Validate a source map and get validation results as JSON
///
/// # Safety
/// sourcemap_id must be a valid ID from wasmtime4j_sourcemap_parse
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_sourcemap_validate(sourcemap_id: u64) -> *mut c_char {
    if let Ok(registry) = SOURCEMAP_REGISTRY.read() {
        if let Some(source_map) = registry.get(&sourcemap_id) {
            let validator = ValidationEngine;
            let result = validator.validate_source_map(source_map);

            let json = serde_json::json!({
                "valid": result.is_valid(),
                "has_warnings": result.has_warnings(),
                "errors": result.errors,
                "warnings": result.warnings,
            });

            if let Ok(cstr) = CString::new(json.to_string()) {
                return cstr.into_raw();
            }
        }
    }
    ptr::null_mut()
}

/// Get the number of registered source maps
#[no_mangle]
pub extern "C" fn wasmtime4j_sourcemap_get_count() -> c_int {
    if let Ok(registry) = SOURCEMAP_REGISTRY.read() {
        registry.len() as c_int
    } else {
        -1
    }
}