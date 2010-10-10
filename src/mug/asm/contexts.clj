(ns mug.asm.contexts
  (:use [mug.asm util code]))

(import (org.objectweb.asm ClassWriter Opcodes Label))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; init
;

(defmulti compile-context-init (fn [& args] (:type (first args))))

(defmethod compile-context-init :mug/script-context [context ast cw]
  (let [sig (sig-context-init context ast)
        mw (.visitMethod cw, Opcodes/ACC_PUBLIC, "<init>", sig, nil, nil)]
		(.visitCode mw)
		(.visitVarInsn mw Opcodes/ALOAD, 0)
		(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-object, "<init>", (sig-call qn-void))    
		(.visitInsn mw Opcodes/RETURN)
		(.visitMaxs mw 1, 1)
		(.visitEnd mw)))

(defmethod compile-context-init :mug/closure-context [context ast cw]
  (let [mw (.visitMethod cw, Opcodes/ACC_PUBLIC, "<init>", (sig-context-init context ast), nil, nil)
        qn (qn-js-context (context-index context ast))]
		(.visitCode mw)
		(.visitVarInsn mw Opcodes/ALOAD, 0)
		(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-js-function, "<init>", (sig-call qn-void))
 
    (doseq [parent (context :parents)]
      (.visitVarInsn mw Opcodes/ALOAD, 0)
      (.visitVarInsn mw Opcodes/ALOAD, (+ parent 1))
      (.visitFieldInsn mw Opcodes/PUTFIELD, qn, (ident-scope parent), (sig-obj (qn-js-scope parent))))
    
		(.visitInsn mw Opcodes/RETURN)
		(.visitMaxs mw 1, 1)
		(.visitEnd mw)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; method
;

(defmulti compile-context-method (fn [& args] (:type (first args))))

(defmethod compile-context-method :mug/script-context [context ast cw]
	(let [mw (.visitMethod cw, Opcodes/ACC_PUBLIC, "execute", sig-execute, nil, (into-array ["java/lang/Exception"]))]
		(.visitCode mw)
		
		; move global scope object to local #6
		(doto mw
			(.visitVarInsn Opcodes/ALOAD, 1)
			(.visitVarInsn Opcodes/ASTORE, 6))
  
    ;[TODO] THIS object
	
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
	
(defmethod compile-context-method :mug/closure-context [context ast cw]
	(let [mw (.visitMethod cw, Opcodes/ACC_PUBLIC, "invoke", sig-invoke, nil, (into-array ["java/lang/Exception"]))
        qn (qn-js-context (context-index context ast))
        qn-scope (qn-js-scope (context-index context ast))]
		(.visitCode mw)
		
		; create scope object
		(doto mw
			(.visitTypeInsn Opcodes/NEW, qn-scope)
			(.visitInsn Opcodes/DUP)
			(.visitMethodInsn Opcodes/INVOKESPECIAL, qn-scope, "<init>", (sig-call qn-void))
			(.visitVarInsn Opcodes/ASTORE, 6))
		
		; initialize arguments
		(doseq [[i arg] (index (context :args))]
			(doto mw
				(.visitVarInsn Opcodes/ALOAD, 6)
				(.visitVarInsn Opcodes/ALOAD, (+ i 2))
				(.visitMethodInsn Opcodes/INVOKEVIRTUAL, qn-scope, (str "set_" arg), (sig-call (sig-obj qn-js-primitive) qn-void))))
    ; initialize self
    (when (not (nil? (context :name)))
      (doto mw
				(.visitVarInsn Opcodes/ALOAD, 6)
				(.visitVarInsn Opcodes/ALOAD, 0)
				(.visitMethodInsn Opcodes/INVOKEVIRTUAL, qn-scope, (str "set_" (context :name)), (sig-call (sig-obj qn-js-primitive) qn-void))))
		
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

(defmethod compile-context-fields :mug/script-context [context ast cw])

(defmethod compile-context-fields :mug/closure-context [context ast cw]
  ; scopes
  (doseq [parent (context :parents)]
    (.visitEnd (.visitField cw, 0, (ident-scope parent), (sig-obj (qn-js-scope parent)), nil, nil))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; class
;

(defmulti compile-context-class (fn [& args] (:type (first args))))

(defmethod compile-context-class :mug/script-context [context ast]
  (let [qn (qn-js-context (context-index context ast))
				cw (new ClassWriter ClassWriter/COMPUTE_MAXS)]
    (.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), qn, nil, qn-object, nil)
		(compile-context-init context ast cw)
		(compile-context-method context ast cw)
    (compile-context-fields context ast cw)
		(.visitEnd cw)
  	[qn (.toByteArray cw)]))

(defmethod compile-context-class :mug/closure-context [context ast]
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