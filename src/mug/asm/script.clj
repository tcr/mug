(ns mug.asm.script
  (:use mug.asm.util))

(import (org.objectweb.asm ClassWriter Opcodes Label))

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