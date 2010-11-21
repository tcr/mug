(ns mug.asm.code
  (:use mug.asm.util))

(import (org.objectweb.asm ClassWriter Opcodes Label))

;
; NOTES:
;
; local variable mapping:
;   0    this object
;   1    JS "this" object at a given time (function object, global object)
;   2-x  arguments
;   x+1    current scope object

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; utilities
;

(defn asm-toplevel [context ast mw]
  (if (= (context-index context ast) 0)
    (.visitVarInsn mw Opcodes/ALOAD, (+ 1 3 arg-limit))
    (do
      (.visitVarInsn mw Opcodes/ALOAD, 0)
      (.visitFieldInsn mw Opcodes/GETFIELD, (qn-js-context (index-of (ast :contexts) context)),
	      (str "SCOPE_" 0), (sig-obj (qn-js-scope 0)))
      (.visitTypeInsn mw Opcodes/CHECKCAST, qn-js-toplevel))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; context code
;

(defmulti compile-code (fn [& args] (:type (first args))))

;
; literals
;

(defmethod compile-code :mug.ast/null-literal [node context ast mw]
  (.visitFieldInsn mw Opcodes/GETSTATIC, qn-js-atoms, "NULL", (sig-obj qn-js-null)))

(defmethod compile-code :mug.ast/boolean-literal [node context ast mw]
	(.visitFieldInsn mw Opcodes/GETSTATIC, qn-js-atoms, (if (node :value) "TRUE" "FALSE"), (sig-obj qn-js-boolean)))

(defmethod compile-code :mug.ast/num-literal [node context ast mw]
	(.visitFieldInsn mw Opcodes/GETSTATIC, (qn-js-constants) (ident-num (index-of (ast :numbers) (node :value))) (sig-obj qn-js-number)))

(defmethod compile-code :mug.ast/str-literal [node context ast mw]
	(.visitFieldInsn mw Opcodes/GETSTATIC (qn-js-constants) (ident-str (index-of (ast :strings) (node :value))) (sig-obj qn-js-string)))

(defmethod compile-code :mug.ast/regex-literal [node context ast mw]
	(.visitTypeInsn mw Opcodes/NEW qn-js-regexp)
	(.visitInsn mw Opcodes/DUP)
  (asm-toplevel context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-toplevel, "getRegExpPrototype", (sig-call (sig-obj qn-js-object)))
	(.visitFieldInsn mw Opcodes/GETSTATIC (qn-js-constants) (ident-regex (index-of (ast :regexes) [(node :expr) (node :flags)])) (sig-obj qn-pattern))
  (.visitLdcInsn mw (node :flags))
	(.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "isPatternGlobal", (sig-call (sig-obj qn-string) sig-boolean))
	(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-regexp, "<init>", (sig-call (sig-obj qn-js-object) (sig-obj qn-pattern) sig-boolean sig-void)))

(defmethod compile-code :mug.ast/array-literal [node context ast mw]
	(.visitTypeInsn mw Opcodes/NEW qn-js-array)
	(.visitInsn mw Opcodes/DUP)
  (asm-toplevel context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-toplevel, "getArrayPrototype", (sig-call (sig-obj qn-js-object)))
	(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-array, "<init>", (sig-call (sig-obj qn-js-object) sig-void))
	(.visitInsn mw Opcodes/DUP)
  (.visitIntInsn mw Opcodes/BIPUSH, (count (node :exprs)))
  (.visitTypeInsn mw Opcodes/ANEWARRAY, qn-js-primitive)
  (doseq [[i expr] (index (node :exprs))]
    (.visitInsn mw Opcodes/DUP)
    (.visitIntInsn mw Opcodes/BIPUSH, i)
    (compile-code expr context ast mw)
    (.visitInsn mw Opcodes/AASTORE))
  (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-array, "append", (sig-call (sig-array (sig-obj qn-js-primitive)) sig-void)))
;  (compile-code
;    {:type :mug.ast/call-expr
;     :ref {:type :mug.ast/scope-ref-expr :value "Array"}
;     :args (node :exprs)} context ast mw))

(defmethod compile-code :mug.ast/obj-literal [node context ast mw]
	(.visitTypeInsn mw Opcodes/NEW qn-js-object)
	(.visitInsn mw Opcodes/DUP)
  ; object prototype
  (asm-toplevel context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-toplevel, "getObjectPrototype", (sig-call (sig-obj qn-js-object)))
	(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-object, "<init>", (sig-call (sig-obj qn-js-object) sig-void))
	(doseq [[k v] (node :props)]
    (.visitInsn mw Opcodes/DUP)
    (.visitLdcInsn mw k)
    (compile-code v context ast mw)
  	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "set", (sig-call (sig-obj qn-string) (sig-obj qn-js-primitive) sig-void))))

