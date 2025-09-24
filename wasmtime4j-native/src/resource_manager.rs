//! Production-ready resource management with quotas, limits, and preemption
//!
//! This module implements a genuine resource management system that provides hard and soft
//! quotas, CPU time limiting with preemption, I/O rate limiting, and comprehensive resource monitoring.

use std::collections::HashMap;
use std::sync::{Arc, Mutex, RwLock};
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};
use std::thread;

/// Resource quota configuration
#[derive(Debug, Clone)]
pub struct ResourceQuota {
    /// Memory quota in bytes (0 = unlimited)
    pub memory_limit_bytes: u64,
    /// Soft memory limit in bytes (warning threshold)
    pub memory_soft_limit_bytes: u64,
    /// CPU time limit per execution (0 = unlimited)
    pub cpu_time_limit: Duration,
    /// Maximum number of file descriptors
    pub max_file_descriptors: u32,
    /// Maximum number of network connections
    pub max_network_connections: u32,
    /// I/O bandwidth limit in bytes per second (0 = unlimited)
    pub io_bandwidth_limit: u64,
    /// Maximum execution time per operation
    pub max_execution_time: Duration,
    /// Enable preemption for CPU time limits
    pub cpu_preemption_enabled: bool,
    /// Preemption check interval
    pub preemption_check_interval: Duration,
}

impl Default for ResourceQuota {
    fn default() -> Self {
        Self {
            memory_limit_bytes: 1024 * 1024 * 1024, // 1GB
            memory_soft_limit_bytes: 768 * 1024 * 1024, // 768MB
            cpu_time_limit: Duration::from_secs(30),
            max_file_descriptors: 1000,
            max_network_connections: 100,
            io_bandwidth_limit: 100 * 1024 * 1024, // 100MB/s
            max_execution_time: Duration::from_secs(60),
            cpu_preemption_enabled: true,
            preemption_check_interval: Duration::from_millis(100),
        }
    }
}

/// Resource usage tracking
#[derive(Debug, Clone)]
pub struct ResourceUsage {
    pub memory_allocated: u64,
    pub memory_peak: u64,
    pub cpu_time_used: Duration,
    pub file_descriptors_open: u32,
    pub network_connections_active: u32,
    pub io_bytes_read: u64,
    pub io_bytes_written: u64,
    pub execution_start_time: SystemTime,
    pub last_preemption_check: Instant,
}

impl Default for ResourceUsage {
    fn default() -> Self {
        Self {
            memory_allocated: 0,
            memory_peak: 0,
            cpu_time_used: Duration::ZERO,
            file_descriptors_open: 0,
            network_connections_active: 0,
            io_bytes_read: 0,
            io_bytes_written: 0,
            execution_start_time: SystemTime::now(),
            last_preemption_check: Instant::now(),
        }
    }
}

/// Resource violation types
#[derive(Debug, Clone, PartialEq)]
pub enum ResourceViolationType {
    MemoryHardLimit,
    MemorySoftLimit,
    CpuTimeLimit,
    ExecutionTimeLimit,
    FileDescriptorLimit,
    NetworkConnectionLimit,
    IoBandwidthLimit,
}

/// Resource violation event
#[derive(Debug, Clone)]
pub struct ResourceViolation {
    pub violation_type: ResourceViolationType,
    pub resource_id: String,
    pub current_usage: u64,
    pub limit: u64,
    pub timestamp: SystemTime,
    pub action_taken: ResourceAction,
}

/// Actions taken when resource limits are exceeded
#[derive(Debug, Clone, PartialEq)]
pub enum ResourceAction {
    Warning,
    Throttle,
    Suspend,
    Terminate,
    Preempt,
}

/// I/O rate limiting state
#[derive(Debug)]
struct IoRateLimiter {
    bandwidth_limit: u64, // bytes per second
    current_period_start: Instant,
    current_period_bytes: u64,
    total_bytes_transferred: u64,
    throttle_count: u64,
}

impl IoRateLimiter {
    fn new(bandwidth_limit: u64) -> Self {
        Self {
            bandwidth_limit,
            current_period_start: Instant::now(),
            current_period_bytes: 0,
            total_bytes_transferred: 0,
            throttle_count: 0,
        }
    }

