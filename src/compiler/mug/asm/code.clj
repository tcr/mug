(ns mug.asm.code
  (:use mug.asm.util))

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

(defmulti compile-code (fn [node context ast mw] (first node)))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; asm utilities
;

; top-level asm

(defn asm-toplevel [context ast mw]
  (if (= (context-index context ast) 0)
    (.visitVarInsn mw Opcodes/ALOAD, scope-reg)
    (do
      (.visitVarInsn mw Opcodes/ALOAD, 0)
      (.visitFieldInsn mw Opcodes/GETFIELD, (qn-js-context (index-of (ast :contexts) context)),
	      (str "SCOPE_" 0), (sig-obj (qn-js-scope 0)))
      (.visitTypeInsn mw Opcodes/CHECKCAST, qn-js-toplevel))))

; to-object asm 

(defn asm-to-object [context ast mw]
  (asm-toplevel context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-primitive, "toObject", (sig-call (sig-obj qn-js-toplevel) (sig-obj qn-js-object))))

; search scopes
; asm loads scope object, fn returns qn of scope object

(defn asm-search-scopes [name parents context ast mw]
;  (println (str "Seeking var \"" name "\" in current scope (" (context-scope-vars context) ")"))
  (if (contains? (context-scope-vars context) name)
    ; variable found in current scope
    (do
      (.visitVarInsn mw Opcodes/ALOAD, scope-reg)
      (qn-js-scope (context-index context ast)))
  (loop [parent (last parents) parents (butlast parents)]
	  (if (nil? parent)
	    (if (contains? script-default-vars name)
        ; found variable in global scope
	      (do
;	        (println (str " . Found in global scope: " name))
          (if (= (context-index context ast) 0) ; script-context scope inherits from globals
            (.visitVarInsn mw Opcodes/ALOAD, scope-reg)
            (do
		          (.visitVarInsn mw Opcodes/ALOAD, 0)
		          (.visitFieldInsn mw Opcodes/GETFIELD, (qn-js-context (index-of (ast :contexts) context)),
		            (str "SCOPE_" 0), (sig-obj (qn-js-scope 0)))))
          qn-js-toplevel)
        ; identifier not found at all
	      (throw (new Exception (str "Identifier not defined in any scope: " name))))
		  (if (contains? (context-scope-vars ((ast :contexts) parent)) name)
        ; found variable in ancestor scope
		    (do
;		      (println (str " . Found in higher scope: " name))
		      (.visitVarInsn mw Opcodes/ALOAD, 0)
		      (.visitFieldInsn mw Opcodes/GETFIELD, (qn-js-context (index-of (ast :contexts) context)),
		        (str "SCOPE_" parent), (sig-obj (qn-js-scope parent)))
          (qn-js-scope parent))
        ; must recur to parent scope
	      (do
;          (println (str " . Not found in parent scope " (context-scope-vars ((ast :contexts) parent))))
          (recur (last parents) (butlast parents))))))))

; invoke arguments

(defn asm-invoke-args [args context ast mw]
  ; arg count
  (.visitLdcInsn mw (new Integer (int (count args))))
  ; defined args
	(doseq [arg (subvec (vec args) 0 (min (count args) arg-limit))]
		(compile-code arg context ast mw))
  ; undefined args
	(doseq [_ (range (count args) arg-limit)]
    (if (= _ (count args))
          (.visitInsn mw Opcodes/ACONST_NULL)
          (.visitInsn mw Opcodes/DUP)))
  ; extra args
  (if (> (count args) arg-limit)
    (do
      (.visitIntInsn mw Opcodes/BIPUSH, (- (count args) arg-limit))
      (.visitTypeInsn mw Opcodes/ANEWARRAY, qn-js-primitive)
      (doseq [_ (range (- (count args) arg-limit))]
        (.visitInsn mw Opcodes/DUP)
        (.visitIntInsn mw Opcodes/BIPUSH, _)
        (compile-code ((vec args) (+ arg-limit _)) context ast mw)
        (.visitInsn mw Opcodes/AASTORE)))
    (.visitInsn mw Opcodes/ACONST_NULL)))

; comparison operations