(defmethod compile-code :mug.ast/func-literal [node context ast mw]
  (let [qn (qn-js-context (node :closure))
        closure ((ast :contexts) (node :closure))]
    ; create context instance
    (.visitTypeInsn mw Opcodes/NEW, qn)
		(.visitInsn mw Opcodes/DUP)
    ; function prototype
    (asm-toplevel context ast mw)
    (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-toplevel, "getFunctionPrototype", (sig-call (sig-obj qn-js-object)))
    ; load scopes
    (doseq [parent (closure :parents)]
      (if (= ((ast :contexts) parent) context)
        (.visitVarInsn mw Opcodes/ALOAD, (+ 1 3 arg-limit))
        (do
          (.visitVarInsn mw Opcodes/ALOAD, 0)
          (.visitFieldInsn mw Opcodes/GETFIELD, (qn-js-context (context-index context ast)), (str "SCOPE_" parent), (sig-obj (qn-js-scope parent))))))
		(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn, "<init>", (sig-context-init closure ast))))

;
; expressions
;

(defn asm-to-object [context ast mw]
  (asm-toplevel context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-primitive, "toObject", (sig-call (sig-obj qn-js-toplevel) (sig-obj qn-js-object))))

; SEARCH SCOPES
; loads scope object, then return the qn of the scope
; in which it is located
(defn asm-compile-closure-search-scopes [name parents context ast mw]
  (if (contains? (context-scope-vars context) name)
    ; variable found in current scope
    (do
      (.visitVarInsn mw Opcodes/ALOAD, (+ 1 3 arg-limit))
      (qn-js-scope (context-index context ast)))
  (loop [parent (last parents) parents (butlast parents)]
	  (if (nil? parent)
	    (if (contains? script-default-vars name)
        ; found variable in global scope
	      (do
	        (println (str " . Found in global scope: " name))
          (if (= (context-index context ast) 0) ; script-context scope inherits from globals
            (.visitVarInsn mw Opcodes/ALOAD, (+ 1 3 arg-limit))
            (do
		          (.visitVarInsn mw Opcodes/ALOAD, 0)
		          (.visitFieldInsn mw Opcodes/GETFIELD, (qn-js-context (index-of (ast :contexts) context)),
		            (str "SCOPE_" 0), (sig-obj (qn-js-scope 0)))))
          qn-js-toplevel)
;		      (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-toplevel,
;	          (str "get_" name), (sig-call (sig-obj qn-js-primitive))))
        ; identifier not found at all
	      (throw (new Exception (str "Identifier not defined in any scope: " name))))
		  (if (contains? (context-scope-vars ((ast :contexts) parent)) name)
        ; found variable in ancestor scope
		    (do
		      (println (str " . Found in higher scope: " name))
		      (.visitVarInsn mw Opcodes/ALOAD, 0)
		      (.visitFieldInsn mw Opcodes/GETFIELD, (qn-js-context (index-of (ast :contexts) context)),
		        (str "SCOPE_" parent), (sig-obj (qn-js-scope parent)))
          (qn-js-scope parent))
;		      (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (qn-js-scope parent),
;	          (str "get_" name), (sig-call (sig-obj qn-js-primitive))))
        ; must recur to parent scope
	      (do
          (println (str " . Not found in parent scope " (context-scope-vars ((ast :contexts) parent))))
          (recur (last parents) (butlast parents))))))))

(defmethod compile-code :mug.ast/scope-ref-expr [node context ast mw]
  (println (str "Seeking var \"" (node :value) "\" in current scope (" (context-scope-vars context) ")"))
  (let [qn-parent (asm-compile-closure-search-scopes (node :value) (context :parents) context ast mw)]
    (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-parent, (str "get_" (node :value)), (sig-call (sig-obj qn-js-primitive)))))
  