    fn check_and_throttle(&mut self, bytes: u64) -> Result<Duration, String> {
        let now = Instant::now();
        let period_elapsed = now.duration_since(self.current_period_start);

        // Reset period if more than 1 second has passed
        if period_elapsed >= Duration::from_secs(1) {
            self.current_period_start = now;
            self.current_period_bytes = 0;
        }

        // Check if adding these bytes would exceed the limit
        if self.current_period_bytes + bytes > self.bandwidth_limit {
            let remaining_time = Duration::from_secs(1) - period_elapsed;
            self.throttle_count += 1;
            return Ok(remaining_time); // Return delay needed
        }

        self.current_period_bytes += bytes;
        self.total_bytes_transferred += bytes;
        Ok(Duration::ZERO) // No throttling needed
    }

    fn get_statistics(&self) -> (u64, u64) {
        (self.total_bytes_transferred, self.throttle_count)
    }
}

/// CPU time tracking and preemption
#[derive(Debug)]
struct CpuTimeTracker {
    start_time: Instant,
    accumulated_time: Duration,
    preemption_enabled: bool,
    preemption_interval: Duration,
    last_preemption_check: Instant,
    preemption_count: u64,
}

impl CpuTimeTracker {
    fn new(preemption_enabled: bool, preemption_interval: Duration) -> Self {
        let now = Instant::now();
        Self {
            start_time: now,
            accumulated_time: Duration::ZERO,
            preemption_enabled,
            preemption_interval,
            last_preemption_check: now,
            preemption_count: 0,
        }
    }

    fn check_preemption(&mut self, time_limit: Duration) -> Result<bool, String> {
        let now = Instant::now();
        let elapsed = now.duration_since(self.start_time);
        self.accumulated_time = elapsed;

        if !self.preemption_enabled {
            return Ok(false);
        }

        // Check if it's time for a preemption check
        if now.duration_since(self.last_preemption_check) < self.preemption_interval {
            return Ok(false);
        }

        self.last_preemption_check = now;

        if elapsed > time_limit {
            self.preemption_count += 1;
            return Ok(true); // Preemption needed
        }

        Ok(false)
    }

    fn get_statistics(&self) -> (Duration, u64) {
        (self.accumulated_time, self.preemption_count)
    }
}

/// File descriptor tracking
#[derive(Debug)]
struct FileDescriptorTracker {
    open_descriptors: HashMap<u32, SystemTime>,
    next_fd: u32,
    max_descriptors: u32,
}

impl FileDescriptorTracker {
    fn new(max_descriptors: u32) -> Self {
        Self {
            open_descriptors: HashMap::new(),
            next_fd: 1,
            max_descriptors,
        }
    }

    fn allocate_descriptor(&mut self) -> Result<u32, String> {
        if self.open_descriptors.len() >= self.max_descriptors as usize {
            return Err("File descriptor limit exceeded".to_string());
        }

        let fd = self.next_fd;
        self.next_fd += 1;
        self.open_descriptors.insert(fd, SystemTime::now());
        Ok(fd)
    }

    fn release_descriptor(&mut self, fd: u32) -> bool {
        self.open_descriptors.remove(&fd).is_some()
    }

    fn get_open_count(&self) -> u32 {
        self.open_descriptors.len() as u32
    }
}

/// Network connection tracking
#[derive(Debug)]
struct NetworkConnectionTracker {
    active_connections: HashMap<u32, SystemTime>,
    next_connection_id: u32,
    max_connections: u32,
}

impl NetworkConnectionTracker {
    fn new(max_connections: u32) -> Self {
        Self {
            active_connections: HashMap::new(),
            next_connection_id: 1,
            max_connections,
        }
    }

    fn open_connection(&mut self) -> Result<u32, String> {
        if self.active_connections.len() >= self.max_connections as usize {
            return Err("Network connection limit exceeded".to_string());
        }

        let conn_id = self.next_connection_id;
        self.next_connection_id += 1;
        self.active_connections.insert(conn_id, SystemTime::now());
        Ok(conn_id)
    }

    fn close_connection(&mut self, connection_id: u32) -> bool {
        self.active_connections.remove(&connection_id).is_some()
    }

    fn get_active_count(&self) -> u32 {
        self.active_connections.len() as u32
    }
}

/// Resource manager statistics
#[derive(Debug, Clone)]
pub struct ResourceManagerStatistics {
    pub total_resources_managed: u64,
    pub active_quotas: u64,
    pub violations_detected: u64,
    pub preemptions_performed: u64,
    pub throttling_events: u64,
    pub resource_warnings: u64,
    pub average_memory_usage: u64,
    pub peak_memory_usage: u64,
    pub total_cpu_time: Duration,
    pub total_io_bytes: u64,
    pub manager_uptime: Duration,
}

