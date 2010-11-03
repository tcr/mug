(ns mug.asm.code
  (:use mug.asm.util))

(import (org.objectweb.asm ClassWriter Opcodes Label))

;
; NOTES:
;
; local variable mapping:
;   0    this object
;   1    JS this object at a given time (function object, global object)
;   2-9  arguments
;   10    current scope object

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; context code
;

(defmulti compile-code (fn [& args] (:type (first args))))

;
; literals
;

(defmethod compile-code :mug.ast/null-literal [node context ast mw]
  (.visitInsn mw Opcodes/ACONST_NULL))

(defmethod compile-code :mug.ast/num-literal [node context ast mw]
	(.visitFieldInsn mw Opcodes/GETSTATIC, qn-js-constants (ident-num (index-of (ast :numbers) (node :value))) (sig-obj qn-js-number)))

(defmethod compile-code :mug.ast/str-literal [node context ast mw]
	(.visitFieldInsn mw Opcodes/GETSTATIC qn-js-constants (ident-str (index-of (ast :strings) (node :value))) (sig-obj qn-js-string)))

(defmethod compile-code :mug.ast/array-literal [node context ast mw]
  (compile-code
    {:type :mug.ast/call-expr
     :ref {:type :mug.ast/scope-ref-expr :value "Array"}
     :args (node :exprs)} context ast mw))

(defmethod compile-code :mug.ast/func-literal [node context ast mw]
  (let [qn (qn-js-context (node :closure))
        closure ((ast :contexts) (node :closure))]
    ; create context instance
    (.visitTypeInsn mw Opcodes/NEW, qn)
		(.visitInsn mw Opcodes/DUP)
    ; load scopes
    (doseq [parent (closure :parents)]
      (if (= ((ast :contexts) parent) context)
        (.visitVarInsn mw Opcodes/ALOAD, 6)
        (do
          (.visitVarInsn mw Opcodes/ALOAD, 0)
          (.visitFieldInsn mw Opcodes/GETFIELD, (qn-js-context (context-index context ast)), (str "SCOPE_" parent), (sig-obj (qn-js-scope parent))))))
		(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn, "<init>", (sig-context-init closure ast))))

;
; expressions
;

(defn asm-compile-closure-search-scopes [name parents context ast mw]
  (loop [parent (last parents) parents (butlast parents)]
	  (if (nil? parent)
	    (if (contains? script-default-vars name)
	      (do
	        (println (str " . Found in global scope: " name))
          (if (= (context-index context ast) 0) ; script-context scope inherits from globals
            (.visitVarInsn mw Opcodes/ALOAD, 6)
            (do
		          (.visitVarInsn mw Opcodes/ALOAD, 0)
		          (.visitFieldInsn mw Opcodes/GETFIELD, (qn-js-context (index-of (ast :contexts) context)),
		            (str "SCOPE_" 0), (sig-obj (qn-js-scope 0)))))
		      (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-globalscope,
	          (str "get_" name), (sig-call (sig-obj qn-js-primitive))))
	      (println (str " . [ERROR] Not found: " name)))
		  (if (contains? (context-scope-vars ((ast :contexts) parent)) name)
		    (do
		      (println (str " . Found in higher scope: " name))
		      (.visitVarInsn mw Opcodes/ALOAD, 0)
		      (.visitFieldInsn mw Opcodes/GETFIELD, (qn-js-context (index-of (ast :contexts) context)),
		        (str "SCOPE_" parent), (sig-obj (qn-js-scope parent)))
		      (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (qn-js-scope parent),
	          (str "get_" name), (sig-call (sig-obj qn-js-primitive))))
	      (do
          (println (str " . Not found in scope " (context-scope-vars ((ast :contexts) parent))))
          (recur (last parents) (butlast parents)))))))

