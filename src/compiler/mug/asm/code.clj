(ns mug.asm.code
  (:gen-class)
  (:use
    mug.ast
    [mug.asm config analyze util])
  (:import
    [mug.js JSArray JSBoolean JSFunction JSModule JSNull JSNumber JSObject JSRegExp JSString JSEnvironment JSUtils]))

(import (org.objectweb.asm ClassWriter Opcodes Label))

;
; NOTES:
;
; local variable mapping:
;   0    this object
;   1    JS "this" object at a given time (function object, global object)
;   2    argument count
;   3-x  arguments
;   x+1    current scope object

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; expression typing
;
; Determines the (lowest common denominator of) the object
; or primitive type an expression will generate. Used for
; optimizing type conversion.
;

(defmulti compile-type (fn [node] (first node)))
(defmethod compile-type :default [[type & _]]
  (throw (new Exception (str "Missing compile type analysis for " type))))

;
; literals
;

(defmethod compile-type :mug.ast/null-literal [[_ ln]]
  JSNull)
(defmethod compile-type :mug.ast/boolean-literal [[_ ln value]]
	:boolean)
(defmethod compile-type :mug.ast/num-literal [[_ ln value]]
	:number)
(defmethod compile-type :mug.ast/undef-literal [[_ ln value]]
	Object)
(defmethod compile-type :mug.ast/str-literal [[_ ln value]]
	String)
(defmethod compile-type :mug.ast/regexp-literal [[_ ln expr flags]]
	JSRegExp)
(defmethod compile-type :mug.ast/array-literal [[_ ln exprs]]
	JSArray)
(defmethod compile-type :mug.ast/obj-literal [[_ ln props]]
	JSObject)
(defmethod compile-type :mug.ast/func-literal [[_ ln closure]]
  JSFunction)
  
;
; operations
;

(defmethod compile-type :mug.ast/add-op-expr [[_ ln left right]]
  ;[TODO] if types are known, then we know output type; so this should be dynamic
  Object)
(defmethod compile-type :mug.ast/sub-op-expr [[_ ln left right]]
  :number)
(defmethod compile-type :mug.ast/div-op-expr [[_ ln left right]]
  :number)
(defmethod compile-type :mug.ast/mul-op-expr [[_ ln left right]]
  :number)
(defmethod compile-type :mug.ast/mod-op-expr [[_ ln left right]]
  :number)
(defmethod compile-type :mug.ast/lsh-op-expr [[_ ln left right]]
  :number)
(defmethod compile-type :mug.ast/eq-op-expr [[_ ln left right]]
  :boolean)
(defmethod compile-type :mug.ast/neq-op-expr [[_ ln left right]]
  :boolean)
(defmethod compile-type :mug.ast/not-op-expr [[_ ln expr]]
  :boolean)
(defmethod compile-type :mug.ast/eqs-op-expr [[_ ln left right]]
  :boolean)
(defmethod compile-type :mug.ast/neqs-op-expr [[_ ln left right]]
  :boolean)
(defmethod compile-type :mug.ast/lt-op-expr [[_ ln left right]]
  :boolean)
(defmethod compile-type :mug.ast/lte-op-expr [[_ ln left right]]
  :boolean)
(defmethod compile-type :mug.ast/gt-op-expr [[_ ln left right]]
  :boolean)
(defmethod compile-type :mug.ast/gte-op-expr [[_ ln left right]]
  :boolean)
(defmethod compile-type :mug.ast/neg-op-expr [[_ ln expr]]
  :number)
(defmethod compile-type :mug.ast/or-op-expr [[_ ln left right]]
  Object)
(defmethod compile-type :mug.ast/and-op-expr [[_ ln left right]]
  Object)
(defmethod compile-type :mug.ast/if-expr [[_ ln left right]]
  Object)

;
; expressions
;

(defmethod compile-type :mug.ast/scope-ref-expr [[_ ln value]]
  Object)
(defmethod compile-type :mug.ast/static-ref-expr [[_ ln base value]]
  Object)
(defmethod compile-type :mug.ast/dyn-ref-expr [[_ ln base index]]
  Object)
(defmethod compile-type :mug.ast/static-method-call-expr [[_ ln base value args]]
  Object)
(defmethod compile-type :mug.ast/call-expr [[_ ln ref args]]
	Object)
(defmethod compile-type :mug.ast/new-expr [[_ ln constructor args]]
  Object)
(defmethod compile-type :mug.ast/scope-assign-expr [[_ ln value expr]]
	Object) ;(compile-type expr))
(defmethod compile-type :mug.ast/static-assign-expr [[_ ln base value expr]]
	Object) ;(compile-type expr))
(defmethod compile-type :mug.ast/dyn-assign-expr [[_ ln base index expr]]
	Object) ;(compile-type expr))
(defmethod compile-type :mug.ast/typeof-expr [[_ ln expr]]
  String)
(defmethod compile-type :mug.ast/this-expr [[_ ln]]
  Object)
(defmethod compile-type :mug.ast/if-expr [[_ ln expr then-expr else-expr]]
  Object)
(defmethod compile-type :mug.ast/seq-expr [[_ ln pre expr]]
  (compile-type expr))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; compilation prototype
;

(defn compile-line-number [ln mw]
  (if (not= (get-state "line-number") ln)
	  (let [label (new Label)]
	    (.visitLabel mw, label)
	    (.visitLineNumber mw, ln, label)
      (update-state "line-number" ln))))