;  (if (contains? (context-scope-vars context) (node :value))
    ; in current scope?
;    (do
;      (.visitVarInsn mw Opcodes/ALOAD, (+ 1 3 arg-limit))
 ;     (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (qn-js-scope (context-index context ast)), (str "get_" (node :value)), (sig-call (sig-obj qn-js-primitive))))
    ; we have to search parent scopes
 ;   (do
;      (asm-compile-closure-search-scopes (node :value) (context :parents) context ast mw))))

(defmethod compile-code :mug.ast/static-ref-expr [node context ast mw]
  (compile-code (node :base) context ast mw)
  (asm-to-object context ast mw)
	(doto mw
;    (.visitTypeInsn Opcodes/CHECKCAST, qn-js-object)
    (.visitLdcInsn (node :value))
		(.visitMethodInsn Opcodes/INVOKEVIRTUAL, qn-js-object, "get", (sig-call (sig-obj qn-string) (sig-obj qn-js-primitive)))))

(defmethod compile-code :mug.ast/dyn-ref-expr [node context ast mw]
  (compile-code (node :base) context ast mw)
;  (.visitTypeInsn mw, Opcodes/CHECKCAST, qn-js-object)
  (asm-to-object context ast mw)
  (compile-code (node :index) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asString", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-string)))
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "get", (sig-call (sig-obj qn-string) (sig-obj qn-js-primitive))))

(defn invoke-args [node context ast mw]
  ; arg count
  (.visitLdcInsn mw (new Integer (int (count (node :args)))))
  ; defined args
	(doseq [arg (subvec (vec (node :args)) 0 (min (count (node :args)) arg-limit))]
		(compile-code arg context ast mw))
  ; undefined args
	(doseq [_ (range (count (node :args)) arg-limit)]
    (if (= _ (count (node :args)))
          (.visitInsn mw Opcodes/ACONST_NULL)
          (.visitInsn mw Opcodes/DUP)))
  ; extra args
  (if (> (count (node :args)) arg-limit)
    (do
      (.visitIntInsn mw Opcodes/BIPUSH, (- (count (node :args)) arg-limit))
      (.visitTypeInsn mw Opcodes/ANEWARRAY, qn-js-primitive)
      (doseq [_ (range (- (count (node :args)) arg-limit))]
        (.visitInsn mw Opcodes/DUP)
        (.visitIntInsn mw Opcodes/BIPUSH, _)
        (compile-code ((vec (node :args)) (+ arg-limit _)) context ast mw)
        (.visitInsn mw Opcodes/AASTORE)))
    (.visitInsn mw Opcodes/ACONST_NULL)))

(defmethod compile-code :mug.ast/static-method-call-expr [node context ast mw]
  (compile-code (node :base) context ast mw)
  ; get argument and method
  (asm-to-object context ast mw)
	(doto mw
    (.visitInsn Opcodes/DUP)
    (.visitLdcInsn (node :value))
		(.visitMethodInsn Opcodes/INVOKEVIRTUAL, qn-js-object, "get", (sig-call (sig-obj qn-string) (sig-obj qn-js-primitive)))
	  (.visitTypeInsn Opcodes/CHECKCAST, qn-js-function)
    (.visitInsn Opcodes/SWAP))
  (invoke-args node context ast mw)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-function, "invoke", sig-invoke))

(defmethod compile-code :mug.ast/call-expr [node context ast mw]
	(compile-code (node :ref) context ast mw)
	(.visitTypeInsn mw, Opcodes/CHECKCAST, qn-js-function)
  ; "this"
	(.visitInsn mw Opcodes/ACONST_NULL)
  (invoke-args node context ast mw)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-function, "invoke", sig-invoke))

(defmethod compile-code :mug.ast/new-expr [node context ast mw]
  (compile-code (node :constructor) context ast mw)
	(.visitTypeInsn mw, Opcodes/CHECKCAST, qn-js-function)
  (invoke-args node context ast mw)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-function, "instantiate", sig-instantiate))

(defmethod compile-code :mug.ast/scope-assign-expr [node context ast mw]
	(compile-code (node :expr) context ast mw)
  (.visitInsn mw Opcodes/DUP)
  (let [qn-parent (asm-compile-closure-search-scopes (node :value) (context :parents) context ast mw)]
    (.visitInsn mw Opcodes/SWAP)
	  (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-parent, (str "set_" (node :value)), (sig-call (sig-obj qn-js-primitive) sig-void))))

