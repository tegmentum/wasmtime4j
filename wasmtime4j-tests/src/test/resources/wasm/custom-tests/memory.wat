(module
  (memory 1)
  (export "memory" (memory 0))
  (func $load (param $offset i32) (result i32)
    local.get $offset
    i32.load)
  (func $store (param $offset i32) (param $value i32)
    local.get $offset
    local.get $value
    i32.store)
  (export "load" (func $load))
  (export "store" (func $store))
)