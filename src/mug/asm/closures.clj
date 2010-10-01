(ns mug.asm.closures
  (:use mug.asm.util))

(import (org.objectweb.asm ClassWriter Opcodes Label))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; closures
;

(defmulti asm-compile-closure-ast (fn [& args] (:type (first args))))

(defn asm-compile-closure-scope-qn [closure profile]
  (str "mug/compiled/JSScope_" (index-of (profile :closures) closure)))

(defn asm-compile-closure-sig [closure profile]
  (str (apply str (cons "(" (map (fn [x] (str "Lmug/compiled/JSScope_" (index-of (profile :closures) x) ";")) 
    ((profile :scopes) closure)))) ")V"))

;
; literals
;

(defmethod asm-compile-closure-ast :mug/null-literal [node closure profile mw]
  (.visitInsn mw Opcodes/ACONST_NULL))

(defmethod asm-compile-closure-ast :mug/num-literal [node closure profile mw]
	(.visitFieldInsn mw Opcodes/GETSTATIC, "mug/compiled/JSConstants" (str "NUM_" (index-of (profile :numbers) (node :value))) "Lmug/JSNumber;"))

(defmethod asm-compile-closure-ast :mug/str-literal [node closure profile mw]
	(.visitFieldInsn mw Opcodes/GETSTATIC "mug/compiled/JSConstants" (str "STR_" (index-of (profile :strings) (node :value))) "Lmug/JSString;"))

(defmethod asm-compile-closure-ast :mug/func-literal [node closure profile mw]
  (let [i (index-of (profile :closures) (node :closure))
        qn (str "mug/compiled/JSClosure_" i)]
    ; create closure instance
    (.visitTypeInsn mw Opcodes/NEW, qn)
		(.visitInsn mw Opcodes/DUP)
    ; load scopes
    (doseq [scope ((profile :scopes) (node :closure))]
      (if (= scope closure)
        (.visitVarInsn mw Opcodes/ALOAD, 6)
        (.visitFieldInsn Opcodes/GETFIELD, (str "mug/compiled/JSClosure_" (index-of (profile :closures) closure)), (str "SCOPE_" (index-of (profile :closures) scope)), (str "Lmug/compiled/JSScope_" (index-of (profile :closures) scope) ";"))))
		(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn, "<init>", (asm-compile-closure-sig (node :closure) profile))))

;
; expressions
;

(defn asm-compile-closure-search-scopes [name scopes closure profile mw]
  (if (empty? scopes)
    (println (str "NOTFOUND:" name))
    (if (contains? (into ((first scopes) :vars) ((first scopes) :args)) name)
      (do
        (println (str "Found in higher scope: " name))
        (.visitVarInsn mw Opcodes/ALOAD, 0)
        (.visitFieldInsn mw Opcodes/GETFIELD, (str "mug/compiled/JSClosure_" (index-of (profile :closures) closure)), (str "SCOPE_" (index-of (profile :closures) (first scopes))), (str "Lmug/compiled/JSScope_" (index-of (profile :closures) (first scopes)) ";"))
        (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (asm-compile-closure-scope-qn (first scopes) profile), (str "get_" name), "()Lmug/JSPrimitive;"))
      (asm-compile-closure-search-scopes name (next scopes) closure profile mw))))

(defmethod asm-compile-closure-ast :mug/scope-ref-expr [node closure profile mw]
  (if (contains? (into (closure :vars) (closure :args)) (node :value))
    (do
      (.visitVarInsn mw Opcodes/ALOAD, 6)
      (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (asm-compile-closure-scope-qn closure profile), (str "get_" (node :value)), "()Lmug/JSPrimitive;"))
    (asm-compile-closure-search-scopes (node :value) (reverse ((profile :scopes) closure)) closure profile mw)))

(defmethod asm-compile-closure-ast :mug/static-ref-expr [node closure profile mw]
  (asm-compile-closure-ast (node :base) closure profile mw)
	(doto mw
    (.visitTypeInsn Opcodes/CHECKCAST, "mug/compiled/JSObject")
		(.visitMethodInsn Opcodes/INVOKEVIRTUAL, "mug/compiled/JSObject", (str "get_" (node :value)), "()Lmug/JSPrimitive;")))

