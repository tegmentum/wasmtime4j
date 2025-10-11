(module
  ;; Export memory
  (memory 1 10)
  (export "memory" (memory 0))

  ;; Export table
  (table 5 funcref)
  (export "table" (table 0))

  ;; Export globals
  (global $g_i32 (mut i32) (i32.const 42))
  (global $g_i64 (mut i64) (i64.const 100))
  (global $g_f32 (mut f32) (f32.const 3.14))
  (export "g_i32" (global $g_i32))
  (export "g_i64" (global $g_i64))
  (export "g_f32" (global $g_f32))

  ;; Export a simple function
  (func $test (result i32)
    i32.const 7)
  (export "test" (func $test))
)
