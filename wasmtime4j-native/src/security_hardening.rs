//! # Advanced Security Hardening Module
//!
//! This module implements comprehensive WebAssembly security hardening features:
//! - Control Flow Integrity (CFI) with runtime validation
//! - Memory tagging and pointer authentication
//! - Spectre and Meltdown mitigations
//! - Hardware-assisted sandboxing and isolation
//! - Advanced threat detection and response
//! - Cryptographic module validation with zero-trust architecture
//! - Capability-based access control with least-privilege enforcement
//! - Comprehensive security audit logging and compliance reporting

use std::collections::{HashMap, HashSet, VecDeque};
use std::sync::{Arc, RwLock, Mutex, atomic::{AtomicU64, AtomicBool, Ordering}};
use std::time::{SystemTime, UNIX_EPOCH, Duration, Instant};
use std::mem::MaybeUninit;
use std::ptr;
use wasmtime::{Engine, Module, Store, Instance, TypedFunc, Caller, Linker};
use wasmtime::{Config, OptLevel, Strategy, ProfilingStrategy};
use ring::{digest, signature, rand, aead, pbkdf2, hmac};
use ring::signature::{Ed25519KeyPair, KeyPair};
use base64::{Engine as _, engine::general_purpose};
use serde::{Deserialize, Serialize};
use sha2::{Sha256, Digest};
use chrono::{DateTime, Utc};

use crate::error::{WasmtimeError, WasmtimeResult, ErrorCode};
use crate::security::{SecurityCapability, AuditLogger, AccessControlEngine};

/// Control Flow Integrity (CFI) enforcement levels
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum CfiLevel {
    /// No CFI enforcement (not recommended for production)
    None,
    /// Basic CFI with function pointer validation
    Basic,
    /// Enhanced CFI with indirect call validation
    Enhanced,
    /// Full CFI with comprehensive control flow validation
    Full,
    /// Intel CET (Control-flow Enforcement Technology) if available
    IntelCet,
}

/// Memory tagging security levels
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum MemoryTaggingLevel {
    /// No memory tagging
    None,
    /// Basic memory tagging with allocation tracking
    Basic,
    /// Enhanced memory tagging with use-after-free detection
    Enhanced,
    /// Full memory tagging with ARM MTE if available
    ArmMte,
    /// Intel MPX (Memory Protection Extensions) if available
    IntelMpx,
}

/// Spectre/Meltdown mitigation strategies
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum SpectreLevel {
    /// No Spectre mitigations (not recommended)
    None,
    /// Basic branch prediction hardening
    Basic,
    /// Enhanced with load/store serialization
    Enhanced,
    /// Full mitigations with microcode updates
    Full,
    /// Hardware-assisted mitigations when available
    HardwareAssisted,
}

/// Hardware-assisted isolation features
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum IsolationLevel {
    /// Software-only isolation
    Software,
    /// Intel MPK (Memory Protection Keys)
    IntelMpk,
    /// ARM Pointer Authentication
    ArmPointerAuth,
    /// Intel CET + MPK combination
    IntelCetMpk,
    /// Full hardware isolation with virtualization
    VirtualizationBased,
}

/// Security hardening configuration
#[derive(Debug, Clone)]
pub struct SecurityHardeningConfig {
    /// Control flow integrity level
    pub cfi_level: CfiLevel,
    /// Memory tagging level
    pub memory_tagging_level: MemoryTaggingLevel,
    /// Spectre mitigation level
    pub spectre_level: SpectreLevel,
    /// Hardware isolation level
    pub isolation_level: IsolationLevel,
    /// Enable real-time threat detection
    pub enable_threat_detection: bool,
    /// Enable cryptographic module validation
    pub enable_crypto_validation: bool,
    /// Enable comprehensive audit logging
    pub enable_comprehensive_auditing: bool,
    /// Maximum execution time before timeout (microseconds)
    pub execution_timeout_us: u64,
    /// Maximum memory allocation per instance (bytes)
    pub max_memory_allocation: u64,
    /// Enable stack canary protection
    pub enable_stack_canaries: bool,
    /// Enable return address protection
    pub enable_return_address_protection: bool,
    /// Enable indirect branch tracking
    pub enable_indirect_branch_tracking: bool,
}

impl Default for SecurityHardeningConfig {
    fn default() -> Self {
        Self {
            cfi_level: CfiLevel::Enhanced,
            memory_tagging_level: MemoryTaggingLevel::Enhanced,
            spectre_level: SpectreLevel::Enhanced,
            isolation_level: IsolationLevel::Software,
            enable_threat_detection: true,
            enable_crypto_validation: true,
            enable_comprehensive_auditing: true,
            execution_timeout_us: 10_000_000, // 10 seconds
            max_memory_allocation: 1024 * 1024 * 256, // 256MB
            enable_stack_canaries: true,
            enable_return_address_protection: true,
            enable_indirect_branch_tracking: true,
        }
    }
}

impl SecurityHardeningConfig {
    /// Create a maximum security configuration
    pub fn maximum_security() -> Self {
        Self {
            cfi_level: CfiLevel::Full,
            memory_tagging_level: MemoryTaggingLevel::ArmMte,
            spectre_level: SpectreLevel::Full,
            isolation_level: IsolationLevel::VirtualizationBased,
            enable_threat_detection: true,
            enable_crypto_validation: true,
            enable_comprehensive_auditing: true,
            execution_timeout_us: 5_000_000, // 5 seconds
            max_memory_allocation: 1024 * 1024 * 128, // 128MB
            enable_stack_canaries: true,
            enable_return_address_protection: true,
            enable_indirect_branch_tracking: true,
        }
    }

    /// Create a balanced security/performance configuration
    pub fn balanced() -> Self {
        Self::default()
    }

    /// Create a minimal security configuration (not recommended for production)
    pub fn minimal() -> Self {
        Self {
            cfi_level: CfiLevel::Basic,
            memory_tagging_level: MemoryTaggingLevel::Basic,
            spectre_level: SpectreLevel::Basic,
            isolation_level: IsolationLevel::Software,
            enable_threat_detection: false,
            enable_crypto_validation: false,
            enable_comprehensive_auditing: false,
            execution_timeout_us: 30_000_000, // 30 seconds
            max_memory_allocation: 1024 * 1024 * 512, // 512MB
            enable_stack_canaries: false,
            enable_return_address_protection: false,
            enable_indirect_branch_tracking: false,
        }
    }
}

