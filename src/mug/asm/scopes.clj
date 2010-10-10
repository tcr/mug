(ns mug.asm.scopes
  (:use mug.asm.util))

(import (org.objectweb.asm ClassWriter Opcodes Label))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; scopes
;

(defn asm-compile-scope-fields [qn scope cw]
	(doseq [var scope]
		(.visitEnd (.visitField cw, 0, (str "_" var), (sig-obj qn-js-primitive), nil, nil))))

(defn asm-compile-scope-methods [qn scope cw]
	(doseq [var scope]
		(doto (.visitMethod cw, Opcodes/ACC_PUBLIC, (str "get_" var), (sig-call (sig-obj qn-js-primitive)), nil, nil)
			(.visitCode)
			(.visitVarInsn Opcodes/ALOAD, 0)
			(.visitFieldInsn Opcodes/GETFIELD, qn, (str "_" var), (sig-obj qn-js-primitive));
			(.visitInsn Opcodes/ARETURN)
			(.visitMaxs 1, 1)
			(.visitEnd))
		(doto (.visitMethod cw, Opcodes/ACC_PUBLIC, (str "set_" var), (sig-call (sig-obj qn-js-primitive) qn-void), nil, nil)
			(.visitCode)
			(.visitVarInsn Opcodes/ALOAD, 0)
			(.visitVarInsn Opcodes/ALOAD, 1)
			(.visitFieldInsn Opcodes/PUTFIELD, qn, (str "_" var), (sig-obj qn-js-primitive));
			(.visitInsn Opcodes/RETURN)
			(.visitMaxs 2, 2)
			(.visitEnd))))

(defn asm-compile-scope-init [qn scope cw]
  (let [mw (.visitMethod cw, Opcodes/ACC_PUBLIC, "<init>", (sig-call qn-void), nil, nil)]
  	(.visitCode mw)
  	(.visitVarInsn mw Opcodes/ALOAD, 0)
  	(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-object, "<init>", "()V")  
  	(.visitInsn mw Opcodes/RETURN)
  	(.visitMaxs mw 1, 1)
  	(.visitEnd mw)))

(defn asm-compile-scope-classes [ast]
	(into {}
		(map-indexed (fn [i context]
			(let [qn (qn-js-scope i)
            scope (context-scope-vars context)
            cw (new ClassWriter ClassWriter/COMPUTE_MAXS)]
				(.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), qn, nil, qn-object, nil)
				(asm-compile-scope-fields qn scope cw)
				(asm-compile-scope-methods qn scope cw)
				(asm-compile-scope-init qn scope cw)
				(.visitEnd cw)
				[qn (.toByteArray cw)]))
		(ast :contexts))))