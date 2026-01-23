(module $A
  (type (func (param i32) (result i32)))

  (table (export "table") 1 1 funcref)

  (func (export "f") (param i32) (result i32)
    local.get 0
    i32.eqz
    if
      (return (i32.const 42))
    else
      (i32.sub (local.get 0) (i32.const 1))
      i32.const 0
      return_call_indirect (type 0)
    end
    unreachable
  )
)