(defn asm-compare-op [op left right context ast mw]
  (let [false-case (new Label) true-case (new Label)]
    (compile-code left context ast mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
    (compile-code right context ast mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
    (doto mw
      (.visitInsn Opcodes/DCMPG)
      (.visitJumpInsn op, true-case)
      (.visitFieldInsn Opcodes/GETSTATIC, qn-js-atoms, "FALSE", (sig-obj qn-js-boolean))
      (.visitJumpInsn Opcodes/GOTO, false-case)
		  (.visitLabel true-case)
		  (.visitFieldInsn Opcodes/GETSTATIC, qn-js-atoms, "TRUE", (sig-obj qn-js-boolean))
		  (.visitLabel false-case))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; context compilation
;

;
; literals
;

(defmethod compile-code :mug.ast/null-literal [[_] context ast mw]
  (.visitFieldInsn mw Opcodes/GETSTATIC, qn-js-atoms, "NULL", (sig-obj qn-js-null)))

(defmethod compile-code :mug.ast/boolean-literal [[_ value] context ast mw]
	(.visitFieldInsn mw Opcodes/GETSTATIC, qn-js-atoms, (if value "TRUE" "FALSE"), (sig-obj qn-js-boolean)))

(defmethod compile-code :mug.ast/num-literal [[_ value] context ast mw]
	(.visitFieldInsn mw Opcodes/GETSTATIC, (qn-js-constants) (ident-num (index-of (ast :numbers) value)) (sig-obj qn-js-number)))

(defmethod compile-code :mug.ast/str-literal [[_ value] context ast mw]
	(.visitFieldInsn mw Opcodes/GETSTATIC (qn-js-constants) (ident-str (index-of (ast :strings) value)) (sig-obj qn-js-string)))

(defmethod compile-code :mug.ast/regex-literal [[_ expr flags] context ast mw]
	(.visitTypeInsn mw Opcodes/NEW qn-js-regexp)
	(.visitInsn mw Opcodes/DUP)
  (asm-toplevel context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-toplevel, "getRegExpPrototype", (sig-call (sig-obj qn-js-object)))
	(.visitFieldInsn mw Opcodes/GETSTATIC (qn-js-constants) (ident-regex (index-of (ast :regexes) [expr flags])) (sig-obj qn-pattern))
  (.visitLdcInsn mw flags)
	(.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "isPatternGlobal", (sig-call (sig-obj qn-string) sig-boolean))
	(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-regexp, "<init>", (sig-call (sig-obj qn-js-object) (sig-obj qn-pattern) sig-boolean sig-void)))

(defmethod compile-code :mug.ast/array-literal [[_ exprs] context ast mw]
	(.visitTypeInsn mw Opcodes/NEW qn-js-array)
	(.visitInsn mw Opcodes/DUP)
  (asm-toplevel context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-toplevel, "getArrayPrototype", (sig-call (sig-obj qn-js-object)))
  (.visitLdcInsn mw (count exprs))
	(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-array, "<init>", (sig-call (sig-obj qn-js-object) sig-integer sig-void))
	(.visitInsn mw Opcodes/DUP)
  (.visitIntInsn mw Opcodes/BIPUSH, (count exprs))
  (.visitTypeInsn mw Opcodes/ANEWARRAY, qn-js-primitive)
  (doseq [[i expr] (index exprs)]
    (.visitInsn mw Opcodes/DUP)
    (.visitIntInsn mw Opcodes/BIPUSH, i)
    (compile-code expr context ast mw)
    (.visitInsn mw Opcodes/AASTORE))
  (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-array, "load", (sig-call (sig-array (sig-obj qn-js-primitive)) sig-void)))

(defmethod compile-code :mug.ast/obj-literal [[_ props] context ast mw]
	(.visitTypeInsn mw Opcodes/NEW qn-js-object)
	(.visitInsn mw Opcodes/DUP)
  ; object prototype
  (asm-toplevel context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-toplevel, "getObjectPrototype", (sig-call (sig-obj qn-js-object)))
	(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-object, "<init>", (sig-call (sig-obj qn-js-object) sig-void))
	(doseq [[k v] props]
    (.visitInsn mw Opcodes/DUP)
    (.visitLdcInsn mw k)
    (compile-code v context ast mw)
  	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "set", (sig-call (sig-obj qn-string) (sig-obj qn-js-primitive) sig-void))))

(defmethod compile-code :mug.ast/func-literal [[_ closure] context ast mw]
  (let [qn (qn-js-context closure)
        closure ((ast :contexts) closure)]
    ; create context instance``
    (.visitTypeInsn mw Opcodes/NEW, qn)
		(.visitInsn mw Opcodes/DUP)
    ; function prototype
    (asm-toplevel context ast mw)
    (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-toplevel, "getFunctionPrototype", (sig-call (sig-obj qn-js-object)))
    ; load scopes
    (doseq [parent (context-parents closure)]
      (if (= ((ast :contexts) parent) context)
        (.visitVarInsn mw Opcodes/ALOAD, scope-reg)
        (do
          (.visitVarInsn mw Opcodes/ALOAD, 0)
          (.visitFieldInsn mw Opcodes/GETFIELD, (qn-js-context (context-index context ast)), (str "SCOPE_" parent), (sig-obj (qn-js-scope parent))))))
		(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn, "<init>", (sig-context-init closure ast))))
  
;
; operations
;

;(defmethod compile-code :mug.ast/post-inc-op-expr [node context ast mw]
;  (compile-code left context ast mw)
;  (.visitInsn mw Opcodes/DUP)
;  (compile-code right context ast mw)
;  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "add", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-js-primitive) (sig-obj qn-js-primitive))))