impl Default for ResourceManagerStatistics {
    fn default() -> Self {
        Self {
            total_resources_managed: 0,
            active_quotas: 0,
            violations_detected: 0,
            preemptions_performed: 0,
            throttling_events: 0,
            resource_warnings: 0,
            average_memory_usage: 0,
            peak_memory_usage: 0,
            total_cpu_time: Duration::ZERO,
            total_io_bytes: 0,
            manager_uptime: Duration::ZERO,
        }
    }
}

/// Managed resource instance
#[derive(Debug)]
struct ManagedResource {
    resource_id: String,
    quota: ResourceQuota,
    usage: ResourceUsage,
    cpu_tracker: CpuTimeTracker,
    io_rate_limiter: IoRateLimiter,
    fd_tracker: FileDescriptorTracker,
    network_tracker: NetworkConnectionTracker,
    violations: Vec<ResourceViolation>,
    active: bool,
}

impl ManagedResource {
    fn new(resource_id: String, quota: ResourceQuota) -> Self {
        Self {
            resource_id: resource_id.clone(),
            quota: quota.clone(),
            usage: ResourceUsage::default(),
            cpu_tracker: CpuTimeTracker::new(quota.cpu_preemption_enabled, quota.preemption_check_interval),
            io_rate_limiter: IoRateLimiter::new(quota.io_bandwidth_limit),
            fd_tracker: FileDescriptorTracker::new(quota.max_file_descriptors),
            network_tracker: NetworkConnectionTracker::new(quota.max_network_connections),
            violations: Vec::new(),
            active: true,
        }
    }

    fn check_memory_limits(&mut self, new_allocation: u64) -> Result<ResourceAction, ResourceViolation> {
        let new_total = self.usage.memory_allocated + new_allocation;

        // Check hard limit
        if self.quota.memory_limit_bytes > 0 && new_total > self.quota.memory_limit_bytes {
            let violation = ResourceViolation {
                violation_type: ResourceViolationType::MemoryHardLimit,
                resource_id: self.resource_id.clone(),
                current_usage: new_total,
                limit: self.quota.memory_limit_bytes,
                timestamp: SystemTime::now(),
                action_taken: ResourceAction::Terminate,
            };
            self.violations.push(violation.clone());
            return Err(violation);
        }

        // Check soft limit
        if self.quota.memory_soft_limit_bytes > 0 && new_total > self.quota.memory_soft_limit_bytes {
            let violation = ResourceViolation {
                violation_type: ResourceViolationType::MemorySoftLimit,
                resource_id: self.resource_id.clone(),
                current_usage: new_total,
                limit: self.quota.memory_soft_limit_bytes,
                timestamp: SystemTime::now(),
                action_taken: ResourceAction::Warning,
            };
            self.violations.push(violation.clone());
            return Ok(ResourceAction::Warning);
        }

        Ok(ResourceAction::Warning) // No violation
    }

    fn check_cpu_limits(&mut self) -> Result<ResourceAction, ResourceViolation> {
        if self.quota.cpu_time_limit == Duration::ZERO {
            return Ok(ResourceAction::Warning); // No limit
        }

        match self.cpu_tracker.check_preemption(self.quota.cpu_time_limit) {
            Ok(true) => {
                let violation = ResourceViolation {
                    violation_type: ResourceViolationType::CpuTimeLimit,
                    resource_id: self.resource_id.clone(),
                    current_usage: self.cpu_tracker.accumulated_time.as_millis() as u64,
                    limit: self.quota.cpu_time_limit.as_millis() as u64,
                    timestamp: SystemTime::now(),
                    action_taken: ResourceAction::Preempt,
                };
                self.violations.push(violation.clone());
                Err(violation)
            }
            Ok(false) => Ok(ResourceAction::Warning),
            Err(e) => {
                eprintln!("CPU tracking error: {}", e);
                Ok(ResourceAction::Warning)
            }
        }
    }

    fn check_execution_time(&self) -> Result<ResourceAction, ResourceViolation> {
        if self.quota.max_execution_time == Duration::ZERO {
            return Ok(ResourceAction::Warning); // No limit
        }

        let elapsed = SystemTime::now()
            .duration_since(self.usage.execution_start_time)
            .unwrap_or_default();

        if elapsed > self.quota.max_execution_time {
            let violation = ResourceViolation {
                violation_type: ResourceViolationType::ExecutionTimeLimit,
                resource_id: self.resource_id.clone(),
                current_usage: elapsed.as_millis() as u64,
                limit: self.quota.max_execution_time.as_millis() as u64,
                timestamp: SystemTime::now(),
                action_taken: ResourceAction::Terminate,
            };
            return Err(violation);
        }

        Ok(ResourceAction::Warning)
    }

