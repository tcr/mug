(ns mug
  (:use mug))

(import '(org.objectweb.asm ClassWriter Opcodes Label))
(import 'java.io.FileOutputStream)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Compile java from AST
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn write-file [path bytes]
	(let [fos (new FileOutputStream path)]
		(.write fos bytes)
		(.close fos)))
		
(defn index [coll]
	(map vector (iterate inc 0) coll))

;(defn map-indexed [callback coll]
;	(map (fn [[k v]] (callback k v)) (index coll)))

(defn index-of [s x]
	((zipmap (vec s) (iterate inc 0)) x))

;
; context manipulation	
;

(defn new-asm-ctx [] {
	:closures #{} :structs #{} :numbers #{} :strings #{} :accessors #{}
})

(defn asm-ctx-add [k v ctx]
	(if (contains? v (ctx k)) ctx (merge ctx {k (merge (ctx k) v)})))

(defn asm-ctx-add-coll [k coll ctx]
	(loop [ctx ctx coll coll]
		(if (empty? coll)
      ctx
			(recur (asm-ctx-add k (first coll) ctx) (next coll)))))

; deprecated
;(defn asm-ctx-set [k v ctx]
;	(if (contains? v (ctx k)) ctx (merge ctx {k (assoc (ctx k) v (count (keys (ctx k))))})))
;(defn asm-ctx-get [k ctx]
;	(first (ctx k)))
;(defn asm-ctx-push [k v ctx]
;	(merge ctx {k (cons v (ctx k))}))
;(defn asm-ctx-pull [k ctx]
;	(let [v (asm-ctx-get k ctx)]
;		(merge ctx {k (rest (ctx k))})
;		v))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; analysis
;

(defmulti asm-analyze-ast (fn [& args] (:type (first args))))
	
(defn asm-analyze-ast-coll [coll ctx]
	(if (first coll)
		(loop [ctx (asm-analyze-ast (first coll) ctx) coll (next coll)]
			(if coll
				(recur (asm-analyze-ast (first coll) ctx) (next coll))
				ctx))
		ctx))
		
;
; literals
;

(defmethod asm-analyze-ast ::num-literal [node ctx]
	(asm-ctx-add :numbers (node :value) ctx))

(defmethod asm-analyze-ast ::str-literal [node ctx]
	(asm-ctx-add :strings (node :value) ctx))

(defmethod asm-analyze-ast ::null-literal [node ctx]
	ctx)

(defmethod asm-analyze-ast ::func-literal [node ctx]
	(asm-analyze-ast (node :closure) ctx))

;
; expressions
;

(defmethod asm-analyze-ast ::binary-op-expr [node ctx]
	(->> ctx
		(asm-analyze-ast (node :left) ,,,)
		(asm-analyze-ast (node :right) ,,,)))

(defmethod asm-analyze-ast ::scope-ref-expr [node ctx]
	ctx)

(defmethod asm-analyze-ast ::static-ref-expr [node ctx]
	(->> (asm-ctx-add :accessors (node :value) ctx)
		(asm-analyze-ast (node :base) ,,,)))

(defmethod asm-analyze-ast ::static-method-call-expr [node ctx]
	(->> (asm-ctx-add :accessors (node :value) ctx)
		(asm-analyze-ast (node :base) ,,,)
    (asm-analyze-ast-coll (node :args) ,,,)))

(defmethod asm-analyze-ast ::scope-assign-expr [node ctx]
	(->> ctx
		(asm-analyze-ast (node :expr) ,,,)))

(defmethod asm-analyze-ast ::static-assign-expr [node ctx]
	(->> (asm-ctx-add :accessors (node :value) ctx)
		(asm-analyze-ast (node :base) ,,,)
    (asm-analyze-ast (node :expr) ,,,)))

(defmethod asm-analyze-ast ::new-expr [node ctx]
	(->> ctx
		(asm-analyze-ast (node :constructor) ,,,)
    (asm-analyze-ast-coll (node :args) ,,,)))

(defmethod asm-analyze-ast ::this-expr [node ctx]
	ctx)
	
(defmethod asm-analyze-ast ::call-expr [node ctx]
	(->> ctx
		(asm-analyze-ast (node :ref) ,,,)
		(asm-analyze-ast-coll (node :args) ,,,)))

;
; statements
;

(defmethod asm-analyze-ast ::class-stat [node ctx]
  (->> ctx
    (asm-ctx-add :structs (set (keys (node :prototype))) ,,,)
    (asm-ctx-add-coll :accessors (keys (or (node :prototype) {})) ,,,)
    (asm-analyze-ast ((node :constructor) :closure) ,,,)
    (asm-analyze-ast-coll (vals (or (node :prototype) {})) ,,,)
    (asm-analyze-ast-coll (vals (or (node :static) {})) ,,,)))

(defmethod asm-analyze-ast ::expr-stat [node ctx]
	(asm-analyze-ast (node :expr) ctx))

(defmethod asm-analyze-ast ::block-stat [node ctx]
	(asm-analyze-ast-coll (node :stats) ctx))

(defmethod asm-analyze-ast ::ret-stat [node ctx]
	(if (nil? (node :expr))
    ctx
    (asm-analyze-ast (node :expr) ctx)))

(defmethod asm-analyze-ast ::if-stat [node ctx]
	(let [ctx (->> ctx
		(asm-analyze-ast (node :expr) ,,,)
		(asm-analyze-ast (node :then-stat) ,,,))]
    (if (nil? (node :else-stat))
      ctx
      (asm-analyze-ast (node :else-stat) ctx))))

(defmethod asm-analyze-ast ::while-stat [node ctx]
	(->> ctx
		(asm-analyze-ast (node :expr) ,,,)
		(asm-analyze-ast (node :stat) ,,,)))

(defmethod asm-analyze-ast ::for-in-stat [node ctx]
	(->> ctx
		(asm-analyze-ast (node :from) ,,,)
		(asm-analyze-ast (node :to) ,,,)
    (asm-analyze-ast (node :by) ,,,)
    (asm-analyze-ast (node :stat) ,,,)))

;
; closure
;

(defmethod asm-analyze-ast ::closure [node ctx]
	(->> ctx
		(asm-ctx-add :closures node ,,,)
		(asm-analyze-ast-coll (node :stats) ,,,)))
	
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; closures
;

(defmulti asm-compile-closure-ast (fn [& args] (:type (first args))))

(defn asm-compile-closure-scope-qn [closure ctx]
  (str "mug/compiled/JSScope_" (index-of (ctx :closures) closure)))

;
; literals
;

(defmethod asm-compile-closure-ast ::null-literal [node closure ctx mw]
  (.visitInsn mw Opcodes/ACONST_NULL))

(defmethod asm-compile-closure-ast ::num-literal [node closure ctx mw]
	(.visitFieldInsn mw Opcodes/GETSTATIC, "mug/compiled/JSConstants" (str "NUM_" (index-of (ctx :numbers) (node :value))) "Lmug/JSNumber;"))

(defmethod asm-compile-closure-ast ::str-literal [node closure ctx mw]
	(.visitFieldInsn mw Opcodes/GETSTATIC "mug/compiled/JSConstants" (str "STR_" (index-of (ctx :strings) (node :value))) "Lmug/JSString;"))

(defmethod asm-compile-closure-ast ::func-literal [node closure ctx mw]
  (let [i (index-of (ctx :closures) (node :closure))
        qn (str "mug/compiled/JSClosure_" i)]
    ; create closure instance
    (.visitTypeInsn mw Opcodes/NEW, qn)
		(.visitInsn mw Opcodes/DUP)
		(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn, "<init>", "()V")))

;
; expressions
;

(defmethod asm-compile-closure-ast ::scope-ref-expr [node closure ctx mw]
	(doto mw
		(.visitVarInsn Opcodes/ALOAD, 6)
		(.visitMethodInsn Opcodes/INVOKEVIRTUAL, (asm-compile-closure-scope-qn closure ctx), (str "get_" (node :value)), "()Lmug/JSPrimitive;")))

(defmethod asm-compile-closure-ast ::static-ref-expr [node closure ctx mw]
  (asm-compile-closure-ast (node :base) closure ctx mw)
	(doto mw
    (.visitTypeInsn Opcodes/CHECKCAST, "mug/compiled/JSObject")
		(.visitMethodInsn Opcodes/INVOKEVIRTUAL, "mug/compiled/JSObject", (str "get_" (node :value)), "()Lmug/JSPrimitive;")))

(defmethod asm-compile-closure-ast ::static-method-call-expr [node closure ctx mw]
  (asm-compile-closure-ast (node :base) closure ctx mw)
	(doto mw
    (.visitTypeInsn Opcodes/CHECKCAST, "mug/compiled/JSObject")
    (.visitInsn Opcodes/DUP)
		(.visitMethodInsn Opcodes/INVOKEVIRTUAL, "mug/compiled/JSObject", (str "get_" (node :value)), "()Lmug/JSPrimitive;")
	  (.visitTypeInsn Opcodes/CHECKCAST, "mug/JSFunction")
    (.visitInsn Opcodes/SWAP))
	(doseq [arg (node :args)]
		(asm-compile-closure-ast arg closure ctx mw))
	(doseq [_ (range (count (node :args)) 4)]
		(.visitInsn mw Opcodes/ACONST_NULL))
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, "mug/JSFunction", "invoke", "(Lmug/compiled/JSObject;Lmug/JSPrimitive;Lmug/JSPrimitive;Lmug/JSPrimitive;Lmug/JSPrimitive;)Lmug/JSPrimitive;"))