(defmethod compile-code :mug.ast/add-op-expr [[_ left right] context ast mw]
  (compile-code left context ast mw)
  (compile-code right context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "add", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-js-primitive) (sig-obj qn-js-primitive))))

(defmethod compile-code :mug.ast/sub-op-expr [[_ left right] context ast mw]
  (.visitTypeInsn mw Opcodes/NEW, qn-js-number)
  (.visitInsn mw Opcodes/DUP)
  (compile-code left context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
  (compile-code right context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
  (.visitInsn mw Opcodes/DSUB)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-number, "<init>", (sig-call sig-double sig-void)))

(defmethod compile-code :mug.ast/div-op-expr [[_ left right] context ast mw]
  (.visitTypeInsn mw Opcodes/NEW, qn-js-number)
  (.visitInsn mw Opcodes/DUP)
  (compile-code left context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
  (compile-code right context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
  (.visitInsn mw Opcodes/DDIV)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-number, "<init>", (sig-call sig-double sig-void)))

(defmethod compile-code :mug.ast/mul-op-expr [[_ left right] context ast mw]
  (.visitTypeInsn mw Opcodes/NEW, qn-js-number)
  (.visitInsn mw Opcodes/DUP)
  (compile-code left context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
  (compile-code right context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
  (.visitInsn mw Opcodes/DMUL)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-number, "<init>", (sig-call sig-double sig-void)))

(defmethod compile-code :mug.ast/lsh-op-expr [[_ left right] context ast mw]
  (.visitTypeInsn mw Opcodes/NEW, qn-js-number)
  (.visitInsn mw Opcodes/DUP)
  (compile-code left context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
  (.visitInsn mw Opcodes/D2I)
  (compile-code right context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
  (.visitInsn mw Opcodes/D2I)
  (.visitInsn mw Opcodes/ISHL)
  (.visitInsn mw Opcodes/I2D)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-number, "<init>", (sig-call sig-double sig-void)))

(defmethod compile-code :mug.ast/eq-op-expr [[_ left right] context ast mw]
  (compile-code left context ast mw)
  (compile-code right context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "testEquality", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-js-primitive) (sig-obj qn-js-boolean))))

(defmethod compile-code :mug.ast/neq-op-expr [[_ left right] context ast mw]
  (compile-code left context ast mw)
  (compile-code right context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "testInequality", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-js-primitive) (sig-obj qn-js-boolean))))

(defmethod compile-code :mug.ast/not-op-expr [[_ expr] context ast mw]
  (let [false-case (new Label) true-case (new Label)]
    (compile-code expr context ast mw)
    (doto mw
      (.visitMethodInsn Opcodes/INVOKESTATIC, qn-js-utils, "asBoolean", (sig-call (sig-obj qn-js-primitive) sig-boolean))
      (.visitJumpInsn Opcodes/IFNE, false-case)
      (.visitFieldInsn Opcodes/GETSTATIC, qn-js-atoms, "TRUE", (sig-obj qn-js-boolean))
      (.visitJumpInsn Opcodes/GOTO, true-case)
		  (.visitLabel false-case)
		  (.visitFieldInsn Opcodes/GETSTATIC, qn-js-atoms, "FALSE", (sig-obj qn-js-boolean))
		  (.visitLabel true-case))))

