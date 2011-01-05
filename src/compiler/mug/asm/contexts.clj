(ns mug.asm.contexts
  (:gen-class)
  (:use
    mug.ast
    [mug.asm util code analyze config]))

(import (org.objectweb.asm ClassWriter Opcodes Label))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; init
;

(defmulti compile-context-init (fn [ci ast cw] (first ((ast-contexts ast) ci))))

(defmethod compile-context-init :mug.ast/script-context [ci ast cw]
  (let [sig (sig-context-init ci ast)
        mw (.visitMethod cw, Opcodes/ACC_PUBLIC, "<init>", sig, nil, nil)]
		(.visitCode mw)
		(.visitVarInsn mw Opcodes/ALOAD, 0)
		(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-module, "<init>", (sig-call sig-void))    
		(.visitInsn mw Opcodes/RETURN)
		(.visitMaxs mw 1, 1)
		(.visitEnd mw)))

(defmethod compile-context-init :mug.ast/closure-context [ci ast cw]
  (let [mw (.visitMethod cw, Opcodes/ACC_PUBLIC, "<init>", (sig-context-init ci ast), nil, nil)
        qn (qn-js-context ci)]
		(.visitCode mw)
		(.visitVarInsn mw Opcodes/ALOAD, 0)
    (.visitVarInsn mw Opcodes/ALOAD, 1)
		(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-function, "<init>", (sig-call (sig-obj qn-js-object) sig-void))
 
    (doseq [[i parent] (index ((ast-context-hierarchy ast) ci))]
      (.visitVarInsn mw Opcodes/ALOAD, 0)
      (.visitVarInsn mw Opcodes/ALOAD, (+ i 2))
      (.visitFieldInsn mw Opcodes/PUTFIELD, qn, (ident-scope parent), (sig-obj (qn-js-scope parent))))
    
		(.visitInsn mw Opcodes/RETURN)
		(.visitMaxs mw 1, 1)
		(.visitEnd mw)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; method
;

(defmulti compile-context-method (fn [ci ast cw] (first ((ast-contexts ast) ci))))

(defmethod compile-context-method :mug.ast/script-context [ci ast cw]
  (let [context ((ast-contexts ast) ci)
        [_ ln stats] context
	      mw (.visitMethod cw, Opcodes/ACC_PUBLIC, "load", (sig-load), nil, (into-array ["java/lang/Exception"]))]
		(.visitCode mw)
		
		; scope
    (doto mw
      ; create scope and store in register
      (.visitTypeInsn Opcodes/NEW, (qn-js-scriptscope))
      (.visitInsn Opcodes/DUP)
      (.visitInsn Opcodes/DUP)
      (.visitMethodInsn Opcodes/INVOKESPECIAL, (qn-js-scriptscope), "<init>", "()V")
			(.visitVarInsn Opcodes/ASTORE, scope-reg)
      ; "exports" object
      (.visitMethodInsn Opcodes/INVOKEVIRTUAL, (qn-js-scriptscope), "get_exports", (sig-call (sig-obj qn-object)))
      (.visitTypeInsn Opcodes/CHECKCAST, qn-js-object)
      (.visitVarInsn Opcodes/ASTORE, exports-reg))
  
    ;[TODO] THIS object
	
    ; compile functions
		(doseq [stat stats]
      (when (isa? (first stat) :mug.ast/defn-stat)
			  (asm-compile stat ci ast mw)))
    ; compile code
		(doseq [stat stats]
      (when (not (isa? (first stat) :mug.ast/defn-stat))
			  (asm-compile stat ci ast mw)))
  
    ; wait for timeouts
    (asm-toplevel ci ast mw)
    (.visitMethodInsn mw Opcodes/INVOKEVIRTUAL, qn-js-toplevel, "waitForTimers", (sig-call sig-void))
		
  	; return "exports" object
		(doto mw
			(.visitVarInsn Opcodes/ALOAD, exports-reg)
			(.visitInsn Opcodes/ARETURN))
  
		; finish closure
		(.visitMaxs mw 0, 0)
		(.visitEnd mw)))
	
(defmethod compile-context-method :mug.ast/closure-context [ci ast cw]
	(let [context ((ast-contexts ast) ci)
        [_ ln name args stats] context
        mw (.visitMethod cw, Opcodes/ACC_PUBLIC, "invoke", sig-invoke, nil, (into-array ["java/lang/Exception"]))
        qn (qn-js-context ci)
        qn-scope (qn-js-scope ci)]
		(.visitCode mw)
		
		; create scope object
		(doto mw
			(.visitTypeInsn Opcodes/NEW, qn-scope)
			(.visitInsn Opcodes/DUP)
			(.visitMethodInsn Opcodes/INVOKESPECIAL, qn-scope, "<init>", (sig-call sig-void))
			(.visitVarInsn Opcodes/ASTORE, scope-reg))
		
    ; initialize arguments object
    (when (ast-uses-arguments context) (do
      (asm-toplevel ci ast mw)
      (.visitVarInsn mw Opcodes/ILOAD 2)
      (doseq [i (range 3 (+ 1 offset-reg arg-limit))]
        (.visitVarInsn mw Opcodes/ALOAD i))
      (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "arguments", (sig-call sig-integer (sig-obj qn-object) (sig-obj qn-object) (sig-obj qn-object) (sig-obj qn-object) (sig-obj qn-object) (sig-obj qn-object) (sig-obj qn-object) (sig-obj qn-object) (sig-array (sig-obj qn-object)) (sig-array (sig-obj qn-object))))
      (.visitMethodInsn mw Opcodes/INVOKESTATIC, qn-js-utils, "createArgumentsObject", (sig-call (sig-obj qn-js-toplevel) (sig-array (sig-obj qn-object)) (sig-obj qn-js-object)))
      (if (nil? (ref-reg context "arguments"))
	      (doto mw
					(.visitVarInsn Opcodes/ALOAD, scope-reg)
          (.visitInsn Opcodes/SWAP)
					(.visitMethodInsn Opcodes/INVOKEVIRTUAL, qn-scope, (str "set_" "arguments"), (sig-call (sig-obj qn-object) sig-void)))
        (doto mw
          (.visitVarInsn Opcodes/ASTORE (ref-reg context "arguments"))))))
		; initialize arguments
		(doseq [[i arg] (index args)]
      (when (nil? (ref-reg context arg))
        (doto mw
					(.visitVarInsn Opcodes/ALOAD, scope-reg)
					(.visitVarInsn Opcodes/ALOAD, (+ i 3))
					(.visitMethodInsn Opcodes/INVOKEVIRTUAL, qn-scope, (str "set_" arg), (sig-call (sig-obj qn-object) sig-void)))))
    ; initialize self
    (when (not (nil? name))
      (if (nil? (ref-reg context name))
	      (doto mw
					(.visitVarInsn Opcodes/ALOAD, scope-reg)
					(.visitVarInsn Opcodes/ALOAD, 0)
					(.visitMethodInsn Opcodes/INVOKEVIRTUAL, qn-scope, (str "set_" name), (sig-call (sig-obj qn-object) sig-void)))
        (doto mw
          (.visitVarInsn Opcodes/ALOAD, 0)
          (.visitVarInsn Opcodes/ASTORE (ref-reg context name)))))
		
    ; compile functions
		(doseq [stat stats]
      (when (isa? (first stat) :mug.ast/defn-stat)
			  (asm-compile stat ci ast mw)))
    ; compile code
		(doseq [stat stats]
      (when (not (isa? (first stat) :mug.ast/defn-stat))
			  (asm-compile stat ci ast mw)))
		
		; catch-all return
		(doto mw
			(.visitInsn Opcodes/ACONST_NULL)
			(.visitInsn Opcodes/ARETURN))

		; finish closure
		(.visitMaxs mw 0, 0)
		(.visitEnd mw)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; fields
;

(defmulti compile-context-fields (fn [ci ast cw] (first ((ast-contexts ast) ci))))

(defmethod compile-context-fields :mug.ast/script-context [ci ast cw])

(defmethod compile-context-fields :mug.ast/closure-context [ci ast cw]
  ; scopes
  (doseq [parent ((ast-context-hierarchy ast) ci)]
	  (.visitEnd (.visitField cw, 0, (ident-scope parent), (sig-obj (qn-js-scope parent)), nil, nil))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; class
;

(defmulti compile-context-class (fn [ci ast qn path] (first ((ast-contexts ast) ci))))

(defmethod compile-context-class :mug.ast/script-context [ci ast qn path]
  (let [qn (qn-js-context ci)
				cw (new ClassWriter ClassWriter/COMPUTE_MAXS)]
    (.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), qn, nil, qn-js-module, nil)
    (.visitSource cw, path, nil)
    (update-state "line-number" -1)
    
		(compile-context-init ci ast cw)
		(compile-context-method ci ast cw)
    (compile-context-fields ci ast cw)
    
    ; main
    (let [mw (.visitMethod cw (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), "main", "([Ljava/lang/String;)V", nil, (into-array ["java/lang/Exception"]))]
      (doto mw
        (.visitCode)
        (.visitTypeInsn Opcodes/NEW, (qn-js-script))
        (.visitInsn Opcodes/DUP)
        (.visitMethodInsn Opcodes/INVOKESPECIAL, (qn-js-script), "<init>", "()V")
        (.visitMethodInsn Opcodes/INVOKEVIRTUAL, (qn-js-script), "load", (sig-call (sig-obj qn-js-object)))
        (.visitInsn Opcodes/POP)
        (.visitInsn Opcodes/RETURN)
        (.visitMaxs 1, 1)
        (.visitEnd)))
    
		(.visitEnd cw)
  	[qn (.toByteArray cw)]))

(defmethod compile-context-class :mug.ast/closure-context [ci ast qn path]
  (let [qn (qn-js-context ci)
				cw (new ClassWriter ClassWriter/COMPUTE_MAXS)]
    (.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), qn, nil, qn-js-function, nil)
    (.visitSource cw, path, nil)
    (update-state "line-number" -1)
    
		(compile-context-init ci ast cw)
		(compile-context-method ci ast cw)
    (compile-context-fields ci ast cw)
		(.visitEnd cw)
  	[qn (.toByteArray cw)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; context compilation
;

(defn compile-context-classes [ast qn path]
	(into {} (map-indexed
    (fn [ci context] (compile-context-class ci ast qn path))
    (ast-contexts ast))))