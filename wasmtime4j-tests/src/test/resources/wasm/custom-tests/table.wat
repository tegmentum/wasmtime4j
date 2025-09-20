(module
  (table 10 funcref)
  (export "table" (table 0))

  (func $func1 (result i32)
    i32.const 42)

  (func $func2 (result i32)
    i32.const 84)

  (elem (i32.const 0) $func1 $func2)

  (func $call_indirect (param $index i32) (result i32)
    local.get $index
    call_indirect (type 0))

  (type (func (result i32)))
  (export "call_indirect" (func $call_indirect))
  (export "func1" (func $func1))
  (export "func2" (func $func2))
)