(defmethod compile-code :mug.ast/eqs-op-expr [[_ left right] context ast mw]
  (compile-code left context ast mw)
  (compile-code right context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "testStrictEquality", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-js-primitive) (sig-obj qn-js-boolean))))

(defmethod compile-code :mug.ast/neqs-op-expr [[_ left right] context ast mw]
  (compile-code left context ast mw)
  (compile-code right context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "testStrictInequality", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-js-primitive) (sig-obj qn-js-boolean))))
  
(defmethod compile-code :mug.ast/lt-op-expr [[_ left right] context ast mw]
  (asm-compare-op Opcodes/IFLT left right context ast mw))

(defmethod compile-code :mug.ast/lte-op-expr [[_ left right] context ast mw]
  (asm-compare-op Opcodes/IFLE left right context ast mw))

(defmethod compile-code :mug.ast/gt-op-expr [[_ left right] context ast mw]
  (asm-compare-op Opcodes/IFGT left right context ast mw))

(defmethod compile-code :mug.ast/gte-op-expr [[_ left right] context ast mw]
  (asm-compare-op Opcodes/IFGE left right context ast mw))

(defmethod compile-code :mug.ast/neg-op-expr [[_ expr] context ast mw]
  (.visitTypeInsn mw Opcodes/NEW, qn-js-number)
  (.visitInsn mw Opcodes/DUP)
  (compile-code expr context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
  (.visitInsn mw Opcodes/DNEG)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-number, "<init>", (sig-call sig-double sig-void)))

(defmethod compile-code :mug.ast/or-op-expr [[_ left right] context ast mw]
  (compile-code left context ast mw)
  (.visitInsn mw Opcodes/DUP)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asBoolean", (sig-call (sig-obj qn-js-primitive) sig-boolean))
  (let [true-case (new Label)]
    (.visitJumpInsn mw, Opcodes/IFNE, true-case)
    (.visitInsn mw Opcodes/POP)
    (compile-code right context ast mw)
    (.visitLabel mw true-case)))

;
; expressions
;

(defmethod compile-code :mug.ast/scope-ref-expr [[_ value] context ast mw]
  (if-let [reg (ref-reg context value)]
    (.visitVarInsn mw Opcodes/ALOAD reg) 
    (let [qn-parent (asm-search-scopes value (context-parents context) context ast mw)]
      (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-parent, (str "get_" value), (sig-call (sig-obj qn-js-primitive))))))

(defmethod compile-code :mug.ast/static-ref-expr [[_ base value] context ast mw]
  (compile-code base context ast mw)
  (asm-to-object context ast mw)
	(doto mw
    (.visitLdcInsn value)
		(.visitMethodInsn Opcodes/INVOKEVIRTUAL, qn-js-object, "get", (sig-call (sig-obj qn-string) (sig-obj qn-js-primitive)))))

(defmethod compile-code :mug.ast/dyn-ref-expr [[_ base index] context ast mw]
  (compile-code base context ast mw)
  (asm-to-object context ast mw)
  (compile-code index context ast mw)
;  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asString", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-string)))
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "get", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-js-primitive))))

(defmethod compile-code :mug.ast/static-method-call-expr [[_ base value args] context ast mw]
  (compile-code base context ast mw)
  ; get argument and method
  (asm-to-object context ast mw)
	(doto mw
    (.visitInsn Opcodes/DUP)
    (.visitLdcInsn value)
		(.visitMethodInsn Opcodes/INVOKEVIRTUAL, qn-js-object, "get", (sig-call (sig-obj qn-string) (sig-obj qn-js-primitive)))
	  (.visitTypeInsn Opcodes/CHECKCAST, qn-js-function)
    (.visitInsn Opcodes/SWAP))
  (asm-invoke-args args context ast mw)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-function, "invoke", sig-invoke))

(defmethod compile-code :mug.ast/call-expr [[_ ref args] context ast mw]
	(compile-code ref context ast mw)
	(.visitTypeInsn mw, Opcodes/CHECKCAST, qn-js-function)
  ; "this"
	(.visitInsn mw Opcodes/ACONST_NULL)
  (asm-invoke-args args context ast mw)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-function, "invoke", sig-invoke))

(defmethod compile-code :mug.ast/new-expr [[_ constructor args] context ast mw]
  (compile-code constructor context ast mw)
	(.visitTypeInsn mw, Opcodes/CHECKCAST, qn-js-function)
  (asm-invoke-args args context ast mw)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-function, "instantiate", sig-instantiate))

