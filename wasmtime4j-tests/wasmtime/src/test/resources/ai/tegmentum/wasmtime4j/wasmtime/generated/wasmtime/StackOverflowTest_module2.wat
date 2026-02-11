(module
  (func $foo
    (call $bar)
  )
  (func $bar
    (call $foo)
  )
  (func (export "stack_overflow")
    (call $foo)
  )
)
