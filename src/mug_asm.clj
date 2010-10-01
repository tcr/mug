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

(defn new-asm-profile [] {
	:closures #{} :scopes {} :structs #{} :numbers #{} :strings #{} :accessors #{}
})

(defn asm-profile-add [n v profile]
	(if (contains? v (profile n)) profile (merge profile {n (merge (profile n) v)})))

(defn asm-profile-assign [n k v profile]
	(if (contains? v (profile n)) profile (merge profile {n (merge (profile n) {k v})})))

(defn asm-profile-add-coll [k coll profile]
	(loop [profile profile coll coll]
		(if (empty? coll)
      profile
			(recur (asm-profile-add k (first coll) profile) (next coll)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; analysis
;

(defmulti asm-analyze-ast (fn [& args] (:type (first args))))
	
(defn asm-analyze-ast-coll [coll closure profile]
	(loop [profile profile coll coll]
		(if (empty? coll)
      profile
			(recur (asm-analyze-ast (first coll) closure profile) (next coll)))))
		
;
; literals
;

(defmethod asm-analyze-ast ::num-literal [node closure profile]
	(asm-profile-add :numbers (node :value) profile))

(defmethod asm-analyze-ast ::str-literal [node closure profile]
	(asm-profile-add :strings (node :value) profile))

(defmethod asm-analyze-ast ::null-literal [node closure profile]
	profile)

(defmethod asm-analyze-ast ::func-literal [node closure profile]
	(asm-analyze-ast (node :closure) closure profile))

;
; expressions
;

(defmethod asm-analyze-ast ::unary-op-expr [node closure profile]
	(asm-analyze-ast (node :expr) closure ,,,))

(defmethod asm-analyze-ast ::binary-op-expr [node closure profile]
	(->> profile
		(asm-analyze-ast (node :left) closure ,,,)
		(asm-analyze-ast (node :right) closure ,,,)))

(defmethod asm-analyze-ast ::scope-ref-expr [node closure profile]
	profile)

(defmethod asm-analyze-ast ::static-ref-expr [node closure profile]
	(->> (asm-profile-add :accessors (node :value) profile)
		(asm-analyze-ast (node :base) closure ,,,)))

(defmethod asm-analyze-ast ::static-method-call-expr [node closure profile]
	(->> (asm-profile-add :accessors (node :value) profile)
		(asm-analyze-ast (node :base) closure ,,,)
    (asm-analyze-ast-coll (node :args) closure ,,,)))

(defmethod asm-analyze-ast ::scope-assign-expr [node closure profile]
	(->> profile
		(asm-analyze-ast (node :expr) closure ,,,)))

(defmethod asm-analyze-ast ::static-assign-expr [node closure profile]
	(->> profile
    (asm-profile-add :accessors (node :value) ,,,)
		(asm-analyze-ast (node :base) closure ,,,)
    (asm-analyze-ast (node :expr) closure ,,,)))

(defmethod asm-analyze-ast ::new-expr [node closure profile]
	(->> profile
		(asm-analyze-ast (node :constructor) closure ,,,)
    (asm-analyze-ast-coll (node :args) closure ,,,)))

(defmethod asm-analyze-ast ::this-expr [node closure profile]
	profile)
	
(defmethod asm-analyze-ast ::call-expr [node closure profile]
	(->> profile
		(asm-analyze-ast (node :ref) closure ,,,)
		(asm-analyze-ast-coll (node :args) closure ,,,)))

;
; statements
;

(defmethod asm-analyze-ast ::class-stat [node closure profile]
  (->> profile
    (asm-profile-add :structs (set (keys (node :prototype))) ,,,)
    (asm-profile-add-coll :accessors (keys (or (node :prototype) {})) ,,,)
    (asm-analyze-ast ((node :constructor) :closure) closure ,,,)
    (asm-analyze-ast-coll (vals (or (node :prototype) {})) closure ,,,)
    (asm-analyze-ast-coll (vals (or (node :static) {})) closure ,,,)))

(defmethod asm-analyze-ast ::expr-stat [node closure profile]
	(asm-analyze-ast (node :expr) closure profile))

(defmethod asm-analyze-ast ::block-stat [node closure profile]
	(asm-analyze-ast-coll (node :stats) closure profile))

(defmethod asm-analyze-ast ::ret-stat [node closure profile]
	(if (nil? (node :expr))
    profile
    (asm-analyze-ast (node :expr) closure profile)))

(defmethod asm-analyze-ast ::if-stat [node closure profile]
	(let [profile (->> profile
		(asm-analyze-ast (node :expr) closure ,,,)
		(asm-analyze-ast (node :then-stat) closure ,,,))]
    (if (nil? (node :else-stat))
      profile
      (asm-analyze-ast (node :else-stat) closure profile))))

(defmethod asm-analyze-ast ::while-stat [node closure profile]
	(->> profile
		(asm-analyze-ast (node :expr) closure ,,,)
		(asm-analyze-ast (node :stat) closure ,,,)))

(defmethod asm-analyze-ast ::for-in-stat [node closure profile]
	(->> profile
		(asm-analyze-ast (node :from) closure ,,,)
		(asm-analyze-ast (node :to) closure ,,,)
    (asm-analyze-ast (node :by) closure ,,,)
    (asm-analyze-ast (node :stat) closure ,,,)))

;
; closure
;

(defmethod asm-analyze-ast ::closure [node closure profile]
	(->> profile
    (asm-profile-assign :scopes node
      (if (nil? closure) [] (into ((profile :scopes) closure) [closure])) ,,,)
;    (asm-profile-assign :scopes node 
;      (into (if (nil? closure) #{} ((profile :scopes) closure))
;        (into (into (node :args) (node :vars)) (if (nil? (node :name)) [] [(node :name)]))) ,,,)
		(asm-profile-add :closures node ,,,)
		(asm-analyze-ast-coll (node :stats) node ,,,)))
	
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

(defmethod asm-compile-closure-ast ::null-literal [node closure profile mw]
  (.visitInsn mw Opcodes/ACONST_NULL))

(defmethod asm-compile-closure-ast ::num-literal [node closure profile mw]
	(.visitFieldInsn mw Opcodes/GETSTATIC, "mug/compiled/JSConstants" (str "NUM_" (index-of (profile :numbers) (node :value))) "Lmug/JSNumber;"))

(defmethod asm-compile-closure-ast ::str-literal [node closure profile mw]
	(.visitFieldInsn mw Opcodes/GETSTATIC "mug/compiled/JSConstants" (str "STR_" (index-of (profile :strings) (node :value))) "Lmug/JSString;"))

(defmethod asm-compile-closure-ast ::func-literal [node closure profile mw]
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

(defmethod asm-compile-closure-ast ::scope-ref-expr [node closure profile mw]
  (if (contains? (into (closure :vars) (closure :args)) (node :value))
    (do
      (.visitVarInsn mw Opcodes/ALOAD, 6)
      (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (asm-compile-closure-scope-qn closure profile), (str "get_" (node :value)), "()Lmug/JSPrimitive;"))
    (asm-compile-closure-search-scopes (node :value) (reverse ((profile :scopes) closure)) closure profile mw)))

(defmethod asm-compile-closure-ast ::static-ref-expr [node closure profile mw]
  (asm-compile-closure-ast (node :base) closure profile mw)
	(doto mw
    (.visitTypeInsn Opcodes/CHECKCAST, "mug/compiled/JSObject")
		(.visitMethodInsn Opcodes/INVOKEVIRTUAL, "mug/compiled/JSObject", (str "get_" (node :value)), "()Lmug/JSPrimitive;")))

(defmethod asm-compile-closure-ast ::static-method-call-expr [node closure profile mw]
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

(defmethod asm-compile-closure-ast ::call-expr [node closure profile mw]
	(asm-compile-closure-ast (node :ref) closure profile mw)
	(.visitTypeInsn mw, Opcodes/CHECKCAST, "mug/JSFunction")
	(.visitInsn mw Opcodes/ACONST_NULL)
	(doseq [arg (node :args)]
		(asm-compile-closure-ast arg closure profile mw))
	(doseq [_ (range (count (node :args)) 4)]
		(.visitInsn mw Opcodes/ACONST_NULL))
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, "mug/JSFunction", "invoke", "(Lmug/compiled/JSObject;Lmug/JSPrimitive;Lmug/JSPrimitive;Lmug/JSPrimitive;Lmug/JSPrimitive;)Lmug/JSPrimitive;"))

(defmethod asm-compile-closure-ast ::scope-assign-expr [node closure profile mw]
	(asm-compile-closure-ast (node :expr) closure profile mw)
  (.visitInsn mw Opcodes/DUP)
  (.visitVarInsn mw Opcodes/ALOAD, 6)
  (.visitInsn mw Opcodes/SWAP)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, (asm-compile-closure-scope-qn closure profile), (str "set_" (node :value)), "(Lmug/JSPrimitive;)V"))

(defmethod asm-compile-closure-ast ::static-assign-expr [node closure profile mw]
	(asm-compile-closure-ast (node :expr) closure profile mw)
  (.visitInsn mw Opcodes/DUP)
  (asm-compile-closure-ast (node :base) closure profile mw)
  (.visitTypeInsn mw, Opcodes/CHECKCAST, "mug/compiled/JSObject")
  (.visitInsn mw Opcodes/SWAP)
	(.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, "mug/compiled/JSObject", (str "set_" (node :value)), "(Lmug/JSPrimitive;)V"))

(defmethod asm-compile-closure-ast ::add-op-expr [node closure profile mw]
  (asm-compile-closure-ast (node :left) closure profile mw)
  (asm-compile-closure-ast (node :right) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "add", "(Lmug/JSPrimitive;Lmug/JSPrimitive;)Lmug/JSPrimitive;"))

(defmethod asm-compile-closure-ast ::sub-op-expr [node closure profile mw]
  (.visitTypeInsn mw Opcodes/NEW, "mug/JSNumber")
  (.visitInsn mw Opcodes/DUP)
  (asm-compile-closure-ast (node :left) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
  (asm-compile-closure-ast (node :right) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
  (.visitInsn mw Opcodes/DSUB)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, "mug/JSNumber", "<init>", "(D)V"))

(defmethod asm-compile-closure-ast ::div-op-expr [node closure profile mw]
  (.visitTypeInsn mw Opcodes/NEW, "mug/JSNumber")
  (.visitInsn mw Opcodes/DUP)
  (asm-compile-closure-ast (node :left) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
  (asm-compile-closure-ast (node :right) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
  (.visitInsn mw Opcodes/DDIV)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, "mug/JSNumber", "<init>", "(D)V"))

(defmethod asm-compile-closure-ast ::mul-op-expr [node closure profile mw]
  (.visitTypeInsn mw Opcodes/NEW, "mug/JSNumber")
  (.visitInsn mw Opcodes/DUP)
  (asm-compile-closure-ast (node :left) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
  (asm-compile-closure-ast (node :right) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
  (.visitInsn mw Opcodes/DMUL)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, "mug/JSNumber", "<init>", "(D)V"))

(defmethod asm-compile-closure-ast ::lsh-op-expr [node closure profile mw]
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

(defmethod asm-compile-closure-ast ::eqs-op-expr [node closure profile mw]
  (asm-compile-closure-ast (node :left) closure profile mw)
  (asm-compile-closure-ast (node :right) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "testStrictEquality", "(Lmug/JSPrimitive;Lmug/JSPrimitive;)Lmug/JSBoolean;"))

(defmethod asm-compile-closure-ast ::neqs-op-expr [node closure profile mw]
  (asm-compile-closure-ast (node :left) closure profile mw)
  (asm-compile-closure-ast (node :right) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "testStrictInequality", "(Lmug/JSPrimitive;Lmug/JSPrimitive;)Lmug/JSBoolean;"))

(defmethod asm-compile-closure-ast ::lt-op-expr [node closure profile mw]
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

(defmethod asm-compile-closure-ast ::lte-op-expr [node closure profile mw]
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

(defmethod asm-compile-closure-ast ::gt-op-expr [node closure profile mw]
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

(defmethod asm-compile-closure-ast ::gte-op-expr [node closure profile mw]
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

(defmethod asm-compile-closure-ast ::neg-op-expr [node closure profile mw]
  (.visitTypeInsn mw Opcodes/NEW, "mug/JSNumber")
  (.visitInsn mw Opcodes/DUP)
  (asm-compile-closure-ast (node :expr) closure profile mw)
  (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asNumber", "(Lmug/JSPrimitive;)D")
  (.visitInsn mw Opcodes/DNEG)
  (.visitMethodInsn mw Opcodes/INVOKESPECIAL, "mug/JSNumber", "<init>", "(D)V"))
    
(defmethod asm-compile-closure-ast ::this-expr [node closure profile mw]
  (.visitVarInsn mw Opcodes/ALOAD, 1))

(defmethod asm-compile-closure-ast ::new-expr [node closure profile mw]
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

(defmethod asm-compile-closure-ast ::block-stat [node closure profile mw]
  (doseq [stat (node :stats)]
    (asm-compile-closure-ast stat closure profile mw)))

(defmethod asm-compile-closure-ast ::expr-stat [node closure profile mw]
	(asm-compile-closure-ast (node :expr) closure profile mw)
	(.visitInsn mw Opcodes/POP))

(defmethod asm-compile-closure-ast ::ret-stat [node closure profile mw]
  (if (nil? (node :expr))
    (.visitInsn mw Opcodes/ACONST_NULL)
    (asm-compile-closure-ast (node :expr) closure profile mw))
  (.visitInsn mw Opcodes/ARETURN))

(defmethod asm-compile-closure-ast ::while-stat [node closure profile mw]
  (let [test-case (new Label) false-case (new Label)]
    (.visitLabel mw test-case)
    (asm-compile-closure-ast (node :expr) closure profile mw)
    (.visitMethodInsn mw Opcodes/INVOKESTATIC, "mug/JSUtils", "asBoolean", "(Lmug/JSPrimitive;)Z")
    (.visitJumpInsn mw, Opcodes/IFEQ, false-case)
    (asm-compile-closure-ast (node :stat) closure profile mw)
    (.visitJumpInsn mw, Opcodes/GOTO, test-case)
    (.visitLabel mw false-case)))

(defmethod asm-compile-closure-ast ::if-stat [node closure profile mw]
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

(defmethod asm-compile-closure-ast ::for-in-stat [node closure profile mw]
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

(defmethod asm-compile-closure-ast ::class-stat [node closure profile mw]
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; constants
;

(defn asm-compile-constants-fields [profile cw]
	; undefined
	(.visitEnd (.visitField cw, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), "UNDEFINED", "Lmug/JSUndefined;", nil, nil))

	; booleans
	(.visitEnd (.visitField cw, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), "TRUE", "Lmug/JSBoolean;", nil, nil))
	(.visitEnd (.visitField cw, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), "FALSE", "Lmug/JSBoolean;", nil, nil))

	; strings
	(doseq [v (vec (profile :strings))]
			(.visitEnd (.visitField cw, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), (str "STR_" (index-of (profile :strings) v)), "Lmug/JSString;", nil, nil)))
 
 	; numbers
	(doseq [v (vec (profile :numbers))]
			(.visitEnd (.visitField cw, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), (str "NUM_" (index-of (profile :numbers) v)), "Lmug/JSNumber;", nil, nil))))

(defn asm-compile-constants-clinit [profile cw]
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
		(doseq [v (vec (profile :strings))]
			(doto mv
				(.visitTypeInsn Opcodes/NEW "mug/JSString")
				(.visitInsn Opcodes/DUP)
				(.visitLdcInsn v)
				(.visitMethodInsn Opcodes/INVOKESPECIAL, "mug/JSString", "<init>", "(Ljava/lang/String;)V")
				(.visitFieldInsn Opcodes/PUTSTATIC, "mug/compiled/JSConstants", (str "STR_" (index-of (profile :strings) v)), "Lmug/JSString;")))

		; numbers
		(doseq [v (vec (profile :numbers))]
			(doto mv
				(.visitTypeInsn Opcodes/NEW "mug/JSNumber")
				(.visitInsn Opcodes/DUP)
				(.visitLdcInsn (new Double (double v)))
				(.visitMethodInsn Opcodes/INVOKESPECIAL, "mug/JSNumber", "<init>", "(D)V")
				(.visitFieldInsn Opcodes/PUTSTATIC, "mug/compiled/JSConstants", (str "NUM_" (index-of (profile :numbers) v)), "Lmug/JSNumber;")))

		(doto mv
			(.visitInsn Opcodes/RETURN)
			(.visitMaxs 4, 0)
			(.visitEnd))))

(defn asm-compile-constants-class [profile]
	(let [cw (new ClassWriter ClassWriter/COMPUTE_MAXS)]
		(.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), "mug/compiled/JSConstants", nil, "java/lang/Object", nil)
		(asm-compile-constants-fields profile cw)
		(asm-compile-constants-clinit profile cw)
		(.visitEnd cw)
		(.toByteArray cw)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; object
;

(defn asm-compile-object-methods [name accessors cw]
  (doseq [k (filter (fn [x] (not= x "prototype")) (vec accessors))]
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

(defn asm-compile-object-class [profile]
		(let [cw (new ClassWriter ClassWriter/COMPUTE_MAXS)
			name "mug/compiled/JSObject"]
			(.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), name, nil, "mug/JSObjectBase", nil)
			(asm-compile-object-methods name (profile :accessors) cw)
			(asm-compile-object-init name cw)
			(.visitEnd cw)
			(.toByteArray cw)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; scopes
;

;TODO remove name, scope vars from each function input

(defn asm-compile-scope-vars [closure]
  (into (into (closure :vars) (closure :args)) (if (nil? (closure :name)) [] [(closure :name)])))

(defn asm-compile-scope-fields [name closure scope profile cw]
	(doseq [var scope]
		(.visitEnd (.visitField cw, 0, (str "_" var), "Lmug/JSPrimitive;", nil, nil))))

(defn asm-compile-scope-methods [name closure scope profile cw]
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

(defn asm-compile-scope-init [name closure scope profile cw]
  (let [mw (.visitMethod cw, 0, "<init>", "()V", nil, nil)]
  	(.visitCode mw)
  	(.visitVarInsn mw Opcodes/ALOAD, 0)
  	(.visitMethodInsn mw Opcodes/INVOKESPECIAL, "java/lang/Object", "<init>", "()V")  
  	(.visitInsn mw Opcodes/RETURN)
  	(.visitMaxs mw 1, 1)
  	(.visitEnd mw)))

(defn asm-compile-scope-classes [profile]
	(into {}
		(map-indexed (fn [i closure]
			(let [cw (new ClassWriter ClassWriter/COMPUTE_MAXS)
            scope (asm-compile-scope-vars closure)
				    qn (str "mug/compiled/JSScope_" i)]
				(.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), qn, nil, "java/lang/Object", nil)
				(asm-compile-scope-fields qn closure scope profile cw)
				(asm-compile-scope-methods qn closure scope profile cw)
				(asm-compile-scope-init qn closure scope profile cw)
				(.visitEnd cw)
				[(str "JSScope_" i ".class") (.toByteArray cw)]))
		(profile :closures))))

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

;
; josephus problem
;

(def josephus-ast
  (func-closure nil ["print"]  #{"Chain" "ITER" "Person" "chain" "end" "i" "start"} (block-stat 
          
    (expr-stat (scope-assign-expr "Person" (func-literal (func-closure nil ["count"]  #{} (block-stat 
        (expr-stat (static-assign-expr (this-expr) "count" (scope-ref-expr "count") ))
        (ret-stat  (this-expr))
    ) )) ))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "Person") "prototype") "count" (num-literal 0) ))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "Person") "prototype") "prev" (null-literal) ))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "Person") "prototype") "next" (null-literal) ))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "Person") "prototype") "shout" (func-literal (func-closure nil ["shout" "deadif"]  #{} (block-stat 
        (if-stat (lt-op-expr (scope-ref-expr "shout") (scope-ref-expr "deadif") ) (expr-stat (ret-stat  (add-op-expr (scope-ref-expr "shout") (num-literal 1) ))) nil )
        (expr-stat (static-assign-expr (static-ref-expr (this-expr) "prev") "next" (static-ref-expr (this-expr) "next") ))
        (expr-stat (static-assign-expr (static-ref-expr (this-expr) "next") "prev" (static-ref-expr (this-expr) "prev") ))
        (ret-stat  (num-literal 1))
    ) )) ))
    (expr-stat (scope-assign-expr "Chain" (func-literal (func-closure nil ["size"]  #{"current" "i" "last"} (block-stat 
          
        (expr-stat (scope-assign-expr "last" (null-literal) ))
        (expr-stat (scope-assign-expr "current" (null-literal) ))
        (scope-assign-expr "i" (num-literal 0) ) 
 (while-stat (lt-op-expr (scope-ref-expr "i") (scope-ref-expr "size") )(block-stat 
            (block-stat 
                (expr-stat (scope-assign-expr "current" (new-expr  (scope-ref-expr "Person") (scope-ref-expr "i")) ))
                (if-stat (eqs-op-expr (static-ref-expr (this-expr) "first") (null-literal) ) (expr-stat (static-assign-expr (this-expr) "first" (scope-ref-expr "current") )) nil )
                (if-stat (neqs-op-expr (scope-ref-expr "last") (null-literal) ) (block-stat 
                    (expr-stat (static-assign-expr (scope-ref-expr "last") "next" (scope-ref-expr "current") ))
                    (expr-stat (static-assign-expr (scope-ref-expr "current") "prev" (scope-ref-expr "last") ))
                ) nil )
                (expr-stat (scope-assign-expr "last" (scope-ref-expr "current") ))
            )
            (expr-stat (scope-assign-expr "i" (add-op-expr (scope-ref-expr "i") (num-literal 1) ) ))
        ))
        (expr-stat (static-assign-expr (static-ref-expr (this-expr) "first") "prev" (scope-ref-expr "last") ))
        (expr-stat (static-assign-expr (scope-ref-expr "last") "next" (static-ref-expr (this-expr) "first") ))
        (ret-stat  (this-expr))
    ) )) ))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "Chain") "prototype") "first" (null-literal) ))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "Chain") "prototype") "kill" (func-literal (func-closure nil ["nth"]  #{"current" "shout" } (block-stat 
         
        (expr-stat (scope-assign-expr "current" (static-ref-expr (this-expr) "first") ))
        (expr-stat (scope-assign-expr "shout" (num-literal 1) ))
        (while-stat (neqs-op-expr (static-ref-expr (scope-ref-expr "current") "next") (scope-ref-expr "current") ) (block-stat 
            (expr-stat (scope-assign-expr "shout" (static-method-call-expr (scope-ref-expr "current") "shout" (scope-ref-expr "shout") (scope-ref-expr "nth")) ))
            (expr-stat (scope-assign-expr "current" (static-ref-expr (scope-ref-expr "current") "next") ))
        ) )
        (expr-stat (static-assign-expr (this-expr) "first" (scope-ref-expr "current") ))
        (ret-stat  (scope-ref-expr "current"))
    ) )) ))
    (expr-stat (scope-assign-expr "ITER" (num-literal 5e5) ))
    (scope-assign-expr "i" (num-literal 0) ) 
 (while-stat (lt-op-expr (scope-ref-expr "i") (scope-ref-expr "ITER") )(block-stat 
        (block-stat 
            (expr-stat (scope-assign-expr "chain" (new-expr  (scope-ref-expr "Chain") (num-literal 40)) ))
            (expr-stat (static-method-call-expr (scope-ref-expr "chain") "kill" (num-literal 3)))
        )
        (expr-stat (scope-assign-expr "i" (add-op-expr (scope-ref-expr "i") (num-literal 1) ) ))
    ))
) )
)

