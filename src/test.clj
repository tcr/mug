(ns test
  (:use mug [mug.asm code contexts constants object scopes util ; profile script
             ]
    clojure.java.io))

;
; file i/o
;

(import (java.io FileOutputStream File))

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
  (js-ast
[
(script-context #{ } #{ "Chain" "ITER" "Person" "chain" "end" "i" "start" } (block-stat 
          
    (expr-stat (scope-assign-expr "Person" (func-literal 1) ))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "Person") "prototype") "count" (num-literal 0) ))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "Person") "prototype") "prev" (null-literal) ))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "Person") "prototype") "next" (null-literal) ))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "Person") "prototype") "shout" (func-literal 2) ))
    (expr-stat (scope-assign-expr "Chain" (func-literal 3) ))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "Chain") "prototype") "first" (null-literal) ))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "Chain") "prototype") "kill" (func-literal 4) ))
    (expr-stat (scope-assign-expr "ITER" (num-literal 5e5) ))
    (scope-assign-expr "i" (num-literal 0) ) 
 (while-stat (lt-op-expr (scope-ref-expr "i") (scope-ref-expr "ITER") )(block-stat 
        (block-stat 
            (expr-stat (scope-assign-expr "chain" (new-expr  (scope-ref-expr "Chain") (num-literal 40)) ))
            (expr-stat (static-method-call-expr (scope-ref-expr "chain") "kill" (num-literal 3)))
        )
        (expr-stat (scope-assign-expr "i" (add-op-expr (scope-ref-expr "i") (num-literal 1) ) ))
    ))
))
(closure-context [0] nil ["count"]  #{} (block-stat 
    (expr-stat (static-assign-expr (this-expr) "count" (scope-ref-expr "count") ))
    (ret-stat  (this-expr))
) )
(closure-context [0] nil ["shout" "deadif"]  #{} (block-stat 
    (if-stat (lt-op-expr (scope-ref-expr "shout") (scope-ref-expr "deadif") ) (expr-stat (ret-stat  (add-op-expr (scope-ref-expr "shout") (num-literal 1) ))) nil )
    (expr-stat (static-assign-expr (static-ref-expr (this-expr) "prev") "next" (static-ref-expr (this-expr) "next") ))
    (expr-stat (static-assign-expr (static-ref-expr (this-expr) "next") "prev" (static-ref-expr (this-expr) "prev") ))
    (ret-stat  (num-literal 1))
) )
(closure-context [0] nil ["size"]  #{"current" "i" "last"} (block-stat 
      
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
) )
(closure-context [0] nil ["nth"]  #{"current" "shout"} (block-stat 
     
    (expr-stat (scope-assign-expr "current" (static-ref-expr (this-expr) "first") ))
    (expr-stat (scope-assign-expr "shout" (num-literal 1) ))
    (while-stat (neqs-op-expr (static-ref-expr (scope-ref-expr "current") "next") (scope-ref-expr "current") ) (block-stat 
        (expr-stat (scope-assign-expr "shout" (static-method-call-expr (scope-ref-expr "current") "shout" (scope-ref-expr "shout") (scope-ref-expr "nth")) ))
        (expr-stat (scope-assign-expr "current" (static-ref-expr (scope-ref-expr "current") "next") ))
    ) )
    (expr-stat (static-assign-expr (this-expr) "first" (scope-ref-expr "current") ))
    (ret-stat  (scope-ref-expr "current"))
) )

]
[]
#{ "count" "prototype" "prev" "next" "shout" "first" "kill" }
#{ 0 1 3 40 5e5 }
#{ }
)
)

;
; binary tree problem
;