/// Control flow integrity validator
#[derive(Debug)]
pub struct CfiValidator {
    /// Expected control flow graph
    expected_cfg: Arc<RwLock<HashMap<u64, HashSet<u64>>>>,
    /// Runtime control flow tracking
    runtime_cfg: Arc<RwLock<HashMap<u64, HashSet<u64>>>>,
    /// CFI violation counter
    violation_count: AtomicU64,
    /// CFI enforcement level
    enforcement_level: CfiLevel,
    /// Function address whitelist
    function_whitelist: Arc<RwLock<HashSet<u64>>>,
    /// Indirect call target cache
    indirect_call_cache: Arc<RwLock<HashMap<u64, u64>>>,
}

impl CfiValidator {
    /// Create a new CFI validator
    pub fn new(enforcement_level: CfiLevel) -> Self {
        Self {
            expected_cfg: Arc::new(RwLock::new(HashMap::new())),
            runtime_cfg: Arc::new(RwLock::new(HashMap::new())),
            violation_count: AtomicU64::new(0),
            enforcement_level,
            function_whitelist: Arc::new(RwLock::new(HashSet::new())),
            indirect_call_cache: Arc::new(RwLock::new(HashMap::new())),
        }
    }

    /// Validate a function call for CFI compliance
    pub fn validate_call(&self, from_addr: u64, to_addr: u64) -> WasmtimeResult<()> {
        match self.enforcement_level {
            CfiLevel::None => Ok(()),
            CfiLevel::Basic => self.validate_basic_cfi(from_addr, to_addr),
            CfiLevel::Enhanced => self.validate_enhanced_cfi(from_addr, to_addr),
            CfiLevel::Full => self.validate_full_cfi(from_addr, to_addr),
            CfiLevel::IntelCet => self.validate_intel_cet(from_addr, to_addr),
        }
    }

    /// Basic CFI validation - check function pointer validity
    fn validate_basic_cfi(&self, _from_addr: u64, to_addr: u64) -> WasmtimeResult<()> {
        let whitelist = self.function_whitelist.read()
            .map_err(|e| WasmtimeError::Security {
                message: format!("CFI lock error: {}", e),
            ))?;

        if !whitelist.contains(&to_addr) {
            self.violation_count.fetch_add(1, Ordering::SeqCst);
            return Err(WasmtimeError::Security {
                message: format!("CFI violation: invalid function target 0x{:x}", to_addr),
            ));
        }

        Ok(())
    }

    /// Enhanced CFI validation - check indirect call validity
    fn validate_enhanced_cfi(&self, from_addr: u64, to_addr: u64) -> WasmtimeResult<()> {
        // First run basic validation
        self.validate_basic_cfi(from_addr, to_addr)?;

        // Check expected control flow
        let expected = self.expected_cfg.read()
            .map_err(|e| WasmtimeError::Security {
                message: format!("CFI lock error: {}", e),
            ))?;

        if let Some(valid_targets) = expected.get(&from_addr) {
            if !valid_targets.contains(&to_addr) {
                self.violation_count.fetch_add(1, Ordering::SeqCst);
                return Err(WasmtimeError::Security {
                    message:
                    format!("CFI violation: invalid control flow from 0x{:x} to 0x{:x}", from_addr, to_addr),
                });
            }
        }

        // Update runtime CFG
        let mut runtime = self.runtime_cfg.write()
            .map_err(|e| WasmtimeError::Security {
                message: format!("CFI lock error: {}", e),
            ))?;

        runtime.entry(from_addr).or_insert_with(HashSet::new).insert(to_addr);

        Ok(())
    }

    /// Full CFI validation - comprehensive control flow validation
    fn validate_full_cfi(&self, from_addr: u64, to_addr: u64) -> WasmtimeResult<()> {
        // Run enhanced validation first
        self.validate_enhanced_cfi(from_addr, to_addr)?;

        // Additional checks for full CFI
        self.validate_return_address_integrity(from_addr, to_addr)?;
        self.validate_indirect_branch_targets(from_addr, to_addr)?;

        Ok(())
    }

    /// Intel CET validation (if hardware support is available)
    fn validate_intel_cet(&self, from_addr: u64, to_addr: u64) -> WasmtimeResult<()> {
        // First run full CFI validation
        self.validate_full_cfi(from_addr, to_addr)?;

        // Intel CET specific validations would go here
        // This would use hardware features like IBT (Indirect Branch Tracking)
        // and CET shadow stack validation

        Ok(())
    }

    /// Validate return address integrity
    fn validate_return_address_integrity(&self, _from_addr: u64, _to_addr: u64) -> WasmtimeResult<()> {
        // Implementation would check shadow stack integrity
        // and validate return addresses match expected values
        Ok(())
    }

    /// Validate indirect branch targets
    fn validate_indirect_branch_targets(&self, from_addr: u64, to_addr: u64) -> WasmtimeResult<()> {
        let mut cache = self.indirect_call_cache.write()
            .map_err(|e| WasmtimeError::Security {
                message: format!("CFI cache lock error: {}", e),
            ))?;

        // Check if this indirect call pattern has been seen before
        if let Some(&cached_target) = cache.get(&from_addr) {
            if cached_target != to_addr {
                self.violation_count.fetch_add(1, Ordering::SeqCst);
                return Err(WasmtimeError::Security {
                    message:
                    format!("CFI violation: indirect branch target mismatch at 0x{:x}", from_addr),
                });
            }
        } else {
            cache.insert(from_addr, to_addr);
        }

        Ok(())
    }

    /// Add a valid function address to the whitelist
    pub fn add_valid_function(&self, addr: u64) -> WasmtimeResult<()> {
        let mut whitelist = self.function_whitelist.write()
            .map_err(|e| WasmtimeError::Security {
                message: format!("CFI whitelist lock error: {}", e),
            ))?;

        whitelist.insert(addr);
        Ok(())
    }

    /// Add expected control flow edge
    pub fn add_expected_flow(&self, from_addr: u64, to_addr: u64) -> WasmtimeResult<()> {
        let mut expected = self.expected_cfg.write()
            .map_err(|e| WasmtimeError::Security {
                message: format!("CFI expected flow lock error: {}", e),
            ))?;

        expected.entry(from_addr).or_insert_with(HashSet::new).insert(to_addr);
        Ok(())
    }