(defmethod compile-code :mug.ast/scope-ref-expr [node context ast mw]
  (println (str "Seeking var \"" (node :value) "\" in current scope (" (context-scope-vars context) ")"))
  (if (contains? (context-scope-vars context) (node :value))
    ; in current scope?
    (do
      (.visitVarInsn mw Opcodes/ALOAD, 6)
      (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (qn-js-scope (context-index context ast)), (str "get_" (node :value)), (sig-call (sig-obj qn-js-primitive))))
    ; we have to search parent scopes
    (do (println (str " . Now looking for " (node :value) " in parent scopes."))
      (asm-compile-closure-search-scopes (node :value) (context :parents) context ast mw))))

(defmethod compile-code :mug.ast/static-ref-expr [node context ast mw]
  (compile-code (node :base) context ast mw)
	(doto mw
    (.visitTypeInsn Opcodes/CHECKCAST, qn-js-object)
    (.visitLdcInsn (node :value))
		(.visitMethodInsn Opcodes/INVOKEVIRTUAL, qn-js-object, "get", (sig-call (sig-obj qn-string) (sig-obj qn-js-primitive)))))

(defmethod compile-code :mug.ast/dyn-ref-expr [node context ast mw]
  (compile-code (node :base) context ast mw)
  (.visitTypeInsn mw, Opcodes/CHECKCAST, qn-js-object)
  (compile-code (node :index) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asString", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-string)))
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "get", (sig-call (sig-obj qn-string) (sig-obj qn-js-primitive))))

(defmethod compile-code :mug.ast/static-method-call-expr [node context ast mw]
  (compile-code (node :base) context ast mw)
  ; get argument and method
	(doto mw
    (.visitTypeInsn Opcodes/CHECKCAST, qn-js-object)
    (.visitInsn Opcodes/DUP)
    (.visitLdcInsn (node :value))
		(.visitMethodInsn Opcodes/INVOKEVIRTUAL, qn-js-object, "get", (sig-call (sig-obj qn-string) (sig-obj qn-js-primitive)))
	  (.visitTypeInsn Opcodes/CHECKCAST, qn-js-function)
    (.visitInsn Opcodes/SWAP))
  ; arg count
  (.visitLdcInsn (new Integer (int (count (node :args)))))
  ; defined args
	(doseq [arg (node :args)]
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
        (compile-code ((node :args) (+ arg-limit _)) context ast mw)
        (.visitInsn mw Opcodes/AASTORE)))
    (.visitInsn mw Opcodes/ACONST_NULL))
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-function, "invoke", sig-invoke))

(defmethod compile-code :mug.ast/call-expr [node context ast mw]
	(compile-code (node :ref) context ast mw)
	(.visitTypeInsn mw, Opcodes/CHECKCAST, qn-js-function)
  ; "this"
	(.visitInsn mw Opcodes/ACONST_NULL)
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
    (.visitInsn mw Opcodes/ACONST_NULL))
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-function, "invoke", sig-invoke))

(defmethod compile-code :mug.ast/scope-assign-expr [node context ast mw]
	(compile-code (node :expr) context ast mw)
  (.visitInsn mw Opcodes/DUP)
  (.visitVarInsn mw Opcodes/ALOAD, 6)
  (.visitInsn mw Opcodes/SWAP)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (qn-js-scope (context-index context ast)), (str "set_" (node :value)), (sig-call (sig-obj qn-js-primitive) sig-void)))

(defmethod compile-code :mug.ast/static-assign-expr [node context ast mw]
	(compile-code (node :expr) context ast mw)
  (.visitInsn mw Opcodes/DUP)
  (compile-code (node :base) context ast mw)
  (.visitTypeInsn mw, Opcodes/CHECKCAST, qn-js-object)
  (.visitInsn mw Opcodes/SWAP)
  (.visitLdcInsn mw (node :value))
  (.visitInsn mw Opcodes/SWAP)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "set", (sig-call (sig-obj qn-string) (sig-obj qn-js-primitive) sig-void)))

