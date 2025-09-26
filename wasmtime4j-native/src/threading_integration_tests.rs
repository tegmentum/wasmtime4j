//! Comprehensive integration tests for advanced WebAssembly threading optimizations.
//! These tests validate all 8 threading optimization modules working together.

#[cfg(test)]
mod tests {
    use crate::error::WasmtimeError;
    use std::{
        sync::{Arc, Barrier, Mutex},
        thread::{self, ThreadId},
        time::{Duration, Instant},
        collections::HashMap,
        sync::atomic::{AtomicU64, AtomicBool, Ordering},
    };

    /// Integration test for work-stealing scheduler with real WebAssembly workloads
    #[test]
    fn test_work_stealing_with_wasm_execution() {
        let config = WorkStealingConfig {
            num_workers: 4,
            steal_attempts: 3,
            backoff_strategy: BackoffStrategy::Exponential { initial_delay_ns: 1000, max_delay_ns: 1000000 },
            numa_awareness: true,
            preferred_cores: vec![0, 1, 2, 3],
            locality_factor: 0.8,
        };

        let scheduler = WorkStealingScheduler::new(config).expect("Failed to create scheduler");
        let execution_count = Arc::new(AtomicU64::new(0));

        // Create multiple WebAssembly computation tasks
        for i in 0..100 {
            let count = execution_count.clone();
            let task = Arc::new(WorkStealingTask::new(
                format!("wasm_task_{}", i),
                TaskPriority::Normal,
                move || {
                    // Simulate WebAssembly module execution
                    let engine = Engine::default();
                    let wat = r#"
                        (module
                            (func $compute (result i32)
                                (local $i i32)
                                (local $sum i32)
                                (local.set $i (i32.const 0))
                                (local.set $sum (i32.const 0))
                                (loop $loop
                                    (local.set $sum
                                        (i32.add (local.get $sum) (local.get $i)))
                                    (local.set $i
                                        (i32.add (local.get $i) (i32.const 1)))
                                    (br_if $loop
                                        (i32.lt_s (local.get $i) (i32.const 1000)))
                                )
                                (local.get $sum)
                            )
                            (export "compute" (func $compute))
                        )
                    "#;

                    if let Ok(module) = Module::new(&engine, wat) {
                        let mut store = Store::new(&engine, ());
                        if let Ok(instance) = Instance::new(&mut store, &module, &[]) {
                            if let Ok(compute) = instance.get_typed_func::<(), i32>(&mut store, "compute") {
                                let _ = compute.call(&mut store, ());
                            }
                        }
                    }

                    count.fetch_add(1, Ordering::SeqCst);
                    Ok(())
                },
            ));

            scheduler.submit_task(task).expect("Failed to submit task");
        }

        // Wait for all tasks to complete
        let start_time = Instant::now();
        while execution_count.load(Ordering::SeqCst) < 100 && start_time.elapsed() < Duration::from_secs(30) {
            thread::sleep(Duration::from_millis(10));
        }

        let stats = scheduler.get_statistics();
        assert_eq!(execution_count.load(Ordering::SeqCst), 100);
        assert!(stats.tasks_completed >= 100);
        assert!(stats.total_steal_attempts > 0);

        println!("Work-stealing test completed: {} tasks executed, {} steals attempted",
                stats.tasks_completed, stats.total_steal_attempts);
    }

    /// Integration test for thread affinity with CPU topology awareness
    #[test]
    fn test_thread_affinity_with_numa_awareness() {
        let config = AffinityConfig {
            numa_aware: true,
            prefer_local_memory: true,
            load_balancing: true,
            priority_based: true,
            core_isolation: Some(vec![0, 1]),
            max_threads_per_core: 2,
        };

        let manager = ThreadAffinityManager::new(config).expect("Failed to create affinity manager");
        let num_threads = 8;
        let barrier = Arc::new(Barrier::new(num_threads + 1));
        let thread_assignments = Arc::new(std::sync::Mutex::new(HashMap::new()));

        let handles: Vec<_> = (0..num_threads)
            .map(|i| {
                let manager = manager.clone();
                let barrier = barrier.clone();
                let assignments = thread_assignments.clone();

                thread::spawn(move || {
                    let thread_id = thread::current().id();

                    // Request CPU core assignment
                    manager.assign_thread_to_core(thread_id, ThreadPriority::Normal)
                        .expect("Failed to assign thread to core");

                    // Get current assignment
                    let assignment = manager.get_thread_assignment(thread_id)
                        .expect("Failed to get thread assignment");

                    assignments.lock().unwrap().insert(i, assignment);

                    barrier.wait();

                    // Perform some work to validate affinity
                    let mut sum = 0u64;
                    for j in 0..10000 {
                        sum = sum.wrapping_add(j);
                    }

                    // Update thread metrics
                    manager.update_thread_metrics(thread_id, ThreadMetrics {
                        cpu_time_ns: 1000000,
                        cache_misses: 100,
                        memory_accesses: 50000,
                        numa_remote_accesses: 10,
                    }).ok();

                    sum
                })
            })
            .collect();

        barrier.wait(); // Wait for all threads to start and get assignments

        // Collect results
        let results: Vec<u64> = handles.into_iter().map(|h| h.join().unwrap()).collect();
        assert_eq!(results.len(), num_threads);

        let assignments = thread_assignments.lock().unwrap();
        assert_eq!(assignments.len(), num_threads);

        // Verify that threads are distributed across available cores
        let mut core_usage = HashMap::new();
        for assignment in assignments.values() {
            *core_usage.entry(assignment.assigned_core).or_insert(0) += 1;
        }

        // Should have some distribution across cores
        assert!(core_usage.len() > 1, "Threads should be distributed across multiple cores");

        let stats = manager.get_statistics();
        println!("Thread affinity test completed: {} assignments made, {} cores used",
                stats.successful_assignments, core_usage.len());
    }