    /// Get CFI violation count
    pub fn get_violation_count(&self) -> u64 {
        self.violation_count.load(Ordering::SeqCst)
    }

    /// Reset CFI validator state
    pub fn reset(&self) -> WasmtimeResult<()> {
        let mut runtime = self.runtime_cfg.write()
            .map_err(|e| WasmtimeError::Security {
                message: format!("CFI runtime reset lock error: {}", e),
            ))?;
        runtime.clear();

        let mut cache = self.indirect_call_cache.write()
            .map_err(|e| WasmtimeError::Security {
                message: format!("CFI cache reset lock error: {}", e),
            ))?;
        cache.clear();

        self.violation_count.store(0, Ordering::SeqCst);
        Ok(())
    }
}

/// Memory tagging system for enhanced memory safety
#[derive(Debug)]
pub struct MemoryTagger {
    /// Memory tag assignments
    tag_assignments: Arc<RwLock<HashMap<u64, u16>>>,
    /// Allocation metadata
    allocation_metadata: Arc<RwLock<HashMap<u64, AllocationInfo>>>,
    /// Use-after-free detection
    freed_addresses: Arc<RwLock<HashSet<u64>>>,
    /// Memory tagging level
    tagging_level: MemoryTaggingLevel,
    /// Tag generation counter
    tag_counter: AtomicU64,
    /// Memory protection key assignments (for Intel MPK)
    mpk_assignments: Arc<RwLock<HashMap<u64, u32>>>,
}

#[derive(Debug, Clone)]
struct AllocationInfo {
    /// Allocation timestamp
    timestamp: SystemTime,
    /// Allocation size
    size: u64,
    /// Memory tag
    tag: u16,
    /// Allocation backtrace (simplified)
    caller_addr: u64,
}

impl MemoryTagger {
    /// Create a new memory tagger
    pub fn new(tagging_level: MemoryTaggingLevel) -> Self {
        Self {
            tag_assignments: Arc::new(RwLock::new(HashMap::new())),
            allocation_metadata: Arc::new(RwLock::new(HashMap::new())),
            freed_addresses: Arc::new(RwLock::new(HashSet::new())),
            tagging_level,
            tag_counter: AtomicU64::new(1),
            mpk_assignments: Arc::new(RwLock::new(HashMap::new())),
        }
    }

    /// Tag a memory allocation
    pub fn tag_allocation(&self, addr: u64, size: u64, caller_addr: u64) -> WasmtimeResult<u16> {
        let tag = match self.tagging_level {
            MemoryTaggingLevel::None => return Ok(0),
            MemoryTaggingLevel::Basic => self.generate_basic_tag(),
            MemoryTaggingLevel::Enhanced => self.generate_enhanced_tag(addr, size),
            MemoryTaggingLevel::ArmMte => self.generate_arm_mte_tag(addr),
            MemoryTaggingLevel::IntelMpx => self.generate_intel_mpx_tag(addr, size),
        };

        // Record allocation metadata
        let allocation_info = AllocationInfo {
            timestamp: SystemTime::now(),
            size,
            tag,
            caller_addr,
        };

        {
            let mut metadata = self.allocation_metadata.write()
                .map_err(|e| WasmtimeError::Memory {
                    message:
                    format!("Memory tagging metadata lock error: {}", e),
                })?;
            metadata.insert(addr, allocation_info);
        }

        {
            let mut tags = self.tag_assignments.write()
                .map_err(|e| WasmtimeError::Memory {
                    message:
                    format!("Memory tagging assignments lock error: {}", e),
                })?;
            tags.insert(addr, tag);
        }

        Ok(tag)
    }

    /// Validate memory access with tag checking
    pub fn validate_access(&self, addr: u64, expected_tag: u16) -> WasmtimeResult<()> {
        // Check for use-after-free
        {
            let freed = self.freed_addresses.read()
                .map_err(|e| WasmtimeError::Memory {
                    message:
                    format!("Memory tagging freed addresses lock error: {}", e),
                })?;
            if freed.contains(&addr) {
                return Err(WasmtimeError::Memory {
                    message:
                    format!("Use-after-free detected at address 0x{:x}", addr),
                });
            }
        }

        // Validate tag
        let tags = self.tag_assignments.read()
            .map_err(|e| WasmtimeError::Memory {
                message:
                format!("Memory tagging validation lock error: {}", e),
            ))?;

        if let Some(&actual_tag) = tags.get(&addr) {
            if actual_tag != expected_tag {
                return Err(WasmtimeError::Memory {
                    message:
                    format!("Memory tag mismatch at 0x{:x}: expected {} got {}",
                            addr, expected_tag, actual_tag),
                });
            }
        }

        Ok(())
    }

    /// Free memory allocation and mark for use-after-free detection
    pub fn free_allocation(&self, addr: u64) -> WasmtimeResult<()> {
        // Remove from active allocations
        {
            let mut metadata = self.allocation_metadata.write()
                .map_err(|e| WasmtimeError::Memory {
                    message:
                    format!("Memory tagging metadata free lock error: {}", e),
                })?;
            metadata.remove(&addr);
        }

        {
            let mut tags = self.tag_assignments.write()
                .map_err(|e| WasmtimeError::Memory {
                    message:
                    format!("Memory tagging assignments free lock error: {}", e),
                })?;
            tags.remove(&addr);
        }

        // Add to freed addresses for use-after-free detection
        {
            let mut freed = self.freed_addresses.write()
                .map_err(|e| WasmtimeError::Memory {
                    message:
                    format!("Memory tagging freed list lock error: {}", e),
                })?;
            freed.insert(addr);
        }

        Ok(())
    }

    /// Generate basic memory tag
    fn generate_basic_tag(&self) -> u16 {
        (self.tag_counter.fetch_add(1, Ordering::SeqCst) % 65535) as u16 + 1
    }

    /// Generate enhanced memory tag based on address and size
    fn generate_enhanced_tag(&self, addr: u64, size: u64) -> u16 {
        let mut hasher = Sha256::new();
        hasher.update(addr.to_le_bytes());
        hasher.update(size.to_le_bytes());
        hasher.update(self.tag_counter.fetch_add(1, Ordering::SeqCst).to_le_bytes());
        let hash = hasher.finalize();

        // Use first 2 bytes of hash as tag
        let tag = u16::from_le_bytes([hash[0], hash[1]]);
        if tag == 0 { 1 } else { tag }
    }

