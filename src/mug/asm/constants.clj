(ns mug.asm.constants
  (:use mug.asm.util))

(import (org.objectweb.asm ClassWriter Opcodes Label))

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