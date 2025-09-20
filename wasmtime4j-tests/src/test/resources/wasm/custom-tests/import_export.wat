(module
  (import "env" "imported_func" (func $imported (param i32) (result i32)))
  (import "env" "memory" (memory 1))

  (func $double (param $x i32) (result i32)
    local.get $x
    i32.const 2
    i32.mul)

  (func $use_import (param $x i32) (result i32)
    local.get $x
    call $imported
    call $double)

  (export "double" (func $double))
  (export "use_import" (func $use_import))
  (export "memory" (memory 0))
)