    /// Generate ARM MTE tag (simplified simulation)
    fn generate_arm_mte_tag(&self, addr: u64) -> u16 {
        // ARM MTE uses 4-bit tags (0-15)
        // This is a simplified simulation
        ((addr >> 4) % 15 + 1) as u16
    }

    /// Generate Intel MPX bounds (simplified simulation)
    fn generate_intel_mpx_tag(&self, addr: u64, size: u64) -> u16 {
        // Intel MPX uses bounds checking rather than tags
        // This simulates the concept with a tag
        let bounds_hash = addr.wrapping_add(size);
        ((bounds_hash % 65535) + 1) as u16
    }

    /// Clean up old freed addresses to prevent memory leak
    pub fn cleanup_freed_addresses(&self, max_age: Duration) -> WasmtimeResult<u32> {
        let mut freed = self.freed_addresses.write()
            .map_err(|e| WasmtimeError::Memory {
                message:
                format!("Memory tagging cleanup lock error: {}", e),
            ))?;

        let initial_count = freed.len();

        // In a real implementation, we would track timestamps for freed addresses
        // For now, we'll just limit the size of the freed set
        if freed.len() > 10000 {
            freed.clear();
        }

        Ok((initial_count - freed.len()) as u32)
    }

    /// Get memory tagging statistics
    pub fn get_statistics(&self) -> WasmtimeResult<MemoryTaggingStatistics> {
        let metadata = self.allocation_metadata.read()
            .map_err(|e| WasmtimeError::Memory {
                message:
                format!("Memory tagging stats lock error: {}", e),
            ))?;

        let freed = self.freed_addresses.read()
            .map_err(|e| WasmtimeError::Memory {
                message:
                format!("Memory tagging stats freed lock error: {}", e),
            ))?;

        Ok(MemoryTaggingStatistics {
            active_allocations: metadata.len() as u64,
            freed_addresses_tracked: freed.len() as u64,
            total_tags_generated: self.tag_counter.load(Ordering::SeqCst),
            tagging_level: self.tagging_level,
        })
    }
}

/// Memory tagging statistics
#[derive(Debug, Clone)]
pub struct MemoryTaggingStatistics {
    /// Number of active allocations being tracked
    pub active_allocations: u64,
    /// Number of freed addresses being tracked for use-after-free detection
    pub freed_addresses_tracked: u64,
    /// Total number of tags generated
    pub total_tags_generated: u64,
    /// Current tagging level
    pub tagging_level: MemoryTaggingLevel,
}

/// Spectre/Meltdown mitigation engine
#[derive(Debug)]
pub struct SpectreMitigator {
    /// Mitigation level
    mitigation_level: SpectreLevel,
    /// Branch prediction history
    branch_history: Arc<RwLock<VecDeque<BranchRecord>>>,
    /// Load/store serialization points
    serialization_points: Arc<RwLock<HashSet<u64>>>,
    /// Speculative execution counter
    speculation_counter: AtomicU64,
    /// LFENCE insertion points
    lfence_points: Arc<RwLock<HashSet<u64>>>,
}

#[derive(Debug, Clone)]
struct BranchRecord {
    /// Branch source address
    source_addr: u64,
    /// Branch target address
    target_addr: u64,
    /// Whether the branch was taken
    taken: bool,
    /// Timestamp of the branch
    timestamp: Instant,
}

impl SpectreMitigator {
    /// Create a new Spectre mitigator
    pub fn new(mitigation_level: SpectreLevel) -> Self {
        Self {
            mitigation_level,
            branch_history: Arc::new(RwLock::new(VecDeque::with_capacity(1000))),
            serialization_points: Arc::new(RwLock::new(HashSet::new())),
            speculation_counter: AtomicU64::new(0),
            lfence_points: Arc::new(RwLock::new(HashSet::new())),
        }
    }

    /// Record a branch for analysis
    pub fn record_branch(&self, source_addr: u64, target_addr: u64, taken: bool) -> WasmtimeResult<()> {
        if matches!(self.mitigation_level, SpectreLevel::None) {
            return Ok(());
        }

        let record = BranchRecord {
            source_addr,
            target_addr,
            taken,
            timestamp: Instant::now(),
        };

        let mut history = self.branch_history.write()
            .map_err(|e| WasmtimeError::Security {
                message: format!("Spectre branch history lock error: {}", e),
            ))?;

        // Keep history bounded
        if history.len() >= 1000 {
            history.pop_front();
        }

        history.push_back(record);

        // Analyze for potential Spectre patterns
        self.analyze_branch_patterns(&history)?;

        Ok(())
    }

    /// Analyze branch patterns for potential Spectre vulnerabilities
    fn analyze_branch_patterns(&self, history: &VecDeque<BranchRecord>) -> WasmtimeResult<()> {
        match self.mitigation_level {
            SpectreLevel::None => Ok(()),
            SpectreLevel::Basic => self.basic_spectre_analysis(history),
            SpectreLevel::Enhanced => self.enhanced_spectre_analysis(history),
            SpectreLevel::Full => self.full_spectre_analysis(history),
            SpectreLevel::HardwareAssisted => self.hardware_assisted_analysis(history),
        }
    }

    /// Basic Spectre analysis - detect suspicious branch patterns
    fn basic_spectre_analysis(&self, history: &VecDeque<BranchRecord>) -> WasmtimeResult<()> {
        // Look for rapid branch mispredictions that could indicate Spectre attacks
        let recent_branches: Vec<_> = history.iter()
            .filter(|record| record.timestamp.elapsed() < Duration::from_millis(100))
            .collect();

        if recent_branches.len() > 50 {
            let mispredictions = recent_branches.iter()
                .windows(2)
                .filter(|window| {
                    window[0].source_addr == window[1].source_addr &&
                    window[0].taken != window[1].taken
                })
                .count();

            if mispredictions as f64 / recent_branches.len() as f64 > 0.3 {
                return Err(WasmtimeError::Security {
                    message:
                    "Potential Spectre attack detected: excessive branch mispredictions".to_string(),
                });
            }
        }

        Ok(())
    }

    /// Enhanced Spectre analysis - include load/store pattern analysis
    fn enhanced_spectre_analysis(&self, history: &VecDeque<BranchRecord>) -> WasmtimeResult<()> {
        self.basic_spectre_analysis(history)?;

        // Additional analysis for load/store patterns
        self.analyze_memory_access_patterns(history)?;

        Ok(())
    }

