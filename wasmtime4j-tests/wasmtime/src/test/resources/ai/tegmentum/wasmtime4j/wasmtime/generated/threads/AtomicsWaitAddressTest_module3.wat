(module
  (type (;0;) (func))
  (func $wait32 (type 0)
    i32.const -64
    i32.const 42
    i64.const 0
    memory.atomic.wait32 offset=1
    unreachable)
  (func $wait64 (type 0)
    i32.const -64
    i64.const 43
    i64.const 0
    memory.atomic.wait64 offset=3
    unreachable)
  (memory (;0;) 4 4)
  (export "wait32" (func $wait32))
  (export "wait64" (func $wait64))
)
