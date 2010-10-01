(ns mug.asm.object
  (:use mug.asm.util))

(import (org.objectweb.asm ClassWriter Opcodes Label))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; object
;

(defn asm-compile-object-methods [name accessors cw]
  (doseq [k (filter (fn [x] (not= x "prototype")) (vec accessors))]
		(doto (.visitMethod cw, Opcodes/ACC_PUBLIC, (str "get_" k), "()Lmug/JSPrimitive;", nil, nil)
			(.visitCode)
			(.visitVarInsn Opcodes/ALOAD, 0)
      (.visitLdcInsn k)
			(.visitMethodInsn Opcodes/INVOKEVIRTUAL, "mug/compiled/JSObject", "get", "(Ljava/lang/String;)Lmug/JSPrimitive;")
			(.visitInsn Opcodes/ARETURN)
      (.visitMaxs 1, 1)
			(.visitEnd))
		(doto (.visitMethod cw, Opcodes/ACC_PUBLIC, (str "set_" k), "(Lmug/JSPrimitive;)V", nil, nil)
			(.visitCode)
			(.visitVarInsn Opcodes/ALOAD, 0)
      (.visitLdcInsn k)
      (.visitVarInsn Opcodes/ALOAD, 1)
			(.visitMethodInsn Opcodes/INVOKEVIRTUAL, "mug/compiled/JSObject", "set", "(Ljava/lang/String;Lmug/JSPrimitive;)V")
			(.visitInsn Opcodes/RETURN)
      (.visitMaxs 1, 1)
			(.visitEnd))))

(defn asm-compile-object-init [name cw]
	(doto (.visitMethod cw, Opcodes/ACC_PUBLIC, "<init>", "()V", nil, nil)
		(.visitCode)
		(.visitVarInsn Opcodes/ALOAD, 0)
		(.visitMethodInsn Opcodes/INVOKESPECIAL, "mug/JSObjectBase", "<init>", "()V")
		(.visitInsn Opcodes/RETURN)
    (.visitMaxs 1, 1)
		(.visitEnd))
 (doto (.visitMethod cw, Opcodes/ACC_PUBLIC, "<init>", "(Lmug/compiled/JSObject;)V", nil, nil)
		(.visitCode)
		(.visitVarInsn Opcodes/ALOAD, 0)
		(.visitMethodInsn Opcodes/INVOKESPECIAL, "mug/JSObjectBase", "<init>", "()V")
    (.visitVarInsn Opcodes/ALOAD, 0)
    (.visitVarInsn Opcodes/ALOAD, 1)
    (.visitFieldInsn Opcodes/PUTFIELD, "mug/compiled/JSObject", "__proto__", "Lmug/compiled/JSObject;")
		(.visitInsn Opcodes/RETURN)
    (.visitMaxs 1, 1)
		(.visitEnd)))

(defn asm-compile-object-class [profile]
		(let [cw (new ClassWriter ClassWriter/COMPUTE_MAXS)
			name "mug/compiled/JSObject"]
			(.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), name, nil, "mug/JSObjectBase", nil)
			(asm-compile-object-methods name (profile :accessors) cw)
			(asm-compile-object-init name cw)
			(.visitEnd cw)
			(.toByteArray cw)))