    /// Integration test for lock-free data structures under high contention
    #[test]
    fn test_lockfree_structures_high_contention() {
        let num_threads = 16;
        let operations_per_thread = 1000;

        // Test lock-free queue
        let queue = Arc::new(LockFreeQueue::<u64>::new());
        let total_enqueued = Arc::new(AtomicU64::new(0));
        let total_dequeued = Arc::new(AtomicU64::new(0));
        let barrier = Arc::new(Barrier::new(num_threads));

        let handles: Vec<_> = (0..num_threads)
            .map(|thread_id| {
                let queue = queue.clone();
                let enqueued = total_enqueued.clone();
                let dequeued = total_dequeued.clone();
                let barrier = barrier.clone();

                thread::spawn(move || {
                    barrier.wait();

                    for i in 0..operations_per_thread {
                        let value = (thread_id as u64) * operations_per_thread + i;

                        // Alternate between enqueue and dequeue
                        if i % 2 == 0 {
                            queue.enqueue(value).expect("Failed to enqueue");
                            enqueued.fetch_add(1, Ordering::SeqCst);
                        } else {
                            if let Ok(_) = queue.dequeue() {
                                dequeued.fetch_add(1, Ordering::SeqCst);
                            }
                        }

                        // Small delay to increase contention
                        if i % 100 == 0 {
                            thread::yield_now();
                        }
                    }
                })
            })
            .collect();

        // Wait for all threads to complete
        for handle in handles {
            handle.join().unwrap();
        }

        let queue_stats = queue.get_statistics();
        println!("Lock-free queue test: {} enqueued, {} dequeued, {} contentions",
                total_enqueued.load(Ordering::SeqCst),
                total_dequeued.load(Ordering::SeqCst),
                queue_stats.contentions);

        // Test lock-free hash table
        let hash_table = Arc::new(LockFreeHashTable::<u64, String>::new(1024));
        let barrier = Arc::new(Barrier::new(num_threads));

        let handles: Vec<_> = (0..num_threads)
            .map(|thread_id| {
                let table = hash_table.clone();
                let barrier = barrier.clone();

                thread::spawn(move || {
                    barrier.wait();

                    for i in 0..operations_per_thread {
                        let key = (thread_id as u64) * operations_per_thread + i;
                        let value = format!("value_{}_{}", thread_id, i);

                        // Insert
                        table.insert(key, value.clone()).expect("Failed to insert");

                        // Lookup
                        if let Ok(found_value) = table.get(&key) {
                            assert_eq!(found_value, value);
                        }

                        // Occasionally remove
                        if i % 10 == 0 {
                            table.remove(&key).ok();
                        }
                    }
                })
            })
            .collect();

        for handle in handles {
            handle.join().unwrap();
        }

        let table_stats = hash_table.get_statistics();
        println!("Lock-free hash table test: {} operations, {} collisions",
                table_stats.total_operations, table_stats.hash_collisions);
    }

