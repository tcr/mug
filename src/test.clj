(ns test
  (:use mug [mug.asm closures constants object profile scopes script util]))

;
; file i/o
;

(import java.io.FileOutputStream)

(defn write-file [path bytes]
	(let [fos (new FileOutputStream path)]
		(.write fos bytes)
		(.close fos)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; examples
;

;
; josephus problem
;

(def josephus-ast
  (func-closure nil ["print"]  #{"Chain" "ITER" "Person" "chain" "end" "i" "start"} (block-stat 
          
    (expr-stat (scope-assign-expr "Person" (func-literal (func-closure nil ["count"]  #{} (block-stat 
        (expr-stat (static-assign-expr (this-expr) "count" (scope-ref-expr "count") ))
        (ret-stat  (this-expr))
    ) )) ))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "Person") "prototype") "count" (num-literal 0) ))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "Person") "prototype") "prev" (null-literal) ))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "Person") "prototype") "next" (null-literal) ))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "Person") "prototype") "shout" (func-literal (func-closure nil ["shout" "deadif"]  #{} (block-stat 
        (if-stat (lt-op-expr (scope-ref-expr "shout") (scope-ref-expr "deadif") ) (expr-stat (ret-stat  (add-op-expr (scope-ref-expr "shout") (num-literal 1) ))) nil )
        (expr-stat (static-assign-expr (static-ref-expr (this-expr) "prev") "next" (static-ref-expr (this-expr) "next") ))
        (expr-stat (static-assign-expr (static-ref-expr (this-expr) "next") "prev" (static-ref-expr (this-expr) "prev") ))
        (ret-stat  (num-literal 1))
    ) )) ))
    (expr-stat (scope-assign-expr "Chain" (func-literal (func-closure nil ["size"]  #{"current" "i" "last"} (block-stat 
          
        (expr-stat (scope-assign-expr "last" (null-literal) ))
        (expr-stat (scope-assign-expr "current" (null-literal) ))
        (scope-assign-expr "i" (num-literal 0) ) 
 (while-stat (lt-op-expr (scope-ref-expr "i") (scope-ref-expr "size") )(block-stat 
            (block-stat 
                (expr-stat (scope-assign-expr "current" (new-expr  (scope-ref-expr "Person") (scope-ref-expr "i")) ))
                (if-stat (eqs-op-expr (static-ref-expr (this-expr) "first") (null-literal) ) (expr-stat (static-assign-expr (this-expr) "first" (scope-ref-expr "current") )) nil )
                (if-stat (neqs-op-expr (scope-ref-expr "last") (null-literal) ) (block-stat 
                    (expr-stat (static-assign-expr (scope-ref-expr "last") "next" (scope-ref-expr "current") ))
                    (expr-stat (static-assign-expr (scope-ref-expr "current") "prev" (scope-ref-expr "last") ))
                ) nil )
                (expr-stat (scope-assign-expr "last" (scope-ref-expr "current") ))
            )
            (expr-stat (scope-assign-expr "i" (add-op-expr (scope-ref-expr "i") (num-literal 1) ) ))
        ))
        (expr-stat (static-assign-expr (static-ref-expr (this-expr) "first") "prev" (scope-ref-expr "last") ))
        (expr-stat (static-assign-expr (scope-ref-expr "last") "next" (static-ref-expr (this-expr) "first") ))
        (ret-stat  (this-expr))
    ) )) ))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "Chain") "prototype") "first" (null-literal) ))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "Chain") "prototype") "kill" (func-literal (func-closure nil ["nth"]  #{"current" "shout" } (block-stat 
         
        (expr-stat (scope-assign-expr "current" (static-ref-expr (this-expr) "first") ))
        (expr-stat (scope-assign-expr "shout" (num-literal 1) ))
        (while-stat (neqs-op-expr (static-ref-expr (scope-ref-expr "current") "next") (scope-ref-expr "current") ) (block-stat 
            (expr-stat (scope-assign-expr "shout" (static-method-call-expr (scope-ref-expr "current") "shout" (scope-ref-expr "shout") (scope-ref-expr "nth")) ))
            (expr-stat (scope-assign-expr "current" (static-ref-expr (scope-ref-expr "current") "next") ))
        ) )
        (expr-stat (static-assign-expr (this-expr) "first" (scope-ref-expr "current") ))
        (ret-stat  (scope-ref-expr "current"))
    ) )) ))
    (expr-stat (scope-assign-expr "ITER" (num-literal 5e5) ))
    (scope-assign-expr "i" (num-literal 0) ) 
 (while-stat (lt-op-expr (scope-ref-expr "i") (scope-ref-expr "ITER") )(block-stat 
        (block-stat 
            (expr-stat (scope-assign-expr "chain" (new-expr  (scope-ref-expr "Chain") (num-literal 40)) ))
            (expr-stat (static-method-call-expr (scope-ref-expr "chain") "kill" (num-literal 3)))
        )
        (expr-stat (scope-assign-expr "i" (add-op-expr (scope-ref-expr "i") (num-literal 1) ) ))
    ))
) )
)