(defmethod asm-compile-closure-ast ::call-expr [node closure ctx mw]
	(asm-compile-closure-ast (node :ref) closure ctx mw)
	(.visitTypeInsn mw, Opcodes/CHECKCAST, "mug/JSFunction")
	(.visitInsn mw Opcodes/ACONST_NULL)
	(doseq [arg (node :args)]
		(asm-compile-closure-ast arg closure ctx mw))
	(doseq [_ (range (count (node :args)) 4)]
		(.visitInsn mw Opcodes/ACONST_NULL))
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, "mug/JSFunction", "invoke", "(Lmug/compiled/JSObject;Lmug/JSPrimitive;Lmug/JSPrimitive;Lmug/JSPrimitive;Lmug/JSPrimitive;)Lmug/JSPrimitive;"))

(defmethod asm-compile-closure-ast ::scope-assign-expr [node closure ctx mw]
	(asm-compile-closure-ast (node :expr) closure ctx mw)
  (.visitInsn mw Opcodes/DUP)
  (.visitVarInsn mw Opcodes/ALOAD, 6)
  (.visitInsn mw Opcodes/SWAP)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (asm-compile-closure-scope-qn closure ctx), (str "set_" (node :value)), "(Lmug/JSPrimitive;)V"))

(defmethod asm-compile-closure-ast ::static-assign-expr [node closure ctx mw]
	(asm-compile-closure-ast (node :expr) closure ctx mw)
  (.visitInsn mw Opcodes/DUP)
  (asm-compile-closure-ast (node :base) closure ctx mw)
  (.visitTypeInsn mw, Opcodes/CHECKCAST, "mug/compiled/JSObject")
  (.visitInsn mw Opcodes/SWAP)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, "mug/compiled/JSObject", (str "set_" (node :value)), "(Lmug/JSPrimitive;)V"))