    /// Integration test for adaptive scaling with machine learning predictions
    #[test]
    fn test_adaptive_scaling_with_workload_prediction() {
        let config = ScalingConfig {
            min_threads: 2,
            max_threads: 16,
            target_cpu_utilization: 0.75,
            scale_up_threshold: 0.85,
            scale_down_threshold: 0.5,
            prediction_window: Duration::from_millis(1000),
            learning_rate: 0.01,
            momentum: 0.9,
            regularization: 0.001,
        };

        let manager = AdaptiveScalingManager::new(config).expect("Failed to create scaling manager");
        let active_workers = Arc::new(AtomicU64::new(2)); // Start with min threads

        // Simulate varying workload patterns
        for phase in 0..5 {
            println!("Testing scaling phase {}", phase + 1);

            let workload_intensity = match phase {
                0 => 0.3, // Low load
                1 => 0.7, // Medium load
                2 => 0.9, // High load
                3 => 0.5, // Medium load again
                4 => 0.2, // Low load again
                _ => 0.5,
            };

            // Submit workload metrics
            let metrics = WorkloadMetrics {
                cpu_utilization: workload_intensity,
                memory_utilization: workload_intensity * 0.8,
                queue_depth: (workload_intensity * 1000.0) as u64,
                throughput: workload_intensity * 10000.0,
                response_time_ms: (1.0 / workload_intensity) * 10.0,
                error_rate: 0.01,
            };

            manager.update_metrics(metrics).expect("Failed to update metrics");

            // Get scaling decision
            let decision = manager.make_scaling_decision().expect("Failed to make scaling decision");

            match decision.action {
                ScalingAction::ScaleUp { target_threads } => {
                    active_workers.store(target_threads as u64, Ordering::SeqCst);
                    println!("Scaled up to {} threads (confidence: {:.2})",
                            target_threads, decision.confidence);
                }
                ScalingAction::ScaleDown { target_threads } => {
                    active_workers.store(target_threads as u64, Ordering::SeqCst);
                    println!("Scaled down to {} threads (confidence: {:.2})",
                            target_threads, decision.confidence);
                }
                ScalingAction::NoChange => {
                    println!("No scaling needed (confidence: {:.2})", decision.confidence);
                }
            }

            // Wait before next phase
            thread::sleep(Duration::from_millis(500));
        }

        let stats = manager.get_statistics();
        println!("Adaptive scaling test completed: {} decisions made, {} predictions",
                stats.decisions_made, stats.predictions_made);

        assert!(stats.decisions_made > 0);
        assert!(stats.predictions_made > 0);
        assert!(stats.accuracy > 0.5); // Should have reasonable prediction accuracy
    }

    /// Integration test for advanced synchronization primitives
    #[test]
    fn test_advanced_synchronization_primitives() {
        let num_readers = 8;
        let num_writers = 2;
        let operations_per_thread = 100;

        // Test advanced RwLock with priority queues
        let data = Arc::new(AdvancedRwLock::new(0u64));
        let barrier = Arc::new(Barrier::new(num_readers + num_writers));
        let total_reads = Arc::new(AtomicU64::new(0));
        let total_writes = Arc::new(AtomicU64::new(0));

        // Reader threads
        let reader_handles: Vec<_> = (0..num_readers)
            .map(|reader_id| {
                let data = data.clone();
                let barrier = barrier.clone();
                let reads = total_reads.clone();

                thread::spawn(move || {
                    barrier.wait();

                    for i in 0..operations_per_thread {
                        let priority = if i % 10 == 0 {
                            Priority::High
                        } else {
                            Priority::Normal
                        };

                        let _guard = data.read_with_priority(priority)
                            .expect("Failed to acquire read lock");
                        let value = *_guard;
                        reads.fetch_add(1, Ordering::SeqCst);

                        // Simulate read work
                        thread::sleep(Duration::from_micros(10));

                        // Verify data consistency
                        assert!(value % 2 == 0, "Data should always be even (reader {})", reader_id);
                    }
                })
            })
            .collect();

        // Writer threads
        let writer_handles: Vec<_> = (0..num_writers)
            .map(|writer_id| {
                let data = data.clone();
                let barrier = barrier.clone();
                let writes = total_writes.clone();

                thread::spawn(move || {
                    barrier.wait();

                    for i in 0..operations_per_thread {
                        let priority = if i % 5 == 0 {
                            Priority::High
                        } else {
                            Priority::Normal
                        };

                        let mut guard = data.write_with_priority(priority)
                            .expect("Failed to acquire write lock");

                        // Ensure we write even numbers for consistency check
                        *guard = writer_id as u64 * operations_per_thread * 2 + i * 2;
                        writes.fetch_add(1, Ordering::SeqCst);

                        // Simulate write work
                        thread::sleep(Duration::from_micros(50));
                    }
                })
            })
            .collect();

        // Wait for all threads
        for handle in reader_handles {
            handle.join().unwrap();
        }
        for handle in writer_handles {
            handle.join().unwrap();
        }

        let rw_stats = data.get_statistics();
        println!("Advanced RwLock test: {} reads, {} writes, avg wait time: {:.2}ms",
                total_reads.load(Ordering::SeqCst),
                total_writes.load(Ordering::SeqCst),
                rw_stats.average_wait_time_ns as f64 / 1_000_000.0);

        // Test advanced semaphore
        let semaphore = Arc::new(AdvancedSemaphore::new(4)); // Allow 4 concurrent access
        let active_count = Arc::new(AtomicU64::new(0));
        let max_concurrent = Arc::new(AtomicU64::new(0));
        let barrier = Arc::new(Barrier::new(10));

        let sem_handles: Vec<_> = (0..10)
            .map(|_| {
                let sem = semaphore.clone();
                let active = active_count.clone();
                let max_conc = max_concurrent.clone();
                let barrier = barrier.clone();

                thread::spawn(move || {
                    barrier.wait();

                    let _permit = sem.acquire_with_priority(Priority::Normal)
                        .expect("Failed to acquire semaphore");

                    let current_active = active.fetch_add(1, Ordering::SeqCst) + 1;

                    // Update maximum concurrent access
                    let mut current_max = max_conc.load(Ordering::SeqCst);
                    while current_active > current_max {
                        match max_conc.compare_exchange_weak(
                            current_max,
                            current_active,
                            Ordering::SeqCst,
                            Ordering::Relaxed
                        ) {
                            Ok(_) => break,
                            Err(new_max) => current_max = new_max,
                        }
                    }

                    // Simulate critical section work
                    thread::sleep(Duration::from_millis(100));

                    active.fetch_sub(1, Ordering::SeqCst);
                })
            })
            .collect();

        for handle in sem_handles {
            handle.join().unwrap();
        }

        let sem_stats = semaphore.get_statistics();
        assert!(max_concurrent.load(Ordering::SeqCst) <= 4,
                "Semaphore should limit concurrent access to 4");

        println!("Advanced semaphore test: max concurrent = {}, avg wait time: {:.2}ms",
                max_concurrent.load(Ordering::SeqCst),
                sem_stats.average_wait_time_ns as f64 / 1_000_000.0);
    }