(defmulti asm-compile
  (fn [node ci ast mw]
    (let [[type ln & args] node]
	    ; compile line-number
	    (compile-line-number (+ ln 1) mw)
      ; return type
	    type)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; utility functions
;

; label state

(defn push-label [label continue break]
  (update-state (list :label label)
    (conj (or (get-state (list :label label)) []) {:break break :continue continue})))
(defn pop-label [label]
  (update-state (list :label label)
    (pop (get-state (list :label label)))))
(defn get-label [label]
  (last (or (get-state (list :label label)) [])))

; pop and dup

(defn asm-compile-pop [node mw]
  (case (compile-type node)
    :number (.visitInsn mw Opcodes/POP2)
    (.visitInsn mw Opcodes/POP)))

; integer code optimization

(defn asm-compile-load-int [i mw]
  (if (and (>= i -128) (<= i 127))
    (.visitIntInsn mw Opcodes/BIPUSH, i)
    (.visitLdcInsn mw (new Integer i))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; asm utilities
;

; top-level asm

(defn asm-toplevel [ci ast mw]
  (if (= ci 0)
    (.visitVarInsn mw Opcodes/ALOAD, scope-reg)
    (do
      (.visitVarInsn mw Opcodes/ALOAD, 0)
      (.visitFieldInsn mw Opcodes/GETFIELD, (qn-js-context ci),
	      (str "SCOPE_" 0), (sig-obj (qn-js-scope 0)))
      (.visitTypeInsn mw Opcodes/CHECKCAST, qn-js-toplevel))))

; search scopes
; asm loads scope object, fn returns qn of scope object

(defn asm-search-scopes [name ci ast mw]
  (let [context ((ast-contexts ast) ci)
        parents ((ast-context-hierarchy ast) ci)]
;   (println (str "Seeking var \"" name "\" in current scope (" (ast-context-vars context) ")"))
	  (if (contains? (ast-context-vars context) name)
	    ; variable found in current scope
	    (do
	      (.visitVarInsn mw Opcodes/ALOAD, scope-reg)
	      (qn-js-scope ci))
	  (loop [parent (last parents) parents (butlast parents)]
		  (if (nil? parent)
		    (if (contains? script-default-vars name)
	        ; found variable in global scope
		      (do
; 	        (println (str " . Found in global scope: " name))
	          (if (= ci 0) ; script-context scope inherits from globals
	            (.visitVarInsn mw Opcodes/ALOAD, scope-reg)
	            (do
			          (.visitVarInsn mw Opcodes/ALOAD, 0)
			          (.visitFieldInsn mw Opcodes/GETFIELD, (qn-js-context ci),
			            (str "SCOPE_" 0), (sig-obj (qn-js-scope 0)))))
	          qn-js-toplevel)
	        ; identifier not found at all
		      (throw (new Exception (str "Identifier not defined in any scope: " name))))
			  (if (contains? (ast-context-vars ((ast-contexts ast) parent)) name)
	        ; found variable in ancestor scope
			    (do
;  		      (println (str " . Found in higher scope: " name))
			      (.visitVarInsn mw Opcodes/ALOAD, 0)
			      (.visitFieldInsn mw Opcodes/GETFIELD, (qn-js-context ci),
			        (str "SCOPE_" parent), (sig-obj (qn-js-scope parent)))
	          (qn-js-scope parent))
	        ; must recur to parent scope
		      (do
;           (println (str " . Not found in parent scope " (ast-context-vars ((ast :contexts) parent))))
	          (recur (last parents) (butlast parents)))))))))

; asm boxing/unboxing

(defn asm-compile-autobox [expr ci ast mw]
  (if (isa? (first expr) :mug.ast/num-literal)
    (let [[_ ln value] expr]
      ;(println "Num literal optimization.")
      (.visitFieldInsn mw Opcodes/GETSTATIC, (qn-js-constants) (ident-num (index-of (ast-numbers ast) value)) (sig-obj "java/lang/Double")))
  (let [type (compile-type expr)]
	  (case type
	    :boolean (doto mw
	      (.visitTypeInsn Opcodes/NEW "java/lang/Boolean")
			  (.visitInsn Opcodes/DUP))
	    :number (doto mw
	      (.visitTypeInsn Opcodes/NEW "java/lang/Double")
			  (.visitInsn Opcodes/DUP))
      nil)
    (asm-compile expr ci ast mw)
	  (case type
	    :boolean (doto mw
				(.visitMethodInsn Opcodes/INVOKESPECIAL, "java/lang/Boolean", "<init>", (sig-call sig-boolean sig-void)))
	    :number (doto mw
				(.visitMethodInsn Opcodes/INVOKESPECIAL, "java/lang/Double", "<init>", (sig-call sig-double sig-void)))
      nil))))

; as-* conversion asm

(defn asm-compile-js-object [expr ci ast mw]
  (let [type (compile-type expr)
        type (if (class? type) (str type) type)]
	  (case type
	    :boolean (do
	      (.visitTypeInsn mw Opcodes/NEW qn-js-boolean)
			  (.visitInsn mw Opcodes/DUP)
        (asm-toplevel ci ast mw)
        (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-toplevel, "getBooleanPrototype", (sig-call (sig-obj qn-js-object)))
        (asm-compile expr ci ast mw)
        (.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-boolean, "<init>", (sig-call (sig-obj qn-js-object) sig-boolean sig-void)))
	    :number (do
	      (.visitTypeInsn mw Opcodes/NEW qn-js-number)
			  (.visitInsn mw Opcodes/DUP)
        (asm-toplevel ci ast mw)
        (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-toplevel, "getNumberPrototype", (sig-call (sig-obj qn-js-object)))
        (asm-compile expr ci ast mw)
        (.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-number, "<init>", (sig-call (sig-obj qn-js-object) sig-double sig-void)))
	    "class java.lang.String" (do
	      (.visitTypeInsn mw Opcodes/NEW qn-js-string)
			  (.visitInsn mw Opcodes/DUP)
        (asm-toplevel ci ast mw)
        (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-toplevel, "getStringPrototype", (sig-call (sig-obj qn-js-object)))
        (asm-compile expr ci ast mw)
        (.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-string, "<init>", (sig-call (sig-obj qn-js-object) (sig-obj qn-string) sig-void)))
	    (do
        (asm-toplevel ci ast mw)
	      (asm-compile expr ci ast mw)
        (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asJSObject", (sig-call (sig-obj qn-js-toplevel) (sig-obj qn-object) (sig-obj qn-js-object)))))))

(defn asm-as-number [type ci ast mw]
  (case type
    ; todo :boolean 
    :number nil
    ; todo String
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-object) sig-double))))

(defn asm-as-boolean [type ci ast mw]
  (case type
    :boolean nil
    ; todo :number
    ; todo String
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asBoolean", (sig-call (sig-obj qn-object) sig-boolean))))

; invoke arguments

(defn asm-invoke-args [args ci ast mw]
  ; arg count
  (.visitLdcInsn mw (new Integer (int (count args))))
  ; defined args
	(doseq [arg (subvec (vec args) 0 (min (count args) arg-limit))]
    (asm-compile-autobox arg ci ast mw))
  ; undefined args
	(doseq [_ (range (count args) arg-limit)]
    (if (= _ (count args))
      (.visitInsn mw Opcodes/ACONST_NULL)
      (.visitInsn mw Opcodes/DUP)))
  ; extra args
  (if (> (count args) arg-limit)
    (do
      (asm-compile-load-int (- (count args) arg-limit) mw)
      (.visitTypeInsn mw Opcodes/ANEWARRAY, qn-object)
      (doseq [i (range (- (count args) arg-limit))]
        (.visitInsn mw Opcodes/DUP)
        (asm-compile-load-int i mw)
        (asm-compile-autobox ((vec args) (+ arg-limit i)) ci ast mw)
        (.visitInsn mw Opcodes/AASTORE)))
    (.visitInsn mw Opcodes/ACONST_NULL)))

; comparison operations

(defn asm-compare-op [op left right ci ast mw]
  (let [false-case (new Label) true-case (new Label)]
    (asm-compile left ci ast mw)
    (asm-as-number (compile-type left) ci ast mw)
    (asm-compile right ci ast mw)
    (asm-as-number (compile-type right) ci ast mw)
    (doto mw
      (.visitInsn Opcodes/DCMPG)
      (.visitJumpInsn op, true-case)
      (.visitInsn Opcodes/ICONST_0)
      (.visitJumpInsn Opcodes/GOTO, false-case)
		  (.visitLabel true-case)
		  (.visitInsn Opcodes/ICONST_1)
		  (.visitLabel false-case))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; context compilation
;

;
; literals
;

(defmethod asm-compile :mug.ast/null-literal [[_ ln] ci ast mw]
  (.visitFieldInsn mw Opcodes/GETSTATIC, qn-js-null, "NULL", (sig-obj qn-js-null)))

(defmethod asm-compile :mug.ast/boolean-literal [[_ ln value] ci ast mw]
  (if value (.visitInsn mw Opcodes/ICONST_1) (.visitInsn mw Opcodes/ICONST_0))) 

(defmethod asm-compile :mug.ast/undef-literal [[_ ln] ci ast mw]
  (.visitInsn mw Opcodes/ACONST_NULL)) 

(defmethod asm-compile :mug.ast/num-literal [[_ ln value] ci ast mw]
  ; take advantage of AST representation of ints to see if we should decode
  ; long numbers like 0x88FFFF00 into (negative) ints or raw doubles
	(.visitLdcInsn mw (new Double (double
    (if (= (type value) java.lang.Long) (.intValue value) (double value))))))

(defmethod asm-compile :mug.ast/str-literal [[_ ln value] ci ast mw]
	(.visitLdcInsn mw value))

(defmethod asm-compile :mug.ast/regexp-literal [[_ ln expr flags] ci ast mw]
	(.visitTypeInsn mw Opcodes/NEW qn-js-regexp)
	(.visitInsn mw Opcodes/DUP)
  (asm-toplevel ci ast mw)
  (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-toplevel, "getRegExpPrototype", (sig-call (sig-obj qn-js-object)))
	(.visitFieldInsn mw Opcodes/GETSTATIC (qn-js-constants) (ident-regex (index-of (ast-regexps ast) [expr flags])) (sig-obj qn-pattern))
  (.visitLdcInsn mw flags)
	(.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "isPatternGlobal", (sig-call (sig-obj qn-string) sig-boolean))
	(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-regexp, "<init>", (sig-call (sig-obj qn-js-object) (sig-obj qn-pattern) sig-boolean sig-void)))

(defmethod asm-compile :mug.ast/array-literal [[_ ln exprs] ci ast mw]
	(.visitTypeInsn mw Opcodes/NEW qn-js-array)
	(.visitInsn mw Opcodes/DUP)
  (asm-toplevel ci ast mw)
  (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-toplevel, "getArrayPrototype", (sig-call (sig-obj qn-js-object)))
  (.visitLdcInsn mw (count exprs))
	(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-array, "<init>", (sig-call (sig-obj qn-js-object) sig-integer sig-void))
	(.visitInsn mw Opcodes/DUP)
  (asm-compile-load-int (count exprs) mw)
  (.visitTypeInsn mw Opcodes/ANEWARRAY, qn-object)
  (doseq [[i expr] (index exprs)]
    (.visitInsn mw Opcodes/DUP)
    (asm-compile-load-int i mw)
    (asm-compile-autobox expr ci ast mw)
    (.visitInsn mw Opcodes/AASTORE))
  (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-array, "load", (sig-call (sig-array (sig-obj qn-object)) sig-void)))

(defmethod asm-compile :mug.ast/obj-literal [[_ ln props] ci ast mw]
	(.visitTypeInsn mw Opcodes/NEW qn-js-object)
	(.visitInsn mw Opcodes/DUP)
  ; object prototype
  (asm-toplevel ci ast mw)
  (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-toplevel, "getObjectPrototype", (sig-call (sig-obj qn-js-object)))
	(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-object, "<init>", (sig-call (sig-obj qn-js-object) sig-void))
	(doseq [[k v] props]
    (.visitInsn mw Opcodes/DUP)
    (.visitLdcInsn mw k)
    (asm-compile-autobox v ci ast mw)
  	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "set", (sig-call (sig-obj qn-string) (sig-obj qn-object) sig-void))))

(defn child-context-index [closure ci ast]
  (let [parents (conj ((ast-context-hierarchy ast) ci) ci)]
    (index-of
      (map #(identity [%1 %2]) (ast-contexts ast) (ast-context-hierarchy ast))
      [closure parents])))

(defmethod asm-compile :mug.ast/func-literal [[_ ln closure] ci ast mw]
  (let [pci (child-context-index closure ci ast)
        qn (qn-js-context pci)]
    ; create context instance
    (.visitTypeInsn mw Opcodes/NEW, qn)
		(.visitInsn mw Opcodes/DUP)
    ; function prototype
    (asm-toplevel ci ast mw)
    (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-toplevel, "getFunctionPrototype", (sig-call (sig-obj qn-js-object)))
    ; load scopes
    (doseq [parent ((ast-context-hierarchy ast) pci)]
      (if (= parent ci)
        (.visitVarInsn mw Opcodes/ALOAD, scope-reg)
        (do
          (.visitVarInsn mw Opcodes/ALOAD, 0)
          (.visitFieldInsn mw Opcodes/GETFIELD, (qn-js-context ci), (str "SCOPE_" parent), (sig-obj (qn-js-scope parent))))))
		(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn, "<init>", (sig-context-init pci ast))))
  
;
; operations
;

;(defmethod asm-compile :mug.ast/post-inc-op-expr [node ci ast mw]
;  (asm-compile left ci ast mw)
;  (.visitInsn mw Opcodes/DUP)
;  (asm-compile right ci ast mw)
;  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "add", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-js-primitive) (sig-obj qn-js-primitive))))

(defmethod asm-compile :mug.ast/add-op-expr [[_ ln left right] ci ast mw]
  ;[TODO] optimize based on type
  (asm-compile-autobox left ci ast mw)
  (asm-compile-autobox right ci ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "add", (sig-call (sig-obj qn-object) (sig-obj qn-object) (sig-obj qn-object))))

(defmethod asm-compile :mug.ast/sub-op-expr [[_ ln left right] ci ast mw]
  (asm-compile left ci ast mw)
  (asm-as-number (compile-type left) ci ast mw)
  (asm-compile right ci ast mw)
  (asm-as-number (compile-type right) ci ast mw)
  (.visitInsn mw Opcodes/DSUB))

(defmethod asm-compile :mug.ast/div-op-expr [[_ ln left right] ci ast mw]
  (asm-compile left ci ast mw)
  (asm-as-number (compile-type left) ci ast mw)
  (asm-compile right ci ast mw)
  (asm-as-number (compile-type right) ci ast mw)
  (.visitInsn mw Opcodes/DDIV))

(defmethod asm-compile :mug.ast/mul-op-expr [[_ ln left right] ci ast mw]
  (asm-compile left ci ast mw)
  (asm-as-number (compile-type left) ci ast mw)
  (asm-compile right ci ast mw)
  (asm-as-number (compile-type right) ci ast mw)
  (.visitInsn mw Opcodes/DMUL))

(defmethod asm-compile :mug.ast/mod-op-expr [[_ ln left right] ci ast mw]
  (asm-compile left ci ast mw)
  (asm-as-number (compile-type left) ci ast mw)
  (asm-compile right ci ast mw)
  (asm-as-number (compile-type right) ci ast mw)
  (.visitInsn mw Opcodes/DREM))

(defmethod asm-compile :mug.ast/lsh-op-expr [[_ ln left right] ci ast mw]
  (asm-compile left ci ast mw)
  (asm-as-number (compile-type left) ci ast mw)
  (.visitInsn mw Opcodes/D2I)
  (asm-compile right ci ast mw)
  (asm-as-number (compile-type right) ci ast mw)
  (.visitInsn mw Opcodes/D2I)
  (.visitInsn mw Opcodes/ISHL)
  (.visitInsn mw Opcodes/I2D))

(defmethod asm-compile :mug.ast/eq-op-expr [[_ ln left right] ci ast mw]
  (asm-compile-autobox left ci ast mw)
  (asm-compile-autobox right ci ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "testEquality", (sig-call (sig-obj qn-object) (sig-obj qn-object) sig-boolean)))

(defmethod asm-compile :mug.ast/neq-op-expr [[_ ln left right] ci ast mw]
  (asm-compile-autobox left ci ast mw)
  (asm-compile-autobox right ci ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "testEquality", (sig-call (sig-obj qn-object) (sig-obj qn-object) sig-boolean))
  (let [false-case (new Label) true-case (new Label)]
    (doto mw
      (.visitJumpInsn Opcodes/IFNE, false-case)
      (.visitInsn Opcodes/ICONST_1)
      (.visitJumpInsn Opcodes/GOTO, true-case)
		  (.visitLabel false-case)
		  (.visitInsn Opcodes/ICONST_0)
		  (.visitLabel true-case))))

(defmethod asm-compile :mug.ast/not-op-expr [[_ ln expr] ci ast mw]
  (let [false-case (new Label) true-case (new Label)]
    (asm-compile expr ci ast mw)
    (asm-as-boolean (compile-type expr) ci ast mw)
    (doto mw
      (.visitJumpInsn Opcodes/IFNE, false-case)
      (.visitInsn Opcodes/ICONST_1)
      (.visitJumpInsn Opcodes/GOTO, true-case)
		  (.visitLabel false-case)
		  (.visitInsn Opcodes/ICONST_0)
		  (.visitLabel true-case))))

(defmethod asm-compile :mug.ast/eqs-op-expr [[_ ln left right] ci ast mw]
  (asm-compile-autobox left ci ast mw)
  (asm-compile-autobox right ci ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "testStrictEquality", (sig-call (sig-obj qn-object) (sig-obj qn-object) sig-boolean)))

(defmethod asm-compile :mug.ast/neqs-op-expr [[_ ln left right] ci ast mw]
  (asm-compile-autobox left ci ast mw)
  (asm-compile-autobox right ci ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "testStrictEquality", (sig-call (sig-obj qn-object) (sig-obj qn-object) sig-boolean))
  (let [false-case (new Label) true-case (new Label)]
    (doto mw
      (.visitJumpInsn Opcodes/IFNE, false-case)
      (.visitInsn Opcodes/ICONST_1)
      (.visitJumpInsn Opcodes/GOTO, true-case)
		  (.visitLabel false-case)
		  (.visitInsn Opcodes/ICONST_0)
		  (.visitLabel true-case))))
  
(defmethod asm-compile :mug.ast/lt-op-expr [[_ ln left right] ci ast mw]
  (asm-compare-op Opcodes/IFLT left right ci ast mw))

(defmethod asm-compile :mug.ast/lte-op-expr [[_ ln left right] ci ast mw]
  (asm-compare-op Opcodes/IFLE left right ci ast mw))

(defmethod asm-compile :mug.ast/gt-op-expr [[_ ln left right] ci ast mw]
  (asm-compare-op Opcodes/IFGT left right ci ast mw))

(defmethod asm-compile :mug.ast/gte-op-expr [[_ ln left right] ci ast mw]
  (asm-compare-op Opcodes/IFGE left right ci ast mw))

(defmethod asm-compile :mug.ast/neg-op-expr [[_ ln expr] ci ast mw]
  (asm-compile-autobox expr ci ast mw) ;(asm-compile expr ci ast mw)
  (asm-as-number Object ci ast mw) ;(asm-as-number (compile-type expr) ci ast mw)
  (.visitInsn mw Opcodes/DNEG))

(defmethod asm-compile :mug.ast/or-op-expr [[_ ln left right] ci ast mw]
  (asm-compile-autobox left ci ast mw) ;(asm-compile left ci ast mw)
  (.visitInsn mw Opcodes/DUP) ;TODO without this DUP, we could post-facto autobox
  (asm-as-boolean Object ci ast mw) ;(asm-as-boolean (compile-type left) ci ast mw)
  (let [true-case (new Label)]
    (.visitJumpInsn mw, Opcodes/IFNE, true-case)
    (.visitInsn mw Opcodes/POP)
    (asm-compile-autobox right ci ast mw)
    (.visitLabel mw true-case)))

(defmethod asm-compile :mug.ast/and-op-expr [[_ ln left right] ci ast mw]
  (asm-compile-autobox left ci ast mw) ;(asm-compile left ci ast mw)
  (.visitInsn mw Opcodes/DUP) ;TODO without this DUP, we could post-facto autobox
  (asm-as-boolean Object ci ast mw) ;(asm-as-boolean (compile-type left) ci ast mw)
  (let [false-case (new Label)]
    (.visitJumpInsn mw, Opcodes/IFEQ, false-case)
    (.visitInsn mw Opcodes/POP)
    (asm-compile-autobox right ci ast mw)
    (.visitLabel mw false-case)))

;
; expressions
;

(defmethod asm-compile :mug.ast/scope-ref-expr [[_ ln value] ci ast mw]
  (if-let [reg (ref-reg ((ast-contexts ast) ci) value)]
    (.visitVarInsn mw Opcodes/ALOAD reg) 
    (let [qn-parent (asm-search-scopes value ci ast mw)]
      (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-parent, (str "get_" value), (sig-call (sig-obj qn-object))))))

(defmethod asm-compile :mug.ast/static-ref-expr [[_ ln base value] ci ast mw]
  (asm-compile-js-object base ci ast mw)
	(doto mw
    (.visitLdcInsn value)
		(.visitMethodInsn Opcodes/INVOKEVIRTUAL, qn-js-object, "get", (sig-call (sig-obj qn-string) (sig-obj qn-object)))))

(defmethod asm-compile :mug.ast/dyn-ref-expr [[_ ln base index] ci ast mw]
  (asm-compile-js-object base ci ast mw)
  (case (compile-type index)
    ;TODO not valid, only if number is integer
    :number (do
		  (asm-compile index ci ast mw)
      (.visitInsn mw Opcodes/D2I)
			(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "get", (sig-call sig-integer (sig-obj qn-object))))  
    (do 
		  (asm-compile-autobox index ci ast mw)
			(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "get", (sig-call (sig-obj qn-object) (sig-obj qn-object))))))

(defmethod asm-compile :mug.ast/static-method-call-expr [[_ ln base value args] ci ast mw]
  ; get argument and method
  (asm-compile-js-object base ci ast mw)
	(doto mw
    (.visitInsn Opcodes/DUP)
    (.visitLdcInsn value)
		(.visitMethodInsn Opcodes/INVOKEVIRTUAL, qn-js-object, "get", (sig-call (sig-obj qn-string) (sig-obj qn-object)))
	  (.visitTypeInsn Opcodes/CHECKCAST, qn-js-object)
    (.visitInsn Opcodes/SWAP))
  (asm-invoke-args args ci ast mw)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "invoke", sig-invoke))

(defmethod asm-compile :mug.ast/call-expr [[_ ln expr args] ci ast mw]
  (asm-compile expr ci ast mw)
	(.visitTypeInsn mw, Opcodes/CHECKCAST, qn-js-object)
	(.visitInsn mw Opcodes/ACONST_NULL) ; "this"
  (asm-invoke-args args ci ast mw)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "invoke", sig-invoke))

(defmethod asm-compile :mug.ast/new-expr [[_ ln constructor args] ci ast mw]
  (asm-compile constructor ci ast mw)
	(.visitTypeInsn mw, Opcodes/CHECKCAST, qn-js-object)
  (asm-invoke-args args ci ast mw)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "instantiate", sig-instantiate))

;TODO
; so the big problem with these assigns
; is that they autobox then duplicate
; when really they should be preserving the type
; of whatever is put into them, and autoboxing the
; duplicate. this may be solved by having the
; set_ functions return their values.
; anyway, fix this,
; and adjust (compile-type) accordingly

(defmethod asm-compile :mug.ast/scope-assign-expr [[_ ln value expr] ci ast mw]
  (asm-compile-autobox expr ci ast mw)
  (.visitInsn mw Opcodes/DUP)
  (if-let [reg (ref-reg ((ast-contexts ast) ci) value)]
    (.visitVarInsn mw Opcodes/ASTORE reg) 
	  (let [qn-parent (asm-search-scopes value ci ast mw)]
	    (.visitInsn mw Opcodes/SWAP)
		  (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-parent, (str "set_" value), (sig-call (sig-obj qn-object) sig-void)))))

(defmethod asm-compile :mug.ast/static-assign-expr [[_ ln base value expr] ci ast mw]
  (asm-compile-js-object base ci ast mw)
  (.visitLdcInsn mw value)
	(asm-compile-autobox expr ci ast mw)
  (.visitInsn mw Opcodes/DUP_X2)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "set", (sig-call (sig-obj qn-string) (sig-obj qn-object) sig-void)))

(defmethod asm-compile :mug.ast/dyn-assign-expr [[_ ln base index expr] ci ast mw]
;  (.visitInsn mw Opcodes/DUP)
  (asm-compile-js-object base ci ast mw)
;  (.visitInsn mw Opcodes/SWAP)
  (case (compile-type index)
    ;TODO not valid, only if number is integer
    :number (do
		  (asm-compile index ci ast mw)
      (.visitInsn mw Opcodes/D2I)
      (asm-compile-autobox expr ci ast mw)
		  (.visitInsn mw Opcodes/DUP_X2)
			(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "set", (sig-call sig-integer (sig-obj qn-object) sig-void)))
    (do 
		  (asm-compile-autobox index ci ast mw)
      (asm-compile-autobox expr ci ast mw)
		  (.visitInsn mw Opcodes/DUP_X2)
      (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "set", (sig-call (sig-obj qn-object) (sig-obj qn-object) sig-void)))))

(defmethod asm-compile :mug.ast/typeof-expr [[_ ln expr] ci ast mw]
  (asm-compile expr ci ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "typeof", (sig-call (sig-obj qn-object) (sig-obj qn-string))))
    
(defmethod asm-compile :mug.ast/this-expr [[_ ln] ci ast mw]
  (.visitVarInsn mw Opcodes/ALOAD, 1))

(defmethod asm-compile :mug.ast/if-expr [[_ ln expr then-expr else-expr] ci ast mw]
  (asm-compile expr ci ast mw)
  (asm-as-boolean (compile-type expr) ci ast mw)
  (let [false-case (new Label) true-case (new Label)]
    (.visitJumpInsn mw, Opcodes/IFEQ, false-case)
    (asm-compile-autobox then-expr ci ast mw)
    (.visitJumpInsn mw, Opcodes/GOTO, true-case)
    (.visitLabel mw false-case)
    (asm-compile-autobox else-expr ci ast mw)
    (.visitLabel mw true-case)))

(defmethod asm-compile :mug.ast/seq-expr [[_ ln pre expr] ci ast mw]
  (asm-compile pre ci ast mw)
  (asm-compile-pop pre mw)
  (asm-compile expr ci ast mw))

;
; statements
;

(defmethod asm-compile :mug.ast/block-stat [[_ ln stats] ci ast mw]
  (doseq [stat stats]
    (asm-compile stat ci ast mw)))

(defmethod asm-compile :mug.ast/expr-stat [[_ ln expr] ci ast mw]
	(asm-compile expr ci ast mw)
  (asm-compile-pop expr mw))

(defmethod asm-compile :mug.ast/ret-stat [[_ ln expr] ci ast mw]
  (if (nil? expr)
    (.visitInsn mw Opcodes/ACONST_NULL)
    (asm-compile-autobox expr ci ast mw))
  (.visitInsn mw Opcodes/ARETURN))

(defmethod asm-compile :mug.ast/while-stat [[_ ln expr stat] ci ast mw]
  (let [true-case (new Label) false-case (new Label)]
    (push-label nil true-case false-case)
    
    (.visitLabel mw true-case)
    (asm-compile expr ci ast mw)
    (asm-as-boolean (compile-type expr) ci ast mw)
    (.visitJumpInsn mw, Opcodes/IFEQ, false-case)
    (when stat
      (asm-compile stat ci ast mw))
    (.visitJumpInsn mw, Opcodes/GOTO, true-case)
    (.visitLabel mw false-case)
    
    (pop-label nil)))

(defmethod asm-compile :mug.ast/do-while-stat [[_ ln expr stat] ci ast mw]
  (let [true-case (new Label) false-case (new Label)]
    (push-label nil true-case false-case)
    
    (.visitLabel mw true-case)
    (asm-compile stat ci ast mw)
    (asm-compile expr ci ast mw)
    (asm-as-boolean (compile-type expr) ci ast mw)
    (.visitJumpInsn mw, Opcodes/IFNE, true-case)
    (.visitLabel mw false-case)
    
    (pop-label nil)))

(defmethod asm-compile :mug.ast/for-stat [[_ ln init expr step stat] ci ast mw]
  (when init
    (asm-compile init ci ast mw)
    (when (isa? (first init) :mug.ast/expr)
      (asm-compile-pop init mw)))

  (let [start-label (new Label) continue-label (new Label) break-label (new Label)]
    (push-label nil continue-label break-label)
    
    (.visitLabel mw start-label)
    (when expr
      (asm-compile expr ci ast mw)
      (asm-as-boolean (compile-type expr) ci ast mw)
      (.visitJumpInsn mw, Opcodes/IFEQ, break-label))
    (when stat
      (asm-compile stat ci ast mw))
    (.visitLabel mw continue-label)
    (when step
      (asm-compile step ci ast mw)
      (asm-compile-pop step mw))
    (.visitJumpInsn mw, Opcodes/GOTO, start-label)
    (.visitLabel mw break-label)
    
    (pop-label nil)))

(defmethod asm-compile :mug.ast/if-stat [[_ ln expr then-stat else-stat] ci ast mw]
  (asm-compile expr ci ast mw)
  (asm-as-boolean (compile-type expr) ci ast mw)
  (let [false-case (new Label) true-case (new Label)]
    (.visitJumpInsn mw, Opcodes/IFEQ, false-case)
    (asm-compile then-stat ci ast mw)
    (.visitJumpInsn mw, Opcodes/GOTO, true-case)
    (.visitLabel mw false-case)
    (when (not (nil? else-stat))
      (asm-compile else-stat ci ast mw))
    (.visitLabel mw true-case)))

(defmethod asm-compile :mug.ast/switch-stat [[_ ln expr cases] ci ast mw]
  (let [end-case (new Label)
        labels (vec (map (fn [x] (new Label)) cases))]
    (push-label nil nil end-case)
    
    (asm-compile-autobox expr ci ast mw)
    ; cases
    (doseq [[i [expr stats]] (index cases)]
      (when (not (nil? expr))
        (.visitInsn mw Opcodes/DUP)
        (asm-compile-autobox expr ci ast mw)
        (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "testEquality", (sig-call (sig-obj qn-object) (sig-obj qn-object) sig-boolean))
        (.visitJumpInsn mw, Opcodes/IFNE, (labels i))))
    ; default
    (doseq [[i [expr stats]] (index cases)]
      (when (nil? expr)
        (.visitJumpInsn mw, Opcodes/GOTO, (labels i))))
    (.visitJumpInsn mw, Opcodes/GOTO, end-case)
    
    ; clauses
    (doseq [[i [expr stats]] (index cases)]
      (.visitLabel mw (labels i))
      (doseq [stat stats]
        (asm-compile stat ci ast mw)))
    
    (.visitLabel mw end-case)
    (.visitInsn mw Opcodes/POP) ; original switch expression
    
    (pop-label nil)))

(defmethod asm-compile :mug.ast/throw-stat [[_ ln expr] ci ast mw]
  (.visitTypeInsn mw Opcodes/NEW, qn-js-exception)
  (.visitInsn mw Opcodes/DUP)
  (asm-compile-autobox expr ci ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-exception, "<init>", (sig-call (sig-obj qn-object) sig-void))
  (.visitInsn mw Opcodes/ATHROW))

(defmethod asm-compile :mug.ast/try-stat [[_ ln stats catch-block finally-stats] ci ast mw]
  (let [try-label (new Label)
        catch-label (new Label)
        finally-label (new Label)
        double-throw-label (new Label)
        end-label (new Label)]
    
    ; catch block
    (.visitTryCatchBlock mw try-label, catch-label, catch-label, "java/lang/Exception")
    ; finally block
    (.visitTryCatchBlock mw try-label, finally-label, double-throw-label, nil)
    
    ; try
    (.visitLabel mw try-label)
    (doseq [stat stats]
      (asm-compile stat ci ast mw))
    (.visitJumpInsn mw Opcodes/GOTO finally-label)
    
    ; catch (may be empty)
    (.visitLabel mw catch-label)
    (when-let [[value stats] catch-block]
      ; unwrap js exceptions
      (.visitInsn mw Opcodes/DUP)
      (.visitTypeInsn mw Opcodes/INSTANCEOF, qn-js-exception)
      (let [cast-label (new Label)]
        (.visitJumpInsn mw Opcodes/IFEQ cast-label)
        (.visitTypeInsn mw Opcodes/CHECKCAST, qn-js-exception)
        (.visitFieldInsn mw Opcodes/GETFIELD, qn-js-exception, "value", (sig-obj qn-object))
        (.visitLabel mw cast-label))
      ; assign variable
		  (if-let [reg (ref-reg ((ast-contexts ast) ci) value)]
		    (.visitVarInsn mw Opcodes/ASTORE reg) 
			  (let [qn-parent (asm-search-scopes value ci ast mw)]
			    (.visitInsn mw Opcodes/SWAP)
				  (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-parent, (str "set_" value), (sig-call (sig-obj qn-object) sig-void))))
      ; body
	    (doseq [stat stats]
	      (asm-compile stat ci ast mw)))

    ; finally (may be empty)
    (.visitLabel mw finally-label)
    (when finally-stats
	    (doseq [stat finally-stats]
	      (asm-compile stat ci ast mw)))
    (.visitJumpInsn mw Opcodes/GOTO, end-label)
    
    ; throw inside catch (repeats finally body)
    (.visitLabel mw double-throw-label)
    (when finally-stats
	    (doseq [stat finally-stats]
	      (asm-compile stat ci ast mw)))
    (.visitInsn mw Opcodes/ATHROW)

    (.visitLabel mw end-label)))

(defmethod asm-compile :mug.ast/break-stat [[_ ln label] ci ast mw]
  (if (> (count (get-label nil)) 0)
    (.visitJumpInsn mw, Opcodes/GOTO, ((get-label nil) :break))
    (throw (new Exception "Cannot break outside of loop"))))

(defmethod asm-compile :mug.ast/continue-stat [[_ ln label] ci ast mw]
  (if (> (count (get-label nil)) 0)
    (.visitJumpInsn mw, Opcodes/GOTO, ((get-label nil) :continue))
    (throw (new Exception "Cannot continue outside of loop"))))

(defmethod asm-compile :mug.ast/var-stat [[_ ln vars] ci ast mw]
  (doseq [[value expr] (filter #(not (nil? (second %))) vars)]
	  (asm-compile-autobox expr ci ast mw)
	  (if-let [reg (ref-reg ((ast-contexts ast) ci) value)]
	    (.visitVarInsn mw Opcodes/ASTORE reg) 
		  (let [qn-parent (asm-search-scopes value ci ast mw)]
		    (.visitInsn mw Opcodes/SWAP)
	      (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-parent, (str "set_" value), (sig-call (sig-obj qn-object) sig-void))))))

(defmethod asm-compile :mug.ast/defn-stat [[_ ln closure] ci ast mw]
  (let [[_ ln name args stats] closure]
    (asm-compile (expr-stat ln (scope-assign-expr ln name (func-literal ln closure))) ci ast mw)))

(defmethod asm-compile :mug.ast/for-in-stat [[_ ln isvar value expr stat] ci ast mw]
	(let [check-label (new Label) stat-label (new Label)]
    (push-label nil stat-label check-label)
   
    (asm-compile-js-object expr ci ast mw)
		(.visitMethodInsn mw, Opcodes/INVOKEVIRTUAL, "mug/js/JSObject", "getKeys", "()[Ljava/lang/String;")
  	(.visitInsn mw, Opcodes/ICONST_0)
		(.visitJumpInsn mw, Opcodes/GOTO, check-label)
		(.visitLabel mw stat-label)
      ; load from array
      (.visitInsn mw, Opcodes/DUP2)
			(.visitInsn mw, Opcodes/AALOAD)
      ; store in scope
		  (if-let [reg (ref-reg ((ast-contexts ast) ci) value)]
		    (.visitVarInsn mw Opcodes/ASTORE reg) 
	      (let [qn-parent (asm-search-scopes value ci ast mw)]
	        (.visitInsn mw Opcodes/SWAP)
		      (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-parent, (str "set_" value), (sig-call (sig-obj qn-object) sig-void))))
	
			(asm-compile stat ci ast mw)
   
			(.visitInsn mw, Opcodes/ICONST_1)
			(.visitInsn mw, Opcodes/IADD)
		(.visitLabel mw, check-label)
			(.visitInsn mw, Opcodes/DUP2)
			(.visitInsn mw, Opcodes/SWAP)
			(.visitInsn mw, Opcodes/ARRAYLENGTH)
			(.visitJumpInsn mw, Opcodes/IF_ICMPLT, stat-label)
      (.visitInsn mw, Opcodes/POP2)
      
      (pop-label nil)))