(module
  (type $empty (func))
  (table $t1 2 funcref)
  (elem (table $t1) (i32.const 0) func $nop)
  (func $nop)

  (func (export "t1") (param i32)
    local.get 0
    call_indirect $t1 (type $empty))
  (func (export "t1-wrong-type") (param i32)
    i32.const 0
    local.get 0
    call_indirect $t1 (type 1))

  (type (;1;) (func (param i32)))
)