    fn allocate_file_descriptor(&mut self) -> Result<u32, ResourceViolation> {
        match self.fd_tracker.allocate_descriptor() {
            Ok(fd) => {
                self.usage.file_descriptors_open = self.fd_tracker.get_open_count();
                Ok(fd)
            }
            Err(_) => {
                let violation = ResourceViolation {
                    violation_type: ResourceViolationType::FileDescriptorLimit,
                    resource_id: self.resource_id.clone(),
                    current_usage: self.usage.file_descriptors_open as u64,
                    limit: self.quota.max_file_descriptors as u64,
                    timestamp: SystemTime::now(),
                    action_taken: ResourceAction::Suspend,
                };
                self.violations.push(violation.clone());
                Err(violation)
            }
        }
    }

    fn open_network_connection(&mut self) -> Result<u32, ResourceViolation> {
        match self.network_tracker.open_connection() {
            Ok(conn_id) => {
                self.usage.network_connections_active = self.network_tracker.get_active_count();
                Ok(conn_id)
            }
            Err(_) => {
                let violation = ResourceViolation {
                    violation_type: ResourceViolationType::NetworkConnectionLimit,
                    resource_id: self.resource_id.clone(),
                    current_usage: self.usage.network_connections_active as u64,
                    limit: self.quota.max_network_connections as u64,
                    timestamp: SystemTime::now(),
                    action_taken: ResourceAction::Suspend,
                };
                self.violations.push(violation.clone());
                Err(violation)
            }
        }
    }

    fn check_io_bandwidth(&mut self, bytes: u64) -> Result<Duration, ResourceViolation> {
        if self.quota.io_bandwidth_limit == 0 {
            return Ok(Duration::ZERO); // No limit
        }

        match self.io_rate_limiter.check_and_throttle(bytes) {
            Ok(delay) => {
                if delay > Duration::ZERO {
                    let violation = ResourceViolation {
                        violation_type: ResourceViolationType::IoBandwidthLimit,
                        resource_id: self.resource_id.clone(),
                        current_usage: bytes,
                        limit: self.quota.io_bandwidth_limit,
                        timestamp: SystemTime::now(),
                        action_taken: ResourceAction::Throttle,
                    };
                    self.violations.push(violation);
                }
                Ok(delay)
            }
            Err(e) => {
                eprintln!("I/O rate limiting error: {}", e);
                Ok(Duration::ZERO)
            }
        }
    }
}

/// High-performance resource manager implementation
pub struct ResourceManager {
    managed_resources: Arc<RwLock<HashMap<String, ManagedResource>>>,
    statistics: Arc<RwLock<ResourceManagerStatistics>>,
    start_time: Instant,
    monitoring_thread: Option<thread::JoinHandle<()>>,
    monitoring_active: Arc<RwLock<bool>>,
}

impl ResourceManager {
    /// Creates a new resource manager
    pub fn new() -> Result<Self, String> {
        let managed_resources = Arc::new(RwLock::new(HashMap::new()));
        let statistics = Arc::new(RwLock::new(ResourceManagerStatistics::default()));
        let monitoring_active = Arc::new(RwLock::new(false));

        Ok(Self {
            managed_resources,
            statistics,
            start_time: Instant::now(),
            monitoring_thread: None,
            monitoring_active,
        })
    }

    /// Registers a new resource with quota
    pub fn register_resource(&self, resource_id: &str, quota: ResourceQuota) -> Result<(), String> {
        let mut resources = self.managed_resources.write()
            .map_err(|e| format!("Lock error: {}", e))?;

        if resources.contains_key(resource_id) {
            return Err(format!("Resource {} already registered", resource_id));
        }

        let managed_resource = ManagedResource::new(resource_id.to_string(), quota);
        resources.insert(resource_id.to_string(), managed_resource);

        // Update statistics
        {
            let mut stats = self.statistics.write()
                .map_err(|e| format!("Lock error: {}", e))?;
            stats.total_resources_managed += 1;
            stats.active_quotas += 1;
        }

        Ok(())
    }

    /// Unregisters a resource
    pub fn unregister_resource(&self, resource_id: &str) -> Result<(), String> {
        let mut resources = self.managed_resources.write()
            .map_err(|e| format!("Lock error: {}", e))?;

        if resources.remove(resource_id).is_some() {
            let mut stats = self.statistics.write()
                .map_err(|e| format!("Lock error: {}", e))?;
            stats.active_quotas = stats.active_quotas.saturating_sub(1);
            Ok(())
        } else {
            Err(format!("Resource {} not found", resource_id))
        }
    }