(defmethod compile-code :mug.ast/dyn-assign-expr [node context ast mw]
	(compile-code (node :expr) context ast mw)
  (.visitInsn mw Opcodes/DUP)
  (compile-code (node :base) context ast mw)
  (.visitTypeInsn mw, Opcodes/CHECKCAST, qn-js-object)
  (.visitInsn mw Opcodes/SWAP)
  (compile-code (node :index) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asString", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-string)))
  (.visitInsn mw Opcodes/SWAP)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-object, "set", (sig-call (sig-obj qn-string) (sig-obj qn-js-primitive) sig-void)))

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

(defmethod compile-code :mug.ast/eqs-op-expr [node context ast mw]
  (compile-code (node :left) context ast mw)
  (compile-code (node :right) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "testStrictEquality", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-js-primitive) (sig-obj qn-js-boolean))))

(defmethod compile-code :mug.ast/neqs-op-expr [node context ast mw]
  (compile-code (node :left) context ast mw)
  (compile-code (node :right) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "testStrictInequality", (sig-call (sig-obj qn-js-primitive) (sig-obj qn-js-primitive) (sig-obj qn-js-boolean))))

(defmethod compile-code :mug.ast/lt-op-expr [node context ast mw]
  (let [false-case (new Label) true-case (new Label)]
    (compile-code (node :left) context ast mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
    (compile-code (node :right) context ast mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
    (doto mw
      (.visitInsn Opcodes/DCMPG)
      (.visitJumpInsn Opcodes/IFGE, false-case)
      (.visitFieldInsn Opcodes/GETSTATIC, qn-js-atoms, "TRUE", (sig-obj qn-js-boolean))
      (.visitJumpInsn Opcodes/GOTO, true-case)
		  (.visitLabel false-case)
		  (.visitFieldInsn Opcodes/GETSTATIC, qn-js-atoms, "FALSE", (sig-obj qn-js-boolean))
		  (.visitLabel true-case))))

(defmethod compile-code :mug.ast/lte-op-expr [node context ast mw]
  (let [false-case (new Label) true-case (new Label)]
    (compile-code (node :left) context ast mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
    (compile-code (node :right) context ast mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
    (doto mw
      (.visitInsn Opcodes/DCMPG)
      (.visitJumpInsn Opcodes/IFGT, false-case)
      (.visitFieldInsn Opcodes/GETSTATIC, qn-js-atoms, "TRUE", (sig-obj qn-js-boolean))
      (.visitJumpInsn Opcodes/GOTO, true-case)
		  (.visitLabel false-case)
		  (.visitFieldInsn Opcodes/GETSTATIC, qn-js-atoms, "FALSE", (sig-obj qn-js-boolean))
		  (.visitLabel true-case))))

(defmethod compile-code :mug.ast/gt-op-expr [node context ast mw]
  (let [false-case (new Label) true-case (new Label)]
    (compile-code (node :left) context ast mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
    (compile-code (node :right) context ast mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
    (doto mw
      (.visitInsn Opcodes/DCMPG)
      (.visitJumpInsn Opcodes/IFLE, false-case)
      (.visitFieldInsn Opcodes/GETSTATIC, qn-js-atoms, "TRUE", (sig-obj qn-js-boolean))
      (.visitJumpInsn Opcodes/GOTO, true-case)
		  (.visitLabel false-case)
		  (.visitFieldInsn Opcodes/GETSTATIC, qn-js-atoms, "FALSE", (sig-obj qn-js-boolean))
		  (.visitLabel true-case))))

(defmethod compile-code :mug.ast/gte-op-expr [node context ast mw]
  (let [false-case (new Label) true-case (new Label)]
    (compile-code (node :left) context ast mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
    (compile-code (node :right) context ast mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
    (doto mw
      (.visitInsn Opcodes/DCMPG)
      (.visitJumpInsn Opcodes/IFLT, false-case)
      (.visitFieldInsn Opcodes/GETSTATIC, qn-js-atoms, "TRUE", (sig-obj qn-js-boolean))
      (.visitJumpInsn Opcodes/GOTO, true-case)
		  (.visitLabel false-case)
		  (.visitFieldInsn Opcodes/GETSTATIC, qn-js-atoms, "FALSE", (sig-obj qn-js-boolean))
		  (.visitLabel true-case))))

(defmethod compile-code :mug.ast/neg-op-expr [node context ast mw]
  (.visitTypeInsn mw Opcodes/NEW, qn-js-number)
  (.visitInsn mw Opcodes/DUP)
  (compile-code (node :expr) context ast mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
  (.visitInsn mw Opcodes/DNEG)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-number, "<init>", (sig-call sig-double sig-void)))
    
(defmethod compile-code :mug.ast/this-expr [node context ast mw]
  (.visitVarInsn mw Opcodes/ALOAD, 1))

(defmethod compile-code :mug.ast/new-expr [node context ast mw]
  (compile-code (node :constructor) context ast mw)
	(.visitTypeInsn mw, Opcodes/CHECKCAST, qn-js-function)
	(doseq [arg (node :args)]
		(compile-code arg context ast mw))
	(doseq [_ (range (count (node :args)) arg-limit)]
		(.visitInsn mw Opcodes/ACONST_NULL))
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-function, "instantiate", sig-instantiate))

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
  (let [test-case (new Label) false-case (new Label)]
    (.visitLabel mw test-case)
    (compile-code (node :expr) context ast mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asBoolean", (sig-call (sig-obj qn-js-primitive) sig-boolean))
    (.visitJumpInsn mw, Opcodes/IFEQ, false-case)
    (compile-code (node :stat) context ast mw)
    (.visitJumpInsn mw, Opcodes/GOTO, test-case)
    (.visitLabel mw false-case)))

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

(comment
(defmethod compile-code :mug.ast/for-in-stat [node context ast mw]
  (let [test-label (new Label) body-label (new Label)]
    ; assign variable
	  (.visitVarInsn mw Opcodes/ALOAD, 6)
    (.visitTypeInsn mw Opcodes/NEW, qn-js-number)
    (.visitInsn mw Opcodes/DUP)
	  (compile-code (node :from) context ast mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
    (.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-number, "<init>", (sig-call sig-double sig-void))
		(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (qn-js-scope (context-index context ast)), (str "set_" (node :value)), (sig-call (sig-obj qn-js-primitive) sig-void))
    ; label
    (.visitJumpInsn mw Opcodes/GOTO, test-label)
  
    ; body
    (.visitLabel mw body-label)
    (compile-code (node :stat) context ast mw)
    
    ; increment
		(.visitVarInsn mw Opcodes/ALOAD, 6)
		(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (qn-js-scope (context-index context ast)), (str "get_" (node :value)), (sig-call (sig-obj qn-js-primitive)))
    (.visitTypeInsn mw Opcodes/CHECKCAST, qn-js-number)
    (.visitInsn mw Opcodes/DUP)
    (.visitFieldInsn mw Opcodes/GETFIELD, qn-js-number, "value", sig-double)
    (compile-code (node :by) context ast mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
    (.visitInsn mw Opcodes/DADD)
    (.visitFieldInsn mw Opcodes/PUTFIELD, qn-js-number, "value", sig-double)
    
    ; condition
    (.visitLabel mw test-label)
		(.visitVarInsn mw Opcodes/ALOAD, 6)
		(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (qn-js-scope (context-index context ast)), (str "get_" (node :value)), (sig-call (sig-obj qn-js-primitive)))
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
    (compile-code (node :to) context ast mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "asNumber", (sig-call (sig-obj qn-js-primitive) sig-double))
    (.visitInsn mw Opcodes/DCMPG)
    (.visitJumpInsn mw Opcodes/IFLT, body-label)))
)

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
    (.visitVarInsn mw Opcodes/ALOAD, 6)
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