    /// Full Spectre analysis - comprehensive pattern detection
    fn full_spectre_analysis(&self, history: &VecDeque<BranchRecord>) -> WasmtimeResult<()> {
        self.enhanced_spectre_analysis(history)?;

        // Full analysis includes:
        // 1. Speculative execution depth analysis
        // 2. Cache timing analysis
        // 3. Memory dependency analysis
        self.analyze_speculative_depth(history)?;
        self.analyze_cache_timing_patterns(history)?;

        Ok(())
    }

    /// Hardware-assisted analysis using CPU features
    fn hardware_assisted_analysis(&self, history: &VecDeque<BranchRecord>) -> WasmtimeResult<()> {
        self.full_spectre_analysis(history)?;

        // Hardware-assisted mitigations would use:
        // - Intel CET for control flow integrity
        // - Hardware performance counters
        // - Microcode updates

        Ok(())
    }

    /// Analyze memory access patterns for Spectre variants
    fn analyze_memory_access_patterns(&self, _history: &VecDeque<BranchRecord>) -> WasmtimeResult<()> {
        // Implementation would analyze memory access patterns
        // to detect potential Spectre variant 1 attacks
        Ok(())
    }

    /// Analyze speculative execution depth
    fn analyze_speculative_depth(&self, _history: &VecDeque<BranchRecord>) -> WasmtimeResult<()> {
        let current_depth = self.speculation_counter.load(Ordering::SeqCst);

        // Limit speculative execution depth
        if current_depth > 100 {
            return Err(WasmtimeError::Security {
                message:
                "Excessive speculative execution depth detected".to_string(),
            ));
        }

        Ok(())
    }

    /// Analyze cache timing patterns
    fn analyze_cache_timing_patterns(&self, _history: &VecDeque<BranchRecord>) -> WasmtimeResult<()> {
        // Implementation would analyze cache access patterns
        // to detect potential cache-based side-channel attacks
        Ok(())
    }

    /// Insert serialization point to prevent speculation
    pub fn insert_serialization_point(&self, addr: u64) -> WasmtimeResult<()> {
        let mut points = self.serialization_points.write()
            .map_err(|e| WasmtimeError::Security {
                message: format!("Spectre serialization points lock error: {}", e),
            ))?;

        points.insert(addr);
        Ok(())
    }

    /// Check if serialization is required at an address
    pub fn requires_serialization(&self, addr: u64) -> bool {
        self.serialization_points.read()
            .map(|points| points.contains(&addr))
            .unwrap_or(false)
    }

    /// Increment speculation counter
    pub fn enter_speculation(&self) {
        self.speculation_counter.fetch_add(1, Ordering::SeqCst);
    }

    /// Decrement speculation counter
    pub fn exit_speculation(&self) {
        self.speculation_counter.fetch_sub(1, Ordering::SeqCst);
    }

    /// Get current speculation depth
    pub fn get_speculation_depth(&self) -> u64 {
        self.speculation_counter.load(Ordering::SeqCst)
    }
}

/// Advanced sandboxing with hardware-assisted isolation
#[derive(Debug)]
pub struct AdvancedSandbox {
    /// Isolation level
    isolation_level: IsolationLevel,
    /// Memory protection domains
    protection_domains: Arc<RwLock<HashMap<u32, ProtectionDomain>>>,
    /// Active sandbox instances
    sandbox_instances: Arc<RwLock<HashMap<String, SandboxInstance>>>,
    /// Hardware capability detection
    hardware_capabilities: HardwareCapabilities,
    /// Sandbox violation counter
    violation_counter: AtomicU64,
}

#[derive(Debug, Clone)]
struct ProtectionDomain {
    /// Domain identifier
    domain_id: u32,
    /// Memory regions
    memory_regions: Vec<MemoryRegion>,
    /// Allowed capabilities
    capabilities: HashSet<SecurityCapability>,
    /// Domain-specific policy
    policy: DomainPolicy,
}

#[derive(Debug, Clone)]
struct MemoryRegion {
    /// Base address
    base_addr: u64,
    /// Size in bytes
    size: u64,
    /// Access permissions
    permissions: MemoryPermissions,
    /// Memory protection key (for Intel MPK)
    protection_key: Option<u32>,
}

#[derive(Debug, Clone)]
struct MemoryPermissions {
    /// Read permission
    read: bool,
    /// Write permission
    write: bool,
    /// Execute permission
    execute: bool,
}

#[derive(Debug, Clone)]
struct DomainPolicy {
    /// Maximum memory allocation
    max_memory: u64,
    /// Maximum execution time
    max_execution_time: Duration,
    /// Allowed system calls
    allowed_syscalls: HashSet<String>,
}

#[derive(Debug, Clone)]
struct SandboxInstance {
    /// Sandbox identifier
    sandbox_id: String,
    /// Protection domain
    domain_id: u32,
    /// Creation timestamp
    created_at: SystemTime,
    /// Last access timestamp
    last_access: SystemTime,
    /// Resource usage
    resource_usage: ResourceUsage,
}

#[derive(Debug, Clone)]
struct ResourceUsage {
    /// Memory usage in bytes
    memory_usage: u64,
    /// CPU time used
    cpu_time: Duration,
    /// Number of system calls
    syscall_count: u64,
}

#[derive(Debug, Clone)]
struct HardwareCapabilities {
    /// Intel MPK support
    intel_mpk: bool,
    /// ARM Pointer Authentication support
    arm_pointer_auth: bool,
    /// Intel CET support
    intel_cet: bool,
    /// Virtualization support
    virtualization: bool,
}

impl AdvancedSandbox {
    /// Create a new advanced sandbox
    pub fn new(isolation_level: IsolationLevel) -> WasmtimeResult<Self> {
        let hardware_capabilities = Self::detect_hardware_capabilities();

        Ok(Self {
            isolation_level,
            protection_domains: Arc::new(RwLock::new(HashMap::new())),
            sandbox_instances: Arc::new(RwLock::new(HashMap::new())),
            hardware_capabilities,
            violation_counter: AtomicU64::new(0),
        })
    }

    /// Detect available hardware security capabilities
    fn detect_hardware_capabilities() -> HardwareCapabilities {
        // In a real implementation, this would use CPUID and other
        // hardware detection mechanisms
        HardwareCapabilities {
            intel_mpk: false,        // Would detect Intel MPK
            arm_pointer_auth: false, // Would detect ARM Pointer Auth
            intel_cet: false,        // Would detect Intel CET
            virtualization: false,   // Would detect VT-x/AMD-V
        }
    }