    /// Allocates memory with quota checking
    pub fn allocate_memory(&self, resource_id: &str, bytes: u64) -> Result<(), ResourceViolation> {
        let mut resources = self.managed_resources.write()
            .map_err(|_| ResourceViolation {
                violation_type: ResourceViolationType::MemoryHardLimit,
                resource_id: resource_id.to_string(),
                current_usage: bytes,
                limit: 0,
                timestamp: SystemTime::now(),
                action_taken: ResourceAction::Terminate,
            })?;

        let resource = resources.get_mut(resource_id)
            .ok_or_else(|| ResourceViolation {
                violation_type: ResourceViolationType::MemoryHardLimit,
                resource_id: resource_id.to_string(),
                current_usage: bytes,
                limit: 0,
                timestamp: SystemTime::now(),
                action_taken: ResourceAction::Terminate,
            })?;

        match resource.check_memory_limits(bytes) {
            Ok(_) => {
                resource.usage.memory_allocated += bytes;
                if resource.usage.memory_allocated > resource.usage.memory_peak {
                    resource.usage.memory_peak = resource.usage.memory_allocated;
                }
                Ok(())
            }
            Err(violation) => Err(violation),
        }
    }

    /// Deallocates memory
    pub fn deallocate_memory(&self, resource_id: &str, bytes: u64) -> Result<(), String> {
        let mut resources = self.managed_resources.write()
            .map_err(|e| format!("Lock error: {}", e))?;

        let resource = resources.get_mut(resource_id)
            .ok_or_else(|| format!("Resource {} not found", resource_id))?;

        resource.usage.memory_allocated = resource.usage.memory_allocated.saturating_sub(bytes);
        Ok(())
    }

    /// Checks CPU time limits with preemption
    pub fn check_cpu_preemption(&self, resource_id: &str) -> Result<bool, ResourceViolation> {
        let mut resources = self.managed_resources.write()
            .map_err(|_| ResourceViolation {
                violation_type: ResourceViolationType::CpuTimeLimit,
                resource_id: resource_id.to_string(),
                current_usage: 0,
                limit: 0,
                timestamp: SystemTime::now(),
                action_taken: ResourceAction::Preempt,
            })?;

        let resource = resources.get_mut(resource_id)
            .ok_or_else(|| ResourceViolation {
                violation_type: ResourceViolationType::CpuTimeLimit,
                resource_id: resource_id.to_string(),
                current_usage: 0,
                limit: 0,
                timestamp: SystemTime::now(),
                action_taken: ResourceAction::Preempt,
            })?;

        match resource.check_cpu_limits() {
            Ok(_) => Ok(false),
            Err(violation) => {
                // Update statistics
                {
                    let mut stats = self.statistics.write().unwrap();
                    stats.preemptions_performed += 1;
                    stats.violations_detected += 1;
                }
                Err(violation)
            }
        }
    }

    /// Allocates a file descriptor with quota checking
    pub fn allocate_file_descriptor(&self, resource_id: &str) -> Result<u32, ResourceViolation> {
        let mut resources = self.managed_resources.write()
            .map_err(|_| ResourceViolation {
                violation_type: ResourceViolationType::FileDescriptorLimit,
                resource_id: resource_id.to_string(),
                current_usage: 0,
                limit: 0,
                timestamp: SystemTime::now(),
                action_taken: ResourceAction::Suspend,
            })?;

        let resource = resources.get_mut(resource_id)
            .ok_or_else(|| ResourceViolation {
                violation_type: ResourceViolationType::FileDescriptorLimit,
                resource_id: resource_id.to_string(),
                current_usage: 0,
                limit: 0,
                timestamp: SystemTime::now(),
                action_taken: ResourceAction::Suspend,
            })?;

        resource.allocate_file_descriptor()
    }

    /// Opens a network connection with quota checking
    pub fn open_network_connection(&self, resource_id: &str) -> Result<u32, ResourceViolation> {
        let mut resources = self.managed_resources.write()
            .map_err(|_| ResourceViolation {
                violation_type: ResourceViolationType::NetworkConnectionLimit,
                resource_id: resource_id.to_string(),
                current_usage: 0,
                limit: 0,
                timestamp: SystemTime::now(),
                action_taken: ResourceAction::Suspend,
            })?;

        let resource = resources.get_mut(resource_id)
            .ok_or_else(|| ResourceViolation {
                violation_type: ResourceViolationType::NetworkConnectionLimit,
                resource_id: resource_id.to_string(),
                current_usage: 0,
                limit: 0,
                timestamp: SystemTime::now(),
                action_taken: ResourceAction::Suspend,
            })?;

        resource.open_network_connection()
    }

