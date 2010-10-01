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
; output
;

(let [ast binary-ast
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