(defmethod asm-compile-closure-ast :mug/dyn-ref-expr [node closure profile mw]
  (asm-compile-closure-ast (node :base) closure profile mw)
  (.visitTypeInsn mw, Opcodes/CHECKCAST, "mug/compiled/JSObject")
  (asm-compile-closure-ast (node :index) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asString", "(Lmug/JSPrimitive;)Ljava/lang/String;")
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, "mug/compiled/JSObject", "get", "(Ljava/lang/String;)Lmug/JSPrimitive;"))

(defmethod asm-compile-closure-ast :mug/static-method-call-expr [node closure profile mw]
  (asm-compile-closure-ast (node :base) closure profile mw)
	(doto mw
    (.visitTypeInsn Opcodes/CHECKCAST, "mug/compiled/JSObject")
    (.visitInsn Opcodes/DUP)
		(.visitMethodInsn Opcodes/INVOKEVIRTUAL, "mug/compiled/JSObject", (str "get_" (node :value)), "()Lmug/JSPrimitive;")
	  (.visitTypeInsn Opcodes/CHECKCAST, "mug/JSFunction")
    (.visitInsn Opcodes/SWAP))
	(doseq [arg (node :args)]
		(asm-compile-closure-ast arg closure profile mw))
	(doseq [_ (range (count (node :args)) 4)]
		(.visitInsn mw Opcodes/ACONST_NULL))
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, "mug/JSFunction", "invoke", "(Lmug/compiled/JSObject;Lmug/JSPrimitive;Lmug/JSPrimitive;Lmug/JSPrimitive;Lmug/JSPrimitive;)Lmug/JSPrimitive;"))

(defmethod asm-compile-closure-ast :mug/call-expr [node closure profile mw]
	(asm-compile-closure-ast (node :ref) closure profile mw)
	(.visitTypeInsn mw, Opcodes/CHECKCAST, "mug/JSFunction")
	(.visitInsn mw Opcodes/ACONST_NULL)
	(doseq [arg (node :args)]
		(asm-compile-closure-ast arg closure profile mw))
	(doseq [_ (range (count (node :args)) 4)]
		(.visitInsn mw Opcodes/ACONST_NULL))
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, "mug/JSFunction", "invoke", "(Lmug/compiled/JSObject;Lmug/JSPrimitive;Lmug/JSPrimitive;Lmug/JSPrimitive;Lmug/JSPrimitive;)Lmug/JSPrimitive;"))

(defmethod asm-compile-closure-ast :mug/scope-assign-expr [node closure profile mw]
	(asm-compile-closure-ast (node :expr) closure profile mw)
  (.visitInsn mw Opcodes/DUP)
  (.visitVarInsn mw Opcodes/ALOAD, 6)
  (.visitInsn mw Opcodes/SWAP)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (asm-compile-closure-scope-qn closure profile), (str "set_" (node :value)), "(Lmug/JSPrimitive;)V"))

(defmethod asm-compile-closure-ast :mug/static-assign-expr [node closure profile mw]
	(asm-compile-closure-ast (node :expr) closure profile mw)
  (.visitInsn mw Opcodes/DUP)
  (asm-compile-closure-ast (node :base) closure profile mw)
  (.visitTypeInsn mw, Opcodes/CHECKCAST, "mug/compiled/JSObject")
  (.visitInsn mw Opcodes/SWAP)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, "mug/compiled/JSObject", (str "set_" (node :value)), "(Lmug/JSPrimitive;)V"))

(defmethod asm-compile-closure-ast :mug/dyn-assign-expr [node closure profile mw]
	(asm-compile-closure-ast (node :expr) closure profile mw)
  (.visitInsn mw Opcodes/DUP)
  (asm-compile-closure-ast (node :base) closure profile mw)
  (.visitTypeInsn mw, Opcodes/CHECKCAST, "mug/compiled/JSObject")
  (.visitInsn mw Opcodes/SWAP)
  (asm-compile-closure-ast (node :index) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asString", "(Lmug/JSPrimitive;)Ljava/lang/String;")
  (.visitInsn mw Opcodes/SWAP)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, "mug/compiled/JSObject", "set", "(Ljava/lang/String;Lmug/JSPrimitive;)V"))

