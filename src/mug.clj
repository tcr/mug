(ns mug)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; mug ast
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; util
(defn escape-str [s]
	(apply str (map (fn [c] (or (char-escape-string c) c)) s)))
;(defn flatten [x]
;	(filter (complement sequential?)
;		(rest (tree-seq sequential? seq x))))
(defn join [x y]
	(apply str (interpose x y)))

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
				
; closures
(derive ::closure ::ast-node)
(defast ::func-closure ::closure [name args vars & stats])
(defast ::global-closure ::closure [vars & stats])

; literals
(derive ::literal ::expr)
(defast ::num-literal ::literal [value])
(defast ::str-literal ::literal [value])
(defast ::obj-literal ::literal [map])
(defast ::array-literal ::literal [exprs])
(defast ::undef-literal ::literal [])
(defast ::null-literal ::literal [])
(defast ::func-literal ::literal [closure])

; operations
(derive ::op-expr ::expr)
(derive ::unary-op-expr ::op-expr)
	(defn operand [n] (:expr n))
(derive ::binary-op-expr ::op-expr)
	(defn left-operand [n] (:left n))
	(defn right-operand [n] (:right n))
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
(defast ::neq-op-expr ::binary-op-expr [left right])
(defast ::eq-op-expr ::binary-op-expr [left right])
(defast ::neqs-op-expr ::binary-op-expr [left right])
(defast ::eqs-op-expr ::binary-op-expr [left right])

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
(defast ::class-expr ::expr [name prototype constructor static])
	(defn class-name [n] (:name n))
	(defn class-static [n] (:static n))
	(defn class-prototype [n] (:prototype n))
(defast ::constructor ::ast-node [closure])

; statements
(derive ::stat ::ast-node)
(defast ::block-stat ::stat [& stats])
(defast ::if-stat ::stat [expr then-stat else-stat])
(defast ::class-stat ::stat [name prototype constructor static])
(defast ::ret-stat ::stat [expr])
(defast ::while-stat ::stat [expr stat])
(defast ::for-in-stat ::stat [value from to by stat])
(defast ::expr-stat ::stat [expr])