(defmethod compile-code :mug.ast/static-assign-expr [node context ast mw]
	(compile-code (node :expr) context ast mw)
  (.visitInsn mw Opcodes/DUP)
  (compile-code (node :base) context ast mw)
;  (.visitTypeInsn mw, Opcodes/CHECKCAST, qn-js-object)
  (asm-to-object context ast mw)
  (.visitInsn mw Opcodes/SWAP)
  (.visitLdcInsn mw (node :value))
  (.visitInsn mw Opcodes/SWAP)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "set", (sig-call (sig-obj qn-string) (sig-obj qn-js-primitive) sig-void)))

(defmethod compile-code :mug.ast/dyn-assign-expr [node context ast mw]
	(compile-code (node :expr) context ast mw)
  (.visitInsn mw Opcodes/DUP)
  (compile-code (node :base) context ast mw)
;  (.visitTypeInsn mw, Opcodes/CHECKCAST, qn-js-object)
  (asm-to-object context ast mw)
  (.visitInsn mw Opcodes/SWAP)
  (compile-code (node :index) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asString", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-string)))
  (.visitInsn mw Opcodes/SWAP)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "set", (sig-call (sig-obj qn-string) (sig-obj qn-js-primitive) sig-void)))

;(defmethod compile-code :mug.ast/post-inc-op-expr [node context ast mw]
;  (compile-code (node :left) context ast mw)
;  (.visitInsn mw Opcodes/DUP)
;  (compile-code (node :right) context ast mw)
;  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "add", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-js-primitive) (sig-obj qn-js-primitive))))

(defmethod compile-code :mug.ast/add-op-expr [node context ast mw]
  (compile-code (node :left) context ast mw)
  (compile-code (node :right) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "add", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-js-primitive) (sig-obj qn-js-primitive))))

(defmethod compile-code :mug.ast/sub-op-expr [node context ast mw]
  (.visitTypeInsn mw Opcodes/NEW, qn-js-number)
  (.visitInsn mw Opcodes/DUP)
  (compile-code (node :left) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
  (compile-code (node :right) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
  (.visitInsn mw Opcodes/DSUB)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-number, "<init>", (sig-call sig-double sig-void)))

(defmethod compile-code :mug.ast/div-op-expr [node context ast mw]
  (.visitTypeInsn mw Opcodes/NEW, qn-js-number)
  (.visitInsn mw Opcodes/DUP)
  (compile-code (node :left) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
  (compile-code (node :right) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
  (.visitInsn mw Opcodes/DDIV)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-number, "<init>", (sig-call sig-double sig-void)))

(defmethod compile-code :mug.ast/mul-op-expr [node context ast mw]
  (.visitTypeInsn mw Opcodes/NEW, qn-js-number)
  (.visitInsn mw Opcodes/DUP)
  (compile-code (node :left) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
  (compile-code (node :right) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
  (.visitInsn mw Opcodes/DMUL)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-number, "<init>", (sig-call sig-double sig-void)))

(defmethod compile-code :mug.ast/lsh-op-expr [node context ast mw]
  (.visitTypeInsn mw Opcodes/NEW, qn-js-number)
  (.visitInsn mw Opcodes/DUP)
  (compile-code (node :left) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
  (.visitInsn mw Opcodes/D2I)
  (compile-code (node :right) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
  (.visitInsn mw Opcodes/D2I)
  (.visitInsn mw Opcodes/ISHL)
  (.visitInsn mw Opcodes/I2D)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-number, "<init>", (sig-call sig-double sig-void)))

(defmethod compile-code :mug.ast/eq-op-expr [node context ast mw]
  (compile-code (node :left) context ast mw)
  (compile-code (node :right) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "testEquality", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-js-primitive) (sig-obj qn-js-boolean))))

(defmethod compile-code :mug.ast/neq-op-expr [node context ast mw]
  (compile-code (node :left) context ast mw)
  (compile-code (node :right) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "testInequality", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-js-primitive) (sig-obj qn-js-boolean))))

