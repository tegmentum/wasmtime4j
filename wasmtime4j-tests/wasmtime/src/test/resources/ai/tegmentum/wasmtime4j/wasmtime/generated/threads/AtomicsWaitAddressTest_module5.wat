(module
  (memory 1 1 shared)
  (type (;0;) (func))
  (func $wait32 (result i32)
    i32.const 0
    i32.const 42
    i64.const -1
    memory.atomic.wait32
    )
  (func $wait64 (result i32)
    i32.const 0
    i64.const 43
    i64.const -1
    memory.atomic.wait64
    )
  (export "wait32" (func $wait32))
  (export "wait64" (func $wait64))
)