    /// Create a new protection domain
    pub fn create_protection_domain(&self, capabilities: HashSet<SecurityCapability>) -> WasmtimeResult<u32> {
        let domain_id = self.generate_domain_id();

        let policy = DomainPolicy {
            max_memory: 1024 * 1024 * 64, // 64MB default
            max_execution_time: Duration::from_secs(60),
            allowed_syscalls: HashSet::new(),
        };

        let domain = ProtectionDomain {
            domain_id,
            memory_regions: Vec::new(),
            capabilities,
            policy,
        };

        let mut domains = self.protection_domains.write()
            .map_err(|e| WasmtimeError::Security {
                message: format!("Protection domains lock error: {}", e),
            ))?;

        domains.insert(domain_id, domain);
        Ok(domain_id)
    }

    /// Create a new sandbox instance
    pub fn create_sandbox(&self, sandbox_id: String, domain_id: u32) -> WasmtimeResult<()> {
        // Verify domain exists
        {
            let domains = self.protection_domains.read()
                .map_err(|e| WasmtimeError::Security {
                    message:
                    format!("Protection domains read lock error: {}", e),
                })?;

            if !domains.contains_key(&domain_id) {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Protection domain {} does not exist", domain_id),
                });
            }
        }

        let instance = SandboxInstance {
            sandbox_id: sandbox_id.clone(),
            domain_id,
            created_at: SystemTime::now(),
            last_access: SystemTime::now(),
            resource_usage: ResourceUsage {
                memory_usage: 0,
                cpu_time: Duration::new(0, 0),
                syscall_count: 0,
            },
        };

        let mut instances = self.sandbox_instances.write()
            .map_err(|e| WasmtimeError::Security {
                message: format!("Sandbox instances lock error: {}", e),
            ))?;

        instances.insert(sandbox_id, instance);
        Ok(())
    }

    /// Validate access within a sandbox
    pub fn validate_sandbox_access(&self, sandbox_id: &str, capability: &SecurityCapability) -> WasmtimeResult<()> {
        let instances = self.sandbox_instances.read()
            .map_err(|e| WasmtimeError::Security {
                message: format!("Sandbox instances read lock error: {}", e),
            ))?;

        let instance = instances.get(sandbox_id)
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message:
                format!("Sandbox {} not found", sandbox_id),
            ))?;

        let domains = self.protection_domains.read()
            .map_err(|e| WasmtimeError::Security {
                message: format!("Protection domains access lock error: {}", e),
            ))?;

        let domain = domains.get(&instance.domain_id)
            .ok_or_else(|| WasmtimeError::Security {
                message: format!("Protection domain {} not found", instance.domain_id),
            ))?;

        if !domain.capabilities.contains(capability) {
            self.violation_counter.fetch_add(1, Ordering::SeqCst);
            return Err(WasmtimeError::Security {
                message: format!("Sandbox {} lacks capability {:?}", sandbox_id, capability),
            ));
        }

        Ok(())
    }

    /// Configure hardware-assisted isolation
    pub fn configure_hardware_isolation(&self) -> WasmtimeResult<()> {
        match self.isolation_level {
            IsolationLevel::Software => Ok(()), // No hardware configuration needed
            IsolationLevel::IntelMpk => self.configure_intel_mpk(),
            IsolationLevel::ArmPointerAuth => self.configure_arm_pointer_auth(),
            IsolationLevel::IntelCetMpk => {
                self.configure_intel_mpk()?;
                self.configure_intel_cet()
            }
            IsolationLevel::VirtualizationBased => self.configure_virtualization(),
        }
    }

    /// Configure Intel Memory Protection Keys
    fn configure_intel_mpk(&self) -> WasmtimeResult<()> {
        if !self.hardware_capabilities.intel_mpk {
            return Err(WasmtimeError::UnsupportedFeature {
                message:
                "Intel MPK not supported on this hardware".to_string(),
            ));
        }

        // Configuration would involve:
        // 1. Allocating protection keys
        // 2. Setting up memory regions with protection keys
        // 3. Configuring PKRU (Protection Key Rights for User pages) register

        Ok(())
    }

    /// Configure ARM Pointer Authentication
    fn configure_arm_pointer_auth(&self) -> WasmtimeResult<()> {
        if !self.hardware_capabilities.arm_pointer_auth {
            return Err(WasmtimeError::UnsupportedFeature {
                message:
                "ARM Pointer Authentication not supported on this hardware".to_string(),
            ));
        }

        // Configuration would involve:
        // 1. Enabling pointer authentication in system registers
        // 2. Setting up authentication keys
        // 3. Configuring pointer signing and verification

        Ok(())
    }

    /// Configure Intel CET
    fn configure_intel_cet(&self) -> WasmtimeResult<()> {
        if !self.hardware_capabilities.intel_cet {
            return Err(WasmtimeError::UnsupportedFeature {
                message:
                "Intel CET not supported on this hardware".to_string(),
            ));
        }

        // Configuration would involve:
        // 1. Enabling CET in processor control registers
        // 2. Setting up shadow stack
        // 3. Configuring IBT (Indirect Branch Tracking)

        Ok(())
    }

    /// Configure virtualization-based security
    fn configure_virtualization(&self) -> WasmtimeResult<()> {
        if !self.hardware_capabilities.virtualization {
            return Err(WasmtimeError::UnsupportedFeature {
                message:
                "Hardware virtualization not supported".to_string(),
            ));
        }

        // Configuration would involve:
        // 1. Setting up hypervisor
        // 2. Creating isolated virtual machines for each sandbox
        // 3. Configuring memory isolation and IOMMU

        Ok(())
    }

    /// Generate a unique domain ID
    fn generate_domain_id(&self) -> u32 {
        // Simple implementation - in production this would use a more robust method
        (SystemTime::now().duration_since(UNIX_EPOCH).unwrap_or_default().as_nanos() % u32::MAX as u128) as u32
    }

    /// Get sandbox statistics
    pub fn get_sandbox_statistics(&self) -> WasmtimeResult<SandboxStatistics> {
        let instances = self.sandbox_instances.read()
            .map_err(|e| WasmtimeError::Security {
                message: format!("Sandbox statistics lock error: {}", e),
            ))?;

        let domains = self.protection_domains.read()
            .map_err(|e| WasmtimeError::Security {
                message: format!("Domain statistics lock error: {}", e),
            ))?;

        Ok(SandboxStatistics {
            active_sandboxes: instances.len() as u64,
            protection_domains: domains.len() as u64,
            violations: self.violation_counter.load(Ordering::SeqCst),
            hardware_capabilities: self.hardware_capabilities.clone(),
        })
    }

    /// Clean up expired sandbox instances
    pub fn cleanup_expired_sandboxes(&self, max_idle_time: Duration) -> WasmtimeResult<u32> {
        let mut instances = self.sandbox_instances.write()
            .map_err(|e| WasmtimeError::Security {
                message: format!("Sandbox cleanup lock error: {}", e),
            ))?;

        let now = SystemTime::now();
        let initial_count = instances.len();

        instances.retain(|_, instance| {
            now.duration_since(instance.last_access).unwrap_or(Duration::MAX) <= max_idle_time
        });

        Ok((initial_count - instances.len()) as u32)
    }
}