(defmethod compile-code :mug.ast/scope-assign-expr [[_ value expr] context ast mw]
	(compile-code expr context ast mw)
  (.visitInsn mw Opcodes/DUP)
  (if-let [reg (ref-reg context value)]
    (.visitVarInsn mw Opcodes/ASTORE reg) 
	  (let [qn-parent (asm-search-scopes value (context-parents context) context ast mw)]
	    (.visitInsn mw Opcodes/SWAP)
		  (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-parent, (str "set_" value), (sig-call (sig-obj qn-js-primitive) sig-void)))))

(defmethod compile-code :mug.ast/static-assign-expr [[_ base value expr] context ast mw]
	(compile-code expr context ast mw)
  (.visitInsn mw Opcodes/DUP)
  (compile-code base context ast mw)
;  (.visitTypeInsn mw, Opcodes/CHECKCAST, qn-js-object)
  (asm-to-object context ast mw)
  (.visitInsn mw Opcodes/SWAP)
  (.visitLdcInsn mw value)
  (.visitInsn mw Opcodes/SWAP)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "set", (sig-call (sig-obj qn-string) (sig-obj qn-js-primitive) sig-void)))

(defmethod compile-code :mug.ast/dyn-assign-expr [[_ base index expr] context ast mw]
	(compile-code expr context ast mw)
  (.visitInsn mw Opcodes/DUP)
  (compile-code base context ast mw)
  (asm-to-object context ast mw)
  (.visitInsn mw Opcodes/SWAP)
  (compile-code index context ast mw)
;  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asString", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-string)))
  (.visitInsn mw Opcodes/SWAP)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "set", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-js-primitive) sig-void)))

(defmethod compile-code :mug.ast/typeof-expr [[_ expr] context ast mw]
  (compile-code expr context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "typeof", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-js-string))))
    
(defmethod compile-code :mug.ast/this-expr [[_] context ast mw]
  (.visitVarInsn mw Opcodes/ALOAD, 1))