    /// Integration test for thread profiler and performance monitoring
    #[test]
    fn test_thread_profiler_performance_monitoring() {
        let config = ProfilerConfig {
            sampling_interval: Duration::from_millis(10),
            stack_depth: 16,
            enable_memory_tracking: true,
            enable_cpu_tracking: true,
            enable_function_timing: true,
            enable_contention_tracking: true,
            buffer_size: 10000,
        };

        let profiler = ThreadProfiler::new(config).expect("Failed to create profiler");
        profiler.start().expect("Failed to start profiler");

        let num_threads = 8;
        let barrier = Arc::new(Barrier::new(num_threads));
        let function_calls = Arc::new(AtomicU64::new(0));

        let handles: Vec<_> = (0..num_threads)
            .map(|thread_id| {
                let profiler = profiler.clone();
                let barrier = barrier.clone();
                let calls = function_calls.clone();

                thread::spawn(move || {
                    let thread_id_val = thread::current().id();

                    // Register thread with profiler
                    profiler.register_thread(thread_id_val, format!("worker_{}", thread_id))
                        .expect("Failed to register thread");

                    barrier.wait();

                    // Simulate various function calls with different characteristics
                    for i in 0..100 {
                        let function_name = format!("test_function_{}", i % 5);

                        // Start function timing
                        let start_time = profiler.start_function_timing(&function_name)
                            .expect("Failed to start timing");

                        // Simulate different types of work
                        match i % 4 {
                            0 => {
                                // CPU-intensive work
                                let mut sum = 0u64;
                                for j in 0..10000 {
                                    sum = sum.wrapping_add(j);
                                }
                                std::hint::black_box(sum);
                            }
                            1 => {
                                // Memory allocation work
                                let data: Vec<u8> = (0..1000).map(|x| x as u8).collect();
                                std::hint::black_box(data);
                            }
                            2 => {
                                // I/O-like delay
                                thread::sleep(Duration::from_micros(100));
                            }
                            3 => {
                                // Mixed work with contention simulation
                                let mutex = std::sync::Mutex::new(0);
                                let _guard = mutex.lock().unwrap();
                                thread::sleep(Duration::from_micros(10));
                            }
                            _ => {}
                        }

                        // End function timing
                        profiler.end_function_timing(&function_name, start_time)
                            .expect("Failed to end timing");

                        calls.fetch_add(1, Ordering::SeqCst);

                        // Sample memory usage periodically
                        if i % 20 == 0 {
                            let memory_info = MemoryInfo {
                                heap_used: 1024 * 1024 * (thread_id + 1) as u64,
                                heap_committed: 2 * 1024 * 1024 * (thread_id + 1) as u64,
                                non_heap_used: 512 * 1024,
                                gc_count: i as u64 / 20,
                                gc_time_ms: (i / 20) as u64,
                            };

                            profiler.update_memory_info(thread_id_val, memory_info)
                                .expect("Failed to update memory info");
                        }
                    }

                    // Unregister thread
                    profiler.unregister_thread(thread_id_val)
                        .expect("Failed to unregister thread");
                })
            })
            .collect();

        // Wait for all threads to complete
        for handle in handles {
            handle.join().unwrap();
        }

        // Generate performance report
        let report = profiler.generate_report().expect("Failed to generate report");
        profiler.stop().expect("Failed to stop profiler");

        println!("Thread profiler test completed:");
        println!("  Total function calls: {}", function_calls.load(Ordering::SeqCst));
        println!("  Threads monitored: {}", report.thread_count);
        println!("  Functions profiled: {}", report.function_profiles.len());
        println!("  Total samples: {}", report.total_samples);

        assert!(report.thread_count == num_threads as u64);
        assert!(report.function_profiles.len() >= 5); // Should have profiled our 5 test functions
        assert!(report.total_samples > 0);
    }

