(ns mug.asm.constants
  (:use
    [mug.asm util analyze config]
    mug.ast))

(import (org.objectweb.asm ClassWriter Opcodes Label))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; constants
;

(defn asm-compile-constants-fields [ast cw]
	; strings
	(doseq [[i v] (index (ast-strings ast))]
			(.visitEnd (.visitField cw, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), (ident-str i), (sig-obj qn-js-string), nil, nil)))
 
 	; numbers
	(doseq [[i v] (index (ast-numbers ast))]
			(.visitEnd (.visitField cw, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), (ident-num i), (sig-obj qn-js-number), nil, nil)))

 	; regexes
	(doseq [[i [expr flags]] (index (ast-regexps ast))]
			(.visitEnd (.visitField cw, (+ Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC), (ident-regex i), (sig-obj qn-pattern), nil, nil))))

(defn asm-compile-constants-clinit [ast cw]
	(let [mv (.visitMethod cw, Opcodes/ACC_STATIC, "<clinit>", (sig-call sig-void), nil, nil)]
		(.visitCode mv)

		; strings
		(doseq [[i v] (index (ast-strings ast))]
			(doto mv
				(.visitTypeInsn Opcodes/NEW qn-js-string)
				(.visitInsn Opcodes/DUP)
				(.visitLdcInsn v)
				(.visitMethodInsn Opcodes/INVOKESPECIAL, qn-js-string, "<init>", (sig-call (sig-obj qn-string) sig-void))
				(.visitFieldInsn Opcodes/PUTSTATIC, (qn-js-constants), (ident-str i), (sig-obj qn-js-string))))

		; numbers
		(doseq [[i v] (index (ast-numbers ast))]
			(doto mv
				(.visitTypeInsn Opcodes/NEW qn-js-number)
				(.visitInsn Opcodes/DUP)
				(.visitLdcInsn (new Double (double v)))
				(.visitMethodInsn Opcodes/INVOKESPECIAL, qn-js-number, "<init>", (sig-call sig-double sig-void))
				(.visitFieldInsn Opcodes/PUTSTATIC, (qn-js-constants), (ident-num i), (sig-obj qn-js-number))))
  
    ; regex
		(doseq [[i [expr flags]] (index (ast-regexps ast))]
			(doto mv
				(.visitLdcInsn expr)
        (.visitLdcInsn flags)
				(.visitMethodInsn Opcodes/INVOKESTATIC, qn-js-utils, "compilePattern", (sig-call (sig-obj qn-string) (sig-obj qn-string) (sig-obj qn-pattern)))
				(.visitFieldInsn Opcodes/PUTSTATIC, (qn-js-constants), (ident-regex i), (sig-obj qn-pattern))))

		(doto mv
			(.visitInsn Opcodes/RETURN)
			(.visitMaxs 4, 0)
			(.visitEnd))))

(defn asm-compile-constants-class [ast]
	(let [cw (new ClassWriter ClassWriter/COMPUTE_MAXS)]
		(.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), (qn-js-constants), nil, qn-object, nil)
		(asm-compile-constants-fields ast cw)
		(asm-compile-constants-clinit ast cw)
		(.visitEnd cw)
		(.toByteArray cw)))