(defmethod compile-code :mug.ast/if-expr [[_ expr then-expr else-expr] context ast mw]
  (compile-code expr context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asBoolean", (sig-call (sig-obj qn-js-primitive) sig-boolean))
  (let [false-case (new Label) true-case (new Label)]
    (.visitJumpInsn mw, Opcodes/IFEQ, false-case)
    (compile-code then-expr context ast mw)
    (.visitJumpInsn mw, Opcodes/GOTO, true-case)
    (.visitLabel mw false-case)
    (compile-code else-expr context ast mw)
    (.visitLabel mw true-case)))

(defmethod compile-code :mug.ast/seq-expr [[_ pre expr] context ast mw]
  (compile-code pre context ast mw)
  (.visitInsn mw Opcodes/POP)
  (compile-code expr context ast mw))

;
; statements
;

(defmethod compile-code :mug.ast/block-stat [[_ stats] context ast mw]
  (doseq [stat stats]
    (compile-code stat context ast mw)))

(defmethod compile-code :mug.ast/expr-stat [[_ expr] context ast mw]
	(compile-code expr context ast mw)
	(.visitInsn mw Opcodes/POP))

(defmethod compile-code :mug.ast/ret-stat [[_ expr] context ast mw]
  (if (nil? expr)
    (.visitInsn mw Opcodes/ACONST_NULL)
    (compile-code expr context ast mw))
  (.visitInsn mw Opcodes/ARETURN))

(defmethod compile-code :mug.ast/while-stat [[_ expr stat] context ast mw]
  (let [true-case (new Label) false-case (new Label)]
    (push-label nil true-case false-case)
    
    (.visitLabel mw true-case)
    (compile-code expr context ast mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asBoolean", (sig-call (sig-obj qn-js-primitive) sig-boolean))
    (.visitJumpInsn mw, Opcodes/IFEQ, false-case)
    (when stat
      (compile-code stat context ast mw))
    (.visitJumpInsn mw, Opcodes/GOTO, true-case)
    (.visitLabel mw false-case)
    
    (pop-label nil)))

(defmethod compile-code :mug.ast/do-while-stat [[_ expr stat] context ast mw]
  (let [true-case (new Label) false-case (new Label)]
    (push-label nil true-case false-case)
    
    (.visitLabel mw true-case)
    (compile-code stat context ast mw)
    (compile-code expr context ast mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asBoolean", (sig-call (sig-obj qn-js-primitive) sig-boolean))
    (.visitJumpInsn mw, Opcodes/IFNE, true-case)
    (.visitLabel mw false-case)
    
    (pop-label nil)))

(defmethod compile-code :mug.ast/for-stat [[_ init expr step stat] context ast mw]
  (when init
    (compile-code init context ast mw)
    (when (isa? (first init) :mug.ast/expr)
      (.visitInsn mw Opcodes/POP)))
  (let [start-label (new Label) continue-label (new Label) break-label (new Label)]
    (push-label nil continue-label break-label)
    
    (.visitLabel mw start-label)
    (when expr
      (compile-code expr context ast mw)
      (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asBoolean", (sig-call (sig-obj qn-js-primitive) sig-boolean))
      (.visitJumpInsn mw, Opcodes/IFEQ, break-label))
    (when stat
      (compile-code stat context ast mw))
    (.visitLabel mw continue-label)
    (when step
      (compile-code step context ast mw)
      (.visitInsn mw Opcodes/POP))
    (.visitJumpInsn mw, Opcodes/GOTO, start-label)
    (.visitLabel mw break-label)
    
    (pop-label nil)))

(defmethod compile-code :mug.ast/if-stat [[_ expr then-stat else-stat] context ast mw]
  (compile-code expr context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asBoolean", (sig-call (sig-obj qn-js-primitive) sig-boolean))
  (let [false-case (new Label) true-case (new Label)]
    (.visitJumpInsn mw, Opcodes/IFEQ, false-case)
    (compile-code then-stat context ast mw)
    (.visitJumpInsn mw, Opcodes/GOTO, true-case)
    (.visitLabel mw false-case)
    (when (not (nil? else-stat))
      (compile-code else-stat context ast mw))
    (.visitLabel mw true-case)))

(defmethod compile-code :mug.ast/break-stat [[_ label] context ast mw]
  (if (> (count (get-label nil)) 0)
    (.visitJumpInsn mw, Opcodes/GOTO, ((get-label nil) :break))
    (throw (new Exception "Cannot break outside of loop"))))

(defmethod compile-code :mug.ast/continue-stat [[_ label] context ast mw]
  (if (> (count (get-label nil)) 0)
    (.visitJumpInsn mw, Opcodes/GOTO, ((get-label nil) :continue))
    (throw (new Exception "Cannot continue outside of loop"))))

(defmethod compile-code :mug.ast/for-in-stat [[_ value expr stat] context ast mw]
	(let [check-label (new Label) stat-label (new Label)]
    (push-label nil stat-label check-label)
   
    (compile-code expr context ast mw)
    (asm-to-object context ast mw)
		(.visitMethodInsn mw, Opcodes/INVOKEVIRTUAL, "mug/js/JSObject", "getKeys", "()[Ljava/lang/String;")
  	(.visitInsn mw, Opcodes/ICONST_0)
		(.visitJumpInsn mw, Opcodes/GOTO, check-label)
		(.visitLabel mw stat-label)
      ; load from array
      (.visitInsn mw, Opcodes/DUP2)
			(.visitInsn mw, Opcodes/AALOAD)
      ; create string obj
			(.visitTypeInsn mw Opcodes/NEW qn-js-string)
			(.visitInsn mw Opcodes/DUP2)
      (.visitInsn mw Opcodes/SWAP)
			(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-string, "<init>", (sig-call (sig-obj qn-string) sig-void))
      ; store in scope
      (let [qn-parent (asm-search-scopes value (context-parents context) context ast mw)]
        (.visitInsn mw Opcodes/SWAP)
	      (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-parent, (str "set_" value), (sig-call (sig-obj qn-js-primitive) sig-void)))
	
			(compile-code stat context ast mw)
   
      (.visitInsn mw Opcodes/POP)
			(.visitInsn mw, Opcodes/ICONST_1)
			(.visitInsn mw, Opcodes/IADD)
		(.visitLabel mw, check-label)
			(.visitInsn mw, Opcodes/DUP2)
			(.visitInsn mw, Opcodes/SWAP)
			(.visitInsn mw, Opcodes/ARRAYLENGTH)
			(.visitJumpInsn mw, Opcodes/IF_ICMPLT, stat-label)
      (.visitInsn mw, Opcodes/POP2)
      
      (pop-label nil)))