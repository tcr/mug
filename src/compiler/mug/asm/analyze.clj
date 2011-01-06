(ns mug.asm.analyze
  (:gen-class)
  (:use
    mug.ast
    [mug.asm config util]))

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
  (throw (new Exception (str "No AST walker found for type " (first node) " (contents: " node ")"))))

; contexts

(defmethod ast-walker :mug.ast/script-context [[_ ln stats] walker]
  (apply concat (map #(walker % walker) stats)))
(defmethod ast-walker :mug.ast/closure-context [[_ ln name args stats] walker]
  (apply concat (map #(walker % walker) stats)))

; literals

(defmethod ast-walker :mug.ast/null-literal [[_ ln] walker]
  [])
(defmethod ast-walker :mug.ast/boolean-literal [[_ ln value] walker]
  [])
(defmethod ast-walker :mug.ast/num-literal [[_ ln value] walker]
  [])
(defmethod ast-walker :mug.ast/undef-literal [[_ ln value] walker]
  [])
(defmethod ast-walker :mug.ast/str-literal [[_ ln value] walker]
  [])
(defmethod ast-walker :mug.ast/regexp-literal [[_ ln expr flags] walker]
  [])
(defmethod ast-walker :mug.ast/array-literal [[_ ln exprs] walker]
  (apply concat (map #(walker % walker) exprs)))
(defmethod ast-walker :mug.ast/obj-literal [[_ ln props] walker]
  (apply concat (map #(walker % walker) (vals props))))
(defmethod ast-walker :mug.ast/func-literal [[_ ln closure] walker]
  (walker closure walker))
  
; operations

(defmethod ast-walker :mug.ast/unary-op-expr [[_ ln expr] walker]
  (walker expr walker))
(defmethod ast-walker :mug.ast/binary-op-expr [[_ ln left right] walker]
  (concat (walker left walker) (walker right walker)))

; expressions

(defmethod ast-walker :mug.ast/scope-ref-expr [[_ ln value] walker]
  [])
(defmethod ast-walker :mug.ast/static-ref-expr [[_ ln base value] walker]
  (walker base walker))
(defmethod ast-walker :mug.ast/dyn-ref-expr [[_ ln base index] walker]
  (concat (walker base walker) (walker index walker)))
(defmethod ast-walker :mug.ast/static-method-call-expr [[_ ln base value args] walker]
  (concat (walker base walker) (apply concat (map #(walker % walker) args))))
(defmethod ast-walker :mug.ast/call-expr [[_ ln expr args] walker]
  (concat (walker expr walker) (apply concat (map #(walker % walker) args))))
(defmethod ast-walker :mug.ast/new-expr [[_ ln constructor args] walker]
  (concat (walker constructor walker) (apply concat (map #(walker % walker) args))))
(defmethod ast-walker :mug.ast/scope-assign-expr [[_ ln value expr] walker]
  (walker expr walker))
(defmethod ast-walker :mug.ast/static-assign-expr [[_ ln base value expr] walker]
  (concat (walker base walker) (walker expr walker)))
(defmethod ast-walker :mug.ast/dyn-assign-expr [[_ ln base index expr] walker]
  (concat (walker base walker) (walker index walker) (walker expr walker)))
(defmethod ast-walker :mug.ast/scope-delete-expr [[_ ln value] walker]
  [])
(defmethod ast-walker :mug.ast/static-delete-expr [[_ ln base value] walker]
  (walker base walker))
(defmethod ast-walker :mug.ast/dyn-delete-expr [[_ ln base index] walker]
  (walker base walker) (walker index walker))
(defmethod ast-walker :mug.ast/typeof-expr [[_ ln expr] walker]
  (walker expr walker))
(defmethod ast-walker :mug.ast/void-expr [[_ ln expr] walker]
  (walker expr walker))
(defmethod ast-walker :mug.ast/this-expr [[_ ln] walker]
  [])
(defmethod ast-walker :mug.ast/if-expr [[_ ln expr then-expr else-expr] walker]
  (concat (walker expr walker) (walker then-expr walker) (walker else-expr walker)))
(defmethod ast-walker :mug.ast/seq-expr [[_ ln pre expr] walker]
  (concat (walker pre walker) (walker expr walker)))

; statements

(defmethod ast-walker :mug.ast/block-stat [[_ ln stats] walker]
  (apply concat (map #(walker % walker) stats)))
(defmethod ast-walker :mug.ast/expr-stat [[_ ln expr] walker]
  (walker expr walker))
(defmethod ast-walker :mug.ast/ret-stat [[_ ln expr] walker]
  (if expr (walker expr walker) []))
(defmethod ast-walker :mug.ast/throw-stat [[_ ln expr] walker]
  (walker expr walker))
(defmethod ast-walker :mug.ast/while-stat [[_ ln expr stat] walker]
  (concat (walker expr walker) (walker stat walker)))
(defmethod ast-walker :mug.ast/do-while-stat [[_ ln expr stat] walker]
  (concat (walker expr walker) (walker stat walker)))
(defmethod ast-walker :mug.ast/try-stat [[_ ln stats catch-block finally-stats] walker]
  (concat
    (apply concat (map #(walker % walker) stats))
    (if-let [[label stats] catch-block]
      (apply concat (map #(walker % walker) stats))
      [])
    (if finally-stats
      (apply concat (map #(walker % walker) finally-stats))
      [])))
(defmethod ast-walker :mug.ast/for-stat [[_ ln init expr step stat] walker]
  (concat
    (if init (walker init walker) [])
    (if expr (walker expr walker) [])
    (if step (walker step walker) [])
    (walker stat walker)))
(defmethod ast-walker :mug.ast/if-stat [[_ ln expr then-stat else-stat] walker]
  (concat
    (walker expr walker)
    (walker then-stat walker)
    (if else-stat (walker else-stat walker) [])))
(defmethod ast-walker :mug.ast/switch-stat [[_ ln expr cases] walker]
  (concat
    (walker expr walker)
    (apply concat (map (fn [[expr stats]]
      (concat (if (nil? expr) [] (walker expr walker))
        (apply concat (map #(walker % walker) stats)))) cases))))
(defmethod ast-walker :mug.ast/break-stat [[_ ln label] walker]
  [])
(defmethod ast-walker :mug.ast/continue-stat [[_ ln label] walker]
  [])
(defmethod ast-walker :mug.ast/for-in-stat [[_ ln isvar value expr stat] walker]
  (concat (walker expr walker) (walker stat walker)))
(defmethod ast-walker :mug.ast/var-stat [[_ ln vars] walker]
  (apply concat (map (fn [[k v]] (if (nil? v) [] (walker v walker))) vars))) 
(defmethod ast-walker :mug.ast/defn-stat [[_ ln closure] walker]
  (walker closure walker))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; ast analysis
;

; strings

(defmulti ast-strings-walker (fn [node & _] (first node)))
(defmethod ast-strings-walker :default [node & _]
  (ast-walker node ast-strings-walker))
(defmethod ast-strings-walker :mug.ast/str-literal [[_ ln value] & _]
  [value])

(def ast-strings
  (memoize (fn [node]
    (vec (set (ast-strings-walker node))))))

; numbers

(defmulti ast-numbers-walker (fn [node & _] (first node)))
(defmethod ast-numbers-walker :default [node & _]
  (ast-walker node ast-numbers-walker))
(defmethod ast-numbers-walker :mug.ast/num-literal [[_ ln value] & _]
  [value])

(def ast-numbers
  (memoize (fn [node]
    (vec (set (ast-numbers-walker node))))))

; regexps

(defmulti ast-regexps-walker (fn [node & _] (first node)))
(defmethod ast-regexps-walker :default [node & _]
  (ast-walker node ast-regexps-walker))
(defmethod ast-regexps-walker :mug.ast/regexp-literal [[_ ln expr flags] & _]
  [[expr flags]])

(def ast-regexps
  (memoize (fn [node]
    (vec (set (ast-regexps-walker node))))))

; contexts

(defmulti ast-contexts-walker (fn [node] (first node)))
(defmethod ast-contexts-walker :default [node]
  (ast-walker node (fn [node & _] (ast-contexts-walker node))))
(defmethod ast-contexts-walker :mug.ast/script-context [node]
  (let [[_ ln stats] node]
    (concat [node] (apply concat (map #(ast-contexts-walker %) stats))))) 
(defmethod ast-contexts-walker :mug.ast/closure-context [node]
  (let [[_ ln name args stats] node]
    (concat [node] (apply concat (map #(ast-contexts-walker %) stats)))))

(def ast-contexts
  (memoize (fn [node]
    (vec (ast-contexts-walker node)))))

; context hierarchy
; this is a list of indexes 

(defmulti ast-context-parents-walker (fn [node] (first node)))
(defmethod ast-context-parents-walker :default [node]
  (ast-walker node (fn [node & _] (ast-context-parents-walker node))))
(defmethod ast-context-parents-walker :mug.ast/script-context [node]
  (let [[_ ln stats] node]
    [(concat [] (apply concat (map #(ast-context-parents-walker %) stats)))]))
(defmethod ast-context-parents-walker :mug.ast/closure-context [node]
  (let [[_ ln name args stats] node]
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

; various variable scope walkers:
; - all declared vars in a context
; - all undeclared vars in a context and its subcontexts
; - all subcontexts (used to find all undeclared vars in a subcontext declared by a parent context)

; vars (those declared by a context in var statements, but not its subcontexts)
; this includes the "arguments" variable

(defmulti ast-context-vars-walker (fn [node] (first node)))
(defmethod ast-context-vars-walker :default [node]
  (ast-walker node (fn [node & _] (ast-context-vars-walker node))))
; root nodes
(defmethod ast-context-vars-walker :mug.ast/closure-context [[_ ln name args stats]]
  (concat (if (nil? name) [] [name]) args (apply concat (map #(ast-context-vars-walker %) stats))))
; definitions
(defmethod ast-context-vars-walker :mug.ast/var-stat [node]
  (let [[_ ln vars] node]
    (concat ; var names, also arguments object may be used in expression
      (map first vars)
      (ast-walker node (fn [node & _] (ast-context-vars-walker node))))))
(defmethod ast-context-vars-walker :mug.ast/for-in-stat [[_ ln isvar value expr stat]]
  (concat
    (if isvar [value] [])
    (ast-context-vars-walker stat)))
(defmethod ast-context-vars-walker :mug.ast/try-stat [node]
  (let [[_ ln stats catch-block finally-stats] node]
	  (concat
      (if-let [[label _] catch-block] [label] [])
	    (ast-walker node (fn [node & _] (ast-context-vars-walker node))))))
; identifiers (only arguments variable)
(defmethod ast-context-vars-walker :mug.ast/scope-ref-expr [[_ ln value]]
  (if (= value "arguments") ["arguments"] []))
; skip nested contexts
(defmethod ast-context-vars-walker :mug.ast/defn-stat [[_ ln [_ ln name args stats]]]
  [name])
(defmethod ast-context-vars-walker :mug.ast/func-literal [[_ ln [_ ln name args stats]]]
  (if (nil? name) [] [name]))

(def ast-context-vars
  (memoize (fn [node]
    (set (ast-context-vars-walker node)))))

(defn ast-uses-arguments [context]
  (contains? (ast-context-vars context) "arguments"))

; undeclared variables (those variables without var statements)

(defmulti ast-context-globals-walker (fn [node vars] (first node)))
(defmethod ast-context-globals-walker :default [node vars]
  (ast-walker node (fn [node & _] (ast-context-globals-walker node vars))))
; contexts
(defmethod ast-context-globals-walker :mug.ast/script-context [node vars]
  (let [[_ ln stats] node
        vars (into vars (ast-context-vars node))]
    (apply concat (map #(ast-context-globals-walker % vars) stats))))
(defmethod ast-context-globals-walker :mug.ast/closure-context [node vars]
  (let [[_ ln name args stats] node
        vars (into vars (concat args [name] (ast-context-vars node)))]
    (apply concat (map #(ast-context-globals-walker % vars) stats))))
; scope references
(defmethod ast-context-globals-walker :mug.ast/scope-ref-expr [[_ ln value] vars]
  (if (contains? vars value) [] [value]))
(defmethod ast-context-globals-walker :mug.ast/scope-assign-expr [[_ ln value expr] vars]
  (concat (if (contains? vars value) [] [value]) (ast-context-globals-walker expr vars)))

(def ast-undeclared-vars
  (memoize (fn [node]
    (set (ast-context-globals-walker node #{})))))

; enclosed variables
; pass in as second argument all variables declared in context; returns
; all variables which are referenced by a child scope

(defmulti ast-descendent-contexts-walker (fn [node] (first node)))
(defmethod ast-descendent-contexts-walker :default [node]
  (ast-walker node (fn [node walker] (ast-descendent-contexts-walker node))))
(defmethod ast-descendent-contexts-walker :mug.ast/closure-context [node]
  [node])

(defmulti ast-descendent-contexts (fn [node] (first node)))
(defmethod ast-descendent-contexts :mug.ast/script-context [[_ ln stats]]
  (apply concat (map ast-descendent-contexts-walker stats)))
(defmethod ast-descendent-contexts :mug.ast/closure-context [[_ ln name args stats]]
  (apply concat (map ast-descendent-contexts-walker stats)))

(def ast-enclosed-vars
  (memoize (fn [context vars]
	  (set (filter #(contains? vars %)
	    (apply concat (map ast-undeclared-vars (ast-descendent-contexts context))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; scopes
;

(defmulti sig-context-init (fn [ci ast] (first ((ast-contexts ast) ci))))
(defmethod sig-context-init :mug.ast/script-context [ci ast]
  (sig-call sig-void))
(defmethod sig-context-init :mug.ast/closure-context [ci ast] 
  (apply sig-call (concat
    [(sig-obj qn-js-object)]
    (vec (map #(sig-obj (qn-js-scope %)) ((ast-context-hierarchy ast) ci)))
    [sig-void])))  

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; local variable registers
;
; Optimization when a variable is not used in a child context
; otherwise returns nil, and variable is saved as scope property
;

(defmulti ref-reg (fn [context value] (first context)))
(defmethod ref-reg :mug.ast/script-context [context value]
  (when *local-variable-opt*
	  (let [[_ ln stats] context
	        vars (ast-context-vars context)]
		  (when (not (contains? (ast-enclosed-vars context vars) value))
		    (when-let [pos (index-of (vec vars) value)]
		      (+ ref-offset-reg pos))))))
(defmethod ref-reg :mug.ast/closure-context [context value]
  (when *local-variable-opt*
	  (let [[_ ln name args stats] context
	        vars (ast-context-vars context)]
		  (when (not (contains? (ast-enclosed-vars context vars) value))
	      (do
				  (if-let [pos (index-of args value)]
				    (+ offset-reg pos)
				    (if-let [pos (index-of (vec vars) value)]
				      (+ ref-offset-reg pos)
	            (when (= name value)
	              0))))))))