    /// Integration test for memory coordination and thread-safe sharing
    #[test]
    fn test_memory_coordination_thread_safe_sharing() {
        let config = CoordinatorConfig {
            max_shared_memories: 16,
            memory_pool_size: 64 * 1024 * 1024, // 64MB
            page_size: 4096,
            enable_numa_awareness: true,
            gc_threshold: 0.8,
            compaction_interval: Duration::from_secs(60),
        };

        let coordinator = MemoryCoordinator::new(config).expect("Failed to create coordinator");
        let num_threads = 8;
        let memory_size = 1024 * 1024; // 1MB per shared memory

        // Create multiple shared memories
        let shared_memories: Vec<_> = (0..4)
            .map(|i| {
                let memory_id = MemoryId(i);
                coordinator.create_shared_memory(memory_id, memory_size)
                    .expect("Failed to create shared memory");
                memory_id
            })
            .collect();

        let barrier = Arc::new(Barrier::new(num_threads));
        let total_operations = Arc::new(AtomicU64::new(0));
        let memory_errors = Arc::new(AtomicU64::new(0));

        let handles: Vec<_> = (0..num_threads)
            .map(|thread_id| {
                let coordinator = coordinator.clone();
                let memories = shared_memories.clone();
                let barrier = barrier.clone();
                let operations = total_operations.clone();
                let errors = memory_errors.clone();

                thread::spawn(move || {
                    barrier.wait();

                    for i in 0..1000 {
                        let memory_id = memories[i % memories.len()];

                        // Perform atomic operations on shared memory
                        match i % 4 {
                            0 => {
                                // Atomic read
                                let offset = (thread_id * 8) % (memory_size - 8);
                                if let Err(_) = coordinator.atomic_read_u64(memory_id, offset) {
                                    errors.fetch_add(1, Ordering::SeqCst);
                                }
                            }
                            1 => {
                                // Atomic write
                                let offset = (thread_id * 8) % (memory_size - 8);
                                let value = (thread_id as u64) << 32 | i as u64;
                                if let Err(_) = coordinator.atomic_write_u64(memory_id, offset, value) {
                                    errors.fetch_add(1, Ordering::SeqCst);
                                }
                            }
                            2 => {
                                // Atomic compare-and-swap
                                let offset = (thread_id * 8) % (memory_size - 8);
                                let expected = 0u64;
                                let new_value = thread_id as u64;
                                if let Err(_) = coordinator.atomic_compare_and_swap_u64(
                                    memory_id, offset, expected, new_value
                                ) {
                                    errors.fetch_add(1, Ordering::SeqCst);
                                }
                            }
                            3 => {
                                // Memory barrier operation
                                if let Err(_) = coordinator.memory_barrier(memory_id) {
                                    errors.fetch_add(1, Ordering::SeqCst);
                                }
                            }
                            _ => {}
                        }

                        operations.fetch_add(1, Ordering::SeqCst);

                        // Occasionally trigger memory management operations
                        if i % 100 == 0 {
                            coordinator.request_gc().ok();
                        }
                    }
                })
            })
            .collect();

        // Wait for all threads to complete
        for handle in handles {
            handle.join().unwrap();
        }

        // Clean up shared memories
        for memory_id in shared_memories {
            coordinator.destroy_shared_memory(memory_id)
                .expect("Failed to destroy shared memory");
        }

        let stats = coordinator.get_statistics();
        println!("Memory coordination test completed:");
        println!("  Total operations: {}", total_operations.load(Ordering::SeqCst));
        println!("  Memory errors: {}", memory_errors.load(Ordering::SeqCst));
        println!("  GC cycles: {}", stats.gc_cycles);
        println!("  Memory utilization: {:.2}%", stats.memory_utilization * 100.0);

        assert!(memory_errors.load(Ordering::SeqCst) == 0, "Should have no memory errors");
        assert!(total_operations.load(Ordering::SeqCst) == num_threads as u64 * 1000);
    }

