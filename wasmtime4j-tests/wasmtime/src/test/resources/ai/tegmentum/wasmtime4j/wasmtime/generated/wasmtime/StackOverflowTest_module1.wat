(module
  (func $foo
    (call $foo)
  )
  (func (export "stack_overflow")
    (call $foo)
  )
)
