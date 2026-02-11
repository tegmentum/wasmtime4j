(module
  ;; Shared memory required for atomic operations
  (memory (export "memory") 1 1 shared)

  ;; Atomic compare-and-swap i32
  (func (export "atomic_cas_i32") (param $offset i32) (param $expected i32) (param $new i32) (result i32)
    local.get $offset
    local.get $expected
    local.get $new
    i32.atomic.rmw.cmpxchg
  )

  ;; Atomic compare-and-swap i64
  (func (export "atomic_cas_i64") (param $offset i32) (param $expected i64) (param $new i64) (result i64)
    local.get $offset
    local.get $expected
    local.get $new
    i64.atomic.rmw.cmpxchg
  )

  ;; Atomic load i32
  (func (export "atomic_load_i32") (param $offset i32) (result i32)
    local.get $offset
    i32.atomic.load
  )

  ;; Atomic load i64
  (func (export "atomic_load_i64") (param $offset i32) (result i64)
    local.get $offset
    i64.atomic.load
  )

  ;; Atomic store i32
  (func (export "atomic_store_i32") (param $offset i32) (param $value i32)
    local.get $offset
    local.get $value
    i32.atomic.store
  )

  ;; Atomic store i64
  (func (export "atomic_store_i64") (param $offset i32) (param $value i64)
    local.get $offset
    local.get $value
    i64.atomic.store
  )

  ;; Atomic add i32
  (func (export "atomic_add_i32") (param $offset i32) (param $value i32) (result i32)
    local.get $offset
    local.get $value
    i32.atomic.rmw.add
  )

  ;; Atomic add i64
  (func (export "atomic_add_i64") (param $offset i32) (param $value i64) (result i64)
    local.get $offset
    local.get $value
    i64.atomic.rmw.add
  )

  ;; Atomic and i32
  (func (export "atomic_and_i32") (param $offset i32) (param $value i32) (result i32)
    local.get $offset
    local.get $value
    i32.atomic.rmw.and
  )

  ;; Atomic or i32
  (func (export "atomic_or_i32") (param $offset i32) (param $value i32) (result i32)
    local.get $offset
    local.get $value
    i32.atomic.rmw.or
  )

  ;; Atomic xor i32
  (func (export "atomic_xor_i32") (param $offset i32) (param $value i32) (result i32)
    local.get $offset
    local.get $value
    i32.atomic.rmw.xor
  )

  ;; Fence
  (func (export "atomic_fence")
    atomic.fence
  )

  ;; Notify
  (func (export "atomic_notify") (param $offset i32) (param $count i32) (result i32)
    local.get $offset
    local.get $count
    memory.atomic.notify
  )

  ;; Wait32
  (func (export "atomic_wait32") (param $offset i32) (param $expected i32) (param $timeout i64) (result i32)
    local.get $offset
    local.get $expected
    local.get $timeout
    memory.atomic.wait32
  )

  ;; Wait64
  (func (export "atomic_wait64") (param $offset i32) (param $expected i64) (param $timeout i64) (result i32)
    local.get $offset
    local.get $expected
    local.get $timeout
    memory.atomic.wait64
  )
)
