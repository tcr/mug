(ns mug.asm.scopes
  (:use mug.asm.util))

(import (org.objectweb.asm ClassWriter Opcodes Label))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; scopes
;

;TODO remove name, scope vars from each function input

(defn asm-compile-scope-vars [closure]
  (into (into (closure :vars) (closure :args)) (if (nil? (closure :name)) [] [(closure :name)])))

(defn asm-compile-scope-fields [name closure scope profile cw]
	(doseq [var scope]
		(.visitEnd (.visitField cw, 0, (str "_" var), (sig-obj qn-js-primitive), nil, nil))))

(defn asm-compile-scope-methods [name closure scope profile cw]
	(doseq [var scope]
		(doto (.visitMethod cw, Opcodes/ACC_PUBLIC, (str "get_" var), (sig-call (sig-obj qn-js-primitive)), nil, nil)
			(.visitCode)
			(.visitVarInsn Opcodes/ALOAD, 0)
			(.visitFieldInsn Opcodes/GETFIELD, name, (str "_" var), (sig-obj qn-js-primitive));
			(.visitInsn Opcodes/ARETURN)
			(.visitMaxs 1, 1)
			(.visitEnd))
		(doto (.visitMethod cw, Opcodes/ACC_PUBLIC, (str "set_" var), (sig-call (sig-obj qn-js-primitive) qn-void), nil, nil)
			(.visitCode)
			(.visitVarInsn Opcodes/ALOAD, 0)
			(.visitVarInsn Opcodes/ALOAD, 1)
			(.visitFieldInsn Opcodes/PUTFIELD, name, (str "_" var), (sig-obj qn-js-primitive));
			(.visitInsn Opcodes/RETURN)
			(.visitMaxs 2, 2)
			(.visitEnd))))

(defn asm-compile-scope-init [name closure scope profile cw]
  (let [mw (.visitMethod cw, 0, "<init>", (sig-call qn-void), nil, nil)]
  	(.visitCode mw)
  	(.visitVarInsn mw Opcodes/ALOAD, 0)
  	(.visitMethodInsn mw Opcodes/INVOKESPECIAL, qn-object, "<init>", "()V")  
  	(.visitInsn mw Opcodes/RETURN)
  	(.visitMaxs mw 1, 1)
  	(.visitEnd mw)))

(defn asm-compile-scope-classes [profile]
	(into {}
		(map-indexed (fn [i closure]
			(let [cw (new ClassWriter ClassWriter/COMPUTE_MAXS)
            scope (asm-compile-scope-vars closure)
				    qn (qn-js-scope i)]
				(.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), qn, nil, qn-object, nil)
				(asm-compile-scope-fields qn closure scope profile cw)
				(asm-compile-scope-methods qn closure scope profile cw)
				(asm-compile-scope-init qn closure scope profile cw)
				(.visitEnd cw)
				[(qn-js-scope i) (.toByteArray cw)]))
		(profile :closures))))