(defmethod asm-compile-closure-ast ::add-op-expr [node closure ctx mw]
  (asm-compile-closure-ast (node :left) closure ctx mw)
  (asm-compile-closure-ast (node :right) closure ctx mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "add", "(Lmug/JSPrimitive;Lmug/JSPrimitive;)Lmug/JSPrimitive;"))

(defmethod asm-compile-closure-ast ::sub-op-expr [node closure ctx mw]
  (.visitTypeInsn mw Opcodes/NEW, "mug/JSNumber")
  (.visitInsn mw Opcodes/DUP)
  (asm-compile-closure-ast (node :left) closure ctx mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
  (asm-compile-closure-ast (node :right) closure ctx mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
  (.visitInsn mw Opcodes/DSUB)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, "mug/JSNumber", "<init>", "(D)V"))

(defmethod asm-compile-closure-ast ::div-op-expr [node closure ctx mw]
  (.visitTypeInsn mw Opcodes/NEW, "mug/JSNumber")
  (.visitInsn mw Opcodes/DUP)
  (asm-compile-closure-ast (node :left) closure ctx mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
  (asm-compile-closure-ast (node :right) closure ctx mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
  (.visitInsn mw Opcodes/DDIV)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, "mug/JSNumber", "<init>", "(D)V"))

(defmethod asm-compile-closure-ast ::eqs-op-expr [node closure ctx mw]
  (asm-compile-closure-ast (node :left) closure ctx mw)
  (asm-compile-closure-ast (node :right) closure ctx mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "testStrictEquality", "(Lmug/JSPrimitive;Lmug/JSPrimitive;)Lmug/JSBoolean;"))

(defmethod asm-compile-closure-ast ::neqs-op-expr [node closure ctx mw]
  (asm-compile-closure-ast (node :left) closure ctx mw)
  (asm-compile-closure-ast (node :right) closure ctx mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "testStrictInequality", "(Lmug/JSPrimitive;Lmug/JSPrimitive;)Lmug/JSBoolean;"))

(defmethod asm-compile-closure-ast ::lt-op-expr [node closure ctx mw]
  (let [false-case (new Label) true-case (new Label)]
    (asm-compile-closure-ast (node :left) closure ctx mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
    (asm-compile-closure-ast (node :right) closure ctx mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
    (doto mw
      (.visitInsn Opcodes/DCMPG)
      (.visitJumpInsn Opcodes/IFGE, false-case)
      (.visitFieldInsn Opcodes/GETSTATIC, "mug/compiled/JSConstants", "TRUE", "Lmug/JSBoolean;")
      (.visitJumpInsn Opcodes/GOTO, true-case)
		  (.visitLabel false-case)
		  (.visitFieldInsn Opcodes/GETSTATIC, "mug/compiled/JSConstants", "FALSE", "Lmug/JSBoolean;")
		  (.visitLabel true-case))))

(defmethod asm-compile-closure-ast ::this-expr [node closure ctx mw]
  (.visitVarInsn mw Opcodes/ALOAD, 1))

(defmethod asm-compile-closure-ast ::new-expr [node closure ctx mw]
  (asm-compile-closure-ast (node :constructor) closure ctx mw)
	(.visitTypeInsn mw, Opcodes/CHECKCAST, "mug/JSFunction")
	(doseq [arg (node :args)]
		(asm-compile-closure-ast arg closure ctx mw))
	(doseq [_ (range (count (node :args)) 4)]
		(.visitInsn mw Opcodes/ACONST_NULL))
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, "mug/JSFunction", "instantiate", "(Lmug/JSPrimitive;Lmug/JSPrimitive;Lmug/JSPrimitive;Lmug/JSPrimitive;)Lmug/JSPrimitive;"))

;
; statements
;

(defmethod asm-compile-closure-ast ::block-stat [node closure ctx mw]
  (doseq [stat (node :stats)]
    (asm-compile-closure-ast stat closure ctx mw)))

(defmethod asm-compile-closure-ast ::expr-stat [node closure ctx mw]
	(asm-compile-closure-ast (node :expr) closure ctx mw)
	(.visitInsn mw Opcodes/POP))

(defmethod asm-compile-closure-ast ::ret-stat [node closure ctx mw]
  (if (nil? (node :expr))
    (.visitInsn mw Opcodes/ACONST_NULL)
    (asm-compile-closure-ast (node :expr) closure ctx mw))
  (.visitInsn mw Opcodes/ARETURN))

(defmethod asm-compile-closure-ast ::while-stat [node closure ctx mw]
  (let [test-case (new Label) false-case (new Label)]
    (.visitLabel mw test-case)
    (asm-compile-closure-ast (node :expr) closure ctx mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asBoolean", "(Lmug/JSPrimitive;)Z")
    (.visitJumpInsn mw, Opcodes/IFEQ, false-case)
    (asm-compile-closure-ast (node :stat) closure ctx mw)
    (.visitJumpInsn mw, Opcodes/GOTO, test-case)
    (.visitLabel mw false-case)))

(defmethod asm-compile-closure-ast ::if-stat [node closure ctx mw]
  (asm-compile-closure-ast (node :expr) closure ctx mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asBoolean", "(Lmug/JSPrimitive;)Z")
  (let [false-case (new Label) true-case (new Label)]
    (.visitJumpInsn mw, Opcodes/IFEQ, false-case)
    (asm-compile-closure-ast (node :then-stat) closure ctx mw)
    (.visitJumpInsn mw, Opcodes/GOTO, true-case)
    (.visitLabel mw false-case)
    (when (not (nil? (node :else-stat)))
      (asm-compile-closure-ast (node :else-stat) closure ctx mw))
    (.visitLabel mw true-case)))

(defmethod asm-compile-closure-ast ::for-in-stat [node closure ctx mw]
  (let [test-label (new Label) body-label (new Label)]
    ; assign variable
	  (.visitVarInsn mw Opcodes/ALOAD, 6)
    (.visitTypeInsn mw Opcodes/NEW, "mug/JSNumber")
    (.visitInsn mw Opcodes/DUP)
	  (asm-compile-closure-ast (node :from) closure ctx mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
    (.visitMethodInsn mw Opcodes/INVOKESPECIAL, "mug/JSNumber", "<init>", "(D)V")
		(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (asm-compile-closure-scope-qn closure ctx), (str "set_" (node :value)), "(Lmug/JSPrimitive;)V")
    ; label
    (.visitJumpInsn mw Opcodes/GOTO, test-label)
  
    ; body
    (.visitLabel mw body-label)
    (asm-compile-closure-ast (node :stat) closure ctx mw)
    
    ; increment
		(.visitVarInsn mw Opcodes/ALOAD, 6)
		(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (asm-compile-closure-scope-qn closure ctx), (str "get_" (node :value)), "()Lmug/JSPrimitive;")
    (.visitTypeInsn mw Opcodes/CHECKCAST, "mug/JSNumber")
    (.visitInsn mw Opcodes/DUP)
    (.visitFieldInsn mw Opcodes/GETFIELD, "mug/JSNumber", "value", "D")
    (asm-compile-closure-ast (node :by) closure ctx mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
    (.visitInsn mw Opcodes/DADD)
    (.visitFieldInsn mw Opcodes/PUTFIELD, "mug/JSNumber", "value", "D")
    
    ; condition
    (.visitLabel mw test-label)
		(.visitVarInsn mw Opcodes/ALOAD, 6)
		(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (asm-compile-closure-scope-qn closure ctx), (str "get_" (node :value)), "()Lmug/JSPrimitive;")
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
    (asm-compile-closure-ast (node :to) closure ctx mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
    (.visitInsn mw Opcodes/DCMPG)
    (.visitJumpInsn mw Opcodes/IFLT, body-label)))

(defmethod asm-compile-closure-ast ::class-stat [node closure ctx mw]
  (let [i (index-of (ctx :closures) ((node :constructor) :closure))
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
		(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (asm-compile-closure-scope-qn closure ctx), (str "set_" (node :name)), "(Lmug/JSPrimitive;)V")

    ; set prototype
    (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, "mug/compiled/JSObject", "get_prototype", "()Lmug/JSPrimitive;")
    (doseq [[k v] (node :prototype)]
      (.visitInsn mw Opcodes/DUP)
      (.visitTypeInsn mw Opcodes/CHECKCAST, "mug/compiled/JSObject")
      (asm-compile-closure-ast v closure ctx mw)
      (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, "mug/compiled/JSObject", (str "set_" k), "(Lmug/JSPrimitive;)V"))
    (.visitInsn mw Opcodes/POP))

;
; closure
;

(defn asm-compile-closure-init [closure ctx cw]
	(doto (.visitMethod cw, Opcodes/ACC_PUBLIC, "<init>", "()V", nil, nil)
		(.visitCode)
		(.visitVarInsn Opcodes/ALOAD, 0)
		(.visitMethodInsn Opcodes/INVOKESPECIAL, "mug/JSFunction", "<init>", "()V")
		(.visitInsn Opcodes/RETURN)
		(.visitMaxs 1, 1)
		(.visitEnd)))
		
(defn asm-compile-closure-method [closure ctx cw]
	(let [mw (.visitMethod cw, Opcodes/ACC_PUBLIC, "invoke", "(Lmug/compiled/JSObject;Lmug/JSPrimitive;Lmug/JSPrimitive;Lmug/JSPrimitive;Lmug/JSPrimitive;)Lmug/JSPrimitive;", nil, (into-array ["java/lang/Exception"]))]
		(.visitCode mw)
		
		; create scope object
		(doto mw
			(.visitTypeInsn Opcodes/NEW, (asm-compile-closure-scope-qn closure ctx))
			(.visitInsn Opcodes/DUP)
			(.visitMethodInsn Opcodes/INVOKESPECIAL, (asm-compile-closure-scope-qn closure ctx), "<init>", "()V")
			(.visitVarInsn Opcodes/ASTORE, 6))
		
		; initialize arguments
		(doseq [[i arg] (index (closure :args))]
			(doto mw
				(.visitVarInsn Opcodes/ALOAD, 6)
				(.visitVarInsn Opcodes/ALOAD, (+ i 2))
				(.visitMethodInsn Opcodes/INVOKEVIRTUAL, (asm-compile-closure-scope-qn closure ctx), (str "set_" arg), "(Lmug/JSPrimitive;)V")))
		
		; compile body
		(doseq [stat (closure :stats)]
			(asm-compile-closure-ast stat closure ctx mw))
		
		; catch-all return
		(doto mw
			(.visitInsn Opcodes/ACONST_NULL)
			(.visitInsn Opcodes/ARETURN))

		; finish closure
		(.visitMaxs mw 0, 0)
		(.visitEnd mw)))

(defn asm-compile-closure-classes [ctx]
	(into {}
		(map-indexed (fn [i closure] 
			(let [name (str "mug/compiled/JSClosure_" i)
				cw (new ClassWriter ClassWriter/COMPUTE_MAXS)]
				(.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), name, nil, "mug/JSFunction", nil)
				(asm-compile-closure-init closure ctx cw)
				(asm-compile-closure-method closure ctx cw)
				(.visitEnd cw)
				[(str "JSClosure_" i ".class") (.toByteArray cw)]))
   (ctx :closures))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; constants
;

(defn asm-compile-constants-fields [ctx cw]
	; undefined
	(.visitEnd (.visitField cw, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), "UNDEFINED", "Lmug/JSUndefined;", nil, nil))

	; booleans
	(.visitEnd (.visitField cw, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), "TRUE", "Lmug/JSBoolean;", nil, nil))
	(.visitEnd (.visitField cw, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), "FALSE", "Lmug/JSBoolean;", nil, nil))

	; strings
	(doseq [v (vec (ctx :strings))]
			(.visitEnd (.visitField cw, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), (str "STR_" (index-of (ctx :strings) v)), "Lmug/JSString;", nil, nil)))
 
 	; numbers
	(doseq [v (vec (ctx :numbers))]
			(.visitEnd (.visitField cw, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), (str "NUM_" (index-of (ctx :numbers) v)), "Lmug/JSNumber;", nil, nil))))

(defn asm-compile-constants-clinit [ctx cw]
	(let [mv (.visitMethod cw, Opcodes/ACC_STATIC, "<clinit>", "()V", nil, nil)]
		(.visitCode mv)

		; undefined
		(doto mv
			(.visitTypeInsn Opcodes/NEW "mug/JSUndefined")
			(.visitInsn Opcodes/DUP)
			(.visitMethodInsn Opcodes/INVOKESPECIAL, "mug/JSUndefined", "<init>", "()V")
			(.visitFieldInsn Opcodes/PUTSTATIC, "mug/compiled/JSConstants", "UNDEFINED", "Lmug/JSUndefined;"))

		; booleans
		(doto mv
			(.visitTypeInsn Opcodes/NEW "mug/JSBoolean")
			(.visitInsn Opcodes/DUP)
			(.visitInsn Opcodes/ICONST_1)
			(.visitMethodInsn Opcodes/INVOKESPECIAL, "mug/JSBoolean", "<init>", "(Z)V")
			(.visitFieldInsn Opcodes/PUTSTATIC, "mug/compiled/JSConstants", "TRUE", "Lmug/JSBoolean;"))
		(doto mv
			(.visitTypeInsn Opcodes/NEW "mug/JSBoolean")
			(.visitInsn Opcodes/DUP)
			(.visitInsn Opcodes/ICONST_0)
			(.visitMethodInsn Opcodes/INVOKESPECIAL, "mug/JSBoolean", "<init>", "(Z)V")
			(.visitFieldInsn Opcodes/PUTSTATIC, "mug/compiled/JSConstants", "FALSE", "Lmug/JSBoolean;"))

		; strings
		(doseq [v (vec (ctx :strings))]
			(doto mv
				(.visitTypeInsn Opcodes/NEW "mug/JSString")
				(.visitInsn Opcodes/DUP)
				(.visitLdcInsn v)
				(.visitMethodInsn Opcodes/INVOKESPECIAL, "mug/JSString", "<init>", "(Ljava/lang/String;)V")
				(.visitFieldInsn Opcodes/PUTSTATIC, "mug/compiled/JSConstants", (str "STR_" (index-of (ctx :strings) v)), "Lmug/JSString;")))

		; numbers
		(doseq [v (vec (ctx :numbers))]
			(doto mv
				(.visitTypeInsn Opcodes/NEW "mug/JSNumber")
				(.visitInsn Opcodes/DUP)
				(.visitLdcInsn (new Double (double v)))
				(.visitMethodInsn Opcodes/INVOKESPECIAL, "mug/JSNumber", "<init>", "(D)V")
				(.visitFieldInsn Opcodes/PUTSTATIC, "mug/compiled/JSConstants", (str "NUM_" (index-of (ctx :numbers) v)), "Lmug/JSNumber;")))

		(doto mv
			(.visitInsn Opcodes/RETURN)
			(.visitMaxs 4, 0)
			(.visitEnd))))

(defn asm-compile-constants-class [ctx]
	(let [cw (new ClassWriter ClassWriter/COMPUTE_MAXS)]
		(.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), "mug/compiled/JSConstants", nil, "java/lang/Object", nil)
		(asm-compile-constants-fields ctx cw)
		(asm-compile-constants-clinit ctx cw)
		(.visitEnd cw)
		(.toByteArray cw)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; object
;

(defn asm-compile-object-methods [name accessors cw]
  (doseq [k (vec accessors)]
		(doto (.visitMethod cw, Opcodes/ACC_PUBLIC, (str "get_" k), "()Lmug/JSPrimitive;", nil, nil)
			(.visitCode)
			(.visitVarInsn Opcodes/ALOAD, 0)
      (.visitLdcInsn k)
			(.visitMethodInsn Opcodes/INVOKEVIRTUAL, "mug/compiled/JSObject", "get", "(Ljava/lang/String;)Lmug/JSPrimitive;")
			(.visitInsn Opcodes/ARETURN)
      (.visitMaxs 1, 1)
			(.visitEnd))
		(doto (.visitMethod cw, Opcodes/ACC_PUBLIC, (str "set_" k), "(Lmug/JSPrimitive;)V", nil, nil)
			(.visitCode)
			(.visitVarInsn Opcodes/ALOAD, 0)
      (.visitLdcInsn k)
      (.visitVarInsn Opcodes/ALOAD, 1)
			(.visitMethodInsn Opcodes/INVOKEVIRTUAL, "mug/compiled/JSObject", "set", "(Ljava/lang/String;Lmug/JSPrimitive;)V")
			(.visitInsn Opcodes/RETURN)
      (.visitMaxs 1, 1)
			(.visitEnd))))

(defn asm-compile-object-init [name cw]
	(doto (.visitMethod cw, Opcodes/ACC_PUBLIC, "<init>", "()V", nil, nil)
		(.visitCode)
		(.visitVarInsn Opcodes/ALOAD, 0)
		(.visitMethodInsn Opcodes/INVOKESPECIAL, "mug/JSObjectBase", "<init>", "()V")
		(.visitInsn Opcodes/RETURN)
    (.visitMaxs 1, 1)
		(.visitEnd))
 (doto (.visitMethod cw, Opcodes/ACC_PUBLIC, "<init>", "(Lmug/compiled/JSObject;)V", nil, nil)
		(.visitCode)
		(.visitVarInsn Opcodes/ALOAD, 0)
		(.visitMethodInsn Opcodes/INVOKESPECIAL, "mug/JSObjectBase", "<init>", "()V")
    (.visitVarInsn Opcodes/ALOAD, 0)
    (.visitVarInsn Opcodes/ALOAD, 1)
    (.visitFieldInsn Opcodes/PUTFIELD, "mug/compiled/JSObject", "__proto__", "Lmug/compiled/JSObject;")
		(.visitInsn Opcodes/RETURN)
    (.visitMaxs 1, 1)
		(.visitEnd)))

(defn asm-compile-object-class [ctx]
		(let [cw (new ClassWriter ClassWriter/COMPUTE_MAXS)
			name "mug/compiled/JSObject"]
			(.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), name, nil, "mug/JSObjectBase", nil)
			(asm-compile-object-methods name (ctx :accessors) cw)
			(asm-compile-object-init name cw)
			(.visitEnd cw)
			(.toByteArray cw)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; scopes
;

(defn asm-compile-scope-fields [name scope cw]
	(doseq [var scope]
			(.visitEnd (.visitField cw, 0, (str "_" var), "Lmug/JSPrimitive;", nil, nil))))

(defn asm-compile-scope-methods [name scope cw]
	(doseq [var scope]
		(doto (.visitMethod cw, Opcodes/ACC_PUBLIC, (str "get_" var), "()Lmug/JSPrimitive;", nil, nil)
			(.visitCode)
			(.visitVarInsn Opcodes/ALOAD, 0)
			(.visitFieldInsn Opcodes/GETFIELD, name, (str "_" var), "Lmug/JSPrimitive;");
			(.visitInsn Opcodes/ARETURN)
			(.visitMaxs 1, 1)
			(.visitEnd))
		(doto (.visitMethod cw, Opcodes/ACC_PUBLIC, (str "set_" var), "(Lmug/JSPrimitive;)V", nil, nil)
			(.visitCode)
			(.visitVarInsn Opcodes/ALOAD, 0)
			(.visitVarInsn Opcodes/ALOAD, 1)
			(.visitFieldInsn Opcodes/PUTFIELD, name, (str "_" var), "Lmug/JSPrimitive;");
			(.visitInsn Opcodes/RETURN)
			(.visitMaxs 2, 2)
			(.visitEnd))))

(defn asm-compile-scope-init [name scope cw]
	(doto (.visitMethod cw, 0, "<init>", "()V", nil, nil)
		(.visitCode)
		(.visitVarInsn Opcodes/ALOAD, 0)
		(.visitMethodInsn Opcodes/INVOKESPECIAL, "java/lang/Object", "<init>", "()V")
		(.visitInsn Opcodes/RETURN)
		(.visitMaxs 1, 1)
		(.visitEnd)))

(defn asm-compile-scope-classes [ctx]
	(into {}
		(map-indexed (fn [i closure] 
			(let [cw (new ClassWriter ClassWriter/COMPUTE_MAXS)
				scope (into (closure :vars) (closure :args))
				name (str "mug/compiled/JSScope_" i)]
				(.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), name, nil, "java/lang/Object", nil)
				(asm-compile-scope-fields name scope cw)
				(asm-compile-scope-methods name scope cw)
				(asm-compile-scope-init name scope cw)
				(.visitEnd cw)
				[(str "JSScope_" i ".class") (.toByteArray cw)]))
		(ctx :closures))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; script file
;

(defn asm-compile-script-class [index]
  (let [cw (new ClassWriter ClassWriter/COMPUTE_MAXS)
				qn (str "mug/compiled/JSClosure_" index)]
		(.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), "mug/compiled/JSScript", nil, qn, nil)
		(doto (.visitMethod cw Opcodes/ACC_PUBLIC, "<init>", "()V", nil, nil)
      (.visitVarInsn Opcodes/ALOAD, 0)
      (.visitMethodInsn Opcodes/INVOKESPECIAL, qn, "<init>", "()V")
      (.visitInsn Opcodes/RETURN)
			(.visitMaxs 2, 2)
      (.visitEnd))
    (.visitEnd cw)
		(.toByteArray cw)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; examples
;

(def josephus-ast (func-closure nil ["print", "nanoTime"] #{"ITER", "Chain", "Person", "chain", "start", "end", "i"}
	(class-stat "Chain" {
		"first" (null-literal)
		"kill" (func-literal (func-closure nil ["nth"] #{"current" "shout"}
			(expr-stat (scope-assign-expr "current" (static-ref-expr (this-expr) "first")))
			(expr-stat (scope-assign-expr "shout" (num-literal 1)))
			(while-stat (neqs-op-expr (static-ref-expr (scope-ref-expr "current") "next") (scope-ref-expr "current")) (block-stat
				(expr-stat (scope-assign-expr "shout" (static-method-call-expr (scope-ref-expr "current") "shout" (scope-ref-expr "shout") (scope-ref-expr "nth"))))
				(expr-stat (scope-assign-expr "current" (static-ref-expr (scope-ref-expr "current") "next")))))
			(expr-stat (static-assign-expr (this-expr) "first" (scope-ref-expr "current")))
			(ret-stat (scope-ref-expr "current"))
    ))
    }
		(constructor (func-closure nil ["size" "Person" "print"] #{"last" "current" "i"}
			(expr-stat (scope-assign-expr "last" (null-literal)))
			(expr-stat (scope-assign-expr "current" (null-literal)))
			(for-in-stat "i" (num-literal 0) (scope-ref-expr "size") (num-literal 1) (block-stat
				(expr-stat (scope-assign-expr "current" (new-expr (scope-ref-expr "Person") (scope-ref-expr "i"))))
				(if-stat (eqs-op-expr (static-ref-expr (this-expr) "first") (null-literal))
					(expr-stat (static-assign-expr (this-expr) "first" (scope-ref-expr "current"))) nil)
				(if-stat (neqs-op-expr (scope-ref-expr "last") (null-literal)) (block-stat
					(expr-stat (static-assign-expr (scope-ref-expr "last") "next" (scope-ref-expr "current")))
					(expr-stat (static-assign-expr (scope-ref-expr "current") "prev" (scope-ref-expr "last")))) nil)
				(expr-stat (scope-assign-expr "last" (scope-ref-expr "current")))
      ))   
			(expr-stat (static-assign-expr (static-ref-expr (this-expr) "first") "prev" (scope-ref-expr "last")))
			(expr-stat (static-assign-expr (scope-ref-expr "last") "next" (static-ref-expr (this-expr) "first")))
    ))
    nil)
 
  ; Person class
	(class-stat "Person" {
		"count" (num-literal 0)
		"prev" (null-literal)
		"next" (null-literal)
		"shout" (func-literal (func-closure nil ["shout", "deadif"] #{}
			(if-stat (lt-op-expr (scope-ref-expr "shout") (scope-ref-expr "deadif"))
				(ret-stat (add-op-expr (scope-ref-expr "shout") (num-literal 1))) nil)
			(expr-stat (static-assign-expr (static-ref-expr (this-expr) "prev") "next" (static-ref-expr (this-expr) "next")))
			(expr-stat (static-assign-expr (static-ref-expr (this-expr) "next") "prev" (static-ref-expr (this-expr) "prev")))
			(ret-stat (num-literal 1))
		))}
		(constructor (func-closure nil ["count"] #{}
			(expr-stat (static-assign-expr (this-expr) "count" (scope-ref-expr "count")))))
		nil)

  ; main body
	(expr-stat (call-expr (scope-ref-expr "print") (str-literal "Starting.")))
	(expr-stat (scope-assign-expr "start" (call-expr (scope-ref-expr "nanoTime"))))
	(expr-stat (scope-assign-expr "ITER" (num-literal 500000)))
;;  (expr-stat (scope-assign-expr "ITER" (num-literal 50)))
	(for-in-stat "i" (num-literal 0) (scope-ref-expr "ITER") (num-literal 1) (block-stat
;;    (expr-stat (call-expr (scope-ref-expr "print") (scope-ref-expr "i")))
;;		(expr-stat (scope-assign-expr "chain" (new-expr (scope-ref-expr "Chain") (num-literal 40) (scope-ref-expr "Person"))))
    (expr-stat (scope-assign-expr "chain" (new-expr (scope-ref-expr "Chain") (num-literal 40) (scope-ref-expr "Person") (scope-ref-expr "print"))))
		(expr-stat (static-method-call-expr (scope-ref-expr "chain") "kill" (num-literal 3)))))
	(expr-stat (call-expr (scope-ref-expr "print") (str-literal "Ended.")))
	(expr-stat (scope-assign-expr "end" (call-expr (scope-ref-expr "nanoTime"))))
	(expr-stat (call-expr (scope-ref-expr "print") (add-op-expr (add-op-expr (str-literal "Time per iteration = ") (div-op-expr (sub-op-expr (scope-ref-expr "end") (scope-ref-expr "start")) (scope-ref-expr "ITER"))) (str-literal " nanoseconds."))))
))

(let [ast josephus-ast
      ctx (asm-analyze-ast ast (new-asm-ctx))]
  ; closures
	(doseq [[path bytes] (asm-compile-closure-classes ctx)]
		(write-file (str "out/" path) bytes))
	
  ; constants
	(write-file "out/JSConstants.class" (asm-compile-constants-class ctx))
	
  ; object shim
	(write-file "out/JSObject.class" (asm-compile-object-class ctx))
	
  ; scopes
	(doseq [[path bytes] (asm-compile-scope-classes ctx)]
		(write-file (str "out/" path) bytes))
	
  ; script entry point
	(write-file "out/JSScript.class" (asm-compile-script-class (index-of (ctx :closures) ast))))