;
; binary trees
;

(def binary-ast
(func-closure nil ["print"]  #{"TreeNode" "bottomUpTree" "minDepth" "n" "maxDepth" "stretchDepth" "check" "longLivedTree" "iterations" "i" "depth"} (block-stat 
    (expr-stat (scope-assign-expr "TreeNode" (func-literal (func-closure "TreeNode" ["left" "right" "item"]  #{} (block-stat 
        (expr-stat (static-assign-expr (this-expr) "left" (scope-ref-expr "left") ))
        (expr-stat (static-assign-expr (this-expr) "right" (scope-ref-expr "right") ))
        (expr-stat (static-assign-expr (this-expr) "item" (scope-ref-expr "item") ))
    ) ))))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "TreeNode") "prototype") "itemCheck" (func-literal (func-closure nil []  #{} (if-stat (eqs-op-expr (static-ref-expr (this-expr) "left") (null-literal) ) (ret-stat  (static-ref-expr (this-expr) "item")) (ret-stat  (sub-op-expr (add-op-expr (static-ref-expr (this-expr) "item") (static-method-call-expr (static-ref-expr (this-expr) "left") "itemCheck" ) ) (static-method-call-expr (static-ref-expr (this-expr) "right") "itemCheck" ) )) ) )) ))
    (expr-stat (scope-assign-expr "bottomUpTree" (func-literal (func-closure "bottomUpTree" ["item" "depth"]  #{} (if-stat (gt-op-expr (scope-ref-expr "depth") (num-literal 0) ) (expr-stat (ret-stat  (new-expr  (scope-ref-expr "TreeNode") (call-expr (scope-ref-expr "bottomUpTree") (sub-op-expr (mul-op-expr (num-literal 2) (scope-ref-expr "item") ) (num-literal 1) ) (sub-op-expr (scope-ref-expr "depth") (num-literal 1) )) (call-expr (scope-ref-expr "bottomUpTree") (mul-op-expr (num-literal 2) (scope-ref-expr "item") ) (sub-op-expr (scope-ref-expr "depth") (num-literal 1) )) (scope-ref-expr "item")))) (expr-stat (ret-stat  (new-expr  (scope-ref-expr "TreeNode") (null-literal) (null-literal) (scope-ref-expr "item")))) ) ))))
    (expr-stat (scope-assign-expr "minDepth" (num-literal 4)))
    (expr-stat (scope-assign-expr "n" (num-literal 16)))
    (expr-stat (scope-assign-expr "maxDepth" (num-literal 16)))
    (expr-stat (scope-assign-expr "stretchDepth" (add-op-expr (scope-ref-expr "maxDepth") (num-literal 1) )))
    (expr-stat (scope-assign-expr "check" (static-method-call-expr (call-expr (scope-ref-expr "bottomUpTree") (num-literal 0) (scope-ref-expr "stretchDepth")) "itemCheck" )))
    (expr-stat (call-expr (scope-ref-expr "print") (add-op-expr (add-op-expr (add-op-expr (str-literal "stretch tree of depth ") (scope-ref-expr "stretchDepth") ) (str-literal "\t check: ") ) (scope-ref-expr "check") )))
    (expr-stat (scope-assign-expr "longLivedTree" (call-expr (scope-ref-expr "bottomUpTree") (num-literal 0) (scope-ref-expr "maxDepth"))))
    (expr-stat (scope-assign-expr "depth" (scope-ref-expr "minDepth"))) 
 (while-stat (lte-op-expr (scope-ref-expr "depth") (scope-ref-expr "maxDepth") )(block-stat 
        (block-stat 
            (expr-stat (scope-assign-expr "iterations" (lsh-op-expr (num-literal 1) (add-op-expr (sub-op-expr (scope-ref-expr "maxDepth") (scope-ref-expr "depth") ) (scope-ref-expr "minDepth") ) )))
            (expr-stat (scope-assign-expr "check" (num-literal 0) ))
            (expr-stat (scope-assign-expr "i" (num-literal 1))) 
 (while-stat (lte-op-expr (scope-ref-expr "i") (scope-ref-expr "iterations") )(block-stat 
                (block-stat 
                    (expr-stat (scope-assign-expr "check" (add-op-expr (scope-ref-expr "check") (static-method-call-expr (call-expr (scope-ref-expr "bottomUpTree") (scope-ref-expr "i") (scope-ref-expr "depth")) "itemCheck" ) ) ))
                    (expr-stat (scope-assign-expr "check" (add-op-expr (scope-ref-expr "check") (static-method-call-expr (call-expr (scope-ref-expr "bottomUpTree") (neg-op-expr (scope-ref-expr "i") ) (scope-ref-expr "depth")) "itemCheck" ) ) ))
                )
                (expr-stat (scope-assign-expr "i" (add-op-expr (scope-ref-expr "i") (num-literal 1) ) ))
            ))
            (expr-stat (call-expr (scope-ref-expr "print") (add-op-expr (add-op-expr (add-op-expr (add-op-expr (mul-op-expr (scope-ref-expr "iterations") (num-literal 2) ) (str-literal "\t trees of depth ") ) (scope-ref-expr "depth") ) (str-literal "\t check: ") ) (scope-ref-expr "check") )))
        )
        (expr-stat (scope-assign-expr "depth" (add-op-expr (scope-ref-expr "depth") (num-literal 2) ) ))
    ))
    (expr-stat (call-expr (scope-ref-expr "print") (add-op-expr (add-op-expr (add-op-expr (str-literal "long lived tree of depth ") (scope-ref-expr "maxDepth") ) (str-literal "\t check: ") ) (static-method-call-expr (scope-ref-expr "longLivedTree") "itemCheck" ) )))
) ))

;
; testing ast
;

(def test-ast-2
(func-closure nil ["print" "Math" "Array"]  #{"a"} (block-stat 
    (expr-stat (call-expr (scope-ref-expr "print") (static-method-call-expr (scope-ref-expr "Math") "sqrt" (num-literal 2))))
    (expr-stat (scope-assign-expr "a" (call-expr (scope-ref-expr "Array") )))
    (expr-stat (dyn-assign-expr (scope-ref-expr "a") (num-literal 0) (str-literal "First entry") ))
    (expr-stat (call-expr (scope-ref-expr "print") (dyn-ref-expr (scope-ref-expr "a") (num-literal 0))))
    (expr-stat (dyn-assign-expr (scope-ref-expr "a") (num-literal 10) (str-literal "Tenth entry") ))
    (expr-stat (call-expr (scope-ref-expr "print") (dyn-ref-expr (scope-ref-expr "a") (num-literal 10))))
    (expr-stat (call-expr (scope-ref-expr "print") (add-op-expr (str-literal "Length: ") (static-ref-expr (scope-ref-expr "a") "length") )))
) )
)

(def test-ast
(func-closure nil ["print" "Math" "Array"]  #{"PI" "SOLAR_MASS" "DAYS_PER_YEAR" "Body" "Jupiter" "Saturn" "Uranus" "Neptune" "Sun" "NBodySystem" "n" "bodies" "i"} (block-stat 
    (expr-stat (scope-assign-expr "PI" (num-literal 3.141592653589793)))
    (expr-stat (scope-assign-expr "SOLAR_MASS" (mul-op-expr (mul-op-expr (num-literal 4) (scope-ref-expr "PI") ) (scope-ref-expr "PI") )))
    (expr-stat (scope-assign-expr "DAYS_PER_YEAR" (num-literal 365.24)))
    (expr-stat (scope-assign-expr "Body" (func-literal (func-closure "Body" ["x" "y" "z" "vx" "vy" "vz" "mass"]  #{} (block-stat 
        (expr-stat (static-assign-expr (this-expr) "x" (scope-ref-expr "x") ))
        (expr-stat (static-assign-expr (this-expr) "y" (scope-ref-expr "y") ))
        (expr-stat (static-assign-expr (this-expr) "z" (scope-ref-expr "z") ))
        (expr-stat (static-assign-expr (this-expr) "vx" (scope-ref-expr "vx") ))
        (expr-stat (static-assign-expr (this-expr) "vy" (scope-ref-expr "vy") ))
        (expr-stat (static-assign-expr (this-expr) "vz" (scope-ref-expr "vz") ))
        (expr-stat (static-assign-expr (this-expr) "mass" (scope-ref-expr "mass") ))
    ) ))))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "Body") "prototype") "offsetMomentum" (func-literal (func-closure nil ["px" "py" "pz"]  #{} (block-stat 
        (expr-stat (static-assign-expr (this-expr) "vx" (div-op-expr (neg-op-expr (scope-ref-expr "px")) (scope-ref-expr "SOLAR_MASS") ) ))
        (expr-stat (static-assign-expr (this-expr) "vy" (div-op-expr (neg-op-expr (scope-ref-expr "py")) (scope-ref-expr "SOLAR_MASS") ) ))
        (expr-stat (static-assign-expr (this-expr) "vz" (div-op-expr (neg-op-expr (scope-ref-expr "pz")) (scope-ref-expr "SOLAR_MASS") ) ))
        (ret-stat  (this-expr))
    ) )) ))
    (expr-stat (scope-assign-expr "Jupiter" (func-literal (func-closure "Jupiter" []  #{} (expr-stat (ret-stat  (new-expr  (scope-ref-expr "Body") (num-literal 4.841431442464721) (neg-op-expr (num-literal 1.1603200440274284)) (mul-op-expr (neg-op-expr (num-literal 1.036220444711231)) (num-literal 0.1) ) (mul-op-expr (mul-op-expr (num-literal 1.660076642744037) (num-literal 1e-3) ) (scope-ref-expr "DAYS_PER_YEAR") ) (mul-op-expr (mul-op-expr (num-literal 7.6990111841974045) (num-literal 1e-3) ) (scope-ref-expr "DAYS_PER_YEAR") ) (mul-op-expr (mul-op-expr (neg-op-expr (num-literal 6.90460016972063)) (num-literal 1e-5) ) (scope-ref-expr "DAYS_PER_YEAR") ) (mul-op-expr (mul-op-expr (num-literal 9.547919384243267) (num-literal 1e-4) ) (scope-ref-expr "SOLAR_MASS") )))) ))))
    (expr-stat (scope-assign-expr "Saturn" (func-literal (func-closure "Saturn" []  #{} (expr-stat (ret-stat  (new-expr  (scope-ref-expr "Body") (num-literal 8.34336671824458) (num-literal 4.124798564124305) (mul-op-expr (neg-op-expr (num-literal 4.035234171143213)) (num-literal 0.1) ) (mul-op-expr (mul-op-expr (neg-op-expr (num-literal 2.767425107268624)) (num-literal 1e-3) ) (scope-ref-expr "DAYS_PER_YEAR") ) (mul-op-expr (mul-op-expr (num-literal 4.998528012349173) (num-literal 1e-3) ) (scope-ref-expr "DAYS_PER_YEAR") ) (mul-op-expr (mul-op-expr (num-literal 2.3041729757376395) (num-literal 1e-5) ) (scope-ref-expr "DAYS_PER_YEAR") ) (mul-op-expr (mul-op-expr (num-literal 2.8588598066613082) (num-literal 1e-4) ) (scope-ref-expr "SOLAR_MASS") )))) ))))
    (expr-stat (scope-assign-expr "Uranus" (func-literal (func-closure "Uranus" []  #{} (expr-stat (ret-stat  (new-expr  (scope-ref-expr "Body") (mul-op-expr (num-literal 1.2894369562139132) (num-literal 10) ) (mul-op-expr (neg-op-expr (num-literal 1.511115140169863)) (num-literal 10) ) (mul-op-expr (neg-op-expr (num-literal 2.2330757889265573)) (num-literal 0.1) ) (mul-op-expr (mul-op-expr (num-literal 2.964601375647616) (num-literal 1e-3) ) (scope-ref-expr "DAYS_PER_YEAR") ) (mul-op-expr (mul-op-expr (num-literal 2.3784717395948096) (num-literal 1e-3) ) (scope-ref-expr "DAYS_PER_YEAR") ) (mul-op-expr (mul-op-expr (neg-op-expr (num-literal 2.9658956854023755)) (num-literal 1e-5) ) (scope-ref-expr "DAYS_PER_YEAR") ) (mul-op-expr (mul-op-expr (num-literal 4.366244043351563) (num-literal 1e-5) ) (scope-ref-expr "SOLAR_MASS") )))) ))))
    (expr-stat (scope-assign-expr "Neptune" (func-literal (func-closure "Neptune" []  #{} (expr-stat (ret-stat  (new-expr  (scope-ref-expr "Body") (mul-op-expr (num-literal 1.5379697114850917) (num-literal 10) ) (mul-op-expr (neg-op-expr (num-literal 2.5919314609987962)) (num-literal 10) ) (mul-op-expr (num-literal 1.7925877295037118) (num-literal 0.1) ) (mul-op-expr (mul-op-expr (num-literal 2.680677724903893) (num-literal 1e-3) ) (scope-ref-expr "DAYS_PER_YEAR") ) (mul-op-expr (mul-op-expr (num-literal 1.628241700382423) (num-literal 1e-3) ) (scope-ref-expr "DAYS_PER_YEAR") ) (mul-op-expr (mul-op-expr (neg-op-expr (num-literal 9.515922545197158)) (num-literal 1e-5) ) (scope-ref-expr "DAYS_PER_YEAR") ) (mul-op-expr (mul-op-expr (num-literal 5.151389020466114) (num-literal 1e-5) ) (scope-ref-expr "SOLAR_MASS") )))) ))))
    (expr-stat (scope-assign-expr "Sun" (func-literal (func-closure "Sun" []  #{} (expr-stat (ret-stat  (new-expr  (scope-ref-expr "Body") (num-literal 0) (num-literal 0) (num-literal 0) (num-literal 0) (num-literal 0) (num-literal 0) (scope-ref-expr "SOLAR_MASS")))) ))))
    (expr-stat (scope-assign-expr "NBodySystem" (func-literal (func-closure "NBodySystem" ["bodies"]  #{"px" "py" "pz" "size" "b" "m" "i"} (block-stat 
        (expr-stat (static-assign-expr (this-expr) "bodies" (scope-ref-expr "bodies") ))
        (expr-stat (scope-assign-expr "px" (num-literal 0)))
        (expr-stat (scope-assign-expr "py" (num-literal 0)))
        (expr-stat (scope-assign-expr "pz" (num-literal 0)))
        (expr-stat (scope-assign-expr "size" (static-ref-expr (static-ref-expr (this-expr) "bodies") "length")))
        (expr-stat (scope-assign-expr "i" (num-literal 0))) 
 (while-stat (lt-op-expr (scope-ref-expr "i") (scope-ref-expr "size") )(block-stat 
            (block-stat 
                (expr-stat (scope-assign-expr "b" (dyn-ref-expr (static-ref-expr (this-expr) "bodies") (scope-ref-expr "i"))))
                (expr-stat (scope-assign-expr "m" (static-ref-expr (scope-ref-expr "b") "mass")))
                (expr-stat (scope-assign-expr "px" (add-op-expr (scope-ref-expr "px") (mul-op-expr (static-ref-expr (scope-ref-expr "b") "vx") (scope-ref-expr "m") ) ) ))
                (expr-stat (scope-assign-expr "py" (add-op-expr (scope-ref-expr "py") (mul-op-expr (static-ref-expr (scope-ref-expr "b") "vy") (scope-ref-expr "m") ) ) ))
                (expr-stat (scope-assign-expr "pz" (add-op-expr (scope-ref-expr "pz") (mul-op-expr (static-ref-expr (scope-ref-expr "b") "vz") (scope-ref-expr "m") ) ) ))
            )
            (expr-stat (scope-assign-expr "i" (add-op-expr (scope-ref-expr "i") (num-literal 1) ) ))
        ))
        (expr-stat (static-method-call-expr (dyn-ref-expr (static-ref-expr (this-expr) "bodies") (num-literal 0)) "offsetMomentum" (scope-ref-expr "px") (scope-ref-expr "py") (scope-ref-expr "pz")))
    ) ))))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "NBodySystem") "prototype") "advance" (func-literal (func-closure nil ["dt"]  #{"dx" "dy" "dz" "distance" "mag" "size" "bodyi" "bodyj" "j" "i" "body"} (block-stat 
            
        (expr-stat (scope-assign-expr "size" (static-ref-expr (static-ref-expr (this-expr) "bodies") "length")))
        (expr-stat (scope-assign-expr "i" (num-literal 0))) 
 (while-stat (lt-op-expr (scope-ref-expr "i") (scope-ref-expr "size") )(block-stat 
            (block-stat 
                (expr-stat (scope-assign-expr "bodyi" (dyn-ref-expr (static-ref-expr (this-expr) "bodies") (scope-ref-expr "i"))))
                (expr-stat (scope-assign-expr "j" (add-op-expr (scope-ref-expr "i") (num-literal 1) ))) 
 (while-stat (lt-op-expr (scope-ref-expr "j") (scope-ref-expr "size") )(block-stat 
                    (block-stat 
                        (expr-stat (scope-assign-expr "bodyj" (dyn-ref-expr (static-ref-expr (this-expr) "bodies") (scope-ref-expr "j"))))
                        (expr-stat (scope-assign-expr "dx" (sub-op-expr (static-ref-expr (scope-ref-expr "bodyi") "x") (static-ref-expr (scope-ref-expr "bodyj") "x") ) ))
                        (expr-stat (scope-assign-expr "dy" (sub-op-expr (static-ref-expr (scope-ref-expr "bodyi") "y") (static-ref-expr (scope-ref-expr "bodyj") "y") ) ))
                        (expr-stat (scope-assign-expr "dz" (sub-op-expr (static-ref-expr (scope-ref-expr "bodyi") "z") (static-ref-expr (scope-ref-expr "bodyj") "z") ) ))
                        (expr-stat (scope-assign-expr "distance" (static-method-call-expr (scope-ref-expr "Math") "sqrt" (add-op-expr (add-op-expr (mul-op-expr (scope-ref-expr "dx") (scope-ref-expr "dx") ) (mul-op-expr (scope-ref-expr "dy") (scope-ref-expr "dy") ) ) (mul-op-expr (scope-ref-expr "dz") (scope-ref-expr "dz") ) )) ))
                        (expr-stat (scope-assign-expr "mag" (div-op-expr (scope-ref-expr "dt") (mul-op-expr (mul-op-expr (scope-ref-expr "distance") (scope-ref-expr "distance") ) (scope-ref-expr "distance") ) ) ))
                        (expr-stat (static-assign-expr (scope-ref-expr "bodyi") "vx" (sub-op-expr (static-ref-expr (scope-ref-expr "bodyi") "vx") (mul-op-expr (mul-op-expr (scope-ref-expr "dx") (static-ref-expr (scope-ref-expr "bodyj") "mass") ) (scope-ref-expr "mag") ) ) ))
                        (expr-stat (static-assign-expr (scope-ref-expr "bodyi") "vy" (sub-op-expr (static-ref-expr (scope-ref-expr "bodyi") "vy") (mul-op-expr (mul-op-expr (scope-ref-expr "dy") (static-ref-expr (scope-ref-expr "bodyj") "mass") ) (scope-ref-expr "mag") ) ) ))
                        (expr-stat (static-assign-expr (scope-ref-expr "bodyi") "vz" (sub-op-expr (static-ref-expr (scope-ref-expr "bodyi") "vz") (mul-op-expr (mul-op-expr (scope-ref-expr "dz") (static-ref-expr (scope-ref-expr "bodyj") "mass") ) (scope-ref-expr "mag") ) ) ))
                        (expr-stat (static-assign-expr (scope-ref-expr "bodyj") "vx" (add-op-expr (static-ref-expr (scope-ref-expr "bodyj") "vx") (mul-op-expr (mul-op-expr (scope-ref-expr "dx") (static-ref-expr (scope-ref-expr "bodyi") "mass") ) (scope-ref-expr "mag") ) ) ))
                        (expr-stat (static-assign-expr (scope-ref-expr "bodyj") "vy" (add-op-expr (static-ref-expr (scope-ref-expr "bodyj") "vy") (mul-op-expr (mul-op-expr (scope-ref-expr "dy") (static-ref-expr (scope-ref-expr "bodyi") "mass") ) (scope-ref-expr "mag") ) ) ))
                        (expr-stat (static-assign-expr (scope-ref-expr "bodyj") "vz" (add-op-expr (static-ref-expr (scope-ref-expr "bodyj") "vz") (mul-op-expr (mul-op-expr (scope-ref-expr "dz") (static-ref-expr (scope-ref-expr "bodyi") "mass") ) (scope-ref-expr "mag") ) ) ))
                    )
                    (expr-stat (scope-assign-expr "j" (add-op-expr (scope-ref-expr "j") (num-literal 1) ) ))
                ))
            )
            (expr-stat (scope-assign-expr "i" (add-op-expr (scope-ref-expr "i") (num-literal 1) ) ))
        ))
        (expr-stat (scope-assign-expr "i" (num-literal 0))) 
 (while-stat (lt-op-expr (scope-ref-expr "i") (scope-ref-expr "size") )(block-stat 
            (block-stat 
                (expr-stat (scope-assign-expr "body" (dyn-ref-expr (static-ref-expr (this-expr) "bodies") (scope-ref-expr "i"))))
                (expr-stat (static-assign-expr (scope-ref-expr "body") "x" (add-op-expr (static-ref-expr (scope-ref-expr "body") "x") (mul-op-expr (scope-ref-expr "dt") (static-ref-expr (scope-ref-expr "body") "vx") ) ) ))
                (expr-stat (static-assign-expr (scope-ref-expr "body") "y" (add-op-expr (static-ref-expr (scope-ref-expr "body") "y") (mul-op-expr (scope-ref-expr "dt") (static-ref-expr (scope-ref-expr "body") "vy") ) ) ))
                (expr-stat (static-assign-expr (scope-ref-expr "body") "z" (add-op-expr (static-ref-expr (scope-ref-expr "body") "z") (mul-op-expr (scope-ref-expr "dt") (static-ref-expr (scope-ref-expr "body") "vz") ) ) ))
            )
            (expr-stat (scope-assign-expr "i" (add-op-expr (scope-ref-expr "i") (num-literal 1) ) ))
        ))
    ) )) ))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "NBodySystem") "prototype") "energy" (func-literal (func-closure nil []  #{"dx" "dy" "dz" "distance" "e" "size" "bodyi" "bodyj" "j" "i"} (block-stat 
           
        (expr-stat (scope-assign-expr "e" (num-literal 0)))
        (expr-stat (scope-assign-expr "size" (static-ref-expr (static-ref-expr (this-expr) "bodies") "length")))
        (expr-stat (scope-assign-expr "i" (num-literal 0))) 
 (while-stat (lt-op-expr (scope-ref-expr "i") (scope-ref-expr "size") )(block-stat 
            (block-stat 
                (expr-stat (scope-assign-expr "bodyi" (dyn-ref-expr (static-ref-expr (this-expr) "bodies") (scope-ref-expr "i"))))
                (expr-stat (scope-assign-expr "e" (add-op-expr (scope-ref-expr "e") (mul-op-expr (mul-op-expr (num-literal 0.5) (static-ref-expr (scope-ref-expr "bodyi") "mass") ) (add-op-expr (add-op-expr (mul-op-expr (static-ref-expr (scope-ref-expr "bodyi") "vx") (static-ref-expr (scope-ref-expr "bodyi") "vx") ) (mul-op-expr (static-ref-expr (scope-ref-expr "bodyi") "vy") (static-ref-expr (scope-ref-expr "bodyi") "vy") ) ) (mul-op-expr (static-ref-expr (scope-ref-expr "bodyi") "vz") (static-ref-expr (scope-ref-expr "bodyi") "vz") ) ) ) ) ))
                (expr-stat (scope-assign-expr "j" (add-op-expr (scope-ref-expr "i") (num-literal 1) ))) 
 (while-stat (lt-op-expr (scope-ref-expr "j") (scope-ref-expr "size") )(block-stat 
                    (block-stat 
                        (expr-stat (scope-assign-expr "bodyj" (dyn-ref-expr (static-ref-expr (this-expr) "bodies") (scope-ref-expr "j"))))
                        (expr-stat (scope-assign-expr "dx" (sub-op-expr (static-ref-expr (scope-ref-expr "bodyi") "x") (static-ref-expr (scope-ref-expr "bodyj") "x") ) ))
                        (expr-stat (scope-assign-expr "dy" (sub-op-expr (static-ref-expr (scope-ref-expr "bodyi") "y") (static-ref-expr (scope-ref-expr "bodyj") "y") ) ))
                        (expr-stat (scope-assign-expr "dz" (sub-op-expr (static-ref-expr (scope-ref-expr "bodyi") "z") (static-ref-expr (scope-ref-expr "bodyj") "z") ) ))
                        (expr-stat (scope-assign-expr "distance" (static-method-call-expr (scope-ref-expr "Math") "sqrt" (add-op-expr (add-op-expr (mul-op-expr (scope-ref-expr "dx") (scope-ref-expr "dx") ) (mul-op-expr (scope-ref-expr "dy") (scope-ref-expr "dy") ) ) (mul-op-expr (scope-ref-expr "dz") (scope-ref-expr "dz") ) )) ))
                        (expr-stat (scope-assign-expr "e" (sub-op-expr (scope-ref-expr "e") (div-op-expr (mul-op-expr (static-ref-expr (scope-ref-expr "bodyi") "mass") (static-ref-expr (scope-ref-expr "bodyj") "mass") ) (scope-ref-expr "distance") ) ) ))
                    )
                    (expr-stat (scope-assign-expr "j" (add-op-expr (scope-ref-expr "j") (num-literal 1) ) ))
                ))
            )
            (expr-stat (scope-assign-expr "i" (add-op-expr (scope-ref-expr "i") (num-literal 1) ) ))
        ))
        (ret-stat  (scope-ref-expr "e"))
    ) )) ))
    (expr-stat (scope-assign-expr "n" (num-literal 5e7)))
    (expr-stat (scope-assign-expr "bodies" (new-expr  (scope-ref-expr "NBodySystem") (call-expr (scope-ref-expr "Array") (call-expr (scope-ref-expr "Sun") ) (call-expr (scope-ref-expr "Jupiter") ) (call-expr (scope-ref-expr "Saturn") ) (call-expr (scope-ref-expr "Uranus") ) (call-expr (scope-ref-expr "Neptune") )))))
    (expr-stat (call-expr (scope-ref-expr "print") (static-method-call-expr (scope-ref-expr "bodies") "energy" )))
    (expr-stat (scope-assign-expr "i" (num-literal 0))) 
 (while-stat (lt-op-expr (scope-ref-expr "i") (scope-ref-expr "n") )(block-stat 
        (expr-stat (static-method-call-expr (scope-ref-expr "bodies") "advance" (num-literal 0.01)))
        (expr-stat (scope-assign-expr "i" (add-op-expr (scope-ref-expr "i") (num-literal 1) ) ))
    ))
    (expr-stat (call-expr (scope-ref-expr "print") (static-method-call-expr (scope-ref-expr "bodies") "energy" )))
) )
)

;
; output
;

(let [ast josephus-ast
      profile (asm-analyze-ast ast nil (new-asm-profile))]
  ; closures
  (println "Closures...")
	(doseq [[path bytes] (asm-compile-closure-classes profile)]
		(write-file (str "out/" path) bytes))
	
  ; constants
  (println "Constants...")
	(write-file "out/JSConstants.class" (asm-compile-constants-class profile))
	
  ; object shim
  (println "Objects...")
	(write-file "out/JSObject.class" (asm-compile-object-class profile))
	
  ; scopes
  (println "Scopes...")
	(doseq [[path bytes] (asm-compile-scope-classes profile)]
		(write-file (str "out/" path) bytes))
	
  ; script entry point
  (println "Script...")
	(write-file "out/JSScript.class" (asm-compile-script-class (index-of (profile :closures) ast)))
 
  (println "Done."))