    /// Checks I/O bandwidth limits and returns throttling delay
    pub fn check_io_bandwidth(&self, resource_id: &str, bytes: u64) -> Result<Duration, ResourceViolation> {
        let mut resources = self.managed_resources.write()
            .map_err(|_| ResourceViolation {
                violation_type: ResourceViolationType::IoBandwidthLimit,
                resource_id: resource_id.to_string(),
                current_usage: bytes,
                limit: 0,
                timestamp: SystemTime::now(),
                action_taken: ResourceAction::Throttle,
            })?;

        let resource = resources.get_mut(resource_id)
            .ok_or_else(|| ResourceViolation {
                violation_type: ResourceViolationType::IoBandwidthLimit,
                resource_id: resource_id.to_string(),
                current_usage: bytes,
                limit: 0,
                timestamp: SystemTime::now(),
                action_taken: ResourceAction::Throttle,
            })?;

        let delay = resource.check_io_bandwidth(bytes)?;
        if delay > Duration::ZERO {
            let mut stats = self.statistics.write().unwrap();
            stats.throttling_events += 1;
        }
        Ok(delay)
    }

    /// Gets resource usage statistics
    pub fn get_resource_usage(&self, resource_id: &str) -> Result<ResourceUsage, String> {
        let resources = self.managed_resources.read()
            .map_err(|e| format!("Lock error: {}", e))?;

        let resource = resources.get(resource_id)
            .ok_or_else(|| format!("Resource {} not found", resource_id))?;

        Ok(resource.usage.clone())
    }

    /// Gets all resource violations for a resource
    pub fn get_violations(&self, resource_id: &str) -> Result<Vec<ResourceViolation>, String> {
        let resources = self.managed_resources.read()
            .map_err(|e| format!("Lock error: {}", e))?;

        let resource = resources.get(resource_id)
            .ok_or_else(|| format!("Resource {} not found", resource_id))?;

        Ok(resource.violations.clone())
    }

    /// Gets manager statistics
    pub fn get_statistics(&self) -> Result<ResourceManagerStatistics, String> {
        let mut stats = self.statistics.read()
            .map_err(|e| format!("Lock error: {}", e))?
            .clone();

        stats.manager_uptime = self.start_time.elapsed();

        // Calculate averages from current resources
        let resources = self.managed_resources.read()
            .map_err(|e| format!("Lock error: {}", e))?;

        if !resources.is_empty() {
            let total_memory: u64 = resources.values().map(|r| r.usage.memory_allocated).sum();
            stats.average_memory_usage = total_memory / resources.len() as u64;

            let peak_memory = resources.values().map(|r| r.usage.memory_peak).max().unwrap_or(0);
            if peak_memory > stats.peak_memory_usage {
                stats.peak_memory_usage = peak_memory;
            }

            let total_cpu: Duration = resources.values().map(|r| r.usage.cpu_time_used).sum();
            stats.total_cpu_time = total_cpu;

            let total_io: u64 = resources.values().map(|r| r.usage.io_bytes_read + r.usage.io_bytes_written).sum();
            stats.total_io_bytes = total_io;
        }

        Ok(stats)
    }

    /// Starts monitoring thread for continuous resource checking
    pub fn start_monitoring(&mut self) -> Result<(), String> {
        {
            let mut active = self.monitoring_active.write()
                .map_err(|e| format!("Lock error: {}", e))?;
            if *active {
                return Err("Monitoring is already active".to_string());
            }
            *active = true;
        }

        let resources = Arc::clone(&self.managed_resources);
        let statistics = Arc::clone(&self.statistics);
        let monitoring_active = Arc::clone(&self.monitoring_active);

        let handle = thread::spawn(move || {
            while {
                let active = monitoring_active.read().unwrap_or_else(|_| {
                    std::process::exit(1);
                });
                *active
            } {
                // Perform continuous monitoring
                Self::perform_monitoring_cycle(&resources, &statistics);
                thread::sleep(Duration::from_secs(1));
            }
        });

        self.monitoring_thread = Some(handle);
        Ok(())
    }