    /// Integration test for deadlock detection and prevention
    #[test]
    fn test_deadlock_detection_and_prevention() {
        let config = DeadlockConfig {
            enable_detection: true,
            enable_prevention: true,
            detection_interval: Duration::from_millis(100),
            max_wait_time: Duration::from_secs(5),
            banker_algorithm: true,
            resource_ordering: true,
            priority_inheritance: true,
        };

        let system = DeadlockPreventionSystem::new(config)
            .expect("Failed to create deadlock prevention system");

        // Create resources that could potentially deadlock
        let resource_ids: Vec<_> = (0..4).map(ResourceId).collect();
        for &resource_id in &resource_ids {
            system.register_resource(resource_id, ResourceType::Mutex)
                .expect("Failed to register resource");
        }

        let num_threads = 8;
        let barrier = Arc::new(Barrier::new(num_threads));
        let successful_acquisitions = Arc::new(AtomicU64::new(0));
        let prevented_deadlocks = Arc::new(AtomicU64::new(0));
        let deadlock_detected = Arc::new(AtomicBool::new(false));

        let handles: Vec<_> = (0..num_threads)
            .map(|thread_id| {
                let system = system.clone();
                let resources = resource_ids.clone();
                let barrier = barrier.clone();
                let acquisitions = successful_acquisitions.clone();
                let prevented = prevented_deadlocks.clone();
                let detected = deadlock_detected.clone();

                thread::spawn(move || {
                    let thread_id_val = ThreadId::from(thread_id);

                    barrier.wait();

                    for i in 0..50 {
                        // Create potentially problematic resource acquisition patterns
                        let resource_pattern = match thread_id % 4 {
                            0 => vec![resources[0], resources[1]], // Forward order
                            1 => vec![resources[1], resources[0]], // Reverse order (potential deadlock)
                            2 => vec![resources[2], resources[3], resources[0]], // Complex pattern
                            3 => vec![resources[3], resources[1], resources[2]], // Another complex pattern
                            _ => vec![resources[0]],
                        };

                        // Attempt to acquire resources with deadlock prevention
                        let mut acquired_resources = Vec::new();
                        let mut acquisition_successful = true;

                        for &resource_id in &resource_pattern {
                            match system.request_resource(thread_id_val, resource_id, Priority::Normal) {
                                Ok(_) => {
                                    acquired_resources.push(resource_id);
                                }
                                Err(WasmtimeError::WouldDeadlock) => {
                                    prevented.fetch_add(1, Ordering::SeqCst);
                                    acquisition_successful = false;
                                    break;
                                }
                                Err(_) => {
                                    acquisition_successful = false;
                                    break;
                                }
                            }

                            // Small delay to increase chance of contention
                            thread::sleep(Duration::from_micros(10));
                        }

                        if acquisition_successful {
                            acquisitions.fetch_add(1, Ordering::SeqCst);

                            // Hold resources for a short time
                            thread::sleep(Duration::from_millis(1));
                        }

                        // Release all acquired resources in reverse order
                        for &resource_id in acquired_resources.iter().rev() {
                            system.release_resource(thread_id_val, resource_id)
                                .expect("Failed to release resource");
                        }

                        // Check if deadlock was detected
                        if system.check_for_deadlock().unwrap_or(false) {
                            detected.store(true, Ordering::SeqCst);
                            println!("Deadlock detected and resolved by thread {}", thread_id);
                        }

                        // Small delay between iterations
                        if i % 10 == 0 {
                            thread::yield_now();
                        }
                    }
                })
            })
            .collect();

        // Monitor deadlock detection in separate thread
        let monitor_system = system.clone();
        let monitor_detected = deadlock_detected.clone();
        let monitor_handle = thread::spawn(move || {
            for _ in 0..100 {
                thread::sleep(Duration::from_millis(50));

                if let Ok(deadlock_found) = monitor_system.check_for_deadlock() {
                    if deadlock_found {
                        monitor_detected.store(true, Ordering::SeqCst);
                        println!("Background monitor detected deadlock");

                        // Attempt recovery
                        if let Err(e) = monitor_system.recover_from_deadlock() {
                            println!("Deadlock recovery failed: {:?}", e);
                        } else {
                            println!("Deadlock recovery successful");
                        }
                    }
                }
            }
        });

        // Wait for all threads to complete
        for handle in handles {
            handle.join().unwrap();
        }
        monitor_handle.join().unwrap();

        // Clean up resources
        for &resource_id in &resource_ids {
            system.unregister_resource(resource_id)
                .expect("Failed to unregister resource");
        }

        let stats = system.get_statistics();
        println!("Deadlock prevention test completed:");
        println!("  Successful acquisitions: {}", successful_acquisitions.load(Ordering::SeqCst));
        println!("  Prevented deadlocks: {}", prevented_deadlocks.load(Ordering::SeqCst));
        println!("  Deadlocks detected: {}", stats.deadlocks_detected);
        println!("  Deadlocks resolved: {}", stats.deadlocks_resolved);
        println!("  Recovery attempts: {}", stats.recovery_attempts);

        // Verify that the system prevented or resolved deadlocks
        assert!(prevented_deadlocks.load(Ordering::SeqCst) > 0 || stats.deadlocks_resolved > 0,
                "System should have prevented or resolved deadlocks");
        assert!(stats.recovery_attempts >= stats.deadlocks_resolved,
                "Recovery attempts should be at least as many as resolved deadlocks");
    }

