(ns mug.asm.contexts
  (:use [mug.asm util code]))

(import (org.objectweb.asm ClassWriter Opcodes Label))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; init
;

(defmulti compile-context-init (fn [& args] (:type (first args))))

(defmethod compile-context-init :mug.ast/script-context [context ast cw]
  (let [sig (sig-context-init context ast)
        mw (.visitMethod cw, Opcodes/ACC_PUBLIC, "<init>", sig, nil, nil)]
		(.visitCode mw)
		(.visitVarInsn mw Opcodes/ALOAD, 0)
		(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-module, "<init>", (sig-call sig-void))    
		(.visitInsn mw Opcodes/RETURN)
		(.visitMaxs mw 1, 1)
		(.visitEnd mw)))

(defmethod compile-context-init :mug.ast/closure-context [context ast cw]
  (let [mw (.visitMethod cw, Opcodes/ACC_PUBLIC, "<init>", (sig-context-init context ast), nil, nil)
        qn (qn-js-context (context-index context ast))]
		(.visitCode mw)
		(.visitVarInsn mw Opcodes/ALOAD, 0)
    (.visitVarInsn mw Opcodes/ALOAD, 1)
		(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-function, "<init>", (sig-call (sig-obj qn-js-object) sig-void))
 
    (doseq [[i parent] (index (context :parents))]
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

(defmulti compile-context-method (fn [& args] (:type (first args))))

(defmethod compile-context-method :mug.ast/script-context [context ast cw]
	(let [mw (.visitMethod cw, Opcodes/ACC_PUBLIC, "load", (sig-load), nil, (into-array ["java/lang/Exception"]))]
		(.visitCode mw)
		
		; scope
    (doto mw
      ; create scope and store in register
      (.visitTypeInsn Opcodes/NEW, (qn-js-scriptscope))
      (.visitInsn Opcodes/DUP)
      (.visitInsn Opcodes/DUP)
      (.visitMethodInsn Opcodes/INVOKESPECIAL, (qn-js-scriptscope), "<init>", "()V")
			(.visitVarInsn Opcodes/ASTORE, (+ 1 3 arg-limit))
      ; "exports" object
      (.visitMethodInsn Opcodes/INVOKEVIRTUAL, (qn-js-scriptscope), "get_exports", (sig-call (sig-obj qn-js-primitive)))
      (.visitTypeInsn Opcodes/CHECKCAST, qn-js-object)
      (.visitVarInsn Opcodes/ASTORE, (+ 2 3 arg-limit)))
  
    ;[TODO] THIS object
	
    ; compile body
		(doseq [stat (context :stats)]
			(compile-code stat context ast mw))
		
  	; return "exports" object
		(doto mw
			(.visitVarInsn Opcodes/ALOAD, (+ 2 3 arg-limit))
			(.visitInsn Opcodes/ARETURN))
  
		; finish closure
		(.visitMaxs mw 0, 0)
		(.visitEnd mw)))
	
(defmethod compile-context-method :mug.ast/closure-context [context ast cw]
	(let [mw (.visitMethod cw, Opcodes/ACC_PUBLIC, "invoke", sig-invoke, nil, (into-array ["java/lang/Exception"]))
        qn (qn-js-context (context-index context ast))
        qn-scope (qn-js-scope (context-index context ast))]
		(.visitCode mw)
		
		; create scope object
		(doto mw
			(.visitTypeInsn Opcodes/NEW, qn-scope)
			(.visitInsn Opcodes/DUP)
			(.visitMethodInsn Opcodes/INVOKESPECIAL, qn-scope, "<init>", (sig-call sig-void))
			(.visitVarInsn Opcodes/ASTORE, (+ 1 3 arg-limit)))
		
		; initialize arguments
		(doseq [[i arg] (index (context :args))]
			(doto mw
				(.visitVarInsn Opcodes/ALOAD, (+ 1 3 arg-limit))
				(.visitVarInsn Opcodes/ALOAD, (+ i 3))
				(.visitMethodInsn Opcodes/INVOKEVIRTUAL, qn-scope, (str "set_" arg), (sig-call (sig-obj qn-js-primitive) sig-void))))
    ; initialize self
    (when (not (nil? (context :name)))
      (doto mw
				(.visitVarInsn Opcodes/ALOAD, (+ 1 3 arg-limit))
				(.visitVarInsn Opcodes/ALOAD, 0)
				(.visitMethodInsn Opcodes/INVOKEVIRTUAL, qn-scope, (str "set_" (context :name)), (sig-call (sig-obj qn-js-primitive) sig-void))))
		
		; compile body
		(doseq [stat (context :stats)]
			(compile-code stat context ast mw))
		
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

(defmulti compile-context-fields (fn [& args] (:type (first args))))

(defmethod compile-context-fields :mug.ast/script-context [context ast cw])

(defmethod compile-context-fields :mug.ast/closure-context [context ast cw]
  ; scopes
  (doseq [parent (context :parents)]
    (.visitEnd (.visitField cw, 0, (ident-scope parent), (sig-obj (qn-js-scope parent)), nil, nil))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; class
;

(defmulti compile-context-class (fn [& args] (:type (first args))))

(defmethod compile-context-class :mug.ast/script-context [context ast]
  (let [qn (qn-js-context (context-index context ast))
				cw (new ClassWriter ClassWriter/COMPUTE_MAXS)]
    (.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), qn, nil, qn-js-module, nil)
		(compile-context-init context ast cw)
		(compile-context-method context ast cw)
    (compile-context-fields context ast cw)
    
    ; main
    (let [mw (.visitMethod cw (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), "main", "([Ljava/lang/String;)V", nil, (into-array ["java/lang/Exception"]))]
      (doto mw
        (.visitCode)
        (.visitTypeInsn Opcodes/NEW, (qn-js-script))
        (.visitInsn Opcodes/DUP)
        (.visitMethodInsn Opcodes/INVOKESPECIAL, (qn-js-script), "<init>", "()V")
;        (.visitTypeInsn Opcodes/NEW, (qn-js-scriptscope))
;        (.visitInsn Opcodes/DUP)
;        (.visitMethodInsn Opcodes/INVOKESPECIAL, (qn-js-scriptscope), "<init>", "()V")
;        (.visitMethodInsn Opcodes/INVOKEVIRTUAL, (qn-js-script), "execute", (sig-call (sig-obj (qn-js-scriptscope)) (sig-obj qn-js-primitive)))
        (.visitMethodInsn Opcodes/INVOKEVIRTUAL, (qn-js-script), "load", (sig-call (sig-obj qn-js-object)))
        (.visitInsn Opcodes/POP)
        (.visitInsn Opcodes/RETURN)
        (.visitMaxs 1, 1)
        (.visitEnd)))
    
		(.visitEnd cw)
  	[qn (.toByteArray cw)]))

(defmethod compile-context-class :mug.ast/closure-context [context ast]
  (let [qn (qn-js-context (context-index context ast))
				cw (new ClassWriter ClassWriter/COMPUTE_MAXS)]
    (.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), qn, nil, qn-js-function, nil)
		(compile-context-init context ast cw)
		(compile-context-method context ast cw)
    (compile-context-fields context ast cw)
		(.visitEnd cw)
  	[qn (.toByteArray cw)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; context compilation
;

(defn asm-compile-closure-classes [ast]
	(into {} (map-indexed
    (fn [i context] (compile-context-class context ast))
    (ast :contexts))))