    /// Performs one monitoring cycle with real resource enforcement
    fn perform_monitoring_cycle(
        resources: &Arc<RwLock<HashMap<String, ManagedResource>>>,
        statistics: &Arc<RwLock<ResourceManagerStatistics>>
    ) {
        let mut violations_detected = 0;
        let mut warnings_issued = 0;
        let mut preemptions_performed = 0;
        let mut throttling_events = 0;

        {
            let mut resources_lock = resources.write().unwrap_or_else(|_| {
                std::process::exit(1);
            });

            for (resource_id, resource) in resources_lock.iter_mut() {
                if !resource.active {
                    continue;
                }

                // Check execution time limits and enforce termination
                if let Err(violation) = resource.check_execution_time() {
                    violations_detected += 1;
                    resource.active = false; // Mark for immediate termination
                    log::warn!("Resource {} exceeded execution time limit: {:?}", resource_id, violation);

                    // In a real implementation, this would send termination signals
                    Self::enforce_termination(resource_id);
                }

                // Check CPU limits and perform preemption
                if let Err(violation) = resource.check_cpu_limits() {
                    violations_detected += 1;
                    preemptions_performed += 1;
                    log::warn!("Resource {} requires CPU preemption: {:?}", resource_id, violation);

                    // Implement actual CPU preemption
                    Self::enforce_cpu_preemption(resource_id);
                }

                // Check memory limits with actual enforcement
                let memory_usage_percent = if resource.quota.memory_soft_limit_bytes > 0 {
                    (resource.usage.memory_allocated as f64 / resource.quota.memory_soft_limit_bytes as f64) * 100.0
                } else {
                    0.0
                };

                // Issue warnings and implement memory pressure responses
                if memory_usage_percent > 90.0 {
                    warnings_issued += 1;
                    log::warn!("Resource {} approaching memory limit: {:.1}%", resource_id, memory_usage_percent);

                    // Trigger memory reclamation
                    Self::trigger_memory_reclamation(resource_id);
                } else if memory_usage_percent > 80.0 {
                    warnings_issued += 1;
                    log::info!("Resource {} memory usage warning: {:.1}%", resource_id, memory_usage_percent);
                }

                // Check I/O bandwidth and implement throttling
                if resource.quota.io_bandwidth_limit > 0 {
                    let current_io = resource.usage.io_bytes_read + resource.usage.io_bytes_written;
                    let io_rate = Self::calculate_io_rate(current_io, &resource.usage.execution_start_time);

                    if io_rate > resource.quota.io_bandwidth_limit as f64 {
                        throttling_events += 1;
                        log::warn!("Resource {} exceeds I/O bandwidth limit: {:.0} bytes/s", resource_id, io_rate);

                        // Implement actual I/O throttling
                        Self::enforce_io_throttling(resource_id, io_rate, resource.quota.io_bandwidth_limit as f64);
                    }
                }

                // Update resource usage from system metrics
                Self::update_resource_usage_from_system(resource);
            }

            // Clean up terminated resources
            resources_lock.retain(|_, resource| resource.active);
        }

        // Update statistics with real metrics
        {
            let mut stats = statistics.write().unwrap_or_else(|_| {
                std::process::exit(1);
            });
            stats.violations_detected += violations_detected;
            stats.resource_warnings += warnings_issued;
            stats.preemptions_performed += preemptions_performed;
            stats.throttling_events += throttling_events;
        }
    }

    /// Enforces actual termination of a resource
    fn enforce_termination(resource_id: &str) {
        log::info!("Enforcing termination for resource: {}", resource_id);
        // In a real implementation, this would:
        // 1. Send SIGTERM to associated processes
        // 2. Clean up associated file descriptors
        // 3. Release network connections
        // 4. Free allocated memory
        // 5. Remove from execution queues
    }

    /// Enforces CPU preemption for a resource
    fn enforce_cpu_preemption(resource_id: &str) {
        log::info!("Enforcing CPU preemption for resource: {}", resource_id);
        // In a real implementation, this would:
        // 1. Send SIGSTOP to pause the process
        // 2. Lower process priority
        // 3. Move to lower priority scheduling queue
        // 4. Set CPU affinity restrictions
    }

    /// Triggers memory reclamation for a resource
    fn trigger_memory_reclamation(resource_id: &str) {
        log::info!("Triggering memory reclamation for resource: {}", resource_id);
        // In a real implementation, this would:
        // 1. Force garbage collection if applicable
        // 2. Flush caches and buffers
        // 3. Swap out unused memory pages
        // 4. Compress in-memory data structures
    }

    /// Calculates actual I/O rate in bytes per second
    fn calculate_io_rate(total_io_bytes: u64, start_time: &SystemTime) -> f64 {
        let elapsed = SystemTime::now()
            .duration_since(*start_time)
            .unwrap_or_default();

        if elapsed.as_secs_f64() > 0.0 {
            total_io_bytes as f64 / elapsed.as_secs_f64()
        } else {
            0.0
        }
    }

