;; Component with a void function (no parameters, no return)
(component
  (core module $m
    (type (func))
    (export "noop" (func 0))
    (func (type 0)
      ;; empty function body - just returns
    )
  )
  (core instance $i (instantiate $m))
  (type (func))
  (alias core export $i "noop" (core func))
  (func (type 0) (canon lift (core func 0)))
  (export "noop" (func 0))
)
