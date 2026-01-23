(module
  (table $table 1 1 (ref any) (ref.i31 (i32.const 0)))

  (func (export "get") (param i32) (result (ref any))
    local.get 0
    table.get $table
  )

  (func $do_set (param i32 (ref any))
    local.get 0
    local.get 1
    table.set $table
  )

  (func (export "set") (param i32 i32)
    local.get 0
    (ref.i31 local.get 1)
    call $do_set
  )
)