(defmethod asm-compile-closure-ast :mug/add-op-expr [node closure profile mw]
  (asm-compile-closure-ast (node :left) closure profile mw)
  (asm-compile-closure-ast (node :right) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "add", "(Lmug/JSPrimitive;Lmug/JSPrimitive;)Lmug/JSPrimitive;"))

(defmethod asm-compile-closure-ast :mug/sub-op-expr [node closure profile mw]
  (.visitTypeInsn mw Opcodes/NEW, "mug/JSNumber")
  (.visitInsn mw Opcodes/DUP)
  (asm-compile-closure-ast (node :left) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
  (asm-compile-closure-ast (node :right) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
  (.visitInsn mw Opcodes/DSUB)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, "mug/JSNumber", "<init>", "(D)V"))

(defmethod asm-compile-closure-ast :mug/div-op-expr [node closure profile mw]
  (.visitTypeInsn mw Opcodes/NEW, "mug/JSNumber")
  (.visitInsn mw Opcodes/DUP)
  (asm-compile-closure-ast (node :left) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
  (asm-compile-closure-ast (node :right) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
  (.visitInsn mw Opcodes/DDIV)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, "mug/JSNumber", "<init>", "(D)V"))

(defmethod asm-compile-closure-ast :mug/mul-op-expr [node closure profile mw]
  (.visitTypeInsn mw Opcodes/NEW, "mug/JSNumber")
  (.visitInsn mw Opcodes/DUP)
  (asm-compile-closure-ast (node :left) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
  (asm-compile-closure-ast (node :right) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
  (.visitInsn mw Opcodes/DMUL)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, "mug/JSNumber", "<init>", "(D)V"))

(defmethod asm-compile-closure-ast :mug/lsh-op-expr [node closure profile mw]
  (.visitTypeInsn mw Opcodes/NEW, "mug/JSNumber")
  (.visitInsn mw Opcodes/DUP)
  (asm-compile-closure-ast (node :left) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
  (.visitInsn mw Opcodes/D2I)
  (asm-compile-closure-ast (node :right) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
  (.visitInsn mw Opcodes/D2I)
  (.visitInsn mw Opcodes/ISHL)
  (.visitInsn mw Opcodes/I2D)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, "mug/JSNumber", "<init>", "(D)V"))

(defmethod asm-compile-closure-ast :mug/eqs-op-expr [node closure profile mw]
  (asm-compile-closure-ast (node :left) closure profile mw)
  (asm-compile-closure-ast (node :right) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "testStrictEquality", "(Lmug/JSPrimitive;Lmug/JSPrimitive;)Lmug/JSBoolean;"))

(defmethod asm-compile-closure-ast :mug/neqs-op-expr [node closure profile mw]
  (asm-compile-closure-ast (node :left) closure profile mw)
  (asm-compile-closure-ast (node :right) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "testStrictInequality", "(Lmug/JSPrimitive;Lmug/JSPrimitive;)Lmug/JSBoolean;"))

(defmethod asm-compile-closure-ast :mug/lt-op-expr [node closure profile mw]
  (let [false-case (new Label) true-case (new Label)]
    (asm-compile-closure-ast (node :left) closure profile mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
    (asm-compile-closure-ast (node :right) closure profile mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
    (doto mw
      (.visitInsn Opcodes/DCMPG)
      (.visitJumpInsn Opcodes/IFGE, false-case)
      (.visitFieldInsn Opcodes/GETSTATIC, "mug/compiled/JSConstants", "TRUE", "Lmug/JSBoolean;")
      (.visitJumpInsn Opcodes/GOTO, true-case)
		  (.visitLabel false-case)
		  (.visitFieldInsn Opcodes/GETSTATIC, "mug/compiled/JSConstants", "FALSE", "Lmug/JSBoolean;")
		  (.visitLabel true-case))))

(defmethod asm-compile-closure-ast :mug/lte-op-expr [node closure profile mw]
  (let [false-case (new Label) true-case (new Label)]
    (asm-compile-closure-ast (node :left) closure profile mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
    (asm-compile-closure-ast (node :right) closure profile mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
    (doto mw
      (.visitInsn Opcodes/DCMPG)
      (.visitJumpInsn Opcodes/IFGT, false-case)
      (.visitFieldInsn Opcodes/GETSTATIC, "mug/compiled/JSConstants", "TRUE", "Lmug/JSBoolean;")
      (.visitJumpInsn Opcodes/GOTO, true-case)
		  (.visitLabel false-case)
		  (.visitFieldInsn Opcodes/GETSTATIC, "mug/compiled/JSConstants", "FALSE", "Lmug/JSBoolean;")
		  (.visitLabel true-case))))

(defmethod asm-compile-closure-ast :mug/gt-op-expr [node closure profile mw]
  (let [false-case (new Label) true-case (new Label)]
    (asm-compile-closure-ast (node :left) closure profile mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
    (asm-compile-closure-ast (node :right) closure profile mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
    (doto mw
      (.visitInsn Opcodes/DCMPG)
      (.visitJumpInsn Opcodes/IFLE, false-case)
      (.visitFieldInsn Opcodes/GETSTATIC, "mug/compiled/JSConstants", "TRUE", "Lmug/JSBoolean;")
      (.visitJumpInsn Opcodes/GOTO, true-case)
		  (.visitLabel false-case)
		  (.visitFieldInsn Opcodes/GETSTATIC, "mug/compiled/JSConstants", "FALSE", "Lmug/JSBoolean;")
		  (.visitLabel true-case))))

(defmethod asm-compile-closure-ast :mug/gte-op-expr [node closure profile mw]
  (let [false-case (new Label) true-case (new Label)]
    (asm-compile-closure-ast (node :left) closure profile mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
    (asm-compile-closure-ast (node :right) closure profile mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
    (doto mw
      (.visitInsn Opcodes/DCMPG)
      (.visitJumpInsn Opcodes/IFLT, false-case)
      (.visitFieldInsn Opcodes/GETSTATIC, "mug/compiled/JSConstants", "TRUE", "Lmug/JSBoolean;")
      (.visitJumpInsn Opcodes/GOTO, true-case)
		  (.visitLabel false-case)
		  (.visitFieldInsn Opcodes/GETSTATIC, "mug/compiled/JSConstants", "FALSE", "Lmug/JSBoolean;")
		  (.visitLabel true-case))))

(defmethod asm-compile-closure-ast :mug/neg-op-expr [node closure profile mw]
  (.visitTypeInsn mw Opcodes/NEW, "mug/JSNumber")
  (.visitInsn mw Opcodes/DUP)
  (asm-compile-closure-ast (node :expr) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
  (.visitInsn mw Opcodes/DNEG)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, "mug/JSNumber", "<init>", "(D)V"))
    
(defmethod asm-compile-closure-ast :mug/this-expr [node closure profile mw]
  (.visitVarInsn mw Opcodes/ALOAD, 1))

(defmethod asm-compile-closure-ast :mug/new-expr [node closure profile mw]
  (asm-compile-closure-ast (node :constructor) closure profile mw)
	(.visitTypeInsn mw, Opcodes/CHECKCAST, "mug/JSFunction")
	(doseq [arg (node :args)]
		(asm-compile-closure-ast arg closure profile mw))
	(doseq [_ (range (count (node :args)) 4)]
		(.visitInsn mw Opcodes/ACONST_NULL))
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, "mug/JSFunction", "instantiate", "(Lmug/JSPrimitive;Lmug/JSPrimitive;Lmug/JSPrimitive;Lmug/JSPrimitive;)Lmug/JSPrimitive;"))

;
; statements
;

(defmethod asm-compile-closure-ast :mug/block-stat [node closure profile mw]
  (doseq [stat (node :stats)]
    (asm-compile-closure-ast stat closure profile mw)))

(defmethod asm-compile-closure-ast :mug/expr-stat [node closure profile mw]
	(asm-compile-closure-ast (node :expr) closure profile mw)
	(.visitInsn mw Opcodes/POP))

(defmethod asm-compile-closure-ast :mug/ret-stat [node closure profile mw]
  (if (nil? (node :expr))
    (.visitInsn mw Opcodes/ACONST_NULL)
    (asm-compile-closure-ast (node :expr) closure profile mw))
  (.visitInsn mw Opcodes/ARETURN))

(defmethod asm-compile-closure-ast :mug/while-stat [node closure profile mw]
  (let [test-case (new Label) false-case (new Label)]
    (.visitLabel mw test-case)
    (asm-compile-closure-ast (node :expr) closure profile mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asBoolean", "(Lmug/JSPrimitive;)Z")
    (.visitJumpInsn mw, Opcodes/IFEQ, false-case)
    (asm-compile-closure-ast (node :stat) closure profile mw)
    (.visitJumpInsn mw, Opcodes/GOTO, test-case)
    (.visitLabel mw false-case)))

(defmethod asm-compile-closure-ast :mug/if-stat [node closure profile mw]
  (asm-compile-closure-ast (node :expr) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asBoolean", "(Lmug/JSPrimitive;)Z")
  (let [false-case (new Label) true-case (new Label)]
    (.visitJumpInsn mw, Opcodes/IFEQ, false-case)
    (asm-compile-closure-ast (node :then-stat) closure profile mw)
    (.visitJumpInsn mw, Opcodes/GOTO, true-case)
    (.visitLabel mw false-case)
    (when (not (nil? (node :else-stat)))
      (asm-compile-closure-ast (node :else-stat) closure profile mw))
    (.visitLabel mw true-case)))

(defmethod asm-compile-closure-ast :mug/for-in-stat [node closure profile mw]
  (let [test-label (new Label) body-label (new Label)]
    ; assign variable
	  (.visitVarInsn mw Opcodes/ALOAD, 6)
    (.visitTypeInsn mw Opcodes/NEW, "mug/JSNumber")
    (.visitInsn mw Opcodes/DUP)
	  (asm-compile-closure-ast (node :from) closure profile mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
    (.visitMethodInsn mw Opcodes/INVOKESPECIAL, "mug/JSNumber", "<init>", "(D)V")
		(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (asm-compile-closure-scope-qn closure profile), (str "set_" (node :value)), "(Lmug/JSPrimitive;)V")
    ; label
    (.visitJumpInsn mw Opcodes/GOTO, test-label)
  
    ; body
    (.visitLabel mw body-label)
    (asm-compile-closure-ast (node :stat) closure profile mw)
    
    ; increment
		(.visitVarInsn mw Opcodes/ALOAD, 6)
		(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (asm-compile-closure-scope-qn closure profile), (str "get_" (node :value)), "()Lmug/JSPrimitive;")
    (.visitTypeInsn mw Opcodes/CHECKCAST, "mug/JSNumber")
    (.visitInsn mw Opcodes/DUP)
    (.visitFieldInsn mw Opcodes/GETFIELD, "mug/JSNumber", "value", "D")
    (asm-compile-closure-ast (node :by) closure profile mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
    (.visitInsn mw Opcodes/DADD)
    (.visitFieldInsn mw Opcodes/PUTFIELD, "mug/JSNumber", "value", "D")
    
    ; condition
    (.visitLabel mw test-label)
		(.visitVarInsn mw Opcodes/ALOAD, 6)
		(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (asm-compile-closure-scope-qn closure profile), (str "get_" (node :value)), "()Lmug/JSPrimitive;")
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
    (asm-compile-closure-ast (node :to) closure profile mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
    (.visitInsn mw Opcodes/DCMPG)
    (.visitJumpInsn mw Opcodes/IFLT, body-label)))

(defmethod asm-compile-closure-ast :mug/class-stat [node closure profile mw]
  (let [i (index-of (profile :closures) ((node :constructor) :closure))
        qn (str "mug/compiled/JSClosure_" i)]
    ; create closure instance
    (.visitTypeInsn mw Opcodes/NEW, qn)
		(.visitInsn mw Opcodes/DUP)
		(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn, "<init>", "()V"))

    ; set environment variable
		(.visitInsn mw Opcodes/DUP)
    (.visitVarInsn mw Opcodes/ALOAD, 6)
	  (.visitInsn mw Opcodes/SWAP)
    ;;;[TODO] scope might be any scope
		(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (asm-compile-closure-scope-qn closure profile), (str "set_" (node :name)), "(Lmug/JSPrimitive;)V")

    ; set prototype
    (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, "mug/compiled/JSObject", "get_prototype", "()Lmug/JSPrimitive;")
    (doseq [[k v] (node :prototype)]
      (.visitInsn mw Opcodes/DUP)
      (.visitTypeInsn mw Opcodes/CHECKCAST, "mug/compiled/JSObject")
      (asm-compile-closure-ast v closure profile mw)
      (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, "mug/compiled/JSObject", (str "set_" k), "(Lmug/JSPrimitive;)V"))
    (.visitInsn mw Opcodes/POP))

;
; closure
;

(defn asm-compile-closure-init [closure profile cw]
  (let [sig (asm-compile-closure-sig closure profile)
        mw (.visitMethod cw, Opcodes/ACC_PUBLIC, "<init>", sig, nil, nil)
        name (str "mug/compiled/JSClosure_" (index-of (profile :closures) closure))]
		(.visitCode mw)
		(.visitVarInsn mw Opcodes/ALOAD, 0)
		(.visitMethodInsn mw Opcodes/INVOKESPECIAL, "mug/JSFunction", "<init>", "()V")
 
    (doseq [[i scope] (index ((profile :scopes) closure))]
      (.visitVarInsn mw Opcodes/ALOAD, 0)
      (.visitVarInsn mw Opcodes/ALOAD, (+ i 1))
      (.visitFieldInsn mw Opcodes/PUTFIELD, name, (str "SCOPE_" (index-of (profile :closures) scope)),
        (str "Lmug/compiled/JSScope_" (index-of (profile :closures) scope) ";")))
    
		(.visitInsn mw Opcodes/RETURN)
		(.visitMaxs mw 1, 1)
		(.visitEnd mw)))
		
(defn asm-compile-closure-method [closure profile cw]
	(let [mw (.visitMethod cw, Opcodes/ACC_PUBLIC, "invoke", "(Lmug/compiled/JSObject;Lmug/JSPrimitive;Lmug/JSPrimitive;Lmug/JSPrimitive;Lmug/JSPrimitive;)Lmug/JSPrimitive;", nil, (into-array ["java/lang/Exception"]))]
		(.visitCode mw)
		
		; create scope object
		(doto mw
			(.visitTypeInsn Opcodes/NEW, (asm-compile-closure-scope-qn closure profile))
			(.visitInsn Opcodes/DUP)
			(.visitMethodInsn Opcodes/INVOKESPECIAL, (asm-compile-closure-scope-qn closure profile), "<init>", "()V")
			(.visitVarInsn Opcodes/ASTORE, 6))
		
		; initialize arguments
		(doseq [[i arg] (index (closure :args))]
			(doto mw
				(.visitVarInsn Opcodes/ALOAD, 6)
				(.visitVarInsn Opcodes/ALOAD, (+ i 2))
				(.visitMethodInsn Opcodes/INVOKEVIRTUAL, (asm-compile-closure-scope-qn closure profile), (str "set_" arg), "(Lmug/JSPrimitive;)V")))
    ; initialize self
    (when (not (nil? (closure :name)))
      (doto mw
				(.visitVarInsn Opcodes/ALOAD, 6)
				(.visitVarInsn Opcodes/ALOAD, 0)
				(.visitMethodInsn Opcodes/INVOKEVIRTUAL, (asm-compile-closure-scope-qn closure profile), (str "set_" (closure :name)), "(Lmug/JSPrimitive;)V")))
		
		; compile body
		(doseq [stat (closure :stats)]
			(asm-compile-closure-ast stat closure profile mw))
		
		; catch-all return
		(doto mw
			(.visitInsn Opcodes/ACONST_NULL)
			(.visitInsn Opcodes/ARETURN))

		; finish closure
		(.visitMaxs mw 0, 0)
		(.visitEnd mw)))

(defn asm-compile-closure-fields [closure profile cw]
  ; scopes
  (doseq [scope ((profile :scopes) closure)]
    (let [i (index-of (profile :closures) scope)]
      (.visitEnd (.visitField cw, 0, (str "SCOPE_" i), (str "Lmug/compiled/JSScope_" i ";"), nil, nil)))))

(defn asm-compile-closure-classes [profile]
	(into {}
		(map-indexed (fn [i closure] 
			(let [name (str "mug/compiled/JSClosure_" i)
				cw (new ClassWriter ClassWriter/COMPUTE_MAXS)]
				(.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), name, nil, "mug/JSFunction", nil)
				(asm-compile-closure-init closure profile cw)
				(asm-compile-closure-method closure profile cw)
        (asm-compile-closure-fields closure profile cw)
				(.visitEnd cw)
				[(str "JSClosure_" i ".class") (.toByteArray cw)]))
   (profile :closures))))