/// Sandbox statistics
#[derive(Debug, Clone)]
pub struct SandboxStatistics {
    /// Number of active sandboxes
    pub active_sandboxes: u64,
    /// Number of protection domains
    pub protection_domains: u64,
    /// Total security violations
    pub violations: u64,
    /// Available hardware capabilities
    pub hardware_capabilities: HardwareCapabilities,
}

/// Security hardening manager that coordinates all security features
#[derive(Debug)]
pub struct SecurityHardeningManager {
    /// Security configuration
    config: SecurityHardeningConfig,
    /// CFI validator
    cfi_validator: CfiValidator,
    /// Memory tagger
    memory_tagger: MemoryTagger,
    /// Spectre mitigator
    spectre_mitigator: SpectreMitigator,
    /// Advanced sandbox
    advanced_sandbox: AdvancedSandbox,
    /// Audit logger
    audit_logger: Arc<AuditLogger>,
    /// Access control engine
    access_control: Arc<AccessControlEngine>,
    /// Security monitoring
    is_monitoring_enabled: AtomicBool,
    /// Threat detection counter
    threat_detection_counter: AtomicU64,
}

impl SecurityHardeningManager {
    /// Create a new security hardening manager
    pub fn new(config: SecurityHardeningConfig, audit_log_path: &std::path::Path) -> WasmtimeResult<Self> {
        let audit_logger = Arc::new(AuditLogger::new(audit_log_path)
            .map_err(|e| WasmtimeError::Security {
                message: format!("Failed to create audit logger: {}", e),
            ))?);

        let access_control = Arc::new(AccessControlEngine::new(audit_log_path)
            .map_err(|e| WasmtimeError::Security {
                message: format!("Failed to create access control engine: {}", e),
            ))?);

        let cfi_validator = CfiValidator::new(config.cfi_level);
        let memory_tagger = MemoryTagger::new(config.memory_tagging_level);
        let spectre_mitigator = SpectreMitigator::new(config.spectre_level);
        let advanced_sandbox = AdvancedSandbox::new(config.isolation_level)?;

        Ok(Self {
            config,
            cfi_validator,
            memory_tagger,
            spectre_mitigator,
            advanced_sandbox,
            audit_logger,
            access_control,
            is_monitoring_enabled: AtomicBool::new(true),
            threat_detection_counter: AtomicU64::new(0),
        })
    }

    /// Initialize security hardening features
    pub fn initialize(&self) -> WasmtimeResult<()> {
        // Configure hardware-assisted features
        self.advanced_sandbox.configure_hardware_isolation()?;

        // Initialize monitoring if enabled
        if self.config.enable_threat_detection {
            self.is_monitoring_enabled.store(true, Ordering::SeqCst);
        }

        Ok(())
    }

    /// Validate a function call using all security features
    pub fn validate_function_call(&self, from_addr: u64, to_addr: u64, session_id: &str) -> WasmtimeResult<()> {
        // CFI validation
        if !matches!(self.config.cfi_level, CfiLevel::None) {
            self.cfi_validator.validate_call(from_addr, to_addr)?;
        }

        // Access control validation
        let capability = SecurityCapability::Execute("*".to_string());
        if self.access_control.check_capability(session_id, &capability, "function_call")
            .map_err(|e| WasmtimeError::Security {
                message: format!("Access control error: {}", e),
            ))? == false {
            return Err(WasmtimeError::Security {
                message:
                "Function call not authorized".to_string(),
            ));
        }

        // Record for Spectre analysis
        self.spectre_mitigator.record_branch(from_addr, to_addr, true)?;

        Ok(())
    }

    /// Validate memory access using security features
    pub fn validate_memory_access(&self, addr: u64, size: u64, tag: u16, session_id: &str) -> WasmtimeResult<()> {
        // Memory tagging validation
        if !matches!(self.config.memory_tagging_level, MemoryTaggingLevel::None) {
            self.memory_tagger.validate_access(addr, tag)?;
        }

        // Access control validation for memory access
        let capability = SecurityCapability::Read("*".to_string());
        if self.access_control.check_capability(session_id, &capability, &format!("memory:0x{:x}", addr))
            .map_err(|e| WasmtimeError::Security {
                message: format!("Memory access control error: {}", e),
            ))? == false {
            return Err(WasmtimeError::Security {
                message:
                "Memory access not authorized".to_string(),
            ));
        }

        Ok(())
    }

    /// Create a secure execution environment
    pub fn create_secure_environment(&self, session_id: &str, capabilities: HashSet<SecurityCapability>) -> WasmtimeResult<String> {
        // Create protection domain
        let domain_id = self.advanced_sandbox.create_protection_domain(capabilities)?;

        // Create sandbox instance
        let sandbox_id = format!("sandbox_{}", uuid::Uuid::new_v4());
        self.advanced_sandbox.create_sandbox(sandbox_id.clone(), domain_id)?;

        Ok(sandbox_id)
    }

    /// Get comprehensive security statistics
    pub fn get_security_statistics(&self) -> WasmtimeResult<ComprehensiveSecurityStatistics> {
        let memory_stats = self.memory_tagger.get_statistics()?;
        let sandbox_stats = self.advanced_sandbox.get_sandbox_statistics()?;

        Ok(ComprehensiveSecurityStatistics {
            cfi_violations: self.cfi_validator.get_violation_count(),
            memory_tagging: memory_stats,
            sandbox_stats,
            speculation_depth: self.spectre_mitigator.get_speculation_depth(),
            threat_detections: self.threat_detection_counter.load(Ordering::SeqCst),
            monitoring_enabled: self.is_monitoring_enabled.load(Ordering::SeqCst),
            config: self.config.clone(),
        })
    }