(defmethod compile-code :mug.ast/not-op-expr [node context ast mw]
  (let [false-case (new Label) true-case (new Label)]
    (compile-code (node :expr) context ast mw)
    (doto mw
      (.visitMethodInsn Opcodes/INVOKESTATIC, qn-js-utils, "asBoolean", (sig-call (sig-obj qn-js-primitive) sig-boolean))
      (.visitJumpInsn Opcodes/IFNE, false-case)
      (.visitFieldInsn Opcodes/GETSTATIC, qn-js-atoms, "TRUE", (sig-obj qn-js-boolean))
      (.visitJumpInsn Opcodes/GOTO, true-case)
		  (.visitLabel false-case)
		  (.visitFieldInsn Opcodes/GETSTATIC, qn-js-atoms, "FALSE", (sig-obj qn-js-boolean))
		  (.visitLabel true-case))))

(defmethod compile-code :mug.ast/eqs-op-expr [node context ast mw]
  (compile-code (node :left) context ast mw)
  (compile-code (node :right) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "testStrictEquality", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-js-primitive) (sig-obj qn-js-boolean))))

(defmethod compile-code :mug.ast/neqs-op-expr [node context ast mw]
  (compile-code (node :left) context ast mw)
  (compile-code (node :right) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "testStrictInequality", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-js-primitive) (sig-obj qn-js-boolean))))

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
  
(defmethod compile-code :mug.ast/lt-op-expr [node context ast mw]
  (asm-compare-op Opcodes/IFLT (node :left) (node :right) context ast mw))

(defmethod compile-code :mug.ast/lte-op-expr [node context ast mw]
  (asm-compare-op Opcodes/IFLE (node :left) (node :right) context ast mw))

(defmethod compile-code :mug.ast/gt-op-expr [node context ast mw]
  (asm-compare-op Opcodes/IFGT (node :left) (node :right) context ast mw))

(defmethod compile-code :mug.ast/gte-op-expr [node context ast mw]
  (asm-compare-op Opcodes/IFGE (node :left) (node :right) context ast mw))

(defmethod compile-code :mug.ast/neg-op-expr [node context ast mw]
  (.visitTypeInsn mw Opcodes/NEW, qn-js-number)
  (.visitInsn mw Opcodes/DUP)
  (compile-code (node :expr) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
  (.visitInsn mw Opcodes/DNEG)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-number, "<init>", (sig-call sig-double sig-void)))

(defmethod compile-code :mug.ast/typeof-expr [node context ast mw]
  (compile-code (node :expr) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "typeof", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-js-string))))
    
(defmethod compile-code :mug.ast/this-expr [node context ast mw]
  (.visitVarInsn mw Opcodes/ALOAD, 1))

(defmethod compile-code :mug.ast/or-op-expr [node context ast mw]
  (compile-code (node :left) context ast mw)
  (.visitInsn mw Opcodes/DUP)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asBoolean", (sig-call (sig-obj qn-js-primitive) sig-boolean))
  (let [true-case (new Label)]
    (.visitJumpInsn mw, Opcodes/IFNE, true-case)
    (.visitInsn mw Opcodes/POP)
    (compile-code (node :right) context ast mw)
    (.visitLabel mw true-case)))

