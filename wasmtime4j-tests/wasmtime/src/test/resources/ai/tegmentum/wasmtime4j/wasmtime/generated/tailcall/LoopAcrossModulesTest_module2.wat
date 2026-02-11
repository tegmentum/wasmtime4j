(module $B
  (import "A" "table" (table $table 1 1 funcref))
  (import "A" "f" (func $f (param i32) (result i32)))

  (func $g (export "g") (param i32) (result i32)
    local.get 0
    return_call $f
  )

  (func $start
    (table.set $table (i32.const 0) (ref.func $g))
  )
  (start $start)
)