(def binary-ast
(js-ast
[
(script-context #{ "print" } #{ "TreeNode" "bottomUpTree" "minDepth" "n" "maxDepth" "stretchDepth" "check" "longLivedTree" "iterations" "i" "depth" } (block-stat 
    (expr-stat (scope-assign-expr "TreeNode" (func-literal 1)))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "TreeNode") "prototype") "itemCheck" (func-literal 2) ))
    (expr-stat (scope-assign-expr "bottomUpTree" (func-literal 3)))
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
                    (expr-stat (scope-assign-expr "check" (add-op-expr (scope-ref-expr "check") (static-method-call-expr (call-expr (scope-ref-expr "bottomUpTree") (sub-op-expr (num-literal 0) (scope-ref-expr "i") ) (scope-ref-expr "depth")) "itemCheck" ) ) ))
                )
                (expr-stat (scope-assign-expr "i" (add-op-expr (scope-ref-expr "i") (num-literal 1) ) ))
            ))
            (expr-stat (call-expr (scope-ref-expr "print") (add-op-expr (add-op-expr (add-op-expr (add-op-expr (mul-op-expr (scope-ref-expr "iterations") (num-literal 2) ) (str-literal "\t trees of depth ") ) (scope-ref-expr "depth") ) (str-literal "\t check: ") ) (scope-ref-expr "check") )))
        )
        (expr-stat (scope-assign-expr "depth" (add-op-expr (scope-ref-expr "depth") (num-literal 2) ) ))
    ))
    (expr-stat (call-expr (scope-ref-expr "print") (add-op-expr (add-op-expr (add-op-expr (str-literal "long lived tree of depth ") (scope-ref-expr "maxDepth") ) (str-literal "\t check: ") ) (static-method-call-expr (scope-ref-expr "longLivedTree") "itemCheck" ) )))
))
(closure-context [0] "TreeNode" ["left" "right" "item"]  #{} (block-stat 
    (expr-stat (static-assign-expr (this-expr) "left" (scope-ref-expr "left") ))
    (expr-stat (static-assign-expr (this-expr) "right" (scope-ref-expr "right") ))
    (expr-stat (static-assign-expr (this-expr) "item" (scope-ref-expr "item") ))
) )
(closure-context [0] nil []  #{} (if-stat (eqs-op-expr (static-ref-expr (this-expr) "left") (null-literal) ) (ret-stat  (static-ref-expr (this-expr) "item")) (ret-stat  (sub-op-expr (add-op-expr (static-ref-expr (this-expr) "item") (static-method-call-expr (static-ref-expr (this-expr) "left") "itemCheck" ) ) (static-method-call-expr (static-ref-expr (this-expr) "right") "itemCheck" ) )) ) )
(closure-context [0] "bottomUpTree" ["item" "depth"]  #{} (if-stat (gt-op-expr (scope-ref-expr "depth") (num-literal 0) ) (expr-stat (ret-stat  (new-expr  (scope-ref-expr "TreeNode") (call-expr (scope-ref-expr "bottomUpTree") (sub-op-expr (mul-op-expr (num-literal 2) (scope-ref-expr "item") ) (num-literal 1) ) (sub-op-expr (scope-ref-expr "depth") (num-literal 1) )) (call-expr (scope-ref-expr "bottomUpTree") (mul-op-expr (num-literal 2) (scope-ref-expr "item") ) (sub-op-expr (scope-ref-expr "depth") (num-literal 1) )) (scope-ref-expr "item")))) (expr-stat (ret-stat  (new-expr  (scope-ref-expr "TreeNode") (null-literal) (null-literal) (scope-ref-expr "item")))) ) )

]
[]
#{ "left" "right" "item" "itemCheck" "prototype" }
#{ 0 1 2 4 16 }
#{ "stretch tree of depth " "\t check: " "\t trees of depth " "long lived tree of depth " }
)
)

;
; test
;

(def test-ast
(js-ast
[
(script-context #{ "print" } #{ "a" "b" } (block-stat 
    (expr-stat (scope-assign-expr "a" (func-literal 1)))
    (expr-stat (scope-assign-expr "b" (func-literal 2)))
    (expr-stat (call-expr (scope-ref-expr "a") (str-literal "apples")))
    (expr-stat (call-expr (scope-ref-expr "b") (num-literal 5)))
))
(closure-context [0] nil ["str"]  #{"c" "str2"} (block-stat 
    (expr-stat (scope-assign-expr "c" (func-literal 3)))
    (expr-stat (scope-assign-expr "str2" (scope-ref-expr "str")))
    (expr-stat (scope-assign-expr "str" (num-literal 5) ))
    (expr-stat (call-expr (scope-ref-expr "c") (scope-ref-expr "str2")))
    (expr-stat (call-expr (scope-ref-expr "print") (scope-ref-expr "str")))
) )
(closure-context [0] nil ["left"]  #{} (expr-stat (call-expr (scope-ref-expr "print") (add-op-expr (scope-ref-expr "left") (num-literal 5) ))) )
(closure-context [0 1] "c" ["str"]  #{} (expr-stat (call-expr (scope-ref-expr "print") (scope-ref-expr "str"))) )

]
[]
#{ }
#{ 5 }
#{ "apples" }
)
)

;
; output
;

; clean output directory
(doseq [f (.listFiles (new File "out/mug/compiled"))]
  (delete-file f true))

; compile files
(let [ast binary-ast]
  ; script
  ;(println "Script...")
	;(write-file (str "out/" qn-js-script ".class") (asm-compile-script-class ast))
  
  ; closures
  (println "Closures...")
	(doseq [[qn bytes] (asm-compile-closure-classes ast)]
		(write-file (str "out/" qn ".class") bytes))
	
  ; constants
  (println "Constants...")
	(write-file (str "out/" qn-js-constants ".class") (asm-compile-constants-class ast))
	
  ; object shim
  (println "Objects...")
	(write-file (str "out/" qn-js-object ".class") (asm-compile-object-class ast))
	
  ; scopes
  (println "Scopes...")
	(doseq [[qn bytes] (asm-compile-scope-classes ast)]
		(write-file (str "out/" qn ".class") bytes))
 
  (println "Done."))