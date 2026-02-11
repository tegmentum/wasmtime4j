(module $m1
  ;; A pair of recursive types.
  (rec (type $type_a (sub final (func (result i32 (ref null $type_b)))))
       (type $type_b (sub final (func (result i32 (ref null $type_a))))))

  (func (export "func_a") (type $type_a)
    i32.const 1234
    ref.null $type_b
  )

  (func (export "func_b") (type $type_b)
    i32.const 4321
    ref.null $type_a
  )
)
