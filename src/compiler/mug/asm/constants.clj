(ns mug.asm.constants
  (:use mug.asm.util))

(import (org.objectweb.asm ClassWriter Opcodes Label))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; constants
;

(defn asm-compile-constants-fields [ast cw]
(comment
	; undefined
	(.visitEnd (.visitField cw, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), "UNDEFINED", (sig-obj qn-js-undefined), nil, nil))

	; booleans
	(.visitEnd (.visitField cw, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), "TRUE", (sig-obj qn-js-boolean), nil, nil))
	(.visitEnd (.visitField cw, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), "FALSE", (sig-obj qn-js-boolean), nil, nil))
)

	; strings
	(doseq [[i v] (index (ast :strings))]
			(.visitEnd (.visitField cw, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), (ident-str i), (sig-obj qn-js-string), nil, nil)))
 
 	; numbers
	(doseq [[i v] (index (ast :numbers))]
			(.visitEnd (.visitField cw, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), (ident-num i), (sig-obj qn-js-number), nil, nil))))

(defn asm-compile-constants-clinit [ast cw]
	(let [mv (.visitMethod cw, Opcodes/ACC_STATIC, "<clinit>", (sig-call qn-void), nil, nil)]
		(.visitCode mv)

(comment
		; undefined
		(doto mv
			(.visitTypeInsn Opcodes/NEW (sig-obj qn-js-undefined))
			(.visitInsn Opcodes/DUP)
			(.visitMethodInsn Opcodes/INVOKESPECIAL, (sig-obj qn-js-undefined), "<init>", (sig-call qn-void))
			(.visitFieldInsn Opcodes/PUTSTATIC, qn-js-constants, "UNDEFINED", (sig-obj qn-js-undefined)))

		; booleans
		(doto mv
			(.visitTypeInsn Opcodes/NEW qn-js-boolean)
			(.visitInsn Opcodes/DUP)
			(.visitInsn Opcodes/ICONST_1)
			(.visitMethodInsn Opcodes/INVOKESPECIAL, qn-js-boolean, "<init>", (sig-call qn-boolean qn-void))
			(.visitFieldInsn Opcodes/PUTSTATIC, qn-js-constants, "TRUE", (sig-obj qn-js-boolean)))
		(doto mv
			(.visitTypeInsn Opcodes/NEW qn-js-boolean)
			(.visitInsn Opcodes/DUP)
			(.visitInsn Opcodes/ICONST_0)
			(.visitMethodInsn Opcodes/INVOKESPECIAL, qn-js-boolean, "<init>", (sig-call qn-boolean qn-void))
			(.visitFieldInsn Opcodes/PUTSTATIC, qn-js-constants, "FALSE", (sig-obj qn-js-boolean)))
)

		; strings
		(doseq [[i v] (index (ast :strings))]
			(doto mv
				(.visitTypeInsn Opcodes/NEW qn-js-string)
				(.visitInsn Opcodes/DUP)
				(.visitLdcInsn v)
				(.visitMethodInsn Opcodes/INVOKESPECIAL, qn-js-string, "<init>", (sig-call (sig-obj qn-string) qn-void))
				(.visitFieldInsn Opcodes/PUTSTATIC, qn-js-constants, (ident-str i), (sig-obj qn-js-string))))

		; numbers
		(doseq [[i v] (index (ast :numbers))]
			(doto mv
				(.visitTypeInsn Opcodes/NEW qn-js-number)
				(.visitInsn Opcodes/DUP)
				(.visitLdcInsn (new Double (double v)))
				(.visitMethodInsn Opcodes/INVOKESPECIAL, qn-js-number, "<init>", (sig-call qn-double qn-void))
				(.visitFieldInsn Opcodes/PUTSTATIC, qn-js-constants, (ident-num i), (sig-obj qn-js-number))))

		(doto mv
			(.visitInsn Opcodes/RETURN)
			(.visitMaxs 4, 0)
			(.visitEnd))))

(defn asm-compile-constants-class [ast]
	(let [cw (new ClassWriter ClassWriter/COMPUTE_MAXS)]
		(.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), qn-js-constants, nil, qn-object, nil)
		(asm-compile-constants-fields ast cw)
		(asm-compile-constants-clinit ast cw)
		(.visitEnd cw)
		(.toByteArray cw)))