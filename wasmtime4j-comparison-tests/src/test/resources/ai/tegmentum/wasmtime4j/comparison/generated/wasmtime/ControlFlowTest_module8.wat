( module ( func ( export "br_table") ( param $i i32) ( result i32) ( return ( block 
              $2 ( result i32) ( i32.add ( i32.const 10) ( block $1 ( result i32) ( i32.add ( i32.const 
                            100) ( block $0 ( result i32) ( i32.add ( i32.const 1000) ( block $default ( result 
                                        i32) ( br_table $0 $1 $2 $default ( i32.mul ( i32.const 2) ( local.get $i)) ( i32.and 
                                                  ( i32.const 3) ( local.get $i)))))))))))))