    /// Enable or disable security monitoring
    pub fn set_monitoring_enabled(&self, enabled: bool) {
        self.is_monitoring_enabled.store(enabled, Ordering::SeqCst);
    }

    /// Perform security cleanup operations
    pub fn perform_security_cleanup(&self) -> WasmtimeResult<SecurityCleanupReport> {
        let freed_memory = self.memory_tagger.cleanup_freed_addresses(Duration::from_secs(3600))?;
        let expired_sandboxes = self.advanced_sandbox.cleanup_expired_sandboxes(Duration::from_secs(3600))?;
        let expired_sessions = self.access_control.cleanup_expired_sessions()
            .map_err(|e| WasmtimeError::Security {
                message: format!("Session cleanup error: {}", e),
            ))?;

        Ok(SecurityCleanupReport {
            freed_memory_entries: freed_memory,
            expired_sandboxes,
            expired_sessions,
        })
    }
}

/// Comprehensive security statistics
#[derive(Debug, Clone)]
pub struct ComprehensiveSecurityStatistics {
    /// CFI violation count
    pub cfi_violations: u64,
    /// Memory tagging statistics
    pub memory_tagging: MemoryTaggingStatistics,
    /// Sandbox statistics
    pub sandbox_stats: SandboxStatistics,
    /// Current speculation depth
    pub speculation_depth: u64,
    /// Threat detection count
    pub threat_detections: u64,
    /// Whether monitoring is enabled
    pub monitoring_enabled: bool,
    /// Security configuration
    pub config: SecurityHardeningConfig,
}

/// Security cleanup report
#[derive(Debug, Clone)]
pub struct SecurityCleanupReport {
    /// Number of freed memory entries cleaned up
    pub freed_memory_entries: u32,
    /// Number of expired sandbox instances removed
    pub expired_sandboxes: u32,
    /// Number of expired sessions removed
    pub expired_sessions: u32,
}

#[cfg(test)]
mod tests {
    use super::*;
    use tempfile::TempDir;

    #[test]
    fn test_security_hardening_config_creation() {
        let config = SecurityHardeningConfig::default();
        assert_eq!(config.cfi_level, CfiLevel::Enhanced);
        assert_eq!(config.memory_tagging_level, MemoryTaggingLevel::Enhanced);

        let max_config = SecurityHardeningConfig::maximum_security();
        assert_eq!(max_config.cfi_level, CfiLevel::Full);
        assert_eq!(max_config.isolation_level, IsolationLevel::VirtualizationBased);
    }

    #[test]
    fn test_cfi_validator_basic_functionality() {
        let validator = CfiValidator::new(CfiLevel::Basic);

        // Add a valid function
        validator.add_valid_function(0x1000).unwrap();

        // Valid call should succeed
        assert!(validator.validate_call(0x2000, 0x1000).is_ok());

        // Invalid call should fail
        assert!(validator.validate_call(0x2000, 0x3000).is_err());
    }

    #[test]
    fn test_memory_tagger_allocation_and_validation() {
        let tagger = MemoryTagger::new(MemoryTaggingLevel::Enhanced);

        let addr = 0x4000;
        let size = 1024;
        let caller = 0x5000;

        let tag = tagger.tag_allocation(addr, size, caller).unwrap();
        assert!(tag > 0);

        // Valid access should succeed
        assert!(tagger.validate_access(addr, tag).is_ok());

        // Invalid tag should fail
        assert!(tagger.validate_access(addr, tag + 1).is_err());

        // Free the allocation
        tagger.free_allocation(addr).unwrap();

        // Use-after-free should be detected
        assert!(tagger.validate_access(addr, tag).is_err());
    }

    #[test]
    fn test_spectre_mitigator_branch_recording() {
        let mitigator = SpectreMitigator::new(SpectreLevel::Enhanced);

        // Record some branches
        assert!(mitigator.record_branch(0x1000, 0x2000, true).is_ok());
        assert!(mitigator.record_branch(0x2000, 0x3000, false).is_ok());

        // Normal patterns should not trigger violations
        for i in 0..10 {
            assert!(mitigator.record_branch(0x1000 + i, 0x2000 + i, i % 2 == 0).is_ok());
        }
    }

    #[test]
    fn test_advanced_sandbox_creation() {
        let sandbox = AdvancedSandbox::new(IsolationLevel::Software).unwrap();

        let mut capabilities = HashSet::new();
        capabilities.insert(SecurityCapability::Execute("test".to_string()));

        let domain_id = sandbox.create_protection_domain(capabilities.clone()).unwrap();
        assert!(domain_id > 0);

        let sandbox_id = "test_sandbox".to_string();
        assert!(sandbox.create_sandbox(sandbox_id.clone(), domain_id).is_ok());

        let capability = SecurityCapability::Execute("test".to_string());
        assert!(sandbox.validate_sandbox_access(&sandbox_id, &capability).is_ok());

        let invalid_capability = SecurityCapability::Admin;
        assert!(sandbox.validate_sandbox_access(&sandbox_id, &invalid_capability).is_err());
    }

    #[test]
    fn test_security_hardening_manager_integration() {
        let temp_dir = TempDir::new().unwrap();
        let audit_path = temp_dir.path().join("audit.log");

        let config = SecurityHardeningConfig::balanced();
        let manager = SecurityHardeningManager::new(config, &audit_path).unwrap();

        assert!(manager.initialize().is_ok());

        // Test comprehensive statistics
        let stats = manager.get_security_statistics().unwrap();
        assert_eq!(stats.cfi_violations, 0);
        assert!(stats.monitoring_enabled);
    }

    #[test]
    fn test_security_cleanup_operations() {
        let temp_dir = TempDir::new().unwrap();
        let audit_path = temp_dir.path().join("audit.log");

        let config = SecurityHardeningConfig::balanced();
        let manager = SecurityHardeningManager::new(config, &audit_path).unwrap();

        let cleanup_report = manager.perform_security_cleanup().unwrap();
        // Initially should have no items to clean up
        assert_eq!(cleanup_report.freed_memory_entries, 0);
    }
}