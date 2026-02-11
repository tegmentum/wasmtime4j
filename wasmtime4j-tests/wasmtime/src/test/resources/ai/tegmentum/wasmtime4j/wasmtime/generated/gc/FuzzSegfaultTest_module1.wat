(module
  (func (export "run")
    loop
      ref.null 0
      ref.test (ref 0)
      br_if 0
    end
  )
)