    /// Comprehensive stress test combining all threading optimizations
    #[test]
    fn test_comprehensive_threading_integration() {
        println!("Starting comprehensive threading integration test...");

        // Initialize all subsystems
        let work_stealing = Arc::new(WorkStealingScheduler::new(WorkStealingConfig {
            num_workers: 6,
            steal_attempts: 3,
            backoff_strategy: BackoffStrategy::Exponential { initial_delay_ns: 1000, max_delay_ns: 100000 },
            numa_awareness: true,
            preferred_cores: vec![0, 1, 2, 3, 4, 5],
            locality_factor: 0.8,
        }).expect("Failed to create work-stealing scheduler"));

        let thread_affinity = Arc::new(ThreadAffinityManager::new(AffinityConfig {
            numa_aware: true,
            prefer_local_memory: true,
            load_balancing: true,
            priority_based: true,
            core_isolation: None,
            max_threads_per_core: 2,
        }).expect("Failed to create thread affinity manager"));

        let profiler = Arc::new(ThreadProfiler::new(ProfilerConfig {
            sampling_interval: Duration::from_millis(50),
            stack_depth: 8,
            enable_memory_tracking: true,
            enable_cpu_tracking: true,
            enable_function_timing: true,
            enable_contention_tracking: true,
            buffer_size: 5000,
        }).expect("Failed to create thread profiler"));

        let memory_coordinator = Arc::new(MemoryCoordinator::new(CoordinatorConfig {
            max_shared_memories: 8,
            memory_pool_size: 32 * 1024 * 1024,
            page_size: 4096,
            enable_numa_awareness: true,
            gc_threshold: 0.75,
            compaction_interval: Duration::from_secs(30),
        }).expect("Failed to create memory coordinator"));

        profiler.start().expect("Failed to start profiler");

        // Create shared data structures
        let shared_queue = Arc::new(LockFreeQueue::<u64>::new());
        let shared_data = Arc::new(AdvancedRwLock::new(HashMap::<u64, String>::new()));

        let num_workers = 12;
        let tasks_per_worker = 100;
        let barrier = Arc::new(Barrier::new(num_workers));
        let completed_tasks = Arc::new(AtomicU64::new(0));

        // Launch worker threads with comprehensive integration
        let handles: Vec<_> = (0..num_workers)
            .map(|worker_id| {
                let scheduler = work_stealing.clone();
                let affinity = thread_affinity.clone();
                let prof = profiler.clone();
                let mem_coord = memory_coordinator.clone();
                let queue = shared_queue.clone();
                let data = shared_data.clone();
                let barrier = barrier.clone();
                let completed = completed_tasks.clone();

                thread::spawn(move || {
                    let thread_id = thread::current().id();

                    // Register with profiler and set thread affinity
                    prof.register_thread(thread_id, format!("integrated_worker_{}", worker_id))
                        .expect("Failed to register with profiler");

                    affinity.assign_thread_to_core(thread_id, ThreadPriority::Normal)
                        .expect("Failed to assign thread affinity");

                    barrier.wait();

                    // Execute integrated workload
                    for task_id in 0..tasks_per_worker {
                        let task_name = format!("integrated_task_{}_{}", worker_id, task_id);

                        // Start profiling the task
                        let timing_start = prof.start_function_timing(&task_name)
                            .expect("Failed to start timing");

                        // Create and execute a comprehensive task
                        let task = Arc::new(WorkStealingTask::new(
                            task_name.clone(),
                            if task_id % 10 == 0 { TaskPriority::High } else { TaskPriority::Normal },
                            {
                                let queue = queue.clone();
                                let data = data.clone();
                                let mem_coord = mem_coord.clone();
                                let task_name = task_name.clone();

                                move || {
                                    // Lock-free queue operations
                                    let value = (worker_id as u64) * 1000 + task_id;
                                    queue.enqueue(value)?;

                                    if let Ok(dequeued_value) = queue.dequeue() {
                                        // Shared memory operations
                                        let memory_id = MemoryId(worker_id % 4);
                                        if mem_coord.get_shared_memory(memory_id).is_ok() {
                                            let offset = (value % 1000) * 8;
                                            if offset < 1024 * 1024 - 8 {
                                                mem_coord.atomic_write_u64(memory_id, offset, dequeued_value).ok();
                                            }
                                        }

                                        // Advanced synchronization
                                        {
                                            let guard = data.write_with_priority(Priority::Normal)?;
                                            let mut data_map = guard;
                                            data_map.insert(dequeued_value, task_name);
                                        }

                                        // Read operation
                                        {
                                            let guard = data.read_with_priority(Priority::Normal)?;
                                            let data_map = guard;
                                            if data_map.contains_key(&dequeued_value) {
                                                // Simulate processing
                                                thread::sleep(Duration::from_micros(50));
                                            }
                                        }
                                    }

                                    Ok(())
                                }
                            },
                        ));

                        // Submit task to work-stealing scheduler
                        if scheduler.submit_task(task).is_ok() {
                            completed.fetch_add(1, Ordering::SeqCst);
                        }

                        // End profiling
                        prof.end_function_timing(&task_name, timing_start)
                            .expect("Failed to end timing");

                        // Update thread metrics periodically
                        if task_id % 20 == 0 {
                            affinity.update_thread_metrics(thread_id, ThreadMetrics {
                                cpu_time_ns: task_id as u64 * 1000,
                                cache_misses: task_id as u64 / 10,
                                memory_accesses: task_id as u64 * 100,
                                numa_remote_accesses: task_id as u64 / 50,
                            }).ok();

                            prof.update_memory_info(thread_id, MemoryInfo {
                                heap_used: (worker_id + 1) as u64 * 1024 * 1024,
                                heap_committed: (worker_id + 1) as u64 * 2 * 1024 * 1024,
                                non_heap_used: 512 * 1024,
                                gc_count: task_id as u64 / 20,
                                gc_time_ms: task_id as u64 / 100,
                            }).ok();
                        }

                        // Small delay for more realistic threading behavior
                        if task_id % 50 == 0 {
                            thread::yield_now();
                        }
                    }

                    prof.unregister_thread(thread_id).ok();
                })
            })
            .collect();

        // Wait for all workers to complete
        println!("Waiting for {} workers to complete {} tasks each...", num_workers, tasks_per_worker);
        for handle in handles {
            handle.join().unwrap();
        }

        // Generate comprehensive statistics
        profiler.stop().expect("Failed to stop profiler");
        let profiler_report = profiler.generate_report().expect("Failed to generate profiler report");
        let scheduler_stats = work_stealing.get_statistics();
        let affinity_stats = thread_affinity.get_statistics();
        let queue_stats = shared_queue.get_statistics();
        let rw_stats = shared_data.get_statistics();
        let memory_stats = memory_coordinator.get_statistics();

        println!("\n=== Comprehensive Threading Integration Test Results ===");
        println!("Scheduler Stats:");
        println!("  Tasks completed: {}", scheduler_stats.tasks_completed);
        println!("  Total steal attempts: {}", scheduler_stats.total_steal_attempts);
        println!("  Successful steals: {}", scheduler_stats.successful_steals);

        println!("\nThread Affinity Stats:");
        println!("  Successful assignments: {}", affinity_stats.successful_assignments);
        println!("  Load balancing operations: {}", affinity_stats.load_balancing_operations);

        println!("\nLock-free Queue Stats:");
        println!("  Enqueue operations: {}", queue_stats.enqueue_operations);
        println!("  Dequeue operations: {}", queue_stats.dequeue_operations);
        println!("  Contentions: {}", queue_stats.contentions);

        println!("\nAdvanced RwLock Stats:");
        println!("  Read operations: {}", rw_stats.read_operations);
        println!("  Write operations: {}", rw_stats.write_operations);
        println!("  Average wait time: {:.2}ms", rw_stats.average_wait_time_ns as f64 / 1_000_000.0);

        println!("\nMemory Coordinator Stats:");
        println!("  GC cycles: {}", memory_stats.gc_cycles);
        println!("  Memory utilization: {:.2}%", memory_stats.memory_utilization * 100.0);

        println!("\nProfiler Report:");
        println!("  Threads monitored: {}", profiler_report.thread_count);
        println!("  Functions profiled: {}", profiler_report.function_profiles.len());
        println!("  Total samples: {}", profiler_report.total_samples);

        println!("\nOverall Integration:");
        println!("  Total completed tasks: {}", completed_tasks.load(Ordering::SeqCst));
        println!("  Expected tasks: {}", num_workers * tasks_per_worker);

        // Verify successful integration
        assert!(completed_tasks.load(Ordering::SeqCst) > 0, "Should have completed some tasks");
        assert!(scheduler_stats.tasks_completed > 0, "Scheduler should have executed tasks");
        assert!(affinity_stats.successful_assignments > 0, "Should have made thread assignments");
        assert!(queue_stats.enqueue_operations > 0, "Should have used lock-free queue");
        assert!(profiler_report.thread_count == num_workers as u64, "Should have monitored all threads");

        println!("\n✅ Comprehensive threading integration test completed successfully!");
    }
}