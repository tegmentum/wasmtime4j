(module
  (memory $m1 1)
  (memory $m2 1)

  (func (export "init1") (result i32)
      i32.const 1
      i32.const 0
      i32.const 4
      memory.init $m1 $d
      i32.const 1
      i32.load)

  (func (export "init2") (result i32)
      i32.const 1
      i32.const 4
      i32.const 4
      memory.init $m2 $d
      i32.const 1
      i32.load $m2)

  (data $d "\01\00\00\00" "\02\00\00\00")
)
