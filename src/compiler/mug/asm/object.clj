(ns mug.asm.object
  (:use mug.asm.util))

(import (org.objectweb.asm ClassWriter Opcodes Label))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; object
;

(defn asm-compile-object-methods [accessors cw]
  ;[TODO] not filter out "prototype"
  (doseq [k (filter (fn [x] (not= x "prototype")) (vec accessors))]
		(doto (.visitMethod cw, Opcodes/ACC_PUBLIC, (str "get_" k), "()Lmug/js/JSPrimitive;", nil, nil)
			(.visitCode)
			(.visitVarInsn Opcodes/ALOAD, 0)
      (.visitLdcInsn k)
			(.visitMethodInsn Opcodes/INVOKEVIRTUAL, qn-js-object, "get", "(Ljava/lang/String;)Lmug/js/JSPrimitive;")
			(.visitInsn Opcodes/ARETURN)
      (.visitMaxs 1, 1)
			(.visitEnd))
		(doto (.visitMethod cw, Opcodes/ACC_PUBLIC, (str "set_" k), "(Lmug/js/JSPrimitive;)V", nil, nil)
			(.visitCode)
			(.visitVarInsn Opcodes/ALOAD, 0)
      (.visitLdcInsn k)
      (.visitVarInsn Opcodes/ALOAD, 1)
			(.visitMethodInsn Opcodes/INVOKEVIRTUAL, qn-js-object, "set", "(Ljava/lang/String;Lmug/js/JSPrimitive;)V")
			(.visitInsn Opcodes/RETURN)
      (.visitMaxs 1, 1)
			(.visitEnd))))

(defn asm-compile-object-init [cw]
	(doto (.visitMethod cw, Opcodes/ACC_PUBLIC, "<init>", "()V", nil, nil)
		(.visitCode)
		(.visitVarInsn Opcodes/ALOAD, 0)
		(.visitMethodInsn Opcodes/INVOKESPECIAL, qn-js-objectbase, "<init>", "()V")
		(.visitInsn Opcodes/RETURN)
    (.visitMaxs 1, 1)
		(.visitEnd))
 (doto (.visitMethod cw, Opcodes/ACC_PUBLIC, "<init>", "(Lmug/js/compiled/JSObject;)V", nil, nil)
		(.visitCode)
		(.visitVarInsn Opcodes/ALOAD, 0)
		(.visitMethodInsn Opcodes/INVOKESPECIAL, "mug/js/JSObjectBase", "<init>", "()V")
    (.visitVarInsn Opcodes/ALOAD, 0)
    (.visitVarInsn Opcodes/ALOAD, 1)
    (.visitFieldInsn Opcodes/PUTFIELD, qn-js-object, "__proto__", (sig-obj qn-js-object))
		(.visitInsn Opcodes/RETURN)
    (.visitMaxs 1, 1)
		(.visitEnd)))

(defn asm-compile-object-class [ast]
		(let [cw (new ClassWriter ClassWriter/COMPUTE_MAXS)]
			(.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), qn-js-object, nil, qn-js-objectbase, nil)
			(asm-compile-object-methods (ast :accessors) cw)
			(asm-compile-object-init cw)
			(.visitEnd cw)
			(.toByteArray cw)))