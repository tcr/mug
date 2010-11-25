;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; mug.ast
;
; AST definition and helper functions
;

(ns mug.ast)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; env
;

(def script-default-vars #{"exports" "require" "print" "Math" "Array" "parseInt" "parseFloat" "Number" "Object" "String" "Boolean"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; ast nodes
;

(defmacro defast [type parent types]
  (do
		(derive type parent)
	  (eval (list 'defn (symbol (name type)) types
	    (concat (list 'list type) types)))))
				
; contexts

(derive ::context ::ast-node)

(defast ::script-context ::closure [stats])
(defast ::closure-context ::closure [name args stats])

; literals

(derive ::literal ::expr)

(defast ::num-literal ::literal [value])
(defast ::str-literal ::literal [value])
(defast ::obj-literal ::literal [props])
(defast ::array-literal ::literal [exprs])
(defast ::undef-literal ::literal [])
(defast ::null-literal ::literal [])
(defast ::boolean-literal ::literal [value])
(defast ::func-literal ::literal [closure])
(defast ::regexp-literal ::literal [expr flags])

; operations

(derive ::op-expr ::expr)
(derive ::unary-op-expr ::op-expr)
(derive ::binary-op-expr ::op-expr)

(defast ::post-inc-op-expr ::unary-op-expr [expr])
(defast ::pre-inc-op-expr ::unary-op-expr [expr])
(defast ::num-op-expr ::unary-op-expr [expr])
(defast ::neg-op-expr ::unary-op-expr [expr])
(defast ::not-op-expr ::unary-op-expr [expr])
(defast ::lt-op-expr ::binary-op-expr [left right])
(defast ::lte-op-expr ::binary-op-expr [left right])
(defast ::gt-op-expr ::binary-op-expr [left right])
(defast ::gte-op-expr ::binary-op-expr [left right])
(defast ::add-op-expr ::binary-op-expr [left right])
(defast ::sub-op-expr ::binary-op-expr [left right])
(defast ::mul-op-expr ::binary-op-expr [left right])
(defast ::div-op-expr ::binary-op-expr [left right])
(defast ::mod-op-expr ::binary-op-expr [left right])
(defast ::or-op-expr ::binary-op-expr [left right])
(defast ::and-op-expr ::binary-op-expr [left right])
(defast ::lsh-op-expr ::binary-op-expr [left right])
(defast ::eq-op-expr ::binary-op-expr [left right])
(defast ::eqs-op-expr ::binary-op-expr [left right])
(defast ::neq-op-expr ::binary-op-expr [left right])
(defast ::neqs-op-expr ::binary-op-expr [left right])

; expressions

(derive ::expr ::node)

(defast ::seq-expr ::expr [pre expr])
(defast ::this-expr ::expr [])
(defast ::scope-ref-expr ::expr [value])
(defast ::static-ref-expr ::expr [base value])
(defast ::dyn-ref-expr ::expr [base index])
(defast ::static-method-call-expr ::expr [base value args])
(defast ::dyn-method-call-expr ::expr [base index args])
(defast ::new-expr ::expr [constructor args])
(defast ::call-expr ::expr [expr args])
(defast ::scope-assign-expr ::expr [value expr])
(defast ::static-assign-expr ::expr [base value expr])
(defast ::dyn-assign-expr ::expr [base index expr])
(defast ::typeof-expr ::expr [expr])
(defast ::if-expr ::expr [expr then-expr else-expr])

; statements

(derive ::stat ::node)

(defast ::block-stat ::stat [stats])
(defast ::expr-stat ::stat [expr])
(defast ::ret-stat ::stat [expr])
(defast ::if-stat ::stat [expr then-stat else-stat])
(defast ::while-stat ::stat [expr stat])
(defast ::do-while-stat ::stat [expr stat])
(defast ::for-stat ::stat [init expr step stat])
(defast ::for-in-stat ::stat [isvar value expr stat])
(defast ::var-stat ::stat [vars]) 
(defast ::defn-stat ::stat [closure])
(defast ::break-stat ::stat [label])
(defast ::continue-stat ::stat [label])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; ast walker
;
; Method for walking AST and iterating all nodes, resulting
; in an array of data. Custom walkers defer to this method
; using third argument for all nodes except those they are
; interested in
;

(defmulti ast-walker (fn [node walker] (first node)))
(defmethod ast-walker :default [node walker]
  (println (str "Warning: No AST walker found for type " (first node) " (contents: " node ")")))

; contexts

(defmethod ast-walker ::script-context [[_ stats] walker]
  (apply concat (map #(walker % walker) stats)))
(defmethod ast-walker ::closure-context [[_ name args stats] walker]
  (apply concat (map #(walker % walker) stats)))

; literals

(defmethod ast-walker ::null-literal [[_] walker]
  [])
(defmethod ast-walker ::boolean-literal [[_ value] walker]
  [])
(defmethod ast-walker ::num-literal [[_ value] walker]
  [])
(defmethod ast-walker ::str-literal [[_ value] walker]
  [])
(defmethod ast-walker ::regexp-literal [[_ expr flags] walker]
  [])
(defmethod ast-walker ::array-literal [[_ exprs] walker]
  (apply concat (map #(walker % walker) exprs)))
(defmethod ast-walker ::obj-literal [[_ props] walker]
  (apply concat (map #(walker % walker) (vals props))))
(defmethod ast-walker ::func-literal [[_ closure] walker]
  (walker closure walker))
  
; operations

(defmethod ast-walker ::unary-op-expr [[_ expr] walker]
  (walker expr walker))
(defmethod ast-walker ::binary-op-expr [[_ left right] walker]
  (concat (walker left walker) (walker right walker)))

; expressions

(defmethod ast-walker ::scope-ref-expr [[_ value] walker]
  [])
(defmethod ast-walker ::static-ref-expr [[_ base value] walker]
  (walker base walker))
(defmethod ast-walker ::dyn-ref-expr [[_ base index] walker]
  (concat (walker base walker) (walker index walker)))
(defmethod ast-walker ::static-method-call-expr [[_ base value args] walker]
  (concat (walker base walker) (apply concat (map #(walker % walker) args))))
(defmethod ast-walker ::call-expr [[_ expr args] walker]
  (concat (walker expr walker) (apply concat (map #(walker % walker) args))))
(defmethod ast-walker ::new-expr [[_ constructor args] walker]
  (concat (walker constructor walker) (apply concat (map #(walker % walker) args))))
(defmethod ast-walker ::scope-assign-expr [[_ value expr] walker]
  (walker expr walker))
(defmethod ast-walker ::static-assign-expr [[_ base value expr] walker]
  (concat (walker base walker) (walker expr walker)))
(defmethod ast-walker ::dyn-assign-expr [[_ base index expr] walker]
  (concat (walker base walker) (walker index walker) (walker expr walker)))
(defmethod ast-walker ::typeof-expr [[_ expr] walker]
  (walker expr walker))
(defmethod ast-walker ::this-expr [[_] walker]
  [])
(defmethod ast-walker ::if-expr [[_ expr then-expr else-expr] walker]
  (concat (walker expr walker) (walker then-expr walker) (walker else-expr walker)))
(defmethod ast-walker ::seq-expr [[_ pre expr] walker]
  (concat (walker pre walker) (walker expr walker)))

; statements

(defmethod ast-walker ::block-stat [[_ stats] walker]
  (apply concat (map #(walker % walker) stats)))
(defmethod ast-walker ::expr-stat [[_ expr] walker]
  (walker expr walker))
(defmethod ast-walker ::ret-stat [[_ expr] walker]
  (walker expr walker))
(defmethod ast-walker ::while-stat [[_ expr stat] walker]
  (concat (walker expr walker) (walker stat walker)))
(defmethod ast-walker ::do-while-stat [[_ expr stat] walker]
  (concat (walker expr walker) (walker stat walker)))
(defmethod ast-walker ::for-stat [[_ init expr step stat] walker]
  (concat
    (if init (walker init walker) [])
    (if expr (walker expr walker) [])
    (if step (walker step walker) [])
    (walker stat walker)))
(defmethod ast-walker ::if-stat [[_ expr then-stat else-stat] walker]
  (concat
    (walker expr walker)
    (walker then-stat walker)
    (if else-stat (walker else-stat walker) [])))
(defmethod ast-walker ::break-stat [[_ label] walker]
  [])
(defmethod ast-walker ::continue-stat [[_ label] walker]
  [])
(defmethod ast-walker ::for-in-stat [[_ isvar value expr stat] walker]
  (concat (walker expr walker) (walker stat walker)))
(defmethod ast-walker ::var-stat [[_ vars] walker]
  (apply concat (map (fn [[k v]] (if (nil? v) [] (walker v walker))) vars))) 
(defmethod ast-walker ::defn-stat [[_ closure] walker]
  (walker closure walker))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; ast analysis
;

; strings

(defmulti ast-strings-walker (fn [node & _] (first node)))
(defmethod ast-strings-walker :default [node & _]
  (ast-walker node ast-strings-walker))
(defmethod ast-strings-walker ::str-literal [[_ value] & _]
  [value])

(def ast-strings
  (memoize (fn [node]
    (vec (set (ast-strings-walker node))))))

; numbers

(defmulti ast-numbers-walker (fn [node & _] (first node)))
(defmethod ast-numbers-walker :default [node & _]
  (ast-walker node ast-numbers-walker))
(defmethod ast-numbers-walker ::num-literal [[_ value] & _]
  [value])

(def ast-numbers
  (memoize (fn [node]
    (vec (set (ast-numbers-walker node))))))

; regexps

(defmulti ast-regexps-walker (fn [node & _] (first node)))
(defmethod ast-regexps-walker :default [node & _]
  (ast-walker node ast-regexps-walker))
(defmethod ast-regexps-walker ::regexp-literal [[_ expr flags] & _]
  [[expr flags]])

(def ast-regexps
  (memoize (fn [node]
    (vec (set (ast-regexps-walker node))))))

; contexts

(defmulti ast-contexts-walker (fn [node] (first node)))
(defmethod ast-contexts-walker :default [node]
  (ast-walker node (fn [node & _] (ast-contexts-walker node))))
(defmethod ast-contexts-walker ::script-context [node]
  (let [[_ stats] node]
    (concat [node] (apply concat (map #(ast-contexts-walker %) stats))))) 
(defmethod ast-contexts-walker ::closure-context [node]
  (let [[_ name args stats] node]
    (concat [node] (apply concat (map #(ast-contexts-walker %) stats)))))

(def ast-contexts
  (memoize (fn [node]
    (vec (ast-contexts-walker node)))))

; context hierarchy

(defmulti ast-context-parents-walker (fn [node] (first node)))
(defmethod ast-context-parents-walker :default [node]
  (ast-walker node (fn [node & _] (ast-context-parents-walker node))))
(defmethod ast-context-parents-walker ::script-context [node]
  (let [[_ stats] node]
    [(concat [] (apply concat (map #(ast-context-parents-walker %) stats)))]))
(defmethod ast-context-parents-walker ::closure-context [node]
  (let [[_ name args stats] node]
    [(concat [] (apply concat (map #(ast-context-parents-walker %) stats)))]))

(defn tree-hierarchy [[& items] idx prn]
  (concat [prn]
  (loop [items items prev [] nidx idx]
    (if (empty? items)
      prev
      ; depth-first
      (let [children (tree-hierarchy (first items) (+ nidx 1) (conj prn idx))]
        ; then breadth
        (recur (next items) (concat prev children) (+ nidx (count children))))))))

(def ast-context-hierarchy
  (memoize (fn [node]
    (vec (tree-hierarchy (first (ast-context-parents-walker node)) 0 [])))))

; vars

(defmulti ast-context-vars-walker (fn [node] (first node)))
(defmethod ast-context-vars-walker :default [node]
  (ast-walker node (fn [node & _] (ast-context-vars-walker node))))
; root nodes
(defmethod ast-context-vars-walker ::closure-context [[_ name args stats]]
  (concat (if (nil? name) [] [name]) args (apply concat (map #(ast-context-vars-walker %) stats))))
; definitions
(defmethod ast-context-vars-walker ::var-stat [[_ vars]]
  (map first vars))
(defmethod ast-context-vars-walker ::for-in-stat [[_ isvar value expr stat]]
  (if isvar [value] []))
; skip nested contexts
(defmethod ast-context-vars-walker ::defn-stat [[_ [_ name args stats]]]
  [name])
(defmethod ast-context-vars-walker ::func-literal [[_ [_ name args stats]]]
  (if (nil? name) [] [name]))

(def ast-context-vars
  (memoize (fn [node]
    (set (ast-context-vars-walker node)))))

; undeclared variables

(defmulti ast-context-globals-walker (fn [node vars] (first node)))
(defmethod ast-context-globals-walker :default [node vars]
  (ast-walker node (fn [node & _] (ast-context-globals-walker node vars))))
; contexts
(defmethod ast-context-globals-walker ::script-context [node vars]
  (let [[_ stats] node
        vars (into vars (ast-context-vars node))]
    (apply concat (map #(ast-context-globals-walker % vars) stats))))
(defmethod ast-context-globals-walker ::closure-context [node vars]
  (let [[_ name args stats] node
        vars (into vars (concat args [name] (ast-context-vars node)))]
    (apply concat (map #(ast-context-globals-walker % vars) stats))))
; scope references
(defmethod ast-context-globals-walker ::scope-ref-expr [[_ value] vars]
  (if (contains? vars value) [] [value]))
(defmethod ast-context-globals-walker ::scope-assign-expr [[_ value expr] vars]
  (if (contains? vars value) [] [value]))

(def ast-undeclared-vars
  (memoize (fn [node]
    (set (ast-context-globals-walker node #{})))))

; enclosed variables

(defmulti ast-descendent-contexts-walker (fn [node] (first node)))
(defmethod ast-descendent-contexts-walker :default [node]
  (ast-walker node (fn [node walker] (ast-descendent-contexts-walker node))))
(defmethod ast-descendent-contexts-walker ::closure-context [node]
  [node])

(defmulti ast-descendent-contexts (fn [node] (first node)))
(defmethod ast-descendent-contexts ::script-context [[_ stats]]
  (apply concat (map ast-descendent-contexts-walker stats)))
(defmethod ast-descendent-contexts ::closure-context [[_ name args stats]]
  (apply concat (map ast-descendent-contexts-walker stats)))

(defn ast-enclosed-vars [node vars]
  (set (filter #(contains? vars %) (apply concat (map ast-undeclared-vars (ast-descendent-contexts node))))))
;  (memoize (fn [node]
;    (set (ast-context-globals-walker node #{})))))