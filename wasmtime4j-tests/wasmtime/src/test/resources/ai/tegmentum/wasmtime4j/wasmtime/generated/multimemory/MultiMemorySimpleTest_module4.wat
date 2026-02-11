(module
  (memory $m1 1)
  (memory $m2 1)

  (func (export "fill1") (result i32)
      i32.const 1
      i32.const 0x01
      i32.const 4
      memory.fill $m1
      i32.const 1
      i32.load)

  (func (export "fill2") (result i32)
      i32.const 1
      i32.const 0x02
      i32.const 2
      memory.fill $m2
      i32.const 1
      i32.load $m2)
)