(defmethod compile-code :mug.ast/if-expr [node context ast mw]
  (compile-code (node :expr) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asBoolean", (sig-call (sig-obj qn-js-primitive) sig-boolean))
  (let [false-case (new Label) true-case (new Label)]
    (.visitJumpInsn mw, Opcodes/IFEQ, false-case)
    (compile-code (node :then-expr) context ast mw)
    (.visitJumpInsn mw, Opcodes/GOTO, true-case)
    (.visitLabel mw false-case)
    (compile-code (node :else-expr) context ast mw)
    (.visitLabel mw true-case)))

(defmethod compile-code :mug.ast/seq-expr [node context ast mw]
  (compile-code (node :pre) context ast mw)
  (.visitInsn mw Opcodes/POP)
  (compile-code (node :expr) context ast mw))

;
; statements
;

(defmethod compile-code :mug.ast/block-stat [node context ast mw]
  (doseq [stat (node :stats)]
    (compile-code stat context ast mw)))

(defmethod compile-code :mug.ast/expr-stat [node context ast mw]
	(compile-code (node :expr) context ast mw)
	(.visitInsn mw Opcodes/POP))

(defmethod compile-code :mug.ast/ret-stat [node context ast mw]
  (if (nil? (node :expr))
    (.visitInsn mw Opcodes/ACONST_NULL)
    (compile-code (node :expr) context ast mw))
  (.visitInsn mw Opcodes/ARETURN))

(defmethod compile-code :mug.ast/while-stat [node context ast mw]
  (let [true-case (new Label) false-case (new Label)]
    (.visitLabel mw true-case)
    (compile-code (node :expr) context ast mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asBoolean", (sig-call (sig-obj qn-js-primitive) sig-boolean))
    (.visitJumpInsn mw, Opcodes/IFEQ, false-case)
    (when (node :stat)
      (compile-code (node :stat) context ast mw))
    (.visitJumpInsn mw, Opcodes/GOTO, true-case)
    (.visitLabel mw false-case)))

(defmethod compile-code :mug.ast/do-while-stat [node context ast mw]
  (let [true-case (new Label)]
    (.visitLabel mw true-case)
    (compile-code (node :stat) context ast mw)
    (compile-code (node :expr) context ast mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asBoolean", (sig-call (sig-obj qn-js-primitive) sig-boolean))
    (.visitJumpInsn mw, Opcodes/IFNE, true-case)
    ))

(defmethod compile-code :mug.ast/if-stat [node context ast mw]
  (compile-code (node :expr) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asBoolean", (sig-call (sig-obj qn-js-primitive) sig-boolean))
  (let [false-case (new Label) true-case (new Label)]
    (.visitJumpInsn mw, Opcodes/IFEQ, false-case)
    (compile-code (node :then-stat) context ast mw)
    (.visitJumpInsn mw, Opcodes/GOTO, true-case)
    (.visitLabel mw false-case)
    (when (not (nil? (node :else-stat)))
      (compile-code (node :else-stat) context ast mw))
    (.visitLabel mw true-case)))

(defmethod compile-code :mug.ast/for-in-stat [node context ast mw]
	(let [check-label (new Label) stat-label (new Label)]
    (compile-code (node :expr) context ast mw)
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
      (let [qn-parent (asm-compile-closure-search-scopes (node :value) (context :parents) context ast mw)]
        (.visitInsn mw Opcodes/SWAP)
	      (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-parent, (str "set_" (node :value)), (sig-call (sig-obj qn-js-primitive) sig-void)))
	
			(compile-code (node :stat) context ast mw)
   
      (.visitInsn mw Opcodes/POP)
			(.visitInsn mw, Opcodes/ICONST_1)
			(.visitInsn mw, Opcodes/IADD)
		(.visitLabel mw, check-label)
			(.visitInsn mw, Opcodes/DUP2)
			(.visitInsn mw, Opcodes/SWAP)
			(.visitInsn mw, Opcodes/ARRAYLENGTH)
			(.visitJumpInsn mw, Opcodes/IF_ICMPLT, stat-label)
      (.visitInsn mw, Opcodes/POP2)))

(comment
(defmethod compile-code :mug.ast/class-stat [node context ast mw]
  (let [i (index-of (ast :closures) ((node :constructor) :context))
        qn (qn-js-context i)]
    ; create context instance
    (.visitTypeInsn mw Opcodes/NEW, qn)
		(.visitInsn mw Opcodes/DUP)
		(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn, "<init>", (sig-call sig-void)))

    ; set environment variable
		(.visitInsn mw Opcodes/DUP)
    (.visitVarInsn mw Opcodes/ALOAD, (+ 1 3 arg-limit))
	  (.visitInsn mw Opcodes/SWAP)
    ;;;[TODO] scope might be any scope
		(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (qn-js-scope (context-index context ast)), (str "set_" (node :name)), (sig-call (sig-obj qn-js-primitive) sig-void))

    ; set prototype
    (.visitLdcInsn "prototype")
    (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "get", (sig-call (sig-obj qn-string) (sig-obj qn-js-primitive)))
    (doseq [[k v] (node :prototype)]
      (.visitInsn mw Opcodes/DUP)
      (.visitTypeInsn mw Opcodes/CHECKCAST, qn-js-object)
      (.visitLdcInsn k)
      (compile-code v context ast mw)
      (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "set", (sig-call (sig-obj qn-string) (sig-obj qn-js-primitive) sig-void)))
    (.visitInsn mw Opcodes/POP))
)