;
; binary trees
;

; WHY DOESN'T THIS WORK
;(def binary-ast (neg-op-expr (num-literal 5)))

(def binary-ast
(func-closure nil ["print"]  #{"TreeNode" "bottomUpTree" "minDepth" "n" "maxDepth" "stretchDepth" "check" "longLivedTree" "iterations" "i" "depth"} (block-stat 
    (expr-stat (scope-assign-expr "TreeNode" (func-literal (func-closure "TreeNode" ["left" "right" "item"]  #{} (block-stat 
        (expr-stat (static-assign-expr (this-expr) "left" (scope-ref-expr "left") ))
        (expr-stat (static-assign-expr (this-expr) "right" (scope-ref-expr "right") ))
        (expr-stat (static-assign-expr (this-expr) "item" (scope-ref-expr "item") ))
    ) ))))
    (expr-stat (static-assign-expr (static-ref-expr (scope-ref-expr "TreeNode") "prototype") "itemCheck" (func-literal (func-closure nil []  #{} (if-stat (eqs-op-expr (static-ref-expr (this-expr) "left") (null-literal) ) (ret-stat  (static-ref-expr (this-expr) "item")) (ret-stat  (sub-op-expr (add-op-expr (static-ref-expr (this-expr) "item") (static-method-call-expr (static-ref-expr (this-expr) "left") "itemCheck" ) ) (static-method-call-expr (static-ref-expr (this-expr) "right") "itemCheck" ) )) ) )) ))
    (expr-stat (scope-assign-expr "bottomUpTree" (func-literal (func-closure "bottomUpTree" ["item" "depth"]  #{} (if-stat (gt-op-expr (scope-ref-expr "depth") (num-literal 0) ) (expr-stat (ret-stat  (new-expr  (scope-ref-expr "TreeNode") (call-expr (scope-ref-expr "bottomUpTree") (sub-op-expr (mul-op-expr (num-literal 2) (scope-ref-expr "item") ) (num-literal 1) ) (sub-op-expr (scope-ref-expr "depth") (num-literal 1) )) (call-expr (scope-ref-expr "bottomUpTree") (mul-op-expr (num-literal 2) (scope-ref-expr "item") ) (sub-op-expr (scope-ref-expr "depth") (num-literal 1) )) (scope-ref-expr "item")))) (expr-stat (ret-stat  (new-expr  (scope-ref-expr "TreeNode") (null-literal) (null-literal) (scope-ref-expr "item")))) ) ))))
    (expr-stat (scope-assign-expr "minDepth" (num-literal 4)))
    (expr-stat (scope-assign-expr "n" (num-literal 16)))
    (expr-stat (scope-assign-expr "maxDepth" (num-literal 16)))
    (expr-stat (scope-assign-expr "stretchDepth" (add-op-expr (scope-ref-expr "maxDepth") (num-literal 1) )))
    (expr-stat (scope-assign-expr "check" (static-method-call-expr (call-expr (scope-ref-expr "bottomUpTree") (num-literal 0) (scope-ref-expr "stretchDepth")) "itemCheck" )))
    (expr-stat (call-expr (scope-ref-expr "print") (add-op-expr (add-op-expr (add-op-expr (str-literal "stretch tree of depth ") (scope-ref-expr "stretchDepth") ) (str-literal "\t check: ") ) (scope-ref-expr "check") )))
    (expr-stat (scope-assign-expr "longLivedTree" (call-expr (scope-ref-expr "bottomUpTree") (num-literal 0) (scope-ref-expr "maxDepth"))))
    (expr-stat (scope-assign-expr "depth" (scope-ref-expr "minDepth"))) 
 (while-stat (lte-op-expr (scope-ref-expr "depth") (scope-ref-expr "maxDepth") )(block-stat 
        (block-stat 
            (expr-stat (scope-assign-expr "iterations" (lsh-op-expr (num-literal 1) (add-op-expr (sub-op-expr (scope-ref-expr "maxDepth") (scope-ref-expr "depth") ) (scope-ref-expr "minDepth") ) )))
            (expr-stat (scope-assign-expr "check" (num-literal 0) ))
            (expr-stat (scope-assign-expr "i" (num-literal 1))) 
 (while-stat (lte-op-expr (scope-ref-expr "i") (scope-ref-expr "iterations") )(block-stat 
                (block-stat 
                    (expr-stat (scope-assign-expr "check" (add-op-expr (scope-ref-expr "check") (static-method-call-expr (call-expr (scope-ref-expr "bottomUpTree") (scope-ref-expr "i") (scope-ref-expr "depth")) "itemCheck" ) ) ))
                    (expr-stat (scope-assign-expr "check" (add-op-expr (scope-ref-expr "check") (static-method-call-expr (call-expr (scope-ref-expr "bottomUpTree") (sub-op-expr (num-literal 0) (scope-ref-expr "i") ) (scope-ref-expr "depth")) "itemCheck" ) ) ))
                )
                (expr-stat (scope-assign-expr "i" (add-op-expr (scope-ref-expr "i") (num-literal 1) ) ))
            ))
            (expr-stat (call-expr (scope-ref-expr "print") (add-op-expr (add-op-expr (add-op-expr (add-op-expr (mul-op-expr (scope-ref-expr "iterations") (num-literal 2) ) (str-literal "\t trees of depth ") ) (scope-ref-expr "depth") ) (str-literal "\t check: ") ) (scope-ref-expr "check") )))
        )
        (expr-stat (scope-assign-expr "depth" (add-op-expr (scope-ref-expr "depth") (num-literal 2) ) ))
    ))
    (expr-stat (call-expr (scope-ref-expr "print") (add-op-expr (add-op-expr (add-op-expr (str-literal "long lived tree of depth ") (scope-ref-expr "maxDepth") ) (str-literal "\t check: ") ) (static-method-call-expr (scope-ref-expr "longLivedTree") "itemCheck" ) )))
) ))

;
; output
;

(let [ast test-ast
      profile (asm-analyze-ast ast nil (new-asm-profile))]
  ; closures
  (println "Closures...")
	(doseq [[path bytes] (asm-compile-closure-classes profile)]
		(write-file (str "out/" path) bytes))
	
  ; constants
  (println "Constants...")
	(write-file "out/JSConstants.class" (asm-compile-constants-class profile))
	
  ; object shim
  (println "Objects...")
	(write-file "out/JSObject.class" (asm-compile-object-class profile))
	
  ; scopes
  (println "Scopes...")
	(doseq [[path bytes] (asm-compile-scope-classes profile)]
		(write-file (str "out/" path) bytes))
	
  ; script entry point
  (println "Script...")
	(write-file "out/JSScript.class" (asm-compile-script-class (index-of (profile :closures) ast)))
 
  (println "Done."))