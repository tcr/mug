(ns mug.ast)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; mug ast
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; nodes
(defn js-ast [contexts structs accessors numbers strings]
  {:contexts contexts :structs structs :accessors accessors :numbers numbers :strings strings})

; ast

(defmacro defast [type parent types]
	(do
		(derive type parent)
		(let [keywords (conj (map keyword (map name (filter (fn [x] (not= '& x)) types))) :type)
			symbols (conj (filter (fn [x] (not= '& x)) types) type)
			ast-struct (symbol (str "ast-" (name type)))]
			(eval (concat (list 'defstruct ast-struct) keywords))
			(eval (list 'defn (symbol (name type)) types
				(list 'apply 'struct (list 'cons ast-struct (vec symbols))))))))
				
; contexts
(derive ::context ::ast-node)
(defast ::closure-context ::closure [parents name args vars & stats])
(defast ::script-context ::closure [globals vars & stats])

; literals
(derive ::literal ::expr)
(defast ::num-literal ::literal [value])
(defast ::str-literal ::literal [value])
(defast ::obj-literal ::literal [map])
(defast ::array-literal ::literal [exprs])
(defast ::undef-literal ::literal [])
(defast ::null-literal ::literal [])
(defast ::boolean-literal ::literal [value])
(defast ::func-literal ::literal [closure])

; operations
(derive ::op-expr ::expr)
(derive ::unary-op-expr ::op-expr)
	(defn operand [n] (:expr n))
(derive ::binary-op-expr ::op-expr)
	(defn left-operand [n] (:left n))
	(defn right-operand [n] (:right n))
(defast ::num-op-expr ::unary-op-expr [expr])
(defast ::neg-op-expr ::unary-op-expr [expr])
(defast ::lt-op-expr ::binary-op-expr [left right])
(defast ::lte-op-expr ::binary-op-expr [left right])
(defast ::gt-op-expr ::binary-op-expr [left right])
(defast ::gte-op-expr ::binary-op-expr [left right])
(defast ::add-op-expr ::binary-op-expr [left right])
(defast ::sub-op-expr ::binary-op-expr [left right])
(defast ::mul-op-expr ::binary-op-expr [left right])
(defast ::div-op-expr ::binary-op-expr [left right])
(defast ::mod-op-expr ::binary-op-expr [left right])
(defast ::lsh-op-expr ::binary-op-expr [left right])
(defast ::eq-op-expr ::binary-op-expr [left right])
(defast ::eqs-op-expr ::binary-op-expr [left right])
(defast ::neq-op-expr ::binary-op-expr [left right])
(defast ::neqs-op-expr ::binary-op-expr [left right])

; expressions
(derive ::expr ::ast-node)
(defast ::stats-expr ::expr [stats expr])
(defast ::this-expr ::expr [])
(defast ::scope-ref-expr ::expr [value])
(defast ::static-ref-expr ::expr [base value])
(defast ::dyn-ref-expr ::expr [base index])
(defast ::static-method-call-expr ::expr [base value & args])
(defast ::dyn-method-call-expr ::expr [base index & args])
(defast ::new-expr ::expr [constructor & args])
(defast ::call-expr ::expr [ref & args])
(defast ::scope-assign-expr ::expr [value expr])
(defast ::static-assign-expr ::expr [base value expr])
(defast ::dyn-assign-expr ::expr [base index expr])
(defast ::typeof-expr ::expr [ref])
(defast ::if-expr ::expr [expr then-expr else-expr])
(comment
	(defast ::class-expr ::expr [name prototype constructor static])
		(defn class-name [n] (:name n))
		(defn class-static [n] (:static n))
		(defn class-prototype [n] (:prototype n))
  (defast ::constructor ::ast-node [closure])
)

; statements
(derive ::stat ::ast-node)
(defast ::if-stat ::stat [expr then-stat else-stat])
(defast ::class-stat ::stat [name prototype constructor static])
(defast ::ret-stat ::stat [expr])
(defast ::while-stat ::stat [expr stat])
(defast ::expr-stat ::stat [expr])
(defast ::block-stat ::stat [& stats])
(comment
  (defast ::for-in-stat ::stat [value from to by stat])
)