(module
  (table $t1 2 funcref)
  (elem (table $t1) (i32.const 0) func $nop)
  (func $nop)

  (func (export "t1") (param i32)
    local.get 0
    call_indirect $t1)
  (func (export "t1-wrong-type") (param i32)
    i32.const 0
    local.get 0
    call_indirect $t1 (param i32))

  (type $empty (func))
  (table $t2 2 (ref null $empty))
  (elem (table $t2) (i32.const 0) (ref null $empty) (ref.func $nop))

  (func (export "t2") (param i32)
    local.get 0
    call_indirect $t2)
  (func (export "t2-wrong-type") (param i32)
    i32.const 0
    local.get 0
    call_indirect $t2 (param i32))

  (table $t3 2 (ref $empty) (ref.func $nop))

  (func (export "t3") (param i32)
    local.get 0
    call_indirect $t3)
  (func (export "t3-wrong-type") (param i32)
    i32.const 0
    local.get 0
    call_indirect $t3 (param i32))
)