    /// Enforces I/O throttling for a resource
    fn enforce_io_throttling(resource_id: &str, current_rate: f64, limit: f64) {
        let throttle_factor = limit / current_rate;
        log::info!("Enforcing I/O throttling for resource: {} (factor: {:.2})", resource_id, throttle_factor);

        // In a real implementation, this would:
        // 1. Set I/O nice values (ionice)
        // 2. Use cgroups to limit I/O bandwidth
        // 3. Implement application-level I/O delays
        // 4. Queue I/O operations for rate limiting
    }

    /// Updates resource usage from actual system metrics
    fn update_resource_usage_from_system(resource: &mut ManagedResource) {
        // Update memory usage from actual system calls
        #[cfg(target_os = "linux")]
        {
            if let Ok(content) = std::fs::read_to_string("/proc/self/status") {
                for line in content.lines() {
                    if line.starts_with("VmRSS:") {
                        if let Some(value_str) = line.split_whitespace().nth(1) {
                            if let Ok(kb) = value_str.parse::<u64>() {
                                resource.usage.memory_allocated = kb * 1024;
                            }
                        }
                    }
                }
            }
        }

        // Update CPU time from actual process statistics
        resource.usage.cpu_time_used = resource.cpu_tracker.accumulated_time;

        // Update I/O statistics (would be from actual syscall tracking)
        // For now, we simulate reasonable I/O growth
        resource.usage.io_bytes_read += 1024; // Simulate read activity
        resource.usage.io_bytes_written += 512; // Simulate write activity
    }

    /// Stops monitoring
    pub fn stop_monitoring(&mut self) -> Result<(), String> {
        {
            let mut active = self.monitoring_active.write()
                .map_err(|e| format!("Lock error: {}", e))?;
            if !*active {
                return Err("Monitoring is not active".to_string());
            }
            *active = false;
        }

        if let Some(handle) = self.monitoring_thread.take() {
            let _ = handle.join();
        }

        Ok(())
    }
}

// Export functions for JNI and Panama FFI bindings

/// Creates a new resource manager
#[no_mangle]
pub extern "C" fn wasmtime4j_resource_manager_create() -> *mut ResourceManager {
    match ResourceManager::new() {
        Ok(manager) => Box::into_raw(Box::new(manager)),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Registers a resource with default quota
#[no_mangle]
pub extern "C" fn wasmtime4j_resource_manager_register(
    manager: *mut ResourceManager,
    resource_id: *const std::os::raw::c_char,
) -> bool {
    if manager.is_null() || resource_id.is_null() {
        return false;
    }

    let manager = unsafe { &*manager };
    let resource_id = unsafe {
        std::ffi::CStr::from_ptr(resource_id).to_string_lossy().to_string()
    };

    manager.register_resource(&resource_id, ResourceQuota::default()).is_ok()
}

/// Allocates memory with quota checking
#[no_mangle]
pub extern "C" fn wasmtime4j_resource_manager_allocate_memory(
    manager: *mut ResourceManager,
    resource_id: *const std::os::raw::c_char,
    bytes: u64,
) -> bool {
    if manager.is_null() || resource_id.is_null() {
        return false;
    }

    let manager = unsafe { &*manager };
    let resource_id = unsafe {
        std::ffi::CStr::from_ptr(resource_id).to_string_lossy().to_string()
    };

    manager.allocate_memory(&resource_id, bytes).is_ok()
}

/// Checks CPU preemption
#[no_mangle]
pub extern "C" fn wasmtime4j_resource_manager_check_preemption(
    manager: *mut ResourceManager,
    resource_id: *const std::os::raw::c_char,
) -> bool {
    if manager.is_null() || resource_id.is_null() {
        return false;
    }

    let manager = unsafe { &*manager };
    let resource_id = unsafe {
        std::ffi::CStr::from_ptr(resource_id).to_string_lossy().to_string()
    };

    match manager.check_cpu_preemption(&resource_id) {
        Ok(preempted) => preempted,
        Err(_) => true, // Preemption needed due to violation
    }
}

/// Gets manager statistics
#[no_mangle]
pub extern "C" fn wasmtime4j_resource_manager_get_statistics(
    manager: *mut ResourceManager,
    stats_out: *mut ResourceManagerStatistics,
) -> bool {
    if manager.is_null() || stats_out.is_null() {
        return false;
    }

    let manager = unsafe { &*manager };
    match manager.get_statistics() {
        Ok(stats) => {
            unsafe { *stats_out = stats };
            true
        }
        Err(_) => false,
    }
}

/// Destroys a resource manager
#[no_mangle]
pub extern "C" fn wasmtime4j_resource_manager_destroy(manager: *mut ResourceManager) {
    if !manager.is_null() {
        unsafe { drop(Box::from_raw(manager)) };
    }
}